# Change log: Open secure-note sheets fully expanded

Date: 2026-06-23 07:55:32 (local)
Implements: [plans/20260623_075428_note-sheet-fully-expanded.md](../plans/20260623_075428_note-sheet-fully-expanded.md)

## What changed

- `FileExplorerScreen.kt`: both secure-note `ModalBottomSheet`s now use
  `rememberModalBottomSheetState(skipPartiallyExpanded = true)` so they open straight to full
  height instead of resting at the half-expanded detent (which forced the user to drag the sheet
  up).
  - Creator sheet (new secure note).
  - Viewer sheet (read-only decrypted note).

No logic, strings, or dependencies changed.

## Verification
- `./gradlew compileDebugKotlin` → BUILD SUCCESS.
