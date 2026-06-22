# Fix unresolved `R` reference in ExampleRobolectricTest

Implements plan `plans/20260621_011822_fix-robolectric-r-import.md`.

## Change

- `app/src/test/java/com/example/ExampleRobolectricTest.kt`: added
  `import `in`.sreerajp.vault_files.R` so the unqualified `R.string.app_name`
  reference resolves to the generated resource class under the module namespace
  (`in.sreerajp.vault_files`) instead of the non-existent `com.example.R`.

Note: `in` is a Kotlin hard keyword, so the package segment is backtick-escaped in the import.

No production code or resources changed.
