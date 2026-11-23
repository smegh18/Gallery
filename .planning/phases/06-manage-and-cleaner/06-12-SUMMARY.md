---
phase: 06-manage-and-cleaner
plan: 12
subsystem: database
status: complete
tags: [repository, delete, trash, restore, room]
requires: [06-01]
provides:
  - "PathMutationOutcome(path, status SUCCESS | FILE_FAILED | METADATA_FAILED) with successfulPaths()/failedPaths()"
  - "deletePermanently(path|paths): file first, row only once the file is gone; per-path outcomes"
  - "moveToTrash/restoreFromTrash(path|paths): row-only, per-path outcomes (bulk overloads are new)"
  - "internal deleteFilesThenRows / updateRows / deleteFileIfPresent in MediaRepository.kt, JVM-testable with lambdas"
affects: [06-06, 06-07, 06-08, 06-09, 06-10, 06-11]
tech-stack:
  added: []
  patterns: ["destructive repository calls return per-path outcomes; callers partition with successfulPaths/failedPaths"]
key-files:
  created:
    - app/src/main/java/com/quizango/gallery/data/local/PathMutationOutcome.kt
    - app/src/test/java/com/quizango/gallery/MediaRepositoryMutationTest.kt
  modified:
    - app/src/main/java/com/quizango/gallery/data/repository/MediaRepository.kt
decisions:
  - "Three statuses only: SUCCESS, FILE_FAILED (file still there, row untouched), METADATA_FAILED (file gone or untouched, row change failed; resend the same request to retry)"
  - "An already-absent file counts as file-side success, so the stale row is cleaned; this is what makes a METADATA_FAILED retry recover"
  - "Bulk delete removes all files first, then rows in 500-path chunks (SQLite IN-list cap); a failed chunk marks only its paths METADATA_FAILED"
  - "Single-path APIs are bulk(listOf(path)).single(), so both share one contract"
  - "Trash/restore failure is exception-based; an UPDATE that matches no row still reports SUCCESS (DAO returns Unit)"
  - "CancellationException is rethrown, never turned into a failed outcome"
requirements-completed: [CLN-01, CLN-02, CLN-03, CLN-04]
metrics:
  duration: "~45 min (most of it waiting on host commit memory for the test JVM)"
  completed: 2026-11-23
estimate:
  tokens: 70000
  tasks: 3
actuals:
  tokens: 3200
  tasks: 3
  commits: 3
plan_head_before: 5a5efde
plan_head_after: 5fb7a981bd58a4151a864184e8c85b75cafa310c
---

# Phase 6 Plan 12: Path mutation outcomes Summary

Permanent delete now removes the file before its Room row and reports a typed outcome per path; trash and restore report per path too, so Phase 6 callers can count, keep failed selection and claim completion truthfully.

## What changed

- `PathMutationOutcome` (data/local): `path`, `status`, `succeeded`, plus `List.successfulPaths()` / `failedPaths()`.
- `MediaRepository.deletePermanently(path)` returns one outcome, `deletePermanently(paths)` a list. The old code deleted the rows first and swallowed file errors; now a row is removed only after `File.delete()` succeeds or the file is already absent.
- `moveToTrash` / `restoreFromTrash` return outcomes and gained `List<String>` overloads.
- Paths are deduplicated in input order; outcomes are deterministic.

## Tasks

| Task | Commit | Notes |
|---|---|---|
| 1 Tracer: file-first delete with outcome | 403203d | Tracer re-verified (4/4 tests) before expanding |
| 2 Absent file, DAO failure, retry, duplicates | a600664 | Test-only: Task 1's coordinator already handled these cases |
| 3 Trash/restore outcomes | 5fb7a98 | Existing callers compile unchanged |

## Deviations from Plan

- Task 2 needed no production change: `deleteFilesThenRows` from Task 1 already treated absent files as gone, separated DAO failure, and deduplicated paths. Task 2 adds the four behavior tests.
- TDD RED commits were not made separately; the user asked for one atomic commit per task. Each task's tests ran green before its commit.

## Caller audit (deferred, not owned by this plan)

Every existing caller ignores the result and compiles unchanged: HomeViewModel (2), MediaListActions, GalleryNavHost (duplicate delete), LargeVideoScreen, ScreenShortScreen, OrganizerRecycleScreen (3), QuickOrganizerScreen, RecycleScreen (3), MediaViewerScreen (3).

Behavior change to know: if `File.delete()` fails (for example an SD-card file without a grant), the item now stays in the list rather than disappearing until the next sync. MediaListActions still shows the "deleted successfully" toast regardless. The Phase 6 plans that own these callers (06-06 delete process, recycle and organizer) should switch to the bulk overloads and use `failedPaths()` for counts, selection and toasts.

## Device steps for 06-11

1. Recycle Bin: permanently delete one item. Check with `adb shell ls` that the file is gone, and that the item does not come back after the app restarts (resync).
2. Remove a file with `adb shell rm`, then before resync delete it permanently from the app. The entry should disappear with no error.
3. Duplicate or Large Video delete (permanent): the deleted items are gone from disk and from the list; the counts match.
4. If an SD card is available: delete an SD-card file without the folder grant. The item should stay visible and the file should stay on disk.
5. Move to Recycle Bin, then Restore: both still work from the viewer and from multi-select.

## Verification

- `MediaRepositoryMutationTest`: 8 tests, all pass (ordering, file failure and exception, non-empty folder, absent file, DAO failure then retry, duplicate paths, single/bulk contract, trash/restore partition).
- `:app:testDebugUnitTest`: 46 classes, 0 failures. `:app:assembleDebug`: OK. Because host commit memory was low, the test JVM ran with a scratchpad init script (maxHeapSize 384m, SerialGC). No project files were changed for this.

## Known Stubs

None.

## Self-Check: PASSED

- FOUND: PathMutationOutcome.kt, MediaRepositoryMutationTest.kt
- FOUND: 403203d, a600664, 5fb7a98 (ancestors of HEAD)
