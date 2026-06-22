# Plan: Show a spinner on the folder size field while sizes compute

Date: 2026-06-22 08:01:51 (local)

## The issue / goal

After the two-phase listing change, folder rows (and the header total) briefly show `0 B` before
the background pass fills in real folder sizes. The user wants a small inline spinner on each
folder's size field while that background computation is running, instead of the transient `0 B`.

## The fix

### 1. Expose a "computing folder sizes" state from the ViewModel
- Add `private val _isComputingDirectorySizes = MutableStateFlow(false)` and public
  `isComputingDirectorySizes: StateFlow<Boolean>`.
- Rework `loadFilesInDirectory(dir)` to use a monotonic generation token so concurrent/rapid
  navigations can't leave the flag stuck. Only the latest load writes state:
  - increment `loadGeneration`, cancel the previous size job;
  - after the fast listing, bail if this isn't the latest generation; otherwise emit the list and
    set `_isComputingDirectorySizes = true`;
  - in the background size job, emit the sized list and clear the flag in a `finally`, both gated
    on `generation == loadGeneration`.
- This replaces the current `_currentDirectory.value == dir` guard (which it subsumes and also
  handles reloading the same directory correctly).

### 2. Render the spinner in the Files list
- In `FileExplorerScreen`, collect `isComputingDirectorySizes` and pass it into `FileRowItem`.
- In `FileRowItem`, when `item.isDirectory && isComputingSize`, replace the trailing
  `formatBytes(item.size)` text with a small `CircularProgressIndicator` (~14.dp, ~2.dp stroke,
  primary tint). Files and already-sized folders render the size text as before.

### Notes
- Category-filter mode lists files (not directories), so no spinner appears there — the flag only
  toggles during normal directory browsing and `item.isDirectory` is false for category items.
- The header total still shows partial/0 until sizes arrive; this plan only adds the per-row
  folder spinner as requested (no header spinner).

## Files to be changed

- `app/src/main/java/com/example/ui/StorageViewModel.kt`
  - add `_isComputingDirectorySizes` / `isComputingDirectorySizes`;
  - generation-gated two-phase `loadFilesInDirectory`.
- `app/src/main/java/com/example/ui/FileExplorerScreen.kt`
  - collect the flag, pass to `FileRowItem`;
  - `FileRowItem`: new `isComputingSize` param + spinner branch on the size field.

## Verification

- `./gradlew compileDebugKotlin`.
- Manual: open Entire Device / a deep folder — folder rows show a small spinner where the size
  goes, which is replaced by the computed size a moment later; files always show their size.
