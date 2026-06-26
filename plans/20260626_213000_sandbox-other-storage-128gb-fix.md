# Fix: App (sandbox) source reports ~128 GB of phantom "Other" storage

## Issue
On the Storage Analysis screen with the **App** source selected (`sandbox` mode),
the "Other formats" category tile shows a huge size (e.g. 128.08 GB / 55%) even
though the app sandbox has essentially no files.

### Root cause
`StorageRepository.getStorageUsageStats(storageRoot)`
(`StorageRepository.kt:305-360`) computes:

```
val stat = android.os.StatFs(storageRoot.path)
val totalInternalMax = stat.blockCountLong * stat.blockSizeLong
val freeBytes = stat.availableBlocksLong * stat.blockSizeLong
val usedBytesResult = totalInternalMax - freeBytes
val unscannedSpace = (usedBytesResult - totalSize).coerceAtLeast(0L)
val finalOtherBytes = otherSize + unscannedSpace
```

For the **sandbox** source the root is `filesDir/Storage`
(`StorageRepository.kt:47`). `StatFs` on that path reports the stats of the whole
`/data` partition, so `usedBytesResult` is the **entire device's** used space
(~128 GB), while `totalSize` only itemizes the app's own seeded files (~0). The
difference (`unscannedSpace`) is therefore ~128 GB and gets dumped into "Other",
and `usedBytes` shows the whole partition in the ring too.

Attributing unscanned partition space to "Other" is only meaningful for the
**device** source (real external storage, where files genuinely exist that we
cannot itemize). For the app sandbox it is wrong.

## Files to change
- `app/src/main/java/com/example/data/StorageRepository.kt`
- `app/src/main/java/com/example/ui/StorageViewModel.kt`

## Plan

### 1. `StorageRepository.getStorageUsageStats`
- Add a parameter `isDeviceSource: Boolean` (default `true` to preserve existing
  callers, though there is only one).
- Keep the real-capacity readout (`totalInternalMax`, `freeBytes`) for both
  sources so the ring's "X free of Y" still shows real device capacity.
- Branch the figures:
  - **device source** (`isDeviceSource == true`): unchanged —
    `otherBytes = otherSize + unscannedSpace`, `usedBytes = usedBytesResult`.
  - **sandbox source** (`isDeviceSource == false`): report only what was actually
    scanned in the app sandbox —
    `otherBytes = otherSize` (no `unscannedSpace`),
    `usedBytes = totalSize` (the app's own files).
    `totalLimitBytes` stays `totalInternalMax`.

This makes the App view show the app's real footprint (~0) with an empty "Other"
tile, while the Entire-Device view is untouched.

### 2. `StorageViewModel.refreshStorageStats` (`StorageViewModel.kt:282-286`)
- Pass the flag from the current mode:
  `repository.getStorageUsageStats(userStorageRoot, isDeviceSource = _storageSourceMode.value == "device")`.

## Out of scope / notes
- No UI changes to `StorageAnalyzerScreen.kt`; the tiles/ring already render
  whatever the stats provide.
- The "Entire Device" source behavior is intentionally left exactly as-is.
- This is a logic fix only; no string or layout changes.
