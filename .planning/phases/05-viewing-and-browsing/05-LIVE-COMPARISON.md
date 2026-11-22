# Phase 5 Live Comparison (viewing and browsing)

Status: **partial. Automated gates ran; every device result is PENDING-DEVICE.** No adb device was attached and the Artemis MCP was disconnected when plan 05-08 ran (2026-11-23). Nothing below was observed on a device. Do not copy any PENDING-DEVICE field to a pass, and do not tick the parity checklist, until a device run fills it in.

Reference: Gallery 2.7.2 (APK on the device). Reconstruction: `app/build/outputs/apk/debug/app-debug.apk`, package `com.quizango.gallery`. UI-SPEC: `05-UI-SPEC.md`.

## 1. Automated gates (run, not pending)

| Gate | Command | Result |
|------|---------|--------|
| Unit suite | `./gradlew.bat :app:testDebugUnitTest -q` | pass: 44 result files, 242 tests, 0 failures, 0 skipped (after fix 08a85d3) |
| Debug build | `./gradlew.bat :app:assembleDebug -q` | pass (exit 0) |
| Strings | `python -I scripts/ref_res.py check-strings` | pass: 24 languages up to date |
| Colors | `python -I scripts/ref_res.py check-colors` | pass: 213 light / 129 night names match 2.7.2 |
| CodeRabbit | not run: the CLI is not installed on this machine | see section 2 |

## 2. Review and defect record

| # | Defect | Reproducer | Owning plan | Failing gate | Expected result | Status |
|---|--------|-----------|-------------|--------------|-----------------|--------|
| D1 | Route arguments were decoded twice: androidx.navigation 2.8.8 already percent-decodes path and query arguments (`NavDeepLink` calls `Uri.decode` / `Uri.getQueryParameters`), then the app ran `URLDecoder.decode` again | Search `c++` then View All: query becomes `c  `. Open a photo named `50% off.jpg`: `URLDecoder` throws in the viewer route. A file named `%41.jpg` opens `A.jpg` | 05-01 (viewer, AlbumDetail), 05-03 (player), 05-06 (Place), 05-07 (View All, Color) | `RouteArgsTest`, `BrowseToViewerTracerTest` | Every awkward name round-trips through route build then the navigation decode, and the viewer opens the tapped page | Fixed in 08a85d3. Device recheck PENDING-DEVICE |

Manual audit of the Phase 5 diff (`53df7e8..HEAD`, `app/src/main/java`), in place of CodeRabbit:

| Check | Finding | Disposition |
|-------|---------|-------------|
| Hard-coded user-facing strings in `ui/viewer`, `ui/browse`, `ui/discovery`, `ui/search`, `ui/favorites` | Only `OrganizerRecycleScreen.kt` (Phase 6) and numeric formats (`2s`, `done/total`) | Phase 6 file out of scope; numeric formats are not copy |
| Broad exception swallowing added in the diff | `MediaViewerScreen` Print and `VideoPlayback.isPlayableVideo`, `captureFrame` / `rotateImage` in `MediaRepository` | Accepted: each turns the failure into a toast or a `false` result the UI reports; none hides data loss |
| Stub or appearance-only controls | `VideoPlayerScreen` Floating button is a no-op | Accepted: Phase 10 owns it (UI-SPEC 2.2). Top-bar Capture is now wired (no `onCapture = { }` left) |
| Fake data, TODO, FIXME, placeholder text | None in the Phase 5 files | n/a |
| Dead compatibility branches | None found | n/a |
| Same double-decode pattern outside Phase 5 (`Editor`, `StoryView`, `Wallpaper`, vault and organizer routes in `GalleryNavHost.kt`) | Same defect class | Deferred: not Phase 5 routes. See 05-08-SUMMARY.md |

## 3. Row traceability (baseline rows #13, #14, #16-#24)

Result column for every row: **PENDING-DEVICE**.

| Row | Screen | Reconstruction route | Reference evidence / observed flow | Fixtures | Automated gate | Device states to compare | Result |
|-----|--------|---------------------|-----------------------------------|----------|----------------|--------------------------|--------|
| #13 | Full-screen viewer (MediaFullScreenActivity) | `viewer?path=&folder=&isRecycle=&isVault=` | `evidence/ref272-viewer-photo.png`, `evidence/ref272-viewer-more-popup.png`; UI-SPEC 1 | Portrait, landscape and GIF photos; a geotagged photo; a photo with `%`, `+` and `&` in its name; recycle and vault items | `MediaViewerTest`, `ViewerZoomTest`, `BrowseToViewerTracerTest` | normal, recycle, vault; glass chrome; icon-only dock; per-image zoom; suggestions delay; More, dialogs; 2 s slideshow; Show on map | PENDING-DEVICE |
| #14 | Video player (VideoPlayerActivity) and inline short video | `video?path=&folder=...` and the inline player in `viewer` | `evidence/ref272-viewer-short-video-paused.png`, `evidence/ref272-viewer-short-video-playing.png`; UI-SPEC 1.8 and 2 | A video of about 24 s; a video over 60 s; one with two audio tracks; one without subtitles; a corrupt video | `VideoPlayerTest`, `MediaViewerTest` | short video paused to playing, seek, mute, speed, capture, end, swipe away; long video playlist, previous/next, repeat, tracks, rotate, lock, brightness/volume, background, close; audio stops on ownership change | PENDING-DEVICE |
| #16 | Media list (MediaListActivity) | `album/{bucketId}/{bucketName}` | UI-SPEC 3 and 4 | A folder with a few items; a large folder | `SharedMediaListTest`, `AlbumDetailTest` | pinch 3/4/5 columns, selection and drag, fast scroll, spinner then data, no "No items" flash, entrance animation | PENDING-DEVICE |
| #17 | Other Albums (AllAlbumActivity) | `all_albums` (no in-app entry; use the route) | UI-SPEC 5 | Several device folders | `AllAlbumsTest`, `AlbumSelectionTest`, `AlbumsTabTest` | cards and counts, album selection bar, Pin, Add to, Delete, Change cover, Rename, Back clears selection | PENDING-DEVICE |
| #18 | Moments (MomentsActivity) | `moments` | UI-SPEC 6 | Photos that the on-device labeler tags | `DiscoveryBrowseTest`, `MomentScannerTest` | first-run scan header with no "Oops!" flash, cards fill in, empty state, card opens a titled list | PENDING-DEVICE |
| #19 | Place (MapActivity) | `place?key={key}` | UI-SPEC 7 | Geotagged photos in more than one place; photos with no location | `PlaceScreenTest`, `ExploreTabTest`, `MediaViewerTest` | no-key blank map with usable sheet; permission denied, approximate, precise; marker tap and sheet sync; viewer Show on map | PENDING-DEVICE |
| #20 | Animated (AnimatedMediaListActivity) | `media/animated` | UI-SPEC 8 | At least one GIF, and a library without GIFs | `DiscoveryBrowseTest` | spinner then grid, six selection slots, viewer pages only animated items, empty copy | PENDING-DEVICE |
| #21 | Burst (BurstPhotoActivity) | `media/burst` | UI-SPEC 9 | A disposable burst (MIUI `..._TIMEBURST` frames) | `BurstPhotoTest` | single-cover cards "Burst 1..N", a card lists only its frames | PENDING-DEVICE |
| #22 | Favorites (FavoriteActivity) | `favorites` | UI-SPEC 10 | Two disposable favourites | `FavoritesTest`, `SharedMediaListTest` | four-slot bar, unfavourite prunes at once, "No Favorites" empty state, viewer pages only favourites, no pinch tutorial | PENDING-DEVICE |
| #23 | Search and View All (SearchActivity, SearchViewAllActivity) | `search`, `search/all?section=&query=` | UI-SPEC 11 | An album, a place and a moment that share a word; a section with more than six matches; a query containing `+` and `%` | `SearchScreenTest`, `RouteArgsTest` | Colors row, previews filter as you type, View All shows every match and Back keeps the query, no keyboard resize, no "Oops!" flash | PENDING-DEVICE |
| #24 | Search by color (SearchPhotoColorActivity) | `search/color?color=` | UI-SPEC 12 | Photos in the ten swatch colors | `SearchColorTest` | swatch and Choose color Ok open Media Color, four actions only, pager limited to that color, custom non-swatch color shows the empty state | PENDING-DEVICE |

## 4. State matrices

### 4.1 Viewer variants (#13, #14)

| State | Rows to exercise | Result |
|-------|------------------|--------|
| normal | dock, More card, rename, Rotate, slideshow, Show on map | PENDING-DEVICE |
| recycle | Restore and Delete only, no edit actions | PENDING-DEVICE |
| vault | no Add to, no Show on map, no slideshow, vault delete dialog without the Recycle Bin box | PENDING-DEVICE |

### 4.2 Video length (#14)

| State | Rows to exercise | Result |
|-------|------------------|--------|
| short video (60 s or less) | paused pill, play, seek, mute, five speeds, Capture, ends and stops, Play restarts | PENDING-DEVICE |
| long video (over 60 s) | centre button opens the player, previous/next toasts at the ends, repeat cycle persists, rotate, lock, brightness/volume, 3 s auto-hide | PENDING-DEVICE |
| audio ownership | audio stops on page change, Home, Back and close in both players; brightness and orientation restored after the player | PENDING-DEVICE |

### 4.3 Library size

| State | Rows to exercise | Result |
|-------|------------------|--------|
| small library | every list reaches its populated and its empty state | PENDING-DEVICE |
| large library | fast scroll in Media list, scan progress in Moments, Search typing latency, no jank on pinch | PENDING-DEVICE |

### 4.4 Theme and locale

| State | Rows to exercise | Result |
|-------|------------------|--------|
| light | all eleven rows | PENDING-DEVICE |
| dark | headers white, divider `#30FFFFFF`, dock and More card, Search toolbar | PENDING-DEVICE |
| German | long dock labels, toolbar titles | PENDING-DEVICE |
| Russian | long dock labels, toolbar titles | PENDING-DEVICE |
| Tamil | glyph height and line clipping in cards and dock | PENDING-DEVICE |
| Arabic RTL | toolbars mirror, tap and drag selection hit the right cell, dock order, seek bar direction | PENDING-DEVICE |

### 4.5 Place (#19)

| State | Rows to exercise | Result |
|-------|------------------|--------|
| permission denied | stripe shows, no location read, non-location browsing unaffected; deny again opens settings with the toast | PENDING-DEVICE |
| permission approximate | coarse grant removes the stripe, My Location dot appears | PENDING-DEVICE |
| permission precise | fine grant removes the stripe, My Location dot appears | PENDING-DEVICE |
| Place without a Maps key (mandatory) | no crash, blank map region of stable size, sheet loads, cards open scoped media | PENDING-DEVICE |
| live map, conditional | only if the developer supplies `MAPS_API_KEY` in `local.properties`: tiles, photo markers with name, marker tap animates the camera and scrolls the sheet. Key absent is an accepted state and blocks nothing | PENDING-DEVICE (conditional, not supplied) |

### 4.6 Ad slots

| State | Rows to exercise | Result |
|-------|------------------|--------|
| collapsed ad slots | every reserved ad slot in the eleven rows stays collapsed (no gap, no placeholder) | PENDING-DEVICE |

## 5. Parity checklist

`.planning/milestones/v2.7.2-parity/PARITY-CHECKLIST.md` is **unchanged**. Rows 13, 14 and 16-24 stay Ported unchecked until every PENDING-DEVICE field above passes on a device. Verified stays unchecked for Phase 13.
