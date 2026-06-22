# Fix annotation-default-target warnings (KT-73255)

Implements `plans/20260622_000000_fix-annotation-target-warnings.md`.

## Changes
- `app/src/main/java/com/example/ui/FileExplorerScreen.kt` — `FileSortMode` enum:
  `@androidx.annotation.StringRes` → `@param:androidx.annotation.StringRes` on `labelRes`.
- `app/src/main/java/com/example/ui/StorageAnalyzerScreen.kt` — `CategoryData` data class:
  `@androidx.annotation.StringRes` → `@param:androidx.annotation.StringRes` on `titleRes`.

## Effect
Pins the annotation to the value-parameter use-site, preserving current behavior and
silencing the two `compileDebugKotlin` warnings. No runtime/behavior change.
