# Fix gaps and inaccuracies in docs/features.md

**Status:** completed

## Files to change
- `docs/features.md`

## What the issue is
A cross-check of `docs/features.md` against the real app code found it is mostly accurate, but has a few missing features and a couple of claims that overstate what the app does. Since this file is meant to be read by another AI as ground truth, these should be fixed:

1. "Create a home-screen shortcut" is described as opening the specific item, but the code actually pins a generic app-launch shortcut (it does not deep-link to that file/folder).
2. The doc says "Protect delete & move actions" covers vault actions, but the Vault's own Restore and Delete buttons do not check that setting — only Files-tab delete/move do.
3. Opening an unrecognized file from another app (VIEW intent) silently copies it into the app as a "shared import" — this fallback path is not mentioned anywhere.
4. `.securenote` files have their own VIEW intent-filter (so another app, or the OS, can ask this app to open a specific note file) — not mentioned next to the Secure Notes encryption section.
5. The Room database uses destructive migration (wipes all tables on a version bump) — this means a future update could silently delete the stored PIN, the list of shielded folders, and the Vault's file index (leaving the actual vault file bytes orphaned on disk, unreachable). This risk is not mentioned anywhere.
6. The biometric section doesn't mention that the OS biometric prompt itself can also fall back to the phone's own screen-lock PIN/pattern (a different thing from this app's own PIN).
7. The "Vault bytes are not encrypted" caveat should also note it applies to files shared in directly to the Vault, not just "Move to Vault".

## Plan for the fix
Edit `docs/features.md` in place, no restructuring:
- Under **File Explorer tab → multi-select menu**, correct the shortcut bullet to say it pins a generic shortcut, not a deep link to the item.
- Under **Secure Vault tab**, add a line noting Restore/Delete are not covered by "Protect delete & move actions."
- Under **App shell**, add a bullet describing the unrecognized-file-from-other-app fallback (silent import as a shared file).
- Under **Secure Notes encryption**, add a bullet noting the dedicated VIEW intent-filter for `.securenote` files.
- Add a new bullet under "Read this before assuming..." caveats section about the destructive Room migration risk.
- Under **App shell** (or wherever app-lock is described), clarify the OS biometric prompt can itself fall back to device screen-lock credential, separate from the app's own PIN fallback.
- Extend the existing "Vault does not encrypt files" caveat bullet to mention shared-in-directly-to-Vault files too.

No app code changes — documentation only.
