# Fix annotation-default-target warnings (KT-73255)

## Files to be changed
- `app/src/main/java/com/example/ui/FileExplorerScreen.kt` (line 52)
- `app/src/main/java/com/example/ui/StorageAnalyzerScreen.kt` (line 65)

## What the issue is
The Kotlin compiler emits warnings for `@androidx.annotation.StringRes` applied to
constructor `val` parameters:

> This annotation is currently applied to the value parameter only, but in the future
> it will also be applied to field. (KT-73255)

Both sites are `val` parameters in declarations:
- `enum class FileSortMode(@androidx.annotation.StringRes val labelRes: Int)`
- `private data class CategoryData(@androidx.annotation.StringRes val titleRes: Int, ...)`

## The plan for the fix
Pin the annotation to the value parameter only by adding the explicit `@param:` use-site
target, which preserves current behavior and silences the warning without a project-wide
compiler-arg change:
- `@androidx.annotation.StringRes` → `@param:androidx.annotation.StringRes` at both sites.

No behavior change; `@StringRes` is a tooling/lint annotation only.
