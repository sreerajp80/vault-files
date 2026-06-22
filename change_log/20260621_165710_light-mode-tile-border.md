# Change log: fix light-mode tile border visibility

Implements plan `plans/20260621_165710_light-mode-tile-border.md`.

## What changed
- `app/src/main/java/com/example/ui/StorageAnalyzerScreen.kt` — in `StorageRingTile`,
  made the tile border color theme-aware. Previously the border used
  `MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)`, which is invisible in light mode
  (light `outline` = `#E6E1E5` at 40% alpha over a white tile on a near-white background).

  Now:
  - **light mode:** uses the full `MaterialTheme.colorScheme.outline` color (no alpha
    reduction), matching the already-visible `SourcePill` border treatment.
  - **dark mode:** unchanged — still `outline.copy(alpha = 0.4f)`.

## Not changed
- `SourcePill` border (already full `outline`, visible).
- `VaultTile` (borderless gradient, intentional).
- `CategoryStatTile` (category-color tinted borders, out of scope).
