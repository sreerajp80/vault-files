# Plan: Fix remaining gaps in docs/features.md (third pass)

**Status:** completed

## Files to change

- `docs/features.md` only. No app code changes.

## What the issue is

The user asked for a critical re-check of `docs/features.md` for completeness and an
inclusive app description. Two earlier fix rounds already happened (see
`change_log/20260802_010500_features_doc_fixes.md` and
`change_log/20260805_120500_features_doc_backup_gap.md`). I did a third, independent pass —
reading every screen/data file plus the manifest, backup XML files, and build.gradle.kts —
and found four things still wrong or missing, all verified directly against the code:

1. **The Auto Backup caveat is inaccurate, and understates the problem.**
   `docs/features.md` (Caveats section) says: "The backup rules file only excludes one small
   settings file (`device.xml`)". I checked `app/src/main/res/xml/backup_rules.xml` — the
   `<include>`/`<exclude>` lines (including the `device.xml` exclusion) are inside an XML
   comment, so they don't actually apply. `<full-backup-content>` is effectively empty, so
   **nothing is excluded at all**, not even `device.xml`. I also checked
   `app/src/main/res/xml/data_extraction_rules.xml` (which governs cloud backup and device
   transfer on Android 12+, alongside `backup_rules.xml` for older versions) — its
   `<cloud-backup>` block is also empty (contents commented out) and `<device-transfer>` is
   entirely commented out. So on every Android version, 100% of app data is eligible for
   backup, not "everything except one file." The real risk is slightly worse than what the
   doc currently says.

2. **The "Firebase AI... present but unused" line overstates what's in the build.**
   `docs/features.md` line 16 says "the Firebase AI and networking libraries in the build are
   present but unused." I checked `app/build.gradle.kts`: the Firebase AI dependency is
   commented out (`// implementation(libs.firebase.ai)`, line 132) — it is not compiled into
   the app at all. Only `platform(libs.firebase.bom)` (line 106) is applied, which is just a
   version-alignment helper with no code of its own. The networking libraries (OkHttp,
   Retrofit, Moshi, logging-interceptor, lines 133-139) genuinely are compiled in and unused,
   so that part of the sentence is correct. The Firebase AI part is not — it should say
   Firebase AI is commented out / not part of the build, not "present but unused."

3. **Sort dialog's per-mode default direction isn't documented.**
   In the (currently uncommitted) sort dialog code in `FileExplorerScreen.kt`, switching
   between sort modes inside the dialog resets the direction: switching to Name always resets
   to ascending, but switching to Size or Date always resets to descending
   (`tempSortAscending = (mode == FileSortMode.NAME)`, line ~1269). The doc's current
   description ("Sort by Name, Size, or Date, each ascending or descending... OK button...")
   doesn't mention that tapping a different sort mode does not preserve whatever direction was
   previously chosen — it jumps to that mode's own default direction.

4. **"Full localization" claim is contradicted by several hardcoded English strings.**
   `docs/features.md` says: "Full localization: English and Malayalam, covering both static UI
   text and in-app status messages." I confirmed (by grep, not in `strings.xml`) that these
   are hardcoded English literals with no Malayalam translation:
   - `FileExplorerScreen.kt:312` — pick-mode header text ("Pick Multiple Files" / "Pick a
     File")
   - `FileExplorerScreen.kt:326` — pick-mode "Cancel" button text
   - `FileExplorerScreen.kt:1026-1027` — multi-select FAB's content description ("Confirm
     Selection") and label ("Done (N)")
   - `StorageViewModel.kt:827,840,885` — three snackbar messages: "Loading external file...",
     "Successfully loaded: <name>", "Could not open external file."
   These won't switch to Malayalam even when the device/app language is set to Malayalam,
   which directly contradicts the "full localization" claim.

## Plan for the fix

Edit `docs/features.md` only, in four places:

1. **Caveats section, Auto Backup bullet** — rewrite to say the backup rules file and the
   data extraction rules file both have their include/exclude sections entirely commented
   out, so nothing is excluded at all (not even `device.xml`) — correcting the current "only
   excludes one file" wording to "excludes nothing."
2. **"What this app is" section** — reword the Firebase AI sentence to say the Firebase AI
   dependency itself is commented out and not compiled into the app (only the version-BOM is
   applied, which carries no code), while the networking libraries (Retrofit/OkHttp/Moshi) are
   genuinely compiled in and unused.
3. **File Explorer tab, Search & sort section** — add one sentence noting that switching sort
   mode inside the dialog resets direction to that mode's own default (ascending for Name,
   descending for Size/Date), rather than keeping the previous direction.
4. **Cross-cutting features section, localization bullet** — add a short caveat listing the
   known hardcoded-English spots (pick-mode header/cancel button, multi-select FAB text, and
   three StorageViewModel snackbar messages) that don't translate to Malayalam.

No app code changes in this pass — purely correcting/completing the documentation, per the
user's request to critically analyze and ensure the doc lists all features and gives an
inclusive app description.
