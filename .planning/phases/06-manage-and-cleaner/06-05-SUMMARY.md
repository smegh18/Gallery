---
phase: 06-manage-and-cleaner
plan: 05
subsystem: cleaner
status: complete
tags: [cleaner, duplicates, parity]
requires: [06-01]
provides:
  - "DuplicateWaitingScreen: real scan, then success Lottie once, then results (runDuplicateWaiting); no timers"
  - "DuplicatePhotoScreen: 2.7.2 groups, Keep One Photo, 4-column item_duplicate_sub cells, fast scroll, loading/empty/grouped"
  - "DuplicateDeleteRequest(items, moveToRecycleBin) handed out through onDelete after the Phase 3 dialog_delete"
  - "ic_ads_left drawable; PhotosEmptyState titleHorizontalPadding; SharedMediaList fallDown now internal"
affects: [06-06, 06-11]
tech-stack:
  added: []
  patterns: ["LottieAnimatable.animate suspends until the single play ends, replacing fixed delays", "selection snapshot frozen at tap time, confirmed as an immutable request"]
key-files:
  created:
    - app/src/main/res/drawable/ic_ads_left.xml
  modified:
    - app/src/main/java/com/quizango/gallery/ui/cleaner/DuplicateWaitingScreen.kt
    - app/src/main/java/com/quizango/gallery/ui/cleaner/DuplicatePhotoScreen.kt
    - app/src/main/java/com/quizango/gallery/ui/navigation/GalleryNavHost.kt
    - app/src/main/java/com/quizango/gallery/ui/home/PhotosTab.kt
    - app/src/main/java/com/quizango/gallery/ui/browse/SharedMediaList.kt
    - app/src/main/res/values/strings.xml
    - app/src/test/java/com/quizango/gallery/DuplicateTest.kt
decisions:
  - "The waiting spinner is indeterminate, as in activity_duplicate_waiting (no progress readout); the scan lambda lost its unused onProgress"
  - "Delete dialog always uses deleted_title + N + deleted_items (customTitle), also for one item, per UI-SPEC §4.2"
  - "Until 06-06, GalleryNavHost runs the request itself (moveToTrash or deletePermanently by the checkbox) and opens the existing complete route"
  - "English values fixed to 2.7.2: scanning = \"Scanning...\", keep_best_photo = \"Keep One Photo\" (translations were already right)"
metrics:
  duration: "~70 min (about 25 min lost to host commit-memory failures)"
  completed: 2026-11-23
estimate:
  tokens: 85000
  tasks: 3
actuals:
  tokens: 6400
  tasks: 3
  commits: 4
plan_head_before: 420b48c481f8ba54a91781d193ff8c454c16ec8c
plan_head_after: 73565feb02d0af7d30be8da56e901fac9fa67ed8
---

# Phase 6 Plan 05: Duplicate waiting, groups and Delete Selected Summary

Duplicate Finder now follows 2.7.2 from scan to confirmation. The blue waiting screen runs the real fingerprint scan, plays `success` once and opens the groups when the animation ends. There are no timers. The groups screen has "Group 1 (2 Images)" labels, the Keep One Photo switch (off on open), 4-column cells captioned with the folder name, and "Delete Selected (N items)" with the AD badge. Tapping it opens the Phase 3 `dialog_delete` and hands a frozen request onward.

## Tasks

| # | Task | Commit |
|---|------|--------|
| 1 | Tracer: waiting → done → results on Lottie end | 8cb923c |
| 2 | Groups, Keep One Photo, cells, fast scroll, states | 939253a |
| 3 | Delete Selected → dialog_delete → `DuplicateDeleteRequest` | 4efff6b |
| - | Drop unused `permanent_delete_duplicate_*` keys | 73565fe |

## What changed
- **`DuplicateWaitingScreen.kt`** (§4.1):
  - `color_success_lottie_blue` background, 14.sdp padding, a 48.dp white spinner, then `scanning` and `dup_media_wait_hint` (18.sdp / 8.sdp, GalleryText).
  - The done state shows `success` at 150.sdp and `scan_complete`.
  - `runDuplicateWaiting` orders the steps: scan, then done, then `LottieAnimatable.animate` (one play), then `onScanFinished`. Both `delay`s are gone. If the composition fails to load, the screen goes straight on.
- **`DuplicatePhotoScreen.kt`** (§4.2):
  - Uses `SharedMediaToolbar`. The media flow is remembered (it was re-collected on every recomposition). `null` until grouped, so the states are spinner, then `PhotosEmptyState` with the duplicate copy at 53.sdp, then the groups.
  - The Keep One Photo row uses a switch with a `colorPrimary` thumb. Turning it on runs `keepOneSelection` (drop(1) per group); turning it off clears the selection.
  - Group headers use `duplicateGroupLabel` (1-based, BodySmallMedium `black`). The grid has 4 columns with padding 2.sdp h / 50.sdp bottom, keys by file path, the `fallDown` entrance on first fill, and `FastScrollThumb`.
  - Cells: radius 3.sdp, margin 1.sdp, `ic_check` / `ic_un_check` top-end, and the folder name in BodyXSmallMedium with a shadow.
  - When media changes, paths that are no longer listed are pruned from the selection (`pruneSelection`).
  - The Delete Selected bar (40.sdp, `ic_delete_duplicate_btn_selected_bg`, BodyXLargeSemiBold, `ic_ads_left` at 8.dp) floats over the grid's bottom padding and only shows while something is selected.
  - Removed: BEST, "Group X (Y images)", Select All, the AlertDialog, every `.sp`, and the in-screen delete.
- **Delete flow:**
  1. The tap snapshots `selectedDuplicates` (display order, only paths still listed). If that is empty, the `please_select_items` toast shows.
  2. `DeleteMediaDialog` opens with `customTitle`.
  3. Yes builds `DuplicateDeleteRequest(items, moveToRecycleBin)` and calls `onDelete`. The screen deletes nothing itself.
- **`GalleryNavHost.kt`:** the scan lambda changed. The interim `onDelete` runs the request (trash or permanent), then navigates to `DuplicateComplete` (`shortcut:` comment, replaced by 06-06).
- **Shared code:** `PhotosEmptyState` gains `titleHorizontalPadding` and a centred title (single-line titles look the same). `fallDown` is now `internal`.

## Verification
- `:app:testDebugUnitTest` (full, 45 suites) passes, and `:app:assembleDebug` passes. `ref_res.py check-strings` reports 24 languages up to date. The gates ran with `-Dorg.gradle.jvmargs=-Xmx2048m`, then `./gradlew.bat --stop`.
- `DuplicateTest` has 11 tests: the waiting order (scan → done → success → results); no `delay(` in the waiting screen; no `DuplicateScanningComplete` in `Screen.kt`; 1-based labels; Keep One = drop(1); unique path keys, with the selection surviving a flow update that removes one photo; LOADING/EMPTY/CONTENT stay distinct; zero or stale-only selection gives null (toast); the CTA label and the `ic_ads_left` badge; the frozen request carries its paths and recycle choice, and the results screen contains no delete call. `DuplicateFinderTest` (2) is unchanged and passes.

## Deviations from Plan

### Auto-fixed Issues

**1. [Rule 3 - Blocking] Test JVM heap under host memory pressure**
- **Issue:** Parallel executors filled the host commit charge. The forked test JVM failed to commit its default 512 MB initial heap (errno 1455).
- **Fix:** Ran the test gates with an init script from the session scratchpad (`minHeapSize 64m`, `maxHeapSize 768m`). This changed only the environment; no project files changed.

**2. [Rule 1 - Parity] English `scanning` and `keep_best_photo` values**
- 2.7.2 has "Scanning..." and "Keep One Photo". Ours had "Scanning…" and "Keep Best Photo". The locales already matched. The `scanning` fix also reaches the junk scan state, which uses the same key. Commit 939253a.

**3. [Rule 2 - Spec cleanup] Removed `permanent_delete_duplicate_*` keys**
- The UI-SPEC lists them for removal once their last use goes, and this plan removed that use. Commit 73565fe.

Task-boundary notes:
- Task 1's tests landed in the Task 2 commit (with Task 2's own tests).
- The Delete Selected bar's look landed in Task 2, and Task 3 replaced the old dialog.
- Every commit compiles. The tests ran only after Task 3 because host memory blocked the forked test JVM. The full suite passes at HEAD.
- `DuplicateFinderTest` was not edited: DuplicateFinder is unchanged, as the plan requires.

## Known Stubs
- `GalleryNavHost` Duplicates `onDelete` runs the delete directly, without the §4.3 process state and without per-path success. Plan 06-06 replaces it with `DuplicateDeleteProcessScreen` (it has a `shortcut:` comment).
- The §4.2 "Scanning header" (the screen opened while the scan still runs, from a notification) is not built. Its entry point belongs to Phase 9.

## Threat Flags
None. T-06-11: the cached, chunked fingerprint scan is unchanged, and it stops when the user leaves the waiting screen. T-06-12: path keys, Keep One tests, and the snapshot taken at tap time give an immutable confirmed request.

## Device steps for phase-end acceptance (06-11)
- Put a disposable duplicate pair on the phone (for example, copy one JPEG twice into `/sdcard/Pictures/ZZDupTest`). Open Explore → System Cleaner → Duplicate Images.
- **Waiting screen:** blue full screen, white spinner, "Scanning..." and the hint. Then `success` plays once with "Scanning Completed!", and the groups open as soon as it ends (no pause after it, no second play). Compare with `evidence/ref272-duplicate-waiting.png`. Back during the scan returns to System Cleaner.
- **Groups screen:**
  - "Group 1 (2 Images)" headers, 4 columns, rounded cells with the folder name caption and a check at the top end.
  - Keep One Photo is off on open. Turning it on checks all but the first photo of each group and shows "Delete Selected (N items)" with the AD badge. Turning it off clears the selection and hides the button.
  - Compare with `evidence/ref272-duplicate-photo.png` and `ref272-duplicate-keep-one.png` in light and dark.
  - The fast-scroll thumb drags. The entrance animation runs once.
  - With no duplicates, the empty copy is centred under `ic_empty_img`, with no flash of it before the first result.
- **Delete:** tap Delete Selected. The dialog reads "Are you sure you want to delete N items?" with Move to Recycle Bin checked; No deletes nothing.
  - Yes with the box checked moves the files to the Recycle Bin.
  - Yes with the box unchecked deletes them permanently. Test this on disposable files only.
  - Both open Cleanup complete. Until 06-06 there is no "Deleting" screen.
- **Locales and layout:** long locales (de, ru, ta) must not clip "Delete Selected (N items)" and the badge. Check the RTL mirror in ar (the check is at the top end).
- Remove `ZZDupTest` afterwards.

## Self-Check: PASSED
- FOUND: DuplicateWaitingScreen.kt, DuplicatePhotoScreen.kt, ic_ads_left.xml, DuplicateTest.kt
- FOUND commits: 8cb923c, 939253a, 4efff6b, 73565fe
