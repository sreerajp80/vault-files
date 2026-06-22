# Plan: Add mandatory workflow rules to CLAUDE.md

## Issue

The mandatory plan→approve→log workflow rules currently live only in the user's global
`~/.claude/CLAUDE.md`. The project `CLAUDE.md` does not restate them, so a future instance reading
only the project file would not see the requirement to write a plan in `plans/`, wait for explicit
approval before editing, and write a change log in `change_log/` afterward. The `plans/` and
`change_log/` folders already exist in this repo and are in active use.

## Files to be changed

- **`CLAUDE.md`** — add a "Workflow rules (mandatory)" section.

No other files touched.

## Plan for the fix

Add a new top section (right after the intro line, before "Overview") titled
**"Workflow rules (mandatory)"** containing the two rules the user provided, kept faithful to the
source wording:

1. **Plan before changing** — write a full plan to `plans/` named
   `yyyymmdd_hhMMss_<short-slug>.md` listing files to change, the issue, and the fix plan; then
   STOP at a mandatory approval gate (explicitly ask for approval, wait, proceed only on explicit
   affirmative approval; re-approve if the plan changes; skip only if the user explicitly says so
   for that change).
2. **Log after changing** — write a change log to `change_log/` named
   `yyyymmdd_hhMMss_<short-slug>.md` describing changes and referencing the plan.

Placement rationale: putting it first makes the workflow gate the most prominent instruction a
future instance sees. Keep the existing slim structure (Overview, Namespace caveat, Commands,
Architecture in brief, Detailed docs) intact below it.

## Net effect

Project `CLAUDE.md` self-contains the mandatory workflow, independent of the global file.
