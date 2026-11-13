---
phase: 02-onboarding
plan: 02
status: completed
executed_at: 2026-11-13
---

# Plan 02-02 Summary: Splash Screen & Language Screen Onboarding

### Completed Actions:
1. Reconstructed `SplashScreen.kt` to 2.7.2 layout:
   - Logo card in `140.sdp` x `140.sdp` with `20.sdp` corner radius.
   - Title "Your Photo Manager" styled with `GalleryText.BodyMediumMedium` and `colorResource(R.color.titleColor)` with `5.sdp` bottom padding.
   - Replaced custom wavy canvas with M3 `LinearProgressIndicator` (width `120.sdp`, bottom padding `30.sdp`, `colorPrimary` and `splash_loader_track`).
   - Clean navigation to `Screen.Language.route` on fresh launch.
2. Upgraded `LanguageScreen.kt`:
   - Sized toolbar back button with `ic_back` and `2.sdp` padding.
   - Title styled with `GalleryText.BodyXLargeSemiBold` and `titleColor`.
   - Tick button sized `50.sdp` x `28.sdp` with `bg_rounded_red` shape and `colorPrimary` background.
   - Hand Lottie (`R.raw.hand`, `55.sdp`) visible in onboarding mode (`isOnboarding = true`).
   - Navigation wired: onboarding mode navigates directly to `Screen.Permission.route`.
3. Verified debug build with `./gradlew.bat :app:assembleDebug -q`.
