# Plan: Terracotta / Cream color scheme (light + dark)

## What the user wants

Recolor the app to match the screenshot supplied (a japa/mala counter):
warm **cream** background with **terracotta / orange‑red** accents, dark warm
text. Apply this as the **light** theme, and build a "similar" (same warm family,
inverted lightness) **dark** theme.

## Color analysis of the screenshot

| Role                         | Sampled value (approx) |
|------------------------------|------------------------|
| Cream background             | ~#F4ECDB               |
| Near‑white tile (LIFETIME)   | ~#FBF6EC               |
| Terracotta accent (DAILY bar)| ~#BF4A2E               |
| Soft rose petal strokes      | ~#CFA293               |
| Dark warm text               | ~#2C2620               |

## Files to change

1. **`app/src/main/java/com/example/ui/theme/Color.kt`**
   Replace the "Professional Polish" purple palette with a warm terracotta/cream
   palette (keep the same `val` names where practical so references in
   `Theme.kt`, `AboutScreen.kt`, `SettingsScreen.kt` keep compiling; rename only
   the obviously‑purple ones).

2. **`app/src/main/java/com/example/ui/theme/Theme.kt`**
   Re‑map `LightColorScheme` and `DarkColorScheme` to the new palette.

3. **`app/src/main/java/com/example/ui/StorageAnalyzerScreen.kt`** (lines 72‑88 only)
   The `ringGradientColors`, `vaultGradientColors`, and `accentColor` helpers are
   hardcoded **purple/blue** and would clash badly with terracotta. Recolor these
   to the terracotta family so the analyzer stays cohesive.

## Proposed palette

### Light mode
- background `#F4ECDB` (cream)
- surface / tiles `#FBF6EC`
- primary `#BF4A2E` (terracotta), onPrimary `#FFFFFF`
- primaryContainer `#F3DDD3`, onPrimaryContainer `#4A1A0E`
- secondary `#9C6B5C`
- onBackground / onSurface `#2C2620`, onSurfaceVariant `#6E5D52`
- outline `#C9A593`
- TileBorderLight `#E0CDBD`

### Dark mode (same warm family, inverted)
- background `#1B1714`
- surface `#262019`, surfaceVariant `#2E2620`
- primary `#E08763` (lightened terracotta), onPrimary `#3A1206`
- primaryContainer `#5A2A1A`, onPrimaryContainer `#F8DDD0`
- secondary `#D2A491`
- onBackground / onSurface `#ECE0D2`, onSurfaceVariant `#C3B2A4`
- outline `#5A4A40`
- TileBorderDark `#3E342C`

## Explicitly OUT of scope (left untouched)

- **Semantic icon tints** — red delete `#E74C3C/#E53935`, green security
  `#2ECC71`, yellow key `#F1C40F`. These convey meaning, not branding.
- **Donut category colors** (`CatImages`/`CatVideos`/… in StorageAnalyzerScreen)
  — they must stay visually distinct per file category.

## Risk / notes
- `dynamicColor` is already `false` in `MyApplicationTheme`, so Material‑You won't
  override these on Android 12+. Good.
- Pure value swap; no structural/logic change. Verify with `./gradlew assembleDebug`.
