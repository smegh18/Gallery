---
phase: "6"
slug: "manage-and-cleaner"
status: approved
reviewed_at: "2026-11-23"
shadcn_initialized: false
preset: none
created: "2026-11-23"
scope: "Phase 6 system cleaner (activity_system_cleaner, junk scan / remove junk / junk cleared, large videos, screenshots), duplicate flow (waiting, scan done, photos, delete process, delete complete), Recycle Bin (item_grid_recycle_media CHANGED), Quick Organize + Organizer Recycle (layout_organizer_stripe, organize_intro_step_layout CHANGED); CLN-01..04; checklist #25-#31"
---

# Phase 6 — UI Design Contract (Manage and Cleaner)

> Parity port, not a new design. Every value below comes from the decompiled reference 2.7.2
> (layout XML for looks, smali for behaviour) or from the reference running on the test phone on
> 2026-11-23 (marked **[device]**, screenshots in `evidence/`). Nothing here is a preference.
> Tokens (`N.sdp`, `N.ssp`, Inter, `GalleryText.*`, colour names) come from the Phase 1 contract; selection bars,
> `DeleteMediaDialog`, `HideMediaDialog`, `PhotosEmptyState` and `CommonDialog` from Phase 3; the list frame
> (`SharedMediaToolbar`, `MediaSelectionBar`, `MediaAction`), viewer recycle mode and its Restore / Delete items
> dialogs from Phase 5. They are referenced here, not repeated.

**Evidence roots (read-only):**
- `R272` = `%TEMP%\ref272\dec\res` (2.7.2). 2.6.7 comparison: `aapt2 dump xmltree --file res/layout/<x>.xml gallery.apk`.
- `S272` = `%TEMP%\ref272\dec\smali\albums\gallery\photo\folder\picasa\app\web\gallery`
- `SC` `presentation/systemcleaner/main/activity/SystemCleanerActivity` (clicks `du/a`); `JS` `…/removeJunk/view/activity/JunkScanActivity` (coroutines `dx/e`, `dx/f`);
  `RJ` `…/RemoveJunkActivity` (binding `ar/al`, clicks `dx/g`, lottie listener `dx/j`, adapters `dy/m` main, `dy/g` row, `dy/n` sub); `JC` `…/JunkClearedActivity` (`dx/a`, list `dy/d`, clicks `aw/b`);
  `JV` `RemoveJunkViewModel`; `LV` `…/largeVideo/view/activity/LargeVideoActivity` + `LargeVideoViewModel` (adapter `dt/c`, `dt/a` cell, `dt/b` header, delete `ds/a`);
  `SS` `…/screenshort/view/activity/ScreenShortActivity` + `ScreenshotsViewModel` (adapter `b0/*`, delete `a0/e`); `CD` `dw/b` (cleaner delete dialog);
  `DW` `presentation/duplicatephoto/view/activity/DuplicateWaitingActivity` (done listener `ak/h`); `DP` `…/DuplicatePhotoActivity` (adapters `bj/d`, `bj/b`); `DR` `…/DuplicateDeleteProcessActivity` (`bi/b`, `bi/d`);
  `DC` `…/DuplicateDeleteCompleteActivity` (`bi/a`); `DS` `service/DuplicateFindService`; `RB` `presentation/recycle/view/activity/RecycleActivity` (adapter `da/f`, cell `da/e`; days in `cz/k`, `db/a`);
  `OA` `presentation/organizer/view/OrganizerActivity` (adapters `cq/*`, `cr/*`); `OR` `…/OrganizerRecycleActivity`; `ML` `presentation/medialist/view/activity/MediaListActivity` (stripe, `ck/a`, `be/a`); `K` `s9/k` (size formatters).
- **[device]** `.planning/phases/06-manage-and-cleaner/evidence/ref272-*.png` (Redmi M2006C3MII, app in light theme, 2026-11-23): `junk-scan`, `remove-junk`, `duplicate-waiting`, `duplicate-photo`, `duplicate-keep-one`, `recycle-empty`, `organizer-intro-1`, `organizer-intro-2`. No clean, delete, restore, move or album create was run.
- Ours = `app/src/main/java/com/quizango/gallery/ui/`, `data/`, `app/src/main/res/`

**Ads (D5), carried from Phases 3-5:** every ad slot in this phase is a `global_ad_framelayout` / `flAd` / `fl_ad` / `rl_banner_ad` /
`flSqueezeNative` frame with no skeleton of its own → **collapsed, 0 height**: the Remove Junk "Ads" list row (`dy/m` → `global_ad_framelayout`),
the Junk Cleared list ad (`dy/d`), the duplicate list native and banner (`relBannerAd`), Recycle and Organizer Recycle banners, the squeeze natives
on the scan screens (`flSqueezeNative`, `ivAdClose`). No interstitials or rewarded ads: every step that waits for one (`scan_interstitial`,
`back_interstitial`, `interstitial`, `reward`, `style_reward`, `perm_delete_interstitial`) continues at once. The two **AD badges on buttons** are a decision (Q2).

---

## Design System

| Property | Value |
|----------|-------|
| Tool | none (Android, Jetpack Compose; shadcn not applicable) |
| Preset | not applicable |
| Component library | Compose Material 3 (Phase 1). House pieces to reuse: `ui/browse/SharedMediaList.kt` (`SharedMediaToolbar` = `layout_toolbar`, `FastScrollThumb` use), `ui/browse/MediaListActions.kt` (`MediaAction`, `MediaSelectionBar` = `layout_select_media_bottom`), `ui/home/HomeSelection.kt` (`DeleteMediaDialog` = `dialog_delete`, `DialogButton`, `drawableBackground`), `ui/home/HomeDialogs.kt` (`CommonDialog`, `HideMediaDialog`, `CreateAlbumDialog`), `ui/home/PhotosTab.kt` (`PhotosEmptyState`, `buildPhotoRows`/`headerKind` for Today / Yesterday), `ui/viewer/ViewerDialogs.kt` (`RECYCLE_DELETE` / `RESTORE` kinds), `data/local/DuplicateFinder.kt` (reference fingerprint, keep), `data/local/JunkScanner.kt` (rules change, §2.6) |
| Icon library | Reference vector drawables via `painterResource`. No `material-icons` glyph where the reference ships one (today: `CleanerScreen` `Icons.AutoMirrored.Filled.ArrowBack`, every `TopAppBar` navigation icon in `ui/cleaner`, `ui/recycle`, `ui/discovery/QuickOrganizer*`/`OrganizerRecycle*`: remove) |
| Font | Inter (Phase 1) |
| Animation | Compose `animate*`/`Animatable`/`Crossfade`; Lottie (all present in `res/raw`): `scan_cleaner_lottie`, `done_cleaner_lottie`, `cleaner_lottie`, `junk_done`, `remove_junk_button_animation`, `success`, `duplicate_scanner_lottie`, `dm_success`, `quick_oraganize`, `hand_click`, `hand_left_arrow`, `hand_upside_arrow`, `remove_bg_tooltip` |

Component Inventory section omitted: `Tool: none` (no registry to enumerate; same as Phases 1, 3, 4 and 5).

**Screen map (planner gap list):**

| # | Reference | Ours today | Gap |
|---|---|---|---|
| 25 | SystemCleanerActivity (`activity_system_cleaner`, `item_system_cleaner`) | `cleaner/CleanerScreen.kt` (M3 `TopAppBar` 19.sp, `RoundedCornerShape(16.dp)` cards with `cardBackground` fill, hard-coded English row texts, 48.dp icons) | Restyle to §1 (layout_toolbar, `layout_shape` stroke cards, reference keys) |
| 26 | JunkScan / RemoveJunk / JunkCleared | `cleaner/RemoveJunkScreen.kt` (847 lines: all three merged, "Scanning..." / "Cleaning..." / "Done" / "Your phone is in optimal condition" / "Release more space" literals, `formatStorageBytes` with `%.2f`, categories Junk / APK / Temp / **.log** / Empty) | Split into the three states of §2; scanner rules §2.6; size format §2.7 |
| 27 | LargeVideoActivity (`activity_large_video`, `item_large_video`) | `cleaner/LargeVideoScreen.kt` (3 columns ok; "Delete (size)" labels, "Empty", "Selected" literals, no date headers) | §3 |
| 28 | ScreenShortActivity (`activity_screen_short`) | `cleaner/ScreenShortScreen.kt` (**3** columns, size labels, uses the duplicate-image delete copy) | §3 with 4 columns, screenshot copy |
| 29 | Duplicate Waiting / Photo / DeleteProcess / DeleteComplete | `cleaner/DuplicateWaitingScreen.kt` ("Scanning...", hint literals, `delay(2200)`), `DuplicatePhotoScreen.kt` (4 columns ok; "Group X (Y images)", "BEST", "Keep best photo", non-reference `permanent_delete_duplicate_*` keys), `DuplicateCompleteScreen.kt` ("N freed" literal, `.sp` sizes — HANDOFF deferral), **no delete-process screen** | §4; new delete-process state; plural + `%1$s freed` |
| 30 | RecycleActivity (`activity_recycle`, `item_grid_recycle_media` CHANGED) | `recycle/RecycleScreen.kt` (literal texts, "N days left", `maxOf(1, …)`, own "Empty Recycle Bin" dialog, auto-delete default **true**) | §5 |
| 31 | OrganizerActivity / OrganizerRecycleActivity | `discovery/QuickOrganizerScreen.kt` (2.6.7-era swipe card, "All caught up!" literals, no intro steps, no album row), `discovery/OrganizerRecycleScreen.kt` ("Organizer Bin", "Empty Bin" literals, ad-hoc action dialog) | Rebuild to §6; media-list stripe §6.6 |

---

## 1. System Cleaner (`R272/layout/activity_system_cleaner.xml`, `item_system_cleaner.xml` — #25, CLN-01)

| Element | Value | Reference |
|---|---|---|
| Screen | `backgroundColor` (#FFFFFF / #0C0C0C), edge-to-edge insets as Phase 5 lists | `main` |
| Toolbar | `SharedMediaToolbar` (layout_toolbar), title "System Cleaner" `tool_bar_system_cleaner` | `toolBarLayout`, `SC` 0x7f130ad2 |
| Remove Junk card | margin top/start/end 14.sdp; `layout_shape` (no fill, radius 10.sdp, 1.sdp stroke `cleaner_item_stroke` #F2F2F2 / #1E1E1E; import); padding 14.sdp | `item_remove_junk` |
| — icon | `ic_remove_junk_list` at intrinsic size, start, centred vertically | `img_transparent` |
| — title | "Remove Junk" `remove_junk`, `GalleryText.BodyLargeSemiBold` `titleColor`, marginStart 12.sdp, marginTop 5.sdp, marginEnd 2.sdp | `txt_heading` |
| — subtitle | `txt_cleaner_sub`, `GalleryText.BodySmallMedium` `item_cleaner_sub_txt` (#7D90A3), max 2 lines ellipsize end, marginStart 12.sdp, marginTop 5.sdp, marginEnd 6.sdp | `txt_heading_sub` |
| — button | "Scan now" `scan_now`, width 82.sdp, 1 line ellipsize end, `GalleryText.BodyMediumSemiBold` `onlyWhite`, `cleaner_red` #E11A1A fill, radius 25.dp (MaterialButton default insets), marginStart 18.sdp, end-aligned, centred vertically | `btn_remove` |
| Row list | vertical list under the card, one `item_system_cleaner` per row in this order: Duplicate Images · Screenshots · Large Video | `SC` onCreate string/drawable arrays |
| Row card | margin top 9.sdp, start/end 14.sdp; `layout_shape`; padding 14.sdp | `item_system_cleaner` |
| — icon | `img_system_cleaner_duplicate` / `img_system_cleaner_screenshot` / `img_system_cleaner_large_video` at intrinsic size | `img_transparent` |
| — title | "Duplicate Images" `duplicate_images` / "Screenshots" `tool_bar_screenshot` / "Large Video" `tool_bar_large_video`; `GalleryText.BodyLargeSemiBold` `ic_cleaner_expand_arrow_color` (#071427 / #FFFFFF), marginStart 12.sdp, marginTop 5.sdp | `txt_heading` |
| — subtitle | `txt_sub_duplicate` / `txt_sub_screenshot` / `txt_sub_large_video`; `GalleryText.BodySmallMedium` `item_cleaner_sub_txt`, max 2 lines ellipsize end, marginStart 12.sdp, marginTop 5.sdp | `txt_sub_text` |
| — arrow | `system_cleaner_banner_arrow` padding 5.sdp, marginStart 15.sdp, end-aligned, centred | `btn_remove` |

| Behaviour | Value | Reference |
|---|---|---|
| Card / Scan now tap | storage permission check (keep ours), then Junk scan (§2.1) | `du/a` → `SC.OoooOo0` → `JunkScanActivity` |
| Row tap | Duplicate Images → Duplicate waiting (§4.1); Screenshots → §3 (4 columns); Large Video → §3 (3 columns) | `du/a` pswitch 0/1/2 |
| Back | finish (to Explore). Opened from a notification: Home instead (Phase 9 wires it) | `SC` `s9/k.OooOo0O` |

Ours: drop the `TopAppBar`, the 16.dp rounded filled cards, the `Spacer`-based gaps and the three English row literals.

---

## 2. Junk flow — scan, remove, cleared (#26, CLN-01)

Ours keeps one route (`Screen.RemoveJunk`) or splits into three; either way the three states below are separate full-screen states and Back from any of them returns to whoever opened the scan (Explore or System Cleaner), never to an earlier state (`JS` / `RJ` / `JC` each `finish()` when they hand over).

### 2.1 Junk scan (`R272/layout/activity_junk_scan.xml`) **[device]** `ref272-junk-scan.png`

| Element | Value | Reference |
|---|---|---|
| Background | `layout_progress_background`: linear gradient 90° #FF7000 (bottom) → #FF8D01 (top) (import); full screen, padding bottom 22.sdp; status bar sits on it | `viewContainer` |
| Toolbar | height 56.dp (`?actionBarSize`), marginTop 24.sdp; nav icon `ic_back_white` (import); title "Remove Junk" `remove_junk` in `onlyWhite` (Toolbar default title style; **[device]** ≈ 18.sp, use `GalleryText.BodyXXLargeSemiBold`) | `toolBarLayout` |
| Animation | Lottie 180×180.sdp, centred in a packed vertical chain with the two texts below; **during the scan `scan_cleaner_lottie`** loop (set in code; the XML's `done_cleaner_lottie` is replaced at start) | `imgScanProgress`, `JS` line 1801 |
| Status | "Scanning..." `scanning`, `GalleryText.BodyLargeSemiBold` `onlyWhite`, centred, margin h 24.sdp | `txtScanStatus`, `dx/e` |
| Path | current scanned path, `GalleryText.BodyMediumRegular` `onlyWhite`, 1 line, ellipsize **middle**, centred, marginTop 6.sdp, margin h 24.sdp. Blank or "Scanning..." values show `/storage/emulated/0` | `txtScanningPath`, `JS.OoooOO0` |
| Done state | when progress reaches 100: path hides; status text becomes `junk_scan_completed_message` (see Q1); background becomes `layout_complete_background` (90° #EE2713 → #F74835, import); Lottie switches to `done_cleaner_lottie` played once; hold **2500 ms** (0x9c4) then open Remove junk with the stored result and close this screen | `dx/f` |
| Back | toolbar back / system Back: finish (cancels the scan) | `JS` |

Progress steps: the view model posts 20 / 40 / 60 / 80 / 100 after each of its five category passes (`JV` 0x14…0x64); only 100 is used by this screen.

### 2.2 Remove junk — results (`R272/layout/activity_remove_junk.xml`) **[device]** `ref272-remove-junk.png`

| Element | Value | Reference |
|---|---|---|
| Screen | `backgroundColor`; collapsing header + list (`CoordinatorLayout`, header scrolls away with `exitUntilCollapsed`) | `clMain` |
| Header | `layout_progress_image_view` (90° #EB1D08 → #F94D3C, bottom radii 10.sdp; import); dark-theme overlay (white toolbar text). Height animates **350.dp → 300.dp in 500 ms** when the result binds (`RJ.Ooooo00`, `ValueAnimator.ofInt(350, 300)` 0x1f4); **[device]** header bottom at 300.dp | `appBarLayout`, `layout_progress` |
| Header toolbar | 56.dp, title "Storage Clean" `storage_clean` `onlyWhite` (**[device]** no back arrow drawn on the expanded header; system Back works) | `toolBarLayout`, `dx/h` |
| Size circle | 150×150.sdp `storage_clean_bg_img` (two white circles α 0.12 / 0.15; import), marginTop 50.sdp, centred horizontally | `clJunkSizeAndMessage` |
| — size | total junk found, Inter **Bold** 18.ssp `onlyWhite`, `includeFontPadding=false`; the unit (" MB") at **0.5× relative size** (`K.OooO0Oo` `RelativeSizeSpan(0.5)`) **[device]** "23.2 MB" | `txt_size` |
| — label | "Junk Found" `junk_found`, `GalleryText.BodySmallBold` `onlyWhite`, max 2 lines | `txt_junk_found` |
| List | vertical, padding bottom 70.sdp, `clipToPadding=false`, no overscroll; rows below in this order | `rv_remove_junk`, `RJ` constructor |
| Row order | [Ads slot — collapsed] · **Junk Files** (app cache) · **Obsolete APK Files** · **Temp Files** · **Log Files** (old documents, Q6) · **Empty Folders** **[device]** | `RJ` `dataMap` keys `ads, app_cache, apk_files, temp_file, old_document, empty_folder`; display names `dy/m` 0x7f130781 / 0x7f1308b0 / 0x7f1307a0 |
| Row wrapper | margin h 10.sdp, top 12.sdp, `backgroundColor` | `item_remove_junk_main` |
| Row header | height 22.sdp: icon 18×18.sdp start; title `GalleryText.BodyLargeSemiBold` `titleColor`, 1 line ellipsize, margin h 8.sdp; expand arrow (`ic_expand_arrow` when the category has items, `ic_item_not_expand` otherwise, tinted `black`, marginTop 2.sdp, marginStart 2.sdp); size `GalleryText.BodyMediumSemiBold` `black`, marginEnd 5.sdp; check 20×20.sdp end | `item_remove_junk`, `dy/g` |
| Row icons | Junk Files `ic_cleaner_junk_file`; Obsolete APK `ic_cleaner_apk_file`; Temp `ic_cleaner_temp_file`; Log Files `ic_cleaner_log_file`; Empty Folders `ic_cleaner_folder_file` | `dy/g` 694…758 |
| Row states | scanning: `ProgressBar` 25×25.sdp `dark_blue` at the end; empty: "No Junk" `no_junk` `GalleryText.BodySmallSemiBold` `junkCleanerSubTitleColor` (#7B8999) at the end, no arrow, no size, no check; with items: arrow + size + check | `item_remove_junk_progress`, `txt_check`, `imgCheck` |
| Category check | `ic_check_new` all children checked · `ic_uncheck_new` none · `ic_radio_foreground_new` some; categories with items start **all checked** **[device]**, except Log Files, whose items start **unticked** (user decision Q6) | `dy/g` 0x7f08028c / 0x7f08045c / 0x7f0803d7 |
| Expand | tapping a row with items toggles its sub-list | `rv_remove_junk` in `item_remove_junk_main` |
| Sub row | marginTop 8.sdp: icon 20×20.sdp (app icon for APK files, else the category icon; placeholder `img_remove_junk_sun_item`); name **12.sdp** text (sdp in the reference, keep) `black`, 1 line ellipsize, margin h 8.sdp; size **12.sdp** `titleColor` marginEnd 5.sdp; check 20×20.sdp padding 2.sdp `ic_check_new` / `ic_uncheck_new_sub`, unchecked name colour `junkCleanerSubTitleColor` | `item_remove_junk_sub`, `dy/n` |
| Bottom bar | floating over the list at the bottom: height 38.sdp, margin h 19.sdp, bottom 12.sdp; two equal-weight buttons | `bottomActionLayout` |
| — Clean | "Clean" `clean`, Inter Bold 11.ssp `junkCleanerSubTitleColor`, `includeFontPadding=false`, `bg_remove_junk_clean_disabled` (#F0F1F8, radius 22.sdp; import), marginEnd 4.sdp. Always this look (the reference never restyles it) and always tappable | `btnClean`, `dx/g` case 0xb |
| — Deep Clean | "Deep Clean" `deep_clean`, Inter Bold 11.ssp `onlyWhite`, `bg_remove_junk_deep_clean` (#0B72F6 radius 22.sdp, ripple `ripple_color`; import), padding h 12.sdp, drawableStart `deep_clean_ic` tinted `onlyWhite` with 5.sdp gap (Q2), marginStart 4.sdp. Alpha 1 + enabled when the checked size > 0; **alpha 0.6 + disabled** at 0 | `btnDelete`, `RJ.OoooOO0` |
| Button hint | `remove_junk_button_animation` Lottie (38.sdp tall, padding 4.sdp, marginBottom 35.dp, loop) over the bar; hidden when the checked size is 0 | `lavRemoveJunkAnimation`, `RJ.Oooooo` (3000 ms post) |

| Behaviour | Value | Reference |
|---|---|---|
| Source | the result stored by the scan (`JunkScanResultStore`); if none (opened from a notification, Phase 9) this screen scans itself with the same rules and the header crossfades to `layout_progress_image_view_done` (90° #E11A1A → #F9637C, bottom radii 10.sdp; import) in 500 ms once progress passes 40 | `RJ` onCreate, `RJ.OoooOoo` |
| Nothing found | total 0 → buttons disabled as above, the hint Lottie never shows | `RJ` cond_a |
| Clean / Deep Clean | both start §2.3 for the checked items (Deep Clean's rewarded ad is skipped, D5) | `dx/g` 0xb, 0xc → case 5 |
| Back | finish | `RJ` |

### 2.3 Remove junk — cleaning (same screen, `clAnimation`)

| Element | Value | Reference |
|---|---|---|
| Swap | header and list hide; full-screen `clAnimation` shows | `RJ.OoooooO` |
| Animation | `cleaner_lottie` full screen, `centerCrop`, loop | `clean_lottie_view` |
| Top bar | transparent row, 56.dp tall, marginTop 24.sdp: back box 56.dp with `ic_back_white`; "Remove Junk" `remove_junk` `GalleryText.BodyXXLargeSemiBold` `onlyWhite`, 1 line, weight 1, marginStart 5.sdp | `llCleanerAnimations`, `llBack` (`dx/g` case 0xa = Back) |
| Size | the checked size, `GalleryText.BodyXXLargeBold` at **35.sdp** `onlyWhite`, marginBottom 10.sdp above the label, counted with a float animator **800 ms AccelerateDecelerate** (`ai/f` update) | `txt_scanning_size`, `RJ` 0x320 |
| Label | "Cleaning...." `cleaning_item`, `GalleryText.BodyLargeSemiBold` `onlyWhite`, centred-bottom, marginBottom 50.sdp, marginStart 5.sdp | `txt_scanning_process` |
| Done | when deletion finishes, on the next loop of `cleaner_lottie` the full-screen `junk_done` Lottie (centerCrop, no loop) shows; when it ends → Junk cleared with `total_checked_size` and this screen closes | `dx/m`, `dx/j`, `dx/g` case 0xd |

### 2.4 Junk cleared (`R272/layout/activity_junk_cleared.xml`)

| Element | Value | Reference |
|---|---|---|
| Screen / toolbar | `backgroundColor`; `SharedMediaToolbar`, title "Remove Junk" `tool_bar_remove_junk` | `JC` 0x7f130ad0 |
| Illustration | `img_junk_cleared_done` at intrinsic size, marginTop 30.sdp, centred | `img_remove_done` |
| Size | Inter Bold 18.ssp `black` (#000000 / #FFFFFF), marginTop 12.sdp, centred, locale text direction; counts **0 → cleaned size in 800 ms AccelerateDecelerate**, text = `K.OooO0o0` format (§2.7); a cleaned size of 0 shows "Finished" `finish` | `txt_remove_size`, `dx/a` |
| Caption | "Junk files Cleaned" `phone_good_condition`, `GalleryText.BodyMediumMedium` `black`, marginTop 10.sdp | `txt_condition` |
| Section | "Release more space now" `release_more_space`, `GalleryText.BodyLargeBold` `black`, marginTop 20.sdp, marginStart 20.sdp | `txt_more_space` |
| Rows | the three §1 `item_system_cleaner` rows (same order, icons, titles, subtitles), list marginBottom 10.sdp; ad slot collapsed | `rv_other_item`, `dy/d` |
| Row tap | Duplicate Images → §4.1, Screenshots → §3, Large Video → §3 | `aw/b` |
| Side effect | saves `isJunkCleanedForToday = true` (ours: `junkCleanedToday`, already read by Explore Scan now) | `JC` line 371 |
| Back | finish | `JC.Oooo0oO` |

### 2.5 Removed literals (ours)

"Scanning...", "Junk Files Found: … Scanning Completed!" (built by hand), "Cleaning...", "Optimizing storage space", "Done", "Junk Cleared", "Your phone is in optimal condition", "Release more space", "Deep Clean" cd, "Selected", the three row literals, and every `fontSize = N.sp`.

### 2.6 Junk categories and rules (`JV`, `RJ`, `S272/aw/b`)

| Row | Reference category | Rule | Ours |
|---|---|---|---|
| Junk Files | app cache | per-app cache size through `PackageManager` (`JV.OooO0Oo`) | `APP_CACHE` (keep) |
| Obsolete APK Files | APK files | files ending `.apk` (`S272/c7/f`) | `APK` (keep) |
| Temp Files | temp files | `JV` temp pass (planner reads the exact suffix list in `JV`'s temp method before coding; ours `.tmp`/`.temp`) | keep until confirmed |
| Log Files | **old documents** | extensions doc, docx, pdf, txt, csv, xls, xlsx, ppt, pptx, xml whose `lastModified` is older than **now − 1 year** (`Calendar.add(YEAR, −1)` in `dx/e`/`dx/l`) | port the reference rule; items **unticked by default** (Q6 resolved) |
| Empty Folders | empty folders | `JV` empty-folder pass; standard top-level folders excluded (keep ours) | keep |

### 2.7 Size text (`K.OooO0o0`, `K.OooO0Oo`)

1024-based units `B, kB, MB, GB, TB`; number `DecimalFormat("#,##0.#")` (one decimal at most, no trailing zero, grouping), then a space and the unit; 0 → "0 B".
Ours `formatStorageBytes` uses `%.2f GB` / `%.1f MB`: replace with this rule (one unit test: 0, 1023, 1024, 23.2 MB, 1.5 GB, 1,024.5 GB).

---

## 3. Large videos and Screenshots (`activity_large_video.xml`, `activity_screen_short.xml`, `item_large_video.xml`, `item_screenshot_main.xml` — #27, #28, CLN-01)

One composable for both (the reference shares the cell and header layouts). Differences in the last table.

| Element | Value | Reference |
|---|---|---|
| Screen | Large video: `white` (#FFFFFF / **#000000** in night); Screenshots: `backgroundColor` | `main` |
| Toolbar | `SharedMediaToolbar`, title "Large Video" `tool_bar_large_video` / "Screenshots" `tool_bar_screenshot`; the `selectAll` text ("Select all" `select_all` / "Unselect all" `unselect_all`, `GalleryText.BodyMediumMedium` `colorPrimary`, padding 5.sdp) is **always shown** when the list is not empty; while ≥ 1 item is selected the title reads "%d item(s) selected" `items_selected` | `LV`/`SS` 387, 666, 2913 |
| Grid | always in pick mode (no long-press needed): date header rows span the full width, cells fill the columns | `dt/c`, `b0/d` |
| Date header | "Today" `today` / "Yesterday" `yesterday` / `dd MMM yyyy`; **12.sp** (unscaled) Inter Regular `black`, padding 5.sdp h / 4.sdp v | `item_screenshot_main`, `dt/b` |
| Cell | square, margin 1.sdp, centre-crop thumbnail; check `ic_check` (selected) / `ic_un_check` top-end, margin 1.sdp; the `txt_file_size` pill stays hidden (`gone` in the layout, never shown) | `item_large_video`, `dt/a` |
| Cell tap | toggles the item (no viewer) | `dt/a` |
| Delete button | bottom, height 38.sdp, margin h 15.sdp, bottom 10.sdp; `ic_delete_duplicate_btn_selected_bg` (radius 25.dp, ripple) tinted `btn_delete_disable` #BABBBC while nothing is selected (disabled) and `dark_blue` #0066FF when ≥ 1 is selected; text **16.sp bold** (unscaled) `onlyWhite`, centred | `btn_video_del`, `btn_cleaner_del` |
| Label | none selected: "Delete Selected" `delete_selected`; some: "Delete" `delete` + " (" + N + ")"; all: "Delete All" `delete_all` + " (" + N + ")" | `LV.OoooOOO`, `SS` 1092…1619 |
| Confirm | `dialog_cleaner_permanently_delete` (§3.1) | `ds/a`, `a0/e` |
| Progress | full-screen progress "Deleting" `deleting` while files delete, then toast "Media files deleted successfully." `selected_media_files_successfully_deleted`; deleted items leave the grid | `LV` 2674, 1976 |
| Empty | centred: `ic_empty_img` + text marginTop 20.sdp / bottom 10.sdp, margin h 53.sdp, `GalleryText.BodyLargeSemiBold` `patternColor` (#1E3354 / #FFFFFF), centred | `emptyLayout` |
| Loading | centred `colorPrimary` spinner until the first query returns (never the empty text first) | Phase 5 §3.3 rule |

| Screen | Columns | Items | Empty text | Confirm copy |
|---|---|---|---|---|
| Large Video | 3 | videos whose file length is **≥ 73 400 320 bytes (70 MiB)** (`LargeVideoViewModel` 0x4600000, `if-ltz` skips smaller), grouped by day, newest first | "No large videos found. Your storage is clean and optimized.👍" `cleaner_video_empty_layout_text` | `permanent_delete_video_title` / `permanent_delete_video_sub_title` |
| Screenshots | 4 | images whose path contains `Pictures/Screenshot`, `Pictures/Screenshots`, `DCIM/Screenshot` or `DCIM/Screenshots` (`ScreenshotsViewModel` keys `PICTURES_SCREENSHOT(S)`, `DCIM_SCREENSHOT(S)`), grouped by day | "No screenshots found. Your storage is clean and optimized.👍" `cleaner_screenshots_empty_layout_text` | `permanent_delete_screenshot_title` / `permanent_delete_screenshot_sub_title` |

### 3.1 Cleaner delete dialog (`R272/layout/dialog_cleaner_permanently_delete.xml`, `CD`)

`CommonDialog` card `dialogBack`, radius 14.sdp, `AnimatedDialog` window animation (Phase 3 dialogs); column padding 20.sdp h / 10.sdp v, start-aligned:
title `GalleryText.BodyXLargeBold` `ic_cleaner_expand_arrow_color`, marginTop 5.sdp, line spacing +2.sdp; subtitle `GalleryText.BodyMediumMedium` `cleaner_delete_sub_text` (#616161 / #FFFFFF), marginTop 10.sdp, +2.sdp;
"Permanently delete" `permanently_delete` 40.sdp tall, `round_button` (radius 25.dp, import) tinted `cleaner_red`, `GalleryText.BodyLargeSemiBold` `onlyWhite`, marginTop 18.sdp;
"Cancel" `cancel` 40.sdp tall, `permanent_btn_border` (1.dp #969696 pill outline; import), `GalleryText.BodyLargeSemiBold` `item_cleaner_sub_txt`, marginTop 7.sdp. Files are deleted permanently (no Recycle Bin), as the copy says.

The video usage-access prompt (`dialog_usage_access_video`) is **not** triggered here in 2.7.2; see Q5.

---

## 4. Duplicate flow (#29, CLN-02)

### 4.1 Waiting → scan done (`R272/layout/activity_duplicate_waiting.xml`) **[device]** `ref272-duplicate-waiting.png`

| Element | Value | Reference |
|---|---|---|
| Screen | `color_success_lottie_blue` #0066FF (add) full screen, padding 14.sdp; status bar stays light | `viewContainer` |
| Scanning block | centred: white large circular spinner (48.dp, `progressBarStyleLarge`); "Scanning..." `scanning` `GalleryText.BodyLargeSemiBold` `onlyWhite` centred marginTop 18.sdp; "This may take a while on large libraries." `dup_media_wait_hint` `GalleryText.BodyMediumRegular` `onlyWhite` centred marginTop 8.sdp | `clScanning` |
| Done block | when the duplicate service finishes: the scanning block hides, `success` Lottie 150×150.sdp plays **once**, "Scanning Completed!" `scan_complete` `GalleryText.BodyLargeSemiBold` `onlyWhite` below it (margin h 10.sdp); when the Lottie ends → Duplicate photos (§4.2) and this screen closes | `clDoneAnimation`, `DW.OoooOo0`, `ak/h` |
| Back | finish (the scan keeps running in its service; ours: cancel or keep, as today) | `DW` |

Ours: replace `delay(2200)` with "go on when `success` ends"; drop the two literals. `DuplicateScanningCompleteActivity` is **not built** (Q4).

### 4.2 Duplicate photos (`R272/layout/activity_duplicate_photo.xml`, `item_duplicate_main`, `item_duplicate_sub`) **[device]** `ref272-duplicate-photo.png`, `ref272-duplicate-keep-one.png`

| Element | Value | Reference |
|---|---|---|
| Toolbar | `SharedMediaToolbar`, title "Duplicate Images" `duplicate_images` | `DP` 5346 |
| Keep row | under the toolbar, marginTop 5.sdp, padding h 10.sdp: "Keep One Photo" `keep_best_photo` `GalleryText.BodyMediumMedium` `titleColor` start; Material 3 `Switch` end (SwitchCompat look: thumb `colorPrimary` when on **[device]**). **Off on open** **[device]** | `keepPhotoLayout`, `duplicateSelectAllSwitch` |
| Ad row | collapsed (D5) | native |
| Group header | full-width row: "Group" `group` + " " + N + " (" + count + " " + "Images" `images` + ")" → "Group 1 (2 Images)"; N is **1-based** **[device]**; `GalleryText.BodySmallMedium` `black`, padding 5.sdp h / 4.sdp v | `item_duplicate_main`, `DP` 2179…2300 |
| Grid | **4 columns** (`GridLayoutManager(…, 4)`), list padding bottom 50.sdp, `clipToPadding=false`, fast-scroll thumb on the end edge (Phase 3 `FastScrollThumb`), item entrance as Phase 5 §3.2 (`layout_animation`) | `duplicateRecycler`, `DP` 5405 |
| Cell | square card radius 3.sdp, margin 1.sdp, no elevation; thumbnail fills; check top-end margin 1.sdp `ic_check` / `ic_un_check`; bottom caption = the item's **folder name**, `GalleryText.BodyXSmallMedium` `onlyWhite`, 1 line ellipsize, centred, padding 10.sdp h, column padding top 6.sdp / bottom 3.sdp, shadow #FF000000 (2, 2) r 4 | `item_duplicate_sub`, `bj/b` |
| Cell tap | toggles the item | `bj/b` |
| Keep One Photo on | selects every photo **except the first of each group** **[device]**; off clears the selection | `DP` lines 322-660 |
| Delete button | shown only while ≥ 1 is selected: bottom, height 40.sdp, margin h 15.sdp, bottom 10.sdp, `ic_delete_duplicate_btn_selected_bg` (`dark_blue`, radius 25.dp); centred row: "Delete Selected" `delete_selected` + " (" + N + " " + "items" `items` + ")" `GalleryText.BodyXLargeSemiBold` `onlyWhite`, then `ic_ads_left` marginStart 8.dp (Q2) **[device]** "Delete Selected (19 items)" | `delBtnLayout`, `DP` 546-651 |
| Delete tap | none selected → toast "Please select media." `please_select_items`; else the Phase 3 `DeleteMediaDialog` (`dialog_delete`) with title "Are you sure you want to delete" `deleted_title` + " " + N + " " + "items?" `deleted_items`, "Move to Recycle Bin" checkbox as Phase 3; Yes → §4.3 | `DP` 1284-1406 (`oa/j`) |
| Scanning header | only when this screen opens while the scan still runs (notification entry, Phase 9): 75.sdp block marginTop 10.sdp — "Scanning Photos...." `scanning_photos` `GalleryText.BodySmallRegular` start; processed count `GalleryText.BodyLargeSemiBold` bold end; determinate bar margin h 10.sdp / v 5.sdp, track `seekbar_color_bg`, progress `colorPrimary`; "Scanning time depends on number of photos" `scanning_time` `GalleryText.BodyXSmallRegular` centred; plus centred `duplicate_scanner_lottie` 140.sdp (fitXY, loop) and "Kindly wait as we are scanning for duplicate images. This won’t take long.." `lottie_text` `GalleryText.BodySmallMedium` `titleColor` 10.sdp below, margin h 20.sdp, until the first group arrives | `duplicateScanningLayout`, `rl_scanner_lottie_view` |
| Empty | centred `ic_empty_img`; "No duplicate photos found. Your storage is clean and optimized.👍" `cleaner_duplicate_empty_layout_text` `GalleryText.BodyLargeSemiBold` `titleColor` margin h 53.sdp, top 20.sdp, bottom 10.sdp; "No duplicate files here." `no_duplicate_files` `GalleryText.BodySmallRegular` `titleColor` margin h 50.sdp | `emptyLayout` |
| Loading | full-screen `colorPrimary` spinner layer (elevation 10.sdp) while a file action runs | `progressBar` |
| Grouping | reference fingerprint (`DS`: 32×32 system thumbnail → 8×8 → PNG → MD5); same fingerprint = same group; groups of ≥ 2 only. **Keep ours** (`DuplicateFinder`) | `DS` |

Ours: drop "BEST", "Group X (Y images)", "Keep best photo", "Select All"/"Unselect All", "Selected"/"Unselected" cds and the `permanent_delete_duplicate_*` keys (not in 2.7.2), plus every `.sp`.

### 4.3 Delete process (`R272/layout/activity_duplicate_delete_running.xml`) — new state

`color_success_lottie_blue` full screen, padding 14.sdp; centred white large spinner; "Deleting" `deleting` `GalleryText.BodyLargeSemiBold` `onlyWhite` marginTop 18.sdp; `dup_media_wait_hint` `GalleryText.BodyMediumRegular` `onlyWhite` marginTop 8.sdp.
Shown **at least 3000 ms** (`bi/d`: waits `3000 − elapsed`), then Delete complete (§4.4) replaces it. **Back is blocked** (`l1/a` with no callback). An empty pending list closes at once (`DR` onCreate).

### 4.4 Delete complete (`R272/layout/activity_duplicate_delete_complete.xml`) — HANDOFF Phase 6 deferral

| Element | Value | Reference |
|---|---|---|
| Toolbar | `SharedMediaToolbar`, title "Cleanup complete" `dup_delete_complete_title`; end icon `ic_home` (import) in the `settingBtn` slot (padding 3.sdp, borderless ripple) | `DC` 487, 542 |
| Body | vertical scroll, padding 14.sdp, bottom 20.sdp | — |
| Animation | `dm_success` 100×100.sdp, marginTop 5.sdp, centred, autoplay **loop** | `lottieAnimation` |
| Title | "Task completed" `task_completed` `GalleryText.BodyLargeSemiBold` `titleColor` centred marginTop 18.sdp | `tvCleanupAllDone` |
| Subtitle | "Your device is ready to use" `your_device_is_ready_to_use` `GalleryText.BodyMediumRegular` `subtitleColor` #777777 (add) centred marginTop 7.sdp | `tvCleanupSubtitle` |
| Card | marginTop 15.sdp, `backgroundColor`, radius 15.sdp, 1.dp stroke `seekbar_color_bg`; padding 14.sdp | `cardCleanupSummary` |
| — count | plural `dup_delete_files_removed` ("%d file removed" / "%d files removed") with the deleted count, `GalleryText.BodyMediumSemiBold` `titleColor` | `tvCleanupFilesRemoved`, `DC` `getQuantityString(0x7f110000)` |
| — freed row | marginTop 12.sdp, `bg_dup_storage_feed_card` (`white` fill, 1.5.dp `colorPrimary` stroke, radius 8.sdp, inner padding 12.dp; import) padding 14.sdp h / 12.sdp v: `img_system_cleaner_duplicate` 35×35.sdp; text column marginStart 12.sdp — "Storage freed" `dup_delete_storage_freed` `GalleryText.BodyMediumSemiBold` `titleColor`; `dup_delete_space_freed` ("%1$s freed") with §2.7 size, `GalleryText.BodyMediumSemiBold` `colorPrimary`, marginTop 5.sdp | `rowCleanupSpaceFreed` |
| Home / Back | the home icon performs Back; Back finishes (to the screen that opened the flow) | `bi/a`, `DC.OoooOO0` |
| Ad | `fl_ad` collapsed | — |

Ours: replace `dup_delete_file_removed` / `dup_delete_files_removed` strings ("duplicate file(s) removed", not reference) with the reference **plural** in a new `values/plurals.xml` (English-only in 2.7.2, so no translations; `sync-strings` does not handle plurals), the "N freed" literal with `dup_delete_space_freed`, and every `.sp` with `GalleryText` styles.

---

## 5. Recycle Bin (`R272/layout/activity_recycle.xml`, `item_grid_recycle_media.xml` CHANGED — #30, CLN-03) **[device]** `ref272-recycle-empty.png`

| Element | Value | Reference |
|---|---|---|
| Toolbar | `SharedMediaToolbar`, title "Recycle Bin" `recycle_bin`; end `deleteBtn` `ic_delete` (padding 2.sdp, borderless ripple) = **empty all**, shown while the list has items and nothing is selected; selection mode: Close, "%d item(s) selected", "Select all"/"Unselect all" | `RB` 2722-3250, 4409 |
| Auto-delete card | below the toolbar, margin h 10.sdp, v 8.sdp; `recycle_switch_background_color` (#E8F2FF / #1E1E1E, add), radius 8.sdp, no elevation; row: "Recycle bin items will be removed after 30 days." `remove_items` `GalleryText.BodySmallRegular` `ic_lock_color` (#0066FF / #FFFFFF, add), padding v 2.sdp, marginStart 10.sdp, marginEnd 4.sdp, weight 1; Material 3 `Switch` height 30.sdp marginEnd 2.sdp **[device]** | `autoDeleteLayout`, `deleteAutoSwitch` |
| Switch | saves `isAutoDelete`; **default off** (Q3) **[device]** | `RB` 1858, 11414 |
| Grid | **3 columns**, padding 2.sdp h / bottom 4.sdp, `clipToPadding=false`, fast-scroll thumb, entrance animation (Phase 5 §3.2); no date headers; long-press then drag selects a range (`SelectAndReverse`) | `RB` 11377, 11813 |
| Cell (CHANGED) | square card radius 3.sdp, margin 1.sdp, transparent; centre-crop thumbnail, placeholder `image_loading_square` (import) | `thumbRoot`, `thumbnail` |
| — video row | bottom, margin 3.sdp start/end/bottom: `ic_media_video` + duration `%02d:%02d` (`00:00` default) `GalleryText.BodySmallMedium` `onlyWhite` shadow #FF000000 (2, 2) r 4, padding 3.sdp h, weight 1; `ic_common_fav` end when a favourite | `playBtn`, `duration`, `ivFavImg` |
| — days | full-width strip at the bottom, `blackTran` #90000000 (add), padding v 5.sdp, `GalleryText.BodyXSmallMedium` `onlyWhite` centred: **N + " " + "Days"** `days` — shown **only while auto-delete is on** | `dayCount`, `da/e` 720-830 |
| — file type | "GIF" / "RAW" / "SVG" label top-start, margin 6.sdp, 12.sp Inter Medium `onlyWhite`, shadow r 4 (reuse `fileTypeLabel`) | `file_type` |
| — selection | `ic_check` / `ic_un_check` top-end margin 1.sdp, only in selection mode | `selected` |
| Days formula | `daysLeft = 30 − floor((now − deletedAt) / 86 400 000)`, where `deletedAt` = the time the item went to the bin (the reference stamps it as the file's `lastModified`); no lower clamp | `cz/k` 270-287, `db/a` 1175-1197 |
| Cell tap | Phase 5 viewer in recycle mode (Restore · Delete dock) with this list | `RB` 5944 |
| Selection bar | `MediaSelectionBar` with two slots: **Restore** (`ic_restore` + "Restore" `restore`, in the Share slot, width 50.sdp) · **Delete** (`ic_album_select_delete` + "Delete" `delete`, 50.sdp) | `RB` 9106-9137, layout_select_media_bottom |
| Restore | Phase 5 `RESTORE` dialog ("Restore items" `restore_items` / `restore_item_permission`) → progress "Restoring" `restoring` → toast "Media files restored successfully." `selected_media_files_successfully_restored` | `RB` 2318-2331, 8626, 4293 |
| Delete / Empty all | Phase 5 `RECYCLE_DELETE` dialog ("Delete items" `delete_items` / `delete_items_description`, button "Delete" `delete`) → progress "Deleting" → toast `selected_media_files_successfully_deleted`; empty all applies it to every item | `RB` 2132-2166, 4607-4649 |
| Empty | `ic_empty_img`; "No items" `no_items` `GalleryText.BodyLargeSemiBold` `titleColor` marginTop 20.sdp / bottom 10.sdp; "No deleted files here. When you remove items from your phone, they'll appear here," `deleting_files` (trailing comma is the reference) `GalleryText.BodySmallRegular` `titleColor` centred, margin h 50.sdp **[device]** | `emptyLayout` |
| Loading | full-screen `colorPrimary` spinner layer (elevation 10.sdp) during restore/delete | `progressBar` |
| Back | clears the selection first; else records the RECYCLE usage-access trigger (no-op hook `recordRecycleUsageAccessTrigger()` with a `shortcut:` comment naming Phase 12, as `StoryViewScreen`) and finishes | `RB.Oooo0oO` 6910-6919 |
| Auto purge | with auto-delete on, items whose days reach ≤ 0 are removed (reference `db/a` reads `isAutoDelete` before purging); with it off nothing is purged | `db/a` 309 |

What 2.7.2 changed in the cell (vs 2.6.7 `aapt2` tree): the root and thumbnail gained ids (`thumbRoot`, `thumbnail`) the code binds, the placeholder is `image_loading_square`, and `file_type` lost its `sans-serif-medium` family (Inter now). Layout values otherwise unchanged.

Ours: drop "N days left", the `maxOf(1, …)` clamp, the own "Empty Recycle Bin" dialog and toast, the literal dialog/button texts and `TopAppBar`.

---

## 6. Quick Organize and Organizer Recycle (#31, CLN-04)

### 6.1 Organizer screen (`R272/layout/activity_organizer.xml`) **[device]** `ref272-organizer-intro-1.png`

| Element | Value | Reference |
|---|---|---|
| Screen | `white` (#FFFFFF / #000000) | `main` |
| Toolbar | Material toolbar, no content inset; nav `ic_back` tinted `titleColor`; title row: "Unorganized" `unorganized` `GalleryText.BodyLargeSemiBold` `titleColor` + `ic_down_arrow` 24×24.dp (import, cd "Dropdown" literal → `translatable="false"`); menu end: Undo `ic_undo_unchecked` / `ic_undo_checked` when an undo exists (import both), Delete `ic_delete`; both items' title is "Undo" `undo` in the reference (TalkBack) | `mToolBar`, `menu_organized` |
| Photo pager | horizontal pager under the toolbar, one photo per page (`item_gallery_image`: image with padding 10.sdp, fit) | `mViewPagerImageList` |
| Album row | horizontal list under the pager, marginTop 6.sdp, elevation 12.dp, no clipping; first item "Create Album", then the albums (defaults "Photos" `org_album_name_photo`, "Family" `org_album_name_family` when the user has none) **[device]** | `mRvAlbumList`, `OA.o00Oo0` |
| Album item | padding 8.dp: 48×48.dp image, corner 8.dp (`OrganizerRoundedImageViewStyle`), centre-crop, placeholder `ic_album_placeholder` (import), Create Album uses `organizer_create_album` (import); name 12.sp (unscaled) Inter Medium `black`, max 2 lines ellipsize, centred, paddingTop 5.sdp, marginStart 2.sdp; first-time `hand_click` Lottie 60.dp over it | `item_album`, `cq/*` |
| Jump grid | title-row tap toggles the all-photos grid: info strip (`shimmer_color` #F2F2F2 / #262424, padding 5.dp v, start 10.dp): `ic_information` + "You can select a photo to jump to" `you_can_select_a_photo_to_jump_to` `GalleryText.BodyXSmallSemiBold` `black` marginStart 10.dp; grid `white` marginTop 10.dp, columns from `mediaColCount`; tapping a photo jumps the pager to it | `mLLAllImageContainer`, `OA` "mediaColCount" |
| Empty | `ic_empty_img`; "No items" `no_items`; "No media files here." `no_media_file` (Phase 3 empty block) | `emptyLayout` |
| Loading | `colorPrimary` spinner, marginBottom 8.dp | `mProgressBar` |

| Behaviour | Value | Reference |
|---|---|---|
| Swipe left | skip to the next photo | intro step 3 copy, `OA` |
| Swipe up | move the photo to the organizer bin (`deleteCount` + 1) | intro step 4 copy, `OA` "deleteCount" |
| Album tap | move the photo into that album, next photo; same-folder → toast `media_files_should_not_move_to_same_folder` | `OA` 442 |
| Create Album | Phase 3 `CreateAlbumDialog`; success toast "Folder created successfully!" and failure "Folder already exists or failed to create!" are **English literals in the reference** (keep as `translatable="false"`) | `OA` 2080, 2154 |
| Undo | reverts the last action; nothing to undo → toast "No actions to undo" (literal, `translatable="false"`) | `OA` strings |
| Delete menu | opens Organizer Recycle (§6.4) | `menu_delete` |
| Back | when the intro is finished (`organizerStepFlow` = 7) and `deleteCount` > 0: `dialog_organizer_permanently_delete` (§6.5); Permanently delete empties the organizer bin then closes, Cancel closes; otherwise close | `OA.Oooo0oO` 3127-3274 |

### 6.2 Intro steps (`R272/layout/organize_intro_step_layout.xml` CHANGED) **[device]** `ref272-organizer-intro-1.png`, `-2.png`

Overlay `#CC000000` over the screen (album row stays above it, elevation 12.dp). Steps are driven by the `organizerStepFlow` preference (1 → 7; 7 = done) and the `isOrgSkipToolTip` flag:

| Step | Shows | Values |
|---|---|---|
| 1 | `clFirst` (now hidden by default, focusable + clickable so it eats touches) | hearts `ic_heart_left_org` start and `ic_heart_top_org` top-end (import); close `ic_close_new_org` (import) top-end, padding top 6.sdp / bottom 8.sdp / start 6.sdp, marginBottom 6.sdp; **`quick_oraganize`** Lottie 250×330.sdp loop (2.6.7 used the removed `organizer_guide_lottie`); "ORGANIZE NOW" `organize_now` **all caps**, 14.ssp, weight **700**, `whiteOnlyColor`, `bg_round_with_border` (`colorPrimary`, 1.dp `whiteOnlyColor` stroke, radius 100.sdp; import), padding v 12.sdp, margin h 30.dp, v 24.sdp **[device]** |
| 2 | `clTwo`: "Tap to Create Album" | `str_step_1` 16.ssp weight 600 `whiteOnlyColor` centred, marginBottom 6.sdp above a bottom card `bg_org_intro_step_2` (`dialogBack`, top radii 12.sdp; import) padding 12.sdp with `organizer_create_album` + "Create Album" `org_create_album` `titleColor` and `hand_click` 45.dp over it **[device]** |
| 3 | `txtAlbumListTitle`: "Tap to add photos in the album" | `str_step_2` 14.ssp `whiteOnlyColor` centred, marginBottom 12.sdp above the album row |
| 4 | `clThree`: "Swipe left to skip" | `hand_left_arrow` 100.dp Lottie + `str_step_3` 16.ssp `whiteOnlyColor` |
| 5 | `clFour`: "Swipe up to move to\nrecycle bin" | **`remove_bg_tooltip`** 45.dp Lottie (2.6.7 used the removed `org_album_touch`), margin v 5.sdp; 35.dp `round_button` circle with `ic_delete` tinted `white`, marginTop 12.dp, marginEnd 12.dp (points at the toolbar Delete); `hand_upside_arrow` 100.dp; `str_step_4` 16.ssp `whiteOnlyColor` |

Exact step→value transitions (which user action advances 1→7, when `isOrgSkipToolTip` is set, what the step-1 close does) are read from every `organizerStepFlow` site in `OA` (lines 287, 536, 2105-2135, 3139, 7490) before building (UI Considerations ⚠).

### 6.3 What 2.7.2 changed in the stripe (`layout_organizer_stripe.xml` CHANGED)

| Element | Value |
|---|---|
| Stripe | `organizer_stripe_color` #358F38 (add), padding 12.dp h / 4.dp v, full width |
| Icon | `ic_organizer_white` (import) start |
| Text | "Organize your photos quickly" `organize_your_photos_quickly` `GalleryText.BodySmallSemiBold` `whiteOnlyColor`, **1 line, ellipsize end** (2.7.2), marginStart 12.dp, weight 1 |
| Close | `ic_close_stripe` end, clickable |

### 6.4 Organizer Recycle (`R272/layout/activity_organizer_recycle.xml`)

Same frame as §5 without the auto-delete card: toolbar "Recycle Bin" `recycle_bin` (selection mode as §5), 3-column grid of the §5 cell over the organizer bin, selection bar Restore · Delete,
Restore → `restore_items` dialog → "Restoring" → `selected_media_files_successfully_restored`; Delete → `dialog_organizer_permanently_delete` (§6.5) → "Deleting" → `selected_media_files_successfully_deleted`;
empty block "No items" + `deleting_files`; tap opens the viewer in recycle mode. The day strip follows `isAutoDelete` as in §5 (`OR` 8636).

### 6.5 Organizer delete dialogs (`dialog_organizer_permanently_delete.xml`, `dialog_delete_org_bin.xml`)

Card `dialogBack` radius 14.sdp, column padding 18.sdp h / 15.sdp v:
title "Delete item?" `organizer_dialog_title` `GalleryText.BodyXLargeBold` `titleColor` marginTop 5.sdp (+2.sdp line spacing); body `organizer_deleting_select_item` `GalleryText.BodyMediumRegular` `titleColor` marginTop 10.sdp;
"Permanently delete" `permanently_delete` 42.sdp tall, `bg_rounded_red` (`red` #E11A1A, radius 50.sdp; import), `GalleryText.BodyLargeSemiBold` `white`, margin v 5.sdp;
"Cancel" `cancel` 42.sdp tall, `bg_rounded_with_border` (#FFFFFF fill, 1.dp #969696 stroke, radius 50.dp; import), `GalleryText.BodyLargeMedium` #969696.
(`dialog_delete_org_bin` — literal English title "Do you want to delete photos in \nrecycle bin?" + Delete/Cancel — has no caller found in `OA`/`OR`; not built unless the planner finds one.)

### 6.6 Media-list stripe (`activity_media_list` `organizerStripeStub`, `ML` 14036-14133)

Shown at the top of the media list **only for the "All Photos" list** (`isFrom == ALL_IMAGE` = 2) while `isOrganizerStripeShown` is false.
Close icon → set `isOrganizerStripeShown = true`, hide (`be/a`). Stripe tap → set the flag, close the list with result 777 (0x309); the Albums fragment (`ca/at`) posts an event — the planner confirms it opens Quick Organize (UI Considerations ⚠).
Ours: add the stub to `album/AlbumDetailScreen.kt` above the `SharedMediaList` grid for `filter:all_photos`, plus the preference key.

---

## Spacing Scale

Phase 1 scale applies (sdp units: 2, 4, 8, 12, 16, 24, 32, 48). Values used here, all exact reference values:
2.sdp (list padding, sub-check padding, row margins), 4.sdp (button gap, Lottie padding), 8.sdp (sub-row gap, row text margins, radius of the freed card), 12.sdp (card text start, header row top, freed row v padding, intro step-2 padding), 24.sdp (scan text margins, scan toolbar top, intro button v), 48.dp (organizer album image), 32/16 not used.

Exceptions (parity overrides multiples of 4, per Phase 1): 1, 3, 5, 6, 7, 9, 10, 14, 15, 18, 19, 20, 22, 25, 30, 35, 38, 40, 42, 50, 53, 60, 70, 75, 82, 100, 140, 150, 180, 250, 330 sdp;
unscaled 0.5.dp, 1.dp, 1.5.dp, 2.dp, 4.dp, 5.dp, 8.dp, 10.dp, 12.dp, 24.dp, 25.dp, 30.dp, 35.dp, 45.dp, 50.dp, 56.dp, 60.dp, 100.dp, 300.dp, 350.dp. Never round them.

---

## Typography

All Inter, `includeFontPadding = false`, natural line height (Phase 1 rule).

| Role | Size | Weight | Line Height | Where |
|------|------|--------|-------------|-------|
| Body | 10ssp | 400 Regular | natural (~1.21) | auto-delete text, empty bodies, scanning text (`BodySmallRegular`) |
| Label | 10ssp | 500 Medium | natural (~1.21) | cleaner subtitles, duplicate group headers, recycle durations (`BodySmallMedium`) |
| Heading | 13ssp | 600 SemiBold | natural (~1.21) | toolbar titles, row titles, status texts, empty headings (`BodyLargeSemiBold`) |
| Display | 18ssp | 700 Bold | natural (~1.21) | junk size (header, cleared) |

Exception (parity, as in Phases 1, 3-5): 8ssp/400 (`BodyXSmallRegular`), 8ssp/500 (`BodyXSmallMedium` days strip, captions), 8ssp/600 (`BodyXSmallSemiBold` jump hint), 10ssp/600 (`BodySmallSemiBold` No Junk, stripe), 10ssp/700 (`BodySmallBold` Junk Found),
11ssp/400 (`BodyMediumRegular` path, hints), 11ssp/500 (`BodyMediumMedium` Keep One Photo, Select all, caption), 11ssp/600 (`BodyMediumSemiBold` sizes, Scan now, freed card), 11ssp/700 (Clean / Deep Clean), 13ssp/500 (`BodyLargeMedium` org Cancel), 13ssp/700 (`BodyLargeBold` Release more space),
14ssp/600 (`BodyXLargeSemiBold` Delete Selected), 14ssp/700 (`BodyXLargeBold` dialog titles), 14ssp/700 caps (ORGANIZE NOW), 14ssp/400 (step 2 text), 16ssp/600 (`BodyXXLargeSemiBold` cleaning title, step texts), 16ssp/700 (`BodyXXLargeBold` at **35.sdp** cleaning size),
12.sdp/400 (junk sub rows, sdp in the reference), unscaled 12.sp/400 (date headers), 12.sp/500 (album names, file type), 16.sp/700 (large video / screenshot delete button). Weights in use: 400, 500, 600, 700. The section tables are binding.

---

## Color

Phase 1 roles, applied here:

| Role | Value | Usage |
|------|-------|-------|
| Dominant (60%) | `backgroundColor` #FFFFFF / #0C0C0C; `white` #FFFFFF / #000000 (large video, organizer) | every list screen |
| Secondary (30%) | `layout_shape` stroke `cleaner_item_stroke` #F2F2F2 / #1E1E1E; `dialogBack` #FFFFFF / #1E1E1E; `recycle_switch_background_color` #E8F2FF / #1E1E1E; `shimmer_color` #F2F2F2 / #262424 | cleaner cards, dialogs, auto-delete card, jump strip |
| Accent (10%) | `colorPrimary` / `dark_blue` / `color_success_lottie_blue` #0066FF | reserved list below |
| Destructive | `cleaner_red` / `red` #E11A1A | Scan now, Permanently delete buttons only |

Accent reserved for: the duplicate waiting / delete-process full-screen fill, the Delete Selected / Delete (N) buttons when enabled, Deep Clean (#0B72F6), loading spinners, the junk category check marks, "Select all"/"Unselect all", the Keep One Photo switch, the freed size and its card stroke, the auto-delete text (`ic_lock_color` light), ORGANIZE NOW.
Not for titles, row icons or the cleaner cards.

Fixed non-theme colours from the reference: junk gradients #FF7000→#FF8D01 (scan), #EE2713→#F74835 (scan done), #EB1D08→#F94D3C (header), #E11A1A→#F9637C (header done); #F0F1F8 (Clean), #0B72F6 (Deep Clean), `item_cleaner_sub_txt` #7D90A3, `junkCleanerSubTitleColor` #7B8999,
`ic_cleaner_expand_arrow_color` #071427 / #FFFFFF, `cleaner_delete_sub_text` #616161 / #FFFFFF, `btn_delete_disable` #BABBBC, `patternColor` #1E3354 / #FFFFFF, `blackTran` #90000000, `subtitleColor` #777777, `organizer_stripe_color` #358F38, intro overlay #CC000000, #969696 (outline buttons), `seekbar_color_bg` #BDD7FE / #5B5D5F.

**Colours to add** (missing in `values/colors.xml` / `values-night`): `color_success_lottie_blue`, `blackTran`, `organizer_stripe_color` (check with `python -I scripts/ref_res.py check-colors`). Already present: `cleaner_red`, `item_cleaner_sub_txt`, `ic_cleaner_expand_arrow_color`, `cleaner_item_stroke`, `junkCleanerSubTitleColor`, `dark_blue`, `btn_delete_disable`, `patternColor`, `cleaner_delete_sub_text`, `subtitleColor`, `seekbar_color_bg`, `recycle_switch_background_color`, `ic_lock_color`, `whiteOnlyColor`, `shimmer_color`, `red`, `ripple_color`.

**Drawables to add** (missing in ours, copy from `R272/drawable/`): `layout_shape`, `ic_back_white`, `layout_progress_background`, `layout_complete_background`, `layout_progress_image_view`, `layout_progress_image_view_done`, `ic_item_not_expand`, `ic_check_new`, `ic_uncheck_new`, `ic_uncheck_new_sub`, `ic_radio_foreground_new`, `img_remove_junk_sun_item`, `round_button`, `permanent_btn_border`, `ic_ads_left` (Q2), `ic_home`, `image_loading_square`, `organizer_create_album`, `ic_album_placeholder`, `ic_undo_checked`, `ic_undo_unchecked`, `ic_down_arrow`, `ic_information`, `ic_organizer_white`, `ic_heart_left_org`, `ic_heart_top_org`, `ic_close_new_org`, `bg_round_with_border`, `bg_org_intro_step_2`, `bg_rounded_with_border`.
Already present: `ic_remove_junk_list`, `img_system_cleaner_*`, `system_cleaner_banner_arrow`, `storage_clean_bg_img`, `bg_remove_junk_clean_disabled`, `bg_remove_junk_deep_clean`, `deep_clean_ic`, `ic_expand_arrow`, `ic_cleaner_*_file`, `ic_remove_junk_unfill`, `img_junk_cleared_done`, `ic_delete_duplicate_btn_selected_bg`, `large_video_duration`, `ic_check`, `ic_un_check`, `ic_empty_img`, `bg_dup_storage_feed_card`, `ic_media_video`, `ic_common_fav`, `ic_restore`, `ic_delete`, `ic_close`, `ic_close_stripe`, `bg_rounded_red`, `select_album_bg`, `ic_album_select_delete`.

---

## Copywriting Contract

Every visible string is a reference key; translations come from the Phase 1 sync (`python -I scripts/ref_res.py sync-strings`, then `check-strings`). Keys 2.7.2 leaves in English stay English:
`org_album_name_photo` (English-only in 2.7.2), the plural `dup_delete_files_removed` (no translations in 2.7.2), and the code literals "Folder created successfully!", "Folder already exists or failed to create!", "No actions to undo", "Dropdown" (store as `translatable="false"`).
No hard-coded English remains in the Phase 6 files (§2.5, §4.2, §4.4, §5 lists, plus `QuickOrganizerScreen` "Unorganized", "All caught up!", "Your library has been completely organized.", "Trash", "Keep", and `OrganizerRecycleScreen` "Organizer Bin", "Empty Bin", "Organizer bin is empty", "Photos you discard…", "Choose an action for this photo.", "Restore", "Delete Permanently").

| Element | Copy (reference key) |
|---------|------|
| Primary CTA | "Scan now" (`scan_now`, System Cleaner); "Deep Clean" (`deep_clean`) / "Clean" (`clean`), Remove junk; "Delete Selected (N items)" (`delete_selected` + `items`), Duplicates; "ORGANIZE NOW" (`organize_now`), Quick Organize |
| Empty state heading | "No items" (`no_items`) — Recycle Bin, Organizer Recycle, Quick Organize; "No duplicate photos found. Your storage is clean and optimized.👍" (`cleaner_duplicate_empty_layout_text`); "No large videos found. Your storage is clean and optimized.👍" (`cleaner_video_empty_layout_text`); "No screenshots found. Your storage is clean and optimized.👍" (`cleaner_screenshots_empty_layout_text`); "No Junk" (`no_junk`) per junk row |
| Empty state body | "No deleted files here. When you remove items from your phone, they'll appear here," (`deleting_files`); "No duplicate files here." (`no_duplicate_files`); "No media files here." (`no_media_file`) |
| Error state | "Please select media." (`please_select_items`, duplicate delete with nothing picked); "Media files should not be moved to the same folder." (`media_files_should_not_move_to_same_folder`, organizer); "Wrong folder selected, please select the main folder of your SD card" (`wrong_root_selected`, SD-card grant failure on delete/restore/move); "Folder already exists or failed to create!" (literal, organizer create) |
| Destructive confirmation | Large videos: "Want to delete the videos?" / "Once deleted, the videos can’t be restored." (`permanent_delete_video_title` / `_sub_title`), "Permanently delete" / "Cancel"; Screenshots: "Want to delete the screenshots?" / "Once deleted, the screenshots can’t be restored." (`permanent_delete_screenshot_title` / `_sub_title`); Duplicates: `dialog_delete` — "Are you sure you want to delete N items?" (`deleted_title` + N + `deleted_items`) + "Move to Recycle Bin", No / Yes; Recycle Bin delete and empty all: "Delete items" / "All items will be permanently deleted from your device and cannot be recovered." (`delete_items` / `delete_items_description`), Cancel / Delete; Recycle restore: "Restore items" / "Are you sure you want to restore items?"; Organizer bin and organizer Back: "Delete item?" / "All selected items will be deleted from your device and cannot be recovered." (`organizer_dialog_title` / `organizer_deleting_select_item`), "Permanently delete" / "Cancel"; Junk: no confirmation in 2.7.2 (Clean runs at once) |

Success / progress copy: "Scanning..." `scanning`; `junk_scan_completed_message` (Q1); "Junk Found" `junk_found`; "Cleaning...." `cleaning_item`; "Junk files Cleaned" `phone_good_condition`; "Release more space now" `release_more_space`; "Finished" `finish`; "Scanning Completed!" `scan_complete`; "This may take a while on large libraries." `dup_media_wait_hint`; "Deleting" `deleting`; "Restoring" `restoring`; "Task completed" `task_completed`; "Your device is ready to use" `your_device_is_ready_to_use`; "Storage freed" `dup_delete_storage_freed`; "%1$s freed" `dup_delete_space_freed`; "Media files deleted successfully." / "Media files restored successfully."; intro `str_step_1..4`.

**Keys to import** (missing in `values/strings.xml`; copy from `R272/values/strings.xml`, then sync translations):
`junk_scan_completed_message`, `tool_bar_remove_junk`, `mb_size`, `cleaning_item`, `app_cache`, `junk_files_display`, `apk_files`, `obsolete_apk_files`, `temp_file`, `old_document`, `log_files`, `empty_folder`, `phone_good_condition`, `release_more_space`, `finish`,
`permanent_delete_screenshot_title`, `permanent_delete_screenshot_sub_title`, `deleting`, `lottie_text`, `group`, `images`, `please_select_items`, `dup_delete_complete_title`, `dup_delete_space_freed`, `remove_items`, `deleting_files`,
`selected_media_files_successfully_restored`, `restoring`, `days`, `unorganized`, `you_can_select_a_photo_to_jump_to`, `organize_now`, `str_step_1`, `str_step_2`, `str_step_3`, `str_step_4`, `org_create_album`, `org_album_name_photo`, `org_album_name_family`, `undo`,
`organizer_dialog_title`, `organizer_deleting_select_item`, `organize_your_photos_quickly`, `wrong_root_selected`; plural `dup_delete_files_removed` (new `values/plurals.xml`).
Remove from ours (not in 2.7.2): `dup_delete_file_removed`, the string `dup_delete_files_removed`, `permanent_delete_duplicate_title`, `permanent_delete_duplicate_single_title`, `permanent_delete_duplicate_sub_title` (after their last use goes).

---

## UI Considerations

Applicable state considerations resolved: 20 covered, 10 backstop, 3 unresolved (planner traces)

| Category | Element(s) | Status | Resolution / Reason |
|----------|------------|--------|---------------------|
| populated | System Cleaner | ✅ covered | System Cleaner shows the Remove Junk card with a red Scan now button and three stroked rows (Duplicate Images, Screenshots, Large Video) with 2.7.2 titles, subtitles and arrows |
| loading | junk scan | ✅ covered | The junk scan shows the orange gradient, the scanning Lottie, "Scanning..." and the current path, then turns red with the done Lottie for 2.5 s before the results |
| populated | junk results | ✅ covered | Results list Junk Files, Obsolete APK Files, Temp Files, Log Files, Empty Folders in that order; rows with junk show arrow, size and a checked mark, rows without show "No Junk" |
| zero-one-many | junk total | ✅ covered | With nothing checked Deep Clean drops to 60 % opacity and does nothing; Clean keeps its grey look |
| interaction | junk cleaning | ✅ covered | Clean runs the full-screen cleaning Lottie with the size counting over 800 ms, then the done Lottie, then Junk cleared counting the freed size up from 0 |
| empty | junk cleared | ✅ covered | A cleaned size of 0 shows "Finished" instead of a size |
| format | sizes | ✅ covered | Sizes read like "23.2 MB" / "54.5 kB" (1024-based, at most one decimal, no trailing zero), with the header unit at half size |
| populated | large videos / screenshots | ✅ covered | Large videos (≥ 70 MiB) in 3 columns and screenshots in 4 columns, grouped under Today / Yesterday / dd MMM yyyy headers, each cell with a check circle |
| selection | large videos / screenshots | ✅ covered | The bottom button is grey "Delete Selected" with nothing picked, blue "Delete (N)" with some, blue "Delete All (N)" with all, and the toolbar text flips Select all / Unselect all |
| empty | every list screen | ✅ covered | Each screen shows its own empty copy from the Copywriting table under `ic_empty_img`, never before the first query returns |
| loading | duplicate waiting | ✅ covered | Duplicate Finder opens a blue full screen with a white spinner, "Scanning..." and the hint, then plays the success Lottie once with "Scanning Completed!" and opens the groups |
| populated | duplicate groups | ✅ covered | Groups read "Group 1 (2 Images)" (1-based) over 4-column rounded cells captioned with the folder name; Keep One Photo starts off |
| selection | Keep One Photo | ✅ covered | Turning Keep One Photo on checks every photo except the first of each group and shows "Delete Selected (N items)"; off clears it |
| timing | delete process | ✅ covered | Deleting duplicates shows the blue "Deleting" screen for at least 3 s with Back blocked, then Cleanup complete |
| populated | cleanup complete | ✅ covered | Cleanup complete shows the looping success Lottie, "Task completed", the plural file count and "N MB freed" in blue inside the stroked card; the home icon acts as Back |
| populated | Recycle Bin | ✅ covered | The Recycle Bin shows the blue-tinted 30-day card with its switch off by default and a 3-column grid; with the switch on each cell shows "N Days" on a dark strip at its bottom |
| visual | organizer intro | ✅ covered | Quick Organize first opens on the dark overlay with hearts, the quick_oraganize Lottie and the blue "ORGANIZE NOW" pill above the Create Album / Photos / Family row; the next step is "Tap to Create Album" over a white bottom card with the hand Lottie |
| animation | scan / clean Lotties, header 350→300 dp, list entrance | 🧪 backstop | Compare junk scan → done → results → cleaning → done → cleared and the duplicate waiting → done sequence side by side with 2.7.2 (no clean on real media in the reference; use a test folder on ours only) |
| locale | long translations (de, ru, ta) in Clean / Deep Clean, Delete Selected (N items), ORGANIZE NOW, row titles | 🧪 backstop | All single-line; check nothing clips at 82.sdp (Scan now) and in the 38.sdp junk buttons |
| rtl | junk rows, check positions, stripe, intro arrows | 🧪 backstop | ar/fa/ur mirror rows; hand-arrow Lotties are not mirrored in the reference |
| dark | every screen | 🧪 backstop | Light and dark screenshots match 2.7.2 (large video and organizer use `white` = #000000 at night; cleaner cards keep a #1E1E1E stroke) |
| behaviour | junk cleaning count direction | 🧪 backstop | `RJ` float animator over 800 ms; confirm on ours whether the size counts down to 0 or up, against a reference video of a clean on the user's own device only if they choose |
| behaviour | remove-junk hint Lottie | 🧪 backstop | `remove_junk_button_animation` posted after 3000 ms (`RJ.Oooooo`, `a5/h` case 0x1b): confirm on device where it draws over the bar |
| media | recycle cells | 🧪 backstop | Days strip, video duration and favourite mark with items in the bin (the reference bin was empty on 2026-11-23; check with a test item on ours and, if the user agrees, one throwaway test photo on the reference) |
| selection | recycle top Delete visibility | 🧪 backstop | Empty-all icon shows only with items and no selection (from `RB` 4409 flow); confirm on device |
| dialog | duplicate delete recycle checkbox | 🧪 backstop | `oa/j` may hide "Move to Recycle Bin" (`isShowRecycle`); confirm which on device before shipping |
| performance | large library in duplicate scan, large video query | 🧪 backstop | Scroll and selection stay smooth with thousands of items (checklist D-15) |
| scope | DuplicateScanningCompleteActivity | ✅ resolved | Not built; "complete" = waiting done state (Q4, user decision) |
| data-loss | junk "Log Files" row | ✅ resolved | Reference rule (documents older than a year) with items unticked by default (Q6, user decision) |
| behaviour | organizer step flow 1→7 | ⚠ unresolved | Planner traces every `organizerStepFlow` / `isOrgSkipToolTip` site in `OA` before building §6.2; device confirmed steps 1 and 2 only |
| behaviour | organizer stripe tap | ⚠ unresolved | Result 777 is handled by an event from `ca/at`; planner confirms it opens Quick Organize |
| behaviour | junk temp-file rule | ⚠ unresolved | Read the temp pass in `JV` before changing ours; keep `.tmp`/`.temp` until then |
| scope | video usage-access hook | ✅ resolved | No-op hook in video player and viewer, none on Large videos (Q5, user decision) |

---

## Registry Safety

| Registry | Blocks Used | Safety Gate |
|----------|-------------|-------------|
| none (Android, no shadcn) | – | not applicable |

Asset provenance gate (Phase 1 rule): copy only from `R272/res`; no ad-SDK resources (`gnt_*` templates are not imported); brand-art decision of 2026-11-13 stands.
Device rule followed on 2026-11-23: read-only on the reference (no clean, delete, restore, move or album create; ads closed with Back).

---

## Open questions (all resolved 2026-11-23, see User decisions)

| # | Question | Resolution |
|---|---|---|
| Q1 | 2.7.2's scan-done text is the **fixed string** `junk_scan_completed_message` = "Junk Files Found: 1.40 GB\nScanning Completed!" (translated with "1.40 GB" baked in; `dx/f` calls `getString` with no argument), so it always says 1.40 GB. Copy it as is, or show the real size? | Resolved: reference string as is |
| Q2 | Deep Clean carries the `deep_clean_ic` "AD" TV icon and Delete Selected the `ic_ads_left` badge, both promising a rewarded ad our app never shows (D5). Keep the badges for the look, or drop them? | Resolved: keep both badges; action runs at once |
| Q3 | 2.7.2's Recycle Bin auto-delete switch **defaults off** (no day strips, no purge until turned on); ours defaults on. Switch the default to off for fresh installs (existing saved values kept)? | Resolved: default off for fresh installs; saved value kept |
| Q4 | `DuplicateScanningCompleteActivity` exists in 2.7.2 but nothing opens it. The roadmap's "complete" step is the waiting screen's done state. Skip the separate screen? | Resolved: skip it |
| Q5 | CLN-01 says "including the video usage-access prompt", but 2.7.2 records that trigger on Back from the **Video player** and in the viewer (`VideoPlayerActivity.Oooo0oO`, `MediaFullScreenActivity.o000ooO0`), not on Large videos. Put the Phase 6 no-op hook there (Phase 5 files)? | Yes: `recordVideoUsageAccessTrigger()` no-op in `VideoPlayerScreen` Back and the matching viewer point, `shortcut:` comment naming Phase 12; Large videos gets none (resolved) |
| Q6 | 2.7.2's "Log Files" junk row actually lists **documents older than one year** (doc, docx, pdf, txt, csv, xls, xlsx, ppt, pptx, xml), pre-checked, and Clean deletes them permanently with no confirmation. Port that exactly, or keep our `.log`-only rule under the same label? This can delete real user documents. | Port the reference rule (parity is the core value) **only after the user confirms**; Resolved: port the rule, Log Files items **unticked by default** |

---

## Re-runnable provenance (2026-11-23)

- Layout id → class: `grep 'name="<layout>"' R272/values/public.xml`, then ripgrep the id under `S272` (0x7f0d00df → `RJ` native, 0x7f0d010d → `da/f`, 0x7f0d0117 → `ar/da` (large video and screenshot cell), 0x7f0d0124 → `ar/dj` (date header), 0x7f0d008b → `dw/b`, 0x7f0d002b → `DR`).
- Resource ids in a smali file: `python -I <scratch>/ids.py <file.smali>` (reads `public.xml` + `strings.xml`); compact smali with ids resolved: `python -I <scratch>/sm.py <file> <from> <to>`; compact layout: `python -I <scratch>/lay.py <layout>` (scratch tools under the session scratchpad; recreate from this list if gone).
- Junk: `dx/f` (0x9c4 hold, `junk_scan_completed_message`, `layout_complete_background`, `done_cleaner_lottie`); `RJ` constructor keys; `dy/m` 320-430 display names; `dy/g` icons and check states; `ar/al` constructor (field order) → `btnClean` = `eyd3OXAZgV`, `btnDelete` = `DxDJysLV5r`, `lavRemoveJunkAnimation` = `OooO00o`; `RJ.OoooOO0` (alpha 0.6); `RJ.Ooooo00` (350→300, 500 ms); `RJ.OoooooO` (800 ms, `cleaner_lottie`); `dx/j`; `JC` 360-560, `dx/a`; `K.OooO0o0` / `K.OooO0Oo` (`#,##0.#`, `RelativeSizeSpan 0.5`); old documents `aw/b` 480-545 + `dx/e` `Calendar.add(1, −1)`.
- Large video / screenshots: `LargeVideoViewModel` 368 (0x4600000), `ScreenshotsViewModel` 344-482, spans `LV` 3167/3411 (3), `SS` 3274 (4); dialogs `ds/a`, `a0/e`.
- Duplicates: `DW.OoooOo0` + `ak/h`; `DP` 322-660 (switch), 546-651 (button), 2160-2300 (group label), 1284-1406 (`oa/j` = `dialog_delete`), 5405 (4 columns); `bi/d` 380-440 (3000 ms); `DC` 400-790; `s9/bn` lists `DuplicateScanningCompleteActivity` only as a name.
- Recycle: `da/e` 720-830 (`days`), `cz/k` 270-287 and `db/a` 1175-1197 (30 − days), `RB` 11052/11414 (`isAutoDelete` default false), 11377 (3 columns), 6910-6919 (RECYCLE trigger on Back).
- Organizer: `menu_organized` via `aapt2 dump xmltree --file res/menu/menu_organized.xml reference/gallery-2.7.2.apk`; `OA` 2191-2300 (default albums), 3127-3274 (Back dialog); `ML` 14000-14133 + `ck/a` + `be/a` (stripe); 2.6.7 intro/stripe via `aapt2 dump xmltree … gallery.apk`.
- Device: launch `adb shell monkey -p albums.gallery.photo.folder.picasa.app.web.gallery -c android.intent.category.LAUNCHER 1`, close interstitials with Back, Explore → Manage rows / Scan now; `adb exec-out screencap -p`; UI dump `MSYS_NO_PATHCONV=1 adb shell uiautomator dump /sdcard/ui2.xml`.
- Missing keys/assets in ours: `grep -q 'name="<key>"' app/src/main/res/values/strings.xml`; `ls app/src/main/res/drawable*/<name>.*`; `python -I scripts/ref_res.py check-colors`.

---

## Checker Sign-Off

- [x] Dimension 1 Copywriting: PASS
- [x] Dimension 2 Visuals: PASS
- [x] Dimension 3 Color: PASS
- [x] Dimension 4 Typography: PASS
- [x] Dimension 5 Spacing: PASS
- [x] Dimension 6 Registry Safety: PASS
- [x] Dimension 7 Inventory Provenance: PASS

**Approval:** approved 2026-11-23 (gsd-ui-checker)

## User decisions (2026-11-23), final; these override any open Q1-Q6 wording above
- **Q1:** copy the fixed "1.40 GB" scan-done string exactly.
- **Q2:** keep the AD badges on Deep Clean and Delete Selected; the action runs directly (no ad).
- **Q3:** Recycle Bin auto-delete is **off** for fresh installs; an existing saved value is kept.
- **Q4 (default accepted):** no separate scanning-complete screen. "Complete" is the waiting screen's done state.
- **Q5 (default accepted):** the video usage-access no-op hook goes where 2.7.2 triggers it (video player and viewer).
- **Q6 (data loss):** keep the "Log Files" row and its look, but its items are **unticked by default**. Users must tick documents themselves before Clean deletes them.
