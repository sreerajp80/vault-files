# Change log: Secure Note — bottom-sheet dialog + real at-rest encryption

Date: 2026-06-23 07:48:35 (local)
Implements: [plans/20260623_074431_secure-note-bottomsheet-encrypt.md](../plans/20260623_074431_secure-note-bottomsheet-encrypt.md)

## What changed

### 1. Real at-rest encryption for secure notes
- **New file** `app/src/main/java/com/example/data/CryptoManager.kt`: Android Keystore-backed
  AES-256-GCM encryption. A non-exportable key (`vault_secure_note_key`) lives in
  `AndroidKeyStore`; content is sealed with `AES/GCM/NoPadding`. Blob layout is
  `[1-byte IV length][IV][ciphertext+tag]`. Exposes `encrypt`/`decrypt`.
- `StorageRepository.kt`:
  - Added `private val cryptoManager = CryptoManager()`.
  - Replaced `createNewTextFile(...)` with `createEncryptedNote(...)`, which encrypts the note
    content and writes it with a `.securenote` extension (ciphertext, not plain text).
- `StorageViewModel.createTextFile(...)` now calls `repository.createEncryptedNote(...)`.

### 2. Bottom-sheet UI for the note creator
- `FileExplorerScreen.kt`: replaced the centered `AlertDialog` with a Material3
  `ModalBottomSheet` that slides up from the bottom; the content `Column` uses
  `fillMaxHeight(0.75f)` to occupy ~75% of the screen. Title, the two fields (filename +
  content), and Save/Cancel actions were preserved, along with the existing test tags
  (`note_filename_field`, `note_content_field`, `confirm_create_note_btn`). Dismiss animates
  the sheet out via `sheetState.hide()`. Added `kotlinx.coroutines.launch` import.

### 3. Strings
- `values/strings.xml` and `values-ml/strings.xml`: `dialog_filename_label` hint changed from
  `(.txt)` to `(.securenote)` in both locales.

## Notes / scope
- Notes are write-only in the app today (tapping a file only shows a toast), so encrypting on
  write breaks no existing read path. `CryptoManager.decrypt` was added for a future in-app
  note viewer, but no viewer UI was built in this change.
- No new Gradle dependency. `minSdk = 24` satisfies Keystore AES/GCM (API 23+).

## Verification
- `./gradlew compileDebugKotlin` succeeds (only JVM native-access warnings, no errors).
- No unit/instrumented tests reference the renamed method or the note dialog tags.
