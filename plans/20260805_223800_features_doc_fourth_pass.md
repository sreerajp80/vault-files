# Fourth audit pass on docs/features.md

**Status:** completed

## What the issue is

The user asked to critically re-check `docs/features.md` against the real code, and make
sure the app description at the top covers everything important. This doc has already been
through three earlier audit passes (see change_log for `features_doc`, `features_doc_fixes`,
`features_doc_backup_gap`, `features_doc_third_pass`, `features_doc_audit_fixes`), so most of
it is already accurate. I re-checked it against the current code (MainActivity, FileExplorerScreen,
StorageViewModel, StorageRepository, Room entities/DAOs, VaultDocumentsProvider, the manifest,
and the backup XML files) and found seven small gaps — missing details or slightly misleading
wording. Nothing major is wrong; these are precision fixes.

## Files to change

- `docs/features.md`

## The fixes (each one is a small, targeted edit)

1. **Summary paragraph misses "share out".** Lines 13-17 list picker/share-in/open-specific-file/
   SAF-expose, but not that the app can also share files out to other apps (the normal Android
   share sheet, already documented later at line 152). Add a short mention.

2. **No mention of what happens when a file/folder name is already taken.** The doc never says
   what happens on a naming collision. In the actual code this is inconsistent and worth a
   caveat:
   - Copy, move, create-folder, create-note, and in-app rename: silently fail if the name is
     already taken (a generic "failed" message, nothing overwritten).
   - Files shared in from another app, and SAF `createDocument`: auto-add `(1)`, `(2)`, etc. to
     avoid a clash.
   - Vault Restore: silently **overwrites** any existing file of the same name in the "Restored"
     folder, with no check or warning at all — this is the one real data-loss risk in the group.
   Add a new short bullet/caveat covering this, since the Vault Restore case is a genuine risk a
   reader should know about.

3. **Sort choice isn't remembered, unlike view mode.** The "View modes" section says layout is
   "remembered as a setting." The "Search & sort" section doesn't say the opposite is true for
   sort — sort mode and direction reset to Name/Ascending every time the app restarts (they're
   not saved anywhere). Add one clause noting this.

4. **"Select all" confirmation is described as if it's a special case.** Currently the doc singles
   out Select-all as using "the same confirmation dialog used for destructive actions," which
   reads like an exception worth calling out. In the code, that OK/Cancel dialog is actually the
   default for almost every multi-select menu action (Hide/Unhide, Shield/Unshield, Decompress,
   Create shortcut, "Set as", and Select All) — only Share, Copy-path, and Open-with skip it, and
   Delete/Move-to-Vault use the separate phone-lock confirmation instead. Reword so Select-all
   isn't singled out as unusual.

5. **No note that the app PIN can't be removed, only changed.** The Security section says
   "Set/change a 4+ digit app PIN" — true, but there's no way in the UI to clear a PIN back to
   "no PIN set" once one exists, only overwrite it with a different PIN. Add a short clause.

6. **`decompressZip` destination folder name is more specific than "a new folder."** Extracting
   `foo.zip` always creates a sibling folder literally named `foo_extracted`. Tighten the wording
   at line 138 to say this exactly, since it's an easy, concrete detail to get right.

7. **Minor: Android 10 legacy storage nuance.** The manifest sets
   `requestLegacyExternalStorage="true"` plus a maxSdkVersion=29 WRITE_EXTERNAL_STORAGE permission,
   which means on Android 10 specifically the app gets broad storage access from the ordinary
   storage permission alone, without needing "All files access." The current permission-flow
   bullet glosses over this one-OS-version difference. Add a short clause for completeness (low
   priority, but accurate and easy to verify).

## What is NOT changing

Everything the audit re-checked and found still accurate (Coil GIF/SVG limits, tap-vs-double-tap
behavior, the "Decrypt" button mislabeling, destructive Room migration, plaintext PIN storage,
full Auto Backup exposure, SAF hiding of Vault/shielded folders, localization exceptions, the
"Protect delete & move" scope, Storage tab behavior, Permissions screen, biometric/PIN fallback)
stays as written — no changes there.

## After approval

Once approved, I'll make these seven edits directly in `docs/features.md`, then write a change
log entry referencing this plan.
