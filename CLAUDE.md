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

## Namespace caveat

Two different identifiers — don't conflate them: the source package is still `com.example`, while
the build `namespace` and `applicationId` are both `in.sreerajp.vault_files`.

## Common commands

```bash
./gradlew assembleDebug          # build debug APK
./gradlew installDebug           # build + install on device/emulator
./gradlew testDebugUnitTest      # run JVM/Robolectric unit tests
./gradlew testDebugUnitTest --tests "com.example.ExampleRobolectricTest"   # single test
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
