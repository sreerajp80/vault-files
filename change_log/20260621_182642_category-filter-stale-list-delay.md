# Change log: fix stale file list / delay when opening a category filter

Implements plan `plans/20260621_181719_category-filter-stale-list-delay.md`.

## Problem

Tapping a category tile (e.g. "Videos") in Storage Analysis flipped the Files filter chip to the
new category instantly, but the file list kept showing the previous category's results (e.g.
"Others") until the async recursive scan finished — looking like a delay/mismatch.

## Changes

### `app/src/main/java/com/example/ui/StorageViewModel.kt`
- Added `_isCategoryLoading` / public `isCategoryLoading` StateFlow.
- `openCategoryFilter`: now clears `_categoryFilteredFiles` to `emptyList()` immediately on
  filter change, dropping the stale list before the new scan starts.
- `loadCategoryFilteredFiles`: sets `_isCategoryLoading = true` before the scan and resets it to
  `false` in a `finally` block after results are assigned.
- `clearCategoryFilter`: also resets `_isCategoryLoading = false`.

### `app/src/main/java/com/example/ui/FileExplorerScreen.kt`
- Collects `isCategoryLoading`.
- Added a loading branch in the content area (before the empty-state check): when
  `isCategoryFiltered && isCategoryLoading`, shows a centered `CircularProgressIndicator` with a
  "Scanning <category>…" caption (testTag `category_filter_loading_state`) instead of the stale
  or empty list.

## Notes

- Does not change `getFilesByCategoryRecursive` or speed up the scan itself; it removes the wrong
  list shown during the scan and replaces the gap with a proper loading state.
- Verified with `./gradlew compileDebugKotlin` (compiles cleanly).
