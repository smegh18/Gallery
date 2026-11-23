---
phase: 07-private-vault
plan: 09
subsystem: verification
tags: [gates, review, device-matrix, parity-checklist]
requires:
  - phase: 07-01
    provides: resources, lock rules
  - phase: 07-08
    provides: Add to Private lock gate
provides:
  - 07-GATE-FINDINGS.md (five gates PASS, manual review, unslop audit, dispositions)
  - 07-LIVE-COMPARISON.md (rows #32-#37, UI states, backstops, deviation ledger, fixture procedure, capture list; all PENDING-DEVICE)
  - Phase 7 known-deviation note in PARITY-CHECKLIST.md (no box changed)
affects: []
status: "partial — device acceptance pending"
actuals:
  tokens: 14000
  tasks: 2
  commits: 1
key-files:
  created:
    - .planning/phases/07-private-vault/07-GATE-FINDINGS.md
    - .planning/phases/07-private-vault/07-LIVE-COMPARISON.md
  modified:
    - .planning/milestones/v2.7.2-parity/PARITY-CHECKLIST.md
key-decisions:
  - "No defect accepted from the manual review, so no correction plan 07-10+ exists"
  - "Ported for #32-#37 stays unticked; Task 3 is open until a phone runs the matrix (D7)"
requirements-completed: []
---

# Phase 7 Plan 09: Phase close gates and device matrix Summary

Phase 7 builds as one: 342 unit tests, assembleDebug, string and colour checks and `git diff --check` all pass, a manual review of the whole diff found no defect to route, and the device pass is written down in full but not run, because no phone is attached and Artemis is down.

## Status
Partial: Tasks 1 and 2 done, Task 3 (device acceptance) NOT run and not faked. Rows #32-#37 are not Ported, none Verified. STATE.md and ROADMAP.md were not touched. No push.

## Task 1: gates and review (07-GATE-FINDINGS.md)

| Gate | Exact command | Result |
|------|---------------|--------|
| Unit suite | `./gradlew.bat :app:testDebugUnitTest -q` (also forced with `--rerun`) | PASS: 54 suites, 342 tests, 0 failures/errors/skipped |
| Build | `./gradlew.bat :app:assembleDebug -q` | PASS (incremental on an unchanged tree) |
| Strings | `python -I scripts/ref_res.py check-strings` | PASS: 24 languages up to date |
| Colours | `python -I scripts/ref_res.py check-colors` | PASS: 226 light / 134 night match |
| Whitespace | `git diff --check` | PASS |

- **CodeRabbit: NOT RUN, still owed.** The CLI is not installed (`command -v coderabbit cr` empty); not installed per instruction. The intended command is `coderabbit review --base 398add0`. A manual review of `398add0..HEAD` stood in. VER-03 "CodeRabbit with no open findings" stays open.
- Manual sweep (vault dir and diff): no English user copy, no Material app bars/AlertDialog/dropdowns/material-icons, no raw `sp`, no fake data, no appearance-only controls, only two accurate `shortcut:` comments; broad catches are confined to `VaultRepository` file operations that roll back and return false.
- Unslop audit over the six screens (lock, vault media, settings, recovery question, forgot password, picker): no gradients, glow, emoji, tracked caps or hard-coded colours. FAB elevation value is the only item to confirm on a device.
- Findings: F1 (outer `catch (_: Exception)` swallows a rethrown cancellation in `moveToVault`/`restoreFromVault`) rejected as low risk, no data loss; F2 (Bundle size of saved paths) and F3 (rotation mid-setup restarts entry) rejected as documented shortcuts; F4 CodeRabbit owed. No correction plans written.

## Task 2: matrix (07-LIVE-COMPARISON.md)
Created with: shared gate evidence; six row entries (#32-#37); 19 UI-SPEC state entries; 18 backstops (A1, A2, A3, A4, A8, P4, toast gravity, PIN glyph, pattern tints, biometric delay, recovery selector arrow, settings row tap, thumbnails/MIUI, German/Russian/Tamil, Arabic RTL, light, dark, picker loader and tray arrow, rotation); deviation ledger (D2, D3, hashed storage, no shake D4, `vault_click` not built, secured toast overlay, UI-SPEC section 6 picker correction, vault Share decrypt, accepted limits); the ZZPhase7Test fixture-root procedure; the Wave 0 capture list. Every result is PENDING-DEVICE. Checklist verification script passed; the note "Phase 7 known deviations" sits under row 37; rows 32-37 are unchanged.

## Task 3: device acceptance, PENDING-DEVICE
Precondition unmet (`adb devices` empty, Artemis MCP `CONNECT_TIMEOUT`). Checkpoint stays open (D7).

### Recording the ZZPhase7Test fixture root (checker warning)
When a phone is attached, before anything destructive:
1. `adb devices` shows one authorized device. `ROOT=ZZPhase7Test-$(date +%Y%m%d-%H%M)`.
2. `adb shell mkdir -p /sdcard/Pictures/$ROOT`; `adb push` disposable files into it: at least two distinct photos, one animated GIF/WebP, one strongly coloured image, one short video.
3. Index each file: `adb shell am broadcast -a android.intent.action.MEDIA_SCANNER_SCAN_FILE -d file:///sdcard/Pictures/$ROOT/<file>`.
4. Write `ROOT`, `adb shell ls -l /sdcard/Pictures/$ROOT`, device serial and Android version into the "Fixture root and Wave 0 reference captures" section of `07-LIVE-COMPARISON.md` (replacing "none yet"); create the screenshot directory.
5. Before every Hide/Unhide/Delete confirmation check each selected path starts with `/storage/emulated/0/Pictures/<ROOT>/`; if not, cancel and stop.
6. Unhide all fixtures before Clear data (it deletes encrypted vault files).

### Everything to run on a device (aggregated from 07-01..07-08)
Install: `adb install -r app/build/outputs/apk/debug/app-debug.apk` beside reference 2.7.2; take the Wave 0 captures first.

- **07-01 (lock rules, resources):** toast copy; 4-dot pattern minimum (A1); wrong-pattern silence (red path only); Show Pattern default on for a fresh install.
- **07-02 (safe hide/restore):** hide and unhide on a real MIUI device: gallery row disappears/reappears at once (`scanFiles` not unit tested); unhide never overwrites a same-named fixture.
- **07-03 (lock screen and routing):** Explore > Vault on a fresh install: Set PIN, Re-enter, three questions, vault; Back from the vault returns to Explore. PIN and pattern screens side by side in light and dark (measurements, PIN glyph, pattern dot/path colours and sizes). Fifth wrong attempt opens Forgot; fingerprint must not open the vault afterwards (A2); biometric title and 600 ms delay (A4). Settings > Change Password and Change Recovery Question with toasts. Back from Forgot after lockout versus after the link. Arabic RTL: slots and keypad stay left to right. Kill and reopen after lockout: still Forgot.
- **07-04 (recovery and forgot):** first setup reaches the question screen, three different questions, Submit opens the vault; wrong answer toasts, right answer returns to Set PIN; system Back on the question screen wipes the half-made lock and returns to Explore; system Back on Forgot after lockout closes the lock; selector popup width/position, answer field, soft keyboard in light and dark; long German/Russian/Tamil strings and the Next/Submit label (A8); whether the reference selector shows a drop arrow.
- **07-05 (settings):** each of Change Password and Change Recovery Question opens its flow, success toast on return, user stays on Settings; fingerprint on with no enrolled finger or no sensor keeps the switch off and shows the androidx message; row spacing, switch look, long subtitles in light and dark; whether the whole row is tappable for the two switch rows.
- **07-06 (vault media):** select a fixture, Unhide, Ok: "Unhiding", toast, photo back in Photos at once (MIUI); empty, populated, selection and delete side by side with 2.7.2 in light and dark; decrypt speed on a large vault; first setup shows the secured toast once (position and duration versus 2.7.2); `adb shell run-as com.quizango.gallery ls cache/vault_view` empty after leaving the vault, and not emptied while the viewer or picker is open; Share a vault photo: the target receives the decrypted image.
- **07-07 (picker):** Vault > Add Items, pick two fixtures, Add: both appear in the vault and vanish from Photos at once; folder row timing (row always visible, Add and tray only with a selection), 4-column grid, cell and tray look versus 2.7.2 in light and dark (P4); folder list open/close, folder switch with a selection kept, tray strip expand and remove, empty folder, Back order, long folder names, loader after a folder switch.
- **07-08 (lock gate):** after Clear data, Add to Private from Photos, an album, Favorites, Animated, Search by colour and the viewer: each runs setup then the Hide dialog, Ok hides and the list updates at once (MIUI); Back during setup (lock screen and question screen) hides nothing, on each of the six entry points; viewer path hides the same photo after setup.
- **07-09 cross-cutting:** German, Russian, Tamil long strings; Arabic RTL; light and dark side by side; PIN glyph; secured-toast gravity; FAB elevation.

When all pass: tick Ported only for #32-#37 in `PARITY-CHECKLIST.md`, leave Verified, tick VLT-01 and VLT-02 in REQUIREMENTS.md at phase verification (07-07 left VLT-02 unchecked on purpose), then rerun the Task 3 verify command. On any failure, write 07-10+ correction plans via Task 1's route and retest.

## Deviations from Plan
- **[Environment]** `gsd-tools windows append` was not run: `.planning/WINDOWS.md` already holds an invalid pre-existing row (entry 4, kind "deferred") that makes the verb error; the file was left untouched (out of scope, ledger is best-effort). The open device acceptance and the CodeRabbit gap are recorded in this summary and in 07-LIVE-COMPARISON.md / 07-GATE-FINDINGS.md instead.

No other deviations. No app source was edited.

## Decisions
- No accepted review finding, hence no correction plan.
- Commit contains only planning files.

## Known Stubs
None.

## Threat Flags
None. T-07-28 (fixture confinement) is a documented procedure; no device action occurred. T-07-29 held: no Ported or Verified box changed. T-07-30: no correction plan needed.

## Self-Check: PASSED
07-GATE-FINDINGS.md, 07-LIVE-COMPARISON.md exist; checklist verify script passed; rows 32-37 still `[ ] | [ ]`; STATE.md and ROADMAP.md unmodified.
