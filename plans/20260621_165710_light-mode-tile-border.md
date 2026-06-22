# Fix: storage-overview tile border invisible in light mode

## Files to be changed
- `app/src/main/java/com/example/ui/StorageAnalyzerScreen.kt`

## The issue
On the Storage Analysis screen, the storage-overview "donut" tile (`StorageRingTile`)
has a border that is essentially invisible in **light mode**, while it looks fine in
dark mode (per the user's screenshot).

Root cause (from `Theme.kt`):
- Light `surfaceVariant` = `Color.White` (the tile fill).
- Light `background` = `PolishBg` = `#FCFCFF` (near-white page behind it).
- Light `outline` = `PolishBorder` = `#E6E1E5` (already a very light grey).

`StorageRingTile` draws its border with `outline.copy(alpha = 0.4f)`. A light-grey
outline at 40% opacity over a white-on-near-white tile has almost no contrast, so the
border disappears. In dark mode `outline` = `#49454F` at 0.4 alpha has real contrast
against the dark surface, so dark mode is fine and must stay unchanged.

Note: the `SourcePill` ("App Sandbox") border uses the **full** `outline` color (no alpha
reduction) and is visible in the screenshot, so it is left as-is. The `VaultTile` is a
gradient fill with no border (intentional). The `CategoryStatTile` tiles use category-color
tinted borders and are not part of this report. So the only fix needed is `StorageRingTile`.

## The plan for the fix
In `StorageRingTile`, make the border theme-aware so light mode gets a clearly visible
border while dark mode is unchanged:

- Compute the border color based on `isSystemInDarkTheme()`:
  - **dark:** `MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)` (unchanged behavior).
  - **light:** use the full `MaterialTheme.colorScheme.outline` color (no alpha reduction),
    matching the visible `SourcePill` border treatment for consistency.
- Keep border width at `1.dp` and the `RoundedCornerShape(24.dp)`.

This is a one-spot change to the `.border(...)` call in `StorageRingTile`
(around `StorageAnalyzerScreen.kt:408`).
