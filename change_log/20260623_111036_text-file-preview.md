# Change log: In-app text file preview with an on/off gating setting

Implements [plans/20260623_092450_text-file-preview.md](../plans/20260623_092450_text-file-preview.md).

## Summary
Added a full-screen, read-only in-app text viewer. Tapping a detected text file opens its
contents over a dark scrim in a scrollable monospace view; a new **Text Preview** setting
(Display & Themes) gates the feature as a pure on/off toggle, mirroring the existing **Image
Preview** toggle. When the setting is off, text files keep the generic "Viewing…" toast.
"All types of text files" is implemented as a generous extension allowlist; files outside it
keep the toast (no "attempt any file" mode). No new dependencies were added.

## Files changed
- **app/src/main/java/com/example/data/StorageRepository.kt**
  - Added `TEXT_PREVIEW_EXTENSIONS` (generous allowlist: plain text, markup, config/data, logs,
    common source code), `TEXT_PREVIEW_MAX_BYTES` (512 KB cap), and a `TextPreviewContent`
    data class (`text`, `truncated`).
  - Added `isLikelyTextFile(file)` (cheap extension check for tap routing) and
    `readTextFilePreview(file)` (suspend, `Dispatchers.IO`): reads up to the cap, decodes UTF-8,
    flags truncation, and returns `null` when the read fails or a NUL byte indicates binary.
  - Added settings helpers `getTextPreviewEnabledFlow()`, `isTextPreviewEnabled()` (default
    `true`), `saveTextPreviewEnabled(enabled)` (key `text_preview_enabled`).
- **app/src/main/java/com/example/ui/StorageViewModel.kt**
  - Added a `TextPreviewUi` sealed interface (`Loading` / `Ready(text, truncated)` / `Failed`).
  - Added `textPreviewEnabled` StateFlow (default `true`, loaded in `init`) and
    `updateTextPreviewEnabled(enabled)` (persists + emits a toast).
  - Added `textPreview` StateFlow with `openTextPreview(item)` (sets `Loading`, reads on a
    coroutine, then `Ready`/`Failed`; ignores stale results if the user changed previews) and
    `closeTextPreview()`, plus `isTextPreviewable(item)` passthrough to the repository.
- **app/src/main/java/com/example/ui/FileExplorerScreen.kt**
  - Collected `textPreviewEnabled` and `textPreview`; added `horizontalScroll` and `FontFamily`
    imports.
  - In `onItemClick`, route taps on `isTextPreviewable` files to `openTextPreview` when the
    setting is on (after the image branch; otherwise the old "Viewing…" toast).
  - Added a private `TextPreviewDialog` composable: full-screen dark `Dialog` with a top bar
    (filename + close, reusing `cd_close_preview`), a loading spinner, a `Description`-icon
    error state, an optional truncation banner, and a `SelectionContainer` wrapping a
    vertically/horizontally scrollable monospace `Text`.
- **app/src/main/java/com/example/ui/SettingsScreen.kt** — collected `textPreviewEnabled`; added
  a "Text Preview" `SettingsTile` + `Switch` in the Display page (after Image Preview), wired to
  `updateTextPreviewEnabled` (testTag `text_preview_switch`).
- **app/src/main/res/values/strings.xml** and **values-ml/strings.xml** — added
  `text_preview_title`, `text_preview_subtitle`, `text_preview_error`, `text_preview_truncated`,
  `msg_text_preview_enabled`, `msg_text_preview_disabled` (English + Malayalam).

## Verification
- `./gradlew assembleDebug` → **BUILD SUCCESSFUL**.
- Not yet manually exercised on a device/emulator.

## Notes
- Local `File` read only — no network or new manifest permissions; UTF-8, read-only (no editing,
  no encoding picker, no syntax highlighting).
- `getCategoryForFile` was intentionally left unchanged (txt/csv/json stay `Document` for Storage
  Analysis); preview detection uses a separate, broader allowlist.
- Reads are capped at 512 KB with a truncation banner; binary content (NUL-byte sniff) shows a
  graceful "can't display this file as text" state.
