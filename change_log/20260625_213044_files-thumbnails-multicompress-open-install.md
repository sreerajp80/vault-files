# Change log — Files tab: image thumbnails, multi-select compress, open-with, APK install

Date: 2026-06-25 21:30 (local)

Implements plan `plans/20260625_212108_files-thumbnails-multicompress-open-install.md`
(approved; multi-folder compression explicitly confirmed). Four tasks delivered.

## Task 1 — Image thumbnails as the file icon (List & Compact views)
- `FileExplorerScreen.kt`: added a `showThumbnail` branch (mirroring the Grid view) to the icon
  `Box` of `FileRowItem` (44dp) and `FileCompactRow` (30dp). Image files now render a Coil
  `SubcomposeAsyncImage` thumbnail clipped to the rounded icon box, falling back to the category
  icon while loading or on decode failure. Grid view was already thumbnailed.

## Task 2 — Compress multiple selected items
- `ZipUtility.kt`: added `zipMultiple(sources, destZipFile)` — writes each source into one archive
  at the root under its own name (directories nested under `name/…`).
- `StorageRepository.kt`: added `compressMultiple(sources, targetZipName)` — resolves the
  destination beside the first source and delegates to `ZipUtility.zipMultiple`.
- `StorageViewModel.kt`: added `compressItems(items, zipName)` (success message
  `msg_zip_success_multi`, reloads dir, refreshes stats).
- `FileExplorerScreen.kt`:
  - `SelectionToolbar` now shows **Compress** for any selection except a lone `.zip` (which still
    shows **Extract**); **Details** remains single-selection only.
  - Compress dialog state generalized from `showZipDialogForFile: FileItem?` to
    `showZipDialogForItems: List<FileItem>?`. Single item keeps the existing
    `compressFolderOrFile` path and `name.zip` default; multiple items default to `archive.zip`
    and call `compressItems`.
  - `onCompress` now passes the whole selection.

## Task 3 — Double-tap a file to open it externally
- `AndroidManifest.xml`: declared a `FileProvider` (authority `${applicationId}.fileprovider`).
- `res/xml/file_paths.xml` (new): exposes internal, external, and root paths so both sandbox and
  device-mode files can be shared.
- `FileExplorerScreen.kt`: added `openFileExternally(context, item, viewModel)` — builds a
  FileProvider `content://` URI, resolves MIME via `MimeTypeMap`, and fires `ACTION_VIEW` (opens
  the default app or the system chooser). Wired as `onDoubleClick` on file rows/tiles/compact rows
  (folders unaffected; disabled in selection mode). On `ActivityNotFoundException` shows
  `msg_no_app_to_open`.

## Task 4 — Double-tap an APK to install/update
- `AndroidManifest.xml`: added `REQUEST_INSTALL_PACKAGES` permission.
- `openFileExternally` special-cases `.apk`: on Android O+ checks
  `canRequestPackageInstalls()`; if not granted it shows `msg_install_permission_needed` and opens
  `ACTION_MANAGE_UNKNOWN_APP_SOURCES` for this package. Once allowed it launches the system package
  installer via `ACTION_VIEW` + `application/vnd.android.package-archive`, handling install/update.

## Strings
- `values/strings.xml` + `values-ml/strings.xml`: added `msg_zip_success_multi`,
  `msg_no_app_to_open`, `msg_open_failed`, `msg_install_permission_needed`, `msg_install_failed`.

## Verification
- `./gradlew assembleDebug` succeeded; `app/build/outputs/apk/debug/app-debug.apk` produced.
- No tests referenced the changed composable signatures or the renamed dialog state.

## Notes
- Adding `onDoubleClick` introduces the standard Compose tap-disambiguation delay on single taps
  for files — expected for the requested gesture.
- The install-sources permission can only be granted by the user via the settings screen; the app
  routes there and asks the user to re-tap the APK.
