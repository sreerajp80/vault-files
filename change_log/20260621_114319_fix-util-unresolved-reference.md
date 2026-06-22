# Change log: Fix "Unresolved reference 'util'" in app/build.gradle.kts

Implements plan: `plans/20260621_114319_fix-util-unresolved-reference.md`

## What changed

`app/build.gradle.kts`:

1. Added `import java.util.Properties` after the existing
   `import org.jetbrains.kotlin.gradle.dsl.JvmTarget` import.
2. Changed `val keystoreProps = java.util.Properties().apply { ... }` to
   `val keystoreProps = Properties().apply { ... }`.

## Effect

Silences the spurious IDE "Unresolved reference 'util'" warning by importing the type
instead of referencing it fully-qualified. No behavioral/build change.
