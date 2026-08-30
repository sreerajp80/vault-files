# Fix features.md based on code audit

**Status:** completed

## Files to be changed

- `docs/features.md`

## What the issue is

The user asked to critically check `docs/features.md` against the real app code, and
make sure the "App Description" part covers everything. A careful read of the source code
found a few places where the document is wrong or leaves things out:

1. **Top summary ("What this app is") is incomplete.** It only says the app is "a file
   manager and storage-analytics app with some security-flavoured extras." It never
   mentions, at the top level, that the app also:
   - acts as a file picker for other apps,
   - can receive files shared in from other apps,
   - can be asked to open a specific image/text/note/folder by another app,
   - exposes its own storage as a source inside the system file picker.
   These are all real and already described later in the doc, just not in the opening
   summary — so a reader skimming only the top section would miss them.

2. **"Open a file by tapping it" is wrong (File Explorer section, line ~144-145).**
   The doc says a single tap routes APKs to the installer and opens other files with the
   matching app. In the actual code, a single tap only handles `.zip` (decompress),
   `.securenote` (open note), images (preview, if enabled), and previewable text
   (preview, if enabled) — any other file type just shows a toast message ("Viewing:
   filename, size"), it does not open. The APK-installer / "open with matching app"
   behaviour the doc describes is real, but it happens on a **double-tap**, not a single
   tap. This double-tap gesture is not mentioned anywhere in the doc.

3. **Grid/List/Compact thumbnails do not actually show GIF/SVG (line ~122-123).**
   The doc says "Grid view shows real image thumbnails (including GIF/SVG)". In code,
   only the full-screen image preview dialog uses a Coil image loader with GIF/SVG
   decoders registered. The small thumbnails in Grid, List, and Compact views use Coil's
   default loader, which has no GIF/SVG decoders — so GIF and SVG files fall back to a
   generic icon in the thumbnail views, and only render correctly once opened full-screen.

4. **SAF exposure is undersold as "browse" only (Cross-cutting section).**
   The doc says the app's sandbox storage is exposed "so other apps can browse into it,"
   which reads as read-only. In code, the provider also supports create, delete, and
   rename through the system picker — so another app can write into this app's storage
   through SAF, not just look at it.

5. **"Protect delete & move actions" scope is implicit (Settings section).**
   The doc says this setting covers "delete/move actions in the Files tab" and separately
   notes Vault Restore/Delete are excluded. It never says outright that "Move to Vault"
   (started from the Files tab) is also covered by this same setting. This is correct in
   code, just not spelled out, and the doc is otherwise careful to be explicit about
   exactly what is/isn't covered.

6. **"Select all" also asks for confirmation — not documented.**
   Choosing "Select all" from the multi-select menu pops the same OK/Cancel confirmation
   dialog used for destructive actions, before it actually selects everything. This small
   but real behaviour isn't mentioned anywhere.

## Plan for the fix

Edit `docs/features.md` only, section by section:

1. Expand the "What this app is" summary paragraph to add one sentence covering the
   app's role as a file picker / share target / view-intent handler / SAF storage
   source, so the top-level description is a complete picture, not just file-manager +
   security extras.

2. Fix the "Open a file by tapping it" bullet in the File Explorer section: describe
   single-tap correctly (zip/secure-note/image-preview/text-preview only, else shows a
   toast) and add the double-tap gesture as its own bullet (opens externally — APK to
   installer with permission check, everything else with the matching app).

3. Correct the grid-thumbnail bullet to note that GIF/SVG only render correctly in the
   full-screen preview, not in the Grid/List/Compact thumbnails (which fall back to a
   generic icon for those formats).

4. Reword the cross-cutting SAF bullet to say the exposed storage supports create,
   delete, and rename from other apps via the system picker, not just browsing.

5. Add a clause to the "Protect delete & move actions" bullet (Settings > Security)
   stating explicitly that Move-to-Vault from the Files tab is also covered by this
   setting.

6. Add a short note under File Explorer multi-select actions that "Select all" also
   triggers the same confirmation dialog before executing.

No code changes — this is a documentation-only fix. After edits, re-read the whole file
once to make sure section flow and phrasing stay consistent with the rest of the doc's
style (plain English, "accurate not aspirational" tone).
