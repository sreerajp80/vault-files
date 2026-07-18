# Change log — Receive shared files (appear in the Android share sheet)

Implements plan `plans/20260710_072401_receive-shared-files.md`.

## What changed and why

Before this change, "Vault Files" never appeared in the Android share sheet, so a
file shared or exported from another app could not be sent into it. The app's only
activity declared just a `MAIN`/`LAUNCHER` intent-filter. The app is now a share
target: when a file is shared in, it asks each time whether to save it to the
encrypted vault or a folder the user picks.

## Files changed

- **`app/src/main/AndroidManifest.xml`**
  - Added two intent-filters to `MainActivity`: `ACTION_SEND` and
    `ACTION_SEND_MULTIPLE`, both `category.DEFAULT` with `mimeType="*/*"`. This is
    what makes the app show up in the share sheet for any file type.

- **`app/src/main/java/com/example/data/StorageRepository.kt`**
  - Added `importUriToFolder(uri, displayName, destDir)` — copies bytes from a shared
    content `Uri` into a folder, with `uniqueDestFile(...)` collision-safe naming
    (`name (1).ext`, …).
  - Added `importUriToVault(uri, displayName)` — copies a shared `Uri` straight into
    the encrypted vault and records a `VaultFile` row.
  - Added the private helper `uniqueDestFile(...)`; imported `android.net.Uri`.

- **`app/src/main/java/com/example/ui/StorageViewModel.kt`**
  - Added the `SharedImport(uri, name)` data class.
  - Added `pendingSharedImports` state plus `setPendingSharedImports(...)` and
    `clearPendingSharedImports()`.
  - Added `importSharedToFolder(imports, destDir)` and `importSharedToVault(imports)`,
    which call the new repository functions, show a result message, then refresh.
  - Imported `android.net.Uri`.

- **`app/src/main/java/com/example/MainActivity.kt`**
  - `onCreate` now calls `handleShareIntent(...)`; added `onNewIntent(...)` so a share
    delivered to the already-running app is handled too.
  - Added `handleShareIntent(...)` (parses `EXTRA_STREAM` for single/multiple via
    `IntentCompat`) and `resolveDisplayName(...)` (reads `OpenableColumns.DISPLAY_NAME`
    with fallbacks).
  - In the unlocked UI branch, shows `ShareDestinationDialog` (Vault / Choose a
    folder), and on "Choose a folder" opens the shared `FolderPickerDialog`. The
    pending files are held in the ViewModel so the choice waits past the app-lock gate.
  - Added the `ShareDestinationDialog` composable; added imports for `IntentCompat`,
    `OpenableColumns`, `FolderPickerDialog`, and `SharedImport`.

- **`app/src/main/java/com/example/ui/FileExplorerScreen.kt`**
  - Extracted the folder-browsing UI from `MoveCopyPickerDialog` into a reusable
    `FolderPickerDialog(title, confirmLabel, …)`. `MoveCopyPickerDialog` now delegates
    to it, so move/copy and the new share-import flow use one picker. No behaviour
    change to the existing move/copy path.

- **`app/src/main/res/values/strings.xml`**
  - Added strings for the destination dialog, the folder picker title/confirm, and the
    save result/failure messages.

## Out of scope (unchanged)

- The app still does not appear inside the system file picker ("Save to…") as a
  browsable location; that needs a `DocumentsProvider` and is separate work.

## Verification

- `./gradlew assembleDebug` → **BUILD SUCCESSFUL**. The only warning is a pre-existing
  `Icons.Filled.OpenInNew` deprecation unrelated to this change.
- Manual on-device testing (share a file → confirm the app appears → save to vault and
  to a folder) is still recommended.
