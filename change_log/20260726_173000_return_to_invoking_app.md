# Change Log: Return to Invoking App on Share Finish / Cancel

**Plan Reference:** `plans/20260726_172826_return_to_invoking_app.md`

## Summary of Changes
- Added an optional `onComplete: (() -> Unit)? = null` callback parameter to `importSharedToFolder` and `importSharedToVault` in `StorageViewModel.kt`.
- Updated `MainActivity.kt` to invoke `finish()` after importing shared files completes when the user taps **OK / Save**.
- Updated `MainActivity.kt` to invoke `finish()` immediately when the user taps **Cancel** or presses Back while in pending share mode.
- Verified build succeeds cleanly with `./gradlew assembleDebug`.
