# Plan: Secure Note viewer (decrypt & read encrypted notes)

Date: 2026-06-23 07:49:50 (local)

## Context / what the user asked

Encrypted notes (`.securenote`, AES-GCM via `CryptoManager`) are currently **write-only** — the
app has no way to read one back. The user wants to be able to **view** an encrypted note's
decrypted content in-app.

## Current behavior (verified)

- Tapping a non-folder, non-zip file only shows a toast
  ([FileExplorerScreen.kt:626-627](app/src/main/java/com/example/ui/FileExplorerScreen.kt#L626)).
- `CryptoManager.decrypt(...)` already exists but is unused.
- `StorageViewModel` exposes state via `StateFlow`s and reads via `repository` on
  `Dispatchers.IO` (e.g. `createEncryptedNote`).
- `action_close` string already exists in both locales.

## Plan

When a user taps a `.securenote` file, decrypt it and show the plaintext in a read-only
**ModalBottomSheet** (consistent with the creator sheet, ~75% height). Content is selectable
but not editable (editing/saving-back is out of scope for this change).

### Files to change

1. **`app/src/main/java/com/example/data/StorageRepository.kt`**
   - Add `suspend fun readEncryptedNote(file: File): String?` — reads bytes, calls
     `cryptoManager.decrypt(...)`, returns UTF-8 text; returns `null` on any failure
     (corrupt/foreign file, wrong key).

2. **`app/src/main/java/com/example/ui/StorageViewModel.kt`**
   - Add a small holder `data class OpenNote(val name: String, val content: String)`.
   - Add `private val _openNote = MutableStateFlow<OpenNote?>(null)` + public `openNote` StateFlow.
   - Add `fun openNote(item: FileItem)` — launches IO read+decrypt; on success sets `_openNote`,
     on failure dispatches a new `msg_note_open_failed` message.
   - Add `fun closeNote()` to clear `_openNote`.

3. **`app/src/main/java/com/example/ui/FileExplorerScreen.kt`**
   - In `onItemClick`, add a branch: if the file name ends with `.securenote`, call
     `viewModel.openNote(item)` (instead of the generic "viewing file" toast).
   - Collect `openNote` state; when non-null, render a read-only `ModalBottomSheet` showing the
     note name as the title and the decrypted content (scrollable, `SelectionContainer`,
     `fillMaxHeight(0.75f)`), with a Close button that calls `viewModel.closeNote()`.

4. **`app/src/main/res/values/strings.xml`** and **`app/src/main/res/values-ml/strings.xml`**
   - Add `msg_note_open_failed` ("Couldn't open this note" / Malayalam equivalent). Reuse
     existing `action_close`.

## Scope / notes

- **Read-only**: no edit-and-resave in this change (can be a follow-up).
- Only `.securenote` files route to the viewer; `.txt`, `.zip`, folders, and secured-folder
  gating are unchanged. Legacy plain-text `.txt` notes still just show the existing toast.
- A note created on a different install/key won't decrypt (key is device-bound) → handled by
  the `null` → failure-message path.
- No new Gradle dependency.

---
Per the workflow rules I will not touch any project file until you approve. **Do you approve this plan?**
