# Plan: Stop showing every user message twice (Toast + Snackbar)

## Issue

Each one-shot user feedback message (e.g. "Switched storage source to Device Storage")
appears twice on screen: once as an Android `Toast` (bottom rounded pill with the app
icon) and once as a Compose `Snackbar` (top bar).

The `StorageViewModel` emits the message only once via
`dispatchMessage` → `_userMessage.emit(msg)`. The duplication is purely in the UI
collector in `MainActivity`, which deliberately renders each message as both a Toast and
a Snackbar.

Source: `app/src/main/java/com/example/MainActivity.kt`, lines ~64–70:

```kotlin
// Collect user feedback messages from flow and display as Toast + Snackbar
LaunchedEffect(viewModel.userMessage) {
    viewModel.userMessage.collectLatest { message ->
        Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
        snackbarHostState.showSnackbar(message)
    }
}
```

## Decision

Keep the **Snackbar** only (in-app, themed, auto-dismissing, integrated with the
`Scaffold`). Remove the `Toast`.

## Files to be changed

- `app/src/main/java/com/example/MainActivity.kt`
  - Remove the `Toast.makeText(context, message, Toast.LENGTH_SHORT).show()` line from
    the `userMessage` collector, keeping only `snackbarHostState.showSnackbar(message)`.
  - Update the comment ("display as Toast + Snackbar" → "display as Snackbar").
  - Remove the now-unused `android.widget.Toast` import (and the `LocalContext`/`context`
    val if it becomes unused — verify it isn't referenced elsewhere before removing).

## Plan for the fix

1. Edit the collector so only the Snackbar is shown.
2. Clean up the comment and the unused `Toast` import.
3. Check whether `context` (`LocalContext.current`) is still used elsewhere in the
   composable; only remove it if it has no remaining references.
4. Build (`./gradlew assembleDebug`) to confirm no unused-import/compile issues.

## Out of scope

- No change to `StorageViewModel` / the `userMessage` SharedFlow — it already emits once.
