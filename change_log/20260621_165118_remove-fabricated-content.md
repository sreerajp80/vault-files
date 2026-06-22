# Change log: Remove all fabricated/seeded content (production cleanup)

Implements `plans/20260621_164421_remove-fabricated-content.md`.

## Summary

Removed every source of fabricated demo data from the app and added a one-time cleanup that
deletes junk already written on existing installs. Sandbox/device storage now reflects real
filesystem capacity instead of a simulated 128 MB cap.

## Files changed

### `app/src/main/java/com/example/data/StorageRepository.kt`

- **Removed generation code:**
  - `seedInitialDataIfNeeded()` — created 6 category folders + 11 dummy files in `filesDir/Storage`.
  - `createDummyFile()` — size-padding writer used only for seeding.
  - `getVirtualFile()` — wrote fake files into `cacheDir/virtual_external`.
  - `seedDeviceExternalStorageIfNeeded()` — seeded the virtual cache and wrote dummy files into the
    user's real DCIM/Pictures/Download/Documents/Music/Movies folders; also removed its call from
    `getStorageUsageStats`.
  - Private `hasAllFilesPermission()` helper (only used by the device seeder).
  - Removed the now-unused `import java.io.IOException`.
- **`getFilesAndFoldersInDirectory()`** — dropped the virtual-item merge block and the
  `isVirtual`/`virtual_external` resolved-path handling; now lists only real files. Secured-folder
  detection, hidden-file filtering, sizing, category and sort order are unchanged.
- **`getStorageUsageStats()`** — removed the `virtual_external` scan and the simulated 128 MB
  branch. Now always computes real capacity via `StatFs(storageRoot.path)` for both sandbox and
  device roots; unscanned partition usage is reported under "Other".
- **Added one-time cleanup:**
  - `purgeFabricatedDataIfNeeded()` — guarded by a new `fabricated_data_purged_v1` settings flag so
    it runs once. Deletes the `virtual_external` cache wholesale; deletes seeded sandbox files by
    exact name + seeded size and then removes the 6 category folders only if empty; deletes the
    dummy files written into real external folders, only when filename and exact byte-size match.
  - `deleteSeededFiles()` — private helper that deletes a `(subDir, name)` entry under a root only
    when it exists and `length()` equals the seeded size.

### `app/src/main/java/com/example/ui/StorageViewModel.kt`

- `init` now calls `repository.purgeFabricatedDataIfNeeded()` instead of
  `repository.seedInitialDataIfNeeded()`; updated the misleading comment.

## Notes

- `phone_lock_delete_enabled` was previously defaulted to `true` inside the removed seeder;
  `isPhoneLockDeleteEnabled()` already defaults to `true`, so behavior is preserved.
- The pending status-bar inset fix (`plans/20260621_163716_storage-tab-status-bar-inset.md`) was
  intentionally not included here.

## Verification

- `./gradlew compileDebugKotlin` — BUILD SUCCESSFUL.
- Manual on-device verification (clean-up of pre-existing junk, real capacity display, device-mode
  listing without injected fakes) still to be performed per the plan's verification section.
