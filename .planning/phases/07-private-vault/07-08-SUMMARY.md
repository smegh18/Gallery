---
phase: 07-private-vault
plan: 08
subsystem: ui
tags: [compose, vault, lock-gate, add-to-private]
requires:
  - phase: 07-01
    provides: VaultGate.lockSet/setupFinished, isVaultLockSet, hasRecoveryQuestion
  - phase: 07-02
    provides: safe hide/restore, callers re-sync with syncMediaStore
  - phase: 07-03
    provides: Screen.VaultLockFor, setup wipe on cancel
provides:
  - rememberVaultHideGate (shared Add to Private lock gate) and VaultGate.resolveReturn
  - Gate wired into Home, album list, Favorites, Animated, Search by colour, viewer
affects: [07-09]
status: complete
actuals:
  tokens: 9000
  tasks: 3
  commits: 3
tech-stack:
  added: []
  patterns: [stored lock plus question as the setup result, ON_RESUME observer resolves the pending paths]
key-files:
  created: []
  modified:
    - app/src/main/java/com/quizango/gallery/ui/vault/VaultGate.kt
    - app/src/test/java/com/quizango/gallery/VaultGateTest.kt
    - app/src/main/java/com/quizango/gallery/ui/home/HomeScreen.kt
    - app/src/main/java/com/quizango/gallery/ui/home/HomeViewModel.kt
    - app/src/main/java/com/quizango/gallery/ui/browse/MediaListActions.kt
    - app/src/main/java/com/quizango/gallery/ui/album/AlbumDetailScreen.kt
    - app/src/main/java/com/quizango/gallery/ui/favorites/FavoritesScreen.kt
    - app/src/main/java/com/quizango/gallery/ui/discovery/AnimatedMediaScreen.kt
    - app/src/main/java/com/quizango/gallery/ui/discovery/SearchColorScreen.kt
    - app/src/main/java/com/quizango/gallery/ui/viewer/MediaViewerScreen.kt
key-decisions:
  - "The stored lock plus recovery question is the setup result (A10 replaced); no navigation result key"
requirements-completed: [VLT-01]
---

# Phase 7 Plan 08: Add to Private lock gate Summary

All six Add to Private entry points (Home, album list, Favorites, Animated, Search by colour, viewer) now go through one gate: with a lock the Hide dialog opens as before, without one first-time setup (lock, then recovery question) runs and the Hide dialog opens only after it finishes; Back during setup hides nothing.

## Commits
- 6d7c850 feat(07-08): Home Add to Private routes through the vault lock gate and syncs after hide (Task 1, tracer)
- 1c6f46a feat(07-08): media-list Add to Private goes through the vault lock gate (Task 2)
- 435cdfb feat(07-08): viewer Add to Private goes through the vault lock gate and syncs after hide/unhide (Task 3)

## Verification (run on this machine)
- Tracer gate (Task 1): `./gradlew.bat :app:testDebugUnitTest --tests "com.quizango.gallery.VaultGateTest" --tests "com.quizango.gallery.HomeTest" -q` -> exit 0; `./gradlew.bat :app:assembleDebug -q` -> exit 0. Tracer verified, expansion proceeded.
- Task 2: `:app:assembleDebug -q` -> exit 0.
- Task 3: `./gradlew.bat :app:testDebugUnitTest --tests "com.quizango.gallery.MediaViewerTest" -q` -> exit 0; `:app:assembleDebug -q` -> exit 0.
- Full suite on the final tree: `./gradlew.bat :app:testDebugUnitTest -q` -> exit 0; 342 tests, 0 failures, 0 errors, 0 skipped (from the XML reports).
- Acceptance greps: `VaultLockFor.createRoute(0, false)` in VaultGate 1; `rememberVaultHideGate(` 1 each in HomeScreen, MediaListActions, MediaViewerScreen; `syncMediaStore()` within `moveSelectedToVault` 1 and in MediaViewerScreen 1; `PendingDialog.Hide(picked)` outside comments 0; all four list hosts now pass `onNavigate` to `rememberMediaListActions`.

## Behaviour now
- `rememberVaultHideGate(onNavigate, onReady)` returns a function taking paths. Lock set: `onReady(paths)`. No lock: paths are kept in a `rememberSaveable` list and the lock route (TYPE 0, no vault open) opens. On ON_RESUME with stored paths, they are cleared and `VaultGate.resolveReturn` decides: lock and question saved -> `onReady`, otherwise nothing.
- Home: the selection bar hands the selected paths to the gate; the Hide dialog is driven by those paths; `moveSelectedToVault(paths)` hides the matching items, calls `syncMediaStore()` once, then clears the selection.
- Media lists: `rememberMediaListActions` takes `onNavigate`; the gate's `onReady` keeps only items still in the list and opens the existing Hide dialog (nothing opens if none remain). Confirm branch unchanged.
- Viewer: dock Add to Private calls the gate with the current item's path; the HIDE dialog opens only if that item is still current. After a successful HIDE or UNHIDE the gallery table is synced before the close-or-next-page logic.

## Deviations from Plan
None. `resolveReturn` lives on the `VaultGate` object (the plan left its placement open).

## Decisions / accepted limits
- With a lock already set the gate does not also check for a recovery question (plan: lock set -> Hide dialog directly). 07-03 wipes half-made locks, so lock-without-question should not persist.
- Stored paths use a `ArrayList` in `rememberSaveable`; a selection of thousands of paths could exceed the saved-state Bundle limit if the process is stopped during setup. Marked in code as a shortcut.
- The Home selection survives the setup trip in the ViewModel; if the user changed it before returning, the gate's stored paths still decide what is hidden.

## Known Stubs
None.

## Pending device checks (all PENDING-DEVICE, no phone, Artemis down)
- Fresh install: select a disposable photo on Photos, Add to Private: Set PIN, Re-enter, three questions, then the Hide dialog; Ok hides it and Photos updates at once (D7).
- Back during setup (lock screen and question screen) hides nothing, on each of the six entry points.
- Viewer Add to Private with no lock goes through setup, then the Hide dialog for the same photo.
- MIUI: MediaStore row of a hidden/unhidden file disappears/reappears immediately.

## Threat Flags
None. T-07-25 (no hide without lock), T-07-26 (stored paths filtered to present items / current item, dialog still asks) and T-07-27 (cancelled setup leaves no lock so `resolveReturn` returns null, unit-tested) are mitigated as planned.

## Self-Check: PASSED
Commits 6d7c850, 1c6f46a, 435cdfb present; all listed files modified; no deletions in any commit.
