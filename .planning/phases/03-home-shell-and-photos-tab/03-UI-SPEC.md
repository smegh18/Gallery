---
phase: "3"
slug: "home-shell-and-photos-tab"
status: draft
shadcn_initialized: false
preset: none
created: "2026-11-14"
scope: "Phase 3 Home shell (activity_home, home_shell_popup_menu, layout_home_vault_hint, exit popup) and Photos tab (fragment_all_media, photos_tab_item_*); HOME-01..03, PHO-01; checklist #6, #7, #10"
---

# Phase 3 — UI Design Contract (Home Shell and Photos Tab)

> Parity port, not a new design. Every value below comes from the decompiled reference 2.7.2
> (layout XML for looks, smali for behaviour) and carries its source. Nothing here is a preference.
> Tokens (sdp/ssp, Inter, `GalleryText.*`, colour names, radii) are defined in the Phase 1 contract
> and are only referenced here.

**Evidence roots (read-only):**
- `R272` = `%TEMP%\ref272\dec\res` (2.7.2), `R267` = `%TEMP%\ref267\dec\res` (2.6.7)
- `S272` = `%TEMP%\ref272\dec\smali\albums\gallery\photo\folder\picasa\app\web\gallery`
- `HA` = `S272/presentation/home/view/activity/HomeActivity.smali`; `PF` = `S272/ca/br.smali` (Photos fragment);
  `PA` = `S272/ka/j.smali` (Photos adapter); `PH` = `S272/ka/h.smali` (grid view holder); `PD` = `S272/ka/i.smali` (grid spacing)
- Ours = `app/src/main/java/com/quizango/gallery/ui/{home,components}/`, `app/src/main/res/`

**Phase 1 token contract:** `.planning/phases/01-foundation/01-UI-SPEC.md` — `N.sdp`, `N.ssp`, `GalleryText.*`,
every colour name (`home_shell_*`, `photos_tab_*`, `backgroundColor`, `titleColor`, `colorPrimary` …) and the corner-radius
table. Use them; do not redefine. Reference `_Nsdp` → `N.sdp`, `_Nssp` → `N.ssp`, plain `dip` → unscaled `N.dp`.

---

## Design System

| Property | Value |
|----------|-------|
| Tool | none (Android, Jetpack Compose; shadcn not applicable) |
| Preset | not applicable |
| Component library | Compose Material 3 (Phase 1 contract); house components in `ui/components/` |
| Icon library | Reference vector drawables via `painterResource` (Phase 1 contract). No `material-icons` for any glyph the reference ships |
| Font | Inter (Phase 1 contract) |
| Animation | Lottie `lottie-compose` (`R.raw.zoom_in_out_gesture`, already imported) |

Component Inventory section omitted: `Tool: none` (no registry to enumerate; same as Phase 1).

---

## 1. Home shell (`R272/layout/activity_home.xml` CHANGED — checklist #6, HOME-01)

### 1.1 Structural changes from 2.6.7 (do these first)

| Change | Evidence | Action in ours (`ui/home/HomeScreen.kt`) |
|---|---|---|
| Navigation drawer removed | `R267/activity_home` has `drawerLayout`, `navBtn`, `lockIcon`, `mediaShortBtn`, `homeDivider`; `R272/activity_home` has none | Delete `ModalNavigationDrawer` / `GalleryDrawer` and the hamburger from Home. Recycle Bin and Settings move to the ⋮ popup (§1.5); the rest live in Explore (Phase 4) |
| Tab order is Photos / Albums / Explore, default Photos | `HA` `<clinit>`: `HOME_TAB_ORDER = [1, 0, 2]`; constructor `activeTabNumber = 1`. Logical ids: 0 = Album, 1 = Photos, 2 = Explore; pager page = index in `HOME_TAB_ORDER` | Pager pages: 0 = Photos, 1 = Albums, 2 = Explore. `HomeViewModel.selectedTab` default = Photos page. Today ours has 0 = Albums, 1 = Photos: reorder |
| Swipe between tabs enabled, offscreen limit 2 | `HA` onCreate `setUserInputEnabled(true)`, `setOffscreenPageLimit(2)`; disabled while selecting (`HA` `o0000o0`) | `HorizontalPager(userScrollEnabled = !isSelectionMode, beyondViewportPageCount = 2)` |
| Title "GALLERY" + lock icon gone; per-tab title | `HA.o000o0oO(I)` | See §1.3 |

### 1.2 Layout (top to bottom)

| Element | Property | Value | Reference |
|---|---|---|---|
| Root | background | `backgroundColor` | `homeRoot` |
| System nav-bar area | fill | `home_shell_tab_bg` (edge-to-edge: the area behind the gesture/nav bar under the tab bar uses the tab colour) | `navBarBg` |
| Top bar (`toolBar`) | height / bg / padding | 42.sdp / `backgroundColor` / end 7.sdp; content vertically centred | `toolBar` |
| Content | — | `HorizontalPager` filling the space between top bar and bottom bar | `homePager` |
| Sync progress | look | indeterminate linear bar at the top edge of the content, 5.sdp tall drawn with -3.dp top / -7.dp bottom margin (a thin line), `colorPrimary`, visible only while syncing | `dataSync` |
| Bottom bar (`bottomMainBar`) | order | banner-ad slot, then tab bar; overlays the pager bottom (pager content uses bottom padding, see §2.2) | `bottomMainBar` |
| Banner-ad slot | look | collapsed (0 height) placeholder — no shimmer exists in this slot in the reference, so the no-fill state is empty (ads decision D5) | `rl_banner_ad` / `fl_ad` |
| Tab bar | bg / divider | `home_shell_tab_bg`; 1.dp top divider `home_shell_tab_divider` | `segmentedViewLay`, `tabBarDivider` |
| Tab row | height | 44.sdp, 3 equal-weight cells | row in `segmentedViewLay` |
| Tab cell | content | column, centred horizontally, top padding 7.sdp; icon 19.sdp; label marginTop 4.sdp, horizontal padding 3.sdp, 10.ssp, 1 line ellipsize end; borderless ripple | `allMediaLay`/`albumsLay`/`exploreLay` |
| Tab icons | selected / unselected | Photos `ic_tab_photos_fill` / `ic_tab_photos_outline`; Albums `ic_tab_albums_fill` / `_outline`; Explore `ic_tab_explore_fill` / `_outline` | `HA.o000Oo00(IZ)` |
| Tab label colour | selected / unselected | `home_shell_tab_selected` (#0066FF) / `home_shell_tab_unselected` | `HA.o000Oo00`, colours 0x7f06019c/d |
| Tab label weight | all three | Inter SemiBold (W600). Reference quirk: `HA.o000Oo0O` applies `tabTypefaceSelected` (`if_inter_semi_bold`) to every label on every tab change, and it runs at start, so the XML Medium on Albums/Explore never shows | `HA.o000Oo0O` |
| Bottom-bar scroll behaviour | — | Bar (banner slot + tabs) translates down with content scroll (follows the nested-scroll delta, clamped to its height), settles to fully shown or hidden 220 ms after scrolling stops, and snaps back to shown on tab change | `S272/custom/HomeBottomBarBehavior.smali` (`SETTLE_MS = 0xdc`), `S272/bw/ap.smali` |

### 1.3 Top bar per tab (`HA.o000o0oO(I)`)

| Slot (left → right) | Look | Photos | Albums | Explore | Action |
|---|---|---|---|---|---|
| Title (`title` in `hideLay`) | marginStart 13.sdp, 16.ssp Inter SemiBold, `home_shell_title`, 1 line ellipsize | "Photos" `home_shell_photos` | "Album" `home_shell_album` | "Explore" `explore` | Long-press: haptic, close vault hint, open Vault lock (`HA.o000OO0o` → `VaultLockActivity`) |
| Spacer | weight 1 | ✓ | ✓ | ✓ | — |
| Ad-free `ivPro` | 32.sdp box, 7.sdp padding, `home_shell_ic_ad_free`, borderless ripple, no content description | ✓ | ✓ | ✓ | Opens the Pro/subscription screen (`HA.Ooooo00`). Phase 9 builds it: in Phase 3 the tap is a no-op hook (do NOT route to About as today) |
| Add album `addAlbumBtn` | same box, `home_shell_ic_add`, cd `create_album` | — | ✓ (needs media permission) | — | Create-album dialog (§3) |
| Search `searchBtn` | same box, `home_shell_ic_search`, cd `search` | ✓ | ✓ | ✓ | Search screen |
| Camera `cameraBtn` | same box, `home_shell_ic_camera`, cd `camera` | ✓ | — | — | `MediaStore.INTENT_ACTION_STILL_IMAGE_CAMERA` |
| More `moreBtn` | same box, `home_shell_ic_more`, cd `home_shell_more_options` | ✓ (needs media permission) | ✓ (needs media permission) | — | ⋮ popup (§1.5) |

All top-bar slots hide in selection mode (§2.6). Glyph size = 32 − 2×7 = 18.sdp. Ad-free is visible whenever the user is not premium, which in ours is always (no billing).

### 1.4 Vault hint (`R272/layout/layout_home_vault_hint.xml` NEW — HOME-02)

**When it shows** (`HA.o000o0O0`, defaults from `R272/xml/default_remote_config.xml` `vault_hint_tooltip`). All must hold:
1. App session number is 4, 8 or 12 (`"session": [4,8,12]`). A session = one cold app launch; ours needs a `sessionCount` preference incremented once per cold start (starts at 1).
2. `isVaultHintToolTip` is false — set true when the vault lock screen is opened (`S272/presentation/vaultlock/view/VaultLockActivity.smali:8858`); after that it never shows again.
3. `vaultHintClosedSession` ≠ current session (closed once per session).
4. Home was not opened from a notification, widget or deep link (`HA.o000O00O`).
5. Media permission granted, the notification-permission request is not running, and no other Home dialog is up (permission dialog, floating-button dialog Phase 10, usage-access popup Phase 12). If gated by 5 only, retry when that dialog closes (`isVaultHintShowPending`).

**Look:**

| Element | Value | Reference |
|---|---|---|
| Scrim | full screen over the whole Home (top bar and bottom bar included), `hint_home` #E5000000, consumes touches | `vaultHintLayout`, elevation 9.sdp |
| Title pill | background `ic_hint_home` (white rounded rect, 20% white ring, blue lock glyph at the right end); padding 6.sdp h / 4.sdp v; minHeight 24.sdp | `vaultHintTitleLay` |
| Pill text | the current top-bar title text (Photos / Album / Explore), colour #1F3355 (hard-coded `-0xe0ccab`), same typeface and size as the title (16.ssp SemiBold), marginEnd 10.sdp (room for the lock) | `HA.o000OoOo`, `HA.Oooooo` |
| Pill position | placed so the pill text sits exactly over the toolbar title (measured from the title's window position; RTL mirrored). XML fallback: marginStart 40.sdp, marginTop 5.sdp | `HA.Oooooo` |
| Arrow + copy block | below the pill, aligned to its start + 16.sdp: `ic_vault_hint_arrow` (paddingBottom 6.sdp), then to its end a column: "Hold Gallery to open Private Vault" `vault_hint_title` `GalleryText.BodyMediumMedium` `onlyWhite` (marginTop 4.sdp), "Secure your hidden photos here" `vault_hint_description` `GalleryText.BodySmallRegular` `onlyWhite` | `vaultHintArrow`, `vaultHintTitle`, `sec_txt` |

**Interaction:** tap anywhere on the scrim → hide and record `vaultHintClosedSession` = current session (`S272/bw/i.smali` case 1, `HA.o000O0O`). Long-press the pill → same as long-pressing the title (haptic, close, open Vault lock) (`S272/bw/l.smali`). The pill's literal "Gallery" in the copy is reference wording; keep it even though the title reads "Photos".

### 1.5 ⋮ popup menu (`R272/layout/home_shell_popup_menu.xml` NEW — HOME-01)

| Element | Value | Reference |
|---|---|---|
| Container | `Popup` anchored under `moreBtn`, end-aligned; x offset −8.sdp (+8.sdp in RTL), y offset +8.sdp; background `home_shell_popup_bg` (radius 10.sdp, clipped); elevation 8.sdp; minWidth 120.sdp; vertical padding 3.sdp; outside tap dismisses | `HA.OoooooO` (`showAsDropDown(moreBtn, ∓_8sdp, _8sdp, END)`, `setElevation(_8sdp)`) |
| Row | height 37.sdp, paddingStart 12.sdp, paddingEnd 17.sdp, ripple `home_shell_popup_ripple` | `homeMenu*` |
| Row icon | 16.sdp, decorative (no content description) | — |
| Row text | 11.ssp Inter Medium, `home_shell_popup_text`, 1 line, marginStart 10.sdp | — |

| Order | Row | Icon | Copy | Shown on | Action (smali) |
|---|---|---|---|---|---|
| 1 | Sort | `home_shell_ic_sort` | "Sort" `home_shell_sort` | Photos only (`HA.OoooooO`: visible iff `activeTabNumber == 1`) | Dismiss, open the Sort dialog `dialog_sort` (§3) (`S272/bw/b.smali` case 0 → `S272/bz/f`) |
| 2 | Recycle Bin | `home_shell_ic_recycle` | "Recycle Bin" `recycle_bin` | Photos, Albums | Dismiss, open Recycle Bin (`S272/bw/c.smali` case 0) |
| 3 | Settings | `home_shell_ic_settings` | "Settings" `settings` | Photos, Albums | Dismiss, open Settings (`S272/bw/c.smali` case 1) |

The popup is dismissed when selection mode starts or the Explore tab is selected (`HA.o000o0oO`).

### 1.6 Exit popup (Back on Home — checklist #10, HOME-03)

**Correction to the checklist:** `dialog_exit_popup` (CHANGED only in font: `@font/font` → `inter` / `if_inter_medium`) is the photo editor's "Exit without saving?" dialog (`S272/me/d.smali`, used by `PhotoEditActivity`). It belongs to Phase 8. The Home exit uses `R272/layout/dialog_exit.xml` (same as 2.6.7) through `S272/jt/a.smali`.

**Back order on Home:** selection mode → clear selection; ⋮ popup open → dismiss; otherwise the exit popup.

| Element | Value | Reference |
|---|---|---|
| Window | bottom-anchored dialog, full width, wrap height, transparent window bg, system dim, cancelable | `jt/a.onCreate` (gravity 0x50, `setLayout(-1,-2)`) |
| Sheet | `exit_rectangle`: `exitViewBackgroundColor` #FFFFFF / dark #131111, top corners 25.dp (unscaled); padding 4.dp | `rl_exit_bg` |
| Ad slot | collapsed placeholder (the reference hides it with no network and has no shimmer here; D5) | `admob_native_template` |
| Exit text | "Tap again to exit" `tap_again_to_exit`; height 55.dp; centred; colour #9A9A9A; size `_15sdp` (a dp size: use `with(LocalDensity.current) { 15.sdp.toSp() }`), Inter Regular | `txt_exit` |
| Tap exit text | close the app (`finishAffinity`) | `jt/a.Wja3o2vx62` |
| Back / tap outside | dismiss, stay on Home | `jt/a.HISPj7KHQ7` |

---

## 2. Photos tab (`R272/layout/fragment_all_media.xml` CHANGED; `photos_tab_item_date/grid/ad` NEW — checklist #7, PHO-01)

### 2.1 List composition

The list is one lazy grid of three row kinds, in this order: **ad row** at index 0 (above the first date header), then for each day a **date header** followed by that day's **grid items** (`PA.getItemViewType`: String → date, model named "AD" → ad, else grid; `PF` puts the ad model at index 0).

### 2.2 Grid geometry (`PD` = `ka/i.smali`, constructed in `PF` with `_13sdp`, `_4sdp`)

| Property | Value |
|---|---|
| Columns | default 3, min 3, max 5 (`PF` fields `currentSpanCount=3`, `minSpanCount=3`, `maxSpanCount=5`); persisted as `mediaColCount` (`PF.OooOO0`). Ours today: default 4, clamp 2..5 → change to 3 and 3..5 |
| Outer edge | 13.sdp left and right of the grid |
| Gap | 4.sdp between columns; 4.sdp between rows (2.sdp top + 2.sdp bottom per cell). Cells are equal width: `(contentWidth − 2×13.sdp − (n−1)×4.sdp) / n` |
| Grid width | the full pager page width. Known bug to fix: the All Media grid currently draws only in the left half of the width |
| List bottom padding | 50.sdp, content draws under it (clipToPadding false) so the last row clears the bottom bar | `allMediaRecycler` |
| Fast scroller | thumb `ic_scroll` (import from `R272/drawable/ic_scroll.xml`) on the end edge, elevation 8.sdp, draggable; tint `fastScrollerColor` (#0066FF light, #FFFFFF dark); date bubble per `fastscroller_popup` (min 56.dp, padding 16.dp) | `fastScroller`, `layout_fastscroller_track_thumb`, `fastscroller_popup` |

### 2.3 Date header (`photos_tab_item_date`)

| Property | Value |
|---|---|
| Padding | top 8.sdp, bottom 6.sdp; start/end 13.sdp (the `PD` edge inset) |
| Text | 10.ssp Inter Medium, `photos_tab_header_text`, 1 line ellipsize end, start-aligned |
| Label (`PA.eyd3OXAZgV`) | same day as today → "Today" `today`; yesterday → "Yesterday" `yesterday`; same year → `DateFormat.getBestDateTimePattern(locale, "dMMM")` (en-US "Nov 5"); other year → best pattern `"dMMMyyyy"` (en-US "Nov 5, 2023"). Group key per calendar day (`DD_MMM_YYYY` = "dd MMM yyyy"). Labels follow the device locale |

Ours today: 13.sp Normal black/white, fixed "dd MMM yyyy" English, hard-coded "Today"/"Yesterday" → replace.

### 2.4 Grid item (`photos_tab_item_grid`, bind in `PH`)

| Element | Value |
|---|---|
| Card | square; radius 3.sdp; background `photos_tab_thumb_bg`; no elevation |
| Thumbnail | fill, centre-crop; placeholder `image_loading_albums` while loading |
| Bottom-start badges | row at start 3.sdp, bottom 5.sdp: video `ic_common_video` 17.sdp (media type video) then favourite `ic_common_fav` 17.sdp (favourite). No duration text (ours shows duration: remove) |
| File-type label | top-start, margin 5.sdp: "GIF" / "RAW" / "SVG" (`GIF`/`RAW`/`SVG` keys) for those media types; 8.ssp Inter SemiBold `onlyWhite`, text shadow #424242 radius 4; hidden otherwise |
| Selection mark | only in selection mode: top-end 17.sdp, margin 2.sdp: `ic_check` if selected, `ic_un_check` if not (`PH.DxDJysLV5r`). No tint overlay on the thumbnail (ours draws a 35% blue overlay: remove) |
| Tap | normal: open the viewer; selection mode: toggle |
| Long-press | enter selection mode with this item selected (§2.6) |

### 2.5 Ad row (`photos_tab_item_ad` + `photos_tab_native_ad_template`) — placeholder (D5)

Ads stay placeholders but keep their slot and size. The placeholder is the reference's own loading skeleton
(`adLib_gnt_id_vg_shimmer_main`), drawn static (no shimmer animation), not clickable, no "AD" tag, no text:

| Part | Value |
|---|---|
| Row | full width (no 13.sdp edge inset: `PD` gives ad rows an empty inset); padding top 10.sdp, bottom 8.sdp, start/end 18.sdp |
| Icon block | 22.sdp square, `photos_tab_shimmer_block` (radius 2.sdp) |
| Title bar | height 8.sdp, from icon end + 5.sdp to CTA start − 25.sdp, top-aligned with the icon |
| Subtitle bar | height 7.sdp, from title start to CTA start − 7.sdp, bottom-aligned with the icon |
| CTA pill | 50.sdp × 22.sdp, `photos_tab_ad_cta_bg`, top-end |
| Media block | full width × 132.sdp, colour `photos_tab_shimmer`, 7.sdp below the CTA |
| Divider | 1.dp `photos_tab_divider` under the row |

### 2.6 Selection mode (Photos) — top bar `activity_home`, bottom `R272/layout/layout_select_media_bottom.xml` (same as 2.6.7)

**Top bar** (`HA.o0000o0`): ad-free, title, search, camera, more hide; show:

| Element | Value |
|---|---|
| Close `backBtn` | `ic_close`, 32.sdp box, 8.sdp padding, marginStart 5.sdp, tint `home_shell_icon`, cd `cancel`; tap = exit selection |
| Count `selectTitle` | "%d item(s) selected" `items_selected`; 12.ssp Inter SemiBold `home_shell_title`, marginStart 3.sdp, weight 1, 1 line ellipsize |
| Select all `mediaSelectAllBtn` | "Select all" `select_all` / "Unselect all" `unselect_all` when everything is selected; `GalleryText.BodySmallMedium`, `colorPrimary`, padding 3.sdp, marginEnd 5.sdp |

**Bottom bar:** replaces the tab bar (`segmentedViewLay` gone). Background `select_album_bg` (`backgroundColor`, top corners 16.sdp), elevation 10.sdp, horizontally scrollable row centred, row padding 5.sdp h. Each action: column, padding 10.sdp v, borderless ripple, icon at intrinsic size, label `GalleryText.BodyXSmallMedium` `titleColor` marginTop 4.sdp.

| Order | Width | Icon | Label (key) | Action |
|---|---|---|---|---|
| 1 | 50.sdp | `ic_home_select_share` | "Share" `share` | system share sheet |
| 2 | 55.sdp | `ic_home_select_fav` | "Favorite" `favorite` | add all to favourites; if every selected item is already a favourite, remove them (`PF` loop over `isFavorite`) |
| 3 | 55.sdp | `ic_home_select_add` | "Add to" `add_to` | copy/move to album picker |
| 4 | 63.sdp | `ic_home_select_hide` | "Add to Private" `ad_to_private` | move to vault |
| 5 | 50.sdp | `ic_album_select_delete` | "Delete" `delete` | delete confirmation (§3) |
| 6 | 60.sdp | `ic_home_select_create_album` | "Create album" `create_albums` | create-album dialog (§3) |

Pager swipe is off while selecting. Back or ✕ clears the selection.

### 2.7 Pinch to change columns + first-run tutorial (`R272/layout/layout_pinch_zoom_in_out.xml`)

- **Pinch:** two-finger pinch on the grid changes the column count one step at a time within 3..5 (spread → fewer, pinch → more), animated, and saves `mediaColCount`.
- **Tutorial shows** when the Photos page becomes selected (first launch included), media permission is granted and `isPinchInOutToolTip` is false (`S272/bw/ap.smali`).

| Element | Value |
|---|---|
| Overlay | full Home (over top bar and bottom bar), `tooltip_bg` #CC202331, elevation 10.sdp, consumes touches |
| Lottie | `R.raw.zoom_in_out_gesture`, 140.sdp square, centred, autoplay, loop |
| Line 1 | "Zoom Out" `zoom_out`, `GalleryText.BodyXLargeSemiBold`, `onlyWhite`, centred, 5.sdp under the Lottie, 40.sdp side margins |
| Line 2 | "Change the number of columns" `change_number`, `GalleryText.BodyLargeSemiBold`, `onlyWhite`, centred, 5.sdp under line 1, 40.sdp side margins |
| GOT IT | bottom-centre, 15.sdp from the bottom; background `pinch_in_out_btn` (`dark_blue` #0066FF, radius 35.sdp, ripple `ripple_color`); padding 35.sdp h; text "GOT IT" `got_it` (the value is already upper case; no all-caps transform), `GalleryText.BodyMediumSemiBold`, `onlyWhite` |
| Dismiss | GOT IT only: hide and save `isPinchInOutToolTip = true` (`S272/bw/i.smali` case 0) |

### 2.8 States

| State | Look | Reference |
|---|---|---|
| Loading (before the first media query returns) | centred indeterminate circular progress, `colorPrimary`; no empty copy | `progressBar` in `fragment_all_media` |
| Empty (query returned nothing) | centred column: `ic_empty_img` at intrinsic size; "No items" `no_items` `GalleryText.BodyLargeSemiBold` `titleColor`, marginTop 20.sdp, marginBottom 10.sdp; "No media files here." `no_media_file` `GalleryText.BodySmallRegular` `titleColor`, centred, side margins 50.sdp | `emptyLayout` |
| Populated | §2.1–2.5 | — |

Ours today: empty image 160.dp, 18.sp title, `onSurfaceVariant` body, no loading state → replace.
**Albums tab loading (known bug, fixed here because the shell owns first load):** `R272/layout/fragment_album.xml` has no empty-state text at all, only a centred `colorPrimary` progress bar and shimmer. Remove the "No Albums Found" literal from `AlbumsTab.kt` (no such string exists in the reference) and show the centred progress until albums load. Full Albums content is Phase 4.

---

## 3. Dialogs reached from this phase (layouts unchanged in 2.7.2; Phase 3 swaps literals for keys)

| Dialog | Reference layout | Copy (keys) |
|---|---|---|
| Sort | `dialog_sort` | title "Sort by" `sort_by` (`BodyXLargeSemiBold`); "Last Modified" `last_modified`, "File Name" `filename`, "File Size" `file_size`, "Created Date" `created_date`; "Ascending Order" `ascending_order`, "Descending Order" `descending_order` (options `BodyMediumMedium`); "Cancel" `cancel`, "Ok" `ok` (`BodyMediumSemiBold`) |
| Create album | `dialog_create_album` | "Create an album" `create_an_album`; "Album Name" `album_name`; hint "Enter Album Name" `enter_album_name`; "Cancel" `cancel`; "OK" `caller_ok` |
| Delete (selection) | `dialog_delete` | 1 item: "Are you sure you want to delete 1 item?" `delete_dialog_permission`; N items: `deleted_title` + " N " + `deleted_items` ("Are you sure you want to delete 3 items?"); checkbox "Move to Recycle Bin" `delete_dialog_move`; "No" `no` / "Yes" `yes` |

Ours today moves selected items to the trash with no confirmation: add the confirmation.

---

## Spacing Scale

Phase 1 scale applies (sdp units: 2, 4, 8, 12, 16, 24, 32, 48). Values used in this phase, all exact reference values:
4.sdp (tab label gap, grid gap), 8.sdp (popup offsets, back-button padding, date header top), 12.sdp (popup row start), 16.sdp (vault-hint copy indent, popup icon, selection-bar top radius), 32.sdp (top-bar buttons), 48.sdp not used.

Exceptions (parity overrides multiples of 4, per Phase 1): 3.sdp, 5.sdp, 6.sdp, 7.sdp, 10.sdp, 13.sdp, 15.sdp, 17.sdp, 18.sdp, 19.sdp, 20.sdp, 22.sdp, 25.sdp, 35.sdp, 37.sdp, 40.sdp, 42.sdp, 44.sdp, 50.sdp, 55.sdp, 60.sdp, 63.sdp, 120.sdp, 132.sdp, 140.sdp; unscaled 1.dp, 4.dp, 25.dp, 55.dp. Never round them.

---

## Typography

All Inter, `includeFontPadding = false`, natural line height (Phase 1 rule).

| Role | Size | Weight | Line Height | Where |
|------|------|--------|-------------|-------|
| Body | 10ssp | 500 Medium | natural (~1.21) | date header, tab labels' base size, Select all |
| Label | 11ssp | 500 Medium | natural (~1.21) | popup rows, vault hint title |
| Heading | 16ssp | 600 SemiBold | natural (~1.21) | top-bar title, vault-hint pill text |
| Display | 14ssp | 600 SemiBold | natural (~1.21) | pinch "Zoom Out" |

Exception (parity, as in Phase 1): the reference also uses 13ssp/600 (pinch line 2, empty title), 12ssp/600 (selection count), 11ssp/600 (GOT IT), 10ssp/600 (tab labels), 10ssp/400 (empty body, hint description), 8ssp/500 (selection labels), 8ssp/600 (file type), and `_15sdp` 400 (exit text). Weights in use: 400, 500, 600. The table in each section is binding.

---

## Color

Phase 1 roles, applied here:

| Role | Value | Usage |
|------|-------|-------|
| Dominant (60%) | `backgroundColor` #FFFFFF / #0C0C0C | Home root, top bar, Photos list, selection bar |
| Secondary (30%) | `home_shell_tab_bg` #F7F7F7 / #1C1C1E; `home_shell_popup_bg` #FFFFFF / #2A2A2C; `photos_tab_thumb_bg` #EEF0F3 / #1A1C1F | tab bar and nav-bar area, ⋮ popup, thumbnail backing |
| Accent (10%) | #0066FF (`colorPrimary`, `home_shell_tab_selected`, `dark_blue`) | reserved list below |
| Destructive | `red` #E11A1A (Phase 1) | delete confirmation only |

Accent reserved for: selected tab icon and label; "Select all"/"Unselect all" text; GOT IT button; the lock glyph inside `ic_hint_home`; fast-scroller thumb (light mode); loading spinner and sync bar. Not for titles, popup text, date headers or icons.

Fixed non-theme colours from the reference: scrims `hint_home` #E5000000 and `tooltip_bg` #CC202331; pill text #1F3355; exit text #9A9A9A; file-type shadow #424242.

**Colours to add** (missing in ours): `hint_home`, `tooltip_bg`, `exitViewBackgroundColor` (+ night #131111).
**Drawables to add** (copy from `R272/drawable/`): `ic_hint_home`, `ic_vault_hint_arrow`, `pinch_in_out_btn`, `exit_rectangle`, `ic_common_video`, `select_album_bg`, `ic_scroll`.

---

## Copywriting Contract

Every visible string is a reference key; translations come from the Phase 1 sync (`python -I scripts/ref_res.py sync-strings`, then `check-strings`). No hard-coded English remains in `HomeScreen.kt`, `PhotosTab.kt`, `AlbumsTab.kt` or `NavigationComponents.kt` (today: "GALLERY", "$selectedCount Selected", "Sort by", "Last Modified" …, "Create an album", "Album name", "Enter album name", "OK", "Ok", "Cancel", "Today", "Yesterday", "No Albums Found", "Share Media", and content descriptions "Private Vault", "Close Selection", "Back", "Menu", "Pro", "Search", "Camera", "Sort", "Create Album", "Video", "Favorite", "Selected", "Not selected").

| Element | Copy (reference key) |
|---------|------|
| Primary CTA | "GOT IT" (`got_it`, pinch tutorial); "Tap again to exit" (`tap_again_to_exit`, exit popup) |
| Empty state heading | "No items" (`no_items`) |
| Empty state body | "No media files here." (`no_media_file`) |
| Error state | none new in this phase; reuse Phase 1 "An issue occurred!" (`something_went_wrong`) if the media query fails |
| Destructive confirmation | Delete: "Are you sure you want to delete 1 item?" (`delete_dialog_permission`) or `deleted_title` N `deleted_items`, with "Move to Recycle Bin" (`delete_dialog_move`), "No" (`no`) / "Yes" (`yes`) |

**Keys to import** (missing in `values/strings.xml`; copy values from `R272/values/strings.xml`, then sync translations):
`albums`, `explore`, `camera`, `today`, `yesterday`, `items_selected`, `vault_hint_title`, `vault_hint_description`, `zoom_out`, `zoom_in`, `change_number`, `got_it`, `tap_again_to_exit`, `sort_by`, `last_modified`, `filename`, `file_size`, `created_date`, `ascending_order`, `descending_order`, `GIF`, `RAW`, `SVG`, `create_an_album`, `album_name`, `enter_album_name`, `caller_ok`, `deleted_title`, `deleted_items`.
Already present: `home_shell_photos`, `home_shell_album`, `home_shell_sort`, `home_shell_more_options`, `recycle_bin`, `settings`, `search`, `create_album`, `cancel`, `ok`, `no`, `yes`, `share`, `favorite`, `add_to`, `ad_to_private`, `delete`, `create_albums`, `select_all`, `unselect_all`, `no_items`, `no_media_file`, `delete_dialog_permission`, `delete_dialog_move`.

---

## UI Considerations

Applicable state considerations resolved: 9 covered, 6 backstop, 2 unresolved

| Category | Element(s) | Status | Resolution / Reason |
|----------|------------|--------|---------------------|
| loading | Photos tab, Albums tab first load | ✅ covered | Before the first media query returns, both tabs show only a centred `colorPrimary` spinner; "No items" and any "No Albums Found" text never appear during loading |
| empty | Photos tab | ✅ covered | An empty library shows `ic_empty_img`, "No items" and "No media files here." per §2.8 |
| populated | Photos grid | ✅ covered | The grid spans the full page width with 13.sdp edges and 4.sdp gaps, 3 columns by default |
| zero-one-many | selection count | ✅ covered | The selection title reads "%d item(s) selected" with the live count; Select all flips to Unselect all when every item is selected |
| zero-one-many | delete confirmation | ✅ covered | One item uses `delete_dialog_permission`; more than one uses `deleted_title` + count + `deleted_items` |
| locale | date headers, all shell copy | ✅ covered | Headers use Today/Yesterday keys and locale best patterns dMMM / dMMMyyyy; every label resolves from a reference key and translates on language change |
| permission | add-album and ⋮ buttons, vault hint, pinch tutorial | ✅ covered | Without media permission, + and ⋮ are hidden and neither overlay shows |
| tab | top-bar slots per tab | ✅ covered | Photos: ad-free, search, camera, ⋮; Albums: ad-free, +, search, ⋮; Explore: ad-free, search; Sort only in the Photos popup |
| first-run | pinch tutorial | ✅ covered | Shows once on the Photos page until GOT IT is tapped, then never again |
| overflow | top-bar title and selection count in long translations (de, ru, ta) | 🧪 backstop | Both are 1 line with end ellipsis; check no button is pushed off screen |
| overflow | selection bar on narrow screens and RTL (ar, fa, ur) | 🧪 backstop | The 6 actions scroll horizontally; popup x offset and vault-hint pill mirror in RTL |
| animation | pinch column change, bottom-bar hide on scroll | 🧪 backstop | Compare side by side with 2.7.2 on the phone (smoothness and 220 ms settle) |
| visual | fast scroller thumb and date bubble | 🧪 backstop | Thumb `ic_scroll` tinted `fastScrollerColor`; bubble look checked against 2.7.2 on device |
| visual | dialog_sort / dialog_create_album / dialog_delete layouts | 🧪 backstop | Copy is bound here; full layout parity of these unchanged dialogs is checked on device |
| dark | every element above | 🧪 backstop | Light and dark screenshots match 2.7.2 (scrims and pill text are fixed colours in both modes) |
| behaviour | exit popup vs direct exit | ⚠ unresolved | 2.7.2's bundled default config has `app_exit: "direct"`, which makes Back exit immediately with no popup; the popup only appears when remote config overrides it. This contract implements the popup (roadmap criterion 4, HOME-03). Phase 13 must confirm on the phone which one 2.7.2 shows; switch to direct exit if the phone exits directly |
| behaviour | ad-free button target | ⚠ unresolved | Opens the Pro screen, which Phase 9 builds; until then the tap is a no-op hook |

---

## Registry Safety

| Registry | Blocks Used | Safety Gate |
|----------|-------------|-------------|
| none (Android, no shadcn) | – | not applicable |

Asset provenance gate (Phase 1 rule): copy only from `R272/res`; no ad-SDK resources; brand-art decision of 2026-11-13 stands.

---

## Re-runnable provenance (2026-11-14)

- Binding field → view id: `ar/u.smali` fields follow the alphabetical id order of `activity_home` (checked against types; e.g. `OooO` = `ivPro`, `OooOO0o` = `moreBtn`, `cWbN6pumKk` = `cameraBtn`, `HISPj7KHQ7` = `addAlbumBtn`, `OooOo0o` = `title`).
- Resource ids: `grep 'id="0x7f…"' R272/values/public.xml` (e.g. 0x7f130741 `home_shell_photos`, 0x7f13073f `home_shell_album`, 0x7f13067c `explore`, 0x7f0d00e7 `home_shell_popup_menu`, 0x7f0d0093 `dialog_exit`).
- Remote-config defaults: `R272/xml/default_remote_config.xml` (`vault_hint_tooltip`, `app_exit`, `home.exit_dialog`, `all_media.native`).
- Missing keys/assets in ours: `grep -q 'name="<key>"' app/src/main/res/values/strings.xml` and `ls app/src/main/res/drawable/<name>.*`.

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
