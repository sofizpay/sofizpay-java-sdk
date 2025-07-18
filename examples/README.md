# SofizPay SDK Examples

This directory contains example implementations of the SofizPay SDK in both Java and Kotlin.

## Available Examples

### Java Example
- **File**: `CompleteSofizPayExample.java`
- **Run with**: `../gradlew runJavaExample`
- **Description**: Complete demonstration of all SDK features in Java

### Kotlin Example  
- **File**: `CompleteSofizPayExample.kt`
- **Run with**: `../gradlew runKotlinExample`
- **Description**: Complete demonstration of all SDK features in Kotlin

## Features Demonstrated

Both examples demonstrate:

1. **🔑 Key Management**
   - Extract public key from secret key
   - Create new accounts
   - Fund accounts from testnet faucet

2. **💰 Balance Operations**
   - Check DZT/XLM balances
   - Account balance monitoring

3. **💸 Payment Operations**
   - Send DZT payments with memo
   - Transaction submission and confirmation

4. **📜 Transaction History**
   - Retrieve transaction history
   - Search transactions by memo
   - Get specific transactions by hash

5. **📡 Real-time Monitoring**
   - Start/stop transaction streams
   - Real-time transaction callbacks
   - Stream status monitoring

6. **🔐 Security Features**
   - RSA signature verification
   - Secure transaction handling

7. **🏦 CIB Integration**
   - CIB bank transaction creation
   - Fiat-to-crypto integration

## Running Examples

### Prerequisites
```bash
# Build the project first
cd ..
./gradlew build
```

### Run Java Example
```bash
cd ..
./gradlew runJavaExample
```

### Run Kotlin Example
```bash
cd ..
./gradlew runKotlinExample
```

## Configuration

Before running examples, you may want to modify:

- **Network Settings**: Change `useMainnet` variable to switch between testnet/mainnet
- **Account Keys**: Replace with your own test account keys
- **Test Parameters**: Adjust amounts, memos, etc.

⚠️ **Important**: Examples use testnet by default for safety. Never use real funds in examples.

## Example Output

The examples will show:
- ✅ Successful operations
- ❌ Failed operations with error messages
- 🚨 Real-time transaction notifications
- 📊 Transaction status and details

## Next Steps

After running the examples, refer to the main [README.md](../README.md) for:
- Integration guides
- API documentation
- Best practices
- Publishing instructions
