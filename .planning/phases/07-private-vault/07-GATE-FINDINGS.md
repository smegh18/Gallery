# Phase 7 Gate Findings

Date: 2026-11-24 (IST). Review base: `398add0` (approved UI contract) to HEAD `26446fa`: 30 commits, 90 files, app source in 26 Kotlin files plus resources.

## Gate commands

| # | Command | Result |
|---|---------|--------|
| 1 | `./gradlew.bat :app:testDebugUnitTest -q` (then forced rerun with `--rerun`) | PASS: 54 suites, 342 tests, 0 failures, 0 errors, 0 skipped |
| 2 | `./gradlew.bat :app:assembleDebug -q` | PASS (`app/build/outputs/apk/debug/app-debug.apk`, 33 MB) |
| 3 | `python -I scripts/ref_res.py check-strings` | PASS: 24 languages up to date |
| 4 | `python -I scripts/ref_res.py check-colors` | PASS: 226 light / 134 night names match 2.7.2 |
| 5 | `git diff --check` | PASS (no whitespace errors) |

Gate 2 was an incremental build on an unchanged tree (sources have not changed since the 07-08 build); gate 1 was forced to rerun.

## CodeRabbit

- Command that would be run: `coderabbit review --base 398add0` (code-review skill, committed diff against the Phase 7 base).
- Status: **NOT RUN, still owed.** The `coderabbit` / `cr` CLI is not installed on this machine (`command -v coderabbit cr` found nothing). Per instruction it was not installed. A manual review of the same diff replaced it (below). The VER-03 "CodeRabbit with no open findings" gate stays open until the CLI runs; run it once and route any finding through a 07-10+ correction plan.

## Manual review of the diff (stand-in for CodeRabbit)

Swept `app/src/main/java/com/quizango/gallery/ui/vault` and the diff `398add0..HEAD` for the listed classes of defect.

| Check | Method | Result |
|-------|--------|--------|
| English user copy in code | grep for `Text("`, `text = "`, `title = "`, `label = "`, literal `Toast.makeText` strings | None. Only `"●"` (PIN slot glyph, a symbol) and `"$number"` (step digit) |
| Material app bars, AlertDialog, dropdown menus, material-icons glyphs | grep `TopAppBar`, `AlertDialog`, `DropdownMenu`, `Icons.` | None. Only `Text`, `Switch`, `CircularProgressIndicator`, `ripple` from material3 |
| Raw `sp` text sizes | grep `\.sp\b` over the diff | None (all `ssp` / `GalleryText`) |
| Broad exception swallowing | grep `catch (e: Exception)` over the diff | Only `VaultRepository.kt` file operations; each rolls back and returns false (data-safety design, 07-02), inner scan failures rethrow `CancellationException`. See F1 |
| Fake data / TODO / FIXME | grep | None; `placeholder =` hits are Coil image placeholders |
| Appearance-only controls | read the settings rows, gate, picker, lock | Every control is wired (switches write preferences, rows navigate, FAB opens the picker) |
| Stale shortcut comments | grep `shortcut:` | Two, both still accurate (VaultGate saved-state size, PIN slot glyph) |
| Data safety | read `VaultRepository.hideFileSafely/restoreFileSafely/moveToVault/restoreFromVault` | Row inserted before the source is deleted; failed delete removes the row and keeps the ciphertext; restore never overwrites (unique name) and keeps the vault copy if the delete fails; MediaStore re-indexed |
| Lock gate | read `VaultGate.kt`, `GalleryNavHost.kt` diff | Hide runs only after a lock plus recovery question exist; cancelled setup wipes the half lock, so `resolveReturn` returns null |

## Unslop audit (VER-03), six screens

Scope: lock (`VaultLockScreen.kt`), vault media (`VaultMediaScreen.kt`), Privacy Settings (`VaultSettingScreen.kt`), recovery question (`VaultRecoveryQuestionScreen.kt`), Forgot Password (`VaultForgotPasswordScreen.kt`), Add Items picker (`VaultAddMediaScreen.kt`).

These screens are a port of 2.7.2, so the audit checks for AI tells, not for a new direction. Found none: no gradients or Brush, no glow, blur or pulsing animation, no emoji, no uppercase tracked labels, no hard-coded colours (`Color(0x...)` / `Color.Red`; all `colorResource` from the imported 2.7.2 palette), no New badges, all type on `GalleryText` / `ssp`, all dimensions `sdp`. The only shadows are the vault FAB elevation (6.sdp, ordinary FAB elevation, not a glow; UI-SPEC does not give the value, so confirm it on the device), the picker tray remove badge (2.sdp, spec'd in UI-SPEC) and the video-duration text shadow on grid cells (Phase 3/5 cell). Decorative images carry `contentDescription = null` with a comment; meaningful ones (FAB, close) carry a string resource.

Unslop cannot judge device rendering. Spacing, glyph and look remain a side-by-side device check (see 07-LIVE-COMPARISON.md).

## Findings and dispositions

| ID | Finding | Disposition | Evidence |
|----|---------|-------------|----------|
| F1 | `moveToVault` / `restoreFromVault` outer `catch (_: Exception) { false }` also catches a `CancellationException` rethrown by the inner scan guard, so a cancelled caller sees `false` for a hide that completed | Rejected as a defect, logged as a low-risk note | No data loss: the file is already hidden/restored and the row state is consistent. Cancellation only happens when the collecting screen leaves, and callers discard the result then. A correction plan would add churn to the safest file in the phase for no user-visible effect |
| F2 | `rememberVaultHideGate` stores selected paths in a Bundle (`rememberSaveable`) | Rejected | Already marked `shortcut:` in code and in 07-08 (hand-picked selections only; limit is thousands of paths while the process is stopped during setup) |
| F3 | Lock screen state other than `wrongCount` is `remember`ed, so rotation mid-setup restarts the entry | Rejected | Same as the old screen; recorded in 07-03 and 07-04; the lockout counter, which carries the security effect, survives |
| F4 | CodeRabbit CLI not installed | Open: owed | See CodeRabbit section |

No accepted finding, so no correction plan 07-10+ was written. No app source file is in this plan's commits.
