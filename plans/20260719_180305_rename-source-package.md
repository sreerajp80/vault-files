# Rename source package `com.example` → `in.sreerajp.vault_files`

**Status:** completed

Change log: `change_log/20260719_181142_rename-source-package.md`

## What the issue is

The build IDs were already changed to `in.sreerajp.vault_files`:

- `namespace = "in.sreerajp.vault_files"` (app/build.gradle.kts:33)
- `applicationId = "in.sreerajp.vault_files"` (app/build.gradle.kts:37)

But the Kotlin source still lives in the old scaffold package `com.example`. So every
file starts with `package com.example...`, while `R` and `BuildConfig` are generated
into `in.sreerajp.vault_files`. That is why files contain the odd-looking line:

```kotlin
import `in`.sreerajp.vault_files.R
```

This works, but it is confusing. The goal is to make the source package match the
namespace so there is one identifier instead of two.

## Important wrinkle: `in` is a Kotlin keyword

The package starts with `in`, which is a reserved word in Kotlin. Every package and
import that starts with it must use backticks:

```kotlin
package `in`.sreerajp.vault_files.ui
import `in`.sreerajp.vault_files.data.StorageRepository
```

The folder on disk is plain `in` (no backticks). Java/Kotlin allow this; the backticks
are only needed in source text. This is ugly but unavoidable without changing the
applicationId, which we do not want to do (it is the published app ID).

## Files to be changed

### 1. Move source folders (git mv, keeps history)

- `app/src/main/java/com/example/` → `app/src/main/java/in/sreerajp/vault_files/`
- `app/src/test/java/com/example/` → `app/src/test/java/in/sreerajp/vault_files/`
- `app/src/androidTest/java/com/example/` → `app/src/androidTest/java/in/sreerajp/vault_files/`
- Delete the now-empty `com/example` parent folders.

### 2. Kotlin sources (21 files) — rewrite `package` / `import` lines

Main (18):
- `MainActivity.kt`
- `data/AppDatabase.kt`, `data/CryptoManager.kt`, `data/DatabaseEntities.kt`, `data/StorageRepository.kt`
- `ui/AboutScreen.kt`, `ui/FileExplorerScreen.kt`, `ui/PermissionsScreen.kt`,
  `ui/SecureVaultScreen.kt`, `ui/SettingsScreen.kt`, `ui/StorageAnalyzerScreen.kt`,
  `ui/StorageViewModel.kt`
- `ui/theme/Color.kt`, `ui/theme/Theme.kt`, `ui/theme/Type.kt`
- `utils/BiometricHelper.kt`, `utils/ZipUtility.kt`

Tests (4):
- `test/.../ExampleUnitTest.kt`, `test/.../ExampleRobolectricTest.kt`, `test/.../GreetingScreenshotTest.kt`
- `androidTest/.../ExampleInstrumentedTest.kt`

Changes in each:
- `package com.example[.x]` → ``package `in`.sreerajp.vault_files[.x]``
- `import com.example.…` → ``import `in`.sreerajp.vault_files.…``
- Existing ``import `in`.sreerajp.vault_files.R`` / `.BuildConfig` lines: keep them in
  sub-packages (`ui`, `data`, `utils`); remove them in `MainActivity.kt` only if it now
  sits in the same package as `R` (it will — `R` is generated at the namespace root).

### 3. `app/src/main/AndroidManifest.xml`

- Line 21: `android:name="com.example.MainActivity"` → `android:name=".MainActivity"`
  (relative form; resolves against the namespace, so it stays correct if IDs ever change).
- No other change. The FileProvider already uses `${applicationId}.fileprovider`.

### 4. `app/src/androidTest/java/.../ExampleInstrumentedTest.kt`

- Line 20 asserts `assertEquals("com.example", appContext.packageName)`. This assertion
  is currently **wrong** (the real package is `in.sreerajp.vault_files`; it only passes
  because instrumented tests are not run in CI). Fix to `"in.sreerajp.vault_files"`.

### 5. Docs

- `CLAUDE.md` — remove/replace the "Namespace caveat" section, since the two identifiers
  will now be the same. Replace with a short note about the backtick-`in` requirement.
- `docs/architecture.md`, `docs/build.md` — update any `com.example` paths/examples.
- `CLAUDE.md` common-commands block: the single-test example
  `--tests "com.example.ExampleRobolectricTest"` → `"in.sreerajp.vault_files.ExampleRobolectricTest"`.

### 6. Not changed

- `app/build.gradle.kts` — namespace/applicationId already correct.
- `app/proguard-rules.pro` — contains no `com.example` references.
- `app/src/main/res/**` — no `com.example` references.
- Old files under `plans/` and `change_log/` — these are historical records; leave as-is.
- Room database name, table names, schema — unaffected by a package rename. No migration
  needed and no user data loss.

## Plan for the fix

1. `./gradlew clean` first, so no stale generated `com/example` classes linger in `app/build/`.
2. `git mv` the three source trees.
3. Rewrite `package`/`import` lines in the 21 Kotlin files.
4. Fix the manifest activity name.
5. Fix the instrumented-test package assertion.
6. Update the docs.
7. Verify:
   - `./gradlew assembleDebug`
   - `./gradlew testDebugUnitTest`
   - `grep -rn "com\.example" app/src` returns nothing.
8. Write the change log to `change_log/`.

## Risks

- **Backtick misses.** Any `in`-prefixed package or import written without backticks is a
  compile error. Caught immediately by `assembleDebug`, so low risk.
- **Roborazzi screenshot test.** `GreetingScreenshotTest` writes to
  `src/test/screenshots/greeting.png`. The path is package-independent, so it should be
  unaffected; if the baseline compare fails, re-record it.
- **IDE indexes.** Android Studio may need an "Invalidate Caches / Restart" after the move.
- **Installed app.** `applicationId` is unchanged, so an existing install upgrades normally
  — no uninstall, no data loss.

## Rollback

The whole change is a single commit on a clean tree. `git revert` (or `git reset --hard`)
restores the old layout.

## Note on the working tree

`git status` currently shows 6 modified files (build.gradle.kts, AndroidManifest.xml,
MainActivity.kt, StorageRepository.kt, FileExplorerScreen.kt, StorageViewModel.kt).
Those in-progress edits should be committed or stashed **before** this rename starts, so
the rename lands as its own clean commit and is easy to revert.
