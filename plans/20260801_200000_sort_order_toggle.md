# Plan: Add Ascending/Descending Sort Order Toggle (Option 1)

## Issue
Currently, file sorting (by Name, Size, or Date) in `FileExplorerScreen.kt` has fixed direction defaults (Name = Ascending, Size = Descending, Date = Descending). Users cannot toggle or customize whether the sort order is Ascending or Descending.

## List of Files to be Changed
- `app/src/main/res/values/strings.xml`: Add string resources `sort_ascending` ("Ascending") and `sort_descending` ("Descending").
- `app/src/main/res/values-ml/strings.xml`: Add Malayalam string resources `sort_ascending` ("ആരോഹണം") and `sort_descending` ("അവരോഹണം").
- `app/src/main/java/in/sreerajp/vault_files/ui/FileExplorerScreen.kt`:
  - Add state variable `var sortAscending by remember { mutableStateOf(true) }`.
  - Update `displayedFiles` comparator logic to respect `sortAscending` (reversing direction when `sortAscending` is false, keeping folders at top).
  - Update `showSortByDialog` AlertDialog to display direction indicators (`↑`/`↓` icons) alongside checkmarks and offer interactive `FilterChip` / option buttons for selecting Ascending vs. Descending order.

## Plan for the Fix
1. Add strings in `values/strings.xml` and `values-ml/strings.xml` for Ascending and Descending.
2. In `FileExplorerScreen.kt`:
   - Define state `var sortAscending by remember { mutableStateOf(true) }`.
   - Update `displayedFiles` comparator so `sortAscending` controls whether `thenBy` or `thenByDescending` is applied to file properties, while folders remain listed first.
   - Extend the "Sort By" dialog to show current sort direction arrow icon (`ArrowUpward` / `ArrowDownward`) next to the active option, and add a toggle row for selecting Ascending or Descending order.
3. Test build via `./gradlew testDebugUnitTest`.
