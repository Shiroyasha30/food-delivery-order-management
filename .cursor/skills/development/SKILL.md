---
name: development
description: >-
  Feature development workflow: write an implementation plan and get user review
  before coding; implement only after approval; keep the plan out of git. Use
  when starting a feature, implementing a story, or when the user asks to develop
  or build something in this project.
---

# Development

## Gate: plan before code

**Before starting development of a feature, write up a plan for implementation and get it reviewed by user. Only when user approves, start implementing the changes.**

Do not write production code, scaffolding for the feature, or tests for that feature until approval.

## Steps

1. **Clarify scope** — feature goal, affected domains (orders, stock, partners, ratings, etc.), and out-of-scope items.
2. **Write the plan** — short, concrete, reviewable. Prefer a file under `.development-plans/` (gitignored), e.g. `.development-plans/<feature-slug>.md`. Also present the plan in chat for review.
3. **Stop and wait** — ask the user to approve, request changes, or reject. Do not proceed on silence; wait for explicit approval.
4. **On approval** — implement per the plan (and project `AGENTS.md` constraints). Prefer small, focused steps.
5. After implementation, run **validate-changes** before any commit.
6. Commit only if the user asks — via **commit-changes**.

## After done: keep the plan out of git

**Make sure to not add this plan to git.**

- Leave plan files untracked / under `.development-plans/`
- Never `git add` plan files
- If a plan was staged by mistake, unstage/remove it before commit

## Plan template

```markdown
# Plan: <feature name>

## Goal
## Approach
## Files / layers touched
## Concurrency / consistency notes (if any)
## Tests to add or extend
## Out of scope
## Open questions
```
