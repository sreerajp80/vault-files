# Plan: Update outdated "Namespace caveat" in CLAUDE.md

## Files to be changed
- `CLAUDE.md` (project root) — the "Namespace caveat" section (lines 42–45).

## Issue
The "Namespace caveat" section claims three distinct identifiers, including
`applicationId` = `com.aistudio.filevault.kxmpzq`. This is stale: in
`app/build.gradle.kts`, both `namespace` (line 13) and `applicationId` (line 17)
are now set to `in.sreerajp.vault_files`. The doc no longer matches the build config.

## Fix
Rewrite the caveat so it reflects current reality:
- Source package is still `com.example`.
- `namespace` and `applicationId` are now both `in.sreerajp.vault_files` (no longer distinct).

Proposed replacement text:

> ## Namespace caveat
>
> Two different identifiers — don't conflate them: the source package is still
> `com.example`, while the build `namespace` and `applicationId` are both
> `in.sreerajp.vault_files`.

No code or build changes — documentation only.
