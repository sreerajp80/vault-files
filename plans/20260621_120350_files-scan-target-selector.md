# Plan: Add Scan Target Location selector to the Files section

## Issue / Request
The "Scan Target Location" selector (App Sandbox / Entire Device) currently exists only on
the **Storage Analysis** screen (`StorageAnalyzerScreen.kt`). The user wants the same selector
available in the **Files** section (`FileExplorerScreen.kt`) so the storage source location can
be changed from within the file explorer itself, without switching tabs.

## Background (why this is low-risk)
- The selector is purely a thin UI over shared ViewModel state:
  - reads `viewModel.storageSourceMode` (StateFlow, default `"sandbox"`)
  - writes via `viewModel.updateStorageSourceMode("sandbox" | "device")`
- `FileExplorerScreen` already collects `storageSourceMode` (line 57) and already handles the
  `device` + no-permission case. So the two screens will stay perfectly in sync automatically —
  no ViewModel/repository changes are needed.

## Files to change
1. `app/src/main/java/com/example/ui/FileExplorerScreen.kt`
   - Add a compact "Scan Target Location" chip row (same two `FilterChip`s as the analyzer:
     **App Sandbox** with `Dns`/`Check` icon, **Entire Device** with `PhoneAndroid`/`Check` icon)
     near the top of the file list, inside the existing `Column` (above the file list `Box`,
     and below/around the hidden-unlock banner).
   - Wire `onClick` to `viewModel.updateStorageSourceMode(...)` exactly like the analyzer.
   - Use **distinct** `testTag`s to avoid collisions with the analyzer's tags:
     `files_storage_source_card`, `files_select_sandbox_chip`, `files_select_device_chip`.
   - Required imports (`FilterChip`, icons `Check`, `Dns`, `PhoneAndroid`, `Card`, `RoundedCornerShape`)
     are already covered by the existing wildcard imports (`material3.*`,
     `material.icons.filled.*`, `RoundedCornerShape`). No new import expected; will confirm at edit time.

## Explicitly NOT changing
- `StorageViewModel.kt` / repository — shared state already does the job.
- The analyzer screen's existing selector — left as-is.
- No behavior change to permission flow; switching to "Entire Device" in Files will keep showing
  the existing permission-request card when access isn't granted.

## Verification
- Build: `./gradlew assembleDebug`
- Manual: open Files tab, toggle between App Sandbox / Entire Device, confirm the list reloads
  from root and that the choice is reflected on the Storage Analysis tab too (shared state).

## Change log
- After implementation, write `change_log/20260621_hhMMss_files-scan-target-selector.md`.
