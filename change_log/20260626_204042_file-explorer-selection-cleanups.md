# File Explorer cleanups (selection UI, breadcrumb, viewing message)

Implements plan `plans/20260626_202055_selection-mode-menu-cleanup.md`.

## Changes

### Issue 1 — duplicate overflow menu during selection
- `FileExplorerScreen.kt`: the section-header (breadcrumb) options menu `Box`
  (view-type / sort-by / create) is now wrapped in `if (!selectionMode)`, so during
  selection only the selection toolbar's overflow `⋮` is shown.

### Issue 2 — redundant dialogs before Share / Open With / Copy path
- `FileExplorerScreen.kt`: `SHARE`, `OPEN_WITH`, and `COPY_PATH` no longer route through
  the generic `pendingConfirm` dialog. They invoke the action (system app-chooser /
  clipboard copy) directly and then clear the selection.

### Issue 3 — selection retained when an action is cancelled
- `FileExplorerScreen.kt`:
  - `INFO`: removed the `clear()` call so opening details keeps the selection.
  - `COMPRESS`: removed the upfront `clear()`; the selection is now cleared inside the
    zip-name dialog's confirm handler after a successful compress (cancelling keeps it).
  - `DELETE` / `MOVE_VAULT` phone-lock branches: removed the upfront `clear()` and moved
    it into `PendingAction.onValidated`, so the selection is only cleared after
    successful biometric/PIN validation (cancelling auth keeps it).
  - Other actions already retained the selection on cancel and were unchanged.

### Issue 4 — breadcrumb now shows file count
- `FileExplorerScreen.kt`: added `fileCount = filesList.count { !it.isDirectory }` and
  pass it to the breadcrumb summary.
- `values/strings.xml`, `values-ml/strings.xml`: added a `file_count` plural and changed
  `files_breadcrumb_summary` from `%1$s · %2$s` to `%1$s · %2$s · %3$s`. Summary now
  reads `<folders> · <files> · <size>`.

### Issue 5 — selection count shows only the selected count
- `FileExplorerScreen.kt`: the toolbar now uses `files_selected_count` (`%1$d selected`)
  with only `count`; the unused `totalCount` parameter was removed from `SelectionToolbar`
  and its call site.

### Issue 6 — "Viewing:" prefix removed
- `values/strings.xml`, `values-ml/strings.xml`: `msg_viewing_file` changed from
  `Viewing: %1$s (%2$s)` (and the Malayalam `കാണുന്നു: …`) to `%1$s (%2$s)`.

## Notes
- Now-unused string resources (`confirm_share`, `confirm_share_multi`,
  `confirm_open_with`, `confirm_copy_path`, `files_selected_count_total`) were left in
  place as agreed.
- Verified with `./gradlew compileDebugKotlin` — BUILD SUCCESSFUL.
