# Change log — Files dotted menu: View Type / Sort By / Create + top icon removal

Implements plan `plans/20260625_201815_files-menu-submenus-create.md` (approved).

## What changed

### `app/src/main/java/com/example/ui/FileExplorerScreen.kt`
- **Top header:** removed the create-action `Row` holding the two `HeaderActionButton`s
  (New Folder + New Note icons). Removed the now-unused `HeaderActionButton` composable.
- **Three-dot (⋮) menu:** replaced the inline View/Sort sections with three
  `DropdownMenuItem`s — **View Type** (`files_menu_view_type`), **Sort By**
  (`files_menu_sort_by`), **Create** (`files_menu_create`). Each closes the menu and
  opens its own dialog.
- **New dialogs (`AlertDialog`):**
  - **View Type** (`showViewTypeDialog`): tappable rows for List / Grid / Compact with
    the mode icon and a check on the current mode; tapping applies
    `viewModel.updateFileViewMode` and closes. Rows keep testTag `files_view_<key>`.
  - **Sort By** (`showSortByDialog`): tappable rows for Name / Size / Date with a check
    on the current `sortMode`. Rows keep testTag `files_sort_<mode>`.
  - **Create** (`showCreateDialog`): a folder-name `OutlinedTextField`
    (`folder_input_field`) + a **Folder** confirm button (`confirm_create_folder_btn`,
    enabled only when the name is non-blank → `viewModel.createFolder`), plus a
    **Secure Note** option row (`create_secure_note_option`) that closes this dialog
    and opens the existing Secure Note bottom sheet.
- **State:** added `showCreateDialog`, `showViewTypeDialog`, `showSortByDialog`;
  removed `showCreateFolderDialog` and its standalone dialog (folded into Create).
  `showCreateFileDialog` (Secure Note sheet) unchanged, now launched from the Create
  dialog.

### `app/src/main/res/values/strings.xml`
- Added `menu_view_type` ("View Type"), `menu_sort_by` ("Sort By"),
  `menu_create` ("Create"), `create_option_folder` ("Folder"),
  `create_option_secure_note` ("Secure Note").
- Removed now-unused `menu_section_view`, `menu_section_sort`.

### `app/src/main/res/values-ml/strings.xml`
- Added Malayalam translations for the five new strings; removed the two unused ones.

## Notes
- Leftover now-unused strings (`cd_new_folder`, `cd_new_text_file`,
  `dialog_create_folder_title`) and the `Icons.Default.CreateNewFolder` reference were
  left in place (harmless).
- No ViewModel/repository changes.
- Verified with `./gradlew assembleDebug` — build succeeded (exit 0).
