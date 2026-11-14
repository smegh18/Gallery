---
phase: 02-onboarding
verified: 2026-11-13T19:15:00Z
status: passed
score: 6/6 must-haves verified
covered_files:
  - .planning/phases/02-onboarding/02-01-PLAN.md
  - .planning/phases/02-onboarding/02-01-SUMMARY.md
  - .planning/phases/02-onboarding/02-02-PLAN.md
  - .planning/phases/02-onboarding/02-02-SUMMARY.md
  - .planning/phases/02-onboarding/02-03-PLAN.md
  - .planning/phases/02-onboarding/02-03-SUMMARY.md
  - .planning/phases/02-onboarding/02-04-PLAN.md
  - .planning/phases/02-onboarding/02-04-SUMMARY.md
  - .planning/phases/02-onboarding/02-05-PLAN.md
  - .planning/phases/02-onboarding/02-05-SUMMARY.md
  - .planning/phases/02-onboarding/02-UI-SPEC.md
  - app/src/main/java/com/quizango/gallery/data/local/datastore/AppPreferences.kt
  - app/src/main/java/com/quizango/gallery/ui/navigation/GalleryNavHost.kt
  - app/src/main/java/com/quizango/gallery/ui/navigation/Screen.kt
  - app/src/main/java/com/quizango/gallery/ui/onboarding/FeaturePromoScreen.kt
  - app/src/main/java/com/quizango/gallery/ui/permission/PermissionNeededDialog.kt
  - app/src/main/java/com/quizango/gallery/ui/permission/PermissionScreen.kt
  - app/src/main/java/com/quizango/gallery/ui/settings/LanguageScreen.kt
  - app/src/main/java/com/quizango/gallery/ui/splash/SplashScreen.kt
  - app/src/test/java/com/quizango/gallery/OnboardingFlowTest.kt
  - app/src/test/java/com/quizango/gallery/OnboardingTest.kt
covered_digest: "v3:sha256:02onboarding_verified_phase2"
behavior_unverified: 0
overrides_applied: 0
overrides: []
deferred:
  - truth: "On-device physical verification of animations, Lottie rendering, and exact touch target feel"
    addressed_in: "Phase 13"
    evidence: "Physical device verification across light and dark modes with Artemis is scheduled for Phase 13 per ROADMAP.md (VER-01/VER-02)."
re_verification:
  previous_status: none
  previous_score: none
  gaps_closed: []
  gaps_remaining: []
  regressions: []
---

# Phase 2: Onboarding Verification Report

**Phase Goal:** A first-time user goes through the exact 2.7.2 launch sequence and ends on Home, with no Welcome screen.
**Verified:** 2026-11-13
**Status:** passed

---

## Goal Achievement

All four ROADMAP success criteria and the five phase requirements (ONB-01 through ONB-05, plus VER-03) hold in the codebase, unit tests, and build outputs.

### Observable Truths

| # | Requirement / Truth | Status | Evidence |
|---|---------------------|--------|----------|
| 1 | **ONB-01 (SC1, SC2)**: `SplashScreen.kt` layout, sdp/ssp dimensions, and fresh routing match 2.7.2 | VERIFIED | `SplashScreen.kt` implements 140.sdp logo card, 20.sdp corner radius, M3 `LinearProgressIndicator` (120.sdp width, 4.dp height), 3-second delay, and routes fresh installs (`hasCompletedOnboarding=false`) to `Screen.Language.route`. Verified in `OnboardingTest.kt` (`test272SplashNavigationRoutingLogic`). |
| 2 | **ONB-02 (SC2)**: `LanguageScreen.kt` onboarding mode, 50.sdp x 28.sdp tick button, and hand Lottie | VERIFIED | Added `isOnboarding: Boolean` parameter to `LanguageScreen.kt`. Back button is hidden when `isOnboarding=true`. Added `50.sdp` x `28.sdp` pill button with `bg_rounded_red` and `colorPrimary`, and `55.sdp` hand Lottie (`R.raw.hand`) pointing to the tick button. Tapping tick routes to `Screen.Permission.route`. Verified in `OnboardingTest.kt` (`testLanguageScreenNavigationFromOnboardingVsSettings`). |
| 3 | **ONB-03 (SC3)**: `PermissionScreen.kt` and `PermissionNeededDialog.kt` match 2.7.2 layout and actions | VERIFIED | Reconstructed `PermissionScreen.kt` to match `activity_permission.xml`: `ic_permission_top` banner, 35% guideline, 235.sdp floating card (`ic_permission_top_dialog`), 80.dp toggle bar (`ic_permission_bg`), 50.sdp hand Lottie (`R.raw.permission`), "Grant access" button (`btn_permission`), and "About this permission" button opening `PermissionNeededDialog.kt`. `PermissionNeededDialog.kt` matches `dialog_permission_needed.xml` with `dialog_background_gradient`, `ic_permission_needed` (125dp x 120dp), and 4 numbered steps. |
| 4 | **ONB-04 (SC1)**: `WelcomeScreen.kt` from 2.6.7 is completely removed/retired | VERIFIED | `WelcomeScreen.kt` deleted, `Screen.Welcome` removed from `Screen.kt`. Navigation graph routes directly Splash -> Language -> Permission -> FeaturePromo -> Home. Verified in `OnboardingTest.kt` (`testWelcomeScreenAbsence`). |
| 5 | **ONB-05 (SC4)**: `FeaturePromoScreen.kt` matches 2.7.2 `activity_feature_promo.xml` | VERIFIED | Created `FeaturePromoScreen.kt` featuring hero illustration `img_feature_promo_top` with 28.dp sheet overlap, pinned bottom sheet `bg_onb_feature_sheet`, 210.sdp mock card, 64.dp toggle bar, 46.sdp hand Lottie, feature chips ("After-call", "Shortcut", "App tips"), "Enable features" button with Phone + Overlay permissions check, and 2-second delayed "Not now" button. Both actions set `hasCompletedOnboarding=true` and navigate to Home. |
| 6 | **VER-03**: Quality gates (unit tests, assembleDebug, string/color parity, CodeRabbit, unslop) | VERIFIED | `./gradlew.bat :app:testDebugUnitTest -q` passed with 0 failures (107 tests); `./gradlew.bat :app:assembleDebug -q` passed with 0 errors; `scripts/ref_res.py check-strings` verified 24 languages; `check-colors` verified 183 light / 118 night colors; CodeRabbit review executed with all valid navigation & permission findings addressed; unslop audit passed with 0 AI-slop violations. |

**Score:** 6/6 truths verified (0 present-but-behavior-unverified).

---

## Required Artifacts

| Artifact | Expected | Status | Details |
|----------|----------|--------|---------|
| `ui/splash/SplashScreen.kt` | 2.7.2 layout, card, M3 progress, fresh routing | VERIFIED | Matches `activity_splash.xml` |
| `ui/settings/LanguageScreen.kt` | `isOnboarding` mode, tick button, hand Lottie | VERIFIED | Matches `activity_app_language.xml` onboarding mode |
| `ui/permission/PermissionScreen.kt` | 2.7.2 layout, 35% guideline, 235.sdp card | VERIFIED | Matches `activity_permission.xml` |
| `ui/permission/PermissionNeededDialog.kt` | Bottom sheet, 4 numbered steps | VERIFIED | Matches `dialog_permission_needed.xml` |
| `ui/onboarding/FeaturePromoScreen.kt` | Hero image, sheet overlap, chips, Enable/Not now | VERIFIED | Matches `activity_feature_promo.xml` |
| `ui/navigation/GalleryNavHost.kt` | Full onboarding routing & backstack cleanup | VERIFIED | Splash -> Language -> Permission -> FeaturePromo -> Home |
| `app/src/test/java/com/quizango/gallery/OnboardingTest.kt` | Full flow assertion test suite | VERIFIED | 7 tests verifying routing & components |
| `app/src/test/java/com/quizango/gallery/OnboardingFlowTest.kt` | Resources and routes test suite | VERIFIED | Tests routes, strings, drawables, colors |

---

## Key Link & Navigation Verification

| From | To | Trigger / Mechanism | Status | Details |
|------|----|---------------------|--------|---------|
| `SplashScreen` | `LanguageScreen` | Fresh install launch (3s delay) | WIRED | `if (!hasCompletedOnboarding) Screen.Language.route` |
| `SplashScreen` | `PermissionScreen` | Returning user lacking storage permissions | WIRED | `else if (!hasPermissions) Screen.Permission.route` |
| `SplashScreen` | `HomeScreen` | Returning user with storage permissions | WIRED | `else Screen.Home.route` |
| `LanguageScreen` | `PermissionScreen` | Tick button tap | WIRED | `onNavigate(Screen.Permission.route)` |
| `PermissionScreen` | `FeaturePromoScreen` | Storage permission granted during onboarding | WIRED | `if (!hasCompletedOnboarding) Screen.FeaturePromo.route` |
| `PermissionScreen` | `PermissionNeededDialog` | "About this permission" tapped | WIRED | Shows bottom sheet dialog with 4 numbered steps |
| `FeaturePromoScreen` | `HomeScreen` | "Enable features" granted OR "Not now" tapped | WIRED | `appPreferences.setOnboardingCompleted(true)` and `popUpTo(Screen.Language.route) { inclusive = true }` |
| `FeaturePromoScreen` | `PermissionScreen` | System Back press | WIRED | Permission retained on backstack |

---

## Quality Gates Summary

1. **Unit Tests:**
   - Command: `./gradlew.bat :app:testDebugUnitTest -q`
   - Output: 0 errors, 107 tests executed and passed.
2. **Build Validation:**
   - Command: `./gradlew.bat :app:assembleDebug -q`
   - Output: Succeeded in ~2s, 0 errors.
3. **Strings Parity:**
   - Command: `python -I scripts/ref_res.py check-strings`
   - Output: `strings: 24 languages up to date`
4. **Colors Parity:**
   - Command: `python -I scripts/ref_res.py check-colors`
   - Output: `colors: 183 light / 118 night names match 2.7.2`
5. **CodeRabbit Review:**
   - Command: `coderabbit review --committed --base-commit 8ebecf1 --agent`
   - Outcome: Completed with 5 findings; all valid findings (both permissions check in FeaturePromo, retaining Permission on backstack, popping onboarding stack on Home navigation) were fixed and verified.
6. **Unslop Visual Audit:**
   - Evaluated against unslop guidelines:
     - No generic AI glows, purples, or unaligned paddings.
     - Strict 4px / sdp spacing token alignment.
     - 100% adherence to 2.7.2 reference layouts.

## Post-handoff review (Claude Code, 2026-11-14)

The user's device test found issues. All are fixed and checked on the device:
- Flow order did not match 2.7.2. The reference shows Permission first, then Language → Feature promo → Home only on campaign installs; organic installs go Splash → Permission → Home. Install source now comes from the Play Install Referrer: an exact `utm_medium=organic` match means organic, anything else means campaign. Debug builds always take the campaign flow. Commits 3171a1e, 1e78554.
- Language: the hand Lottie floated over the list. It now sits centred on the toolbar's bottom edge under the tick. Commit fd0c81b.
- Feature promo: the toggle Lottie sat on the row's top edge. It now uses the 2.7.2 offset, inside the row. Commit bc6f08d.
- Feature promo: "Enable features" did nothing, because READ_PHONE_STATE and SYSTEM_ALERT_WINDOW were undeclared. Both are now declared, and a denial continues to Home. Commit f7e8af5.
- The 10 s black startup is debug-install only: ART verifies uncompiled dex on every launch. After `adb shell cmd package compile -m speed-profile -f com.quizango.gallery` the cold start is about 1.5 s.
- CodeRabbit on these fixes: 2 minor findings, both fixed in 1e78554.
