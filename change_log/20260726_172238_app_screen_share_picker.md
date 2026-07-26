# Change Log: In-App Screen Selection for Shared Files

**Plan Reference:** `plans/20260726_172033_app_screen_share_picker.md`

## Summary of Changes
- Replaced extra modal dialog screens (`ShareDestinationDialog` / `FolderPickerDialog`) when receiving files shared from other apps (`ACTION_SEND` / `ACTION_SEND_MULTIPLE`).
- Implemented in-app screen selection for shared files: users can browse the main app screens directly (Files tab or Vault tab), navigate subfolders, and switch between App Storage and Device Storage.
- Added `ShareConfirmationBottomBar` composable in `MainActivity.kt` displaying:
  - Title and file count (e.g., `Save N shared file(s)` / `%1$d പങ്കിട്ട ഫയൽ(കൾ) സംരക്ഷിക്കുക`).
  - Target destination name (`To: <Folder Name>` or `Encrypted Vault`).
  - **Save Here** (OK) button (`importSharedToFolder` or `importSharedToVault`).
  - **Cancel** button (`clearPendingSharedImports`).
- Updated `BackHandler` logic to handle exiting share mode cleanly when Back is pressed at the root level.
- Added localized string resources for English (`values/strings.xml`) and Malayalam (`values-ml/strings.xml`).
