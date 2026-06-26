# Permissions: correct displayed status + request on first launch

Two related permission issues. Both stem from the app's real storage permission on
Android 11+ (R+) being **MANAGE_EXTERNAL_STORAGE** ("All files access"), while the
legacy `READ_EXTERNAL_STORAGE` / `WRITE_EXTERNAL_STORAGE` runtime permissions are never
granted there (`WRITE` is `maxSdkVersion=29`; `READ` is deprecated on 13+).

## Issue 1 — Permissions screen shows storage as "not granted"

In the Settings → Permissions screen, `READ_EXTERNAL_STORAGE` (and `WRITE_EXTERNAL_STORAGE`)
show as denied even when `MANAGE_EXTERNAL_STORAGE` is granted, which is misleading — with
All-files access the app already has full storage access. `hasAllFilesPermission()` uses
exactly this (`Environment.isExternalStorageManager()` on R+), so the screen disagrees
with the app's actual capability.

Cause: `resolveGranted()` in `PermissionsScreen.kt` falls back to the manifest
`REQUESTED_PERMISSION_GRANTED` flag for READ/WRITE, which is false on R+.

### Fix
In `resolveGranted()`, treat `READ_EXTERNAL_STORAGE` and `WRITE_EXTERNAL_STORAGE` as
granted when All-files access is held:

```
"android.permission.READ_EXTERNAL_STORAGE",
"android.permission.WRITE_EXTERNAL_STORAGE" ->
    (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R && Environment.isExternalStorageManager()) || flagGranted
```

Pre-R behavior is unchanged (still reflects the real runtime grant).

## Issue 2 — Request permission on first launch

Today the app only prompts for storage access when the user taps the button on the
Files-tab permission state. It should proactively request on the first launch.

### Fix
Add a one-time auto-request, gated by a persisted flag so it fires only once (the
in-app button remains for later manual requests):

- **Repository** (`StorageRepository.kt`): add, following the existing k/v pattern,
  `isStoragePermissionRequested()` and `saveStoragePermissionRequested(Boolean)` for
  key `storage_permission_requested` (default `false`).
- **ViewModel** (`StorageViewModel.kt`): load the flag in the init sync block, expose a
  `storagePermissionRequested` StateFlow, and add `markStoragePermissionRequested()`.
- **MainActivity** (`MainActivity.kt`): inside the unlocked branch, a `LaunchedEffect`
  that, when `!hasAllFilesPermission(context)` and the flag is false, triggers the
  request and then marks the flag:
  - R+ → start `Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION` for our package
    (same intent the Files-tab button uses).
  - pre-R → launch a `RequestPermission()` contract for `READ_EXTERNAL_STORAGE`.
  - Gating on unlock ensures we don't prompt over the app-lock screen.

## Files to change

- `app/src/main/java/com/example/ui/PermissionsScreen.kt` — Issue 1 (`resolveGranted`).
- `app/src/main/java/com/example/data/StorageRepository.kt` — Issue 2 (new k/v getter/setter).
- `app/src/main/java/com/example/ui/StorageViewModel.kt` — Issue 2 (state + load + setter).
- `app/src/main/java/com/example/MainActivity.kt` — Issue 2 (first-launch request effect).

## Notes / open questions

- **Auto-launch aggressiveness**: on R+ the first-launch request sends the user straight
  to the system All-files-access settings screen (no in-app dialog is possible for this
  permission). This matches how most file managers behave. Acceptable, or would you
  prefer an in-app explainer/dialog first with a "Continue" button before redirecting?
- No manifest changes are needed for either issue.
