---
phase: "5"
slug: "viewing-and-browsing"
status: approved
reviewed_at: 2026-11-21
shadcn_initialized: false
preset: none
created: "2026-11-21"
scope: "Phase 5 full-screen viewer (activity_media_full_screen + item_bottom_option_layout CHANGED), video player (activity_video_player), media list, all albums, Moments, Place (MapActivity), Animated, Burst, Favorites, Search / Search view all, Search by colour; VIEW-01, VIEW-02, VIEW-03; checklist #13, #14, #16-#24"
---

# Phase 5 — UI Design Contract (Viewing and Browsing)

> Parity port, not a new design. Every value below comes from the decompiled reference 2.7.2
> (layout XML for looks, smali for behaviour) or from the reference running on the test phone on
> 2026-11-21 (marked **[device]**). Nothing here is a preference.
> Tokens (`N.sdp`, `N.ssp`, Inter, `GalleryText.*`, colour names, radii) come from the Phase 1 contract;
> the Photos grid helpers, selection bars, pinch tutorial and dialogs come from the Phase 3 contract;
> album cards and album selection from the Phase 4 contract. They are referenced here, not repeated.

**Evidence roots (read-only):**
- `R272` = `%TEMP%\ref272\dec\res` (2.7.2). 2.6.7 comparison: `aapt2 dump xmltree --file res/layout/<x>.xml gallery.apk`
  (the `%TEMP%\ref267\dec\res` copy only holds the Phase 3/4 layouts).
- `S272` = `%TEMP%\ref272\dec\smali\albums\gallery\photo\folder\picasa\app\web\gallery`
- `MF` = `S272/presentation/mediafull/view/activity/MediaFullScreenActivity.smali` (layout 0x7f0d003d, binding `ar/ab`, bottom binding `ar/ck`, suggestion binding `ar/dr`);
  `PA` = `S272/ch/w.smali` (pager adapter; holders `ch/i` image, `ch/m` video); `GS` = `S272/ab/o.smali` (gesture settings of `custom/gestures/views/GestureImageView`);
  `VP` = `S272/presentation/videoplayer/view/activity/VideoPlayerActivity.smali` (0x7f0d005b);
  `MA` = `S272/ka/d.smali` (shared media adapter, holders `ka/a` date, `ka/c` grid) used by MediaList, Favorite, AnimatedMediaList, SearchPhotoColor;
  `ML` `MediaListActivity`, `AL` `AllAlbumActivity` (adapter `bx/g`), `MO` `MomentsActivity` (adapter `dg/g`), `MP` `MapActivity` (adapter `ce/b`),
  `AN` `AnimatedMediaListActivity`, `BU` `BurstPhotoActivity` (adapter `be/c`), `FA` `FavoriteActivity`, `SE` `SearchActivity` + `SearchViewModel`,
  `SV` `SearchViewAllActivity`, `SC` `SearchPhotoColorActivity` (all under `S272/presentation/`); `RC` = `R272/xml/default_remote_config.xml`
- **[device]** screenshots: `.planning/phases/05-viewing-and-browsing/evidence/ref272-viewer-*.png` (Redmi M2006C3MII, app in light theme, 2026-11-21).
- Ours = `app/src/main/java/com/quizango/gallery/ui/`, `data/`, `app/src/main/res/`

**Ads (D5), carried from Phases 3-4:** a slot whose reference layout has a loading skeleton draws it static; a slot without one stays collapsed.
Every ad slot in this phase (`rl_banner_ad`/`global_ad_framelayout`, `relAdView`, `item_ad_all_album`, `item_search_ad`, `toastSubscribeLayout`)
has no skeleton → **collapsed, 0 height**. No interstitials (`click_interstitial`, `back_interstitial`, `fullscreen_media_inter`): open/close directly.

---

## Design System

| Property | Value |
|----------|-------|
| Tool | none (Android, Jetpack Compose; shadcn not applicable) |
| Preset | not applicable |
| Component library | Compose Material 3 (Phase 1). House pieces to reuse: `ui/home/PhotosTab.kt` (`buildPhotoRows`, `headerKind`, `fileTypeLabel`, `nextColumnCount`, `FastScrollThumb`, `PhotosEmptyState`), `ui/home/HomeSelection.kt` (`HomeSelectionBottomBar`, `AlbumSelectionBottomBar`, `DeleteMediaDialog`, `DialogButton`, `drawableBackground`), `ui/home/HomeDialogs.kt` (`HideMediaDialog`, `AddingMediaDialog`, `CreateAlbumDialog`, `RenameAlbumDialog`, `AlbumPropertyDialog`, `CommonDialog`), `ui/components/AddToAlbumSheet.kt`, `ui/viewer/ViewerZoom.kt` |
| Icon library | Reference vector drawables via `painterResource`. No `material-icons` glyph where the reference ships one (today: `FavoritesScreen` `Icons.Default.Favorite/PlayArrow`, `MomentsScreen` `Icons.Filled.Videocam`, `BurstPhotoScreen` `Icons.Default.BurstMode`, `SearchColorScreen` `Icons.Default.Check`, `MediaViewerScreen` `Icons.Outlined.Folder`, every `Icons.AutoMirrored.Filled.ArrowBack`: remove) |
| Font | Inter (Phase 1) |
| Animation | Compose `animate*`/`Animatable`; Lottie only `moment_scanner_lottie` (present in ours) |
| Video | Media3 ExoPlayer (already a dependency) |

Component Inventory section omitted: `Tool: none` (no registry to enumerate; same as Phases 1, 3 and 4).

**Screen map (planner gap list):**

| # | Reference | Ours today | Gap |
|---|---|---|---|
| 13 | MediaFullScreenActivity | `viewer/MediaViewerScreen.kt` (2.6.7 look: surface top bar with date/time, flat bottom bar with labels) | Rebuild chrome to the 2.7.2 glass look (§1) |
| 14 | VideoPlayerActivity | none: every video plays inline in `MediaViewerScreen` | New `viewer/VideoPlayerScreen.kt` + route; inline play only for videos ≤ 60 s (§1.8, §2) |
| 16 | MediaListActivity | `album/AlbumDetailScreen.kt` (fixed 4 columns, own bars) | Shared media-list frame (§3, §4) |
| 17 | AllAlbumActivity ("Other Albums") | none | New `album/AllAlbumsScreen.kt` + route (§5) |
| 18 | MomentsActivity | `discovery/MomentsScreen.kt` | Restyle to `activity_moments` + `item_search_sub` (§6) |
| 19 | MapActivity (Place) | `AlbumDetail` `filter:place` list | New `discovery/PlaceScreen.kt` (§7; map needs a decision) |
| 20 | AnimatedMediaListActivity | `discovery/AnimatedMediaScreen.kt` (3 fixed columns) | Shared frame + scanning header (§8) |
| 21 | BurstPhotoActivity | `discovery/BurstPhotoScreen.kt` | `item_grid_square_album` mosaic cards (§9) |
| 22 | FavoriteActivity | `favorites/FavoritesScreen.kt` (fixed 4 columns) | Shared frame (§10) |
| 23 | SearchActivity / SearchViewAllActivity | `search/SearchScreen.kt` (in-place "View All"/"Collapse" toggle) | Sections + separate View-all screen (§11) |
| 24 | SearchPhotoColorActivity | `discovery/SearchColorScreen.kt` (fixed 4 columns, `ic_more`, `ic_pro_premium`) | Shared frame (§12) |

---

## 1. Full-screen viewer (`R272/layout/activity_media_full_screen.xml` + `item_bottom_option_layout.xml` CHANGED — #13, VIEW-01)

### 1.1 What 2.7.2 changed versus 2.6.7 (do these first)

| 2.6.7 (ours today) | 2.7.2 | Evidence |
|---|---|---|
| Opaque top bar (`colorScheme.surface`) with date + time text and a 1.dp divider | Transparent bar over the photo; round **glass buttons**; `date`/`time` stay `gone` (never shown) | `activity_media_full_screen` `toolbarLay`; `aapt2` 2.6.7 tree shows the divider View; **[device]** `ref272-viewer-photo.png` |
| Opaque bottom bar with icon **and label** per action, divider, `navigationBarsPadding` | Floating dark **pill dock**, icons only (every label `visibility=gone`) | `item_bottom_option_layout` `bottomActionView`; **[device]** |
| Video controls on a surface strip with Material `Slider` | Short-video controls are the first row **inside** the dock; seek bar floats above it | `videoControlBottom`, `progressLay` |
| Every video plays inline | Inline only for videos ≤ 60 000 ms; longer ones show a centre play button that opens the Video player (§2) | `ch/m` (`getVideoDuration` vs 0xea60) |
| Double-tap to 1.75×, max 5× (`ViewerZoom`) | Max zoom depends on the image (§1.3); double-tap goes to that max | `GS`, `ch/i.BsUTWEAMAI` |

### 1.2 Layout

| Element | Value | Reference |
|---|---|---|
| Window | edge-to-edge, status bar colour transparent (`MF.o000Oo00`); screen background `fullImageBackColor` (#000000 light / #0C0C0C dark) while browsing — **[device]** black in light theme. Add the colour | `haulerView`, `MF.o0O0ooO` (0x7f06015a) |
| Pager | horizontal, full screen under the bars, one page per item; swipe disabled while the page is zoomed | `galleryPager` |
| Top bar | row, height 56.dp, padding start/end 12.dp, centred vertically, elevation 2.sdp, below the status bar inset; no background, no divider, no title | `toolbar` |
| Glass button | 36×36.dp circle `bg_media_full_glass_btn` (oval `media_full_glass_bg` #8C1C1D21, ripple #33888888), icon padding 7.dp, `centerInside`, tint `media_full_glass_icon` #FFFFFF (except favourite) | `backBtn` etc. |
| Back | `ic_back`, cd "Back" (hard-coded English in the reference: store it as a `translatable="false"` resource) | `backBtn` |
| Spacer | weight 1 | date/time column (gone) |
| Rotate | `ic_media_rotate`, marginStart 8.dp, cd `rotate`. Shown for still images; hidden for videos, media types 4/16/64 (GIF/RAW/SVG) and `.heif` files, and in the Recycle Bin | `MF.o000oOoo` |
| Favourite | `ic_favorite` (outline, untinted) / `ic_fav_fill` when the item is a favourite, marginStart 8.dp, cd `favorite`. Shown in normal browsing **[device]**; hidden in Recycle Bin and vault (`type` 2 / 13), and while a short video is playing **[device]** | `favBtn`, `MF.o0000OOo` |
| Capture | `ic_camera_ss_short` tinted white, marginStart 8.dp, cd "Capture" (literal in the reference). Shown only while a short video plays **[device]** `ref272-viewer-short-video-playing.png` | `captureBtn` |
| Details | `ic_property`, marginStart 8.dp, cd `details`. Shown in every mode except `type` 2 (Recycle Bin / vault) | `propertyBtn`, `MF.o0000OOo` |
| Status scrim | `bg_media_full_status_scrim_dark` (#73000000 → transparent, top-down), status-bar height, shown only while the chrome is hidden; status-bar icons light while the scrim shows | `statusBarScrim`, `MF.o000Oo00` |
| Suggestion strip | §1.5, 10.sdp above the dock | `itemSuggLay` |
| Seek row | §1.8, 4.sdp above the dock | `progressLay` |
| Dock | §1.6, bottom, above the (collapsed) banner slot | `bottomAction` |
| Loading | centred `colorPrimary` spinner over everything (elevation 10.sdp) while a file action runs | `progressBar` |
| Ad overlay `relAdView`, `videoAdCloseBtn` | collapsed (D5) | — |

Ours: replace the `Column(... background(surface) ... statusBarsPadding)` top bar and its `HorizontalDivider`, the `dateText`/`timeText` texts and every `IconButton` with the glass buttons above.

### 1.3 Page content, zoom and swipe (`item_image_full`, `item_video_full`, `GS`, `ch/i`)

| Behaviour | Value | Reference |
|---|---|---|
| Image page | fit-centre photo on `video_bg` (#00000000, add), gestures as below | `item_image_full` |
| Pinch | zoom between 1× (fit) and the item's max; may overshoot to 2× the max while pinching (`overzoomFactor` 2.0) and springs back on release | `GS` constructor |
| Max zoom | still image with a "W×H" dimension: landscape (W > H) → **2.0**, otherwise **5.0**. GIF / WebP: from the shorter decoded side `s`: s ≤ 150 → 10.0, ≤ 250 → 6.0, ≤ 350 → 5.0, ≤ 450 → 4.0, else 3.0 | `ch/i.BsUTWEAMAI` |
| Double-tap | toggles fit ↔ the max zoom, centred on the tap point, animated **200 ms** (`doubleTapZoom` −1 = max; `animationsDuration` 0xc8) | `GS` |
| Pan | only while zoomed; edges clamp to the screen (keep `ViewerZoom.clamp`) | `GS` bounds |
| Single tap | toggles the chrome (§1.4) | `PA.onClick` |
| Swipe pages | only at fit zoom (keep ours) | `galleryPager` |
| Swipe down at fit zoom | closes the viewer (keep ours; `PA.onStartAnimation/onDismissAnimation`) | `PA` |

Ours: change `ViewerZoom.DOUBLE_TAP_SCALE = 1.75f` / `MAX_SCALE = 5f` to a `maxZoomFor(width, height, isGifOrWebp)` with the table above (one unit test), double-tap to that value, 200 ms.

### 1.4 Show / hide chrome (`MF.o0O0ooO` hide, `MF.o000Oooo` show)

- **Hide** (tap while shown): top bar slides up by its own height and fades to alpha 0; dock slides down by its height and fades to 0; both **150 ms** (0x96); suggestion strip and seek row hide at once; status scrim shows.
- **Show** (tap while hidden): reverse, 150 ms, translation back to 0, alpha 1; scrim hides.
- System bars are not hidden by the reference (ours hides them through `WindowInsetsController`: remove; keep them visible, edge-to-edge).
- The background stays `fullImageBackColor` in both states (ours switches between `colorScheme.background` and black: remove).

### 1.5 Edit suggestion strip (`R272/layout/item_suggestion_layout.xml`, same as 2.6.7)

Shown for still images (not videos, GIF/animated, Recycle Bin, vault) **1500 ms** after a page settles (`suggestionRunnable`, `postDelayed` 0x5dc); hidden when the chrome hides or the page changes; ✕ hides it for the current page.

| Element | Value |
|---|---|
| Container | horizontal scroll, no scrollbar, `clipToPadding=false`, padding 5.sdp all round, marginBottom 10.sdp above the dock |
| Pill | `edit_opt_back` (`blackTran3` #DD000000, radius 4.sdp; import), margin h 5.sdp, padding 4.sdp h / 3.sdp v, icon 13.sdp tinted `onlyWhite`, text `GalleryText.BodySmallMedium` `onlyWhite` marginStart 3.sdp |
| Close pill | first; same background, padding 3.sdp, `ic_close` 12.sdp `onlyWhite` |
| Order | Copy text (`copy_text_from_photo`, **hidden**: `enable_copy_text_feature` false in `RC`), Remove Background `remove_background`, Blur Background **`blur_background_guide`** (ours uses `blur_background`: change), Color Splash `color_splash`, Crop `crop`, Markup `markup`, Adjust `adjust`, Filter `filter` |
| Taps | keep ours (Remove BG / Blur BG / Colour Splash screens; editor tabs crop / brush / adjust / filter) |

Ours: replace the 32.dp `Color(0xFF242730)` circle and `SuggestionPill` styling with the values above.

### 1.6 Pill dock (`item_bottom_option_layout` CHANGED)

| Element | Value |
|---|---|
| Dock | `bg_media_full_pill_dock` (`media_full_pill_bg` #E61C1D21, 1.dp stroke `media_full_pill_stroke` #26FFFFFF, radius 26.dp), margin 14.dp left/right/bottom, padding 2.dp v, elevation 6.dp; sits above the collapsed banner slot and the navigation-bar inset |
| Short-video row | only for inline videos (§1.8): mute `ic_short_video_unmute`/`ic_short_video_mute` start (marginStart 18.sdp, padding 12.sdp, `custom_ripple` oval), play `ic_short_video_play`/`ic_short_video_pause` centre (padding 2.sdp h / 6.sdp v), speed `ic_short_video_speed` end (marginEnd 18.sdp, padding 12.sdp); all tinted white |
| Action row | equal-weight slots, each a borderless-ripple column with padding 12.sdp v, icon at intrinsic size tinted `media_full_glass_icon`, **no label** (labels exist but are `gone`) |

**Action rows per mode** (`MF.o0000OOo`, **[device]**):

| Mode | Slots in order | Notes |
|---|---|---|
| Normal, still image | Share `ic_share` · Private `ic_hide` (padlock) · Edit `ic_main_edit` · Delete `ic_delete` · More `ic_more` | **[device]** `ref272-viewer-photo.png` |
| Normal, video | Share · Private · Add to `ic_add` (replaces Edit) · Delete · More | **[device]** `ref272-viewer-short-video-paused.png` |
| Recycle Bin (`isRecycle`) | Restore `ic_restore` · Delete `ic_delete` | `recyclerBottomAction` |
| Vault (`HIDDEN_IMAGE`) | Share · Delete (`hiddenBottomAction`); Unhide `ic_unhide` and More as ours today | `hiddenBottomAction`, `unHideBtn` |

Content descriptions (labels are hidden, keep them for TalkBack): `share`, `private_bottom`, `edit`, `add_to`, `delete`, `more`, `restore`, `unhide`.

| Action | Result |
|---|---|
| Share | system share sheet for the current item (keep `shareMedia`) |
| Private | Phase 3 `HideMediaDialog` (`hide_media` / `hide_item_permission`, Cancel / Ok), then move to vault |
| Edit | photo editor; animated image → toast "Unable to edit animated image." `toast_animated_image`; corrupt image → `toast_corrupted_image` |
| Add to (video) | Phase 3 add-to flow (`AddingMediaDialog` Move/Copy, then the album sheet) |
| Delete | normal: Phase 3 `DeleteMediaDialog` single-item copy ("Are you sure you want to delete 1 item?" + "Move to Recycle Bin" checkbox, No / Yes); Recycle Bin: permanent-delete dialog "Delete items" `delete_items` / `delete_items_description`; vault: `deleted_item_permission` |
| Restore | "Restore items" `restore_items` / `restore_item_permission` confirmation, then restore (ours restores without asking and shows an English toast: fix) |
| More | §1.7 |

### 1.7 More popup (`R272/layout/popmenu_media_more.xml`)

Popup window anchored at the bottom (`Gravity.BOTTOM`, y = dock height), end-aligned above the dock **[device]** `ref272-viewer-more-popup.png`:
card `dialogBack` (#FFFFFF / #1E1E1E), radius 16.sdp, elevation 1.dp; column width 160.sdp, padding 8.sdp h / 5.sdp v.
Row: padding 5.sdp v, borderless ripple; icon at intrinsic size with padding 5.sdp; label `GalleryText.BodySmallMedium` `titleColor` marginStart 5.sdp.

| Order | Icon | Label | Shown |
|---|---|---|---|
| 1 | `ic_add` | "Add to" `add_to` | normal browsing **[device]** |
| 2 | `ic_rename` | "Rename" `rename` | normal browsing **[device]** |
| 3 | `ic_slideshow` | "Slide show" `slide_show` | always |
| 4 | `ic_set_as` | "Set as Wallpaper" `set_as_wallpaper` | always |
| 5 | `ic_print` | "Print" `print` | always |
| 6 | `ic_map` | "Show on map" `show_on_map` | only when the item has a place (`GalleryModel.getPlace()` non-empty); opens Place (§7) |

Ours uses a Material `DropdownMenu`: replace with this popup. Rename uses `dialog_rename` (§1.9).

### 1.8 Inline short video (`item_video_full`, `ch/m`)

| Rule | Value |
|---|---|
| Which videos | duration ≤ 60 000 ms and a normal file path → inline. Longer videos (or `content://` / `file://` URIs) show `ic_full_video_play` centred (padding 30.sdp) over the thumbnail; tapping it checks the file plays (corrupt → toast `toast_corrupted_video`) and opens the Video player (§2) with the list's videos and this one selected (`MF.Oooooo0`) |
| Start state | paused on the first frame with the dock's play icon **[device]**; play starts on the play button |
| Playing | play icon → `ic_short_video_pause`; heart hides and Capture shows in the top bar; seek row shows **[device]** |
| Seek row | above the dock (marginBottom 4.sdp): `AppCompatSeekBar` margin h 10.sdp, 5.dp tall, track `sb_progress_drawable` (#26FFFFFF track, `onlyWhite` progress, radius 2.5.dp; import) tinted `shortVideoProgress`, thumb `sb_thumb` 16.dp oval (import); below it start/end times "00:00" format `%02d:%02d` (`%02d:%02d:%02d` over an hour), `GalleryText.BodyXSmallMedium` `onlyWhite`, shadow black (2, 2) r 4, row padding 10.sdp h, marginTop 4.sdp |
| Mute | toggles volume 0/1 and the icon |
| Speed | popup `popmenu_short_video_speed`: card `dialogBack`, radius 16.sdp, width 100.sdp, padding 10.sdp h / 8.sdp v; rows 0.25x / 0.5x / 1.0x / 1.5x / 2.0x, padding 8.sdp v, `GalleryText.BodyMediumMedium` `titleColor` marginStart 5.sdp, `ic_short_video_speed_select` on the current speed |
| Capture | saves the current frame as `IMG_yyyyMMdd_<time>.png` under Pictures/Screenshots and rescans (keep ours, but its two English toasts go: success/failure copy not in the reference → show nothing) |
| Loop | the inline player stops at the end (no repeat in the reference inline view; ours sets `REPEAT_MODE_ONE`: confirm on device, UI Considerations) |
| Audio stops | the player pauses when the page changes and is released when the viewer closes or goes to background (keep ours `DisposableEffect`/`finally` release; add pause on `ON_STOP`) |

### 1.9 Dialogs reached from the viewer

| Dialog | Reference | Copy / look |
|---|---|---|
| Details (image) | `dialog_property` — bottom sheet `property_dialog_back` (`dialogBack`, top radii 16.sdp; import) | handle `bottom_header` 40×4.sdp in a 10.sdp v band; title "Details" `details` `GalleryText.BodyXXLargeSemiBold` `titleColor` start 15.sdp, top 8.sdp, bottom 5.sdp; rows Name `name`, Path `path`, Size `size`, Resolution `resolution`, Taken on `taken_on`: padding 20.sdp h / 10.sdp v, label weight 1 `property_text`, value weight 2 `titleColor`, both `GalleryText.BodyMediumMedium`; OK button "Ok" `ok` end-aligned, `btn_permission`, padding 50.sdp h / 10.sdp v, marginEnd 25.sdp, top 12.sdp, bottom 15.sdp |
| Details (video) | `dialog_video_property` | same keys (`details`, `name`, `path`, `resolution`, `size`, `taken_on`, `ok`) |
| Rename (file) | `dialog_rename` | card `dialogBack` radius 14.sdp; "Rename" `rename` `BodyXLargeSemiBold` start 16.sdp top 15.sdp; field hint "Enter Name" `enter_name`, max 50 chars, `BodyMediumMedium`, margin h 16.sdp, padding 15.sdp v; buttons "Cancel" `cancel` (`help_delete_cancel_bg`, `fastScrollerColor`) and "Save" `save` (`btn_permission`, `onlyWhite`), equal weight, margin h 10.sdp |
| Hide / Delete / Add to | Phase 3 dialogs (`dialog_hide` copy for a single photo is overridden in code to `hide_media` / `hide_item_permission`) | as Phase 3 §3 |
| Rotate failure | toast "Couldn't rotate image!" `couldn_t_rotate_image` | Rotate writes the 90° rotation to the file (`PA.onRotate`/`onRotateBitmap`), not only the view (ours rotates the view only: fix) |

### 1.10 Slide show (More → Slide show; `slideControl`)

Full-screen touch layer (elevation 10.sdp) over the pager; chrome hidden; advances pages automatically. Controls, each 28.sdp circle `ic_slide_control_back` (#90000000, ripple; import), margin 40.sdp:
Close `ic_story_close` padding 6.sdp bottom-end; Volume `ic_slide_unmute`/`ic_slide_mute` (import both) padding 4.sdp h, top-end; Duration text "2s" `time_duration` `GalleryText.BodyMediumMedium` `onlyWhite` bottom-start.
Interval and duration choices are read from `MF` slide-show code by the planner before building (UI Considerations).

---

## 2. Video player (`R272/layout/activity_video_player.xml`, same as 2.6.7 — #14, VIEW-02)

New screen: `viewer/VideoPlayerScreen.kt`, route `Screen.VideoPlayer` (args: path, list source). Opened from §1.8 only. Orientation follows the sensor.
Background `video_player_background` #0C0C0C. All icons to import from `R272/drawable` (none exist in ours): `ic_subtitle`, `ic_audio_track`, `ic_video_property`, `ic_video_speed`, `ic_video_speed_select`,
`ic_video_no_repeat`, `ic_video_repeat`, `ic_video_loop`, `ic_video_unmute`, `ic_video_mute`, `ic_video_ss`, `ic_video_unlock`, `ic_video_lock`, `ic_video_previous`, `ic_video_play`, `ic_video_pause`,
`ic_video_next`, `ic_video_ten_backward`, `ic_video_ten_forward`, `ic_video_rotate`, `ic_video_floating`, `ic_video_fastbackward`, `ic_video_fastforward`, `ic_video_brightness`, `ic_video_stripe_unmute`,
`ic_video_stripe_mute`, `ic_video_center_pause`, `ic_close_video`; backgrounds `custom_video_ripple`, `video_control_back`, `video_bottom_back`, `video_pause_back`, `video_subtitle_bg`, `sb_progress_drawable`, `sb_thumb`.

### 2.1 Layout

| Element | Value |
|---|---|
| Player | full screen, fit, no built-in controller |
| Top bar | height 56.dp (`?actionBarSize`), background `videoPlayerColor` #1E1E1E (add), padding 4.sdp h: Back `ic_back` tinted white padding 6.sdp; title = file name, `GalleryText.BodyLargeSemiBold` `onlyWhite` 1 line ellipsize, marginStart 4.sdp, weight 1; Subtitle `ic_subtitle` padding 3.sdp marginStart 6.sdp; Audio track `ic_audio_track` padding 3.sdp marginStart 4.sdp; Details `ic_video_property` white padding 3.sdp marginStart 4.sdp. Every button `custom_video_ripple` |
| Quick-control card | below the top bar, marginTop 10.sdp, margin h 10.sdp, `video_control_back`; four 52.sdp-wide columns (padding 10.sdp v, margin h 1.sdp; last marginEnd 8.sdp): Speed `ic_video_speed` "Speed" `speed`; Repeat `ic_video_no_repeat` "No Repeat" `no_repeat`; Mute `ic_video_unmute` "Unmute" `unmute`; Capture `ic_video_ss` "Capture ss" `capture_ss`. Labels `GalleryText.BodyXSmallMedium` `onlyWhite` marginTop 4.sdp |
| Bottom panel | `video_bottom_back`, padding 12.sdp top / 16.sdp bottom. Row 1 (padding 10.sdp h): start time (minWidth 34.sdp) · seek bar (weight 1, 5.dp, `sb_progress_drawable`, `sb_thumb`, tint white) · end time; times "00:00" `GalleryText.BodyXSmallMedium` white. Row 2 (marginTop 12.sdp): Lock `ic_video_unlock` (padding 5.sdp) start; Previous `ic_video_previous` · Play `ic_video_play` (padding 2.sdp, centre) · Next `ic_video_next`; Rotate `ic_video_rotate` (marginEnd 8.sdp) · Floating `ic_video_floating` (marginEnd 8.sdp) end. Landscape adds Repeat (marginStart 12.sdp after Lock), −10 s `ic_video_ten_backward` (marginEnd 22.sdp before Previous) and +10 s `ic_video_ten_forward` (marginStart 25.sdp after Next) |
| Seek feedback | double-tap left / right third shows `backWardPlayLayout` / `foreWardPlayLayout` (marginStart/End 25.sdp, centred vertically): `ic_video_fastbackward`/`ic_video_fastforward` over "10 seconds" `ten_sec` `GalleryText.BodySmallMedium` white |
| Brightness / volume | vertical swipe on the left / right half shows a `video_control_back` pill (padding 3.sdp h, 10.sdp top, 8.sdp bottom, margin 10.sdp from the edge): 5×80.sdp vertical bar + `ic_video_brightness` / `ic_video_stripe_unmute` (`ic_video_stripe_mute` at 0) 8.sdp below |
| Paused chip | double-tap centre while playing: `video_pause_back` chip centred, padding 8.sdp h / 3.sdp v, `ic_video_center_pause` + "Paused" `paused` `GalleryText.BodyMediumMedium` white marginStart 2.sdp |
| Locked | Lock hides all controls and shows only `ic_video_lock` in `video_control_back` (padding 4.sdp) at top-start (margin 26.sdp); tapping it unlocks |
| Subtitle text | `video_subtitle_bg`, centred, 15.sdp above the bottom panel, padding 5.sdp h / 2.sdp v, `GalleryText.BodyLargeMedium` white |
| Buffering | centred white spinner |
| Subscribe toast, ad view | collapsed (D5) |

### 2.2 Behaviour (`VP`)

| Behaviour | Value |
|---|---|
| Playlist | the videos of the list the viewer was opened from, starting at the tapped one; Previous / Next move through it; at the ends toast "No previous video" `no_previous_video` / "No next video" `no_next_video` |
| Controls | tap toggles; auto-hide 3000 ms after the last interaction (`VP` 0xbb8) |
| Seek ±10 s | double-tap sides and the landscape ±10 buttons seek 10 000 ms (0x2710) |
| Repeat | cycles No Repeat → Repeat One → Loop (`no_repeat` / `repeat_one` / `loop`, icons `ic_video_no_repeat` / `ic_video_repeat` / `ic_video_loop`); saved as `isNoRepeat` |
| Mute | toggles volume; label "Unmute"/"Mute" (`unmute` / `mute`), icon `ic_video_unmute` / `ic_video_mute` |
| Speed | popup `popmenu_video_speed`: card `videoSpeedBackColor` #2D2D2D (add), radius 16.sdp, width 100.sdp; rows 0.25x … 2.0x `GalleryText.BodyMediumMedium` **white**, `ic_video_speed_select` on the current one |
| Subtitle / audio track | track pickers; none available → toast "Subtitle is not available." `subtitle_not_available` / "Audio is not available." `audio_not_available` |
| Details | `dialog_video_property` (§1.9) |
| Capture | as §1.8 |
| Rotate | toggles portrait / landscape |
| Floating | picture-in-picture popup needs Display-over-other-apps (`MANAGE_OVERLAY_PERMISSION`). Phase 5 builds the button; its popup player is the floating feature (Phase 10). Until then the tap is a no-op hook with a `shortcut:` comment naming Phase 10 |
| Errors | missing file → "Video not found." `video_not_found`; unsupported → "Unsupported video format" `this_video_format_not_supported`; generic → "An issue occurred!" `something_went_wrong` |
| Close / Back | stop and release the player immediately (audio must stop); also pause on `ON_STOP` |

---

## 3. Shared list frame (four screens share `MA` in the reference: Media list §4, Animated §8, Favorites §10, Search colour §12)

One composable for all four (the reference uses one adapter). Pieces:

### 3.1 Toolbar (`R272/layout/layout_toolbar.xml`)

| Element | Value |
|---|---|
| Bar | height 56.dp, `backgroundColor`, elevation 2.sdp; inner row padding start 4.sdp / end 10.sdp; 1.dp bottom divider `actionbarDividerColor` (#FFFFFF light / #30FFFFFF dark) |
| Back | `ic_back`, padding 6.sdp, borderless ripple |
| Title | `GalleryText.BodyLargeSemiBold` `titleColor`, 1 line ellipsize end, weight 1, marginStart 4.sdp |
| Selection mode | Back → Close `ic_close` (padding 6.sdp); title → "%d item(s) selected" `items_selected`; "Select all" `select_all` / "Unselect all" `unselect_all` shows at the end, `GalleryText.BodyMediumMedium` `colorPrimary`, padding 5.sdp |

### 3.2 Grid (`MA`, `item_grid_gallery`, `item_date`)

| Element | Value | Reference |
|---|---|---|
| List padding | 2.sdp h / 4.sdp v, `clipToPadding=false`, fast-scroll thumb on the end edge (reuse the Phase 3 `FastScrollThumb`) | `fastScroller` |
| Columns | the shared `mediaColCount` (Phase 3: 3..5, default 3), pinch changes it one step and saves it (Phase 3 §2.7 rule); every `MA` screen pinches (`*_zoom_level` events) | `ML`, `FA`, `AN`, `SC` |
| Date header | full-width row: "Today" `today` / "Yesterday" `yesterday` / `dd MMM yyyy`; `GalleryText.BodySmallMedium`, colour `black` (#000000 light / #FFFFFF dark), padding 5.sdp h / 6.sdp v, start-aligned. Group by day (reuse `buildPhotoRows` / `headerKind`) | `item_date`, `ka/a` |
| Cell | square, margin 0.5.dp all round, radius 0, centre-crop thumbnail, placeholder `image_loading_albums` | `item_grid_gallery` |
| Video | bottom-start row, margin 3.sdp: `ic_media_video` + duration `%02d:%02d` `GalleryText.BodyXSmallMediumGrid` `onlyWhite` shadow #FF000000 (2, 2) r 4, padding 3.sdp h | `playBtn`, `duration` |
| File type | GIF / RAW / SVG label top-start, margin 6.sdp, 12.sp Inter Medium `onlyWhite`, shadow r 4 (reuse `fileTypeLabel`) | `file_type`, `ka/c` |
| Favourite mark | `ic_common_fav` bottom-end, margin 1.sdp, when the item is a favourite | `ivFavImg` |
| Selection mark | `ic_check` / `ic_un_check` top-end, margin 1.sdp, only in selection mode | `selected` |
| Entrance | items fade in (0→1), scale 1.05→1.0 and slide in, 150 ms, staggered 15 % (`R272/anim/item_animation_fall_down`, `layout_animation`) | `layoutAnimation` |
| Tap | opens the viewer at that item with this list as its pages; in selection mode toggles | `MA` |
| Long-press | enters selection with that item; dragging selects a range (`DragMultiSelectHelper`) | `t9/h` |

### 3.3 States

| State | Look |
|---|---|
| Loading | centred `colorPrimary` spinner (elevation 10.sdp) until the first query returns |
| Empty | Phase 3 empty block (`ic_empty_img`; heading `GalleryText.BodyLargeSemiBold` `titleColor` marginTop 20.sdp / bottom 10.sdp; body `GalleryText.BodySmallRegular` `titleColor` centred, side margins 50.sdp) with the per-screen copy below |
| Banner slot | collapsed (D5) |

### 3.4 Selection bottom bar (`layout_select_media_bottom`, Phase 3 `HomeSelectionBottomBar`)

Same look as Phase 3 §2.6. Favourite slot: icon `ic_home_select_fav` + "Favorite" `favorite`, or `ic_fav_fill` + "Unfavorite" `unfavorite` when every selected item is already a favourite.
Slots per screen (derived from each activity's bottom-menu events and string refs; confirm on device):

| Screen | Slots |
|---|---|
| Media list, Animated | Share · Favorite/Unfavorite · Add to · Add to Private · Delete · Create album |
| Favorites | Share · Unfavorite · Add to Private · Delete |
| Search colour | Share · Favorite/Unfavorite · Add to Private · Delete |

Results: toasts "Media files deleted successfully." `selected_media_files_successfully_deleted`, "Media files hidden successfully." `selected_media_files_successfully_hide`, "Media files copied successfully." / "…moved successfully." `selected_media_files_successfully_copied` / `_moved`; progress titles "Deleting" `deleting`, "Copying" `coping`, "Moving" `moving`; same-folder move → `media_files_should_not_move_to_same_folder`. Back clears the selection first.

---

## 4. Media list (`R272/layout/activity_media_list.xml` — #16)

Ours: `album/AlbumDetailScreen.kt` (all `filter:*` lists and device folders). Use the §3 frame.

| Element | Value |
|---|---|
| Title | the album name; virtual lists: "All Photos" `all_photos`, "Videos" `videos`, "Camera" `camera`, "Screenshots" `screenshot` (Phase 4 §1.4); moment lists the moment name; place lists the place name |
| Organizer stripe slot | under the toolbar (`organizerStripeStub`, `isOrganizerStripeShown`): Phase 6 (CLN-04) owns it; Phase 5 leaves it out |
| Pinch tutorial | the Phase 3 overlay (`pinchToZoomInStub`) shows here too when `isPinchInOutToolTip` is false |
| Empty | "No items" `no_items` / "No media files here." `no_media_file` |
| Bottom bar | §3.4 (6 slots) |

Ours: drop `GridCells.Fixed(4)` and the literal `Text("No")`/`Text("Yes")` dialogs (use Phase 3 `DeleteMediaDialog`).

---

## 5. All albums — "Other Albums" (`R272/layout/activity_all_album.xml` — #17)

2.7.2 opens it only from notifications / deep links (`SplashActivity` redirect); nothing in-app links to it. Build the screen and route (`Screen.AllAlbums`) now; Phase 9 (SET-04/05) wires the entry.

| Element | Value |
|---|---|
| Toolbar | §3.1, title "Other Albums" `other_albums` |
| Grid | 3 columns (`AL`), `clipToPadding=false`, cards `item_grid_square_album` single-cover face (`mainLayout`): square card radius 10.sdp, centre-crop cover, type badge `ic_common_fav` bottom-start (margin 5.sdp start / 3.sdp bottom) when relevant, selection mark 24.sdp top-end margin 4.sdp; card margin end 8.sdp / bottom 8.sdp |
| Title / count | title marginTop 8.sdp `GalleryText.BodyMediumMedium` `titleColor` 2 lines ellipsize, padding 5.sdp h; count marginTop 2.sdp / bottom 2.sdp `GalleryText.BodyXSmallMedium` `countTitle` #7A8389 (add) 1 line, bare number |
| Tap | media list of that album (§4) |
| Selection | long-press → Phase 4 album selection (§1.5 of 04-UI-SPEC: Pin/Unpin, Add to, Delete, Change cover, Rename + top-bar Info) with `layout_select_album_bottom` |
| Empty | `ic_empty_img` + "No items" `no_items` only (no body line) |
| Ad rows `item_ad_all_album` | collapsed (D5) |

---

## 6. Moments (`R272/layout/activity_moments.xml` — #18)

Ours: `discovery/MomentsScreen.kt` (keep `MomentScanner` and its labels; restyle).

| Element | Value |
|---|---|
| Toolbar | §3.1, title "Moments" `moment` |
| Scanning header | while the scan runs: block marginTop 10.sdp under the toolbar; "Scanning Photos...." `scanning_photos` `GalleryText.BodySmallRegular` `titleColor` (top 10.sdp, start 10.sdp); processed count top-end `GalleryText.BodyLargeSemiBold` `titleColor` (top 10.sdp, end 10.sdp); determinate horizontal bar full width, margins 5.sdp v, track `seekbar_color_bg` (#BDD7FE / #5B5D5F, add), progress `colorPrimary`; "Scanning time depends on number of photos" `scanning_time` `GalleryText.BodyXSmallRegular` `titleColor` centred, bottom 5.sdp |
| First scan, nothing found yet | centred `moment_scanner_lottie` 140.sdp, loop, + "Kindly wait as we are scanning images. This won’t take long.." `moment_lottie_text` `GalleryText.BodySmallMedium` `titleColor` centred, 10.sdp below |
| Grid | 3 columns of `item_search_sub` (§11.3) — moment cover, moment name, count |
| Tap | media list titled with the moment name |
| Empty (scan done, none) | `ic_empty_img`; "Oops!" `oops`; "No moment files here." `no_moment_files_found` (no side margins) |
| Banner | collapsed |

---

## 7. Place (`R272/layout/activity_map.xml` — #19)

Reference: a Google Map with photo markers under a draggable bottom sheet listing places. Ours today shows a plain list (`AlbumDetail filter:place`). New `discovery/PlaceScreen.kt`; the Explore Place tile (Phase 4 §3.7) and the viewer's "Show on map" route to it.

| Element | Value |
|---|---|
| Toolbar | §3.1, title "Geographic photo" `geographic_photo` |
| Location stripe | under the toolbar while location permission is not granted (`layout_explore_allow_permission`): background `premium_explore_bg` #1EA543; `ic_location` 18.sdp margin 3.sdp; "Allow Gallery to access your location." `allow_gallery_location` `GalleryText.BodyXSmallSemiBold` `onlyWhite` centred, weight 1, marginStart 5.sdp / end 10.sdp; pill `premium_location_button_bg` (#E3FFE4, radius 15.sdp, 1.sdp stroke #03681F; import) padding 10.sdp h / 4.sdp v, marginEnd 5.sdp: "Allow" `allow` `GalleryText.BodySmallSemiBold` `premium_explore_button` #03681F + `ic_location_premium_arrow` marginStart 6.sdp. Tap requests ACCESS_FINE + ACCESS_COARSE location; permanently denied → app settings + toast "Please grant location permission." `home_permission_location` |
| Map | fills below the toolbar; one marker per place: bitmap of `item_map_marker` — 60.sdp thumbnail in `marker_bg` (`backgroundColor`, radii 30.sdp with the bottom-start corner 6.sdp; import), padding 6.sdp, margin 10.sdp top / 6.sdp bottom, elevation 20.sdp; place name below `GalleryText.BodySmallSemiBold` `onlyBlack` (add). Marker tap animates the camera to it (`newLatLngZoom`) |
| Bottom sheet | header `bottom_sheet_header_back` (`backgroundColor`, top radii 20.sdp; import) with `ic_bottom_sheet_drag` (41×4.dp #DADADA; import) margin 12.sdp v; body `backgroundColor`; 3-column grid of `item_location` |
| Place card (`item_location`) | margin 4.sdp; square card radius 10.sdp, cover centre-crop (or the 2×2 mosaic face on `other_album_bg_color`, 2.dp gaps, as `item_grid_square_album`); title marginTop 4.sdp `GalleryText.BodyMediumMedium` `titleColor` 2 lines; count marginTop/Bottom 2.sdp `GalleryText.BodySmallMedium` `grayTitle` 1 line |
| Card tap | media list of that place |
| Empty | "No items" `no_items` `GalleryText.BodyLargeSemiBold`, marginTop 30.sdp, in the sheet |
| Loading | spinner marginTop 30.sdp in the sheet |
| Banner | collapsed |

**Decided by the user 2026-11-21 (see User decisions):** the map needs the Google Maps SDK (`play-services-maps`, not in ours) and **our own** API key; the reference key in its manifest must not be reused.
Contract default if the user agrees: add the Maps SDK with a key from `local.properties` (`MAPS_API_KEY`); without a key the map area stays `backgroundColor` and the sheet still works. Location permission is new to our manifest (also ⚠).

---

## 8. Animated (`R272/layout/activity_animated_media_list.xml` — #20)

Opened in 2.7.2 only from notification/deep link (`SplashActivity`); keep our route `Screen.AnimatedMedia` reachable for Phase 9.

| Element | Value |
|---|---|
| Toolbar | §3.1, title "Animated" `animated` |
| Scanning header | as Moments but fixed height 75.sdp, marginTop 10.sdp / bottom 5.sdp, bar margin h 10.sdp, count colour default (`titleColor`) |
| Scanner state | `moment_scanner_lottie` 140.sdp + `moment_lottie_text`, margin h 20.sdp |
| Grid | §3.2 frame |
| Empty | "No items" `no_items` / "No media files here." `no_media_file` (ours: "No animated photos found", "GIF and motion photos will appear here.": remove) |
| Bottom bar | §3.4 (6 slots) |

---

## 9. Burst (`R272/layout/activity_burst_photo.xml` — #21)

Opened only from notification/deep link in 2.7.2; keep `Screen.BurstPhotos`.

| Element | Value |
|---|---|
| Toolbar | §3.1, title "Burst" `burst` |
| Grid | 3 columns, list padding 4.sdp all round; one card per burst group, `item_grid_square_album` **mosaic face** (`otherLayout`): square card `other_album_bg_color` (#BFD4E4 / #333333, add) radius 10.sdp; 2×2 frames (centre-crop, 1.dp inner margins, 2.dp gaps); the 4th tile dimmed `other_album_trans` #80000000 (add) with an `albumCount` overlay (`GalleryText.BodyXXLargeMedium`, bold, `onlyWhite`, alpha 0.9, shadow #80000000 (0.5, 0.5) r 2) |
| Title / count | as §5 (title padding 5.sdp h; count bare number `String.valueOf(size)`, `be/b`) |
| Tap | media list of the burst frames |
| Empty | "No items" / "No media files here." |
| Loading | centred spinner |

Ours: drop `Icons.Default.BurstMode` and the "BURST" / "Burst Photos" / "No burst shots found" literals.

---

## 10. Favorites (`R272/layout/activity_favorite.xml` — #22)

Ours: `favorites/FavoritesScreen.kt`. §3 frame.

| Element | Value |
|---|---|
| Toolbar | §3.1, title "Favorites" `favorites` |
| Empty | "No Favorites" `no_favorites` / "Tap on the heart icon to save your favorite media to quickly locate them." `no_favorites_empty` (side margins 50.sdp) |
| Bottom bar | §3.4 (4 slots); unfavouriting removes the items from this list at once |

Ours: drop `GridCells.Fixed(4)`, `Icons.Default.Favorite/PlayArrow`, the literal empty texts and `Text("No")`/`Text("Yes")`.

---

## 11. Search (`R272/layout/activity_search.xml`, `layout_search_toolbar.xml`, `item_search_*` — #23)

Ours: `search/SearchScreen.kt`. Activity theme is transparent with `adjustNothing` (keyboard does not resize the page).

### 11.1 Search toolbar

| Element | Value |
|---|---|
| Bar | 56.dp, `backgroundColor`, elevation 2.sdp, row padding start 4.sdp / end 10.sdp, 1.dp divider `actionbarDividerColor` |
| Back | `ic_back` padding 6.sdp |
| Field | `search_back` box, marginStart 4.sdp / end 10.sdp; text `GalleryText.BodySmallMedium` colour `black`, hint "Search" `search` `hintLanguageColor` (#501D242E / #50FFFFFF), 1 line, IME action Search, padding 10.sdp h / 7.sdp v |
| Icons | `ic_search_common` marginEnd 10.sdp while empty; `ic_close` 19.sdp (padding 1.sdp, marginEnd 10.sdp) clears the text when not empty |

### 11.2 Body (one vertical scroll)

| Element | Value |
|---|---|
| "Colors" header | marginTop 10.sdp: "Colors" `colors` `GalleryText.BodyLargeMedium` `black`, marginStart 5.sdp |
| Colour row | horizontal scroll, padding 4.sdp h, marginTop 5.sdp: colour-picker button `ic_search_color_picker` (padding 6.sdp, borderless ripple) then ten swatches (`item_search_color`): 20.sdp circle (radius 10.sdp), 1.sdp `onlyWhite` stroke, margin 4.sdp, colours `colorSearch1..10` in order #F2F2F2, #000000, #FF0000, #FFE500, #FF7A00, #FF00E5, #9E00FF, #0066FF, #00C2FF, #008625 (ours already matches) |
| Divider | 0.5.dp `dividerColor` (#1A002847 / #20FFFFFF), marginTop 10.sdp, margin h 8.sdp |
| Sections (`item_search_main`) | "Albums" `albums`, "Places" `places`, "Moments" `moment` (`SearchViewModel`; events `search_albums_album`, `search_places_album`, `search_moment_album`), each only when it has results. Header row marginTop 10.sdp: title `GalleryText.BodyMediumSemiBold` `black` marginStart 5.sdp, weight 1; "View All" `view_all` `GalleryText.BodyMediumMedium` `colorPrimary` marginEnd 10.sdp. Grid 3 columns of `item_search_sub`, marginTop 5.sdp, padding 2.sdp h; 0.5.dp divider below as above |
| Ad row (`item_search_ad`) | collapsed (D5) |
| Typing | filters the three sections by name (folder name, place, moment label) |
| Swatch tap | Search colour (§12) with that colour |
| Picker tap | "Choose color" `choose_color` picker dialog starting at #007AFF, "Cancel" `cancel` / "Ok" `ok`; Ok opens §12 with the chosen colour |
| Section item tap | media list of that album / place / moment |
| View All tap | Search view all (§11.4) — a new screen, not the in-place expand (ours: remove "View All"/"Collapse" toggle) |
| Empty (nothing matches) | `ic_empty_img`; "Oops!" `oops`; "We couldn't locate the folder you're searching for. It might have been moved, renamed, or deleted." `search_deleted` (side margins 50.sdp) |
| Loading | centred `colorPrimary` spinner |

### 11.3 Search sub-item (`item_search_sub`, also Moments §6)

margin 4.sdp; square card radius 10.sdp, elevation 0, centre-crop thumbnail; title marginTop 4.sdp `GalleryText.BodyMediumMedium` `titleColor` 2 lines ellipsize end, padding 5.sdp h;
count marginTop 2.sdp / bottom 2.sdp `GalleryText.BodyXSmallMedium` `countTitle` 1 line, padding 5.sdp h, bare number.

### 11.4 Search view all (`R272/layout/activity_search_view_all.xml`)

New route `Screen.SearchViewAll` (section type + query). §3.1 toolbar titled with the section name; 3-column grid of `item_search_sub`, list padding 2.sdp h / 4.sdp v; empty "Oops!" + `search_deleted`.

---

## 12. Search by colour (`R272/layout/activity_search_photo_color.xml` — #24)

Ours: `discovery/SearchColorScreen.kt`. §3 frame; toolbar title "Media Color" `media_color`; loading spinner sits below the toolbar; empty "No items" / "No media files here."; bottom bar §3.4 (4 slots).
Ours: remove `ic_more`, `ic_pro_premium`, `Icons.Default.Check` and `GridCells.Fixed(4)`.

---

## Spacing Scale

Phase 1 scale applies (sdp units: 2, 4, 8, 12, 16, 24, 32, 48). Values used here, all exact reference values:
2.sdp (list padding, count gap), 4.sdp (card margins, list padding v, seek gap), 8.sdp (album card margins, more-popup padding, Paused chip padding), 12.sdp (dock row padding, video bottom row gap, short-video button padding), 16.sdp (rename margins), 24.sdp (album selection mark), 32.sdp not used, 48.sdp not used.

Exceptions (parity overrides multiples of 4, per Phase 1): 1, 3, 5, 6, 7, 10, 13, 14, 15, 18, 19, 20, 22, 25, 26, 28, 30, 34, 40, 50, 52, 60, 75, 80, 100, 140, 160 sdp;
unscaled 0.5.dp (grid cell margin, dividers), 1.dp, 2.dp, 2.5.dp, 5.dp, 6.dp, 7.dp, 8.dp, 12.dp, 14.dp, 16.dp, 26.dp, 36.dp, 56.dp. Never round them.

---

## Typography

All Inter, `includeFontPadding = false`, natural line height (Phase 1 rule).

| Role | Size | Weight | Line Height | Where |
|------|------|--------|-------------|-------|
| Body | 10ssp | 400 Regular | natural (~1.21) | empty-state body, scanning text (`BodySmallRegular`) |
| Label | 11ssp | 500 Medium | natural (~1.21) | search-sub titles, property rows, speed rows, View All (`BodyMediumMedium`) |
| Heading | 13ssp | 600 SemiBold | natural (~1.21) | toolbar titles, empty headings (`BodyLargeSemiBold`) |
| Display | 16ssp | 600 SemiBold | natural (~1.21) | Details sheet title (`BodyXXLargeSemiBold`) |

Exception (parity, as in Phases 1, 3, 4): the reference also uses 8ssp/400 (`BodyXSmallRegular` scan hint), 8ssp/500 (`BodyXSmallMedium` times, quick-control labels, counts; `BodyXSmallMediumGrid` durations),
8ssp/600 (location stripe), 10ssp/500 (`BodySmallMedium` suggestion pills, popup rows, date headers, search field), 10ssp/600 (marker label, Allow), 11ssp/600 (section titles),
13ssp/500 (`BodyLargeMedium` "Colors", subtitle), 14ssp/600 (rename title), 16ssp/500 bold (burst "N+" overlay), and 12sp Inter Medium (grid file-type label, unscaled in the reference). Weights in use: 400, 500, 600. The section tables are binding.

---

## Color

Phase 1 roles, applied here:

| Role | Value | Usage |
|------|-------|-------|
| Dominant (60%) | `backgroundColor` #FFFFFF / #0C0C0C; viewer `fullImageBackColor` #000000 / #0C0C0C; video `video_player_background` #0C0C0C | list screens; viewer; video player |
| Secondary (30%) | glass `media_full_glass_bg` #8C1C1D21, dock `media_full_pill_bg` #E61C1D21; `dialogBack` #FFFFFF / #1E1E1E (popups, sheets); `videoPlayerColor` #1E1E1E | viewer chrome; menus; video top bar |
| Accent (10%) | `colorPrimary` #0066FF | reserved list below |
| Destructive | `red` #E11A1A (Phase 1) | delete confirmations only |

Accent reserved for: "Select all"/"Unselect all", "View All", loading spinners, the moments/animated scan progress, the short-video seek bar in dark mode (`shortVideoProgress` #1E3354 light / #0066FF dark), the dialog primary buttons (`btn_permission`).
Not for icons, titles, cards or the glass chrome (icons on the chrome are `media_full_glass_icon` #FFFFFF in both themes).

Fixed non-theme colours from the reference: `blackTran3` #DD000000 (suggestion pills), status scrim #73000000, slide-show controls #90000000, `premium_explore_bg` #1EA543 / `premium_explore_button` #03681F / `premium_explore_button_bg` #E3FFE4 (location stripe),
`other_album_bg_color` #BFD4E4 / #333333 and `other_album_trans` #80000000 (mosaic), `countTitle` #7A8389, `grayTitle` #585758 / #7A8389, `property_text` #5C7A90 / #FFFFFF, `videoSpeedBackColor` #2D2D2D, `seekbar_color_bg` #BDD7FE / #5B5D5F, map drag handle #DADADA.

**Colours to add** (missing in `values/colors.xml` / `values-night`): `fullImageBackColor`, `video_bg`, `videoPlayerColor`, `videoSpeedBackColor`, `seekbar_color_bg`, `countTitle`, `other_album_bg_color`, `other_album_trans`, `blackTran1`, `blackTran2`, `onlyBlack`, `colorSearch2` (check with `python -I scripts/ref_res.py check-colors`).
**Drawables to add** (missing in ours, copy from `R272/drawable/`): every video-player icon listed in §2; `ic_full_video_play`, `edit_opt_back`, `sb_progress_drawable`, `sb_thumb`, `ic_slide_control_back`, `ic_slide_mute`, `ic_slide_unmute`, `custom_ripple`, `custom_video_ripple`, `property_dialog_back`, `ic_map`, `ic_location`, `ic_location_premium_arrow`, `premium_location_button_bg`, `marker_bg`, `bottom_sheet_header_back`, `ic_bottom_sheet_drag`, `bottom_sheet_back`, `ic_image_count_back`, `ic_video_ad_close` (only if its state is built). Already present: `bg_media_full_glass_btn`, `bg_media_full_pill_dock`, `bg_media_full_status_scrim_dark`, `ic_media_rotate`, `ic_favorite`, `ic_fav_fill`, `ic_property`, `ic_camera_ss_short`, `ic_short_video_*`, `ic_hide`, `ic_main_edit`, `ic_unhide`, `ic_restore`, `ic_add`, `ic_rename`, `ic_slideshow`, `ic_set_as`, `ic_print`, `ic_edit_small_*`, `ic_common_fav`, `ic_media_video`, `search_back`, `ic_search_common`, `ic_search_color_picker`.

---

## Copywriting Contract

Every visible string is a reference key; translations come from the Phase 1 sync (`python -I scripts/ref_res.py sync-strings`, then `check-strings`). Keys 2.7.2 leaves in English stay English.
No hard-coded English remains in the Phase 5 files (today, among others: "Share Media", "Add to Private", "Added to Favorites", "Removed from Favorites", "Restored to Gallery", "Moved to Recycle Bin",
"Deleted permanently", "Renamed successfully", "Could not rename file", "Screenshot captured & saved to Gallery", "Failed to capture screenshot", "Capture Screenshot", "Close suggestions", "Playback Speed",
"Play", "Pause", "Mute", "Unmute", "Delete", "Delete Permanently", "Cancel", "Ok", "No", "Yes", "Select All", "Deselect All", "Unselect All", "X Selected", "BURST", "Burst Photos", "No burst shots found",
"No animated photos found", "GIF and motion photos will appear here.", "No search results found", "Collapse", "View All", "Colors", "Albums", "Moments", "Scanning Photos....",
"Scanning time depends on number of photos", "Oops!", "No Favorites", "No Albums Found"). Reference literals kept as `translatable="false"` resources: "Back" (back cd), "Capture" (capture cd).
Toasts ours shows but the reference does not (favourite added/removed, restored, moved, renamed, screenshot saved/failed) are removed.

| Element | Copy (reference key) |
|---------|------|
| Primary CTA | "View All" (`view_all`, Search sections); "Allow" (`allow`, Place location stripe) |
| Empty state heading | "No items" (`no_items`) — media list, all albums, animated, burst, colour, place; "No Favorites" (`no_favorites`); "Oops!" (`oops`) — moments, search, view all |
| Empty state body | "No media files here." (`no_media_file`); "Tap on the heart icon to save your favorite media to quickly locate them." (`no_favorites_empty`); "No moment files here." (`no_moment_files_found`); "We couldn't locate the folder you're searching for. It might have been moved, renamed, or deleted." (`search_deleted`) |
| Error state | "Video not found." (`video_not_found`); "Unsupported video format" (`this_video_format_not_supported`); "Corrupted video not play." (`toast_corrupted_video`); "Couldn't rotate image!" (`couldn_t_rotate_image`); "Unable to edit animated image." (`toast_animated_image`) / "Unable to edit corrupted image." (`toast_corrupted_image`) / "Unable to edit video." (`toast_video`); "Subtitle is not available." / "Audio is not available."; "No next video" / "No previous video"; "Please grant location permission." (`home_permission_location`); generic "An issue occurred!" (`something_went_wrong`) |
| Destructive confirmation | Viewer delete: Phase 3 `dialog_delete` — "Are you sure you want to delete 1 item?" (`delete_dialog_permission`) + "Move to Recycle Bin" (`delete_dialog_move`), "No" / "Yes"; Recycle Bin viewer: "Delete items" (`delete_items`) — "All items will be permanently deleted from your device and cannot be recovered." (`delete_items_description`); vault viewer: "Are you sure you want to delete this item?" (`deleted_item_permission`); list selections: `deleted_title` + N + `deleted_items`; hide: "Hide Media" / "Are you sure you want to hide media?" (`hide_media` / `hide_item_permission`); unhide: "Unhide items" / "Are you sure you want to unhide items?" (`unhide_items` / `unhide_item_permission`); restore: "Restore items" / "Are you sure you want to restore items?" (`restore_items` / `restore_item_permission`) |

**Keys to import** (missing in `values/strings.xml`; copy from `R272/values/strings.xml`, then sync translations):
`rotate`, `blur_background_guide`, `copy_all_text`, `show_on_map`, `time_duration`, `couldn_t_rotate_image`, `delete_items`, `delete_items_description`, `deleted_item_permission`, `restore_items`, `restore_item_permission`,
`unhide_items`, `unhide_item_permission`, `toast_animated_image`, `toast_corrupted_image`, `toast_corrupted_video`, `toast_video`, `this_video_format_not_supported`,
`speed`, `no_repeat`, `repeat_one`, `loop`, `mute`, `unmute`, `capture_ss`, `ten_sec`, `paused`, `no_next_video`, `no_previous_video`, `subtitle_not_available`, `audio_not_available`, `video_not_found`,
`unfavorite`, `deleting`, `coping`, `moving`, `selected_media_files_successfully_hide`, `selected_media_files_successfully_copied`, `selected_media_files_successfully_moved`, `wrong_root_selected`,
`other_albums`, `moment`, `animated`, `burst`, `geographic_photo`, `allow_gallery_location`, `allow`, `home_permission_location`, `places`, `colors`, `view_all`, `choose_color`, `oops`, `search_deleted`,
`no_moment_files_found`, `scanning_photos`, `scanning_time`, `moment_lottie_text`, `no_favorites`, `no_favorites_empty`.
Already present: `share`, `private_bottom`, `edit`, `add_to`, `delete`, `more`, `restore`, `unhide`, `favorite`, `favorites`, `details`, `name`, `path`, `size`, `resolution`, `taken_on`, `ok`, `cancel`, `save`, `enter_name`,
`rename`, `slide_show`, `set_as_wallpaper`, `print`, `remove_background`, `color_splash`, `crop`, `markup`, `adjust`, `filter`, `copy_text_from_photo`, `hide_media`, `hide_item_permission`, `delete_dialog_permission`,
`delete_dialog_move`, `deleted_title`, `deleted_items`, `items_selected`, `select_all`, `unselect_all`, `no_items`, `no_media_file`, `today`, `yesterday`, `search`, `albums`, `media_color`, `all_photos`, `videos`,
`camera`, `screenshot`, `add_to_title`, `media_files_should_not_move_to_same_folder`, `selected_media_files_successfully_deleted`, `something_went_wrong`.

---

## UI Considerations

Applicable state considerations resolved: 16 covered, 9 backstop, 5 unresolved

| Category | Element(s) | Status | Resolution / Reason |
|----------|------------|--------|---------------------|
| visual | viewer chrome | ✅ covered | The viewer shows round glass buttons over the photo (back; rotate, heart, details) and a floating dark pill dock with icon-only Share, Private, Edit, Delete, More; no date/time text and no opaque bars |
| interaction | chrome toggle | ✅ covered | A single tap slides the top bar up and the dock down with a fade in 150 ms and shows the status scrim; another tap brings them back |
| interaction | zoom | ✅ covered | Double-tap zooms a portrait photo to 5× (landscape 2×) in 200 ms and back to fit; swiping between photos works only at fit zoom |
| timing | suggestion strip | ✅ covered | The edit-suggestion pills appear 1.5 s after a still photo settles, never on videos, Recycle Bin or vault, and ✕ hides them |
| mode | dock per mode | ✅ covered | Video pages show Add to instead of Edit; Recycle Bin shows Restore + Delete; vault shows its own row |
| populated | More popup | ✅ covered | More opens a rounded card above the dock with Add to, Rename, Slide show, Set as Wallpaper, Print, plus Show on map only for photos with a place |
| media | short vs long video | ✅ covered | A video ≤ 60 s plays inside the viewer with mute/play/speed in the dock and a seek row; a longer one shows a centre play button that opens the Video player |
| audio | closing | ✅ covered | Leaving the viewer or the Video player, changing page, or sending the app to background stops the audio immediately |
| populated | video player | ✅ covered | The Video player shows the top bar, Speed/Repeat/Mute/Capture card and the bottom panel with seek, lock, previous/play/next, rotate, floating, with 2.7.2 icons and labels |
| empty | every list screen | ✅ covered | Each list shows its own empty copy from the Copywriting table (No items / No Favorites / Oops!) under `ic_empty_img` |
| loading | every list screen | ✅ covered | Lists show a centred blue spinner until the first query returns, never the empty copy first |
| zero-one-many | counts | ✅ covered | Album, moment, place and burst counts are bare numbers |
| populated | media grids | ✅ covered | Media list, Favorites, Animated and Colour grids use the shared column count, day headers (Today / Yesterday / dd MMM yyyy), 0.5.dp gaps, video duration and favourite marks |
| interaction | pinch | ✅ covered | Pinching any of those grids changes the columns within 3..5 and the change is shared with the Photos tab |
| selection | list selection bars | ✅ covered | Long-press shows the close / count / Select all toolbar and the per-screen bottom slots from §3.4 |
| navigation | search | ✅ covered | Typing filters Albums, Places and Moments; a swatch or the picker opens Media Color; View All opens its own screen |
| animation | list entrance, chrome slides | 🧪 backstop | Compare item fall-down entrance and 150 ms chrome slides with 2.7.2 side by side |
| locale | long translations (de, ru, ta) in popup rows, toolbar titles, quick-control labels | 🧪 backstop | All 1 line (labels 52.sdp wide may truncate); check nothing wraps |
| rtl | dock order, seek row, More popup anchor | 🧪 backstop | ar/fa/ur mirror rows and the popup's end anchor |
| dark | every screen | 🧪 backstop | Light and dark screenshots match 2.7.2 (viewer #000/#0C0C0C, popups #1E1E1E, `black` headers turn white) |
| selection | slot lists for Favorites / Colour | 🧪 backstop | Derived from bottom-menu event names and string refs; confirm on device |
| media | inline video end | 🧪 backstop | Ours loops (`REPEAT_MODE_ONE`); confirm whether 2.7.2 stops or loops at the end, then match |
| video | repeat cycle order, controls 3 s auto-hide, brightness/volume gestures | 🧪 backstop | Values from `VP` constants; compare on device |
| burst | mosaic count overlay | 🧪 backstop | The 4th-tile `albumCount` text format ("15+" sample) is confirmed on device before shipping |
| performance | large library in lists and viewer | 🧪 backstop | Scroll and page swipe stay smooth with thousands of items (checklist D-15) |
| dependency | Place map | ✓ resolved (user: add Maps SDK, key from local.properties) | Needs Google Maps SDK + our own API key (user decision); default: add with `MAPS_API_KEY` from `local.properties`, blank map area without it |
| permission | Place location stripe | ✓ resolved (user: add like 2.7.2) | ACCESS_FINE/COARSE_LOCATION are new to our manifest; the user confirms adding them (parity says yes) |
| scope | Video player Floating button | ⚠ unresolved | Popup player belongs to the floating feature (Phase 10); Phase 5 leaves a no-op hook |
| behaviour | slide show interval/options | ⚠ unresolved | Read `MF` slide-show code (`slideShowDuration`, `isSlideShowContinue`) before building §1.10 |
| behaviour | search section limit | ⚠ unresolved | How many items each section shows before "View All" is read from `dg/c`/`SearchViewModel` before building; contract until then: keep ours |

---

## Registry Safety

| Registry | Blocks Used | Safety Gate |
|----------|-------------|-------------|
| none (Android, no shadcn) | – | not applicable |

Asset provenance gate (Phase 1 rule): copy only from `R272/res`; no ad-SDK resources; the reference Google Maps API key is never copied; brand-art decision of 2026-11-13 stands.

---

## Out-of-phase notes (not specified here)

- **Phase 6 (CLN-04):** the media list's organizer stripe (`organizerStripeStub`, `layout_organizer_stripe` CHANGED).
- **Phase 9 (SET-04/05):** notification / deep-link entries to All albums, Animated, Burst, Search, Moments, Place, Favorites (`SplashActivity` redirect).
- **Phase 10:** Video player Floating popup.
- **Reference oddity seen on device:** a video page briefly shows Home through the transparent viewer theme; not part of the contract.

---

## Re-runnable provenance (2026-11-21)

- Layout id → class: `grep 'name="<layout>"' R272/values/public.xml`, then ripgrep the id under `S272` (0x7f0d003d → `MF`, 0x7f0d005b → `VP`, 0x7f0d0114/0x7f0d0138 → `ch/w`, 0x7f0d003e → `ML`, 0x7f0d001e → `AL`, 0x7f0d003f → `MO`, 0x7f0d003c → `MP`, 0x7f0d001f → `AN`, 0x7f0d0023 → `BU`, 0x7f0d002f → `FA`, 0x7f0d004a → `SE`, 0x7f0d004c → `SV`, 0x7f0d004b → `SC`).
- Binding field names: `ar/ab` and `ar/ck` constructor params follow the alphabetical id order of their layouts (backBtn, bottomAction, captureBtn, date, endTime, favBtn, …).
- Chrome toggle: `MF.o0O0ooO` / `MF.o000Oooo` (0x96 ms, `fullImageBackColor` 0x7f06015a); status scrim `MF.o000Oo00`; top-bar visibility `MF.o0000OOo`, `MF.o000oOoo`; suggestion delay 0x5dc at `MF` line ~24692.
- More popup: `MF` method containing 0x7f0d01da (`showAtLocation` gravity 0x50; `getPlace()` gate).
- Zoom: `S272/ab/o.smali` constructor (maxZoom 3.0, doubleTapZoom −1, overzoom 2.0, 200 ms), `S272/ch/i.smali` `BsUTWEAMAI` (2/5 and 10/6/5/4/3 thresholds 150/250/350/450).
- Inline vs external video: `S272/ch/m.smali` (0xea60, `content://`/`file://`), `S272/ch/w.smali` fields `onVideoPlay` / `onExternalVideoPlay`, `MF.Oooooo0` (video list → `VideoPlayerActivity`).
- Adapters: `MF`→`ch/w`; `ML`/`FA`/`AN`/`SC`→`ka/d` (`ka/a` Today/Yesterday/`DD_MMM_YYYY`); `AL`→`bx/g`; `MO`/`SV`→`dg/g`; `BU`→`be/c`; `MP`→`ce/b` (`item_location`). Spans: `GridLayoutManager(ctx, 3)` in `MO`, `SV`, `AL`, `MP`; `spanCount=3` in `activity_burst_photo`.
- Search sections: `SearchViewModel` (0x7f13047f `albums`, 0x7f130933 `places`, `moment`), events in `dg/c`.
- Device: `adb shell monkey -p albums.gallery.photo.folder.picasa.app.web.gallery -c android.intent.category.LAUNCHER 1`, open a photo and a short video from Photos, `adb exec-out screencap -p`.
- Missing keys/assets in ours: `grep -q 'name="<key>"' app/src/main/res/values/strings.xml`; `ls app/src/main/res/drawable*/<name>.*`; `python -I scripts/ref_res.py check-colors`.

---

## Checker Sign-Off

- [ ] Dimension 1 Copywriting: PASS
- [ ] Dimension 2 Visuals: PASS
- [ ] Dimension 3 Color: PASS
- [ ] Dimension 4 Typography: PASS
- [ ] Dimension 5 Spacing: PASS
- [ ] Dimension 6 Registry Safety: PASS
- [ ] Dimension 7 Inventory Provenance: PASS

**Approval:** pending

## User decisions (2026-11-21)
- **Place map:** use `play-services-maps`, with the key read from `local.properties` (blank map area when no key is set), as the spec defaulted.
- **Location permission:** add ACCESS_FINE_LOCATION and ACCESS_COARSE_LOCATION to the manifest. The Place "Allow" strip asks for them as 2.7.2 does.
