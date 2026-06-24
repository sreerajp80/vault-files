# Plan: Secure Note — bottom-sheet dialog + real at-rest encryption

Date: 2026-06-23 07:44:31 (local)

## Context / what the user asked

On the "New Secure Notes" creation flow the user wants two changes:

1. **UI** — the dialog should slide up **from the bottom** and occupy **~75% of the screen
   height** (a modal bottom sheet), instead of the current centered `AlertDialog`.
2. **Security** — the note must be **stored encrypted** at rest. Today it is written as a
   plain-text `.txt` file, so the "Secure" label is cosmetic — "otherwise no security."

## Current behavior (verified)

- The dialog is a centered `AlertDialog` in
  [FileExplorerScreen.kt](app/src/main/java/com/example/ui/FileExplorerScreen.kt#L737-L782)
  (`showCreateFileDialog`).
- Confirm calls `viewModel.createTextFile(name, content)`
  ([StorageViewModel.kt:258](app/src/main/java/com/example/ui/StorageViewModel.kt#L258)) →
  `repository.createNewTextFile(...)`
  ([StorageRepository.kt:346](app/src/main/java/com/example/data/StorageRepository.kt#L346)),
  which writes **raw UTF-8 bytes** to `<filesDir>/Storage/<current dir>/<name>.txt`.
- There is **no encryption anywhere in the app** — even `moveFileToVault`
  ([StorageRepository.kt:411](app/src/main/java/com/example/data/StorageRepository.kt#L411))
  just copies bytes to a `.secured` file. So encryption must be built, not reused.
- Tapping a non-zip file only shows a toast
  ([FileExplorerScreen.kt:626](app/src/main/java/com/example/ui/FileExplorerScreen.kt#L626));
  notes are **never read back in-app**. So encrypting on write breaks no existing read path.

## Design decisions

- **Encryption**: dependency-free, real, standard Android.
  - New `CryptoManager` (in `com.example.data`) backed by the **Android Keystore**
    (`AndroidKeyStore`). It lazily creates a non-exportable **AES-256** key
    (`KeyGenParameterSpec`, `ENCRYPT|DECRYPT`, `BLOCK_MODE_GCM`, `ENCRYPTION_PADDING_NONE`).
  - Encrypt with **AES/GCM/NoPadding** (128-bit tag). File layout: `[1-byte IV length][IV][ciphertext]`.
  - Key never leaves the Keystore (hardware-backed where available); ciphertext at rest is
    unreadable without the device + key. This is genuine at-rest security.
- **File naming/extension**: store the encrypted note as `<name>.securenote` (not `.txt`),
  since the bytes are ciphertext and labeling them `.txt` would misrepresent them and let
  other apps try to open garbage. The category mapping treats unknown extensions as "Others",
  which is fine.
- **Read-back is out of scope** (the app has no in-app text viewer today). I will add a
  `decryptTextFile` helper to `CryptoManager` so a future "view note" feature is trivial, but
  no new viewer UI in this change. Calling this out so it is an explicit, approved boundary.
- **UI**: use Material3 `ModalBottomSheet` (already on the classpath via
  `androidx.compose.material3`). Sheet content `Column` uses `Modifier.fillMaxHeight(0.75f)`
  to hit the ~75% target; `rememberModalBottomSheetState()` so it animates up from the bottom.
  Keep the same two fields (filename, content), Save and Cancel actions, and all existing
  `testTag`s (`note_filename_field`, `note_content_field`, `confirm_create_note_btn`) so tests
  keep working.

## Files to change

1. **`app/src/main/java/com/example/data/CryptoManager.kt`** *(new)*
   - Keystore-backed AES-256-GCM `encrypt(ByteArray): ByteArray` / `decrypt(ByteArray): ByteArray`.

2. **`app/src/main/java/com/example/data/StorageRepository.kt`**
   - Add `private val cryptoManager = CryptoManager()` (or inject).
   - Add `createEncryptedTextFile(parentDir, name, content)`: ensures `.securenote` extension,
     writes `cryptoManager.encrypt(content.toByteArray())`. Returns `Boolean` like the existing
     method. Keep `createNewTextFile` for now (unused by the note flow) or remove if nothing
     else calls it — will verify references before deciding.

3. **`app/src/main/java/com/example/ui/StorageViewModel.kt`**
   - Point `createTextFile(...)` at `repository.createEncryptedTextFile(...)` (keep the same
     success/failure messaging and refresh calls).

4. **`app/src/main/java/com/example/ui/FileExplorerScreen.kt`**
   - Replace the `AlertDialog` (lines ~737-782) with a `ModalBottomSheet` (75% height, fields,
     Save/Cancel, same testTags). Add `@OptIn(ExperimentalMaterial3Api::class)` if needed and
     any new imports (`ModalBottomSheet`, `rememberModalBottomSheetState`).

5. **`app/src/main/res/values/strings.xml`** and **`app/src/main/res/values-ml/strings.xml`**
   - Update `dialog_filename_label` (drop the "(.txt)" hint, or change to ".securenote") and
     keep both locales in sync. No functional strings removed.

## Risks / notes

- Keystore key generation requires an API level already satisfied by the project's `minSdk`
  (AES/GCM in Keystore is API 23+); will confirm `minSdk` in `build.gradle.kts` before coding.
- Existing instrumented/Robolectric tests reference the testTags above — preserved on purpose.
- No new Gradle dependency required.

## Open question for you

- **Extension**: OK to store encrypted notes as `<name>.securenote`? (Alternative: keep `.txt`
  for familiarity even though contents are ciphertext.) Default in this plan: `.securenote`.

---
Per the workflow rules I will not touch any project file until you approve. **Do you approve this plan?**
