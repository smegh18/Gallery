---
phase: 03-home-shell-and-photos-tab
plan: 05
subsystem: ui-home
tags: [compose, datastore, overlay, dialog, back-handling, parity-2.7.2]
requires:
  - 03-01 (HomeTopBar title and long-press, exit_rectangle, exitViewBackgroundColor, hint strings and colours)
  - 03-03 (selection BackHandler, Modifier.drawableBackground)
  - 03-04 (Home root Box, HomeOverlays.kt, PinchTutorialOverlay)
provides:
  - AppPreferences sessionCount / incrementSessionCount, isVaultHintToolTip / setVaultHintToolTip, vaultHintClosedSession / setVaultHintClosedSession
  - HomeShell.kt VAULT_HINT_SESSIONS, shouldShowVaultHint; HomeTopBar onTitlePositioned
  - HomeUiState.sessionCount / vaultHintSeen / vaultHintClosedSession, HomeViewModel.closeVaultHint
  - HomeOverlays.kt VaultHintOverlay, HomeExitPopup
affects: [03-06 (device check of the hint, RTL pill and exit popup), Phase 9/10/12 (start-up dialogs must hold the hint), Phase 13 (direct exit vs popup)]
tech-stack:
  added: []
  patterns: [Layout modifier placing a block from another composable's window bounds, Dialog window reshaped through DialogWindowProvider]
key-files:
  created: []
  modified:
    - app/src/main/java/com/quizango/gallery/data/local/datastore/AppPreferences.kt
    - app/src/main/java/com/quizango/gallery/ui/splash/SplashScreen.kt
    - app/src/main/java/com/quizango/gallery/ui/navigation/GalleryNavHost.kt
    - app/src/main/java/com/quizango/gallery/ui/home/HomeViewModel.kt
    - app/src/main/java/com/quizango/gallery/ui/home/HomeShell.kt
    - app/src/main/java/com/quizango/gallery/ui/home/HomeOverlays.kt
    - app/src/main/java/com/quizango/gallery/ui/home/HomeScreen.kt
    - app/src/test/java/com/quizango/gallery/HomeShellTest.kt
decisions:
  - "A session is one Splash run: incrementSessionCount is awaited first in the Splash LaunchedEffect, so Home always reads this launch's number"
  - "The VaultLock nav destination sets isVaultHintToolTip in a LaunchedEffect, so every route into the lock stops the hint for good"
  - "One Home BackHandler in HA.Oooo0oO order (hint, selection, exit popup); the ⋮ popup's focusable window eats Back itself; the pinch tutorial is not checked"
  - "Exit popup built per HOME-03 although 2.7.2's bundled remote config says app_exit direct; Phase 13 decides on the phone"
status: complete
metrics:
  duration: ~25 min
  completed: 2026-11-15
actuals:
  tokens: 5200
  tasks: 3
  commits: 4
---

# Phase 3 Plan 5: Vault hint and Home exit popup Summary

The app now counts cold launches. Each Splash run adds 1 to `sessionCount`, and the first launch after this build is session 1. On the 4th, 8th and 12th launch, a user who has never opened the vault lock sees the 2.7.2 vault hint over the whole Home, provided media permission is granted and the hint was not already closed that session. The hint has a dark `hint_home` scrim and an `ic_hint_home` pill that repeats the top-bar title in #1F3355, placed over the real title. Under the pill are the arrow and the "Hold Gallery to open Private Vault" / "Secure your hidden photos here" copy. Tapping the scrim closes the hint for the session. Long-pressing the pill gives haptic feedback, closes the hint and opens the Vault lock. Opening the Vault lock by any route stops the hint for good.

Back on Home now follows 2.7.2's order (`HA.Oooo0oO`):
1. A showing hint is closed for the session.
2. An open ⋮ popup closes itself.
3. A selection is cleared.
4. Otherwise the `dialog_exit` bottom sheet opens with "Tap again to exit". Tapping the text calls `finishAffinity()`. Back or a tap outside only closes the sheet.

## Tasks

| Task | Commit(s) | Result |
|------|-----------|--------|
| 1 Session counter, hint flags, show rule | 97a3241 (RED), 6e8d1d0 (GREEN) | 3 preferences under the 2.7.2 key names, Splash increment, VaultLock marks seen, `shouldShowVaultHint`, HomeViewModel state and `closeVaultHint` |
| 2 Vault-hint overlay | 3add8cb | `VaultHintOverlay`, `HomeTopBar.onTitlePositioned` (`boundsInWindow`), wired in HomeScreen under the pinch tutorial |
| 3 Exit popup and Back order | 5ff6bd2 | `HomeExitPopup`, a single Home `BackHandler`, `finishAffinity` on the text tap |

## How the pieces work

- **Counter.** `incrementSessionCount` reads and writes the value inside one `DataStore.edit`. A missing value counts as 0, so the first launch becomes 1. Splash awaits it before it reads anything else, so there is no race with Home. A configuration change or a background process start never runs Splash, so neither counts as a session.
- **Show rule.** `shouldShowVaultHint(session, hintSeen, closedSession, hasMediaPermission)` = `session in {4, 8, 12} && !hintSeen && closedSession != session && hasMediaPermission`. HomeScreen also hides the hint in selection mode. `HomeUiState.vaultHintSeen` starts as true, so the hint cannot flash before the preferences load. `closeVaultHint` updates the UI state at once and then saves `vaultHintClosedSession`.
- **Conditions with no source yet** (`shortcut:` comments in HomeShell.kt):
  - No notification, widget or deep-link entry to Home exists until Phase 9.
  - No other start-up dialog exists on Home until Phases 9, 10 and 12. Each of those must hold the hint until its own dialog closes (`isVaultHintShowPending`).
- **Pill placement.** The overlay records its own `positionInWindow`. A `layout` modifier then places the pill and copy block so that the pill's text inset (6.sdp horizontal, 4.sdp vertical) lands on the title's `boundsInWindow`:
  - LTR: the block's left edge = title.left − 6.sdp.
  - RTL: the block's right edge = title.right + 6.sdp. The Column content mirrors, so the arrow and copy hang from the pill's start on the right.
  - Before the title has been measured (at most one frame), the block sits at the XML fallback: start 40.sdp, top 5.sdp.
  - The block is measured with the width that is left on its side of the screen, so long translations wrap instead of running off the edge.
- **Pill touch.** The pill uses `detectTapGestures(onLongPress)`, which consumes a short tap. A short tap on the pill therefore does nothing, which matches a long-clickable View with no click listener. A tap anywhere else on the scrim closes the hint.
- **Exit window.** It is a Compose `Dialog` with `usePlatformDefaultWidth = false`. In a `SideEffect`, the `DialogWindowProvider` window gets `Gravity.BOTTOM`, `MATCH_PARENT` × `WRAP_CONTENT` and a transparent background, which mirrors `jt/a.onCreate`. The system dim is kept. The sheet is `exit_rectangle` (#FFFFFF, or #131111 in dark mode, with 25.dp top corners) with 4.dp padding. There is no ad frame (D5). The text is 55.dp tall, centred, #9A9A9A, Inter Regular, and its size is 15.sdp converted to sp through `LocalDensity`. Because the dialog is its own window, it draws above the pinch tutorial when Back is pressed with only the tutorial up. The tutorial stays until GOT IT.

## Open question for Phase 13 (direct exit vs popup)

2.7.2's bundled default remote config has `app_exit` set to `"direct"`, which means Back on Home exits at once. The popup only appears when the live remote config overrides it. This plan builds the popup because HOME-03 / roadmap criterion 4 require it. **On the phone, check which one 2.7.2 actually does. If it exits directly, replace `showExitPopup = true` in the HomeScreen BackHandler with the direct finish.**

## Verification (actual results)

- **RED:** `./gradlew.bat :app:testDebugUnitTest --tests "com.quizango.gallery.HomeShellTest" -q` failed at compile with `HomeShellTest.kt:18:37 Unresolved reference 'shouldShowVaultHint'` (and line 103).
- **GREEN:** the same command exited 0.
- **Task 1 gate:** `pref markers=4/4, splash increments=1, vault-lock marks seen=1`.
- **Task 2 gate:** `hint markers=7/7, title bounds reported=2, wired=1`.
- **Task 3 gate:** `exit markers=6/6, finishAffinity=1, wired=1`.
- **Unit tests and build:** after each task, `./gradlew.bat :app:testDebugUnitTest :app:assembleDebug -q` exited 0 with no `FAILED` or `e:` lines. On the final run the tail was `BUILD SUCCESSFUL in 3s / 44 actionable tasks: 44 up-to-date`. The test result XMLs show 129 tests, 0 failures, 0 errors. That count includes `vaultHintShowsInSessions4_8_12Only`, OnboardingTest and OnboardingFlowTest.
- **Device: not run.** The Redmi has a differently signed build installed. Installing would need an uninstall, which wipes app data, so I skipped the plan's adb Back check as instructed. I checked none of these on hardware:
  - the hint's look and placement, including RTL
  - the haptic on long-press
  - the exit sheet's window, dim and outside tap
  - `finishAffinity`

  This is logged in `.planning/WINDOWS.md` (entry 3). The 03-06 checkpoint is the backstop.

## Deviations from Plan

1. **[Rule 3 - Blocking] 2.7.2 decode not on this machine.** I could not read `layout_home_vault_hint.xml`, `dialog_exit.xml` or the smali. All values come from the plan and UI-SPEC. The UI-SPEC does not give two things, so I guessed them:
   - The arrow is drawn at the vector's intrinsic size, 65 × 71 dp.
   - The copy column sits at the arrow's bottom. The arrow head points up toward the pill and its tail ends bottom-right.

   This is logged in WINDOWS.md (entry 4) for the 03-06 comparison.
2. **Title long-press hook.** `onTitleLongPress` already existed (03-01). I did not add a new parameter. HomeScreen's lambda now closes a showing hint before it opens the Vault lock (`HA.o000OO0o`). In practice the scrim covers the title while the hint shows, so this branch only matters for parity.
3. **Single BackHandler.** The plan asked for one handler for the non-popup branches. It replaces the 03-03 `BackHandler(enabled = isSelectionMode)`. It is always enabled while Home's back-stack entry is resumed, so Back on Home no longer finishes the activity directly.
4. **`(context as? Activity)?.finishAffinity()`** is a safe cast instead of the plan's hard cast. `LocalContext` is the activity here, since nothing wraps it.

## Known Stubs

None. The two show conditions with no source yet are documented `shortcut:` comments owned by Phases 9, 10 and 12, as the plan prescribes.

## Threat Flags

None new.
- **T-03-11:** the pill long-press only navigates to `Screen.VaultLock.route`, which still asks for the PIN, pattern or fingerprint.
- **T-03-13:** Back never exits directly. Only a tap on the popup text calls `finishAffinity`.

## Self-Check: PASSED

- Commits 97a3241, 6e8d1d0, 3add8cb and 5ff6bd2 are on main.
- All 8 modified files exist.
- I did not touch STATE.md or ROADMAP.md.
