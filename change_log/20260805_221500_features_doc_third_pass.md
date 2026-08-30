# Change Log: Fix four remaining gaps in docs/features.md (third pass)

Implements plan [20260805_221200_features_doc_third_pass.md](file:///l:/Android/vault-files/plans/20260805_221200_features_doc_third_pass.md).

## What was changed

`docs/features.md` only — no app code changes.

1. **"What this app is" section**: reworded the Firebase AI sentence. It previously said
   Firebase AI and the networking libraries are "present but unused." Checked
   `app/build.gradle.kts` and found the Firebase AI dependency is commented out — it isn't
   compiled into the app at all (only a no-code version-alignment BOM is applied). The
   networking libraries (Retrofit/OkHttp/Moshi) genuinely are compiled in and unused, so that
   part was left as is.
2. **Caveats section, Auto Backup bullet**: corrected the claim that only `device.xml` is
   excluded from backup. Checked `backup_rules.xml` and `data_extraction_rules.xml` — both
   have their include/exclude rules entirely commented out, so nothing is excluded at all, on
   any Android version. Reworded the bullet to say the exposure is total, not "everything
   except one file."
3. **File Explorer tab, Search & sort section**: added a sentence noting that switching sort
   mode inside the sort dialog resets the direction to that mode's own default (ascending for
   Name, descending for Size/Date) instead of preserving the previously chosen direction. Found
   by reading the (currently uncommitted) sort dialog code in `FileExplorerScreen.kt`.
4. **Cross-cutting features section, localization bullet**: added a caveat listing the known
   hardcoded-English strings that don't translate to Malayalam — the file-picker mode's header
   and Cancel button, the multi-select FAB's "Done (N)" text, and three status messages in
   `StorageViewModel.kt` for opening files requested by other apps.

These four points came from a third, independent cross-check of the doc against the code
(build.gradle.kts, backup_rules.xml, data_extraction_rules.xml, FileExplorerScreen.kt,
StorageViewModel.kt), following the two earlier fix rounds referenced in
`change_log/20260802_010500_features_doc_fixes.md` and
`change_log/20260805_120500_features_doc_backup_gap.md`. All four were independently verified
against the source files before editing the doc. No other missing feature, screen, or
inaccurate claim was found on this pass.
