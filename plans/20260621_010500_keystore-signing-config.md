# Plan: Fix signing config to use vfkeystore.jks via keystore.properties

## Issue

The `debug` build type is wired to a `debugConfig` signing config that expects
`debug.keystore` at the project root, which does not exist. This causes:

```
Execution failed for task ':app:validateSigningDebug'.
> Keystore file 'L:\Android\vault-files\debug.keystore' not found for signing config 'debugConfig'.
```

The actual keystore is `vfkeystore.jks` at the project root, which should be used for
signing both debug and release builds. Credentials will be supplied via a gitignored
`keystore.properties` file (the user will populate the values themselves).

## Files to be changed

1. **`app/build.gradle.kts`** — load `keystore.properties` and point both the `release`
   and `debugConfig` signing configs at `vfkeystore.jks`.
2. **`.gitignore`** — add `keystore.properties` so credentials are not committed.
3. **`keystore.properties.example`** (new) — template documenting the expected keys
   (no real secrets). The user creates the real `keystore.properties` from this.
4. **`docs/build.md`** — update the "Signing" gotcha to describe the new setup.

## Plan for the fix

### `app/build.gradle.kts`
- At the top of the `android {}` block (or file), read `keystore.properties` if present:
  ```kotlin
  val keystorePropsFile = rootProject.file("keystore.properties")
  val keystoreProps = java.util.Properties().apply {
      if (keystorePropsFile.exists()) keystorePropsFile.inputStream().use { load(it) }
  }
  ```
- Rewrite `signingConfigs` so both configs use `vfkeystore.jks` and the properties:
  ```kotlin
  signingConfigs {
    create("release") {
      storeFile = file("${rootDir}/${keystoreProps.getProperty("storeFile", "vfkeystore.jks")}")
      storePassword = keystoreProps.getProperty("storePassword")
      keyAlias = keystoreProps.getProperty("keyAlias")
      keyPassword = keystoreProps.getProperty("keyPassword")
    }
    create("debugConfig") {
      storeFile = file("${rootDir}/${keystoreProps.getProperty("storeFile", "vfkeystore.jks")}")
      storePassword = keystoreProps.getProperty("storePassword")
      keyAlias = keystoreProps.getProperty("keyAlias")
      keyPassword = keystoreProps.getProperty("keyPassword")
    }
  }
  ```
  (`debug` and `release` buildType blocks remain unchanged — they already reference these
  configs by name.)

### `.gitignore`
- Add a line `keystore.properties`.

### `keystore.properties.example` (new)
```
storeFile=vfkeystore.jks
storePassword=
keyAlias=
keyPassword=
```

### `docs/build.md`
- Replace the current "Signing" bullet so it documents `vfkeystore.jks` +
  `keystore.properties` (storeFile/storePassword/keyAlias/keyPassword) for both build types,
  and drop the now-obsolete `debug.keystore` / env-var instructions.

## Notes / verification
- The user will create and populate `keystore.properties`. The build will only succeed once
  that file contains valid credentials for `vfkeystore.jks`.
- After implementation, suggest running `./gradlew assembleDebug` to confirm signing works.
