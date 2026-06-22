# Fix: `kotlin {}` block not resolving in app/build.gradle.kts

## Issue

The IDE/Gradle reports at `app/build.gradle.kts:61`:

```
None of the following candidates is applicable:
fun DependencyHandler.kotlin(module: String, version: String? = ...): Any
fun PluginDependenciesSpec.kotlin(module: String): PluginDependencySpec
```

Root cause: the `kotlin { compilerOptions { jvmTarget = JvmTarget.JVM_17 } }` block
(lines 61-65) is the DSL provided by the **Kotlin Android Gradle plugin**
(`org.jetbrains.kotlin.android`). That plugin is **not applied**. The `plugins {}`
block only applies `org.jetbrains.kotlin.plugin.compose` (the Compose compiler plugin),
which does not register the `kotlin {}` project extension. With no extension named
`kotlin`, the `kotlin` identifier resolves to the unrelated `DependencyHandler.kotlin` /
`PluginDependenciesSpec.kotlin` functions — none of which match `kotlin { ... }`.

Secondary symptom: `JvmTarget` (line 63) is also unresolved without that plugin on the
classpath (its import comes from `org.jetbrains.kotlin.gradle.dsl.JvmTarget`).

Note: the Compose compiler plugin requires the Kotlin plugin to be applied anyway, so
this is almost certainly an accidental omission rather than an intentional setup.

## Files to be changed

1. `gradle/libs.versions.toml` — add a plugin alias for the Kotlin Android plugin under
   `[plugins]`, reusing the existing `kotlin = "2.2.10"` version ref:
   ```toml
   kotlin-android = { id = "org.jetbrains.kotlin.android", version.ref = "kotlin" }
   ```

2. `app/build.gradle.kts` — apply the new alias in the `plugins {}` block (placed before
   the compose plugin):
   ```kotlin
   alias(libs.plugins.kotlin.android)
   ```

## Plan for the fix

1. Add the `kotlin-android` plugin entry to `[plugins]` in the version catalog.
2. Add `alias(libs.plugins.kotlin.android)` to the `plugins {}` block in
   `app/build.gradle.kts`.
3. Verify with `./gradlew help` (or a Gradle sync / `./gradlew assembleDebug`) that the
   `kotlin {}` block now resolves and the build configures without the candidate error.

No source code changes; this is purely Gradle build configuration.
