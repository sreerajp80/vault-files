# Plan: Permissions section in Settings + Permissions detail screen

## Issue / goal
The Settings screen currently has Display, Security, FAQ, and About entries, but nothing that
shows the user which Android permissions the app uses. We want to:

1. Add a **Permissions** entry (tile) to the Settings hub.
2. Tapping it opens a new sub-screen that lists **all** permissions the app uses — both the
   ones we **explicitly declare** in our `AndroidManifest.xml` and any **implicit** ones that
   get merged into the final manifest from libraries / the system.

### What "explicit" vs "implicit" means here
- **Explicit** = the 4 `uses-permission` entries declared in our own `AndroidManifest.xml`:
  `READ_EXTERNAL_STORAGE`, `WRITE_EXTERNAL_STORAGE`, `MANAGE_EXTERNAL_STORAGE`,
  `REQUEST_INSTALL_PACKAGES`.
- **Implicit** = anything else that ends up in the merged manifest (added by AndroidX /
  other dependencies, e.g. things like `INTERNET`, `ACCESS_NETWORK_STATE`, ad/identifier
  permissions, etc.). We don't hardcode these — we read them at runtime so the list is always
  accurate to the installed build.

### How we get the full list
Query `PackageManager` at runtime:
```kotlin
val info = pm.getPackageInfo(packageName, PackageManager.GET_PERMISSIONS)
val requested = info.requestedPermissions        // every permission in the merged manifest
val flags     = info.requestedPermissionsFlags   // granted state per permission
```
For each permission we show:
- a friendly label (our own string for the 4 explicit ones; otherwise the system label via
  `pm.getPermissionInfo(name,0).loadLabel(pm)`, falling back to the short constant name),
- the raw permission constant (e.g. `android.permission.READ_EXTERNAL_STORAGE`),
- a **Granted / Not granted** status chip (from `REQUESTED_PERMISSION_GRANTED`).

The screen splits the list into two grouped sections:
- **Declared by this app** (the explicit set found in `requested`),
- **Added by libraries & system** (everything else in `requested`).

If a section is empty it is hidden. If the package query fails, an empty/error note is shown.

## Files to change
1. **`app/src/main/java/com/example/ui/PermissionsScreen.kt`** *(new)*
   - New `PermissionsScreen(onBack, modifier)` composable following the existing
     `AboutScreen` pattern (same `SettingsSubPageHeader`, `SettingsIconChip`,
     `SettingsSectionLabel`, tile border, LazyColumn layout).
   - Runtime `PackageManager` query + explicit/implicit grouping as described above.
   - A small private `PermissionRow` composable (label, constant, granted chip).

2. **`app/src/main/java/com/example/ui/SettingsScreen.kt`**
   - Add a **Permissions** tile to the hub (in the "Help & Information" section, above About),
     using `Icons.Default.Security` (or `VerifiedUser`) and a chevron, that sets
     `settingsPage = "permissions"`.
   - Render `PermissionsScreen(...)` when `settingsPage == "permissions"` — mirror the existing
     `if (settingsPage == "about") { AboutScreen(...) }` branch so the sub-screen draws its own
     full-screen layout. (The existing `BackHandler` already routes back to the hub for any
     non-"settings" page, so no change needed there.)

3. **`app/src/main/res/values/strings.xml`** — add new strings:
   - `settings_permissions_title` = "Permissions"
   - `settings_permissions_subtitle` = "Permissions this app uses"
   - `permissions_title` = "App Permissions"
   - `permissions_section_declared` = "Declared by this app"
   - `permissions_section_implicit` = "Added by libraries & system"
   - `permissions_status_granted` = "Granted"
   - `permissions_status_denied` = "Not granted"
   - `permissions_empty` = "No permission information available"
   - `permissions_intro` = short explanatory line shown at the top of the screen
   - Friendly labels/descriptions for the 4 explicit permissions
     (`perm_read_storage`, `perm_write_storage`, `perm_manage_storage`,
     `perm_install_packages`) + a `cd_*` content description if needed.

4. **`app/src/main/res/values-ml/strings.xml`** — Malayalam translations for every string
   added in step 3 (keeps parity with the existing fully-translated `values-ml`).

## Notes / non-goals
- Read-only/informational screen. No requesting or revoking permissions, no new app
  permissions added to the manifest.
- No new dependencies. Pure Compose + framework `PackageManager` APIs.
- Matches existing visual language (tiles, borders, icon chips, sub-page header).
