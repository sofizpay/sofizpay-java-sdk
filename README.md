# SofizPay SDK for Java/Kotlin

<div align="center">
  <img src="https://github.com/kenandarabeh/sofizpay-sdk-java/blob/main/assets/sofizpay-logo.png?raw=true" alt="SofizPay Logo" width="200" height="200">
  
  <h3>🚀 A powerful Java/Kotlin SDK for Stellar blockchain DZT token payments</h3>
  
  [![Maven Central](https://img.shields.io/maven-central/v/com.sofizpay/sofizpay-sdk-java.svg)](https://search.maven.org/artifact/com.sofizpay/sofizpay-sdk-java)
  [![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](https://opensource.org/licenses/MIT)
  [![GitHub Stars](https://img.shields.io/github/stars/kenandarabeh/sofizpay-sdk-java.svg)](https://github.com/kenandarabeh/sofizpay-sdk-java/stargazers)
  [![Issues](https://img.shields.io/github/issues/kenandarabeh/sofizpay-sdk-java.svg)](https://github.com/kenandarabeh/sofizpay-sdk-java/issues)
  [![Java 8+](https://img.shields.io/badge/Java-8%2B-orange.svg)](https://www.oracle.com/java/)
  [![Kotlin](https://img.shields.io/badge/Kotlin-1.9.0-blue.svg)](https://kotlinlang.org/)
</div>

---

## 📋 Table of Contents

- [Overview](#overview)
- [Features](#features)
- [Installation](#installation)
- [Quick Start](#quick-start)
- [API Reference](#api-reference)
- [Usage Examples](#usage-examples)
- [Real-time Transaction Monitoring](#real-time-transaction-monitoring)
- [CIB Integration](#cib-integration)
- [Error Handling](#error-handling)
- [Best Practices](#best-practices)
- [Java vs Kotlin](#java-vs-kotlin)
- [Testing](#testing)
- [Contributing](#contributing)
- [Support](#support)
- [License](#license)

---

## 🌟 Overview

SofizPay SDK is a comprehensive Java/Kotlin library for Stellar blockchain DZT token payments with real-time transaction monitoring, CIB bank integration, and advanced payment management capabilities.

**Key Benefits:**
- 🔐 Secure Stellar blockchain integration
- ⚡ Real-time transaction monitoring with callbacks
- 🎯 Simple, intuitive API for both Java and Kotlin
- 🏦 CIB bank transaction support
- 📊 Comprehensive transaction history and search
- 🔍 Advanced signature verification
- 🌐 Testnet and Mainnet support

---

## ✨ Features

- ✅ **Send DZT/XLM Payments**: Secure token transfers with memo support
- ✅ **Transaction History**: Retrieve and filter transaction records with pagination
- ✅ **Balance Checking**: Real-time DZT and XLM balance queries
- ✅ **Transaction Search**: Find transactions by memo, hash, or account
- ✅ **Real-time Streams**: Live transaction monitoring with customizable callbacks
- ✅ **CIB Transactions**: Create CIB bank transactions for fiat deposits
- ✅ **Account Management**: Create new accounts and fund from testnet faucet
- ✅ **Signature Verification**: RSA signature validation for secure communications
- ✅ **Error Handling**: Robust error management and detailed reporting
- ✅ **Kotlin Support**: Modern Kotlin implementation with data classes and coroutines-ready

---

## 📦 Installation

### Gradle (Groovy)
```gradle
dependencies {
    implementation 'com.sofizpay:sofizpay-sdk-java:1.0.0'
}
```

### Gradle (Kotlin DSL)
```kotlin
dependencies {
    implementation("com.sofizpay:sofizpay-sdk-java:1.0.0")
}
```

### Maven
```xml
<dependency>
    <groupId>com.sofizpay</groupId>
    <artifactId>sofizpay-sdk-java</artifactId>
    <version>1.0.0</version>
</dependency>
```

### Build from Source
```bash
git clone https://github.com/kenandarabeh/sofizpay-sdk-java.git
cd sofizpay-sdk-java
./gradlew build
```

---

## 🚀 Quick Start

### Java Example

```java
import com.sofizpay.sdk.SofizPayStellarSDK;

public class QuickStart {
    public static void main(String[] args) {
        // Initialize SDK (testnet by default)
        try (SofizPayStellarSDK sdk = new SofizPayStellarSDK(true)) {
            
            // Create payment data
            SofizPayStellarSDK.PaymentData paymentData = new SofizPayStellarSDK.PaymentData();
            paymentData.secret = "YOUR_SECRET_KEY";
            paymentData.destination = "DESTINATION_PUBLIC_KEY";
            paymentData.amount = "10.0";
            paymentData.memo = "Payment for services";
            paymentData.assetCode = "DZT"; // or "XLM"
            
            // Send payment
            SofizPayStellarSDK.PaymentResult result = sdk.submit(paymentData);
            
            if (result.success) {
                System.out.println("✅ Payment successful! TX: " + result.transactionId);
            } else {
                System.out.println("❌ Payment failed: " + result.message);
            }
        }
    }
}
```

### Kotlin Example

```kotlin
import com.sofizpay.sdk.SofizPayStellarSDK

fun main() {
    // Initialize SDK (testnet by default)
    SofizPayStellarSDK(isTestnet = true).use { sdk ->
        
        // Create payment data
        val paymentData = SofizPayStellarSDK.PaymentData(
            secret = "YOUR_SECRET_KEY",
            destination = "DESTINATION_PUBLIC_KEY",
            amount = "10.0",
            memo = "Payment for services",
            assetCode = "DZT" // or "XLM"
        )
        
        // Send payment
        val result = sdk.submit(paymentData)
        
        if (result.success) {
            println("✅ Payment successful! TX: ${result.transactionId}")
        } else {
            println("❌ Payment failed: ${result.message}")
        }
    }
}
```

---

## 📚 API Reference

### Core Payment Methods

#### `submit()` - Send Payment
Send DZT or XLM payments with memo support.

**Java:**
```java
PaymentData paymentData = new PaymentData();
paymentData.secret = "SECRET_KEY";
paymentData.destination = "DESTINATION_PUBLIC_KEY";
paymentData.amount = "10.0";
paymentData.memo = "Payment memo";
paymentData.assetCode = "DZT"; // "DZT" or "XLM"

PaymentResult result = sdk.submit(paymentData);
```

**Kotlin:**
```kotlin
val paymentData = PaymentData(
    secret = "SECRET_KEY",
    destination = "DESTINATION_PUBLIC_KEY",
    amount = "10.0",
    memo = "Payment memo",
    assetCode = "DZT" // "DZT" or "XLM"
)

val result = sdk.submit(paymentData)
```

#### `getDZTBalance()` - Check Balance
Get account balance for DZT and XLM.

```java
BalanceResult balance = sdk.getDZTBalance("ACCOUNT_PUBLIC_KEY");
System.out.println("Balance: " + balance.balance);
```

#### `getPublicKey()` - Extract Public Key
Extract public key from secret key.

```java
PublicKeyResult result = sdk.getPublicKey("SECRET_KEY");
String publicKey = result.publicKey;
```

### Transaction Management

#### `getTransactions()` - Transaction History
Get paginated transaction history for an account.

```java
TransactionHistoryResult result = sdk.getTransactions("ACCOUNT_ID", 50);
for (TransactionInfo tx : result.transactions) {
    System.out.println("TX: " + tx.hash + " Amount: " + tx.amount);
}
```

#### `searchTransactionsByMemo()` - Search by Memo
Find transactions containing specific memo text.

```java
SearchTransactionsResult result = sdk.searchTransactionsByMemo("ACCOUNT_ID", "invoice-123", 10);
```

#### `getTransactionByHash()` - Get by Hash
Retrieve specific transaction by hash.

```java
TransactionByHashResult result = sdk.getTransactionByHash("TRANSACTION_HASH");
if (result.found) {
    TransactionInfo tx = result.transaction;
    System.out.println("Found transaction: " + tx.amount);
}
```

### Account Management

#### `createAccount()` - Create New Account
Generate a new Stellar account keypair.

```java
AccountCreationResult result = sdk.createAccount();
System.out.println("Account ID: " + result.accountId);
System.out.println("Secret Key: " + result.secretKey);
```

#### `fundAccountFromFaucet()` - Fund from Testnet Faucet
Fund account with testnet XLM (testnet only).

```java
FundResult result = sdk.fundAccountFromFaucet("ACCOUNT_ID");
if (result.success) {
    System.out.println("Account funded with 10,000 XLM");
}
```

### CIB Integration

#### `makeCIBTransaction()` - CIB Bank Transaction
Create CIB bank transactions for fiat deposits.

```java
CIBTransactionData cibData = new CIBTransactionData();
cibData.account = "BANK_ACCOUNT_NUMBER";
cibData.amount = "100.0";
cibData.reference = "PAYMENT-REF-123";
cibData.description = "DZT token purchase";
cibData.apiKey = "YOUR_CIB_API_KEY";

CIBTransactionResult result = sdk.makeCIBTransaction(cibData);
```

### Security

#### `verifySignature()` - RSA Signature Verification
Verify RSA signatures for secure communication.

```java
boolean isValid = sdk.verifySignature("message", "base64_signature");
if (isValid) {
    System.out.println("Signature is valid");
}
```

---

## 🔄 Real-time Transaction Monitoring

Monitor transactions in real-time with customizable callbacks.

### Java Implementation

```java
// Define callback
TransactionCallback callback = new TransactionCallback() {
    @Override
    public void onNewTransaction(TransactionInfo transaction) {
        System.out.println("New transaction: " + transaction.type + 
                          " Amount: " + transaction.amount + " DZT");
        
        if ("received".equals(transaction.type)) {
            handleIncomingPayment(transaction);
        }
    }
};

// Start monitoring
StreamResult streamResult = sdk.startTransactionStream("ACCOUNT_ID", callback);

// Check status
StreamStatusResult status = sdk.getStreamStatus("ACCOUNT_ID");
System.out.println("Stream active: " + status.active);

// Stop monitoring
StreamResult stopResult = sdk.stopTransactionStream("ACCOUNT_ID");
```

### Kotlin Implementation

```kotlin
// Start monitoring with lambda
val streamResult = sdk.startTransactionStream("ACCOUNT_ID") { transaction ->
    println("New transaction: ${transaction.type} Amount: ${transaction.amount} DZT")
    
    if (transaction.type == "received") {
        handleIncomingPayment(transaction)
    }
}

// Check status
val status = sdk.getStreamStatus("ACCOUNT_ID")
println("Stream active: ${status.active}")

// Stop monitoring
val stopResult = sdk.stopTransactionStream("ACCOUNT_ID")
```

---

## 🏦 CIB Integration

Complete CIB bank integration for fiat-to-crypto transactions.

```java
public class CIBPaymentProcessor {
    private final SofizPayStellarSDK sdk;
    
    public CIBPaymentProcessor() {
        this.sdk = new SofizPayStellarSDK(false); // Production
    }
    
    public void processCIBDeposit(String account, String amount, String reference) {
        CIBTransactionData cibData = new CIBTransactionData();
        cibData.account = account;
        cibData.amount = amount;
        cibData.reference = reference;
        cibData.description = "DZT token purchase via CIB";
        cibData.apiKey = System.getenv("CIB_API_KEY");
        
        CIBTransactionResult result = sdk.makeCIBTransaction(cibData);
        
        if (result.success) {
            System.out.println("CIB transaction successful");
            Map<String, Object> responseData = result.data;
            // Process response data
        } else {
            System.err.println("CIB transaction failed: " + result.message);
        }
    }
    
    public void close() {
        sdk.close();
    }
}
```

---

## 💡 Usage Examples

### Complete Payment System (Java)

```java
public class PaymentSystem {
    private final SofizPayStellarSDK sdk;
    
    public PaymentSystem() {
        this.sdk = new SofizPayStellarSDK(true); // Testnet
    }
    
    public void processPayment(String secretKey, String destination, 
                             String amount, String memo) {
        try {
            // Check balance first
            String accountId = sdk.getPublicKey(secretKey).publicKey;
            BalanceResult balance = sdk.getDZTBalance(accountId);
            
            System.out.println("Current balance: " + balance.balance);
            
            // Create payment
            PaymentData paymentData = new PaymentData();
            paymentData.secret = secretKey;
            paymentData.destination = destination;
            paymentData.amount = amount;
            paymentData.memo = memo;
            paymentData.assetCode = "DZT";
            
            // Submit payment
            PaymentResult result = sdk.submit(paymentData);
            
            if (result.success) {
                System.out.println("✅ Payment successful!");
                System.out.println("Transaction ID: " + result.transactionId);
                
                // Start monitoring for confirmation
                startPaymentMonitoring(accountId, result.transactionId);
            } else {
                System.err.println("❌ Payment failed: " + result.message);
            }
            
        } catch (Exception e) {
            System.err.println("Error processing payment: " + e.getMessage());
        }
    }
    
    private void startPaymentMonitoring(String accountId, String expectedTxId) {
        TransactionCallback callback = new TransactionCallback() {
            @Override
            public void onNewTransaction(TransactionInfo transaction) {
                if (expectedTxId.equals(transaction.hash)) {
                    System.out.println("✅ Payment confirmed!");
                    sdk.stopTransactionStream(accountId);
                }
            }
        };
        
        sdk.startTransactionStream(accountId, callback);
    }
    
    public void close() {
        sdk.close();
    }
}
```

### E-commerce Integration (Kotlin)

```kotlin
class ECommercePaymentProcessor(private val isTestnet: Boolean = true) : AutoCloseable {
    private val sdk = SofizPayStellarSDK(isTestnet)
    
    suspend fun processOrder(order: Order): PaymentResult {
        return try {
            // Validate order
            if (order.amount <= 0) {
                return PaymentResult(false, "Invalid amount", null)
            }
            
            // Create payment
            val paymentData = SofizPayStellarSDK.PaymentData(
                secret = order.customerSecretKey,
                destination = getCompanyPublicKey(),
                amount = order.amount.toString(),
                memo = "Order #${order.id}",
                assetCode = "DZT"
            )
            
            // Submit payment
            val result = sdk.submit(paymentData)
            
            if (result.success) {
                // Log successful payment
                logPayment(order.id, result.transactionId!!)
                
                // Start monitoring for confirmation
                startOrderMonitoring(order)
            }
            
            result
        } catch (e: Exception) {
            PaymentResult(false, "Payment processing error: ${e.message}", null)
        }
    }
    
    private fun startOrderMonitoring(order: Order) {
        val companyAccountId = getCompanyPublicKey()
        
        sdk.startTransactionStream(companyAccountId) { transaction ->
            if (transaction.memo == "Order #${order.id}" && transaction.type == "received") {
                println("✅ Order ${order.id} payment confirmed!")
                fulfillOrder(order)
                sdk.stopTransactionStream(companyAccountId)
            }
        }
    }
    
    private fun getCompanyPublicKey(): String = "COMPANY_PUBLIC_KEY"
    private fun logPayment(orderId: String, txId: String) { /* Log to database */ }
    private fun fulfillOrder(order: Order) { /* Fulfill order */ }
    
    override fun close() = sdk.close()
}

data class Order(
    val id: String,
    val customerSecretKey: String,
    val amount: Double,
    val items: List<String>
)
```

### Transaction Analytics

```java
public class TransactionAnalytics {
    private final SofizPayStellarSDK sdk;
    
    public TransactionAnalytics() {
        this.sdk = new SofizPayStellarSDK(false); // Production
    }
    
    public void generateReport(String accountId, int days) {
        try {
            // Get recent transactions
            TransactionHistoryResult result = sdk.getTransactions(accountId, 200);
            
            double totalReceived = 0;
            double totalSent = 0;
            int receivedCount = 0;
            int sentCount = 0;
            
            for (TransactionInfo tx : result.transactions) {
                if ("received".equals(tx.type)) {
                    totalReceived += Double.parseDouble(tx.amount);
                    receivedCount++;
                } else if ("sent".equals(tx.type)) {
                    totalSent += Double.parseDouble(tx.amount);
                    sentCount++;
                }
            }
            
            System.out.println("=== Transaction Report ===");
            System.out.println("Total Received: " + totalReceived + " DZT (" + receivedCount + " transactions)");
            System.out.println("Total Sent: " + totalSent + " DZT (" + sentCount + " transactions)");
            System.out.println("Net Flow: " + (totalReceived - totalSent) + " DZT");
            
        } catch (Exception e) {
            System.err.println("Error generating report: " + e.getMessage());
        }
    }
    
    public void close() {
        sdk.close();
    }
}
```

---

## ⚠️ Error Handling

All methods return structured response objects with consistent error handling:

```java
// Java
PaymentResult result = sdk.submit(paymentData);
if (result.success) {
    System.out.println("Success: " + result.transactionId);
} else {
    System.err.println("Error: " + result.message);
}
```

```kotlin
// Kotlin
val result = sdk.submit(paymentData)
if (result.success) {
    println("Success: ${result.transactionId}")
} else {
    println("Error: ${result.message}")
}
```

### Common Error Messages

- `"Secret key is required."`
- `"Destination address is required."`
- `"Amount is required."`
- `"Bad request: Invalid destination address"`
- `"Network error: Connection timeout"`
- `"Insufficient balance for transaction"`

### Exception Handling

```java
try (SofizPayStellarSDK sdk = new SofizPayStellarSDK()) {
    // SDK operations
} catch (Exception e) {
    System.err.println("SDK error: " + e.getMessage());
    e.printStackTrace();
}
```

---

## 🏆 Best Practices

### Security
```java
// ✅ Store secret keys securely
String secretKey = System.getenv("STELLAR_SECRET_KEY"); // From environment
// ❌ Never hardcode secret keys in source code

// ✅ Validate inputs
if (amount == null || amount.isEmpty()) {
    throw new IllegalArgumentException("Amount is required");
}

// ✅ Use try-with-resources for automatic cleanup
try (SofizPayStellarSDK sdk = new SofizPayStellarSDK()) {
    // Use SDK
} // Automatically closed
```

### Performance
```java
// ✅ Reuse SDK instances
private static final SofizPayStellarSDK sdk = new SofizPayStellarSDK();

// ✅ Use reasonable limits for transaction queries
TransactionHistoryResult result = sdk.getTransactions(accountId, 50); // Not 1000+

// ✅ Stop streams when no longer needed
sdk.stopTransactionStream(accountId);
```

### Error Resilience
```java
// ✅ Always check success status
if (result.success) {
    // Process successful result
} else {
    // Handle error gracefully
    logError("Payment failed", result.message);
}

// ✅ Implement retry logic for network operations
public PaymentResult submitWithRetry(PaymentData data, int maxRetries) {
    for (int i = 0; i < maxRetries; i++) {
        PaymentResult result = sdk.submit(data);
        if (result.success || !isRetryableError(result.message)) {
            return result;
        }
        try {
            Thread.sleep(1000 * (i + 1)); // Exponential backoff
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            break;
        }
    }
    return new PaymentResult(false, "Max retries exceeded", null);
}
```

---

## 🔄 Java vs Kotlin

This SDK provides both Java and Kotlin implementations with identical functionality:

### Java Strengths
- **Familiar Syntax**: Traditional Java patterns
- **Enterprise Ready**: Mature ecosystem
- **Explicit Types**: Clear type declarations
- **Wide Adoption**: Large developer community

### Kotlin Advantages
- **Concise Syntax**: Less boilerplate code
- **Null Safety**: Compile-time null checks
- **Data Classes**: Built-in equals/hashCode/toString
- **Lambda Support**: Modern functional programming

### Migration Path
You can easily migrate from Java to Kotlin implementation:

```java
// Java
PaymentData data = new PaymentData();
data.secret = "SECRET";
data.destination = "DEST";
data.amount = "10.0";
```

```kotlin
// Kotlin
val data = PaymentData(
    secret = "SECRET",
    destination = "DEST", 
    amount = "10.0"
)
```

---

## 🧪 Testing

### Unit Tests
```bash
./gradlew test
```

### Integration Tests
```bash
./gradlew integrationTest
```

### Test Example
```java
@Test
public void testPaymentSubmission() {
    SofizPayStellarSDK sdk = new SofizPayStellarSDK(true); // Testnet
    
    // Create test account
    AccountCreationResult account = sdk.createAccount();
    
    // Fund account
    FundResult fundResult = sdk.fundAccountFromFaucet(account.accountId);
    assertTrue(fundResult.success);
    
    // Test payment
    PaymentData paymentData = new PaymentData();
    paymentData.secret = account.secretKey;
    paymentData.destination = "DESTINATION_PUBLIC_KEY";
    paymentData.amount = "1.0";
    paymentData.memo = "Test payment";
    
    PaymentResult result = sdk.submit(paymentData);
    assertTrue(result.success);
    assertNotNull(result.transactionId);
    
    sdk.close();
}
```

---

## 🤝 Contributing

We welcome contributions! Here's how to get started:

### Development Setup
```bash
# Clone repository
git clone https://github.com/kenandarabeh/sofizpay-sdk-java.git
cd sofizpay-sdk-java

# Build project
./gradlew build

# Run tests
./gradlew test

# Run examples
./gradlew runJavaExample
./gradlew runKotlinExample
```

### Contribution Guidelines

1. **Fork** the repository
2. **Create** feature branch: `git checkout -b feature/amazing-feature`
3. **Write** tests for new functionality
4. **Ensure** all tests pass: `./gradlew test`
5. **Commit** changes: `git commit -m 'Add amazing feature'`
6. **Push** to branch: `git push origin feature/amazing-feature`
7. **Open** a Pull Request

### Code Style
- Follow Java/Kotlin conventions
- Add Javadoc/KDoc for public methods
- Include unit tests for new features
- Use meaningful variable names
- Handle errors gracefully

---

## 📞 Support

- 📖 [Documentation](https://github.com/kenandarabeh/sofizpay-sdk-java#readme)
- 🐛 [Report Issues](https://github.com/kenandarabeh/sofizpay-sdk-java/issues)
- 💬 [Discussions](https://github.com/kenandarabeh/sofizpay-sdk-java/discussions)
- ⭐ [Star the Project](https://github.com/kenandarabeh/sofizpay-sdk-java)
- 📧 [Email Support](mailto:support@sofizpay.com)

### Getting Help

1. **Check the documentation** above
2. **Search existing issues** on GitHub
3. **Join our community** discussions
4. **Contact support** for enterprise needs

---

## 📄 License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.

```
MIT License

Copyright (c) 2025 SofizPay

Permission is hereby granted, free of charge, to any person obtaining a copy
of this software and associated documentation files (the "Software"), to deal
in the Software without restriction, including without limitation the rights
to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
copies of the Software, and to permit persons to whom the Software is
furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in all
copies or substantial portions of the Software.
```

---

## 🙏 Acknowledgments

- **Stellar Development Foundation** - For the robust Stellar blockchain platform
- **Stellar Java SDK** - Foundation library for Stellar operations
- **OkHttp** - Reliable HTTP client for network operations
- **Gson** - JSON serialization and deserialization
- **Gradle** - Build automation and dependency management
- **JetBrains** - Kotlin language development
- **Open Source Community** - For continuous improvement and feedback

---

## 🔮 Roadmap

### Upcoming Features
- [ ] **WebSocket Streams** - Real-time WebSocket transaction monitoring
- [ ] **Multi-signature Support** - Advanced security with multiple signatures
- [ ] **Token Operations** - Custom token creation and management
- [ ] **Advanced Analytics** - Built-in transaction analytics and reporting
- [ ] **Spring Boot Integration** - Auto-configuration for Spring applications
- [ ] **Reactive Streams** - RxJava/Reactor support for reactive applications

### Version History
- **v1.0.0** - Initial release with core payment functionality
- **v1.0.0-kotlin** - Kotlin implementation with modern language features

---

<div align="center">
  <h3>🚀 Ready to integrate blockchain payments?</h3>
  <p>Start building with SofizPay SDK today!</p>
  
  <p>
    <a href="https://github.com/kenandarabeh/sofizpay-sdk-java">GitHub</a> •
    <a href="https://search.maven.org/artifact/com.kenandarabeh/sofizpay-sdk-java">Maven Central</a> •
    <a href="https://github.com/kenandarabeh/sofizpay-sdk-java/issues">Support</a> •
    <a href="mailto:support@sofizpay.com">Contact</a>
  </p>
  
  <p>Made with ❤️ by the SofizPay Team</p>
</div>
