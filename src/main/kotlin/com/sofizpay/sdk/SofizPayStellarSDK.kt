package com.sofizpay.sdk

import com.google.gson.Gson
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import org.stellar.sdk.*
import org.stellar.sdk.exception.BadRequestException
import org.stellar.sdk.exception.BadResponseException
import org.stellar.sdk.exception.NetworkException
import org.stellar.sdk.operations.PaymentOperation
import org.stellar.sdk.requests.RequestBuilder
import org.stellar.sdk.responses.AccountResponse
import org.stellar.sdk.responses.Page
import org.stellar.sdk.responses.TransactionResponse
import org.stellar.sdk.responses.operations.OperationResponse
import org.stellar.sdk.responses.operations.PaymentOperationResponse
import java.io.IOException
import java.math.BigDecimal
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.security.*
import java.security.spec.X509EncodedKeySpec
import java.time.Instant
import java.util.*
import java.util.concurrent.*
import javax.crypto.Cipher


class SofizPayStellarSDK(private val isTestnet: Boolean = true) : AutoCloseable {
    
    companion object {
        private const val VERSION = "1.0.0-kotlin"
        private const val DZT_ASSET_CODE = "DZT"
        private const val DZT_ISSUER = "GCAZI7YBLIDJWIVEL7ETNAZGPP3LC24NO6KAOBWZHUERXQ7M5BC52DLV"
        private const val CUTOFF_DATE = "2025-07-18T16:14:16Z"
        
        init {
            try {
                val bcProvider = Class.forName("org.bouncycastle.jce.provider.BouncyCastleProvider")
                val provider = bcProvider.getDeclaredConstructor().newInstance() as Provider
                Security.addProvider(provider)
            } catch (e: Exception) {
                println("Note: BouncyCastle provider not available - signature verification will use standard Java crypto")
            }
        }
    }
    
    private val server: Server
    private val network: Network
    
    private val activeStreams = ConcurrentHashMap<String, StreamInfo>()
    private val transactionCallbacks = ConcurrentHashMap<String, TransactionCallback>()
    private var streamExecutor: ScheduledExecutorService? = null
    
    private val httpClient = OkHttpClient()
    private val gson = Gson()
    
    private var lastProcessedHash = ""
    
    init {
        streamExecutor = Executors.newScheduledThreadPool(2)
        
        if (isTestnet) {
            server = Server("https://horizon-testnet.stellar.org")
            network = Network.TESTNET
        } else {
            server = Server("https://horizon.stellar.org")
            network = Network.PUBLIC
        }
    }

    fun getVersion(): String = VERSION

    fun getPublicKey(secretKey: String): PublicKeyResult {
        return try {
            val keyPair = org.stellar.sdk.KeyPair.fromSecretSeed(secretKey)
            val publicKey = keyPair.accountId
            PublicKeyResult(true, "Public key extracted successfully", publicKey)
        } catch (e: Exception) {
            PublicKeyResult(false, "Error extracting public key: ${e.message}", null)
        }
    }
    

    fun getBalance(accountId: String): BalanceResult {
        return try {
            val account = server.accounts().account(accountId)
            
            for (balance in account.balances) {
                when {
                    balance.assetType.equals("native") -> {
                        return BalanceResult(true, "Balance retrieved successfully", "${balance.balance}")
                    }
                    balance.assetCode == DZT_ASSET_CODE -> {
                        return BalanceResult(true, "Balance retrieved successfully", "${balance.balance}")
                    }
                }
            }
            
            if (account.balances.isNotEmpty()) {
                val nativeBalance = account.balances[0]
                return BalanceResult(true, "Balance not found, showing native balance", "${nativeBalance.balance}")
            }
            
            BalanceResult(true, "No balances found", "0")
            
        } catch (e: Exception) {
            BalanceResult(false, "Error fetching balance: ${e.message}", "0")
        }
    }
    

    fun submit(paymentData: PaymentData): PaymentResult {
        // Validation
        if (paymentData.secret.isNullOrEmpty()) {
            return PaymentResult(false, "Secret key is required.", null)
        }
        
        if (paymentData.destination.isNullOrEmpty()) {
            return PaymentResult(false, "Destination address is required.", null)
        }
        
        if (paymentData.amount.isNullOrEmpty()) {
            return PaymentResult(false, "Amount is required.", null)
        }
        
        return try {
            val sourceKeyPair = org.stellar.sdk.KeyPair.fromSecretSeed(paymentData.secret)
            
            val sourceAccount = server.loadAccount(sourceKeyPair.accountId)
            
            val amount = BigDecimal(paymentData.amount)
            
            val asset = Asset.createNonNativeAsset(DZT_ASSET_CODE, DZT_ISSUER)
            
            val destination = paymentData.destination!!
            val paymentOperation = PaymentOperation.builder()
                .destination(destination)
                .asset(asset)
                .amount(amount)
                .build()
            
            val transactionBuilder = TransactionBuilder(sourceAccount, network)
                .setBaseFee(Transaction.MIN_BASE_FEE)
                .addOperation(paymentOperation)
                .setTimeout(30)
            
            if (!paymentData.memo.isNullOrEmpty()) {
                transactionBuilder.addMemo(Memo.text(paymentData.memo))
            }
            
            val transaction = transactionBuilder.build()
            
            transaction.sign(sourceKeyPair)
            
            val response = server.submitTransaction(transaction)
            
            val transactionId = response.hash
            println("✅ Payment sent successfully! Transaction: $transactionId")
            
            PaymentResult(true, "Payment submitted successfully", transactionId)
            
        } catch (e: BadRequestException) {
            PaymentResult(false, "Bad request: ${e.message}", null)
        } catch (e: BadResponseException) {
            PaymentResult(false, "Bad response: ${e.message}", null)
        } catch (e: NetworkException) {
            PaymentResult(false, "Network error: ${e.message}", null)
        } catch (e: Exception) {
            PaymentResult(false, "Error submitting payment: ${e.message}", null)
        }
    }
    

    fun getTransactions(accountId: String, limit: Int): TransactionHistoryResult {
        return try {
            val transactionsPage = server.transactions()
                .forAccount(accountId)
                .limit(limit)
                .order(RequestBuilder.Order.DESC)
                .execute()
            
            val transactions = mutableListOf<TransactionInfo>()
            
            for (tx in transactionsPage.records) {
                val txInfo = TransactionInfo()
                txInfo.apply {
                    id = tx.hash
                    hash = tx.hash
                    created_at = tx.createdAt
                    source_account = tx.sourceAccount
                    fee_charged = tx.feeCharged.toString()
                    successful = tx.successful
                    operation_count = tx.operationCount
                    
                    if (tx.memo != null) {
                        memo = tx.memo.toString()
                    }
                }
                
                try {
                    val operations = server.operations().forTransaction(tx.hash).execute()
                    
                    for (operation in operations.records) {
                        if (operation.type == "payment") {
                            val paymentOp = operation as PaymentOperationResponse
                            txInfo.apply {
                                amount = paymentOp.amount
                                from = paymentOp.from
                                to = paymentOp.to
                                
                                type = when {
                                    paymentOp.from == accountId -> "sent"
                                    paymentOp.to == accountId -> "received"
                                    else -> "unknown"
                                }
                                
                                if (paymentOp.asset.type.equals("native")) {
                                    asset_type = "native"
                                    asset_code = "DZT"
                                } else {
                                    asset_type = "credit_alphanum4"
                                    asset_code = paymentOp.asset.toString()
                                }
                            }
                            break
                        }
                    }
                } catch (e: Exception) {
                    System.err.println("Cannot fetch operation details for transaction: ${tx.hash}")
                }
                
                transactions.add(txInfo)
            }
            
            TransactionHistoryResult(
                true,
                "Fetched all transactions (${transactions.size} transactions)",
                transactions
            )
            
        } catch (e: Exception) {
            TransactionHistoryResult(false, "Error fetching transactions: ${e.message}", emptyList())
        }
    }
    

    fun searchTransactionsByMemo(accountId: String, memo: String, limit: Int): SearchTransactionsResult {
        return try {
            val transactionsPage = server.transactions()
                .forAccount(accountId)
                .limit(maxOf(limit * 5, 100)) 
                .order(RequestBuilder.Order.DESC)
                .execute()
            
            val matchingTransactions = mutableListOf<TransactionInfo>()
            
            for (tx in transactionsPage.records) {
                if (tx.memo != null && tx.memo.toString().contains(memo)) {
                    val txInfo = TransactionInfo().apply {
                        id = tx.hash
                        hash = tx.hash
                        created_at = tx.createdAt
                        source_account = tx.sourceAccount
                        this.memo = tx.memo.toString()
                        successful = tx.successful
                    }
                    
                    try {
                        val operations = server.operations().forTransaction(tx.hash).execute()
                        for (operation in operations.records) {
                            if (operation.type == "payment") {
                                val paymentOp = operation as PaymentOperationResponse
                                txInfo.apply {
                                    amount = paymentOp.amount
                                    from = paymentOp.from
                                    to = paymentOp.to
                                }
                                break
                            }
                        }
                    } catch (e: Exception) {
                    }
                    
                    matchingTransactions.add(txInfo)
                    
                    if (matchingTransactions.size >= limit) {
                        break
                    }
                }
            }
            
            SearchTransactionsResult(
                true,
                "Found ${matchingTransactions.size} transactions containing \"$memo\"",
                matchingTransactions
            )
            
        } catch (e: Exception) {
            SearchTransactionsResult(false, "Error searching transactions: ${e.message}", emptyList())
        }
    }

    fun getTransactionByHash(transactionHash: String): TransactionByHashResult {
        return try {
            val transaction = server.transactions().transaction(transactionHash)
            
            val txInfo = TransactionInfo().apply {
                id = transaction.hash
                hash = transaction.hash
                created_at = transaction.createdAt
                source_account = transaction.sourceAccount
                successful = transaction.successful
                fee_charged = transaction.feeCharged.toString()
                operation_count = transaction.operationCount
                
                if (transaction.memo != null) {
                    memo = transaction.memo.toString()
                }
            }
            
            try {
                val operations = server.operations().forTransaction(transactionHash).execute()
                for (operation in operations.records) {
                    if (operation.type == "payment") {
                        val paymentOp = operation as PaymentOperationResponse
                        txInfo.apply {
                            amount = paymentOp.amount
                            from = paymentOp.from
                            to = paymentOp.to
                        }
                        break
                    }
                }
            } catch (e: Exception) {
            }
            
            TransactionByHashResult(true, "Transaction found", true, txInfo, transactionHash)
            
        } catch (e: Exception) {
            TransactionByHashResult(true, "Transaction not found", false, null, transactionHash)
        }
    }


    fun startTransactionStream(accountId: String, callback: TransactionCallback): StreamResult {
        return try {
            if (activeStreams.containsKey(accountId)) {
                return StreamResult(false, "Stream already active for this account")
            }
            
            org.stellar.sdk.KeyPair.fromAccountId(accountId)
            
            val streamInfo = StreamInfo().apply {
                this.accountId = accountId
                isActive = true
                startTime = Instant.now()
            }
            
            activeStreams[accountId] = streamInfo
            transactionCallbacks[accountId] = callback
            
            streamExecutor?.scheduleAtFixedRate({
                monitorTransactions(accountId)
            }, 0, 5, TimeUnit.SECONDS) 
            
            println("🚀 Started transaction stream for: $accountId")
            StreamResult(true, "Transaction stream started successfully")
            
        } catch (e: Exception) {
            StreamResult(false, "Error starting stream: ${e.message}")
        }
    }
    

    fun stopTransactionStream(accountId: String): StreamResult {
        val streamInfo = activeStreams.remove(accountId)
        transactionCallbacks.remove(accountId)
        
        return if (streamInfo != null) {
            streamInfo.isActive = false
            println("⏹️ Stopped transaction stream for: $accountId")
            StreamResult(true, "Transaction stream stopped successfully")
        } else {
            StreamResult(false, "No active stream found for this account")
        }
    }
    

    fun getStreamStatus(accountId: String): StreamStatusResult {
        val streamInfo = activeStreams[accountId]
        
        return if (streamInfo != null) {
            StreamStatusResult(true, "Stream status retrieved", true, streamInfo.startTime)
        } else {
            StreamStatusResult(true, "No active stream found", false, null)
        }
    }
    
 
    private fun monitorTransactions(accountId: String) {
        try {
            val streamInfo = activeStreams[accountId] ?: return
            if (!streamInfo.isActive) return
            
            val transactionsPage = server.transactions()
                .forAccount(accountId)
                .order(RequestBuilder.Order.DESC)
                .limit(10)
                .execute()
            
            val transactions = transactionsPage.records
            
            if (streamInfo.lastProcessedHash == null && transactions.isNotEmpty()) {
                streamInfo.lastProcessedHash = transactions[0].hash
                return
            }
            
            for (transaction in transactions) {
                if (transaction.hash == streamInfo.lastProcessedHash) {
                    break 
                }
                
                val callback = transactionCallbacks[accountId]
                if (callback != null) {
                    val txInfo = TransactionInfo().apply {
                        id = transaction.hash
                        hash = transaction.hash
                        created_at = transaction.createdAt
                        source_account = transaction.sourceAccount
                        successful = transaction.successful
                    }
                    
                    callback.onNewTransaction(txInfo)
                }
            }
            
            if (transactions.isNotEmpty()) {
                streamInfo.lastProcessedHash = transactions[0].hash
            }
            
        } catch (e: Exception) {
            System.err.println("Error monitoring transactions: ${e.message}")
        }
    }
    

    fun makeCIBTransaction(transactionData: CIBTransactionData): CIBTransactionResult {
        if (transactionData.account.isNullOrEmpty()) {
            return CIBTransactionResult(false, "Account is required.", null)
        }
        
        if (transactionData.amount.isNullOrEmpty()) {
            return CIBTransactionResult(false, "Amount is required.", null)
        }
        
        if (transactionData.full_name.isNullOrEmpty()) {
            return CIBTransactionResult(false, "Full name is required.", null)
        }
        
        if (transactionData.phone.isNullOrEmpty()) {
            return CIBTransactionResult(false, "Phone is required.", null)
        }
        
        if (transactionData.email.isNullOrEmpty()) {
            return CIBTransactionResult(false, "Email is required.", null)
        }
        
        return try {
            // Build URL with parameters
            val urlBuilder = StringBuilder("https://www.sofizpay.com/make-cib-transaction?")
            urlBuilder.append("account=").append(java.net.URLEncoder.encode(transactionData.account, "UTF-8"))
            urlBuilder.append("&amount=").append(java.net.URLEncoder.encode(transactionData.amount, "UTF-8"))
            urlBuilder.append("&full_name=").append(java.net.URLEncoder.encode(transactionData.full_name, "UTF-8"))
            urlBuilder.append("&phone=").append(java.net.URLEncoder.encode(transactionData.phone, "UTF-8"))
            urlBuilder.append("&email=").append(java.net.URLEncoder.encode(transactionData.email, "UTF-8"))
            
            if (!transactionData.memo.isNullOrEmpty()) {
                urlBuilder.append("&memo=").append(java.net.URLEncoder.encode(transactionData.memo, "UTF-8"))
            }
            if (!transactionData.return_url.isNullOrEmpty()) {
                urlBuilder.append("&return_url=").append(java.net.URLEncoder.encode(transactionData.return_url, "UTF-8"))
            }
            urlBuilder.append("&redirect=").append(if (transactionData.redirect) "yes" else "no")
            
            val fullUrl = urlBuilder.toString()
            
            val request = Request.Builder()
                .url(fullUrl)
                .get()
                .addHeader("Accept", "application/json")
                .build()
            
            httpClient.newCall(request).execute().use { response ->
                val responseBody = response.body?.string() ?: ""
                
                if (response.isSuccessful) {
                    @Suppress("UNCHECKED_CAST")
                    val responseData = gson.fromJson(responseBody, Map::class.java) as Map<String, Any>
                    CIBTransactionResult(true, "CIB transaction completed successfully", responseData)
                } else {
                    CIBTransactionResult(false, "CIB transaction failed: ${response.code} - $responseBody", null)
                }
            }
            
        } catch (e: Exception) {
            CIBTransactionResult(false, "Error making CIB transaction: ${e.message}", null)
        }
    }
    

    fun verifySignature(message: String?, signatureUrlSafe: String?): Boolean {
        if (message.isNullOrEmpty() || signatureUrlSafe.isNullOrEmpty()) {
            return false
        }
        
        val publicKeyPem = """
            -----BEGIN PUBLIC KEY-----
            MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEA1N+bDPxpqeB9QB0affr/
            02aeRXAAnqHuLrgiUlVNdXtF7t+2w8pnEg+m9RRlc+4YEY6UyKTUjVe6k7v2p8Jj
            UItk/fMNOEg/zY222EbqsKZ2mF4hzqgyJ3QHPXjZEEqABkbcYVv4ZyV2Wq0x0ykI
            +Hy/5YWKeah4RP2uEML1FlXGpuacnMXpW6n36dne3fUN+OzILGefeRpmpnSGO5+i
            JmpF2mRdKL3hs9WgaLSg6uQyrQuJA9xqcCpUmpNbIGYXN9QZxjdyRGnxivTE8awx
            THV3WRcKrP2krz3ruRGF6yP6PVHEuPc0YDLsYjV5uhfs7JtIksNKhRRAQ16bAsj/
            9wIDAQAB
            -----END PUBLIC KEY-----
        """.trimIndent()
        
        return try {
            var base64 = signatureUrlSafe
                .replace('-', '+')
                .replace('_', '/')
            
            while (base64.length % 4 != 0) {
                base64 += '='
            }
            
            val signatureBytes = java.util.Base64.getDecoder().decode(base64)
            
            val publicKeyContent = publicKeyPem
                .replace("-----BEGIN PUBLIC KEY-----", "")
                .replace("-----END PUBLIC KEY-----", "")
                .replace("\\s".toRegex(), "")
            
            val publicKeyBytes = java.util.Base64.getDecoder().decode(publicKeyContent)
            val keySpec = X509EncodedKeySpec(publicKeyBytes)
            val keyFactory = KeyFactory.getInstance("RSA")
            val publicKey = keyFactory.generatePublic(keySpec)
            
            // Verify signature
            val signature = Signature.getInstance("SHA256withRSA")
            signature.initVerify(publicKey)
            signature.update(message.toByteArray(Charsets.UTF_8))
            
            signature.verify(signatureBytes)
            
        } catch (e: Exception) {
            System.err.println("Signature verification error: ${e.message}")
            false
        }
    }
    

    fun fundAccountFromFaucet(accountId: String): FundResult {
        if (!isTestnet) {
            return FundResult(false, "Faucet funding only available on testnet")
        }
        
        return try {
            val client = HttpClient.newHttpClient()
            val faucetUrl = "https://friendbot.stellar.org?addr=$accountId"
            
            val request = HttpRequest.newBuilder()
                .uri(URI.create(faucetUrl))
                .GET()
                .build()
            
            val response = client.send(request, HttpResponse.BodyHandlers.ofString())
            
            if (response.statusCode() == 200) {
                FundResult(true, "Account funded successfully! Added 10,000 XLM to your account")
            } else {
                FundResult(false, "Failed to fund account: ${response.statusCode()}")
            }
        } catch (e: IOException) {
            FundResult(false, "Error funding account: ${e.message}")
        } catch (e: InterruptedException) {
            FundResult(false, "Error funding account: ${e.message}")
        }
    }
    



    override fun close() {
        for (accountId in ArrayList(activeStreams.keys)) {
            stopTransactionStream(accountId)
        }
        
        streamExecutor?.let { executor ->
            executor.shutdown()
            try {
                if (!executor.awaitTermination(5, TimeUnit.SECONDS)) {
                    executor.shutdownNow()
                }	else{
					println("✅ All streams stopped and executor shutdown gracefully")
				}
            } catch (e: InterruptedException) {
                executor.shutdownNow()
            }
        }
        
        try {
            server.close()
            println("🔌 SofizPay SDK connection closed")
        } catch (e: Exception) {
            System.err.println("Error closing SDK: ${e.message}")
        }
    }
    
    data class PaymentData(
        var secret: String? = null,
        var destination: String? = null,
        var amount: String? = null,
        var memo: String? = null
    )
    
    data class PaymentResult(
        val success: Boolean,
        val message: String,
        val transactionId: String?
    )
    
    data class PublicKeyResult(
        val success: Boolean,
        val message: String,
        val publicKey: String?
    )
    
    data class BalanceResult(
        val success: Boolean,
        val message: String,
        val balance: String
    )
    
    class TransactionInfo {
        var id: String? = null
        var hash: String? = null
        var created_at: String? = null
        var source_account: String? = null
        var memo: String? = null
        var amount: String? = null
        var from: String? = null
        var to: String? = null
        var type: String? = null
        var asset_type: String? = null
        var asset_code: String? = null
        var fee_charged: String? = null
        var successful: Boolean = false
        var operation_count: Int = 0
    }
    
    data class TransactionHistoryResult(
        val success: Boolean,
        val message: String,
        val transactions: List<TransactionInfo>
    )
    
    data class SearchTransactionsResult(
        val success: Boolean,
        val message: String,
        val transactions: List<TransactionInfo>
    )
    
    data class TransactionByHashResult(
        val success: Boolean,
        val message: String,
        val found: Boolean,
        val transaction: TransactionInfo?,
        val hash: String
    )
    
    data class StreamResult(
        val success: Boolean,
        val message: String
    )
    
    data class StreamStatusResult(
        val success: Boolean,
        val message: String,
        val active: Boolean,
        val startTime: Instant?
    )
    
    data class CIBTransactionData(
        var account: String? = null,
        var amount: String? = null,
        var full_name: String? = null,
        var phone: String? = null,
        var email: String? = null,
        var memo: String? = null,
        var return_url: String? = null,
        var redirect: Boolean = true
    )
    
    data class CIBTransactionResult(
        val success: Boolean,
        val message: String,
        val data: Map<String, Any>?
    )
    
    data class SignatureVerificationResult(
        val valid: Boolean,
        val message: String,
        val data: String
    )
    
    data class FundResult(
        val success: Boolean,
        val message: String
    )
    
    data class AccountCreationResult(
        val success: Boolean,
        val message: String,
        val accountId: String?,
        val secretKey: String?
    )
    
    fun interface TransactionCallback {
        fun onNewTransaction(transaction: TransactionInfo)
    }
    
    private class StreamInfo {
        var accountId: String? = null
        var isActive: Boolean = false
        var startTime: Instant? = null
        var lastProcessedHash: String? = null
    }
}
