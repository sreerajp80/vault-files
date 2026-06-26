# Plan: Show info/details for multi-selection

## Issue / request

In the file explorer's selection toolbar, the **info (details) icon** is only shown when exactly
one item is selected (`if (count == 1)` in `SelectionToolbar`). When multiple files/folders are
selected there is no way to view their details.

The user wants the info icon to be visible for multi-selection too, opening a details dialog that
shows aggregate information:

- **Size** — total (summed) size of all selected items.
- **Path** — the parent folder (all selected items share the current directory).
- **Type** — `Files and folders` if both files and folders are selected; `Folders` if all
  selected are folders; `Files` if all selected are files.
- **Count** — total number of items selected.
- **Modified** — null / no value (shown as an em dash `—`).

The existing single-selection details dialog (`FileDetailsDialog`) stays exactly as-is.

## Files to change

1. `app/src/main/java/com/example/ui/FileExplorerScreen.kt`
   - Add a new state `showDetailsForSelection: List<FileItem>?` alongside the existing
     `showDetailsForFileItem` (~line 167).
   - In the `SelectionToolbar(...)` call's `onDetails` callback (~line 342): when a single item is
     selected keep using `showDetailsForFileItem`; when multiple are selected set
     `showDetailsForSelection = selectedItems`.
   - Render a new `SelectionDetailsDialog(...)` when `showDetailsForSelection` is non-null
     (next to the existing `showDetailsForFileItem?.let { ... }` block ~line 866).
   - In `SelectionToolbar` (~line 1944): remove the `if (count == 1)` guard around the info
     `IconButton` so the icon shows whenever in selection mode (single or multiple). testTag and
     content description unchanged.
   - Add a new `SelectionDetailsDialog(items: List<FileItem>, onDismiss)` composable (near
     `FileDetailsDialog`) that renders the aggregate rows described above using the existing
     `DetailRow`, `formatBytes`, and `AlertDialog` patterns. Title reuses
     `files_selected_count` ("N selected"). Modified row shows `—`.

2. `app/src/main/res/values/strings.xml`
   - Add `detail_files_and_folders` = "Files and folders"
   - Add `detail_files` = "Files"
   - Add `detail_folders` = "Folders"
   - Add `detail_count` = "Count"

3. `app/src/main/res/values-ml/strings.xml`
   - Add Malayalam translations for the same four new keys (to keep string-resource parity).

## Notes / decisions

- Parent-folder path is taken from `items.first().file.parent` (selection is always within the
  current directory, so they share a parent).
- "Modified is null" is rendered as an em dash `—` for clean UI rather than the literal text
  "null". Confirm if you'd prefer a different placeholder.
- Directory sizes that haven't finished computing contribute their current `size` value (same
  behavior as the existing single-item dialog); no new size computation is added.
