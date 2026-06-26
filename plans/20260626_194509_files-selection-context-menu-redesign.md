# Files selection context menu redesign

## Issue / goal

When a file/folder is selected (long-press → selection mode), the selection toolbar
should be restructured to a Google-Files-style layout, and the bulk of the actions
should move into a 3-dot overflow menu whose contents vary by selection type.

### Target UI (from user's screenshots)

**Selection toolbar (replaces search field while in selection mode):**
- NO back arrow.
- Left: the existing clear/close (X) + a count shown as `current / total` style
  (screenshot showed `1 / 15`). *(See open decision on count format below.)*
- Always-visible icons: **Delete**, **Info**, **Rename**.
- A **3-dot overflow** menu holding everything else.

**Overflow menu contents by selection type:**

| Action                   | Single file | Multiple items | Single folder |
|--------------------------|:-----------:|:--------------:|:-------------:|
| Share                    | ✓ | ✓ | ✓ |
| Hide                     | ✓ | ✓ | ✓ |
| Create shortcut          | ✓ | — | ✓ |
| Copy path to clipboard   | ✓ | — | ✓ |
| Set as (images only)     | ✓* | — | — |
| Open with                | ✓ | — | — |
| Open as                  | ✓ | — | — |
| Copy to                  | ✓ | ✓ | ✓ |
| Move to                  | ✓ | ✓ | ✓ |
| Compress                 | ✓ | ✓ | ✓ |
| Select all               | ✓ | ✓ | ✓ |

\* "Set as" only appears when the single selected file is an image.

**Plus (per approval):** the existing secure-app actions are KEPT, moved into the
overflow menu where they apply:
- **Lock & Secure folder / Remove shield** (folders) — existing `toggleFolderShield`.
- **Move to Biometric Vault** (files) — existing `secureFileInVault`.
- **Decompress ZIP** (single `.zip`) — existing `decompressZip`.

These appear in the overflow in addition to the screenshot items.

### Mandatory: every action shows a dialog

Every menu/toolbar action must surface a dialog before it takes effect:

- Actions that already have a purpose-built dialog satisfy this requirement on their
  own: **Rename** (text field), **Open as** (radio type list), **Copy to / Move to**
  (folder picker). These dialogs are themselves the confirmation; OK performs the action.
- Every other action shows a generic **confirmation dialog** stating exactly what it is
  about to do (including the item name or count), with **OK** and **Cancel** buttons.
  Cancel aborts and does nothing. This covers: Share, Hide, Create shortcut,
  Copy path to clipboard, Set as, Open with, Open as (the launch step is preceded by the
  type dialog, which acts as its confirmation), Select all, Compress, Delete, and the
  kept secure actions (Lock/Shield, Move to Vault, Decompress).
- Implementation: a single reusable `ConfirmActionDialog` composable (title, message,
  onConfirm, onDismiss). The screen holds a `pendingConfirm: ConfirmSpec?` state; menu
  items set it, and the dialog runs `onConfirm` then clears selection. The existing
  phone-lock `PendingAction` validation for Delete / Move-to-Vault is preserved and runs
  in addition where it already applies.

### Confirmed behavior decisions

1. Toolbar: no back arrow (keep the existing X to clear selection).
2. **Hide** = real rename prepending `.` to the name (system-hidden; consistent with
   the existing "Show Hidden Files" setting). For an already-hidden item it un-hides
   (strips the leading `.`); label/text stays "Hide".
3. **Copy to / Move to** = real, via an in-app folder-picker dialog, then copies/moves
   on `Dispatchers.IO`. The picker spans **both storage areas** so the user can move
   files between **App storage** (the app's private/simulated root) and **Device
   storage** (real external storage). The dialog has a root switch (App storage /
   Device storage) and browses subfolders within the chosen root; "Move/Copy here"
   targets the currently-browsed folder. Device storage browsing requires the
   all-files permission (already handled by `hasAllFilesPermission`); if not granted the
   dialog prompts to grant it. This is the mechanism for moving items between device and
   app storage.
4. Share / Set as / Open with / Open as / Create shortcut = **all really implemented**:
   - Share: `ACTION_SEND` (single) / `ACTION_SEND_MULTIPLE` (multi) with FileProvider
     URIs, wrapped in `Intent.createChooser(...)` so the OS shows the system share
     sheet of receiving apps (the screenshot). Folders can't be shared via intent →
     for a folder, dispatch a message that folders can't be shared.
   - Set as: `ACTION_ATTACH_DATA` with the image URI + mime (images only).
   - Open with: fire `ACTION_VIEW` wrapped in `Intent.createChooser(...)` so the OS
     always shows the system app-picker bottom sheet (the screenshot). A small variant
     of the existing `openFileExternally` (FileProvider URI + mime), forced through the
     chooser instead of opening the default app silently.
   - Open as: dialog with radio options **Text file / Image file / Audio file /
     Video file / Other file** → `ACTION_VIEW` with mime `text/*`, `image/*`,
     `audio/*`, `video/*`, `*/*` respectively.
   - Create shortcut: real pinned launcher shortcut via
     `ShortcutManagerCompat.requestPinShortcut` (guarded by `isRequestPinShortcutSupported`;
     falls back to a message if unsupported). The shortcut opens the app.
5. **Select all** selects every item currently in the listing (`baseList`).
6. **Rename** icon only shown for a single selection; hidden when multiple selected.

## Files to change

1. **`app/src/main/java/com/example/ui/FileExplorerScreen.kt`**
   - Rewrite `SelectionToolbar` composable: count + Delete/Info/Rename icons + overflow
     `IconButton` opening a `DropdownMenu`. Build menu items conditionally on
     single-file / multi / folder / image / zip / secured.
   - Add new callbacks to `SelectionToolbar` and wire them at the call site (~line 339):
     `onRename`, `onShare`, `onHide`, `onCreateShortcut`, `onCopyPath`, `onSetAs`,
     `onOpenWith`, `onOpenAs`, `onCopyTo`, `onMoveTo`, `onSelectAll` (plus the kept
     existing `onShieldToggle`, `onMoveVault`, `onExtract`, `onCompress`, `onDetails`,
     `onDelete`).
   - Add a **Rename dialog** (single-line text field, pre-filled name) → calls
     `viewModel.renameFileItem`.
   - Add a reusable **`ConfirmActionDialog`** (title/message/OK/Cancel) and a
     `pendingConfirm` state so every action that lacks its own dialog is gated by it.
   - Add an **"Open as" dialog** (radio list of the 5 types) → `ACTION_VIEW` with mime.
   - Add a **folder-picker dialog** for Copy to / Move to with an **App storage /
     Device storage** root switch, browsing subfolders within the chosen root;
     "Move/Copy here" confirm → `viewModel.copyItemsTo` / `moveItemsTo`. Enables moving
     items between device and app storage.
   - Add helpers: `shareFiles(context, items)`, `setAsImage(context, item)`,
     `copyPathToClipboard(context, items)`, `createPinnedShortcut(context, item)`,
     `openAs(context, item, mime)`.
   - `Select all`: set `selectedPaths = baseList.map { it.absolutePath }.toSet()`.
   - Decide count display string (see open decision).

2. **`app/src/main/java/com/example/ui/StorageViewModel.kt`**
   - Add `renameFileItem(item, newName)`, `hideOrUnhideItems(items)` (prepend/strip `.`),
     `copyItemsTo(items, destDir)`, `moveItemsTo(items, destDir)`. Each follows the
     existing pattern: call repo on IO, `dispatchMessage`, then
     `loadFilesInDirectory(currentDirectory)` + `refreshStorageStats()`.

3. **`app/src/main/java/com/example/data/StorageRepository.kt`**
   - Add `renameFile(file, newName): Boolean` (via `File.renameTo`).
   - Add `copyFileOrFolder(source, destDir): Boolean` and
     `moveFileOrFolder(source, destDir): Boolean` (recursive copy for dirs, then
     delete-on-move), guarding against same-path / name collisions. Move uses
     copy-then-delete (not `renameTo`) so it works **across filesystems** (device ↔ app
     storage), where `renameTo` would fail.
   - Add a helper to expose the device external-storage root + the app storage root so
     the folder picker can browse both.
   - Add `listSubdirectories(dir): List<File>` (or reuse existing listing) to back the
     folder picker, if a dedicated helper is cleaner.

4. **`app/src/main/res/values/strings.xml`** — add strings + content descriptions:
   `menu_share`, `menu_hide`, `menu_create_shortcut`, `menu_copy_path`, `menu_set_as`,
   `menu_open_with`, `menu_open_as`, `menu_copy_to`, `menu_move_to`, `menu_select_all`,
   `menu_rename`, `cd_more_actions`, rename-dialog strings, open-as type labels
   (`open_as_text/image/audio/video/other`), folder-picker strings, and result messages
   (`msg_renamed`, `msg_rename_failed`, `msg_hidden`, `msg_unhidden`, `msg_copied`,
   `msg_moved`, `msg_copy_failed`, `msg_move_failed`, `msg_path_copied`,
   `msg_shortcut_created`, `msg_cannot_share_folder`, `msg_no_app_to_open`, etc.).
   Update `files_selected_count` if the count format changes. Add per-action
   **confirmation-dialog** strings (title + message stating the action, with the item
   name / count) for Share, Hide, Create shortcut, Copy path, Set as, Open with,
   Select all, Compress, Delete, and the secure actions, plus generic
   `action_ok` / `action_cancel` button labels (reuse existing where present).

5. **`app/src/main/res/values-ml/strings.xml`** — Malayalam translations for all the
   above (mirror keys; placeholder/English fallback acceptable where no translation, but
   will provide Malayalam to match existing file).

## Notes / non-goals
- FileProvider (`${applicationId}.fileprovider`) + `xml/file_paths.xml` already exist, so
  Share/Set as/Open with reuse them.
- Copy/Move operate on the simulated storage tree like the rest of the app; no vault
  encryption involved.

## Open decision (please confirm with approval)
- **Count format:** screenshot shows `1 / 15` (selected / total-in-folder). The current
  string is `"%1$d selected"`. Switch to `selected / total` (`%1$d / %2$d`), or keep the
  `N selected` text? I'll go with `selected / total` to match the screenshot unless you
  say otherwise.
