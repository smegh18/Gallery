# Phase 5 Live Comparison (viewing and browsing)

Status: **device pass run 2026-11-23 on the Redmi M2006C3MII (Android 11, MIUI), side by side with 2.7.2 on the same phone.** Totals over the 31 entries below: **17 PASS, 4 FAIL, 9 NEEDS-USER, 1 not supplied** (the conditional live-map row). The parity checklist is not ticked: FAIL and NEEDS-USER entries remain.

Fixtures were disposable folders `Pictures/ZZ5View`, `ZZ5Burst/DCIM/Camera` and `ZZ5Video`: photos including `ZZ5_50% off+&.jpg`, a GIF, two geotagged JPEGs, three TIMEBURST frames, and 24 s, 65 s (with a subtitle track), 70 s and 75 s (two audio tracks) videos. They were deleted at the end with their MediaStore rows and the 2.7.2 Recycle Bin and vault copies.

Reference: Gallery 2.7.2 (APK on the device). Reconstruction: `app/build/outputs/apk/debug/app-debug.apk`, package `com.quizango.gallery`. UI-SPEC: `05-UI-SPEC.md`.

## 1. Automated gates (run, not pending)

| Gate | Command | Result |
|------|---------|--------|
| Unit suite | `./gradlew.bat :app:testDebugUnitTest -q` | pass: 242 tests, 0 failures (re-run after the device fixes f64b33b and 760a251) |
| Debug build | `./gradlew.bat :app:assembleDebug -q` | pass (exit 0) |
| Strings | `python -I scripts/ref_res.py check-strings` | pass: 24 languages up to date |
| Colors | `python -I scripts/ref_res.py check-colors` | pass: 213 light / 129 night names match 2.7.2 |
| CodeRabbit | not run: the CLI is not installed on this machine | see section 2 |

## 2. Review and defect record

| # | Defect | Reproducer | Owning plan | Failing gate | Expected result | Status |
|---|--------|-----------|-------------|--------------|-----------------|--------|
| D1 | Route arguments were decoded twice: androidx.navigation 2.8.8 already percent-decodes path and query arguments (`NavDeepLink` calls `Uri.decode` / `Uri.getQueryParameters`), then the app ran `URLDecoder.decode` again | Search `c++` then View All: query becomes `c  `. Open a photo named `50% off.jpg`: `URLDecoder` throws in the viewer route. A file named `%41.jpg` opens `A.jpg` | 05-01 (viewer, AlbumDetail), 05-03 (player), 05-06 (Place), 05-07 (View All, Color) | `RouteArgsTest`, `BrowseToViewerTracerTest` | Every awkward name round-trips through route build then the navigation decode, and the viewer opens the tapped page | Fixed in 08a85d3. Device recheck PASS: `ZZ5_50% off+&.jpg` opens in the viewer from its folder (`evidence/p5-viewer-awkward-name.png`) |

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

Evidence: `p5-*` = ours, `r-*` = 2.7.2, in `evidence/`.

| Row | Screen | Result | Observed |
|-----|--------|--------|----------|
| #13 | Full-screen viewer | **FAIL** | Chrome, dock, More card, dialogs, slide show, delete/hide-next and recycle now match (`p5-viewer-awkward-name`, `p5-viewer-more` vs `r-viewer-photo`, `ref272-viewer-more-popup`). Remaining: **Show on map never appears** on geotagged photos, because no photo location is ever read (see #19). |
| #14 | Video player and inline short video | **FAIL** | Inline short clip, player chrome, No Repeat and audio ownership pass. Remaining: the **subtitle picker** is a small top dropdown listing only the tracks (`p5-player-subtitle-picker`); 2.7.2 shows a bottom "Subtitle" dialog with a **Disable** row ticked by default and pauses playback (`r-player-subtitle-picker`). 2.7.2 also greys CC out when the file has no subtitles; ours keeps it enabled and toasts. Minor: bottom-row spacing differs (lock 36 vs 60 px from the edge, previous/next 24 px closer to Play). |
| #16 | Media list | NEEDS-USER | Folder list, headers, spinner then data seen (`p5-medialist`). Pinch 3/4/5, drag-select and fast-scroll drag need multi-touch or held drags that adb `input` cannot do. |
| #17 | Other Albums | NEEDS-USER | No in-app entry (route only); not reachable without a debug launcher. |
| #18 | Moments | PASS | Cards from the real library; a card opens a titled list. The first-run scan header was not seen (the scan was already cached). |
| #19 | Place | **FAIL** | Without a key: no crash, blank map region, the sheet loads (`p5-place-nokey`). But the sheet shows "No items": **the media scan never reads EXIF GPS**, so `latitude`, `longitude` and `placeName` stay 0 or empty. 2.7.2 lists "Minato City 1" and "Paris 1" for the same fixtures (`r-place`). 2.7.2's sheet also peeks higher (top at about 990 px against 1215 px). |
| #20 | Animated | NEEDS-USER | No in-app entry in ours, nor on 2.7.2's Explore; route only. |
| #21 | Burst | NEEDS-USER | No in-app entry; route only. The fixture frames were indexed (Photos tab). |
| #22 | Favorites | PASS | Two favourites listed, four-slot bar (Share, Unfavorite, Add to Private, Delete), unfavourite prunes at once, "No Favorites" empty state (`p5-favorites*`). |
| #23 | Search and View All | PASS | Colors row; typing `ZZ5` filters Albums; View All is titled "Albums"; Back keeps the query. |
| #24 | Search by color | PASS | The red swatch opens "Media Color"; selection shows only Share, Favorite, Add to Private, Delete. A custom non-swatch colour was not tried. |

## 4. State matrices

### 4.1 Viewer variants (#13, #14)

| State | Result | Observed |
|-------|--------|----------|
| normal | PASS (after fixes) | Fixed in f64b33b: glass icons were dark blue and the circles 48 dp and overlapping (now white, 36 dp, 8 dp gaps); the background is `backgroundColor` while the chrome shows and black once hidden, as in `r-viewer-hidden`; Delete closed the viewer where 2.7.2 shows the next page (`r-viewer-after-delete`); the dialog said "1 item" where 2.7.2 says "this item"; video More listed Add to where 2.7.2 lists only Rename and Slide show (`r-viewer-video-more`). Slide show: about 2 s per photo, stops at the last one. Show on map: see #19. |
| recycle | PASS (after fix) | Only Restore and Delete (`p5-viewer-recycle`). Restore or Delete **crashed the app** (ClassCastException in `MediaViewerScreen`, also on the pre-fix HEAD); fixed in f64b33b. Restore now shows the next binned item (`p5-viewer-recycle-after-restore`). |
| vault | NEEDS-USER | Needs the vault PIN set up; not touched. |

### 4.2 Video length (#14)

| State | Result | Observed |
|-------|--------|----------|
| short video (60 s or less) | PASS (after fix) | The paused pill matches `ref272-viewer-short-video-paused`. **Play did nothing on first open** (the player was released during the first list load); fixed in f64b33b. Playing: seek row, Capture replaces the heart (`p5-short-playing`). Ends at 23-24 s and stops, no loop (`p5-short-ended`). |
| long video (over 60 s) | **FAIL** | The centre button opens the player; the quick-control card and bars match `r-player-controls`; No Repeat moves on to the next queued video exactly like 2.7.2 (`p5-player-end-norepeat`, `r-player-end-norepeat`). The mute label was inverted ("Unmute" while sound is on; 2.7.2 shows "Mute" with the crossed icon): fixed in 760a251, not re-checked on the device. Open: the subtitle picker (see #14). Not exercised: previous/next end toasts, rotate, lock, brightness/volume swipes, repeat persistence. |
| audio ownership | PASS | Player: Home pauses it (`dumpsys audio` shows paused), Back releases it. Inline: swiping away releases the player. |

### 4.3 Library size

| State | Result | Observed |
|-------|--------|----------|
| small library | PASS | Fixture folder lists; Favorites populated and empty. |
| large library | NEEDS-USER | On the real library (hundreds of items) Moments and Search fill in without delay. Fast-scroll drag and pinch jank need a manual check. |

### 4.4 Theme and locale

| State | Result | Observed |
|-------|--------|----------|
| light | PASS | All the screens above. |
| dark | PASS | In-app Night Mode: dark glass chrome as in `ref272-viewer-photo`, More card `#1E1E1E` (`p5-dark-viewer-more`), Search toolbar. Restored to System Mode. |
| German | PASS | `p5-de-explore`, `p5-de-favorites-empty`. |
| Russian | PASS | `p5-ru-favorites-empty`, `p5-ru-player`. |
| Tamil | PASS | No clipping in the toolbar, stripe or empty state (`p5-ta-place`, `p5-ta-favorites-empty`). |
| Arabic RTL | PASS | Grid, top bar and dock mirror; the seek bar runs right to left (`p5-ar-short-playing`). Restored to English. |

### 4.5 Place (#19)

| State | Result | Observed |
|-------|--------|----------|
| permission denied | NEEDS-USER | The runtime dialog is left to the user. |
| permission approximate | NEEDS-USER | As above. |
| permission precise | NEEDS-USER | As above. |
| Place without a Maps key (mandatory) | PASS | No crash, blank map region of stable size, stripe and sheet render (`p5-place-nokey`). "Cards open scoped media" cannot be tested until #19 is fixed. |
| live map, conditional | not supplied | No `MAPS_API_KEY`; accepted state. |

### 4.6 Ad slots

| State | Result | Observed |
|-------|--------|----------|
| collapsed ad slots | PASS | No gap or placeholder in the viewer, player, lists, Place or Search (2.7.2 shows banners there). |

## 5. Assumptions settled on the device

| Question | 2.7.2 answer | Ours now |
|----------|--------------|----------|
| Delete or Hide in the viewer: close, or show the next item? | Stays open and shows the next item for both (`r-viewer-after-delete`, `r-viewer-after-hide`) | Same (f64b33b); closes only when the last page goes |
| No Repeat at the end | Moves on to the next video in the queue | Same (ExoPlayer REPEAT_MODE_OFF) |
| Subtitle picker Off row | Yes: "Disable", ticked by default, in a bottom "Subtitle" dialog | Missing (FAIL, open) |
| Wallpaper and Print on video pages | Not shown: video More is Rename, Slide show | Same (f64b33b removed the extra Add to) |

## 6. Steps for the user

1. **Location permission** (Explore > Place). First revoke location for Gallery in Android Settings > Apps. (a) Tap Allow and choose "Don't allow": the stripe stays and nothing else changes; tap Allow and deny again: app settings open with the toast "Please grant location permission." (b) Revoke, tap Allow, pick "Approximate" and "While using the app": the stripe disappears. (c) Revoke, tap Allow, pick "Precise": the stripe disappears. With no Maps key no My Location dot can show.
2. **Media list gestures**: in a folder, pinch 3 > 4 > 5 > 3 columns, long-press then drag across rows to range-select, and drag the fast-scroll thumb in a large folder.
3. **Vault viewer**: with the vault PIN set, hide a disposable photo and open it from the vault: no Add to, no Show on map, no slide show, and the delete dialog without the Recycle Bin box.
4. **Other Albums, Animated, Burst**: no in-app entry exists; confirm where 2.7.2 exposes them, or add a debug entry to test them.
5. **Long video extras**: previous/next toasts at the playlist ends, rotate, lock, brightness/volume swipes, repeat mode kept after reopening, and the mute label fix (760a251).

## 7. Parity checklist

`.planning/milestones/v2.7.2-parity/PARITY-CHECKLIST.md` is **unchanged**: #13, #14 and #19 fail and several entries need the user. Verified stays unchecked for Phase 13.
