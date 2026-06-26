# File Explorer cleanups: overflow menu, redundant dialogs, selection retention, breadcrumb file count, selected-count label, viewing message

Six cleanups to the File Explorer experience.

## Issue 1 — Two overflow (`⋮`) menus during selection

When items are selected, two three-dot menus appear stacked:

1. **Selection toolbar** `⋮` — actions on the selected items (correct/relevant).
2. **Breadcrumb / section-header row** `⋮` — view-type, sort-by, create. This row
   renders whenever `!needsPermission`, regardless of selection mode, so its menu
   shows even though those options are irrelevant while acting on a selection.

### Fix (option A)
Keep the breadcrumb visible for context, but hide **only its overflow menu** while in
selection mode, so the selection toolbar owns the only `⋮` on screen.

## Issue 2 — Redundant confirmation dialog before Share / Open With / Copy path

`SHARE`, `OPEN_WITH`, and `COPY_PATH` route through `pendingConfirm` (an in-app
confirmation dialog) before acting:

- `SHARE` / `OPEN_WITH` → already present the Android system app-chooser (a list of
  apps), so the extra dialog is redundant.
- `COPY_PATH` → copies to the clipboard instantly and surfaces a message afterward,
  so a confirmation dialog is just friction.

### Fix
For `SHARE`, `OPEN_WITH`, and `COPY_PATH`, skip `pendingConfirm` and invoke the
action directly, then `clear()`. (Other actions that have real/destructive side
effects keep their confirmation.)

## Issue 3 — Selection lost when an action is cancelled

The selection should persist until the user actually performs an operation or taps to
deselect. Most actions already retain it on cancel (their `onDismiss` just closes the
dialog), but three paths clear it **up front**, before the user can cancel, and one
clears it for a non-operation:

- `COMPRESS` (line ~377): `clear()` runs immediately, then the zip-name dialog opens —
  cancelling the dialog loses the selection.
- `DELETE` with phone-lock (line ~381): `clear()` runs before biometric/PIN
  validation — cancelling auth loses the selection.
- `MOVE_VAULT` with phone-lock (line ~399): same pattern.
- `INFO` (line ~371): clears on opening the read-only details view, which is not an
  operation. Per the agreed behavior, the selection should be kept here too.

### Fix
Move each `clear()` to *after* the operation actually runs, and drop it for `INFO`:

- `COMPRESS`: remove the upfront `clear()`; clear inside the zip dialog's confirm
  handler after compressing (not on its dismiss).
- `DELETE` / `MOVE_VAULT` phone-lock paths: remove the upfront `clear()`; add `clear()`
  to the `PendingAction.onValidated` lambda so it runs only on successful validation.
  Cancelling the auth dialog already only sets `activeActionPendingValidation = null`,
  so the selection is retained.
- `INFO`: remove the `clear()` call so opening details keeps the selection.

## Issue 4 — Breadcrumb summary shows only folder count, not file count

The breadcrumb summary line (e.g. `10 folders · 67.6 GB`) shows folder count and total
size but omits the number of files in the current folder.

### Fix
Add a file count so the line reads `<folders> · <files> · <size>`
(e.g. `10 folders · 24 files · 67.6 GB`):

- Compute `fileCount = filesList.count { !it.isDirectory }` next to the existing
  `folderCount` (~line 271).
- Add a `file_count` plural to `values/strings.xml` and `values-ml/strings.xml`.
- Change `files_breadcrumb_summary` from `%1$s · %2$s` to `%1$s · %2$s · %3$s` in both
  resource files.
- Update the breadcrumb call (~lines 637–641) to pass folder-count, file-count, size.

## Issue 5 — Selection count shows "selected / total"

The selection toolbar shows `selected / total` (e.g. `1 / 15`). Only the selected count
is wanted (e.g. `1 selected`).

### Fix
Switch the toolbar count (~lines 2094–2095) from `files_selected_count_total`
(`%1$d / %2$d`) to the existing `files_selected_count` (`%1$d selected`), passing only
`count`. Drop the now-unused `totalCount` parameter from `SelectionToolbar` and its
call site (~line 468). The `files_selected_count_total` string becomes unused.

## Issue 6 — "Viewing:" prefix on the non-previewable-file message

Tapping a file with no in-app viewer dispatches `msg_viewing_file`
(`Viewing: %1$s (%2$s)`). The `Viewing:` prefix is unwanted; the message should show
just the file name and size, e.g. `Veda.pdf (170.9 KB)`.

### Fix
Change `msg_viewing_file` to `%1$s (%2$s)` in `values/strings.xml` and the Malayalam
equivalent (drop `കാണുന്നു: `) in `values-ml/strings.xml`. No code change — the call
site at ~line 864 already passes name and size.

## Files to change

- `app/src/main/java/com/example/ui/FileExplorerScreen.kt`
  - **Issue 1** (~lines 649–692): wrap the section-header options-menu `Box`
    (`IconButton` + `DropdownMenu` for view-type / sort-by / create) in
    `if (!selectionMode) { … }`.
  - **Issue 2** (~lines 428–432, 448–451, 456–459):
    - `SelectionAction.SHARE` → `{ shareItems(context, items, viewModel); clear() }`
      directly (no `pendingConfirm`).
    - `SelectionAction.OPEN_WITH` → `{ single?.let { openWithChooser(context, it, viewModel) }; clear() }`
      directly (no `pendingConfirm`).
    - `SelectionAction.COPY_PATH` → `{ copyPathToClipboard(context, items, viewModel); clear() }`
      directly (no `pendingConfirm`).
  - **Issue 3**:
    - `SelectionAction.INFO` (~line 371): remove `clear()`.
    - `SelectionAction.COMPRESS` (~line 377): change to `{ showZipDialogForItems = items }`
      (drop `clear()`); in the zip dialog confirm handler (~line 1415) add
      `selectedPaths = emptySet()` after a successful compress.
    - `SelectionAction.DELETE` phone-lock branch (~lines 380–387): remove the upfront
      `clear()`; append `clear()` to `onValidated`.
    - `SelectionAction.MOVE_VAULT` phone-lock branch (~lines 398–405): remove the
      upfront `clear()`; append `clear()` to `onValidated`.
  - **Issue 4**: add `fileCount` (~line 271) and update the breadcrumb summary call
    (~lines 637–641).
  - **Issue 5**: use `files_selected_count` with `count` only (~lines 2094–2095);
    remove the `totalCount` param from `SelectionToolbar` and its call site (~line 468).
- `app/src/main/res/values/strings.xml` — add `file_count` plural; change
  `files_breadcrumb_summary` to `%1$s · %2$s · %3$s`; change `msg_viewing_file` to
  `%1$s (%2$s)`.
- `app/src/main/res/values-ml/strings.xml` — add `file_count` plural (Malayalam);
  change `files_breadcrumb_summary` to `%1$s · %2$s · %3$s`; change `msg_viewing_file`
  to `%1$s (%2$s)`.

## Notes / scope

- No ViewModel or repository changes.
- The `confirm_share` / `confirm_share_multi` / `confirm_open_with` /
  `confirm_copy_path` strings, and `files_selected_count_total`, become unused but will
  be left in place (harmless) unless you want them removed.
- Breadcrumb text + up-navigation behavior unchanged.
