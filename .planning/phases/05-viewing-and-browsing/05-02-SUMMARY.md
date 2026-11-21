---
phase: 05-viewing-and-browsing
plan: 02
subsystem: viewer
status: complete
tags: [viewer, dock, dialogs, slideshow, VIEW-01]
requires: [05-01]
provides: [viewer-dock, viewer-more-card, viewer-dialogs, viewer-slideshow, MediaRepository.rotateImage]
affects: [05-03, 05-06]
tech-stack:
  added: []
  patterns: [dialog state captures the page path and re-checks it on confirm]
key-files:
  created:
    - app/src/main/java/com/quizango/gallery/ui/viewer/ViewerDialogs.kt
  modified:
    - app/src/main/java/com/quizango/gallery/ui/viewer/MediaViewerScreen.kt
    - app/src/main/java/com/quizango/gallery/ui/viewer/ViewerChrome.kt
    - app/src/main/java/com/quizango/gallery/data/repository/MediaRepository.kt
    - app/src/main/java/com/quizango/gallery/ui/home/HomeDialogs.kt
    - app/src/main/java/com/quizango/gallery/ui/home/HomeSelection.kt
    - app/src/test/java/com/quizango/gallery/MediaViewerTest.kt
    - app/src/main/res/values*/strings.xml
decisions:
  - "Viewer dialogs reuse the Phase 3/4 surfaces (dialog_hide, dialog_delete, dialog_rename, property card) with the reference copy instead of new composables"
  - "Rename failure shows something_went_wrong; success shows nothing (reference has no success toast)"
  - "Wallpaper and Print stay hidden on video pages (both decode a still image); Slide show is hidden in the vault"
  - "Slide show music does not exist in our app; the slide-show mute button mutes video pages instead"
requirements: [VIEW-01]
metrics:
  completed: 2026-11-21
actuals:
  tokens: 27000
  tasks: 3
  commits: 3
plan_head_before: cd54a213b80d964fe58e71c003da5cf4c93510f8
plan_head_after: b99dafb9e507f51a1fbc0256079200ae3baac069
---

# Phase 5 Plan 02: Viewer dock, actions and slide show Summary

The viewer now has the 2.7.2 icon-only pill dock with a row for each mode, the More card, 1500 ms edit suggestions, and guarded file actions that re-check their page before running. Slide show now runs inside the viewer: it starts on the current page, turns every 2 s, and stops on the last page.

## Done

### Task 1: More card and Rename (tracer), commit 6327ac7
- The `popmenu_media_more` card replaces the Material dropdown:
  - `dialogBack` background, radius 16.sdp, 160.sdp wide.
  - Anchored above the dock and aligned to its end edge (on the start edge in RTL).
  - Rows in reference order: Add to, Rename, Slide show, Set as Wallpaper, Print, then Show on map only when the item has a place.
  - The vault (type 2) drops Add to and Show on map.
- Rename reuses `RenameAlbumDialog` (the reference uses one `ci/h` dialog for albums and files), with a new `ext` parameter:
  - The 50-character limit carries over (`if-lt 0x32`).
  - A name that is already taken shows "File name already exists.".
  - A successful rename shows no toast, and a failed one shows `something_went_wrong`.
- `showOnMapRoute` sends the user to the existing `filter:place` list when the item has a `placeName`. It needs no Maps key; 05-06 replaces the target.
- `MediaRepository.renameMedia` now:
  - refuses an existing target;
  - moves the Room row to the new path, so the favourite flag survives;
  - asks MediaStore to scan both the old and the new path before syncing.
- Imported 16 reference string keys and synced them to 24 languages.

### Task 2: Dock, suggestions, dialogs and actions, commit 1f9893e
- `ViewerDock` (`bg_media_full_pill_dock`) has equal icon-only slots tinted `media_full_glass_icon`, with TalkBack labels. The rows per mode:

  | Mode | Slots |
  |---|---|
  | Normal photo | Share · Private · Edit · Delete · More |
  | Normal video | Share · Private · Add to · Delete · More |
  | Recycle Bin | Restore · Delete |
  | Vault | Share · Unhide · Delete · More |
- `SuggestionStrip` uses `edit_opt_back` pills:
  - The close pill comes first, then the pills in reference order. The Blur pill uses the `blur_background_guide` label.
  - It appears 1500 ms after a still page settles.
  - It hides immediately when the chrome hides or the page scrolls. ✕ closes it for the current page only.
- Dialogs go through `ViewerDialogHost`. Each one stores the path of the page it was opened on, and every confirm re-checks it with `targetIfCurrent`. The dialog also closes when the page changes. Mapping to the reference dialogs:

  | Viewer action | Dialog |
  |---|---|
  | Delete | `DeleteMediaDialog`, 1 item with the "Move to Recycle Bin" checkbox |
  | Vault delete | `deleted_item_permission`, no checkbox |
  | Recycle Bin delete, Restore, Hide, Unhide | the `dialog_hide` surface (`oa/n`) with `delete_items`, `restore_items`, `hide_media` or `unhide_items` copy |
  | Add to | the album sheet, then the Move/Copy dialog |
  | Details | the property card, now shared with `AlbumPropertyDialog` as `PropertyDialog` |
- Edit refuses animated images with `toast_animated_image` and images it cannot decode with `toast_corrupted_image`.
- Rotate now writes to the file instead of only turning the view. New `MediaRepository.rotateImage`:
  - JPEG: steps the EXIF orientation.
  - PNG / WebP: re-encodes to a temporary file, then swaps it in.
  - Then it rescans. The page reloads through a request version, and a failure shows `couldn_t_rotate_image`.
- Share uses an image or video MIME type and no English chooser title. Print uses the PrintHelper `Uri` overload, which decodes off the main thread.
- A spinner shows while a file action runs.
- Removed:
  - the English toasts: moved, deleted, restored, renamed, hidden, "Added to X";
  - the 2.6.7 surface bottom bar, dropdown, AlertDialogs and folder-icon album grid;
  - `Icons.Outlined.Folder`.

### Task 3: Slide show, commit b99dafb
- More → Slide show stays inside the viewer and no longer opens Stories. What it does:
  - Hides the chrome and turns off swipes and chrome taps.
  - Keeps the screen on.
  - Turns every 2 s (`slideShowInterval` = 2).
  - The duration text shows "2s", and tapping it cycles 1 to 5 s, as in MF.
  - It stops on the last page and shows the chrome again.
- Controls: 28.sdp `ic_slide_control_back` circles for Close, Mute and the duration.
- One `LaunchedEffect` timer drives it. It stops on Close, Back, `ON_STOP`, or any change to the media list. Suggestions are off while it runs.
- Recycle Bin, vault and undecodable pages cannot start it.

## Verification
- `GRADLE_USER_HOME="$HOME/.gradle" ./gradlew.bat :app:testDebugUnitTest -q` passes. `MediaViewerTest` adds tests for:
  - More order
  - dock rows per mode
  - suggestion eligibility and delay
  - the dialog page guard
  - the 50-character rename limit and kept extension
  - edit toasts
  - Show on map
  - slide show interval, end and eligibility
- `./gradlew.bat :app:assembleDebug -q` passes.
- `python -I scripts/ref_res.py check-strings`: 24 languages up to date.

## Not verified (device)
- **No device was attached**: `adb devices` was empty and mDNS found nothing, so no on-device check ran. Still to check on the Redmi:
  - the More card against `ref272-viewer-more-popup.png`;
  - renaming a disposable `/sdcard/Pictures/ZZ*` photo;
  - Rotate on a JPEG and a PNG;
  - the normal, Recycle Bin and vault rows;
  - suggestion timing;
  - the Details card;
  - the slide show (2 s turns, stops at the end, nothing advances after Close or background);
  - the dock with RTL and long translations.
- The pager under a modal dialog cannot be swiped by touch, so only the page-guard logic was tested; a test that changes the list while a dialog is open was not run.
- Lifecycle cancellation (`ON_STOP`, list change) has no Compose UI test; it is checked only by code review.

## Deviations from Plan

### Auto-fixed Issues
1. **[Rule 1 - Bug] File rename lost favourites and MIUI indexing.** `renameMedia` only called `syncMediaStore`. That sync dropped the old row and its favourite flag, and on MIUI the renamed file was never indexed. The row now moves to the new path and both paths are scanned (6327ac7).
2. **[Rule 1 - Bug] A stale fallback page appeared after a rename.** The viewer added the opened path back as an extra page once that path had left the list. It now does this only while the file still exists (6327ac7).
3. **[Rule 1 - Bug] Rotate turned only the view.** Spec §1.9 asks for a file rotation. Added `rotateImage` (1f9893e).

### Reference reads that settled open points
- Slide show options: `slideShowInterval` starts at 2. Tapping the duration does +1 up to 5, then wraps to 1. The last page stops the show. The reference also plays background music through a `MediaPlayer` set from outside MF, but we have no music, so Mute applies to video pages.
- More rows per type, from MF around line 15460: type 2 (vault) hides Add to and Show on map and keeps Rename. Slide show is still hidden in the vault, as this plan requires.

### Judgement calls
- Wallpaper and Print are hidden on video pages, against the spec's "always", because both would silently do nothing on a video.
- The vault delete dialog hides the "Move to Recycle Bin" checkbox, because vault files have no Recycle Bin.
- Delete, Restore, Hide and Unhide still close the viewer afterwards, as ours did before. Whether 2.7.2 shows the next page instead was not checked on a device.

## Known Stubs
- `MediaViewerScreen` `onCapture = { }` (top-bar Capture) and the inline video controls, which stay as before above the dock. Both belong to 05-03 (inline short video).

## Threat Flags
None. The file mutations stay on the existing repository paths, and the new rotate writes through a temporary file plus rename.

## Self-Check: PASSED
- Commits 6327ac7, 1f9893e and b99dafb are on the branch.
- `ViewerDialogs.kt` exists.

## Not updated here
- STATE.md, ROADMAP.md and REQUIREMENTS.md were not touched in this worktree; the orchestrator owns them during parallel waves.
