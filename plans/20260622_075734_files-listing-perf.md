# Plan: Fix slow folder/file display in File Section

Date: 2026-06-22 07:57:34 (local)

## The issue

The user reports two delays in the **Files** tab:

1. When the app opens with **Entire Device** selected, all the folders take a while to appear.
2. Navigating back/forward through folders is slow before the list shows.

### Root cause

**(A) Recursive folder-size computation during listing — primary cause.**
`StorageRepository.getFilesAndFoldersInDirectory()` builds each `FileItem` with:

```kotlin
size = if (file.isDirectory) getDirectorySize(file) else file.length(),
itemCount = if (file.isDirectory) (file.listFiles()?.size ?: 0) else 0
```

`getDirectorySize()` (lines 192-199) **recursively walks the entire subtree** of every
sub-directory just to compute a size shown in each row. The whole list is built and only then
emitted, so nothing is displayed until every subtree of the current directory has been fully
walked. On **Entire Device**, the storage root contains large trees (`Android/`, `DCIM/`,
`Download/`, …), so opening it walks essentially the whole filesystem before drawing. Every
back/forward navigation repeats the recursive walk for the new directory.

**(B) Full-device stats scan on every navigation — secondary cause.**
In `StorageViewModel.init`, the directory collector runs:

```kotlin
combine(_currentDirectory, _showHiddenItems, _passwordProtectHidden, _isHiddenUnlocked) { dir, _, _, _ -> dir }
    .collect { dir -> loadFilesInDirectory(dir); refreshStorageStats() }
```

`refreshStorageStats()` does a **full recursive scan of the entire storage root** every time the
current directory changes. Storage stats don't depend on which folder you're viewing, so this is
wasted work that contends with the file listing for `Dispatchers.IO`, making navigation slower.

## The fix

### 1. Two-phase directory load (folder names instantly, sizes fill in after)
Make the listing fast by not computing recursive folder sizes up front, then fill sizes in
asynchronously so the existing "folder size" feature is preserved.

- **`StorageRepository.getFilesAndFoldersInDirectory()`**: for directories set `size = 0L`
  (keep cheap `itemCount` via a single `listFiles()?.size`). Files keep `file.length()`.
  This makes listing O(entries) — one `listFiles()` for the directory plus one shallow
  `listFiles()` per sub-folder for the item count — with no deep recursion.
- **Add `StorageRepository.computeDirectorySizes(items: List<FileItem>): List<FileItem>`**:
  returns a copy of the list with each directory's `size` filled via `getDirectorySize()`.
  Calls `ensureActive()` between items so the work stops promptly when the coroutine is
  cancelled (fast navigation).
- **`StorageViewModel.loadFilesInDirectory(dir)`**: emit the fast list immediately, then in a
  cancellable background job compute folder sizes and re-emit — but only if the user is still on
  `dir` (guard with `_currentDirectory.value == dir`). Track the size job in a field and cancel
  the previous one on each new load so stale/slow scans don't overwrite the current listing.

Effect: the header total and per-row sizes for folders briefly show then update once sizes are
ready; `displayedFiles` already recomputes on `baseList` changes, so Size-sort self-corrects.

### 2. Stop the full-device stats scan on every navigation
- Remove `refreshStorageStats()` from the per-directory collector in `StorageViewModel.init`
  (keep `loadFilesInDirectory(dir)`).
- Call `refreshStorageStats()` once after preferences are synced in `init`.
- Stats are already refreshed on every mutation (create/delete/zip/extract/vault/restore,
  show-hidden toggle, source-mode switch), so removing it from navigation does not make stats
  stale.

### Out of scope / unchanged
- Category-filter listing (`getFilesByCategoryRecursive`) is already async with a loading spinner
  and is recursive by nature — not part of this complaint, left as-is.
- No change to `getDirectorySize` semantics; it is just no longer called inline during listing.

## Files to be changed

- `app/src/main/java/com/example/data/StorageRepository.kt`
  - `getFilesAndFoldersInDirectory`: stop inline recursive folder-size computation.
  - add `computeDirectorySizes(...)` helper.
- `app/src/main/java/com/example/ui/StorageViewModel.kt`
  - `loadFilesInDirectory`: two-phase load with a cancellable size job.
  - `init`: remove `refreshStorageStats()` from the directory collector; call it once on startup.

## Risk / verification

- Behavior preserved: folder sizes still appear (just a moment after the list).
- Build with `./gradlew assembleDebug`; existing unit tests with `./gradlew testDebugUnitTest`.
- Manual: open app on Entire Device — folders appear immediately; navigate in/out of deep
  folders — listing is instant, sizes populate shortly after.
