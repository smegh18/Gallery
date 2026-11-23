---
phase: 07-private-vault
plan: 01
subsystem: ui
tags: [compose, vault, strings, datastore, state-machine]
requires:
  - phase: 06-organizer-cleaner
    provides: ref_res.py sync-strings/check-colors tooling
provides:
  - Phase 7 reference strings (25 files), colours, drawables
  - VaultLockFlow pure state machine, settingsToast
  - VaultGate lock checks; lock-state preferences
affects: [07-02, 07-03, 07-04, 07-05, 07-06, 07-07, 07-08, 07-09]
status: complete
actuals:
  tokens: 60000
  tasks: 3
  commits: 3
tech-stack:
  added: []
  patterns: [pure state machine with injected matcher, 2.7.2 key names for new DataStore keys]
key-files:
  created:
    - app/src/main/java/com/quizango/gallery/ui/vault/VaultLockFlow.kt
    - app/src/main/java/com/quizango/gallery/ui/vault/VaultGate.kt
    - app/src/test/java/com/quizango/gallery/VaultLockFlowTest.kt
    - app/src/test/java/com/quizango/gallery/VaultGateTest.kt
  modified:
    - app/src/main/res/values/strings.xml (+24 generated locale files)
    - app/src/main/res/values/colors.xml, values-night/colors.xml
    - app/src/main/java/com/quizango/gallery/ui/vault/VaultLockScreen.kt
    - app/src/main/java/com/quizango/gallery/data/local/datastore/AppPreferences.kt
    - app/src/main/java/com/quizango/gallery/ui/vault/VaultSettingScreen.kt
key-decisions:
  - "Show Pattern default flipped to on via showPatternValue(null)=true; key is_vault_pattern_mode unchanged"
  - "VaultLockFlow takes a matcher lambda (String)->Boolean so hashes stay in VaultSecret"
requirements-completed: [VLT-01, VLT-02]
---

# Phase 7 Plan 01: Resources and lock rules Summary

Phase 7 reference strings, colours and drawables are in, the lock screen speaks 2.7.2 copy, and the vault lock rules (setup, verify, lockout, TYPE 0/1/2 routing, Settings toast) live in a unit-tested pure `VaultLockFlow`.

## Commits
- 05ce5dd feat(07-01): resources and reference lock copy (Task 1, tracer)
- bbb51a3 feat(07-01): VaultLockFlow state machine with tests (Task 2)
- 4c4dbb1 feat(07-01): Show Pattern default, lock-state preferences, VaultGate (Task 3)

## Verification (run on this machine)
- `python -I scripts/ref_res.py check-strings` -> 24 languages up to date; `check-colors` -> 226 light / 134 night match.
- `./gradlew.bat :app:processDebugResources -q` -> exit 0.
- `./gradlew.bat :app:testDebugUnitTest --tests "com.quizango.gallery.VaultLockFlowTest" --tests "com.quizango.gallery.VaultGateTest" :app:assembleDebug -q` -> exit 0; VaultLockFlowTest 13/13, VaultGateTest 3/3 pass.
- Plan acceptance greps: 5 string keys found, 0 literal makeText strings, 8 R.string hits in VaultLockScreen.kt.
- The tests and assembleDebug ran once on the final tree; the three commits were made afterwards from that same tree, not built separately.

## Deviations from Plan

**1. [Rule 3 - Blocking] Four extra colours** - the imported drawables reference `toast_lock`, `security_answer_stroke`, `ad_title_color` and `bgColor`, which did not exist; added with R272 light values (and the night value where R272 has one: `ad_title_color`). Also added R272 night values for the other planned colours where they exist. Files: values/colors.xml, values-night/colors.xml. Commit 05ce5dd.

**2. [Scope] Show Pattern initial value** - `collectAsState(initial = false)` changed to `true` in VaultLockScreen.kt (in the Task 1 commit) and VaultSettingScreen.kt (Task 3 commit) so the first frame matches the new default.

**3. Lock screen toolbar** stays "Private Vault" in changeMode (the Change Password title needs the type-1 verify step that 07-03 adds). Change-mode save toast now uses `password_changed_successfully`.

## Decisions
- `submitPin` rejects any entry that is not exactly 4 digits with `please_enter_pin` in every stage (setup and verify); the screen also toasts on a short check-mark press.
- Pattern first-draw minimum (4) applies only while setting up the first entry; confirm and verify have no minimum.
- `afterSuccess`: no recovery question or type 2 -> Recovery; type 1 verifying -> Stay (flips to type-0 PIN setup); type 0 -> Vault if openVault else ResultOk(passwordSaved).

## Known Stubs
None.

## Pending device checks
PENDING-DEVICE: none run (no phone, Artemis down). Toast copy, 4-dot minimum, wrong-pattern silence and Show Pattern default are covered only by unit tests and the build.

## Threat Flags
None.

## Self-Check: PASSED
Commits 05ce5dd, bbb51a3, 4c4dbb1 present; created files exist.
