# Change log — rename source package `com.example` → `in.sreerajp.vault_files`

Implements: `plans/20260719_180305_rename-source-package.md`

## What was changed

### Source folders moved (via `git mv`, history kept)

- `app/src/main/java/com/example/` → `app/src/main/java/in/sreerajp/vault_files/`
- `app/src/test/java/com/example/` → `app/src/test/java/in/sreerajp/vault_files/`
- `app/src/androidTest/java/com/example/` → `app/src/androidTest/java/in/sreerajp/vault_files/`

The empty `com/example` parent folders were removed.

### Kotlin sources (21 files)

Every `package com.example…` line and every `import com.example.…` line was rewritten to the
new package. Because `in` is a Kotlin keyword, all of them are backticked:

```kotlin
package `in`.sreerajp.vault_files.ui
import `in`.sreerajp.vault_files.data.StorageRepository
```

Extra fixes found on the way:

- `ui/SettingsScreen.kt` — one fully-qualified inline call `com.example.data.FileItem(` was
  replaced with a normal `import \`in\`.sreerajp.vault_files.data.FileItem` plus plain `FileItem(`.
- `MainActivity.kt` and `test/ExampleRobolectricTest.kt` — the
  `import \`in\`.sreerajp.vault_files.R` line was removed. Those classes now sit in the same
  package that `R` is generated into, so the import is redundant.

### `app/src/main/AndroidManifest.xml`

- The activity entry `android:name="com.example.MainActivity"` became the relative form
  `android:name=".MainActivity"`, which resolves against the namespace.

### `app/src/androidTest/.../ExampleInstrumentedTest.kt`

- The assertion `assertEquals("com.example", appContext.packageName)` was **already wrong**
  before this change (the real package has been `in.sreerajp.vault_files` for a while; the test
  simply was not being run). Corrected to `"in.sreerajp.vault_files"`.

### Docs

- `CLAUDE.md` — the "Namespace caveat" section was replaced with a "Package naming" section,
  since the two identifiers are now one. It explains the backtick-`in` rule instead.
- `docs/build.md` — same section replaced. It also listed a stale `applicationId`
  (`com.aistudio.filevault.kxmpzq`); that is now corrected to `in.sreerajp.vault_files`.
- `docs/architecture.md`, `docs/build.md`, `CLAUDE.md` — `com.example` package references and
  `--tests` command examples updated.

### Not changed

- `app/build.gradle.kts` — `namespace` and `applicationId` were already `in.sreerajp.vault_files`.
- `app/proguard-rules.pro`, `app/src/main/res/**` — no `com.example` references.
- Room database name, tables and schema — a package rename does not affect them. No migration
  was needed and no user data is lost. `applicationId` is unchanged, so an existing install
  upgrades normally.
- Older files in `plans/` and `change_log/` — left as historical records.

## Unrelated fixes made to unblock the build

The working tree had uncommitted work in progress that did not compile *before* this rename
started (confirmed against `HEAD`). Two errors blocked verification, and were fixed with the
user's go-ahead:

1. `ui/StorageViewModel.kt` — `withContext` was used at line 833 but never imported. Added
   `import kotlinx.coroutines.withContext`.
2. `ui/FileExplorerScreen.kt` — the new multi-pick "Done" FloatingActionButton used
   `Modifier.align(Alignment.BottomEnd)`, which needs a `BoxScope`, but the block had been
   placed inside the `Column` (opened at line 279) rather than the outer `Box` (line 278).
   The block was moved out one level, so it is now a direct child of the `Box`. Behaviour is
   what was clearly intended: the FAB pinned to the bottom-end of the screen.

## Verification

- `./gradlew clean assembleDebug` — **BUILD SUCCESSFUL**. Only one pre-existing deprecation
  warning remains (`Icons.Filled.OpenInNew` in `FileExplorerScreen.kt:2230`).
- `grep -rn "com\.example" app/src` — no matches.
- `./gradlew testDebugUnitTest` — `ExampleUnitTest` passes. The two Robolectric tests
  (`ExampleRobolectricTest`, `GreetingScreenshotTest`) fail with
  `Android SDK 36 requires Java 21 (have Java 17)`. This is a local toolchain problem, not
  related to the rename — both classes compiled and ran under the new package names. To run
  them, build with a Java 21 JDK.
