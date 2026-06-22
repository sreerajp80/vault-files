# Fix `Icons.Filled.Sort` deprecation warning

Implements plan `plans/20260622_212740_automirror-sort-icon.md`.

## Changes
- `app/src/main/java/com/example/ui/FileExplorerScreen.kt`
  - Added import `androidx.compose.material.icons.automirrored.filled.Sort`.
  - Changed the sort-control icon from `Icons.Default.Sort` to
    `Icons.AutoMirrored.Filled.Sort` (line ~412).

## Rationale
Compose deprecated `Icons.Filled.Sort` in favor of the auto-mirrored variant so the glyph
mirrors correctly in RTL layouts. Same glyph, no behavior change.

## Verification
- `./gradlew compileDebugKotlin` — BUILD SUCCESSFUL, deprecation warning no longer emitted.
