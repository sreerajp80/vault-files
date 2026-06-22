# Plan: Honour the status bar on the Storage Analysis tab

## Issue

With `enableEdgeToEdge()` active (`MainActivity.kt:37`), app content draws behind the
system status bar. The outer `Scaffold` content area only applies the **bottom** inset:

```kotlin
.padding(bottom = innerPadding.calculateBottomPadding())
```
(`MainActivity.kt:121`)

The Files / Vault / Settings tabs each host their own `Scaffold` + `TopAppBar`, which
auto-pad below the status bar, so they look correct. But the **Storage** tab
(`StorageAnalyzerScreen`) is a bare `LazyColumn` with
`contentPadding = PaddingValues(top = 12.dp, bottom = 24.dp)` and no status-bar inset,
so the "Storage Analysis" header is drawn under the clock/battery icons (matches the
reported screenshot).

## Files to be changed

- `app/src/main/java/com/example/ui/StorageAnalyzerScreen.kt`

## Fix

In `StorageAnalyzerScreen`'s `LazyColumn`, add the status-bar inset to the existing top
content padding so the background still fills edge-to-edge (nice immersive look) while the
scrolling content begins below the status bar.

- Read the status-bar top inset:
  `val statusBarTop = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()`
- Change `contentPadding` from
  `PaddingValues(top = 12.dp, bottom = 24.dp)` to
  `PaddingValues(top = 12.dp + statusBarTop, bottom = 24.dp)`
- Add the needed import(s):
  `androidx.compose.foundation.layout.WindowInsets`,
  `androidx.compose.foundation.layout.statusBars`,
  `androidx.compose.foundation.layout.asPaddingValues`
  (the `layout.*` wildcard import already present likely covers these; verify and only add
  what is missing).

No changes to `MainActivity` are required — the other three tabs already handle the top
inset via their own `TopAppBar`/`Scaffold`.

## Verification

- Build: `./gradlew assembleDebug`
- Visually confirm on the Storage tab that "Storage Analysis" sits below the status bar,
  and that the other three tabs are unaffected.
