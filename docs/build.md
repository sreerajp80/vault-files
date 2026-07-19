# Build, run, and test

Uses the Gradle wrapper (`./gradlew` / `gradlew.bat`). Single `:app` module.
Java/Kotlin **17**, `compileSdk`/`targetSdk` **36**, `minSdk` **24**. Compose + KSP (Room, Moshi).

```bash
./gradlew assembleDebug          # build debug APK
./gradlew installDebug           # build + install on connected device/emulator
./gradlew testDebugUnitTest      # run JVM/Robolectric unit tests
./gradlew connectedAndroidTest   # run instrumented (espresso) tests on a device
./gradlew lint                   # Android lint
```

Run a single test class / method:

```bash
./gradlew testDebugUnitTest --tests "in.sreerajp.vault_files.ExampleRobolectricTest"
./gradlew testDebugUnitTest --tests "in.sreerajp.vault_files.GreetingScreenshotTest.greeting_screenshot"
```

Screenshot tests use **Roborazzi** (golden images under `app/src/test/screenshots/`):

```bash
./gradlew recordRoborazziDebug   # regenerate golden screenshots
./gradlew verifyRoborazziDebug   # verify against goldens
```

## Build gotchas

- **Signing:** both the `debug` (`debugConfig`) and `release` signing configs use
  `vfkeystore.jks` at the project root. Credentials are read from a gitignored
  `keystore.properties` (also at the root) with keys `storeFile`, `storePassword`,
  `keyAlias`, `keyPassword`. Copy `keystore.properties.example` to `keystore.properties`
  and fill in the values before building. (`storeFile` defaults to `vfkeystore.jks` if
  omitted.)
- **Secrets plugin:** `.env` (gitignored) is read via the secrets-gradle-plugin, falling back to
  `.env.example`. Builds work without a real key; nothing in the app consumes it currently.
- Gradle **configuration cache and caching are on** (`gradle.properties`); if you change build
  logic and see stale behavior, add `--no-configuration-cache`.

## Package naming

All three identifiers are now the same — `in.sreerajp.vault_files`:
- Source package (all `.kt` files live here)
- Build `namespace` (R class / BuildConfig)
- `applicationId` (installed package)

`in` is a Kotlin keyword, so `package` lines and imports of our own code must backtick it:
`` package `in`.sreerajp.vault_files.ui ``. The folder on disk is plain `in`.
