# Pull-to-refresh on the Files explorer

## Issue / request

On the Files explorer screen the only ways to refresh the current folder listing are
navigating away and back, or returning the app from background (the `ON_RESUME`
observer). There is no gesture to manually re-read the current directory. The user
wants: **drag the list down → the current screen/folder refreshes.**

## Approach

Wrap the content list in Material3's `PullToRefreshBox` (stable in material3 1.3.0,
which is what Compose BOM `2024.09.00` resolves to). The screen already opts into
`ExperimentalMaterial3Api`, so no new opt-in is needed.

On refresh:
- If a category filter is active → `viewModel.loadCategoryFilteredFiles()`.
- Otherwise → `viewModel.loadFilesInDirectory(currentDir)`.
- Always `viewModel.refreshStorageStats()` (kept in sync with the other refresh paths).

The existing `loadFilesInDirectory` / `loadCategoryFilteredFiles` are fire-and-forget
(they launch their own jobs), so the screen drives a local `isRefreshing` flag for the
spinner: set it true on pull, then clear it after a short delay via a remembered
coroutine scope. This keeps the indicator visible briefly without re-plumbing the
ViewModel's internal generation/job bookkeeping.

The `PullToRefreshBox` wraps the LIST / GRID / COMPACT lists, the empty state, and the
category-loading state (i.e. all the non-permission branches), so the gesture works
whether the folder has items or is empty. The "needs permission" branch stays outside
the refresh wrapper (nothing to refresh there). To let the gesture register on the
empty/loading states (which are not otherwise scrollable), those Columns get a
`verticalScroll` so the nested-scroll drag reaches the pull container.

## Files to change

- `app/src/main/java/com/example/ui/FileExplorerScreen.kt`
  - Add imports: `androidx.compose.material3.pulltorefresh.PullToRefreshBox`,
    `androidx.compose.material3.pulltorefresh.rememberPullToRefreshState`,
    `androidx.compose.foundation.verticalScroll` (already imported),
    `androidx.compose.foundation.rememberScrollState` (already imported),
    `androidx.compose.runtime.rememberCoroutineScope`, `kotlinx.coroutines.delay`.
  - In `FileExplorerScreen`, add `isRefreshing` state + `rememberCoroutineScope` + an
    `onRefresh` lambda.
  - In the content `Box`, wrap the non-permission branches in `PullToRefreshBox`.
  - Add `verticalScroll` to the empty-state and category-loading Columns so the pull
    gesture registers there too.

No string resources or ViewModel changes required.

## Verification

`./gradlew assembleDebug`, then manually: drag down on a folder list, an empty folder,
and a category-filtered list → spinner shows and the listing reloads.
