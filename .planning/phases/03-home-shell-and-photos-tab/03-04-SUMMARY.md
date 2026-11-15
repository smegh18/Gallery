---
phase: 03-home-shell-and-photos-tab
plan: 04
subsystem: ui-home
tags: [compose, gestures, pinch, lottie, datastore, parity-2.7.2]
requires:
  - 03-01 (clampColumnCount, HomeViewModel.setColumnCount, mediaColCount, pinch_in_out_btn, Phase 3 strings/colours)
  - 03-02 (Photos grid in PhotosTab.kt)
  - 03-03 (Home root layout, Modifier.drawableBackground)
provides:
  - PhotosTab.kt nextColumnCount, pinchPreviewScale, two-finger pinch gesture, onColumnCountChange parameter
  - ui/home/HomeOverlays.kt PinchTutorialOverlay (03-05 adds the vault hint and exit popup here)
  - AppPreferences.isPinchInOutToolTip / setPinchInOutToolTip
  - HomeUiState.pinchTipSeen, HomeViewModel.dismissPinchTip
affects: [03-05 (HomeOverlays.kt, Home root is now a Box), 03-06 (side-by-side pinch check)]
tech-stack:
  added: []
  patterns: [Initial-pass pointerInput that consumes only while two or more pointers are down, local shown value that the persisted flow catches up with]
key-files:
  created:
    - app/src/main/java/com/quizango/gallery/ui/home/HomeOverlays.kt
  modified:
    - app/src/main/java/com/quizango/gallery/ui/home/PhotosTab.kt
    - app/src/main/java/com/quizango/gallery/ui/home/HomeScreen.kt
    - app/src/main/java/com/quizango/gallery/ui/home/HomeViewModel.kt
    - app/src/main/java/com/quizango/gallery/data/local/datastore/AppPreferences.kt
    - app/src/test/java/com/quizango/gallery/HomeTest.kt
decisions:
  - "Pinch commit threshold: cumulative zoom 1.2 (spread) / 1/1.2 (pinch). This is the plan's fallback because the 2.7.2 smali is not on this machine"
  - "Commit 280 ms, cancel 200 ms. Which smali duration is which is unconfirmed"
  - "Pinch detector runs on PointerEventPass.Initial so the grid and pager never scroll during a pinch"
  - "Home root wrapped in a Box so overlays cover the top bar, the bottom bars and the nav-bar strip"
status: complete
commits: 4
metrics:
  duration: ~30 min
  completed: 2026-11-15
actuals:
  tokens: 6500
  tasks: 2
  commits: 4
---

# Phase 3 Plan 4: Pinch to change columns and first-run tutorial Summary

A two-finger pinch on the Photos grid now changes the column count by one step per gesture, within 3 to 5 columns. Spreading gives fewer, bigger cells and pinching gives more. The grid scales live around the pinch centre while your fingers are down. On release it settles with the 2.7.2 interpolator, and the cells glide to their new places. The new count is saved through `viewModel::setColumnCount` and `mediaColCount`. The first time the Photos page is shown with media permission, the 2.7.2 "Zoom Out / Change the number of columns" overlay covers the whole Home. Only GOT IT closes it, and after that it never shows again (`isPinchInOutToolTip`).

## Tasks

| Task | RED | GREEN | Result |
|------|-----|-------|--------|
| 1 Pinch steps columns with live preview | d70a45f | 5ee8761 | `nextColumnCount`, `pinchPreviewScale`, Initial-pass two-finger detector, graphicsLayer preview, animateItem settle, `onColumnCountChange` wired to `setColumnCount` |
| 2 First-run pinch tutorial | n/a | a134147 | `isPinchInOutToolTip` preference, `pinchTipSeen` / `dismissPinchTip`, `HomeOverlays.kt` `PinchTutorialOverlay`, drawn last in the new Home root Box |

## Values used, and where they come from

The R272/S272 decode (`C:/Users/megh/AppData/Local/Temp/ref272`) is not on this machine, so I could not read `ai/n.smali` again. I took everything from the plan's "facts verified at planning time":

| Value | Used | Source |
|---|---|---|
| Interpolator | `CubicBezierEasing(0.18f, 0f, 0.08f, 1f)` | plan: S272/ai/n.smali constructor, `PathInterpolator(0.18, 0, 0.08, 1)`, line ~235 |
| Elastic limits at 3 / 5 columns | 1.05 (spread at 3) / 0.95 (pinch at 5) | plan: S272/ai/n.smali lines ~521-526 |
| Commit duration | 280 ms | plan: S272/ai/n.smali `0x118`, line ~3021. **Unconfirmed that this is the commit duration** |
| Cancel duration | 200 ms | plan: S272/ai/n.smali `0xc8`, line ~4188. **Unconfirmed that this is the cancel duration** |
| 120 ms base with a 60..140 lerp | not used | plan: line ~1337. I could not tell what it drives without the smali |
| Commit threshold | cumulative zoom >= 1.2 gives one fewer column, <= 1/1.2 gives one more | **plan fallback, not found in the smali** |
| Preview range away from the limits | `current/(current+1)` .. `current/(current-1)` (for example 4 columns previews 0.8x to 1.33x) | my choice: the preview stops at the next step's cell size |

## How the preview differs from 2.7.2 (what to look at in 03-06)

- **2.7.2** uses a custom `SamsungGalleryLayoutManager` with a pinch controller that morphs each cell (`frameDisplayScale`, `frameCurrentSpan`, `capture*` positions, `commitAnim*`). Cells move between the two span layouts while the pinch is in progress.
- **Ours** scales the whole grid as one `graphicsLayer` around the centroid while the pinch is in progress. Nothing reflows until release. On commit the layout switches to the new count immediately, through a local `shownCount`. The scale is first re-expressed against the new layout (`scale * next / from`) and then animated to 1 over 280 ms. At the same time `Modifier.animateItem(placementSpec = tween(280, PinchEasing))` glides every row (cells, date headers, ad row) from its old position to its new one. Fade specs are null, so photos added later do not start fading in.
- What to compare: during the gesture, 2.7.2 shows cells cross-fading or reflowing while ours shows a uniform zoom. Check how far the zoom has to go before the grid commits (1.2 here), how the settle feels at 280 ms, and the rubber-band at 3 and 5 columns.
- First visible photo: I did not add an explicit `scrollToItem`. LazyGrid keeps the first visible item by key when the span count changes, so its row stays at the top. Check on the phone that this matches 2.7.2's anchor, which may be the item under the centroid instead.

## Verification (actual results)

- RED: `./gradlew.bat :app:testDebugUnitTest --tests "com.quizango.gallery.HomeTest" -q` failed at compile with `HomeTest.kt:17:37 Unresolved reference 'nextColumnCount'` (and four more on lines 168-171).
- Task 1 gate: `pinch markers=5/5, HomeScreen wiring lines=1`. HomeTest passed with exit 0.
- Task 2 gate: `overlay markers=7/7, pref key=1, wired=1`.
- After each task, `./gradlew.bat :app:testDebugUnitTest -q` and `./gradlew.bat :app:assembleDebug -q` both exited 0.
- Final `./gradlew.bat :app:testDebugUnitTest :app:assembleDebug`: `BUILD SUCCESSFUL in 3s`. Test result XMLs: 128 tests, 0 failures, 0 errors. `testNextColumnCount` passed.
- Device: **not run.** The Redmi has a differently signed build installed. Installing would need an uninstall, which wipes app data, so I skipped the device check as instructed. Nothing about the pinch feel, one-finger scroll, taps and long-presses during the pinch, or the overlay's look was checked on hardware.

## Deviations from Plan

1. **[Rule 3 - Blocking] 2.7.2 smali not available.** I could not do step 1, pinning the threshold and durations from `ai/n.smali`. I used the plan's 1.2 fallback, as the plan allows. I picked 280 ms for commit and 200 ms for cancel, but this is not confirmed. I put a `shortcut:` comment in PhotosTab.kt and logged it in `.planning/WINDOWS.md` (entry 1).
2. **[Rule 2] Extra pure helper `pinchPreviewScale`** with four asserts added to `testNextColumnCount` in the GREEN commit. It bounds the live preview and applies the elastic limits. The asserts were not part of the RED commit.
3. **Detector on `PointerEventPass.Initial`.** The plan only says to consume during the pinch. On the default Main pass the grid (a child) and the pager would see the two-finger move first and scroll. One-finger input is never consumed (T-03-09).
4. **animateItem on every row, not only media cells.** Headers and the ad row would otherwise jump while the cells glide. Each row is wrapped in a `Box(Modifier.animateItem(...))`.
5. **Local `shownCount`.** The grid switches columns on release, without waiting for the DataStore round trip. `remember(columnCount)` re-syncs when the saved value comes back. Reads and writes still go through `clampColumnCount` (T-03-10). `nextColumnCount` clamps as well.
6. **Home root is now `Box { Column(...); overlay }`.** The 03-03 Column was re-indented inside it. `git diff -w` shows only 6 added lines. 03-05 can add its overlays in the same Box.
7. **GOT IT size.** In the reference this is an AppCompatButton with no vertical padding and a background that has no padding. I gave it the framework minimum of 88 x 48dp with the text centred. I inferred that minimum from the framework defaults (Widget.AppCompat.Button), and I did not check it against a 2.7.2 screen. The press ripple comes from Compose `clickable`, clipped to the 35.sdp pill. As in 03-03, `drawableBackground` only paints the ripple's static layer.
8. **Overlay centring.** The Lottie is centred in the full Home box, which runs edge to edge behind the status and navigation bars. GOT IT sits 15.sdp above the navigation bar. If 2.7.2's overlay only covers the content area, its Lottie centre would be about half a status bar lower. Check this in 03-06.
9. **Tutorial condition** uses `uiState.selectedTab`, which follows the pager's settled page, plus `checkStoragePermissions(context)` on each recomposition. The overlay does not handle Back. Back keeps its existing Home behaviour while the tutorial shows. I did not check this against 2.7.2.

## Known Stubs

None.

## Threat Flags

None new. T-03-09: the detector consumes only while at least two pointers are pressed, so single-finger drags, taps and long-presses reach the grid. T-03-10: `nextColumnCount` and `setColumnCount` both go through `clampColumnCount`.

## Self-Check: PASSED

- HomeOverlays.kt and this SUMMARY are present. Commits d70a45f, 5ee8761 and a134147 are on main.
- STATE.md and ROADMAP.md were not touched.
