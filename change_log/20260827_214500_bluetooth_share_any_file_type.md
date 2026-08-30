# Bluetooth now appears in the share sheet for every file type

Implements `plans/20260827_212800_bluetooth_share_any_file_type.md`.

## What was wrong

Sharing an APK (and many other types: 7z, rar, epub, …) did not show "Bluetooth" in the share
sheet. Android's Bluetooth send activity declares a fixed allow-list of MIME types in its intent
filter, and `application/vnd.android.package-archive` is not on it, so the system chooser dropped
Bluetooth. Verified on a Motorola device running Android 17 by dumping the filter and by running
`cmd package query-activities`. Launching the Bluetooth activity explicitly with the APK type does
open the device picker, so the Bluetooth transfer code itself was never the problem.

## What changed

**New: `app/src/main/java/in/sreerajp/vault_files/ui/ShareSupport.kt`**
- `mimeTypeForName()` — MIME lookup: platform `MimeTypeMap` first, then a small table for
  extensions it can miss (`apk`, `7z`, `rar`, `epub`, `heic`, `mkv`, …), then a wildcard.
- `shareTypeFor()` — the type for a share intent. One file keeps its own type; many files keep the
  shared family (`image/*`) when they agree, wildcard when mixed.
- `bluetoothInitialIntents()` — returns an explicit Bluetooth send intent, but only when Bluetooth
  does not already resolve for the share type (so the sheet never shows two Bluetooth rows). An
  explicit component skips intent-filter matching, which is what makes any file type work. It also
  grants the Bluetooth package read access to the shared URIs and attaches matching `ClipData`.

**`app/src/main/java/in/sreerajp/vault_files/ui/FileExplorerScreen.kt`**
- `mimeTypeOf()` now delegates to `mimeTypeForName()`. It keeps the wildcard fallback, because it
  also feeds "Open with", where a wildcard matches the most apps.
- `shareItems()` uses `shareTypeFor()` for multi-file shares instead of a hardcoded wildcard, and
  passes the Bluetooth entry to the chooser through `Intent.EXTRA_INITIAL_INTENTS`.

**`app/src/main/AndroidManifest.xml`**
- Added a `<queries>` block for `SEND` / `SEND_MULTIPLE` with `*/*`. On API 30+ the app cannot see
  the Bluetooth activity without it. Confirmed on device that `com.android.bluetooth` is now in the
  app's visible set.

**New tests**
- `app/src/test/java/in/sreerajp/vault_files/ShareSupportTest.kt` — MIME resolution and share-type
  rules (APK, unknown extension, no extension, one-family batch, mixed batch).
- `app/src/test/java/in/sreerajp/vault_files/BluetoothShareIntentTest.kt` — with a stand-in
  Bluetooth activity that accepts only `image/*` and `application/pdf`: an explicit entry is added
  for an APK and not added for types Bluetooth already lists.

## Notes

- The second test caught a real bug during development: the first version built the `ClipData` with
  `ClipData.newUri()`, which queries the content resolver and can throw — that would have aborted
  the whole share. It now builds the `ClipData` from the intent's own type, with no resolver call.
- If no Bluetooth send activity exists (no Bluetooth hardware, package hidden), the helper returns
  nothing and the share sheet behaves exactly as before.

## Verification

- `./gradlew testDebugUnitTest` — all tests pass.
- `./gradlew installDebug` — installed on the connected device.
