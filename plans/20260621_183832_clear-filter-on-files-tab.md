# Clear category filter when tapping the Files tab directly

## Issue

When the user taps a category tile on the Storage Analysis screen, the app navigates to
the Files tab with a category filter applied (e.g. the "Documents · 136" chip shown above
the file list). This filter persists. If the user then taps the **Files** tab directly in
the bottom navigation bar, they expect to return to normal directory browsing, but the
stale category filter chip / filtered list remains in place.

## Root cause

In `MainActivity.kt`, the Files `NavigationBarItem` `onClick` only sets
`activeTabIndex = 1`. It never clears the active category filter, so
`viewModel.activeCategoryFilter` stays set from the earlier Storage Analysis tile tap.

The ViewModel already exposes a `clearCategoryFilter()` method
(`StorageViewModel.kt:167`) that resets `_activeCategoryFilter`, `_categoryFilteredFiles`,
and `_isCategoryLoading` — it just isn't called from the Files tab click.

## Files to change

- `app/src/main/java/com/example/MainActivity.kt` — the Files `NavigationBarItem`
  `onClick` (around line 95).

## Plan for the fix

Change the Files tab's `onClick` from:

```kotlin
onClick = { activeTabIndex = 1 },
```

to also clear the filter:

```kotlin
onClick = {
    viewModel.clearCategoryFilter()
    activeTabIndex = 1
},
```

This leaves the Storage Analysis tile path untouched (it calls `openCategoryFilter(category)`
then sets `activeTabIndex = 1` directly via the `onOpenFilesWithCategory` callback, not via
the nav item's `onClick`), so tapping a tile still navigates with the filter applied, while
tapping the Files tab itself always returns to normal browsing.

## Notes / scope decisions

- Filter is cleared on **every** tap of the Files tab (including when already on Files),
  which matches the request "When I tap on Files directly the filter ... should be cleared."
- No ViewModel changes needed — `clearCategoryFilter()` already exists and is reused.
- No test changes strictly required; behavior is a single click handler addition.
