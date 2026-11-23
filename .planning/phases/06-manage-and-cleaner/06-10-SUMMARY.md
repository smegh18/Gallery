---
phase: 06-manage-and-cleaner
plan: 10
subsystem: organizer-recycle
status: complete
tags: [organizer, recycle, datastore, navigation, album-stripe]
requires: [06-09, 06-12]
provides:
  - "Bounded, persisted organizer-bin ownership separate from regular recycle entries."
  - "Organizer Recycle selection, view, restore, permanent-delete and retry behavior."
  - "One-time All Photos organizer stripe with direct Quick Organizer routing."
affects: [06-11]
tech-stack:
  added: []
  patterns: ["persisted path ownership", "immutable mutation snapshots", "scoped recycle viewer source"]
key-files:
  created:
    - app/src/main/res/drawable/ic_organizer_white.xml
    - app/src/main/res/drawable/bg_rounded_with_border.xml
  modified:
    - app/src/main/java/com/quizango/gallery/data/local/datastore/AppPreferences.kt
    - app/src/main/java/com/quizango/gallery/ui/discovery/QuickOrganizerScreen.kt
    - app/src/main/java/com/quizango/gallery/ui/discovery/OrganizerRecycleScreen.kt
    - app/src/main/java/com/quizango/gallery/ui/album/AlbumDetailScreen.kt
    - app/src/main/java/com/quizango/gallery/ui/viewer/MediaViewerScreen.kt
    - app/src/main/java/com/quizango/gallery/ui/navigation/GalleryNavHost.kt
    - app/src/main/java/com/quizango/gallery/ui/discovery/OrganizerActions.kt
    - app/src/test/java/com/quizango/gallery/OrganizerTest.kt
decisions:
  - "The organizer-owned path set is bounded at 2000 paths and reconciled against the current recycle rows."
  - "Successful organizer trash is persisted before Quick Organizer advances; ordinary Recycle never adds ownership."
  - "Organizer viewer routes carry `organizer:recycle` and filter pages to owned paths."
  - "Restore/delete clear only successful owned paths; failures stay selected and retryable."
  - "The one-time stripe is visible only on filter:all_photos and stores dismissal before direct navigation."
metrics:
  completed: 2026-11-23
  tasks: 3
  commits: 0
---

# Phase 6 Plan 10: Organizer Recycle Summary

Completed Organizer Recycle and the one-time All Photos organizer stripe. Organizer bin ownership is persisted separately from ordinary recycle entries and every mutation keeps failed paths available for retry.

## What changed

- Added a bounded DataStore path set for organizer-bin ownership, startup reconciliation, and the one-time stripe visibility flag.
- Persisted successful organizer trash paths before advancing; Organizer Recycle filters by ownership and updates ownership only for successful restore/delete outcomes.
- Replaced the old single-item dialog with a shared toolbar, 3-column recycle cells, select-all/long-press, view routing, restore/delete confirmation, retention strip, and retryable failures.
- Added a dedicated `organizer:recycle` viewer source that filters pager entries to the organizer-owned path set.
- Added the All Photos-only green stripe; close persists dismissal and tap persists then opens Quick Organizer directly.
- Corrected organizer gestures to left-skip and up-trash, and added regression coverage for path ownership, stripe eligibility, and swipe directions.

## Verification

- `./gradlew.bat :app:testDebugUnitTest --tests "com.quizango.gallery.OrganizerTest" --tests "com.quizango.gallery.MediaRepositoryMutationTest" --tests "com.quizango.gallery.AlbumDetailTest" -q` passed.
- `./gradlew.bat :app:assembleDebug -q` passed.
- `python -I scripts/ref_res.py check-strings` passed (24 locales).
- `git diff --check` passed.

## Self-Check: PASSED

- FOUND: Plan 06-10 screen, preference, navigation, drawable and test outputs.
- VERIFIED: focused tests, debug assembly and resource checks.
