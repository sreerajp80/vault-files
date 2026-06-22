# Fix compiler deprecation warnings

Implements `plans/20260621_122726_fix-deprecation-warnings.md`.

## Changes

1. **`app/src/main/java/com/example/data/AppDatabase.kt`** — line 29:
   `.fallbackToDestructiveMigration()` → `.fallbackToDestructiveMigration(dropAllTables = true)`.
   Passing `true` preserves the prior behavior of dropping all tables on a fallback migration.

2. **`app/src/main/java/com/example/ui/FileExplorerScreen.kt`**:
   - line 830: `Divider(...)` → `HorizontalDivider(...)`. No import change needed (covered by the
     existing `androidx.compose.material3.*` wildcard).
   - line 863: `Icons.Default.InsertDriveFile` → `Icons.AutoMirrored.Filled.InsertDriveFile`.
   - Added import `androidx.compose.material.icons.automirrored.filled.InsertDriveFile`.

3. **`app/src/main/java/com/example/ui/SecureVaultScreen.kt`**:
   - line 413: `Icons.Default.InsertDriveFile` → `Icons.AutoMirrored.Filled.InsertDriveFile`.
   - Added import `androidx.compose.material.icons.automirrored.filled.InsertDriveFile`.

## Verification

`./gradlew compileDebugKotlin` → BUILD SUCCESSFUL, with none of the four targeted deprecation
warnings present in the output.
