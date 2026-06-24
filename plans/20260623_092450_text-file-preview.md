# Plan: In-app text file preview with an on/off gating setting

## What the user asked for
- Add a **text file preview** feature: tapping a text file opens its contents inside the app.
- Support **all types of text files** — not just `.txt` (a generous extension set, not a single ext).
- Add a **setting** that gates the feature. Per the user's choice, this is a **pure on/off
  gate** mirroring the existing **Image Preview** toggle: when ON, detected text files open in
  the viewer; when OFF, text files keep the current behavior (the "Viewing: …" toast). There is
  **no "attempt any file as text" mode** — files not detected as text always keep the toast.

## Current behavior (the gap)
In [FileExplorerScreen.kt](app/src/main/java/com/example/ui/FileExplorerScreen.kt#L638-L648),
`onItemClick` for a plain file routes directories, `.zip`, `.securenote`, and (newly) images,
then ends in an `else` that only dispatches a `msg_viewing_file` toast. There is no viewer for
text. This mirrors exactly the gap that the image-preview feature
([plans/20260623_085025_image-preview.md](plans/20260623_085025_image-preview.md)) filled, and
this plan follows that feature's structure closely so the two stay consistent.

## Design decisions
- **No new dependency.** Unlike image preview (which needed Coil decoders), text preview just
  reads bytes and renders them with a Compose `Text`. Plain `File` read on `Dispatchers.IO`.
- **"All types of text files" = a generous extension allowlist** (single source of truth in the
  repository). Covers plain text, markup, config, data, logs, and common source code:
  `txt, text, log, md, markdown, csv, tsv, json, xml, yaml, yml, ini, cfg, conf, properties,
  toml, env, gradle, kt, kts, java, js, ts, jsx, tsx, py, rb, go, rs, c, h, cpp, hpp, cs, php,
  sh, bash, bat, ps1, sql, html, htm, css, scss, gitignore, gitattributes, lst`. Files outside
  this set keep the toast (consistent with the "no attempt-any-file" choice).
- **Routing is by extension (cheap, synchronous)**; the actual read is async. A VM passthrough
  `isTextPreviewable(item)` (just an extension check) is used in `onItemClick` so the screen
  doesn't reach into the repository directly.
- **Safety caps on read:** read at most ~512 KB and decode as UTF-8. If the file is larger, show
  the first 512 KB and flag it as **truncated** (a small banner). This bounds memory and keeps
  the UI responsive for huge logs.
- **Binary guard:** even an allowlisted extension can contain binary. The read sniffs for NUL
  bytes (and a high ratio of non-text bytes) in the first chunk; if it looks binary or the read
  fails, the viewer shows a clean **"Can't display this file as text"** state instead of garbage.
- **Setting semantics & default:** new preference `text_preview_enabled`, **default `true`**
  (matches Image Preview's default of `true`; feature works out of the box, user can turn it
  off). Lives in the **Display & Themes** settings sub-page, directly after the existing
  **Image Preview** tile.
- **Viewer UI:** a full-screen `Dialog` with a dark scrim (mirroring `ImagePreviewDialog`): a top
  bar with the filename + a close button (reusing `cd_close_preview`), and a vertically- and
  horizontally-scrollable **monospace** `Text` body. `Loading` → spinner; `Failed` → broken-file
  icon + message; truncated → a one-line banner. Dismiss via close button, scrim, or system back.
- **Trigger order in `onItemClick`:** dir → `.zip` → `.securenote` → image (if enabled) → **text
  (if `text_preview_enabled` and `isTextPreviewable`)** → existing toast. Image keeps priority, so
  no overlap (no extension is in both sets).

## Files to change
1. **app/src/main/java/com/example/data/StorageRepository.kt**
   - Add a `TEXT_PREVIEW_EXTENSIONS` set and `isLikelyTextFile(file): Boolean`.
   - Add `readTextFilePreview(file): TextPreviewContent?` (suspend, `Dispatchers.IO`): reads up
     to ~512 KB, decodes UTF-8, returns `TextPreviewContent(text, truncated)`; returns `null`
     when the content looks binary or the read fails. (`TextPreviewContent` is a small data class.)
   - Add settings helpers mirroring the image ones: `getTextPreviewEnabledFlow()`,
     `isTextPreviewEnabled()` (default `true`), `saveTextPreviewEnabled(enabled)`
     (key `text_preview_enabled`).
2. **app/src/main/java/com/example/ui/StorageViewModel.kt**
   - Add `textPreviewEnabled` StateFlow (default `true`, loaded in `init`) and
     `updateTextPreviewEnabled(enabled)` (persists + emits a toast).
   - Define a `TextPreviewUi` sealed interface: `Loading(item)`, `Ready(item, text, truncated)`,
     `Failed(item)`; add `textPreview: StateFlow<TextPreviewUi?>` with `openTextPreview(item)`
     (sets `Loading`, launches the read, then `Ready`/`Failed`) and `closeTextPreview()`.
   - Add `isTextPreviewable(item): Boolean` passthrough to `repository.isLikelyTextFile(item.file)`.
3. **app/src/main/java/com/example/ui/FileExplorerScreen.kt**
   - Collect `textPreviewEnabled` and `textPreview`.
   - In `onItemClick`, add the text branch after the image branch:
     `else if (textPreviewEnabled && viewModel.isTextPreviewable(item)) viewModel.openTextPreview(item)`.
   - Render a `TextPreviewDialog` when `textPreview != null` (next to the existing
     `imagePreview?.let { ImagePreviewDialog(...) }`).
   - Add a private `TextPreviewDialog` composable (full-screen dark `Dialog`, scrollable
     monospace text, loading/error/truncated states, top bar with filename + close).
4. **app/src/main/java/com/example/ui/SettingsScreen.kt**
   - Collect `textPreviewEnabled`; add a "Text Preview" `SettingsTile` + `Switch` in the
     `"display"` page right after the Image Preview tile, wired to
     `viewModel.updateTextPreviewEnabled` (icon `Icons.AutoMirrored.Filled.Article` or
     `Icons.Default.Description`; testTag `text_preview_switch`).
5. **app/src/main/res/values/strings.xml**
   - Add: `text_preview_title`, `text_preview_subtitle`, `text_preview_error`,
     `text_preview_truncated`, `msg_text_preview_enabled`, `msg_text_preview_disabled`.
     (Reuse the existing `cd_close_preview` for the close button.)
6. **app/src/main/res/values-ml/strings.xml**
   - Add matching Malayalam translations so the two resource files stay in parity.

## Notes / non-goals
- Local `File` read only — no network, no new manifest permissions (device source already
  requires All-Files-Access, which covers reading bytes).
- UTF-8 only in this pass; no encoding picker, no syntax highlighting, no editing (read-only).
- No `getCategoryForFile` change (txt/csv/json stay categorized as `Document` for Storage
  Analysis); detection for previewing is a separate, broader allowlist.
- No new tests added unless requested; existing Robolectric/screenshot tests are unaffected.

## Verification
- `./gradlew assembleDebug` to confirm it compiles (no new deps to resolve).
- Manual: with the setting ON, tap a `.txt`/`.md`/`.json`/`.kt` → viewer opens with contents;
  tap a large file → truncation banner; tap a renamed-binary `.txt` → graceful error. Turn the
  setting OFF → tapping a text file shows the old "Viewing…" toast.
