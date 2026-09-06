# Release Process — Vault Files

This document provides step-by-step procedures for building, signing, verifying, and releasing production builds of Vault Files.
Read this before preparing or distributing an APK/AAB release.

Read first:
- [../AGENTS.md](../AGENTS.md) (or [../CLAUDE.md](../CLAUDE.md))
- [guidelines/release_process.md](guidelines/release_process.md)
- [build.md](build.md)
- [security.md](security.md)

---

## 1. Release Architecture & Keystore Rules

> [!CAUTION]
> The release keystore (`vfkeystore.jks`) and `keystore.properties` contain private cryptographic signing credentials. Never commit them to Git. Maintain at least one encrypted offline backup.

### 1.1 Required Signing Files
- **Keystore:** `vfkeystore.jks` at the project root.
- **Properties File:** `keystore.properties` at the project root, structured as follows:
  ```properties
  storePassword=<your-keystore-password>
  keyPassword=<your-key-password>
  keyAlias=<your-key-alias>
  storeFile=vfkeystore.jks
  ```

---

## 2. Pre-Release Preparation & Versioning

### 2.1 Versioning Policy
Vault Files uses `app/src/main/assets/config/app_config.json` as the **single source of truth** for application versioning and About-screen metadata (Pattern A).
Gradle's `app/build.gradle.kts` automatically reads `version` (for `versionName`) and `build` (for `versionCode`) directly from this asset configuration at build time.

To update the version before a release, edit `app/src/main/assets/config/app_config.json`:

```json
{
  "appName": "Vault Files",
  "description": "Secure file & storage manager.",
  "version": "18.0",
  "build": "18",
  "details": {
    "Author": "Sreeraj P",
    "IDE used": "Android Studio, Visual Studio Code",
    "AI used": "Claude Opus 4.8",
    "License": "All libraries used are open source."
  }
}
```

### 2.2 About Metadata Update
Update `details` in `app/src/main/assets/config/app_config.json` if author, IDE, AI tooling, or other informational fields have changed.
`ConfigService.loadAndVerify()` will automatically load these at runtime and ensure the asset's version matches the build's package info.

---

## 3. Step-by-Step Release Runbook

### Step 1: Clean & Run Test Suite
Ensure the codebase is clean, all tests pass, and screenshot regressions are verified:
```bash
./gradlew clean testDebugUnitTest
./gradlew verifyRoborazziDebug
./gradlew lint
```

### Step 2: Build Release APK
Execute the Gradle release build task:
```bash
./gradlew assembleRelease
```
The output APK is generated at:
```
app/build/outputs/apk/release/app-release.apk
```

### Step 3: Verify APK Signature & Alignment
Use Android SDK `apksigner` and `zipalign` to verify the artifact:

```bash
# Verify signing scheme v2/v3
apksigner verify --verbose app/build/outputs/apk/release/app-release.apk

# Verify 4-byte zip alignment
zipalign -c -v 4 app/build/outputs/apk/release/app-release.apk
```

### Step 4: Smoke Test Release Build
Install the signed release APK onto a physical test device:
```bash
adb install -r app/build/outputs/apk/release/app-release.apk
```
Perform smoke testing across core flows:
1. Cold app launch and PIN unlock.
2. File browsing and folder creation.
3. Secure note creation, editing, and decryption.
4. Vault file move and restore.
5. Storage analyzer recalculation.

---

## 4. Release Checklist

- [ ] `git status` is clean with no uncommitted working tree changes.
- [ ] `versionCode` and `versionName` incremented in `app/build.gradle.kts`.
- [ ] `about.properties` updated.
- [ ] `./gradlew testDebugUnitTest` passed with zero test failures.
- [ ] `apksigner verify` confirms valid v2/v3 signature.
- [ ] Smoke test completed on a physical Android device.
- [ ] Keystore files (`vfkeystore.jks`, `keystore.properties`) remain git-ignored.
