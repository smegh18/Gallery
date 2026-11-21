---
phase: 05-viewing-and-browsing
plan: 05
subsystem: browse
status: complete
tags: [other-albums, moments, animated, burst, VIEW-03]
requires: [05-04]
provides: [AllAlbumsScreen, AlbumSquareCard, rememberAlbumSelectionDialogs, SharedMediaToolbar, burstGroupKey, burstGroups, momentsState]
affects: [05-06, 05-07, phase-09]
tech-stack:
  added: []
  patterns:
    - "Album selection (Phase 4) is one composable pair, rememberAlbumSelectionDialogs + AlbumSelectionBottomBar(viewModel, ...), used by the Albums tab and Other Albums"
    - "Derived group ids (moment label, burst key) travel as filter:<kind>:<id> and are re-resolved against current rows by getMediaByFolderFlow; never used as file paths"
key-files:
  created:
    - app/src/main/java/com/quizango/gallery/ui/album/AllAlbumsScreen.kt
    - app/src/main/java/com/quizango/gallery/ui/browse/AlbumSquareCards.kt
    - app/src/main/java/com/quizango/gallery/ui/home/AlbumSelectionActions.kt
    - app/src/test/java/com/quizango/gallery/AllAlbumsTest.kt
    - app/src/test/java/com/quizango/gallery/DiscoveryBrowseTest.kt
    - app/src/test/java/com/quizango/gallery/BurstPhotoTest.kt
  modified:
    - app/src/main/java/com/quizango/gallery/ui/discovery/MomentsScreen.kt
    - app/src/main/java/com/quizango/gallery/ui/discovery/AnimatedMediaScreen.kt
    - app/src/main/java/com/quizango/gallery/ui/discovery/BurstPhotoScreen.kt
    - app/src/main/java/com/quizango/gallery/data/local/MomentScanner.kt
    - app/src/main/java/com/quizango/gallery/data/repository/MediaRepository.kt
    - app/src/main/java/com/quizango/gallery/ui/browse/SharedMediaList.kt
    - app/src/main/java/com/quizango/gallery/ui/home/HomeScreen.kt
    - app/src/main/java/com/quizango/gallery/ui/home/PhotosTab.kt
    - app/src/main/java/com/quizango/gallery/ui/navigation/Screen.kt
    - app/src/main/java/com/quizango/gallery/ui/navigation/GalleryNavHost.kt
    - app/src/main/res/values*/strings.xml
decisions:
  - "Burst cards use the single-cover face, not the 2x2 mosaic: 2.7.2's burst holder be/b shows mainLayout, hides otherLayout and albumCount (evidence below), so no N+ overlay is drawn"
  - "Burst groups follow 2.7.2's relativePath rule (s9/ao.OooOOOO) computed from the path, not a new DB column"
  - "Other Albums gets its own HomeViewModel instance (same factory), so its selection never mixes with the Albums tab and the route also works without Home on the back stack (Phase 9 deep links)"
  - "Animated stays on getAnimatedMediaFlow as the plan says; 2.7.2's AnimatedFindService scan (and its scanning header) has no counterpart in ours, so the shared frame's spinner is the only wait state"
requirements: [VIEW-03]
metrics:
  completed: 2026-11-21
actuals:
  tokens: 18000
  tasks: 3
  commits: 3
plan_head_before: c815bacf01d1620be3d8fb78217b4f02751f7287
plan_head_after: 34c81cf
---

# Phase 5 Plan 05: Other Albums, Moments, Animated and Burst Summary

Other Albums is a new routed 3-column album screen that reuses the Phase 4 album selection. Moments has the 2.7.2 toolbar, scanning header, Lottie wait and cards. Animated runs on the 05-04 shared list frame. Burst groups frames into per-burst album cards using the 2.7.2 grouping rule, and each card opens only its own frames.

## Done

### Task 1 (tracer): Other Albums, commit ac50674
- `AllAlbumsScreen` (activity_all_album, §5):
  - layout_toolbar titled `other_albums`
  - device folders only, in repository order, in 3 columns
  - list padding 2.sdp horizontal and 4.sdp vertical
  - cards with 8.sdp end and bottom margins
  - bare counts
  - a spinner until the first sync
  - when empty, `ic_empty_img` and "No items" only
  - ad rows are not added (D5)
- `AlbumSquareCard` (new `ui/browse/AlbumSquareCards.kt`): the item_grid_square_album single-cover face, also used as item_search_sub with `titleTop = 4.sdp`.
  - 10.sdp radius, centre-crop cover
  - 24.sdp `ic_check` / `ic_un_check` at the top end, 4.sdp margin
  - `BodyMediumMedium` 2-line title
  - `BodyXSmallMedium` `countTitle` count
- Album selection reuse:
  - The Phase 4 cover picker, rename (with its progress dialog), Info, album delete and album Add to (with Move/Copy) moved out of `HomeScreen` into `rememberAlbumSelectionDialogs(viewModel, uiState)`, with a wired `AlbumSelectionBottomBar(viewModel, uiState, open)` overload.
  - The Albums tab and Other Albums call the same `HomeViewModel` methods, so no mutation code was duplicated.
  - Home's media-selection dialogs are unchanged.
- `SharedMediaToolbar` is now internal with defaults. It takes an optional `onInfo` that shows `propertyBtn` (`ic_property`) while selecting; Other Albums passes it only when exactly one album is selected.
- `PhotosEmptyState` takes `body = null` for the heading-only empty state.
- Added `Screen.AllAlbums` ("all_albums") and its destination. There is still no in-app entry, as in 2.7.2; Phase 9 wires the notification and deep-link entries.

### Task 2: Moments and Animated, commit 197df0d
- Moments (`activity_moments`, §6):
  - `SharedMediaToolbar` titled `moment`.
  - momentsScanningLayout:
    - "Scanning Photos...." with `done/total`, as `MO` does
    - a determinate bar on the `seekbar_color_bg` track, in `colorPrimary`
    - the `scanning_time` hint
  - The header shows once the scan has a count (2.7.2 shows it only when max > 0 and progress > 0).
  - While nothing is found: the looping `moment_scanner_lottie` at 140.sdp with `moment_lottie_text`.
  - Cards: 3 columns of `AlbumSquareCard` as item_search_sub, list padding 2.sdp horizontal and 6.sdp vertical.
  - Empty: "Oops!" / `no_moment_files_found`.
- No empty flash:
  - `momentsState(groupCount, settled, scanning)` only returns EMPTY after the groups are rebuilt from the current photos and labels and no scan is running.
  - `MomentScanner.scan()` now reports `Progress(0, 0)` from the call itself and clears it in a `finally`, including the path where nothing needs labelling.
- Taps go through `momentRoute(label)`, which builds `filter:moment:<label>` (URL-encoded) and is titled with the label.
- Animated (§8):
  - `SharedMediaList` titled `animated`, on the shared persisted column count.
  - The six media-list slots via `rememberMediaListActions`.
  - The default empty copy: `no_items` / `no_media_file`.
  - The viewer source is `filter:animated`, so the viewer pages only through animated rows.
- Removed: Material `TopAppBar`, `ArrowBack`, `Videocam`, the GIF badge, `GridCells.Fixed(3)` and every English literal.
- Imported `moment`, `animated`, `oops`, `no_moment_files_found`, `scanning_photos`, `scanning_time` and `moment_lottie_text`, synced to 24 languages.

### Task 3: Burst, commit 34c81cf
- `burstGroupKey` ports 2.7.2's `relativePath` (S272/s9/ao.OooOOOO). The marker depends on the maker:
  - Xiaomi: `timeburst`
  - OPPO and Samsung: none
  - all others: `burst`
- The frame must be an image, in `DCIM/Camera` (`DCIM/Camera/Cshot` on OPPO), and its name must contain `_` and `.jpg`.
- The key is the path up to the last `_` (OPPO uses the parent folder name, Motorola the suffix). A `cover` frame drops one more `_` segment, so it joins its burst.
- `getBurstPhotosFlow` uses the key, matching the 2.7.2 query (`mediaType == 1 AND relativePath != ''`, newest first).
  - `filter:burst:<key>` re-resolves only that group's current frames.
  - `filter:burst` reuses the same flow.
- `burstGroups` follows bf/a: frames are grouped in query order, groups are sorted by their first frame's date, newest first (ax/u case 11), and frame order is kept.
- `BurstPhotoScreen`:
  - toolbar titled `burst`
  - 3 columns, list padding 4.sdp
  - cards titled "Burst N" (`getString(burst) + " " + position`), showing the first frame as cover and the bare frame count
  - a tap opens the media list `filter:burst:<key>` titled "Burst N"
  - the spinner while loading; `no_items` / `no_media_file` when empty
- Removed `Icons.Default.BurstMode`, the "BURST" badge and the "Burst Photos" / "No burst shots found" literals.

## Reference evidence: Burst card face (plan asked for this before encoding an N+ rule)
- `S272/be/b.smali` `HISPj7KHQ7(AlbumModel)` binds `z9/av` (item_grid_square_album, 0x7f0d010e). By constructor order, `DxDJysLV5r` = `mainLayout` and `BsUTWEAMAI` = `otherLayout`. The method:
  - sets `mainLayout` VISIBLE and `otherLayout` GONE
  - loads only `data[0]` into `thumbnail`
  - sets the title, and the count to `String.valueOf(data.size())`
  - sets `albumCount` GONE and the selection mark GONE
- AllAlbum's `bx/g.eyd3OXAZgV` binds the same single-cover face.
- No code sets the `albumCount` text. The "15+" in the layout is only the XML design-time sample.
- **Result:** 2.7.2 never shows the 2x2 mosaic or an overflow count on Burst. UI-SPEC §9 (mosaic) and the plan's must-have truth "2x2 mosaic cards with the dimmed fourth-tile count" do not match the decoded reference, so the reference was followed. `other_album_bg_color` and `other_album_trans` stay unused here.
- Burst grouping evidence: `bf/a.invokeSuspend` (LinkedHashMap by relativePath, `ax/u(0xb)` sort, `burst` + " " + n titles); `y9/r` SQL `SELECT * FROM images_table WHERE mediaType == 1 AND relativePath != '' ORDER BY lastDate DESC`; `bd/a` opens MediaListActivity with the title and path.

## Verification
- `GRADLE_USER_HOME="$HOME/.gradle" ./gradlew.bat :app:testDebugUnitTest -q` passes the full suite (39 result files, no failures).
- `:app:assembleDebug -q` passes.
- `python -I scripts/ref_res.py check-strings` passes.
- New tests:
  - **AllAlbumsTest** (3): device folders only, in order; loading vs empty; a tap opens the folder's AlbumDetail route.
  - **DiscoveryBrowseTest** (4):
    - the Moments WAIT/EMPTY/CONTENT states, including stale groups after the scan finishes
    - the scan-header gate
    - the encoded moment route
    - the six Animated slots and the `filter:animated` source
  - **BurstPhotoTest** (4):
    - the key rule, including cover, non-camera, non-JPEG, video, Xiaomi and OPPO
    - grouping order
    - the route plus the key filter returning only that burst's frames
- `MomentScannerTest` still passes, and the existing Phase 4 album-selection tests (AlbumSelectionTest, AlbumsTabTest) stay green after the HomeScreen extraction.

## Deviations from Plan

### Auto-fixed Issues

**1. [Rule 1 - Bug] Moments could flash "Oops!" before the scan reported progress**
- **Found during:** Task 2
- **Issue:** `MomentScanner` set its progress only after the saved labels loaded and the to-do list was built, and the groups were built asynchronously, so the screen could briefly look settled and empty.
- **Fix:** `scan()` marks the scan running from the call. Moments only shows EMPTY when the groups were built from the current photos and labels.
- **Files modified:** `MomentScanner.kt`, `MomentsScreen.kt`
- **Commit:** 197df0d

**2. [Reference over spec] Burst uses the single-cover face, with no mosaic and no N+ overlay**
- See the evidence above. The plan's 2x2 mosaic and overflow-count test were replaced with tests of the grouping and the group-scoped route.
- **Commit:** 34c81cf

**3. [Rule 3 - Reuse] Album-selection dialogs extracted from HomeScreen**
- Needed so Other Albums reuses the Phase 4 flow without copying it. Home's behaviour is unchanged.
- **Commit:** ac50674

**4. [Rule 3 - Blocking] Strings missing**
- `other_albums`, `moment`, `animated`, `burst`, `oops`, `no_moment_files_found`, `scanning_photos`, `scanning_time` and `moment_lottie_text` were not in `values/strings.xml`, although the UI-SPEC lists them as imported. They were imported from R272 and synced.

## Known gaps (not stubs)
- **Animated scanning header:** 2.7.2 runs `AnimatedFindService`, a background service that detects animated types item by item and drives `animatedScanningLayout` and its Lottie. Ours flags GIFs during the MediaStore sync, so the plan kept `getAnimatedMediaFlow`. The screen shows the frame's spinner, then the list or the empty copy; the scanning header never appears.
  - Motion-photo or animated-WebP detection would need that service ported, which is a data-layer feature for a later plan.
- **Other Albums contents:** shown in repository order with every device folder. 2.7.2's `AllAlbumViewModel` also reads a `myAlbums` preference and pages the DB 400 rows at a time; whether it excludes any folders was not traced.
- **Other Albums type badge:** `albumType` (`ic_common_fav`) is never referenced by `bx/g`, so no badge is drawn.

## Not verified (device disconnected): phase-end device check
1. **Other Albums:** navigate directly to the `all_albums` route in a debug run (no in-app entry exists).
   - The 3-column cards show bare counts, and a tap opens the folder's media list.
   - Long-press shows the close / count / Select all toolbar with Info when one album is selected, and the 5-slot album bar.
   - Pin, Add to (Move/Copy), Delete, Change cover and Rename work as on the Albums tab.
   - Back clears the selection first.
2. **Albums tab regression:** the same album selection flows still work after the extraction, including Info, the rename progress dialog and the toasts.
3. **Moments** (from Explore):
   - On the first run, the Lottie and "Kindly wait..." show, with no "Oops!" flash.
   - During a scan, the header shows `done/total` and the bar fills.
   - The cards fill in as moments are found.
   - With no moments, "Oops!" / "No moment files here." shows.
   - A tap opens the media list titled with the label, and the viewer pages only within it.
4. **Animated** (route `media/animated`): with and without GIFs:
   - the spinner, then the grid on the shared column count (pinch changes it)
   - the six selection slots
   - the viewer pages only through animated items
   - "No items" / "No media files here." when empty
5. **Burst** (route `media/burst`), with a disposable burst on the Redmi (MIUI names frames `..._TIMEBURST`):
   - The cards read "Burst 1..N", newest first, with the first frame as cover.
   - Opening a card lists only that burst's frames.
   - Compare with 2.7.2: it should also show single-cover cards, not a mosaic.
6. **Dark mode and RTL:** the toolbar divider and the `countTitle` colour; the cards mirror.

## Threat model
- **T-05-07:** moment labels and burst keys are URL-encoded into `filter:moment:` and `filter:burst:` ids. `getMediaByFolderFlow` resolves them by comparing them with labels and keys derived from current rows, and never opens them as paths.
- **T-05-08:** Other Albums calls the same `HomeViewModel` album methods as the Albums tab. Those methods act only on the selected repository album paths and keep the same-folder guard and generation checks.

## Self-Check: PASSED
- Files exist: `AllAlbumsScreen.kt`, `AlbumSquareCards.kt`, `AlbumSelectionActions.kt`, `AllAlbumsTest.kt`, `DiscoveryBrowseTest.kt` and `BurstPhotoTest.kt`.
- Commits ac50674, 197df0d and 34c81cf are ancestors of HEAD.
