# Fix unresolved `R` reference in ExampleRobolectricTest

## Issue

`app/src/test/java/com/example/ExampleRobolectricTest.kt:18` references `R.string.app_name`,
but the symbol `R` is unresolved (IDE error: `Unresolved reference 'R'`).

The test lives in package `com.example`, so an unqualified `R` resolves to `com.example.R`,
which does not exist. The generated resource class lives under the module's `namespace`,
which is `in.sreerajp.vault_files` (see `app/build.gradle.kts:13`). The file never imports it.

`R.string.app_name` is a valid resource (`app/src/main/res/values/strings.xml` → "Vault Files"),
so only the import is missing.

## Files to change

- `app/src/test/java/com/example/ExampleRobolectricTest.kt`

## Fix

Add the import for the generated R class under the build namespace:

```kotlin
import in.sreerajp.vault_files.R
```

Place it with the other imports (alphabetical/grouped as appropriate). No other changes needed.

## Verification

- IDE error clears.
- Optionally: `./gradlew testDebugUnitTest --tests "com.example.ExampleRobolectricTest"`.
