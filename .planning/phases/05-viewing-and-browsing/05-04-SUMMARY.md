---
phase: 05-viewing-and-browsing
plan: 04
subsystem: browse
status: complete
tags: [media-list, favorites, shared-frame, selection, VIEW-03]
requires: [05-03]
provides: [SharedMediaList, MediaSelectionBar, MediaAction, rememberMediaListActions, listStateOf]
affects: [05-05, 05-06, 05-07]
tech-stack:
  added: []
  patterns:
    - "One list frame for every ka/d screen: hosts pass items (null while loading), the selection and a slot list; the frame owns toolbar, grid, pinch, range selection and states"
    - "Bulk actions act on the items selected when the slot was tapped, never on list indices"
key-files:
  created:
    - app/src/main/java/com/quizango/gallery/ui/browse/SharedMediaList.kt
    - app/src/main/java/com/quizango/gallery/ui/browse/MediaListActions.kt
    - app/src/test/java/com/quizango/gallery/SharedMediaListTest.kt
  modified:
    - app/src/main/java/com/quizango/gallery/ui/album/AlbumDetailScreen.kt
    - app/src/main/java/com/quizango/gallery/ui/favorites/FavoritesScreen.kt
    - app/src/main/java/com/quizango/gallery/ui/home/PhotosTab.kt
    - app/src/main/java/com/quizango/gallery/ui/home/HomeSelection.kt
    - app/src/main/java/com/quizango/gallery/ui/home/SharedMediaGrid.kt
    - app/src/main/java/com/quizango/gallery/ui/navigation/GalleryNavHost.kt
    - app/src/test/java/com/quizango/gallery/AlbumDetailTest.kt
    - app/src/test/java/com/quizango/gallery/FavoritesTest.kt
    - app/src/main/res/values*/strings.xml
decisions:
  - "The frame reads and writes mediaColCount through the app's single AppPreferences (GalleryApplication.preferences), so a pinch in any list or in Photos moves all of them"
  - "Taps and long-press/drag selection are detected on the grid, not on each cell, so a long press never also toggles on release; cells expose click/long-click through semantics for TalkBack"
  - "HomeSelectionBottomBar now renders through MediaSelectionBar, so Home and the lists share one slot implementation"
  - "Header dates use dd MMM yyyy in Locale.ENGLISH, as ka/d does, not the Photos tab's locale pattern"
  - "List results toast with the 2.7.2 selected_media_files_successfully_* copy; favourite toggles show no toast (ML/FA never reference the favourite toasts)"
requirements: [VIEW-03]
metrics:
  completed: 2026-11-21
actuals:
  tokens: 29000
  tasks: 3
  commits: 4
plan_head_before: de6ef58d46d72cf2ac07ae115ca996e86ecf9d49
plan_head_after: 66e03c80462f00522bc2565d0be1eddd53a2de8b
---

# Phase 5 Plan 04: Shared media-list frame, Media list and Favorites Summary

The 2.7.2 shared list (`ka/d`) is now one Compose frame, `SharedMediaList`. Media list (`AlbumDetailScreen`, every folder and `filter:*` list) and Favorites run on it, with their exact selection slots and real file, vault and favourite operations. Animated and Search colour can adopt it next without copying any grid code.

## Done

### Task 1 (tracer): the frame and Media list, commit 4cbd5fa
- `SharedMediaList` (§3):
  - `layout_toolbar`: 56.dp with a 1.dp `actionbarDividerColor` divider, a 2.sdp shadow and padding 4.sdp at the start and 10.sdp at the end.
    - Normal mode: back, then the title.
    - Selection mode: close, then "%d item(s) selected", then Select all / Unselect all.
  - Grid:
    - Padding 2.sdp horizontal and 4.sdp vertical.
    - Square cells with 0.5.dp margins.
    - The `ic_media_video` and duration row, the GIF/RAW/SVG label, the `ic_common_fav` mark and `ic_check` / `ic_un_check`.
    - Day headers via `buildPhotoRows(withAd = false)`: Today, Yesterday or `dd MMM yyyy`, `BodySmallMedium` in `black`.
  - Columns: 3..5 from the shared `mediaColCount`. Pinch uses the Photos step logic, now extracted as `Modifier.pinchColumnSteps`, and the same 200 ms placement morph.
  - Also: the Phase 3 `FastScrollThumb` (made internal) and the fall-down entrance on the first fill.
  - States: a spinner until both the first query and the saved column count arrive, then the per-screen empty copy through `PhotosEmptyState` (now parameterised).
  - The pinch tutorial overlay shows when `showPinchTip` is set. Media list sets it; Favorites does not, since `activity_favorite` has no stub.
- `MediaListActions.kt`:
  - `MediaAction` defines each slot's icon, label and Phase 3 width. `mediaListActions(allFavorite)` returns six slots and turns Favorite into Unfavorite when every selected item is a favourite.
  - `MediaSelectionBar` draws exactly the listed slots, and Home's bar delegates to it.
  - `rememberMediaListActions` handles the operations:
    - It snapshots the selected items at tap time.
    - It runs Share, Favorite/Unfavorite, Add to (`AddToAlbumSheet` → `dialog_select_add`), Add to Private (`HideMediaDialog` → `moveToVault`), Delete (`DeleteMediaDialog`, Recycle Bin or permanent), and Create album (`CreateAlbumDialog` → `dialog_select_add`).
    - A move into the folder that already holds the items is refused with `media_files_should_not_move_to_same_folder`.
    - Operations run in the screen's scope, so closing the selection does not cancel them.
- `AlbumDetailScreen` went from 517 to 52 lines. A tap opens `Screen.MediaViewer.createRoute(path, bucketId)`, so the viewer pages only through this list. The organizer stripe slot stays empty for Phase 6.
- Imported `unfavorite`, `no_favorites`, `no_favorites_empty` and `selected_media_files_successfully_{hide,copied,moved}`, synced to 24 languages (`check-strings` passes). The English `selected_media_files_successfully_deleted` now reads "Media files deleted successfully.", as in 2.7.2.

### Task 2: Favorites, commit 1174162
- `FavoritesScreen` runs on the frame:
  - title `favorites`
  - empty copy `no_favorites` / `no_favorites_empty`
  - slots Share, Unfavorite, Add to Private, Delete
  - viewer source `filter:favorites` (reuses `ALBUM_FAVORITES`)
- `GalleryNavHost` passes `VaultRepository`, so Add to Private does the real vault move after `dialog_hide`.
- Unfavourited or deleted rows drop out of `getFavoritesFlow`, and the frame removes them from the selection on the same emission (`pruneSelection`). The selection is then cleared, so no stale index is left.
- Removed:
  - the fixed 4-column grid
  - the `Icons.Default.Favorite` / `PlayArrow` glyphs
  - the literal "Remove", "No" / "Yes" and "N Selected" text
  - the English toasts
  - the duplicate share helper

### Task 3: hardening, commit 37475fb (plus 66e03c8, a stale comment)
- Keys are file paths for media and `header_<day>` for headers, and the tests confirm they are unique and stable.
- Back clears the selection first (`BackHandler` while selecting).
- Select all / Unselect all is derived from the pruned selection.
- Drag ranges are inclusive in either direction and clamped to the current list (`rangePaths`).
- The hit test accounts for content padding and RTL, using LazyGrid's mirrored offsets (checked against the foundation 1.10.4 source).
- For large lists, rows are built on `Dispatchers.Default`, and range selection uses remembered display-order paths instead of mapping the list on every move.

## Verification
- `GRADLE_USER_HOME="$HOME/.gradle" ./gradlew.bat :app:testDebugUnitTest -q` passes (full suite).
- `:app:assembleDebug -q` passes.
- New and updated tests:
  - **SharedMediaListTest** (6 tests):
    - day grouping with no ad row
    - Today, Yesterday and `dd MMM yyyy` labels
    - the pinch one-step rule within 3..5
    - Favorite / Unfavorite slots
    - Select all after list changes
    - range clamping
    - key uniqueness and header span
  - **FavoritesTest** (4 tests, replacing tautological ones):
    - the four slots in order, with no Add to or Create album
    - unfavourite pruning
    - LOADING vs EMPTY
    - the viewer source
  - **AlbumDetailTest**: a viewer route test (path and folder encoded).
- `python -I scripts/ref_res.py check-strings` passes.

## Deviations from Plan

### Auto-fixed Issues

**1. [Rule 1 - Bug] The 2.7.2 deleted-toast copy was wrong in English**
- **Found during:** Task 1
- **Issue:** `selected_media_files_successfully_deleted` read "Selected files successfully deleted", while 2.7.2 says "Media files deleted successfully." The translations were already the 2.7.2 ones.
- **Fix:** The English value now matches. This also changes the Large videos and Screenshots cleaner toasts that use the key.
- **Commit:** 4cbd5fa

**2. [Rule 3 - Reuse] Preferences instance**
- **Issue:** The plan said `AppPreferences(applicationContext)`.
- **Fix:** The frame uses `GalleryApplication.preferences`, the same single DataStore, without a second wrapper.
- **Commit:** 4cbd5fa

**3. [Rule 3 - Reuse] Home selection bar**
- **Issue:** `HomeSelectionBottomBar` duplicated the slot layout that the shared bar needed.
- **Fix:** It now renders through `MediaSelectionBar`. Home's behaviour is unchanged: it still always shows Favorite.
- **Commit:** 4cbd5fa

None of these change the plan's scope.

## Known gaps (not stubs)
- **Progress dialogs:** the "Deleting" / "Copying" / "Moving" progress dialogs from §3.4 are not shown, and Home has none either. Operations run in the background and finish with the toast. Add them when a shared progress dialog is ported.
- **Drag-select edge auto-scroll:** not built. A `shortcut:` comment in `SharedMediaList.kt` marks it.
- **Entrance animation:** `R272/anim/item_animation_fall_down.xml` is missing from the decoded resources. The spec's values (fade, 1.05 → 1.0 scale, 150 ms, 15 % stagger) are used, plus a -20 % drop that was assumed. Compare on the device.
- **Toolbar:** the old AlbumDetail search icon was removed, because 2.7.2's media-list toolbar has none.

## Not verified (device was disconnected): phase-end device check
1. Open a device folder from Albums:
   - The toolbar and headers (Today, Yesterday, `dd MMM yyyy`) match 2.7.2, with 0.5.dp gaps and the video duration and favourite marks.
   - The spinner shows first, never "No items".
2. Pinch 3 → 4 → 5 → 3 in the folder, then open Photos and Favorites. All three use the same count.
3. Fast scroll a large folder: the thumb follows the scroll and dragging it jumps the list.
4. Selection:
   - Long-press, drag across several rows: the range selects.
   - Release: the item is not toggled off.
   - Tap: toggles an item.
   - Back: clears the selection before leaving.
   - Select all / Unselect all: the label is correct.
5. Tap an item in a folder: the viewer pages only within that folder. From Favorites it pages only through the favourites.
6. Media-list slots:
   - Share
   - Favorite, which turns into Unfavorite when everything selected is a favourite
   - Add to: Move and Copy work, and a move into the same folder is refused
   - Add to Private: dialog, then vault
   - Delete: Recycle Bin and permanent
   - Create album: copies the selection into the new album
   - The toasts use the 2.7.2 copy.
7. Favorites:
   - Favourite two disposable items, unfavourite one in selection mode: it disappears at once.
   - The four-slot bar shows.
   - After removing both, "No Favorites" and the body copy show.
8. Pinch tutorial: it shows once on the media list when `isPinchInOutToolTip` is false, and never on Favorites.
9. RTL (ar): the toolbar mirrors, and tap and drag selection hit the right cell.
10. Dark mode: the headers turn white and the divider is #30FFFFFF.
11. Entrance animation: compare it with 2.7.2 side by side.

## Threat model
- **T-05-05:** every destructive or bulk action takes an immutable item snapshot, goes through the reference confirmation, and keeps the same-folder guard. No action targets an index.
- **T-05-06:** the grid is lazy with stable keys, grouping runs off the main thread, and the fast-scroll thumb is reused. The device check is still pending.

## Self-Check: PASSED
- Files exist: `ui/browse/SharedMediaList.kt`, `ui/browse/MediaListActions.kt` and `SharedMediaListTest.kt`.
- Commits 4cbd5fa, 1174162, 37475fb and 66e03c8 are ancestors of HEAD.
