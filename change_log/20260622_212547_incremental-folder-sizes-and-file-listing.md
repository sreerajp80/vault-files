# Change log: Incremental folder-size display and incremental file listing

Date: 2026-06-22 21:25:47 (local)

Implements plan: `plans/20260622_211740_incremental-folder-sizes-and-file-listing.md`

## Summary

Both the Files screen's folder-size pass and its directory listing now update the UI
**progressively** instead of all-at-once: small/fast folders show their size as soon as it
resolves while large folders fill in later and independently, and files appear in chunks as the
directory is read.

## Files changed

### `app/src/main/java/com/example/data/StorageRepository.kt`
- Added two file-private constants: `LISTING_CHUNK = 50` (listing emission chunk size) and
  `SIZE_EMIT_INTERVAL_MS = 100` (minimum gap between progressive folder-size emissions).
- Added `sizeComputed: Boolean = true` to the `FileItem` data class — `false` marks a directory
  whose recursive size is still pending (drives the per-row spinner); always `true` for files.
- Extracted a private `mapToFileItem(file, securedFoldersList)` helper that builds a `FileItem`
  with cheap metadata only (sets `size = 0`, `sizeComputed = false` for directories).
- **Replaced** the one-shot `getFilesAndFoldersInDirectory(...)` with
  `getFilesAndFoldersFlow(directory, showHidden): Flow<List<FileItem>>`, which emits a growing
  (unsorted) list in chunks of `LISTING_CHUNK`, with a guaranteed final emission, on
  `Dispatchers.IO`. Cooperatively cancellable via `currentCoroutineContext().ensureActive()`.
- **Replaced** the one-shot `computeDirectorySizes(...)` with
  `computeDirectorySizesFlow(items): Flow<List<FileItem>>`, which walks each pending directory in
  turn and re-emits the full list after each folder resolves (throttled to one emission per
  `SIZE_EMIT_INTERVAL_MS`, plus a guaranteed final emission), marking each resolved folder
  `sizeComputed = true`. Cooperatively cancellable between folders.
- Updated the `getFilesByCategoryRecursive` KDoc reference from the removed method to
  `getFilesAndFoldersFlow`.

### `app/src/main/java/com/example/ui/StorageViewModel.kt`
- Replaced the `directorySizeJob` field with a `loadJob` that wraps the entire directory load
  (both phases); a new navigation cancels it so a slow listing or size scan can't overwrite the
  current directory.
- Reworked `loadFilesInDirectory`:
  - Phase 1 collects `getFilesAndFoldersFlow(...)`, writing each progressive emission to
    `_currentDirectoryFiles` (gated on the `loadGeneration` token).
  - Phase 2, only if any folder is still pending, sets `_isComputingDirectorySizes = true` and
    collects `computeDirectorySizesFlow(listing)`, writing each progressive emission to
    `_currentDirectoryFiles`; a `finally` resets `_isComputingDirectorySizes` (all gated on the
    generation token to avoid a stale job clobbering newer state or leaving a stuck spinner).

### `app/src/main/java/com/example/ui/FileExplorerScreen.kt`
- Per-row size cell in `FileRowItem` now shows the spinner when `item.isDirectory &&
  !item.sizeComputed` and the size text once `sizeComputed` is true — replacing the dependence on
  the single global `isComputingDirectorySizes` boolean, so each folder switches independently.
- Removed the now-unused `isComputingSize` parameter from `FileRowItem`, its call-site argument,
  and the unused `isComputingDirectorySizes` `collectAsState()` in the screen. (The VM still
  exposes the `isComputingDirectorySizes` StateFlow for a possible future screen-level affordance.)

## Notes / behavior

- The screen already re-sorts the list client-side and the `LazyColumn` keys on `absolutePath`,
  so progressive/unsorted emissions sort into place and animate without losing row identity.
  `totalSize`/`folderCount` derive from the list and tick upward naturally as data arrives.
- When the user sorts by SIZE, rows reorder as sizes arrive (expected; animates via the key).
- All incremental writes remain gated by the `loadGeneration` token so rapid navigation cannot
  interleave a stale stream or leave a stuck spinner.

## Verification

- `./gradlew compileDebugKotlin` — BUILD SUCCESSFUL. The only warning (`Icons.Filled.Sort`
  deprecation) is pre-existing and unrelated to this change.

## Out of scope (unchanged)

- Category-filter mode (`getFilesByCategoryRecursive` + `_isCategoryLoading`) remains a single
  recursive scan emitted once — a possible follow-up.
