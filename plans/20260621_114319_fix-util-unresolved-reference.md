# Fix "Unresolved reference 'util'" in app/build.gradle.kts

## Issue

The IDE reports `Unresolved reference 'util'` at `app/build.gradle.kts:15:26`, on:

```kotlin
val keystoreProps = java.util.Properties().apply { ... }
```

This is a spurious Kotlin-script-analysis error in the IDE — `java.util.Properties()` is
valid and the Gradle build compiles fine. The fully-qualified `java.util.…` reference can
confuse the `.gradle.kts` script analyzer. Using an explicit import resolves the editor
warning.

## Files to be changed

- `app/build.gradle.kts`

## Plan for the fix

1. Add `import java.util.Properties` after the existing top-of-file import
   (`import org.jetbrains.kotlin.gradle.dsl.JvmTarget`).
2. Change line 15 from `java.util.Properties()` to `Properties()`.

No behavioral change — purely silences the IDE warning by importing the type instead of
referencing it fully-qualified.
