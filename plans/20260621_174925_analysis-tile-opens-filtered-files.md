# Plan: Tapping a Storage Analysis tile opens Files filtered to that type

## Issue / goal

In the **Storage Analysis** tab, the category tiles (Images, Videos, Audio,
Documents, Archives, Other) are purely informational — they show aggregated byte
totals but are not clickable. The user wants tapping a tile to jump to the
**Files** tab showing **all files of that type, listed recursively** across the
currently-selected storage source (sandbox or device).

### Key facts established from the code
- `CategoryStatTile` ([StorageAnalyzerScreen.kt:559]) currently has no click handler.
- Each tile is built from a `CategoryData` whose `title` is a display label
  ("Images", "Audio Tracks", "Other Formats"). The repository's canonical
  category strings are different: `"Image"`, `"Video"`, `"Audio"`, `"Document"`,
  `"Archive"`, `"Other"` (see `getCategoryForFile`, [StorageRepository.kt:157]).
  We must map tile → canonical category, not reuse the display title.
- The Files screen ([FileExplorerScreen.kt]) lists **one directory at a time**
  via `currentDirectoryFiles`; it has search + sort but no category filter and no
  recursive/flat mode.
- Tab switching is owned by `MainActivity` through `activeTabIndex`
  ([MainActivity.kt:123]). The Vault tile already uses this exact pattern via an
  `onOpenVault` callback that sets `activeTabIndex = 2`.
- `StorageStats` is already produced by a recursive scan, so a recursive
  collector for a single category is a natural addition to the repository.

## Approach

Mirror the existing `onOpenVault` wiring, and add a "category filter" mode to the
Files screen that shows a flat, recursive list overriding the normal directory
listing. The filter is cleared when the user dismisses it or navigates normally.

### Behavior details
- Tapping a tile sets a category filter in the ViewModel and switches to the
  Files tab.
- While a filter is active, Files shows a flat recursive list of every matching
  file under `userStorageRoot` for the current source, with a dismissible chip
  like **"Images · 42 files ✕"** at the top. Folder navigation / breadcrumb is
  hidden or disabled in this mode.
- Search and sort continue to work over the filtered list.
- Clearing the chip returns to the normal directory view at the storage root.
- Switching the storage source pill while filtered clears the filter (keeps
  behavior predictable, since source switch already resets the directory).
- The "Other" tile maps to the canonical `"Other"` category. Note: the tile's
  byte total for "Other" includes unscanned/unitemized partition space, so the
  recursive file list for "Other" will legitimately total less than the tile —
  this is expected and acceptable (the list only shows real files).

## Files to change

1. **`app/src/main/java/com/example/data/StorageRepository.kt`**
   - Add `suspend fun getFilesByCategoryRecursive(root: File, category: String, showHidden: Boolean): List<FileItem>`
     that walks the tree on `Dispatchers.IO`, includes only non-directory files
     whose `getCategoryForFile` equals the requested category, respects the
     hidden-files rule, and returns `FileItem`s (reusing the same construction as
     `getFilesAndFoldersInDirectory`, including `isSecured` evaluation).

2. **`app/src/main/java/com/example/ui/StorageViewModel.kt`**
   - Add `_activeCategoryFilter: MutableStateFlow<String?>` + public `activeCategoryFilter`.
   - Add `_categoryFilteredFiles: MutableStateFlow<List<FileItem>>` + public flow.
   - Add `fun openCategoryFilter(category: String)` — sets the filter and loads
     the recursive list for the current `userStorageRoot`.
   - Add `fun clearCategoryFilter()` — resets filter to null.
   - Add `fun loadCategoryFilteredFiles()` helper used on load/refresh.
   - Clear the filter inside `updateStorageSourceMode(...)`.

3. **`app/src/main/java/com/example/ui/StorageAnalyzerScreen.kt`**
   - Add a canonical-category field to `CategoryData` (e.g. `category: String`)
     and populate it for the 6 entries ("Image", "Video", "Audio", "Document",
     "Archive", "Other").
   - Add an `onOpenFilesWithCategory: (String) -> Unit = {}` param to
     `StorageAnalyzerScreen` and thread it down to `CategoryStatTile`.
   - Make `CategoryStatTile`'s root `Column` `.clickable { onClick() }`.

4. **`app/src/main/java/com/example/MainActivity.kt`**
   - Pass `onOpenFilesWithCategory = { category -> viewModel.openCategoryFilter(category); activeTabIndex = 1 }`
     to `StorageAnalyzerScreen`.

5. **`app/src/main/java/com/example/ui/FileExplorerScreen.kt`**
   - Collect `activeCategoryFilter` and `categoryFilteredFiles`.
   - When a filter is active:
     - Source the list from `categoryFilteredFiles` instead of
       `currentDirectoryFiles`.
     - Render a dismissible filter chip (label + count + ✕ that calls
       `viewModel.clearCategoryFilter()`).
     - Hide/disable the breadcrumb up-navigation and the folder-count meta line
       (or replace with "Showing all <Type> files").
   - Keep existing search + sort working over the filtered list.
   - `onItemClick` for a file in filter mode keeps current behavior (zip extract /
     "Viewing:" message); directories won't appear in the filtered list.

## Out of scope
- No real navigation library; we keep the integer-tab model.
- No change to how `StorageStats` totals are computed.
- No new permissions (recursive scan uses the same root already scanned for stats).

## Testing
- Build: `./gradlew assembleDebug`.
- Manual: tap each category tile in both Sandbox and Device sources; verify Files
  opens showing only that type recursively, the chip shows the right count,
  search/sort work, clearing the chip restores normal browsing, and switching the
  source pill clears the filter.

## Change log
- On completion, write `change_log/<ts>_analysis-tile-opens-filtered-files.md`
  referencing this plan.
