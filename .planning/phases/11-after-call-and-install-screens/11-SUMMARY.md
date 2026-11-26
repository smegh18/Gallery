---
phase: 11-after-call-and-install-screens
requirements: [GS-01, GS-02, GS-03, GS-04]
checklist_rows: [50, 51]
decision: D3 ported
status: built, automated gates green, device pass not run (user decision)
---

# Phase 11 Summary: After-Call and Install Screens

## Reference findings that drove the build

**After-call (GS)**
- Trigger: manifest `GsSetupCardReceiver` on `android.intent.action.PHONE_STATE` (exported=false, priority 999). PHONE_STATE is exempt from the implicit-broadcast limits and only arrives with READ_PHONE_STATE. No TelephonyCallback/CallScreeningService anywhere.
- Gate order: `afterCallScreenEnabled` (default **true**) -> RC `gs2_controller.gs_sdk` (default true) -> duplicate state skipped (`lastPhoneState` static) -> daily cap `daily_show_limit_gs` (default **-1**, unlimited).
- RINGING: start=end=0. OFFHOOK: start=now if 0, end=0. IDLE: end=now (prefs `gsCallStartMs`, `gsCallEndMs`). No minimum call length, no cooldown.
- RINGING/OFFHOOK with the overlay permission add a 1x1 transparent overlay window (bs/d) so the screen can start from the background when the call ends; removed 1 s after the launch.
- IDLE: no overlay and app not in front -> "Quick Info disabled" notification; screen locked -> "Conversation ended / See details" notification (tap opens Quick Info) and a USER_PRESENT receiver that opens it 300 ms after unlock; app in front -> start directly; else start through the overlay anchor (or a 500 ms transparent popup first, bs/b).
- Notifications: channel `GS_CARD_CHANNEL_ID` (name "Quick Info Card", importance HIGH), id 129 for both, big text, default sound, auto-cancel. "Quick Info disabled" opens Home with `key_notification_intent=gs_permission`, which runs the call-permission flow.
- GsActivity: own task (`gs1.core` affinity, singleTask, excludeFromRecents), keeps screen on, cancels notification 129. Header: avatar, always "Private Number" (the caller name view is never bound), "Duration · mm:ss" (minutes wrap at the hour; 00:00 if never answered), green call (ACTION_DIAL) and blue message (APP_MESSAGING selector) circles, Photos / Settings buttons (open Home with keys "Photos" / "call end setting"). Tabs Albums | More | Games.
  - Albums (fragment_gs_media): 3-column grid of All Photos, Videos, Camera, Screenshots, Favorite, each only with count > 0 and a cover.
  - More (fragment_gs_menu): View Moments, Favorite, Collage Maker, Videos rows (no row padding: the 18 sdp icon sets the height).
  - Games (fragment_gs_games): 5-column cross-promo grid from RC `play_zen_game_data` (third-party games, remote icons); empty state when absent.
  - Bottom native ad (gnt_gs_medium_template_view), banner card GONE.
- GsSettingActivity: "After Call Feature", Always ON / Always OFF cards writing `afterCallScreenEnabled`. Opened from Settings only when CALL_PHONE + READ_PHONE_STATE and overlay are granted; otherwise Settings shows `dialog_gs_settings` and Allow runs bu/c: request Phone -> overlay settings with the overlay guide; a 2nd Phone denial (`callPermissionDenyCount`) or 2 overlay refusals show `dialog_permission_needed`, whose button opens App info with `dialog_gs_permission_guide_anime` (app_setings Lottie) or overlay settings with `dialog_overlay_guide_anime`. Guides are a translucent GuideActivity shown 1 s after the system page, closed by touch or after 6 s; the overlay helper polls every 200 ms and brings the app back once granted.

**Install/uninstall (IU)**
- `PackageMonitorService`: special-use FGS (channel `PackageMonitorChannel` "Package", LOW; text "Monitoring app changes"), runtime receiver for PACKAGE_ADDED/REMOVED/REPLACED (scheme package). Started from HomeActivity.onResume, IUBootReceiver (BOOT_COMPLETED, MY_PACKAGE_REPLACED) and a 15 min WorkManager keep-alive, only with the overlay permission, not ad-free, `is_pms_service_stop` false.
- Receiver order: valid action + data, not ad-free, **network available**, own package skipped, intermediate REPLACING broadcasts skipped, same action+package within 5 s skipped, keyguard locked -> skip, no overlay -> skip, updates limited by `gs2_screen_cooldown_minutes` (30, static elapsedRealtime from 0), daily `daily_show_limit_gs2` (-1), mode `notification_mode` ("Always ON" default). In front -> start; else transparent popup 500 ms then start.
- Screen: icon (app icon 62 sdp with 22% corners; uninstall uses ic_gs2_default_app), title/subtitle/progress title per event, 0-100% over 4 s (ValueAnimator accelerate-decelerate). Complete: ic_gs2_success scales 0.6->1 in 350 ms overshoot, title/subtitle change, "You're all set!" status for 1.5 s, then Open (launch app) / Done. Back ignored until complete. Vault promo card (Try It Now -> vault) where the top ad fails; settings gear -> IU settings.
- IU settings: Always ON / Always OFF (also starts/stops the service).

## What was built (files)

| Row | Status | Files |
|-----|--------|-------|
| #50 Quick Info (GS-01) | built | `ui/gs/GsSetupCardReceiver.kt`, `ui/gs/GsLaunch.kt` (overlay anchor, notifications, intents), `ui/gs/QuickInfoActivity.kt` (screen, tabs, albums, more), `ui/gs/GsRules.kt` |
| #50 Gs Settings + guide (GS-02) | built | `ui/gs/GsSettingScreens.kt` (`GsSettingScreen`), `ui/gs/GsPermissions.kt` (dialog_gs_settings, permission flow, openers, `GsAppEffects`), `ui/gs/PermissionGuideActivity.kt` |
| #51 IU screens + settings (GS-03) | built | `service/PackageMonitorService.kt` (+ `IuBootReceiver`), `ui/iu/InstallUninstallActivity.kt`, `GsSettingScreens.kt` (`IuSettingScreen`) |
| #50 Quick Info disabled (GS-04) | built | `GsLaunch.kt` `postOverlayDisabledNotification`, routed by `GsAppEffects` |

Shared edits (one Phase 11 block each): `AppPreferences` (afterCallScreenEnabled, gsCallStart/EndMs, callPermissionDenyCount, iuAlwaysOn), `Screen.kt` (`GsSetting` "gs_setting", `IuSetting` "iu_setting"), `GalleryNavHost.kt` (two destinations + one `GsAppEffects(navController)` line before `NavHost`), manifest (FOREGROUND_SERVICE, FOREGROUND_SERVICE_SPECIAL_USE, RECEIVE_BOOT_COMPLETED, MAIN/LAUNCHER `<queries>`, 3 activities, 2 receivers, 1 service), strings (22 reference keys + 3 English-only), colours (black_color, install_setting_card_stroke, iu_setting_sub_text, bg_overlay_dialog_top, light+night), themes (`Theme.NewGallery.Gs`, `Theme.NewGallery.Guide`), drawables ic_iu_setting, ic_iu_private (+night), img_overlay_dialog. `ui/onboarding/FeaturePromoScreen.kt`: one hook line, the overlay guide (and auto-return) after opening the overlay page.

Why activities: Quick Info and the IU screen start from the background and live in their own tasks so Back returns to the dialer / previous app, as in 2.7.2; a NavHost route would open the main task (and the splash). Their buttons open the app through `appRouteIntent(route)` (MainActivity extra `gs_route`), which `GsAppEffects` navigates once Home is on the back stack.

## Deviations from 2.7.2

- Games tab: shows 2.7.2's own empty state ("No items"); the remote Playzen cross-promo list (third-party game names and icons, network) is not reproduced. `item_gs_game` unused.
- Ads: no native/banner/interstitial/full-screen ads (NativeAdCard renders nothing), so the IU top area always shows the vault card (2.7.2's ad-failed fallback) and Open/Done act at once (2.7.2 shows the `fsn_done` ad first).
- Remote config: defaults are hard-wired (gs_sdk/enable_gs2_sdk on, daily caps off, update cooldown 30 min). No analytics events.
- Permissions: only READ_PHONE_STATE is requested (2.7.2 also asks CALL_PHONE, same system Phone dialog; not needed since the call button uses ACTION_DIAL). CALL_PHONE, ANSWER_PHONE_CALLS, MANAGE_OWN_CALLS are not declared.
- IU prefs: `notification_mode` and `is_pms_service_stop` merged into one `iuAlwaysOn` (2.7.2 always flips them together).
- No WorkManager keep-alive (not a dependency here); the monitor restarts on app resume, boot and app update, and is START_STICKY.
- "Photos" (header and All Photos card) returns to Home on its current tab; 2.7.2 also switches Home to the Photos tab. Camera/Screenshots/Videos open the album list; Settings opens our Settings screen.
- Notification small icon: generic `ic_gs_photo` instead of 2.7.2's brand vector (shortcut comment in code); `img_overlay_dialog` is copied as-is (contains the current app logo, same pre-release brand-art todo as Phase 1).
- Overlay anchor is 1x1 not-touchable for both paths (2.7.2's popup is a tiny touchable window); guide polling stops after 2 min (2.7.2 polls until granted).
- Home session prompt `home_controller.show_call_permission_dialog` (sessions 1-5, forced on 4) is not built (Home wiring, outside this row).
- Settings rows themselves belong to Phase 9 (see hooks). Call/message buttons have no content description, as in 2.7.2.
- Faithful quirks kept: an outgoing call right after another keeps the earlier start (only RINGING clears it); no update screen in the first 30 min after boot; IU needs a network connection.

## Play-policy / platform notes

- Special-use foreground service (`PROPERTY_SPECIAL_USE_FGS_SUBTYPE` declared) needs a Play Console justification; `<queries>` MAIN/LAUNCHER gives broad package visibility (no QUERY_ALL_PACKAGES); background screens rely on SYSTEM_ALERT_WINDOW. Treat D3 as a release review item.
- All PendingIntents FLAG_IMMUTABLE; every new component exported=false; receivers use RECEIVER_NOT_EXPORTED; boot restart uses a type Android 15 still allows from BOOT_COMPLETED.

## Tests

`app/src/test/java/com/quizango/gallery/AfterCallInstallTest.kt` (7 tests): call-end show/wait/notify decision, call times per phone state, duration text, package event classification, duplicate + update throttle, IU texts per event, Quick Info album cards and routes.

## Gates (worktree, final commit)

`testDebugUnitTest` pass (353 tests, 0 failures) | `assembleDebug` pass | `check-strings` 24 languages up to date | `check-colors` 230 light / 138 night match | `git diff --check` clean. UI self-audit: no gradients, glows, emoji (other than 2.7.2's own "⚠️ Quick Info disabled" copy), tracked caps or hard-coded colours.

## Hooks for the integrator

- Settings (Phase 9): `onOpenQuickInfoSettings = rememberQuickInfoSettingsOpener { navController.navigate(Screen.GsSetting.route) }` and `onOpenInstallSettings = rememberInstallSettingsOpener { navController.navigate(Screen.IuSetting.route) }` (both in `ui/gs/GsPermissions.kt`, call them in the Settings destination).
- If Phase 9 adds its own notification/deep-link router, keep `GsAppEffects(navController)` (it consumes only the `gs_route` extra) or fold its `gs_route` handling into that router.

## Left undone

Device verification (Artemis) of every flow, Android 15 background-start behaviour of the guide card, the Home session call-permission prompt, the keep-alive worker.
