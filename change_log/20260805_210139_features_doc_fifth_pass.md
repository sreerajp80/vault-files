# Fifth audit pass on docs/features.md

Implements: `plans/20260805_210139_features_doc_fifth_pass.md`

## What changed

Made four targeted additions to `docs/features.md` after a fresh critical re-check of the doc
against the current code (four earlier passes had already happened, so this pass looked only for
what those missed):

1. Noted that the Vault list always shows file size as raw kilobytes with no unit scaling
   (unlike the Files tab's B/KB/MB/GB/TB formatter), so large files show an unwieldy number.
2. Added a bullet to the Storage Analyzer section noting the "Entire Device" source shows its
   own "Grant" button for All-files access right on that tab — a second, always-available entry
   point separate from the one-time first-launch permission flow.
3. Noted that images browsed through the system file picker (into this app's exposed storage)
   get real thumbnails, not just a generic icon.
4. Added the "needs a PIN set first; turning off requires re-verification" caveat to "Protect
   delete & move actions," matching what the doc already says for the other two protection
   toggles — the code gates all three identically.

## Why

A background code search across MainActivity, StorageViewModel, StorageRepository, all UI
screens, the manifest, Room entities, and build.gradle.kts found these four gaps; each was
independently verified by reading the exact source lines before being added to the doc. No
other gaps were found — the rest of the document, including the "What this app is" summary
paragraph, was re-checked and found still accurate and inclusive.
