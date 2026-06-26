# Pull-to-refresh on the Files explorer

Implements plan `plans/20260625_213752_files-pull-to-refresh.md`.

## What changed

`app/src/main/java/com/example/ui/FileExplorerScreen.kt`:

- Added imports: `androidx.compose.material3.pulltorefresh.PullToRefreshBox` and
  `kotlinx.coroutines.delay`. (`verticalScroll`, `rememberScrollState`, and
  `rememberCoroutineScope` were already available.)
- Added screen-local pull-to-refresh state in `FileExplorerScreen`:
  - `isRefreshing` boolean + a `refreshScope` (`rememberCoroutineScope`).
  - An `onRefresh` lambda that sets `isRefreshing = true`, then reloads via
    `loadCategoryFilteredFiles()` when a category filter is active or
    `loadFilesInDirectory(currentDir)` otherwise, plus `refreshStorageStats()`, and
    clears `isRefreshing` after a 600 ms delay (the ViewModel load functions are
    fire-and-forget, so the screen owns the spinner timing).
  - Placed after `isCategoryFiltered` is declared, since the lambda references it.
- Wrapped all non-permission content branches (category-scanning state, empty state,
  and the LIST/GRID/COMPACT lists) in a single `PullToRefreshBox(isRefreshing,
  onRefresh)`. The "needs permission" branch stays outside it.
- Added `verticalScroll(rememberScrollState())` to the category-loading and empty-state
  Columns so the pull gesture registers on those non-scrollable states too.

No ViewModel, string, or resource changes were needed.

## Verification

`./gradlew compileDebugKotlin` succeeds (only unrelated JVM native-access warnings).
Manual check to perform on device: drag down on a populated folder, an empty folder, and
a category-filtered list — the spinner shows and the listing reloads.
