# Fix `Icons.Filled.Sort` deprecation warning

## Files to be changed
- `app/src/main/java/com/example/ui/FileExplorerScreen.kt`

## Issue
Compile produces one deprecation warning: `Icons.Default.Sort` (a.k.a. `Icons.Filled.Sort`)
is deprecated. Compose moved this glyph to the `automirrored` package so it mirrors correctly
in right-to-left layouts. The deprecated alias still resolves but emits a warning.

- Usage: `FileExplorerScreen.kt:411` — `Icons.Default.Sort` inside the sort-control `Icon`.
- Current imports bring `Icons.Default.*` via `androidx.compose.material.icons.filled.*`
  (line 23); other auto-mirrored icons are already imported individually (lines 20-22).

## Plan for the fix
1. Add import `androidx.compose.material.icons.automirrored.filled.Sort` (alongside the
   existing `automirrored.filled.*` imports, lines 20-22).
2. Change `Icons.Default.Sort` → `Icons.AutoMirrored.Filled.Sort` at line 411.

No behavior change (same glyph, now RTL-aware). Verify with `./gradlew compileDebugKotlin`
to confirm the warning is gone.
