---
phase: "1"
slug: "foundation"
status: approved
reviewed_at: 2026-11-13
shadcn_initialized: false
preset: none
created: "2026-11-13"
scope: "milestone v2.0 design-token contract (Phases 1-13 reuse it)"
---

# Phase 1 — UI Design Contract (milestone v2.0 token contract)

> Parity port, not a new design. Every token below is copied from the decompiled reference 2.7.2
> and carries its reference resource name and source file. Nothing here is a design preference.
> Later phases (2-13) reuse this file as their token source; their own UI-SPECs only add per-screen layout.

**Reference roots (read-only evidence):**
- `R272` = `%TEMP%\ref272\dec\res` (2.7.2, versionCode 152)
- `R267` = `%TEMP%\ref267\dec\res` (2.6.7, our current baseline)
- Ours = `app/src/main/res` and `app/src/main/java/com/quizango/gallery/ui/theme/`

**Rule for every later phase:** when a reference layout says `@dimen/_Nsdp`, `@dimen/_Nssp`, `@color/x`
or `@style/BodyY`, the Compose port uses the token of the same name from this file. Never hand-convert
sdp to a dp literal (current code has inconsistent 1.2x/1.25x/1.3x guesses in comments).

---

## Design System

| Property | Value |
|----------|-------|
| Tool | none (native Android, Jetpack Compose; shadcn not applicable) |
| Preset | not applicable |
| Component library | Compose Material 3 (`material3` 1.3.1, Compose BOM 2025.02.00, from `gradle/libs.versions.toml`); house components in `ui/components/` (`BannerAdView.kt`, `NativeAdCard.kt`, `NavigationComponents.kt`) |
| Icon library | Reference vector drawables copied into `res/drawable`, drawn with `painterResource(R.drawable.x)`. No icon font, no `material-icons-extended` for any icon the reference ships as a drawable |
| Font | Inter (reference `R272/font/if_inter_{light,regular,medium,semi_bold,bold}.ttf`, family `R272/font/inter.xml`, weights 300/400/500/600/700). 2.7.2 made Inter the app-wide default: every theme now inherits `Base.Inter.*` whose `TextAppearance.Inter` sets `fontFamily=@font/inter` (`R272/values/styles.xml`, 277 `TextAppearance.Inter*` entries). Inter is SIL OFL, so the files may be copied |
| Animation | Lottie (`lottie-compose` 6.6.2), JSON and dotLottie (`.lottie`) from `R272/raw` |

Component Inventory section omitted: `Tool: none` (no shadcn-style registry to enumerate).

---

## Spacing Scale (sdp/ssp contract)

### How the reference sizes things

The reference uses the `sdp`/`ssp` libraries. `R272/values/dimens.xml` holds the base (`_10sdp` = 10dp,
`_10ssp` = 10sp) and `R272/values-sw330dp` … `values-sw1080dp` scale every entry by the smallest-width bucket:

| Smallest width | Factor | Example `_10sdp` (source) |
|---|---|---|
| < 330dp | 1.0 | 10dp (`values/dimens.xml`) |
| 330dp | 1.1 | 11dp (`values-sw330dp/dimens.xml`) |
| **360dp (test phone M2006C3MII, 720x1600 @320dpi)** | **1.2** | **12dp** (`values-sw360dp/dimens.xml`) |
| 390dp | 1.3 | 13dp |
| 420dp | 1.4 | 14dp |
| 600dp | 2.0 | 20dp |
| 1080dp | 3.6 | 36dp |

Formula (matches every bucket file): `factor = clamp(floor(sw / 30) * 30, 300, 1080) / 300`.
`ssp` uses the same factor (`values-sw360dp`: `_10ssp` = 12sp, `_12ssp` = 14.4sp). Plain `dip`/`sp` values in reference XML (e.g. `26.0dip`, `100.0dip`) are NOT scaled.

### How we map it in Compose (Phase 1 creates this, every later phase uses it)

New file `ui/theme/Dimens.kt` (the only new theme abstraction in this phase):

```kotlin
private fun sdpFactor(sw: Int) = (sw / 30 * 30).coerceIn(300, 1080) / 300f
val Int.sdp: Dp @Composable @ReadOnlyComposable get() = (this * sdpFactor(LocalConfiguration.current.smallestScreenWidthDp)).dp
val Int.ssp: TextUnit @Composable @ReadOnlyComposable get() = (this * sdpFactor(LocalConfiguration.current.smallestScreenWidthDp)).sp
```

Reference `@dimen/_12sdp` → `12.sdp`; `@dimen/_10ssp` → `10.ssp`; reference `26.0dip` → `26.dp` (unscaled).
Needs one unit test covering buckets 320→1.0, 360→1.2, 411→1.3, 1200→3.6.

**XML resources:** imported drawables reference `@dimen/_Nsdp` directly (e.g. `explore_card_bg` radius `_13sdp`).
Copy the `_Nsdp`/`_Nssp` entries of `R272/values-sw330dp … values-sw1080dp/dimens.xml` into matching
`values-swNNNdp/dimens.xml` folders in ours (our `values/dimens.xml` already holds the 1.0 base). Without
these, every imported drawable renders 1/1.2 too small on the test phone.

### Declared scale (sdp units; real dp = value x factor)

| Token | Value | Usage (frequency in the 23 new/changed 2.7.2 layouts) |
|-------|-------|-------|
| xxs | 2.sdp | Tag/shimmer radius, label top gap (17 uses) |
| xs | 4.sdp | Icon-to-text gaps, grid gutters (43 uses) |
| sm | 8.sdp | Compact spacing, toolbar icon padding (20 uses) |
| md | 12.sdp | Default screen/card horizontal padding (66 uses, the most common) |
| lg | 16.sdp | Section label top margin (`ExploreSectionLabel`), popup icon size (12 uses) |
| xl | 24.sdp | Sheet padding |
| 2xl | 32.sdp | Toolbar icon button box (`activity_home` all 32sdp) |
| 3xl | 48.sdp | Large block gaps |

Exceptions (parity overrides the multiple-of-4 rule; use the exact reference value, never round):
`3.sdp` (27 uses), `5.sdp` (11), `6.sdp` (25), `7.sdp` (20, toolbar icon padding), `9.sdp` (10), `10.sdp` (33),
`13.sdp`, `14.sdp`, `18.sdp`, `20.sdp`, `22.sdp`, `30.sdp`, `40.sdp`, and unscaled `2/7/8/10/12/14.dp` where the
reference writes `dip`. Source: `grep -oE '(layout_margin|padding)[A-Za-z]*="[^"]*"'` over `R272/layout/{activity_home,home_shell_popup_menu,explore_tab_*,photos_tab_*,item_home_album_card,…}.xml`.

### Corner radii (from the reference drawable/layout that owns them)

| Radius | Where (reference source) |
|---|---|
| 2.sdp | `explore_ad_tag_bg`, `explore_ad_shimmer_block`, `photos_tab_shimmer_block`; `photos_tab_ad_tag_bg` = 2.dp unscaled |
| 3.sdp | `bg_usage_grabber`, `bg_floating_intro_dot` |
| 5.sdp | `explore_ad_cta_bg`, `explore_ad_shimmer_media/cta`, `bg_floating_sidebar_handle` |
| 6.sdp | `bg_gs_more_row` |
| 9.sdp | `explore_chip_bg` |
| 10.sdp | `home_shell_popup_bg`, `bg_gs2_status`; `cardCornerRadius` (16 layouts) |
| 12.sdp | `bg_floating_sidebar_icon`, `bg_gs2_open_btn` |
| 13.sdp | Explore cards: `explore_card_bg`, `explore_hero_bg`, `explore_ad_card_bg` |
| 14.sdp | `bg_usage_allow_settings`, `bg_usage_not_now`, `bg_floating_intro_drag_pill`; `cardCornerRadius` (16 layouts) |
| 18.sdp top | `bg_floating_bottom_sheet` |
| 20.sdp | `bg_usage_eyebrow_settings` |
| 24.dp top (unscaled) | `bg_onb_feature_sheet` |
| 28.sdp top | `bg_usage_sheet` |
| 26.dp (unscaled) | `bg_media_full_pill_dock`, `bg_iu_open_button` |
| 42.sdp / 50.sdp / 100.dp | Pills: `photos_tab_ad_cta_bg`, `explore_hero_btn_bg`, `bg_onb_feature_chip`, `bg_iu_*_cta` → use `CircleShape`/`RoundedCornerShape(50)` |

`GalleryShapes` in `Shape.kt` stays unchanged in Phase 1 (existing screens depend on it). Ported screens use
`RoundedCornerShape(N.sdp)` with N from this table, or draw the imported drawable itself.

### Icon sizes

| Size | Where (reference layout) |
|---|---|
| 32.sdp box, 7.sdp padding (18.sdp glyph) | Home top-bar buttons: Pro, add album, search, camera, more, album info (`activity_home`); back button uses 8.sdp padding |
| 19.sdp | Bottom tab icons `imgAllMedia/imgAlbums/imgExplore` (`activity_home`); Explore tile icon (`explore_tab_item_tile`) |
| 17.sdp | Grid overlays play/favourite/selected (`photos_tab_item_grid`); Explore feature icon (`explore_tab_item_feature`) |
| 16.sdp | Popup menu icons (`home_shell_popup_menu`); row chevron (`explore_tab_item_row`); sidebar badge (`item_floating_sidebar_action`) |
| 12.sdp / 18.sdp | Album card type badge / selected tick (`item_home_album_card`) |
| 44.sdp | Explore hero illustration (`explore_tab_hero`) |

Vector intrinsic size is 24dp (16dp for `album_tab_ic_*`); always size by the layout value above, not the intrinsic.

---

## Typography

### Font family

`ui/theme/Type.kt` gets `val Inter = FontFamily(Font(R.font.if_inter_light, W300), Font(R.font.if_inter_regular, W400), Font(R.font.if_inter_medium, W500), Font(R.font.if_inter_semi_bold, W600), Font(R.font.if_inter_bold, W700))`.
Copy the five `.ttf` files from `R272/font/` into `res/font/` (same names). Roboto is NOT needed: 2.7.2 uses it only in the cross-promo `dialog_app_redirector` / `dialog_round_app_redirector` (ad surfaces, not ported).

### Reference text styles → Compose (`R272/values/styles.xml`)

All share: `fontFamily=Inter`, `includeFontPadding=false`, no `lineSpacing`, no letter spacing.
Compose: `TextStyle(fontFamily = Inter, fontWeight = W, fontSize = N.ssp, platformStyle = PlatformTextStyle(includeFontPadding = false))`,
`lineHeight` left Unspecified (Inter's natural line box, about 1.21 x size, which is what the reference renders).
Expose as composable getters on one object, e.g. `GalleryText.BodySmallSemiBold`.

| Reference style | Size | Weight(s) available | On test phone (x1.2) |
|---|---|---|---|
| BodyXXSmallMedium | 7ssp | 500 | 8.4sp |
| BodyXSmall{Regular,Medium,SemiBold,Bold} (+`BodyXSmallMediumGrid`) | 8ssp | 400/500/600/700 | 9.6sp |
| BodySmall{Regular,Medium,SemiBold,Bold} | 10ssp | 400/500/600/700 | 12sp |
| BodyMedium{Regular,Medium,SemiBold} | 11ssp | 400/500/600 | 13.2sp |
| BodyLarge{Regular,Medium,SemiBold,Bold} | 13ssp | 400/500/600/700 | 15.6sp |
| BodyXLarge{Medium,SemiBold,Bold} | 14ssp | 500/600/700 | 16.8sp |
| BodyXXLarge{Medium,SemiBold,Bold} | 16ssp | 500/600/700 | 19.2sp |

Only the 23 combinations listed exist; do not create others. Layouts that set `textSize` directly
(9, 12, 15, 17ssp seen in new layouts) use `N.ssp` with the layout's own `fontFamily` weight.

Composite styles new in 2.7.2 (port with the screen that uses them):
- `ExploreSectionLabel` = 10ssp Inter Medium, colour `explore_subtext`, margin top 16sdp / bottom 8sdp.
- `OnbFeatureChip` = BodySmallMedium, colour `iu_primary`, background `bg_onb_feature_chip`, 1 line.
- `FloatingSidebarItemLabel` = BodyXSmallRegular, colour `floating_sidebar_text`, centred, ellipsize end, top margin 2sdp.

### Roles used by this contract (checker summary)

| Role | Size | Weight | Line Height |
|------|------|--------|-------------|
| Body | 11ssp (BodyMedium) | 400 Regular | natural (~1.21) |
| Label | 10ssp (BodySmall) | 500 Medium | natural (~1.21) |
| Heading | 14ssp (BodyXLarge) | 600 SemiBold | natural (~1.21) |
| Display | 16ssp (BodyXXLarge) | 700 Bold | natural (~1.21) |

Exception (parity): the reference uses 7 sizes and 4 weights (400/500/600/700); 300 Light ships in the family but no 2.7.2 style uses it. The full table above is binding; the 4-row summary is only the dominant usage.

### Material 3 slots

Keep the sizes in `GalleryTypography` (existing screens rely on them, no-regression rule) but set
`fontFamily = Inter` on every slot. This mirrors 2.7.2, where the default text appearance became Inter
app-wide while explicit sizes stayed per layout.

---

## Color

Values below are exact. "Dark = light" means `values-night` has no override, so the light value applies in dark mode.
All imported XML drawables resolve these through `@color/…`, so each name a drawable references must exist in
our `values/colors.xml` AND (when overridden) `values-night/colors.xml` with the reference value.

### 60 / 30 / 10

| Role | Light | Dark | Reference name (source) | Usage |
|------|-------|------|------|-------|
| Dominant (60%) | #FFFFFF | #0C0C0C | `backgroundColor` (`R272/values`, `values-night/colors.xml`) | Screen background, top bars, Albums tab (`album_tab_bg` = `@color/backgroundColor`) |
| Secondary (30%) | #F6F7FB screen / #FFFFFF cards | #0C0C0C screen / #1A1B1F cards | `explore_screen_bg`, `explore_card_bg` | Explore screen and cards; also `dialogBack` #FFFFFF/#1E1E1E (dialogs, sheets), `home_shell_tab_bg` #F7F7F7/#1C1C1E (bottom tab bar) |
| Accent (10%) | #0066FF | #0066FF | `colorPrimary`, `mainAppColor` | See reserved list |
| Destructive | #E11A1A | #E11A1A | `red` / `cleaner_red` | Delete / permanently-delete buttons (`bg_rounded_red`, `dialog_cleaner_permanently_delete`); text-only delete uses `permanent_delete_red` #FF4646; sidebar remove badge uses `floating_sidebar_remove` #F2453D |

Accent reserved for: selected bottom-tab icon and label (`home_shell_tab_selected`), story ring (`album_tab_story_ring`),
Explore hero background (`explore_hero_bg`, dark #1F4FD6), ad CTA text (`explore_ad_cta`/`photos_tab_ad_cta`, dark #3D8BFF),
usage-access Allow button and eyebrow (`usage_settings_accent`), fast scroller and lock icon (`fastScrollerColor`, `ic_lock_color`, both white in dark),
floating sidebar tool icon background (`floating_sidebar_icon_bg`), splash loader track (`splash_loader_track` #290066FF).
Not for body text, dividers or generic icons.

### Breaking change in 2.7.2 (Phase 1 must apply)

| Name | 2.6.7 | 2.7.2 | Effect on ours |
|---|---|---|---|
| `colorPrimary` (light; night has no override) | #3478F6 | **#0066FF** | `res/values/colors.xml` `colorPrimary` #3478f6 → #0066ff. `Theme.kt` `DarkColorScheme.primary` = `GalleryPrimaryBlue` (#3478F6) → `GalleryBlueLight` (#0066FF), because night `mainAppColor` = `@color/colorPrimary`. #3478F6 survives only as `floating_sidebar_accent` and `gs_banner_text_color` |
| `gs_menu_card_color` | #EDF3FF / #2C2C2C | #F2F3F5 / #1E1E1E | Phase 11 |
| `gs_top_bg` | #D4E3FF / #131111 | #F5F5F7 / #1C1C1E | Phase 11 |

### Core roles (used across most layouts; unchanged from 2.6.7, already in `Color.kt`)

| Name (`R272/values/colors.xml`) | Light | Dark | Compose today |
|---|---|---|---|
| `titleColor` / `per_text_color` | #1E3354 | #FFFFFF | `TextPrimaryLight/Dark` ✓ |
| `radioButtonUncheckedColor` / `album_tab_count` light | #7A8389 | #7A8389 (count: #8E99A8) | `TextTertiaryDark` value |
| `dialogBack` | #FFFFFF | #1E1E1E | `GallerySurfaceLight/Dark` ✓ |
| `settings_divider` | #F2F9FF | #20FFFFFF | `SettingsDivider*` ✓ |
| `dividerColor` | #1A002847 | #20FFFFFF | add |
| `property_text` | #5C7A90 | #FFFFFF | XML only |
| `itemsColor` | #323232 | #323232 | XML only |
| `onlyWhite` / `whiteOnlyColor` | #FFFFFF | #FFFFFF | – |
| `white` / `black` | #FFFFFF / #000000 | **#000000 / #FFFFFF (inverted)** | beware: `black` is white in dark |
| `shimmerColorMedium` / `shimmerColorDark` | #E4E5E7 / #C7CACD | same | – |
| `ad_sub_title_color` | #A1A1A1 | #898888 | – |
| `op_onboarding_desc_color` | #7A8389 | #AAAAAA | – |
| `disableTabTextColor` | #000000 | #737376 | – |

### New colour groups in 2.7.2 (108 light, 70 night overrides). Import all app groups in Phase 1.

Command: `python -I resdiff.py R267/values/colors.xml R272/values/colors.xml color new` (same for `values-night`).

**Home shell** (Phase 3)

| Name | Light | Dark |
|---|---|---|
| home_shell_title / home_shell_icon / home_shell_popup_text | #1F3355 | #FFFFFF |
| home_shell_tab_bg | #F7F7F7 | #1C1C1E |
| home_shell_tab_divider | #F0F1F2 | #2C2C2E |
| home_shell_tab_icon_cutout | #FFFFFF | #1C1C1E |
| home_shell_tab_selected | #0066FF | #0066FF |
| home_shell_tab_unselected | #7A8389 | #8E8E93 |
| home_shell_popup_bg | #FFFFFF | #2A2A2C |
| home_shell_popup_ripple | #141F3355 | #1FFFFFFF |
| home_shell_ad_free_blue / _red | #64B9FC / #FB4A59 | same |

**Photos tab** (Phase 3)

| Name | Light | Dark |
|---|---|---|
| photos_tab_header_text | #1E3353 | #E4E7EB |
| photos_tab_divider | #EEF0F2 | #1E2024 |
| photos_tab_thumb_bg | #EEF0F3 | #1A1C1F |
| photos_tab_shimmer | #E4E5E7 | #24272B |
| photos_tab_ad_title | #1E3353 | #F2F3F5 |
| photos_tab_ad_subtitle | #8A9399 | #9AA1A8 |
| photos_tab_ad_tag | #6B6B6B | #A8ADB2 |
| photos_tab_ad_cta | #0066FF | #3D8BFF |
| photos_tab_ad_media_bg | #F2F4F6 | #16181B |

**Albums tab** (Phase 4)

| Name | Light | Dark |
|---|---|---|
| album_tab_bg | @color/backgroundColor | (follows) |
| album_tab_name | #1F3355 | #FFFFFF |
| album_tab_story_label | #2C4060 | #E6E9EE |
| album_tab_count | #7A8389 | #8E99A8 |
| album_tab_card_placeholder | #EEF0F3 | #1E2126 |
| album_tab_divider | #F2F3F5 | #1AFFFFFF |
| album_tab_story_ring | #0066FF | #0066FF |
| album_tab_icon | #FFFFFF | #FFFFFF |
| album_tab_deleted_dim | #8F000000 | same |

**Explore tab** (Phase 4)

| Name | Light | Dark |
|---|---|---|
| explore_screen_bg | #F6F7FB | #0C0C0C |
| explore_card_bg | #FFFFFF | #1A1B1F |
| explore_card_ripple | #14000000 | #1FFFFFFF |
| explore_divider | #E7E9F0 | #2A2C32 |
| explore_text | #14161C | #F2F3F5 |
| explore_subtext | #6B7080 | #9A9FAD |
| explore_hero_bg | #0066FF | #1F4FD6 |
| explore_hero_text / explore_hero_btn_bg | #FFFFFF | #FFFFFF |
| explore_hero_subtext | #D9E6FF | #C9D6FA |
| explore_hero_btn_text | #0047B3 | #0C2E8A |
| explore_tab_row_glyph | #1F3355 | #1F3355 |
| explore_tab_tile_glyph | #FFFFFF | #FFFFFF |
| explore_chip_ai_bg / _fg | #EEEDFE / #534AB7 | #3C3489 / #CECBF6 |
| explore_chip_collage_bg / _fg | #FBEAF0 / #993556 | #72243E / #F4C0D1 |
| explore_chip_favorites_bg / _fg | #FCEBEB / #A32D2D | #791F1F / #F7C1C1 |
| explore_chip_moments_bg / _fg | #FAEEDA / #854F0B | #633806 / #FAC775 |
| explore_chip_place_bg / _fg | #E6F1FB / #185FA5 | #0C447C / #B5D4F4 |
| explore_chip_vault_bg / _fg | #E1F5EE / #0F6E56 | #085041 / #9FE1CB |
| explore_ad_title / _body | #212226 / #A1A8B0 | #F2F3F5 / #8E959D |
| explore_ad_cta | #0066FF | #3D8BFF |
| explore_ad_media_bg / _shimmer | #F2F4F6 / #E4E5E7 | #1C1E21 / #24272B |
| explore_ad_tag_bg / _tag_text | #CED0D6 / #FFFFFF | #3A3D43 / #C9CDD3 |

**Full-screen viewer** (Phase 5): `media_full_glass_bg` #8C1C1D21, `media_full_glass_icon` #FFFFFF, `media_full_pill_bg` #E61C1D21, `media_full_pill_stroke` #26FFFFFF (no night overrides; viewer is always dark).

**Usage access** (Phase 12): `usage_sheet_bg` #FFFFFF, `usage_settings_accent` #0066FF, `usage_settings_accent_soft` #E1ECFF, `usage_subtitle` #6B7280, `usage_grabber` / `usage_not_now_stroke` #E5E7EB. No night overrides in `R272/values-night/colors.xml` (sheet stays white in dark, parity).

**Onboarding feature promo + install/update screens** (Phases 2, 11): `iu_bg` #FFFFFF/#121212, `iu_primary` #1073FE/#0169FB, `iu_primary_soft` #E5F2FF/#132A45, `iu_stroke` #EAEFF6/#2A2F38, `iu_button_bg` #EAECF0/#313135, `iu_text_primary` #101828/#F3F5F8, `iu_text_secondary` #667085/#98A2B3, `splash_loader_track` #290066FF.

**Floating sidebar** (Phase 10): `floating_sidebar_accent` #3478F6, `floating_sidebar_icon_bg` #0066FF, `floating_sidebar_panel_bg` #F7F7F8/#2B2B2E, `floating_sidebar_text` #1B1A1F/#FFFFFF, `floating_sidebar_subtext` #8A8A8E/#9E9EA3, `floating_sidebar_collapse_bg` #E8E8EB/#3D3D42, `floating_sidebar_collapse_icon` #8A8A8E/#BDBDC2, `floating_sidebar_handle` #D9F2F2F2, `floating_sidebar_handle_drag` #B3808080, `floating_sidebar_handle_stroke` #33000000, `floating_sidebar_remove` #F2453D.

**GS after-call / install** (Phase 11): `gs2_background` #FFFFFF/#0E110E, `gs2_open_btn_bg` #A3A8B1/#3A3F47, `gs2_progress_track` #FFFFFF/#2A3A52, `gs2_status_bg` #EDF4FF/#16263D, `gs2_subtitle` #9BA2AB/#8E9196, `gs_avatar_bg` #9E9EA3/#2C2C2E, `gs_call_green` #1DB954, `gs_header_button_bg` #FFFFFF/#1E1E20, `gs_message_blue` #0A66FF, `gs_tab_unselected` #8E8E93, plus pre-existing names the new drawables need: `callercad_screen_bg` #FFFFFF/#000000, `gs_banner_text_color` #3478F6/#7A8389, `ic_text_button` #FFFFFF/#131111.

**Not imported (ad SDK only, D5 placeholders):** `dismiss_icon_grey_500/700`, `im_browser_bg`, `im_browser_loader_bg`, `tt_bf161823`, `background_dark/light`.

Missing in ours today and referenced by imported drawables (must exist after Phase 1, 46 names): `album_tab_divider album_tab_icon black callercad_screen_bg floating_sidebar_* (8) gs2_open_btn_bg gs2_status_bg gs_avatar_bg gs_banner_text_color gs_call_green gs_menu_card_color gs_message_blue gs_top_bg home_shell_* (9) ic_text_button iu_button_bg iu_primary iu_primary_soft iu_stroke media_full_* (3) photos_tab_ad_cta photos_tab_ad_tag photos_tab_shimmer usage_* (5) whiteOnlyColor`.

---

## Drawables (FND-01)

Command: `comm -13 <(cd R267 && find drawable* -type f | sort) <(cd R272 && find drawable* -type f | sort)` → **111 new files (110 names;
`img_feature_promo_top` has a night variant)**; `cmp` over shared files → **5 changed**. The checklist's "107" is an approximation; this list is authoritative.

| Group | Files | Action |
|---|---|---|
| App vectors/shapes (explore_*, home_shell_*, photos_tab_*, album_tab_*, ic_tab_*_{fill,outline}, bg_usage_*, ic_usage_*, ic_setting_usage_data, bg_floating_*, ic_floating_sidebar_*, bg_onb_*, bg_media_full_*, bg_iu_*, bg_gs*, ic_gs*, ic_gs2_check_fill, ic_close_stripe) | 98 | Copy verbatim from `R272/drawable/`. 14 `explore_*` files already in ours differ from 2.7.2: overwrite them with the 2.7.2 copies |
| Changed: `bg_tab_indicator` (width 36→53sdp), `ic_vault_with_bg` (scale 0.75→1.1), `bg_floating_bottom_sheet` (fill `dialogBack`→`callercad_screen_bg`), `splash_bg` (centre logo removed: `@drawable/app_icon_pre_splash` → `@null`) | 4 | Copy 2.7.2 version; `splash_bg` also removes the reference logo from our window background |
| Changed `at_reward_icon` | 1 | Skip (ad SDK) |
| Ad-SDK only: `im_back`, `im_close_button`, `im_close_icon`, `im_close_transparent`, `im_forward_active`, `im_forward_inactive` (InMobi), `liftoff_icon_mute/unmute`, `logo_play_prism_24dp` | 9 | Do not import (ads are placeholders) |
| Raster art: `drawable-nodpi/img_feature_promo_top.webp` (1125x1005) + `drawable-night-nodpi/…`, `drawable-nodpi/ic_gs2_default_app.png` (195x204), `ic_gs2_success.png` (336x258) | 4 | Replace with our own art at the same pixel size and aspect (see brand register) |

Dark mode: all vectors take colour from `@color/…` names listed above, so they draw correctly in dark only after the colour import and the `values-swNNNdp` dimens import.

### Brand-art register (must be our own art, never the reference's)

| Asset | Status in ours today | Contract |
|---|---|---|
| Launcher icon `mipmap-*/ic_launcher_gallery.webp`, `ic_launcher_round_gallery.png`, `mipmap-anydpi-v26/ic_launcher_gallery.xml` | **byte-identical to the reference** (`cmp`) | Replace with our own icon, same densities and filenames |
| `drawable/app_icon_splash.png` (splash `iv_splash`, 140sdp) | **byte-identical to reference** | Replace with our own logo, same canvas |
| `drawable/about_gallery_logo.png` | **byte-identical to reference** | Replace with our own logo |
| `raw/app_icon_lottie.json` (layers "Gallery Appicon", "Gallery V2.0 Outlines") used by `SplashScreen.kt` | **byte-identical to reference** | Replace with our own logo animation, or the static `app_icon_splash` if no animation is made |
| `raw/anime_overlay.json` (2.7.2, overlay-guide dialog) | not in ours | Embeds the reference logo image ("gallery logo.png") and a "#Gallery" text layer: swap the embedded image for our icon and the text for our `app_name` when Phase 10/11 imports it |
| `img_feature_promo_top` (stock photos of people) | not in ours | Our own collage image, 1125x1005, rounded-tile grid look kept; night variant too |
| `ic_gs2_default_app`, `ic_gs2_success` | not in ours | Our own illustrations, same sizes |
| App name | ours `app_name` = "Gallery"; reference "#Gallery" | Every `%1$s` app-name placeholder (usage_access_*, gs1_overlay_disabled_text) is filled with our `app_name` |

The art itself is an open input: the user supplies it, or the planner adds a task to produce original art. Until then the reference files stay, and the phase is NOT complete (success criterion 1).

---

## Lottie (FND-04)

`R272/raw` vs `R267/raw` vs ours (`cmp`):

| Lottie | 2.7.2 status | Ours | Contract |
|---|---|---|---|
| cleaner_lottie, done_cleaner_lottie, duplicate_scanner_lottie, hand, permission, remove_bg_loader, scan_cleaner_lottie, success | same | identical to 2.7.2 | Keep |
| app_icon_lottie | same | identical | Brand art: replace (see register) |
| app_setings.json | **changed** (Phone permission mock + hand) | absent | Import now (`dialog_gs_permission_guide_anime`, Phase 11) |
| dm_success.lottie, junk_done.lottie (dotLottie) | used (`activity_duplicate_delete_complete`, `activity_remove_junk`) | absent | Import now; load with `LottieCompositionSpec.RawRes` (lottie 6.x reads `.lottie`) |
| hand_click, hand_left_arrow, hand_upside_arrow, loader_animation, moment_scanner_lottie, quick_oraganize, remove_bg_tooltip, remove_junk_button_animation, zoom_in_out_gesture, anime_storage | used by ported screens (collage, moments, organize intro, creative guides, pinch hint, storage guide) | absent | Import now so later phases never stop for an asset |
| anime_overlay | used (overlay guide) | absent | Import with brand swap |
| raw-night/duplicate_scanner_lottie | identical to light | – | Not needed |
| interstitial_ad_loading, *.js, keep_cronet_api.xml, slideshow_music.mp3 | ad/SDK/audio | – | Skip (mp3 only if Phase 4 stories prove they play it) |
| **cleaner_done_lottie** | **removed in 2.7.2** | shipped, used by `ui/cleaner/DuplicateCompleteScreen.kt:52` | Point that screen at the 2.7.2 equivalent (`success` for scan complete per `activity_duplicate_scan_complete`, `dm_success` for delete complete per `activity_duplicate_delete_complete`), then delete the file |
| moment_lottie, organizer_guide_lottie, org_album_touch, green_done, toggle.lottie, ad_loader | removed in 2.7.2 | not shipped | Nothing to do; never re-add |

---

## Copywriting Contract

All copy is the reference's exact English text from `R272/values/strings.xml`; translations come from
`R272/values-<lang>/strings.xml`. Emoji and capitalisation in reference copy (e.g. "👍", "⚠️", "PRIVACY FRIENDLY") are parity, not slop: the unslop audit records them as accepted.

| Element | Copy (reference key) |
|---------|------|
| Primary CTA | "Scan now" (`explore_scan_now`, Explore hero); onboarding "Enable features" (`onb_feature_enable`); usage "Allow" (`usage_access_allow`) |
| Secondary / dismiss | "Not now" (`onb_feature_skip`, `usage_access_not_now`); "Close" (`close`) |
| Empty state heading | "No Favorites" (`no_favorites`); generic "No items" (`no_items`) |
| Empty state body | "Tap on the heart icon to save your favorite media to quickly locate them." (`no_favorites_empty`); "No media files here." (`no_media_file`); cleaner: "No duplicate photos found. Your storage is clean and optimized.👍" (`cleaner_duplicate_empty_layout_text`) |
| Error state | "An issue occurred!" (`something_went_wrong`); "File not found!" (`file_not_found`); "Video not found." (`video_not_found`) |
| Destructive confirmation | Delete: "Are you sure you want to delete 1 item?" (`delete_dialog_permission`) with "Move to Recycle Bin" (`delete_dialog_move`) / "Delete" / "Cancel"; sidebar: "Keep at least one tool" (`floating_sidebar_keep_one`) refuses removing the last tool |

### The 8 changed strings (2.6.7 → 2.7.2)

| Key | 2.6.7 | 2.7.2 |
|---|---|---|
| `select_all` | Select All | **Select all** |
| `close` | close | **Close** |
| `about_privacy_policy` | Privacy Policy | **http://appspecials.blogspot.com/2018/04/at-app-specials-we-recognize-that.html** (now a URL; the visible label must come from another key, decided in Phase 9) |
| `caller_private_number` | Private number | **Private Number** |
| `incoming_call_with_duration` | Duration • %1$s | **Duration · %1$s** (middle dot U+00B7) |
| `play_store_not_available` | Billing not supported, Please install or update Play-store Application. | **Billing not supported. Please update the application.** |
| `sub_intro_sub_unlock_feature` | "Unlock all Features\n" | **Unlock all Features** |
| `whatsapp` | WhatsApp | **Whatsapp** |

(The diff also reports two Crashlytics build ids; they are not UI strings.)
Also wrong in ours today (unchanged in the reference): `unselect_all` must be "Unselect all" (ours "Unselect All").
Hardcoded literals that must become string resources so they translate: `"Select All"`/`"Unselect All"` in
`ui/components/NavigationComponents.kt:145`, `"Select All"` in `ui/recycle/RecycleScreen.kt:120` and `ui/vault/VaultMediaScreen.kt:103`.

### New strings (84 keys found; checklist says 85)

Import the 74 app keys below; skip the 10 SDK keys (`CronetProviderClassName`, `INSTALL_UNINSTALL_FSN_SCREEN_EVENT`, `dismiss_button_description`, `test_ad`, `mraid_*` x6).

- Home shell: home_shell_photos "Photos", home_shell_album "Album", home_shell_sort "Sort", home_shell_more_options "More options"
- Albums tab: album_tab_memories "Memories", album_tab_all_video "All Video", album_tab_deleted "Deleted"; close_button "Close button" (content description)
- Explore: explore_free_up_space "Free up space", explore_free_up_space_desc "Duplicates and junk in one scan", explore_scan_now "Scan now", explore_section_create "Create", explore_section_library "Library", explore_section_manage "Manage", explore_tab_ai_editor "AI Editor", explore_ai_editor_desc "Remove background, blur", explore_collage_desc "Mix your photos"
- Onboarding promo: onb_feature_title "Get more from your Gallery", onb_feature_subtitle "Turn on these features to reach your photos faster", onb_feature_chip_call "After-call", onb_feature_chip_shortcut "Shortcut", onb_feature_chip_tips "App tips", onb_feature_permission_note "Needs Phone and Display over other apps permissions", onb_feature_enable "Enable features", onb_feature_skip "Not now", onb_feature_mock_title "Display over other apps", onb_feature_mock_toggle "Allow display over other apps"
- Floating sidebar: floating_intro_swipe_title "Your quick tools live here", floating_intro_swipe_desc "Swipe in from the edge handle to open them on any screen.", floating_intro_move_title "Put it where you like", floating_intro_move_desc "Long press the handle, then drag it to any edge.", floating_intro_tap_continue "Tap anywhere to continue", floating_intro_tap_finish "Tap anywhere to finish", floating_sidebar_all_tools "All tools", floating_sidebar_edit_hint "Tap + to add or − to remove a tool" (U+2212 minus), floating_sidebar_keep_one "Keep at least one tool", floating_sidebar_limit "You can add up to %1$d tools"
- After-call: gs1_call_ended_title "Conversation ended", gs1_call_ended_text "See details", gs1_notif_channel_name "Quick Info Card", gs1_notif_channel_desc "Quick info notifications", gs1_overlay_disabled_title "⚠️ Quick Info disabled", gs1_overlay_disabled_text "Tap now — allow %1$s to display over the apps so you never miss what matters"
- Install/update: gs2_installing_title "Installing...", gs2_updating_title "Updating...", gs2_uninstalling_title "Uninstalling...", gs2_progress_title_update "Updating app…", gs2_progress_desc_install "Almost ready to go.", gs2_progress_desc_update "Please wait a moment.", gs2_progress_desc_uninstall "Removing the app from your device.", gs2_setting_things_up "Finalizing setup…", gs2_cleaning_up "Finalizing removal…", gs2_installation_complete "Installation Complete", gs2_update_complete "Update Successful", gs2_update_complete_desc "Start using the latest version now.", gs2_removal_complete "Removal Completed", gs2_all_set "You're all set!" (reference value carries literal quotes; keep as decoded), gs2_you_can_start_using_the_app_now "You can start using the app now.", gs2_your_app_uninstalled_successfully "Your app uninstalled successfully."
- Usage access + settings: usage_access_home_title "Make %1$s better for you", usage_access_story_title "Loved these stories?", usage_access_video_title "Enjoyed that video?", usage_access_recycle_title "Keep %1$s running great", usage_access_settings_title "Share app usage stats?", usage_access_settings_eyebrow "PRIVACY FRIENDLY", usage_access_desc "Allow Usage Access so %1$s can share de-identified app usage stats.", usage_access_point_media "Your photos, videos and files are never read", usage_access_point_personal "No names, contacts or messages are collected", usage_access_point_control "Turn it off anytime in Settings", usage_access_next_step "Next: find %1$s and turn on “Permit usage access”.", usage_access_allow "Allow", usage_access_not_now "Not now", setting_usage_data_title "Share app usage stats", setting_usage_data_off_toast "App usage stats sharing turned off"

Copy each value from the XML (not from this list) to keep escapes (`\'`, `%1$s`, Unicode) exact.

### Translations (FND-02)

- Ours ships only `values/strings.xml` today, while `LanguageScreen.kt` offers 24 languages (en, ar, bn, de, es, fa, fr, gu, hi, in, it, ja, ko, mr, ms, my, pl, pt, ru, sv, ta, tr, ur, zh). Create `values-<code>/strings.xml` for the 23 non-English codes from `R272/values-<code>/strings.xml`.
- Parity rule for "no untranslated fallbacks": a key shows English in a language only when 2.7.2 itself has no translation for it. Verified gaps in 2.7.2 (all 23 languages): `album_tab_all_video`, `album_tab_deleted`, `album_tab_memories`, `close_button`, `explore_tab_ai_editor`, and all 10 `onb_feature_*` keys. `gs2_*` (16 keys) are missing in bn, gu, mr, ur; `bn` has only 21 of the 84 new keys. These fall back to English exactly as in 2.7.2; do not machine-translate them.
- Copy translations for every key our `values/strings.xml` already has that exists in the reference, not only the new/changed ones; otherwise switching language still shows English on existing screens and success criterion 2 fails.
- RTL: `ar`, `fa`, `ur` (the reference has `layout-ar/fa/ur`); Compose mirrors automatically; drawables that must not mirror keep `autoMirrored` as the reference XML sets it.

---

## UI Considerations

Applicable state considerations resolved: 3 covered, 2 backstop, 1 unresolved

| Category | Element(s) | Status | Resolution / Reason |
|----------|------------|--------|---------------------|
| theme | all existing screens | ✅ covered | Every existing screen renders in light and dark after the colour/primary change, with `colorPrimary` #0066FF in both modes |
| scaling | imported XML drawables | ✅ covered | With `values-swNNNdp` dimens imported, an imported drawable on the 360dp test phone renders at 1.2x its base sdp (e.g. `explore_card_bg` radius 15.6dp) |
| long-text | translated strings in fixed-width chips/buttons (`OnbFeatureChip` maxLines 1, tab labels, sidebar labels ellipsize end) | 🧪 backstop | Switch to de, ru, ta and check no clipped CTA; reference truncation behaviour (ellipsize/maxLines) is the expected result |
| locale | language switch across all 24 languages | ✅ covered | Each changed string shows the 2.7.2 translation; keys untranslated in 2.7.2 show English |
| regression | Inter default font on existing screens | 🧪 backstop | Screenshot Home, Albums, Viewer, Settings before/after; text metrics change (Inter vs Roboto) is expected, clipping or overflow is not |
| brand | launcher icon, splash logo, about logo, app_icon_lottie | ⚠ unresolved | Needs our own art from the user (or a task to create it); planner treats as an input dependency |

---

## Registry Safety

| Registry | Blocks Used | Safety Gate |
|----------|-------------|-------------|
| none (Android, no shadcn) | – | not applicable |

Asset provenance gate instead: only copy from `R272/res`; never copy ad-SDK resources listed above; brand-art register must be empty of reference files before the phase closes.

---

## Re-runnable provenance (2026-11-13)

- Strings: `python -I resdiff.py R267/values/strings.xml R272/values/strings.xml string all` → NEW 84, CHANGED 10 (8 UI + 2 Crashlytics ids).
- Colours: same script with `color` on `values/colors.xml` (NEW 108, CHANGED 3) and `values-night/colors.xml` (NEW 70, CHANGED 2).
- Drawables: `comm -13` of sorted `find drawable* -type f` lists → 111 new files; `cmp` loop → 5 changed.
- Raw: `ls R267/raw` vs `ls R272/raw`, `cmp` against `app/src/main/res/raw`.
- Brand: `cmp` of our `mipmap-*`, `drawable/app_icon_splash.png`, `drawable/about_gallery_logo.png`, `raw/app_icon_lottie.json` against `R272`.
- sdp factor: `grep '"_10sdp"' R272/values-sw*/dimens.xml`; test phone `adb shell wm size` 720x1600, `wm density` 320 → sw 360dp.
(`resdiff.py` is a 15-line ElementTree name/value diff; it lives in the session scratchpad and can be recreated from this description.)

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
