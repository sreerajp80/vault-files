# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Workflow rules (mandatory)

1. **Plan before changing.** For any change to the project, first write a full plan to the
   `plans/` folder. Name the file `yyyymmdd_hhMMss_<short-slug>.md` (date+time prefix, local
   time). The plan must include:
   - the list of files to be changed,
   - what the issue is,
   - the plan for the fix.

   **MANDATORY APPROVAL GATE — you MUST get explicit consent before implementing.**
   - After writing the plan, STOP. Do not edit, create, or delete any project file
     (other than the plan file itself) until the user approves.
   - Present the plan and explicitly ask the user to approve it (e.g. "Do you approve
     this plan?"). Then WAIT for the user's reply.
   - Proceed ONLY on an explicit, affirmative approval (e.g. "yes", "approved", "go ahead").
     Silence, a question, a clarification, or an ambiguous reply is NOT approval — ask again.
   - If you change the plan after feedback, re-present it and get approval again.
   - The only exception is if the user explicitly tells you to skip the plan/approval for a
     specific change. A general earlier "go ahead" does not carry over to later changes.

2. **Log after changing.** After implementing a plan, write a change log to the `change_log/`
   folder. Name the file `yyyymmdd_hhMMss_<short-slug>.md` (date+time prefix, local time),
   describing what was changed and referencing the plan it implements.

## Overview

"Vault Files" is a single-module Android app (Kotlin + Jetpack Compose) — a secure file/storage
manager: storage analytics, file explorer, password/biometric-shielded folders, and an
"encrypted" vault.

**Two things to internalize before touching anything:**
- It was scaffolded from Google AI Studio, but **no Gemini/AI code is wired up** — `firebase.ai`
  and the network libs (Retrofit/OkHttp/Moshi) are present but unused/commented out.
- Most "secure storage" behavior is **simulated demo data**, not real device storage (seeded dummy
  files, synthesized virtual external storage, no real encryption). Don't assume a feature touches
  real user files.

## Package naming

One identifier everywhere: the source package, the build `namespace`, and the `applicationId`
are all `in.sreerajp.vault_files`.

Note that `in` is a Kotlin keyword, so it must be backticked in source — in every `package`
line and in every import of our own code:

```kotlin
package `in`.sreerajp.vault_files.ui
import `in`.sreerajp.vault_files.data.StorageRepository
```

The folder on disk is plain `in` (no backticks). Classes in the root package
(`MainActivity`, the tests) do not need to import `R` or `BuildConfig` — they are generated
into that same package.

## Common commands

```bash
./gradlew assembleDebug          # build debug APK
./gradlew installDebug           # build + install on device/emulator
./gradlew testDebugUnitTest      # run JVM/Robolectric unit tests
./gradlew testDebugUnitTest --tests "in.sreerajp.vault_files.ExampleRobolectricTest"   # single test
```

Note: the `debug` build expects a `debug.keystore` at the project root, or you must remove the
`debugConfig` signing line from `app/build.gradle.kts`. See `docs/build.md` for details.

## Architecture in brief

Plain MVVM, no DI framework, no nav library. `MainActivity` (a `FragmentActivity`, for biometrics)
owns the UI and switches 4 tabs by integer index. A single `StorageViewModel` is the source of
truth for all state (StateFlows) and emits user messages via a SharedFlow. A single
`StorageRepository` does all file I/O on `Dispatchers.IO`. Room (`vault_files_database`) holds
settings as key/value rows, secured-folder paths, and vault-file metadata.

## Detailed docs (read when relevant)

- **`docs/build.md`** — full build/test tasks (Roborazzi screenshots, lint, instrumented tests),
  signing gotchas, secrets plugin, config-cache notes, SDK/JVM versions.
- **`docs/architecture.md`** — layer-by-layer breakdown and code conventions (adding a preference,
  the action→repo→message→refresh flow, `testTag` usage, session-based unlock model).
