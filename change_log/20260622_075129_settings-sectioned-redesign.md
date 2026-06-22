# Change log: Settings sectioned redesign

Implements plan `plans/20260622_074504_settings-sectioned-redesign.md`.

## What changed

Redesigned the Settings tab from one long scroll into a **sectioned hub + sub-pages**
matching `samples/Settings Redesign.dc.html`, with correct light/dark, an honoured phone
top bar, and clearly visible borders on every tile/pill.

### `app/src/main/java/com/example/ui/SettingsScreen.kt` (rewrite)
- Replaced the single `Scaffold`/`TopAppBar` long list with a hub navigated via a local
  `settingsPage` state (`settings` | `display` | `security` | `about`,
  `rememberSaveable`). The old `showAbout` boolean is gone; About is now a sub-page.
- **Hub**: large "Settings" title + subtitle, `PREFERENCES` (Display & Themes, Security
  Protection navigation tiles), `HELP & INFORMATION` (restyled Q&A card), and an
  About Vault Files tile (`testTag("about_row")` preserved).
- **Display sub-page**: Application Theme (Change dropdown) + Show Hidden Files toggle.
- **Security sub-page**: App Passcode + the three lock toggles + Shielded Folders Ledger
  (dashed empty-state pill or the protected-folder list with remove action).
- Root is a `LazyColumn` painted with `colorScheme.background` and padded by the
  **status-bar top inset** (`WindowInsets.statusBars`) so headers sit below the clock —
  no Material app bar, matching the mock.
- Added reusable internal composables: `SettingsSectionLabel`, `SettingsIconChip`,
  `SettingsTile`, `SettingsSubPageHeader`, plus private `ChevronTrailing` / `HelpEntry`.
- All existing state, actions, PIN-setup dialog, biometric/PIN verification dialogs, and
  every `testTag` (`theme_selector_btn`, `theme_opt_*`, `show_hidden_switch`,
  `setup_pin_btn`, `app_lock_switch`, `password_hidden_switch`, `delete_move_lock_switch`,
  `setup_pin_field_*`, `pin_setup_save_btn`, `auth_pin_*`, `about_row`) are preserved.
  The verification dialogs are hoisted to the composable root so they work from any page.

### `app/src/main/java/com/example/ui/AboutScreen.kt` (restyle)
- Dropped the Material `TopAppBar` for the shared `SettingsSubPageHeader` (rounded-square
  back button + title), applied the status-bar top inset and background, and restyled the
  build-info rows to the new 18.dp bordered tile + `SettingsIconChip` look.
- Preserved `onBack`, `about_screen`, and `about_back_btn` tags.

### `app/src/main/java/com/example/ui/theme/Color.kt`
- Added `TileBorderLight = 0xFFD7D1E6` and `TileBorderDark = 0xFF3A3A46` — borders that
  stay clearly identifiable against tiles + page background in each mode (stronger than the
  mock's faint borders). Tiles pick the border by effective mode via background luminance.

### `app/src/main/java/com/example/MainActivity.kt`
- Added a `SideEffect` setting `isAppearanceLightStatusBars = !darkTheme` via
  `WindowCompat`, so status-bar icon contrast is correct for the effective theme even when
  the user forces a theme opposite to the system (the mock's light-mode status-bar fix).
  Global, benefits all tabs.

## Verification
- `./gradlew assembleDebug` → BUILD SUCCESSFUL.
- No data-layer/ViewModel/DB/behavior changes; all `testTag`s preserved.
