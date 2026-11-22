---
phase: 05-viewing-and-browsing
plan: 08
subsystem: ui
tags: [navigation, route-args, regression, evidence-matrix, device-acceptance]
requires:
  - phase: 05-07
    provides: Search, View All, Media Color (the last Phase 5 screens)
provides:
  - Route arguments reach every Phase 5 destination undecoded-twice (viewer, player, AlbumDetail, Place, Search View All, Color)
  - Pure viewerDisplayList / viewerTargetIndex plus a browse-to-viewer tracer test
  - 05-LIVE-COMPARISON.md evidence matrix, every device result PENDING-DEVICE
affects: [phase-05-verification, phase-13]
tech-stack:
  added: []
  patterns: ["encodeRouteArg: URLEncoder with '+' rewritten to %20, used by every route builder", "Nav-hop simulation with java.net.URI in JVM tests"]
key-files:
  created:
    - app/src/test/java/com/quizango/gallery/RouteArgsTest.kt
    - .planning/phases/05-viewing-and-browsing/05-LIVE-COMPARISON.md
    - .planning/phases/05-viewing-and-browsing/deferred-items.md
  modified:
    - app/src/main/java/com/quizango/gallery/ui/navigation/Screen.kt
    - app/src/main/java/com/quizango/gallery/ui/navigation/GalleryNavHost.kt
    - app/src/main/java/com/quizango/gallery/ui/viewer/MediaViewerScreen.kt
    - app/src/main/java/com/quizango/gallery/ui/discovery/PlaceScreen.kt
    - app/src/main/java/com/quizango/gallery/ui/discovery/SearchColorScreen.kt
    - app/src/main/java/com/quizango/gallery/ui/search/SearchViewAllScreen.kt
key-decisions:
  - "Route arguments are decoded once, by Navigation; Phase 5 destinations use them as is"
  - "Spaces in route arguments are encoded as %20 (URLEncoder writes '+', which Navigation keeps literally)"
  - "CodeRabbit CLI is not installed, so the review was a manual audit of the Phase 5 diff; it is recorded as not run"
requirements-completed: []
duration: n/a
completed: 2026-11-23
status: partial — device acceptance pending
actuals:
  tasks: 2
  commits: 1
---

# Phase 5 Plan 08: Phase close, integrated review and device acceptance Summary

The integrated automated gates pass and one real cross-screen defect (route arguments decoded twice) is fixed, but the real-device side-by-side pass (Task 3) did not run: `adb devices` is empty and the Artemis MCP is disconnected. Status is **partial, device acceptance pending**. VIEW-01, VIEW-02 and VIEW-03 are not marked complete and the parity checklist is unchanged.

## Tasks

| Task | Status | Commit | Notes |
|------|--------|--------|-------|
| 1 Tracer: browse-to-viewer path, full suite, build, resource gates, review | Done (device-free part) | 08a85d3 | Tracer test plus defect D1 fixed; CodeRabbit not run (CLI absent) |
| 2 Evidence matrix | Done | committed with this summary | `05-LIVE-COMPARISON.md`: rows #13, #14, #16-#24, six state matrices, all results PENDING-DEVICE |
| 3 Device acceptance | NOT RUN: PENDING-DEVICE | none | No device; nothing faked. Parity checklist untouched |

## Verification (exact commands, run on the final tree at 08a85d3)

- `./gradlew.bat :app:testDebugUnitTest -q`: exit 0. 44 result files, 242 tests, 0 failures, 0 skipped (the 05-07 baseline was 237 tests; +5 new).
- `./gradlew.bat :app:assembleDebug -q`: exit 0.
- `python -I scripts/ref_res.py check-strings`: "24 languages up to date".
- `python -I scripts/ref_res.py check-colors`: "213 light / 129 night names match 2.7.2".
- `adb devices`: empty list. Task 3's precondition is not met, so it was not attempted.
- The Task 2 verify (the plan's python assertion over `05-LIVE-COMPARISON.md`) passes.
- Before the fix, the same suite passed at 237 tests (first run in this plan); the defect was invisible to it.

## Defect found and fixed: route arguments decoded twice

The 05-07 open question ("does Navigation double-decode `query` / `color`") is **confirmed**, and it is wider than those two arguments.

- **Evidence:** the androidx.navigation 2.8.8 `NavDeepLink` bytecode calls `Uri.decode` for path arguments and `Uri.getQueryParameters` for query arguments. The app then ran `URLDecoder.decode` again in `GalleryNavHost`, `SearchViewAllScreen` and `SearchColorScreen`.
- **Effect:** a typed `c++` came back as `c  `; `%41` became `A`; a photo named `50% off.jpg` made `URLDecoder` throw while opening the viewer; `a+b.jpg` opened `a b.jpg` (not found, so a stale fallback page). Spaces only worked by accident.
- **Second half:** `URLEncoder` writes a space as `+`, and Navigation keeps `+` literally. Removing the extra decode alone would have turned every space into `+`, which the new tests caught. Route builders now share `encodeRouteArg` (`URLEncoder` then `+` to `%20`), which round-trips under either `+` semantics.
- **Fix (08a85d3):** `Screen.kt` (all 14 builders) and `placeRoute` use `encodeRouteArg`; the AlbumDetail, viewer, player, Place, View All and Color destinations use the argument as is.
- **Tests:** `RouteArgsTest` (3) and `BrowseToViewerTracerTest` (2) route `c++`, `100%`, `50% off`, `%41`, `a+b c`, `São Paulo & co/50%` and `x=y&z` through route build, a `java.net.URI` simulation of the navigation decode, and the screen argument handlers. `SearchColorTest`, `SearchScreenTest`, `PlaceScreenTest` and `AlbumDetailTest` were corrected where they encoded the wrong assumption (decode of an already-decoded value, `+` for spaces).
- **Tracer:** `viewerDisplayList` and `viewerTargetIndex` were pulled out of `MediaViewerScreen` as pure functions (behaviour unchanged). The tracer test taps each item of a mixed album, builds the route, takes the arguments the viewer would receive, and asserts the pager opens on that item. Old tests of "initial index" re-implemented the logic inline and proved nothing; the new ones call the production functions.
- **Limit of the tracer:** a repository-backed run (Room) and "closes without retained playback" need a device; player teardown is a `DisposableEffect` and cannot run in a JVM test. Both stay in the device list.

## Review (CodeRabbit not run)

`coderabbit` is not installed here and installing it is out of scope without approval. A manual audit of `53df7e8..HEAD` (`app/src/main/java`) covered the plan's checklist; the full table is in `05-LIVE-COMPARISON.md` section 2. Result: no hard-coded copy in the Phase 5 screens (only Phase 6's `OrganizerRecycleScreen`, out of scope), no TODO/FIXME/fake data, the new `catch (_: Exception)` sites all turn a failure into a toast or a `false` the UI reports, and the only no-op control is the Floating button owned by Phase 10. **This does not satisfy the plan's CodeRabbit criterion**; run it before declaring the phase done: `coderabbit review --base 53df7e8` (or the code-review skill with that base).

## Deviations from Plan

**1. [Rule 1 - Bug] Double route decode** (above). Commit 08a85d3. Also touched `PlaceScreenTest` and `AlbumDetailTest` expectations.

**2. Gap-closure plan not created.** The plan asks for a numbered gap-closure plan per defect. The orchestrator ran this unattended and the fix is one commit with its regression tests, so it was fixed inline (Rule 1) and recorded here and in the comparison record.

**3. CodeRabbit loop not run** (tool absent). Substituted a manual audit; see above.

**4. Instrumented navigation test not written.** Nothing can run it; `RouteArgsTest` simulates the decode. Noted in `deferred-items.md`.

## Decisions made without asking (user AFK)

- Fixed the whole route-builder family (`Screen.kt`) rather than only the two 05-07 arguments, because the viewer path is VIEW-01 and a photo with `%` in its name crashed.
- Left the non-Phase-5 destinations' own `URLDecoder` calls alone (Editor, StoryView, Wallpaper, vault, organizer): same defect class, other phases, no tests; logged in `deferred-items.md`.
- Did not touch `PARITY-CHECKLIST.md`, `STATE.md` or `ROADMAP.md`, and did not mark VIEW-01/02/03 complete.

## PENDING DEVICE: what the user must run

Setup: authorized Android device with the Gallery 2.7.2 reference APK, `adb install -r app/build/outputs/apk/debug/app-debug.apk`, `adb shell am force-stop com.quizango.gallery`, launch. Fixtures: portrait, landscape and GIF photos; a geotagged photo; a photo named with `%`, `+` and `&`; a clip of about 24 s; a video over 60 s (one with two audio tracks, one corrupt); a disposable burst (`..._TIMEBURST`); two disposable favourites; photos in the ten swatch colors; non-sensitive files only (T-05-15). Then fill `05-LIVE-COMPARISON.md` (every PENDING-DEVICE field) and only after all pass tick the Ported column for rows 13, 14, 16-24 in `.planning/milestones/v2.7.2-parity/PARITY-CHECKLIST.md` (Verified stays unchecked for Phase 13). Re-run the plan's Task 3 `<verify>` command afterwards.

Cross-plan items, from the prior summaries:

**From 05-01 (viewer):** pinch zoom (adb cannot drive it); per-orientation max scale; side-by-side timing against the reference. Open, double-tap zoom, pan versus paging and Back already passed on the Redmi (`05-01-DEVICE-CHECK.md`).

**From 05-02 (actions, slideshow):**
- More card against `ref272-viewer-more-popup.png`
- rename a disposable `/sdcard/Pictures/ZZ*` photo
- Rotate on a JPEG and a PNG
- normal, Recycle Bin and vault rows; Details card
- suggestion timing
- slideshow (2 s turns, stops at the end, nothing advances after Close or background)
- dock with RTL and long translations
- whether Delete / Restore / Hide / Unhide should show the next page instead of closing the viewer (not checked against the reference)

**From 05-03 (video):**
1. Short clip paused: mute, play, speed in the pill; compare `ref272-viewer-short-video-paused.png`
2. Playing: seek row, heart becomes Capture; compare `ref272-viewer-short-video-playing.png`
3. All five speeds, mute; the clip ends and stops, and Play restarts it
4. Capture writes `IMG_yyyyMMdd_*.png` to Pictures/Screenshots and the Photos tab, no toast
5. Long video: centre button opens the player; Previous/Next toast at the ends
6. Player: 10 s double-taps, Paused chip, repeat cycle (persists after reopening), mute, speed, rotate (both layouts), lock, brightness/volume swipes, 3 s auto-hide
7. Track pickers: no subtitles toast; two audio tracks
8. Audio stops on Home, Back, page change and close, in both players
9. Brightness and portrait orientation return to normal after the player
10. Corrupt video shows the matching error toast
11. Also unconfirmed: resume after Home (paused), and No Repeat moving on to the next queued video

**From 05-04 (lists, Favorites):**
- folder open: toolbar, headers (Today, Yesterday, `dd MMM yyyy`), spinner first and never "No items"
- pinch 3-4-5 shared with Photos and Favorites; fast-scroll thumb
- selection: long-press, drag range, release does not toggle off, Back clears selection, Select all label
- viewer pages only within the folder or favourites
- slots: Share, Favorite/Unfavorite, Add to (Move/Copy, same-folder refused), Add to Private, Delete (bin and permanent), Create album; toast copy
- Favorites flow and "No Favorites" empty state
- pinch tutorial shows once on the media list, never on Favorites
- RTL selection; dark mode headers and divider
- entrance animation (the -20 % drop is assumed; progress dialogs and drag-select edge auto-scroll are known gaps)

**From 05-05 (All Albums, Moments, Animated, Burst):**
- Other Albums via the `all_albums` route: cards, selection bar, Pin / Add to / Delete / Change cover / Rename
- Albums tab regression after the HomeScreen dialog extraction
- Moments: Lottie and "Kindly wait..." with no "Oops!" flash, `done/total` progress, empty state, a card opens a titled list
- Animated (`media/animated`): spinner then grid, six slots, viewer limited to animated items, empty state
- Burst (`media/burst`): single-cover cards "Burst 1..N", a card lists only its frames
- dark mode and RTL of toolbar divider and `countTitle`

**From 05-06 (Place):**
- no key: no crash, blank map area, sheet loads, cards open scoped media
- with a local key (optional): tiles, markers with photo and name, marker tap animates and scrolls the sheet, My Location only after grant
- permission: revoke, deny once, deny again (settings plus toast), grant fine or coarse removes the stripe
- viewer Show on map on a geotagged item opens that place; absent on a non-geotagged one
- marker bitmap proportions (60.sdp thumbnail, 6.sdp padding, 10.ssp label)

**From 05-07 (Search, View All, Color):**
- query matching one album, place and moment: sections filter and each card opens the right list
- more than six matches: six shown, View All lists all in 3 columns, Back keeps the query
- keyboard does not resize the page (adjustNothing, restored on leave)
- swatch and Choose color Ok open Media Color, Cancel and outside tap only close the dialog
- Media Color: pinch columns, selection, four actions only, pager limited to the colour
- moment results fill in during the scan with no "Oops!" flash
- look of the slider dialog (a plain Material 3 dialog, not the reference's color-picker) and swatch and field dimensions
- **recheck of D1 on a device:** search `c++` and `100%` then View All; open a photo with `%`, `+` and `&` in its name

**From 05-08 (this plan):** the full state matrices in `05-LIVE-COMPARISON.md` section 4 (viewer variants, video length, small and large library, light/dark, German, Russian, Tamil, Arabic RTL, permission denied/approximate/precise, Place without a Maps key, conditional live map, collapsed ad slots).

Also still open in `.planning/WINDOWS.md` from Phase 3 (not Phase 5): ledger ids 2.

## Known Stubs

- `VideoPlayerScreen` Floating button: intentional no-op, Phase 10 owns it (UI-SPEC 2.2).

Not appended to `.planning/WINDOWS.md`: the device checks above are unrun-verify items for the orchestrator to track (same as 05-06 and 05-07).

## Threat Flags

None beyond the plan's threat model. T-05-14: the review ran as a manual audit, not CodeRabbit (see above). T-05-15: no device was touched. T-05-16: the parity checklist was not advanced.

## Self-Check: PASSED

Commit 08a85d3 exists; `RouteArgsTest.kt`, `05-LIVE-COMPARISON.md` and `deferred-items.md` exist; `PARITY-CHECKLIST.md`, STATE.md and ROADMAP.md are untouched.
