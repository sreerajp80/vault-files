# Permissions: correct displayed status + request on first launch

Implements plan `plans/20260626_203249_permissions-status-and-first-launch-request.md`.

## Changes

### Issue 1 — Permissions screen showed storage as "not granted"
- `PermissionsScreen.kt`: `resolveGranted()` now treats `READ_EXTERNAL_STORAGE` and
  `WRITE_EXTERNAL_STORAGE` as granted when All-files access is held
  (`Build.VERSION.SDK_INT >= R && Environment.isExternalStorageManager()`), falling back
  to the manifest flag otherwise. This matches `hasAllFilesPermission()` and stops the
  screen reporting storage as denied on Android 11+ when the app actually has full access.

### Issue 2 — request storage permission on first launch
- `StorageRepository.kt`: added `isStoragePermissionRequested()` /
  `saveStoragePermissionRequested(Boolean)` k/v helpers (key `storage_permission_requested`,
  default `false`).
- `StorageViewModel.kt`: added a nullable `storagePermissionRequested` StateFlow
  (null until loaded, to avoid firing on the default value), loaded it in the init sync
  block, and added `markStoragePermissionRequested()`.
- `MainActivity.kt`: inside the unlocked branch, a `LaunchedEffect` fires once when the
  flag is `false` and `!hasAllFilesPermission(context)`: it marks the flag and triggers
  the request — on R+ it opens the system All-files-access settings screen (with a
  fallback to the generic settings action), and pre-R it launches the runtime
  `READ_EXTERNAL_STORAGE` dialog. Added the supporting imports.

## Notes
- Gated on unlock so the prompt never appears over the app-lock screen, and on a
  persisted flag so it only fires on the first launch; the in-app Files-tab grant button
  remains for later manual requests.
- No manifest changes.
- Verified with `./gradlew compileDebugKotlin` — BUILD SUCCESSFUL.
