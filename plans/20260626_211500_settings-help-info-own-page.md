# Move "Help & Information" into its own Settings sub-page

## Issue
On the Settings hub, every section is a tappable tile that opens its own sub-page
(Display, Security, Permissions, About) — except **Help & Information**, whose FAQ
content is rendered inline directly on the hub (the `Surface` with the two
`HelpEntry` items, `SettingsScreen.kt` ~lines 138–157). This is inconsistent with
the rest of Settings and clutters the hub.

We want Help & Information to behave like the other sections: a tile with a chevron
on the hub that navigates to a dedicated sub-page showing the FAQ.

## Files to change
- `app/src/main/java/com/example/ui/SettingsScreen.kt`
- `app/src/main/res/values/strings.xml`
- `app/src/main/res/values-ml/strings.xml`

## Plan

### 1. Strings (both `values/` and `values-ml/`)
Add a tile title + subtitle for the new section (mirroring the Display/About tiles):
- `settings_help_title` — "Help & Information" / "സഹായം & വിവരങ്ങൾ"
- `settings_help_subtitle` — "Compression, securing & common questions"
  (Malayalam equivalent)

The existing `settings_section_help` label and the `help_compression_*` /
`help_securing_*` FAQ strings are reused unchanged (label becomes the sub-page
header title source via `settings_help_title`).

### 2. `SettingsScreen.kt` — hub
Replace the inline Help `Surface` (the FAQ block, ~lines 138–157) with a
`SettingsTile`:
- icon `Icons.AutoMirrored.Filled.HelpOutline`
- title `settings_help_title`, subtitle `settings_help_subtitle`
- `onClick = { settingsPage = "help" }`, `trailing = { ChevronTrailing() }`
- keep it under the existing `settings_section_help` section label, before the
  Permissions and About tiles.

### 3. `SettingsScreen.kt` — new sub-page
Add a `"help"` branch to the `when (settingsPage)` block (alongside `"display"`
and `"security"`), containing:
- a `SettingsSubPageHeader` with `title = stringResource(R.string.settings_help_title)`
  and `onBack = { settingsPage = "settings" }`,
- the FAQ `Surface` (the two `HelpEntry` items + divider) moved verbatim from the hub.

### Back handling
No change needed: the existing `BackHandler(enabled = settingsPage != "settings")`
already returns any sub-page (including `"help"`) to the hub.

## Out of scope
- No change to the FAQ wording or the `HelpEntry` composable.
- No change to Display/Security/Permissions/About.
