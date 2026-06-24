# Change log: Secure Note viewer (decrypt & read encrypted notes)

Date: 2026-06-23 07:53:39 (local)
Implements: [plans/20260623_074950_secure-note-viewer.md](../plans/20260623_074950_secure-note-viewer.md)

## What changed

### 1. Decrypt-and-read in the repository
- `StorageRepository.kt`: added `suspend fun readEncryptedNote(file: File): String?` — reads the
  file bytes, decrypts via `CryptoManager.decrypt`, returns UTF-8 text; returns `null` on any
  failure (missing/corrupt file or a note sealed with a different device's Keystore key).

### 2. ViewModel state for the open note
- `StorageViewModel.kt`:
  - New top-level `data class OpenNote(val name: String, val content: String)`.
  - `_openNote` / `openNote` StateFlow holding the currently viewed note (null when none).
  - `openNote(item: FileItem)`: IO read+decrypt; sets state on success, else dispatches the new
    `msg_note_open_failed` message.
  - `closeNote()`: clears the open note.

### 3. Read-only viewer UI
- `FileExplorerScreen.kt`:
  - `onItemClick` now routes `.securenote` taps to `viewModel.openNote(item)` (other types,
    folders, and secured-folder gating unchanged; legacy `.txt` notes still show the toast).
  - Collects `openNote`; when non-null, renders a read-only `ModalBottomSheet` (~75% height,
    matching the creator sheet) showing the note name as title and the decrypted content in a
    scrollable `SelectionContainer`, with a Close button (animated dismiss → `closeNote()`).
  - Added imports: `rememberScrollState`, `verticalScroll`, `text.selection.SelectionContainer`.

### 4. Strings
- `values/strings.xml` and `values-ml/strings.xml`: added `msg_note_open_failed`
  ("Couldn't open this note." / Malayalam). Reused existing `action_close`.

## Scope / notes
- Read-only: no edit-and-resave (possible follow-up).
- Device-bound key means a note from another install won't decrypt → handled via the failure
  message.
- No new Gradle dependency.

## Verification
- `./gradlew compileDebugKotlin` → BUILD SUCCESS.
