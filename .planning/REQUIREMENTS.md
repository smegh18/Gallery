# Requirements: Gallery, milestone v2.0 (Reference 2.7.2 parity)

**Defined:** 2026-11-13
**Core Value:** a user cannot tell our app from reference 2.7.2: same screens, flows, animations and behaviour.
**Source:** `.planning/milestones/v2.7.2-parity/PARITY-CHECKLIST.md`, built from the decompiled 2.7.2. Numbers like #6 refer to rows in that checklist.

Every UI requirement means: matches 2.7.2 in layout, text, icons, colours, light and dark mode, and behaviour.

## v1 Requirements

### Foundation (shared assets)
- [ ] **FND-01**: The app has every new or changed 2.7.2 drawable it uses (107 new, 5 changed), with brand art replaced by our own.
- [ ] **FND-02**: The app shows the 85 new and 8 changed 2.7.2 strings, in all supported languages.
- [ ] **FND-03**: Theme colours and text styles match 2.7.2 `values` and `values-night`.
- [ ] **FND-04**: Animations (Lottie) match 2.7.2. Ones 2.7.2 removed are no longer shipped unless still used.

### Onboarding
- [ ] **ONB-01**: First run goes Splash → Language → Permission → Feature promo → Home, with no Welcome screen (#1–#5).
- [ ] **ONB-02**: The splash screen matches 2.7.2's `activity_splash` (#1).
- [ ] **ONB-03**: The language screen matches 2.7.2's `activity_app_language` (#3).
- [ ] **ONB-04**: The permission screen and its new "permission needed" dialog match 2.7.2 (#4).
- [ ] **ONB-05**: The user sees the "Get more from your Gallery" feature-promo step. Enable asks for Phone and Display-over-other-apps; Not now skips (#5).

### Home shell
- [ ] **HOME-01**: The home shell matches 2.7.2's `activity_home`: tab order Photos / Album / Explore, top bars, the ⋮ popup menu (Sort, …) and the ad-free button (#6).
- [ ] **HOME-02**: The user sees the vault hint when 2.7.2 shows it (#6).
- [ ] **HOME-03**: The exit popup matches 2.7.2 (#10).

### Photos tab
- [ ] **PHO-01**: The Photos tab matches 2.7.2: date headers, grid items, ad rows, column pinch and selection actions (#7).

### Albums tab
- [ ] **ALB-01**: The Albums tab matches 2.7.2: the story row (Recent / Memories / Flashback / Random / Favorite plus an ad slot), the new album cards, and the Recent / Favorite / All Video / Camera / Screenshots / folders / Deleted grid, with no feature carousel (#8).
- [ ] **ALB-02**: Stories match 2.7.2: header, ad slots, the usage-access prompt afterwards, and auto-advance (#15).

### Explore tab
- [ ] **EXP-01**: The Explore tab matches 2.7.2:
  - Free up space hero with Scan now;
  - Create (AI Editor, Collage);
  - Library (Vault, Favorites, Moments, Place);
  - Manage (Duplicate Finder, Quick Organize, Recycle Bin, Settings).

  Each item opens the same screen as in 2.7.2 (#9).

### Viewing
- [ ] **VIEW-01**: The full-screen viewer and its bottom options row match 2.7.2 (#13).
- [ ] **VIEW-02**: The video player matches 2.7.2. Audio stops when it closes (#14).
- [ ] **VIEW-03**: These match 2.7.2: album/media list, all albums, Moments, Place, Animated, Burst, Favorites, Search, Search all and Search by colour (#16–#24).

### Manage and cleaner
- [ ] **CLN-01**: The system cleaner, junk scan / remove / cleared, large videos and screenshots screens match 2.7.2, including the video usage-access prompt (#25–#28).
- [ ] **CLN-02**: The duplicate flow (waiting → complete → photos → delete process → delete complete) matches 2.7.2 (#29).
- [ ] **CLN-03**: The Recycle Bin matches 2.7.2's changed item layout and its usage-access prompt (#30).
- [ ] **CLN-04**: Quick Organize and Organizer Recycle match 2.7.2's changed stripe and intro steps (#31).

### Vault
- [ ] **VLT-01**: Vault lock, media, settings, recovery question and forgot password match 2.7.2 (#32–#36).
- [ ] **VLT-02**: The in-app media picker matches 2.7.2's `activity_picker_screen` (#37).

### Create and edit
- [ ] **EDT-01**: The photo editor tools, crop, text, font and colour panels match 2.7.2's changed layouts (#38–#39).
- [ ] **EDT-02**: The collage picker, screen and saved flow match 2.7.2, including 7 changed panels and no collage home (#40).
- [ ] **EDT-03**: Remove BG, Blur BG, Colour Splash (each with its guide), Sticker and Wallpaper match 2.7.2 (#41–#45).

### Settings and system
- [ ] **SET-01**: Settings matches 2.7.2's `activity_setting`, including the "Share app usage stats" toggle and its dialog (#46).
- [ ] **SET-02**: Theme and About match 2.7.2. The privacy policy opens the 2.7.2 URL (#47–#48).
- [ ] **SET-03**: The Pro / subscription screen matches 2.7.2's `activity_subscription_purchase`, with no real billing (#49).
- [ ] **SET-04**: Notifications use 2.7.2's big and small layouts. Tapping one opens the matching screen (#52).
- [ ] **SET-05**: The app handles external "select media" requests and deep links like 2.7.2 (#53).

### Floating sidebar
- [ ] **FLT-01**: The user can turn on an edge handle, drawn over other apps, that opens the quick-tools sidebar (#11).
- [ ] **FLT-02**: The user sees the two-step sidebar intro the first time ("Your quick tools live here", "Put it where you like") (#11).
- [ ] **FLT-03**: The user can add or remove sidebar tools within 2.7.2's limits, plus the "Keep at least one tool" rule (#11).
- [ ] **FLT-04**: The floating button popup and the floating settings screen match 2.7.2 (#10–#12).

### After-call and install screens (GS)
- [ ] **GS-01**: After a call ends, the user sees 2.7.2's "Quick Info" card (Gs screen with games / media / menu tabs), if they enabled it (#50).
- [ ] **GS-02**: The user can manage these features in Gs Settings, including the permission-guide animation (#50).
- [ ] **GS-03**: When an app is installed, updated or uninstalled, the user sees 2.7.2's progress and complete screens, plus the IU settings (#51).
- [ ] **GS-04**: When the overlay permission is off, the user sees 2.7.2's "Quick Info disabled" notification (#50).

### Usage access
- [ ] **USG-01**: The user sees 2.7.2's usage-access dialog at the same trigger points (home, story, video, recycle) and in Settings. Allow opens system Usage Access; Not now dismisses. No usage data leaves the device.

### Cleanup
- [ ] **CLR-01**: Code 2.7.2 removed is gone from our app: Welcome screen, feature carousel, collage home, and Lotties no longer used.

### Verification (stage 2)
- [ ] **VER-01**: Every checklist row is ticked **Verified** after a side-by-side Artemis check on the phone, in light and dark mode.
- [ ] **VER-02**: Every flow in checklist section D behaves the same as 2.7.2 on the phone.
- [ ] **VER-03**: Each phase passes unit tests, assembleDebug, a CodeRabbit review with no open findings, and an unslop audit of its changed screens.

## Future requirements
- Real ad units and mediation (currently placeholders).
- Real Play Billing for the subscription screen.

## Out of Scope
| Feature | Reason |
|---|---|
| Ad SDKs (AppLovin, InMobi, Pangle, Mintegral, …) and their screens | No ad units provided; placeholders keep the layout |
| Sending usage stats anywhere | We copy the UI only. No data-collection SDK. |
| Reference branding and assets (logo, name) | Copyright: our own art replaces the reference's |

## Traceability

(Filled by the roadmap.)

---
*Requirements defined: 2026-11-13*
