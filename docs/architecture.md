# Architecture — Vault Files

This document describes the technical architecture, component boundaries, and state flow of Vault Files.
Read this before changing screens, ViewModels, repository logic, database entities, or Storage Access Framework integrations.

Read first:
- [../AGENTS.md](../AGENTS.md) (or [../CLAUDE.md](../CLAUDE.md))
- [guidelines/architecture.md](guidelines/architecture.md)
- [security.md](security.md)

---

## 1. High-Level Design

Vault Files is a single-module native Android application built with Kotlin and Jetpack Compose.
It implements the Model-View-ViewModel (MVVM) architecture with unidirectional data flow (UDF) without third-party dependency injection or navigation libraries.

```
┌─────────────────────────────────────────────────────────┐
│                     MainActivity                        │
│         (FragmentActivity, Tab Index Switcher)          │
└────────────────────────────┬────────────────────────────┘
                             │
                             ▼
┌─────────────────────────────────────────────────────────┐
│                   StorageViewModel                      │
│     (StateFlows, One-Shot User Message SharedFlow)      │
└──────────────┬───────────────────────────┬──────────────┘
               │                           │
               ▼                           ▼
┌──────────────────────────────┐ ┌────────────────────────┐
│      StorageRepository       │ │  Biometric / Zip Utils │
│   (Dispatchers.IO File I/O)  │ │ (BiometricPrompt, Zip) │
└──────┬───────────────┬───────┘ └────────────────────────┘
       │               │
       ▼               ▼
┌──────────────┐ ┌──────────────┐
│  Room (DB)   │ │  Filesystem  │
│(vault_files) │ │(Storage,Vault│
└──────────────┘ └──────────────┘
```

> **Simulated Vault Notice:** Most "secure storage" behaviour in the Vault is simulated byte renaming and database index tracking, not full disk encryption. Only Secure Notes (`.securenote`) use real AES-256-GCM hardware-backed cryptography.

---

## 2. Layered Architecture & Responsibilities

### 2.1 UI Layer (`ui/`)
- **`MainActivity`** (`FragmentActivity`): Owns the top-level window, handles app lock PIN/biometric verification gate on cold starts, and renders the `Scaffold` containing the bottom `NavigationBar`. Tabs are switched via an integer state index (`activeTabIndex`: 0 = Storage, 1 = Files, 2 = Vault, 3 = Settings), avoiding complex navigation graphs.
- **Screens**:
  - `StorageAnalyzerScreen`: Visual storage distribution ring chart, categorized storage buckets, and All-Files permission shortcut.
  - `FileExplorerScreen`: Hierarchical file browser, search, sorting, multi-select operations, breadcrumbs, and in-app file/note viewers.
  - `SecureVaultScreen`: Gated vault file list, restore/delete actions, and vault import dialogs.
  - `SettingsScreen`: Sub-page hub for Display, Security (PIN setup, shielded folders list), Help FAQ, Permissions status, and About metadata.
  - `AboutScreen`: Renders app build metadata read from `BuildConfig` fields (Pattern B).
- **`StorageViewModel`** (`AndroidViewModel`): The single source of truth for the entire application. Exposes immutable `StateFlow`s for file listings, storage usage statistics, active folder paths, preferences, and session-based unlock states. Dispatches transient notifications via `MutableSharedFlow<String> userMessage`.

### 2.2 Domain & Data Layer (`data/`)
- **`StorageRepository`**: Encapsulates all disk I/O, Room database interactions, and file system modifications on `Dispatchers.IO`. Manages two primary on-disk directories under `context.filesDir`:
  - `Storage/`: User-accessible private sandbox storage.
  - `Vault/`: Opaque store for vaulted items renamed to `vault_<uuid>.secured`.
- **`AppDatabase` & DAOs** (`DatabaseEntities.kt`): Room database `vault_files_database` configured with destructive migration fallback (`fallbackToDestructiveMigration()`). Houses three tables:
  1. `app_settings`: Key/value store backing application preferences.
  2. `secured_folders`: Stored directory paths protected by session authentication.
  3. `vault_files`: Metadata linking opaque vault filenames to original file names and sizes.
- **`CryptoManager`**: AES-256-GCM cryptography service using Android Keystore keys for Secure Notes.

### 2.3 System Integration & Storage Access Framework (`data/VaultDocumentsProvider.kt`)
- **`VaultDocumentsProvider`**: Implements Android's `DocumentsProvider` under authority `in.sreerajp.vault_files.documents`.
- Exposes `filesDir/Storage` to the system file picker (`GET_CONTENT` / `OPEN_DOCUMENT`).
- **Exposure Boundary**: The provider enforces `isExposable(file)` across all entry points, strictly blocking access to `Vault/`, all shielded folders, and path-traversal attempts.

---

## 3. Package & Source Structure

```
app/src/main/java/in/sreerajp/vault_files/
├── MainActivity.kt               # Entry Activity and tab coordinator
├── config/
│   └── AppConstants.kt           # Project-wide technical constants (DB name, dir names, thresholds)
├── data/
│   ├── AppDatabase.kt            # Room database definition
│   ├── CryptoManager.kt          # AES-256-GCM Keystore cryptography
│   ├── DatabaseEntities.kt       # Entities & DAOs (settings, shielded folders, vault files)
│   ├── StorageRepository.kt      # Core file I/O & data abstraction
│   └── VaultDocumentsProvider.kt # Storage Access Framework provider
├── ui/
│   ├── AboutScreen.kt            # About screen (Pattern B BuildConfig metadata)
│   ├── FileExplorerScreen.kt     # File manager Composable screen
│   ├── PermissionsScreen.kt      # Real-time permissions status viewer
│   ├── SecureVaultScreen.kt      # Vault browser and actions
│   ├── SettingsScreen.kt         # Preferences hub
│   ├── ShareSupport.kt           # Shared intent parsing & import dispatcher
│   ├── StorageAnalyzerScreen.kt  # Storage ring chart and breakdown
│   ├── StorageViewModel.kt       # Unified ViewModel
│   └── theme/                    # Color, Theme, Type design tokens
└── utils/
    ├── BiometricHelper.kt        # BiometricPrompt integration
    └── ZipUtility.kt             # Zip/Unzip archive utility with Zip-Slip protection
```

---

## 4. Key Architectural Patterns & Conventions

1. **Preference State Flow**:
   - New settings add a typed getter/setter in `StorageRepository` reading from the `app_settings` Room table.
   - `StorageViewModel` loads the setting in `init`, exposes a `StateFlow<T>`, and provides a mutating function that launches a coroutine to update the repository.
2. **Action → Execution → Feedback Loop**:
   - UI triggers a ViewModel method.
   - ViewModel executes repository suspend functions on `Dispatchers.IO`.
   - ViewModel emits one-shot feedback via `_userMessage.emit(message)`.
   - ViewModel reloads active directory files and refreshes storage analytics.
3. **Session-Based Unlock Model**:
   - Folder shields, hidden-file unlocks, and vault access are stored as transient in-memory flags in `StorageViewModel` (`_unlockedFolderSessions`, `_isHiddenUnlocked`, `_isVaultUnlocked`).
   - Unlocks reset when the application process terminates.
4. **UI Test Tags**:
   - Interactive UI elements maintain consistent `Modifier.testTag(...)` attributes (e.g. `nav_files_tab`, `about_screen`, `about_back_btn`) for unit and screenshot testing.
