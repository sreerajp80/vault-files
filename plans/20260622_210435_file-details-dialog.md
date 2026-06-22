# Plan: Add a "Details / Properties" option to the file/folder menu

## Issue

There is no way to view the meta information (properties) of a file or folder in the app.
The item action menu (`CardItemMenu`, opened via the ⋮ icon on each row in the Files tab)
only offers Lock & Secure / Move to Vault, Compress / Decompress ZIP, and Delete. The only
metadata currently surfaced is the inline row subtitle (modified date + item count) and the
size on the right edge. Users have no place to see full path, exact size, category, secured
status, etc.

## Goal

Add a **Details** option to `CardItemMenu` that opens a read-only dialog (`FileDetailsDialog`)
showing the item's meta information. No data-layer changes are required — everything needed is
already on the `FileItem` model (`name`, `absolutePath`, `file`, `isDirectory`, `size`,
`isSecured`, `category`, `itemCount`) plus `file.lastModified()`.

## Files to be changed

- `app/src/main/java/com/example/ui/FileExplorerScreen.kt`
  - Add a new `onDetailsClick` callback parameter to `CardItemMenu` and a "Details" `TextButton`
    row (icon `Icons.Default.Info`, testTag `menu_details`) placed at the top of the menu, above
    the existing actions.
  - Add a new `@Composable fun FileDetailsDialog(item: FileItem, onDismiss: () -> Unit)` that
    renders an `AlertDialog` listing the item's properties as label/value rows.
  - In the `CardItemMenu(...)` call site (around line 622), add state to hold the item whose
    details are shown (e.g. `showDetailsForFileItem`), wire `onDetailsClick` to set it and close
    the menu, and render `FileDetailsDialog` when set.

No changes to `StorageRepository`, `StorageViewModel`, Room, or the `FileItem` model.

## Details dialog contents

- **Name** — `item.name`
- **Type** — "Folder" or the file `item.category` (e.g. Image / Document / Archive / Other)
- **Path** — `item.absolutePath`
- **Size** — `formatBytes(item.size)` (reusing the existing helper); for folders this reflects
  the already-computed size
- **Items** — `item.itemCount` (folders only)
- **Modified** — `item.file.lastModified()` formatted with `SimpleDateFormat`
  ("dd MMM yyyy, HH:mm"), matching the existing date-formatting style in the row
- **Secured** — "Yes / No" from `item.isSecured` (folders only)

Dialog has a single "Close" button (consistent with `CardItemMenu`'s confirm button). Long
values (Path) wrap rather than truncate.

## Out of scope

- No real filesystem `stat` beyond `File.lastModified()`/`length()` (the app's storage is largely
  simulated demo data per project docs; creation/access times aren't reliably available).
- No new strings resource file work unless you want it — labels will be inline literals to match
  the existing screen's style.

## Verification

- `./gradlew assembleDebug` builds.
- Manually: open Files tab → ⋮ on a file and on a folder → "Details" → dialog shows correct
  fields; folder shows Items + Secured rows, file does not.
