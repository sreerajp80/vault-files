# Plan — Adopt Asset Config Pattern A as Single Source of Truth

**Status:** Completed

## Files to Change

- `app/src/main/assets/config/app_config.json` (NEW)
- `app/src/main/java/in/sreerajp/vault_files/config/AppConfig.kt` (NEW)
- `app/src/main/java/in/sreerajp/vault_files/config/ConfigService.kt` (NEW)
- `app/src/main/java/in/sreerajp/vault_files/config/AppConstants.kt` (MODIFY)
- `app/build.gradle.kts` (MODIFY)
- `app/about.properties` (DELETE)
- `app/src/main/java/in/sreerajp/vault_files/ui/AboutScreen.kt` (MODIFY)
- `app/src/test/java/in/sreerajp/vault_files/AppConfigTest.kt` (NEW)
- `docs/architecture.md` (MODIFY)
- `docs/release_process.md` (MODIFY)

## Issue

1. The project does not currently use an asset config file.
2. The project uses Pattern B (`about.properties` and Gradle `BuildConfig` fields) instead of Pattern A (`assets/config/app_config.json`).
3. The app version (`versionCode` and `versionName`) is hardcoded in `app/build.gradle.kts` rather than read from an asset config as a single source of truth.
4. The About screen hardcodes detail rows instead of rendering them dynamically from the config.
5. Project documentation (`docs/architecture.md` and `docs/release_process.md`) references `about.properties` and manual Gradle version bumping instead of the asset config pattern.

## Fix

1. **Create `app/src/main/assets/config/app_config.json`**:
   - Define `appName`, `description`, `version` ("17.2"), `build` ("17"), and dynamic `details` (Author, IDE used, AI used, License).
   - This becomes the single source of truth for versioning and About metadata.

2. **Create `config/AppConfig.kt`**:
   - Implement typed model with safe `fallback` and `fromJson(JSONObject)` parser following `guideline.md §1.1`.

3. **Create `config/ConfigService.kt`**:
   - Implement `load(context)` to load `config/app_config.json` with fallback on any error.
   - Implement `loadAndVerify(context)` to compare asset version with runtime package info using `PackageInfoCompat.getLongVersionCode(info)`.

4. **Update `app/build.gradle.kts`**:
   - Parse `app/src/main/assets/config/app_config.json` at build time with `groovy.json.JsonSlurper`.
   - Set `versionName` and `versionCode` dynamically from the JSON file.
   - Remove `about.properties` loader and `BuildConfig` fields (`AUTHOR`, `IDE`, `AI_VERSION`). Keep `BUILD_DATE`.

5. **Delete `app/about.properties`**:
   - Clean up replaced properties file.

6. **Update `app/src/main/java/in/sreerajp/vault_files/config/AppConstants.kt`**:
   - Update comment to reference Pattern A asset config.

7. **Update `app/src/main/java/in/sreerajp/vault_files/ui/AboutScreen.kt`**:
   - Load config using `ConfigService.loadAndVerify(context)`.
   - Render app name, version, and dynamic `details` entries in order.
   - Keep `Last Build Date` row using `BuildConfig.BUILD_DATE`.

8. **Add unit tests in `app/src/test/java/in/sreerajp/vault_files/AppConfigTest.kt`**:
   - Test JSON parsing, fallback values, and `ConfigService` asset loading.

9. **Update documentation**:
   - Update `docs/architecture.md` to reflect Pattern A and the new files in `config/`.
   - Update `docs/release_process.md` to document `app_config.json` as the single source of truth for versioning and metadata updates.
