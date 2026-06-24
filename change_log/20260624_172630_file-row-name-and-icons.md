# Change log: File row — full names (start + end) and proper icons

Implements `plans/20260624_172630_file-row-name-and-icons.md`. Approved by the user.

## What changed

All edits are in `app/src/main/java/com/example/ui/FileExplorerScreen.kt`.

### Names now show start + end (middle ellipsis)
- Added a `MiddleEllipsisText` composable: a head `Text` (end-ellipsized, weighted) plus a
  fixed-length, never-truncated tail, so long names render as `start…end` (e.g.
  `brahma_muhurta-v…3_1-release.apk`). This avoids needing Compose 1.8's `TextOverflow.MiddleEllipsis`
  (project is on Compose BOM 2024.09.00 / 1.7.x).
- `FileRowItem` now uses `MiddleEllipsisText` for the file name.

### Name gets the full row width
- Removed the standalone right-hand size `Text` / spinner column. The right side of each tile now
  has only the kebab (`MoreVert`) action button, matching the reference file manager.
- File size moved onto the second (meta) line:
  - Files: `date · size`.
  - Directories: `date · N items`, plus `· size` once computed; while the size is still being
    summed a small inline spinner is shown at the end of the meta row.

### Proper icons
- Added an `ApkIcon` composable that extracts the real launcher icon from each APK off the main
  thread (`PackageManager.getPackageArchiveInfo` → set `sourceDir`/`publicSourceDir` → `loadIcon` →
  `Drawable.toBitmap().asImageBitmap()`), rendered with `Image` in the existing 44.dp icon
  container. Falls back to `Icons.Default.Android` while loading or if the APK can't be read.
- `FileRowItem` detects APKs by extension and uses `ApkIcon` for them.
- `getIconForFileCategory` now returns `Icons.Default.Android` for `apk` / `apks` / `xapk` as the
  vector fallback.

### Imports added
`Image`, `ImageBitmap`, `asImageBitmap`, `androidx.core.graphics.drawable.toBitmap`,
`kotlinx.coroutines.Dispatchers`, `kotlinx.coroutines.withContext`.

## Verification
- `./gradlew assembleDebug` → BUILD SUCCESSFUL.

## Notes
- No dependency, categorization, stats, filtering, or Room schema changes.
- APK icon extraction only works for readable files (sandbox, or device files once All-Files access
  is granted); the Android fallback covers the rest.
