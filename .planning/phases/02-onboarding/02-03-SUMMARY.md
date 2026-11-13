---
phase: 02-onboarding
plan: 03
status: completed
executed_at: 2026-11-13
---

# Plan 02-03 Summary: Permission Screen & Permission Needed Dialog

### Completed Actions:
1. Created `PermissionNeededDialog.kt` matching reference 2.7.2 `dialog_permission_needed.xml`:
   - Bottom-sheet dialog with 30.dp rounded top corners and gradient background.
   - 125dp x 120dp illustration (`R.drawable.ic_permission_needed`).
   - Title `why_this_permission_needed`, description `call_permission_need`.
   - 4 numbered steps in 9.ssp `BodyMediumMedium` using `radioButtonUncheckedColor`.
   - Full-width "Grant access" button in `14.ssp` `BodyLargeSemiBold` with `colorPrimary`.
2. Aligned `PermissionScreen.kt` with reference 2.7.2 `activity_permission.xml`:
   - Top image `ic_permission_top` with alpha 0.7.
   - Mock settings card (235.sdp) with rotated arrow, `ic_logo`, and app name.
   - Toggle bar (`ic_permission_bg`, 80.dp height, -25.dp overlap) with "Allow access to manage all files" (`12.ssp`).
   - Hand Lottie (`R.raw.permission`, 50.sdp, -53.dp overlap, zIndex 2f).
   - "Permission Required" heading (18.ssp) in `per_text_color`.
   - Description in `11.ssp` Inter Medium using `gray_permission`.
   - "Grant access" pill button and "About this permission" link.
   - Tapping "About this permission" opens `PermissionNeededDialog`.
   - Granting permission routes to `Screen.FeaturePromo.route` during onboarding.
3. Updated `GalleryNavHost.kt` to pass `appPreferences` to `PermissionScreen`.
4. Verified compilation with `./gradlew.bat :app:assembleDebug -q`.
