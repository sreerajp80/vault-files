# Implementation Plan: In-App Screen Selection for Move and Copy

**Status:** Completed

## Files to be changed:
- `plans/20260905_192700_in_app_move_copy_folder_picker.md` [NEW]
- `app/src/main/java/in/sreerajp/vault_files/ui/StorageViewModel.kt` [MODIFY]
- `app/src/main/java/in/sreerajp/vault_files/ui/FileExplorerScreen.kt` [MODIFY]
- `app/src/main/java/in/sreerajp/vault_files/MainActivity.kt` [MODIFY]
- `app/src/main/java/in/sreerajp/vault_files/data/StorageRepository.kt` [MODIFY]
- `app/src/main/res/values/strings.xml` [MODIFY]
- `app/src/main/res/values-ml/strings.xml` [MODIFY]
- `app/src/test/java/in/sreerajp/vault_files/AppConfigTest.kt` [MODIFY]

## Issue Description:
When files are shared into Vault Files from an external application for saving, the app allows the user to browse destination folders directly on the full app screens (`FileExplorerScreen` / `VaultScreen`) with a persistent bottom confirmation bar (`ShareConfirmationBottomBar`).

However, when moving or copying files inside the app, the app currently opens a cramped modal dialog popup (`MoveCopyPickerDialog` / `FolderPickerDialog`) with a mini-list of directories. The user wants the move/copy destination browsing experience to be the same as the external file sharing flow: navigating full app screens directly with a bottom confirmation bar.

## Plan for Fix:

1. **State Management in `StorageViewModel.kt`:**
   - Define `MoveCopyRequest(items: List<FileItem>, isMove: Boolean)` in or accessible to `StorageViewModel`.
   - Add `_pendingMoveCopy: MutableStateFlow<MoveCopyRequest?>` and public `pendingMoveCopy: StateFlow<MoveCopyRequest?>`.
   - Add helper functions `setPendingMoveCopy(request: MoveCopyRequest)` and `clearPendingMoveCopy()`.

2. **Trigger Move/Copy Mode in `FileExplorerScreen.kt`:**
   - In `SelectionAction.MOVE_TO` and `SelectionAction.COPY_TO`:
     - Clear category filter if active (`viewModel.clearCategoryFilter()`).
     - Set `viewModel.setPendingMoveCopy(...)`.
     - Clear item selection (`clear()`) so the user can freely browse folders.
   - Remove modal dialog states (`moveCopyRequest`) and remove deprecated modal composables `MoveCopyPickerDialog` and `FolderPickerDialog`.

3. **In-App Confirmation Bottom Bar in `MainActivity.kt`:**
   - Add `MoveCopyConfirmationBottomBar` matching the exact design and structure of `ShareConfirmationBottomBar`.
   - When `pendingMoveCopy` is active, display `MoveCopyConfirmationBottomBar` above the bottom navigation bar.
   - Show action icon (move or copy), item count title (`move_bar_title` / `copy_bar_title`), destination target folder ("To: folder_name"), a Cancel button, and a confirm button ("Move here" / "Copy here").
   - Confirming performs `viewModel.moveItemsTo(...)` or `viewModel.copyItemsTo(...)` into `currentDir` (or moves into vault if on Vault tab), then clears `pendingMoveCopy`.
   - Dismissing or pressing Back when at root clears `pendingMoveCopy`.

4. **Directory Nesting Safety in `StorageRepository.kt`:**
   - Prevent moving or copying a folder into itself or any of its own subdirectories.

5. **String Resources:**
   - Add localized strings for `move_bar_title`, `copy_bar_title`, and `msg_copy_vault_unsupported` in both `res/values/strings.xml` and `res/values-ml/strings.xml`.

6. **Tests & Verification:**
   - Update `AppConfigTest.kt` to match the current version configuration in `app_config.json`.
   - Run `./gradlew testDebugUnitTest` to verify all unit tests pass.
