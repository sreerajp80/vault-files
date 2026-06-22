# Change log: Update outdated "Namespace caveat" in CLAUDE.md

Implements plan `plans/20260621_012039_update-namespace-caveat.md`.

## What changed
- `CLAUDE.md`, "Namespace caveat" section: rewrote the note that previously claimed
  **three** distinct identifiers (source package `com.example`, `namespace`
  `in.sreerajp.vault_files`, `applicationId` `com.aistudio.filevault.kxmpzq`).
- It now describes **two** identifiers, matching the current build config: source package
  is still `com.example`, while `namespace` and `applicationId` are both
  `in.sreerajp.vault_files` (per `app/build.gradle.kts` lines 13 and 17).

## Why
The old `applicationId` (`com.aistudio.filevault.kxmpzq`) no longer matches the build;
it now equals the namespace, so the doc was stale.

Documentation only — no code or build changes.
