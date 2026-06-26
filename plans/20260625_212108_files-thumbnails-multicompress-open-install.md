# Plan — Files tab: image thumbnails, multi-select compress, open-with, APK install

Date: 2026-06-25 21:21 (local)

Four independent tasks against the Files explorer. Each is small but they touch overlapping
files (`FileExplorerScreen.kt` mainly), so they are planned together.

---

## Task 1 — Show small image thumbnails as the file icon (List & Compact views)

### Issue
Only the **Grid** view (`FileGridItem`) renders a real Coil thumbnail for image files
(`showThumbnail` → `SubcomposeAsyncImage`). The **List** (`FileRowItem`) and **Compact**
(`FileCompactRow`) views always draw the generic `Icons.Default.Image` vector for images.

### Fix
In `FileRowItem` and `FileCompactRow`, inside the icon `Box`, add the same thumbnail branch the
grid already uses: when `!isSecuredLocked && !item.isDirectory && item.category == "Image"`, render
`SubcomposeAsyncImage(model = ImageRequest…data(item.file)…)` clipped to the icon box's rounded
shape, `ContentScale.Crop`, with the category icon as the `loading`/`error` slot. APK and other
branches stay as-is. Thumbnail fills the existing 44dp (list) / 30dp (compact) box.

### Files
- `app/src/main/java/com/example/ui/FileExplorerScreen.kt`

---

## Task 2 — Compression hidden when multiple folders/items selected

### Issue
In `SelectionToolbar`, the Compress (and Extract) icon buttons are nested inside
`if (count == 1)`, so they disappear the moment a second item is selected. The
`onCompress` handler also uses `selectedItems.singleOrNull()`, and the ZIP pipeline
(`viewModel.compressFolderOrFile` → `repository.compressFileOrFolder` → `ZipUtility.zip`) only
accepts a single source. Result: no way to compress a multi-selection.

### Fix
Support compressing any selection (one or many) into a single archive.

- **`ZipUtility.kt`**: add `fun zipMultiple(sources: List<File>, destZipFile: File): Boolean`
  that opens one `ZipOutputStream` and writes each source under its own top-level name
  (file → `name`; directory → recurse with entries prefixed by `dir.name/…`). Reuses the existing
  `zipFile`/recursion helpers.
- **`StorageRepository.kt`**: add
  `suspend fun compressMultiple(sources: List<File>, targetZipName: String): Boolean` — resolves
  the destination in the parent of the first source (fallback `userStorageRoot`), appends `.zip`
  if missing, calls `ZipUtility.zipMultiple`.
- **`StorageViewModel.kt`**: add `fun compressItems(items: List<FileItem>, zipName: String)` that
  calls `repository.compressMultiple`, dispatches a success/fail message, reloads the directory and
  refreshes stats (mirrors `compressFolderOrFile`).
- **`FileExplorerScreen.kt`**:
  - `SelectionToolbar`: show the **Compress** button whenever `count > 1`, or
    `count == 1 && !isSingleZip` (single-zip still shows **Extract** instead). **Details** and
    **Extract** remain single-selection only.
  - Generalize the compress dialog state from `showZipDialogForFile: FileItem?` to
    `showZipDialogForItems: List<FileItem>?`. Default name = single → `name + ".zip"`,
    multi → `"archive.zip"`. On confirm: size == 1 → existing `compressFolderOrFile(item, name)`
    (preserves current single-item behavior & test tags `zip_name_field`); size > 1 →
    `compressItems(items, name)`.
  - `onCompress` sets `showZipDialogForItems = selectedItems` (instead of `singleOrNull`).

### Files
- `app/src/main/java/com/example/utils/ZipUtility.kt`
- `app/src/main/java/com/example/data/StorageRepository.kt`
- `app/src/main/java/com/example/ui/StorageViewModel.kt`
- `app/src/main/java/com/example/ui/FileExplorerScreen.kt`
- `app/src/main/res/values/strings.xml` (+ `values-ml/strings.xml`): e.g.
  `msg_zip_success_multi` ("Compressed %1$d items into %2$s").

---

## Task 3 — Double-tap a file to open it with the default / chooser app

### Issue
There is no "open externally" path. Single tap only previews (image/text), opens notes,
extracts zips, or shows a toast. The app also has **no `FileProvider`**, so it cannot hand a
`content://` URI to another app.

### Fix
Add a `FileProvider` and a double-tap gesture that opens a file with an external app.

- **`AndroidManifest.xml`**: declare a `FileProvider`
  (`androidx.core.content.FileProvider`, authority `${applicationId}.fileprovider`,
  `grantUriPermissions=true`, exported=false, meta-data → `@xml/file_paths`).
- **New `app/src/main/res/xml/file_paths.xml`**: `root-path`, `files-path`, `external-path`,
  `external-files-path`, `cache-path` each mapping `path="."` so both sandbox (internal
  `filesDir/Storage`) and device-mode (external storage) files can be shared.
- **`FileExplorerScreen.kt`**: add a top-level helper
  `openFileExternally(context, item, viewModel)`:
  - Build `uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", item.file)`.
  - Resolve MIME from extension via `MimeTypeMap` (fallback `*/*`).
  - `Intent(ACTION_VIEW).setDataAndType(uri, mime).addFlags(FLAG_GRANT_READ_URI_PERMISSION)`.
  - `startActivity` directly (the OS opens the user's default app, or shows the disambiguation
    chooser when none is set — exactly the requested behavior). On `ActivityNotFoundException`,
    `viewModel.dispatchMessage(...)` ("No app found to open …").
  - Wire as `onDoubleClick` in the `combinedClickable` of `FileRowItem`, `FileGridItem`,
    `FileCompactRow`, only for `!item.isDirectory` (folders keep single-tap navigation, no
    double-tap action). Plumbed via a new `onOpenExternal: () -> Unit` parameter set from
    `FileExplorerScreen` where `LocalContext`/`viewModel` are available.
  - Note: adding `onDoubleClick` introduces a small tap-delay on `onClick` (Compose waits to
    disambiguate) — accepted as this is the requested gesture.

### Files
- `app/src/main/AndroidManifest.xml`
- `app/src/main/res/xml/file_paths.xml` (new)
- `app/src/main/java/com/example/ui/FileExplorerScreen.kt`
- `app/src/main/res/values/strings.xml` (+ `values-ml/strings.xml`): `msg_no_app_to_open`.

---

## Task 4 — Double-tap an APK to install/update; request & set install permission

### Issue
APKs currently just show their icon and a toast. There is no install flow and no
`REQUEST_INSTALL_PACKAGES` permission.

### Fix
Make the same `openFileExternally` helper special-case APKs (`.apk`/`.apks`/`.xapk` or
package-archive MIME):
- **`AndroidManifest.xml`**: add `<uses-permission android:name="android.permission.REQUEST_INSTALL_PACKAGES" />`.
- In the helper, for APKs:
  - On Android O+ check `context.packageManager.canRequestPackageInstalls()`. If **false**,
    dispatch a message and launch
    `Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES, Uri.parse("package:" + packageName))`
    so the user can grant "install unknown apps" for this app, then return (the user re-taps
    after granting). This is the only supported way to "set" the permission.
  - If allowed, build `Intent(ACTION_VIEW)` with the FileProvider URI and MIME
    `application/vnd.android.package-archive`, `FLAG_GRANT_READ_URI_PERMISSION`, and
    `startActivity` → the system package installer handles install **or update**.
  - `ActivityNotFoundException`/`Exception` → dispatch a failure message.

### Files
- `app/src/main/AndroidManifest.xml`
- `app/src/main/java/com/example/ui/FileExplorerScreen.kt`
- `app/src/main/res/values/strings.xml` (+ `values-ml/strings.xml`):
  `msg_install_permission_needed`, `msg_install_failed`.

---

## Verification
- `./gradlew assembleDebug` (or `installDebug`) to confirm it builds.
- Manual: thumbnails appear in list/compact; multi-select shows Compress and produces one zip;
  double-tap a non-APK opens default app / chooser; double-tap an APK prompts for the
  install-sources permission then launches the installer.

## Notes / caveats
- Sandbox-mode files live under `filesDir/Storage` (app-private). Sharing them out via
  FileProvider works for opening/installing. Device mode uses real external paths.
- A `debug.keystore` at project root is required to build the debug variant (see `docs/build.md`).
- A change-log entry will be written to `change_log/` after implementation.
