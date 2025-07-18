package com.sofizpay.sdk;

import com.sofizpay.sdk.SofizPayStellarSDK;

public class CompleteSofizPayExample {

    public static void main(String[] args) {
        System.out.println("=== SofizPay Stellar SDK Example ===");

        boolean useMainnet = false;
        SofizPayStellarSDK sdk = new SofizPayStellarSDK(useMainnet);
        System.out.println("SDK Version: " + sdk.getVersion());

        String mySecretKey = "YOUR_SECRET_KEY";
        String myPublicKey = "YOUR_PUBLIC_KEY"; 
        String destination = "DESTINATION_PUBLIC_KEY"; 

        System.out.println("\n🔑 Get Public Key from Secret:");
        SofizPayStellarSDK.PublicKeyResult publicKeyResult = sdk.getPublicKey(mySecretKey);
        System.out.println("Result: " + publicKeyResult.publicKey);
        System.out.println("Message: " + publicKeyResult.message);

        System.out.println("\n💰 Get DZT Balance:");
        SofizPayStellarSDK.BalanceResult balanceResult = sdk.getDZTBalance(myPublicKey);
        System.out.println("Balance: " + balanceResult.balance);
        System.out.println("Message: " + balanceResult.message);

        System.out.println("\n💸 Send Payment:");
        SofizPayStellarSDK.PaymentData payment = new SofizPayStellarSDK.PaymentData();
        payment.secret = mySecretKey;
        payment.destination = destination;
        payment.amount = "1";
        payment.memo = "WC_ORDER";
        payment.assetCode = "DZT"; 
        
        SofizPayStellarSDK.PaymentResult paymentResult = sdk.submit(payment);
        System.out.println("Transaction ID: " + paymentResult.transactionId);
        System.out.println("Message: " + paymentResult.message);

        System.out.println("\n📜 Transaction History:");
        SofizPayStellarSDK.TransactionHistoryResult history = sdk.getTransactions(myPublicKey, 5);
        for (SofizPayStellarSDK.TransactionInfo tx : history.transactions) {
            System.out.println("- TX ID: " + tx.id + " | Amount: " + tx.amount + " DZT");
        }

        System.out.println("\n🔍 Search Transactions by Memo:");
        SofizPayStellarSDK.SearchTransactionsResult search = sdk.searchTransactionsByMemo(myPublicKey, "WC_ORDER", 20);
        System.out.println("Search Result: " + (search.success ? "✅ Success" : "❌ Failed"));
        System.out.println("Message: " + search.message);
        for (SofizPayStellarSDK.TransactionInfo tx : search.transactions) {
            System.out.println("- Found: " + tx.memo + " | Amount: " + tx.amount);
        }
        
        System.out.println("\n📡 Start Transaction Stream:");
        SofizPayStellarSDK.StreamResult streamResult = sdk.startTransactionStream(myPublicKey, new SofizPayStellarSDK.TransactionCallback() {
            @Override
            public void onNewTransaction(SofizPayStellarSDK.TransactionInfo transaction) {
                System.out.println("🚨 New Transaction Detected!");
                System.out.println("   - Transaction ID: " + transaction.id);
                System.out.println("   - Source: " + transaction.source_account.substring(0, 8) + "...");
                System.out.println("   - Success: " + (transaction.successful ? "✅ Yes" : "❌ No"));
                System.out.println("   - Time: " + transaction.created_at);
            }
        });
        System.out.println("Stream Status: " + (streamResult.success ? "✅ Active" : "❌ Failed"));
        System.out.println("Message: " + streamResult.message);
        
        System.out.println("\n📊 Check Stream Status:");
        SofizPayStellarSDK.StreamStatusResult statusResult = sdk.getStreamStatus(myPublicKey);
        System.out.println("Status: " + (statusResult.success ? "✅ Success" : "❌ Failed"));
        System.out.println("Active: " + (statusResult.active ? "✅ Yes" : "❌ No"));
        if (statusResult.startTime != null) {
            System.out.println("Start Time: " + statusResult.startTime);
        }
        
        System.out.println("\n⏰ Waiting 10 seconds for stream demonstration...");
        try {
            Thread.sleep(10000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        System.out.println("\n🛑 Stop Transaction Stream:");
        SofizPayStellarSDK.StreamResult stopResult = sdk.stopTransactionStream(myPublicKey);
        System.out.println("Stop Result: " + (stopResult.success ? "✅ Success" : "❌ Failed"));
        System.out.println("Message: " + stopResult.message);

        System.out.println("\n🔐 Verify Signature:");
        String testMessage = "wc_order_LI3SLQ8xA7IY9cib84907success23400";
        String testSignature = "jHrONYl2NuBhjAYTgRq3xwRuW2ZYZIQlx1VWgiObu5FrSnY78pQ-CV0pAjRKWAje-DDZHhvMvzIFSBE9rj87xsWymjWYVlyZmuVr-sDPSa-zWZRsyWJhdj0XPZir4skkDFWlhaWpwtLql0D7N5yw_zu67plVuhaPk4d_jOhn0O0qN3scROa1H1pIAPhIreQHu72-Bx4v2g-NGceFVpiAMyf2j2rvVthkg4o6adxY_E0-y_AJfnJdL1HhmWOBpFEUk6ziV1aFzSJIo-XpueJSFWpL7wrAsQ6shcLE3zQSZXJoXhdR7nr92-Y7SXgEE_a9kP_Q4uExJCWcaOcPkQ5Bgg==";
        
        System.out.println("Test Message: \"" + testMessage + "\"");
        Boolean sigResult = sdk.verifySignature(testMessage, testSignature);
        System.out.println("Signature Valid? " + (sigResult ? "✅ Yes" : "❌ No"));

        System.out.println("\n✅ All Done!");
        System.out.println("🎉 SofizPay SDK Test Complete!");

        sdk.close();
        System.out.println("🧹 Resources cleaned up");
    }
}
