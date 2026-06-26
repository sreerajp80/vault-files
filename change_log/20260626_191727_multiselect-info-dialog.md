# Change log: Show info/details for multi-selection

Implements plan `plans/20260626_191727_multiselect-info-dialog.md`.

## What changed

The file explorer selection toolbar's **info (details)** icon is now visible for any selection,
not just a single item. Tapping it with multiple items selected opens a new aggregate details
dialog.

### `app/src/main/java/com/example/ui/FileExplorerScreen.kt`
- Added `showDetailsForSelection: List<FileItem>?` screen state next to `showDetailsForFileItem`.
- `SelectionToolbar`'s `onDetails` callback now branches: a single selection opens the existing
  per-item `FileDetailsDialog`; a multi-selection sets `showDetailsForSelection`.
- Removed the `if (count == 1)` guard around the info `IconButton` so it shows whenever in
  selection mode (testTag `selection_action_details` and content description unchanged).
- Render the new `SelectionDetailsDialog` when `showDetailsForSelection` is non-null.
- Added the `SelectionDetailsDialog(items, onDismiss)` composable. It shows:
  - **Type** — "Files and folders" when both are selected, else "Folders" / "Files".
  - **Path** — the shared parent folder (`items.first().file.parent`).
  - **Size** — total summed size (`formatBytes`).
  - **Count** — number of selected items.
  - **Modified** — em dash `—` (no single timestamp across a selection / "null").
  - Title reuses `files_selected_count` ("N selected").

### `app/src/main/res/values/strings.xml` and `values-ml/strings.xml`
- Added `detail_count`, `detail_files`, `detail_folders`, `detail_files_and_folders`
  (English + Malayalam).

## Verification
- `./gradlew compileDebugKotlin` completed without errors.

The single-item `FileDetailsDialog` is unchanged.
