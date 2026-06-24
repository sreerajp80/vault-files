# Fix: back gesture handling app-wide (tabs + settings sub-screens)

## Issue

The system back gesture/button only does something useful inside the File Explorer
(fixed previously). Everywhere else it closes the app:

- On the Storage, Vault, or Settings tabs, back closes the app instead of returning
  to the main (Files) screen.
- Inside Settings sub-pages (Display, Security, About), back closes the app instead
  of returning to the Settings hub.

The desired behavior: **the app should only close when back is used from the main
screen** (the Files tab at its root folder with no category filter). Everywhere else,
back should pop one level of in-app navigation.

## Investigation — what navigation exists

- Tabs live in `MainActivity` via `activeTabIndex` (0=Storage, 1=Files [default/main],
  2=Vault, 3=Settings). ([MainActivity.kt:90](app/src/main/java/com/example/MainActivity.kt#L90))
- `SettingsScreen` has an internal hub: `settingsPage` ∈ {settings, display, security,
  about}; sub-pages already expose an `onBack` that sets `settingsPage = "settings"`.
  ([SettingsScreen.kt:59](app/src/main/java/com/example/ui/SettingsScreen.kt#L59))
- `FileExplorerScreen` — already has a `BackHandler` (category filter → navigate up).
- `SecureVaultScreen` and `StorageAnalyzerScreen` have **no** internal multi-page
  navigation (only dialogs / toggles). `AlertDialog`s already dismiss on back by
  default, so they need no handler. `AppLockScreen` should keep closing the app on
  back (can't bypass the lock) — left unchanged.

So only two places need new handlers: the tab level (MainActivity) and the Settings
hub (SettingsScreen).

## How BackHandler priority works (why this composes correctly)

Compose `BackHandler`s register with the `OnBackPressedDispatcher`; the most recently
composed enabled handler wins. The per-tab screens are composed *inside* MainActivity's
content, so a screen-level handler takes priority over the tab-level handler. This gives
the correct precedence automatically:

1. Files tab: FileExplorer handler pops folders/filter; at root it's disabled → app closes.
2. Settings tab on a sub-page: Settings handler pops to the hub; on the hub it's disabled →
   MainActivity handler returns to Files tab.
3. Storage/Vault tab: no screen handler → MainActivity handler returns to Files tab.

## Files to change

### `app/src/main/java/com/example/MainActivity.kt`
- Add `import androidx.activity.compose.BackHandler`.
- Inside the non-locked branch (where `activeTabIndex` is in scope), add a
  `BackHandler(enabled = activeTabIndex != 1)` that returns to the main Files tab:
  `viewModel.clearCategoryFilter(); activeTabIndex = 1`.
  (Clearing the filter keeps Files in its normal browsing state, matching the existing
  Files-tab click behavior at [MainActivity.kt:111](app/src/main/java/com/example/MainActivity.kt#L111).)

### `app/src/main/java/com/example/ui/SettingsScreen.kt`
- Add `import androidx.activity.compose.BackHandler`.
- In `SettingsScreen`, add `BackHandler(enabled = settingsPage != "settings") { settingsPage = "settings" }`
  so back from Display/Security/About returns to the Settings hub.

### `app/src/main/java/com/example/ui/FileExplorerScreen.kt`
- No change (already handled).

## Out of scope / notes

- No full tab back-stack: non-main tabs go straight back to Files (the main screen),
  which satisfies "only the main screen closes the app." A history stack would be
  over-engineering for 4 flat tabs.
- Dialogs rely on their existing `onDismissRequest` (default back-to-dismiss) — unchanged.

## Verification

- Build: `./gradlew assembleDebug`.
- Manual:
  - Storage/Vault tab → back returns to Files tab (not close).
  - Settings → open Display/Security/About → back returns to Settings hub; back again
    returns to Files tab; back again (Files at root) closes the app.
  - Files: sub-folder → back goes up; category filter → back clears it; root → closes app.
