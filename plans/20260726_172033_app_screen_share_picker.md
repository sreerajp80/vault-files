# Implementation Plan: In-App Screen Selection for Shared Files

## Files to be changed:
- `plans/20260726_172033_app_screen_share_picker.md` [NEW]
- `app/src/main/java/in/sreerajp/vault_files/MainActivity.kt` [MODIFY]
- `app/src/main/res/values/strings.xml` [MODIFY]
- `app/src/main/res/values-ml/strings.xml` [MODIFY]

## Issue Description:
When files are shared into Vault Files from another app (via `ACTION_SEND` / `ACTION_SEND_MULTIPLE`), the app currently displays a modal dialog popup (`ShareDestinationDialog` asking "Vault or Choose Folder", followed by `FolderPickerDialog` dialog). 

The user wants to eliminate this extra modal dialog screen and instead allow selecting the destination folder directly on the app screens themselves, with a single extra **OK** / **Cancel** bar when arriving from a share intent.

## Plan for Fix:

1. **Remove Modal Share Dialogs from Share Flow:**
   - In `MainActivity.kt`, remove `ShareDestinationDialog` popup and modal `FolderPickerDialog` share popups.

2. **In-App Screen Folder/Vault Navigation for Shared Files:**
   - When `viewModel.pendingSharedImports` has shared files, ensure the app opens onto the main app interface (Files tab / `FileExplorerScreen` by default).
   - Allow full, normal navigation on the main app screen (browsing folders, switching between App Storage and Device Storage, or switching to the Vault tab).

3. **Share Confirmation Bottom Bar:**
   - Add a `ShareConfirmationBottomBar` composable in `MainActivity.kt` shown when `pendingSharedImports` is active.
   - Show file count & current destination target (e.g. current folder name in Files tab, or "Encrypted Vault" in Vault tab).
   - Include two buttons:
     - **Cancel** ("റദ്ദാക്കുക" / "Cancel"): calls `viewModel.clearPendingSharedImports()`.
     - **OK / Save Here** ("ഇവിടെ സംരക്ഷിക്കുക" / "Save Here"):
       - If on Vault tab (`activeTabIndex == 2`): calls `viewModel.importSharedToVault(shares)`.
       - If on Files tab (or other tab): calls `viewModel.importSharedToFolder(shares, currentDirectory.value)`.

4. **Back Navigation & Edge Cases:**
   - Update `BackHandler` so pressing Back at root in share mode clears pending imports.
