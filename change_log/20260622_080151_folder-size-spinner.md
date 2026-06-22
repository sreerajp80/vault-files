# Change log: Folder size spinner while sizes compute

Date: 2026-06-22 (local)
Implements plan: `plans/20260622_080151_folder-size-spinner.md`

## Summary

Added a small inline spinner on each folder row's size field while the background folder-size pass
runs, instead of the transient `0 B`.

## Changes

### `app/src/main/java/com/example/ui/StorageViewModel.kt`
- Added `_isComputingDirectorySizes` / `isComputingDirectorySizes` StateFlow, set `true` once the
  fast listing is emitted and cleared in a `finally` when the background size pass completes.
- Reworked `loadFilesInDirectory(dir)` to use a monotonic `loadGeneration` token. Each load
  increments it and cancels the previous size job; phase-1 listing, the loading flag, the
  sized-list re-emit, and the flag reset are all gated on `generation == loadGeneration`. This
  replaces the previous `_currentDirectory.value == dir` guard and prevents a stuck spinner or
  stale results under rapid navigation (including reloading the same directory).

### `app/src/main/java/com/example/ui/FileExplorerScreen.kt`
- Collect `isComputingDirectorySizes` and pass it into `FileRowItem`.
- `FileRowItem` gained an `isComputingSize: Boolean = false` parameter; when
  `item.isDirectory && isComputingSize` the trailing size text is replaced by a 14.dp
  `CircularProgressIndicator` (2.dp stroke, primary tint). Files and already-sized folders show
  the size text as before.

## Verification

- `./gradlew compileDebugKotlin` — compiles cleanly (only unrelated JVM native-access warnings).

## Behavior notes

- Spinner appears only during normal directory browsing; category-filter mode lists files
  (`isDirectory == false`) so no spinner shows there.
- The header total still fills in quietly once sizes are computed (no header spinner, per plan).
