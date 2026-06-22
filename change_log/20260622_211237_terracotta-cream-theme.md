# Change log: Terracotta / Cream color scheme (light + dark)

Implements `plans/20260622_210940_terracotta-cream-theme.md`.

## What changed

Replaced the purple "Professional Polish" palette with a warm terracotta/cream
scheme derived from the user-supplied screenshot, for both light and dark themes.

### `app/src/main/java/com/example/ui/theme/Color.kt`
Swapped all palette values to the warm family (kept the existing `val` names for
compatibility with referencing screens):
- `PrimaryPurple` → terracotta `#BF4A2E`
- `LightPrimaryContainer` `#F3DDD3`, `DarkPrimaryText` `#4A1A0E`
- `PolishBg` cream `#F4ECDB`, `PolishSurface` `#FBF6EC`
- `PolishBorder` `#C9A593`, `PolishTextBlack` `#2C2620`, `PolishTextGrey` `#6E5D52`
- `DarkPrimary` `#E08763`, `DarkBackground` `#1B1714`, `DarkSurface` `#2E2620`,
  `DarkOnSurface` `#ECE0D2`
- `TileBorderLight` `#E0CDBD`, `TileBorderDark` `#3E342C`

### `app/src/main/java/com/example/ui/theme/Theme.kt`
Re-mapped `DarkColorScheme` and `LightColorScheme` to the new palette (terracotta
primary/containers, warm secondary, warm outlines). Added explicit `onPrimary`
for the dark scheme.

### `app/src/main/java/com/example/ui/StorageAnalyzerScreen.kt` (lines 72-88)
Recolored the previously purple/blue `ringGradientColors`, `vaultGradientColors`,
and `accentColor` helpers into the terracotta family so the analyzer stays cohesive.

## Out of scope (unchanged, as planned)
- Semantic icon tints (red delete, green security, yellow key).
- Donut category colors (`CatImages`/`CatVideos`/…) — kept distinct per category.

## Verification
- `./gradlew assembleDebug` → BUILD SUCCESSFUL; fresh `app-debug.apk` produced.
