# Build and Test — Vault Files

This document provides build instructions, Gradle task references, testing commands, and signing configurations for Vault Files.
Read this before building release artifacts, executing unit or screenshot tests, or modifying build scripts.

Read first:
- [../AGENTS.md](../AGENTS.md) (or [../CLAUDE.md](../CLAUDE.md))
- [guidelines/kotlin_build_configuration_guide.md](guidelines/kotlin_build_configuration_guide.md)
- [release_process.md](release_process.md)

---

## 1. Environment & Prerequisites

- **JDK:** 17 (Eclipse Temurin or OpenJDK recommended)
- **Android SDK:** `compileSdk` 36, `targetSdk` 36, `minSdk` 24
- **Build System:** Gradle Wrapper (`./gradlew` on Linux/macOS, `gradlew.bat` on Windows)
- **Plugins:** Android Application (`com.android.application`), Kotlin Android, Kotlin Compose, Google KSP, Roborazzi, Secrets Gradle Plugin.

---

## 2. Common Build Commands

### 2.1 Build APKs
```bash
# Debug build
./gradlew assembleDebug

# Release build (requires keystore.properties)
./gradlew assembleRelease
```

### 2.2 Install on Device / Emulator
```bash
./gradlew installDebug
```

### 2.3 Run Tests
```bash
# Run all JVM / Robolectric unit tests
./gradlew testDebugUnitTest

# Run a specific test class
./gradlew testDebugUnitTest --tests "in.sreerajp.vault_files.ExampleRobolectricTest"
./gradlew testDebugUnitTest --tests "in.sreerajp.vault_files.ShareSupportTest"

# Run instrumented tests (connected physical device or emulator)
./gradlew connectedAndroidTest
```

### 2.4 Screenshot Testing (Roborazzi)
Screenshot tests generate golden images located in `app/src/test/screenshots/`:

```bash
# Regenerate golden reference screenshots
./gradlew recordRoborazziDebug

# Verify current rendering against reference goldens
./gradlew verifyRoborazziDebug
```

### 2.5 Code Quality & Linting
```bash
./gradlew lint
```

---

## 3. Configuration & Signing Gotchas

### 3.1 Keystore Configuration
- The build expects a signing keystore at the project root (`vfkeystore.jks`).
- Credentials are read from `keystore.properties` at the project root:
  ```properties
  storeFile=vfkeystore.jks
  storePassword=<keystore-password>
  keyAlias=<alias>
  keyPassword=<key-password>
  ```
- Both `debugConfig` and `release` signing configurations in `app/build.gradle.kts` consume `keystore.properties`.
- Ensure `keystore.properties` and `*.jks` remain git-ignored and are never committed.

### 3.2 Secrets Gradle Plugin
- Secrets are managed via `com.google.android.libraries.mapsplatform.secrets-gradle-plugin`.
- The plugin looks for `.env` at the project root, falling back to `.env.example`.
- No sensitive external API keys are required for local builds.

### 3.3 Configuration Cache
- Gradle configuration cache is enabled in `gradle.properties` (`org.gradle.configuration-cache=true`).
- If you modify build scripts or dependencies and observe stale task execution, bypass cache using:
  ```bash
  ./gradlew testDebugUnitTest --no-configuration-cache
  ```

---

## 4. Package & Identifier Alignment

The single package identifier across the entire repository is:
```
in.sreerajp.vault_files
```
- **Kotlin Source Package:** `package `in`.sreerajp.vault_files` (backticked `in` in source)
- **Android Namespace:** `in.sreerajp.vault_files`
- **Application ID:** `in.sreerajp.vault_files`
