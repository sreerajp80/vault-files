# Change log: Fix signing config to use vfkeystore.jks via keystore.properties

Implements plan `plans/20260621_010500_keystore-signing-config.md`.

## Problem

`:app:validateSigningDebug` failed because the `debugConfig` signing config pointed at a
non-existent `debug.keystore`. The real keystore is `vfkeystore.jks` at the project root,
used for both debug and release signing.

## Changes

- **`app/build.gradle.kts`**
  - Added top-of-file loading of a gitignored `keystore.properties` into a `Properties` object
    (no-op if the file is absent).
  - Rewrote both `release` and `debugConfig` signing configs to use `vfkeystore.jks`
    (`storeFile` defaults to `vfkeystore.jks`) and read `storePassword` / `keyAlias` /
    `keyPassword` from `keystore.properties`. Removed the old env-var-based release config and
    the `debug.keystore` reference.
- **`.gitignore`** — added `keystore.properties`.
- **`keystore.properties.example`** (new) — template with `storeFile` / `storePassword` /
  `keyAlias` / `keyPassword` keys.
- **`docs/build.md`** — updated the "Signing" gotcha to document the new
  `vfkeystore.jks` + `keystore.properties` setup.

## Follow-up

- The user populates `keystore.properties` with real credentials.
- Verify with `./gradlew assembleDebug`.
