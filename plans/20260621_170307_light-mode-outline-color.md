# Fix: faint card/pill borders in light mode (darken light `outline`)

## Files to be changed
- `app/src/main/java/com/example/ui/theme/Color.kt`
- `app/src/main/java/com/example/ui/StorageAnalyzerScreen.kt` (simplify earlier fix)

## The issue
After the first fix the borders are still too faint in light mode (donut tile + both
source pills), while dark mode looks correct. The real root cause is the **light theme
`outline` color itself**:

- `Color.kt`: `PolishBorder = Color(0xFFE6E1E5)` — an extremely pale grey.
- `Theme.kt`: light `outline = PolishBorder`.
- Dark `outline = Color(0xFF49454F)` — has real contrast, so dark mode looks right
  (the user confirmed dark mode is the reference for how borders should look).

Because the light `outline` is nearly white, every border drawn with it is invisible on
the white/near-white surfaces. This affects not just the Storage Analysis screen
(`SourcePill`, `StorageRingTile`) but also the cards in `SettingsScreen`,
`SecureVaultScreen`, `FileExplorerScreen`, and `AboutScreen`, which all use
`MaterialTheme.colorScheme.outline` for their borders.

`PolishBorder` is used in exactly one place (the light `outline`), so changing it is a
contained, global light-mode fix that does not touch dark mode at all.

## The plan for the fix
1. **`Color.kt`** — darken `PolishBorder` from `#E6E1E5` to a clearly visible grey that
   gives light mode border contrast comparable to dark mode:
   `Color(0xFFC2BCC9)`.
   This immediately makes the non-selected `SourcePill` border, the `StorageRingTile`
   border, and all other light-mode card borders properly visible, while dark mode is
   unaffected (it uses its own `outline = #49454F`).

2. **`StorageAnalyzerScreen.kt`** — simplify the `StorageRingTile` border introduced in the
   previous change. Now that the light `outline` is itself visible, the light branch can
   just use the full `outline` (already does) and the dark branch keeps the softer
   `outline.copy(alpha = 0.4f)`. No functional change needed beyond keeping it consistent;
   leave the existing theme-aware `tileBorderColor` as-is.

## Scope notes
- The selected `SourcePill` (tinted background, no border) and the `VaultTile` (gradient
  fill, no border) are intentionally borderless and look correct in dark mode, so they are
  left unchanged.
- The category tiles use their own category-color borders and are unaffected.
