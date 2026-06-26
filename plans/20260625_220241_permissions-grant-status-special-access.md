# Fix incorrect grant status for special-access permissions on the App Permissions screen

## Issue

The in-app **App Permissions** screen (`PermissionsScreen.kt`) shows the wrong
granted/denied state for *special-access (appop)* permissions. It always reports
`MANAGE_EXTERNAL_STORAGE` and `REQUEST_INSTALL_PACKAGES` as **denied**, even when the
user has actually enabled "All files access" / "Install unknown apps" for the app.

### Root cause

The screen computes grant state from the package manifest flag:

```kotlin
val granted = i < flags.size &&
    (flags[i] and PackageInfo.REQUESTED_PERMISSION_GRANTED) != 0
```

`REQUESTED_PERMISSION_GRANTED` only tracks **normal runtime + install-time**
permissions. It does **not** reflect special-access (appop) permissions, whose real
state lives behind dedicated APIs:

| Permission | Correct grant check |
|---|---|
| `MANAGE_EXTERNAL_STORAGE` | `Environment.isExternalStorageManager()` (API 30+) |
| `REQUEST_INSTALL_PACKAGES` | `PackageManager.canRequestPackageInstalls()` (API 26+) |
| `READ_EXTERNAL_STORAGE` | flag is correct (equals runtime check); non-functional on API 33+ anyway — leave as-is |

Library permissions (`USE_BIOMETRIC`, `USE_FINGERPRINT`) correctly show "granted"
only because they are normal install-time permissions that the flag does track.

## Files to change

- `app/src/main/java/com/example/ui/PermissionsScreen.kt` — only file.

## Plan for the fix

1. Add a helper that resolves the real grant state, special-casing the two appop
   permissions and falling back to the manifest flag for everything else:

   ```kotlin
   private fun resolveGranted(
       context: android.content.Context,
       permission: String,
       flagGranted: Boolean
   ): Boolean = when (permission) {
       "android.permission.MANAGE_EXTERNAL_STORAGE" ->
           Build.VERSION.SDK_INT >= Build.VERSION_CODES.R &&
               Environment.isExternalStorageManager()
       "android.permission.REQUEST_INSTALL_PACKAGES" ->
           Build.VERSION.SDK_INT >= Build.VERSION_CODES.O &&
               context.packageManager.canRequestPackageInstalls()
       else -> flagGranted
   }
   ```

   (Version guards are required because `minSdk = 24`: `canRequestPackageInstalls`
   is API 26+, `isExternalStorageManager` is API 30+.)

2. In the `remember(context)` block, replace the inline `granted = ...` with
   `granted = resolveGranted(context, perm, flagGranted)`, where `flagGranted` is the
   existing flag computation.

3. Add the needed imports: `android.os.Build`, `android.os.Environment`.

4. No string changes — the existing `permissions_status_granted` /
   `permissions_status_denied` labels still apply.

5. **Refresh on resume** (included): the grant state is currently cached via
   `remember(context)`, so it won't update after the user toggles a permission in
   system settings and returns. Add a lifecycle observer that bumps a refresh key on
   `ON_RESUME`, and key the `remember` block on it so the list recomputes:

   ```kotlin
   val lifecycleOwner = LocalLifecycleOwner.current
   var refreshKey by remember { mutableStateOf(0) }
   DisposableEffect(lifecycleOwner) {
       val observer = LifecycleEventObserver { _, event ->
           if (event == Lifecycle.Event.ON_RESUME) refreshKey++
       }
       lifecycleOwner.lifecycle.addObserver(observer)
       onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
   }
   val (explicit, implicit) = remember(context, refreshKey) { /* existing body */ }
   ```

   Additional imports: `androidx.compose.runtime.DisposableEffect`,
   `getValue`/`setValue`/`mutableStateOf`, `androidx.lifecycle.Lifecycle`,
   `androidx.lifecycle.LifecycleEventObserver`, and `LocalLifecycleOwner`.

## Verification

- `./gradlew assembleDebug` compiles.
- Manual: with "All files access" / "Install unknown apps" granted, the two rows
  show "granted"; revoke them and they show "denied".
