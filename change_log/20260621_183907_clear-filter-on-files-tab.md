# Clear category filter when tapping the Files tab directly

Implements plan `plans/20260621_183832_clear-filter-on-files-tab.md`.

## What changed

- `app/src/main/java/com/example/MainActivity.kt` — the Files `NavigationBarItem`
  `onClick` now calls `viewModel.clearCategoryFilter()` before setting
  `activeTabIndex = 1`. Previously it only switched the tab, so a category filter applied
  by tapping a Storage Analysis tile (e.g. the "Documents · 136" chip) persisted when the
  user later tapped the Files tab directly.

## Result

Tapping the Files tab in the bottom navigation now always returns to normal directory
browsing with no stale filter chip. The Storage Analysis tile path is unaffected (it
applies the filter via the `onOpenFilesWithCategory` callback, not the nav item's
`onClick`), so tapping a tile still navigates to Files with the filter applied.
