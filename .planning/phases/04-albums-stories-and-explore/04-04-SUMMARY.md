---
phase: 04-albums-stories-and-explore
plan: 04
subsystem: ui
tags: [compose, albums, selection, datastore]
requires:
  - phase: 04-01
    provides: HomeAlbumCard, ic_pin badge, pin/cover drawables, cannot_select_system_folders string
  - phase: 03-home-shell-and-photos-tab
    provides: HomeSelectionTopBar, DeleteMediaDialog, AddToAlbumSheet, AddingMediaDialog
provides:
  - Album selection mode on the Albums tab (marks, system-folder toast, top bar, 5-action bar, Back/close exit)
  - Pin to top / Unpin stored under "pinAlbum", pinned folders first with the pin badge
  - Bulk Delete and Add to over every file of the selected albums
affects: [04-05, 04-06]
tech-stack:
  added: []
  patterns: ["pure selection helpers (albumIsSelectable, pinLabel, singleAlbumActionAlpha, withPins, albumMediaPaths) unit-tested on the JVM", "copyOrMove shared by Photos and album add-to"]
key-files:
  created:
    - app/src/test/java/com/quizango/gallery/AlbumSelectionTest.kt
  modified:
    - app/src/main/java/com/quizango/gallery/ui/home/AlbumsTab.kt
    - app/src/main/java/com/quizango/gallery/ui/home/HomeSelection.kt
    - app/src/main/java/com/quizango/gallery/ui/home/HomeScreen.kt
    - app/src/main/java/com/quizango/gallery/ui/home/HomeViewModel.kt
    - app/src/main/java/com/quizango/gallery/data/local/datastore/AppPreferences.kt
decisions:
  - "Album Delete dialog counts selected albums (HA.count of the selection event), not files"
  - "Pin/Unpin leaves selection afterwards (AF.OooOOoo clears it)"
metrics:
  duration: ~60 min
  completed: 2026-11-20
status: complete
actuals:
  tokens: 8000
  tasks: 3
  commits: 3
plan_head_before: b2af6e6bff614e87e1cc65d51ee8829b97986bb2
plan_head_after: 2c3a649a38b1b4b2d06eee2fe4440d5662fbbbba
---

# Phase 4 Plan 04: Album selection mode, Pin and bulk Delete / Add to Summary

Holding a device folder on the Albums tab now enters 2.7.2's album selection: the Phase 3 selection top bar, a 5-action bar (Pin to top / Unpin, Add to, Delete, Change cover, Rename) in place of the tab bar, a locked pager, check marks on every card, and the English "You can not select system folders" toast for the virtual cards. Pins persist under the 2.7.2 key `pinAlbum`; Delete and Add to act on every file of the selected folders.

## What was built

- **Task 1 (789729e):** `albumIsSelectable`, `pinLabel`, `singleAlbumActionAlpha`; `HomeUiState.selectedAlbums` with `toggleAlbum` / `selectAllAlbums` / `clearAlbumSelection` (`setTab` clears it); `AlbumsTab` cards use `combinedClickable` with `ic_check` / `ic_un_check` marks; `AlbumSelectionBottomBar` (reuses `SelectionAction`, now taking a Modifier and an `albumBar` flag for the 4.sdp side padding and 2.sdp label gap); `HomeScreen` swaps the bars, locks the pager and bar-hide logic via `anySelecting`, and Back clears album selection after the vault hint and Photos selection.
- **Task 2 (b6f2ae1):** `AppPreferences.pinnedAlbums` / `setAlbumsPinned` (`stringSetPreferencesKey("pinAlbum")`); `withPins` feeds `Album.isPinned` in the albums collector; `albumGrid` sorts folders pinned-first (stable); `pinSelectedAlbum` acts for exactly one selected album.
- **Task 3 (2c3a649):** `albumMediaPaths`, `deleteSelectedAlbums` (unfiltered active media list, behind `DeleteMediaDialog`, Recycle Bin ticked by default), `addSelectedAlbumsToFolder` sharing a new `copyOrMove` with the Photos add-to (refuses a move whose only selected album is the target). `HomeScreen` reuses one delete dialog, add-to sheet and `addTarget` for both modes, branching on `albumSelecting`.

## Reference findings (from smali)

- Pin/Unpin clears the selection afterwards: both the ADD_PIN and REMOVE_PIN branches of `AF` end by calling `AF.OooOOoo()`, which posts the exit-selection event and clears the adapter's selected list. Ours does the same.
- Album Delete dialog (HA, event `home_albums_bottom_menu_delete`, tag `home_popup_delete_album`) builds its title as `deleted_title + " " + count + " " + deleted_items` with `count = HA.count`, the same field the top bar's "%d item(s) selected" uses, so it is the number of selected albums (the ao event built in AF carries the selected-album size). This overrides the plan's "number of files" default.
- The "Change cover" label wraps to two lines on the Redmi's 720 px width; the layout is equal-weight columns with 4.sdp side padding as in `layout_select_album_bottom`, so this is expected to match 2.7.2 (confirm in 04-06).

## Gate results

| Gate | Result |
|---|---|
| RED (AlbumSelectionTest compile failure, unresolved helpers) | seen for Task 1 |
| AlbumSelectionTest (6 tests), AlbumsTabTest, HomeShellTest, full unit suite | pass |
| `assembleDebug` after each task | pass |
| Greps: bar wired / toast / un_check mark / 5 bar icons = 1/1/1/5; `pinAlbum` key = 1, pin wired = 1; `deleteSelectedAlbums` and `addSelectedAlbumsToFolder` wired in HomeScreen | pass |
| Device (Redmi M2006C3MII, English), disposable folders `/sdcard/Pictures/ZZPhase4Test` and `ZZPhase4Dest` created via adb | long-press enters selection with the 2.7.2 bars; tapping Recent shows the system-folder toast; Pin shows `ic_pin` and the label turns "Unpin"; the pin survives `am force-stop` and relaunch; two selected albums dim Pin, Change cover and Rename to 0.6 and Pin does nothing; Delete dialog reads "...delete 1 item?" / "...delete 2 items?" with Move to Recycle Bin ticked; No keeps the selection; Add to shows the sheet (count badge 2), Move into the same album shows "Media files should not be moved to the same folder", Copy shows "Media files copied successfully." and the folder goes 2 -> 4; Delete Yes moved all 5 test files to the bin (Deleted card = 5, only my files); Back clears album selection |
| Cleanup | Recycle Bin (held only the 5 test files) emptied in the app, test folders removed, MediaStore rows for them deleted, no real album touched |

## Deviations from Plan

### Auto-fixed / judgement calls

**1. [Rule 1 - plan default overridden] Album delete dialog count**
- **Found during:** Task 3 (reading HA / AF smali as the plan asked)
- **Issue:** the plan defaulted to the number of files; the reference passes the selected-album count (`HA.count`).
- **Fix:** `DeleteMediaDialog(count = selectedAlbums.size)`, reusing the Phase 3 dialog (which prints "delete 1 item?" for 1).
- **Files modified:** ui/home/HomeScreen.kt
- **Commit:** 2c3a649

Otherwise the plan was executed as written. The RED step for Tasks 2 and 3 was a compile failure on the not-yet-existing helpers, not a separate runtime run.

## Known Stubs

- `onChangeCover` and `onRename` in `HomeScreen` are empty lambdas by design; 04-05 binds them.

## Notes for later plans

- 04-05: `selectedAlbums` (folder paths), `singleActionAlpha` and the bar already exist; bind the two empty lambdas and the album info button. Change cover and Rename are dimmed at more than one album but not blocked in the bar itself (`SelectionAction` still receives clicks at 0.6 alpha); 04-05 should ignore them when `selectedAlbums.size > 1`.
- A pinned path that is deleted leaves a stale entry in `pinAlbum` (T-04-09 accepted; it matches no album).
- Pinned order among several pins follows list order (plan); 2.7.2 inserts the newest pin at index 0, so verify in 04-06 if two pins are compared.

## Threat Flags

None. Delete is always behind the confirmation with Recycle Bin ticked (T-04-07); Move reuses `moveIntoFolder` (T-04-08).

## Self-Check: PASSED

AlbumSelectionTest.kt exists; commits 789729e, b6f2ae1, 2c3a649 are ancestors of HEAD; `commits: 3` measured from the ledger.
