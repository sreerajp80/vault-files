# Change Log — Align Project Structure, Code, and Docs with Kotlin Guidelines

**Date:** 2026-08-30
**Plan:** [plans/20260830_194500_guidelines_alignment.md](../plans/20260830_194500_guidelines_alignment.md)

## Changes Implemented

1. **Updated Root AI Instruction Files**:
   - [CLAUDE.md](../CLAUDE.md) and [AGENTS.md](../AGENTS.md): Aligned both files with the Thin pointer profile per [docs/guidelines/CLAUDE_MD_GUIDELINE.md](../docs/guidelines/CLAUDE_MD_GUIDELINE.md) and [docs/guidelines/AGENTS_MD_GUIDELINE.md](../docs/guidelines/AGENTS_MD_GUIDELINE.md). Added canonical 18-section layout including Project Identity table, Doc References table, package naming notes for Kotlin's `` `in` `` keyword, hard rules, architecture rules, copy-paste build/test commands, signing and security rules, string resources rule, testing, directory tree, workflow rules with strict privacy and relative path constraints, and AI agent Dos & Don'ts.

2. **Standardized Existing Documentation**:
   - [docs/architecture.md](../docs/architecture.md): Restructured according to [docs/guidelines/DOCS_FOLDER_GUIDELINE.md](../docs/guidelines/DOCS_FOLDER_GUIDELINE.md) (`# H1` title with app name, purpose paragraph, read-first links, numbered sections) covering MVVM layers, unidirectional data flow, Room database schema, and `VaultDocumentsProvider` SAF exposure guards.
   - [docs/build.md](../docs/build.md): Standardized anatomy, environment requirements, copy-paste Gradle tasks (build, install, Robolectric unit tests, Roborazzi screenshot tests, lint), keystore properties configuration, and configuration cache usage.
   - [docs/features.md](../docs/features.md): Standardized anatomy with read-first links and detailed feature inventories for all 4 tabs (Storage Analyzer, File Explorer, Secure Vault, Settings) alongside clear distinctions between simulated Vault storage and hardware-backed AES-256-GCM Secure Notes encryption.

3. **Created Missing Blueprint Documents**:
   - [docs/security.md](../docs/security.md) (NEW): Created the living security blueprint based on [docs/guidelines/security.md](../docs/guidelines/security.md), detailing offline boundaries, AES-256-GCM Keystore cryptography, Room plain-text PIN reality, Zip-Slip mitigation in `ZipUtility`, and SAF `isExposable` isolation.
   - [docs/release_process.md](../docs/release_process.md) (NEW): Created the living release runbook based on [docs/guidelines/release_process.md](../docs/guidelines/release_process.md), outlining versioning policy, `vfkeystore.jks` release signing configuration, step-by-step build commands (`./gradlew assembleRelease`), `apksigner` verification, and pre-release checklist.

## Verification

- Ran `./gradlew testDebugUnitTest` — build succeeded with all unit tests passing.
- Verified all cross-document relative links across `CLAUDE.md`, `AGENTS.md`, and all `docs/*.md` files.
