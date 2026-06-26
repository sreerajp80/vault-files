# Move "Help & Information" into its own Settings sub-page

Implements plan `plans/20260626_211500_settings-help-info-own-page.md`.

## Problem
On the Settings hub, every section opened its own sub-page (Display, Security,
Permissions, About) except Help & Information, whose FAQ was rendered inline on the
hub — inconsistent and cluttering.

## Changes
- `app/src/main/res/values/strings.xml`, `app/src/main/res/values-ml/strings.xml`
  - Added `settings_help_title` and `settings_help_subtitle` for the new tile/header.
- `app/src/main/java/com/example/ui/SettingsScreen.kt`
  - Added `HelpOutline` import.
  - Hub: replaced the inline FAQ `Surface` with a `SettingsTile`
    (`Icons.AutoMirrored.Filled.HelpOutline`, chevron, `testTag("help_row")`) that
    navigates to `settingsPage = "help"`, kept under the existing
    `settings_section_help` label before Permissions/About.
  - Added a `"help"` branch to the `when (settingsPage)` block with a
    `SettingsSubPageHeader` and the FAQ `Surface` (the two `HelpEntry` items)
    moved verbatim from the hub.

## Scope
- FAQ wording and the `HelpEntry` composable unchanged.
- Back navigation already handled by the existing
  `BackHandler(enabled = settingsPage != "settings")`.
- Verified with `./gradlew compileDebugKotlin` (success).
