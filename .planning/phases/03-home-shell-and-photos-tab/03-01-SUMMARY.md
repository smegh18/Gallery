---
phase: 03-home-shell-and-photos-tab
plan: 01
subsystem: ui-home
tags: [compose, home-shell, tab-bar, popup, resources, parity-2.7.2]
requires: []
provides:
  - HomeShell.kt (HOME_TABS, HomeTopBar, HomeTabBar, TopBarSlot/topBarSlots, MoreMenuRow/moreMenuRows, more popup)
  - PAGE_PHOTOS / PAGE_ALBUMS / PAGE_EXPLORE, clampColumnCount, HomeUiState.initialSyncDone
  - 31 reference strings (+24 translations), 4 colours, 8 drawables
  - PhotosTab signature (mediaList, isLoading, columnCount, selectedPaths, isSelectionMode, onMediaClick, onMediaLongClick)
affects: [03-02, 03-03, 03-04, 03-05, 03-06]
tech-stack:
  added: []
  patterns: [reference resources imported by script, internal pure tables for unit-testable UI rules]
key-files:
  created:
    - app/src/main/java/com/quizango/gallery/ui/home/HomeShell.kt
    - app/src/test/java/com/quizango/gallery/HomeShellTest.kt
  modified:
    - app/src/main/java/com/quizango/gallery/ui/home/HomeScreen.kt
    - app/src/main/java/com/quizango/gallery/ui/home/HomeViewModel.kt
    - app/src/main/java/com/quizango/gallery/ui/home/PhotosTab.kt
    - app/src/main/java/com/quizango/gallery/ui/home/AlbumsTab.kt
    - app/src/main/java/com/quizango/gallery/ui/components/NavigationComponents.kt
    - app/src/main/java/com/quizango/gallery/data/local/datastore/AppPreferences.kt
    - app/src/main/res/values*/strings.xml, values/colors.xml, values-night/colors.xml, 8 drawables
decisions:
  - "Ad-free tap is a no-op (Pro screen is Phase 9); recorded for the 03-06 SUMMARY"
  - "GalleryTopBar kept for album detail and Home selection mode only; 03-03 restyles the Home selection bar"
status: complete
commits: 3
plan_head_before: ad24460bb6ac43e3c16e664d7f3c7462389a4d78
plan_head_after: 65119371d05913e03be057e020521c696e63b93a
actuals:
  tokens: 40000
  tasks: 3
  commits: 3
---

# Phase 3 Plan 1: Home shell, tab bar, top bar and popup Summary

Home now opens on Photos with the 2.7.2 tab bar (Photos / Albums / Explore), per-tab title and top-bar slots, a drawer-free shell, a first-load spinner, a 3..5 column grid defaulting to 3, and the 2.7.2 more popup; every Phase 3 string, colour and drawable is imported so 03-02..03-05 never touch values/strings.xml.

## Tasks

| Task | Commit | Result |
|------|--------|--------|
| 1 Tracer: Photos-first shell + resources | cd264fd | 31 strings (+24 regenerated translations), 4 colours (2 with night overrides), 8 drawables byte-identical to R272; HomeShell.kt with HOME_TABS, HomeTopBar, HomeTabBar; drawer and old bottom nav deleted; HomeShellTest |
| 2 Loading, columns, half-width grid | 038cba5 | initialSyncDone + spinner on Photos and Albums, clampColumnCount 3..5, mediaColCount default 3, "No Albums Found" block removed, PhotosTab onRefresh removed, pager settle fix |
| 3 Top-bar slots, popup, vault long-press, dialog copy | 6511937 | topBarSlots / moreMenuRows tables, popup anchored under the more button, title long-press (haptic, opens VaultLock), ad-free no-op, About route removed, Sort and Create-album dialogs on reference keys |

## Gate results

- 31-key English gate: 31 checked, 0 differ; no zoom_in. check-strings (24 languages up to date) and check-colors (187 light / 120 night match) pass. 8 drawables byte-identical.
- HomeShellTest (tab order, default page, clamp, 3-column default, initialSyncDone, slot and popup-row tables) and the full unit suite pass; assembleDebug passes; APK gate prints albums(de)=1 ic_hint_home=1.
- Greps: drawer/old-nav/GALLERY = 0; hard-coded selection title/close = 0; "No Albums Found" = 0; dialog literals = 0; About route on Home = 0; home_shell_ic_* glyphs = 8; mediaColCount default 3.
- Device (Redmi, wireless adb): launch dump shows Photos title then tabs Photos, Albums, Explore; top bar shows ad-free, search, camera, more on Photos; the more popup opens under the button with Sort / Recycle Bin / Settings; Sort opens the dialog with reference copy; holding the title opens the Private Vault lock screen. Installed with speed-profile compile.

## Half-width grid bug (Task 2)

- Reproduced? No. Three long-press-then-drag runs via `input motionevent` (including a fast drag right at the long-press timeout) left the pager settled and the Photos grid at full width in selection mode (3 columns, 711 of 720 px). adb cannot time the finger to move inside the sub-frame window between the long-press firing and recomposition disabling pager scrolling.
- Leading hypothesis kept (not proven): disabling `userScrollEnabled` mid-drag cancels the pager drag without a snap, leaving the pager between pages.
- Fix applied once at that cause: when selection mode turns on, if `currentPageOffsetFraction != 0`, `animateScrollToPage(currentPage)` (HomeScreen.kt). Confirmation on a real finger is left to the 03-06 device check.
- Note for 03-02: a date group with fewer items than columns leaves the right of that row empty (2 photos per day on the test phone); that is normal layout, not the bug.

## Deviations from Plan

None on scope. Notes:
- GalleryTopBar was reduced rather than left with Home-only branches; it still serves Home selection mode (title via `items_selected`, close via `cancel`) until 03-03. Its back content description stays the literal "Back" (no reference key; Phase 5).
- The more popup reserves 8.sdp of window padding for the shadow and offsets its position by the same amount, so the content sits at the reference offset (-8 x, +8 y) without the shadow being clipped.
- `GalleryDrawer`-only colour accessor `drawerFooterBg` in Theme.kt is now unused; left in place (out of scope).

## Known Stubs

None. The ad-free button deliberately does nothing until Phase 9 builds the Pro screen (documented in HomeShell.kt and the plan).

## Threat Flags

None new. T-03-01 mitigated (script copy, 31-key gate, sync-strings guard); T-03-02 the long-press only navigates to the PIN-protected Vault lock screen.

## Self-Check: PASSED

- HomeShell.kt, HomeShellTest.kt, this SUMMARY present; commits cd264fd, 038cba5, 6511937 are ancestors of HEAD.
