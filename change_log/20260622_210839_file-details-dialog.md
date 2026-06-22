# Change log: Add "Details / Properties" option to file/folder menu

Implements `plans/20260622_210435_file-details-dialog.md`.

## What changed

All changes are in `app/src/main/java/com/example/ui/FileExplorerScreen.kt` — no data-layer,
ViewModel, Room, or `FileItem` model changes.

1. **New state** — added `showDetailsForFileItem` (`mutableStateOf<FileItem?>`) alongside the
   existing `expandedMenuForFileItem` dropdown state.

2. **`CardItemMenu`** — added a new `onDetailsClick: () -> Unit` parameter and a **Details**
   `TextButton` (icon `Icons.Default.Info`, testTag `menu_details`) at the top of the menu,
   followed by a `HorizontalDivider`, above the existing Lock/Vault, Zip, and Delete actions.

3. **Call site** — wired `onDetailsClick` to set `showDetailsForFileItem` and close the action
   menu; rendered the new dialog when that state is non-null.

4. **New `FileDetailsDialog` composable** — read-only `AlertDialog` showing the item's
   properties via a small private `DetailRow(label, value)` helper:
   - Type ("Folder" or `item.category`)
   - Path (`item.absolutePath`)
   - Size (`formatBytes(item.size)`)
   - Items (folders only, from `item.itemCount`)
   - Modified (`item.file.lastModified()` formatted as "dd MMM yyyy, HH:mm")
   - Secured (folders only, Yes/No from `item.isSecured`)

   Single "Close" button, consistent with `CardItemMenu`.

No new imports needed — `Icons.Default.Info` is covered by the existing
`material.icons.filled.*` wildcard import.

## Verification

- `./gradlew assembleDebug` — BUILD SUCCESSFUL; fresh `app-debug.apk` produced.
- Manual UI verification (open Files tab → ⋮ → Details on a file and a folder) still
  recommended on device/emulator.
