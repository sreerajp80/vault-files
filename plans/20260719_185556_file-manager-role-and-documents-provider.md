# Make Vault Files a proper file-manager app (narrow VIEW filter + add DocumentsProvider)

**Status:** completed

## What the issue is

Two separate problems, both about how the app advertises itself to Android.

### Issue 1 — the app claims it can open every file type

`app/src/main/AndroidManifest.xml` (lines 55-62) has:

```xml
<intent-filter>
    <action android:name="android.intent.action.VIEW" />
    <category android:name="android.intent.category.DEFAULT" />
    <data android:scheme="file" />
    <data android:scheme="content" />
    <data android:mimeType="*/*" />
</intent-filter>
```

`*/*` matches everything, so Android offers Vault Files as a *viewer* for APKs, PDFs,
videos, ZIPs — file types the app cannot actually show. When the user double-taps an APK
inside the app, `openFileExternally` fires `ACTION_VIEW` with the APK mime type, and the
chooser lists both the Package Installer and Vault Files itself.

Picking Vault Files then runs `MainActivity.handleIntent` -> `ACTION_VIEW` ->
`StorageViewModel.previewExternalContentUri`. An APK is not an image, not text and not a
`.securenote`, so it falls through to the last branch
(`StorageViewModel.kt:871`) which calls `setPendingSharedImports` — the "copy this file
into the app" flow. That is why the app asks "where do you want to save this".

Real file managers (Files by Google, Solid Explorer, Total Commander) do not register
`VIEW` for `*/*`. Removing it does **not** weaken the file-manager role: folder browsing
comes from the directory-mime `VIEW` filter, and "other apps can pick files from me"
comes from `GET_CONTENT` / `OPEN_DOCUMENT`. Both stay untouched.

### Issue 2 — the app is not a storage source in the system file picker

To behave like a real "Files" app, other apps should be able to see Vault Files storage
*inside* the system document picker (the left-hand drawer of the SAF picker), not only as
a separate app in a chooser. That needs a `DocumentsProvider`, which the project does not
have. Today only `GET_CONTENT` / `OPEN_DOCUMENT` activity filters exist, which show the
app as a whole-app choice, not as a browsable storage root.

## Files to be changed

**Phase 1 — narrow the VIEW filter**

1. `app/src/main/AndroidManifest.xml` — replace the `VIEW` + `*/*` filter with concrete
   mime types the app can really preview.

**Phase 2 — add a DocumentsProvider**

2. `app/src/main/java/in/sreerajp/vault_files/data/VaultDocumentsProvider.kt` — **new
   file**, the provider implementation.
3. `app/src/main/AndroidManifest.xml` — register the provider.
4. `app/src/main/res/values/strings.xml` — root title / summary strings.
5. `app/src/main/res/drawable/` — a small root icon, only if no existing drawable suits
   (otherwise reuse `@mipmap/ic_launcher`).
6. `docs/architecture.md` — short note describing the new provider layer.

## The plan for the fix

### Phase 1 — narrow the VIEW filter

Replace the catch-all filter with the types the app genuinely handles:

```xml
<!-- Handle VIEW only for content the app can actually preview. -->
<intent-filter>
    <action android:name="android.intent.action.VIEW" />
    <category android:name="android.intent.category.DEFAULT" />
    <data android:scheme="file" />
    <data android:scheme="content" />
    <data android:mimeType="image/*" />
    <data android:mimeType="text/*" />
</intent-filter>
```

Plus a separate filter for `.securenote` files, matched by file-name pattern (this only
works reliably for `file://` URIs, which is a known Android limit — acceptable, because
notes are normally opened from inside the app, not from a chooser):

```xml
<intent-filter>
    <action android:name="android.intent.action.VIEW" />
    <category android:name="android.intent.category.DEFAULT" />
    <data android:scheme="file" />
    <data android:host="*" />
    <data android:pathPattern=".*\\.securenote" />
</intent-filter>
```

The directory `VIEW` filter, the `SEND` / `SEND_MULTIPLE` filters and the
`GET_CONTENT` / `OPEN_DOCUMENT` filters are **left exactly as they are**.

Effect: the APK chooser shows only the Package Installer. The app stops appearing as a
viewer for every unrelated file type on the device. Nothing about browsing or picking
changes.

No Kotlin change is needed for this phase — `previewExternalContentUri` keeps its
import fallback, which is still correct for the share-sheet path.

### Phase 2 — add the DocumentsProvider

Create `VaultDocumentsProvider : android.provider.DocumentsProvider`.

**Roots** (`queryRoots`): one root, "Vault Files", backed by
`StorageRepository.userStorageRoot` (`filesDir/Storage`). Flags:
`FLAG_SUPPORTS_CREATE`, `FLAG_SUPPORTS_IS_CHILD`, `FLAG_LOCAL_ONLY`, and
`FLAG_SUPPORTS_SEARCH` if search is added later. Report free space via
`COLUMN_AVAILABLE_BYTES`.

Deliberately **not** exposed as roots:
- the vault (`filesDir/Vault`) — encrypted/private by design,
- any path listed in the secured-folders table — those are password/biometric gated and
  must never be readable through SAF, which has no way to prompt for the app's unlock.

A helper `isExposable(file: File): Boolean` will do this check and be applied in
`queryDocument`, `queryChildDocuments` and `openDocument`, so a caller cannot reach a
protected path even by guessing a document id.

**Document ids**: the file path relative to the root, e.g. `Storage/Photos/a.jpg`, with
the root id as prefix. Two small helpers convert id -> `File` and `File` -> id, rejecting
any id that escapes the root after canonicalisation (`..` traversal guard).

**Methods to implement**:
- `queryRoots` — the single root row.
- `queryDocument` — one row for a given id (name, mime, size, last modified, flags).
- `queryChildDocuments` — directory listing, honouring `isExposable`.
- `openDocument` — `ParcelFileDescriptor.open` in read or write mode.
- `openDocumentThumbnail` — image thumbnails, so the picker shows previews.
- `createDocument`, `deleteDocument`, `renameDocument` — write support, so other apps can
  save into Vault Files.
- `isChildDocument` — required for move/copy in the picker.

**Manifest registration**:

```xml
<provider
    android:name=".data.VaultDocumentsProvider"
    android:authorities="${applicationId}.documents"
    android:exported="true"
    android:grantUriPermissions="true"
    android:permission="android.permission.MANAGE_DOCUMENTS">
    <intent-filter>
        <action android:name="android.content.action.DOCUMENTS_PROVIDER" />
    </intent-filter>
</provider>
```

The authority `${applicationId}.documents` is kept distinct from the existing
`${applicationId}.fileprovider`.

**Refresh**: after the app changes files, call
`contentResolver.notifyChange(DocumentsContract.buildRootsUri(authority), null)` so the
picker does not show stale contents. This will be a single small helper called from the
repository's write paths.

**Threading**: provider callbacks run on binder threads, so all file I/O stays synchronous
inside the provider and must not touch `StorageViewModel` or any coroutine scope.

### Verification

- `./gradlew assembleDebug` builds.
- `./gradlew testDebugUnitTest` still passes.
- Manual: double-tap an APK in the explorer -> installer opens directly, no chooser, no
  "where to save" prompt.
- Manual: in another app (e.g. Gmail attach, or a browser download), the system picker
  shows "Vault Files" as a source in the drawer, and browsing/opening/saving works.
- Manual: the vault and any secured folder do **not** appear in the picker.

### Order of work

Phase 1 first (small, fixes the reported bug), then Phase 2. They are independent, so
Phase 1 can ship on its own if Phase 2 needs more time.

## Risks

- `pathPattern` matching for `.securenote` does not apply to `content://` URIs. If notes
  must be openable from other apps, a custom mime type registered by the provider is the
  proper long-term answer.
- `MANAGE_DOCUMENTS` is a system-level permission; declaring `android:permission` on the
  provider is the standard, correct pattern here (it restricts direct access to the system
  picker, while normal apps reach the files through granted URIs).
- Exposing writable storage over SAF widens the app's attack surface; the `isExposable`
  guard is the main protection and must be applied on every entry point.
