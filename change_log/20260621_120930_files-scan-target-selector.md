# Change log: Add Scan Target Location selector to the Files section

Implements plan `plans/20260621_120350_files-scan-target-selector.md`.

## What changed
- `app/src/main/java/com/example/ui/FileExplorerScreen.kt`
  - Added a "Scan Target Location" card with two `FilterChip`s (**App Sandbox** /
    **Entire Device**) to the top of the Files screen, inside the existing `Column`
    (below the hidden-unlock banner, above the file list `Box`).
  - Chips read `viewModel.storageSourceMode` and call
    `viewModel.updateStorageSourceMode("sandbox" | "device")` on click — identical wiring to
    the Storage Analysis screen, so both screens share state and stay in sync.
  - Used distinct test tags to avoid collisions with the analyzer:
    `files_storage_source_card`, `files_select_sandbox_chip`, `files_select_device_chip`.

## Not changed
- `StorageViewModel.kt` / repository — no changes; existing shared StateFlow/setter reused.
- The Storage Analysis screen's existing selector — untouched.
- Permission flow — unchanged; selecting "Entire Device" without access still shows the
  existing permission-request state.

## Verification
- `./gradlew assembleDebug` → BUILD SUCCESSFUL.
- No new imports required (existing wildcard imports cover `FilterChip`, `Card`, and the
  `Check`/`Dns`/`PhoneAndroid` icons).
