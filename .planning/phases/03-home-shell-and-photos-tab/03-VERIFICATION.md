---
phase: 03-home-shell-and-photos-tab
verified: 2026-11-20T12:00:00Z
status: passed
score: 9/9 must-haves verified
covered_files:
  - ".planning/phases/03-home-shell-and-photos-tab/03-01-PLAN.md"
  - ".planning/phases/03-home-shell-and-photos-tab/03-01-SUMMARY.md"
  - ".planning/phases/03-home-shell-and-photos-tab/03-02-PLAN.md"
  - ".planning/phases/03-home-shell-and-photos-tab/03-02-SUMMARY.md"
  - ".planning/phases/03-home-shell-and-photos-tab/03-03-PLAN.md"
  - ".planning/phases/03-home-shell-and-photos-tab/03-03-SUMMARY.md"
  - ".planning/phases/03-home-shell-and-photos-tab/03-04-PLAN.md"
  - ".planning/phases/03-home-shell-and-photos-tab/03-04-SUMMARY.md"
  - ".planning/phases/03-home-shell-and-photos-tab/03-05-PLAN.md"
  - ".planning/phases/03-home-shell-and-photos-tab/03-05-SUMMARY.md"
  - ".planning/phases/03-home-shell-and-photos-tab/03-06-PLAN.md"
  - ".planning/phases/03-home-shell-and-photos-tab/03-06-SUMMARY.md"
  - "app/src/main/java/com/quizango/gallery/ui/home/HomeOverlays.kt"
  - "app/src/main/java/com/quizango/gallery/ui/home/HomeScreen.kt"
  - "app/src/main/java/com/quizango/gallery/ui/home/HomeSelection.kt"
  - "app/src/main/java/com/quizango/gallery/ui/home/HomeShell.kt"
  - "app/src/main/java/com/quizango/gallery/ui/home/HomeViewModel.kt"
  - "app/src/main/java/com/quizango/gallery/ui/home/PhotosTab.kt"
covered_digest: "v3:sha256:e7c7dca5e621d3928a898326ac59f1738c26c716121fb4e683a310df65f6a494"
behavior_unverified: 0
overrides_applied: 1
overrides:
  - must_have: "Pressing Back on Home shows the 2.7.2 exit popup"
    reason: "Device check on the reference phone showed 2.7.2 exits directly on Back (its bundled app_exit = direct); the exit popup never appears. HOME-03 is satisfied by matching 2.7.2's observed behaviour. Exit sheet removed in db1fe85."
    accepted_by: "user (device check approval)"
    accepted_at: "2026-11-20T00:00:00Z"
advisory:
  - finding: "Fast-scroll thumb (ic_scroll) is always visible in our Photos grid; the reference scroll screenshots (evidence/reference-scrolled.png) did not show one"
    category: other
    reason: "UI-SPEC 2.2 and the 2.7.2 smali (handleVisibilityDuration -1) say the thumb never hides, and 03-06-LIVE-COMPARISON flagged it as a candidate mismatch. The user's device-check answers cover the date bubble (none) but never state the thumb itself matches. User approved the device check with the thumb visible."
    evidence_status: "none provided"
---

# Phase 3: Home Shell and Photos Tab Verification Report

**Phase Goal:** Home looks and behaves like 2.7.2 around its tabs, and the Photos tab shows the 2.7.2 timeline.
**Verified:** 2026-11-20
**Status:** passed
**Re-verification:** No, initial verification
**Tree:** main @ 18193d5 (only uncommitted change: 03-06-SUMMARY.md "Device check closed" section)

## Goal Achievement

### Observable Truths

| # | Truth | Status | Evidence |
|---|-------|--------|----------|
| 1 | SC1: Home shows Photos / Album / Explore in 2.7.2 order with the 2.7.2 top bars; tap and swipe switch tabs; same titles and default tab | VERIFIED | `HomeShell.kt` `HOME_TABS`, `topBarSlots`; `HomeScreen.kt` `HorizontalPager` (`beyondViewportPageCount = 2`, swipe off while selecting), `HomeTopBar(page=…)`. `HomeShellTest` covers order, default Photos, slot table. Device: tab tap and swipe both ways, titles Photos / Album / Explore (03-06-LIVE-COMPARISON, Artemis b48563a6) |
| 2 | SC2: ⋮ popup (Sort on Photos only, Recycle Bin, Settings) and ad-free button match 2.7.2; each entry does what 2.7.2 does | VERIFIED | `moreMenuRows(page)`; `HomeScreen.kt` `onMoreRow` opens `SortDialog` / `Screen.RecycleBin` / `Screen.Settings`; ad-free is a no-op hook by design (Pro screen is Phase 9, tracked in ROADMAP Phase 9 "Carried in"). Device: Photos overflow = Sort / Recycle Bin / Settings, Albums = Recycle Bin / Settings, Sort and Create Album dialogs opened and cancelled |
| 3 | SC3: Vault hint appears in the same situations and place as 2.7.2 | VERIFIED | `shouldShowVaultHint` (sessions 4/8/12, not seen, not closed this session, media permission); session counted in `SplashScreen.kt` via `incrementSessionCount`; VaultLock destination sets `setVaultHintToolTip(true)` (`GalleryNavHost.kt:270`); `VaultHintOverlay` positioned from title bounds. `vaultHintShowsInSessions4_8_12Only` passes. Device: session-4 launch showed the hint, scrim tap closed it, title long-press opened Vault lock. Notification/widget/deep-link and start-dialog conditions are documented shortcuts carried to Phases 9, 10, 12 in ROADMAP (no such entry or dialog exists yet) |
| 4 | SC4: Pressing Back on Home shows the 2.7.2 exit popup | PASSED (override) | Override: 2.7.2 exits directly on Back. `HomeScreen.kt` `BackHandler`: vault hint closes, then selection clears, else `finishAffinity()`. No exit sheet remains (`HomeExitPopup` has no references). Override: user decision on device, db1fe85 |
| 5 | SC5: Photos tab shows 2.7.2 date headers, grid items, ad-placeholder rows; pinch changes column count; long-press enters selection with the 2.7.2 actions | VERIFIED | `PhotosTab.kt`: `buildPhotoRows` (ad row 0, headers, items), locale `dMMM` / `dMMMyyyy` headers, 13.sdp / 4.sdp grid, `PhotosAdRow`, `PhotosTabGridItem`, `nextColumnCount` pinch (3..5, persisted via `setColumnCount`, 200 ms glide, no whole-grid zoom per recorded 2.7.2); `HomeSelection.kt` six actions in order, `DeleteMediaDialog`. Device (Artemis b48563a6): 3 full-width columns, date headers, video badge, tab bar hides and returns, selection bar with all six actions and "1 item(s) selected", Add to sheet, Add to Private confirm, delete text "delete 1 item?" fixed in 8465f2f; pinch recorded side by side and approved (18193d5) |
| 6 | Unit tests and `assembleDebug` pass on the final tree; resource gates hold | VERIFIED | Orchestrator ran both on 18193d5 (pass). Re-run by this verifier: `check-strings` = 24 languages up to date; `check-colors` = 188 light / 120 night match 2.7.2. `HomeShellTest` (12) and `HomeTest` (12) tests present |
| 7 | Quality gates recorded: CodeRabbit, unslop audit | VERIFIED | 03-06-SUMMARY: CodeRabbit final review 0 findings after 2 fixed; unslop static audit recorded with parity-intentional items. No `TBD` / `FIXME` / `XXX` / `TODO` in `ui/home/*.kt` or `AddToAlbumSheet.kt` (grep) |
| 8 | Loading, empty and half-width-grid states behave per UI-SPEC | VERIFIED | `PhotosTab.kt` spinner while `isLoading`, `PhotosEmptyState`; grid is `fillMaxSize` in a full-width `BoxWithConstraints`. Device: grid spans full width, including selection mode |
| 9 | Locale and RTL backstops (de, ru, ta, ar) | VERIFIED | 03-06-LIVE-COMPARISON: titles and selection count on one line in de, ru, ta, ar; Arabic mirrors header and tabs; RTL selection bar scrolls to Create Album |

**Score:** 9/9 truths verified (1 via accepted override), 0 present-but-behavior-unverified.

### Required Artifacts

| Artifact | Expected | Status | Details |
|----------|----------|--------|---------|
| `ui/home/HomeShell.kt` | tab table, top bar, popup, tab bar, settle helpers | VERIFIED | 282 lines, used by `HomeScreen.kt` |
| `ui/home/HomeScreen.kt` | wiring, Back order, bottom-bar scroll, overlays | VERIFIED | 405 lines, all dialogs and overlays wired |
| `ui/home/PhotosTab.kt` | grid, headers, ad row, pinch, fast scroll | VERIFIED | 451 lines, wired from `HomeScreen` |
| `ui/home/HomeSelection.kt` | selection bars, delete dialog | VERIFIED | `HomeSelectionBottomBar`, `DeleteMediaDialog` used in `HomeScreen` |
| `ui/home/HomeOverlays.kt` | pinch tutorial, vault hint | VERIFIED | `PinchTutorialOverlay`, `VaultHintOverlay` used. `HomeExitPopup` intentionally removed (db1fe85) |
| `ui/components/AddToAlbumSheet.kt` | shared add-to sheet | VERIFIED | used by Home and AlbumDetail |
| `AppPreferences.kt` session / hint / pinch flags | persisted flags | VERIFIED | `incrementSessionCount`, `setVaultHintToolTip` wired from Splash and NavHost |
| `PARITY-CHECKLIST.md` rows 6, 7, 10 | Ported ticked | VERIFIED (stale text) | `[x]` Ported; row 10 still names `HomeExitPopup` (see Gaps Summary) |

### Key Link Verification

| From | To | Via | Status |
|------|----|-----|--------|
| `SplashScreen.kt` | `AppPreferences` | `incrementSessionCount()` before routing | WIRED |
| `GalleryNavHost.kt` (VaultLock) | `AppPreferences` | `setVaultHintToolTip(true)` | WIRED |
| `HomeScreen.kt` | `PhotosTab` | `onColumnCountChange = viewModel::setColumnCount` | WIRED |
| `HomeScreen.kt` | `HomeShell.kt` | `HomeTabBar`, `settledBarOffset`, nested scroll | WIRED |
| `HomeSelection` Delete | `HomeViewModel` | `deleteSelected(deleteActionFor(checked))` | WIRED |
| `HomeScreen.kt` | `HomeOverlays.kt` | `VaultHintOverlay`, `PinchTutorialOverlay` | WIRED |

### Behavioral Spot-Checks

| Behavior | Command | Result | Status |
|----------|---------|--------|--------|
| Strings synced to 2.7.2 | `python -I scripts/ref_res.py check-strings` | 24 languages up to date | PASS |
| Colours match 2.7.2 | `python -I scripts/ref_res.py check-colors` | 188 light / 120 night match | PASS |
| Unit tests + `assembleDebug` | run by orchestrator on 18193d5 | pass | PASS (not re-run here) |
| Phone behaviour | Artemis Pro b48563a6 plus user approval | see truths 1 to 5 | PASS (device evidence from SUMMARY, not re-run here) |

### Requirements Coverage

| Requirement | Description | Status | Evidence |
|-------------|-------------|--------|----------|
| HOME-01 | Shell, tab order, top bars, ⋮ popup, ad-free button | SATISFIED | truths 1, 2 |
| HOME-02 | Vault hint when 2.7.2 shows it | SATISFIED | truth 3 |
| HOME-03 | Exit popup matches 2.7.2 | SATISFIED (override) | 2.7.2 has no popup; matching direct exit |
| PHO-01 | Photos tab: headers, items, ad rows, pinch, selection actions | SATISFIED | truth 5 |

No orphaned requirements.

### Anti-Patterns Found

None blocking. `exit_rectangle` drawable, `exitViewBackgroundColor` and the `tap_again_to_exit` string are now unused (info; Phase 12's unused-resource sweep). The ad-free icon has a `go_ads_free` content description where the reference has none (info, accessibility-positive).

### Human Verification Required

None outstanding; the user approved the device check.

### Gaps Summary

No blocking gaps. Non-blocking follow-ups for the orchestrator:

1. Document drift from the exit-popup decision: ROADMAP Phase 3 SC4 and REQUIREMENTS HOME-03 still read "shows the 2.7.2 exit popup"; 03-UI-SPEC section 1.6 and its "behaviour: exit popup vs direct exit" row, and PARITY-CHECKLIST row 10 (names `HomeExitPopup`, which no longer exists) should say Back exits directly. The accepted override above records this until the wording is changed.
2. ROADMAP Phase 3 plan 03-06 box is unticked, REQUIREMENTS HOME-01..03 and PHO-01 are still "Pending", and 03-06-SUMMARY.md (device check closed) is uncommitted.
3. Advisory (fast-scroll thumb): see frontmatter. Confirm in the Phase 13 side-by-side that the thumb matches 2.7.2, or hide it if the reference never shows one.
4. Light mode was the device-check mode; dark-mode selection bar and delete dialog were listed as outstanding in 03-06-LIVE-COMPARISON and are not mentioned as closed. Dark Photos and Albums were compared; the rest falls to Phase 13 (every row, light and dark).

---

_Verified: 2026-11-20_
_Verifier: Claude (gsd-verifier)_
