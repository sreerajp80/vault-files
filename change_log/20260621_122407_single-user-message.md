# Change log: Show each user message once (Snackbar only)

Implements `plans/20260621_122010_single-user-message.md`.

## What changed

User feedback messages (e.g. "Switched storage source to Device Storage") were being
displayed twice — once as an Android `Toast` and once as a Compose `Snackbar`. They are
now shown only as a Snackbar.

### `app/src/main/java/com/example/MainActivity.kt`

- Removed the `Toast.makeText(context, message, Toast.LENGTH_SHORT).show()` call from the
  `userMessage` collector, leaving only `snackbarHostState.showSnackbar(message)`.
- Updated the collector comment from "display as Toast + Snackbar" to "display as Snackbar".
- Removed the now-unused `import android.widget.Toast`.
- Removed the now-unused `val context = LocalContext.current` in the top-level composable
  (it was only referenced by the removed Toast). The `LocalContext` import is retained —
  still used by another composable further down the file.

No changes to `StorageViewModel` / the `userMessage` SharedFlow; it already emitted each
message once.

## Verification

- `./gradlew assembleDebug` completed successfully; `app/build/outputs/apk/debug/app-debug.apk`
  rebuilt with no compile or unused-import errors.
