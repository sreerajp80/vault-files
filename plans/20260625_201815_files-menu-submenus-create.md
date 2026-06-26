# Files screen — restructure dotted menu into View Type / Sort By / Create

## The request

Rework the Files-screen three-dot (⋮) overflow menu so it lists **three** top-level
items instead of showing the View/Sort sections inline:

1. **View Type** → opens a small popup that lets the user tap **List / Grid / Compact**.
2. **Sort By** → opens a small popup that lets the user tap **Name / Size / Date**.
3. **Create** → opens a small popup offering **Folder** (with a name field) and
   **Secure Note**:
   - selecting **Secure Note** opens the existing Secure Note window (the current
     `showCreateFileDialog` bottom sheet),
   - otherwise the user types a name and selects **Folder** to create it (the current
     folder-creation behaviour).

Also: **remove the Folder and Secure Note action icons from the top header** (the two
`HeaderActionButton`s), since Create now lives in the menu.

## Current state (after the previous change)

- `FileExplorerScreen.kt` section-header row has a `MoreVert` `IconButton`
  (`files_options_menu`) opening one `DropdownMenu` that currently shows **View**
  (List/Grid/Compact) and **Sort by** (Name/Size/Date) sections directly.
- The top header `Row` (~301–314) has two `HeaderActionButton`s: New Folder
  (`showCreateFolderDialog`) and New Note (`showCreateFileDialog`).
- Folder Creator `AlertDialog` (~840) and Secure Note Creator `ModalBottomSheet`
  (~877) already exist.

## Design decisions (please confirm)

- The three submenus are shown as **small dialogs (`AlertDialog`)**, matching the
  "small pop window" wording:
  - **View Type** dialog: three tappable rows (icon + label), check on the current
    mode; tapping applies (`viewModel.updateFileViewMode`) and closes.
  - **Sort By** dialog: three tappable rows, check on the current `sortMode`; tapping
    applies and closes.
  - **Create** dialog: a folder-name `OutlinedTextField` + a **Create Folder** confirm
    button (enabled when the name is non-blank → `viewModel.createFolder`), plus a
    **Secure Note** row/button that closes this dialog and opens the existing Secure
    Note bottom sheet.
- The existing standalone Folder Creator dialog is folded into the new **Create**
  dialog (so `showCreateFolderDialog` is removed). The Secure Note bottom sheet
  (`showCreateFileDialog`) is unchanged and reused.
- testTags kept/added: `files_options_menu` (trigger), `files_menu_view_type`,
  `files_menu_sort_by`, `files_menu_create` (menu items); existing `files_view_<key>`,
  `files_sort_<mode>`, `folder_input_field`, `confirm_create_folder_btn`,
  `note_filename_field`, etc. preserved on the corresponding controls.

## Files to be changed

1. **`app/src/main/java/com/example/ui/FileExplorerScreen.kt`**
   - Remove the top header create `Row` (~301–314) with the two `HeaderActionButton`s.
   - Remove the now-unused `private fun HeaderActionButton(...)` (~1196–1220).
   - Replace the inline View/Sort `DropdownMenu` content with three
     `DropdownMenuItem`s: View Type, Sort By, Create — each closes the menu and opens
     the matching dialog.
   - Add state: `showViewTypeDialog`, `showSortByDialog`, `showCreateDialog`.
   - Add the three new `AlertDialog`s described above.
   - Remove `showCreateFolderDialog` state and the standalone folder dialog (folded
     into the Create dialog). Keep `showCreateFileDialog` + its bottom sheet, now
     launched from the Create dialog's Secure Note option.
   - Drop the `menu_section_view`/`menu_section_sort` usages; remove the `NoteAdd`
     import only if it ends up unused (it will be reused as the Secure Note row icon).

2. **`app/src/main/res/values/strings.xml`**
   - Add: `menu_view_type` ("View Type"), `menu_sort_by` ("Sort By"),
     `menu_create` ("Create"), `create_option_folder` ("Folder"),
     `create_option_secure_note` ("Secure Note").
   - Remove now-unused `menu_section_view`, `menu_section_sort`.
   - Reuse existing `dialog_folder_name_label`, `dialog_folder_name_placeholder`,
     `action_create`, `action_cancel`, `cd_view_*`, `sort_*`, `dialog_new_note_title`.

3. **`app/src/main/res/values-ml/strings.xml`**
   - Add Malayalam translations for the five new strings; remove the two unused ones.

## Build / verify

- `./gradlew assembleDebug`.
- Visual check: ⋮ menu shows View Type / Sort By / Create; each opens its small
  dialog; Create → Folder creates a folder, Create → Secure Note opens the note sheet;
  the top folder/note icons are gone.

## Notes
- No ViewModel/repository changes needed (`createFolder`, `createTextFile`,
  `updateFileViewMode`, `sortMode` all already exist).
