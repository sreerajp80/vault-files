# Plan — Create `config/AppConstants.kt` for Guideline Compliance

**Status:** Approved

## Files to Change

- `app/src/main/java/in/sreerajp/vault_files/config/AppConstants.kt` (NEW)
- `app/src/main/java/in/sreerajp/vault_files/data/AppDatabase.kt` (MODIFY)
- `app/src/main/java/in/sreerajp/vault_files/data/StorageRepository.kt` (MODIFY)
- `docs/architecture.md` (MODIFY)

## Issue

`guideline.md` §1 and §3 require a `config/` package. Both `CLAUDE.md` and `AGENTS.md` list `config/` in the architecture layout, but the directory does not exist on disk.

The app uses Pattern B (BuildConfig via `about.properties`) for About-screen constants, which is valid. However, the guideline still expects `config/AppConstants.kt` for project-wide technical constants like database names, directory names, and shared thresholds.

## Fix

1. Create `config/AppConstants.kt` with centralized project-wide constants:
   - `DATABASE_NAME` (currently a string literal in `AppDatabase.kt`)
   - `STORAGE_DIR_NAME` / `VAULT_DIR_NAME` (currently string literals in `StorageRepository.kt`)
   - `TEXT_PREVIEW_MAX_BYTES` (shared threshold from `StorageRepository.kt`)

2. Update `AppDatabase.kt` to reference `AppConstants.DATABASE_NAME`.

3. Update `StorageRepository.kt` to reference `AppConstants` for directory names and the preview threshold.

4. Update `docs/architecture.md` to add `config/` to the source tree.

Constants that are class-private implementation details (e.g., `CryptoManager.KEY_ALIAS`, `VaultDocumentsProvider.ROOT_ID`, `LISTING_CHUNK`, `SIZE_EMIT_INTERVAL_MS`) stay in their owning class — the guideline says `AppConstants` holds project-wide constants, not class internals.
