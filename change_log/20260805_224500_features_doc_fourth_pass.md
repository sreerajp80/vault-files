# Fourth audit pass on docs/features.md

Implements: `plans/20260805_223800_features_doc_fourth_pass.md`

## What changed

Made seven small, targeted edits to `docs/features.md` after a fresh critical re-check of the
doc against the current code:

1. Added "share files out to other apps" to the top summary paragraph — it was documented
   later in the file but missing from the summary.
2. Added a new caveat bullet explaining that naming collisions are handled inconsistently
   across the app: copy/move/create/rename silently fail, shared-in files and SAF-created files
   auto-rename with `(1)`, `(2)`, etc., and **Vault Restore silently overwrites** any existing
   file with the same name in the "Restored" folder with no warning — flagged as the one real
   data-loss risk in the group.
3. Noted that, unlike view mode, the chosen sort mode/direction is not saved anywhere and
   resets to Name/Ascending on every app restart.
4. Reworded the multi-select actions bullet so "Select all" isn't singled out as an unusual
   case for asking OK/Cancel confirmation — that dialog is actually the default for most
   menu actions (Hide/Unhide, Shield/Unshield, Decompress, Create shortcut, "Set as", Select
   all); only Share, Copy file path, and Open with skip it, and Delete/Move to Vault use the
   separate phone-lock confirmation instead.
5. Noted that once an app PIN is set, there is no way to clear it back to "no PIN" through the
   UI — only to overwrite it with a different PIN.
6. Tightened the decompress wording: extracting `foo.zip` creates a sibling folder named
   exactly `foo_extracted`, not just "a new folder."
7. Added a note that Android 10 is a special case for the storage permission flow: the
   manifest's `requestLegacyExternalStorage="true"` means the ordinary storage permission alone
   grants broad access on that one OS version, without needing "All files access."

## Why

The rest of the document was re-checked against the code (MainActivity, FileExplorerScreen,
StorageViewModel, StorageRepository, Room DAOs, VaultDocumentsProvider, the manifest, and the
backup XML files) and found to still be accurate — no other changes were made.
