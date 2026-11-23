---
phase: 07-private-vault
plan: 06
subsystem: ui
tags: [compose, vault, selection-bar, unhide, toast, cache-cleanup]
requires:
  - phase: 07-02
    provides: collision-safe restoreFromVault with MediaStore re-index
provides:
  - MediaAction.UNHIDE and vaultMediaActions() (Share, Unhide, Delete)
  - SharedMediaToolbar onSettings slot (settingBtn)
  - VaultMediaScreen ported to activity_vault_media (#33)
affects: [07-07, 07-08, 07-09]
status: complete
actuals:
  tokens: 22000
  tasks: 3
  commits: 3
tech-stack:
  added: []
  patterns: [ViewModel on a nav back-stack entry owns resource cleanup, Compose overlay instead of a custom-view system toast]
key-files:
  created: []
  modified:
    - app/src/main/java/com/quizango/gallery/ui/vault/VaultMediaScreen.kt
    - app/src/main/java/com/quizango/gallery/ui/browse/MediaListActions.kt
    - app/src/main/java/com/quizango/gallery/ui/browse/SharedMediaList.kt
    - app/src/main/java/com/quizango/gallery/ui/home/HomeSelection.kt
    - app/src/test/java/com/quizango/gallery/SharedMediaListTest.kt
key-decisions:
  - "Decrypted view copies are cleared by a ViewModel on the vault back-stack entry, so they survive the viewer, picker and rotation"
  - "The secured toast is a Compose overlay (3.5 s, bottom-centred), flag cleared before it is shown"
requirements-completed: [VLT-01]
---

# Phase 7 Plan 06: Vault media screen Summary

The vault grid (#33) now uses the house toolbar, selection bar and dialogs: Share / Unhide / Delete bar, confirm then "Unhiding" progress then one `syncMediaStore()` for the batch, a day-grouped 3-column grid, the reference empty state and FAB, a one-time shield toast, and decrypted-copy cleanup tied to the vault entry.

## Commits
- fb32a55 feat(07-06): vault Unhide through the house selection bar, dialog, progress and re-index (Task 1, tracer)
- 1b272ce feat(07-06): rebuild vault media chrome to activity_vault_media (Task 2)
- 7edf3f0 feat(07-06): one-time secured toast and decrypted-cache cleanup tied to the vault entry (Task 3)

## Verification (run on this machine)
- Tracer gate (before Tasks 2/3): `./gradlew.bat :app:testDebugUnitTest --tests "com.quizango.gallery.SharedMediaListTest" -q` -> exit 0; `./gradlew.bat :app:assembleDebug -q` -> exit 0. Tracer verified, expansion proceeded.
- Task 2: `./gradlew.bat :app:testDebugUnitTest -q` (whole suite) -> exit 0 (after fixing one compile error, see below); `./gradlew.bat :app:assembleDebug -q` -> exit 0.
- Task 3 / final tree: `./gradlew.bat :app:assembleDebug -q` -> exit 0; `./gradlew.bat :app:testDebugUnitTest -q` -> exit 0.
- Acceptance greps: `MediaSelectionBar(vaultMediaActions()` 1; `R.string.unhide_items` 1; `syncMediaStore()` 1; `MediaAction.UNHIDE ->` 1 in each of HomeSelection and MediaListActions (2 total); banned-pattern grep (AlertDialog/TopAppBar/Icons.Default/raw `.sp`/English day labels/" Selected") 0; `onSettings` in SharedMediaList 4; listed R.string refs 5; `showMoveToBin = false` 1; `setPrivateToastShow(false)` 1; `R.string.toast_private` 1; `onCleared` 1; `clearViewCache()` 1; `ic_secure|ic_toast_bg` 2.

## Behaviour now
- Selection bar: Share, Unhide, Delete only (A5). Toolbar reads "N item(s) selected" with Select all / Unselect all; Back clears the selection first.
- Unhide: `HideMediaDialog` (Unhide items / Are you sure you want to unhide items?), then `ProgressTextDialog` "Unhiding", then one `mediaRepository.syncMediaStore()` for the batch, then a success toast (or "An issue occurred!" when nothing was restored). Works on a snapshot of the selection.
- Delete: `DeleteMediaDialog(showMoveToBin = false)`, "Deleting" progress, success toast.
- Empty state only after the first query emits (nullable flow); before that a colorPrimary spinner layer that blocks touches. FAB and Add Items both open the picker; FAB only when items exist.
- Grid: 3 columns, day headers via `sharedHeaderDate` and the Phase 3 Today/Yesterday keys, cells on the item_grid_gallery tokens (a vault-specific cell, since `PhotoGridItem` takes a `MediaItem` content URI, not a decrypted file).
- Secured toast: `isPrivateToastShow` read; when true it is cleared first, then the shield overlay shows for 3.5 s.
- `VaultViewCache` ViewModel on the vault back-stack entry calls `clearViewCache()` from `onCleared` (on IO), so copies stay while the viewer, slideshow, picker or settings are on top and across rotation.

## Deviations from Plan

**1. [Rule 1 - Bug] Share handed out ciphertext** - the old `shareSelectedVaultMedia` shared `File(vaultPath)`, which is the encrypted file. Share now decrypts through `viewFile` and passes the cache copies to the house `shareSelectedMedia` (chooser title `R.string.share`). A spinner layer blocks touches while the copies are prepared. The plan said "keeps the existing decrypted-copy share"; the existing code did not do that.

**2. [Rule 3 - Blocking] Compile error** - `TextUnit + TextUnit` is not defined; the empty-state line height (font x 1.2 + 3.sdp) is built from `.value` instead. Not a behaviour change.

**3. Spinner layer reuse** - the plan's `progressBar` layer has no stated trigger besides "while a file action runs"; the unhide/delete actions already use `ProgressTextDialog`, so the layer covers the first query and Share's decrypt step.

## Decisions / accepted limits
- Rotation while the secured toast shows drops it (the flag is already cleared, so it is not shown again). `shortcut:` acceptable for a one-time cosmetic toast.
- Toast bottom offset is `navigationBarsPadding() + 64.dp` (approximating the system toast's default offset); the exact position and the 2.7.2 match are PENDING-DEVICE per the plan.
- `onCleared` clears on a fire-and-forget IO coroutine (as `GalleryApplication` does at start) to keep the main thread free; if the process dies first, app start clears the cache anyway.
- Vault deleted-item cache copies are dropped by `deleteFromVault` / `restoreFromVault` (unchanged).
- D2 honoured: no Hidden Folder section, bar or album cards built.

## Known Stubs
None.

## Pending device checks (all PENDING-DEVICE, no phone, Artemis down)
- Select a disposable vault fixture, Unhide, Ok: "Unhiding" shows, toast appears, photo is back in Photos at once (MIUI re-index).
- Empty vault, populated vault, selection, delete side by side with 2.7.2 in light and dark; thumbnail decrypt speed on a large vault.
- First setup -> vault shows the secured toast once (position and duration vs 2.7.2); reopening does not.
- After leaving the vault: `adb shell run-as com.quizango.gallery ls cache/vault_view` is empty; it is not emptied while the viewer or picker is open.
- Share a vault photo: the target app receives the decrypted image.

## Threat Flags
None. T-07-19 (cache cleanup on entry pop) and T-07-20 (snapshot, confirmations, collision-safe restore) are mitigated as planned; T-07-21 accepted, and Share is now correctly decrypted rather than leaking ciphertext.

## Self-Check: PASSED
Commits fb32a55, 1b272ce, 7edf3f0 present; modified files exist; no file deletions in any commit.
