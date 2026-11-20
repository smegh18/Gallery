---
phase: 04-albums-stories-and-explore
plan: 03
subsystem: ui
tags: [compose, explore, navigation, datastore]
requires:
  - phase: 04-01
    provides: isJunkCleanedForToday and collageMakerNewButtonExplore DataStore keys, explore_* resources
provides:
  - 2.7.2 Explore tab (hero, Create, Library, static native-ad card, Manage)
  - exploreRoute tap routing, Collage "New" badge rule, Duplicate Finder RAM gate
affects: [04-04, 04-06]
tech-stack:
  added: []
  patterns: ["pure route/badge/gate helpers unit-tested on the JVM"]
key-files:
  created:
    - app/src/test/java/com/quizango/gallery/ExploreTabTest.kt
  modified:
    - app/src/main/java/com/quizango/gallery/ui/home/ExploreTab.kt
    - app/src/main/java/com/quizango/gallery/ui/home/HomeViewModel.kt
    - app/src/main/java/com/quizango/gallery/ui/home/HomeScreen.kt
  deleted:
    - app/src/main/res/drawable/ic_organier_explore.xml
    - app/src/main/res/drawable/ic_collage_explore.xml
    - app/src/main/res/drawable/system_cleaner_banner_arrow_new_ornage.xml
    - app/src/main/res/drawable/system_cleaner_banner_arrow_new.xml
decisions:
  - "Scrollbar-free page uses LocalOverscrollConfiguration = null (Compose 1.7 has no per-scroll overscroll parameter)"
metrics:
  duration: ~25 min
  completed: 2026-11-20
status: complete
actuals:
  tokens: 14000
  tasks: 2
  commits: 2
plan_head_before: af546c5
plan_head_after: 81a260e15c98ffcb00b4dddf33689a4450293fed
---

# Phase 4 Plan 03: Explore tab Summary

The 2.6.7 three-card Explore page is replaced by the 2.7.2 page: blue "Free up space" hero with Scan now, Create (AI Editor, Collage with the red New badge), Library (Vault, Favorites, Moments, Place round tiles), a static native-ad skeleton card, and a Manage card (Duplicate Finder, Quick Organize, Recycle Bin, Settings), all routed like 2.7.2.

## What was built

- **Task 1 (tracer, ecbbf46):** `ExploreItem` and `exploreRoute` (Scan now opens Remove Junk, or System Cleaner when `isJunkCleanedForToday`); the full page layout from UI-SPEC 3.1-3.5; `HomeUiState.junkCleanedToday` / `collageNewSeen` fed from the 04-01 preference keys and `HomeViewModel.markCollageNewSeen`; four Explore-only 2.6.7 drawables deleted. Verified on the phone: all 7 above-the-fold texts present.
- **Task 2 (81a260e):** `showsCollageNew` (unseen and fewer than 4 launches; tapping Collage or the badge writes the preference), `showsDuplicateFinder` (SDK 29+ and 3 GB via `ActivityManager.MemoryInfo.totalMem`), `manageRows` (first visible row has no divider), static ad skeleton between Library and Manage.

## Gate results

| Gate | Result |
|---|---|
| ExploreTabTest (routes for 11 items, badge rule, RAM gate, Manage rows) | pass |
| Full unit suite, `assembleDebug` | pass |
| Old hero refs / explore_tab_ic_* glyphs / old drawables / Text string literals | 0 / 11 / 0 / 0 |
| Device (Redmi M2006C3MII, English) | Explore shows hero, Create, Library, ad skeleton, Manage with Duplicate Finder (phone passes the RAM gate); Moments tile opens Moments |

## Deviations from Plan

None - plan executed exactly as written. Remaining device checks (badge on a fresh install, locales, RTL, dark) are left to 04-06 as the plan states.

## Known Stubs

- `ExploreTab.kt` `NativeAdSkeleton`: intentional static ad placeholder (decision D5, no ads); not wired to any ad source.

## Threat Flags

None.

## Self-Check: PASSED

ExploreTab.kt, ExploreTabTest.kt present; commits ecbbf46 and 81a260e are on the branch.
