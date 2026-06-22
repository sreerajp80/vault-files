# Files Section Redesign — Change Log

Implements: `plans/20260621_173706_files-section-redesign.md`

Reworked the Files tab to match `samples/Files Redesign.dc.html`, using
`MaterialTheme.colorScheme` tokens throughout so both light and dark themes track the app's
existing palette automatically.

## Changes

### `app/src/main/java/com/example/ui/FileExplorerScreen.kt` (full rewrite of layout)
- Removed the `Scaffold` + `TopAppBar`. The screen is now a root `Box` containing a `Column` with
  `Modifier.statusBarsPadding()` so the **phone status (top) bar is honoured** (MainActivity runs
  edge-to-edge and only pads the bottom).
- **Header**: bold "Files" title, a breadcrumb row ("Main Storage › N folders · total size")
  that is tappable to navigate up (with a leading chevron) when not at the storage root, and two
  rounded-square action buttons — New Folder (outlined) and New File (filled primary, elevated).
- **Search field** (new): rounded, bordered `Surface` with a `BasicTextField`, placeholder, and a
  clear ("X") affordance. Filters the loaded directory list client-side (case-insensitive name
  match). Tag: `files_search_field`.
- **Source pills**: replaced the "Scan Target Location" card + `FilterChip`s with two equal-width
  bordered pills (App Sandbox / Entire Device). Selected = `primaryContainer` fill + `primary`
  border; unselected = `surfaceVariant` + `outline` border. Preserved tags
  `files_storage_source_card`, `files_select_sandbox_chip`, `files_select_device_chip`.
- **Section header + sort control** (new): "All items" label on the left; a tappable "Name/Size/
  Date" control on the right opening a `DropdownMenu`. Sort is applied client-side (folders first,
  then Name asc / Size desc / Date-modified desc). Tags: `files_sort_control`, `files_sort_*`.
- **`FileRowItem`** redesigned: rounded 18.dp tile, `surfaceVariant` background, **1.dp `outline`
  border + 1.dp shadow** so tiles are clearly identifiable in both modes; 44.dp tinted rounded
  icon tile; bold name with shield indicator; meta line ("date · N items" for folders, date for
  files); right-aligned size via the new `formatBytes` helper; compact 3-dot menu. Secured-lock
  and shield indicators retained. Tags `file_row_*` / `options_*` preserved.
- Empty state now also handles the "no search matches" case (search-off icon + message).
- All dialogs (create folder, create note, zip, biometric/PIN fallback), the hidden-items unlock
  banner (restyled to a bordered `Surface`), and the device-permission state were preserved with
  their existing tags and behaviour. Search/section-header chrome is hidden while the permission
  prompt is shown; the source pills remain visible so the user can switch back to sandbox.
- Added helpers/types: `FileSortMode` enum, `HeaderActionButton`, `SourcePill`, `formatBytes`.
  Removed now-unused imports (`Environment`, `ExperimentalFoundationApi`, `combinedClickable`,
  `ArrowBack`).

### `app/src/main/java/com/example/data/StorageRepository.kt`
- Added `itemCount: Int = 0` to `FileItem` and populated it in `getFilesAndFoldersInDirectory`
  (`file.listFiles()?.size ?: 0` for directories) to drive the "N items" meta line.

## Verification
- `./gradlew :app:compileDebugKotlin` → **BUILD SUCCESSFUL**.
- No theme/color files or `MainActivity` changed; no global palette change.
