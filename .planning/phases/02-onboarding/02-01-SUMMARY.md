---
phase: 02-onboarding
plan: 01
status: completed
executed_at: 2026-11-13
---

# Plan 02-01 Summary: Assets, Resources & Welcome Screen Retirement

### Completed Actions:
1. Imported 4 missing drawables from reference 2.7.2:
   - `ic_floating_btn.xml`
   - `dialog_background_gradient.xml`
   - `ic_permission_needed.png`
   - `ic_logo.webp`
2. Added missing color tokens to `app/src/main/res/values/colors.xml` and `values-night/colors.xml`:
   - `gray_permission`: #7A8389 / #AAAAAA
   - `op_onboarding_ac_prem_desc`: #373742 / #AAAAAA
   - `op_onboarding_desc_color`: #7A8389 / #AAAAAA
   - `per_text_color`: #1E3354 / #FFFFFF
   - `radioButtonUncheckedColor`: #7A8389
3. Added 6 permission dialog strings to `res/values/strings.xml` and synchronized translations across all 24 language files with `python -I scripts/ref_res.py sync-strings`.
4. Verified string and color parity:
   - `strings: 24 languages up to date`
   - `colors: 183 light / 118 night names match 2.7.2`
5. Retired `WelcomeScreen.kt` and `Screen.Welcome`.
6. Updated `AppPreferences.kt` with `hasCompletedOnboarding` and `setOnboardingCompleted`.
7. Updated `SplashScreen.kt` and `GalleryNavHost.kt` to route directly from Splash to Language.
8. Verified clean compilation with `./gradlew.bat :app:assembleDebug -q`.
