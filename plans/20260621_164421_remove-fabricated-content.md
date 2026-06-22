# Plan: Remove all fabricated/seeded content (production cleanup)

## Issue

The app is in production but ships **fabricated demo data**. On first run / on every
storage scan it generates fake files and folders, which is what the user sees as
"App Sandbox" usage and the seeded folders in the Files tab. There are four sources, all in
`app/src/main/java/com/example/data/StorageRepository.kt`:

1. **Sandbox seeding** — `seedInitialDataIfNeeded()` (lines 34–67) creates 6 category folders
   (`Images, Videos, Audio, Documents, Archives, Others`) and writes 11 dummy files into the
   app-private `filesDir/Storage`. Called from `StorageViewModel.init` (`StorageViewModel.kt:85`).
2. **Virtual external files** — `getVirtualFile()` (lines 89–97) writes dummy files into
   `cacheDir/virtual_external`; the merge block in `getFilesAndFoldersInDirectory` (lines 114–154)
   injects them into "Entire Device" listings; `getStorageUsageStats` scans them into totals
   (lines 334–340).
3. **Real device-storage pollution** — `seedDeviceExternalStorageIfNeeded()` (lines 214–295,
   called from line 298) unconditionally seeds the virtual cache AND, when All-Files access is
   granted, writes dummy files directly into the user's **real** `DCIM, Pictures, Download,
   Documents, Music, Movies` folders.
4. **Simulated capacity** — `getStorageUsageStats` hard-codes the sandbox total to 128 MB
   (line 355) instead of real filesystem capacity.

Because the app is already deployed, removing the generation code is not enough: existing
installs already have this junk on disk (including dummy files sitting in users' real photo
folders). A one-time cleanup is required.

## Decisions (confirmed with user)

- **Cleanup scope:** Stop generating AND clean up already-seeded content on app update.
- **Real-storage junk:** Delete fabricated files from real external folders, but only when the
  filename AND the exact seeded byte-size both match (minimize risk to genuine files).
- **Sandbox total:** Use real `StatFs` capacity (consistent with Entire Device mode), drop the
  128 MB simulation.

## Files to be changed

- `app/src/main/java/com/example/data/StorageRepository.kt`
- `app/src/main/java/com/example/ui/StorageViewModel.kt`

## Plan for the fix

### A. Remove all content generation (StorageRepository.kt)

1. Delete `seedInitialDataIfNeeded()` (lines 34–67).
2. Delete `createDummyFile()` (lines 69–87) — only used for generation.
3. Delete `getVirtualFile()` (lines 89–97).
4. Delete `seedDeviceExternalStorageIfNeeded()` (lines 214–295) and its call at line 298.
5. Delete the private `hasAllFilesPermission()` helper (lines 206–212) — only used by #4.
6. In `getFilesAndFoldersInDirectory`:
   - Remove the virtual-merge block (lines 114–154) and the now-unused `externalRoot` /
     `isUnderExternal` vars (lines 102–103).
   - Simplify the mapping (lines 158–179): drop the `isVirtual` / `virtual_external` resolved-path
     logic; use the real `file.absolutePath` directly. Keep the secured-folder check, hidden-file
     filter, sizing, category and sort behavior unchanged.

### B. Show real capacity in both modes (StorageRepository.kt)

In `getStorageUsageStats`:
- Remove the virtual_external scan (lines 334–340).
- Replace the `if external … else simulated-128MB …` branch (lines 342–357) with a single real
  `StatFs(storageRoot.path)` computation that works for both the sandbox (`filesDir`) and the
  external root: `totalLimitBytes = blockCount*blockSize`, `usedBytes = total - free`, and add the
  unscanned remainder (`usedBytes - totalSize`, floored at 0) into `otherBytes`. This keeps the
  category breakdown real while reporting honest device capacity/free.

### C. One-time cleanup migration (StorageRepository.kt + StorageViewModel.kt)

Add `suspend fun purgeFabricatedDataIfNeeded()`, guarded by a new settings flag
`fabricated_data_purged_v1` so it runs at most once per install:

1. **Virtual cache** — delete `cacheDir/virtual_external` recursively (pure junk, always safe).
2. **Sandbox seeded files** — for each known seeded file under `filesDir/Storage`
   (`<category>/<name>` with its exact seeded size), delete only if it exists and
   `length() == knownSize`. Afterwards, delete each of the 6 category folders only if it is now
   empty (preserves any real files/folders the user added).
3. **Real external junk** — for each known fabricated file under
   `Environment.getExternalStorageDirectory()` (`<dir>/<name>` + exact size), delete only if it
   exists and `length() == knownSize`. Wrap in try/catch so missing permission fails silently.
4. Set `fabricated_data_purged_v1 = true`.

Known size tables are taken verbatim from the current seed code:
- Sandbox: vacation_mountains.jpg/2_400_000, avatar_hq.png/850_000, graduation_clip.mp4/15_300_000,
  nature_loop_4k.mp4/42_000_000, acoustic_tune.mp3/6_400_000, voice_meeting_record.wav/11_100_000,
  yearly_tax_statement_2025.pdf/1_600_000, grocery_list.txt/1500, inventory_sheets.xlsx/720_000,
  old_photos_backup.zip/14_900_000, app_workspace_config.json/45_000.
- External: DCIM/family_vacation_skyline_2026.jpg/33_400_000, DCIM/camera_shot_hdr.png/14_850_000,
  Pictures/scenery_gathering.jpg/10_900_000, Pictures/minimalist_workspace.jpg/5_200_000,
  Pictures/tax_receipt_scan.png/1_450_000, Download/report_yearly_template.pdf/4_980_000,
  Download/invoice_48291_rev.pdf/1_240_000, Download/archived_assets_backup.zip/64_400_000,
  Documents/personal_manifesto_notes.txt/120_000, Documents/salary_sheet_may_jun.xlsx/7_250_000,
  Music/lofi_coding_ambient.mp3/17_200_000, Music/orchestral_symphony.mp3/16_100_000,
  Movies/drone_footage_beach_4k.mp4/118_500_000, Movies/cat_funny_moments_hd.mp4/54_400_000.

5. In `StorageViewModel.init` (`StorageViewModel.kt:84–85`): replace the
   `repository.seedInitialDataIfNeeded()` call with `repository.purgeFabricatedDataIfNeeded()` and
   fix the misleading "Populate … gorgeous starter materials" comment.

### Notes / side effects

- `phone_lock_delete_enabled` was defaulted to `true` inside the removed seed method;
  `isPhoneLockDeleteEnabled()` already defaults to `true` (`?: true`), so behavior is preserved.
- After cleanup, a fresh sandbox shows "0 files" and real device capacity — expected and correct.
- The earlier status-bar inset fix
  (`plans/20260621_163716_storage-tab-status-bar-inset.md`) is a separate change and remains
  pending its own approval; it is not included here.

## Verification

- Build: `./gradlew assembleDebug`
- First launch on an install that previously had seeded data: confirm the 6 fake folders and
  dummy files are gone from the sandbox, `virtual_external` cache is gone, and Storage shows real
  capacity with 0 categorized files.
- Confirm "Entire Device" mode lists only real files (no injected fakes) and that any
  previously-written dummy files in real DCIM/Pictures/etc. are removed.
- Confirm no regression in file create/delete, vault, and secured-folder behavior.
