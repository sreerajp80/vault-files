# Plan: Long-press multi-select + contextual action toolbar (Files)

## Issue / Goal

Today, per-item actions on the Files screen are reached only by tapping a small `MoreVert`
("dot") button on each row/grid/compact item, which opens the `CardItemMenu` `AlertDialog`
(Details, Lock/Unlock folder or Move to vault, Compress/Extract, Delete).

Requested behavior:
- **Long-press a file/folder selects it** (does not open it).
- The user can **select multiple** files/folders by long-pressing each one (multi-select).
- While anything is selected, a **contextual action toolbar replaces the search field**,
  showing **icons** for the existing actions.
- The per-item **dot (⋮) button is removed** — long-press is the only way to reach item actions.

Decisions confirmed with the user:
- Selection model: **multi-select**.
- Dot button: **removed**.
- Toolbar placement: **replaces the search field** while selection is active.

## Files to change

1. `app/src/main/java/com/example/ui/FileExplorerScreen.kt` — main change (selection state,
   toolbar, long-press, item visuals, remove dot button + `CardItemMenu`).
2. `app/src/main/res/values/strings.xml` — new strings for the toolbar.
3. `app/src/main/res/values-ml/strings.xml` — matching Malayalam strings.

## Interaction model

- **Not in selection mode:** tap = existing open/navigate/preview behavior. Long-press = select
  the item and enter selection mode.
- **In selection mode** (≥1 item selected):
  - Tap an item = toggle its selection (does not open). Long-press = also toggle.
  - Selecting the last item off (count → 0) exits selection mode → search field returns.
  - System **Back** clears the selection first (added `BackHandler`, higher priority than the
    existing category/navigate-up handler).
- Selection is **cleared automatically** when the directory changes or the category filter
  changes (a `LaunchedEffect` keyed on `currentDir` + `activeCategoryFilter`), since the
  selected items would no longer be on screen.

Selection is tracked as `selectedPaths: Set<String>` (by `absolutePath`, stable across list
refreshes). The selected `FileItem`s are derived by filtering `baseList` against
`selectedPaths` (so stale paths drop out automatically).

## Toolbar contents (replaces the search field Surface)

Left side: a **close (X)** icon to clear selection, plus a **"N selected"** count label.

Right side action icons, acting on the current selection. Because the existing ViewModel
methods are all **single-item** (`deleteFileItem`, `secureFileInVault`, `toggleFolderShield`,
`decompressZip`, `compressFolderOrFile`), batch actions are done by looping those calls; the
dialog-driven actions stay single-selection:

| Icon | Action | Shown when |
|------|--------|-----------|
| `Info` (Details) | open `FileDetailsDialog` | exactly **1** item selected |
| `Compress` | open ZIP-name dialog (`showZipDialogForFile`) | exactly **1** selected and not a `.zip` |
| `FolderZip` (Extract) | `decompressZip` | exactly **1** selected and it **is** a `.zip` |
| `Security` (Lock/Unlock) | `toggleFolderShield` on each | **all** selected are directories |
| `VpnKey` (Move to vault) | `secureFileInVault` on each | **all** selected are files |
| `Delete` | delete each selected item | **always** |

- **Delete** and **Move to vault** keep the existing biometric/PIN gate when
  `phoneLockDeleteEnabled` is on: one `PendingAction` is raised whose `onValidated` loops over
  the whole selection (single auth prompt for the batch), then clears the selection.
- After any action runs, the selection is cleared (toolbar collapses back to the search field).
- Icons reuse existing strings as content descriptions: `menu_details`, `menu_compress`,
  `menu_decompress`, `menu_lock_folder`/`menu_remove_shield`, `menu_move_vault`, `menu_delete`.

## Code changes in `FileExplorerScreen.kt`

1. **State:** remove `expandedMenuForFileItem`; add
   `var selectedPaths by remember { mutableStateOf(setOf<String>()) }`.
   Derive `val selectionMode = selectedPaths.isNotEmpty()` and
   `val selectedItems = baseList.filter { it.absolutePath in selectedPaths }`.
   Add a `toggleSelection(item)` helper.

2. **Auto-clear:** `LaunchedEffect(currentDir, activeCategoryFilter) { selectedPaths = emptySet() }`.

3. **Back handling:** add `BackHandler(enabled = selectionMode) { selectedPaths = emptySet() }`
   after the existing `BackHandler`.

4. **Search/toolbar swap:** in the header, replace
   `if (!needsPermission) { <search Surface> }` with
   `if (!needsPermission) { if (selectionMode) SelectionToolbar(...) else <search Surface> }`.
   New private composable `SelectionToolbar` renders the X + count + action `IconButton`s
   described above, styled to match the search Surface (same rounded `Surface`, height,
   padding). Add `testTag("files_selection_toolbar")` and per-action tags
   (`selection_action_delete`, `selection_action_details`, etc.).

5. **Item composables** (`FileRowItem`, `FileGridItem`, `FileCompactRow`):
   - New params: `selectionMode: Boolean`, `isSelected: Boolean`, `onToggleSelection: () -> Unit`.
   - Remove the `onActionMenuOpen` param and the `MoreVert` dot button (`options_<name>`).
     For the grid item, the top-end overlay box is replaced by a **selection check** circle
     shown only when `isSelected`.
   - Replace `Surface(onClick = onItemClick)` with a non-clickable `Surface` plus
     `Modifier.combinedClickable(onClick = { if (selectionMode) onToggleSelection() else onItemClick() }, onLongClick = onToggleSelection)`.
   - **Selected visual:** when `isSelected`, use a primary-colored border and a tinted
     container (e.g. `primaryContainer`) and/or a `CheckCircle` indicator, so selected items
     read clearly in all three view modes.

6. **Call sites** (the three `items(...)` blocks): pass `selectionMode`,
   `isSelected = item.absolutePath in selectedPaths`,
   `onToggleSelection = { toggleSelection(item) }`; drop `onActionMenuOpen`.

7. **Remove** the `expandedMenuForFileItem?.let { CardItemMenu(...) }` block and the
   `CardItemMenu` composable (now unused). `FileDetailsDialog` and the ZIP dialog remain;
   they are now triggered from the toolbar (Details / Compress for the single selected item).

8. **Imports:** add `androidx.compose.foundation.combinedClickable` (and
   `ExperimentalFoundationApi` opt-in as needed) and any new Material icons (`CheckCircle`).

## New strings (both `values/` and `values-ml/`)

- `cd_clear_selection` — "Clear selection"
- `files_selected_count` — "%1$d selected" (format string; avoids per-language plural rules)

(Action icons reuse the existing `menu_*` strings for their content descriptions.)

## Out of scope / notes

- No `StorageViewModel` / repository changes — batch actions loop existing single-item methods.
- True batch compress (one archive from many items) is **not** added; Compress stays
  single-selection. Can be a follow-up if wanted.
- Existing tests/tags that referenced `options_<name>` or the dot menu will need updating if
  present; will adjust as part of implementation.

## Verification

- `./gradlew assembleDebug` compiles.
- Manual: long-press selects (no open); long-press more items to multi-select; toolbar replaces
  search with correct contextual icons; Delete/Move/Lock act on the whole selection; Details &
  Compress only when exactly one selected; Back and X clear selection; navigating away clears it.
