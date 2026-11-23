---
phase: 07-private-vault
plan: 03
subsystem: ui
tags: [compose, vault, navigation, biometric, lock-screen]
requires:
  - phase: 07-01
    provides: VaultLockFlow, VaultGate, lock-state preferences, reference strings
provides:
  - VaultLockScreen driven by VaultLockFlow (type 0/1/2, openVault true/false)
  - Screen.VaultLockFor(type, openVault) and vault flow routing in GalleryNavHost
  - VaultBiometric on reference strings
affects: [07-04, 07-05, 07-06, 07-08, 07-09]
status: complete
actuals:
  tokens: 40000
  tasks: 3
  commits: 3
tech-stack:
  added: []
  patterns: [lock screen reports typed results and the NavHost owns all routing, rememberSaveable for state that must survive a covering screen]
key-files:
  created: []
  modified:
    - app/src/main/java/com/quizango/gallery/ui/vault/VaultLockScreen.kt
    - app/src/main/java/com/quizango/gallery/ui/vault/VaultLockFlow.kt
    - app/src/main/java/com/quizango/gallery/ui/vault/VaultBiometric.kt
    - app/src/main/java/com/quizango/gallery/ui/vault/VaultSettingScreen.kt
    - app/src/main/java/com/quizango/gallery/ui/navigation/Screen.kt
    - app/src/main/java/com/quizango/gallery/ui/navigation/GalleryNavHost.kt
    - app/src/test/java/com/quizango/gallery/VaultTest.kt
key-decisions:
  - "Lockout check requires a stored lock: once Forgot Password wiped it, the lock shows Set PIN instead of looping back to Forgot"
  - "wrongCount is seeded into VaultLockFlow and held with rememberSaveable so the Forgot link plus Back cannot reset the five-attempt counter"
  - "Show Pattern off hides the path in setup too (reference stealth mode), replacing the old always-show-while-setting-up rule"
requirements-completed: [VLT-01]
---

# Phase 7 Plan 03: Lock screen and vault routing Summary

The lock (#32) is a thin renderer over the unit-tested `VaultLockFlow` in `activity_vault_lock` measurements, and every vault flow (first setup, unlock, lockout, Change Password, Change Recovery Question, Add to Private setup) routes through typed callbacks handled in `GalleryNavHost`.

## Commits
- b563736 feat(07-03): lock screen driven by VaultLockFlow with vault routing in the NavHost (Task 1, tracer)
- d8e3275 feat(07-03): rebuild lock screen chrome to activity_vault_lock (Task 2)
- 863cd76 feat(07-03): Settings and Add to Private lock routes, reference fingerprint prompt copy (Task 3)

## Verification (run on this machine)
- Tracer gate (before Task 2/3): `./gradlew.bat :app:testDebugUnitTest --tests "com.quizango.gallery.VaultLockFlowTest" --tests "com.quizango.gallery.VaultTest" -q` -> exit 0; `./gradlew.bat :app:assembleDebug -q` -> exit 0. Tracer verified, expansion proceeded.
- Task 2: `./gradlew.bat :app:assembleDebug -q` -> exit 0.
- Task 3 / final tree: `./gradlew.bat :app:testDebugUnitTest -q` (whole suite, includes VaultTest route assertions for `VaultLockFor`) -> exit 0; `./gradlew.bat :app:assembleDebug -q` -> exit 0.
- Acceptance greps: `VaultLockFlow(` 1, `setPrivateLock(true)` 1, `isPrivateLock` 1, `clearVaultLock()|setPrivateToastShow(true)` in NavHost 2, `Screen.Vault` in VaultLockScreen 0; TopAppBar/Animatable/0xFF1A5BD6/raw `.sp` 0, `SharedMediaToolbar(` 1, five lock colours 5, sdp sizes 4, `R.string.delete|confirm` 2; `change_lock` 0 hits, `VaultLockFor.createRoute(1|2, false)` 2, `settingsToast(` 2, androidx fingerprint string 1, `R.string.unlock_vault` 1, literal biometric copy 0.

## Behaviour now
- First use: Set PIN -> Re-enter PIN (or pattern pair) -> Recovery Question -> vault replaces the lock. Back from the question pops question and lock and, when no question was saved, wipes the half-made lock (VL.o00Ooo). Saving the question from a type-0 lock sets `isPrivateToastShow`.
- Fifth wrong attempt stores `isPrivateLock` and opens Forgot Password; later opens go straight there before any fingerprint prompt (while a lock is stored). Back from Forgot closes the lock too when `isPrivateLock` is true (VL.o00Oo0), otherwise returns to the lock. A right answer pops back to the lock, which reloads as Set PIN.
- Settings > Change Password opens `vault/lock/1/false`: old PIN/pattern, then Set PIN with toolbar "Private Vault", saves, pops to Settings with "Password changed successfully.". Settings > Change Recovery Question opens `vault/lock/2/false`, unlocks, then the question screen, then back to Settings with "Recovery question changed successfully.". Settings never navigates to the vault.
- Add to Private gate (`openVault=false`) pops itself after setup and recovery; wiring of the gate callers is 07-08.
- Look: SharedMediaToolbar, lock icon 40.sdp from the top, 36x30.sdp slots with 1.5.dp underline, 50x47.sdp keypad cells, 260.sdp pattern view (green/red verdict, clears after 30 ms), forgot and switch links per flow, no shake. Slots and keypad forced LTR.
- Fingerprint: type 0 only, lock stored, preference on, hardware usable, after the lockout check, 600 ms after open, once per entry. Title `private_vault`, subtitle `unlock_vault`, negative `cancel`; unavailable reasons are the androidx `fingerprint_error_*` strings.

## Deviations from Plan

**1. [Rule 3 - Blocking] VaultSettingScreen.kt touched** - `unavailableReason` now returns a string resource id, so its one caller wraps it in `getString`. One line; the screen itself is rebuilt in 07-05.

**2. [Rule 1 - Bug] Lockout loop on return from Forgot** - the plan's "isPrivateLock true -> open Forgot on open" would loop forever here: the lock composition is disposed while Forgot is on top, so it reloads (isPrivateLock is still true) after the answer is verified. The check now also requires a stored lock; Forgot already wiped it, so the lock reloads as Set PIN. Side effect: a user who answers Forgot and then backs out of Set PIN with no lock stored sees Set PIN on the next open instead of Forgot (nothing left to protect). `saveVaultLock` still clears the flag.

**3. [Rule 2 - Security] wrong-attempt count survives Forgot** - same disposal would reset `wrongCount` each time the user took the "Forgot password?" link and pressed Back, giving unlimited attempts. Added an optional `wrongCount` constructor argument to `VaultLockFlow` (default 0, existing tests unaffected) and keep the count with `rememberSaveable`.

**4. Replaced `resetAfterForgot()` use** - the plan said to call it when `isVaultLockSet` turns false while verifying. Because the lock reloads from scratch after Forgot, a flow built with `lockSet=false` and type forced to 0 is the same state, so `resetAfterForgot()` is not called from the screen (it stays in the tested flow).

**5. Show Pattern in setup** - pattern path now follows the Show Pattern setting during setup too (reference stealth mode). The old code always drew the path while setting up.

## Decisions / accepted shortcuts
- PIN slot glyph is the filled dot U+25CF at `pin_style` size (plan assumption), marked `shortcut:` in code until a device capture shows the reference glyph.
- Pattern drawing: unselected dots `titleColor` 12.dp, selected dots and path in the state colour (`patternColor` / green / red), selected dot 28.dp, path 3.dp. How the reference tints dots versus path is not in the XML; device backstop.
- Screen state other than `wrongCount` and the one-time fingerprint flag is `remember`ed, so a configuration change mid-setup restarts the lock entry (same as the old screen).
- Recovery Back via the system back gesture still depends on the question screen (rebuilt in 07-04); the toolbar Back path is the one wired to wipe the half-made lock.

## Known Stubs
None.

## Pending device checks (all PENDING-DEVICE, no phone, Artemis down)
- Explore > Vault on a fresh install: Set PIN, Re-enter, Recovery Question, vault; Back from the vault returns to Explore.
- Lock PIN and pattern screens side by side with 2.7.2 in light and dark (measurements, pin glyph, dot/path colours).
- Fifth wrong attempt -> Forgot; fingerprint does not open the vault afterwards (A2); prompt title and delay (A4, 600 ms).
- Settings > Change Password and Change Recovery Question flows with toasts; Back from Forgot after lockout versus after the link.
- Arabic RTL: slots and keypad stay left to right.

## Threat Flags
None. T-07-09..T-07-11 and T-07-13 are mitigated as planned (lockout before biometric, vault replaces the lock, old lock kept until the new one saves, half-made lock wiped on question Back).

## Self-Check: PASSED
Commits b563736, d8e3275, 863cd76 present; modified files exist; no unexpected deletions.
