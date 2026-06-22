# Change log: Add a close button to snackbar notifications

Implements plan `plans/20260621_143000_snackbar-dismiss-action.md`.

## What changed
- `app/src/main/java/com/example/MainActivity.kt`: Updated the `showSnackbar` call in
  the `userMessage` collection `LaunchedEffect` to pass `withDismissAction = true`.
  Material3's default `SnackbarHost`/`Snackbar` now renders a close (X) icon button,
  letting the user dismiss feedback messages immediately instead of waiting for the
  timeout.

## Notes
- One-line behavioral change; no new imports, no other files affected, no test changes.
