# Plan: Incremental folder-size display and incremental file listing

Date: 2026-06-22 21:17:40 (local)

## Problem

On the **Files** screen, two loads currently update the UI in an all-or-nothing way instead
of progressively:

1. **Folder sizes.** The directory load runs in two phases
   ([StorageViewModel.loadFilesInDirectory](../app/src/main/java/com/example/ui/StorageViewModel.kt#L165-L191)):
   - Phase 1 — fast listing with `size = 0`, emitted immediately. (Already non-blocking.)
   - Phase 2 — [computeDirectorySizes](../app/src/main/java/com/example/data/StorageRepository.kt#L161-L166)
     walks each folder's subtree **sequentially**, but the result is assigned to the StateFlow
     **only once, after every folder is done**:
     ```kotlin
     val withSizes = repository.computeDirectorySizes(list)  // all folders
     _currentDirectoryFiles.value = withSizes                // single emit at the end
     ```
     Consequence: a single large folder early in the list blocks the display of *all* sizes,
     including small folders whose sizes were already computed. The per-row spinner is also
     driven by one **global** boolean `isComputingDirectorySizes`
     ([FileExplorerScreen.kt:1062](../app/src/main/java/com/example/ui/FileExplorerScreen.kt#L1062)),
     so every folder flips from spinner to size at the same instant.

2. **File listing.** [getFilesAndFoldersInDirectory](../app/src/main/java/com/example/data/StorageRepository.kt#L126-L153)
   builds the entire `List<FileItem>` (one non-recursive `listFiles()` plus per-item metadata:
   `getCategoryForFile`, and a one-level `file.listFiles().size` for `itemCount`), sorts it, and
   returns it in one shot. For a directory with many entries the user waits for the whole list
   before anything appears.

Goal: show small/fast folder sizes as soon as they resolve (large ones fill in later), and show
files progressively as they are listed.

## Relevant facts that shape the design

- The UI **already re-sorts** `baseList` client-side in `displayedFiles`
  ([FileExplorerScreen.kt:118-130](../app/src/main/java/com/example/ui/FileExplorerScreen.kt#L118-L130)),
  so the repository can emit partial/unsorted growing lists and the screen will sort them into
  place each time. The `LazyColumn` keys on `absolutePath`
  ([FileExplorerScreen.kt:586](../app/src/main/java/com/example/ui/FileExplorerScreen.kt#L586)), so
  growing/reordering the list animates cleanly without losing row identity.
- `totalSize` and `folderCount` in the screen are derived from the list
  ([FileExplorerScreen.kt:132-133](../app/src/main/java/com/example/ui/FileExplorerScreen.kt#L132-L133));
  they will naturally tick upward as sizes/files arrive — no extra work.
- Per-row spinner needs to become **per-item**, not global, so each folder can switch from
  spinner to size independently. This requires a way to know which folders are still pending.
- The existing `loadGeneration` token + `directorySizeJob` cancellation
  ([StorageViewModel.kt:160-189](../app/src/main/java/com/example/ui/StorageViewModel.kt#L160-L189))
  must keep gating all incremental writes so fast navigation never lets a stale stream overwrite
  the current directory or leave a stuck spinner.

## Files to be changed

1. `app/src/main/java/com/example/data/StorageRepository.kt`
   - Add `sizeComputed: Boolean` to the `FileItem` data class (default `true`) so the UI can tell,
     per row, whether a folder's recursive size is still pending.
   - In `getFilesAndFoldersInDirectory`, set `sizeComputed = !file.isDirectory` (files are known
     immediately; directories start pending).
   - **File listing (incremental):** add a Flow-based variant
     `fun getFilesAndFoldersFlow(directory, showHidden): Flow<List<FileItem>>` that emits a growing
     list in chunks (e.g. every ~50 items, plus a final emit) on `Dispatchers.IO`. Keep the
     existing one-shot `getFilesAndFoldersInDirectory` (used elsewhere / for the final list and
     by other callers) — the flow can be built on top of the same per-file mapping logic to avoid
     duplication. Emit unsorted (UI re-sorts).
   - **Folder sizes (incremental):** add
     `fun computeDirectorySizesFlow(items: List<FileItem>): Flow<List<FileItem>>` that walks each
     directory in turn and, after each folder resolves, emits an updated copy of the full list with
     that folder's `size` filled and `sizeComputed = true`. Keep it cooperatively cancellable
     (`ensureActive()` between folders). Batch emissions for large lists (e.g. emit at most every
     ~100 ms or every N folders) to bound recomposition cost; always emit a final list. Keep the
     existing one-shot `computeDirectorySizes` or replace its body to collect the flow's last value
     (avoid dead code).

2. `app/src/main/java/com/example/ui/StorageViewModel.kt`
   - Rework `loadFilesInDirectory`:
     - Phase 1: collect `getFilesAndFoldersFlow(...)`, writing each emission to
       `_currentDirectoryFiles` (gated on `generation == loadGeneration`). Set
       `_isComputingDirectorySizes = true` once listing has folders pending.
     - Phase 2: after the listing flow completes, collect `computeDirectorySizesFlow(finalList)`,
       writing each progressive emission to `_currentDirectoryFiles` (same generation gate).
       Set `_isComputingDirectorySizes = false` in a `finally` when the size flow completes.
     - Preserve `loadGeneration` and `directorySizeJob` cancellation semantics; cancel any prior
       listing/size collection on a new navigation.
   - `_isComputingDirectorySizes` stays for the screen-level "still working" affordance, but the
     per-row spinner switches to the per-item `sizeComputed` flag (below).

3. `app/src/main/java/com/example/ui/FileExplorerScreen.kt`
   - Change the per-row size cell ([FileExplorerScreen.kt:1062-1075](../app/src/main/java/com/example/ui/FileExplorerScreen.kt#L1062-L1075))
     to show the spinner when `item.isDirectory && !item.sizeComputed`, and the size text once
     `sizeComputed` is true — replacing the dependence on the global `isComputingSize` for the
     per-row decision. Keep passing/global flag only if still needed for any header affordance;
     otherwise simplify the `isComputingSize` parameter out of `FileRow`.
   - No change needed to sorting or `totalSize`/`folderCount` (they react to list growth already).

## Out of scope (call out, don't change)

- **Category-filter mode** (`getFilesByCategoryRecursive` + `_isCategoryLoading`,
  [StorageViewModel.kt:209-219](../app/src/main/java/com/example/ui/StorageViewModel.kt#L209-L219))
  is a separate flat recursive scan emitted once. Making it incremental is a possible follow-up
  but is not part of this change.
- No change to real-vs-simulated storage behavior; this is purely about *when* results are emitted.

## Risks / notes

- **Sort-by-size reordering:** when the user has sorted by SIZE, rows will reorder as sizes arrive.
  This is expected and animates via the `absolutePath` key. Default sort is NAME (no reordering).
- **Recomposition cost:** unbounded per-item emissions on a huge directory could cause jank; the
  batching/throttle in both flows bounds this. Tune chunk size / interval during implementation.
- **Cancellation correctness:** every incremental write must remain gated by the generation token
  so rapid back/forward navigation can't interleave a stale stream into the current listing.

## Testing

- Build: `./gradlew assembleDebug`.
- Manual: open Files at root — folders appear immediately; small folders show their size first,
  large folders' spinners resolve later and independently. Open a folder with many files — items
  appear progressively rather than after a single pause.
- Navigate quickly between folders to confirm no stale sizes/listings and no stuck spinners.
- If practical, a unit/Robolectric test asserting `computeDirectorySizesFlow` emits multiple
  progressive lists and a correct final list.
