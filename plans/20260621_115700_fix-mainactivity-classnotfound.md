# Fix: MainActivity ClassNotFoundException crash on launch

## Issue

The app crashes immediately on launch with:

```
java.lang.ClassNotFoundException: Didn't find class
"in.sreerajp.vault_files.MainActivity" ...
```

### Root cause

- `AndroidManifest.xml` declares the launcher activity as `android:name=".MainActivity"`.
- The leading `.` is shorthand resolved **relative to the manifest package**, which AGP
  derives from the module `namespace` = `in.sreerajp.vault_files`.
- So the system looks for `in.sreerajp.vault_files.MainActivity`.
- But the actual class is `com.example.MainActivity` (source package is still `com.example`,
  as documented in CLAUDE.md's "Namespace caveat").
- Result: class not found → activity fails to instantiate → fatal crash on every launch.

## Files to change

- `app/src/main/AndroidManifest.xml` — the activity declaration.

## Plan for the fix

Change the activity's `android:name` from the relative `.MainActivity` to the
fully-qualified class name:

```xml
<activity
    android:name="com.example.MainActivity"
    ... >
```

This points the manifest at the real class location without touching any Kotlin code or
the `namespace`/`applicationId` (which remain `in.sreerajp.vault_files`).

### Why this approach (not renaming the package)

Renaming `com.example` → `in.sreerajp.vault_files` across all sources would also fix it,
but it is a large, risky refactor (every file's `package`/`import`, Room, tests). The
one-line manifest change is the minimal, correct fix and aligns with the existing
documented split between source package and namespace.

## Verification

- `./gradlew assembleDebug` builds.
- `./gradlew installDebug` then launch — app opens without the ClassNotFoundException.
