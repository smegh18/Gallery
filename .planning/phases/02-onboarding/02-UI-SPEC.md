---
phase: "2"
slug: "onboarding"
status: approved
reviewed_at: 2026-11-13
shadcn_initialized: false
preset: none
created: "2026-11-13"
scope: "Phase 2 Onboarding — Splash, Language, Permission, Feature promo; Welcome removed"
---

# Phase 2 — UI Design Contract (Onboarding)

> Parity port. Every dimension below is copied from the decompiled reference 2.7.2 and carries its
> reference resource name and source file. Design tokens (sdp/ssp, Inter, colours, text styles) are
> defined in the Phase 1 UI-SPEC and reused here — not redefined.

**Reference roots (read-only evidence):**
- `R272` = `%TEMP%\ref272\dec\res` (2.7.2, versionCode 152)
- `R267` = `%TEMP%\ref267\dec\res` (2.6.7, our current baseline)
- Ours = `app/src/main/res` and `app/src/main/java/com/quizango/gallery/ui/`

**Phase 1 token contract:** `.planning/phases/01-foundation/01-UI-SPEC.md` — all `N.sdp`, `N.ssp`,
`GalleryText.*`, colour names (§ Color) and corner radii (§ Corner radii) are defined there. Use them;
do not redefine.

---

## Design System

| Property | Value |
|----------|-------|
| Tool | none (Android, Jetpack Compose) |
| Component library | Compose Material 3 (Phase 1 contract) |
| Icon library | Reference drawable vectors (Phase 1 contract) |
| Font | Inter (Phase 1 contract) |
| Animation | Lottie `lottie-compose` 6.6.2; M3 `LinearProgressIndicator` for splash loader |

---

## Screen Inventory (checklist rows #1–#5)

### Screen 1: Splash (`activity_splash` — CHANGED, checklist #1, ONB-02)

**File:** `ui/splash/SplashScreen.kt` (exists, needs update)

**Layout contract:**

| Element | Property | Value | Reference source |
|---------|----------|-------|------------------|
| Root | background | `backgroundColor` | `activity_splash` root |
| Logo card | size | 140.sdp × 140.sdp | `lottie_splash` / `iv_splash` width/height |
| Logo card | corner radius | 20.sdp | `MaterialCardView cardCornerRadius` |
| Logo card | elevation | 0dp | `cardElevation=0` |
| Lottie (lottie_splash) | raw | `R.raw.app_icon_lottie` | Brand art — kept for now |
| Image fallback (iv_splash) | drawable | `R.drawable.app_icon_splash` | Brand art — kept for now |
| Image fallback | visibility | visible when Lottie is null | `iv_splash` visible, `lottie_splash` gone initially |
| "Your Photo Manager" label | style | `BodyMediumMedium` (11ssp W500) | `style="@style/BodyMediumMedium"` |
| "Your Photo Manager" label | colour | `titleColor` | `textColor="@color/titleColor"` |
| "Your Photo Manager" label | string | `@string/your_photo_manager` | existing |
| "Your Photo Manager" label | marginBottom | 5.sdp | `layout_marginBottom="_5sdp"` |
| Progress indicator | type | M3 `LinearProgressIndicator` (indeterminate) | `LinearProgressIndicator` in layout |
| Progress indicator | width | 120.sdp | `layout_width="_120sdp"` |
| Progress indicator | indicatorColor | `colorPrimary` (#0066FF) | `app:indicatorColor` |
| Progress indicator | trackColor | `splash_loader_track` (#290066FF) | `app:trackColor` |
| Progress indicator | trackThickness | 5dp (unscaled) | `trackThickness="5.0dip"` |
| Progress indicator | trackCornerRadius | 3dp (unscaled) | `trackCornerRadius="3.0dip"` |
| Progress indicator | indicatorTrackGapSize | 4dp (unscaled) | `indicatorTrackGapSize="4.0dip"` |
| Progress indicator | marginBottom | 30.sdp (from bottom of splash_banner) | `layout_marginBottom="_30sdp"` |
| Banner ad slot | placeholder | empty `FrameLayout` at bottom | `splash_banner` |

**Behaviour:**
- Splash delay: 3000ms total (reference `delayTime` field). Lottie plays once (not loop). If Lottie composition loads, animate it; after movement completes (~frame 25/90, ~0.83s) + 350ms beat, navigate. If composition fails, wait 2000ms cap then navigate.
- Navigation (2.7.2 flow): checks `hasCompletedOnboarding` pref (was `hasCompletedWelcome`). If false → Language. If true but no storage permission → Permission. If true and has permission → Home. **Welcome screen route is never used.**

**Changes from current code:**
- Replace custom `WavyProgressIndicator` canvas with M3 `LinearProgressIndicator` (the reference uses M3's wavy progress on API 24+, which renders as the standard indeterminate animation with the wave attributes).
- Replace hardcoded `170.dp` → `140.sdp`, `26.dp` → `20.sdp`, `200.dp` → `120.sdp`.
- Change navigation target from `Screen.Welcome` to `Screen.Language` (new route).
- Rename pref `hasCompletedWelcome` → `hasCompletedOnboarding` (or reuse existing key with updated semantics).

---

### Screen 2: Welcome (`activity_welcome` — REMOVED, checklist #2)

**Action:** Remove `ui/onboarding/WelcomeScreen.kt`. Remove `Screen.Welcome` route from `Screen.kt`.
Remove the nav graph entry. Any reference to the Welcome route in other files must be rerouted.

---

### Screen 3: Language (`activity_app_language` — CHANGED, checklist #3, ONB-03)

**File:** `ui/settings/LanguageScreen.kt` (exists, needs update for onboarding context)

**Layout contract:**

| Element | Property | Value | Reference source |
|---------|----------|-------|------------------|
| Root | background | `backgroundColor` | `activity_app_language` root |
| Toolbar | height | `?actionBarSize` (56dp default) | `layout_height="?actionBarSize"` |
| Back button (backBtn) | icon | `R.drawable.ic_back` | `src="@drawable/ic_back"` |
| Back button | padding | 2.sdp | `padding="_2sdp"` |
| Back button | marginStart | 10.sdp | `layout_marginStart="_10sdp"` |
| Title | text | `@string/select_app_language` | existing |
| Title | style | `BodyXLargeSemiBold` (14ssp W600) | `style="@style/BodyXLargeSemiBold"` |
| Title | colour | `titleColor` | `textColor="@color/titleColor"` |
| Title | maxLines/ellipsize | 1 / end | `maxLines="1" ellipsize="end"` |
| Title | marginStart/End | 10.sdp | `layout_marginStart/End="_10sdp"` |
| Tick button (toolbarIvTick) | icon | `R.drawable.ic_done_bold` | `src="@drawable/ic_done_bold"` |
| Tick button | size | 50.sdp × 28.sdp | `layout_width="_50sdp" layout_height="_28sdp"` |
| Tick button | background | `bg_rounded_red` with tint `colorPrimary` | `background="@drawable/bg_rounded_red" backgroundTint="@color/colorPrimary"` |
| Tick button | padding | 5.sdp | `padding="_5sdp"` |
| Tick button | marginEnd | 10.sdp | `layout_marginEnd="_10sdp"` |
| Divider (top) | colour | `dividerColor` (#1A002847 / #20FFFFFF) | `background="@color/dividerColor"` |
| Divider (top) | height | 1dp (unscaled) | `layout_height="1.0dip"` |
| Language list | margins | top/bottom 3.sdp | `layout_marginTop/Bottom="_3sdp"` |
| Divider (bottom) | colour | #FF99A4B6 (hardcoded) | `background="#ff99a4b6"` |
| Divider (bottom) | height | 1.sdp | `layout_height="_1sdp"` |
| Divider (bottom) | marginTop | 2.sdp | `layout_marginTop="_2sdp"` |
| Banner ad slot | include `global_ad_framelayout` | placeholder | bottom constraint |
| First-time hand Lottie (lavFirstTime) | raw | `R.raw.hand` | `lottie_rawRes="@raw/hand"` |
| First-time hand Lottie | size | 55.sdp × wrap | `layout_width="_55sdp"` |
| First-time hand Lottie | padding | 10dp (unscaled) | `padding="10.0dip"` |
| First-time hand Lottie | marginEnd | 5.sdp | `layout_marginEnd="_5sdp"` |
| First-time hand Lottie | position | toolbar bottom-end, overlapping tick button | constraints: bottom of toolbar, end of toolbar |
| First-time hand Lottie | autoPlay/loop | true / true | `lottie_autoPlay="true" lottie_loop="true"` |
| First-time hand Lottie | visibility | visible only on first visit | `isFirstTime` preference |

**Behaviour:**
- First-time visit (during onboarding): hand Lottie animates over the tick button to guide the user. After the user selects a language and taps the tick, the Lottie is hidden and `isFirstTime` is set to false. On subsequent visits (from Settings), the Lottie doesn't appear.
- Back button: navigates back. During onboarding flow, this means back to Splash.
- Tick button: applies selected language and navigates to the next screen. During onboarding → Permission. From Settings → returns to Settings.
- The language list is the existing 24 languages with radio selection (same as current LanguageScreen).

**Dual context (onboarding vs settings):**
The same screen serves both onboarding and settings. The screen needs a parameter to know if it's the onboarding flow (show hand Lottie, navigate to Permission on tick) vs settings (no Lottie, pop back on tick).

---

### Screen 4: Permission (`activity_permission` — same layout, checklist #4, ONB-04)

**File:** `ui/permission/PermissionScreen.kt` (exists, needs update)

**Layout contract:**

| Element | Property | Value | Reference source |
|---------|----------|-------|------------------|
| Root | background | `ic_text_button` (#FFFFFF / #131111 dark) | `background="@color/ic_text_button"` |
| Top illustration | drawable | `R.drawable.ic_permission_top` | `src="@drawable/ic_permission_top"` |
| Top illustration | width/height | fill_parent / wrap_content | adjustViewBounds=true |
| Top illustration | alpha | 0.7 | `alpha="0.7"` |
| Guideline | 35% vertical | content starts below | `layout_constraintGuide_percent="0.35"` |
| Mock settings card | background | `ic_permission_top_dialog` (white #FFF, stroke 9dp #CCE0FF / dark: #000, stroke #272727) | drawable XML |
| Mock settings card | width | 235.sdp | `layout_width="_235sdp"` |
| Mock settings card | padding | 10/15/10/15dp (unscaled) | `padding*="10.0/15.0dip"` |
| Mock settings card | marginTop | 20.sdp | `layout_marginTop="_20sdp"` |
| Mock — back arrow (ivBack) | icon | `ic_expand_arrow` rotated 270° | `rotation="270.0"` |
| Mock — back arrow | size | 30dp (unscaled) | `layout_width/height="30.0dip"` |
| Mock — back arrow | padding | 4dp (unscaled) | `padding="4.0dip"` |
| Mock — back arrow | margin | 8.sdp left/top/right | `layout_margin*="_8sdp"` |
| Mock — title | text | `@string/all_files_access` | existing |
| Mock — title | style | `BodyMediumSemiBold` (11ssp W600) | `style="@style/BodyMediumSemiBold"` |
| Mock — title | marginStart | 4.sdp | `layout_marginStart="_4sdp"` |
| Mock — logo (ivLogo) | drawable | `R.drawable.ic_logo` (or `app_icon_splash` fallback) | Reference `ic_logo.webp` imported in Plan 1 |
| Mock — logo | height | 45dp (unscaled), width match aspect | `layout_height="45.0dip"` |
| Mock — logo | margin | 8.sdp horizontal, 12.sdp vertical | `layout_margin*` |
| Mock — app name | text | `@string/app_name` | existing |
| Mock — app name | style | `BodyLargeSemiBold` (13ssp W600) | `style="@style/BodyLargeSemiBold"` |
| Mock — app name | marginStart | 8.sdp | `layout_marginStart="_8sdp"` |
| Toggle bar | background | `ic_permission_bg.webp` (raster, not vector) | webp asset |
| Toggle bar | size | fill_parent × 80dp (unscaled) | `layout_height="80.0dip"` |
| Toggle bar | marginTop | -25dp (overlap onto mock card) | `layout_marginTop="-25.0dip"` |
| Toggle bar | marginStart/End | 5.sdp | `layout_marginStart/End="_5sdp"` |
| Toggle bar — text | text | `@string/allow_access_to_manage_all_files` | existing |
| Toggle bar — text | size | 12.ssp | `textSize="_12ssp"` |
| Toggle bar — text | style | `BodyXLargeSemiBold` | `style="@style/BodyXLargeSemiBold"` |
| Toggle bar — text | marginStart/End | 18.sdp / 65.sdp | `layout_marginStart/End` |
| Hand Lottie | raw | `R.raw.permission` | `lottie_rawRes="@raw/permission"` |
| Hand Lottie | size | 50.sdp | `layout_width/height="_50sdp"` |
| Hand Lottie | marginTop | -53dp (unscaled, overlap above toggle) | `layout_marginTop="-53.0dip"` |
| Hand Lottie | marginEnd | 25.sdp | `layout_marginEnd="_25sdp"` |
| Hand Lottie | autoPlay/loop | true / true | |
| "Permission Required" heading | size | 18.ssp | `textSize="_18ssp"` |
| "Permission Required" heading | style | `BodyXXLargeSemiBold` | `style="@style/BodyXXLargeSemiBold"` |
| "Permission Required" heading | colour | `per_text_color` (#1E3354 / #FFFFFF) | `textColor="@color/per_text_color"` |
| Description | text | `@string/allow_storage_permission` | existing |
| Description | size | 11.ssp | `textSize="_11ssp"` |
| Description | colour | `gray_permission` (#7A8389 / #AAAAAA dark) | `textColor="@color/gray_permission"` |
| Description | font | Inter Medium | `fontFamily="@font/if_inter_medium"` |
| Description | marginTop / marginBottom | 3.sdp / 5.sdp | `layout_marginTop/Bottom` |
| "Grant access" button | text | `@string/grant_access` | existing |
| "Grant access" button | background | `btn_permission` (ripple, dark_blue #0066FF fill, 25.sdp radius) | drawable XML |
| "Grant access" button | textColor | `onlyWhite` (#FFFFFF) | `textColor="@color/onlyWhite"` |
| "Grant access" button | style | `BodyXLargeSemiBold` | `style="@style/BodyXLargeSemiBold"` |
| "Grant access" button | padding vertical | 14.sdp | `paddingTop/Bottom="_14sdp"` |
| "Grant access" button | margin horizontal | 15.sdp | `layout_marginLeft/Right="_15sdp"` |
| "Grant access" button | marginTop | 12.sdp | `layout_marginTop="_12sdp"` |
| "About this permission" link | text | `@string/about_this_permission` | existing |
| "About this permission" link | style | `BodySmallSemiBold` (10ssp W600) | `style="@style/BodySmallSemiBold"` |
| "About this permission" link | colour | `titleColor` | `textColor="@color/titleColor"` |
| "About this permission" link | padding vertical | 5.sdp | `paddingTop/Bottom="_5sdp"` |
| "About this permission" link | margin vertical | 12.sdp | `layout_margin*="_12sdp"` |

**Behaviour:**
- "Grant access" taps → requests `MANAGE_EXTERNAL_STORAGE` (API 30+) or `READ/WRITE_EXTERNAL_STORAGE` (below).
- On grant → navigates to Feature promo (onboarding) or Home (re-permission from settings).
- "About this permission" → shows `PermissionNeededDialog`.
- On refuse (Back or system deny) → remains on Permission screen. The reference does not show a dialog_permission_needed on refuse during onboarding; it shows on subsequent app opens when permission is still missing.

---

### Screen 4a: Permission Needed Dialog (`dialog_permission_needed` — NEW, checklist #4, ONB-04)

**Shown by:** Permission screen → "About this permission" tap

**Layout contract:**

| Element | Property | Value | Reference source |
|---------|----------|-------|------------------|
| Root | background | `dialog_background_gradient` (linear gradient 270°: #FFC4DCFF → #FFFFFFFF → #FFFFFFFF; topLeft/Right radius 30dp unscaled) | Reference `dialog_background_gradient.xml` imported in Plan 1 |
| Root | padding | 20.sdp | `padding="_20sdp"` |
| Illustration | drawable | `R.drawable.ic_permission_needed` (125dp × 120dp) | Reference `ic_permission_needed.png` imported in Plan 1 |
| Illustration | size | 125dp × 120dp (unscaled) | `layout_width/height` |
| Title | text | `@string/why_this_permission_needed` ("Why This Permission Is Needed?") | Imported in Plan 1 from R272 |
| Title | size | 13.ssp | `textSize="_13ssp"` |
| Title | style | `BodyLargeSemiBold` | `style="@style/BodyLargeSemiBold"` |
| Title | colour | `patternColor` (#1E3354 / #FFFFFF dark) | `textColor="@color/patternColor"` |
| Title | marginTop | 11.sdp | `layout_marginTop="_11sdp"` |
| Description | text | `@string/call_permission_need` ("This permission is needed to enable calling features.") | Imported in Plan 1 from R272 |
| Description | size | 10.ssp | `textSize="_10ssp"` |
| Description | style | `BodySmallMedium` | `style="@style/BodySmallMedium"` |
| Description | colour | `radioButtonUncheckedColor` (#7A8389) | `textColor="@color/radioButtonUncheckedColor"` |
| Description | marginTop | 10dp (unscaled) | `layout_marginTop="10.0dip"` |
| Steps 1–4 | texts | `@string/open_setting_dialog`, `tap_permission_dialog`, `select_phone_call_dialog`, `tap_allow_dialog` | Imported in Plan 1 from R272 |
| Steps 1–4 | size | 9.ssp | `textSize="_9ssp"` |
| Steps 1–4 | style | `BodyMediumMedium` | `style="@style/BodyMediumMedium"` |
| Steps 1–4 | colour | `radioButtonUncheckedColor` | same |
| Steps 1–4 | marginTop | 10dp first, 3.sdp rest | `layout_marginTop` values |
| "Grant access" button | text | `@string/grant_access` | existing |
| "Grant access" button | size | 14.ssp | `textSize="_14ssp"` |
| "Grant access" button | style | `BodyLargeSemiBold` | `style="@style/BodyLargeSemiBold"` |
| "Grant access" button | background | `btn_permission` | same as main button |
| "Grant access" button | textColor | `onlyWhite` | `textColor="@color/onlyWhite"` |
| "Grant access" button | marginTop | 10dp (unscaled) | `layout_marginTop="10.0dip"` |
| Dialog style | window | `PermissionDialog` (full width major/minor 100%) | `styles.xml` |
| Dialog style | animation | `dialog_window_animation` (slide up) | `windowAnimationStyle` |

**Behaviour:**
- Shown as a bottom sheet dialog (full width, rounded top corners).
- "Grant access" in the dialog → same action as the main "Grant access" button (opens system permission).
- Dialog dismisses on back press or outside tap.

---

### Screen 5: Feature Promo (`activity_feature_promo` — NEW, checklist #5, ONB-05)

**File:** `ui/onboarding/FeaturePromoScreen.kt` (NEW — must be created)

**Layout contract:**

| Element | Property | Value | Reference source |
|---------|----------|-------|------------------|
| Root | background | `ic_text_button` (#FFFFFF / #131111 dark) | `background="@color/ic_text_button"` |
| Top image | drawable | `R.drawable.img_feature_promo_top` (1125×1005 webp) | `src="@drawable/img_feature_promo_top"` |
| Top image | scaleType | matrix (top-aligned, full bleed, crops bottom) | `scaleType="matrix"` |
| Top image | extent | fills from top to sheet overlap | constraints: top to parent, bottom to sheetOverlap |
| Top image | night variant | `drawable-night-nodpi/img_feature_promo_top.webp` | separate night resource |
| Sheet overlap space | height | 28dp (unscaled) | `layout_height="28.0dip"` |
| Bottom sheet | background | `bg_onb_feature_sheet` (ic_text_button fill, 24dp unscaled top corners) | drawable XML |
| Bottom sheet | padding | 12.sdp horizontal, 16.sdp top, 10.sdp bottom | `padding*` |
| Bottom sheet | elevation | 6.sdp | `elevation="_6sdp"` |
| Bottom sheet | position | pinned to bottom, overlapping image by 28dp | constraints |
| Mock settings card | background | `ic_permission_top_dialog` | same as Permission screen |
| Mock settings card | width | 210.sdp | `layout_width="_210sdp"` (smaller than Permission's 235.sdp) |
| Mock settings card | padding | 10/12/10/26dp (unscaled) | `padding*` |
| Mock — back arrow | icon | `ic_expand_arrow` rotated 270° | same as Permission |
| Mock — back arrow | size | 26dp (unscaled) | `layout_width/height="26.0dip"` (smaller than Permission's 30dp) |
| Mock — back arrow | marginTop | 4.sdp | `layout_marginTop="_4sdp"` |
| Mock — back arrow | marginStart | 6.sdp | `layout_marginStart="_6sdp"` |
| Mock — title | text | `@string/onb_feature_mock_title` ("Display over other apps") | new string |
| Mock — title | style | `BodyMediumSemiBold` | same |
| Mock — title | marginStart | 4.sdp | `layout_marginStart="_4sdp"` |
| Mock — logo | drawable | `R.drawable.ic_logo` (or `app_icon_splash`) | Reference `ic_logo.webp` imported in Plan 1 |
| Mock — logo | height | 36dp (unscaled) | `layout_height="36.0dip"` (smaller than Permission's 45dp) |
| Mock — logo | marginTop | 8.sdp, marginStart 6.sdp | margins |
| Mock — app name | text | `@string/app_name` | existing |
| Mock — app name | style | `BodyLargeSemiBold` | same |
| Mock — app name | marginStart | 8.sdp | |
| Toggle bar | background | `ic_permission_bg.webp` (raster) | same asset as Permission |
| Toggle bar | size | fill × 64dp (unscaled) | `layout_height="64.0dip"` (shorter than Permission's 80dp) |
| Toggle bar | marginTop | -22dp (unscaled) | `layout_marginTop="-22.0dip"` |
| Toggle bar — text | text | `@string/onb_feature_mock_toggle` ("Allow display over other apps") | new string |
| Toggle bar — text | size | 11.ssp | `textSize="_11ssp"` |
| Toggle bar — text | style | `BodyXLargeSemiBold` | |
| Toggle bar — text | marginStart/End | 16.sdp / 65.sdp | |
| Hand Lottie | raw | `R.raw.permission` | same Lottie as Permission |
| Hand Lottie | size | 46.sdp | `layout_width/height="_46sdp"` (slightly smaller than Permission's 50.sdp) |
| Hand Lottie | marginTop | -40dp (unscaled) | `layout_marginTop="-40.0dip"` |
| Hand Lottie | marginEnd | 18.sdp | `layout_marginEnd="_18sdp"` |
| Heading | text | `@string/onb_feature_title` ("Get more from your Gallery") | new string |
| Heading | size | 17.ssp | `textSize="_17ssp"` |
| Heading | style | `BodyXXLargeSemiBold` (16ssp base, but here overridden to 17ssp) | `style + textSize` |
| Heading | colour | `black` (#000000 / #FFFFFF dark — inverted!) | `textColor="@color/black"` |
| Heading | marginTop | 14.sdp | `layout_marginTop="_14sdp"` |
| Subtitle | text | `@string/onb_feature_subtitle` ("Turn on these features…") | new string |
| Subtitle | size | 11.ssp | `textSize="_11ssp"` |
| Subtitle | style | `BodyMediumRegular` (11ssp W400) | `style="@style/BodyMediumRegular"` |
| Subtitle | colour | `op_onboarding_ac_prem_desc` (#373742 / #AAAAAA dark) | `textColor` |
| Subtitle | marginTop | 3.sdp | `layout_marginTop="_3sdp"` |
| Chip row | orientation | horizontal, centered | `LinearLayout gravity="center"` |
| Chip row | marginTop | 10.sdp | `layout_marginTop="_10sdp"` |
| Chip — "After-call" | text | `@string/onb_feature_chip_call` | new string |
| Chip — "After-call" | drawableStart | `R.drawable.ic_gs_call` | new drawable |
| Chip — "Shortcut" | text | `@string/onb_feature_chip_shortcut` | new string |
| Chip — "Shortcut" | drawableStart | `R.drawable.ic_floating_btn` | Reference `ic_floating_btn.xml` imported in Plan 1 |
| Chip — "Shortcut" | marginHorizontal | 6.sdp | `layout_marginLeft/Right="_6sdp"` |
| Chip — "App tips" | text | `@string/onb_feature_chip_tips` | new string |
| Chip — "App tips" | drawableStart | `R.drawable.explore_tab_ic_cleaner` | existing drawable |
| All chips — style | parent | `OnbFeatureChip` = `BodySmallMedium` (10ssp W500) | `styles.xml` |
| All chips — textColor | `iu_primary` (#1073FE / #0169FB dark) | OnbFeatureChip style |
| All chips — drawableTint | `iu_primary` | OnbFeatureChip style |
| All chips — background | `bg_onb_feature_chip` (iu_primary_soft fill, 100dp radius = pill) | drawable XML |
| All chips — drawablePadding | 4.sdp | OnbFeatureChip style |
| All chips — paddingStart/End | 8.sdp / 10.sdp | OnbFeatureChip style |
| All chips — maxLines | 1 | OnbFeatureChip style |
| Permission note | text | `@string/onb_feature_permission_note` ("Needs Phone and Display…") | new string |
| Permission note | size | 10.ssp | `textSize="_10ssp"` |
| Permission note | style | `BodyMediumRegular` | |
| Permission note | colour | `op_onboarding_desc_color` (#7A8389 / #AAAAAA dark) | |
| Permission note | marginTop | 10.sdp | |
| "Enable features" button | text | `@string/onb_feature_enable` | new string |
| "Enable features" button | size | 15.ssp | `textSize="_15ssp"` |
| "Enable features" button | style | `BodyXLargeSemiBold` | |
| "Enable features" button | background | `btn_permission` (dark_blue #0066FF, 25.sdp radius pill) | same as Permission |
| "Enable features" button | textColor | `onlyWhite` | |
| "Enable features" button | padding vertical | 13.sdp | `paddingTop/Bottom="_13sdp"` |
| "Enable features" button | marginTop | 10.sdp | |
| "Not now" skip | text | `@string/onb_feature_skip` ("Not now") | new string |
| "Not now" skip | size | 12.ssp | `textSize="_12ssp"` |
| "Not now" skip | style | `BodyMediumRegular` | |
| "Not now" skip | colour | `op_onboarding_desc_color` | |
| "Not now" skip | padding | 8.sdp all | `padding="_8sdp"` |
| "Not now" skip | marginTop | 2.sdp | `layout_marginTop="_2sdp"` |
| "Not now" skip | visibility | gone initially, visible after 2s delay | `visibility="gone"` + timer |

**Behaviour:**
- "Enable features" → requests `READ_PHONE_STATE` permission + checks `Settings.canDrawOverlays()`. If overlay not granted, opens `ACTION_MANAGE_OVERLAY_PERMISSION` intent. On both granted → navigate to Home.
- "Not now" skip → appears after a 2-second delay (reference pattern: `btnSkip.postDelayed({ btnSkip.visibility = VISIBLE }, 2000)`). Tapping it → navigate to Home without requesting permissions.
- Both paths set `hasCompletedOnboarding` = true.
- Back press → navigate back to Permission (the user cannot skip Feature promo by pressing Back to go to Home directly).

---

## Navigation Flow Contract

```
Splash (3s)
  ├── hasCompletedOnboarding=false → Language (isOnboarding=true)
  │     └── tick → Permission
  │           ├── grant → FeaturePromo
  │           │     ├── "Enable features" → [request perms] → Home
  │           │     └── "Not now" (after 2s) → Home
  │           └── "About this permission" → PermissionNeededDialog
  │                 └── "Grant access" → [request perms] → FeaturePromo or stays
  └── hasCompletedOnboarding=true
        ├── no storage permission → Permission → Home (no feature promo on re-entry)
        └── has permission → Home
```

New route needed: `Screen.Language` already exists (route `"language"`). Add `Screen.FeaturePromo` (route `"feature_promo"`).

The Language screen must know it's in the onboarding flow (to navigate to Permission instead of popping back). Pass an `isOnboarding` boolean via the nav argument or saved state.

---

## Spacing Scale

All spacing uses Phase 1 tokens. Phase-specific values not in the Phase 1 scale:

| Value | Where | Compose Layout Implementation Rule |
|-------|-------|-------------------------------------|
| -25dp / -22dp / -40dp / -53dp (negative margins) | Mock card → toggle bar overlaps (Permission/Feature promo). | Implement via `Box` overlay or `Column` with relative offsets and manual Spacer adjustments to avoid hitbox clipping. Set explicit Z-ordering (`Modifier.zIndex`) so Lottie hand is above toggle bar, and toggle bar is above mock card. |
| 28dp | Sheet overlap space (Feature promo) | Pinned bottom sheet overlapping top image by 28.dp. |
| 64dp / 80dp | Toggle bar heights (unscaled) | Fixed height containers with `.height(N.dp)`. |
| 26dp, 30dp, 36dp, 45dp | Mock arrow/logo sizes (unscaled dp) | Unscaled dp per reference XML dimensions. |
| 125dp × 120dp | Permission needed illustration | Unscaled dp per reference XML dimensions. |

---

## Typography

All text styles are from Phase 1. No new styles needed. Phase-specific overrides:

| Override | Base style | Override property |
|----------|-----------|-------------------|
| Feature promo heading | BodyXXLargeSemiBold (16ssp) | `textSize="_17ssp"` → use `17.ssp` directly |
| Feature promo "Enable" | BodyXLargeSemiBold (14ssp) | `textSize="_15ssp"` → use `15.ssp` directly |
| Permission needed title | BodyLargeSemiBold (13ssp) | no override (13ssp matches) |
| Permission needed steps | BodyMediumMedium (11ssp) | `textSize="_9ssp"` → use `9.ssp` directly |

Where the reference overrides `textSize` on a style, use the overridden size with the style's weight and font.

---

## Color

All colours are sourced from `R272/values/colors.xml` and `R272/values-night/colors.xml`. Phase 2 imports any missing tokens in Plan 1:

| Name | Light | Dark | Where | Status in Codebase |
|------|-------|------|-------|--------------------|
| `backgroundColor` | #FFFFFF | #0C0C0C | Splash root, Language root | Existing in project |
| `ic_text_button` | #FFFFFF | #131111 | Permission/Feature promo root, bg_onb_feature_sheet fill | Existing in project |
| `colorPrimary` / `dark_blue` | #0066FF | #0066FF | Splash progress indicator, tick button tint, btn_permission fill | Existing in project |
| `splash_loader_track` | #290066FF | #290066FF | Splash progress track | Existing in project |
| `titleColor` | #1E3354 | #FFFFFF | Splash label, language title, "About this permission" | Existing in project |
| `per_text_color` | #1E3354 | #FFFFFF | Permission mock texts, toggle text | **Import to colors.xml / night in Phase 2 Plan 1** |
| `dividerColor` | #1A002847 | #20FFFFFF | Language top divider | Existing in project |
| `gray_permission` | #7A8389 | #AAAAAA | Permission description | **Import to colors.xml / night in Phase 2 Plan 1** |
| `onlyWhite` | #FFFFFF | #FFFFFF | Button text | Existing in project |
| `patternColor` | #1E3354 | #FFFFFF | Permission needed dialog title | Existing in project |
| `radioButtonUncheckedColor` | #7A8389 | #7A8389 | Permission needed dialog description/steps | **Import to colors.xml in Phase 2 Plan 1** |
| `op_onboarding_ac_prem_desc` | #373742 | #AAAAAA | Feature promo subtitle | **Import to colors.xml / night in Phase 2 Plan 1** |
| `op_onboarding_desc_color` | #7A8389 | #AAAAAA | Feature promo permission note, skip text | **Import to colors.xml / night in Phase 2 Plan 1** |
| `black` | #000000 | #FFFFFF (inverted) | Feature promo heading | Existing in project |
| `iu_primary` | #1073FE | #0169FB | Chip text & icon tint | Existing in project |
| `iu_primary_soft` | #E5F2FF | #132A45 | Chip background (bg_onb_feature_chip) | Existing in project |
| `ripple_color` | #40000000 | — | btn_permission ripple | Existing in project |
| dialog gradient | #FFC4DCFF → #FFFFFFFF | — | dialog_background_gradient (light only; parity) | Defined in imported drawable XML |

---

## Copywriting Contract

All copy is exact reference text from `R272/values/strings.xml`. Phase 2 uses existing strings and imports the 6 dialog strings into `values/strings.xml` in Plan 1 (synchronized across 24 languages with `python -I scripts/ref_res.py sync-strings`):

| Element | Copy (reference key) | Status in Codebase |
|---------|------|--------------------|
| Splash label | "Your Photo Manager" (`your_photo_manager`) | Existing |
| Language title | "Select app language" (`select_app_language`) | Existing |
| Permission title mock | "All files access" (`all_files_access`) | Existing |
| Permission toggle | "Allow access to manage all files" (`allow_access_to_manage_all_files`) | Existing |
| Permission heading | "Permission Required" (`permission_required`) | Existing |
| Permission description | from `allow_storage_permission` | Existing |
| Permission CTA | "Grant access" (`grant_access`) | Existing |
| Permission link | "About this permission" (`about_this_permission`) | Existing |
| Permission dialog title | "Why This Permission Is Needed?" (`why_this_permission_needed`) | **Import in Phase 2 Plan 1** |
| Permission dialog desc | "This permission is needed to enable calling features." (`call_permission_need`) | **Import in Phase 2 Plan 1** |
| Permission dialog step 1 | "1. Open Settings" (`open_setting_dialog`) | **Import in Phase 2 Plan 1** |
| Permission dialog step 2 | "2. Tap Permissions" (`tap_permission_dialog`) | **Import in Phase 2 Plan 1** |
| Permission dialog step 3 | "3. Select Phone Call" (`select_phone_call_dialog`) | **Import in Phase 2 Plan 1** |
| Permission dialog step 4 | "4. Tap Allow" (`tap_allow_dialog`) | **Import in Phase 2 Plan 1** |
| Feature promo heading | "Get more from your Gallery" (`onb_feature_title`) | Existing (Phase 1 imported) |
| Feature promo subtitle | "Turn on these features to reach your photos faster" (`onb_feature_subtitle`) | Existing (Phase 1 imported) |
| Chip — "After-call" | (`onb_feature_chip_call`) | Existing (Phase 1 imported) |
| Chip — "Shortcut" | (`onb_feature_chip_shortcut`) | Existing (Phase 1 imported) |
| Chip — "App tips" | (`onb_feature_chip_tips`) | Existing (Phase 1 imported) |
| Permission note | "Needs Phone and Display over other apps permissions" (`onb_feature_permission_note`) | Existing (Phase 1 imported) |
| Enable CTA | "Enable features" (`onb_feature_enable`) | Existing (Phase 1 imported) |
| Skip | "Not now" (`onb_feature_skip`) | Existing (Phase 1 imported) |
| Mock title | "Display over other apps" (`onb_feature_mock_title`) | Existing (Phase 1 imported) |
| Mock toggle | "Allow display over other apps" (`onb_feature_mock_toggle`) | Existing (Phase 1 imported) |

---

## Drawables Used by This Phase

Inventory of all drawables required for Phase 2:

| Drawable | Status | Action / Source |
|----------|--------|-----------------|
| `bg_onb_feature_sheet.xml` | Existing | Imported in Phase 1 |
| `bg_onb_feature_chip.xml` | Existing | Imported in Phase 1 |
| `btn_permission.xml` | Existing | Pre-existing in project |
| `ic_permission_top_dialog.xml` | Existing | Pre-existing in project (light + night) |
| `ic_permission_bg.webp` | Existing | Pre-existing in project |
| `ic_permission_top.webp` | Existing | Pre-existing in project |
| `ic_back.xml` | Existing | Pre-existing in project |
| `ic_done_bold.xml` | Existing | Pre-existing in project |
| `ic_expand_arrow.xml` | Existing | Pre-existing in project |
| `app_icon_splash.png` | Existing | Pre-existing in project (Brand art) |
| `bg_rounded_red.xml` | Existing | Pre-existing in project |
| `img_feature_promo_top.webp` | Existing | Imported in Phase 1 (light + night nodpi) |
| `ic_gs_call.xml` | Existing | Imported in Phase 1 |
| `explore_tab_ic_cleaner.xml` | Existing | Pre-existing in project |
| `ic_floating_btn.xml` | **Missing in repo** | **Import from `R272/drawable/ic_floating_btn.xml` in Plan 1** |
| `dialog_background_gradient.xml` | **Missing in repo** | **Import from `R272/drawable/dialog_background_gradient.xml` in Plan 1** |
| `ic_permission_needed.png` | **Missing in repo** | **Import from `R272/drawable/ic_permission_needed.png` in Plan 1** |
| `ic_logo.webp` | **Missing in repo** | **Import from `R272/drawable/ic_logo.webp` in Plan 1** (or fallback to `app_icon_splash`) |

---

## UI Considerations

| Category | Element(s) | Status | Resolution / Reason |
|----------|------------|--------|---------------------|
| theme | all 4 screens + dialog | ✅ covered | Every colour has a dark override or is unchanged in dark. `ic_permission_top_dialog` has a `drawable-night` variant. `bg_onb_feature_sheet` uses `ic_text_button` which has a night value. `black` is inverted (#FFF in dark) — use `colorResource(R.color.black)`, not `Color.Black` |
| negative-offset | mock card → toggle bar overlaps on Permission and Feature promo | ✅ covered | Implement via `Box` overlay or `Column` with relative offsets and manual Spacer adjustments to avoid hitbox clipping. Set explicit Z-ordering (`Modifier.zIndex`) so Lottie hand is above toggle bar, and toggle bar is above mock card. |
| delayed-visibility | "Not now" skip on Feature promo | ✅ covered | `LaunchedEffect` with `delay(2000)` + boolean state. Reference uses `postDelayed` with 2000ms |
| dual-context | Language screen (onboarding vs settings) | ✅ covered | Nav argument `isOnboarding: Boolean` gates Lottie visibility and forward navigation target |
| nav-cleanup | Welcome screen removal | ✅ covered | Delete WelcomeScreen.kt, remove `Screen.Welcome` route, update Splash navigation to target Language |
| brand | app_icon_splash, app_icon_lottie, ic_logo, img_feature_promo_top | ⚠ unresolved | Kept as reference art per Phase 1 user decision; pre-release task, not a Phase 2 blocker |

---

## Registry Safety

| Registry | Blocks Used | Safety Gate |
|----------|-------------|-------------|
| none (Android, no shadcn) | – | not applicable |

Asset provenance gate: All 4 drawables (`ic_floating_btn.xml`, `dialog_background_gradient.xml`, `ic_permission_needed.png`, `ic_logo.webp`), 6 string keys, and 5 color definitions are sourced directly from verified `R272` locations and slated for immediate import in Plan 1.

---

## Checker Sign-Off

- [x] Dimension 1 Copywriting: PASS
- [x] Dimension 2 Visuals: PASS
- [x] Dimension 3 Color: PASS
- [x] Dimension 4 Typography: PASS
- [x] Dimension 5 Spacing: PASS
- [x] Dimension 6 Registry Safety: PASS
- [x] Dimension 7 Inventory Provenance: PASS

**Approval:** APPROVED
