# Fix: selected source pill not identifiable in light mode

## Files to be changed
- `app/src/main/java/com/example/ui/StorageAnalyzerScreen.kt`

## The issue
On the Storage Analysis screen, the **selected** `SourcePill` (e.g. "Entire Device") is
hard to identify as selected in light mode. It renders only as an `accent` tint at 16%
alpha with **no border**, so on the white background it looks faint/disabled — less
prominent than the unselected pill (which is a white box with an outline border).

In dark mode the same pill is easily identifiable (filled tint with clear contrast +
check + accent text), which the user confirmed is the desired reference.

Current code (`SourcePill`, ~line 350):
```
val containerModifier = if (selected) {
    Modifier.background(accent.copy(alpha = 0.16f))   // no border, faint in light mode
} else {
    Modifier
        .background(MaterialTheme.colorScheme.surfaceVariant)
        .border(1.5.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(16.dp))
}
```

## The plan for the fix
Make the **selected** pill clearly identifiable in light mode while leaving dark mode
exactly as-is (it is the reference and already looks correct):

- Branch the selected style on `isSystemInDarkTheme()`:
  - **dark (unchanged):** `Modifier.background(accent.copy(alpha = 0.16f))`.
  - **light:** stronger accent tint plus an accent border so it reads clearly as selected:
    - background `accent.copy(alpha = 0.18f)`
    - `border(1.5.dp, accent.copy(alpha = 0.55f), RoundedCornerShape(16.dp))`
- The unselected branch and the accent content color (icon/text) are unchanged.

This is a single localized change to the `containerModifier` computation in `SourcePill`.

## Scope notes
- Dark mode selected pill: untouched.
- Unselected pill: untouched (already uses the now-darkened `outline`).
- `StorageRingTile` / `VaultTile`: untouched.
