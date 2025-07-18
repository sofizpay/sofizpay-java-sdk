package com.sofizpay.sdk;

import com.google.gson.Gson;
import okhttp3.*;
import org.stellar.sdk.*;
import org.stellar.sdk.responses.AccountResponse;
import org.stellar.sdk.responses.TransactionResponse;
import org.stellar.sdk.responses.Page;
import org.stellar.sdk.operations.PaymentOperation;
import org.stellar.sdk.exception.BadRequestException;
import org.stellar.sdk.exception.BadResponseException;
import org.stellar.sdk.exception.NetworkException;
import org.stellar.sdk.requests.RequestBuilder;
import org.stellar.sdk.responses.operations.PaymentOperationResponse;

import java.io.IOException;
import java.math.BigDecimal;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.List;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import javax.crypto.Cipher;
import java.security.*;
import java.security.spec.X509EncodedKeySpec;
import java.time.Instant;


public class SofizPayStellarSDK {
    
    private static final String VERSION = "1.0.0";
    private static final String DZT_ASSET_CODE = "DZT";
    private static final String DZT_ISSUER = "GCAZI7YBLIDJWIVEL7ETNAZGPP3LC24NO6KAOBWZHUERXQ7M5BC52DLV";
    
    private final Server server;
    private final Network network;
    private final boolean isTestnet;
    
    private final Map<String, StreamInfo> activeStreams = new ConcurrentHashMap<>();
    private final Map<String, TransactionCallback> transactionCallbacks = new ConcurrentHashMap<>();
    private ScheduledExecutorService streamExecutor;
    
    private final OkHttpClient httpClient;
    private final Gson gson;
    
    private String lastProcessedHash = "";
    private final String CUTOFF_DATE = "2025-07-18T16:14:16Z";
    
    static {
        try {
            Class<?> bcProvider = Class.forName("org.bouncycastle.jce.provider.BouncyCastleProvider");
            Provider provider = (Provider) bcProvider.getDeclaredConstructor().newInstance();
            Security.addProvider(provider);
        } catch (Exception e) {
            System.out.println("Note: BouncyCastle provider not available - signature verification will use standard Java crypto");
        }
    }
    

    public SofizPayStellarSDK(boolean useTestnet) {
        this.isTestnet = useTestnet;
        this.httpClient = new OkHttpClient();
        this.gson = new Gson();
        this.streamExecutor = Executors.newScheduledThreadPool(2);
        
        if (useTestnet) {
            server = new Server("https://horizon-testnet.stellar.org");
            network = Network.TESTNET;
        } else {
            server = new Server("https://horizon.stellar.org");
            network = Network.PUBLIC;
        }
    }
    

    public String getVersion() {
        return VERSION;
    }
    
 
    public PublicKeyResult getPublicKey(String secretKey) {
        try {
            org.stellar.sdk.KeyPair keyPair = org.stellar.sdk.KeyPair.fromSecretSeed(secretKey);
            String publicKey = keyPair.getAccountId();
            
            return new PublicKeyResult(true, "Public key extracted successfully", publicKey);
        } catch (Exception e) {
            return new PublicKeyResult(false, "Error extracting public key: " + e.getMessage(), null);
        }
    }
    

    public BalanceResult getDZTBalance(String accountId) {
        try {
            AccountResponse account = server.accounts().account(accountId);
            
            for (AccountResponse.Balance balance : account.getBalances()) {
                if (balance.getAssetType().equals("native")) {
                    return new BalanceResult(true, "Balance retrieved successfully", balance.getBalance() + " DZT");
                } else if (balance.getAssetCode() != null && balance.getAssetCode().equals(DZT_ASSET_CODE)) {
                    return new BalanceResult(true, "Balance retrieved successfully", balance.getBalance() + " DZT");
                }
            }
            
            List<AccountResponse.Balance> balances = account.getBalances();
            if (!balances.isEmpty()) {
                AccountResponse.Balance nativeBalance = balances.get(0);
                return new BalanceResult(true, "DZT not found, showing native balance", nativeBalance.getBalance() + " DZT");
            }
            
            return new BalanceResult(true, "No balances found", "0");
            
        } catch (Exception e) {
            return new BalanceResult(false, "Error fetching balance: " + e.getMessage(), "0");
        }
    }

    public PaymentResult submit(PaymentData paymentData) {
        if (paymentData.secret == null || paymentData.secret.isEmpty()) {
            return new PaymentResult(false, "Secret key is required.", null);
        }
        
        if (paymentData.destination == null || paymentData.destination.isEmpty()) {
            return new PaymentResult(false, "Destination address is required.", null);
        }
        
        if (paymentData.amount == null || paymentData.amount.isEmpty()) {
            return new PaymentResult(false, "Amount is required.", null);
        }
        
        try {
            org.stellar.sdk.KeyPair sourceKeyPair = org.stellar.sdk.KeyPair.fromSecretSeed(paymentData.secret);
            
            TransactionBuilderAccount sourceAccount = server.loadAccount(sourceKeyPair.getAccountId());
            
            BigDecimal amount = new BigDecimal(paymentData.amount);
            
            Asset asset;
            if (DZT_ASSET_CODE.equals(paymentData.assetCode)) {
                asset = Asset.createNonNativeAsset(DZT_ASSET_CODE, DZT_ISSUER);
            } else {
                asset = Asset.createNativeAsset();
            }
            
            PaymentOperation paymentOperation = PaymentOperation.builder()
                    .destination(paymentData.destination)
                    .asset(asset)
                    .amount(amount)
                    .build();
            
            TransactionBuilder transactionBuilder = new TransactionBuilder(sourceAccount, network)
                    .setBaseFee(Transaction.MIN_BASE_FEE)
                    .addOperation(paymentOperation)
                    .setTimeout(30);
            
            if (paymentData.memo != null && !paymentData.memo.isEmpty()) {
                transactionBuilder.addMemo(Memo.text(paymentData.memo));
            }
            
            Transaction transaction = transactionBuilder.build();
            
            transaction.sign(sourceKeyPair);
            
            TransactionResponse response = server.submitTransaction(transaction);
            
            String transactionId = response.getHash();
            System.out.println("✅ Payment sent successfully! Transaction: " + transactionId);
            
            return new PaymentResult(true, "Payment submitted successfully", transactionId);
            
        } catch (BadRequestException e) {
            return new PaymentResult(false, "Bad request: " + e.getMessage(), null);
        } catch (BadResponseException e) {
            return new PaymentResult(false, "Bad response: " + e.getMessage(), null);
        } catch (NetworkException e) {
            return new PaymentResult(false, "Network error: " + e.getMessage(), null);
        } catch (Exception e) {
            return new PaymentResult(false, "Error submitting payment: " + e.getMessage(), null);
        }
    }
  
    public TransactionHistoryResult getTransactions(String accountId, int limit) {
        try {
            Page<TransactionResponse> transactionsPage = server.transactions()
                    .forAccount(accountId)
                    .limit(limit)
                    .order(RequestBuilder.Order.DESC)
                    .execute();
            
            List<TransactionInfo> transactions = new ArrayList<>();
            
            for (TransactionResponse tx : transactionsPage.getRecords()) {
                TransactionInfo txInfo = new TransactionInfo();
                txInfo.id = tx.getHash();
                txInfo.hash = tx.getHash();
                txInfo.created_at = tx.getCreatedAt();
                txInfo.source_account = tx.getSourceAccount();
                txInfo.fee_charged = String.valueOf(tx.getFeeCharged());
                txInfo.successful = tx.getSuccessful();
                txInfo.operation_count = tx.getOperationCount();
                
                if (tx.getMemo() != null) {
                    txInfo.memo = tx.getMemo().toString();
                }
                
                try {
                    Page<org.stellar.sdk.responses.operations.OperationResponse> operations = server.operations().forTransaction(tx.getHash()).execute();
                    List<org.stellar.sdk.responses.operations.OperationResponse> operationRecords = operations.getRecords();
                    
                    for (org.stellar.sdk.responses.operations.OperationResponse operation : operationRecords) {
                        if (operation.getType().equals("payment")) {
                            PaymentOperationResponse paymentOp = (PaymentOperationResponse) operation;
                            txInfo.amount = paymentOp.getAmount();
                            txInfo.from = paymentOp.getFrom();
                            txInfo.to = paymentOp.getTo();
                            
                            if (paymentOp.getFrom().equals(accountId)) {
                                txInfo.type = "sent";
                            } else if (paymentOp.getTo().equals(accountId)) {
                                txInfo.type = "received";
                            }
                            
                            if (paymentOp.getAsset().getType().equals("native")) {
                                txInfo.asset_type = "native";
                                txInfo.asset_code = "DZT";
                            } else {
                                txInfo.asset_type = "credit_alphanum4";
                                txInfo.asset_code = paymentOp.getAsset().toString();
                            }
                            break;
                        }
                    }
                } catch (Exception e) {
                    System.err.println("Cannot fetch operation details for transaction: " + tx.getHash());
                }
                
                transactions.add(txInfo);
            }
            
            return new TransactionHistoryResult(
                true, 
                "Fetched all transactions (" + transactions.size() + " transactions)", 
                transactions
            );
            
        } catch (Exception e) {
            return new TransactionHistoryResult(false, "Error fetching transactions: " + e.getMessage(), new ArrayList<>());
        }
    }

    public SearchTransactionsResult searchTransactionsByMemo(String accountId, String memo, int limit) {
        try {
            Page<TransactionResponse> transactionsPage = server.transactions()
                    .forAccount(accountId)
                    .limit(Math.max(limit * 5, 100))
                    .order(RequestBuilder.Order.DESC)
                    .execute();
            
            List<TransactionInfo> matchingTransactions = new ArrayList<>();
            
            for (TransactionResponse tx : transactionsPage.getRecords()) {
                if (tx.getMemo() != null && tx.getMemo().toString().contains(memo)) {
                    TransactionInfo txInfo = new TransactionInfo();
                    txInfo.id = tx.getHash();
                    txInfo.hash = tx.getHash();
                    txInfo.created_at = tx.getCreatedAt();
                    txInfo.source_account = tx.getSourceAccount();
                    txInfo.memo = tx.getMemo().toString();
                    txInfo.successful = tx.getSuccessful();
                    
                    try {
                        Page<org.stellar.sdk.responses.operations.OperationResponse> operations = server.operations().forTransaction(tx.getHash()).execute();
                        for (org.stellar.sdk.responses.operations.OperationResponse operation : operations.getRecords()) {
                            if (operation.getType().equals("payment")) {
                                PaymentOperationResponse paymentOp = (PaymentOperationResponse) operation;
                                txInfo.amount = paymentOp.getAmount();
                                txInfo.from = paymentOp.getFrom();
                                txInfo.to = paymentOp.getTo();
                                break;
                            }
                        }
                    } catch (Exception e) {
                    }
                    
                    matchingTransactions.add(txInfo);
                    
                    if (matchingTransactions.size() >= limit) {
                        break;
                    }
                }
            }
            
            return new SearchTransactionsResult(
                true,
                "Found " + matchingTransactions.size() + " transactions containing \"" + memo + "\"",
                matchingTransactions
            );
            
        } catch (Exception e) {
            return new SearchTransactionsResult(false, "Error searching transactions: " + e.getMessage(), new ArrayList<>());
        }
    }
    

    public TransactionByHashResult getTransactionByHash(String transactionHash) {
        try {
            TransactionResponse transaction = server.transactions().transaction(transactionHash);
            
            TransactionInfo txInfo = new TransactionInfo();
            txInfo.id = transaction.getHash();
            txInfo.hash = transaction.getHash();
            txInfo.created_at = transaction.getCreatedAt();
            txInfo.source_account = transaction.getSourceAccount();
            txInfo.successful = transaction.getSuccessful();
            txInfo.fee_charged = String.valueOf(transaction.getFeeCharged());
            txInfo.operation_count = transaction.getOperationCount();
            
            if (transaction.getMemo() != null) {
                txInfo.memo = transaction.getMemo().toString();
            }
            
            try {
                Page<org.stellar.sdk.responses.operations.OperationResponse> operations = server.operations().forTransaction(transactionHash).execute();
                for (org.stellar.sdk.responses.operations.OperationResponse operation : operations.getRecords()) {
                    if (operation.getType().equals("payment")) {
                        PaymentOperationResponse paymentOp = (PaymentOperationResponse) operation;
                        txInfo.amount = paymentOp.getAmount();
                        txInfo.from = paymentOp.getFrom();
                        txInfo.to = paymentOp.getTo();
                        break;
                    }
                }
            } catch (Exception e) {
            }
            
            return new TransactionByHashResult(true, "Transaction found", true, txInfo, transactionHash);
            
        } catch (Exception e) {
            return new TransactionByHashResult(true, "Transaction not found", false, null, transactionHash);
        }
    }
    
 
    public StreamResult startTransactionStream(String accountId, TransactionCallback callback) {
        try {
            if (activeStreams.containsKey(accountId)) {
                return new StreamResult(false, "Stream already active for this account");
            }
            
            org.stellar.sdk.KeyPair.fromAccountId(accountId);
            
            StreamInfo streamInfo = new StreamInfo();
            streamInfo.accountId = accountId;
            streamInfo.isActive = true;
            streamInfo.startTime = Instant.now();
            
            activeStreams.put(accountId, streamInfo);
            transactionCallbacks.put(accountId, callback);
            
            streamExecutor.scheduleAtFixedRate(() -> {
                monitorTransactions(accountId);
            }, 0, 5, TimeUnit.SECONDS);
        
            System.out.println("🚀 Started transaction stream for: " + accountId);
            return new StreamResult(true, "Transaction stream started successfully");
            
        } catch (Exception e) {
            return new StreamResult(false, "Error starting stream: " + e.getMessage());
        }
    }
    
    public StreamResult stopTransactionStream(String accountId) {
        StreamInfo streamInfo = activeStreams.remove(accountId);
        transactionCallbacks.remove(accountId);
        
        if (streamInfo != null) {
            streamInfo.isActive = false;
            System.out.println("⏹️ Stopped transaction stream for: " + accountId);
            return new StreamResult(true, "Transaction stream stopped successfully");
        } else {
            return new StreamResult(false, "No active stream found for this account");
        }
    }
    

    public StreamStatusResult getStreamStatus(String accountId) {
        StreamInfo streamInfo = activeStreams.get(accountId);
        
        if (streamInfo != null) {
            return new StreamStatusResult(true, "Stream status retrieved", true, streamInfo.startTime);
        } else {
            return new StreamStatusResult(true, "No active stream found", false, null);
        }
    }
    
 
    private void monitorTransactions(String accountId) {
        try {
            StreamInfo streamInfo = activeStreams.get(accountId);
            if (streamInfo == null || !streamInfo.isActive) {
                return;
            }
            
            Page<TransactionResponse> transactionsPage = server.transactions()
                    .forAccount(accountId)
                    .order(RequestBuilder.Order.DESC)
                    .limit(10)
                    .execute();
            
            List<TransactionResponse> transactions = transactionsPage.getRecords();
            
            if (streamInfo.lastProcessedHash == null && !transactions.isEmpty()) {
                streamInfo.lastProcessedHash = transactions.get(0).getHash();
                return;
            }
            
            for (TransactionResponse transaction : transactions) {
                if (transaction.getHash().equals(streamInfo.lastProcessedHash)) {
                    break; 
                }
                
                TransactionCallback callback = transactionCallbacks.get(accountId);
                if (callback != null) {
                    TransactionInfo txInfo = new TransactionInfo();
                    txInfo.id = transaction.getHash();
                    txInfo.hash = transaction.getHash();
                    txInfo.created_at = transaction.getCreatedAt();
                    txInfo.source_account = transaction.getSourceAccount();
                    txInfo.successful = transaction.getSuccessful();
                    
                    callback.onNewTransaction(txInfo);
                }
            }
            
            // تحديث آخر هاش معالج / Update last processed hash
            if (!transactions.isEmpty()) {
                streamInfo.lastProcessedHash = transactions.get(0).getHash();
            }
            
        } catch (Exception e) {
            System.err.println("Error monitoring transactions: " + e.getMessage());
        }
    }
    

    public CIBTransactionResult makeCIBTransaction(CIBTransactionData transactionData) {
        if (transactionData.account == null || transactionData.account.isEmpty()) {
            return new CIBTransactionResult(false, "Account is required.", null);
        }
        
        if (transactionData.amount == null || transactionData.amount.isEmpty()) {
            return new CIBTransactionResult(false, "Amount is required.", null);
        }
        
        try {
            Map<String, Object> requestData = new HashMap<>();
            requestData.put("account", transactionData.account);
            requestData.put("amount", transactionData.amount);
            requestData.put("reference", transactionData.reference != null ? transactionData.reference : "SOFIZPAY-" + System.currentTimeMillis());
            requestData.put("description", transactionData.description != null ? transactionData.description : "SofizPay transaction");
            
            String jsonBody = gson.toJson(requestData);
            
            RequestBody body = RequestBody.create(jsonBody, MediaType.get("application/json; charset=utf-8"));
            
            Request request = new Request.Builder()
                    .url("https://www.sofizpay.com/make-cib-transaction/") 
                    .post(body)
                    .addHeader("Content-Type", "application/json")
                    .addHeader("Authorization", "Bearer " + (transactionData.apiKey != null ? transactionData.apiKey : "test-key"))
                    .build();
            
            try (Response response = httpClient.newCall(request).execute()) {
                String responseBody = response.body() != null ? response.body().string() : "";
                
                if (response.isSuccessful()) {
                    Map<String, Object> responseData = gson.fromJson(responseBody, Map.class);
                    return new CIBTransactionResult(true, "CIB transaction completed successfully", responseData);
                } else {
                    return new CIBTransactionResult(false, "CIB transaction failed: " + response.code() + " - " + responseBody, null);
                }
            }
            
        } catch (Exception e) {
            return new CIBTransactionResult(false, "Error making CIB transaction: " + e.getMessage(), null);
        }
    }
    

    public boolean verifySignature(String message, String signatureUrlSafe) {
        if (message == null || message.isEmpty()) {
            return false;
        }
        if (signatureUrlSafe == null || signatureUrlSafe.isEmpty()) {
            return false;
        }
        
        String publicKeyPem = "-----BEGIN PUBLIC KEY-----\n" +
            "MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEA1N+bDPxpqeB9QB0affr/\n" +
            "02aeRXAAnqHuLrgiUlVNdXtF7t+2w8pnEg+m9RRlc+4YEY6UyKTUjVe6k7v2p8Jj\n" +
            "UItk/fMNOEg/zY222EbqsKZ2mF4hzqgyJ3QHPXjZEEqABkbcYVv4ZyV2Wq0x0ykI\n" +
            "+Hy/5YWKeah4RP2uEML1FlXGpuacnMXpW6n36dne3fUN+OzILGefeRpmpnSGO5+i\n" +
            "JmpF2mRdKL3hs9WgaLSg6uQyrQuJA9xqcCpUmpNbIGYXN9QZxjdyRGnxivTE8awx\n" +
            "THV3WRcKrP2krz3ruRGF6yP6PVHEuPc0YDLsYjV5uhfs7JtIksNKhRRAQ16bAsj/\n" +
            "9wIDAQAB\n" +
            "-----END PUBLIC KEY-----";
        
        try {
            String base64 = signatureUrlSafe
                .replace('-', '+')
                .replace('_', '/');
            
            while (base64.length() % 4 != 0) {
                base64 += '=';
            }
            
            byte[] signatureBytes = java.util.Base64.getDecoder().decode(base64);
            
            String publicKeyContent = publicKeyPem
                .replace("-----BEGIN PUBLIC KEY-----", "")
                .replace("-----END PUBLIC KEY-----", "")
                .replaceAll("\\s", "");
            
            byte[] publicKeyBytes = java.util.Base64.getDecoder().decode(publicKeyContent);
            X509EncodedKeySpec keySpec = new X509EncodedKeySpec(publicKeyBytes);
            KeyFactory keyFactory = KeyFactory.getInstance("RSA");
            PublicKey publicKey = keyFactory.generatePublic(keySpec);
            
            Signature signature = Signature.getInstance("SHA256withRSA");
            signature.initVerify(publicKey);
            signature.update(message.getBytes("UTF-8"));
            
            return signature.verify(signatureBytes);
            
        } catch (Exception e) {
            System.err.println("Signature verification error: " + e.getMessage());
            return false;
        }
    }
    
    
 
    public FundResult fundAccountFromFaucet(String accountId) {
        if (!isTestnet) {
            return new FundResult(false, "Faucet funding only available on testnet");
        }
        
        try {
            HttpClient client = HttpClient.newHttpClient();
            String faucetUrl = "https://friendbot.stellar.org?addr=" + accountId;
            
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(faucetUrl))
                    .GET()
                    .build();
            
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            
            if (response.statusCode() == 200) {
                return new FundResult(true, "Account funded successfully! Added 10,000 DZT to your account");
            } else {
                return new FundResult(false, "Failed to fund account: " + response.statusCode());
            }
        } catch (IOException | InterruptedException e) {
            return new FundResult(false, "Error funding account: " + e.getMessage());
        }
    }

    public AccountCreationResult createAccount() {
        try {
            org.stellar.sdk.KeyPair account = org.stellar.sdk.KeyPair.random();
            
            return new AccountCreationResult(
                true,
                "New account created successfully",
                account.getAccountId(),
                String.valueOf(account.getSecretSeed())
            );
        } catch (Exception e) {
            return new AccountCreationResult(false, "Error creating account: " + e.getMessage(), null, null);
        }
    }
    

    public void close() {
        for (String accountId : new ArrayList<>(activeStreams.keySet())) {
            stopTransactionStream(accountId);
        }
        
        if (streamExecutor != null) {
            streamExecutor.shutdown();
            try {
                if (!streamExecutor.awaitTermination(5, TimeUnit.SECONDS)) {
                    streamExecutor.shutdownNow();
                }
            } catch (InterruptedException e) {
                streamExecutor.shutdownNow();
            }
        }
        
        if (server != null) {
            try {
                server.close();
                System.out.println("🔌 SofizPay SDK connection closed");
            } catch (Exception e) {
                System.err.println("Error closing SDK: " + e.getMessage());
            }
        }
    }
    
    public static class PaymentData {
        public String secret;
        public String destination;
        public String amount;
        public String memo;
        public String assetCode = "DZT"; 
    }
    
    public static class PaymentResult {
        public boolean success;
        public String message;
        public String transactionId;
        
        public PaymentResult(boolean success, String message, String transactionId) {
            this.success = success;
            this.message = message;
            this.transactionId = transactionId;
        }
    }
    
    public static class PublicKeyResult {
        public boolean success;
        public String message;
        public String publicKey;
        
        public PublicKeyResult(boolean success, String message, String publicKey) {
            this.success = success;
            this.message = message;
            this.publicKey = publicKey;
        }
    }
    
    public static class BalanceResult {
        public boolean success;
        public String message;
        public String balance;
        
        public BalanceResult(boolean success, String message, String balance) {
            this.success = success;
            this.message = message;
            this.balance = balance;
        }
    }
    
    public static class TransactionInfo {
        public String id;
        public String hash;
        public String created_at;
        public String source_account;
        public String memo;
        public String amount;
        public String from;
        public String to;
        public String type;
        public String asset_type;
        public String asset_code;
        public String fee_charged;
        public boolean successful;
        public int operation_count;
    }
    
    public static class TransactionHistoryResult {
        public boolean success;
        public String message;
        public List<TransactionInfo> transactions;
        
        public TransactionHistoryResult(boolean success, String message, List<TransactionInfo> transactions) {
            this.success = success;
            this.message = message;
            this.transactions = transactions;
        }
    }
    
    public static class SearchTransactionsResult {
        public boolean success;
        public String message;
        public List<TransactionInfo> transactions;
        
        public SearchTransactionsResult(boolean success, String message, List<TransactionInfo> transactions) {
            this.success = success;
            this.message = message;
            this.transactions = transactions;
        }
    }
    
    public static class TransactionByHashResult {
        public boolean success;
        public String message;
        public boolean found;
        public TransactionInfo transaction;
        public String hash;
        
        public TransactionByHashResult(boolean success, String message, boolean found, TransactionInfo transaction, String hash) {
            this.success = success;
            this.message = message;
            this.found = found;
            this.transaction = transaction;
            this.hash = hash;
        }
    }
    
    public static class StreamResult {
        public boolean success;
        public String message;
        
        public StreamResult(boolean success, String message) {
            this.success = success;
            this.message = message;
        }
    }
    
    public static class StreamStatusResult {
        public boolean success;
        public String message;
        public boolean active;
        public Instant startTime;
        
        public StreamStatusResult(boolean success, String message, boolean active, Instant startTime) {
            this.success = success;
            this.message = message;
            this.active = active;
            this.startTime = startTime;
        }
    }
    
    public static class CIBTransactionData {
        public String account;
        public String amount;
        public String reference;
        public String description;
        public String apiKey;
    }
    
    public static class CIBTransactionResult {
        public boolean success;
        public String message;
        public Map<String, Object> data;
        
        public CIBTransactionResult(boolean success, String message, Map<String, Object> data) {
            this.success = success;
            this.message = message;
            this.data = data;
        }
    }
    
    public static class SignatureVerificationResult {
        public boolean valid;
        public String message;
        public String data;
        
        public SignatureVerificationResult(boolean valid, String message, String data) {
            this.valid = valid;
            this.message = message;
            this.data = data;
        }
    }
    
    public static class FundResult {
        public boolean success;
        public String message;
        
        public FundResult(boolean success, String message) {
            this.success = success;
            this.message = message;
        }
    }
    
    public static class AccountCreationResult {
        public boolean success;
        public String message;
        public String accountId;
        public String secretKey;
        
        public AccountCreationResult(boolean success, String message, String accountId, String secretKey) {
            this.success = success;
            this.message = message;
            this.accountId = accountId;
            this.secretKey = secretKey;
        }
    }
    
    public interface TransactionCallback {
        void onNewTransaction(TransactionInfo transaction);
    }
    
    private static class StreamInfo {
        String accountId;
        boolean isActive;
        Instant startTime;
        String lastProcessedHash;
    }
}
