# Change log: Fix slow folder/file display in File Section

Date: 2026-06-22 (local)
Implements plan: `plans/20260622_075734_files-listing-perf.md`

## Summary

Eliminated the delay when listing folders/files in the **Files** tab (notably on **Entire
Device**) and when navigating back/forward through folders.

## Changes

### `app/src/main/java/com/example/data/StorageRepository.kt`
- `getFilesAndFoldersInDirectory()` no longer computes recursive folder sizes inline. Directories
  are now listed with `size = 0L`; only cheap metadata (file length, one-level `itemCount`) is
  gathered, so the listing returns without walking subtrees. Added a doc comment explaining the
  two-phase contract.
- Added `computeDirectorySizes(items)`: returns a copy of the list with each directory's `size`
  filled via `getDirectorySize()`, calling `ensureActive()` between entries so a slow scan is
  cancelled promptly on fast navigation.
- Added import `kotlinx.coroutines.ensureActive`.

### `app/src/main/java/com/example/ui/StorageViewModel.kt`
- `loadFilesInDirectory(dir)` is now two-phase: emit the fast listing immediately, then in a
  cancellable background job (`directorySizeJob`) compute folder sizes and re-emit only if the
  user is still on `dir`. Each new load cancels the previous size job.
- Removed `refreshStorageStats()` from the per-directory `combine` collector in `init` so
  navigation no longer triggers a full-device recursive stats scan.
- Added a single `refreshStorageStats()` call during startup in `init` (stats are still refreshed
  on every file mutation as before).
- Added import `kotlinx.coroutines.Job`.

## Verification

- `./gradlew compileDebugKotlin` — compiles cleanly (only unrelated JVM native-access warnings).

## Behavior notes

- Folder sizes are preserved: each folder's row and the header total briefly show `0`/partial then
  update once the background size pass completes. Size-sort self-corrects when sizes arrive because
  `displayedFiles` recomputes on the underlying list change.
- Category-filter listing was left unchanged (already async with a loading spinner).
