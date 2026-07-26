# Redesign Save Shared File(s) and Folder Picker Dialogs

## Issue
1. **Filter Chip Line Wrapping**: In `FolderPickerDialog`, raw Compose `FilterChip` elements inside a `Row` lack proper width constraints and weight distribution. In non-English locales like Malayalam (`values-ml`), long text such as "ഉപകരണ സ്റ്റോറേജ്" (Device Storage) causes awkward multi-line wrapping inside the chip outline, rendering it misaligned and visually broken.
2. **Unstyled Dialog Controls**: `ShareDestinationDialog` and `FolderPickerDialog` use basic Material `AlertDialog` text buttons without distinct visual hierarchy, background styling, or proper spacing.
3. **Missing Malayalam Translations**: Share dialog string resources (`share_dest_title`, `share_dest_message`, `share_dest_vault`, `share_dest_choose_folder`, `share_pick_folder_title`, `share_save_here`) are missing from `values-ml/strings.xml`, causing string fallback / language mix-ups.

## Files to be Changed
- `l:\Android\vault-files\app\src\main\res\values-ml\strings.xml`
- `l:\Android\vault-files\app\src\main\res\values\strings.xml`
- `l:\Android\vault-files\app\src\main\java\in\sreerajp\vault_files\ui\FileExplorerScreen.kt`
- `l:\Android\vault-files\app\src\main\java\in\sreerajp\vault_files\MainActivity.kt`

## Plan for the Fix
1. **Add Malayalam Translations**:
   - Add missing `share_...` string keys to `values-ml/strings.xml` so all dialog options render properly in Malayalam when selected.
2. **Redesign `FolderPickerDialog` (`FileExplorerScreen.kt`)**:
   - Replace standard `FilterChip`s with equal-width (`Modifier.weight(1f)`) custom root selection pills (matching the app's `SourcePill` design with rounded corners, icon + single line text with `TextOverflow.Ellipsis`, selected primary container highlights, and subtle borders).
   - Display the current target directory path inside a styled container (`Surface`) with an icon and subtle typography.
   - Improve subdirectory list items with clean spacing, folder icons, and clear parent directory navigation (`..`).
   - Enhance the empty state (`subdirs.isEmpty()`) with a centered icon and empty state text.
   - Use distinct button hierarchy (e.g. Filled/Tonal primary action for "Save here" / "Move here", Outlined/Text for "Cancel").
3. **Redesign `ShareDestinationDialog` (`MainActivity.kt`)**:
   - Upgrade `ShareDestinationDialog` into a rich choice card/option dialog with clear visual hierarchy:
     - Header icon and title.
     - Option cards/buttons for "Encrypted Vault" (with security lock icon) and "Choose a folder..." (with folder icon).
     - Clean layout ensuring responsive design across screen sizes and localized text lengths.
4. **Verification**:
   - Run `./gradlew assembleDebug` to ensure code compiles cleanly.
   - Run `./gradlew testDebugUnitTest` to verify unit tests pass.
