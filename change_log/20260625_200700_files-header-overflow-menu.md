# Change log — Files header: removed "All items" label, moved view/sort to a dotted menu

Implements plan `plans/20260625_195845_files-header-overflow-menu.md` (approved).

## What changed

### `app/src/main/java/com/example/ui/FileExplorerScreen.kt`
- Rewrote the section-header `Row` above the file list:
  - Removed the inline "All items" text (`R.string.files_all_items`). The category
    header text (`files_all_category_header`) is still shown on the left when a
    category filter is active; otherwise a weighted `Spacer` pushes the menu to the end.
  - Replaced the inline `ViewModeToggle` segmented control and the inline sort
    `Row`/`DropdownMenu` with a single three-dot (`Icons.Default.MoreVert`)
    `IconButton` (testTag `files_options_menu`) that opens one `DropdownMenu`.
  - The menu has two labelled sections: **View** (List / Grid / Compact, each with the
    mode icon and a trailing check on the current mode → `viewModel.updateFileViewMode`)
    and, after a `HorizontalDivider`, **Sort by** (Name / Size / Date, trailing check on
    the current `sortMode`). Item testTags preserved: `files_view_<key>`,
    `files_sort_<mode>`.
- Renamed state `sortMenuExpanded` → `optionsMenuExpanded` (now drives the single menu).
- Deleted the now-unused `private fun ViewModeToggle(...)` composable and its KDoc.

### `app/src/main/res/values/strings.xml`
- Added `cd_files_options` ("More options"), `menu_section_view` ("View"),
  `menu_section_sort` ("Sort by").

### `app/src/main/res/values-ml/strings.xml`
- Added Malayalam translations: `cd_files_options` (കൂടുതൽ ഓപ്ഷനുകൾ),
  `menu_section_view` (കാഴ്ച), `menu_section_sort` (അടുക്കുക).

## Notes
- `files_all_items` was left in both resource files (no longer referenced; harmless).
  `cd_sort` may now be unused but was left in place.
- Verified with `./gradlew assembleDebug` — build succeeded.
