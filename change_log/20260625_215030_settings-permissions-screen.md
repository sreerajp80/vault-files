# Change log: Permissions section in Settings + Permissions detail screen

Implements plan `plans/20260625_214531_settings-permissions-screen.md`.

## What changed

Added a **Permissions** entry to the Settings hub and a new read-only screen that lists every
permission the app uses — both the ones declared directly in our manifest and those merged in
from libraries / the system.

### New file
- **`app/src/main/java/com/example/ui/PermissionsScreen.kt`**
  - `PermissionsScreen(onBack, modifier)` composable following the `AboutScreen` pattern
    (reuses `SettingsSubPageHeader`, `SettingsSectionLabel`, tile border, LazyColumn layout).
  - Reads permissions at runtime via
    `PackageManager.getPackageInfo(packageName, GET_PERMISSIONS)`, using
    `requestedPermissions` + `requestedPermissionsFlags` (REQUESTED_PERMISSION_GRANTED) for the
    granted state.
  - Partitions the list into **Declared by this app** (the four explicit manifest permissions)
    vs **Added by libraries & system** (everything else). Empty sections are hidden; a fallback
    message shows if the package query yields nothing.
  - Each row shows a friendly label, the raw permission constant, and a Granted/Not granted chip.
  - `friendlyLabel()` is a plain (non-composable) helper using `context.getString` for the four
    curated permissions and `PackageManager.getPermissionInfo(...).loadLabel(...)` (with a
    constant-name fallback) for the rest — kept non-composable so it can run inside `remember`.

### Modified
- **`app/src/main/java/com/example/ui/SettingsScreen.kt`**
  - Added a **Permissions** tile (`Icons.Default.VerifiedUser`, testTag `permissions_row`) in the
    "Help & Information" section above About, navigating to `settingsPage = "permissions"`.
  - Added an `else if (settingsPage == "permissions")` branch rendering `PermissionsScreen`,
    mirroring the existing About branch. The existing `BackHandler` already routes back to the hub.
- **`app/src/main/res/values/strings.xml`** — added strings: `settings_permissions_title`,
  `settings_permissions_subtitle`, `permissions_title`, `permissions_intro`,
  `permissions_section_declared`, `permissions_section_implicit`, `permissions_status_granted`,
  `permissions_status_denied`, `permissions_empty`, and curated labels `perm_read_storage`,
  `perm_write_storage`, `perm_manage_storage`, `perm_install_packages`.
- **`app/src/main/res/values-ml/strings.xml`** — Malayalam translations for all of the above.

## Notes
- Informational only — no new manifest permissions, no requesting/revoking, no new dependencies.
- Verified with `./gradlew compileDebugKotlin` (success).
