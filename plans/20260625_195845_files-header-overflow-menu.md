# Files screen — remove "All items" label & move view/sort controls to a dotted menu

## The issue / request

On the Files (File Explorer) screen there is a "section header" row that sits just
above the file list. Currently it contains:

- **Left:** the text "All items" (`R.string.files_all_items` → Malayalam
  "എല്ലാ ഇനങ്ങളും"). When a category filter is active it instead shows a category
  header (e.g. "ALL PHOTOS", `R.string.files_all_category_header`).
- **Right:** the view-mode toggle (List / Grid / Compact segmented control) and the
  sort control ("Name / Size / Date" with its own dropdown).

Two requests:

1. **Remove the "All items" text** from the Files screen.
2. **Move "all these"** — i.e. the view-mode toggle and the sort control — **into a
   single "dotted" (three-dot / overflow) menu** instead of showing them inline.

All of this lives in `FileExplorerScreen.kt` lines ~452–522 (the
`// Section header + sort control` block).

## Design decisions (please confirm)

- The plain "All items" text is removed entirely.
- The **category header text is kept** when a category filter is active (it is
  informative context, e.g. "ALL PHOTOS"), with the overflow menu on its right.
  When no filter is active the row shows only the three-dot menu, right-aligned.
- The three-dot menu (`Icons.Default.MoreVert`) opens one `DropdownMenu` with two
  labelled sections:
  - **View** → List, Grid, Compact (check mark on the current mode; tapping calls
    `viewModel.updateFileViewMode(...)`).
  - a divider, then
  - **Sort by** → Name, Size, Date (check mark on the current `sortMode`; tapping
    sets `sortMode`).
- The existing `ViewModeToggle` composable becomes unused and will be removed.
- testTags preserved where practical so existing logic/tests still target controls:
  `files_view_<key>` for the view items, `files_sort_<mode>` for the sort items, and
  a new `files_options_menu` for the trigger.

## Files to be changed

1. **`app/src/main/java/com/example/ui/FileExplorerScreen.kt`**
   - Rewrite the section-header `Row` (~452–522): drop the "All items" `Text`, keep
     the category header (filtered case only), and replace the inline
     `ViewModeToggle` + sort `Row`/`DropdownMenu` with a single `MoreVert`
     `IconButton` + `DropdownMenu` containing the View and Sort sections.
   - Rename/keep `sortMenuExpanded` as the single menu's expanded state (rename to
     `optionsMenuExpanded` for clarity).
   - Remove the now-unused `private fun ViewModeToggle(...)` (~1249–1284).
   - Remove now-unused imports if any (e.g. `BorderStroke`/`Surface` only if no
     other usage — will verify before deleting).

2. **`app/src/main/res/values/strings.xml`**
   - Add `menu_section_view` ("View") and `menu_section_sort` ("Sort by") section
     labels and `cd_files_options` ("More options") for the trigger's content
     description. (Existing `sort_name/size/date` and `cd_view_*` strings reused for
     item labels.)

3. **`app/src/main/res/values-ml/strings.xml`**
   - Add Malayalam translations for the three new strings above.
   - The `files_all_items` string itself is left in place (harmless; no longer
     referenced) unless you'd prefer it deleted from both files.

## Build / verify

- `./gradlew assembleDebug` to confirm it compiles.
- Manual/visual check that the Files header now shows just the three-dot menu (and
  category header when filtered), and that the menu switches view mode and sort.

## Notes

- No ViewModel/repository changes needed: view-mode persistence
  (`updateFileViewMode`) and the local `sortMode` state already exist.
