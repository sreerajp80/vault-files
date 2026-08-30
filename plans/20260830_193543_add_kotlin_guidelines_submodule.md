# Add Kotlin Guidelines Git Submodule

**Status:** completed

## What the issue is

The repository has a `docs/GUIDELINES_MANIFEST.md` file describing shared Kotlin guideline documents, but the guidelines repository has not yet been attached as a Git submodule at `docs/guidelines/`, and neither `CLAUDE.md` nor `AGENTS.md` reference `docs/GUIDELINES_MANIFEST.md`.

The submodule URL provided is: `https://github.com/sreerajp80/Kotlin_Guidelines`.

## Files to be changed

1. **`.gitmodules`** (created/updated via git submodule)
   - Add submodule entry for `docs/guidelines` with URL `https://github.com/sreerajp80/Kotlin_Guidelines`.
2. **`docs/guidelines/`** (new submodule directory)
   - Cloned submodule content.
3. **`CLAUDE.md`**
   - Add reference to `docs/GUIDELINES_MANIFEST.md` in the documentation / guidelines section.
4. **`AGENTS.md`**
   - Add reference to `docs/GUIDELINES_MANIFEST.md` in the documentation / guidelines section.

## Plan for the fix

1. Run `git submodule add https://github.com/sreerajp80/Kotlin_Guidelines docs/guidelines`.
2. Initialize and update submodule if needed (`git submodule update --init --recursive`).
3. Update `CLAUDE.md` and `AGENTS.md` with pointers to `docs/GUIDELINES_MANIFEST.md`.
4. Verify submodule status and presence of guidelines files.
