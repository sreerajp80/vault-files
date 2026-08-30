# Features — Vault Files

This document provides a comprehensive inventory of all user-facing and cross-cutting features implemented in Vault Files, along with clear security reality distinctions.
Read this to understand existing features before modifying functionality or designing similar capabilities.

Read first:
- [../AGENTS.md](../AGENTS.md) (or [../CLAUDE.md](../CLAUDE.md))
- [architecture.md](architecture.md)
- [security.md](security.md)

---

## 1. Executive Summary

Vault Files is a single-module Android file manager and storage analyzer with biometric protection, simulated vault storage, and hardware-backed encrypted notes.
It features four primary navigation tabs: **Storage**, **Files**, **Vault**, and **Settings**, alongside deep Android platform integrations (Storage Access Framework provider, file picker mode, and system share sheet support).

---

## 2. Security Reality & Behavioral Boundaries

> [!IMPORTANT]
> Understand the exact security properties before making architectural assumptions about privacy or encryption.

1. **Secure Notes (Real Cryptography):**
   - Encrypted using AES-256-GCM authenticated encryption.
   - Encryption keys reside inside the hardware-backed Android Keystore.
   - Files are stored with the `.securenote` extension as genuine ciphertext.
2. **Secure Vault (Simulated Privacy):**
   - Files moved into the Vault are copied with raw bytes unchanged and renamed to opaque identifiers (`vault_<uuid>.secured`).
   - Original filenames, sizes, and categories are tracked in the Room database.
   - Files are hidden from the system file picker, but raw file bytes remain unencrypted on disk.
3. **Shielded Folders:**
   - Folders are protected by in-app session authentication (PIN or biometric prompt).
   - Folders and files remain normal unencrypted filesystem nodes on disk.
   - Shielded paths are excluded from `VaultDocumentsProvider` to hide them from the system file picker.
4. **PIN Storage:**
   - The application PIN is stored in plain text in the Room `app_settings` table.
5. **Database Migrations:**
   - Room is configured with `fallbackToDestructiveMigration()`. Changing the schema version drops all tables, wiping settings, shielded folder paths, and vault indexes.

---

## 3. Core Tab Features

### 3.1 Storage Analyzer Tab
- **Visual Storage Breakdown:** Interactive ring chart displaying used, free, and total disk space.
- **Data Source Toggle:** Switch analysis between "App Sandbox" (internal private storage) and "Entire Device" (requires All-Files access permission).
- **Category Buckets:** Images, Videos, Audio, Documents, Archives, and Other. Tapping any bucket opens the Files tab filtered to that category.
- **Vault Summary Tile:** Displays total files and cumulative size stored inside the Vault.
- **Inline Permission Banner:** Contextual permission request card when "Entire Device" is selected without granted storage permissions.

### 3.2 File Explorer Tab
- **Browsing & Navigation:** Hierarchical directory tree, breadcrumb trail navigation, chunked background loading for large folders, and pull-to-refresh.
- **Search & Sorting:** Real-time search query filtering and multi-attribute sorting (Name, Size, Date, Ascending/Descending) with folders listed first.
- **Layout Modes:** Switchable between List, Grid (with image thumbnails and APK icons), and Compact views.
- **File & Folder Operations:**
  - Create new folder and create encrypted `.securenote`.
  - Rename, delete (recursive for folders), copy, and move.
  - Hide / unhide (Unix dot-prefix toggle).
  - Compression: create `.zip` archives and decompress archives into extracted directories with Zip-Slip path traversal protection.
  - Multi-select toolbar: bulk delete, multi-item details summary, batch move/copy, batch shield/unshield, and batch move to Vault.
- **Previews & External Handlers:**
  - In-app image preview and capped text file viewer (with binary file detection).
  - In-app Secure Notes rich text editor.
  - Double-tap external opening via standard Android `VIEW` intents or package installer for APKs.

### 3.3 Secure Vault Tab
- **Session-Locked Gating:** Protected by full-screen biometric or PIN authentication per session.
- **Vault File Management:** View original file names, category icons, timestamps, and sizes.
- **Actions:** Permanently delete or restore files to the dedicated "Restored" folder.
- **Direct Import:** Shared files from external apps can be ingested directly into the Vault.

### 3.4 Settings Tab
- **Display:** App theme selection (System / Light / Dark), show/hide hidden items toggle, and image/text preview toggles.
- **Security:** Set/update 4+ digit app PIN, toggle app launch protection, toggle hidden items protection, toggle delete/move action protection, and manage active shielded folders.
- **Permissions:** Live permission status viewer for standard and special permissions (All-Files access, install unknown apps).
- **Help:** Static FAQ on compression and file security mechanisms.
- **About:** Displays build metadata (version, author, IDE, AI version, last build date) loaded from `BuildConfig` constants.

---

## 4. Cross-Cutting & Platform Integrations

- **Storage Access Framework (`VaultDocumentsProvider`):** Exposes sandbox storage (`filesDir/Storage`) to system document pickers while strictly hiding `Vault/` and shielded folders.
- **File Picker Mode:** Allows Vault Files to act as a system file selector for third-party apps requesting `GET_CONTENT` / `OPEN_DOCUMENT`.
- **System Share Sheet Integration:** Supports receiving incoming `ACTION_SEND` and `ACTION_SEND_MULTIPLE` intents (with Bluetooth share compatibility) and sharing local files out to third-party apps.
- **Localization:** Bilingual support for English (`values/strings.xml`) and Malayalam (`values-ml/strings.xml`).
