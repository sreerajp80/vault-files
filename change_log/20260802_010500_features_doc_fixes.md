# Change Log: Fix gaps and inaccuracies in docs/features.md

Implements plan [20260802_010000_features_doc_fixes.md](file:///l:/Android/vault-files/plans/20260802_010000_features_doc_fixes.md).

## What was changed

`docs/features.md` only — no app code changes.

1. **Caveats section**: added a note that the Room database uses destructive migration, so a
   future schema change would silently wipe the stored PIN, shielded-folder list, and Vault
   index, orphaning the Vault's file bytes on disk. Also extended the "Vault does not encrypt
   files" caveat to cover files shared in directly to the Vault, not just "Move to Vault".
2. **App shell section**: clarified that the OS biometric prompt can itself fall back to the
   phone's own screen-lock PIN/pattern, separate from this app's own PIN. Added a bullet
   describing the fallback behaviour when another app asks this app to open a file type it
   doesn't recognise (it gets silently imported as a "shared" file instead of failing).
3. **File Explorer multi-select menu**: corrected the "create a home-screen shortcut" bullet
   — it pins a generic app-launch shortcut, not a deep link back to that specific file/folder.
4. **Secure Vault tab section**: noted that Restore and Delete are not covered by the
   "Protect delete & move actions" setting, unlike Files-tab delete/move.
5. **Settings > Security section**: reworded the "Protect delete & move actions" bullet to
   scope it to the Files tab and cross-reference the Vault exception above.
6. **Secure Notes encryption section**: added a bullet noting `.securenote` files have their
   own dedicated VIEW intent-filter, so another app can ask to open a specific note directly.

These fixes came from a code cross-check (MainActivity, StorageViewModel, StorageRepository,
CryptoManager, VaultDocumentsProvider, AppDatabase, all UI screens, AndroidManifest.xml,
strings.xml) that found the doc was accurate overall but missed these seven points.
