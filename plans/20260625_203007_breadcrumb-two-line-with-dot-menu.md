# Move breadcrumb onto the dot-menu row as a two-line block

## Issue

In the Files (File Explorer) header, the directory breadcrumb is currently rendered on a
single horizontal line directly under the "Files" title:

```
Primary Storage  ›  18 folders · 69.7 GB
```

(implemented at `FileExplorerScreen.kt` lines ~252–300, inside the header title Row).

Separately, the options "⋮" (MoreVert) dot menu lives in its own row further down
(the "Section header + options menu" row, lines ~440–504), whose left side is just an
empty `Spacer(weight 1f)` when no category filter is active.

The request: bring the breadcrumb onto the **same row as the dot menu**, laid out in
**two lines**:
- **Line 1:** the path (e.g. "Primary Storage"), with the existing back-navigation
  affordance.
- **Line 2:** the folder count and size (e.g. "18 folders · 69.7 GB").

This only affects the non-category-filtered state (the state shown in the screenshot).
The category-filtered state (filter chip in header + "ALL X" section header) stays as-is.

## Files to change

- `app/src/main/java/com/example/ui/FileExplorerScreen.kt`

(No string resource changes needed — `files_breadcrumb_summary` `"%1$s · %2$s"` is reused
for line 2.)

## Plan

1. **Remove** the single-line breadcrumb `Row` from the header title area
   (the `else { ... }` block at lines ~251–300). The `isCategoryFiltered` filter-chip
   branch above it is unchanged. When not filtered, the header then shows just the
   "Files" title (the breadcrumb moves down to the dot-menu row).

2. **In the "Section header + options menu" `Row`** (lines ~440–504), replace the
   non-filtered left-side `Spacer(modifier = Modifier.weight(1f))` with a
   `Column(weight 1f)` carrying the breadcrumb as two lines:
   - **Line 1 (`Row`):** optional `ChevronLeft` icon shown only when `!isAtRoot`
     (clickable → `viewModel.navigateUp()`), followed by the path text
     `getDisplayPath(viewModel.userStorageRoot, currentDir)` (SemiBold, primary color,
     single line, ellipsis). Carries the existing `testTag("files_breadcrumb")` so UI
     tests still resolve it; the clickable/navigateUp behavior is preserved.
   - **Line 2 (`Text`):** `files_breadcrumb_summary` formatted with
     `pluralStringResource(R.plurals.folder_count, folderCount, folderCount)` and
     `formatBytes(totalSize)` (smaller, onSurfaceVariant color, single line, ellipsis).
   - The `isCategoryFiltered` branch (the "ALL X" header `Text`) is left unchanged.

3. Keep the row's `verticalAlignment = Alignment.CenterVertically` so the "⋮" button
   stays vertically centered against the two-line block. Adjust the row's top padding
   slightly if needed so the two-line block has comfortable spacing (minor visual tweak
   only).

## Notes / risks

- No behavior change to navigation, sorting, view-type, or create actions.
- Existing `testTag`s (`files_breadcrumb`, `files_options_menu`) are preserved.
- Category-filter UI is untouched.
