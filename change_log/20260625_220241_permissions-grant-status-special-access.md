# Fix incorrect grant status for special-access permissions on the App Permissions screen

Implements plan: `plans/20260625_220241_permissions-grant-status-special-access.md`.

## Problem

The in-app **App Permissions** screen always showed `MANAGE_EXTERNAL_STORAGE` and
`REQUEST_INSTALL_PACKAGES` as **denied**, even when granted. It derived grant state
from `PackageInfo.REQUESTED_PERMISSION_GRANTED`, which does not track special-access
(appop) permissions.

## Changes — `app/src/main/java/com/example/ui/PermissionsScreen.kt`

1. **Added `resolveGranted(context, permission, flagGranted)` helper** that returns the
   true grant state:
   - `MANAGE_EXTERNAL_STORAGE` → `Environment.isExternalStorageManager()` (API 30+ guard).
   - `REQUEST_INSTALL_PACKAGES` → `PackageManager.canRequestPackageInstalls()` (API 26+ guard).
   - all other permissions → the existing manifest flag (`flagGranted`).
2. **Wired the helper into the permission-list build**: the inline flag computation is
   now `flagGranted`, and the `PermissionEntry.granted` value comes from `resolveGranted(...)`.
3. **Refresh on resume**: added a `LifecycleEventObserver` that increments a `refreshKey`
   on `ON_RESUME`, and keyed the `remember(context, refreshKey)` block on it, so the
   statuses recompute when the user returns from system settings after toggling a
   special-access permission.
4. **Imports added**: `android.os.Build`, `android.os.Environment`,
   `androidx.compose.runtime.DisposableEffect`/`getValue`/`mutableStateOf`/`setValue`,
   `androidx.lifecycle.Lifecycle`, `androidx.lifecycle.LifecycleEventObserver`,
   `androidx.lifecycle.compose.LocalLifecycleOwner`.

No string resources changed.

## Verification

- `./gradlew assembleDebug` completed successfully (no compile errors).
