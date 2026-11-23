---
phase: 07-private-vault
plan: 04
subsystem: ui
tags: [compose, vault, recovery-question, forgot-password]
requires:
  - phase: 07-01
    provides: reference strings, colours, drawables, lock-state preferences
  - phase: 07-03
    provides: typed lock callbacks and vault routing in GalleryNavHost
provides:
  - RecoveryQuestionFlow (pure: steps, choices, answer events, questionRes) and forgotAnswerEvent
  - VaultRecoveryQuestionScreen (#35) with three free-choice questions
  - VaultForgotPasswordScreen (#36) on reference copy
  - shared QuestionSelector and AnswerField
affects: [07-05, 07-09]
status: complete
actuals:
  tokens: 14000
  tasks: 2
  commits: 2
tech-stack:
  added: []
  patterns: [pure flow class plus a version counter keyed with remember to redraw, Popup instead of Material dropdown, reference shape drawables redrawn as Compose shapes]
key-files:
  created:
    - app/src/main/java/com/quizango/gallery/ui/vault/RecoveryQuestionFlow.kt
    - app/src/test/java/com/quizango/gallery/RecoveryQuestionFlowTest.kt
  modified:
    - app/src/main/java/com/quizango/gallery/ui/vault/VaultRecoveryQuestionScreen.kt
    - app/src/main/java/com/quizango/gallery/ui/vault/VaultForgotPasswordScreen.kt
    - app/src/test/java/com/quizango/gallery/VaultTest.kt
key-decisions:
  - "System Back on both screens calls the same callback as the toolbar Back (BackHandler), so Recovery Back wipes a half-made lock and Forgot Back closes the lock after a lockout (07-03 carry-over)"
  - "Toolbar Back on the question screen leaves the flow (no step-back), because used questions leave the list and a step-back would have to restore them; matches the spec's 'Back returns without saving'"
  - "Stored question is the reference id as a string; Forgot localises ids 1..10 and shows older plain-text questions raw"
requirements-completed: [VLT-01]
---

# Phase 7 Plan 04: Recovery question and Forgot Password Summary

Both recovery screens now follow their 2.7.2 layouts: three free-choice questions out of seven with a shrinking list, and a Forgot Password that checks a trimmed, case-insensitive answer against the stored hash (D3) and wipes the lock only on a right answer.

## Commits
- f063fe2 feat(07-04): recovery question with three free-choice questions, 2.7.2 chrome and system Back routing (Task 1, tracer)
- 58270b5 feat(07-04): Forgot Password on reference copy with the D3 answer match (Task 2)

## Verification (run on this machine)
- Tracer gate (Task 1): `./gradlew.bat :app:testDebugUnitTest --tests "com.quizango.gallery.RecoveryQuestionFlowTest" --tests "com.quizango.gallery.VaultTest" -q` -> exit 0; `./gradlew.bat :app:assembleDebug -q` -> exit 0. Tracer verified, expansion proceeded.
- Task 2 / final tree: same test command (RecoveryQuestionFlowTest now 11 tests, VaultTest) -> exit 0; `./gradlew.bat :app:assembleDebug -q` -> exit 0.
- Acceptance greps: forbidden-pattern grep (TopAppBar, DropdownMenu, Icons., `makeText(context, "`, raw `.sp`) 0 in both screens; `fun questionRes` 1; `testSecurityQuestionAnswerMatching` 0; `clearVaultLock()` in Forgot 1; `D3` in the flow file 1; 7 flow tests + 4 forgot tests.

## Behaviour now
- Recovery Question: toolbar `set_security_questions_title`, heading and subtitle, three step circles (reached = filled `recoverSelectBg` with white digit, pending = `recoverUnselectBg` with `titleColor` digit) joined by `ic_recover_arrow`, selector over the seven `security_question1..7`, answer field, button `Next` on steps 1-2 and `Submit` on step 3. Empty answer toasts `please_enter_answer`. A used question leaves the list and the first remaining one is selected. After step 3 the three ids (as strings) and raw answers go to `setThreeSecurityQuestions` (hashed there), the keyboard hides and `onSaved` runs; a second tap is ignored while saving.
- Forgot Password: toolbar `forgot_password`, title, subtitle, selector over the stored questions (ids 1..10 localised, older text shown raw), answer field, Confirm. No stored question -> `something_went_wrong`; empty -> `please_write_an_answer`; wrong -> `enter_correct_security_answer`; right -> `clearVaultLock()` then `onAnswerVerified()`. No toast path touches stored state.

## Deviations from Plan

**1. [Rule 3 - Blocking] Forgot screen touched in Task 1** - removing `DEFAULT_SECURITY_QUESTIONS` broke the old Forgot screen's compile. A one-token patch kept Task 1 building; Task 2 rewrote the screen.

**2. [Rule 1 - Bug] System Back on the question screen (07-03 carry-over)** - the screen now has `BackHandler { onNavigateBack() }`, so system Back runs the NavHost callback that wipes a half-made lock (no question saved yet) and pops question and lock. Same fix applied to Forgot Password, where system Back previously just popped and skipped the lockout-aware routing (VL.o00Oo0). GalleryNavHost unchanged.

**3. Toolbar Back no longer steps back a question** - the old screen decremented the step; with used questions leaving the list that would need restoring them, and the spec says Back returns without saving. Back now always leaves.

**4. Step circles, selector frame and answer field drawn in Compose** - `ic_recover_select/unselect/spinner_bg/answer_bg` are `<shape>`/`<ripple>` XML, which `painterResource` cannot load. The same colours, radii (12.sdp frames, circle steps) and strokes are drawn with `background`/`border`, and the button uses `dark_blue` at 25.sdp radius from `btn_permission`. Only `ic_recover_arrow` (vector) is loaded as a drawable.

## Decisions / accepted shortcuts
- Screen flow state is `remember`ed, so a configuration change mid-setup restarts the three steps (same shortcut as the lock screen).
- The closed selector row shows no drop arrow: `ic_recover_spinner_bg` has none and the layout XML names no arrow asset. Device backstop.
- Stored-question compare when `pairs` has not loaded yet (first frame): Confirm gives `something_went_wrong`, which is the reference "no stored questions" path.

## Known Stubs
None.

## Pending device checks (all PENDING-DEVICE, no phone, Artemis down)
- First setup reaches the question screen, three different questions are chosen, Submit opens the vault (D7).
- Five wrong PINs open Forgot Password; a wrong answer toasts; the right one returns to Set PIN (D7).
- System Back on the question screen wipes the half-made lock and returns to Explore; system Back on Forgot after lockout closes the lock.
- Selector popup width/position, answer field and soft keyboard against 2.7.2 in light and dark, long German/Russian/Tamil strings (A8 button label Next/Submit).
- Whether the reference spinner shows a drop-down arrow.

## Threat Flags
None. T-07-14 (hashed constant-time compare, empty and missing answers never reset, tests per path) and T-07-15 (answers stored hashed) are mitigated as planned.

## Self-Check: PASSED
Commits f063fe2 and 58270b5 present; RecoveryQuestionFlow.kt, RecoveryQuestionFlowTest.kt and both screens exist; no unexpected deletions.
