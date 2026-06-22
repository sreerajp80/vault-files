# Files Section Redesign

Implements the new design in `samples/Files Redesign.dc.html` for the Files tab.

## What the issue is

The current Files screen (`FileExplorerScreen.kt`) uses a Material3 `TopAppBar` +
a "Scan Target Location" `Card` with `FilterChip`s + a `LazyColumn` of `FileRowItem`s.
The new design calls for a cleaner, more professional browser:

- A custom header: bold **Files** title with a breadcrumb line ("Main Storage › N folders · size")
  and two rounded-square action buttons (New Folder, New File) on the right.
- A real **search field** (currently no search exists anywhere).
- Two equal-width source **pills** (App Sandbox / Entire Device) replacing the chip card.
- A section header row: "ALL FOLDERS" label on the left + a **sort control** ("Name") on the right
  (no sort UI exists today).
- Calm list rows: a tinted rounded icon tile, name, a meta line ("date · N items"), size on the
  right, and a 3-dot menu button.

Additional explicit requirements from the request:
- **Light & dark mode** must both be correct.
- The **phone status (top) bar must be honoured** — the screen drops the `TopAppBar`, so the
  custom header must apply a status-bar inset (currently `MainActivity` only pads the bottom and
  runs edge-to-edge, so without this the header would draw under the status bar / clock).
- **Borders must be clearly visible** on every tile and pill in both light and dark modes.

## Approach

Use `MaterialTheme.colorScheme` tokens throughout (not the raw hex values from the mock) so the
screen automatically tracks the app's existing palette and light/dark themes, and stays consistent
with the other tabs. The mock's layout, spacing, radii, and structure are matched; the colors are
mapped to theme tokens:

- title/name text → `onSurface` / `onBackground`
- breadcrumb "Main Storage" + sort label + selected accents → `primary`
- secondary/meta text → `onSurfaceVariant`
- tile / pill / search backgrounds → `surfaceVariant` (light = white, dark = elevated)
- selected pill background → `primaryContainer`
- **borders** → `outline` (the stronger token, ~`#C2BCC9` light / `#49454F` dark) at 1.dp so they
  read clearly in both modes; selected pill border → `primary`.

### Functional additions (part of the design)

- **Search**: a client-side filter over the already-loaded `currentDirectoryFiles` list, held in
  local screen state (`remember`). No ViewModel/repo change needed.
- **Sort**: a local sort state (Name / Size / Date) applied to the list, surfaced via a small
  `DropdownMenu` anchored to the "Name/Size/Date" control. Default = Name (matches today's
  folders-first, name-ascending behaviour).
- **Folder item count** for the meta line ("N items"): add an optional `itemCount` to `FileItem`
  populated cheaply in the repository (`file.listFiles()?.size`); render "date · N items" for
  folders and "date · size" style for files. Files keep showing their size.

## Files to be changed

1. **`app/src/main/java/com/example/ui/FileExplorerScreen.kt`** (main work)
   - Replace the `Scaffold`/`TopAppBar` with a custom `Column` layout that has
     `Modifier.statusBarsPadding()` so the phone top bar is honoured.
   - Build the header (title + breadcrumb with back/up affordance + two action icon buttons).
   - Add the search field (bound to local search state).
   - Replace the "Scan Target Location" card with the two source pills (bordered, selected state).
   - Add the "ALL FOLDERS" + sort control row with a sort `DropdownMenu`.
   - Redesign `FileRowItem` to the new tile (rounded 18.dp, visible `outline` border, tinted icon
     tile, name, "date · N items" meta, size, 3-dot menu). Keep the secured-lock and shield
     indicators and all existing click/menu behaviour.
   - Keep all existing dialogs (`CreateFolder`, `CreateFile`, `Zip`, biometric/PIN), the
     hidden-items unlock banner, the device-permission state, and the empty state — restyled to fit.
   - Preserve every existing `testTag` (e.g. `files_storage_source_card`,
     `files_select_sandbox_chip`, `files_select_device_chip`, `file_row_*`, `options_*`, etc.) so
     instrumented tests keep passing; add new tags for search/sort.

2. **`app/src/main/java/com/example/data/StorageRepository.kt`**
   - Add `itemCount: Int` to `FileItem` and populate it in `getFilesAndFoldersInDirectory`
     (`file.listFiles()?.size ?: 0` for directories, 0 for files).

No theme/color files are changed globally; `MainActivity` is left untouched.

## Out of scope / notes

- The mock's status-bar glyphs and bottom nav are just rendering context — the real status bar and
  the existing `MainActivity` `NavigationBar` provide those; not reimplemented.
- Search and sort are local to the screen (no persistence), matching the lightweight intent.
