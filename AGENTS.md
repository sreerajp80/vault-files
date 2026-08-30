# AGENTS.md — Vault Files

This file is read by AI agents and LLM coding assistants (Gemini, Antigravity, Cursor, Windsurf, Codex, etc.) at the start of every session in this repository.
Read it before making any change. See the docs table below for full detail.

---

## Project identity

| Field | Value |
|---|---|
| App name | Vault Files |
| Type | Secure file & storage manager (analytics, explorer, shielded folders, simulated vault, encrypted notes) |
| Platform | Android (minSdk 24, targetSdk 36, compileSdk 36) |
| Package / namespace | `in.sreerajp.vault_files` |
| Kotlin | 2.1+ |
| Compose BOM | 2025.x |
| AGP | 8.x |
| JDK | 17 |
| State management | `StorageViewModel` + `StateFlow` |
| Navigation | Single-Activity tab switching (4 tabs: Storage, Files, Vault, Settings) |
| Database | Room (`vault_files_database`, destructive migration) |
| Orientation | Portrait only |
| Connectivity | Fully offline — no active network features, no `INTERNET` permission required for core features |

---

## Read these docs before working

| Document | Read when |
|---|---|
| [docs/architecture.md](docs/architecture.md) | Changing structure, screens, state, services, models, repositories |
| [docs/security.md](docs/security.md) | Touching permissions, logging, storage, crypto, manifest |
| [docs/release_process.md](docs/release_process.md) | Building a release, versioning, release checklist |
| [docs/build.md](docs/build.md) | Build tasks, testing, screenshot tests (Roborazzi), signing, Gradle gotchas |
| [docs/features.md](docs/features.md) | Feature inventory and security reality distinctions |
| [docs/GUIDELINES_MANIFEST.md](docs/GUIDELINES_MANIFEST.md) | Master index of shared Kotlin guidelines in `docs/guidelines/` |

> The local copies in `docs/` win over template files in `docs/guidelines/`.

---

## Package naming

One identifier everywhere: the source package, the build `namespace`, and the `applicationId`
are all `in.sreerajp.vault_files`.

Note that `in` is a Kotlin keyword, so it must be backticked in source — in every `package`
line and in every import of our own code:

```kotlin
package `in`.sreerajp.vault_files.ui
import `in`.sreerajp.vault_files.data.StorageRepository
```

The folder on disk is plain `in` (no backticks). Classes in the root package (`MainActivity`, tests)
do not need to import `R` or `BuildConfig` — they are generated into that same package.

---

## Hard rules (must follow — these override convenience)

1. Open source only. No commercial or source-available SDKs.
2. Offline-first. The app operates locally on device storage and sandbox files; no network call is needed.
3. Never crash on bad or unexpected input. Every parser and file operation has a safe fallback and friendly feedback message.
4. SAF exposure boundary: `VaultDocumentsProvider` MUST NEVER expose `Vault/` or shielded folders to external apps.

---

## Architecture rules

- Layout: Single-module MVVM under `app/src/main/java/in/sreerajp/vault_files/` (`config/`, `data/`, `ui/`, `utils/`, `MainActivity.kt`).
- Layer boundaries: Composables must not execute disk I/O or access Room DAOs directly; all actions route through `StorageViewModel` to `StorageRepository`.
- State flow: `StorageViewModel` is the single source of truth (`StateFlow`s for UI state, `SharedFlow` for one-shot user messages).
- Session-based gating: Shielded folder and vault unlock states are transient in-memory flags, reset on app restart.
- Models are immutable data classes (`copy()`). Never mutate in place.

---

## Build & run commands

```bash
./gradlew assembleDebug          # build debug APK
./gradlew installDebug           # build + install on connected device/emulator
./gradlew testDebugUnitTest      # run JVM/Robolectric unit tests
./gradlew connectedAndroidTest   # run instrumented tests on a device
./gradlew lint                   # Android lint
./gradlew recordRoborazziDebug   # regenerate Roborazzi golden screenshots
./gradlew verifyRoborazziDebug   # verify screenshots against goldens
```

---

## Build types / flavors

| Build type | App ID | Display name | Signing |
|---|---|---|---|
| debug | `in.sreerajp.vault_files` | Vault Files (Debug) | `vfkeystore.jks` (`debugConfig` via `keystore.properties`) |
| release | `in.sreerajp.vault_files` | Vault Files | `vfkeystore.jks` (`release` via `keystore.properties`) |

---

## Signing / keystore

- Keystore file: `vfkeystore.jks` at project root.
- Credentials: read from gitignored `keystore.properties` at project root (`storeFile`, `storePassword`, `keyAlias`, `keyPassword`).
- `.gitignore` includes: `keystore.properties`, `*.jks`, `*.keystore`.
- Keep an offline backup of `vfkeystore.jks`.

---

## Security rules

- Never log secrets, passwords, PINs, tokens, or decrypted text — even in debug builds.
- Secure Notes use genuine AES-256-GCM authenticated encryption with Android Keystore hardware-backed keys.
- Realize that the "Vault" is a byte-renaming simulation and the Room database stores PIN in plain text — see [docs/security.md](docs/security.md) for full threat boundaries.
- Respect file permissions: check runtime storage permissions before accessing external media.

---

## String resources

- All user-visible text MUST come from `res/values/strings.xml` (and `res/values-ml/strings.xml`) via `stringResource()` or `context.getString()` — never hard-coded raw string literals in Composables.
- String literals are permitted only for logs, non-UI exception messages, route identifiers, JSON keys, and asset paths.

---

## Code style / naming

- Files: `PascalCase.kt` for classes/composables; `camelCase.kt` acceptable for utilities.
- Classes: `PascalCase`; variables/methods: `camelCase`; constants: `SCREAMING_SNAKE_CASE`.
- Package naming: lowercase, backticked `` `in` `` for all Kotlin source files.
- Prefer `val` over `var`, `data class` for models, `sealed interface/class` for UI state.
- Keep Android Lint at zero errors before committing.

---

## Testing rules

- Unit and Robolectric tests live in `app/src/test/java/in/sreerajp/vault_files/`.
- UI screenshot tests use Roborazzi (`GreetingScreenshotTest.kt`).
- Maintain test coverage across ViewModel state transitions, repository file utilities (`ZipUtility`, `CryptoManager`), and intent handlers (`ShareSupport`).
- Always run `./gradlew testDebugUnitTest` before submitting changes.

---

## Dependency constraints

- Blocked: commercial analytics, proprietary closed-source SDKs, or unvetted ad libraries.
- AI/Firebase libraries: `firebase.ai` is commented out; do not wire external AI network calls without explicit instruction.
- Before adding any new dependency: verify its license, minimal impact, and compatibility.

---

## Where things live

```
AGENTS.md            # AI agent instructions (this file)
CLAUDE.md            # Claude Code project rules (dual-aligned)
docs/                # living architecture, security, build, features, release docs
plans/               # change plans (one per change, yyyymmdd_hhMMss_<slug>.md)
change_log/          # change logs (one per change, yyyymmdd_hhMMss_<slug>.md)
app/src/main/        # application source code and resources
app/src/test/        # unit and Robolectric tests
app/src/androidTest/ # instrumented Espresso tests
```

---

## Workflow rules (mandatory — from global rules)

Every change follows plan-before-changing and log-after-changing:

1. **Plan before changing.** Write a full plan to `plans/` named
   `yyyymmdd_hhMMss_<short-slug>.md` with a `**Status:**` line, the files to change, the issue,
   and the fix. Then **STOP and get explicit approval** before editing/creating/deleting any
   project file (other than the plan). A question or ambiguous reply is not approval.
2. **Log after changing.** After implementing, write a change log to `change_log/` named
   `yyyymmdd_hhMMss_<short-slug>.md` describing what changed and referencing its plan.
3. **Relative paths & privacy only.** `plans/` and `change_log/` files are committed and may become
   public on the internet. They MUST use relative repository paths only (never absolute system
   paths like `C:\...`, `l:\...`, or `file:///...`). They MUST NOT contain any **local system
   details** — OS user name, computer/host name, home or drive-letter paths, network share names,
   LAN/internal IP addresses, local server URLs with ports, device serial numbers, personal email
   addresses — or any secret (API keys, tokens, passwords, keystore passphrases, credentials, PII).
   Write them as if a stranger will read them; nothing should reveal the machine they came from.

---

## Communication rules

- **Always use simple English.** Write all responses, plans, change logs, and explanations in
  plain, simple English. Short sentences, common words. Explain any jargon you must use.

---

## What AI agents must always / never do

**Always:**
- Read this file and referenced `docs/` before making changes.
- Ensure all user-visible text uses string resources (`strings.xml`).
- Backtick the `` `in` `` keyword in all package statements and imports.
- Stop for user approval after writing a plan in `plans/`.

**Never:**
- Perform direct file I/O or DAO queries inside Composables.
- Expose `Vault/` or shielded folders through `VaultDocumentsProvider`.
- Commit keystores, credentials, or absolute local system paths.
- Log plaintext secrets, PINs, or private user files.
