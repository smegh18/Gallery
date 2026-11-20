---
phase: "4"
slug: "albums-stories-and-explore"
status: approved
reviewed_at: 2026-11-20
shadcn_initialized: false
preset: none
created: "2026-11-20"
scope: "Phase 4 Albums tab (fragment_album, item_home_album_card, item_stories), stories viewer (activity_story_view_photo; dialog_usage_access_story hook only) and Explore tab (fragment_explore, explore_tab_*); ALB-01, ALB-02, EXP-01; checklist #8, #9, #15"
---

# Phase 4 — UI Design Contract (Albums, Stories and Explore)

> Parity port, not a new design. Every value below comes from the decompiled reference 2.7.2
> (layout XML for looks, smali for behaviour) and carries its source. Nothing here is a preference.
> Tokens (`N.sdp`, `N.ssp`, Inter, `GalleryText.*`, colour names, radii) are defined in the Phase 1
> contract and the Home shell (top bars, tab bar, ⋮ popup, selection top bar, bottom-bar hide-on-scroll)
> in the Phase 3 contract. Both are only referenced here.

**Evidence roots (read-only):**
- `R272` = `%TEMP%\ref272\dec\res` (2.7.2), `R267` = `%TEMP%\ref267\dec\res` (2.6.7)
- `S272` = `%TEMP%\ref272\dec\smali\albums\gallery\photo\folder\picasa\app\web\gallery`
- `AF` = `S272/ca/at.smali` (Albums fragment, layout 0x7f0d00c8); `AA` = `S272/bx/d.smali` (album card adapter) + `S272/bx/c.smali` (card holder);
  `SA` = `S272/bx/k.smali` (story row adapter) + `S272/bx/j.smali` (story holder); `AC` = `S272/ca/f.smali` (album tap routing);
  `SV` = `S272/presentation/storyviewphoto/view/StoryViewPhotoActivity.smali`; `EF` = `S272/ca/cb.smali` (Explore fragment, layout 0x7f0d00ca);
  `EC` = `S272/ca/bs.smali` (Explore tap routing); `RC` = `R272/xml/default_remote_config.xml`
- Ours = `app/src/main/java/com/quizango/gallery/ui/{home,discovery}/`, `data/repository/MediaRepository.kt`, `app/src/main/res/`

**Upstream contracts:** `.planning/phases/01-foundation/01-UI-SPEC.md` (tokens) and
`.planning/phases/03-home-shell-and-photos-tab/03-UI-SPEC.md` (shell). Reference `_Nsdp` → `N.sdp`,
`_Nssp` → `N.ssp`, plain `dip` → unscaled `N.dp`, `px` → `N` px (`Dp(N / density)`).

**Ads (D5):** placeholders keep their slot and size. Rule carried from Phase 3: a slot whose reference
layout has a loading skeleton draws that skeleton static (no shimmer animation, not clickable, no text);
a slot with no skeleton stays collapsed (its no-fill state).

---

## Design System

| Property | Value |
|----------|-------|
| Tool | none (Android, Jetpack Compose; shadcn not applicable) |
| Preset | not applicable |
| Component library | Compose Material 3 (Phase 1 contract); house components in `ui/components/` and the Phase 3 helpers in `ui/home/` (`HomeSelectionTopBar`, `SelectionAction`, `drawableBackground`, `DeleteMediaDialog`, `DialogButton`) |
| Icon library | Reference vector drawables via `painterResource` (Phase 1 contract). No `material-icons` for any glyph the reference ships (today `AlbumsTab.kt` uses `Icons.Filled.Videocam`, `Icons.Outlined.Folder`; `StoryViewScreen.kt` imports `Icons.Filled.Close/Edit/Share`: remove) |
| Font | Inter (Phase 1 contract) |
| Animation | Compose `animate*`/`Animatable`; no Lottie in this phase |

Component Inventory section omitted: `Tool: none` (no registry to enumerate; same as Phases 1 and 3).

---

## 1. Albums tab (`R272/layout/fragment_album.xml` CHANGED — checklist #8, ALB-01)

### 1.1 Structural changes from 2.6.7 (do these first)

| Change | Evidence | Action in ours (`ui/home/AlbumsTab.kt`) |
|---|---|---|
| Feature carousel removed (`rvFeaturesLists`, `home_top_bg_color` band) | `R267/fragment_album` has `rvFeaturesLists`; `R272` has none (checklist E) | Delete `TopFeatureItem`, `HOME_TOP_FEATURES`, `FeatureItemView`, the `topContainerBg` band and `onFeatureClick` (and its `when` in `HomeScreen.kt`). Its destinations now live in Explore (§3). Delete `ic_home_top_*` drawables if nothing else uses them (grep first) |
| "Media Types" section removed | `R267/fragment_album` lines 32-60 (media-type rows) gone; `R272` keeps only a `gone` 1.dp `myAlbumType` view | Delete `MediaTypeItem`, `MEDIA_TYPES`, the "Media Types" block and `onMediaTypeClick` (and its `when` in `HomeScreen.kt`) |
| Story row restyled, no background band | `R272/fragment_album` `storyRecycler` has no background; `item_stories` CHANGED | §1.3 |
| Album cards are the new `item_home_album_card` | `AA.onCreateViewHolder` inflates 0x7f0d0112 | §1.4 |
| `item_folder`, `item_grid_gallery` | Diff vs `R267`: only `fontFamily` → `if_inter_medium` | Nothing to do here: Inter is already app-wide (Phase 1). These items belong to the collage picker (Phase 8) and media lists (Phase 5) |

### 1.2 Layout (top to bottom, one vertical scroll)

| Element | Value | Reference |
|---|---|---|
| Root | `album_tab_bg` (= `backgroundColor`); vertical scroll of the whole page (NestedScrollView, `clipToPadding=false`), no extra bottom padding (the Phase 3 bottom bar hides on scroll) | `fragment_album` root, `albumNestedScroll` |
| First-load spinner | centred `colorPrimary` indeterminate spinner over the page until albums load (Phase 3 §2.8 already ships this; keep) | `progressBar` |
| Story row | §1.3 | `llStoryView` |
| Album grid | §1.4, `marginTop` 7.sdp under the story row | `albumRecycler` |
| Album native ad slot | collapsed (no skeleton in `flAd`; D5 rule). Reference position for later: under the grid, `album_tab_ad_divider_bg` (2.dp top line `album_tab_divider`), padding 7.sdp h / 5.sdp top / 7.sdp bottom | `flAd` |

### 1.3 Story row (`item_stories` CHANGED, adapter `SA`, list from `HomeViewModel.OooOoO()` + `AF.OooOo`)

**Container:** horizontal list, padding start/end 7.sdp, top 9.sdp, bottom 10.sdp, no item spacing (`storyRecycler`).

**Item:**

| Element | Value | Reference |
|---|---|---|
| Cell | column, width 64.sdp, centred horizontally, clickable | `item_stories` root |
| Ring frame | 61.sdp square | `FrameLayout` |
| Ring | `InsLoadingView` "unclicked": one solid ring, colour `album_tab_story_ring` (#0066FF, start = end colour), ring diameter 0.9 × 61.sdp, stroke 0.025 × 61.sdp; photo clipped to a circle of 0.865 × 61.sdp, centre-crop | `com/baselibrary/custom/InsLoadingView.smali` (`circleDia 0.9f`, `strokeWidth 0.025f`, `bitmapDia 0.865f`); colours set in `SA` constructor (0x7f06003b) |
| Press feedback | the ring view scales to 0.9 while pressed, back to 1.0 on release | `InsLoadingView.onTouchEvent` (0.9f) |
| Cover | first photo of the story | `SA`/`bx/j.HISPj7KHQ7` → `s9/bd.OooO0oO(data[0])` |
| Label | 8.ssp Inter Regular, `album_tab_story_label`, centred, 1 line ellipsize end, padding h 2.sdp, marginTop 1.sdp | `status_text` |

**Labels by story type** (`bx/j.HISPj7KHQ7`): RECENT → "Recent" `recent`; RANDOM → "Random" `random`; FAVORITE → "Favorite" `favorite`;
MONTH1 / YEAR1 → "Memories" `album_tab_memories`; MONTH3 / YEAR3 → "Flashback" `flash_back`. Our `StoryBuilder` already yields these
stories in 2.7.2 order (Recent / Memories / Flashback / Random / Favorite, max 15, photos only): keep its rules, replace its English
`title` literals with these keys (`@StringRes`).

**Ad story circle** (`AF.OooOo`, config `playzen_story_config`, defaults in `ca/g`): inserted into the row at index **2**
(`position` default 2, clamped to `0..size`), shown unless the user is ad-free (`s9/k.OooOo` reads `IsAdFree`; ours is never ad-free).
Observed on the phone: Recent / Memories / (ad) / Random.

| Element | Value | Reference |
|---|---|---|
| Image | `ic_story_playzen` (already in ours) inside the same ring | `bx/j` (type −1001, empty `icon` → 0x7f080437) |
| Badge | `ad_bg` filling the 61.sdp frame with 8.sdp padding, visible only on this item | `adBadge` |
| Label | "Playzen" `playzen` (reference default `name`) | `bx/j` → 0x7f130937 |
| Tap | no-op: the default `redirection_link` is empty and `AF.OooO0O0` only opens a non-empty link | `AF.OooO0O0` (`:cond_5`) |
| In the viewer | never: the queue passed to the viewer filters it out | `AF.OooOoO0` |

**Story tap:** open the viewer (§2) with the row's stories minus the ad circle and the tapped story's index (`AF.OooO0O0` → `StoryViewPhotoActivity`, extra `POSITION`).

**Loading skeleton** (`shimmerStoryLay`, only while the story list is still being built): row with padding h 6.sdp, marginTop 5.sdp, three
`item_stories_shimmer` cells (60.sdp wide, margin 2.sdp): circle `bg_story_shimmer` 60.sdp (ring stroke 2.sdp `shimmer_color`, inner disc inset 5.sdp),
then an 8.sdp bar `shimmer_color` with 7.sdp side margins, 5.sdp below. Ours builds stories synchronously in `remember`, so this state is normally
unreachable; build it only if the device check shows it on 2.7.2 (see UI Considerations). Import `bg_story_shimmer` then.

### 1.4 Album grid (`item_home_album_card` NEW, adapter `AA`)

**Grid:** 3 columns (`AF.onViewCreated`: `GridLayoutManager(ctx, 3)`), list padding start/end 9.sdp; each card has 4.sdp side margins and 20.sdp
bottom margin → 13.sdp outer edge, 8.sdp between columns, card width `(width − 2×9.sdp)/3 − 2×4.sdp`.

**Order** (`AA.OooO0oo`, built from the album list):

| # | Card | Shown when | Title (key) | Tap opens (`AC` case 0) |
|---|---|---|---|---|
| 1 | All images (`ALL_IMAGE`) | the list has it | "Recent" `recent` | Album media list titled "All Photos" `all_photos` (ours: `AlbumDetail` `filter:all_photos`) |
| 2 | Favorite (type 8) | always: a placeholder card with no photos when there are no favourites (`favoritePlaceholder`) | "Favorite" `favorite` | Favorites screen (`FavoriteActivity`; ours `Screen.Favorites`) |
| 3 | All videos (`ALL_VIDEO`) | the list has it | "All Video" `album_tab_all_video` | Album media list titled "Videos" `videos` (ours `filter:videos`) |
| 4 | Camera (`ALL_CAMERA`) | the list has it | "Camera" `camera` | Album media list titled "Camera" `camera` (ours `filter:camera`) |
| 5 | Screenshots (type 10) | the list has it | "Screenshots" `screenshot` | Album media list titled "Screenshots" `screenshot` (ours `filter:screenshots`) |
| 6… | Device folders | each non-virtual album, in list order (ours: newest photo first, keep) | folder name | Album media list of that folder |
| last | Deleted (type −2001, `deletedCard`) | always | "Deleted" `album_tab_deleted` | Recycle Bin (`RecycleActivity`; ours `Screen.RecycleBin`) |

The reference shows an interstitial before opening (`albums.click_interstitial`): no ad in ours (D5), open directly.
Virtual cards 3-5 appear only when the source list contains them (`AA.OooO0oo` skips absent ones; `AF.handleEvents` checks each for
`isEmpty`). Ours today always adds an empty Screenshots album (`MediaRepository.getAlbumsFlow`, comment "Reference always shows Screenshots"):
drop empty All Video / Camera / Screenshots cards, then confirm on device (UI Considerations).

**Card:**

| Element | Value | Reference |
|---|---|---|
| Thumb card | square, radius 10.sdp, background `album_tab_card_placeholder`, no elevation, clipped | `mainLayout` |
| Thumbnail | fill, centre-crop, decoded at 200×200; placeholder and error `image_loading_albums`; an album with no media shows only the card colour | `thumbnail`, `bx/c.eyd3OXAZgV` (`OooO0o0(200,200)`, 0x7f0804af, `empty|` branch) |
| Deleted card | thumbnail = newest Recycle Bin item (none → card colour only); `album_tab_deleted_dim` overlay over the whole card; `album_tab_ic_trash` 17×22.sdp centred | `dimOverlay`, `centerIcon`; `AA.OooOO0` (count, path) |
| Type badge | bottom-start, 12.sdp, margin start 6.sdp, bottom 6.sdp: Favorite → `album_tab_ic_favorite`; All Video → `album_tab_ic_video`; a pinned device folder → `ic_pin` (import from `R272/drawable`); otherwise hidden. No other overlay (remove our `Videocam` icon) | `albumType`, `bx/c.eyd3OXAZgV` |
| Selection mark | only in album selection mode (§1.5): top-end 18.sdp, margin 5.sdp, `ic_check` / `ic_un_check` | `selected`, `bx/c.DxDJysLV5r` |
| Title | marginTop 6.sdp; 9.ssp Inter Medium `album_tab_name`; 1 line, ellipsize end (ours: 2 lines 13.5sp: fix) | `title` |
| Count | marginTop 2.sdp; 9.ssp Inter Regular `album_tab_count`; 1 line; the bare number formatted with `NumberFormat.getIntegerInstance()` (locale grouping, e.g. "1,234"; no "items" word). Deleted card shows the Recycle Bin count | `count`, `AA.countFormat` |
| Tap | §1.4 order table; in selection mode toggles (§1.5) | `bx/c` click (`ay/d` case 3) |
| Long-press | device folder: enter album selection with it selected; virtual card: toast (§1.5) | `bx/b`, `bx/c.HISPj7KHQ7` |

### 1.5 Album selection mode (`R272/layout/layout_select_album_bottom.xml`, same as 2.6.7; missing in ours)

Ours has no album selection. 2.7.2 keeps it unchanged, so it is part of #8 parity. The planner may give it its own plan.

- **Enter:** long-press a device-folder card. Only device folders (`isPublic`) can be selected; tapping or long-pressing a virtual card
  (Recent, Favorite, All Video, Camera, Screenshots, Deleted) during selection shows the toast "You can not select system folders"
  (hard-coded English in the reference, `bx/c.smali:333`; store it as a `translatable="false"` resource so it stays English in every language).
- **Top bar:** the Phase 3 selection top bar (§2.6 of 03-UI-SPEC: close, "%d item(s) selected" `items_selected`, Select all / Unselect all); pager swipe off.
- **Bottom bar** (replaces the tab bar; background `select_album_bg`, elevation 10.sdp): 5 equal-weight actions, each a column with
  padding 4.sdp h / 10.sdp v, borderless ripple, icon at intrinsic size, label `GalleryText.BodyXSmallMedium` `titleColor`, marginTop 2.sdp:

| Order | Icon | Label (key) | Action |
|---|---|---|---|
| 1 | `ic_album_select_pinned` | "Pin to top" `pin_to_top` / "Unpin" `unpin_to_top` when the selection is pinned | pin/unpin; pinned folders show `ic_pin` and sort first among folders |
| 2 | `ic_add` | "Add to" `add_to` | copy/move the albums' media to an album (reference events `home_albums_bottom_menu_add_to_copy/move`, `AF`) |
| 3 | `ic_album_select_delete` | "Delete" `delete` | delete confirmation (Phase 3 `dialog_delete` copy), then delete the albums' media |
| 4 | `ic_album_select_cover` | "Change cover" `change_cover` | pick a cover from the album |
| 5 | `ic_rename` | "Rename" `rename` | rename dialog |

- **Back / ✕:** clear the selection (Phase 3 Back order).

---

## 2. Stories viewer (`R272/layout/activity_story_view_photo.xml` CHANGED — checklist #15, ALB-02)

Ours: `ui/discovery/StoryViewScreen.kt` (+ `StoryQueue`). Keep its queue, next-story and share logic; apply the corrections below.

### 2.1 Layout

| Element | Value | Reference |
|---|---|---|
| Window | content inside the system bars (`fitsSystemWindows`); black behind the photo; screen enters and exits with a 500 ms alpha fade | root; theme `Theme.NewGallery.Story` → `StoryActivityFadeAnimation` (`fade_in_screen` / `fade_out_screen` 500 ms) |
| Photo | full screen, fit (no crop), alpha 0.9, loaded at screen size. No gradient scrim (ours draws a black vertical gradient behind the header: remove) | `blurry_image`, `s9/bd.OooO0oo` |
| Ken Burns | each photo alternates zoom in (scale 1.0 → 1.1) and zoom out (1.1 → 1.0), 3000 ms, accelerate-decelerate, starting with zoom in; pauses and resumes with the progress | `SV.OoooooO`, `R272/animator/zoom_in.xml`, `zoom_out.xml` |
| Tap zones | two equal halves of the screen: left = previous, right = next (ours uses 35%: change to 50%) | `reverse` / `skip` (weight 1 each) |
| Progress bar | top, marginTop 8.dp, height 3.dp, full width; one segment per item, 8.dp between segments, round caps; filled colour `onlyWhite`, track `grayTitle` (#585758 light / #7A8389 dark; ours uses white 35%: change) | `stories` `MultiProgressBar` (`progressColor`, `progressLineColor`; `MultiProgressBar.smali` default padding 8.dp) |
| Header row | marginTop 20.sdp, height 28.sdp | `rlLayout` |
| Close | `ic_story_close` at intrinsic size, padding 5.sdp, marginStart 10.sdp, centred vertically, borderless ripple, cd "Close" `close` | `ic_close_lay` |
| Title | marginStart 16.sdp after Close; `GalleryText.BodySmallSemiBold` (10.ssp, 600), `onlyWhite`, shadow #80000000 offset (2, 2) radius 4; text = the current photo's date (ours `storyDate`, keep), empty on an ad step. Initial text before the first item binds: "Stories" `stories` | `storiesText`, `SV.o00o0O` (`getStringDate`) |
| Share | `ic_share_story` at intrinsic size, no padding, marginEnd 10.sdp, borderless ripple, cd "Share" `share`; shares the current photo through the same share helper as the Photos tab (no "Share Story" literal) | `ic_share`, `SV.OoooOoO` → `s9/ao.OoooOoO` |
| Edit pill | bottom-centre, 30.sdp above the bottom, height 32.sdp, minWidth 80.sdp, padding start 14.sdp / end 16.sdp; background `story_edit_bg` (`blackTran3` #DD000000, radius 6.dp, ripple) — import it; `ic_edit` then "Edit" `edit` `GalleryText.BodySmallSemiBold` `onlyWhite`, marginStart 6.sdp, 1 line. Shown on photo steps, hidden on ad steps. Tap opens the photo editor with the current photo | `rlStoriesEdit`, `SV.o00O0O` |

All header and pill sizes are sdp (ours uses plain dp 12/28/30/32/80/14/16: convert).

### 2.2 Playback (`SV`)

| Behaviour | Value | Reference |
|---|---|---|
| Step time | 3000 ms per photo (ours 3500 ms: change) | `progressSingleDisplayedTime="3.0"` |
| Auto-advance | at the end of a step go to the next item; after the last item of a story, play the next story in the row; after the last story, close | `SV.o00Oo0`, `SV.OooooO0`; ours `StoryQueue.next` (keep) |
| Press | finger down pauses the progress and the zoom immediately | `SV.OoooOO0` (ACTION_DOWN) |
| Tap | release within 500 ms and moved less than 100 px: left half → previous item, right half → next item | `SV.limit = 500`, distance ≥ 100.0 check |
| Long-press | release after 500 ms (or after moving ≥ 100 px): resume where it paused, no navigation | `SV.OoooOO0` |
| Close / Back | stop timers, record the story usage-access trigger, close | `SV.Oooo0oO` |

### 2.3 Ad steps (story ad slot, D5)

`SV.ooOO`: after every 3rd photo (`story.ad_position` = 3 in `RC`) an ad step is inserted (photos 1-3, ad, 4-6, ad, …), only when not ad-free
and `story.native` is on (default true). Placeholder: the step stays (keep slot and size) and draws the static skeleton of
`R272/layout/gnt_story_template_view.xml` (`adLib_gnt_id_vg_shimmer_main`), centred vertically in the screen width (`rl_native_ad`, `layout_centerInParent`):

| Part | Value |
|---|---|
| Icon block | 65.sdp square, `shimmerColorDark`, centred at the top |
| Title bar | 200.sdp wide, one 20.ssp text line tall, `shimmerColorDark`, 15.sdp below the icon, 5.sdp side margins |
| Description bar | full width minus 10.sdp each side, one 15.ssp line tall, `shimmerColorMedium`, 15.sdp below |
| "Ad" tag | top-end, `ad_icon_bg_stroke_1` box with padding 5.dp h / 2.dp v, empty (no text, per the D5 rule) |
| Media block | full width × 150.sdp, `shimmerColorMedium`, 15.sdp below the description |
| CTA bar | full width minus 15.sdp each side, padding 10.sdp v, `ad_btn_bg_white` tinted `shimmerColorCTAButton`, 25.sdp below the media, 5.sdp bottom margin |

On an ad step: photo hidden, title empty, Share stays, Edit hidden, Ken Burns paused. The reference holds the progress while the ad loads
(up to `native_timeout` 10 000 ms, `SV.counterTimer`) and drops the step if the ad fails; with a placeholder there is nothing to wait for, so the step
runs the normal 3000 ms and taps work as on photos.

### 2.4 First-time Edit tooltip (`R272/layout/custom_toast_stories_edit_layout.xml`, same as 2.6.7; missing in ours)

Shown once, the first time a photo step with the Edit pill appears and `isStoriesEditToolTip` is false (`SV.o00O0O` → `SV.o00ooo`):
a popup anchored above the Edit pill, outside-touchable, pausing playback while shown.

| Element | Value |
|---|---|
| Bubble | `popup_stories_edit_bg` (#0066FF speech bubble with a bottom-centre pointer; import), side margins 10.sdp |
| Text | "Edit your photo" `edit_your_photo`, `GalleryText.BodyLargeSemiBold`, `onlyWhite`, centred, marginStart 5.sdp, top 3.sdp, bottom 8.sdp |
| OK | "Ok" `ok`, `GalleryText.BodyMediumSemiBold`, colour `cropIconColor`, on `btn_stories_edit_bg` (white, radius 18.dp; import), padding 15.sdp h / 5.sdp v, margins 8.sdp h / 5.sdp bottom |
| Hit area under the bubble | 80×32.sdp, 5.sdp below, over the Edit pill (tapping it also opens the editor) |
| Dismiss | OK or outside tap: hide, save `isStoriesEditToolTip = true`, resume |

### 2.5 Usage-access prompt (`dialog_usage_access_story` NEW) — hook only

On close/Back the reference records the STORY trigger (`SV.Oooo0oO` → `S272/ta/f.eyd3OXAZgV(ta/d.STORY)`; sessions in `ta/d`, key `usage_access_popup_last_shown_session`).
Phase 4 leaves that call a no-op hook with a `shortcut:` comment naming Phase 12 (USG-01), which builds the dialog and every trigger. No dialog UI in this phase.

---

## 3. Explore tab (`R272/layout/fragment_explore.xml` CHANGED; `explore_tab_hero/item_feature/item_row/item_tile` NEW — checklist #9, EXP-01)

Ours: `ui/home/ExploreTab.kt` is the 2.6.7 three-card page (System Cleaner, Quick Organize, Collage): replace it entirely.

### 3.1 Layout (top to bottom)

| Element | Value | Reference |
|---|---|---|
| Root | vertical scroll, `explore_screen_bg`, no overscroll glow; content padding 12.sdp h, 10.sdp top, 58.sdp bottom | `scrollView` + inner `LinearLayout` |
| Hero | §3.2 | `tileCleaner` |
| "Create" label | `ExploreSectionLabel`: 10.ssp Inter Medium `explore_subtext`, marginTop 16.sdp, bottom 8.sdp, start 2.sdp; text `explore_section_create` | style `ExploreSectionLabel` |
| Create row | two equal-width feature cards (§3.3), 8.sdp apart: AI Editor, Collage (with the "New" badge, §3.3) | `tileAiEditor`, `tileCollage` |
| "Library" label | same style, `explore_section_library` | — |
| Library row | four equal-width round tiles (§3.4): Vault, Favorites, Moments, Place | `tileVault`, `tileFavorites`, `rowMoments`, `rowPlace` |
| Native ad card | §3.6, marginTop 14.sdp | `flAd` |
| "Manage" label | same style, `explore_section_manage` | — |
| Manage card | `explore_ad_card_bg` (`explore_card_bg`, radius 13.sdp), padding 2.sdp v; four rows (§3.5): Duplicate Finder, Quick Organize, Recycle Bin, Settings | `manageCard` |

### 3.2 Hero "Free up space" (`explore_tab_hero`)

| Element | Value |
|---|---|
| Card | row, centred vertically; `explore_hero_bg` (`explore_hero_bg` colour, radius 13.sdp, ripple #33FFFFFF); padding top/bottom 13.sdp, start 14.sdp, end 10.sdp; the whole card is the tap target |
| Text column (weight 1) | "Free up space" `explore_free_up_space` 13.ssp Inter SemiBold `explore_hero_text`; 3.sdp below: "Duplicates and junk in one scan" `explore_free_up_space_desc` 9.ssp Inter Regular `explore_hero_subtext`; 10.sdp below: "Scan now" `explore_scan_now` pill |
| Scan now pill | 9.ssp Inter SemiBold `explore_hero_btn_text` on `explore_hero_btn_bg` (radius 50.sdp), padding 12.sdp h / 5.sdp v; not separately clickable (mirrors the card's pressed state) |
| Illustration | `explore_ic_sparkles` 44.sdp, marginStart 8.sdp, decorative |

### 3.3 Feature card (`explore_tab_item_feature`, bound by `EF.OooO0OO`)

| Element | Value |
|---|---|
| Card | column, `explore_card_bg` drawable (radius 13.sdp, ripple `explore_card_ripple`), padding 11.sdp |
| Chip | 30.sdp square, `explore_chip_bg` (radius 9.sdp) tinted with the item's `*_bg` colour; icon 17.sdp centred, tinted with its `*_fg` colour, decorative |
| Title | 8.sdp below the chip; 11.ssp Inter SemiBold `explore_text`, 1 line ellipsize |
| Description | 3.sdp below; 9.ssp Inter Regular `explore_subtext`, 1 line ellipsize |

| Item | Icon | Chip colours | Title (key) | Description (key) |
|---|---|---|---|---|
| AI Editor | `explore_tab_ic_ai_editor` | `explore_chip_ai_bg` / `_fg` | "AI Editor" `explore_tab_ai_editor` | "Remove background, blur" `explore_ai_editor_desc` |
| Collage | `explore_tab_ic_collage` | `explore_chip_collage_bg` / `_fg` | "Collage" `str_home_collage` | "Mix your photos" `explore_collage_desc` |

**"New" badge on Collage** (`collage_btn_new`): top-end of the Collage card, margin top 8.sdp / end 8.sdp; text "New" `album_collage_new`,
`GalleryText.BodyXSmallSemiBold`, `onlyWhite`, on `bg_rounded_red` tinted `home_collage_new_btn` (#FF1414, pill), padding 4.sdp h / 2.sdp v.
Visible while `collageMakerNewButtonExplore` is false **and** the app-open count is below 4 (`EF.onViewCreated`; count = `AppOpenCount`,
incremented per launch in `SplashActivity.smali:4286`; ours: reuse `sessionCount` if it is incremented once per launch, as Phase 3 does).
Tapping Collage or the badge hides it and saves `collageMakerNewButtonExplore = true` (`EF.OooO0o`).

### 3.4 Round tile (`explore_tab_item_tile`, bound by `EF.OooO0Oo`)

| Element | Value |
|---|---|
| Tile | column, centred horizontally, weight 1, padding 4.sdp v, borderless ripple |
| Circle | 40.sdp, `explore_chip_round_bg` (oval) tinted `*_bg`; icon 19.sdp centred, tinted `*_fg`, decorative |
| Label | 6.sdp below; 9.ssp Inter Medium `explore_text`, centred, 1 line ellipsize |

| Item | Icon | Colours | Label (key) |
|---|---|---|---|
| Vault | `explore_tab_ic_vault` | `explore_chip_vault_bg` / `_fg` | "Vault" `vault` |
| Favorites | `explore_tab_ic_favorites` | `explore_chip_favorites_bg` / `_fg` | "Favorites" `str_home_favourite` |
| Moments | `explore_tab_ic_moments` | `explore_chip_moments_bg` / `_fg` | "Moments" `str_home_Moments` |
| Place | `explore_tab_ic_place` | `explore_chip_place_bg` / `_fg` | "Place" `str_home_place` |

### 3.5 Manage row (`explore_tab_item_row`)

| Element | Value |
|---|---|
| Row | height 42.sdp, ripple (bounded), clickable |
| Divider | 1 px `explore_divider` at the row top, inset start 40.sdp; hidden on the first visible row (`EF` loop over the four rows) |
| Icon | 20.sdp box, marginStart 12.sdp, tinted `explore_text`, decorative |
| Label | 11.ssp Inter Medium `explore_text`, 1 line ellipsize, start-aligned, marginStart 8.sdp, marginEnd 7.sdp |
| Chevron | `explore_tab_ic_chevron` (auto-mirrored) 16.sdp, marginEnd 10.sdp, tinted `explore_subtext`, decorative |

| Order | Icon | Label (key) | Shown |
|---|---|---|---|
| 1 | `explore_tab_ic_duplicate` | "Duplicate Finder" `duplicate_finder` | only on Android 10+ with ≥ 3 GB total RAM (`s9/k.OooOo0o`: `totalMem / 1024² / 1024 ≥ 3`); hidden otherwise |
| 2 | `explore_tab_ic_organize` | "Quick Organize" `organizer_explore_title` | always |
| 3 | `explore_tab_ic_recycle_bin` | "Recycle Bin" `str_home_Recycle_bin` | always |
| 4 | `explore_tab_ic_settings` | "Settings" `settings` | always |

### 3.6 Native ad card (`flAd` + `explore_tab_native_ad_template`) — placeholder (D5)

Card `explore_ad_card_bg` (radius 13.sdp), min height 113.sdp, marginTop 14.sdp, always shown (ours is never ad-free). Draw the static skeleton
(`adLib_gnt_id_vg_shimmer_main`), no "Ad" text, not clickable:

| Part | Value |
|---|---|
| Media block | 167×91.sdp, `explore_ad_shimmer_media` (radius 5.sdp), start 11.sdp, top 11.sdp |
| Title bar | 9.sdp tall, `explore_ad_shimmer_block`, from start 191.sdp to end − 53.sdp, top 15.sdp |
| Tag block | 16×13.sdp, 7.sdp below the title bar, aligned to its start |
| Body bars | 6.sdp tall, 7.sdp below the tag, to end − 13.sdp; second 3.sdp below, to end − 48.sdp |
| CTA block | 26.sdp tall, `explore_ad_shimmer_cta`, from the title start to end − 23.sdp, 14.sdp above the bottom |

### 3.7 Explore tap routing (`EC` cases, bound in `EF.onViewCreated`)

| Item | 2.7.2 target | Ours |
|---|---|---|
| Hero / Scan now | `isJunkCleanedForToday` true → `SystemCleanerActivity`, else `JunkScanActivity` (EC case 6) | true → `Screen.Cleaner`, else `Screen.RemoveJunk`. The flag is set when junk is cleared (`JunkClearedActivity.smali:371`) and reset by Home (`bw/ay.smali:161`); Phase 6 owns writing it, Phase 4 reads it (default false) |
| AI Editor | in-app picker (`InternalSelectMediaActivity`, single photo) then the photo editor (EC case 9 → `EF.SJowARcXwM`) | `Screen.Editor.createRoute()` (its picker-first entry). Picker layout is Phase 7 (VLT-02), editor Phase 8 |
| Collage / New badge | in-app picker then the collage screen, no collage home (EC cases 7, 8 → `EF.OooO0o`) | `Screen.CollageMaker`; Phase 8 (EDT-02) removes the collage home |
| Vault | `VaultLockActivity` (EC case 0) | `Screen.VaultLock` |
| Favorites | `FavoriteActivity` (case 5) | `Screen.Favorites` |
| Moments | `MomentsActivity` (case 2) | `Screen.Moments` |
| Place | `MapActivity` (case 3) | the existing Place list (`AlbumDetail` `filter:place`); Phase 5 restyles it |
| Duplicate Finder | `DuplicateWaitingActivity` (default case) | `Screen.DuplicateWaiting` |
| Quick Organize | `OrganizerActivity` (case 1) | `Screen.QuickOrganizer` |
| Recycle Bin | `RecycleActivity` (case 0xa) | `Screen.RecycleBin` |
| Settings | `SettingActivity` (case 4) | `Screen.Settings` |

Every tap is guarded only by "fragment attached" (`EF.OooO0o0`); no interstitials (D5).

---

## Spacing Scale

Phase 1 scale applies (sdp units: 2, 4, 8, 12, 16, 24, 32, 48). Values used here, all exact reference values:
2.sdp (story label padding, section label start, manage padding, count gap), 4.sdp (card margins, tile padding, badge padding), 8.sdp (gaps between
Create cards, chip-to-title, row label start, badge margins), 12.sdp (Explore side padding, hero pill padding, row icon start), 16.sdp (section label
top, story title start, row chevron), 32.sdp (Edit pill height), 48.sdp not used.

Exceptions (parity overrides multiples of 4, per Phase 1): 1, 3, 5, 6, 7, 9, 10, 11, 13, 14, 15, 17, 18, 19, 20, 22, 23, 25, 26, 28, 30, 40, 42, 44,
53, 58, 60, 61, 64, 65, 80, 91, 113, 150, 167, 191, 200 sdp; unscaled 1 px (row divider), 2.dp, 3.dp (progress height), 5.dp, 6.dp, 8.dp, 18.dp.
Fractions of the 61.sdp ring (0.9, 0.865, 0.025) come from `InsLoadingView`. Never round them.

---

## Typography

All Inter, `includeFontPadding = false`, natural line height (Phase 1 rule).

| Role | Size | Weight | Line Height | Where |
|------|------|--------|-------------|-------|
| Body | 9ssp | 400 Regular | natural (~1.21) | album count, hero and feature descriptions |
| Label | 9ssp | 500 Medium | natural (~1.21) | album title, round-tile labels |
| Heading | 11ssp | 600 SemiBold | natural (~1.21) | feature card titles (row labels use 11ssp 500) |
| Display | 13ssp | 600 SemiBold | natural (~1.21) | hero "Free up space", Edit tooltip |

Exception (parity, as in Phases 1 and 3): the reference also uses 8ssp/400 (story label), 8ssp/500 (album selection labels), 8ssp/600 ("New"
badge), 9ssp/600 (Scan now), 10ssp/500 (section labels), 10ssp/600 (story title, Edit), 11ssp/600 (tooltip OK). Weights in use: 400, 500, 600.
The tables in each section are binding.

---

## Color

Phase 1 roles, applied here:

| Role | Value | Usage |
|------|-------|-------|
| Dominant (60%) | `album_tab_bg` = `backgroundColor` #FFFFFF / #0C0C0C; `explore_screen_bg` #F6F7FB / #0C0C0C | Albums page; Explore page |
| Secondary (30%) | `explore_card_bg` #FFFFFF / #1A1B1F; `album_tab_card_placeholder` #EEF0F3 / #1E2126; story viewer black | Explore cards, Manage card, ad card; album thumb backing |
| Accent (10%) | #0066FF (`album_tab_story_ring`, `explore_hero_bg` light, dark #1F4FD6) | reserved list below |
| Destructive | `red` #E11A1A (Phase 1) | album delete confirmation only |

Accent reserved for: the story ring (photo and ad circles), the Explore hero card, the Edit tooltip bubble (`popup_stories_edit_bg`), and the first-load spinner.
Not for titles, labels, icons, dividers or the "New" badge.

Category colours (Explore only, each pair used only by its own item): `explore_chip_{ai,collage,vault,favorites,moments,place}_{bg,fg}` (Phase 1 values).
Fixed non-theme colours from the reference: "New" badge #FF1414 (`home_collage_new_btn`), Edit pill #DD000000 (`blackTran3`), story title shadow #80000000,
progress track `grayTitle` (#585758 / #7A8389), Deleted dim #8F000000, ad skeleton `shimmerColorDark` #C7CACD / `shimmerColorMedium` #E4E5E7 /
`shimmerColorCTAButton` #CDCFD2, story skeleton `shimmer_color` #F2F2F2 / #262424.

**Colours to add if missing** (check `values/colors.xml` and `values-night`): `grayTitle`, `blackTran3`, `home_collage_new_btn`, `cropIconColor`,
`shimmer_color`, `shimmerColorCTAButton`, `adsBgColor`, `adsBgBorderColor`.
**Drawables to add** (missing in ours, copy from `R272/drawable/`): `ic_pin`, `story_edit_bg`, `popup_stories_edit_bg`, `btn_stories_edit_bg`;
`bg_story_shimmer`, `ad_icon_bg_stroke_1`, `ad_btn_bg_white` only if their state is built. Already present: every `explore_*`, `album_tab_*`,
`ad_bg`, `ic_story_playzen`, `ic_story_close`, `ic_share_story`, `ic_edit`, `ic_un_check`, `bg_rounded_red`, `image_loading_albums`.

---

## Copywriting Contract

Every visible string is a reference key; translations come from the Phase 1 sync (`python -I scripts/ref_res.py sync-strings`, then `check-strings`).
`album_tab_*`, `close_button` and `explore_tab_ai_editor` stay English in every language because 2.7.2 does not translate them (Phase 1).
No hard-coded English remains in `AlbumsTab.kt`, `ExploreTab.kt`, `StoryBuilder.kt`, `StoryViewScreen.kt` or the album names in `MediaRepository.kt`
(today: "Vault", "Favorites", "Cleaner", "Duplicate", "Recycle Bin", "Collage", "Editor", "Videos", "Place", "Moments", "Portrait", "High Definition",
"Burst", "Animated", "Media Types", "Recent", "Memories", "Flashback", "Random", "Favorite", "All Photos", "Camera", "Screenshots", "Edit",
"Close", "Share", "Share Story", "Geographic photo"). The one exception is the reference's own hard-coded toast "You can not select system folders" (§1.5).

| Element | Copy (reference key) |
|---------|------|
| Primary CTA | "Scan now" (`explore_scan_now`, Explore hero); "Edit" (`edit`, story viewer) |
| Empty state heading | none: the Albums tab has no empty text (it shows the Favorite placeholder and Deleted cards even with no media); Explore is static |
| Empty state body | none in this phase. An empty story list shows no story circles; an album with no media shows only its card colour and the count "0" |
| Error state | none new; a photo that fails to load in a story shows the black screen for its 3000 ms step and advances; reuse Phase 1 "An issue occurred!" (`something_went_wrong`) if the media query fails |
| Destructive confirmation | Album delete (selection bar): Phase 3 `dialog_delete` copy — "Are you sure you want to delete 1 item?" (`delete_dialog_permission`) or `deleted_title` N `deleted_items`, with "Move to Recycle Bin" (`delete_dialog_move`), "No" (`no`) / "Yes" (`yes`) |

**Keys to import** (missing in `values/strings.xml`; copy values from `R272/values/strings.xml`, then sync translations):
`stories`, `edit_your_photo`, `playzen`, `flash_back`, `random`, `recent`, `screenshot`, `all_photos`, `videos`, `pin_to_top`, `unpin_to_top`, `change_cover`.
Already present: `edit`, `ok`, `favorite`, `favorites`, `camera`, `album_tab_memories`, `album_tab_all_video`, `album_tab_deleted`, every `explore_*`,
`str_home_collage`, `str_home_favourite`, `str_home_Moments`, `str_home_place`, `str_home_Recycle_bin`, `vault`, `duplicate_finder`,
`organizer_explore_title`, `settings`, `album_collage_new`, `close`, `close_button`, `share`, `add_to`, `delete`, `rename`.

---

## UI Considerations

Applicable state considerations resolved: 12 covered, 7 backstop, 3 unresolved

| Category | Element(s) | Status | Resolution / Reason |
|----------|------------|--------|---------------------|
| removal | Albums tab feature carousel and Media Types | ✅ covered | The Albums tab shows only the story row and the album grid; no feature pills, no "Media Types" heading, no coloured top band |
| populated | story row order | ✅ covered | With stories available the row reads Recent / Memories / Playzen ad circle / Flashback or Random … with the ad circle at index 2 and labels from `recent`, `album_tab_memories`, `flash_back`, `random`, `favorite` |
| interaction | ad story circle | ✅ covered | Tapping the ad circle does nothing, and the viewer never plays it |
| populated | album grid order | ✅ covered | The grid is 3 columns: Recent, Favorite, All Video, Camera, Screenshots (each when present), device folders, then Deleted last |
| empty | Favorite and Deleted cards | ✅ covered | With no favourites the Favorite card still shows (card colour, count 0); Deleted always shows with the dim overlay and trash icon and the Recycle Bin count |
| zero-one-many | album count | ✅ covered | Counts are bare locale-formatted numbers ("0", "1", "1,234"), no unit word |
| navigation | album taps | ✅ covered | Recent → All Photos list, Favorite → Favorites, All Video → Videos list, Camera/Screenshots/folders → their lists, Deleted → Recycle Bin |
| timing | story playback | ✅ covered | Each photo shows 3000 ms with alternating 1.0↔1.1 zoom; a story ends into the next story; the last one closes the viewer |
| interaction | story taps and hold | ✅ covered | Left half goes back, right half goes forward, holding pauses progress and zoom and releasing resumes without moving |
| ads | story ad steps, Explore ad card | ✅ covered | An ad step follows every 3rd story photo and shows the static skeleton for 3000 ms; Explore shows the static 113.sdp skeleton card between Library and Manage; the Albums ad slot stays collapsed |
| navigation | every Explore item | ✅ covered | Each Explore item opens the screen in §3.7, and Scan now opens Remove Junk unless junk was cleared today |
| first-run | Collage "New" badge, Edit tooltip | ✅ covered | The badge shows until Collage is opened or the app has been opened 4 times; the Edit tooltip shows once and never again after OK |
| device | Duplicate Finder row | 🧪 backstop | Hidden on devices under 3 GB RAM or below Android 10; the divider moves to the next row. Check on the test phone (expected visible) |
| locale | long translations (de, ru, ta) in tile labels, feature titles, row labels | 🧪 backstop | All are 1 line with end ellipsis; check nothing wraps or pushes the chevron |
| rtl | Explore rows, story header, ad story index | 🧪 backstop | ar/fa/ur mirror the rows (chevron auto-mirrored), header and tap halves; check side by side |
| dark | every element above | 🧪 backstop | Light and dark screenshots match 2.7.2 (progress track turns #7A8389; hero turns #1F4FD6) |
| loading | story and album skeletons | 🧪 backstop | Ours loads stories and albums in one emission, so 2.7.2's skeleton rows are not built; if the phone shows them on cold start, add them per §1.3 and `item_common_album_shimmer` |
| scroll | Albums last row under the bottom bar | 🧪 backstop | No bottom padding (reference); the bottom bar hides on scroll. Check the Deleted card can be fully seen |
| animation | ring press scale, 500 ms viewer fade | 🧪 backstop | Compare with 2.7.2 on the phone |
| data | empty All Video / Camera / Screenshots cards | ⚠ unresolved | 2.7.2's adapter adds them only when present in the album list; whether an empty one reaches the list depends on `AF.handleEvents`. Contract: hide when empty; Phase 13 confirms on a phone with no screenshots |
| behaviour | album selection sub-actions (multi-select Change cover / Rename, Unpin label) | ⚠ unresolved | Layout and keys are bound (§1.5); the enabled state of Change cover and Rename with more than one album selected, and when Pin reads "Unpin", must be read from `AF` / `HomeActivity` or checked on device before building |
| brand | ad story `ic_story_playzen` and "Playzen" label | ⚠ unresolved | Reference cross-promo art and name, kept under the 2026-11-13 brand decision (Phase 1); add to the pre-release replacement list |

---

## Registry Safety

| Registry | Blocks Used | Safety Gate |
|----------|-------------|-------------|
| none (Android, no shadcn) | – | not applicable |

Asset provenance gate (Phase 1 rule): copy only from `R272/res`; no ad-SDK resources; brand-art decision of 2026-11-13 stands.

---

## Out-of-phase note (not specified here)

- **Phase 2 follow-up (onboarding Language list):** ours draws each language as a grey card; 2.7.2 shows plain rows with a coloured letter circle.
  Fix under Phase 2 (`settings/LanguageScreen.kt`, `R272/layout/activity_app_language.xml`); not part of this contract.

---

## Re-runnable provenance (2026-11-20)

- Layout id → class: `grep 'name="<layout>"' R272/values/public.xml`, then ripgrep the id under `S272` (e.g. 0x7f0d00c8 → `ca/at`, 0x7f0d00ca → `ca/cb`, 0x7f0d0112 → `bx/d`, 0x7f0d012e → `bx/k`, 0x7f0d0052 → `StoryViewPhotoActivity`, 0x7f0d00b0 → `ta/d`).
- Album order and titles: `bx/d.OooO0oo` (type filters `ALL_IMAGE` 0x7f0b000c, 8, `ALL_VIDEO` 0x7f0b0010, `ALL_CAMERA` 0x7f0b000a, 10, then non-static, then `deletedCard` −2001); `bx/c.eyd3OXAZgV` (titles, badges, counts).
- Album tap routing: `ca/f` case 0 (`all_photos`, `videos`, `camera`, `screenshot`, `FavoriteActivity`, `RecycleActivity`).
- Story labels: `bx/j.HISPj7KHQ7` (integers `RECENT`, `RANDOM`, `FAVORITE`, `MONTH1/3`, `YEAR1/3`); ad story: `ca/at.OooOo` + `ca/g` defaults.
- Story viewer: `StoryViewPhotoActivity` constructor (`limit` 500, `counterTimer` 10000), `OoooOO0` (touch), `ooOO` (ad every `story.ad_position`), `OoooooO` (zoom animators), `o00O0O`/`o00ooo` (Edit pill, tooltip), `Oooo0oO` (usage-access hook); `R272/xml/default_remote_config.xml` `story`.
- Explore binding: `ca/cb.onViewCreated` (`OooO0OO`/`OooO0Oo` resource triples, row loop, New badge), `ca/bs` cases, `s9/k.OooOo0o` (RAM gate).
- Changed-layout diffs: `diff <(sed 's/></>\n</g' R267/layout/X.xml) <(sed 's/></>\n</g' R272/layout/X.xml)`.
- Missing keys/assets in ours: `grep -q 'name="<key>"' app/src/main/res/values/strings.xml`; `ls app/src/main/res/drawable*/<name>.*`.

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

## Checker notes (2026-11-20, approved, non-blocking)
- **Album selection dialogs:** before the album-selection plan runs, read `AF`/`HomeActivity` and the rename, cover and add-to dialog layouts in the reference. Bind their string keys first. Settle when Change cover / Rename are enabled with several albums selected, and when Pin reads "Unpin".
- **Colours:** add light and dark hex values for `cropIconColor`, `adsBgColor` and `adsBgBorderColor` from R272 `values(-night)/colors.xml`.
- **Drawables:** check that `ic_album_select_pinned`, `ic_album_select_cover`, `ic_rename` and `ic_add` exist. Import any that are missing from R272.
