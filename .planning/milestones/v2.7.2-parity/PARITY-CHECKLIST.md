# Parity checklist: reference 2.7.2

Target: `albums.gallery.photo.folder.picasa.app.web.gallery` **2.7.2** (versionCode 152, minSdk 29), pulled from the test phone on 2026-11-13.
Baseline: our app, built to match **2.6.7** (`gallery.apk`).

The checklist is built from the decompiled APK: apktool smali and resources, plus the aapt2 resource table. See `tooling-setup.md`.
- Decoded copies: `%TEMP%\ref272\dec` (2.7.2) and `%TEMP%\ref267\dec` (2.6.7).
- APK kept at `reference/gallery-2.7.2.apk` (git-ignored).

How to use it:
- **Phase 1 (code port):** tick **Ported** for each item.
- **Phase 2 (device check with Artemis):** tick **Verified** after a side-by-side check on the phone, in both light and dark mode.

Status key:
- **NEW:** the layout exists only in 2.7.2.
- **CHANGED:** the layout file differs from 2.6.7.
- **same:** identical, but still verify behaviour.

## Size of the change (2.6.7 → 2.7.2)

| Area | Count |
|---|---|
| App screens (activities) | 58, plus 6 library screens (collage x3, crop, guide, paywall) |
| App layouts | 263: 30 NEW, 40 CHANGED, 193 same |
| Drawables | 107 new, 5 changed |
| Raw (Lottie and JS) | 4 new (ad SDK JS), 1 changed (`app_setings.json`), 7 removed, including `moment_lottie`, `organizer_guide_lottie` and `cleaner_done_lottie` |
| English strings | 85 new, 8 changed |
| Removed screens | `WelcomeActivity` (first-run welcome) |

## A. Screens

Columns: reference screen, its layout(s) with status, our file, Ported, Verified.

### A1. Launch and onboarding
| # | Reference | Layouts | Ours | Ported | Verified |
|---|---|---|---|---|---|
| 1 | SplashActivity | activity_splash CHANGED | splash/SplashScreen.kt | [x] | [ ] |
| 2 | ~~WelcomeActivity~~ (removed in 2.7.2) | activity_welcome removed | onboarding/WelcomeScreen.kt: **remove or reroute** | [x] | [ ] |
| 3 | AppLanguageActivity | activity_app_language CHANGED | settings/LanguageScreen.kt | [x] | [ ] |
| 4 | PermissionActivity | activity_permission same; dialog_permission_needed NEW | permission/PermissionScreen.kt | [x] | [ ] |
| 5 | FeaturePromoActivity (obFeature) | activity_feature_promo NEW | onboarding/FeaturePromoScreen.kt | [x] | [ ] |

### A2. Home shell and tabs
| # | Reference | Layouts | Ours | Ported | Verified |
|---|---|---|---|---|---|
| 6 | HomeActivity shell | activity_home CHANGED; home_shell_popup_menu NEW; layout_home_vault_hint NEW | home/HomeScreen.kt | [x] | [ ] |
| 7 | Photos tab | fragment_all_media CHANGED; photos_tab_item_date/grid/ad NEW | home/PhotosTab.kt | [x] | [ ] |
| 8 | Albums tab | fragment_album CHANGED; item_home_album_card NEW; item_stories, item_folder, item_grid_gallery CHANGED (album selection: layout_select_album_bottom, album info, rename, change cover ported) | home/AlbumsTab.kt | [x] | [ ] |
| 9 | Explore tab | fragment_explore CHANGED; explore_tab_hero/item_feature/item_row/item_tile NEW | home/ExploreTab.kt | [x] | [ ] |
| 10 | Exit popup | dialog_exit_popup CHANGED (note: Home exit uses dialog_exit through S272/jt/a; dialog_exit_popup is the photo editor's "Exit without saving?" dialog, ported in Phase 8; Phase 13 confirms popup versus direct exit) | HomeScreen BackHandler: direct exit like 2.7.2 (popup removed 2026-11-20) | [x] | [ ] |
| 11 | Floating button and sidebar | layout_floating_button, layout_floating_popup_menu, dialog_floating_button_popup CHANGED; item_floating_sidebar_action, layout_floating_sidebar_intro NEW | none: **decision D2** | [ ] | [ ] |
| 12 | FloatingSettingActivity | (settings for 11) | none: **D2** | [ ] | [ ] |

### A3. Viewing
| # | Reference | Layouts | Ours | Ported | Verified |
|---|---|---|---|---|---|
| 13 | MediaFullScreenActivity | activity_media_full_screen CHANGED; item_bottom_option_layout CHANGED | viewer/MediaViewerScreen.kt | [x] | [ ] |
| 14 | VideoPlayerActivity | same | viewer/MediaViewerScreen.kt | [x] | [ ] |
| 15 | StoryViewPhotoActivity | activity_story_view_photo CHANGED; dialog_usage_access_story NEW (dialog_usage_access_story is a no-op hook until Phase 12) | discovery/StoryViewScreen.kt | [x] | [ ] |
| 16 | MediaListActivity | same | album/AlbumDetailScreen.kt | [x] | [ ] |
| 17 | AllAlbumActivity | same | home/AlbumsTab.kt / album | [x] | [ ] |
| 18 | MomentsActivity | same; moment_lottie removed | discovery/MomentsScreen.kt | [x] | [ ] |
| 19 | MapActivity (Place) | same | (Place list) | [x] | [ ] |
| 20 | AnimatedMediaListActivity | same | discovery/AnimatedMediaScreen.kt | [x] | [ ] |
| 21 | BurstPhotoActivity | same | discovery/BurstPhotoScreen.kt | [x] | [ ] |
| 22 | FavoriteActivity | same | favorites/FavoritesScreen.kt | [x] | [ ] |
| 23 | SearchActivity / SearchViewAllActivity | same | search/SearchScreen.kt | [x] | [ ] |
| 24 | SearchPhotoColorActivity | same | discovery/SearchColorScreen.kt | [x] | [ ] |

### A4. Manage and cleaner
| # | Reference | Layouts | Ours | Ported | Verified |
|---|---|---|---|---|---|
| 25 | SystemCleanerActivity | same | cleaner/CleanerScreen.kt | [ ] | [ ] |
| 26 | JunkScan / RemoveJunk / JunkCleared | same | cleaner/RemoveJunkScreen.kt | [ ] | [ ] |
| 27 | LargeVideoActivity | same; dialog_usage_access_video NEW | cleaner/LargeVideoScreen.kt | [ ] | [ ] |
| 28 | ScreenShortActivity | same | cleaner/ScreenShortScreen.kt | [ ] | [ ] |
| 29 | DuplicateWaiting / ScanningComplete / Photo / DeleteProcess / DeleteComplete | same | cleaner/Duplicate*.kt | [ ] | [ ] |
| 30 | RecycleActivity | item_grid_recycle_media CHANGED; dialog_usage_access_recycle NEW | recycle/RecycleScreen.kt | [ ] | [ ] |
| 31 | OrganizerActivity / OrganizerRecycleActivity | layout_organizer_stripe, organize_intro_step_layout CHANGED; organizer Lotties removed | discovery/QuickOrganizer*.kt | [ ] | [ ] |

### A5. Vault
| # | Reference | Layouts | Ours | Ported | Verified |
|---|---|---|---|---|---|
| 32 | VaultLockActivity | same | vault/VaultLockScreen.kt | [ ] | [ ] |
| 33 | VaultMediaActivity | same | vault/VaultMediaScreen.kt | [ ] | [ ] |
| 34 | VaultSettingActivity | same | vault/VaultSettingScreen.kt | [ ] | [ ] |
| 35 | VaultRecoveryQuestionActivity | same | vault/VaultRecoveryQuestionScreen.kt | [ ] | [ ] |
| 36 | VaultForgotPasswordActivity | same | vault/VaultForgotPasswordScreen.kt | [ ] | [ ] |
| 37 | InternalSelectMediaActivity (picker) | activity_picker_screen CHANGED | vault/VaultAddMediaScreen.kt | [ ] | [ ] |

### A6. Create and edit
| # | Reference | Layouts | Ours | Ported | Verified |
|---|---|---|---|---|---|
| 38 | PhotoEditActivity | item_crop, item_tools, item_res, fragment_text_input, layout_adjust_text_section, layout_text_color_tab, item_font CHANGED | editor/PhotoEditorScreen.kt | [ ] | [ ] |
| 39 | CropImageActivity | same | editor/crop | [ ] | [ ] |
| 40 | Collage picker / screen / saved | activity_collage_screen CHANGED; layout_*_collage (7) CHANGED; activity_main_collage removed | creative/CollageMakerScreen.kt | [ ] | [ ] |
| 41 | RemoveBgActivity + guide | same | creative/RemoveBgScreen.kt | [ ] | [ ] |
| 42 | BlurBgActivity + guide | same | creative/BlurBgScreen.kt | [ ] | [ ] |
| 43 | ColorSplashActivity + guide | same | creative/ColorSplashScreen.kt | [ ] | [ ] |
| 44 | StickerActivity | same | creative/StickerScreen.kt | [ ] | [ ] |
| 45 | WallpaperActivity | same | creative/WallpaperScreen.kt | [ ] | [ ] |

### A7. Settings and system
| # | Reference | Layouts | Ours | Ported | Verified |
|---|---|---|---|---|---|
| 46 | SettingActivity | activity_setting CHANGED; dialog_usage_access_settings NEW | settings/SettingsScreen.kt | [ ] | [ ] |
| 47 | ThemeActivity | same | settings/ThemeScreen.kt | [ ] | [ ] |
| 48 | AboutActivity | same | settings/AboutScreen.kt | [ ] | [ ] |
| 49 | PayWall / subscription | activity_subscription_purchase CHANGED | none (Pro button) | [ ] | [ ] |
| 50 | GsActivity, GsSettingActivity (+ fragment_gs_games/media/menu, dialog_gs_*) | NEW | none: **decision D3** | [ ] | [ ] |
| 51 | InstallUninstallActivity / IUSettingActivity | activity_install_uninstall CHANGED | none: **D3** | [ ] | [ ] |
| 52 | Notifications (NotificationIntent, ApiNotificationRedirect, NotificationClick) | notification_big/small_layout CHANGED | none | [ ] | [ ] |
| 53 | ExternalSelectMediaActivity, DeepLinkActivity | same | MainActivity intents | [ ] | [ ] |

## B. Decisions needed before porting

| ID | Feature in 2.7.2 | What it does | Why it needs a decision |
|---|---|---|---|
| D1 | Feature promo onboarding step | "Get more from your Gallery": asks for Phone and Display-over-other-apps permissions for after-call, shortcut and tips features | Adds sensitive permissions. Only worth porting if D2 or D3 is ported. |
| D2 | Floating sidebar | An edge handle drawn over other apps with up to N quick tools, plus an intro and settings | Needs the overlay permission and a foreground service |
| D3 | "GS" screens and install/uninstall screens | After-call "Quick Info" card, plus full-screen screens shown when any app is installed, updated or uninstalled | Caller-ID and app-event takeover screens. Play policy risk, and they are not gallery features. |
| D4 | Usage-access dialogs (home, story, video, recycle, settings) plus the "Share app usage stats" setting | Asks for Usage Access to share app usage stats | A data-collection SDK feature |
| D5 | Ads (native, banner, interstitial, story ads, ad story circle) | Present throughout | The clone currently has placeholders. Real ad units are your call. |

## C. Shared parts to port once

- **Drawables:** 107 new drawables from `res/drawable` (Explore tiles and icons, home shell, photos tab, floating sidebar). Copy the vectors; replace any brand art with our own.
- **Strings:** 85 new English strings, with all language folders. The 8 changed ones include "Select All" → "Select all" and "close" → "Close".
- **Theme:** colours and styles diff (`values/colors.xml`, `values/styles.xml`, `values-night`).
- **Animations:** compare every Lottie the app still uses against 2.7.2 copies. Delete Lotties 2.7.2 removed, if we no longer use them.

## D. Behaviour and flows to verify on device (Phase 2)

For each item: same steps on both apps, light and dark mode, screenshots side by side.

1. **First run:** Splash → (no Welcome) → Language → Permission (All files access) → [Feature promo?] → Home.
2. **Home shell:**
   - tab order Photos / Albums / Explore;
   - default tab;
   - swipe between tabs;
   - top bar title per tab;
   - the ⋮ popup menu (Sort, …);
   - search;
   - camera;
   - the ad-free button;
   - the vault hint.
3. **Photos tab:**
   - date headers;
   - grid columns;
   - pinch to change columns;
   - selection mode and its actions;
   - fast scroll;
   - ad rows.
4. **Albums tab:**
   - story row: Recent / Memories / Flashback / Random / Favorite, plus the ad circle;
   - album card style and order;
   - the + (new album) button;
   - the media types;
   - Deleted and All Video.
5. **Explore tab:**
   - the Free up space hero card with Scan now;
   - Create: AI Editor, Collage;
   - Library: Vault, Favorites, Moments, Place;
   - Manage: Duplicate Finder, Quick Organize, Recycle Bin, Settings.
6. **Stories:** the order of stories and photos; date titles; auto-advance to the next story; tapping left and right; long press to pause; share; Edit; ad slots; the usage-access prompt afterwards.
7. **Viewer:**
   - swipe between photos;
   - zoom and double-tap;
   - the bottom options row (CHANGED);
   - info, favourite, delete, hide, share and edit;
   - video playback, and its audio stopping when the viewer closes.
8. **Duplicates:** scanning → complete animation → groups → Keep One Photo → delete process → delete complete.
9. **Cleaner:** junk scan, large videos, screenshots, and each one's completion screen.
10. **Recycle Bin:** days left, restore, delete, empty, and the usage-access prompt.
11. **Vault:**
    - first PIN or pattern setup;
    - recovery question;
    - unlock;
    - fingerprint;
    - hide and unhide;
    - change password;
    - forgot password;
    - settings.
12. **Editor and creative tools:** each tool screen, and saving.
13. **Settings:**
    - each row and toggle;
    - language;
    - theme (Light / Dark / System);
    - about;
    - privacy policy link (now a URL);
    - the usage-stats toggle.
14. **Animations:**
    - splash;
    - screen transitions;
    - tab switching;
    - the story progress bars;
    - scan Lotties;
    - delete or clean done animations;
    - permission toggle hand animation.
15. **Edge cases:** no photos; no favourites; empty vault; denied permission; and a large library (performance).

## E. Removed in 2.7.2: clean up on our side

- Welcome screen: our `onboarding/WelcomeScreen.kt` and its route.
- The feature carousel above stories (`item_feature_lists` removed). The Albums tab row moves to Explore.
- `activity_main_collage` (collage home); check our collage entry point.
- Lotties: `moment_lottie`, `organizer_guide_lottie`, `org_album_touch`, `cleaner_done_lottie`, `green_done`. Check where we use each.
