# Fix: App (sandbox) source reported ~128 GB of phantom "Other" storage

Implements plan `plans/20260626_213000_sandbox-other-storage-128gb-fix.md`.

## Problem
With the **App** source selected on the Storage Analysis screen, the "Other formats"
tile showed a huge size (~128 GB) and the storage ring reported the whole device as
used, even though the app sandbox holds almost no files. Cause: `StatFs` on the
sandbox root (`filesDir/Storage`) reports the whole `/data` partition, so all
un-itemized device space was folded into "Other" and into "used".

## Changes
- `app/src/main/java/com/example/data/StorageRepository.kt`
  - `getStorageUsageStats` now takes an `isDeviceSource: Boolean = true` parameter.
  - Device source: unchanged — unscanned partition space is added to `otherBytes`
    and `usedBytes` reflects the real partition usage.
  - Sandbox source: reports only what was actually scanned — `otherBytes = otherSize`
    (no unscanned space) and `usedBytes = totalSize` (the app's own files).
    `totalLimitBytes` still reports the real device capacity for the "free of" line.
- `app/src/main/java/com/example/ui/StorageViewModel.kt`
  - `refreshStorageStats` passes `isDeviceSource = _storageSourceMode.value == "device"`.

## Scope
- No UI/layout/string changes; the tiles and ring render whatever the stats provide.
- The "Entire Device" source behavior is unchanged.
