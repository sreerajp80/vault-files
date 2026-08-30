# Fifth audit pass on docs/features.md

**Status:** completed

## What the issue is

The user asked for a critical re-check of `docs/features.md` against the current code, to make
sure every feature is listed and the app description is complete. Four earlier passes already
happened (see `change_log/20260805_*_features_doc_*.md`), so this pass looked only for things
those missed.

A background code search found four real gaps between the doc and the code:

1. **Vault file size is never unit-scaled.** `SecureVaultScreen.kt:390` always prints the size
   as `"%.2f KB"`, even for files in the MB/GB range (so a 2 GB file would show as
   "2097152.00 KB"). The Files tab has a proper B/KB/MB/GB/TB formatter
   (`FileExplorerScreen.kt` `formatBytes()`), but the Vault list does not use it. The doc's
   "Secure Vault tab" section says the list "shows... size" without flagging that this
   formatting is broken/inconsistent.

2. **A second "grant All-files access" button exists on the Storage tab**, separate from the
   one-time first-launch permission flow. `StorageAnalyzerScreen.kt:177-253`: whenever "Entire
   Device" is selected as the source and permission isn't held, a card with its own "Grant"
   button (same underlying permission-request logic) appears. The doc only documents the
   one-time first-launch flow and doesn't mention this always-available second entry point.

3. **The system file picker serves real image thumbnails**, not just generic icons, for files
   this app exposes there. `VaultDocumentsProvider.kt:108-121` implements
   `openDocumentThumbnail`, and line 190 sets `FLAG_SUPPORTS_THUMBNAIL` for image files. The
   doc's cross-cutting bullet about the system-picker integration doesn't mention this.

4. **"Protect delete & move actions" has the same PIN-required/re-verify-to-disable gating as
   the other two protection toggles, but the doc doesn't say so for this one.**
   `SettingsScreen.kt:420-456` shows turning it on without a PIN set opens the PIN-setup dialog
   (same as the other two toggles), and turning it off requires biometric/PIN re-verification.
   The doc states this gating explicitly for "Password protect app" and implies it for "Password
   protect hidden items," but the "Protect delete & move actions" bullet omits it, even though
   the code treats all three identically.

All four were independently verified by re-reading the exact source lines above — not just
taken from the search results.

No gaps were found in: MainActivity, StorageViewModel, StorageRepository, AndroidManifest.xml,
Room entities/DAOs, or build.gradle.kts — those areas of the doc are still accurate.

The "What this app is" summary paragraph was also re-read for inclusiveness. It already covers:
app type, tabs, security-flavoured extras, the one real encryption feature, and every
cross-app integration point (file picker, share-in, share-out, open-specific-file, SAF source).
No missing capability was found there — it stays as-is.

## Files to change

- `docs/features.md` — four targeted edits (see below). No other file changes.

## Plan for the fix

In `docs/features.md`:

1. In the **Secure Vault tab** section, add a sentence to the "Vault list shows..." bullet
   noting the size is always shown as raw KB with no unit scaling (unlike the Files tab), so
   large files display an unwieldy number.

2. In the **Storage Analyzer tab** section, add a bullet noting that selecting "Entire Device"
   without All-files access shows a dedicated "Grant" card/button right there on the tab — a
   second, always-available entry point to the same permission request, separate from the
   one-time first-launch flow.

3. In the **Cross-cutting features** section, extend the system-file-picker bullet to note that
   image files served through it get real thumbnails (not generic icons).

4. In the **Settings tab → Security** section, add the same "needs a PIN set first; turning off
   requires biometric/PIN re-verification" caveat to the "Protect delete & move actions" bullet
   that the doc already states for the other two protection toggles.

No wording elsewhere changes. This keeps the doc's existing structure and tone.
