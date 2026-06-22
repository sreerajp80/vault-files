# Plan: Settings sectioned redesign (Display / Security / About sub‑pages)

## Issue / goal

The current Settings tab (`SettingsScreen.kt`) is one long scrolling `LazyColumn` that
stacks every preference (theme, hidden files, PIN, three app‑lock toggles, shielded‑folder
ledger, help card, about row) on a single page. The new design
(`samples/Settings Redesign.dc.html`) replaces this with a **sectioned hub**:

- A **Settings hub** with a large inline title, a `PREFERENCES` section containing two
  navigation tiles (Display & Themes, Security Protection), a `HELP & INFORMATION` section
  with the Q&A card, and an **About Vault Files** navigation tile.
- Tapping a tile opens a **sub‑page** (Display & Themes / Security Protection / About) with
  a rounded‑square back button + inline title. Back returns to the hub.

Specific requirements from the request:
1. Faithfully implement the new sectioned design.
2. Light and dark mode both correct (the mock defines explicit palettes for each).
3. **Phone top bar honoured** — content must start below the status bar, and status‑bar
   icons must keep proper contrast (dark icons in light mode, light icons in dark mode),
   including when the theme is *forced* opposite to the system setting.
4. **Borders clearly visible / identifiable** for every tile and pill in both modes — the
   mock's borders (`rgba(255,255,255,.06)` in dark, `#E9E5F1` in light) are too faint, so we
   use stronger, theme‑aware border colors.

## Files to be changed

- `app/src/main/java/com/example/ui/SettingsScreen.kt` — rewrite into hub + sub‑pages.
- `app/src/main/java/com/example/ui/AboutScreen.kt` — restyle as a sub‑page matching the
  new header/tile aesthetic (inline back‑row header instead of `TopAppBar`, status‑bar
  inset, new card style). Keeps its existing `onBack` contract.
- `app/src/main/java/com/example/ui/theme/Color.kt` — add dedicated, clearly‑visible tile/
  pill border colors for light and dark (so we don't weaken the very‑faint mock borders).
- `app/src/main/java/com/example/MainActivity.kt` — drive status‑bar icon contrast from the
  app's effective `darkTheme` value so the top bar reads correctly even when the theme is
  forced (this is a small global fix that also benefits the other tabs).

No ViewModel/Repository/DB changes — all existing state, actions, dialogs, biometric/PIN
verification flows, and `testTag`s are preserved exactly; only the layout/visual shell
around them changes.

## Plan for the fix

### 1. `SettingsScreen.kt` — hub + sub‑page navigation

- Introduce local navigation state: `var settingsPage by rememberSaveable { mutableStateOf("settings") }`
  with values `settings` | `display` | `security` | `about`. This replaces the current
  `showAbout` boolean (the `about` page reuses `AboutScreen(onBack = { settingsPage = "settings" })`).
- Replace the `Scaffold` + `TopAppBar` shell with a root `Box`/`LazyColumn` painted with
  `MaterialTheme.colorScheme.background`, applying the **status‑bar top inset** to the
  content padding (same pattern already used by `StorageAnalyzerScreen`:
  `WindowInsets.statusBars.asPaddingValues().calculateTopPadding()`), so the header sits
  below the clock/battery — honouring the top bar without a Material app bar.
- Extract reusable composables inside the file to avoid duplication:
  - `SettingsSectionLabel(text)` — the uppercase tracked section caption.
  - `SettingsTile(...)` — the rounded 18.dp card with icon‑chip + title + subtitle and an
    optional trailing slot (chevron / Switch / Button), with a **visible border**.
  - `SettingsSubPageHeader(title, onBack)` — rounded‑square back button + title row, used by
    Display, Security and About sub‑pages.
- **Hub page** (`settings`): large "Settings" title + subtitle; `PREFERENCES` →
  Display & Themes tile (`onClick` → `display`) and Security Protection tile
  (`onClick` → `security`); `HELP & INFORMATION` → existing two‑Q&A card (compression /
  folder securing) restyled; About Vault Files tile (`onClick` → `about`, keeps
  `testTag("about_row")`).
- **Display sub‑page** (`display`): header + Application Theme tile (keeps the
  `theme_selector_btn` Change button and the `theme_opt_system/light/dark` dropdown items
  and `updateThemePreference`) + Show Hidden Files toggle tile (keeps `show_hidden_switch`
  and the existing hidden‑items biometric/PIN gating logic).
- **Security sub‑page** (`security`): header + App Passcode tile (keeps `setup_pin_btn`,
  Setup/Modify label, red "None configured" subtitle) + the three toggle tiles
  (`app_lock_switch`, `password_hidden_switch`, `delete_move_lock_switch`) with their exact
  existing enable/disable + PIN‑setup + `activeActionPendingValidation` logic + the
  `SHIELDED FOLDERS LEDGER` section (dashed empty‑state tile, or the existing protected‑
  folder list with the `LockOpen` remove action).
- **About sub‑page** (`about`): delegate to the restyled `AboutScreen`.
- Keep the PIN‑setup `AlertDialog` and the `activeActionPendingValidation` biometric/PIN
  verification dialogs unchanged (and reachable from the hub regardless of sub‑page, e.g.
  by hoisting that block at the root of the composable).

### 2. Borders that are clearly identifiable (both modes)

- Add to `Color.kt`:
  - `TileBorderLight = Color(0xFFD7D1E6)` and `TileBorderDark = Color(0xFF3A3A46)` —
    both clearly visible against the respective tile + page backgrounds (stronger than the
    mock's near‑invisible borders, per the request).
- In `SettingsScreen`/`AboutScreen`, choose the border by the *effective* mode using the
  color scheme's luminance (`MaterialTheme.colorScheme.background.luminance() < 0.5f`) so it
  is correct even when the theme is forced. Apply a `1.dp` `BorderStroke` to **every** tile,
  the help card, the back button, the dashed ledger pill, and the protected‑folder rows.
  (Filled primary "Change/Setup" pills stay high‑contrast filled buttons — already clearly
  identifiable — so they keep the Material `Button`.)

### 3. Honour the phone top bar (status bar)

- `MainActivity.kt`: after computing `darkTheme`, set the status‑bar icon appearance to
  match via a `SideEffect` using
  `WindowCompat.getInsetsController(window, window.decorView).isAppearanceLightStatusBars = !darkTheme`.
  This fixes the mock's call‑out ("status bar now keeps proper contrast in light mode") for
  the case where the user forces Light while the system is Dark (and vice‑versa). It is a
  global, low‑risk improvement affecting all tabs.
- Content top‑inset is handled per‑screen as described in step 1 (Settings) and already
  exists for the other tabs.

### 4. Verification

- Build: `./gradlew assembleDebug`.
- Manually (or via screenshots) confirm in **both** light and dark:
  - Hub shows Preferences (2 tiles) + Help card + About tile; tiles have clearly visible
    borders; header sits below the status bar.
  - Tapping each tile opens its sub‑page; back returns to the hub.
  - Theme change, all four toggles, PIN setup/modify, and the biometric/PIN verification
    dialogs still work (existing `testTag`s intact).
  - Status‑bar icons are dark on the light theme and light on the dark theme, including when
    the theme is forced opposite to the system.
- Existing instrumented/unit tests that rely on `testTag`s should remain green since all tags
  are preserved.

## Risk / notes

- Pure UI refactor of the Settings shell; no data‑layer or behavior changes.
- `AboutScreen` loses its Material `TopAppBar` in favor of the inline back‑row header to
  match the design; its `onBack`/`about_screen`/`about_back_btn` tags are preserved.
- The status‑bar `SideEffect` is the only change outside the Settings/About/theme files and
  is intentionally global.
