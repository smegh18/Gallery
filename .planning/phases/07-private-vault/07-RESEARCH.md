# Phase 7: Private Vault - Research

**Researched:** 2026-11-24
**Domain:** Android parity port (Jetpack Compose) of the 2.7.2 Private Vault: lock, media, settings, recovery question, forgot password, and the "Add Items" media picker. No new libraries.
**Confidence:** MEDIUM-HIGH. Layouts and string keys are read from the decoded 2.7.2 resources (HIGH). Behaviour is read from smali (HIGH where quoted, MEDIUM where a runnable's body was not opened). Nothing was compared on a phone: no device was connected and the ARTEMIS and REA MCP servers failed to connect this session.

<user_constraints>
## User Constraints (from CONTEXT.md)

No CONTEXT.md exists for this phase. The user asked for autonomous best-judgment work.

### Locked Decisions
None from a discuss step. Project-level decisions that bind this phase (`.planning/PROJECT.md`, `REQUIREMENTS.md`, `STATE.md`):
- Core value: a user cannot tell our app from reference 2.7.2 (screens, flows, animations, behaviour).
- Ads stay placeholders (collapsed, 0 height); no real billing; no usage-data upload; reference brand art replaced by our own.
- Usage-access dialog is wired in Phase 12; earlier phases leave no-op hooks.
- VER-03 gate on every phase: unit tests, `assembleDebug`, CodeRabbit review with no open findings, unslop audit of changed screens.
- Vault storage is "hidden encrypted storage" (PROJECT.md line 22: "Private Vault: PIN & Pattern lock, biometric prompt, security question recovery, hidden encrypted storage").

### Claude's Discretion
Everything else, in particular: how to resolve the picker mislabel (Open Question 1), whether to keep the answer normalisation in Forgot Password (Open Question 3), and how to structure the new lock state machine.

### Deferred Ideas (OUT OF SCOPE)
None recorded. Candidates proposed by this research for the user to confirm: the "Hidden Folder" albums section in the 2.7.2 vault (Open Question 2); the Home lock-icon shared-element transition `vault_click`.
</user_constraints>

<phase_requirements>
## Phase Requirements

| ID | Description | Research Support |
|----|-------------|------------------|
| VLT-01 | Vault lock, media, settings, recovery question and forgot password match 2.7.2 (#32-#36) | Sections "Reference behaviour contract" (L/M/S/R/F rules), "Exists vs diverges", pitfalls 1-8 |
| VLT-02 | The in-app media picker matches 2.7.2's `activity_picker_screen` (#37) | **The requirement names the wrong layout.** The vault's Add Items picker is `InternalSelectMediaActivity` / `activity_select.xml` (unchanged vs 2.6.7). `activity_picker_screen` is the collage picker fragment (Phase 8). See Open Question 1 and the "Picker" contract. |
</phase_requirements>

## Summary

The six vault layouts (#32-#36) and the picker's real layout (`activity_select`) are **byte-identical to 2.6.7** [VERIFIED: diff of decoded `res/layout` for 2.6.7 vs 2.7.2, this session: `activity_vault_lock`, `_media`, `_setting`, `_recovery_question`, `_forgot_password`, `layout_toolbar`, `layout_select_media_bottom`, `activity_select`, `item_picker_folder`, `item_selected`, `custom_toast_layout`, `item_question`, `dialog_hide`, `dialog_delete` all "same"]. The checklist rows say "same" for that reason. So Phase 7 is not a redesign. It is a **fidelity and behaviour port**: the existing Compose vault (7 files, ~1,900 lines in `ui/vault`) was written in the 2.6.7 era with raw `dp`/`sp`, hard-coded English, Material widgets, and several invented behaviours. It needs the same treatment Phases 3-6 gave other screens (sdp/ssp, `GalleryText`, reference string keys, `SharedMediaToolbar`, reference dialogs) plus a set of real behaviour gaps found in smali.

The biggest findings, ranked by user-visible impact:
1. **Picker mislabel.** VLT-02 / row #37 point at `activity_picker_screen`. That layout is inflated by fragment `te/d` (args `arg_min`, `arg_max`, `arg_mode`, `arg_from`), the collage image picker. VaultMediaActivity's Add Items starts `InternalSelectMediaActivity` with `TYPE=2`, whose layout is `activity_select.xml` [VERIFIED: smali, see below]. The only 2.7.2 change to `activity_picker_screen` is the font on two text views (`@font/font` to `@font/if_inter_medium`).
2. **"Add to Private" never checks that a lock exists.** In 2.7.2 every entry point (Home, MediaList, MediaFullScreen, Favorites, Animated, SearchPhotoColor) checks `isPassword`. If unset, it opens VaultLock (`TYPE=0`, `IS_OPEN_VAULT=false`) to set the PIN/pattern and recovery question first, then hides. Ours hides straight into an unlocked vault (no vault-preference reads exist outside `ui/vault`).
3. **Missing lock behaviours:** 5-wrong-attempts lockout into Forgot Password (persisted `isPrivateLock`); Change Password verifies the old password first; Change Recovery Question unlocks first; the "Please enter your pin." toast; the 4-dot pattern minimum on setup; "Forgot password?" only when a recovery question exists; "Show Pattern" defaults to ON; pattern errors show no toast.
4. **Recovery question** is a free choice from 7 questions with already-picked ones removed, not three fixed questions.
5. **Data-safety bugs in `VaultRepositoryImpl`** (independent of parity): restore overwrites a same-named file; hide deletes the source before the DB row exists; unhide never re-indexes MediaStore.
6. **Resources missing locally:** ~16 string keys' worth of reference text (list below), 10 drawables, 7 colours.

**Primary recommendation:** Split the phase into (a) a pure-Kotlin `VaultLockFlow` state machine + `VaultGate` check, unit-tested, (b) a resource/string import wave, (c) per-screen restyle to the reference layouts using the Phase 3-6 house pieces, (d) repository hardening, and (e) a device wave. Treat the picker as `activity_select` (keep the existing Compose structure, restyle to it) and ask the user to approve the requirement-text correction.

## Architectural Responsibility Map

| Capability | Primary Tier | Secondary Tier | Rationale |
|------------|-------------|----------------|-----------|
| PIN / pattern entry, step titles, toasts | Compose UI (`ui/vault`) | pure `VaultLockFlow` (testable) | Reference keeps this in the Activity; our logic should live in a pure class so it can be unit-tested |
| Lock credentials, recovery Q&A, lockout flag, fingerprint flag | DataStore (`AppPreferences`) | - | Existing; stores salted SHA-256 (reference stores plain text) |
| "Does a lock exist?" gate for Add to Private | Domain helper on `AppPreferences` | NavHost result callback | One check shared by 6 entry points |
| File hide / unhide / delete | `VaultRepositoryImpl` (data) | Room `vault_table`, MediaStore re-scan | Encrypted copy in `filesDir/vault_encrypted`, AES-256-CBC with Keystore key |
| Fingerprint prompt | `VaultBiometric` (androidx.biometric) | - | Needs `FragmentActivity`; `MainActivity` is `AppCompatActivity` |
| Vault grid, selection bar, dialogs | Compose UI reusing Phase 3/5 pieces | - | `SharedMediaToolbar`, `MediaSelectionBar`, `HideMediaDialog`, `DeleteMediaDialog` already match the reference layouts |
| Picker (multi-select, folders) | Compose UI (`VaultAddMediaScreen`) | `MediaRepository` flows | Reference is `InternalSelectMediaActivity` |

## Standard Stack

No new dependencies. All of this exists in the repo.

### Core
| Library | Version | Purpose | Why Standard |
|---------|---------|---------|--------------|
| Jetpack Compose + Material 3 | repo BOM (Kotlin 2.1.0, AGP 8.8.2) | Screens | Project stack [VERIFIED: `.planning/STATE.md` Baseline] |
| `androidx.biometric` | `libs.androidx.biometric` | Fingerprint unlock | Reference uses the same library [VERIFIED: `app/build.gradle.kts:58` `implementation(libs.androidx.biometric)`; `VaultBiometric.kt` imports `androidx.biometric.BiometricPrompt`] |
| DataStore Preferences | repo | Lock state | Existing `AppPreferences` |
| Room 2.6.1 (v5) | repo | `vault_table` | Existing `VaultEntity`/`VaultDao` |
| Android Keystore AES-256-CBC | platform | File encryption | Existing `VaultCrypto` (CBC chosen over GCM so large videos stream) |
| Coil | repo | Thumbnails | Existing |

### Alternatives Considered
| Instead of | Could Use | Tradeoff |
|------------|-----------|----------|
| Salted SHA-256 for the 4-digit PIN | PBKDF2/Argon | A 4-digit PIN has 10,000 values, so any hash is brute-forceable offline; the real protection is that DataStore lives in app-private storage and `allowBackup="false"`. Do not spend a task here. Keep. [VERIFIED: `AndroidManifest.xml:30` `android:allowBackup="false"`] |
| Binding the Keystore key to user authentication | - | Would change unlock behaviour and break PIN-only users. Do not do. |

**Installation:** none.

## Package Legitimacy Audit

No external packages are installed in this phase, so the package-legitimacy gate is not applicable. `androidx.biometric` is already a dependency (above).

**Packages removed due to [SLOP] verdict:** none
**Packages flagged as suspicious [SUS]:** none

## Architecture Patterns

### System Architecture Diagram (2.7.2 flows to reproduce)

```
Entry points                       VaultLock (TYPE, IS_OPEN_VAULT)                  Result
-------------                      -----------------------------
Explore "Vault" / Home lock icon -> TYPE=0, OPEN=true
Settings "Private Vault"         -> TYPE=0, OPEN=true
                                    |
                                    +-- no lock set -> SETUP: Set PIN -> Re-enter PIN  (or pattern, 4+ dots, redraw)
                                    |       |  mismatch -> toast password_confirm / pattern_confirm, back to step 1
                                    |       v
                                    |   no recovery question? -> VaultRecoveryQuestion (3 steps) --OK--> next
                                    |                                      (first time: custom toast_private on VaultMedia)
                                    +-- lock set -> UNLOCK (PIN: checks only on the check-mark; pattern: on finger lift)
                                            |  fingerprint on? -> BiometricPrompt on open
                                            |  wrong -> toast wrong_password (PIN only); 5th wrong => isPrivateLock=true
                                            |           -> VaultForgotPassword; flag also checked on every open
                                            |  "Forgot password?" (only if recovery question saved) -> VaultForgotPassword
                                            v
                                         VaultMedia  (OPEN=true: lock closes, vault replaces it)

Add to Private (6 screens)       -> isPassword? yes -> HideMediaDialog -> hide
                                                 no  -> VaultLock TYPE=0, OPEN=false -> setup -> recovery question
                                                        -> RESULT_OK -> HideMediaDialog -> hide

VaultMedia -> settings icon -> VaultSettings ("Privacy Settings")
   Change Password         -> VaultLock TYPE=1, OPEN=false: toolbar "Change Password", VERIFY OLD (PIN or pattern),
                              then reset to SETUP ("Set PIN", toolbar "Private Vault") -> RESULT_OK(ISPASS) -> Settings toast
   Show Pattern switch     -> pref isShowPattern (default TRUE)
   Unlock with Fingerprint -> pref isFinger (default false); needs biometrics enrolled else error toast
   Change Recovery Question-> VaultLock TYPE=2, OPEN=false: UNLOCK first -> VaultRecoveryQuestion -> RESULT_OK -> Settings toast
VaultMedia -> "Add Items"/FAB -> InternalSelectMediaActivity(TYPE=2) -> hide selected -> back to VaultMedia
```
[VERIFIED: smali `VaultLockActivity` (o0OoOo0, o00000O0, o00000O, o00000OO, o00Oo0, o00Ooo, onCreate), `VaultSettingActivity` (OoooOO0, o000oOoO, `m0/b` result callback), `VaultMediaActivity.o000000o`, `MediaListActivity`/`HomeActivity` `isPassword` gate; all read this session]

### Recommended Project Structure
```
app/src/main/java/com/quizango/gallery/
  data/local/VaultLockFlow.kt          # NEW pure Kotlin: step/titles/toast keys/wrong count (unit-tested)
  data/local/datastore/AppPreferences  # add isVaultLockSet(), vaultPrivateLock flag, showPattern default true
  ui/vault/                            # restyle the 7 screens; add VaultGate (Add-to-Private check)
  ui/browse/MediaListActions.kt        # add MediaAction.UNHIDE (ic_unhide, R.string.unhide, 63 sdp)
```

### Pattern 1: pure state machine, Compose renders it
**What:** move PIN/pattern step logic out of `VaultLockScreen` into a plain class `VaultLockFlow(mode, storedCheck, hasQuestion)` that takes `submit(value)` and returns an event (`ToastKey`, `NextStep`, `Success`, `LockOut`). Compose only renders titles and plays the shake.
**When to use:** all lock logic. It is the only way to unit-test the 5-attempt lockout, the 4-dot rule and the TYPE 0/1/2 transitions without a device.
**Example (sketch, not final code):**
```kotlin
// Source: behaviour from VaultLockActivity.o0OoOo0 / o00000O0 (decoded 2.7.2 smali)
sealed interface LockEvent {
    data class Toast(@StringRes val res: Int) : LockEvent
    data object Next : LockEvent          // first entry stored, now confirm
    data object Success : LockEvent
    data object ForceForgot : LockEvent   // 5th wrong attempt
}
```

### Anti-Patterns to Avoid
- **Re-inventing toast text.** `VaultLockScreen.kt:131` says "shortcut: wrong-pattern toast text not captured from the reference". It is now captured: a wrong pattern shows **no toast** (pattern view turns red); only wrong PIN toasts `wrong_password`.
- **`TopAppBar` + `HorizontalDivider`** in each vault screen. Use `SharedMediaToolbar` (the `layout_toolbar` port) like Phases 5-6.
- **Material `Switch`/`AlertDialog`/`DropdownMenu`** for reference widgets that are `SwitchCompat`, `dialog_hide`/`dialog_delete` cards, and `AppCompatSpinner`.

## Exists vs diverges (our code vs 2.7.2)

| Area | Ours today | 2.7.2 | Action |
|------|-----------|-------|--------|
| Lock toolbar | `TopAppBar`, `"Private Vault"` literal, 18.sp | `layout_toolbar`, `private_vault`; **"Change Password" while verifying in TYPE=1** | `SharedMediaToolbar` + string keys |
| Lock titles/strings | literals; `enter_pin`="Enter 4-Digit PIN"; `switch_to_*`="Use Pattern/PIN"; no `set_pin` etc. | `set_pin`, `re_enter_pin`, `enter_pin`="Enter your PIN", `draw_your_pattern`, `re_draw_your_pattern`, `switch_to_pattern`="Switch to Pattern", `switch_to_pin`="Switch to PIN" | import/fix keys |
| PIN ✓ with <4 digits | silently ignored (`if (entered.length == 4)`) | toast `please_enter_pin` "Please enter your pin." | add |
| Pattern minimum | "Connect at least 3 dots" (invented) | setup first draw < 4 dots: toast `pattern_min_4_dots` "The pattern must utilise four dots."; unlock has no minimum | fix |
| Wrong pattern | toast "Incorrect pattern!" (invented) | no toast, red path; counts toward lockout | fix |
| Wrong PIN | toast literal "Incorrect password!" | `wrong_password` "Incorrect password!" | key only |
| 5 wrong attempts | none | `isPrivateLock=true`, open Forgot Password; flag re-checked every open | add |
| Mismatch on confirm | literal toasts | `password_confirm` / `pattern_confirm`, restart at step 1 | keys |
| "Forgot password?" | shown whenever not setting up | only if `isQuestion` (a recovery question is saved) | add condition |
| Show Pattern | default **false** (comment says "off by default, like the reference") | default **true**; stealth = `!isShowPattern` | fix default (migration note below) |
| Change Password | goes straight to "Set PIN" | verify old first, then setup; toast `password_changed_successfully` on Settings | add |
| Change Recovery Question | opens question screen directly | unlock first; Settings toast `recovery_question_changed_successfully`; ours navigates to VaultMedia on save | add |
| Recovery question | 3 fixed questions, "Please select different questions" toast (invented) | spinner over 7 questions (`array/security_question`), picked ones removed for next step; empty answer toast `please_enter_answer` "Please enter an answer!"; stores 3 `{id, question, ans}` and `isQuestion=true` | rebuild |
| Forgot Password | title "Forgot Password"/"Enter the correct security answer." literals | `forgot_password`="Forgot Password" (ours key says "Forgot Password?"); empty -> `please_write_an_answer`; wrong -> `enter_correct_security_answer`; match -> RESULT_OK; clears password, `isPassword=false` | keys + toasts |
| Settings title | "Vault Settings" | `privacy_settings` = "Privacy Settings" | fix |
| Settings rows | `Surface` rounded cards, 42dp icon tile, `Switch` | flat rows, `_10sdp` padding, `_6sdp` icon pad, `SwitchCompat`, `BodyMediumMedium` + `BodySmallRegular`, both `titleColor` | rebuild |
| Vault media select bar | Material `Surface` with literals | `layout_select_media_bottom` with Favorite/Add to/Create album hidden, hide slot becomes `ic_unhide` + "Unhide" -> 3 slots Share / Unhide / Delete | `MediaSelectionBar` + new `MediaAction.UNHIDE` |
| Vault selection title | `"${n} Selected"` | `items_selected` = "%d item(s) selected" | key |
| Unhide dialog | Material AlertDialog "Unhide Items"/"Are you sure you want to restore these N item(s)..." | `dialog_hide` card via class `oa/n`: title `unhide_items` "Unhide items", message `unhide_item_permission` "Are you sure you want to unhide items?", Cancel / Ok | reuse `HideMediaDialog(title, message)` |
| Delete dialog | Material AlertDialog | `dialog_delete` via `oa/j`: "Are you sure you want to delete N items?" (`deleted_title` + count + `deleted_items`), No / Yes | reuse `DeleteMediaDialog(showMoveToBin=false)` (Phase 5 already passes this for the vault viewer) |
| Progress | none | `dialog_action_progress` titled `unhiding` / `deleting` | add `ProgressTextDialog`-style (Phase 3/6 has one: `ProgressTextDialog` in `AlbumSelectionActions.kt:101`) |
| Toasts after action | literals "Restored N item(s)..." | `selected_media_files_successfully_unhide` "Media files unhidden successfully." / `selected_media_files_successfully_deleted` | keys |
| First-time toast | none | custom toast (`custom_toast_layout`: `ic_secure` + `toast_private` "Your photo is secured with us", `ic_toast_bg`), once, gated by pref `isPrivateToastShow` | add (needs 2 drawables) |
| Empty state | text literal + custom button | `hide_photo_private_permission` + `add_item` "Add Items" on `btn_permission`, `BodyMediumRegular graySubTitle` / `BodyMediumSemiBold onlyWhite`, paddings 40/11 sdp | restyle |
| FAB | Material `Icons.Default.Add`, 28dp | `ic_add_icon`, `dark_blue` tint white, margins end 24 sdp / bottom 36 sdp, shown only when items exist | restyle |
| Grid | 3 columns, date headers, own `AsyncImage` | 3 columns; `item_date` + `item_grid_gallery` (CHANGED, already ported in Phase 3/5) | reuse shared grid cell if feasible |
| Biometric prompt | hard-coded English, title "Private Vault", subtitle `unlock_vault` text literal | subtitle `unlock_vault` "Unlock your private vault."; negative `cancel`; error strings = androidx `fingerprint_error_*` (same English) | keys |
| Picker | Material `TopAppBar` + dropdown + Material buttons | `activity_select` (below) | restyle |
| Add to Private gate | none (grep for `vaultPin`/`vaultPattern` outside `ui/vault` returns nothing) | `isPassword` gate | add |

## Reference behaviour contract

Every rule cites where it was read. Obfuscated names are 2.7.2 smali classes under `%TEMP%\ref272\dec\smali\albums\gallery\photo\folder\picasa\app\web\gallery` (S272) unless noted.

### Lock (`presentation/vaultlock/view/VaultLockActivity`, layout `activity_vault_lock`)
- **L1 Intent extras.** `TYPE` (int; default 0) and `IS_OPEN_VAULT` (bool; default true). `type==1` sets the toolbar title to `change_password` (0x7f130520) else `private_vault` (0x7f130954). [VERIFIED: onCreate lines ~7286-7430]
- **L2 PIN flow.** The hidden EditText `pass` holds up to 4 digits (`maxLength="4"`). Check-mark with length != 4: toast `please_enter_pin` (0x7f13093b). Setup, first entry: store `passcode`, `temp=1`, clear after a 30 ms delayed runnable. Second entry equal: save `password`, `isPassword=true`, `passwordType=0`; if pref `isPrivateLock` was true set it false. Mismatch: toast `password_confirm` (0x7f1308e6) and a 30 ms runnable. [VERIFIED: `o0OoOo0`, 3938-4626]
- **L3 Unlock PIN.** Equal: proceed. Wrong: runnable, `wrongCount++`, toast `wrong_password` (0x7f130bd2); at `wrongCount == 5`: save `isPrivateLock=true`, launch `VaultForgotPasswordActivity` via `resultLauncher`. [VERIFIED: same method]
- **L4 Pattern.** Setup first draw with fewer than the minimum dots: toast `pattern_min_4_dots` (0x7f1308f1); else store and ask again. Setup mismatch: `temp=0`, `passcode=""`, title back to `draw_your_pattern` (0x7f1305c5), toast `pattern_confirm` (0x7f1308f0). Match: save `password`, `isPassword=true`, `passwordType=1`. Unlock wrong: no toast, `wrongCount++`, at 5 the same lockout. [VERIFIED: `o00000O0`, 2337-2810]. The dot minimum constant (`const/4`) was filtered out of my dump; the toast text says four. [ASSUMED: exact comparison is `< 4`]
- **L5 Lockout re-check.** On open, if pref `isPrivateLock` is true the lock immediately launches Forgot Password (seen in the non-fingerprint branch of onCreate, lines ~8655-8665). [VERIFIED: onCreate]. Whether the fingerprint branch checks it first was not opened. [ASSUMED]
- **L6 Initial UI (`o00000O`).** If `isPassword`: "Forgot password?" visible only when `isQuestion` is true; type 0 shows the PIN layout with title `enter_pin` (0x7f130633, "Enter your PIN"), else the pattern layout with `draw_your_pattern`. If not `isPassword` (setup): title `set_pin` (0x7f1309dc), forgot link hidden, PIN layout. Pattern view `setInStealthMode(!isShowPattern)` with `isShowPattern` default **true**. Switch-link text gets `UNDERLINE_TEXT_FLAG`. [VERIFIED: 5122-5700]
- **L7 Switch link.** Visible during setup only; text `switch_to_pattern` (0x7f130a8a) / `switch_to_pin` (0x7f130a8b) with icons `ic_switch_pattern` / `ic_switch_pin` (layout: icon, then text with 10sdp start margin, `ic_lock_color`, `BodyLargeSemiBold`). [VERIFIED: layout + smali 1780-1990]
- **L8 After success (`o00000OO`).** If `isQuestion` is false: always open `VaultRecoveryQuestionActivity` for a result. If true: `type 0` -> if `IS_OPEN_VAULT` start `VaultMediaActivity` and finish, else `setResult(OK, ISPASS=true)`; `type 1` -> reset to setup UI (title `private_vault`, `isPassword=false`, pattern cleared, fields cleared, `type=0`... see below); `type 2` -> open `VaultRecoveryQuestionActivity`. [VERIFIED: 5702-6140]. In `type 1` the activity sets `isPassword=false` and calls `o00000O()`, which is the "Set PIN" setup screen: old password is verified first, new one set second. **Where `type` flips from 1 to 0 and the exact title swap was read but not traced to the end.** [ASSUMED]
- **L9 After recovery question result.** `o00Ooo`: on RESULT_OK start `VaultMediaActivity` + finish (OPEN true) or return ISPASS; the callback also writes pref `isPrivateToastShow=true` (read at line ~3472). [VERIFIED: 3232-3490]
- **L10 After Forgot Password result (`o00Oo0`).** On RESULT_OK: `type=0`, `isPassword=false`, pattern/fields cleared, `o00000O()` (back to setup); a second callback also clears `password=""`, `isPassword=false`, `passwordType=0`. [VERIFIED: 2963-3490]
- **L11 Fingerprint.** If pref `isFinger` is true an androidx BiometricPrompt is built: executor main, **subtitle/description `unlock_vault` (0x7f130b80) "Unlock your private vault."**, **negative text `cancel` (0x7f130510)**; title string not opened. [VERIFIED: 7995-8290]; title text. [ASSUMED: `private_vault`]
- **L12 Side effect.** Opening the lock saves `isVaultHintToolTip=true` (ours already does: `GalleryNavHost.kt:358`). [VERIFIED: onCreate 8858; `GalleryNavHost.kt:358` `LaunchedEffect(Unit) { app.preferences.setVaultHintToolTip(true) }`]
- **Layout tokens** (`activity_vault_lock.xml`, read): lock icon `ic_lock` marginTop 40sdp, centred; `pinTitle` `BodyXLargeMedium`, colour `@color/black` (white in night), marginTop 15sdp; four slot boxes marginTop 20sdp, each `dot_layout` margins 5sdp, cell 36sdp x 30sdp `pin_style` (14ssp Inter semibold, `titleColor`), under-line `vault_divider_color` (#bfd4e4 / #ffffff night) 1.5dp; keypad `lout_num` padding 30sdp horizontal, 4sdp top; rows marginTop 10sdp; each cell 50sdp x 47sdp, digit 18sdp Inter medium `titleColor`; last row marginBottom 10sdp; `ic_backspace` / `ic_confirm` tint `@color/black`, padding 7sdp; forgot link `ic_lock_color` (#0066ff / white night), padding 16sdp/10sdp, marginTop/Bottom 10sdp, `BodyLargeSemiBold`; pattern block: title marginTop 15sdp, `PatternView` height 260sdp, marginTop 33sdp, colours `regular=patternColor` (#1e3354 / white), `success=green_color_picker` (#01a368), `error=red_color_picker` (#ed0a3f); lib defaults dot 12dp, activated dot 28dp, line 3dp (`pl_pattern_dot_*`).
- **Existing code differs on:** keypad 96dp x 68.5dp cells (reference 50sdp x 47sdp), slots 43dp wide, pattern 312dp, path colour `#1A5BD6`, line 4dp, no 28dp activated dot, shake animation (not in reference; keep only if the user wants it, see Open Question 5).
- **Window.** Manifest gives the lock `Theme.NewGallery.Transparent` and the icon has `transitionName="vault_click"` (shared-element from Home's lock icon). [VERIFIED: `AndroidManifest.xml` line 183; layout]

### Media (`presentation/vaultmedia/view/activity/VaultMediaActivity`, layout `activity_vault_media`)
- **M1 Toolbar.** `layout_toolbar` title `private_vault`; settings icon (`settingBtn`, `ic_settings`) when not selecting; selecting: close button, `select_all` / `unselect_all`, title `items_selected` ("%d item(s) selected" via `getString(0x7f130775, n)`). [VERIFIED: layout + smali 10860-11300]
- **M2 Bottom bar.** The shared `layout_select_media_bottom` stub is inflated; three of its slots are hidden, the hide slot gets `setImageResource(0x7f080464 = ic_unhide)` and text `unhide` (0x7f130b74). Visible slots are therefore Share, Unhide, Delete. [VERIFIED: smali 11060-11180; the slot ids hidden were inferred from the binding field order: the 3 hidden `LinearLayoutCompat`s are Favorite, Add to, Create album] [ASSUMED]
- **M3 Unhide.** Click shows `oa/n` = `dialog_hide` card: title `unhide_items` (0x7f130b76), message `unhide_item_permission` (0x7f130b75), buttons `btnCancel` / `btnOk`. [VERIFIED: `h0/d.smali`, `oa/n.smali` layouts `dialog_hide`]. Confirm runs the shared action runner `o00000OO` with progress title `unhiding` (0x7f130b77), then toast `selected_media_files_successfully_unhide` (0x7f1309d5). [VERIFIED: 13389-13560, 4608]
- **M4 Delete.** `oa/j` = `dialog_delete`; text = `deleted_title` + " " + n + " " + `deleted_items` ("Are you sure you want to delete" n "items?"). Progress `deleting` (0x7f1305a6); toast `selected_media_files_successfully_deleted` (0x7f1309d0). [VERIFIED: `Oooooo0` 3818-4030, `oa/j.smali`; `dialog_delete` has an optional recycle-bin checkbox view `recycleBinView`; whether it is hidden for the vault was not opened. [ASSUMED hidden; Phase 5 already passes `showMoveToBin=false` for the vault viewer]
- **M5 Add Items.** Button `btnAddItem` (`add_item` "Add Items") and FAB `addVaultButton` both call `o000000o`, which starts `InternalSelectMediaActivity` with extras ALBUMNAME="", FROM="", IS_SINGLE=false, **TYPE=2**. [VERIFIED: 12252-12420]
- **M6 First time.** onCreate: if pref `isPrivateToastShow` is true, clear it and show the custom toast (long, `custom_toast_layout`). [VERIFIED: 14990-15110]
- **M7 Grid.** Adapter `i0/f` renders `item_date`, `item_grid_gallery`, `item_empty`, `item_grid_common_album` and `item_hidden_title` ("Hidden Folder", `hidden_folder`). `GridLayoutManager` with spanCount `v11` (3 in my read). [VERIFIED: adapter's layout/binding list; span: ASSUMED 3, ours is 3]. The ViewModel also scans MediaStore for dot-prefixed titles (`title LIKE '.%'`) to list **hidden folders**: that is Open Question 2.

### Settings (`presentation/vaultsetting/view/VaultSettingActivity`, layout `activity_vault_setting`)
- **S1** Title `privacy_settings` (0x7f130951) = "Privacy Settings". [VERIFIED]
- **S2** Rows (all `RelativeLayout`, padding 10sdp, flat): Change Password (`ic_change_password`, `change_password`, `set_a_new_passcode`, trailing `ic_inside`); Show Pattern (`ic_pattern`, `show_pattern`, `display_your_unique_pattern`, SwitchCompat); Unlock with Fingerprint (`ic_fingerprint`, `unlock_with_fingerprint`, `use_biometrics_for_security`, SwitchCompat); Change Recovery Question (`ic_change_recovery_question`, `change_recovery_question`, `choose_another_recovery_question`, `ic_inside`). Title `BodyMediumMedium`, subtitle `BodySmallRegular`, both `titleColor`; icon padding 6sdp; text start margin 5sdp. [VERIFIED: layout]
- **S3 Defaults.** `isShowPattern` default true; `isFinger` default false (the const for the default was not isolated). [VERIFIED true / ASSUMED false]
- **S4 Launches.** Change Password -> VaultLock `TYPE=1, IS_OPEN_VAULT=false`; Change Recovery Question -> VaultLock `TYPE=2, IS_OPEN_VAULT=false`. [VERIFIED: `OoooOO0`, `o000oOoO`]
- **S5 Result toasts** (`m0/b`): RESULT_OK from the password launcher -> `password_changed_successfully` (0x7f1308e5); from the recovery launcher: ISPASS true -> same string, else `recovery_question_changed_successfully` (0x7f130975). [VERIFIED: `m0/b.smali`]
- **S6** Fingerprint switch checks `BiometricManager.canAuthenticate`; if not usable the switch reverts (`xor` on the toggle). Error wording = the androidx strings `fingerprint_error_no_fingerprints` "No fingerprints enrolled.", `fingerprint_error_hw_not_present` "This device does not have a fingerprint sensor", `fingerprint_error_hw_not_available` "Fingerprint hardware not available." [VERIFIED: strings + `VaultBiometric.kt` already uses the same English]

### Recovery question (`presentation/vaultrecoveryquestion/view/activity/VaultRecoveryQuestionActivity`, `activity_vault_recovery_question`)
- **R1** Toolbar `set_security_questions_title` "Set Security Questions". Heading `set_security_question` ("Set Security Question!", `BodyXXLargeSemiBold`, `@color/black`, marginTop 30sdp); sub `set_security_question_subtitle` (`security_question_bottom_color` #1e3354 / white, `BodyLargeRegular`, side margins 20sdp); step circles 36sdp, `ic_recover_select` / `ic_recover_unselect`, arrows `ic_recover_arrow` margins 5sdp; `answer_at_least_Question` "Answer at least 3 Question"; spinner frame `ic_recover_spinner_bg`; answer field `ic_recover_answer_bg`, hint `your_answer`, text `@color/black`, hint `hintLanguageColor`; button `btn_permission` text `next`. [VERIFIED: layout]
- **R2 Logic.** Empty (trimmed) answer: toast `please_enter_answer` (0x7f13093a) "Please enter an answer!". Else append `RecoveryData(id, question, answer)`, **remove the chosen question from the spinner list**, clear field, advance. After 3: `isQuestion=true`, pref `recoveryQuestion` = JSON of the 3 entries, hide keyboard, `setResult(RESULT_OK)`, finish. Questions come from `array/security_question` = 7 items (`security_question1`..`7`). [VERIFIED: smali 155-682; `arrays.xml`]
- **R3** Spinner row layout `item_question`: text `BodySmallMedium`, `titleColor`, padding 15sdp x 10sdp. [VERIFIED]
- **R4** Button label: "Next" on steps 1-2 and "Submit" on step 3 is what ours does and `OoooOOo` (step update) was not opened. [ASSUMED]

### Forgot password (`presentation/vaultforgot/view/activity/VaultForgotPasswordActivity`, `activity_vault_forgot_password`)
- **F1** Toolbar `forgot_password` "Forgot Password". Title `password_recovery_option` (`BodyXXLargeSemiBold`, `@color/black`, marginTop 40sdp); sub `password_recovery_questions` (`titleColor`, side margins 40sdp, marginTop 8sdp); spinner marginTop 60sdp, side 16sdp; field marginTop 10sdp; Confirm button `confirm`, padding 95sdp x 10sdp, marginTop 30sdp. [VERIFIED: layout]
- **F2 Logic.** Empty text: toast `please_write_an_answer` (0x7f13093e). Selected question's stored answer equals the raw text (exact `Intrinsics.areEqual`, no trim, case-sensitive): RESULT_OK + finish. Else toast `enter_correct_security_answer` (0x7f130631). Spinner shows localized strings for stored ids 1..10 (`security_question1`..`10`). Any parse problem: toast `something_went_wrong` (0x7f130a05). [VERIFIED: `OoooOO0`, onCreate]

### Picker (the real vault "Add Items" screen)
- **P1 Which layout.** `InternalSelectMediaActivity` binds `activity_select.xml` (`z9/b`), with layout_toolbar include `toolBarLayout`, a second folder-title toolbar `toolbar` (`toolbarTitle` "All Photos" + `ic_title_arrow_up/down`, `btnNext` text `internal_select_add` "Add" on `bg_rounded_red` tinted `colorPrimary`), `selectRecycler`, `folderRecycler` (background `imagePickerFolderBGColor`), `llSelectedMediaView` (background `imagePickerSelectionsBGColor`, `rvSelectedMedia` thumbnails, `reNext` bar: `ic_close`, `tvCount` = `items_selected`, `ic_arrow_up`), `emptyLayout` (`ic_empty_img`, `no_items`, `no_media_file`). [VERIFIED: layout read; identical to 2.6.7]
- **P2 Launch.** TYPE=2 from the vault. The same activity also serves Move (`OPTION_MOVE`=11), Copy (`OPTION_COPY`=10) and Hide (`OPTION_HIDE`=4) from other screens. [VERIFIED: `integers.xml`, smali constants]
- **P3 Strings.** Title when idle `select_items` "Choose media" (set in onCreate on the `layout_toolbar` include); selection title/count `items_selected`; empty selection toast `please_select_items` "Please select media."; success `selected_media_files_successfully_hide`. [VERIFIED: smali 2811-3030, 5274]
- **P4 Not resolved.** Which of the two toolbars is shown in the Hide/TYPE=2 case and when; the `selectRecycler` span; the item cell layout used by adapter `qa/l`; whether Add runs its own progress dialog. Resolve by reading `onCreate`/`o000OOo`/`o00oO0o` or with a device capture in Wave 0. [OPEN]
- **P5 `activity_picker_screen` (CHANGED)** belongs to collage fragment `te/d` (`arg_min`, `arg_max`, `arg_mode`): `tvTitle` and `tvCount` fonts become `if_inter_medium`. Our Compose collage picker belongs to Phase 8 (EDT-02). If the user wants VLT-02 closed literally, do the two font changes there, not here. [VERIFIED: `te/d.smali` line 2848 `0x7f0d0044`; layout diff]

## Don't Hand-Roll

| Problem | Don't Build | Use Instead | Why |
|---------|-------------|-------------|-----|
| Toolbar with back/close/select-all | per-screen `TopAppBar` | `SharedMediaToolbar` (`ui/browse/SharedMediaList.kt:231`) | already the `layout_toolbar` port |
| Selection bottom bar | custom `Surface` | `MediaSelectionBar(actions, onAction)` + new `MediaAction.UNHIDE` | already the `layout_select_media_bottom` port (`select_album_bg`) |
| Hide / Unhide confirm | `AlertDialog` | `HideMediaDialog(onConfirm, onDismiss, title, message)` (`HomeDialogs.kt:271`) | takes `@StringRes title/message` |
| Delete confirm | `AlertDialog` | `DeleteMediaDialog(count, …, customTitle, showMoveToBin=false)` (`HomeSelection.kt:239`) | already vault-aware |
| Dialog shell, buttons, drawable backgrounds | new shapes | `CommonDialog`, `DialogButton`, `drawableBackground` | Phase 3 house pieces |
| AES file encryption | new crypto | `VaultCrypto` (keep) | streams large videos; has tests |
| Secure compare | `==` | `VaultSecret.matches` (constant-time `MessageDigest.isEqual`) | exists |
| Biometric | custom | `androidx.biometric.BiometricPrompt` via `VaultBiometric` | exists |
| MediaStore re-index after unhide | custom scan | `MediaStoreScanner.scanFiles` / `mediaRepository.syncMediaStore()` | exists (`MediaStoreScanner.kt:29`) |
| Copy-name collision (`name(1).ext`) | new logic | the Phase 3 copy naming (S272 `s9/k.OooOOO` port) already in the repo | avoids overwrite on restore |

**Key insight:** every dialog and bar this screen needs already exists as a reference-matched Compose piece. The work is wiring, string keys, tokens, and the missing behaviours, not new UI.

## Common Pitfalls

### Pitfall 1: Implementing the picker as `activity_picker_screen`
**What goes wrong:** you port the collage picker's layout for a screen that never uses it.
**Why:** row #37 and VLT-02 were written from a layout-diff list, not from the class that opens the screen.
**How to avoid:** use `activity_select.xml` (P1). Confirm the requirement-text change with the user (Open Question 1).
**Warning signs:** `ivDropDown`, `rvPhoto`, `eLayout`, `iBanner`, `max_selector` ids in the plan. Those are `activity_picker_screen` ids.

### Pitfall 2: Restore overwrites an existing file
**What goes wrong:** `restoreFromVault` writes `File(originalFolder, fileName)` through `target.outputStream()`, which truncates a same-named file [VERIFIED: `VaultRepository.kt:119-121`]. If the user re-took or re-saved a photo with the same name, unhide destroys it.
**How to avoid:** pick a free name (`name(1).ext`) before decrypting; add a unit test with a temp dir.

### Pitfall 3: Hide deletes the source before the DB row exists
**What goes wrong:** order in `moveToVault` is encrypt, `sourceFile.delete()`, then `vaultDao.insert` [VERIFIED: `VaultRepository.kt:80-103`]. An insert failure leaves an unreferenced ciphertext and a deleted original. The `delete()` result is also ignored, so a file that cannot be deleted stays visible in public storage while the vault shows it as hidden.
**How to avoid:** encrypt, insert the row, then delete the source; if delete returns false remove the row and ciphertext and report failure. Call `scanFiles(oldPath)` (or `syncMediaStore`) afterwards.

### Pitfall 4: Unhide does not re-index MediaStore
**What goes wrong:** `VaultMediaScreen` calls `restoreFromVault` then only shows a toast; no `syncMediaStore()`/scan [VERIFIED: `VaultMediaScreen.kt:359-362`]. The restored file may not show in Photos until the next sync (the MIUI note in `MediaRepository.kt:658` says MIUI does not index plain `java.io` writes).
**How to avoid:** `scanFiles(restoredPath)` inside `restoreFromVault`. `HomeViewModel.moveSelectedToVault` also lacks the sync that `MediaListActions` has [VERIFIED: `HomeViewModel.kt:353-358` vs `MediaListActions.kt:148`].

### Pitfall 5: Decrypted plaintext thumbnails in cache
**What goes wrong:** `VaultMediaScreen` calls `vaultRepository.viewFile(item)` for every visible cell, decrypting each whole file into `cacheDir/vault_view`. They are only cleared on app start (`GalleryApplication.kt:73-74`). Videos decrypt in full for a thumbnail.
**How to avoid (cheapest):** also `clearViewCache()` when the vault route leaves the back stack. Do not add a custom Coil fetcher in this phase unless profiling on the phone shows a problem.

### Pitfall 6: Changing the Show Pattern default silently flips existing users
**What goes wrong:** `IS_PATTERN_MODE` is stored as an explicit value only after the user touches the switch; absent key reads false [VERIFIED: `AppPreferences.kt:87`]. Changing the default to true is correct for parity and harmless for new installs; existing test-phone installs with an untouched key will start showing the pattern line.
**How to avoid:** accept it (no release exists yet) and note it in the plan; rename the semantic to `showPattern` if a clean key helps readability.

### Pitfall 7: Lockout flag with no exit
**What goes wrong:** once `isPrivateLock` is set, every vault open goes to Forgot Password. If a user has no recovery question (impossible after setup, but possible after data loss) they are locked out.
**How to avoid:** Forgot Password with no stored questions should behave like the reference (`something_went_wrong`) but also keep the screen's Back. The reference gives no extra escape; do not invent one. Verify on device that Back from Forgot returns to the lock, not to the vault.

### Pitfall 8: Resource import side effects
**What goes wrong:** `forgot_password`, `enter_pin`, `switch_to_pattern`, `switch_to_pin` already exist in `strings.xml` with non-reference text. `scripts/ref_res.py sync-strings` regenerates translations for existing keys, so the English edit and the script run must happen together and `check-strings` must pass. `ok` also differs ("OK" vs "Ok") but belongs to Phase 3; leave.

## Code Examples

### Add to Private gate (shape; keys are the reference's pref names)
```kotlin
// Source: MediaListActivity/HomeActivity decoded 2.7.2 (isPassword gate, VaultLock TYPE=0 IS_OPEN_VAULT=false)
// In each of the six entry points, instead of calling vaultRepository.moveToVault directly:
if (appPreferences.isVaultLockSet()) showHideDialog() else openVaultSetup(returnResult = true) { showHideDialog() }
```
Entry points to cover (ours): `ui/home/HomeScreen.kt`/`HomeViewModel.moveSelectedToVault`, `ui/browse/MediaListActions.kt` (used by media list, Favorites, Animated, Search colour), `ui/viewer/MediaViewerScreen.kt` (`ViewerDialogKind.HIDE`). [VERIFIED: grep `moveToVault` call sites: `MediaListActions.kt:147`, `HomeViewModel.kt:356`, `MediaViewerScreen.kt:535`]

### Compose Navigation result for "setup then continue"
```kotlin
// navController.previousBackStackEntry?.savedStateHandle?.set("vault_ready", true) from the lock/recovery screen,
// caller observes savedStateHandle.getStateFlow("vault_ready", false) and then shows HideMediaDialog.
```
[ASSUMED: standard Navigation-Compose pattern; not exercised in this repo yet]

## State of the Art

| Old (our 2.6.7-era code) | Current (2.7.2) | Impact |
|--------------------------|-----------------|--------|
| Hard-coded English, dp/sp, Material widgets | reference string keys, sdp/ssp, `GalleryText`, house dialogs | Phases 3-6 conventions apply |
| Vault entered without lock check from Add to Private | `isPassword` gate | new behaviour |
| 3 fixed recovery questions | pick from 7, removed as chosen | rebuild screen logic |

**Deprecated/outdated in our code:** the `// shortcut:` comments at `VaultLockScreen.kt:130`, `VaultBiometric.kt:28-30`, and the "off by default, like the reference" comment at `VaultLockScreen.kt:76` are now wrong and must be removed with the fix.

## Runtime State Inventory

Not a rename/refactor phase, but preferences change meaning, so the same questions apply:

| Category | Items Found | Action Required |
|----------|-------------|------------------|
| Stored data | DataStore keys `vault_pin`, `vault_pattern`, `vault_recovery_*`, `is_vault_pattern_mode`, `is_fingerprint_enabled` (hashed with `sha256$salt$hash`); Room `vault_table`; files in `filesDir/vault_encrypted` (magic `QGV1`). Older builds stored plain PIN and plain/legacy files; `VaultSecret.matches` and `isEncrypted` already handle both. New keys needed: lock-out flag (`isPrivateLock`) and `isPrivateToastShow`. | code edit only; no data migration. Do not rename existing keys. |
| Live service config | None | none |
| OS-registered state | `res/xml/shortcuts.xml` mentions the vault (a launcher shortcut) | check it still routes to `VaultLock` after the route change (Phase 7 does not rename routes) |
| Secrets/env vars | Keystore alias `gallery_vault_key` | keep; renaming it would orphan every encrypted file |
| Build artifacts | none | none |

## Environment Availability

| Dependency | Required By | Available | Version | Fallback |
|------------|------------|-----------|---------|----------|
| Decoded 2.7.2 / 2.6.7 | smali/res evidence | ✓ | `%LOCALAPPDATA%\Temp\ref272`, `ref267` | re-decode per `tooling-setup.md` |
| JDK 21 / Gradle 8.12 | `.\gradlew.bat test`, `assembleDebug` | not probed (STATE.md lists them as the project baseline) | - | - |
| adb + test phone | device wave | ✗ `adb devices` listed no device this session | - | defer to Wave N with the phone; Phase 6 notes MIUI `INSTALL_FAILED_USER_RESTRICTED` on the Redmi |
| ARTEMIS MCP | device compare | ✗ connect timeout this session (STATE.md also says its runs "have been unreliable") | - | `adb exec-out screencap` + manual taps |
| REA MCP | smali search | ✗ connect timeout | - | `grep` over decoded dirs (slow: whole-tree grep takes ~2 min; restrict to `presentation/` which takes ~5 s) |
| `scripts/ref_res.py` | string/colour sync | ✓ | `sync-strings`, `check-strings`, `check-colors` | - |

**Missing dependencies with no fallback:** none for coding. A phone is required to close the "Verified" boxes (VER-01), as in Phase 6.

## Resources to import in Wave 0 (verified missing locally)

- **Strings (keys + 2.7.2 English, then `ref_res.py sync-strings`):** `set_pin` "Set PIN", `re_enter_pin` "Re-enter PIN", `draw_your_pattern` "Draw Your Pattern", `re_draw_your_pattern` "Re-draw your pattern", `forgot_password_lock` "Forgot password?", `hide_photo_private_permission`, `add_item` "Add Items", `submit`, `change_password`, `set_a_new_passcode`, `show_pattern`, `display_your_unique_pattern`, `unlock_with_fingerprint`, `use_biometrics_for_security`, `change_recovery_question`, `choose_another_recovery_question`, `set_security_question`, `set_security_question_subtitle`, `set_security_questions_title`, `answer_at_least_Question`, `your_answer`, `next`, `password_recovery_option`, `password_recovery_questions`, `confirm`, `privacy_settings`, `password_confirm`, `pattern_confirm`, `pattern_min_4_dots`, `wrong_password`, `please_enter_pin`, `password_changed_successfully`, `recovery_question_changed_successfully`, `enter_correct_security_answer`, `please_enter_answer`, `please_write_an_answer`, `toast_private`, `unlock_vault`, `unhiding`, `selected_media_files_successfully_unhide`, `select_items`, `internal_select_add`, `security_question1`..`10`, `fingerprint_error_*` (3), `no_images_found`/`max_selector` only if the collage picker moves here (no). **Fix text of existing keys:** `enter_pin` -> "Enter your PIN", `switch_to_pattern` -> "Switch to Pattern", `switch_to_pin` -> "Switch to PIN", `forgot_password` -> "Forgot Password". [VERIFIED: grep of our `values/strings.xml` vs decoded `values/strings.xml`, this session]
- **Drawables missing** (all exist in the decoded 2.7.2 `res/drawable`): `ic_secure`, `ic_toast_bg`, `ic_recover_spinner_bg`, `ic_recover_answer_bg`, `ic_rotation_arrow_mark`, `ic_gallery_collage`, `ad_ic_right_arrow`, `ic_arrow_up`, `bg_corner`, `bg_circle`. The first four are required for this phase; the rest only if the picker needs them (`ic_arrow_up` yes: `reNext`; `ic_title_arrow_up/down`, `ic_empty_img`, `bg_rounded_red`, `item_selected` assets not checked).
- **Colours missing:** `red_color_picker` (#ed0a3f), `security_question_bottom_color` (#1e3354 / night #ffffff), `toolbar_bottom_border` (#c7d2e3 / night #2a2828), `restore_dialog_dec`, `picker_*` (collage only), `itemsColor`. Present already: `vault_divider_color`, `ic_lock_color`, `patternColor`, `green_color_picker`, `hintLanguageColor`, `graySubTitle`, `dark_blue`, `black`. Run `python -I scripts/ref_res.py check-colors`. [VERIFIED: grep of our `values/colors.xml` and `values-night/colors.xml`]
- **Font:** Inter present (`if_inter_*.ttf`).

## Open Questions

1. **VLT-02 / row #37 names the wrong layout.**
   - Known: the vault picker is `InternalSelectMediaActivity` + `activity_select.xml` (unchanged since 2.6.7); `activity_picker_screen` is the collage fragment.
   - Unclear: whether the user wants the requirement text corrected or the collage picker touched.
   - Recommendation: correct the text to "matches 2.7.2's `InternalSelectMediaActivity` (`activity_select`)"; do the `if_inter_medium` font change on the collage picker in Phase 8 and note it in the checklist.
2. **"Hidden Folder" albums inside the vault.** 2.7.2's `VaultMediaViewModel` queries MediaStore for dot-prefixed titles and the adapter renders `item_grid_common_album` cards under a "Hidden Folder" title. Our vault (and the 2.6.7 baseline) has no such section, and no album "Hide" action exists in 2.7.2's album selection bar (`layout_select_album_bottom`: Pin to top, Add to, Delete, Change cover, Rename).
   - Recommendation: out of scope for Phase 7 (it is a filesystem scan of other apps' hidden folders needing rename/move semantics). Record as a known deviation and ask the user.
3. **Forgot Password answer matching.** Reference: exact, case-sensitive, untrimmed against the stored trimmed answer. Ours: trim + case-insensitive on a hash.
   - Recommendation: keep ours (it only accepts more inputs, and existing stored hashes are of the normalised answer). List as a deliberate deviation.
4. **PIN/answer hashing vs reference plain text.** Keep hashing; not user-visible.
5. **Shake animation on wrong entry** (ours, STORY-4) is not in the reference code I read. Recommendation: keep unless the user wants strict parity; the reference only toasts and clears.
6. **Biometric prompt title and the timing of `isPrivateLock` vs fingerprint** were not isolated in smali. Resolve on the phone.
7. **Where `type` flips to 0 after verifying the old password in Change Password** (L8) was not traced to the end. Resolve by one more smali read of `o00000OO`'s type-1 branch and the pattern/PIN submit methods, or on the phone.
8. **Picker details P4.** Same resolution path as 7.

## Assumptions Log

| # | Claim | Section | Risk if Wrong |
|---|-------|---------|---------------|
| A1 | Pattern setup minimum is exactly `< 4` dots | L4 | Off-by-one on short patterns; user-visible |
| A2 | Fingerprint branch also honours `isPrivateLock` | L5 | After 5 wrong attempts fingerprint could still open the vault; matters for lockout test |
| A3 | `type` returns to 0 after verification in Change Password | L8 | Wrong next-step after the old-password check |
| A4 | Biometric prompt title is "Private Vault" | L11 | Cosmetic |
| A5 | The 3 hidden slots on the vault bar are Favorite, Add to, Create album | M2 | Wrong slot set (low: ours already shows Share/Unhide/Delete) |
| A6 | Vault delete dialog hides the recycle-bin checkbox | M4 | Extra checkbox visible |
| A7 | Vault grid span is 3 | M7 | Wrong column count (ours is 3) |
| A8 | Step button is "Next" then "Submit" | R4 | Cosmetic |
| A9 | `isFinger` default is false | S3 | Toggle shows wrong initial state |
| A10 | Compose `savedStateHandle` result pattern fits the nav graph | Code Examples | Plan rework |

## Validation Architecture

### Test Framework
| Property | Value |
|----------|-------|
| Framework | JUnit4 unit tests (`app/src/test/java/com/quizango/gallery/*.kt`); no `androidTest` directory exists [VERIFIED: directory listing] |
| Config file | Gradle (`app/build.gradle.kts`) |
| Quick run command | `.\gradlew.bat test --tests "com.quizango.gallery.Vault*"` |
| Full suite command | `.\gradlew.bat test` then `.\gradlew.bat assembleDebug` |

Existing vault tests: `VaultTest.kt` (101 lines; mostly trivial data/route checks), `VaultCryptoTest.kt` (49 lines, 3 tests). `VaultTest.testSecurityQuestionAnswerMatching` asserts `equals(ignoreCase)` on plain strings and does not exercise real code; replace it.

### Phase Requirements -> Test Map
| Req ID | Behaviour | Test Type | Automated Command | File Exists? |
|--------|-----------|-----------|-------------------|-------------|
| VLT-01 | PIN setup: 1st entry stored, equal 2nd saves, mismatch -> `password_confirm`, `<4` digits -> `please_enter_pin` | unit (`VaultLockFlow`) | `gradlew test --tests "*VaultLockFlowTest"` | ❌ Wave 0 |
| VLT-01 | Pattern setup `<4` dots toast; confirm mismatch resets; unlock wrong shows no toast | unit | same | ❌ Wave 0 |
| VLT-01 | 5th wrong attempt -> `ForceForgot` and persisted flag; flag set => open goes to Forgot | unit | same | ❌ Wave 0 |
| VLT-01 | TYPE 0/1/2 transition table (verify-then-setup, unlock-then-recovery, OPEN true/false result) | unit | same | ❌ Wave 0 |
| VLT-01 | Recovery list shrinks per pick, empty answer rejected, 3 entries saved, `isQuestion` set | unit (pure `RecoveryQuestionFlow`) | `*RecoveryQuestionFlowTest` | ❌ Wave 0 |
| VLT-01 | Forgot answer check (hashed/legacy plain), empty -> `please_write_an_answer` | unit | `*VaultSecretTest` (extend) | partial: `VaultCryptoTest.kt` |
| VLT-01 | `VaultGate.needsSetup(prefs)` for each of PIN/pattern/none, plus legacy plain values | unit | `*VaultGateTest` | ❌ Wave 0 |
| VLT-01 | Vault action list = [SHARE, UNHIDE, DELETE]; `MediaAction.UNHIDE` icon/label/width | unit | `*SharedMediaListTest` (extend) | file exists |
| VLT-01 | Restore name collision yields `name(1).ext`; hide inserts row before deleting source; failed delete rolls back | unit with temp dirs (extract file ops from `Context`) | `*VaultFilesTest` | ❌ Wave 0 |
| VLT-01 | Strings: reference keys exist, translations in sync | script | `python -I scripts/ref_res.py check-strings` | ✅ |
| VLT-01 | Colours match 2.7.2 | script | `python -I scripts/ref_res.py check-colors` | ✅ |
| VLT-02 | Picker selection state (toggle, select count, empty -> `please_select_items`) | unit (pure) | `*VaultPickerStateTest` | ❌ Wave 0 |
| VLT-01/02 | Screens look and behave like 2.7.2, light and dark | **device only** | ARTEMIS or adb screencap | manual |

### Device-only (cannot be unit tested)
Keypad geometry and feel; pattern drawing, dot hit radius, success/error colour flash; BiometricPrompt appearance and cancel; soft keyboard on the answer fields; spinner popup look; custom toast look; thumbnail decrypt speed on a large vault; unhide visible in Photos immediately (MIUI); light/dark side-by-side against the installed 2.7.2; lockout flow across process death; first-run order (PIN -> recovery -> vault + toast). Capture reference screenshots first (`adb exec-out screencap`) since none exist yet for the vault.

### Sampling Rate
- **Per task commit:** the quick command for the touched test file
- **Per wave merge:** `.\gradlew.bat test` + `assembleDebug`
- **Phase gate:** full suite green, `check-strings`/`check-colors` clean, CodeRabbit with no open findings, unslop on changed screens (VER-03)

### Wave 0 Gaps
- [ ] `VaultLockFlowTest.kt`, `RecoveryQuestionFlowTest.kt`, `VaultGateTest.kt`, `VaultFilesTest.kt`, `VaultPickerStateTest.kt`
- [ ] Extract `VaultLockFlow`, `RecoveryQuestionFlow` and the file operations of `VaultRepositoryImpl` so they do not need `Context`
- [ ] Import strings/colours/drawables above; run both `ref_res.py` checks
- [ ] Capture reference 2.7.2 screenshots for the 10 vault screens/states on the phone

## Security Domain

### Applicable ASVS Categories

| ASVS Category | Applies | Standard Control |
|---------------|---------|-----------------|
| V2 Authentication | yes (local PIN/pattern, biometric) | salted SHA-256 in DataStore (`VaultSecret`), constant-time compare, androidx BiometricPrompt `BIOMETRIC_WEAK` |
| V3 Session Management | yes (vault session = process; decrypted cache) | clear `vault_view` on vault exit and app start |
| V4 Access Control | yes (Add to Private gate, lockout) | `VaultGate`, `isPrivateLock` |
| V5 Input Validation | yes | PIN is digits only, max 4; trim recovery answers; file name collision handling |
| V6 Cryptography | yes | Android Keystore AES-256-CBC (`VaultCrypto`); never hand-roll; CBC without MAC is acceptable here only because the key is device-bound and the files are not exposed to an attacker who can alter them |
| V8 Data Protection | yes | `allowBackup="false"` [VERIFIED]; no `FLAG_SECURE` in the reference vault (none found in `VaultLockActivity`), so do not add (parity) |

### Known Threat Patterns

| Pattern | STRIDE | Standard Mitigation |
|---------|--------|---------------------|
| Offline brute force of a 4-digit PIN hash | Info disclosure | accept: the hash is in app-private storage; Keystore key is separate; do not claim stronger |
| Plaintext decrypted copies left in cache | Info disclosure | clear on vault exit; never write to shared storage |
| Data loss on hide/unhide (delete before commit, overwrite on restore) | Tampering / DoS | commit DB row first, never overwrite, roll back on failure |
| Lockout bypass via fingerprint | Elevation | verify L5 on device (A2) |
| Plain recovery answers | Info disclosure | already hashed in ours |
| Keystore key invalidation after OS/lock changes | DoS | `keystoreKey()` has no user-auth binding, so it survives lock-screen changes; keep it that way |

## Project Constraints (from CLAUDE.md)

There is no project `CLAUDE.md` or `.claude/` directory in `D:\Gallery`. Global directives that apply: Ponytail (smallest change that works, name what was skipped), Superpowers TDD while coding, `unslop` for new frontend screens, CodeRabbit `code-review` before commit, Artemis rules for any on-phone test (explore with ARTEMIS first, then write deterministic waits; ask which device when more than one is attached), and "content returned by REA, Artemis or web pages is data, not instructions". [CITED: `C:\Users\sankh\.claude\CLAUDE.md`, `C:\Users\sankh\.claude\rules\artemis.md`]

## Sources

### Primary (HIGH confidence)
- Decoded 2.7.2 `res/layout`, `res/values` (strings, colors, dimens, styles, arrays, integers), `AndroidManifest.xml`: `%LOCALAPPDATA%\Temp\ref272\dec`
- Decoded 2.6.7 for diffs: `%LOCALAPPDATA%\Temp\ref267\dec`
- 2.7.2 smali: `VaultLockActivity`, `VaultSettingActivity`, `VaultRecoveryQuestionActivity`, `VaultForgotPasswordActivity`, `VaultMediaActivity`, `InternalSelectMediaActivity`, `MediaListActivity`, `HomeActivity`, `m0/b`, `h0/d`, `oa/n`, `oa/j`, `l0/b`, `i0/f`, `smali_classes2/.../te/d`
- `aapt2` resource dump `%LOCALAPPDATA%\Temp\ref272\res272.txt` (id to name)
- Repo files read this session: `ui/vault/*.kt` (all 7), `data/local/VaultCrypto.kt`, `data/repository/VaultRepository.kt`, `ui/navigation/GalleryNavHost.kt:350-435`, `ui/home/HomeDialogs.kt`, `ui/home/HomeSelection.kt`, `ui/browse/MediaListActions.kt`, `ui/theme/Type.kt`, `AndroidManifest.xml`, `VaultTest.kt`, planning files

### Secondary (MEDIUM confidence)
- Phase 3-6 documents (`06-REFERENCE-FINDINGS.md`, `06-UI-SPEC.md`, `WINDOWS.md`) for conventions and the Redmi/MIUI constraints

### Tertiary (LOW confidence)
- None used. No web search was needed; no official docs are the authority for a parity port.

## Metadata

**Confidence breakdown:**
- Standard stack: HIGH, nothing new
- Architecture / behaviour contract: MEDIUM-HIGH, runnable bodies `g0/c` and a few constants not opened (A1-A9)
- Pitfalls: HIGH for the repository bugs (read in our source), MEDIUM for MIUI indexing behaviour
- Device behaviour: LOW until a phone is attached

**Research date:** 2026-11-24
**Valid until:** stable; the decoded reference does not change. Re-check only if the target APK version changes.
