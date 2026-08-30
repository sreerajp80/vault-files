# Change Log: Add Ascending/Descending Sort Order Toggle (Option 1)

## Implemented Plan
Implements plan [plans/20260801_200000_sort_order_toggle.md](file:///l:/Android/vault-files/plans/20260801_200000_sort_order_toggle.md).

## What Was Changed
1. **String Resources**:
   - `app/src/main/res/values/strings.xml`: Added `sort_ascending` ("Ascending") and `sort_descending` ("Descending").
   - `app/src/main/res/values-ml/strings.xml`: Added `sort_ascending` ("ആരോഹണം") and `sort_descending` ("അവരോഹണം").
2. **State & Comparator Logic**:
   - `app/src/main/java/in/sreerajp/vault_files/ui/FileExplorerScreen.kt`: Added `sortAscending` state variable and updated `displayedFiles` comparator to support both Ascending and Descending sorting across Name, Size, and Date sort modes while keeping folders listed first.
3. **Sort By Dialog UI**:
   - `app/src/main/java/in/sreerajp/vault_files/ui/FileExplorerScreen.kt`: Extended `showSortByDialog` AlertDialog to display direction arrow icons (`ArrowUpward` / `ArrowDownward`) next to the active option, toggle direction when tapping the active option, and provide FilterChips for Ascending / Descending order selection.
