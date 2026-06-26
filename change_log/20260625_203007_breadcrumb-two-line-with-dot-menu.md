# Breadcrumb moved onto the dot-menu row as a two-line block

Implements plan `plans/20260625_203007_breadcrumb-two-line-with-dot-menu.md`.

## What changed

`app/src/main/java/com/example/ui/FileExplorerScreen.kt`:

1. **Removed** the single-line breadcrumb `Row` (path › folder count · size) that was
   rendered under the "Files" title in the header title area (non-filtered case). The
   `isCategoryFiltered` filter-chip branch in the header is unchanged.

2. **In the "Section header + options (view/sort) menu" `Row`** (the row containing the
   "⋮" MoreVert dot menu), replaced the non-filtered left-side
   `Spacer(weight 1f)` with a two-line `Column(weight 1f)`:
   - **Line 1:** a `Row` with the optional `ChevronLeft` (shown only when `!isAtRoot`,
     whole column clickable → `viewModel.navigateUp()`) and the path text
     (`getDisplayPath(...)`, SemiBold, primary, single line, ellipsis, 13.5sp).
   - **Line 2:** `files_breadcrumb_summary` formatted with the folder-count plural and
     `formatBytes(totalSize)` (onSurfaceVariant, single line, ellipsis, 12sp).
   - The column carries the existing `testTag("files_breadcrumb")`.

   The `isCategoryFiltered` "ALL X" header branch is unchanged. The row keeps
   `verticalAlignment = Alignment.CenterVertically` so the "⋮" button stays centered
   against the two-line block.

## Result

The path now appears on the first line and the folder count + size on the second line,
both on the same row as the "⋮" dot menu, instead of a single line under the "Files"
title.

## Notes

- No string resource changes (`files_breadcrumb_summary` reused for line 2).
- No changes to navigation, sort, view-type, or create behavior.
- `compileDebugKotlin` succeeds.
