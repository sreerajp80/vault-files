# Bluetooth missing from the share sheet for many file types

**Status:** completed

## What the issue is

Sharing an APK from the file explorer does not show "Bluetooth" in the share sheet.
The same happens for many other file types (7z, rar, epub, apk, most "application/*" types).

### Root cause (verified on a real device: Motorola, Android 17)

`shareItems()` in `app/src/main/java/in/sreerajp/vault_files/ui/FileExplorerScreen.kt`
sets the intent type from `mimeTypeOf()`, which asks the platform `MimeTypeMap`.
For an APK that returns `application/vnd.android.package-archive`.

Android's Bluetooth send activity (`com.android.bluetooth.opp.BluetoothOppLauncherActivity`)
declares a **fixed allow-list** of MIME types in its intent filter. Dumped from the device:

```
ACTION_SEND: image/*, video/*, audio/*, text/x-vcard, text/x-vcalendar, text/calendar,
             text/plain, text/html, text/xml, application/zip, application/vnd.ms-excel,
             application/msword, application/vnd.ms-powerpoint, application/pdf,
             application/vnd.openxmlformats-officedocument.{spreadsheetml.sheet,
             wordprocessingml.document, presentationml.presentation}, application/x-hwp
ACTION_SEND_MULTIPLE: image/*, video/*, audio/*, x-mixmedia, text/x-vcard
```

There is no `application/vnd.android.package-archive` in that list, so the system
chooser drops Bluetooth. Confirmed with:

```
cmd package query-activities -a android.intent.action.SEND -t application/vnd.android.package-archive   -> no bluetooth
cmd package query-activities -a android.intent.action.SEND -t '*/*'                                     -> bluetooth present
```

(An intent type of `*/*` matches every filter, which is why other apps that share with
`*/*` still show Bluetooth. Our multi-select path already uses `*/*`, so it is only the
single-file path with a "known but not allow-listed" type that loses Bluetooth. Multi-select
with a shared concrete type would hit the same problem once we improve type detection.)

### Why we cannot fix it by changing the MIME type

Sending the wrong type (`*/*` or `application/octet-stream`) just to please Bluetooth would
hurt every other target in the sheet: apps that filter on the real type would rank worse or
receive a file they cannot interpret. The type we send should stay correct.

### The approach that does work

An **explicit** intent (component set) skips intent-filter matching entirely. Verified on the
device: launching `com.android.bluetooth/.opp.BluetoothOppLauncherActivity` directly with
`type=application/vnd.android.package-archive` opened the Bluetooth device picker normally.
So the OPP transfer code itself has no problem with APKs.

`Intent.createChooser` supports `EXTRA_INITIAL_INTENTS`, which adds extra entries to the top
of the share sheet. We can add a Bluetooth entry there whenever the real MIME type would
otherwise hide it.

## Files to be changed

1. **`app/src/main/java/in/sreerajp/vault_files/ui/ShareSupport.kt`** (new)
   - `mimeTypeForName(name, fallback)` — MIME lookup with a small table for extensions the
     platform `MimeTypeMap` does not know (`7z`, `rar`, `apk` on old devices, `md`, `log`,
     `json`, `epub`, `heic`, `webp` fallbacks, …), then `MimeTypeMap`, then the fallback.
   - `shareTypeFor(names)` — the type for a share intent: the single file's type, or for many
     files the shared `type/*` prefix when they agree, else `*/*`.
   - `bluetoothInitialIntents(context, base, uris)` — returns an explicit Bluetooth intent to
     put in `EXTRA_INITIAL_INTENTS`, but only when Bluetooth does **not** already resolve for
     the share type (so we never show two Bluetooth rows). It:
     - finds the Bluetooth send activity by querying `ACTION_SEND` with `image/*` and keeping
       the match whose package is the Bluetooth package,
     - copies the share intent, sets that component,
     - grants read access on every URI to the Bluetooth package explicitly, and sets
       `ClipData`, so OPP can read our `FileProvider` URIs.

2. **`app/src/main/java/in/sreerajp/vault_files/ui/FileExplorerScreen.kt`**
   - `mimeTypeOf()` delegates to `mimeTypeForName(..., fallback = "*/*")`. Keeping the `*/*`
     fallback here matters: this helper also feeds `ACTION_VIEW` ("Open with"), where `*/*`
     finds the most apps.
   - `shareItems()` uses `shareTypeFor()` for the multi-file type instead of a hardcoded
     `*/*`, and adds `EXTRA_INITIAL_INTENTS` from `bluetoothInitialIntents(...)`.

3. **`app/src/main/AndroidManifest.xml`**
   - Add a `<queries>` block for `ACTION_SEND` with `*/*`. On API 30+ `queryIntentActivities`
     only sees packages we are allowed to see, so without this we cannot find the Bluetooth
     activity to build the explicit intent.

4. **`app/src/test/java/in/sreerajp/vault_files/ShareSupportTest.kt`** (new)
   - Unit tests for `mimeTypeForName` (apk, unknown extension, no extension) and
     `shareTypeFor` (all images -> `image/*`, mixed -> `*/*`).

## Plan for the fix

1. Add `ShareSupport.kt` with the three helpers above.
2. Point `mimeTypeOf()` at the new helper and rework `shareItems()` to use the share type and
   the Bluetooth initial intent.
3. Add the `<queries>` block to the manifest.
4. Add the unit tests and run `./gradlew testDebugUnitTest`.
5. Install on the connected device and confirm Bluetooth now appears when sharing an APK.

## Risk / notes

- If the Bluetooth activity cannot be found (no Bluetooth hardware, package hidden), the
  helper returns nothing and the share sheet behaves exactly as before.
- The explicit grant to the Bluetooth package is read-only and scoped to the shared URIs.
