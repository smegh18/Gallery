---
phase: 06-manage-and-cleaner
plan: 04
subsystem: cleaner
tags: [large-video, screenshots, permanent-delete, usage-access-hook]
status: complete
requires: [06-01, 06-12]
provides:
  - shared cleaner grid (CleanerMediaScreen, CleanerMediaKind)
  - CleanerDeleteDialog (dialog_cleaner_permanently_delete)
  - recordVideoUsageAccessTrigger / leaveWithVideoTrigger
affects: [06-11, phase-12]
key-files:
  created:
    - app/src/main/java/com/quizango/gallery/ui/cleaner/CleanerDeleteDialog.kt
    - app/src/test/java/com/quizango/gallery/CleanerMediaTest.kt
  modified:
    - app/src/main/java/com/quizango/gallery/data/repository/MediaRepository.kt
    - app/src/main/java/com/quizango/gallery/ui/cleaner/LargeVideoScreen.kt
    - app/src/main/java/com/quizango/gallery/ui/cleaner/ScreenShortScreen.kt
    - app/src/main/java/com/quizango/gallery/ui/browse/SharedMediaList.kt
    - app/src/main/java/com/quizango/gallery/ui/home/HomeDialogs.kt
    - app/src/main/java/com/quizango/gallery/ui/home/AlbumSelectionActions.kt
    - app/src/main/java/com/quizango/gallery/ui/discovery/StoryBuilder.kt
    - app/src/main/java/com/quizango/gallery/ui/viewer/VideoPlayerScreen.kt
    - app/src/main/java/com/quizango/gallery/ui/viewer/MediaViewerScreen.kt
    - app/src/test/java/com/quizango/gallery/CleanerTest.kt
    - app/src/test/java/com/quizango/gallery/VideoPlayerTest.kt
    - app/src/test/java/com/quizango/gallery/MediaViewerTest.kt
decisions:
  - "Large Video cells show the txt_file_size pill: decoded dt/a sets it VISIBLE with s9/k size; the UI-SPEC's 'stays hidden' holds only for the screenshot cell (b0/b sets GONE)"
  - "Screenshots match folderPath with case-sensitive contains on the four reference keys, any media type (ScreenshotsViewModel reads all rows)"
  - "Partial or failed cleaner delete shows something_went_wrong; the success toast only when every path went"
metrics:
  duration: ~75 min
  completed: 2026-11-23
actuals:
  tokens: 18000
  tasks: 3
  commits: 3
plan_head_before: 3a42a2eca6fa49a5db90d916653ba09391d0ef2f
plan_head_after: 666339a
---

# Phase 6 Plan 04: Large Videos, Screenshots and the video usage hook Summary

Large Video and Screenshots now share one cleaner screen, as the reference shares its layouts. Large Video lists videos of at least 73,400,320 bytes in 3 columns. Screenshots lists the four reference folders in 4 columns. Both screens group items by day, start in pick mode, and delete permanently through `dialog_cleaner_permanently_delete`. Files that fail to delete stay selected. The Phase 12 usage-access trigger is now a no-op hook at player Back and at viewer exit.

## Tasks

| # | Task | Commit |
|---|------|--------|
| 1 | Tracer: 70 MiB Large Video, shared grid, delete dialog, outcome reconciliation (tracer re-verified before expanding) | bccf06a |
| 2 | Screenshots: four-folder filter, 4 columns, screenshot copy | 550a256 |
| 3 | `recordVideoUsageAccessTrigger()` at player Back and viewer exit | 666339a |

## What changed
- **`MediaRepository`**
  - `LARGE_VIDEO_MIN_BYTES = 73_400_320` is the default threshold.
  - `largeVideos()` filters videos and sorts them newest first by `dateAdded`. The old order was largest first.
  - `isScreenshotFolder()` does a case-sensitive `contains` on `Pictures/Screenshot(s)` and `DCIM/Screenshot(s)`. The old broad name/folder `ignoreCase` match is gone. `StoryBuilder` now reuses this function instead of keeping its own copy of the list.
- **`LargeVideoScreen.kt`** holds `CleanerMediaScreen` and the `CleanerMediaKind` enum (LARGE_VIDEO and SCREENSHOTS: title, columns, empty copy, dialog copy, background, size pill). The screen has:
  - **Toolbar:** `SharedMediaToolbar` with Back. Select all / Unselect all is always shown while the list is not empty. With 1 or more items picked, the title reads `items_selected`.
  - **Grid:** `sharedMediaRows` provides Today / Yesterday / `dd MMM yyyy` headers (12.sp Inter Regular `black`, 5/4.sdp) that span the full row.
  - **Cells:** square with a 1.sdp margin and centre-crop. They show `ic_check` / `ic_un_check` at the top end, and a tap toggles the cell. Large Video cells also show the `large_video_duration` pill with `formatCleanerSize`.
  - **Delete button:** 38.sdp tall, radius 25.dp. It is `btn_delete_disable` and inert with nothing picked, `dark_blue` otherwise. 16.sp bold text: "Delete Selected" / "Delete (N)" / "Delete All (N)". It is hidden when the list is empty.
  - **States:** a spinner until the first query returns, then `PhotosEmptyState` with the per-kind copy at 53.sdp.
- **Delete:**
  1. Tapping Delete freezes the selected paths in display order.
  2. `CleanerDeleteDialog` asks for confirmation.
  3. A `ProgressTextDialog("Deleting")` shows while `runCleanerDelete` runs under `NonCancellable` through `deletePermanently(paths)`.
  4. Afterwards the selection is exactly `failedPaths()`. The toast says success only when nothing failed, and `something_went_wrong` otherwise.
- **`CleanerDeleteDialog.kt`:** a `CommonDialog` card (`dialogBack`, 14.sdp) with the title and subtitle, a `cleaner_red` "Permanently delete" pill and a 1.dp #969696 outlined "Cancel" pill, each 40.sdp tall. Compose shapes replace the `round_button` / `permanent_btn_border` drawables. The vector border would stretch its corners, so it was not imported.
- **`ScreenShortScreen.kt`:** now a 6-line wrapper around the shared screen. It was a 327-line copy.
- **`SharedMediaToolbar`:** new `showToggleAll` parameter, default `selecting`, so existing callers are unchanged. `RenameProgressDialog(count)` became `ProgressTextDialog(text)`, and its one caller passes `modifying_items`.
- **Player:** `back()` ports `VideoPlayerActivity.Oooo0oO`:
  1. It returns while the player is locked.
  2. Otherwise it calls `player.stop()`.
  3. Then `leaveWithVideoTrigger(true)` calls the hook, then navigates.

  A `BackHandler` and the toolbar Back both call it. `close()` (error, missing file) still leaves without the hook.
- **Viewer:** `viewedVideo` is set once any shown page is a video (`hasViewedVideo`, set from `onPageSelected` in `bw/ap`). System Back (when no slideshow is running), toolbar Back and swipe-dismiss all go through `exit()`. `exit()` calls the hook only when `viewedVideo` is set.
- Removed `formatVideoDuration`, `formatVideoSize` and their two `CleanerTest` cases. The old Material `AlertDialog`, `ArrowBack` and `Check` icons, and the "Delete (size)" literals are gone.

## Verification
- `:app:testDebugUnitTest` (full): 278 tests in 47 classes, 0 failures. `:app:assembleDebug`: OK. Both ran with `-Dorg.gradle.jvmargs=-Xmx2048m -Pkotlin.compiler.execution.strategy=in-process`. The tests also used a scratchpad init script (test `maxHeapSize 512m`), and no project files changed. Finished with `./gradlew.bat --stop`.
- `CleanerMediaTest` (6 tests):
  - the threshold is exactly 73,400,320 bytes, and 1 byte less is excluded
  - images are excluded, and the list is newest first
  - headers span 3 and 4 columns, and media cells span 1
  - the label is count-based (0 / some / all)
  - the delete receives only the snapshot, and FILE_FAILED and METADATA_FAILED paths stay selected while all-success leaves nothing selected
  - all four folders match, including an SD card path; a lowercase folder, a bare `/Screenshots` and `Movies/Screenshot` do not
  - the SCREENSHOTS kind has 4 columns, the screenshot copy and no size pill
- `VideoPlayerTest` checks that the trigger runs once and before navigation, and that the hook is a no-op. `MediaViewerTest` checks that the trigger runs only after a video was shown.

## Deviations from Plan

**1. [Rule 1 - Parity] Large Video size pill shown (UI-SPEC §3 said hidden)**
- Decoded `dt/a.Wja3o2vx62` sets `txt_file_size` to `s9/k.OooO0o0(fileSize)` and calls `setVisibility(0)`. The screenshot cell `b0/b` sets it to 8 (GONE). The spec row is right only for screenshots.
- The plan's "duration overlay" is in fact this size pill on `large_video_duration`. 2.7.2 shows no duration text on this screen.

**2. [Rule 1 - Parity] Screenshots include any media type under the four folders**
- `ScreenshotsViewModel` pages `SELECT * FROM images_table` (`y9/r.DxDJysLV5r`), with no type filter, and then calls `contains(folderPath, key, false)`. The spec wording "images whose path contains" was looser than the decoded code.

**3. [Rule 1 - Parity] Locked player ignores system Back**
- Before this plan, system Back popped the player even while it was locked. The decoded Back owner returns early when `isLock` is set, so the hook placement brings that behaviour with it. Unlock stays reachable with a tap.

**4. [Scope] Partial-failure toast**
- The reference toasts success unconditionally. The plan forbids claiming the requested count, so any failure shows `something_went_wrong`. The failed items stay visible and selected.

Not done: `lineSpacingExtra 2.sdp` on the dialog title and subtitle (a cosmetic 2.sdp per line).

Not unit-tested, because the repo has no Compose UI test infrastructure: the visuals, Back ordering against other handlers, and the progress dialog blocking Back.

## Known Stubs
- `recordVideoUsageAccessTrigger()` is an intentional no-op (`shortcut:` comment). Phase 12 / USG-01 owns the dialog and the system intent, per user decision Q5.

## Threat Flags
None. T-06-08: exact dialog, frozen snapshot, and `NonCancellable` so a delete cannot stop half-way. T-06-09: path-only local match, no telemetry. T-06-10: a synchronous no-op that runs after `player.stop()`; the dispose still releases the player.

## Device steps for 06-11
Use a throwaway fixture only. Push a video of at least 70 MiB, for example `fallocate`-style padding of a real mp4, to `/sdcard/DCIM/ZZLargeTest/`, plus one of 69 MiB. Push two PNGs to `/sdcard/Pictures/Screenshots/ZZ/` (or use existing screenshots, but do not delete them), and one `Screenshot_x.png` in `/sdcard/Pictures/ZZOther/`. Resync.
1. Large Video:
   - Only the 70 MiB fixture appears, under Today, in 3 columns, with the blue size pill bottom-start and an empty check circle.
   - The button reads grey "Delete Selected" and does nothing.
   - Tap the cell: the title reads "1 item(s) selected", the toolbar shows "Unselect all", and the button turns blue "Delete All (1)".
2. Delete → the dialog shows "Want to delete the videos?". Check:
   - Cancel closes it.
   - Delete → Permanently delete: "Deleting" appears, then the toast "Media files deleted successfully."
   - The cell leaves the grid and the empty copy shows.
   - `adb shell ls` confirms the file is gone. It is not in the Recycle Bin.
3. Screenshots:
   - 4 columns, the fixtures appear, and `ZZOther/Screenshot_x.png` does not.
   - No size pill on the cells.
   - The dialog copy is "Want to delete the screenshots?".
   - Select all, then deselect one: "Delete (N-1)".
4. Dark mode: the Large Video background is black (`white` at night), and Screenshots uses `backgroundColor`. Compare both with 2.7.2.
5. If an SD card is available: a delete that fails (no grant) keeps the item listed and selected, and the toast reads "Something went wrong".
6. Video player: Back while locked does nothing. Unlock, then Back: audio stops at once and the player closes. Toolbar Back behaves the same.
7. Viewer: open a photo, then Back closes normally. Swipe to a video, then Back / toolbar Back / swipe-down all close. A no-op hook leaves nothing visible to check, so just confirm there is no crash or double pop.
8. Remove the fixtures afterwards.

## Self-Check: PASSED
- FOUND: CleanerDeleteDialog.kt, CleanerMediaTest.kt, LargeVideoScreen.kt, ScreenShortScreen.kt
- FOUND commits: bccf06a, 550a256, 666339a (ancestors of HEAD)
