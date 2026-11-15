# Roadmap: Gallery App Reconstruction

## Milestones

- 🚧 **v2.0 Reference 2.7.2 parity** - Phases 1-13 (in progress)

## Overview

Bring the reconstructed gallery app from reference 2.6.7 parity to 100% parity with reference 2.7.2 (versionCode 152): every screen, flow, animation and behaviour. Stage 1 (Phases 1-12) ports from the decompiled 2.7.2. It starts with the shared drawables, strings and theme, then onboarding, then the screen areas outward from Home, then the new overlay features, and finally the usage-access prompts plus a sweep of removed code. Stage 2 (Phase 13) verifies every checklist row side by side with Artemis on the phone, in light and dark mode.

Source of truth for screen scope: `.planning/milestones/v2.7.2-parity/PARITY-CHECKLIST.md` (rows `#1`-`#53`, sections A-E).

**Cross-cutting rules (apply to every phase):**
- **Quality gate (VER-03):** a phase is not complete until unit tests pass, `assembleDebug` succeeds, a CodeRabbit review has no open findings, and an unslop audit of its changed screens is done. The requirement is mapped to Phase 13 for traceability, but it gates Phases 1-13 individually.
- **Ported ticks:** each phase ticks **Ported** for its checklist rows. Phase 13 ticks **Verified**.
- **Usage-access hooks:** ALB-02, CLN-01, CLN-03 and SET-01 port their screens up to the point where the usage-access dialog appears and leave that trigger as a no-op. The dialog itself and every trigger are delivered together in Phase 12 (USG-01).
- **Removed 2.7.2 code:** each phase deletes what its replacement makes obsolete (Welcome in Phase 2, feature carousel in Phase 4, collage home in Phase 8, retired Lotties where they were used). Phase 12 owns CLR-01 as the final sweep.
- **Ads, billing, brand:** ads stay placeholders, there is no real billing, and reference logo/name art is replaced with our own.
- **Checklist decisions D1-D4:** resolved as "port" by REQUIREMENTS.md (Phases 2, 10, 11, 12). D5 (ads) stays placeholder.

## Phases

**Phase Numbering:**
- Integer phases (1, 2, 3): Planned milestone work
- Decimal phases (2.1, 2.2): Urgent insertions (marked with INSERTED)

Decimal phases appear between their surrounding integers in numeric order.

- [x] **Phase 1: Foundation** - Shared 2.7.2 drawables, strings, theme and Lottie assets (completed 2026-11-13)
- [ ] **Phase 2: Onboarding** - Splash, Language, Permission and the new Feature-promo step; Welcome removed
- [ ] **Phase 3: Home Shell and Photos Tab** - 2.7.2 home shell, popup menu, vault hint, exit popup and Photos tab
- [ ] **Phase 4: Albums, Stories and Explore** - New Albums tab and story row, stories viewer, and the Explore tab
- [ ] **Phase 5: Viewing and Browsing** - Full-screen viewer, video player and the album, discovery and search lists
- [ ] **Phase 6: Manage and Cleaner** - System cleaner, duplicate flow, Recycle Bin and Quick Organize
- [ ] **Phase 7: Private Vault** - Vault lock, media, settings, recovery and the media picker
- [ ] **Phase 8: Create and Edit** - Photo editor, crop, collage and the creative tools
- [ ] **Phase 9: Settings and System** - Settings, Theme, About, Pro screen, notifications, external intents and deep links
- [ ] **Phase 10: Floating Sidebar** - Over-other-apps edge handle with the quick-tools sidebar
- [ ] **Phase 11: After-Call and Install Screens** - Quick Info after calls, install/update/uninstall screens and their settings
- [ ] **Phase 12: Usage Access and Cleanup** - Usage-access dialogs at every trigger point, plus the removed-code sweep
- [ ] **Phase 13: Device Verification and Quality Gates** - Artemis side-by-side checks of every row and flow, light and dark

## Phase Details

### Phase 1: Foundation

**Goal**: Every screen that follows can use the 2.7.2 drawables, strings, theme and animations, so porting a screen never has to stop to add a shared asset.
**Depends on**: Nothing (first phase)
**Requirements**: FND-01, FND-02, FND-03, FND-04
**Checklist rows**: Section C (shared parts)
**Success Criteria** (what must be TRUE):
  1. All 107 new and 5 changed 2.7.2 drawables exist in the app and draw correctly in light and dark mode, with the reference logo/name art replaced by our own.
  2. With the device language switched across every supported language, the 85 new and 8 changed 2.7.2 strings appear translated (for example "Select all" and "Close" use the 2.7.2 wording), translated exactly where 2.7.2 translates them; keys 2.7.2 itself leaves in English (album_tab_*, close_button, explore_tab_ai_editor, onb_feature_*, some gs2_*) stay English for parity.
  3. Existing screens render with the 2.7.2 colours and text styles in both light and dark mode, with no visual regression and a clean `assembleDebug`.
  4. The Lottie animations still in use match their 2.7.2 copies (including the changed `app_setings`), and Lotties 2.7.2 removed are no longer shipped unless a screen still uses them.

**Plans**: 5/5 plans complete

Plans:
- [x] 01-01-PLAN.md — Tracer: Home "Select all" in 2.7.2 wording/accent/Inter/language (strings sync script, #0066FF, Inter), then sdp/ssp helper and 23 text styles (wave 1)
- [x] 01-02-PLAN.md — Lotties to 2.7.2: delete-complete on dm_success, retired Lottie removed, 13 more imported (wave 1)
- [x] 01-03-PLAN.md — 2.7.2 colours light/dark, 26 sdp buckets, 106 drawables, splash_bg window background (wave 2)
- [x] 01-04-PLAN.md — 2.7.2 English strings (74 new, 8 changed) and translations for 24 languages (wave 2)
- [x] 01-05-PLAN.md — Quality gates: CodeRabbit, unslop, release build, on-device light/dark/language check (wave 3, checkpoint)

**UI hint**: yes

### Phase 2: Onboarding

**Goal**: A first-time user goes through the exact 2.7.2 launch sequence and ends on Home, with no Welcome screen.
**Depends on**: Phase 1
**Requirements**: ONB-01, ONB-02, ONB-03, ONB-04, ONB-05
**Checklist rows**: #1-#5
**Success Criteria** (what must be TRUE):
  1. On a fresh install the app goes Splash, Language, Permission, Feature promo, Home, and the Welcome screen never appears; Back and relaunch behave as in 2.7.2.
  2. The Splash and Language screens match 2.7.2 in layout, text, icons and colours, in light and dark mode.
  3. The Permission screen matches 2.7.2, and refusing the permission shows the new "permission needed" dialog with 2.7.2's layout and actions.
  4. The "Get more from your Gallery" step matches 2.7.2: Enable asks for Phone and Display-over-other-apps, Not now skips, and both paths land on Home.

**Plans**: 5 plans
- [x] 02-01-PLAN.md — Import assets, add dialog strings & colors, retire WelcomeScreen, add AppPreferences onboarding flag (wave 1)
- [x] 02-02-PLAN.md — Align SplashScreen to 2.7.2 layout and add onboarding mode with tick button & hand Lottie to LanguageScreen (wave 2)
- [x] 02-03-PLAN.md — Build PermissionNeededDialog and reconstruct PermissionScreen to 2.7.2 layout (wave 2)
- [x] 02-04-PLAN.md — Build FeaturePromoScreen and wire full onboarding navigation flow (wave 3)
- [x] 02-05-PLAN.md — Quality gates: unit tests, assembleDebug, parity scripts, CodeRabbit review, unslop audit, and verification report (wave 4)

**UI hint**: yes

### Phase 3: Home Shell and Photos Tab

**Goal**: Home looks and behaves like 2.7.2 around its tabs, and the Photos tab shows the 2.7.2 timeline.
**Depends on**: Phase 1
**Requirements**: HOME-01, HOME-02, HOME-03, PHO-01
**Checklist rows**: #6, #7, #10
**Success Criteria** (what must be TRUE):
  1. Home shows the tabs in 2.7.2 order (Photos / Album / Explore) with the 2.7.2 top bars, and switching tabs by tap or swipe works with the same titles and default tab.
  2. The ⋮ popup menu (Sort and the other entries) and the ad-free button match 2.7.2, and each menu entry does what it does in 2.7.2.
  3. The vault hint appears in the same situations and place as in 2.7.2.
  4. Pressing Back on Home shows the 2.7.2 exit popup.
  5. The Photos tab shows 2.7.2's date headers, grid items and ad-placeholder rows; pinching changes the column count; long-press enters selection mode with the 2.7.2 actions.

**Plans**: 6 plans

Plans:
- [x] 03-01-PLAN.md — Tracer: Home opens on Photos with the 2.7.2 tab bar and titles (all Phase 3 strings, colours, drawables imported); loading spinner, 3..5 columns, half-width bug; per-tab top bars, ⋮ popup, title long-press (wave 1)
- [x] 03-02-PLAN.md — Photos tab content: grid geometry, 2.7.2 date headers and cells, ad-row skeleton, empty state, fast-scroll thumb (wave 2)
- [x] 03-03-PLAN.md — Selection mode bars, delete confirmation, Add to / Create album, hide-on-scroll tab bar and sync line (wave 2)
- [x] 03-04-PLAN.md — Pinch to change columns with live preview, first-run pinch tutorial (wave 3)
- [x] 03-05-PLAN.md — Vault hint (sessions 4/8/12) and the Home exit popup with Back order (wave 4)
- [ ] 03-06-PLAN.md — Quality gates: CodeRabbit, unslop, parity scripts, checklist ticks, on-device side-by-side check (wave 5, checkpoint)
**UI hint**: yes

### Phase 4: Albums, Stories and Explore

**Goal**: The Albums and Explore tabs and the stories viewer look and behave like 2.7.2, and every Explore item opens the right screen.
**Depends on**: Phase 3
**Requirements**: ALB-01, ALB-02, EXP-01
**Checklist rows**: #8, #9, #15
**Success Criteria** (what must be TRUE):
  1. The Albums tab shows the story row (Recent / Memories / Flashback / Random / Favorite plus an ad slot), the new album cards, and the Recent / Favorite / All Video / Camera / Screenshots / folders / Deleted grid, with no feature carousel.
  2. Opening a story shows 2.7.2's header and ad slots, auto-advances to the next photo and story, and responds to left/right taps and long-press pause.
  3. The Explore tab shows the Free up space hero with Scan now, Create (AI Editor, Collage), Library (Vault, Favorites, Moments, Place) and Manage (Duplicate Finder, Quick Organize, Recycle Bin, Settings), laid out as in 2.7.2.
  4. Every Explore item opens the same screen it opens in 2.7.2.

**Plans**: TBD
**UI hint**: yes

### Phase 5: Viewing and Browsing

**Goal**: Opening, playing, listing and searching media feels the same as 2.7.2.
**Depends on**: Phase 4
**Requirements**: VIEW-01, VIEW-02, VIEW-03
**Checklist rows**: #13, #14, #16-#24
**Success Criteria** (what must be TRUE):
  1. The full-screen viewer swipes between photos, zooms and double-taps as in 2.7.2, and its bottom options row (info, favourite, delete, hide, share, edit) matches the 2.7.2 layout and works.
  2. The video player matches 2.7.2, and closing the viewer stops the audio.
  3. The album/media list and the all-albums screen match 2.7.2 and open the right media.
  4. Moments, Place, Animated, Burst and Favorites each match 2.7.2, including their empty states.
  5. Search, Search all and Search by colour match 2.7.2 and return the same kind of results.

**Plans**: TBD
**UI hint**: yes

### Phase 6: Manage and Cleaner

**Goal**: Every cleaning and organizing flow runs end to end with the 2.7.2 screens and completion states.
**Depends on**: Phase 4
**Requirements**: CLN-01, CLN-02, CLN-03, CLN-04
**Checklist rows**: #25-#31
**Success Criteria** (what must be TRUE):
  1. The system cleaner runs junk scan, remove junk and junk cleared as in 2.7.2, and the large videos and screenshots screens match 2.7.2.
  2. The duplicate flow runs waiting, complete, photos, delete process and delete complete, each step looking and behaving like 2.7.2.
  3. The Recycle Bin uses the changed 2.7.2 item layout with days left, restore, delete and empty all working.
  4. Quick Organize and Organizer Recycle show 2.7.2's changed stripe and intro steps.

**Plans**: TBD
**UI hint**: yes

### Phase 7: Private Vault

**Goal**: The private vault looks and behaves like 2.7.2, from first setup to adding media with the in-app picker.
**Depends on**: Phase 4
**Requirements**: VLT-01, VLT-02
**Checklist rows**: #32-#37
**Success Criteria** (what must be TRUE):
  1. First use walks through PIN or pattern setup and the recovery question, then unlock works by PIN, pattern or fingerprint, all matching 2.7.2.
  2. The vault media screen hides and unhides media and matches 2.7.2.
  3. Vault settings let the user change the password, and forgot password recovers access through the recovery question, as in 2.7.2.
  4. The in-app media picker used to add media to the vault matches 2.7.2's `activity_picker_screen`.

**Plans**: TBD
**UI hint**: yes

### Phase 8: Create and Edit

**Goal**: The photo editor, collage maker and creative tools match 2.7.2 and save their results.
**Depends on**: Phase 5
**Requirements**: EDT-01, EDT-02, EDT-03
**Checklist rows**: #38-#45
**Success Criteria** (what must be TRUE):
  1. The photo editor tool list, crop, text, font and colour panels match 2.7.2's changed layouts and apply edits correctly.
  2. Collage goes straight from the Explore Create item to the picker, then the collage screen with its seven changed panels, then the saved screen, with no collage home screen.
  3. Remove BG, Blur BG and Colour Splash match 2.7.2, each showing its guide the first time.
  4. The Sticker and Wallpaper screens match 2.7.2, and saving or applying a result works.

**Plans**: TBD
**UI hint**: yes

### Phase 9: Settings and System

**Goal**: Settings and the system-level entry points match 2.7.2, including how the app is opened from outside.
**Depends on**: Phase 8
**Requirements**: SET-01, SET-02, SET-03, SET-04, SET-05
**Checklist rows**: #46-#49, #52, #53
**Carried in from Phase 3**: wire the Home ad-free button (`ivPro`, today a no-op in `ui/home/HomeShell.kt`) to the new Pro screen (`HA.Ooooo00`); when Home is opened from a notification, widget or deep link, skip the vault hint (`HA.o000O00O`, shortcut comment at `shouldShowVaultHint`); hold the vault hint while the Home notification-permission request is showing.
**Success Criteria** (what must be TRUE):
  1. Settings matches 2.7.2's `activity_setting`, each row and toggle works, and the "Share app usage stats" toggle is present.
  2. Theme (Light / Dark / System) and About match 2.7.2, and the privacy policy opens the 2.7.2 URL.
  3. The Pro screen matches 2.7.2's `activity_subscription_purchase`, and no purchase or charge can happen.
  4. Notifications use the 2.7.2 big and small layouts, and tapping one opens the matching screen.
  5. Another app asking to select media gets the in-app picker and its result, and 2.7.2's deep links open the right screen.

**Plans**: TBD
**UI hint**: yes

### Phase 10: Floating Sidebar

**Goal**: The user can run the 2.7.2 quick-tools sidebar from an edge handle drawn over other apps.
**Depends on**: Phase 9 (also uses the overlay permission asked for in Phase 2)
**Requirements**: FLT-01, FLT-02, FLT-03, FLT-04
**Checklist rows**: #11, #12
**Carried in from Phase 3**: if the floating-button dialog can open on Home at start, hold the vault hint until it closes (`isVaultHintShowPending`, shortcut comment at `shouldShowVaultHint` in `ui/home/HomeShell.kt`).
**Success Criteria** (what must be TRUE):
  1. After granting Display-over-other-apps, the user can turn on an edge handle that stays over other apps and opens the quick-tools sidebar when used; turning it off removes the handle.
  2. The first time the sidebar opens, the two-step intro shows ("Your quick tools live here", "Put it where you like").
  3. The user can add and remove sidebar tools within 2.7.2's limits, and removing the last tool is refused with the "Keep at least one tool" message.
  4. The floating button popup and the floating settings screen match 2.7.2.

**Plans**: TBD
**UI hint**: yes

### Phase 11: After-Call and Install Screens

**Goal**: The 2.7.2 after-call Quick Info and app install/update/uninstall screens appear when enabled and can be managed in their settings.
**Depends on**: Phase 10
**Requirements**: GS-01, GS-02, GS-03, GS-04
**Checklist rows**: #50, #51
**Success Criteria** (what must be TRUE):
  1. With the feature enabled and permissions granted, ending a call shows the "Quick Info" card with its games, media and menu tabs.
  2. Gs Settings lets the user turn each feature on or off and shows the permission-guide animation when a permission is missing.
  3. Installing, updating or uninstalling an app shows 2.7.2's progress screen and then its complete screen, and the IU settings control them.
  4. With the overlay permission off, the "Quick Info disabled" notification appears as in 2.7.2.

**Plans**: TBD
**UI hint**: yes

### Phase 12: Usage Access and Cleanup

**Goal**: The usage-access prompts appear everywhere 2.7.2 shows them, and nothing 2.7.2 removed is left in the app.
**Depends on**: Phase 11 (hosts the prompts ported in Phases 3, 4, 6 and 9)
**Requirements**: USG-01, CLR-01
**Checklist rows**: #6, #15, #27, #30, #46, section E
**Carried in from Phase 3**: the Home usage-access popup must hold the vault hint until it closes (`isVaultHintShowPending`), and the final sweep checks every vault-hint show condition of 03-UI-SPEC section 1.4 is wired: no hint after notification/widget/deep-link entry (Phase 9) and none while any start dialog is up (Phases 9, 10, 12). Also sweep the drawer-only drawables left unused when Phase 3 removed the Home drawer.
**Success Criteria** (what must be TRUE):
  1. The usage-access dialog appears at 2.7.2's trigger points: Home, after a story, the large-video screen, the Recycle Bin, and from the Settings "Share app usage stats" toggle.
  2. Allow opens the system Usage Access screen, and Not now dismisses the dialog without blocking the screen.
  3. No usage data leaves the device: the app contains no usage-stats collection SDK and makes no upload.
  4. The Welcome screen, feature carousel, collage home and unused Lotties are gone, and no route, string or resource still refers to them.

**Plans**: TBD
**UI hint**: yes

### Phase 13: Device Verification and Quality Gates

**Goal**: Every screen and flow is proven identical to 2.7.2 on the phone, and every phase has passed its quality gates.
**Depends on**: Phase 12
**Requirements**: VER-01, VER-02, VER-03
**Checklist rows**: all (#1-#53, section D flows 1-15)
**Success Criteria** (what must be TRUE):
  1. Every checklist row is ticked **Verified** after an Artemis side-by-side check against reference 2.7.2 on the phone, in light and dark mode.
  2. Every flow in checklist section D (first run through edge cases) behaves the same as 2.7.2 on the phone.
  3. Every difference found is fixed and re-checked, or recorded as an accepted deviation (ad placeholders, no billing, our own brand art).
  4. The quality gate holds for every phase, 1 through 13 (not only this one): each passed unit tests, `assembleDebug`, a CodeRabbit review with no open findings, and an unslop audit of its changed screens before being marked complete.

**Plans**: TBD

## Progress

**Execution Order:**
Phases execute in numeric order: 1 → 2 → 3 → … → 13

| Phase | Plans Complete | Status | Completed |
|-------|----------------|--------|-----------|
| 1. Foundation | 5/5 | Complete    | 2026-11-13 |
| 2. Onboarding | 0/TBD | Not started | - |
| 3. Home Shell and Photos Tab | 0/6 | Planned | - |
| 4. Albums, Stories and Explore | 0/TBD | Not started | - |
| 5. Viewing and Browsing | 0/TBD | Not started | - |
| 6. Manage and Cleaner | 0/TBD | Not started | - |
| 7. Private Vault | 0/TBD | Not started | - |
| 8. Create and Edit | 0/TBD | Not started | - |
| 9. Settings and System | 0/TBD | Not started | - |
| 10. Floating Sidebar | 0/TBD | Not started | - |
| 11. After-Call and Install Screens | 0/TBD | Not started | - |
| 12. Usage Access and Cleanup | 0/TBD | Not started | - |
| 13. Device Verification and Quality Gates | 0/TBD | Not started | - |
