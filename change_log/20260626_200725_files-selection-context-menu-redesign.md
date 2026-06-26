# Files selection context menu redesign — change log

Implements plan `plans/20260626_194509_files-selection-context-menu-redesign.md`
(approved 2026-06-26).

## Summary

Reworked the file-explorer selection mode into a Google-Files-style toolbar + overflow
menu. When one or more items are selected, the toolbar now shows the selected/total count,
quick **Delete / Info / Rename** icons (Rename only for a single selection), and a 3-dot
overflow menu whose contents adapt to the selection type. Every action is gated by a
dialog.

## Overflow menu by selection type

- **Single file:** Share, Hide, Create shortcut, Copy path to clipboard, Set as (images
  only), Open with, Open as, Copy to, Move to, Compress, Decompress (zip only),
  Move to Vault, Select all.
- **Multiple items:** Share, Hide, Copy to, Move to, Compress, Select all, plus the kept
  secure action that applies (all-folders → Lock/Shield, all-files → Move to Vault).
- **Single folder:** Share, Hide, Create shortcut, Copy path, Copy to, Move to, Compress,
  Lock/Remove shield, Select all.

## Action behaviors

- **Rename** — text-field dialog → real `File.renameTo`.
- **Hide** — confirmation → prepends `.` (un-hides if already hidden) via rename.
- **Share** — `ACTION_SEND` / `ACTION_SEND_MULTIPLE` via FileProvider + `createChooser`
  system share sheet. Folders report they cannot be shared.
- **Set as** (images) — `ACTION_ATTACH_DATA`.
- **Open with** — `ACTION_VIEW` forced through `createChooser` (system app picker).
- **Open as** — radio dialog (Text/Image/Audio/Video/Other) → forced-MIME `ACTION_VIEW`.
- **Create shortcut** — pinned launcher shortcut via `ShortcutManagerCompat`.
- **Copy path to clipboard** — real clipboard write.
- **Copy to / Move to** — in-app folder picker spanning **App storage** and **Device
  storage** (root switch), browsing subfolders; real copy/move. Move uses copy-then-delete
  so it works across filesystems (device ↔ app storage). Device browsing prompts for
  all-files access when not granted.
- **Select all** — selects every item in the current listing.
- **Delete / Move to Vault / Decompress / Lock-Shield** — kept existing behavior; Delete &
  Move-to-Vault keep their phone-lock validation (which serves as the confirmation when
  enabled, otherwise a generic confirm dialog is shown).

Every action that lacks a purpose-built dialog is gated by a reusable `ConfirmActionDialog`
(OK/Cancel) that states what it will do.

## Files changed

- `app/src/main/java/com/example/data/StorageRepository.kt` — added `renameFile`,
  `copyFileOrFolder`, `moveFileOrFolder` (cross-filesystem via copy-then-delete),
  `listSubdirectories`, and `appStorageRoot` / `deviceStorageRoot` accessors.
- `app/src/main/java/com/example/ui/StorageViewModel.kt` — added `renameFileItem`,
  `hideOrUnhideItems`, `copyItemsTo`, `moveItemsTo`, `subdirectoriesOf`, and root
  passthroughs.
- `app/src/main/java/com/example/ui/FileExplorerScreen.kt` — new `SelectionAction` enum;
  rewrote `SelectionToolbar` (count + Delete/Info/Rename + overflow `DropdownMenu`);
  added a single `onSelectionAction` handler; added `ConfirmSpec` / `MoveCopyRequest`
  state holders; added `ConfirmActionDialog`, `RenameDialog`, `OpenAsDialog`,
  `MoveCopyPickerDialog`; added intent helpers (`shareItems`, `setAsImage`,
  `openWithChooser`, `openAs`, `copyPathToClipboard`, `createPinnedShortcut`); new imports
  (ClipData/ClipboardManager, ShortcutManagerCompat/ShortcutInfoCompat/IconCompat,
  DriveFileMove icon).
- `app/src/main/res/values/strings.xml` and `app/src/main/res/values-ml/strings.xml` —
  added menu labels, dialog/picker strings, confirmation messages, and result messages
  (reusing the existing `action_ok` / `action_cancel`). Added `files_selected_count_total`
  for the `selected / total` count format.

## Notes / decisions

- Count format switched to `selected / total` (e.g. `1 / 15`) to match the screenshot.
- The pre-existing FileProvider (`${applicationId}.fileprovider`) + `xml/file_paths.xml`
  back Share / Set as / Open with / Open as.
- Build: `./gradlew assembleDebug` → BUILD SUCCESSFUL.

## Not yet verified

- Behavior was verified only by a successful compile; not exercised on a device/emulator.
