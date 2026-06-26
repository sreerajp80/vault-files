# Selection toolbar: show count as number only

## Issue
The long-press selection toolbar shows the count as "1 selected" / "1 തിരഞ്ഞെടുത്തു"
(via `R.string.files_selected_count`, `"%1$d selected"`). The user wants only the
number shown in the toolbar — no "selected" word — to keep it compact and
language-neutral.

## Files to change
- `app/src/main/java/com/example/ui/FileExplorerScreen.kt` — the selection toolbar
  `Text` at ~line 2083.

## Plan
- Replace the toolbar `Text` value
  `stringResource(R.string.files_selected_count, count)` with just `count.toString()`.
- Leave the string resources (`files_selected_count` in both `values/` and
  `values-ml/`) untouched — they're still used by the multi-select context-menu
  dialog title (~line 2230), which is out of scope for this change.
- No string-resource edits, no other call sites touched.

## Scope notes
- Only the toolbar "here" (the long-press selection bar) changes.
- The dialog title at ~line 2230 keeps the descriptive "N selected" wording.
