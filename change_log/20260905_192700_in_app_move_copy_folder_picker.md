# Change Log: In-App Screen Selection for Move and Copy

**Date:** 2026-09-05
**Reference Plan:** [plans/20260905_192700_in_app_move_copy_folder_picker.md](plans/20260905_192700_in_app_move_copy_folder_picker.md)

## Summary of Changes:

1. **Replaced Modal Dialogs with In-App Folder Browsing:**
   - Removed `MoveCopyPickerDialog` and `FolderPickerDialog` modal dialogs from `FileExplorerScreen.kt`.
   - Replaced with the in-app screen browsing flow identical to the experience when receiving files shared from external apps.
   - When a user chooses "Move to" or "Copy to", the file explorer remains open in normal full-screen browsing mode with search, folder navigation, breadcrumbs, and storage switching.

2. **Added Move/Copy Confirmation Bottom Bar:**
   - Created `MoveCopyConfirmationBottomBar` in `MainActivity.kt` matching the design and layout of `ShareConfirmationBottomBar`.
   - Displays operation icon, item count title ("Move X item(s)" or "Copy X item(s)"), dynamic destination folder name ("To: folder_name"), Cancel button, and action button ("Move here" or "Copy here").
   - Integrated with `MainActivity`'s bottom bar container above the main navigation bar.
   - Updated system back navigation handler: pressing Back within subfolders navigates up, and pressing Back at root or tapping Cancel cancels the move/copy operation.

3. **ViewModel State Management:**
   - Added `MoveCopyRequest` data class and `pendingMoveCopy: StateFlow<MoveCopyRequest?>` in `StorageViewModel.kt`.
   - Added `setPendingMoveCopy` and `clearPendingMoveCopy` methods to control the active state.

4. **Directory Nesting Safety:**
   - Updated `moveFileOrFolder` and `copyFileOrFolder` in `StorageRepository.kt` to guard against moving or copying a directory into itself or any of its own subdirectories.
   - Made `cryptoManager` initialization lazy in `StorageRepository.kt` to improve startup performance and avoid unnecessary keystore access during unit tests.

5. **String Resources:**
   - Added `move_bar_title`, `copy_bar_title`, and `msg_copy_vault_unsupported` to `app/src/main/res/values/strings.xml` and `app/src/main/res/values-ml/strings.xml`.

6. **Unit Tests:**
   - Updated `AppConfigTest.kt` version and build values to match `app_config.json`.
   - Added `MoveCopyStateTest.kt` verifying `MoveCopyRequest` model, `pendingMoveCopy` state management in `StorageViewModel`, and directory nesting prevention in `StorageRepository`.
   - Verified that all unit tests (`./gradlew testDebugUnitTest`) pass and release APK (`./gradlew assembleRelease`) builds with zero lint errors.
