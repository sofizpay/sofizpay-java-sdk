# Publishing Guide for SofizPay SDK Java/Kotlin

This guide explains how to publish the SofizPay SDK to Maven Central and other repositories.

## 📋 Table of Contents

- [Prerequisites](#prerequisites)
- [Setup Configuration](#setup-configuration)
- [Maven Central Publishing](#maven-central-publishing)
- [GitHub Packages Publishing](#github-packages-publishing)
- [Local Repository Publishing](#local-repository-publishing)
- [Automated CI/CD Publishing](#automated-cicd-publishing)
- [Version Management](#version-management)
- [Troubleshooting](#troubleshooting)

---

## 🔧 Prerequisites

Before publishing, ensure you have:

### 1. Accounts and Credentials
- **Sonatype OSSRH Account**: [Sign up here](https://issues.sonatype.org/secure/Signup!default.jspa)
- **GPG Key**: For signing artifacts
- **GitHub Account**: For repository access
- **Maven Central Group ID**: Verified domain ownership

### 2. Required Tools
```bash
# Install GPG
# Ubuntu/Debian
sudo apt-get install gnupg

# macOS
brew install gnupg

# Windows
# Download from https://gpg4win.org/
```

### 3. Verify Project Structure
```
sofizpay-sdk-java/
├── build.gradle
├── gradle.properties
├── settings.gradle
├── src/
│   ├── main/
│   │   ├── java/
│   │   └── kotlin/
│   └── test/
├── LICENSE
├── README.md
└── PUBLISHING.md
```

---

## ⚙️ Setup Configuration

### 1. Configure gradle.properties
Create/update `gradle.properties` with your publishing information:

```properties
# Project Information
GROUP=com.sofizpay
VERSION_NAME=1.0.0
POM_DESCRIPTION=SofizPay SDK for Java/Kotlin - Stellar blockchain DZT token payments

# POM Information
POM_NAME=SofizPay SDK Java
POM_ARTIFACT_ID=sofizpay-sdk-java
POM_PACKAGING=jar

# Project URLs
POM_URL=https://github.com/sofizpay/sofizpay-sdk-java
POM_SCM_URL=https://github.com/sofizpay/sofizpay-sdk-java
POM_SCM_CONNECTION=scm:git:git://github.com/sofizpay/sofizpay-sdk-java.git
POM_SCM_DEV_CONNECTION=scm:git:ssh://git@github.com/sofizpay/sofizpay-sdk-java.git

# License
POM_LICENCE_NAME=MIT License
POM_LICENCE_URL=https://opensource.org/licenses/MIT
POM_LICENCE_DIST=repo

# Developer Information
POM_DEVELOPER_ID=sofizpay
POM_DEVELOPER_NAME=SofizPay Team
POM_DEVELOPER_EMAIL=support@sofizpay.com

# Issue Management
POM_ISSUE_MANAGEMENT_SYSTEM=GitHub Issues
POM_ISSUE_MANAGEMENT_URL=https://github.com/sofizpay/sofizpay-sdk-java/issues

# Signing Configuration (will be set via environment variables)
signing.keyId=YOUR_KEY_ID
signing.password=YOUR_KEY_PASSWORD
signing.secretKeyRingFile=YOUR_KEY_RING_FILE

# Repository Configuration
SONATYPE_USERNAME=your_sonatype_username
SONATYPE_PASSWORD=your_sonatype_password
```

### 2. Update build.gradle for Publishing

Add this to your `build.gradle`:

```gradle
plugins {
    id 'java-library'
    id 'kotlin'
    id 'maven-publish'
    id 'signing'
}

// Publishing configuration
publishing {
    publications {
        maven(MavenPublication) {
            from components.java
            
            groupId = project.GROUP
            artifactId = project.POM_ARTIFACT_ID
            version = project.VERSION_NAME
            
            // Include sources and javadoc
            artifact sourcesJar
            artifact javadocJar
            
            pom {
                name = project.POM_NAME
                description = project.POM_DESCRIPTION
                url = project.POM_URL
                
                licenses {
                    license {
                        name = project.POM_LICENCE_NAME
                        url = project.POM_LICENCE_URL
                        distribution = project.POM_LICENCE_DIST
                    }
                }
                
                developers {
                    developer {
                        id = project.POM_DEVELOPER_ID
                        name = project.POM_DEVELOPER_NAME
                        email = project.POM_DEVELOPER_EMAIL
                    }
                }
                
                scm {
                    url = project.POM_SCM_URL
                    connection = project.POM_SCM_CONNECTION
                    developerConnection = project.POM_SCM_DEV_CONNECTION
                }
                
                issueManagement {
                    system = project.POM_ISSUE_MANAGEMENT_SYSTEM
                    url = project.POM_ISSUE_MANAGEMENT_URL
                }
            }
        }
    }
    
    repositories {
        maven {
            name = "OSSRH"
            def releasesRepoUrl = "https://s01.oss.sonatype.org/service/local/staging/deploy/maven2/"
            def snapshotsRepoUrl = "https://s01.oss.sonatype.org/content/repositories/snapshots/"
            url = version.endsWith('SNAPSHOT') ? snapshotsRepoUrl : releasesRepoUrl
            
            credentials {
                username = project.findProperty("SONATYPE_USERNAME") ?: System.getenv("SONATYPE_USERNAME")
                password = project.findProperty("SONATYPE_PASSWORD") ?: System.getenv("SONATYPE_PASSWORD")
            }
        }
        
        maven {
            name = "GitHubPackages"
            url = "https://maven.pkg.github.com/sofizpay/sofizpay-sdk-java"
            credentials {
                username = project.findProperty("GITHUB_USERNAME") ?: System.getenv("GITHUB_USERNAME")
                password = project.findProperty("GITHUB_TOKEN") ?: System.getenv("GITHUB_TOKEN")
            }
        }
    }
}

// Signing configuration
signing {
    required { gradle.taskGraph.hasTask("publish") }
    sign publishing.publications.maven
}

// Create sources jar
task sourcesJar(type: Jar) {
    archiveClassifier = 'sources'
    from sourceSets.main.allSource
}

// Create javadoc jar
task javadocJar(type: Jar) {
    archiveClassifier = 'javadoc'
    from javadoc.destinationDir
    dependsOn javadoc
}

// Ensure artifacts are built
artifacts {
    archives sourcesJar
    archives javadocJar
}
```

---

## 📦 Maven Central Publishing

### 1. Generate GPG Key
```bash
# Generate new GPG key
gpg --gen-key

# List keys to get Key ID
gpg --list-secret-keys --keyid-format LONG

# Export public key to key servers
gpg --keyserver keyserver.ubuntu.com --send-keys YOUR_KEY_ID
gpg --keyserver keys.openpgp.org --send-keys YOUR_KEY_ID
```

### 2. Set Environment Variables
```bash
# For Unix/Linux/macOS
export SONATYPE_USERNAME="your_username"
export SONATYPE_PASSWORD="your_password"
export SIGNING_KEY_ID="your_key_id"
export SIGNING_PASSWORD="your_key_password"
export SIGNING_SECRET_KEY_RING_FILE="$HOME/.gnupg/secring.gpg"

# For Windows PowerShell
$env:SONATYPE_USERNAME="your_username"
$env:SONATYPE_PASSWORD="your_password"
$env:SIGNING_KEY_ID="your_key_id"
$env:SIGNING_PASSWORD="your_key_password"
$env:SIGNING_SECRET_KEY_RING_FILE="C:\Users\YourUser\.gnupg\secring.gpg"
```

### 3. Create OSSRH Ticket
1. Go to [Sonatype JIRA](https://issues.sonatype.org/)
2. Create new issue with project "Community Support - Open Source Project Repository Hosting (OSSRH)"
3. Fill in details:
   - **Summary**: Request for com.sofizpay group
   - **Group Id**: com.sofizpay
   - **Project URL**: https://github.com/sofizpay/sofizpay-sdk-java
   - **SCM URL**: https://github.com/sofizpay/sofizpay-sdk-java.git

### 4. Publish to Staging
```bash
# Clean and build
./gradlew clean build

# Publish to staging repository
./gradlew publishToSonatype

# Or publish all
./gradlew publish
```

### 5. Release from Staging
```bash
# Close and release staging repository
./gradlew closeAndReleaseRepository

# Or manually via Nexus Repository Manager:
# 1. Go to https://s01.oss.sonatype.org/
# 2. Login with your credentials
# 3. Go to "Staging Repositories"
# 4. Find your repository
# 5. Select and click "Close"
# 6. After validation, click "Release"
```

---

## 📋 GitHub Packages Publishing

### 1. Setup GitHub Token
```bash
# Create Personal Access Token with packages:write permission
# Go to GitHub Settings > Developer settings > Personal access tokens
# Generate new token with 'write:packages' scope

export GITHUB_USERNAME="your_github_username"
export GITHUB_TOKEN="your_personal_access_token"
```

### 2. Publish to GitHub Packages
```bash
./gradlew publishMavenPublicationToGitHubPackagesRepository
```

### 3. Usage from GitHub Packages
```gradle
repositories {
    maven {
        name = "GitHubPackages"
        url = "https://maven.pkg.github.com/sofizpay/sofizpay-sdk-java"
        credentials {
            username = "YOUR_GITHUB_USERNAME"
            password = "YOUR_GITHUB_TOKEN"
        }
    }
}

dependencies {
    implementation 'com.sofizpay:sofizpay-sdk-java:1.0.0'
}
```

---

## 🏠 Local Repository Publishing

### Publish to Local Repository
```bash
# Publish to local Maven repository (~/.m2/repository)
./gradlew publishToMavenLocal
```

### Test Local Installation
```gradle
repositories {
    mavenLocal()
    mavenCentral()
}

dependencies {
    implementation 'com.sofizpay:sofizpay-sdk-java:1.0.0'
}
```

---

## 🤖 Automated CI/CD Publishing

### GitHub Actions Workflow
Create `.github/workflows/publish.yml`:

```yaml
name: Publish Package

on:
  release:
    types: [published]
  workflow_dispatch:
    inputs:
      version:
        description: 'Version to publish'
        required: true
        default: '1.0.0'

jobs:
  publish:
    runs-on: ubuntu-latest
    
    steps:
    - name: Checkout code
      uses: actions/checkout@v4
      
    - name: Set up JDK
      uses: actions/setup-java@v4
      with:
        java-version: '11'
        distribution: 'temurin'
        
    - name: Cache Gradle packages
      uses: actions/cache@v3
      with:
        path: |
          ~/.gradle/caches
          ~/.gradle/wrapper
        key: ${{ runner.os }}-gradle-${{ hashFiles('**/*.gradle*', '**/gradle-wrapper.properties') }}
        restore-keys: |
          ${{ runner.os }}-gradle-
          
    - name: Grant execute permission for gradlew
      run: chmod +x gradlew
      
    - name: Import GPG key
      env:
        GPG_PRIVATE_KEY: ${{ secrets.GPG_PRIVATE_KEY }}
        GPG_PASSPHRASE: ${{ secrets.GPG_PASSPHRASE }}
      run: |
        echo "$GPG_PRIVATE_KEY" | base64 --decode | gpg --import --batch --yes --passphrase="$GPG_PASSPHRASE"
        
    - name: Build project
      run: ./gradlew clean build
      
    - name: Run tests
      run: ./gradlew test
      
    - name: Publish to Maven Central
      env:
        SONATYPE_USERNAME: ${{ secrets.SONATYPE_USERNAME }}
        SONATYPE_PASSWORD: ${{ secrets.SONATYPE_PASSWORD }}
        SIGNING_KEY_ID: ${{ secrets.SIGNING_KEY_ID }}
        SIGNING_PASSWORD: ${{ secrets.SIGNING_PASSWORD }}
        SIGNING_SECRET_KEY_RING_FILE: ${{ secrets.SIGNING_SECRET_KEY_RING_FILE }}
      run: ./gradlew publishToSonatype closeAndReleaseRepository
      
    - name: Publish to GitHub Packages
      env:
        GITHUB_USERNAME: ${{ github.actor }}
        GITHUB_TOKEN: ${{ secrets.GITHUB_TOKEN }}
      run: ./gradlew publishMavenPublicationToGitHubPackagesRepository
```

### Required Secrets
Add these secrets to your GitHub repository settings:

```
GPG_PRIVATE_KEY          # Base64 encoded GPG private key
GPG_PASSPHRASE          # GPG key passphrase
SONATYPE_USERNAME       # Sonatype OSSRH username
SONATYPE_PASSWORD       # Sonatype OSSRH password
SIGNING_KEY_ID          # GPG signing key ID
SIGNING_PASSWORD        # GPG signing password
SIGNING_SECRET_KEY_RING_FILE  # Path to GPG secret key ring
```

---

## 🔢 Version Management

### Semantic Versioning
Follow [Semantic Versioning](https://semver.org/):

```
MAJOR.MINOR.PATCH

1.0.0 - Initial release
1.0.1 - Bug fix
1.1.0 - New feature (backward compatible)
2.0.0 - Breaking change
```

### Version Update Script
Create `scripts/update-version.sh`:

```bash
#!/bin/bash

if [ $# -eq 0 ]; then
    echo "Usage: $0 <new-version>"
    echo "Example: $0 1.1.0"
    exit 1
fi

NEW_VERSION=$1

# Update gradle.properties
sed -i "s/VERSION_NAME=.*/VERSION_NAME=$NEW_VERSION/" gradle.properties

# Update README.md
sed -i "s/sofizpay-sdk-java:[0-9]\+\.[0-9]\+\.[0-9]\+/sofizpay-sdk-java:$NEW_VERSION/g" README.md

# Create git tag
git add gradle.properties README.md
git commit -m "Bump version to $NEW_VERSION"
git tag -a "v$NEW_VERSION" -m "Release version $NEW_VERSION"

echo "Version updated to $NEW_VERSION"
echo "Run 'git push origin main --tags' to push changes"
```

### Usage
```bash
chmod +x scripts/update-version.sh
./scripts/update-version.sh 1.1.0
git push origin main --tags
```

---

## 🐛 Troubleshooting

### Common Issues

#### 1. GPG Signing Fails
```bash
# Error: gpg: signing failed: No such file or directory

# Solution: Export secret key ring
gpg --export-secret-keys > ~/.gnupg/secring.gpg

# Or use newer GPG format
gpg --export-secret-keys --armor YOUR_KEY_ID > private-key.asc
```

#### 2. Sonatype Authentication Fails
```bash
# Error: 401 Unauthorized

# Solution: Verify credentials
curl -u "username:password" https://s01.oss.sonatype.org/service/local/authentication/login

# Check token instead of password
# Generate user token in Sonatype account
```

#### 3. POM Validation Errors
```bash
# Error: Missing required metadata

# Solution: Ensure all required POM elements are present:
# - name, description, url
# - license information
# - developer information
# - scm information
```

#### 4. Staging Repository Issues
```bash
# Error: Repository is already closed

# Solution: Check staging repositories manually
# Go to https://s01.oss.sonatype.org/#stagingRepositories
# Find your repository and check status
```

### Debug Publishing
```bash
# Dry run publishing
./gradlew publishToMavenLocal --dry-run

# Verbose output
./gradlew publish --info --stacktrace

# Check what will be published
./gradlew components

# Verify POM generation
./gradlew generatePomFileForMavenPublication
cat build/publications/maven/pom-default.xml
```

### Rollback Release
```bash
# Delete local tag
git tag -d v1.0.0

# Delete remote tag
git push origin :refs/tags/v1.0.0

# Revert version commit
git revert HEAD
```

---

## 📋 Pre-publish Checklist

Before publishing, ensure:

- [ ] All tests pass: `./gradlew test`
- [ ] Code is properly formatted
- [ ] Documentation is up to date
- [ ] Version number is updated
- [ ] Changelog is updated
- [ ] GPG key is properly configured
- [ ] Sonatype credentials are valid
- [ ] POM metadata is complete
- [ ] LICENSE file exists
- [ ] README.md is comprehensive

### Final Verification
```bash
# Complete verification script
./gradlew clean build test sourcesJar javadocJar

# Check generated artifacts
ls -la build/libs/

# Verify POM content
./gradlew generatePomFileForMavenPublication
cat build/publications/maven/pom-default.xml
```

---

## 📞 Support

If you encounter issues during publishing:

1. **Check Sonatype Documentation**: [OSSRH Guide](https://central.sonatype.org/publish/publish-guide/)
2. **Gradle Publishing Plugin**: [Official Documentation](https://docs.gradle.org/current/userguide/publishing_maven.html)
3. **Create GitHub Issue**: [Report Publishing Issues](https://github.com/sofizpay/sofizpay-sdk-java/issues)
4. **Contact Support**: support@sofizpay.com

---

## 🎉 Success!

Once successfully published:

1. **Verify on Maven Central**: https://search.maven.org/artifact/com.sofizpay/sofizpay-sdk-java
2. **Update Documentation**: Add installation instructions
3. **Announce Release**: Create GitHub release notes
4. **Share with Community**: Social media, forums, etc.

Congratulations on publishing your SofizPay SDK! 🚀
