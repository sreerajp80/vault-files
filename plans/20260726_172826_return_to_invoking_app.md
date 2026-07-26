# Implementation Plan: Return to Invoking App on Share Finish / Cancel

## Files to be changed:
- `plans/20260726_172826_return_to_invoking_app.md` [NEW]
- `app/src/main/java/in/sreerajp/vault_files/ui/StorageViewModel.kt` [MODIFY]
- `app/src/main/java/in/sreerajp/vault_files/MainActivity.kt` [MODIFY]

## Issue Description:
When Vault Files is opened via a share intent from another app (`ACTION_SEND` / `ACTION_SEND_MULTIPLE`), clicking **Cancel** or **OK / Save** currently keeps the app open instead of finishing and returning the user back to the invoking app (e.g., WhatsApp, Gallery, File Manager).

## Plan for Fix:

1. **StorageViewModel Update (`StorageViewModel.kt`):**
   - Add an optional `onComplete: (() -> Unit)? = null` parameter to `importSharedToFolder` and `importSharedToVault`.
   - Invoke `onComplete?.invoke()` after the shared files are saved and state is refreshed.

2. **MainActivity Finish Handling (`MainActivity.kt`):**
   - Update **Cancel** button action in `ShareConfirmationBottomBar` and `BackHandler` (when `pendingShares != null`) to call `viewModel.clearPendingSharedImports()` followed by `finish()`.
   - Update **OK / Save** button action to call `importSharedToVault(shares) { finish() }` or `importSharedToFolder(shares, currentDir) { finish() }`.
   - This ensures the activity finishes and returns the user to the invoking app as soon as saving completes or is cancelled.
