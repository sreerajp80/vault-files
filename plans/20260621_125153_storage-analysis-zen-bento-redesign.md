# Storage Analysis — "Zen Bento" Redesign

## Goal

Reimplement the Storage Analysis screen to match the design in
`samples/Storage Analysis Redesign.dc.html` (the "Zen Bento" frame). The design is a calm
bento dashboard:

- Scrolling header: "Storage Analysis" title + source subtitle + a small options icon button.
- Two source pills: "App Sandbox" and "Entire Device".
- A top bento row: a **radial storage ring tile** (left) beside a **purple gradient Vault tile** (right).
- A "Storage breakdown" section heading with a "See all" affordance.
- A **2-column grid of colour-coded category stat tiles** (icon square + % pill + value + label + thin bar).

The HTML's status bar, home indicator and bottom nav are phone-mockup chrome only — the app
already renders the real bottom `NavigationBar` in `MainActivity`, so they are **not** implemented.

## Files to change

1. **`app/src/main/java/com/example/ui/StorageAnalyzerScreen.kt`** — main rewrite of the screen
   body and category tile; add ring tile, vault tile, source pill, and grid helpers.
2. **`app/src/main/java/com/example/MainActivity.kt`** — pass an `onOpenVault` callback into
   `StorageAnalyzerScreen` so the Vault tile's "Open vault" can switch to the Vault tab
   (`activeTabIndex = 2`).

No data-layer / ViewModel changes: `StorageStats` already provides the byte breakdown, and
`viewModel.vaultFiles` (a `StateFlow<List<VaultFile>>`, each with `fileSize`) gives the vault
count and encrypted size for the Vault tile.

## What the current screen does (to preserve)

- Source selector (sandbox / device) via `updateStorageSourceMode`, with testTags
  `storage_source_card`, `select_sandbox_chip`, `select_device_chip`.
- "All Files Access" permission prompt card (device mode, no permission) — testTags
  `permission_request_card`, `grant_permission_btn`. **Kept as-is, functionally unchanged.**
- Loading spinner while `stats == null`.
- Overview card testTag `storage_overview_card` and the per-category row testTags
  (`images_metric_row`, `videos_metric_row`, `audio_metric_row`, `documents_metric_row`,
  `archives_metric_row`, `others_metric_row`). **All testTags preserved** so existing/expected
  tests keep matching.

## Design decisions (please confirm in approval)

- **Category count:** the mock shows a 2×2 grid of 4 (Images, Videos, Audio, Documents). The app
  actually has **6** categories (adds Archives, Other Formats). To avoid dropping real data I will
  keep all **6**, rendered in the same tile style as a 2-column grid (3 rows). Archives = purple
  `#9B59B6`, Other = grey `#95A5A6`.
- **Header subtitle:** the mock reads "Entire device · scanned 2m ago". We do not track a scan
  timestamp, so I will show the truthful source label only — "Entire device" or "App sandbox" —
  and not fabricate a relative time.
- **Options icon (top-right) and "See all":** these are decorative in the mock with no target
  screen. To avoid dead controls I will **omit the top-right options icon** and render
  "See all" as a plain non-interactive caption (kept for visual balance, no onClick). If you'd
  rather I wire them to something, say so.
- **% pill vs bar:** pill = category share of total device storage (`bytes / totalLimitBytes`),
  rounded; bar fill = category size relative to the largest category (so the biggest reads full),
  matching the mock's proportions.

## Implementation plan

### StorageAnalyzerScreen.kt

- Drop the `Scaffold` + `TopAppBar`; the header becomes the first `LazyColumn` item so it scrolls
  with content, as in the design. Background = `MaterialTheme.colorScheme.background`.
- Add a private design palette (light/dark aware via `isSystemInDarkTheme()`): category colours
  (blue `#5B8DEF`, orange `#F2994A`, teal `#2FB39A`, gold `#E0A93B`, purple, grey), the ring
  gradient (`#5B8DEF → #7C6FF0 → #2FB39A`) and the vault gradient (`#7C6FF0 → #6353D8`).
- **Header item:** `Column` with title (`Storage Analysis`, ~23sp bold) + subtitle (source label).
- **Source pills:** replace the two `FilterChip`s with two design-styled pill `Row`s (selected =
  filled tint + check icon; unselected = outlined). Same `onClick` + testTags as today.
- **Permission card / loading:** unchanged logic, kept.
- **Bento top row** (`Row`, `spacedBy(12.dp)`):
  - `StorageRingTile` (weight 1.25): white/surface card, a `Canvas`-drawn ring — background
    circle stroke + foreground arc using `Brush.linearGradient`, round stroke caps, sweep
    animated via `animateFloatAsState` on used/total. Centre shows used size + "NN% used"; below,
    "X free of Y" from `totalLimitBytes - usedBytes`.
  - `VaultTile` (weight 1): gradient card, shield icon chip, big count = `vaultFiles.size`,
    "files in Vault", "<encrypted size> encrypted" = sum of `fileSize`, and an "Open vault →" row
    that calls `onOpenVault`.
- **Breakdown heading row:** "Storage breakdown" + muted "See all" caption.
- **Category grid:** chunk the 6 categories into pairs; for each pair emit a `Row` of two
  `CategoryStatTile`s (`spacedBy(12.dp)`, `weight(1f)` each). Replace `CategoryMetricsCard` with
  `CategoryStatTile`: tinted rounded background + border, top row = colour icon square + % pill,
  then value (`NN.NN MB` with small "MB" unit) + label, then a thin (4dp) rounded bar whose fill
  width = `bytes / maxCategoryBytes`. Keep each tile's testTag.
- Keep `formatBytesToGBorMB`; add a small helper for the "free of total" line if needed.

### MainActivity.kt

- Change the `StorageAnalyzerScreen(...)` call (case `0`) to pass `onOpenVault = { activeTabIndex = 2 }`.

## Risk / notes

- Ring gradient on an arc: use `drawArc(brush = Brush.linearGradient(...))` with `Stroke`
  (`cap = StrokeCap.Round`). Pure Compose, no new deps.
- Grid via chunked Rows (not `LazyVerticalGrid`) keeps it inside the existing `LazyColumn`
  without nested-scroll issues.
- No new libraries, no DB/VM changes; build remains `assembleDebug`.
