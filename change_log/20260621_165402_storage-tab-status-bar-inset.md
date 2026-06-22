# Change log: Honour the status bar on the Storage Analysis tab

Implements `plans/20260621_163716_storage-tab-status-bar-inset.md`.

## Summary

With `enableEdgeToEdge()` active, the Storage tab's bare `LazyColumn` drew its header under the
system status bar. Added the status-bar inset to the list's top content padding so content starts
below the status bar while the background still fills edge-to-edge. The other three tabs already
handle this via their own `TopAppBar`/`Scaffold`, so they were unchanged.

## Files changed

### `app/src/main/java/com/example/ui/StorageAnalyzerScreen.kt`

- In `StorageAnalyzerScreen`, read the status-bar inset:
  `val statusBarTop = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()`.
- Changed the `LazyColumn` `contentPadding` from `PaddingValues(top = 12.dp, bottom = 24.dp)` to
  `PaddingValues(top = 12.dp + statusBarTop, bottom = 24.dp)`.
- No new imports needed — `WindowInsets`, `statusBars`, and `asPaddingValues` are covered by the
  existing `androidx.compose.foundation.layout.*` wildcard import.

## Verification

- `./gradlew compileDebugKotlin` — BUILD SUCCESSFUL.
- Visual confirmation on-device (header sits below the status bar; other tabs unaffected) still to
  be performed.
