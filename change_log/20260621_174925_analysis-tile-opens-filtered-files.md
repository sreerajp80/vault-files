# Change log: Storage Analysis tiles open a recursive type-filtered Files view

Implements plan `plans/20260621_174925_analysis-tile-opens-filtered-files.md`.

## What changed

Tapping a category tile (Images, Videos, Audio, Documents, Archives, Other
Formats) in the **Storage Analysis** tab now jumps to the **Files** tab and shows
a flat, recursive list of every file of that type across the currently-selected
storage source (App Sandbox or Entire Device). A dismissible chip at the top of
the Files screen indicates the active filter and its file count; clearing it (or
switching the storage source) returns to normal folder browsing.

## Files changed

1. **`app/src/main/java/com/example/data/StorageRepository.kt`**
   - Added `getFilesByCategoryRecursive(root, category, showHidden)` — walks the
     tree on `Dispatchers.IO`, collects only non-directory files whose
     `getCategoryForFile` matches the requested canonical category, honors the
     hidden-file rule, and evaluates `isSecured` like the directory listing does.

2. **`app/src/main/java/com/example/ui/StorageViewModel.kt`**
   - Added `activeCategoryFilter` (`StateFlow<String?>`) and
     `categoryFilteredFiles` (`StateFlow<List<FileItem>>`).
   - Added `openCategoryFilter(category)`, `clearCategoryFilter()`, and
     `loadCategoryFilteredFiles()`.
   - `updateStorageSourceMode(...)` now clears the active filter, since the
     filtered list is tied to the previous source's tree.

3. **`app/src/main/java/com/example/ui/StorageAnalyzerScreen.kt`**
   - Added a canonical `category` field to `CategoryData` and populated it for all
     six tiles ("Image", "Video", "Audio", "Document", "Archive", "Other").
   - Added an `onOpenFilesWithCategory: (String) -> Unit` param to the screen and
     an `onClick` to `CategoryStatTile`; the tile root is now `.clickable`.

4. **`app/src/main/java/com/example/MainActivity.kt`**
   - Wired `onOpenFilesWithCategory = { category -> viewModel.openCategoryFilter(category); activeTabIndex = 1 }`
     into `StorageAnalyzerScreen` (mirrors the existing `onOpenVault` pattern).

5. **`app/src/main/java/com/example/ui/FileExplorerScreen.kt`**
   - Collects `activeCategoryFilter` / `categoryFilteredFiles`; sources the list
     from the filtered collection when a filter is active.
   - Replaces the breadcrumb with a dismissible filter chip
     (`files_category_filter_chip`) showing label + count.
   - Filter-aware section header ("ALL IMAGES", etc.) and empty state.
   - Reloads the filtered list on `ON_RESUME`.
   - Added `categoryDisplayLabel(category)` helper.
   - Search and sort continue to operate over the filtered list.

## Verification

- `./gradlew assembleDebug` — **BUILD SUCCESSFUL**.
- Manual testing of tile taps / chip dismissal / source switching not yet run on
  a device.

## Notes

- The "Other" tile's byte total includes unscanned/unitemized partition space, so
  its recursive file list will legitimately total less than the tile figure — the
  list only contains real files. This is expected, per the plan.
