# Change Log — Create `config/AppConstants.kt` for Guideline Compliance

**Date:** 2026-08-30
**Plan:** [plans/20260830_200024_fix_config_architecture_reference.md](../plans/20260830_200024_fix_config_architecture_reference.md)

## Changes

1. **[NEW] `app/src/main/java/in/sreerajp/vault_files/config/AppConstants.kt`**:
   Created the `config/` package with `AppConstants` object holding project-wide technical constants:
   - `DATABASE_NAME` — Room database file name
   - `STORAGE_DIR_NAME` / `VAULT_DIR_NAME` / `RESTORED_DIR_NAME` — on-disk sandbox directory names
   - `TEXT_PREVIEW_MAX_BYTES` — file preview size threshold

2. **[MODIFY] `app/src/main/java/in/sreerajp/vault_files/data/AppDatabase.kt`**:
   Replaced hard-coded `"vault_files_database"` string with `AppConstants.DATABASE_NAME`.

3. **[MODIFY] `app/src/main/java/in/sreerajp/vault_files/data/StorageRepository.kt`**:
   Replaced hard-coded `"Storage"`, `"Vault"`, and `"Restored"` directory name strings with `AppConstants` references. Removed the file-level `TEXT_PREVIEW_MAX_BYTES` constant (now in `AppConstants`).

4. **[MODIFY] `docs/architecture.md`**:
   Added `config/AppConstants.kt` to the source tree diagram in section 3.

Class-private constants (`CryptoManager.KEY_ALIAS`, `VaultDocumentsProvider.ROOT_ID`, `LISTING_CHUNK`, `SIZE_EMIT_INTERVAL_MS`) were left in their owning classes — the guideline specifies `AppConstants` for project-wide constants only.

## Verification

- `./gradlew testDebugUnitTest` — build succeeded, all unit tests passed.
- `CLAUDE.md` and `AGENTS.md` already listed `config/` in the architecture layout — now matches disk.
