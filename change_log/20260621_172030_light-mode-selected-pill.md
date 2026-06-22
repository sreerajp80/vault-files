# Change log: make selected source pill identifiable in light mode

Implements plan `plans/20260621_172030_light-mode-selected-pill.md`.

## What changed
- `app/src/main/java/com/example/ui/StorageAnalyzerScreen.kt` — in `SourcePill`, made the
  **selected** pill's container style theme-aware:
  - **dark mode (unchanged):** `background(accent.copy(alpha = 0.16f))`.
  - **light mode (new):** stronger tint `accent.copy(alpha = 0.18f)` plus an accent border
    `border(1.5.dp, accent.copy(alpha = 0.55f), RoundedCornerShape(16.dp))`.

  Previously the selected pill used only a 16%-alpha accent tint with no border, which
  looked faint/disabled on the white light-mode background and was less prominent than the
  unselected (outlined) pill. Dark mode was the reference and is left untouched.

## Not changed
- Unselected pill styling, accent content (icon/text) color.
- `StorageRingTile` / `VaultTile`.
- Dark color scheme.

## Related
- Follows `change_log/20260621_170307_light-mode-outline-color.md` and
  `change_log/20260621_165710_light-mode-tile-border.md`.
