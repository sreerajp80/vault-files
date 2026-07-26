# Architecture

Plain MVVM with a single ViewModel and a single repository — no DI framework, no navigation
library.

> Note: most "secure storage" behavior is **simulated demo data**, not real device storage. The
> repository seeds dummy files (filled with repeated bytes) into the app sandbox and synthesizes
> "virtual" external-storage files under `cacheDir/virtual_external`. The vault "encrypts" by
> copying bytes + renaming + DB-tracking the original name — it is **not real encryption**.

## Layers

- **`MainActivity`** (`FragmentActivity`, required for `BiometricPrompt`) — owns the whole UI.
  Builds the `StorageViewModel` via `ViewModelProvider`, renders an app-lock gate
  (`AppLockScreen`) when enabled, then a `Scaffold` with a 4-tab `NavigationBar`. **Tabs are
  switched by an integer index (`activeTabIndex`), not a NavController.** The four tabs map to:
  `StorageAnalyzerScreen`, `FileExplorerScreen`, `SecureVaultScreen`, `SettingsScreen` (all under
  `in.sreerajp.vault_files.ui`).

- **`StorageViewModel`** (`AndroidViewModel`, `in.sreerajp.vault_files.ui`) — the single source of truth for
  the entire app. Holds all UI state as `StateFlow`s (current directory, file listing, storage
  stats, secured folders, vault files, every settings toggle, and per-session unlock flags).
  Exposes user-facing one-shot messages via a `MutableSharedFlow<String>` (`userMessage`), which
  `MainActivity` collects to show Toast + Snackbar. All screens receive this one ViewModel.

- **`StorageRepository`** (`in.sreerajp.vault_files.data`) — all file I/O and persistence, every method on
  `Dispatchers.IO`. Two real on-disk roots under `filesDir`: `Storage/` (user-visible sandbox)
  and `Vault/` (moved files, renamed to opaque `vault_<uuid>.secured`). Also fabricates
  external-storage listings (`getFilesAndFoldersInDirectory`) and storage stats
  (`getStorageUsageStats`) by merging real files with virtual seeded entries.

- **Room** (`AppDatabase`, `DatabaseEntities.kt`) — DB `vault_files_database`, three tables:
  `app_settings` (generic key/value store — every preference is a row here, accessed through typed
  helpers on the repo), `secured_folders` (paths shielded behind auth), `vault_files` (metadata
  for vaulted files). Uses `fallbackToDestructiveMigration()`, so bumping the schema wipes data.

- **`VaultDocumentsProvider`** (`in.sreerajp.vault_files.data`) — a `DocumentsProvider` that
  publishes `filesDir/Storage` as a browsable root inside the system file picker (authority
  `${applicationId}.documents`, separate from the `.fileprovider` used for sharing). Read and
  write: list, open, image thumbnails, create, delete, rename. **`isExposable` hides `Vault/` and
  every secured folder and blocks path escapes, and is applied on every entry point** — SAF cannot
  run the app's unlock prompt, so protected content must never be reachable, even by a guessed
  document id. Callbacks run on binder threads, so everything here is synchronous (secured-folder
  paths are read with `runBlocking` and cached for 2s) and never touches the ViewModel.
  `StorageViewModel.loadFilesInDirectory` calls `notifyDirectoryChanged` so an open picker
  reloads after the app changes a folder.

  Related: the activity's `VIEW` intent filters are deliberately **narrow** (`image/*`, `text/*`,
  directories) — a `*/*` filter would offer the app as a viewer for every file type it cannot
  actually display. Folder browsing comes from the directory filter; "other apps can pick files
  from me" comes from `GET_CONTENT` / `OPEN_DOCUMENT` and this provider.

- **`utils/`** — `BiometricHelper` (wraps `BiometricPrompt`, BIOMETRIC_STRONG or DEVICE_CREDENTIAL,
  PIN fallback handled in UI) and `ZipUtility` (recursive zip/unzip with Zip-Slip path-traversal
  guard).

## Conventions worth following

- New persisted preferences: add a typed getter/flow/setter trio on `StorageRepository` backed by
  an `app_settings` key, expose a `StateFlow` on `StorageViewModel`, and sync it in the ViewModel
  `init` block — mirror the existing `theme_preference` / `show_hidden_items` patterns.
- UI actions go ViewModel method → repository suspend fn → `dispatchMessage(...)` for feedback →
  `loadFilesInDirectory(...)` / `refreshStorageStats()` to refresh. Don't do file I/O in Composables.
- Composables tag interactive elements with `Modifier.testTag(...)` (e.g. `nav_files_tab`,
  `app_lock_pin_input`) for instrumented/Robolectric tests — preserve these when editing UI.
- "Secured folder" and "hidden file" gating is **session-based**: unlock state lives in transient
  ViewModel flags (`_unlockedFolderSessions`, `_isHiddenUnlocked`, `_isVaultUnlocked`), not persisted.
