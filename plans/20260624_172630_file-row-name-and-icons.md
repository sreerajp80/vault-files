# Plan: Show full file names (start + end) and proper file icons in the File Explorer

## The issue

In the Files tab, file rows have two problems (see user screenshots):

1. **File names are cut off.** Long names like `brahma_muhurta-v1.5.3_1...` lose their tail
   (version + extension). Two causes:
   - `FileRowItem` renders the file **size in its own column on the right** of the row, which
     steals horizontal space from the name.
   - The name `Text` uses end-only ellipsis (`TextOverflow.Ellipsis`), so only the start is ever
     visible. The user wants **both the start and the end** of the name visible (middle ellipsis,
     like the reference file manager).
   - `TextOverflow.MiddleEllipsis` is only in Compose 1.8+, but this project is on Compose BOM
     2024.09.00 (1.7.x), so it is not available — a manual approach is required.

2. **Icons are not proper for files.** `getIconForFileCategory` maps APKs to the generic
   "Other" → `InsertDriveFile` icon. The reference app shows the **actual app icon extracted from
   each APK**. Generic categories otherwise look fine but APKs (a common case here) look wrong.

The user explicitly said: keep the tile layout, just make the start+end of names visible and fix
the icons.

## Files to change

- `app/src/main/java/com/example/ui/FileExplorerScreen.kt`
  - `FileRowItem` — layout + name rendering + icon rendering.
  - `getIconForFileCategory` — add an APK fallback icon.
  - Add a small `MiddleEllipsisText` helper composable.
  - Add a small `ApkIcon` composable that loads the APK's launcher icon.

(No repository/category changes needed — APK detection is by extension in the UI. No new
dependencies: Coil and `androidx.core-ktx`'s `Drawable.toBitmap()` are already available.)

## The fix

### 1. Names: start + end visible (middle ellipsis)

Replace the single name `Text` with a `MiddleEllipsisText` helper that guarantees the tail stays
visible regardless of width. Implementation (version-safe, no Compose 1.8 dependency):

- Render a `Row` containing two `Text`s:
  - **Head**: `name.dropLast(tail)` with `Modifier.weight(1f, fill = false)`, `maxLines = 1`,
    `TextOverflow.Ellipsis`, `softWrap = false`.
  - **Tail**: the last N characters (default ~14, but never more than the name length; for short
    names the whole name is the head and the tail is empty), `maxLines = 1`, `softWrap = false`,
    so it is never truncated.
- When the name is short enough to fit, nothing is ellipsized; when long, the user sees
  `start…end` (e.g. `brahma_muhurta-v…3_1-release.apk`).
- Keep the existing bold style / font size / color and the inline shield icon next to the name.

### 2. Give the name the full row width

Move the file size off the right-hand column and onto the existing second (meta) line, matching
the reference layout:

- **Files**: meta line = `date · size` (e.g. `23 Jul 2023 · 10.2 MB`).
- **Directories**: meta line = `date · N items`, plus `· size` once computed; while the size is
  still computing show the existing small spinner **inline at the end of the meta line** instead of
  in the right column.
- Remove the standalone right-side size `Text` / spinner block so the only thing on the right is
  the `MoreVert` (kebab) action button — exactly like the reference.

### 3. Icons: real APK icons + better fallback

- Add an `ApkIcon` composable:
  - Detect APKs by extension (`apk`) in `FileRowItem`.
  - Load the launcher icon off the main thread with `produceState(item.absolutePath)`:
    `packageManager.getPackageArchiveInfo(path, 0)`, set `applicationInfo.sourceDir` /
    `publicSourceDir = path`, then `loadIcon(pm)` → `Drawable.toBitmap().asImageBitmap()`.
  - While loading or if extraction fails (e.g. no read access / corrupt APK), fall back to a vector
    `Icons.Default.Android` icon so it still looks like an app, not a generic file.
  - Render the extracted bitmap with `Image` filling the existing 44.dp rounded icon container.
- `getIconForFileCategory`: add `apk` (and `xapk`, `apks`) → `Icons.Default.Android` as the vector
  fallback for the non-APK-bitmap path and for any place that uses the vector.
- The locked-folder and directory icon behavior is unchanged.

## Out of scope / notes

- No change to categorization, stats, filtering, or the Room schema.
- Reading APK icons only works for files the app can actually read (sandbox files, or device files
  once All-Files access is granted); the `Android` fallback covers the rest.
- Will build with `./gradlew assembleDebug` after implementing.
