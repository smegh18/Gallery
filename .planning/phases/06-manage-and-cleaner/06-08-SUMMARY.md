---
phase: 06-manage-and-cleaner
plan: 08
subsystem: quick-organizer
status: complete
tags: [organizer, pager, albums, undo, compose]
requires: [06-01, 06-12]
provides:
  - "Full-width path-tracked photo pager with directional skip and trash gestures."
  - "Organizer album row, Create Album flow, same-folder refusal and persisted-column jump grid."
  - "Outcome-gated immutable action history and retry-safe Undo."
  - "Organizer Back prompt and session deletion outcome handling; tutorial preference read points for 06-09."
affects: [06-09, 06-10, 06-11]
tech-stack:
  added: []
  patterns: ["path-keyed organizer history", "serialized mutations", "bounded outcome-based permanent deletion"]
key-files:
  created:
    - app/src/main/java/com/quizango/gallery/ui/discovery/OrganizerActions.kt
    - app/src/test/java/com/quizango/gallery/OrganizerTest.kt
    - app/src/main/res/drawable/ic_down_arrow.xml
    - app/src/main/res/drawable/ic_undo_unchecked.xml
    - app/src/main/res/drawable/ic_undo_checked.xml
    - app/src/main/res/drawable/ic_album_placeholder.xml
    - app/src/main/res/drawable/organizer_create_album.xml
    - app/src/main/res/drawable/ic_information.xml
  modified:
    - app/src/main/java/com/quizango/gallery/ui/discovery/QuickOrganizerScreen.kt
    - app/src/main/java/com/quizango/gallery/data/local/datastore/AppPreferences.kt
    - app/src/main/res/values/strings.xml
decisions:
  - "Skip keeps a path in the jump grid; the pager's completed-path set only excludes it from the current queue and Undo or jump restores it."
  - "Album-move Undo stores both original and destination folders; failed outcomes preserve the action and current selection."
  - "The Back deletion prompt is gated on persisted tutorial step 7 and session-organized paths, with frozen targets and failures retained for retry."
  - "Organizer defaults read isOrgSkipToolTip without writing it; step-flow defaults to 1 and Plan 06-09 owns transitions."
metrics:
  completed: 2026-11-23
  tasks: 3
  commits: 0
---

# Phase 6 Plan 8: Quick Organizer Summary

Replaced the old card controls with the reference-style photo pager, swipe actions, album strip and all-photo jump grid. Reversible actions are tied to file paths and only commit after the repository reports success.

## What changed

- Added the horizontal photo pager, left-skip and up-to-trash gestures, and a queue that reconciles against active media without losing jump-grid entries.
- Added album rows with Create Album first, Photos and Family defaults honoring the read-only skip-tooltip preference, exact same-folder refusal, and persisted media-column count in the jump grid.
- Added path-based undo for skip, trash and album moves. Failed trash, move or inverse operations leave the current item and history intact.
- Added loading and empty states, delete navigation, and the tutorial-step-gated Back permanent-delete prompt with frozen targets and retryable failures.
- Imported the six organizer drawables and added preference reads for the decoded intro step and skip-tooltip flag.

## Verification

- `./gradlew.bat :app:testDebugUnitTest --tests "com.quizango.gallery.OrganizerTest" -q` passed.
- `./gradlew.bat :app:assembleDebug -q` passed.
- `python -I scripts/ref_res.py check-strings` passed (24 locales).
- `git diff --check` passed.

## Follow-up for Plan 06-09

Plan 06-09 must add `setOrganizerStepFlow` and drive the persisted 1→7 intro transitions. This screen already reads the preference with the first-use default and gates the Back prompt on step 7.

## Self-Check: PASSED

- FOUND: all Plan 06-08 screen, preference, asset and test outputs.
- VERIFIED: focused Organizer tests, debug assembly and resource checks.
