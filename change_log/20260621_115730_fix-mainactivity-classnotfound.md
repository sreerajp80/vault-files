# Change log: Fix MainActivity ClassNotFoundException crash on launch

Implements plan `plans/20260621_115700_fix-mainactivity-classnotfound.md`.

## What changed

- `app/src/main/AndroidManifest.xml`: changed the launcher activity declaration from the
  relative `android:name=".MainActivity"` to the fully-qualified
  `android:name="com.example.MainActivity"`.

## Why

The relative `.MainActivity` resolved against the module `namespace`
(`in.sreerajp.vault_files`), so the system looked for
`in.sreerajp.vault_files.MainActivity`, which does not exist — the class is
`com.example.MainActivity`. This caused a `ClassNotFoundException` and a fatal crash on
every launch. The fully-qualified name points the manifest at the real class.

No Kotlin source, `namespace`, or `applicationId` was changed.
