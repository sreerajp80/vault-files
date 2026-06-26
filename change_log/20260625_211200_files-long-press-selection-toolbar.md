# Change log: Long-press multi-select + contextual action toolbar (Files)

Implements plan `plans/20260625_205621_files-long-press-selection-toolbar.md`.

## Summary

Replaced the per-item ⋮ "dot" menu on the Files screen with **long-press multi-selection** and
a **contextual action toolbar** that takes the place of the search field while items are
selected.

## Files changed

### `app/src/main/java/com/example/ui/FileExplorerScreen.kt`

- **Selection state:** removed `expandedMenuForFileItem`; added
  `selectedPaths: Set<String>` (by `absolutePath`). Derived `selectionMode`,
  `selectedItems` (filtered from `baseList`, so stale paths drop out), and a `toggleSelection`
  helper.
- **Auto-clear:** `LaunchedEffect(currentDir, activeCategoryFilter)` clears the selection when
  the directory or category filter changes.
- **Back handling:** added `BackHandler(enabled = selectionMode)` that clears the selection
  first (registered after the existing handler so it takes priority).
- **Search ↔ toolbar swap:** while `selectionMode` is active the search `Surface` is replaced by
  the new `SelectionToolbar`; otherwise the search field renders as before.
- **`SelectionToolbar`** (new private composable, replaces the removed `CardItemMenu`): a
  rounded `primaryContainer` bar with a clear (✕) button, a "N selected" count, and adaptive
  icon actions:
  - Details (`Info`) — only when exactly 1 item is selected.
  - Compress (`Compress`) / Extract (`FolderZip`) — only when exactly 1 item is selected
    (Extract for a `.zip`, Compress otherwise).
  - Lock/Unlock shield (`Security`) — when **all** selected items are folders (label switches
    on `isSecured`).
  - Move to vault (`VpnKey`) — when **all** selected items are files.
  - Delete (`Delete`) — always; acts on the whole selection.
  - Batch actions loop the existing single-item `StorageViewModel` methods. Delete and
    Move-to-vault keep the biometric/PIN gate (`phoneLockDeleteEnabled`) with **one** prompt
    whose `onValidated` runs over the whole selection.
- **Item composables** (`FileRowItem`, `FileGridItem`, `FileCompactRow`):
  - Removed the `onActionMenuOpen` param and the `MoreVert` dot button (and, in grid, the
    top-end overlay button). Added `selectionMode`, `isSelected`, `onToggleSelection` params.
  - Switched the `Surface(onClick = …)` to a non-clickable `Surface` + `Modifier.combinedClickable`
    (`onClick` toggles selection while in selection mode, otherwise opens; `onLongClick` always
    toggles). Added `@OptIn(ExperimentalFoundationApi::class)`.
  - Selected state shows a primary border + `primaryContainer` fill, plus a `CheckCircle`
    (list/compact) or a filled `Check` badge (grid, `testTag("selected_<name>")`).
- Removed the old `expandedMenuForFileItem?.let { CardItemMenu(...) }` block and the
  `CardItemMenu` composable.
- Imports: added `ExperimentalFoundationApi` and `combinedClickable`.

### `app/src/main/res/values/strings.xml` and `values-ml/strings.xml`

Added: `cd_clear_selection`, `cd_selected_item`, `files_selected_count` (`%1$d selected`),
`confirm_move_subtitle_multi`, `confirm_delete_subtitle_multi` (English + Malayalam).
(The now-unused `cd_menu_actions` string was left in place.)

## Notes / out of scope

- No `StorageViewModel`/repository changes — batch actions reuse the existing single-item methods.
- True batch compress (one archive from many items) was **not** added; Compress stays
  single-selection.

## Verification

- `./gradlew assembleDebug` → **BUILD SUCCESSFUL**.
- Manual verification still recommended on device/emulator (long-press select, multi-select,
  toolbar actions, Back/✕ and navigation clearing the selection).
