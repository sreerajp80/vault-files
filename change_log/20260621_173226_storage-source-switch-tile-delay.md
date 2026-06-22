# Change log: Fix delayed tile population when switching App Sandbox ⇄ Entire Device

Implements plan `plans/20260621_172958_storage-source-switch-tile-delay.md`.

## What changed

- `app/src/main/java/com/example/ui/StorageViewModel.kt`
  - In `updateStorageSourceMode(mode)`, added `_storageStats.value = null`
    immediately after updating `_storageSourceMode` (before `refreshStorageStats()`).

## Why

Switching storage sources kept showing the previous source's tile values for the
whole duration of the recursive scan (slow for "Entire Device"), then jumped to
the new values — the reported "delay in populating the tiles". `_storageStats`
was never cleared on a switch, so the existing `stats == null` loading-spinner
branch in `StorageAnalyzerScreen.kt` never fired.

## Effect

Clearing stats to `null` on switch makes the existing loading spinner appear
immediately, replacing the stale tiles. Tiles repopulate with the new source's
data once the scan completes. No UI changes were required.

## Verification

- `./gradlew assembleDebug` — build succeeded (only unrelated JVM native-access
  warnings).
