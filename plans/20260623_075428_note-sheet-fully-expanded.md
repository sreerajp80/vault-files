# Plan: Open secure-note sheets fully expanded

Date: 2026-06-23 07:54:28 (local)

## Issue

When tapping the secure-note button, the `ModalBottomSheet` opens in its **partially expanded**
(half) state, so the user must drag it upward to see the form. Same applies to the read-only
note viewer sheet.

## Cause

`rememberModalBottomSheetState()` defaults to `skipPartiallyExpanded = false`, which introduces a
half-height detent that the sheet rests at on open.

## Fix

Pass `skipPartiallyExpanded = true` to the state for both sheets so they open straight to full
(content-sized ~75%) height — no manual drag needed.

## Files to change

- `app/src/main/java/com/example/ui/FileExplorerScreen.kt`
  - Creator sheet: `rememberModalBottomSheetState(skipPartiallyExpanded = true)`.
  - Viewer sheet: `rememberModalBottomSheetState(skipPartiallyExpanded = true)`.

No other logic, strings, or dependencies change.

---
Per the workflow rules I will not change any project file until you approve. **Do you approve this plan?**
