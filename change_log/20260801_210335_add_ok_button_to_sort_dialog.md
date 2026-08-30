# Change Log: Add OK Button and Keep Sort Dialog Open on Selection

Implemented plan [20260801_210222_add_ok_button_to_sort_dialog.md](file:///l:/Android/vault-files/plans/20260801_210222_add_ok_button_to_sort_dialog.md).

## Summary of Changes

### `FileExplorerScreen.kt`
[File](file:///l:/Android/vault-files/app/src/main/java/in/sreerajp/vault_files/ui/FileExplorerScreen.kt#L1248-L1335)

- Added temporary state variables (`tempSortMode` and `tempSortAscending`) initialized from `sortMode` and `sortAscending` when `showSortByDialog` opens.
- Removed auto-closing (`showSortByDialog = false`) from option row click handlers so selecting an option toggles/selects in real time within the dialog.
- Added `confirmButton` with `R.string.action_ok` ("OK" / "ശരി") that commits the temporary selection to `sortMode` and `sortAscending` and closes the dialog.
- Preserved Cancel button functionality to dismiss without applying changes.
