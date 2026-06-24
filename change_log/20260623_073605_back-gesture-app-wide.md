# Change log: app-wide back gesture handling (tabs + settings sub-screens)

Implements plan `plans/20260623_073605_back-gesture-app-wide.md`.

## Problem

The back gesture only worked inside the File Explorer. On other tabs, and inside
Settings sub-pages, back closed the app. Desired: the app should only close when
back is used from the main Files tab at its root; everywhere else back pops one
level of in-app navigation.

## Changes

`app/src/main/java/com/example/MainActivity.kt`
- Added import `androidx.activity.compose.BackHandler`.
- Added `BackHandler(enabled = activeTabIndex != 1)` in the unlocked branch that
  clears any category filter and switches back to the main Files tab. Disabled on
  the Files tab, so back there falls through to the screen/system as before.

`app/src/main/java/com/example/ui/SettingsScreen.kt`
- Added import `androidx.activity.compose.BackHandler`.
- Added `BackHandler(enabled = settingsPage != "settings") { settingsPage = "settings" }`
  so back from the Display/Security/About sub-pages returns to the Settings hub.

`app/src/main/java/com/example/ui/FileExplorerScreen.kt`
- No change (folder/filter back handling added previously).

## Behavior / precedence

Compose `BackHandler`s give priority to the most recently composed enabled handler.
Per-tab screens compose inside MainActivity's content, so screen-level handlers run
first:
- Files tab: pops category filter → parent folder → at root, app closes.
- Settings tab: pops sub-page → hub → then MainActivity handler returns to Files tab.
- Storage/Vault tab: MainActivity handler returns to Files tab.

`SecureVaultScreen`, `StorageAnalyzerScreen`, and dialogs were unchanged (no inner
multi-page navigation; `AlertDialog` already dismisses on back). `AppLockScreen`
intentionally still closes the app on back.

## Verification

- `./gradlew assembleDebug` completed successfully (no compilation errors).
- Manual test recommended:
  - Storage/Vault tab → back returns to Files tab.
  - Settings → Display/Security/About → back returns to Settings hub → back returns
    to Files tab → back at Files root closes the app.
  - Files: sub-folder → back goes up; category filter → back clears it; root → closes.
