# Plan: Edit / update / delete a secure note

Date: 2026-06-23 07:58:25 (local)

## What the user asked

Be able to **edit/update** and **delete** a secure note that was created.

## Current state (verified)

- **Delete already works**: the file row's ⋮ menu → Delete calls `viewModel.deleteFileItem(item)`
  ([FileExplorerScreen.kt:681-693](app/src/main/java/com/example/ui/FileExplorerScreen.kt#L681)),
  with the optional phone-lock/biometric confirmation gate. A `.securenote` is just a file, so
  this path already removes it.
- **Edit/update is missing**: the note viewer is read-only — there is no way to change a note's
  content after creation.

So the real work is **editing**, plus making delete reachable from the viewer for convenience.

## Plan

Turn the read-only viewer `ModalBottomSheet` into a **view + edit** sheet:

- It opens in **read mode** (current behavior: title + scrollable decrypted text + Close).
- An **Edit** button switches the content into an editable `OutlinedTextField`; a **Save** button
  re-encrypts and overwrites the same `.securenote` file in place, then returns to read mode.
- A **Delete** button in the viewer reuses the exact same gated-delete flow as the row menu
  (respecting `isPhoneLockDeleteEnabled` → `activeActionPendingValidation`), then closes the sheet.

Scope: edit the note's **content** (the body). **Renaming is out of scope** for this change
(the filename stays the same) — can be a follow-up; I'll note it explicitly.

### Files to change

1. **`app/src/main/java/com/example/data/StorageRepository.kt`**
   - Add `suspend fun overwriteEncryptedNote(file: File, content: String): Boolean` — re-encrypts
     `content` with `CryptoManager` and overwrites `file` in place; returns success/failure.

2. **`app/src/main/java/com/example/ui/StorageViewModel.kt`**
   - Extend `OpenNote` to carry the `file: File` of the open note (so save knows the target);
     populate it in `openNote(item)` from `item.file`.
   - Add `fun saveNoteEdits(newContent: String)`: calls `overwriteEncryptedNote`, on success
     updates `_openNote` content + refreshes the directory listing/stats (size may change) and
     dispatches `msg_note_updated`; on failure dispatches `msg_note_update_failed`.

3. **`app/src/main/java/com/example/ui/FileExplorerScreen.kt`**
   - Rework the viewer sheet: local `isEditing` + `editContent` state; read-mode shows
     text + Edit + Delete + Close; edit-mode shows the editable field + Save + Cancel.
   - Wire Delete to the existing gated-delete logic (close note first, then the same
     `activeActionPendingValidation` / `deleteFileItem` path used by the row menu).
   - Reset `isEditing` when a new note opens.

4. **`app/src/main/res/values/strings.xml`** and **`app/src/main/res/values-ml/strings.xml`**
   - Add: `action_edit`, `action_update` (Save in edit mode), `action_delete`,
     `msg_note_updated`, `msg_note_update_failed`. Reuse existing `action_close`,
     `confirm_delete_title/subtitle`.

## Notes / scope
- Content-only edit; no rename in this change.
- Delete behavior/gating is unchanged — just also exposed inside the viewer.
- No new Gradle dependency.

---
Per the workflow rules I will not change any project file until you approve. **Do you approve this plan?**
