package com.sofizpay.sdk

class CompleteSofizPayExample {
    
    fun execute() {
        println("=== SofizPay Stellar SDK Example - Kotlin ===")

        val useMainnet = false
        val sdk = SofizPayStellarSDK(useMainnet)
        println("SDK Version: ${sdk.getVersion()}")

        val mySecretKey = "YOUR_SECRET_KEY"
        val myPublicKey = "YOUR_PUBLIC_KEY"
        val destination = "YOUR_DESTINATION_PUBLIC_KEY"

        println("\n🔑 Get Public Key from Secret:")
        val publicKeyResult = sdk.getPublicKey(mySecretKey)
        println("Result: ${publicKeyResult.publicKey}")
        println("Message: ${publicKeyResult.message}")

        println("\n💰 Get DZT Balance:")
        val balanceResult = sdk.getBalance(myPublicKey)
        println("Balance: ${balanceResult.balance}")
        println("Message: ${balanceResult.message}")

        println("\n💸 Send Payment:")
        val payment = SofizPayStellarSDK.PaymentData(
            secret = mySecretKey,
            destination = destination,
            amount = "1",
            memo = "WC_ORDER_KOTLIN"
        )
        
        val paymentResult = sdk.submit(payment)
        println("Transaction ID: ${paymentResult.transactionId}")
        println("Message: ${paymentResult.message}")

        println("\n📜 Transaction History:")
        val history = sdk.getTransactions(myPublicKey, 5)
        for (tx in history.transactions) {
            println("- TX ID: ${tx.id} | Amount: ${tx.amount} DZT")
        }

        println("\n🔍 Search Transactions by Memo:")
        val search = sdk.searchTransactionsByMemo(myPublicKey, "WC_ORDER", 20)
        println("Search Result: ${if (search.success) "✅ Success" else "❌ Failed"}")
        println("Message: ${search.message}")
        for (tx in search.transactions) {
            println("- Found: ${tx.memo} | Amount: ${tx.amount}")
        }
        
        println("\n📡 Start Transaction Stream:")
        val streamResult = sdk.startTransactionStream(myPublicKey) { transaction ->
            println("🚨 New Transaction Detected!")
            println("   - Transaction ID: ${transaction.id}")
            println("   - Source: ${transaction.source_account?.substring(0, 8)}...")
            println("   - Success: ${if (transaction.successful) "✅ Yes" else "❌ No"}")
            println("   - Time: ${transaction.created_at}")
        }
        println("Stream Status: ${if (streamResult.success) "✅ Active" else "❌ Failed"}")
        println("Message: ${streamResult.message}")
        
        println("\n📊 Check Stream Status:")
        val statusResult = sdk.getStreamStatus(myPublicKey)
        println("Status: ${if (statusResult.success) "✅ Success" else "❌ Failed"}")
        println("Active: ${if (statusResult.active) "✅ Yes" else "❌ No"}")
        statusResult.startTime?.let {
            println("Start Time: $it")
        }

        println("\n⏰ Waiting 10 seconds for stream demonstration...")
        try {
            Thread.sleep(10000)
        } catch (e: InterruptedException) {
            Thread.currentThread().interrupt()
        }

        println("\n🛑 Stop Transaction Stream:")
        val stopResult = sdk.stopTransactionStream(myPublicKey)
        println("Stop Result: ${if (stopResult.success) "✅ Success" else "❌ Failed"}")
        println("Message: ${stopResult.message}")

        println("\n🔐 Verify Signature:")
        val testMessage = "wc_order_LI3SLQ8xA7IY9cib84907success23400"
        val testSignature = "jHrONYl2NuBhjAYTgRq3xwRuW2ZYZIQlx1VWgiObu5FrSnY78pQ-CV0pAjRKWAje-DDZHhvMvzIFSBE9rj87xsWymjWYVlyZmuVr-sDPSa-zWZRsyWJhdj0XPZir4skkDFWlhaWpwtLql0D7N5yw_zu67plVuhaPk4d_jOhn0O0qN3scROa1H1pIAPhIreQHu72-Bx4v2g-NGceFVpiAMyf2j2rvVthkg4o6adxY_E0-y_AJfnJdL1HhmWOBpFEUk6ziV1aFzSJIo-XpueJSFWpL7wrAsQ6shcLE3zQSZXJoXhdR7nr92-Y7SXgEE_a9kP_Q4uExJCWcaOcPkQ5Bgg=="
        
        println("Test Message: \"$testMessage\"")
        val sigResult = sdk.verifySignature(testMessage, testSignature)
        println("Signature Valid? ${if (sigResult) "✅ Yes" else "❌ No"}")

        println("\n🆕 Create New Account:")
        val newAccount = sdk.createAccount()
        if (newAccount.success) {
            println("New Account Created:")
            println("   - Public Key: ${newAccount.accountId}")
            println("   - Secret Key: ${newAccount.secretKey}")
            
            if (!useMainnet) {
                println("\n💧 Fund New Account from Faucet:")
                newAccount.accountId?.let { accountId ->
                    val fundResult = sdk.fundAccountFromFaucet(accountId)
                    println("Fund Result: ${if (fundResult.success) "✅ Success" else "❌ Failed"}")
                    println("Message: ${fundResult.message}")
                }
            }
        }

        println("\n🏦 CIB Transaction Test:")
        val cibTransaction = SofizPayStellarSDK.CIBTransactionData(
            account = "YOUR_PUBLIC_KEY",
            amount = "100.00",
phone = "+213*********";
email = "ahmed@sofizpay.com";
memo = "Payment";
return_url = "https://yoursite.com/payment-success";
redirect = true;
        )
        
        val cibResult = sdk.makeCIBTransaction(cibTransaction)
        println("CIB Result: ${if (cibResult.success) "✅ Success" else "❌ Failed"}")
        println("Message: ${cibResult.message}")
        cibResult.data?.let { data ->
            println("Response Data: $data")
        }

        println("\n✅ All Done!")
        println("🎉 SofizPay Kotlin SDK Test Complete!")

        sdk.use {
            println("🧹 Resources cleaned up")
        }
    }
}



// يمكن استدعاؤها من أي مكان آخر مثل:
// val example = CompleteSofizPayExample()
// example.execute()