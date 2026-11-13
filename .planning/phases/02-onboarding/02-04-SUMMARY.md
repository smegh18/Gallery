---
phase: 02-onboarding
plan: 04
status: completed
executed_at: 2026-11-13
---

# Plan 02-04 Summary: Feature Promo Screen & E2E Onboarding Flow

### Completed Actions:
1. Created `FeaturePromoScreen.kt` matching reference 2.7.2 `activity_feature_promo.xml` (checklist #5, ONB-05):
   - Hero illustration `img_feature_promo_top` with 28.dp bottom sheet overlap.
   - Pinned bottom sheet with `bg_onb_feature_sheet` (24.dp rounded top corners, 6.sdp elevation).
   - Mock card (`ic_permission_top_dialog`, 210.sdp width), toggle bar (`ic_permission_bg`, 64.dp height, -22.dp overlap), and hand Lottie (`R.raw.permission`, 46.sdp, -40.dp offset).
   - Heading "Get more from your Gallery" in `17.ssp` `BodyXXLargeSemiBold` using `black` color (inverted in dark mode).
   - Subtitle in `11.ssp` `BodyMediumRegular` using `op_onboarding_ac_prem_desc`.
   - Feature chips: "After-call" (`ic_gs_call`), "Shortcut" (`ic_floating_btn`), "App tips" (`explore_tab_ic_cleaner`) styled with `bg_onb_feature_chip` and `iu_primary`.
   - Permission note in `10.ssp` `BodyMediumRegular`.
   - "Enable features" button requesting `READ_PHONE_STATE` and `Settings.canDrawOverlays`, setting onboarding completed, and forwarding to Home.
   - "Not now" button delayed by 2 seconds via `LaunchedEffect(Unit) { delay(2000); showSkipButton = true }`, setting onboarding completed and forwarding to Home.
2. Updated `GalleryNavHost.kt`:
   - Added `composable(Screen.FeaturePromo.route)`.
   - Full flow established: Splash -> Language -> Permission -> FeaturePromo -> Home.
3. Verified compilation with `./gradlew.bat :app:assembleDebug -q`.
