---
phase: 10-floating-sidebar
plan: all
subsystem: ui
tags: [overlay, foreground-service, compose, sidebar]
status: complete (device pass skipped by user decision)
requirements-completed: [FLT-01, FLT-02, FLT-03, FLT-04]
key-files:
  created:
    - app/src/main/java/com/quizango/gallery/ui/floating/ (11 files)
    - app/src/test/java/com/quizango/gallery/FloatingSidebarTest.kt
  modified:
    - AndroidManifest.xml, ui/navigation/Screen.kt, ui/navigation/GalleryNavHost.kt (Phase 10 blocks)
    - MainActivity.kt, ui/home/HomeScreen.kt, ui/onboarding/FeaturePromoScreen.kt (hooks, see below)
    - res: strings (+24 locales), colors (light/night), themes, 13 drawables
---

# Phase 10: Floating Sidebar Summary

## Reference findings that drove the build
- Two services in 2.7.2: **FloatingButtonService** (specialUse FGS, `PROPERTY_SPECIAL_USE_FGS_SUBTYPE` "Floating quick access
  button overlay") is the sidebar. **FloatingViewService** (`floating_view_layout`, a PlayerView with resize/close/prev/play/next)
  is the video player's popup-play window, not the sidebar: out of scope here (see Left undone).
- Helpers decoded with jadx: `bo/d` switch + prefs, `bo/l` handle manager, `bo/s` panel, `bo/ak` + `FloatingSidebarIntroView` intro,
  `FloatingSidebarLayout` liquid neck, `bo/c` dialog, `bo/p` permission launcher, `bo/w` tools, `oa/l` + `ua/l` guide and polling.
- Prefs: SharedPreferences `assistive_touch_prefs` (kept, same keys): `assistive_touch_enabled` (false), `assistive_touch_hint_showed`,
  `home_dialog_shown_session`, `sidebar_actions` (csv keys), `sidebar_handle_y` (-1 = centred), `sidebar_on_left` (false),
  `sidebar_intro_shown_at`, `sidebar_intro_replayed`, `sidebar_nudge_count`, `sidebar_nudge_day`. `sidebar_opened` is read but never
  written by 2.7.2, so the intro replay and the nudges always happen; ours behaves the same without the key.
- On = enabled && `canDrawOverlays` (&& RC `enable_floating_button`, default true). Feature exists on Android 9+ except RC
  `overlay_unsupported_devices` (5 brand_model ids, defaults ported).
- Tools (`bo/w`, key / icon / label / tile colour): gallery (app logo), vault, favorite, moments, recycle_bin, search, camera,
  duplicate, organize, places, cleaner, settings. Default sidebar = first five; max 6; never fewer than 1.
- Targets: Gallery restarts the app (splash); Camera = `STILL_IMAGE_CAMERA`; others open through HomeActivity
  (`FLOATING_TARGET_INTENT`, CLEAR_TASK), Cleaner = Cleaner if junk cleaned today else Remove Junk; panel settings button =
  FloatingSettingActivity; launches happen 200 ms after the panel starts closing.
- Handle window: 32x120 dp touch strip (40 dp while dragged), flags 776, cut-out ALWAYS, gesture-exclusion rects; swipe in past
  slop opens the panel (fling 800 px/s or 35 % rule, 260 ms settle, cascade at 50 %, 700 ms wobble); long press (<=450 ms) picks it
  up, drop snaps to the nearer edge in 220 ms and saves side + y; tap = 7 dp peek; dims to 55 % after 3 s; nudge (2 peeks) from
  day 1 after the intro, max 3, once a day. The handle only shows while the app is in the background (ProcessLifecycleOwner
  stop + 500 ms) and goes away when one of its screens starts.
- Intro: dim 190/255 with a 70 dp spotlight on the handle, two rings (1.8 s), tools pop in a ring and fly into the handle
  (Bezier, spin, glow), then "Your quick tools live here" (swipe demo, preview card) and "Put it where you like" (long press +
  drag demo), two loops each, typed titles, dots, "Tap anywhere to continue/finish". Replay after 7 days: step 1 only, no dots.
- Notification: channel `floating_button_channel` (name = "Quick Access Floating Button", LOW), id 1101, title app_name, text the
  channel name, icon `notification_icon`, ongoing, tap opens the splash. Boot: `IUBootReceiver` (BOOT_COMPLETED,
  MY_PACKAGE_REPLACED) restarts it when on; plus a 15-min WorkManager keep-alive and an alarm restart on task removal.
- Home offer (RC `home_dialog_sessions` default): launch 1 (normal), 4 (forceful), 7 (normal), 10 (forceful), once per launch,
  only when off, media permission granted; a forceful refusal shows it again; the vault hint waits for it (o000o0O0).
- Enable flow (`bo/p`): (phone perms for after-call, see deviations) -> `canDrawOverlays` ? enable : open
  `MANAGE_OVERLAY_PERMISSION`, guide activity after 1 s, poll 200 ms and bring the app back on grant. FeaturePromo's Enable and
  Settings' floating row use the same launcher; Settings opens FloatingSettingActivity when the overlay is already allowed,
  else the (non-forceful) dialog first.
- FloatingSettingActivity: Always ON (permission flow) / Always OFF (rewarded ad, then disable + cancel keep-alive + stop).

## What was built (checklist rows #11, #12)
| Row | Status | Files |
|---|---|---|
| #11 handle + panel + edit (FLT-01, FLT-03) | built | `FloatingButtonService.kt`, `SidebarHandle.kt` (plain Views), `SidebarPanel.kt`, `OverlayWindow.kt`, `FloatingButtons.kt`, `FloatingTool.kt`, `FloatingBootReceiver.kt` |
| #11 intro (FLT-02) | built, close | `SidebarIntro.kt` |
| #11 dialog_floating_button_popup + Home offer (FLT-04) | built | `FloatingButtonOffer.kt` (+ HomeScreen hook) |
| #11 overlay guide (dialog_overlay_guide_anime) | built | `OverlayGuideActivity.kt`, `rememberOverlayPermissionFlow` |
| #12 FloatingSettingActivity (FLT-04) | built | `FloatingSettingScreen.kt`, route `Screen.FloatingSetting` ("floating_setting") |

Overlay approach: the panel and intro are ComposeViews in overlay windows with their own LifecycleOwner + SavedStateRegistryOwner
(`ComposeOverlay`), so they reuse the house `GalleryText`/`sdp`/`colorResource` instead of duplicating layouts and text styles
in XML; `remove()` destroys the lifecycle, so compositions and animations end with the window. The handle is plain Views: its
window is moved and resized on every touch move, which a raw touch listener handles most directly. All windows are hardware
accelerated (needed for Compose layers and shadows) and removed in `SidebarHandle.remove()` / service `onDestroy` (no view leaks);
rotation rebuilds the handle from `onConfigurationChanged`.

## Hooks in other phases' files (integrator)
- `MainActivity.kt` (Phase 9): reads `FloatingButtons.EXTRA_ROUTE` ("floating_route"); with it the NavHost starts on Home and
  navigates to the route once (`savedInstanceState == null`). `GalleryNavHost` got `startRoute` (default Splash).
- `ui/home/HomeScreen.kt` (2 lines + 1 call): `rememberFloatingButtonOffer(uiState.sessionCount)`; `showVaultHint` also needs
  `!floatingOffer.isShowing`; `FloatingButtonOfferDialog(floatingOffer)` after the root Box. The shortcut comment at
  `shouldShowVaultHint` in `HomeShell.kt` still names Phase 10: left untouched to avoid a merge conflict, drop "10" when merging.
- `ui/onboarding/FeaturePromoScreen.kt` (Phase 11): two one-line `FloatingButtons.enable(context)` calls where the overlay
  permission is found granted (after the settings return, and when it already was).
- Settings (Phase 9): wire `onOpenFloatingSettings` to
  `rememberFloatingSettingsEntry { navController.navigateOnce(Screen.FloatingSetting.route) }` inside the Settings destination
  (opens the screen when the overlay is allowed, else the dialog first, as SettingActivity does).
- Order on Home: 2.7.2 runs the notification-permission request before the offer; if Phase 9 adds that request, gate the offer on it.

## Deviations from 2.7.2
1. Enable flow skips the CALL_PHONE + READ_PHONE_STATE request 2.7.2 makes first for its after-call feature (Phase 11 owns
   that; marked `shortcut:` in `rememberOverlayPermissionFlow`).
2. Remote config not fetched: `enable_floating_button`, `home_dialog_sessions`, `overlay_unsupported_devices` use their defaults.
3. No WorkManager keep-alive and no alarm restart on task removal (no WorkManager dependency): START_STICKY, the boot/update
   receiver and Home's start-if-on cover it.
4. Foreground detection: ActivityLifecycleCallbacks plus the process importance at service start (lifecycle-process is not on
   the classpath); the handle shows 1.2 s after the last screen stops (PLO 700 ms + 500 ms).
5. Sidebar targets open MainActivity with Home under them instead of HomeActivity + FLOATING_TARGET_INTENT.
6. Always OFF's rewarded ad is the AdManager placeholder (immediate); the reward icon still shows (no premium).
7. Overlay windows use absolute TOP|LEFT gravity (2.7.2's START mirrors x in RTL locales).
8. Intro animations follow the 2.7.2 timings; a few interpolators are approximated (keyframed drag, decelerate factors).
9. The settings card background is the house `cardBackground` (night #1E1E1E vs MDC surface #121212).
10. Analytics events and the unused `clean_now_progress` counter are not ported.
11. Tile colours, the tile gloss and the dim/scrim blacks are literals, as in 2.7.2's code.

## Tests added
`FloatingSidebarTest` (11 tests): default set, csv parsing (unknown/duplicate keys, max 6, empty -> default), last-tool refusal,
add limit and no-op re-add, every tool's route (Cleaner both ways), Home offer sessions/forceful/once-per-launch, device and SDK
availability, snap side, handle y clamp, release open/close rule, intro replay and nudge rules.

## Gates
`testDebugUnitTest` pass; `assembleDebug` pass; `check-strings` 24 languages up to date; `check-colors` 228 light / 136 night
match; `git diff --check` clean. Not run: device/Artemis pass (user decision), CodeRabbit.

## Left undone
- FloatingViewService popup video player (`floating_view_layout`); `VideoPlayerScreen`'s floating button stays a no-op.
- Possible duplicate keys at merge: `always_on`, `always_off`, `iu_setting_sub_text`, `install_setting_card_stroke` are 2.7.2
  names Phase 11 (IU settings) may add too; keep one copy.
