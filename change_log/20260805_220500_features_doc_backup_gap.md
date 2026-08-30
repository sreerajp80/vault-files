# Change Log: Add missing Auto Backup caveat to docs/features.md

Implements plan [20260805_220000_features_doc_backup_gap.md](file:///l:/Android/vault-files/plans/20260805_220000_features_doc_backup_gap.md).

## What was changed

`docs/features.md` only — no app code changes.

1. **Caveats section**: added a new bullet noting the app has `android:allowBackup="true"`
   with only one small SharedPreferences exclusion, and no exclusion for the Room database
   (plain-text PIN) or the app's private storage (Vault file bytes). This means Android's
   Auto Backup, `adb backup`, or an OEM transfer tool can copy the plain-text PIN and every
   Vault file off the device.
2. **Vault caveat bullet**: extended the existing "Vault does not encrypt files" bullet to
   note that the Vault's own unlock dialog confirm button is labelled "Decrypt", which is
   misleading leftover wording, not real decryption.

This came from a second, independent cross-check of the doc against the code
(AndroidManifest.xml, backup_rules.xml, data_extraction_rules.xml, strings.xml,
SecureVaultScreen.kt), following up on an earlier fix round. The rest of the doc — including
the top-level "What this app is" description and all four tab sections — was checked again and
found accurate and inclusive; no other missing feature or screen was found.
