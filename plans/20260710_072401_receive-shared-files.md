# Receive shared files (appear in the Android share sheet)

**Status:** completed

## What the issue is

When the user shares or exports a file from another app, "Vault Files" does not
appear in the Android share sheet (the screen that says "Sharing 1 file"). So the
user cannot send a file into the app to store it in a folder.

The cause is in `app/src/main/AndroidManifest.xml`. `MainActivity` declares only a
`MAIN` / `LAUNCHER` intent-filter. Android only lists an app in the share sheet if
that app declares it can *receive* a sent file — that is, an intent-filter for
`android.intent.action.SEND` (and `SEND_MULTIPLE` for many files). The app declares
none, so the system leaves it out.

## The plan for the fix

Make the app a share target. When a file is shared in, ask the user each time where
to save it: **the encrypted Vault** or **a folder they pick** (per the chosen
behaviour "Both, ask each time").

Incoming shared data arrives as a content `Uri`, not a real `File`. The existing
copy/move/vault functions all take a `File`, so we add new functions that read from
a `Uri` input stream.

### 1. `app/src/main/AndroidManifest.xml`
- Add two intent-filters to the existing `MainActivity` activity (it is already
  `exported="true"`):
  - `ACTION_SEND` with `category.DEFAULT` and `mimeType = "*/*"`.
  - `ACTION_SEND_MULTIPLE` with `category.DEFAULT` and `mimeType = "*/*"`.

### 2. `app/src/main/java/com/example/data/StorageRepository.kt`
- Add `importUriToFolder(uri: Uri, displayName: String, destDir: File): Boolean`
  — opens `context.contentResolver.openInputStream(uri)` and copies the bytes into
  `File(destDir, displayName)`. If the name already exists, add a numeric suffix
  (e.g. `name (1).ext`) so we never overwrite. Runs on `Dispatchers.IO`.
- Add `importUriToVault(uri: Uri, displayName: String): Boolean` — copies the stream
  into `vaultStorageRoot` as `vault_<UUID>.secured` and inserts a `VaultFile` row
  (like `moveFileToVault`, but the source is a `Uri` and there is no original file to
  delete). File size is taken from the bytes actually written.

### 3. `app/src/main/java/com/example/ui/StorageViewModel.kt`
- Add `importSharedToFolder(uris: List<Uri>, names: List<String>, destDir: File)` —
  loops the URIs, calls `importUriToFolder`, counts successes, emits a result message,
  then refreshes the current directory + storage stats.
- Add `importSharedToVault(uris: List<Uri>, names: List<String>)` — same pattern with
  `importUriToVault`.
- Both use the existing `dispatchMessage(...)` / message flow.

### 4. `app/src/main/java/com/example/MainActivity.kt`
- Read the launch intent in `onCreate`, and also override `onNewIntent` (the app is
  single-activity, so an already-running instance receives shares there). Detect
  `ACTION_SEND` / `ACTION_SEND_MULTIPLE`, pull the `Uri`(s) from `EXTRA_STREAM`
  (single) or `ClipData` / the parcelable list (multiple).
- Resolve each URI's display name via an `OpenableColumns.DISPLAY_NAME` query, with a
  sensible fallback name when none is provided.
- Hold the pending shared URIs in Compose state. Because the app can be lock-gated,
  only act on them **after** the existing unlock gate is passed (reuse `isAppLocked`).
- Show a small **destination dialog**: "Save shared file(s) to?" with two choices —
  *Encrypted Vault* and *Choose a folder…*.
  - Vault → call `viewModel.importSharedToVault(...)`, then clear pending state.
  - Choose a folder → show a folder picker, then call
    `viewModel.importSharedToFolder(..., destDir)`.

### 5. Folder picker reuse
- Reuse the existing folder-browsing UI from
  `FileExplorerScreen.kt` (`MoveCopyPickerDialog`, lines ~2754+). It is currently tied
  to a `MoveCopyRequest`. Extract its inner folder-browser into a small reusable
  composable (e.g. `FolderBrowser`/`FolderPickerDialog`) that both the existing
  move/copy flow and the new share-import flow call, so there is one picker, not two.
  If extraction proves messy, add a minimal dedicated picker dialog for the import
  flow instead — decided during implementation, no behaviour change to the existing
  move/copy path.

### 6. `app/src/main/res/values/strings.xml`
- Add strings for: the destination dialog title, the "Vault" and "Choose a folder"
  buttons, and result messages (e.g. "Saved N file(s) to <folder>", "Saved N file(s)
  to Vault", and the failure cases).

## Out of scope
- Making the app appear inside the system **file picker / "Save to…"** as a browsable
  location. That needs a `DocumentsProvider` (Storage Access Framework) and is a much
  larger, separate piece of work.

## Testing
- Build: `./gradlew assembleDebug`.
- Manual: from Gallery/Files/another app, Share a file → confirm "Vault Files" now
  appears → pick Vault and pick a folder in two runs → confirm the file lands in the
  right place and the success message shows.
