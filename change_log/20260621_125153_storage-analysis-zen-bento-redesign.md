# Change Log — Storage Analysis "Zen Bento" Redesign

Implements plan `plans/20260621_125153_storage-analysis-zen-bento-redesign.md`.

## Summary

Rebuilt the Storage Analysis screen to match the "Zen Bento" frame in
`samples/Storage Analysis Redesign.dc.html`: a calm bento dashboard with a radial storage
ring tile beside a dedicated Vault tile, and colour-coded category stat tiles in a 2-column grid.

## Files changed

### `app/src/main/java/com/example/ui/StorageAnalyzerScreen.kt` (rewrite)

- Removed the `Scaffold` + `TopAppBar`; the header now scrolls as the first `LazyColumn` item
  ("Storage Analysis" title + truthful source subtitle — "Entire device" / "App sandbox").
- Replaced the two `FilterChip`s with design-styled `SourcePill`s (selected = accent tint + check
  icon; unselected = outlined). Same `updateStorageSourceMode` calls and testTags
  (`storage_source_card`, `select_sandbox_chip`, `select_device_chip`).
- Added **bento top row**:
  - `StorageRingTile` — `Canvas`-drawn radial ring (track + gradient arc, round cap, sweep
    animated via `animateFloatAsState`), centre shows used size + "NN% used", below "X free of Y".
  - `VaultTile` — purple gradient card, shield icon, vault file count (`vaultFiles.size`),
    encrypted size (sum of `fileSize`), and an "Open vault →" row invoking the new `onOpenVault`
    callback. Row uses `IntrinsicSize.Min` so the vault tile matches the ring tile height.
  - Wrapped in a Row carrying testTag `storage_overview_card`.
- Added "Storage breakdown" heading with a non-interactive "See all" caption.
- Replaced `CategoryMetricsCard` with `CategoryStatTile`, rendered as a **2-column grid** (categories
  chunked into pairs). Each tile: tinted/bordered background, colour icon square, % pill
  (share of total device storage), value with small unit, label, and a thin 4dp bar whose fill =
  category bytes / largest-category bytes (animated). All six categories kept (Images, Videos,
  Audio Tracks, Documents, Archives, Other Formats) with their original testTags
  (`images_metric_row`, `videos_metric_row`, `audio_metric_row`, `documents_metric_row`,
  `archives_metric_row`, `others_metric_row`).
- Permission-prompt card and loading spinner kept functionally unchanged (testTags
  `permission_request_card`, `grant_permission_btn` preserved).
- Added a light/dark-aware design palette (category colours, ring gradient, vault gradient, accent)
  and a `categorySizeParts` helper for the "NN.NN  MB" split. `formatBytesToGBorMB` retained.
- New signature: `StorageAnalyzerScreen(viewModel, modifier, onOpenVault: () -> Unit = {})`.

### `app/src/main/java/com/example/MainActivity.kt`

- Tab `0` now passes `onOpenVault = { activeTabIndex = 2 }` so the Vault tile switches to the
  Vault tab.

## Notes

- No data-layer / ViewModel changes — `StorageStats` and `viewModel.vaultFiles` already provided
  everything needed.
- No new dependencies; ring drawn with pure Compose `Canvas` + `Brush.linearGradient`.
- Verified with `./gradlew :app:compileDebugKotlin` → BUILD SUCCESSFUL.
