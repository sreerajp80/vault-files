# Selection toolbar: show count as number only

Implements plan `plans/20260626_210000_selection-toolbar-number-only.md`.

## Change
- `app/src/main/java/com/example/ui/FileExplorerScreen.kt`
  - The long-press selection toolbar count `Text` now renders `count.toString()`
    instead of `stringResource(R.string.files_selected_count, count)`, so it shows
    just the number (e.g. "1") rather than "1 selected" / "1 തിരഞ്ഞെടുത്തു".

## Scope
- String resources (`files_selected_count` in `values/` and `values-ml/`) left
  untouched — still used by the multi-select context-menu dialog title.
