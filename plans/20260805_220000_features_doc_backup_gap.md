# Add missing Auto Backup caveat to docs/features.md

**Status:** completed

## Files to change
- `docs/features.md`

## What the issue is

A fresh, independent re-check of `docs/features.md` against the actual code (manifest, all
screens, ViewModel, repository, database, crypto code, strings) found the doc is accurate and
thorough overall — one earlier round of fixes already caught several gaps. This second pass
found one real gap and one small related detail that fit the doc's own stated purpose (calling
out places where the app's security claims are weaker than they sound):

1. **Missing: Auto Backup is turned on, with (almost) nothing excluded.**
   `app/src/main/AndroidManifest.xml:11-13` sets `android:allowBackup="true"`, pointing at
   `backup_rules.xml` and `data_extraction_rules.xml`. Checking those files
   (`app/src/main/res/xml/backup_rules.xml`), the only rule present excludes one
   SharedPreferences file (`device.xml`) — it does **not** exclude the Room database
   (`vault_files_database`, which the doc's own caveats say stores the app PIN in plain
   text) or the app's private storage folders (`filesDir/Storage`, `filesDir/Vault` — the
   Vault's raw, unencrypted file bytes). This means Android's Auto Backup (cloud backup on
   API 31+, or `adb backup`, or OEM device-to-device transfer) can copy the plaintext PIN,
   the shielded-folder list, and every Vault file off the device. This is exactly the kind
   of caveat the doc's "Read this before assuming the app encrypts or secures anything"
   section exists to flag, and it isn't mentioned anywhere.

2. **Small related detail: the Vault's own unlock dialog button is labelled "Decrypt".**
   `app/src/main/res/values/strings.xml` defines `action_decrypt` = "Decrypt", used as the
   confirm button on the Vault's PIN-fallback unlock dialog
   (`SecureVaultScreen.kt:316`). This reinforces the same misleading "the Vault encrypts
   files" impression that the doc's caveats section already goes out of its way to correct
   — worth a one-line mention right next to that existing caveat.

The top-level "What this app is" description and the rest of the doc structure were checked
and found accurate and inclusive (all 4 tabs, the AI/Firebase non-feature, the security scope);
no other missing feature, screen, or capability was found in this pass.

## Plan for the fix

Edit `docs/features.md` in place, no restructuring:
- In the "Read this before assuming the app encrypts or secures anything" caveats section, add
  a new bullet describing the Auto Backup exposure: `allowBackup="true"` with no exclusion
  rule for the Room database or the app's private storage/Vault folders, so Android's Auto
  Backup / `adb backup` / device transfer can copy the plaintext-PIN database and all Vault
  file bytes off the device.
- In the same section, extend (or add a short adjacent note to) the existing "Vault does not
  encrypt files" bullet to mention that the Vault's own unlock dialog button is labelled
  "Decrypt", which is misleading given the above.

No app code changes — documentation only.
