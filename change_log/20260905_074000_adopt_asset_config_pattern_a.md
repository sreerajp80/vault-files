# Adopt Asset Config Pattern A as Single Source of Truth

**Date:** 2026-09-05 07:40:00 IST  
**Plan:** `plans/20260905_074000_adopt_asset_config_pattern_a.md`

## Summary of changes

1. **Created `app/src/main/assets/config/app_config.json`:**
   - Established the JSON asset as the single source of truth for application version (`version` and `build`) and About-screen metadata.

2. **Created `AppConfig.kt` & `ConfigService.kt`:**
   - Implemented `in.sreerajp.vault_files.config.AppConfig` data class model with `fromJson` and fallback values.
   - Implemented `in.sreerajp.vault_files.config.ConfigService` to load the asset configuration and verify version alignment against package info.

3. **Updated Gradle build script (`app/build.gradle.kts`):**
   - Configured `app/build.gradle.kts` to parse `src/main/assets/config/app_config.json` dynamically with `JsonSlurper`.
   - Assigned `versionName` and `versionCode` directly from `app_config.json`.
   - Removed `about.properties` parsing and replaced `BuildConfig` fields (`AUTHOR`, `IDE`, `AI_VERSION`).

4. **Removed `app/about.properties`:**
   - Cleaned up obsolete properties file.

5. **Updated `AboutScreen.kt`:**
   - Replaced static field reads with dynamic rendering of `AppConfig.details` loaded via `ConfigService`.
   - Maintained last build date display from `BuildConfig.BUILD_DATE`.

6. **Updated documentation:**
   - Updated `docs/architecture.md` to reflect Pattern A and the configuration layer.
   - Updated `docs/release_process.md` to document `app_config.json` as the single source of truth for versioning and release updates.

7. **Added unit tests & lint fixes:**
   - Added unit test suite in `app/src/test/java/in/sreerajp/vault_files/AppConfigTest.kt`.
   - Added missing Malayalam strings for documents root in `res/values-ml/strings.xml` to keep Android Lint at 0 errors.

## Verification

- Ran `./gradlew testDebugUnitTest` — all 17 unit tests passed.
- Ran `./gradlew lint` — zero lint errors.
- Ran `./gradlew assembleDebug` — debug APK built successfully.
