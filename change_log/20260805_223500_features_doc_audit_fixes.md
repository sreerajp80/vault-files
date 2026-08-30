# Change log: features.md audit fixes

Implements [plan/20260805_222000_features_doc_audit_fixes.md](file:///l:/Android/vault-files/plans/20260805_222000_features_doc_audit_fixes.md).

## What changed

Fixed `docs/features.md` after checking it against the real app code. All changes are
documentation only — no app code was touched.

1. **Top summary expanded.** "What this app is" now also states, at the top level, that
   the app can act as a file picker, receive shared files, be asked to open a specific
   file by another app, and expose its storage in the system file picker (read and write).

2. **Fixed a wrong claim about tapping a file.** The doc used to say a single tap opens
   APKs with the system installer and other files with the matching app. That's not what
   the code does: a single tap only handles `.zip`, `.securenote`, image preview, and text
   preview — anything else just shows a status toast. The APK/installer and "open with
   matching app" behaviour actually happens on a **double tap**, which is now documented
   as its own bullet.

3. **Fixed an overstated claim about thumbnails.** The doc said Grid view shows real GIF/
   SVG thumbnails. In code, GIF/SVG decoding is only wired up for the full-screen image
   preview; the small thumbnails in Grid/List/Compact use Coil's default loader, which
   has no GIF/SVG support, so those formats fall back to a generic icon in the thumbnail
   views. The doc now says this explicitly.

4. **Clarified the system file picker (SAF) integration.** The doc said other apps can
   "browse into" the app's exposed storage, which read as read-only. The provider also
   supports create/delete/rename from other apps, so the doc now says so.

5. **Clarified "Protect delete & move actions" scope.** Now states explicitly that
   "Move to Vault" started from the Files tab is covered by this setting (it already was
   in code, just wasn't said outright).

6. **Documented that "Select all" asks for confirmation** before it runs, using the same
   confirmation dialog as destructive actions — this wasn't mentioned before.

## How this was checked

Read the relevant source files directly (`FileExplorerScreen.kt`, `MainActivity.kt`,
`VaultDocumentsProvider.kt`, `StorageViewModel.kt`, `SettingsScreen.kt`, and others) and
confirmed each fix against the actual code before editing the doc, rather than relying on
the doc's own prior wording.
