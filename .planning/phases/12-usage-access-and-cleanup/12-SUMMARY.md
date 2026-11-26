---
phase: 12-usage-access-and-cleanup
part: USG-01 (usage-access prompts). CLR-01 (removed-code sweep) is run by the integrator after all merges.
requirements: [USG-01]
checklist_rows: ["#6", "#15", "#27", "#30", "#46"]
decision: D4 port the UI only
status: built, gates green, device pass not run (skipped by user decision)
---

# Phase 12 summary: usage-access prompts (USG-01)

## Reference findings that drove the build

- **Layouts.** `dialog_usage_access_home/story/video/recycle/settings` are byte-identical: grabber, gradient hero circle with
  the shield glyph, "PRIVACY FRIENDLY" eyebrow (letterSpacing 0.08), title, description, three check rows, next-step line,
  underlined Privacy Policy link, Allow (44sdp) and Not now (42sdp). Only the title text differs, set in code.
- **Dialog (`S272/ta/c`).** A Material BottomSheetDialog (theme `PropertyDialog`): full width at the bottom, transparent
  window, dim 0.8, white navigation-bar icons, cancelable (Back, outside tap, swipe). Title = `getString(titleRes, app_name_local)`,
  description and next step also take `app_name_local`. Allow drops the dismiss listener, dismisses, then opens settings;
  Not now and every cancel call the controller's onDone. Privacy link: RC `common_data.social_media_links.privacy_policy`
  (in-app default `https://appsspecials.com/bestgallery/privacy`), else `about_privacy_policy`.
- **Placements (`ta/d`).** title / sessions key / last-shown key / waitsForHome:
  HOME `usage_access_home_title`, `home_sessions`, `usage_access_home_popup_last_shown_session`, no;
  VIDEO, STORY, RECYCLE `usage_access_video/story/recycle_title`, `feature_sessions`, `usage_access_popup_last_shown_session`, yes;
  SETTINGS `usage_access_settings_title`, same shared key, no. The four non-Home prompts share one slot per session.
- **Rules (`ta/f.BsUTWEAMAI`), in order:** access already granted (AppOps `OPSTR_GET_USAGE_STATS`, MODE_DEFAULT falls back
  to `PACKAGE_USAGE_STATS`) -> skip; activity finishing -> skip; then, unless forced: `app_insights_enabled` (default true)
  false -> skip; RC `enable` false -> skip; placement switch (`home_enable`, `video`, `story`, `recycle`) false -> skip;
  waitsForHome and the Home prompt never shown (-1) -> skip; session (`sessionCount`) not in the list -> skip; last-shown
  key == session -> skip. Showing writes last-shown = session first. The forced path (Settings) only checks granted.
- **Remote-config defaults (`R272/xml/default_remote_config.xml` `usage_access_controller`):** `enable: false`,
  `home_enable/video/story/recycle: true`, `home_sessions [1,3,5,9,13]`, `feature_sessions [1,2,3,5,8,10,14]`. The `ta/d`
  lists ([1,2,4,6,8] and [2,3,4,6,8,10,12]) are only fallbacks for a missing or empty array.
- **Where prompts show.** Only Home and Settings own a controller. Story Back (`SV.Oooo0oO`), video player Back
  (`VideoPlayerActivity.Oooo0oO`, not while locked), viewer exit after a video (`MediaFullScreenActivity.o000ooO0`,
  `hasViewedVideo`) and Recycle Bin Back (`RB.Oooo0oO`) only set the static `ta/f.pendingForHome`. `LargeVideoActivity`
  never references `ta/d.VIDEO` (as Phase 6 Q5 found): row #27's dialog arrives through the player.
- **Home (`HomeActivity`).** onResume sets `isUsageAccessPopupPendingOnHome` and posts `OoooOoo` after 0x2bc = 700 ms:
  prompt already showing -> return; no window focus -> retry 700 ms after focus returns; else `ta/f.R7N8DF4OVS` takes the
  pending placement and tries it, then HOME. While a prompt shows, the vault hint gate (`o000o0O0`, canShowVaultHint)
  fails and sets `isVaultHintShowPending`; onDone (`o0000Oo0`) re-runs the hint (or the queued floating-button actions).
  onGranted starts the AppInsights SDK (`x/a`).
- **Settings (`SettingActivity`).** Switch = `app_insights_enabled && granted`, refreshed onResume. Row tap: on -> save
  false + toast `setting_usage_data_off_toast`; off and granted -> save true; off and not granted -> forced SETTINGS prompt.
  Its onDone (`dl/e` case 0): granted -> save true; then refresh the switch.
- **Allow (`ta/f.Wja3o2vx62`).** `ACTION_USAGE_ACCESS_SETTINGS` with `package:<ours>`, else without data, else onDone.
  On return: granted -> onGranted, then onDone.

## What was built (per checklist row)

| Row | Status | What |
|---|---|---|
| #6 Home | built | `homeUsageAccessPrompt()` called from `ui/home/HomeScreen.kt`; the vault hint now also needs `!usagePromptShowing` |
| #15 Story | built | `recordStoryUsageAccessTrigger()` queues STORY (`ui/discovery/StoryViewScreen.kt`) |
| #27 Large video / video | built | `recordVideoUsageAccessTrigger()` queues VIDEO (player Back and viewer exit, `ui/viewer/VideoPlayerScreen.kt`) |
| #30 Recycle Bin | built | `recordRecycleUsageAccessTrigger()` queues RECYCLE (`ui/recycle/RecycleScreen.kt`) |
| #46 Settings | built, not wired | `rememberSettingsUsageAccessPrompt(onDone)` for the integrator (Settings is Phase 9's file) |

Files:
- `ui/components/UsageAccessDialog.kt` (new): `UsageAccessKind`, `UsagePromptState`, `usageAccessAllowed`,
  `pickHomeUsagePrompt`, `UsageAccessQueue`, `isUsageAccessGranted`, `openUsageAccessSettings`, `homeUsageAccessPrompt`,
  `rememberSettingsUsageAccessPrompt`, `UsageAccessDialog` (ModalBottomSheet styled as the layout with the reference
  `bg_usage_*` shapes via `drawableBackground`, `sdp`/`ssp`, reference colours and strings).
- `data/local/datastore/AppPreferences.kt` (Phase 12 block): `appInsightsEnabled` / `setAppInsightsEnabled`
  (`app_insights_enabled`, default true), `usageAccessShownSession(home)` / `setUsageAccessShownSession(home, session)`
  (the two last-shown keys, default -1).
- `AndroidManifest.xml` (Phase 12 block): `PACKAGE_USAGE_STATS` with `tools:ignore="ProtectedPermissions"`, so the app is
  listed on the system Usage Access screen.
- Rotation: the shown prompt is `rememberSaveable`; a restored prompt blocks the next check, as `isUsageAccessPopupShowingOnHome`.
- No new strings, colours or drawables: Phase 1 already imported every `usage_*` resource.

## Integrator wiring (Settings, row #46)

In `ui/settings/SettingsScreen.kt` replace the no-op `recordSettingsUsageAccessTrigger()`:

```kotlin
val askUsageAccess = rememberSettingsUsageAccessPrompt(onDone = { /* re-read the switch */ })
// row tap, switch off and !isUsageAccessGranted(context): askUsageAccess()
```

The switch shows `preferences.appInsightsEnabled && isUsageAccessGranted(context)` (re-read on resume and in onDone).
Tap when on: `setAppInsightsEnabled(false)` and toast `setting_usage_data_off_toast`; when off with access granted:
`setAppInsightsEnabled(true)`. If Phase 9 also added an `app_insights_enabled` key, keep one of the two.

## Deviations from 2.7.2

1. **RC master switch on.** 2.7.2's in-app default `enable: false` keeps its Home, video, story and Recycle Bin prompts
   off until its live remote config enables them (the live value is unknown here). USG-01 requires the prompts, so
   `USAGE_ACCESS_RC_ENABLE = true`; set it false to match 2.7.2's offline default. Session lists and switches use the
   in-app RC defaults, as Phase 3 did for `vault_hint_tooltip`.
2. **No data collection.** onGranted (the AppInsights SDK start in `x/a`) and every analytics event (`usage_access_popup_*`,
   `setting_usage_*`) are dropped. Remote config is never fetched.
3. **Sheet host.** Compose `ModalBottomSheet` with a transparent container replaces the Material BottomSheetDialog: same
   bottom position, 0.8 dim, swipe/Back/outside cancel, white nav-bar icons, and the dim shows under the navigation bar.
   The Material container (28dp corners, `colorSurfaceContainerLow`) that peeks out at the sheet's 28sdp corners in 2.7.2
   is not drawn. The status-bar icons follow Home's theme.
4. **Vault hint.** It is held only while a prompt is up (2.7.2's gate), not during the 700 ms before the check; on
   sessions where both apply, the hint can show first and is hidden while the prompt is up.
5. **Floating-button actions.** 2.7.2 also queues the floating-button dialog while the Home check is pending
   (`HA.o000OOoO`); Phase 10 owns that dialog and nothing exposes the pending state yet.

## Vault-hint show conditions (03-UI-SPEC section 1.4)

Condition 5's usage-access part is wired (hint waits while the prompt is up, shows when it closes). Still open for
other phases: condition 4 (no hint after notification/widget/deep-link entry, Phase 9) and condition 5's
notification-permission and floating-button dialogs (Phases 9 and 10). The shortcut comment at `ui/home/HomeShell.kt:228`
still names Phase 12; it was left alone because Phases 9 and 10 edit those lines.

## Tests

- `UsageAccessTest` (new, 5 tests): Home sessions and once per session; feature prompts wait for the Home prompt, use the
  RC feature sessions and share one slot; granted access or a Settings opt-out suppresses every kind; Home tries the
  queued prompt first, then its own; the story, video and Recycle Bin hooks queue their kind (one slot, latest wins).
- `VideoPlayerTest`: the hook test now checks that the player Back queues VIDEO.

## Gates

`:app:testDebugUnitTest` pass (351 tests), `:app:assembleDebug` pass, `check-strings` pass (24 languages),
`check-colors` pass (226 light / 134 night), `git diff --check` clean. UI self-audit: the gradient hero circle and the
tracked uppercase eyebrow are 2.7.2's own design and stay for parity; the only literal colours are the transparent sheet
container and the 0.8 black scrim (ta/c's window dim).

## Not done

- Settings row wiring (above) and the CLR-01 sweep (integrator).
- Device check of the sheet against 2.7.2 in light and dark (Phase 13).
