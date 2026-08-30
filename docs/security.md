# Security — Vault Files

This document defines the security architecture, threat model, cryptographic boundaries, and data protection rules for Vault Files.
Read this before modifying authentication flows, permission handling, database models, file sharing, or cryptographic operations.

Read first:
- [../AGENTS.md](../AGENTS.md) (or [../CLAUDE.md](../CLAUDE.md))
- [guidelines/security.md](guidelines/security.md)
- [architecture.md](architecture.md)
- [features.md](features.md)

---

## 1. Threat Model & Security Posture

Vault Files operates as an **offline-first** secure storage and file manager. It handles sensitive user files, private notes, and password/biometric-shielded directories.

### 1.1 In-Scope Threats
- Unauthorized local access when another person physically handles the unlocked phone.
- Accidental exposure of private files to third-party apps browsing storage via the system file picker (`DocumentsProvider`).
- Tampering or unauthorized reading of encrypted notes (`.securenote`).
- Path-traversal attacks via malicious ZIP archives (Zip-Slip).

### 1.2 Out-of-Scope Threats / Inherent Limitations
- Rooted devices or direct physical ADB access bypassing application sandboxing.
- Other applications with elevated `MANAGE_EXTERNAL_STORAGE` reading unencrypted external media directly.
- The "Vault" feature renames files and tracks them in Room; it is **not disk-level encryption**.

---

## 2. Cryptographic Architecture

### 2.1 Secure Notes Encryption
- **Algorithm:** AES-256-GCM (Galois/Counter Mode) authenticated encryption.
- **Key Storage:** Android Keystore provider (`AndroidKeyStore`), hardware-backed when supported by device hardware (TEE / StrongBox).
- **Key Alias:** `vault_files_secure_notes_key` (managed by `CryptoManager`).
- **Data Format:** Nonce/IV prepended to the ciphertext and authentication tag.
- **Integrity Guarantee:** Tampered or corrupted ciphertext files fail decryption gracefully and do not output plaintext or throw unhandled exceptions.

### 2.2 PIN Storage & Vault Simulation Reality
- **App PIN:** Currently stored in plain text within the `app_settings` table of the Room database (`vault_files_database`).
- **Vault File Storage:** Stored in the private app sandbox (`filesDir/Vault/`) under opaque UUID filenames (`vault_<uuid>.secured`). Raw byte streams are unchanged.
- **Destructive Migrations:** Room drops and recreates all tables if the schema version increments without an explicit migration script.

---

## 3. Storage Access Framework & Exposure Boundary

The application implements `VaultDocumentsProvider` (`authority = in.sreerajp.vault_files.documents`) to allow users to access files via the Android Storage Access Framework (SAF).

```
System SAF File Picker
         │
         ▼
[VaultDocumentsProvider]
         │
    isExposable()?
    ├── Vault directory (filesDir/Vault) ──► BLOCKED
    ├── Shielded folders (DB list)       ──► BLOCKED
    ├── Path escape / traversal          ──► BLOCKED
    └── Regular sandbox files (Storage/) ──► ALLOWED
```

### Security Invariants:
1. `isExposable(file)` MUST evaluate to `false` for any path inside `filesDir/Vault/` or any path recorded in `secured_folders`.
2. All path queries sanitize inputs against path traversal (`..` escapes).
3. Synchronous binder calls ensure shielded folder paths are cached safely with short TTLs and never block on asynchronous UI operations.

---

## 4. Archive Security (Zip-Slip Mitigation)

`ZipUtility` enforces strict destination canonical path validation during archive decompression:
```kotlin
val targetFile = File(destinationDir, zipEntry.name)
val canonicalDestPath = destinationDir.canonicalPath
val canonicalTargetPath = targetFile.canonicalPath

if (!canonicalTargetPath.startsWith(canonicalDestPath + File.separator)) {
    throw SecurityException("Zip-Slip path traversal attempt detected: ${zipEntry.name}")
}
```

---

## 5. Permissions & Android Platform Hardening

| Permission | Usage | Security Safeguard |
|---|---|---|
| `MANAGE_EXTERNAL_STORAGE` | Browsing non-sandbox device files | Checked dynamically; denied state falls back safely to sandbox |
| `USE_BIOMETRIC` | App lock and shielded folder access | Delegates to Android `BiometricPrompt` with crypto or device credential |
| `REQUEST_INSTALL_PACKAGES` | Installing APKs from explorer | Verified via `packageManager.canRequestPackageInstalls()` before launching installer |
| `INTERNET` | **Not requested / unused** | App functions completely offline |

### Backup Configuration
- `android:allowBackup="true"` is declared in the manifest.
- Sensitive production deployments should configure `backup_rules.xml` and `data_extraction_rules.xml` to exclude the database and `Vault/` directory from cloud and adb backups.

---

## 6. Security Checklist for Changes

- [ ] Zero secrets, PINs, or raw byte streams printed to `Logcat` (even in debug).
- [ ] No direct file I/O on the main/UI thread.
- [ ] All new SAF entry points pass through `isExposable()` checks.
- [ ] All file extractions validate target canonical paths against Zip-Slip.
- [ ] Changes to `CryptoManager` preserve backward compatibility for existing `.securenote` files.
