---
phase: 07-private-vault
plan: 05
subsystem: ui
tags: [compose, vault, settings, biometric]
requires:
  - phase: 07-01
    provides: reference strings, drawables, show-pattern default (showPatternValue)
  - phase: 07-03
    provides: VaultBiometric.unavailableReason as a string id, TYPE 1 / TYPE 2 lock routes
provides:
  - VaultSettingScreen (#34) as four flat reference rows under SharedMediaToolbar
  - vaultSettingRows (tested row contract) and fingerprintToggle (pure S6 decision)
affects: [07-09]
status: complete
actuals:
  tokens: 6000
  tasks: 2
  commits: 2
tech-stack:
  added: []
  patterns: [row contract as a tested Triple list, pure toggle decision returning (write, toast)]
key-files:
  created:
    - app/src/test/java/com/quizango/gallery/VaultSettingTest.kt
  modified:
    - app/src/main/java/com/quizango/gallery/ui/vault/VaultSettingScreen.kt
key-decisions:
  - "Row tap also toggles the switch on rows 2 and 3 (kept from the old screen); the reference layout XML was not re-checked for whether the row or only the switch is clickable"
  - "Switch colours copied from the Phase 6 Keep One Photo switch (thumb colorPrimary, track colorPrimary at 30%)"
requirements-completed: [VLT-01]
---

# Phase 7 Plan 05: Privacy Settings Summary

Privacy Settings is now the four flat 2.7.2 rows (Change Password, Show Pattern, Unlock with Fingerprint, Change Recovery Question) with reference strings, and the fingerprint switch writes the preference only when androidx reports a usable fingerprint.

## Commits
- ac12ada feat(07-05): Privacy Settings as four flat reference rows with SharedMediaToolbar (Task 1, tracer)
- e6948e9 feat(07-05): Show Pattern default on and fingerprint availability check (Task 2)

## Verification (run on this machine)
- Tracer gate (Task 1): `./gradlew.bat :app:testDebugUnitTest --tests "com.quizango.gallery.VaultSettingTest" -q && ./gradlew.bat :app:assembleDebug -q` -> exit 0 (1 test). Tracer verified, expansion proceeded.
- Final tree (Task 2): same command -> exit 0; VaultSettingTest 4 tests, 0 failures, 0 skipped.
- Acceptance greps: forbidden-pattern grep (TopAppBar, Surface(, English `title = "`/`subtitle = "`, raw `.sp`) 0; `R.string.privacy_settings` 1; `initial = true` 1; `unavailableReason(` 1.

## Behaviour now
- Toolbar `privacy_settings` (back via SharedMediaToolbar). Rows are flat (no card, no divider), 10.sdp padding, borderless ripple, icon at intrinsic size with 6.sdp padding, title `BodyMediumMedium` and subtitle `BodySmallRegular` in `titleColor` with 5.sdp start margin; the screen scrolls.
- Rows 1 and 4 end in `ic_inside` (10.sdp padding, content description = row title) and call `onNavigateToChangePin` / `onNavigateToRecoveryQuestion`, which GalleryNavHost routes to the TYPE 1 / TYPE 2 lock; the screen stays on Settings.
- Show Pattern switch collects `isPatternMode` with `initial = true` (the preference default is true via `showPatternValue`), writes `setPatternMode`.
- Fingerprint switch: `fingerprintToggle(enable, reason)` returns `(null, reason)` when turning on and unusable (nothing written, androidx message toasted, switch stays off because it reads the stored value), `(true, null)` when usable, `(false, null)` when turning off.

## Deviations from Plan

**1. Test and screen written together, no separate RED run** - Task 1 and Task 2 tests are pure-Kotlin checks of a list and a one-line function written alongside the code; the first run of each was the green gate. Test-first order was followed in authoring, not as separate failing runs.

**2. Toast and write are separate calls** - the plan said the switch "reverts"; the switch is bound to the stored preference, so not writing is the revert (no local state to reset).

## Decisions / accepted shortcuts
- `fingerprintToggle` takes the reason as a plain `Int?` (a `@StringRes` id) so it stays JVM-testable.
- Existing untouched installs that stored Show Pattern = false keep false; only a missing key reads on (07-01 `showPatternValue`).

## Known Stubs
None.

## Pending device checks (all PENDING-DEVICE, no phone, Artemis down)
- vault > settings icon > Privacy Settings: each of Change Password and Change Recovery Question opens its lock flow, success toast shows on return and the user stays on Settings (D7).
- Fingerprint on with no enrolled finger / no sensor: switch stays off and the androidx message shows.
- Row spacing, switch look, and long German/Russian/Tamil subtitles against 2.7.2 in light and dark (backstop).
- Whether the reference row (not only the switch) is tappable for the two switch rows.

## Threat Flags
None. T-07-17 mitigated (preference written only when BiometricManager reports usable, covered by fingerprintToggle tests); T-07-18 accepted as planned.

## Self-Check: PASSED
Commits ac12ada and e6948e9 present; VaultSettingScreen.kt and VaultSettingTest.kt exist; no deletions.
