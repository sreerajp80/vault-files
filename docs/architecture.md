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
  `com.example.ui`).

- **`StorageViewModel`** (`AndroidViewModel`, `com.example.ui`) — the single source of truth for
  the entire app. Holds all UI state as `StateFlow`s (current directory, file listing, storage
  stats, secured folders, vault files, every settings toggle, and per-session unlock flags).
  Exposes user-facing one-shot messages via a `MutableSharedFlow<String>` (`userMessage`), which
  `MainActivity` collects to show Toast + Snackbar. All screens receive this one ViewModel.

- **`StorageRepository`** (`com.example.data`) — all file I/O and persistence, every method on
  `Dispatchers.IO`. Two real on-disk roots under `filesDir`: `Storage/` (user-visible sandbox)
  and `Vault/` (moved files, renamed to opaque `vault_<uuid>.secured`). Also fabricates
  external-storage listings (`getFilesAndFoldersInDirectory`) and storage stats
  (`getStorageUsageStats`) by merging real files with virtual seeded entries.

- **Room** (`AppDatabase`, `DatabaseEntities.kt`) — DB `vault_files_database`, three tables:
  `app_settings` (generic key/value store — every preference is a row here, accessed through typed
  helpers on the repo), `secured_folders` (paths shielded behind auth), `vault_files` (metadata
  for vaulted files). Uses `fallbackToDestructiveMigration()`, so bumping the schema wipes data.

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
