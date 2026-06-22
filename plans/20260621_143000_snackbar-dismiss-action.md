# Plan: Add a close button to snackbar notifications

## Issue
The app shows user feedback messages (e.g. "Switched storage source to App Sandbox")
as a Material3 Snackbar. The snackbar only disappears on its own timeout — there is no
way for the user to dismiss it quickly. We want a close (X) button on the snackbar.

## Files to change
- `app/src/main/java/com/example/MainActivity.kt`

## Plan for the fix
Material3's `Snackbar` natively renders a dismiss (X) icon button when the snackbar's
visuals have `withDismissAction = true`. The default `SnackbarHost` already wires this
up, so no custom snackbar composable is needed.

Change the `showSnackbar` call (currently `snackbarHostState.showSnackbar(message)`) to
pass the dismiss action flag:

```kotlin
snackbarHostState.showSnackbar(
    message = message,
    withDismissAction = true
)
```

Tapping the X invokes `SnackbarData.dismiss()`, removing the snackbar immediately.

No other files, imports, or tests are affected.
