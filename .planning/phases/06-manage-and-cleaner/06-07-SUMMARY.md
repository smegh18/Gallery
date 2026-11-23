---
phase: 06-manage-and-cleaner
plan: 07
subsystem: recycle-bin
status: complete
tags: [recycle, retention, datastore, repository, compose]
requires: [06-03, 06-04, 06-12]
provides:
  - "Fresh installs leave Recycle Bin auto-delete disabled while saved preference values remain exact."
  - "File-first, bounded expired-trash reconciliation that returns PathMutationOutcome values."
  - "Recycle restore/delete/empty actions retain failed paths for retry and route normal Back through the Phase 12 boundary."
affects: [06-11, 12-usage-access]
tech-stack:
  added: []
  patterns: ["file-first destructive reconciliation", "per-path success partitioning", "frozen confirmation targets"]
key-files:
  created: []
  modified:
    - app/src/main/java/com/quizango/gallery/data/local/datastore/AppPreferences.kt
    - app/src/main/java/com/quizango/gallery/data/repository/MediaRepository.kt
    - app/src/main/java/com/quizango/gallery/ui/recycle/RecycleScreen.kt
    - app/src/test/java/com/quizango/gallery/RecycleTest.kt
decisions:
  - "Absent auto-delete preference is false; persisted true and false values are read unchanged."
  - "Expiry uses deletedDate < now minus exactly 30 elapsed days, then reuses Phase 12 file-first outcomes in batches of at most 500."
  - "A confirmation snapshots requested paths; only successful paths leave selection, so failed paths remain retryable."
metrics:
  duration: "~30 min"
  completed: 2026-11-23
actuals:
  tokens: 132
  tasks: 3
  commits: 0
commits: 0
plan_head_before: 9bbe4a608568b32ee8426f56d140825cfeebde93
plan_head_after: 9bbe4a608568b32ee8426f56d140825cfeebde93
---

# Phase 6 Plan 7: Recycle Bin Summary

Recycle retention now starts off by default, purges only on an already saved opt-in, and uses Phase 12 file-first per-path outcomes so failed files remain visible or selected for retry.

## What changed

- Added a false-only absent preference fallback while preserving either stored Boolean.
- Replaced the row-only expired-trash DAO path with an exact 30-day, strict-cutoff repository reconciliation that deletes files first and caps each run at 500 paths.
- Updated Recycle Bin actions to freeze paths at confirmation, use bulk outcomes, keep failed selections, hide day strips while disabled, and show unclamped overdue day values while enabled.
- Added the Phase 12/USG-01 no-op trigger on ordinary Back; selection Back only clears selection.

## Verification

- `./gradlew.bat :app:testDebugUnitTest --tests "com.quizango.gallery.RecycleTest" -q` passed.
- `./gradlew.bat :app:assembleDebug -q` passed.
- `git diff --check` passed.

## Deviations from Plan

None - implementation followed the approved Q3 default-off decision and consumed the established 06-12 outcome contract.

## Known Stubs

None.

## Commit Note

No commit was created because execution is on protected `main`; the parent executor will stage the focused changes on the correct branch.

## Self-Check: PASSED

- FOUND: all four planned source/test files.
- VERIFIED: focused RecycleTest and debug build.
