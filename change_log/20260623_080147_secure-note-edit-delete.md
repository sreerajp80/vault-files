# Change log: Edit / update / delete a secure note

Date: 2026-06-23 08:01:47 (local)
Implements: [plans/20260623_075825_secure-note-edit-delete.md](../plans/20260623_075825_secure-note-edit-delete.md)

## What changed

### 1. Overwrite (re-encrypt) in the repository
- `StorageRepository.kt`: added `suspend fun overwriteEncryptedNote(file: File, content: String): Boolean`
  — re-encrypts `content` with `CryptoManager` and overwrites the existing `.securenote` in place.

### 2. ViewModel
- `OpenNote` now carries `file: File` (populated from `item.file` in `openNote`) so edits/deletes
  know the backing file.
- `saveNoteEdits(newContent)`: overwrites the file, updates `_openNote` content, refreshes the
  listing/stats, and dispatches `msg_note_updated` / `msg_note_update_failed`.
- `deleteOpenNoteFile(file, name)`: deletes the note's file and dispatches the existing
  `msg_deleted` / `msg_delete_failed` (takes file+name directly because the viewer closes — which
  clears `openNote` — before the optional phone-lock confirmation runs).

### 3. Viewer becomes a view + edit sheet
- `FileExplorerScreen.kt`: the secure-note `ModalBottomSheet` now has two modes:
  - **Read mode**: scrollable/selectable decrypted text + **Delete**, **Close**, **Edit** buttons.
  - **Edit mode**: editable `OutlinedTextField` + **Cancel** / **Update** (calls `saveNoteEdits`).
  - **Delete** reuses the same gated flow as the row ⋮ menu (respects `isPhoneLockDeleteEnabled`
    → `activeActionPendingValidation`), then removes the file via `deleteOpenNoteFile`.
  - Added test tags `note_edit_field`, `note_save_edit_btn`, `note_edit_btn`.

### 4. Strings (both `values` and `values-ml`)
- Added `action_edit`, `action_update`, `action_delete`, `msg_note_updated`,
  `msg_note_update_failed`. Reused existing `action_close`, `action_cancel`,
  `confirm_delete_*`, `msg_deleted`, `msg_delete_failed`.

## Notes / scope
- Edits the note **content** (body). **Rename is out of scope** — the filename is unchanged.
- Delete already worked from the row ⋮ menu before this change; it is now also available inside
  the viewer, using the same confirmation/gating.
- No new Gradle dependency.

## Verification
- `./gradlew compileDebugKotlin` → BUILD SUCCESS.
