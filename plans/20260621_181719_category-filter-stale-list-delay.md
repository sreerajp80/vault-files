# Fix: stale file list / delay when opening a category filter from Storage Analysis

## The issue

When the user taps a category tile (e.g. "Videos") in Storage Analysis, the Files screen
opens with the filter chip updated to **"Videos"** immediately, but the file list below still
shows the **previous** category's files (e.g. "Others") for a noticeable moment before the
correct video files appear.

### Root cause

`StorageViewModel.openCategoryFilter(category)` (lines 154-157):

```kotlin
fun openCategoryFilter(category: String) {
    _activeCategoryFilter.value = category   // synchronous -> chip updates instantly
    loadCategoryFilteredFiles()              // async recursive disk walk -> slow
}
```

- `_activeCategoryFilter` is updated **synchronously**, so the chip label and
  `isCategoryFiltered` flip to the new category instantly.
- `loadCategoryFilteredFiles()` launches a coroutine that runs
  `repository.getFilesByCategoryRecursive(...)` — a full recursive walk of the storage tree
  on `Dispatchers.IO`. This takes time.
- Meanwhile `_categoryFilteredFiles` **still holds the previous category's results**. In
  `FileExplorerScreen`, `baseList = categoryFilteredFiles` when filtered, so the screen renders
  the new chip ("Videos") over the stale old list ("Others") until the walk completes.

So the perceived bug is a stale list shown during the (unavoidable) async scan, with no loading
indication.

## Fix

Clear the stale list immediately on filter change and show a loading spinner while the recursive
scan runs, so the user never sees the previous category's files under the new chip.

### Files to change

1. **`app/src/main/java/com/example/ui/StorageViewModel.kt`**
   - Add `_isCategoryLoading` (`MutableStateFlow<Boolean>`) + public `isCategoryLoading`.
   - `openCategoryFilter`: set filter, immediately clear `_categoryFilteredFiles.value =
     emptyList()` (drop stale list), then call `loadCategoryFilteredFiles()`.
   - `loadCategoryFilteredFiles`: set `_isCategoryLoading.value = true` before the load and
     reset to `false` in a `finally` after assigning results.
   - `clearCategoryFilter`: also reset `_isCategoryLoading.value = false`.

2. **`app/src/main/java/com/example/ui/FileExplorerScreen.kt`**
   - Collect `isCategoryLoading` via `collectAsState()`.
   - In the content-area branch (currently `needsPermission` / `displayedFiles.isEmpty()` /
     `LazyColumn`), add a loading branch: when `isCategoryFiltered && isCategoryLoading`, show a
     centered `CircularProgressIndicator` instead of the stale/empty list. This branch goes
     before the `displayedFiles.isEmpty()` check so the empty "No videos found" state doesn't
     flash during the scan.

### Notes / scope

- This does not make the recursive scan itself faster; it removes the *wrong* list being shown
  and replaces the gap with a proper loading state. The scan cost is inherent to the simulated
  storage model.
- No change to `getFilesByCategoryRecursive` behavior.
- No new strings beyond a loading spinner (no caption needed, but can add "Scanning…" text).
