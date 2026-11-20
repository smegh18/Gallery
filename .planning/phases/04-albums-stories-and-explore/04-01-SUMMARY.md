---
phase: 04-albums-stories-and-explore
plan: 01
subsystem: ui
tags: [compose, albums, stories, datastore, resources]
requires:
  - phase: 03-home-shell-and-photos-tab
    provides: HomeScreen pager, sdp/ssp helpers, drawableBackground, album_tab_* colours and icons
provides:
  - 2.7.2 Albums tab (grid order, HomeAlbumCard, Favorite and Deleted cards, story row with Playzen ad circle)
  - 14 reference string keys (+24 translations), 10 colours, 8 drawables, 3 DataStore keys for 04-02..04-05
  - Album.titleRes and Story.titleRes (reference-key titles instead of English literals)
affects: [04-02, 04-03, 04-04, 04-05, 04-06]
tech-stack:
  added: []
  patterns: ["virtual albums carry titleRes, UI resolves via albumTitle()", "pure grid/route/badge helpers unit-tested on the JVM"]
key-files:
  created:
    - app/src/test/java/com/quizango/gallery/AlbumsTabTest.kt
    - app/src/main/res/drawable/ic_pin.xml
    - app/src/main/res/drawable/story_edit_bg.xml
    - app/src/main/res/drawable/popup_stories_edit_bg.xml
    - app/src/main/res/drawable/btn_stories_edit_bg.xml
    - app/src/main/res/drawable/ic_album_select_pinned.xml
    - app/src/main/res/drawable/ic_album_select_cover.xml
    - app/src/main/res/drawable/ad_icon_bg_stroke_1.xml
    - app/src/main/res/drawable/ad_btn_bg_white.xml
  modified:
    - app/src/main/java/com/quizango/gallery/ui/home/AlbumsTab.kt
    - app/src/main/java/com/quizango/gallery/ui/home/HomeScreen.kt
    - app/src/main/java/com/quizango/gallery/ui/home/HomeViewModel.kt
    - app/src/main/java/com/quizango/gallery/ui/discovery/StoryBuilder.kt
    - app/src/main/java/com/quizango/gallery/ui/discovery/MomentsScreen.kt
    - app/src/main/java/com/quizango/gallery/data/repository/MediaRepository.kt
    - app/src/main/java/com/quizango/gallery/data/local/datastore/AppPreferences.kt
    - app/src/main/java/com/quizango/gallery/domain/model/Models.kt
decisions:
  - "Moments keeps the 2.6.7 album card as a private composable until Phase 5"
  - "albumTitle() was introduced in Task 1 (not Task 2) because the grid needs the list title at tap time"
metrics:
  duration: ~45 min
  completed: 2026-11-20
status: complete
actuals:
  tokens: 45500
  tasks: 3
  commits: 3
plan_head_before: 2e7f4a1f09f128a8eacc1a713c46f426ce977b15
plan_head_after: 364232472f9b76b37318b6a4043da67317c23fef
---

# Phase 4 Plan 01: Albums tab grid, cards and story row Summary

Albums tab rebuilt to 2.7.2: no feature carousel or Media Types section, a 3-column grid (Recent, Favorite, All Video, Camera, Screenshots, folders, Deleted) with item_home_album_card styling, and a story row with a solid #0066FF ring and the Playzen ad circle at index 2.

## What was built

- **Task 1 (tracer, 7d353ed):** imported 14 reference strings (script-copied, 24 translations regenerated with `ref_res.py sync-strings`), `cannot_select_system_folders` (translatable=false), 10 colours, 8 drawables; deleted 26 carousel/media-type drawables; added `isStoriesEditToolTip`, `collageMakerNewButtonExplore`, `isJunkCleanedForToday` DataStore keys; `Album.titleRes`; `favoritesCard`/`deletedCard`/`albumGrid`/`albumCardTitle`/`albumRoute`/`albumTitle`; ViewModel feeds Favorite and Deleted cards; `shareSelectedMedia` made internal for 04-02. Verified on the phone: Albums page shows Recent and Favorite, no Media Types heading, Favorite card opens Favorites.
- **Task 2 (a1423ee):** `HomeAlbumCard` (10.sdp thumb at 200x200, badges via `albumBadge`, Deleted dim + trash, 1-line 9.ssp title, `albumCountText` bare locale-formatted count); virtual album names removed from `MediaRepository`; viewer add-to rows/toast and vault add screen use `albumTitle`; add-to destinations name mapped Camera/Screenshots after the real folder; Moments keeps a private copy of the old card.
- **Task 3 (3642324):** `Story.titleRes` on reference keys; `storyRow` (ad circle at index 2, clamped, none in an empty row); `StoryItem` with solid ring, press-scale 0.9, Playzen ad circle (`ic_story_playzen` + `ad_bg` badge, tap no-op); story tap navigates by id with an empty title; the queue passed to the viewer never contains the ad.

## Gate results

| Gate | Result |
|---|---|
| Key gate | 14 checked, 0 differ from 2.7.2; own toast key ok: True |
| `ref_res.py check-strings` / `check-colors` | pass (24 languages up to date; 198 light / 125 night match) |
| 8 drawables byte-identical to R272, 10/10 colours | pass |
| Carousel/media-type references | 0; 26 drawable files removed |
| English virtual names / material-icons in AlbumsTab / Moments cross-import / 200x200 decode | 0 / 0 / 0 / 1 |
| English story titles / gradient ring / Playzen label / ring colour | 0 / 0 / 1 / 1 |
| AlbumsTabTest, HomeTest, StoryQueueTest, AddMediaTest, full unit suite | pass |
| `assembleDebug`; APK gate | pass; flash_back(de)=1 ic_pin=1 |
| Device (Redmi M2006C3MII, English) | Albums grid, cards, story row with Playzen at index 2 render; Recent story opens the viewer; Playzen tap does nothing; Favorite card opens Favorites |

## Deviations from Plan

### Auto-fixed Issues

**1. [Rule 1 - Bug] Card title/count spacing**
- **Found during:** Task 2 device check
- **Issue:** title and count Texts inherited the Material line height, leaving a ~25px gap between them.
- **Fix:** explicit `lineHeight = 11.ssp` on both (reference uses includeFontPadding=false).
- **Files modified:** ui/home/AlbumsTab.kt
- **Commit:** a1423ee

**2. [Plan ordering] `albumTitle` added in Task 1**
- The grid needs the list title when a card is tapped, so the Task 2 helper landed with the tracer. No behaviour difference.

Otherwise the plan was executed as written. The Task 2 RED step was a compile failure (the new helpers did not exist) rather than a runtime assertion.

## Known Stubs

None. (The story skeleton/shimmer is intentionally not built; UI-SPEC 1.3 defers it to the 04-06 device comparison.)

## Notes for later plans

- Moments still shows the 2.6.7 card (private copy in MomentsScreen.kt); Phase 5 restyles it.
- The Camera card did not appear on the test phone because its camera folder is named `0`; the folder shows as a device folder instead. Phase 13 should confirm Camera/Screenshots matching against 2.7.2 on a phone with a standard DCIM/Camera folder.
- Long-press and selection mode for album cards are 04-04.

## Threat Flags

None. No new endpoints, auth paths or file access beyond the threat-model entries (T-04-01 mitigated by script-copied keys, byte-compared drawables and `check-colors`; T-04-02 accepted).

## Self-Check: PASSED

- Created files exist: AlbumsTabTest.kt, the 8 drawables.
- Commits 7d353ed, a1423ee, 3642324 are ancestors of HEAD.
