# Plan: Swap Files-screen search bar and storage source pills

## Issue
On the File Explorer screen, the search field ("ഫയലുകളും ഫോൾഡറുകളും തിരയുക")
currently renders on the first row and the two storage-source pills
("ആപ്പ് സാൻഡ്‌ബോക്സ്" / "മുഴുവൻ ഉപകരണം") render on the second row.

The desired order is reversed: the storage source pills should be on the
first row and the search field on the second row.

## Files to change
- `app/src/main/java/com/example/ui/FileExplorerScreen.kt`

## Plan for the fix
In `FileExplorerScreen.kt`, within the header `Column`:
1. Move the `// ---------------- Source pills ----------------` block
   (currently lines ~314–336) to render **before** the
   `// ---------------- Search field ----------------` block
   (currently lines ~257–312).
2. Adjust the `padding(top = …)` so vertical spacing stays consistent:
   - The now-first block (source pills) keeps its top padding relative to the
     breadcrumb/header above it (use `top = 16.dp`).
   - The now-second block (search field) uses `top = 12.dp`.

No logic, state, string, or testTag changes — purely a layout reorder plus
two padding tweaks.

## Verification
- Build: `./gradlew assembleDebug`
- Visually confirm pills are on the first row and search on the second.
