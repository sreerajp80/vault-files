# Fix: back gesture closes app instead of going to parent folder

## Issue

When inside a sub-folder in the File Explorer tab, using the system back gesture
(or back button) closes the app instead of navigating to the parent folder.

### Root cause

There is no `BackHandler` anywhere in the app (confirmed: no `BackHandler` /
`onBackPressed` usage in any `.kt` source). The Compose UI never intercepts the
system back event, so it propagates to the default `FragmentActivity` behavior,
which finishes the activity (closes the app).

The navigation logic itself already exists and works:
- `StorageViewModel.navigateUp()` ([StorageViewModel.kt:149](app/src/main/java/com/example/ui/StorageViewModel.kt#L149))
  moves to the parent dir while staying within `userStorageRoot`, returning `false`
  when already at root.
- `FileExplorerScreen` already computes `isAtRoot`
  ([FileExplorerScreen.kt:110](app/src/main/java/com/example/ui/FileExplorerScreen.kt#L110))
  and exposes an up affordance, plus a category-filter mode that can be cleared via
  `viewModel.clearCategoryFilter()`.

So the only thing missing is wiring the system back event to that logic.

## Files to change

- `app/src/main/java/com/example/ui/FileExplorerScreen.kt`
  - Add `import androidx.activity.compose.BackHandler`.
  - Inside the `FileExplorerScreen` composable (after the existing state is read,
    where `isAtRoot` / `isCategoryFiltered` are already available), add a
    `BackHandler` that intercepts back only when there is somewhere to go:
    - enabled when a category filter is active **or** when not at root;
    - on back: if a category filter is active, `viewModel.clearCategoryFilter()`;
      otherwise `viewModel.navigateUp()`.
  - When disabled (at root, no filter), the handler does not consume the event, so
    the default behavior (leave the app) is preserved — matching normal Android
    expectations for the home folder.

## Scope notes

- The `BackHandler` lives in `FileExplorerScreen`, which is only composed when the
  Files tab (`activeTabIndex == 1`) is active, so it correctly does not affect back
  behavior on the Analyzer / Vault / Settings tabs.
- No change to `navigateUp()` semantics; we only add a caller.

## Verification

- Build: `./gradlew assembleDebug`.
- Manual: enter a sub-folder, perform the back gesture → should go up one level
  rather than close the app; repeat until at root, then back should exit the app.
  With a category filter active, back should clear the filter first.
