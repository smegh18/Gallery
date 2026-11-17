---
phase: 03-home-shell-and-photos-tab
plan: 02
subsystem: ui-home
tags: [compose, photos-tab, lazy-grid, fast-scroll, parity-2.7.2]
requires:
  - 03-01 (PhotosTab signature, Phase 3 strings / colours / drawables)
provides:
  - PhotoRow (Ad, Header, Media), dayStartOf, buildPhotoRows, HeaderKind, headerKind, fileTypeLabel
  - thumbFraction, indexForThumbFraction
  - PhotosTabGridItem, ad-row skeleton, 2.7.2 empty state, fast-scroll thumb
  - SharedMediaGrid.kt (old PhotoGridItem, formatGroupHeader, getTodayMidnight, formatDuration moved unchanged)
affects: [03-03, 03-06, phases 5-6 (AlbumDetail / Search by colour / Recycle still use SharedMediaGrid.kt)]
tech-stack:
  added: []
  patterns: [pure internal row/label/scroll helpers unit-tested, shape drawables drawn via ContextCompat in drawBehind]
key-files:
  created:
    - app/src/main/java/com/quizango/gallery/ui/home/SharedMediaGrid.kt
  modified:
    - app/src/main/java/com/quizango/gallery/ui/home/PhotosTab.kt
    - app/src/test/java/com/quizango/gallery/HomeTest.kt
decisions:
  - "Fast-scroll thumb draws no shadow: the thumb ImageView has no background, so its 8.sdp elevation casts none on Android (2.6.7 layout_fastscroller_track_thumb dump)"
  - "Shape drawables (photos_tab_shimmer_block, photos_tab_ad_cta_bg) are drawn with ContextCompat.getDrawable in drawBehind, because painterResource only loads vectors and bitmaps"
status: complete
commits: 6
metrics:
  duration: ~60 min
  completed: 2026-11-15
actuals:
  tokens: 9500
  tasks: 3
  commits: 6
---

# Phase 3 Plan 2: Photos tab content Summary

The Photos tab now draws the 2.7.2 content: a full-width grid (13.sdp edges, 4.sdp gaps, 50.sdp bottom padding), date headers that follow the phone's language (Today / Yesterday keys, otherwise best pattern dMMM or dMMMyyyy), rounded grid cells with video and favourite badges, GIF / RAW / SVG labels and check marks, a static ad-slot skeleton at row 0, the 2.7.2 "No items" empty state, and an `ic_scroll` fast-scroll thumb. The old grid helpers that AlbumDetail, Search by colour and Recycle Bin still use moved unchanged to SharedMediaGrid.kt.

## Tasks

| Task | RED | GREEN | Result |
|------|-----|-------|--------|
| 1 Grid geometry, date headers, grid items | a2cb94c | fc29478 | Pure helpers PhotoRow / dayStartOf / buildPhotoRows / headerKind / fileTypeLabel; LazyVerticalGrid with 2.7.2 padding and spacing; PhotosDateHeader (GalleryText.BodySmallMedium = 10.ssp Inter Medium, photos_tab_header_text); PhotosTabGridItem; shared helpers moved to SharedMediaGrid.kt |
| 2 Ad-slot skeleton and empty state | 9e7ca55 | 7d4539b | PhotoRow.Ad first for a non-empty list only; full-width skeleton (cancels the 13.sdp grid padding with a layout modifier), then a 1.dp photos_tab_divider; empty state uses ic_empty_img at intrinsic size and BodyLargeSemiBold / BodySmallRegular in titleColor |
| 3 Fast-scroll thumb | f765b41 | 62080c4 | thumbFraction / indexForThumbFraction; hoisted rememberLazyGridState; thumb on the end edge follows the scroll, vertical drag calls scrollToItem; hidden when the grid cannot scroll either way; no auto-hide, no date bubble |

## Verification (actual results)

- RED runs: each failed at `:app:compileDebugUnitTestKotlin` on unresolved references (`buildPhotoRows`/`headerKind`/`fileTypeLabel`; `Ad`; `thumbFraction`/`indexForThumbFraction`).
- GREEN, each task: `./gradlew.bat :app:testDebugUnitTest :app:assembleDebug -q` exit 0. I ran the plan's HomeTest-only command and the full suite + assemble command as one Gradle call, since the full suite includes HomeTest.
- Final: HomeTest 11 tests / 0 failures (7 old, including the old formatDuration and formatGroupHeader cases, plus rows, header kind, file-type label and fast-scroll math); full unit suite 123 tests / 0 failures; app-debug.apk rebuilt.
- Grep gates: Task 1 leftover literals/duration/overlay = 0, 2.7.2 markers 6/6, shared helpers moved 3/3; Task 2 ad/empty markers 7/7, old 160.dp = 0; Task 3 fast-scroll markers 4/4.
- AlbumDetailScreen, SearchColorScreen, RecycleScreen, VaultMediaScreen and HomeScreen.kt are unchanged (0 files differ from 48ef2e2); they compile against SharedMediaGrid.kt with no import change.
- Device: installed the debug build on the wireless Redmi (speed-profile compiled) and launched it; the crash buffer has no entries for the app. I did not do a visual check because another app (a browser) was in the foreground on the phone, so the phone looked in use. The 03-06 device check covers visuals.

## Deviations from Plan

1. **[Rule 3 - Blocking] Reference decode not on this machine.** R272/S272 (C:/Users/megh/.../ref272) and E:/Gallery/local.properties do not exist here. The build uses ANDROID_HOME, so local.properties was not needed. Layout values came from the plan's context block and UI-SPEC 2.1-2.8, which list every value used. For the thumb, I dumped `layout_fastscroller_track_thumb` from the 2.6.7 `gallery.apk` in the repo with aapt2. It shows an AppCompatImageView with only `src` and `elevation` (no background), inside a LinearLayout with `background=@null`.
2. **[Rule 1] No shadow on the fast-scroll thumb.** The plan asked for shadow elevation 8.sdp. On Android, a View with no background has no outline, so the reference's elevation casts no visible shadow. Compose `shadow()` would draw a rectangular shadow that the reference does not have, so none is drawn. This is documented in the FastScrollThumb KDoc. Check on the phone in 03-06.
3. **[Rule 3] Shape drawables.** `painterResource` throws on `<shape>` XML. The skeleton blocks and the CTA pill use a small `DrawableBox` that draws `ContextCompat.getDrawable(...)` in `drawBehind`. The drawable is resolved from LocalContext on each composition, so night colours follow the theme.
4. Header text uses `GalleryText.BodySmallMedium` (10.ssp, W500 Inter) and the file-type label uses `GalleryText.BodyXSmallSemiBold` (8.ssp, W600) with the #424242 shadow, blur 4. These are the house equivalents of the specified styles.

## Notes for 03-06 device comparison (fast scroller)

- Drag: the thumb follows the finger (drag fraction = start position + dy / (track - thumb height)), and each move calls `scrollToItem(indexForThumbFraction(f, totalItemsCount))`. When the finger lifts, the thumb snaps back to the scroll-derived position. That position is `firstVisible / (total - visible)` while the jump target is `f x (total - 1)`, so the thumb can move a few percent on release in very long lists. The smali touch handling was not available to compare against on this machine.
- The touch target is the thumb's intrinsic size (10 x 21 dp vector), with no extra hit slop.
- The track is the full grid height, including the 50.sdp area under the bottom bar.
- The thumb draws over the 13.sdp end padding at the very edge. In RTL, end alignment puts it on the left.

## Known Stubs

- The ad row (PhotosTab.kt `PhotosAdRow`) is intentionally a static skeleton with no ad, per decision D5 (ads stay placeholders until real ad units exist). It does not block PHO-01.

## Threat Flags

None. T-03-04 is mitigated: rows are built on Dispatchers.Default via produceState, the grid is lazy and keyed by item id, and the thumb jumps with scrollToItem. For T-03-05, file names only go through a suffix check and contentDescription.

## Self-Check: PASSED

- PhotosTab.kt, SharedMediaGrid.kt, HomeTest.kt are present; commits a2cb94c, fc29478, 9e7ca55, 7d4539b, f765b41, 62080c4 are on main.
- STATE.md and ROADMAP.md were not touched.
