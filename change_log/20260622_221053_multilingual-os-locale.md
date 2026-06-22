# Change log: Multi-lingual support (follows Android OS language) — English + Malayalam

Implements plan: `plans/20260622_213501_multilingual-os-locale.md`.

## Summary

Externalized **every** user-facing string in the app into Android string resources and added a
Malayalam translation. The app now follows the **device/OS language** automatically (Android
resolves `values-ml/` when the system locale is Malayalam, `values/` otherwise). No runtime
locale-switching code and no in-app language picker were added, as requested.

Build verified with `./gradlew :app:compileDebugKotlin` → **BUILD SUCCESSFUL** (resources merged,
`R` generated, all Kotlin compiled). Full `assembleDebug` was not run because it requires a
`debug.keystore` that isn't present (pre-existing, unrelated to this change).

## Files changed

### Resources
- **`app/src/main/res/values/strings.xml`** — added ~180 English strings (grouped by screen) plus
  two `<plurals>` (`folder_count`, `item_count`). `app_name` retained.
- **`app/src/main/res/values-ml/strings.xml`** — *new*. Malayalam translation of the full key set
  (1:1 with the default file), including both plurals. **Pending user review.**

### Kotlin (literals → resource lookups)
- **`MainActivity.kt`** — nav bar labels/content descriptions, `AppLockScreen` text. Biometric
  prompt titles/subtitles (non-composable lambdas) use `context.getString(...)`.
- **`ui/AboutScreen.kt`** — header, version (`about_version` with `%1$s`), build-info row labels,
  "Made with ❤ from India".
- **`ui/StorageAnalyzerScreen.kt`** — titles, source pills, permission card, ring/vault tiles,
  breakdown. `CategoryData.title: String` → `@StringRes titleRes: Int` (the category list is built
  in `LazyListScope`, which is not a composable scope, so the label is resolved with
  `stringResource` inside `CategoryStatTile`).
- **`ui/FileExplorerScreen.kt`** — `FileSortMode(label)` → `FileSortMode(@StringRes labelRes)`;
  helper functions `categoryDisplayLabel()` and `getDisplayPath()` made `@Composable` so they can
  call `stringResource`. All header/search/source/banner/empty/permission/dialog/menu/details/auth
  strings externalized. Item/folder counts use `pluralStringResource`. Parameterized prompts &
  messages use `context.getString(res, args)`.
- **`ui/SettingsScreen.kt`** — hub/display/security pages, theme menu, help entries, all toggle
  tiles, shielded-folders ledger, PIN setup dialog, verification dialogs, back button.
- **`ui/SecureVaultScreen.kt`** — top bar, empty/locked states, disclaimer, unlock button, PIN
  fallback dialog, row action content descriptions.
- **`ui/StorageViewModel.kt`** — added a private `string(@StringRes, vararg)` helper backed by the
  application context; all `dispatchMessage(...)` literals replaced with localized strings.
  Boolean-driven phrasing ("enabled/disabled", "listed/hidden", device/sandbox) split into separate
  resources; theme-change message selects one of three resources instead of concatenating.

## Notes / intentional non-changes
- **Internal canonical keys are NOT translated**: storage mode (`"sandbox"`/`"device"`), theme keys
  (`"system"`/`"light"`/`"dark"`), category keys (`"Image"`, `"Video"`, …), and demo/seed file
  names in `StorageRepository`. Only displayed labels are localized; canonical keys map to a
  resource at display time.
- A few dynamic content descriptions bound to `item.category` remain as the canonical English
  category string (they are not visible labels). The file-details "Type" value also shows the raw
  category for non-folders, matching prior behavior.
- Number/byte/percent formatting helpers (`formatBytes`, `formatBytesToGBorMB`, `"%.2f KB"`, the
  `"0%"`/`"<1%"` symbols) were left in code — units/symbols are locale-neutral.
- `build.gradle.kts` was left unchanged; `generateLocaleConfig` was optional in the plan and not
  required for OS-locale following.

## Known warnings
- Two KT-73255 forward-compat warnings for `@StringRes` on the enum/data-class parameters
  (`FileSortMode.labelRes`, `CategoryData.titleRes`). Harmless; can be silenced later with a
  `@param:` target if desired.

## Follow-up
- User to review the Malayalam translations in `values-ml/strings.xml`.
- To see it live: set the device system language to Malayalam (or use per-app language on
  Android 13+), or temporarily run with a Malayalam locale.
