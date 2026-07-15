---
name: commit-changes
description: >-
  Commit project changes with a small single-line message describing the change.
  Use when the user asks to commit, create a commit, or after validated changes
  are ready to be recorded in git.
---

# Commit changes

## Preconditions

1. Run **validate-changes** first (integration tests must pass) unless the user explicitly skips validation.
2. Only commit when the user asked to commit (or approved committing after validation).

## Steps

1. Inspect status: `git status`, `git diff`, `git diff --staged`, and recent `git log` style.
2. Stage only intended project files. Do **not** stage:
   - secrets (`.env`, tokens, credentials)
   - implementation plans from the `development` skill (e.g. under `.development-plans/`)
   - IDE junk already covered by `.gitignore`
3. Draft a **small, single-line** commit message that describes the change (aligns with `AGENTS.md`).
4. Commit using a HEREDOC with that one-line message (no multi-line body unless the user asks).
5. Verify with `git status`.

## Message style

- One line only
- Concise; focus on why/what changed
- Examples: `Add menu item stock decrement on order placement.` / `Fix concurrent partner assignment race.`
