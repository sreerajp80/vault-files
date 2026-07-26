# Change Log: Redesign Save Shared File(s) and Folder Picker Dialogs

- **Date**: 2026-07-26
- **Implemented Plan**: `plans/20260726_170800_redesign_save_shared_dialogs.md`

## Summary of Changes
1. **Redesigned `FolderPickerDialog` (`FileExplorerScreen.kt`)**:
   - Replaced raw Compose `FilterChip` elements with equal-width (`Modifier.weight(1f)`) custom pills (`SourcePill`) to prevent text wrapping on long localized strings like Malayalam `"ഉപകരണ സ്റ്റോറേജ്"`.
   - Enclosed active folder directory path in a styled `Surface` container with a primary-tinted folder icon.
   - Enhanced subdirectory listing rows with rounded shapes, icons, and clear parent folder navigation (`picker_parent_folder`).
   - Added a centered empty state illustration and text when no subfolders exist.
   - Updated confirm button ("Save here" / "Move here" / "Copy here") to a primary `Button` style and cancel to an `OutlinedButton`.
2. **Redesigned `ShareDestinationDialog` (`MainActivity.kt`)**:
   - Replaced plain text buttons with option cards featuring lock (`Lock`) and folder (`FolderOpen`) icons, clear title typography, and descriptive subtitles ("Store securely inside the encrypted app vault", "Browse and pick a target destination folder").
   - Added a top icon badge (`FileUpload`) and cancel button for dialog dismissal.
3. **Added Malayalam Translations (`values-ml/strings.xml`)**:
   - Added Malayalam localized string resources for `share_dest_title`, `share_dest_message`, `share_dest_vault`, `share_dest_vault_subtitle`, `share_dest_choose_folder`, `share_dest_choose_folder_subtitle`, `share_pick_folder_title`, `share_save_here`, `picker_parent_folder`, and result toast messages.

## Verification
- `./gradlew assembleDebug` passed cleanly.
- `./gradlew testDebugUnitTest` passed cleanly.
