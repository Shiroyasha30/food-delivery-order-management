---
name: validate-changes
description: >-
  Validate code changes by running integration tests before committing. On
  failure, diagnose whether production logic or tests are incorrect/incomplete,
  ask the user to verify findings, then implement fixes. Use when preparing to
  commit, after implementing features, or when the user asks to validate changes.
---

# Validate changes

Run this **before every commit** after code changes. Do not commit until validation passes (or the user explicitly waives it).

## Steps

1. **Discover how to run integration tests**
   - Prefer Maven: `mvn -q verify` (Failsafe IT + unit tests).
   - If Failsafe is not configured yet, fall back to `mvn -q test`.
   - Prefer keeping `OrderLifecycleFlowIT` updated when flows change; `DummyIT` remains a context-load smoke test.

2. **Run the suite** and capture full failure output.

3. **If all tests pass** — report success. Proceed to commit only if the user asked to commit (then follow `commit-changes`).

4. **If any test fails** — analyze before fixing:
   - Is **application/logic** incorrect or incomplete?
   - Is the **test** incorrect, outdated, or incomplete?
   - Summarize findings for the user (failed test names, likely root cause, recommended fix side: code / test / both).

5. **Ask the user to verify the findings** — wait for confirmation or correction.

6. **Only after the user verifies**, implement the agreed fixes, then re-run integration tests from step 2. Repeat until green or the user stops the loop.

## Rules

- Never skip failing tests to force a commit.
- Never “fix” by weakening assertions unless the user agrees the test is wrong.
- Keep diagnosis concrete and tied to specific tests/stack traces.
