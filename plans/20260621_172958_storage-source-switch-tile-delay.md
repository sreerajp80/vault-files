# Plan: Fix delayed tile population when switching App Sandbox ⇄ Entire Device

## Issue

When the user taps the **App Sandbox** / **Entire Device** source pills on the
Storage Analysis screen, the category/storage tiles take a noticeable time to
update, and during that time they keep showing the *previous* source's numbers
before suddenly jumping to the new values.

### Root cause

- `SourcePill.onClick` → `viewModel.updateStorageSourceMode(mode)`
  (`StorageAnalyzerScreen.kt:159` / `:166`).
- `updateStorageSourceMode` (`StorageViewModel.kt:352-361`) launches a coroutine
  and calls `refreshStorageStats()`, which runs
  `repository.getStorageUsageStats(userStorageRoot)` — a **full recursive walk**
  of the storage tree (`StorageRepository.kt:180-204`). For "Entire Device"
  (`Environment.getExternalStorageDirectory()`) this walks every file and is slow.
- `_storageStats` is **not reset** when the switch starts, so it retains the old
  mode's value. The tiles therefore display stale data for the whole duration of
  the scan and only update at the end — this is the perceived "delay".
- The screen already renders a `CircularProgressIndicator` when `stats == null`
  (`StorageAnalyzerScreen.kt:249-260`), but that branch is never reached on a
  switch because stats is never cleared.

## Files to change

1. `app/src/main/java/com/example/ui/StorageViewModel.kt`
   - In `updateStorageSourceMode(...)`: set `_storageStats.value = null`
     immediately after updating `_storageSourceMode` (before/at the start of the
     refresh) so the existing loading spinner shows right away and the stale
     tiles disappear instantly, giving immediate feedback.

## Plan for the fix

- Minimal, targeted change in `updateStorageSourceMode`: clear `_storageStats`
  to `null` as soon as the mode changes, so the UI swaps stale tiles for the
  already-existing loading spinner immediately, then repopulates when the
  recursive scan completes.
- This reuses the existing `stats == null` loading branch — no UI changes
  needed.
- The actual scan time for "Entire Device" is inherent to the recursive walk and
  is left as-is (out of scope for this fix); this change addresses the
  *perceived* delay / stale-tile problem, which is the reported symptom.

### Optional (not included unless requested)

- Caching stats per mode to make switching back instant, or speeding up the
  recursive scan. These add complexity/staleness trade-offs and are deferred.

## Verification

- Build: `./gradlew assembleDebug`.
- Manual: switch between App Sandbox and Entire Device — the spinner should
  appear immediately on tap, then tiles populate with the new source's data
  (no stale numbers shown in between).
