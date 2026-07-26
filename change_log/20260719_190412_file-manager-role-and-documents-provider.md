# File-manager role: narrowed VIEW filter + added DocumentsProvider

Implements plan `plans/20260719_185556_file-manager-role-and-documents-provider.md`.

## Why

Two problems with how the app told Android what it can do:

1. A catch-all `VIEW` + `*/*` intent filter made the app a candidate viewer for **every**
   file type. Double-tapping an APK inside the app showed a chooser with both the Package
   Installer and Vault Files itself, and picking Vault Files opened the "where do you want
   to save this" import flow, because an APK is not previewable and fell through to the
   shared-import branch.
2. The app was not a storage source inside the system file picker, so other apps could not
   browse its storage the way they browse Drive or Files.

## What changed

### Phase 1 — narrowed the VIEW filter

`app/src/main/AndroidManifest.xml`

- Replaced the `VIEW` + `*/*` filter with one that lists only `image/*` and `text/*` —
  what the app can really preview.
- Added a second `VIEW` filter matching `.securenote` by `pathPattern` on `file://` URIs.
- Left untouched: the directory-mime `VIEW` filter (folder browsing), `SEND` /
  `SEND_MULTIPLE` (share sheet), and `GET_CONTENT` / `OPEN_DOCUMENT` (file picking). The
  file-manager role is unchanged.

No Kotlin change was needed. `previewExternalContentUri` keeps its import fallback, which
is still right for the share-sheet path.

### Phase 2 — added a DocumentsProvider

`app/src/main/java/in/sreerajp/vault_files/data/VaultDocumentsProvider.kt` (new)

- One root, "Vault Files", backed by `filesDir/Storage`, with create support and reported
  free space.
- Implemented `queryRoots`, `queryDocument`, `queryChildDocuments`, `openDocument`,
  `openDocumentThumbnail` (images only), `isChildDocument`, `createDocument`,
  `deleteDocument`, `renameDocument`.
- Document ids are `vault_files_storage:<path relative to the storage root>`.
- **Security:** `isExposable` hides the vault (`filesDir/Vault`) and anything inside a
  secured folder, and rejects any path that escapes the storage root after
  canonicalisation. It runs on every entry point — `queryDocument`, `queryChildDocuments`
  and `openDocument` all go through `resolveDocumentId`, so a guessed id cannot reach
  protected content. This matters because SAF has no way to show the app's unlock prompt.
- Secured-folder paths are read through Room with `runBlocking` and cached for 2 seconds,
  since provider callbacks run on binder threads and must stay synchronous.
- `createDocument` and `renameDocument` reject names containing path separators;
  `createDocument` appends " (n)" rather than overwriting an existing file.
- Added a `notifyDirectoryChanged` companion helper so the picker refreshes after the app
  changes a folder.

`app/src/main/AndroidManifest.xml`

- Registered the provider with authority `${applicationId}.documents` (kept separate from
  the existing `.fileprovider`), `android:permission="android.permission.MANAGE_DOCUMENTS"`
  and the `DOCUMENTS_PROVIDER` intent filter.

`app/src/main/java/in/sreerajp/vault_files/ui/StorageViewModel.kt`

- `loadFilesInDirectory` now calls `VaultDocumentsProvider.notifyDirectoryChanged`. Every
  write path already funnels through this function, so one call covers them all.

`app/src/main/res/values/strings.xml`

- Added `documents_root_title` and `documents_root_summary` for the picker root. Reused
  `@mipmap/ic_launcher` as the root icon, so no new drawable was needed.

## Verification

- `./gradlew assembleDebug` — passes.
- `./gradlew testDebugUnitTest` — **2 tests fail**, but these failures are **pre-existing**
  and unrelated: `ExampleRobolectricTest` and `GreetingScreenshotTest` both fail with
  `UnsupportedOperationException at DefaultSdkProvider.java:170`. Confirmed by stashing all
  changes and re-running on a clean tree, which fails identically.
- Manual device checks not yet done. Still to confirm on a device:
  - double-tapping an APK goes straight to the installer, with no chooser and no
    "where to save" prompt;
  - "Vault Files" appears as a source in the system file picker, and browse / open / save
    work;
  - the vault and any secured folder do **not** appear in the picker.

## Known limitation

`pathPattern` matching for `.securenote` only applies to `file://` URIs, not `content://`.
Notes opened from other apps over a content URI will not match. Registering a custom mime
type through the provider is the proper long-term fix.

## Docs

`docs/architecture.md` — added a Layers entry for `VaultDocumentsProvider` (what it exposes,
the `isExposable` rule, the binder-thread constraint) plus a note on why the `VIEW` filters
are kept narrow.
