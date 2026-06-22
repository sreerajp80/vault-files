# Change log: darken light-mode `outline` for visible borders

Implements plan `plans/20260621_170307_light-mode-outline-color.md`.

## What changed
- `app/src/main/java/com/example/ui/theme/Color.kt` — changed `PolishBorder` from
  `Color(0xFFE6E1E5)` to `Color(0xFFC2BCC9)`.

  `PolishBorder` is used in exactly one place — the light color scheme's `outline`
  (`Theme.kt`). It was nearly white, so every border drawn with
  `MaterialTheme.colorScheme.outline` was invisible against the white/near-white light
  surfaces. Darkening it makes all light-mode borders properly visible, matching the
  dark-mode reference the user confirmed looks correct.

  This globally affects light-mode borders that use `outline`, including:
  - Storage Analysis screen: non-selected `SourcePill` border and `StorageRingTile`
    (donut tile) border.
  - Card borders in `SettingsScreen`, `SecureVaultScreen`, `FileExplorerScreen`,
    and `AboutScreen`.

  Dark mode is unaffected — it uses its own `outline = #49454F`.

## Not changed
- `StorageRingTile` theme-aware border logic (from the previous change) was already
  consistent with this plan; no further edit needed.
- Selected `SourcePill` (tinted fill, intentionally borderless) and `VaultTile`
  (gradient fill, intentionally borderless) — left unchanged.
- Dark color scheme — untouched.

## Related
- Builds on `change_log/20260621_165710_light-mode-tile-border.md`.
