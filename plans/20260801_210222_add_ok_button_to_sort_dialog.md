# Implementation Plan: Add OK Button and Keep Sort Dialog Open on Selection

Currently, the **Sort By** dialog in `FileExplorerScreen.kt` is missing an OK button (`confirmButton` is empty), and clicking any sort item row immediately closes the dialog (`showSortByDialog = false`). This prevents users from visually confirming their selected sort criteria before dismissing the window.

## Proposed Changes

### `FileExplorerScreen.kt`
[File](file:///l:/Android/vault-files/app/src/main/java/in/sreerajp/vault_files/ui/FileExplorerScreen.kt#L1250-L1335)

1. Introduce temporary selection state (`tempSortMode` and `tempSortAscending`) when opening the Sort By dialog.
2. Remove `showSortByDialog = false` from the sort option row `.clickable` callback so selecting an option updates the UI state inside the dialog without closing it immediately.
3. Add an **OK** button (`confirmButton`) displaying `R.string.action_ok` ("OK" / "ശരി") that commits `tempSortMode` and `tempSortAscending` to `sortMode` and `sortAscending` and dismisses the dialog.
4. Ensure the **Cancel** button (`dismissButton`) dismisses the dialog without committing changes.

## Verification Plan

### Automated Tests
- Run `./gradlew testDebugUnitTest` to verify all unit tests pass cleanly.

### Manual Verification
- Open the Sort By dialog in the File Explorer screen.
- Click a sort option (e.g. Name, Size, Date) or Ascending/Descending chips.
- Confirm the dialog stays open and visually shows the checkmark/arrow updates.
- Tap **OK** ("ശരി") to apply the sort changes and close the dialog.
