# Plan — Align Project Structure, Code, and Docs with Kotlin Guidelines

**Status:** Proposed

## Files to be changed / created

- [CLAUDE.md](CLAUDE.md) (MODIFY)
- [AGENTS.md](AGENTS.md) (MODIFY)
- [docs/architecture.md](docs/architecture.md) (MODIFY)
- [docs/build.md](docs/build.md) (MODIFY)
- [docs/features.md](docs/features.md) (MODIFY)
- [docs/security.md](docs/security.md) (NEW)
- [docs/release_process.md](docs/release_process.md) (NEW)

## Issue / Context

The repository recently added the `docs/guidelines` Git submodule and `docs/GUIDELINES_MANIFEST.md`, which set out mandatory standards for Kotlin Android projects:
1. `CLAUDE.md` and `AGENTS.md` must follow the canonical section order, include the project identity table, doc references, hard rules, architecture rules, build/run commands, security rules, string resources, testing, dependency constraints, project tree, workflow rules (with relative paths and privacy constraints), communication rules, and Dos & Don'ts.
2. The project falls under three applicability profiles: `Core Baseline`, `Production App Extension`, and `Sensitive Data Extension`. Therefore, the `docs/` folder requires living blueprints for:
   - Architecture (`docs/architecture.md`)
   - Build & Test (`docs/build.md`)
   - Features (`docs/features.md`)
   - Security (`docs/security.md`)
   - Release Process (`docs/release_process.md`)
3. All docs under `docs/` must follow the standard anatomy outlined in `DOCS_FOLDER_GUIDELINE.md` (`# H1` title with app name, purpose paragraph, "read first" links, numbered sections with `---` dividers, simple English, language-tagged code blocks, relative cross-links).

## Plan for the Fix

1. **Update `CLAUDE.md` and `AGENTS.md`**:
   - Align both files with the Thin pointer profile specifications in `CLAUDE_MD_GUIDELINE.md` and `AGENTS_MD_GUIDELINE.md`.
   - Include accurate project identity metadata (SDK versions, MVVM architecture, Room database, offline-first stance, etc.).
   - Include the canonical doc references table pointing to `docs/architecture.md`, `docs/security.md`, `docs/release_process.md`, `docs/build.md`, `docs/features.md`, and `docs/GUIDELINES_MANIFEST.md`.
   - Include mandatory workflow rules (plan-before-changing, mandatory approval gate, log-after-changing, relative repository paths only, no local system details, no sensitive data) and simple English communication rules.
   - Include hard rules, architecture rules, build commands, security rules, string resources rule, code style, testing rules, dependency constraints, directory tree, and Dos & Don'ts.

2. **Standardize and expand `docs/` documentation**:
   - **`docs/architecture.md`**: Update to standard anatomy (`# Architecture — Vault Files`), purpose paragraph, "read first" links, numbered sections covering design goals, layered architecture, package layout, Room schema, session-based unlocks, SAF `VaultDocumentsProvider` design, and theme.
   - **`docs/build.md`**: Update to standard anatomy (`# Build and Test — Vault Files`), purpose paragraph, "read first" links, numbered sections covering Gradle tasks, test execution, Roborazzi screenshots, keystore configuration, and secrets plugin.
   - **`docs/features.md`**: Update to standard anatomy (`# Features — Vault Files`), purpose paragraph, "read first" links, numbered sections detailing accurate app capabilities and security caveats.
   - **`docs/security.md` (NEW)**: Create the living security blueprint following `docs/guidelines/security.md`, documenting the offline model, minimal permissions, AES-256-GCM Secure Notes crypto design, Room plain-text PIN reality, Vault simulation details, SAF exposure protection, and backup considerations.
   - **`docs/release_process.md` (NEW)**: Create the living release runbook following `docs/guidelines/release_process.md`, documenting versioning policy, `vfkeystore.jks` release keystore, `keystore.properties` handling, `./gradlew assembleRelease` build commands, APK verification with `apksigner`, and pre-release checklist.

3. **Verify Code and Build**:
   - Ensure all unit tests run and pass (`./gradlew testDebugUnitTest`).
   - Ensure project structure adheres to guideline conventions (Pattern B About constants via `about.properties`, gitignored keystore properties).
