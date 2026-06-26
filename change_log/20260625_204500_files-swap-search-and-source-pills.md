# Change log: Swap Files-screen search bar and storage source pills

Implements plan `plans/20260625_204500_files-swap-search-and-source-pills.md`.

## What changed
In `app/src/main/java/com/example/ui/FileExplorerScreen.kt` (File Explorer
header `Column`), reordered two layout blocks:

- The storage **source pills** ("ആപ്പ് സാൻഡ്‌ബോക്സ്" / "മുഴുവൻ ഉപകരണം") now
  render on the **first** row.
- The **search field** ("ഫയലുകളും ഫോൾഡറുകളും തിരയുക") now renders on the
  **second** row.

Padding adjusted to keep spacing consistent:
- Source pills row: `top = 16.dp` (now the first element under the header).
- Search field surface: `top = 12.dp`.

No logic, state, string-resource, or `testTag` changes — purely a layout reorder
plus the two padding tweaks.

## Verification
- `./gradlew assembleDebug` — build succeeded.
