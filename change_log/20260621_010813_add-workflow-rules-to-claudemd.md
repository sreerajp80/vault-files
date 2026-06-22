# Change log: Add mandatory workflow rules to CLAUDE.md

Implements plan `plans/20260621_010813_add-workflow-rules-to-claudemd.md`.

## What changed

- **`CLAUDE.md`** — added a new "Workflow rules (mandatory)" section immediately after the intro
  line and before "Overview". It restates the two project workflow rules verbatim:
  1. Plan before changing (write plan to `plans/`, mandatory approval gate, wait for explicit
     approval before editing any project file).
  2. Log after changing (write change log to `change_log/` referencing the plan).

  Placed first so the workflow gate is the most prominent instruction a future instance sees. The
  rest of the slim structure (Overview, Namespace caveat, Common commands, Architecture in brief,
  Detailed docs) is unchanged.

## Effect

The project `CLAUDE.md` now self-contains the mandatory plan→approve→log workflow, independent of
the user's global `~/.claude/CLAUDE.md`.

## Not changed

No source (`.kt`), Gradle, or manifest files were touched — documentation only.
