# Change log: back gesture navigates up instead of closing the app

Implements plan `plans/20260622_213022_back-gesture-navigate-up.md`.

## Problem

Using the system back gesture/button while inside a sub-folder closed the app
instead of navigating to the parent folder, because the app never intercepted the
system back event (no `BackHandler` existed anywhere in the source).

## Changes

`app/src/main/java/com/example/ui/FileExplorerScreen.kt`
- Added import `androidx.activity.compose.BackHandler`.
- Added a `BackHandler` in the `FileExplorerScreen` composable, enabled when a
  category filter is active or when not at the storage root:
  - if a category filter is active → `viewModel.clearCategoryFilter()`;
  - otherwise → `viewModel.navigateUp()`.
- When at the root with no active filter the handler is disabled, so back falls
  through to the default behavior and leaves the app.

The handler is scoped to `FileExplorerScreen`, which is only composed on the Files
tab, so back behavior on the Analyzer / Vault / Settings tabs is unchanged. No
change to `navigateUp()` semantics — only a new caller was added.

## Verification

- `./gradlew assembleDebug` completed successfully (no compilation errors).
- Manual test recommended: enter a sub-folder → back gesture goes up one level;
  repeat to root → back exits the app; with a category filter active → back clears
  the filter first.
