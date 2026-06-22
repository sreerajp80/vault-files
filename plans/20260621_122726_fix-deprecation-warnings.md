# Fix compiler deprecation warnings

## Issue

The build emits four deprecation warnings:

1. `AppDatabase.kt:29` — `fallbackToDestructiveMigration()` is deprecated; replaced by an
   overload taking a boolean indicating whether all tables should be dropped.
2. `FileExplorerScreen.kt:830` — `Divider(...)` is deprecated; renamed to `HorizontalDivider`.
3. `FileExplorerScreen.kt:863` — `Icons.Filled.InsertDriveFile` is deprecated; use the
   AutoMirrored version `Icons.AutoMirrored.Filled.InsertDriveFile`.
4. `SecureVaultScreen.kt:413` — same `Icons.Filled.InsertDriveFile` deprecation.

These are non-breaking warnings; the fix is to migrate each call to its replacement API while
preserving existing behavior.

## Files to be changed

- `app/src/main/java/com/example/data/AppDatabase.kt`
- `app/src/main/java/com/example/ui/FileExplorerScreen.kt`
- `app/src/main/java/com/example/ui/SecureVaultScreen.kt`

## Plan for the fix

1. **AppDatabase.kt:29** — change `.fallbackToDestructiveMigration()` to
   `.fallbackToDestructiveMigration(dropAllTables = true)`. Passing `true` preserves the
   previous behavior (the old no-arg version dropped all tables).

2. **FileExplorerScreen.kt:830** — change `Divider(modifier = Modifier.padding(vertical = 4.dp))`
   to `HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))`. `HorizontalDivider` is
   already available via the existing `import androidx.compose.material3.*` wildcard — no import
   change needed.

3. **FileExplorerScreen.kt:863** — change `Icons.Default.InsertDriveFile` to
   `Icons.AutoMirrored.Filled.InsertDriveFile` and add
   `import androidx.compose.material.icons.automirrored.filled.InsertDriveFile`.

4. **SecureVaultScreen.kt:413** — change `Icons.Default.InsertDriveFile` to
   `Icons.AutoMirrored.Filled.InsertDriveFile` and add
   `import androidx.compose.material.icons.automirrored.filled.InsertDriveFile`.

## Verification

Run `./gradlew assembleDebug` (or `compileDebugKotlin`) and confirm the four warnings no longer
appear and the build succeeds.
