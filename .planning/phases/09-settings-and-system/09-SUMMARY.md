---
phase: 09-settings-and-system
subsystem: ui, system
tags: [settings, theme, about, paywall, notifications, intents, deep-links]
requires:
  - phase: 03-home-shell-and-photos-tab
    provides: HomeTopBar ivPro slot, shouldShowVaultHint
  - phase: 06-manage-and-cleaner
    provides: auto-delete preference, Cleaner / Remove Junk routes
provides:
  - 2.7.2 Settings, Theme, About and Pro (paywall) screens
  - Quick Actions notification (notification_small/big_layout) with tap routing
  - GET_CONTENT / PICK picker result, deep links, launcher shortcuts handled in MainActivity
  - Home: ivPro opens Pro; vault hint skipped after an outside entry and held during the notification ask
status: complete
tech-stack:
  added: []
requirements-completed: [SET-01, SET-02, SET-03, SET-04, SET-05]
---

# Phase 9: Settings and System — Summary

## Reference findings that drove decisions
- **SettingActivity** rows (activity_setting): goProBanner (non-premium; activeGoProBanner + My Subscription only when
  pref `IsAdFree`), Theme, GS "After Call Feature" (`ic_phone`, red dot when CALL_PHONE + READ_PHONE_STATE + overlay
  are not all granted; shown only to campaign installs **and** online), Private Vault, App Languages, Quick Access
  Settings (`floatingLay`: Android 9+ and not in `overlay_unsupported_devices`), Install/Uninstall Monitoring (campaign
  installs; red dot when overlay is off), [My Subscription], About, [Revoke consent: UMP privacy form, only when consent
  is required], Rate us (network, else "Please turn on your internet…"), Share, Play games, Quiz, Recycle Bin header,
  "Delete photos after 30 days" (`isAutoDelete`, default **false**), "Share app usage stats" (`app_insights_enabled`,
  default **true**; switch on = pref && usage access; off → toast; no access → `dialog_usage_access_settings`).
  Rows without ids keep their dividers when the row above is hidden (iu, revoke), as in the layout.
- **About**: version hard-coded "2.7.2" / build 152; links come from remote-config `common_data.social_media_links`
  (privacy `https://appsspecials.com/bestgallery/privacy`, terms `…/terms`, Instagram changed to `galleryphotovault`).
- **PayWallActivity** (grow library): title "Start Like A Pro", subtitle, 6 static slides (auto-advance 3.5 s), plans
  from Play billing; with no products it shows `layoutNoInternet` ("No Data Found." / "No Internet Connection!" +
  Refresh). Terms line literally reads "By Continuing you agree to ourTERMS and PRIVACY POLICIES" (no space).
- **HA.Ooooo00** (ivPro) runs the `home_top_action_loop`; its "pro" action opens the paywall (`ua/i`).
- **Notifications posted locally**: only the "Quick Actions" swappable notification (`g1/b.cWbN6pumKk` →
  `com.swappable_notification.NotificationWorker`): channel id/name "Quick Actions" (IMPORTANCE_DEFAULT, no sound, no
  vibration), id 988, `notification_small/big_layout`, features from `swappable_notification_options` (default camera,
  favorite, cleaner; max 3), period `swappable_notification_timer` minutes (bundled default 0 = cancelled; in-code
  fallback 600). Scheduled from Home once notifications may show (POST_NOTIFICATIONS asked on Android 13+ at Home start
  unless opened from outside). Taps open Splash → Home with `key_notification_intent/action`; Home routes camera
  (still-image camera or "Required app not available"), cleaner (Cleaner if `isJunkCleanedForToday`, else Remove Junk),
  favorite, moments, duplicate, recycle.
- **Server/FCM** notifications (`fr/b` Firebase, `t/d` API reminders with `api_notification_layout(_expanded)`,
  ApiNotificationRedirectActivity) and GS/IU/floating channels (Phases 10/11) are not local → out of scope.
- **ExternalSelectMediaActivity**: PICK + GET_CONTENT for image/*, video/*, vnd.android.cursor.dir/image|video;
  `EXTRA_ALLOW_MULTIPLE` shows the done tick; single tap returns `setData(uri)`, several return ClipData, both with
  FLAG_GRANT_READ_URI_PERMISSION; URIs are MediaStore content URIs.
- **DeepLinkActivity**: `https://api.bestgalleryapp.com/Activity?activity=<name>&key=<value>` (autoVerify dropped later: it made the OS fetch the reference host's assetlinks.json on every install, and could never pass); 21 exact
  names (PayWall, Duplicate, Recycle, Favorite, Burst, Theme, Search, Language, AllAlbum, Map, VaultLock, Home,
  Moments, PhotoEdit, RemoveBg, ColorSplash, BlurBg, RateUs, Animated, InternalSelectMedia, Organizer) + ExternalURL;
  always lands on Home first. `HA.o000O00O` skips the vault hint for those entries.

## Built per checklist row
| # | Status | Files |
|---|---|---|
| 46 Settings | built | `ui/settings/SettingsScreen.kt` (rows, toggles, rate-us bottom sheet `dialog_rate_us`, share, hooks) |
| 47 Theme | built | `ui/settings/ThemeScreen.kt` (activity_theme rows, select / unselect radios) |
| 48 About | built | `ui/settings/AboutScreen.kt` (2.7.2 links, version 2.7.2 / 152) |
| 49 Pro | built (no-products state) | `ui/settings/ProScreen.kt`, route `Screen.Pro`; Settings banner, Home ivPro, deep link |
| 52 Notifications | built (local only) | `notification/QuickActionsNotification.kt` (+ `QuickActionsJob`), `res/layout/notification_*_layout.xml` |
| 53 External intents | built | `MainActivity.kt`, `ui/navigation/ExternalEntry.kt`, `ui/external/ExternalPickScreen.kt`, manifest |
| Carried in | built | `ui/home/HomeShell.kt` (`onPro`, `LocalOpenedFromOutside`, `shouldShowVaultHint(…, openedFromOutside, startDialogShowing)`), `ui/home/HomeScreen.kt` (notification ask, hint hold, schedule) |

Shared files: strings/colors Phase 09 blocks (38 strings, 24 colours + 1 night, 24 locales regenerated), `Screen.Pro`,
NavHost Settings callbacks + Pro destination, manifest (RECEIVE_BOOT_COMPLETED, picker mime types, deep-link filter,
job service), `AppPreferences.appInsightsEnabled`. Drawables copied from 2.7.2 (rate stars with inlined gradients,
premium_bg, sub_*, ic_notification_*, notification_icon, ic_phone, ic_no_internet_available, star_*).

## Decisions and deviations from 2.7.2
- **No purchase can happen**: the Pro screen is the state 2.7.2 shows when billing returns no products (no plans,
  prices or Continue; Refresh only re-checks the network). The "Wait Don't Miss Out" sub-paywall (lifetime upsell on
  close) and the Play Store presence check are omitted. Slider banners stay on `sub_ic_mask_anim` (2.7.2 loads remote
  images; no network calls here).
- **Quick Actions period**: 600 min (2.7.2's in-code fallback); the bundled remote default 0 would switch it off and
  the live value is unknown. One constant (`INTERVAL_MINUTES`) to change. JobScheduler (persisted) replaces WorkManager;
  the first post happens when the job is created, as a new periodic work request runs at once. Routing uses the action
  key, not the translated label 2.7.2 compares.
- **Usage stats**: the row is always shown (SET-01; 2.7.2 gates it on remote `usage_access_controller.enable`, bundled
  false). Only the preference is stored; nothing is collected or sent.
- **Delete after 30 days**: turning it on saves the choice only; Phase 6 purges on Recycle Bin open (2.7.2 purges at
  once). Kept for data safety and consistency with Phase 6.
- **Hidden rows**: My Subscription (never premium) and Revoke consent (no UMP SDK) are not rendered.
- **Play games / Quiz** open the Play Store apps page (2.7.2 opens playzen.io; reference brand omitted). Links open in
  the browser, not a Custom Tab.
- **Deep links**: PhotoEdit / RemoveBg / ColorSplash / BlurBg open the editor on its own picker (2.7.2 picks on Home,
  then opens the tool); RateUs lands on Home without the rate dialog. The https host cannot verify for our app, so the
  filter only matches where the user enables it.
- **Picker**: filtered by the caller's mime type and to MediaStore rows (2.7.2 lists everything); syncs MediaStore once
  when opened cold; without storage permission it shows the empty state. Shortcuts open their screen above Home (2.7.2
  opens the activity alone).
- **ivPro**: opens Pro; the `home_top_action_loop` icon rotation (pro → quiz) is not reproduced.

## Hooks left for the integrator
- `ui/settings/SettingsScreen.kt`: `internal fun recordSettingsUsageAccessTrigger() = Unit` (Phase 12 / USG-01 shows
  `dialog_usage_access_settings`; Settings already turns sharing on when access is granted and the screen resumes).
- `SettingsScreen(onOpenQuickInfoSettings, onOpenFloatingSettings, onOpenInstallSettings)`: no-op lambdas in
  `GalleryNavHost.kt` (`// wired when Phase 10/11 merge`). 2.7.2 checks permissions before opening those screens
  (GS dialog / overlay settings); the callee should do that. CALL_PHONE is not in the manifest yet, so the GS red dot
  stays on until Phase 11 adds it.
- `shouldShowVaultHint(..., startDialogShowing)`: Phases 10 and 12 OR their start dialogs into it.

## Tests
`ExternalEntryTest` (deep-link filter and table, ExternalURL validation, notification and shortcut routing, Quick Actions
slots, picker kind/pickability), `SettingsRowsTest` (usage toggle, floating row gate, rate dialog art/title, About links
and version, paywall slides), `HomeShellTest` (vault hint after outside entry / during start dialogs).

## Gates
`testDebugUnitTest` pass · `assembleDebug` pass · `check-strings` 24 languages up to date · `check-colors` 249 light /
135 night match · `git diff --check` clean. UI audit: no added gradients beyond the reference `premium_bg`, no
emoji, uppercase only where 2.7.2 sets textAllCaps (RATE US, REFRESH), colours from resources.

## Not done
Device / Artemis verification (skipped by user decision); `VIEW image/video` from other apps (MediaFullScreenActivity,
Phase 5 area) is still not handled; Home rate-us visibility counter; API reminder notifications (server-driven).
