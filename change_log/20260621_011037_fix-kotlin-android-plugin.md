# Change log: Fix `kotlin {}` block not resolving

Implements plan `plans/20260621_011037_fix-kotlin-android-plugin.md`.

## Problem

`app/build.gradle.kts:61` failed to resolve the `kotlin { compilerOptions { ... } }`
DSL block ("None of the following candidates is applicable ... DependencyHandler.kotlin
/ PluginDependenciesSpec.kotlin") because the Kotlin Android Gradle plugin
(`org.jetbrains.kotlin.android`) — which registers that extension — was not applied.

## Changes

1. `gradle/libs.versions.toml`
   - Added under `[plugins]`:
     `kotlin-android = { id = "org.jetbrains.kotlin.android", version.ref = "kotlin" }`
     (reuses existing `kotlin = "2.2.10"`).

2. `build.gradle.kts` (root)
   - Added `alias(libs.plugins.kotlin.android) apply false` to the `plugins {}` block.
   - Needed because the Compose compiler plugin transitively places the Kotlin Gradle
     plugin on the build classpath with an "unknown version"; declaring it at the root
     establishes the version build-wide and resolves the
     "already on the classpath with an unknown version" failure.

3. `app/build.gradle.kts`
   - Added `alias(libs.plugins.kotlin.android)` to the `plugins {}` block.
   - Added `import org.jetbrains.kotlin.gradle.dsl.JvmTarget` so the `JvmTarget.JVM_17`
     reference on line 64 resolves.

## Verification

`./gradlew help -q` now configures successfully with no script-compilation or
plugin-resolution errors (only benign JVM restricted-method warnings remain). The
original line-61 candidate error and the follow-on "Unresolved reference 'JvmTarget'"
error are both gone.
