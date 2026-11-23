---
phase: "7"
slug: "private-vault"
status: approved
reviewed_at: 2026-11-24
shadcn_initialized: false
preset: none
created: "2026-11-24"
scope: "Phase 7 Private Vault: lock (activity_vault_lock), vault media (activity_vault_media), settings (activity_vault_setting), recovery question (activity_vault_recovery_question), forgot password (activity_vault_forgot_password), Add Items picker (activity_select, InternalSelectMediaActivity TYPE=2), first-time secured toast (custom_toast_layout), Add to Private lock gate; VLT-01, VLT-02; checklist #32-#37"
---

# Phase 7 — UI Design Contract (Private Vault)

> Parity port, not a new design. Every value below comes from the decompiled reference 2.7.2
> (layout XML for looks, smali for behaviour). The six vault layouts and `activity_select` are
> **byte-identical to 2.6.7** (diffed in 07-RESEARCH.md), so this phase is a fidelity and behaviour port of
> an existing Compose vault, not a redesign. Nothing here is a preference. No phone is available (CONTEXT D7):
> every item that needs the device is marked **PENDING-DEVICE** and takes the most reference-consistent guess
> until a phone is attached; nothing is claimed as verified on a device.
> Tokens (`N.sdp`, `N.ssp`, Inter, `GalleryText.*`, colour names) come from the Phase 1 contract; `SharedMediaToolbar`,
> `MediaSelectionBar`/`MediaAction`, `DeleteMediaDialog`, `HideMediaDialog`, `CommonDialog`, `ProgressTextDialog` from Phases 3-5;
> they are referenced here, not repeated.

**Locked decisions this contract follows (07-CONTEXT.md):** D1 the picker is `activity_select` (not `activity_picker_screen`);
D2 no "Hidden Folder" section (known deviation: the 2.7.2 `hiddenFolderSync` bar, `item_hidden_title` and `item_grid_common_album` cards are not built);
D3 Forgot Password keeps our trimmed, case-insensitive hashed match (deliberate deviation); D4 the wrong-entry shake is removed;
D5 every Add to Private entry point checks that a lock exists first; D6 data safety outranks parity (repository fixes, no visible UI change);
D7 no device: PENDING-DEVICE.

**Evidence roots (read-only):**
- `R272` = `%LOCALAPPDATA%\Temp\ref272\dec\res` (2.7.2 decoded resources). 2.6.7 comparison: `%LOCALAPPDATA%\Temp\ref267\dec`.
- `S272` = `%LOCALAPPDATA%\Temp\ref272\dec\smali\albums\gallery\photo\folder\picasa\app\web\gallery`
- `VL` `presentation/vaultlock/view/VaultLockActivity` (methods `o0OoOo0` PIN submit, `o00000O0` pattern, `o00000O` initial UI, `o00000OO` after success, `o00Ooo`/`o00Oo0` result callbacks); `VM` `presentation/vaultmedia/view/activity/VaultMediaActivity` (`o000000o` add items); `VS` `presentation/vaultsetting/view/VaultSettingActivity` (+ `m0/b` result toasts); `VR` `presentation/vaultrecoveryquestion/view/activity/VaultRecoveryQuestionActivity`; `VF` `presentation/vaultforgot/view/activity/VaultForgotPasswordActivity`; `IS` `com/baselibrary/presentation/internalselectmedia/view/activity/InternalSelectMediaActivity` (`o000OOo` selection changed, binding `z9/b`); `oa/n` unhide dialog (`dialog_hide`), `oa/j` delete dialog (`dialog_delete`).
- **[device]** none this phase. No reference screenshots of the vault exist yet; capture them first when a phone is attached (07-RESEARCH.md Wave 0).
- Ours = `app/src/main/java/com/quizango/gallery/ui/vault/` (7 files), `data/repository/VaultRepository.kt`, `data/local/VaultCrypto.kt`, `app/src/main/res/`.

**Ads (D5 of Phases 3-5), carried:** every ad slot is collapsed, 0 height, no skeleton: `adsLay` in the vault media bottom stack (the 12.dp top margin goes with it),
`rl_native_ad` in Settings and Forgot Password (already `gone`), `rl_banner_ad` in Recovery Question and the picker. No interstitials.

---

## Design System

| Property | Value |
|----------|-------|
| Tool | none (Android, Jetpack Compose; shadcn not applicable) |
| Preset | not applicable |
| Component library | Compose Material 3 (Phase 1). House pieces to reuse, in place of every `TopAppBar`, `AlertDialog`, `Switch`, `DropdownMenu` and Material button now in `ui/vault`: `ui/browse/SharedMediaList.kt` (`SharedMediaToolbar` = `layout_toolbar`), `ui/browse/MediaListActions.kt` (`MediaSelectionBar` = `layout_select_media_bottom`, `MediaAction` + new `MediaAction.UNHIDE`), `ui/home/HomeDialogs.kt` (`CommonDialog`, `HideMediaDialog(title, message)`), `ui/home/HomeSelection.kt` (`DeleteMediaDialog(showMoveToBin=false)`, `DialogButton`, `drawableBackground`), `ui/home/AlbumSelectionActions.kt` (`ProgressTextDialog`), `ui/home/PhotosTab.kt` (`PhotosEmptyState`, `headerKind` Today / Yesterday), Phase 3 fast-scroll thumb. Reuse the Phase 3/5 grid cell for the vault grid where feasible |
| Icon library | Reference vector drawables via `painterResource`. No `material-icons` glyph where the reference ships one (today in `ui/vault`: `Icons.Default.Add` FAB, `ArrowBack`, `Settings`, dropdown arrows: replace with `ic_add_icon`, `ic_back`, `ic_settings`, `ic_title_arrow_up/down`) |
| Font | Inter (Phase 1) |
| Animation | Compose only. Lock: no shake (D4). Pattern view: library defaults (dot 12.dp, activated dot 28.dp, line 3.dp). Window animation: `AnimatedDialog` for dialogs (Phase 3); screens use the Phase 5 route transition. The Home lock-icon shared-element `vault_click` is **not** built (deferred in 07-RESEARCH.md); the lock icon keeps `transitionName="vault_click"` only as a no-op |

Component Inventory section omitted: `Tool: none`.

**Screen map (planner gap list):**

| # | Reference | Ours today | Gap |
|---|---|---|---|
| 32 | VaultLockActivity (`activity_vault_lock`) | `ui/vault/VaultLockScreen.kt` (`TopAppBar`, 96 x 68.5.dp keypad cells, 43.dp slots, 312.dp pattern, `#1A5BD6` path, shake, "Connect at least 3 dots", "Incorrect pattern!", no lockout, no old-password step) | §1 + `VaultLockFlow` |
| 33 | VaultMediaActivity (`activity_vault_media`) | `VaultMediaScreen.kt` (Material `Surface` bar, `AlertDialog` unhide/delete, `"N Selected"`, Material FAB, no UNHIDE action) | §2 |
| 34 | VaultSettingActivity (`activity_vault_setting`) | `VaultSettingsScreen.kt` ("Vault Settings", rounded `Surface` cards, 42.dp tiles, Material `Switch`) | §3 |
| 35 | VaultRecoveryQuestionActivity | `VaultRecoveryQuestionScreen.kt` (3 fixed questions, invented toast) | §4 |
| 36 | VaultForgotPasswordActivity | `VaultForgotPasswordScreen.kt` ("Forgot Password?", literal copy) | §5 |
| 37 | InternalSelectMediaActivity TYPE=2 (`activity_select`) | `VaultAddMediaScreen.kt` (Material `TopAppBar`, dropdown, Material buttons) | §6 |
| — | custom first-time toast | none | §7 |
| — | Add to Private lock gate (6 entry points) | none | §8 |

---

## 1. Lock (`R272/layout/activity_vault_lock.xml`, `VL` — #32, VLT-01)

Window: transparent theme (`Theme.NewGallery.Transparent`), content on `backgroundColor` (#FFFFFF / #0C0C0C). Edge-to-edge insets as Phase 5.

| Element | Value | Reference |
|---|---|---|
| Toolbar | `SharedMediaToolbar` (`layout_toolbar`); title "Private Vault" `private_vault`; **"Change Password" `change_password` while TYPE=1** | `VL` onCreate |
| Lock icon | `ic_lock` at intrinsic size, centred, marginTop 40.sdp; carries `transitionName="vault_click"` (no-op) | `ivLock` |
| Step title | `GalleryText.BodyXLargeMedium`, `black` (#000000 / #FFFFFF), centred, marginTop 15.sdp. PIN: "Set PIN" `set_pin` -> "Re-enter PIN" `re_enter_pin` (setup), "Enter your PIN" `enter_pin` (unlock). Pattern: "Draw Your Pattern" `draw_your_pattern` -> "Re-draw your pattern" `re_draw_your_pattern` (setup and unlock use the first) | `pinTitle`, `VL.o00000O` |
| PIN slots | four boxes in a row, marginTop 20.sdp; each box margin 5.sdp, cell 36.sdp x 30.sdp; digit `pin_style` = Inter SemiBold 14.ssp `titleColor`, centred; underline 1.5.dp `vault_divider_color` (#BFD4E4 / #FFFFFF). The hidden field holds at most 4 digits | `dot_layout`, `pass` `maxLength=4` |
| Keypad | block padding 30.sdp horizontal, 4.sdp top; rows marginTop 10.sdp; each cell 50.sdp x 47.sdp, digit Inter Medium 18.sdp `titleColor`; last row: `ic_backspace` and `ic_confirm`, both tint `black`, padding 7.sdp; last row marginBottom 10.sdp | `lout_num` |
| Forgot link | "Forgot password?" `forgot_password_lock`, `GalleryText.BodyLargeSemiBold` `ic_lock_color` (#0066FF / #FFFFFF), padding 16.sdp h / 10.sdp v, marginTop and bottom 10.sdp. **Visible only when unlocking and a recovery question is saved** | `L6` |
| Switch link | setup only (hidden when unlocking and in TYPE 1 verify): leading `ic_switch_pattern` / `ic_switch_pin`, then text 10.sdp start margin, `GalleryText.BodyLargeSemiBold` `ic_lock_color`, **underlined** (`UNDERLINE_TEXT_FLAG`). Text: "Switch to Pattern" `switch_to_pattern` / "Switch to PIN" `switch_to_pin` | `L7` |
| Pattern block | replaces the PIN block; title marginTop 15.sdp; `PatternView` height 260.sdp, marginTop 33.sdp; colours regular `patternColor` (#1E3354 / #FFFFFF), success `green_color_picker` (#01A368), error `red_color_picker` (#ED0A3F); dot 12.dp, activated dot 28.dp, path 3.dp; stealth mode = **not** `isShowPattern` (default **true**, so the path is drawn) | `PatternView` |

Ours: drop 96 x 68.5.dp keypad cells, 43.dp slots, 312.dp pattern, `#1A5BD6` path colour, 4.dp line, the shake animation, the `TopAppBar`, and every `.sp`/`.dp` literal in `VaultLockScreen.kt`.

### 1.1 Behaviour (state machine `VaultLockFlow`, pure Kotlin, unit-tested)

| Rule | Value | Reference |
|---|---|---|
| Intent | `TYPE` (0 default, 1 change password, 2 change recovery question), `IS_OPEN_VAULT` (default true). Ours: route args | `L1` |
| PIN confirm tap | length != 4: toast "Please enter your pin." `please_enter_pin`. PIN checks run **only on the check-mark**; pattern checks run on finger lift | `L2` |
| PIN setup | first entry stored, slots clear after 30 ms, title -> "Re-enter PIN". Second equal: save, `isPassword=true`, `passwordType=0`, clear `isPrivateLock`. Mismatch: toast "The confirm password does not match!" `password_confirm`, back to step 1 | `L2` |
| Pattern setup | first draw with **fewer than 4 dots**: toast "The pattern must utilise four dots." `pattern_min_4_dots` (exact comparison `< 4` is ASSUMED A1, PENDING-DEVICE); else store, title -> "Re-draw your pattern". Mismatch: toast "The confirm pattern does not match!" `pattern_confirm`, title back to "Draw Your Pattern". Match: save, `passwordType=1`. No minimum when unlocking | `L4` |
| Wrong PIN | toast "Incorrect password!" `wrong_password`, slots clear, `wrongCount++` | `L3` |
| Wrong pattern | **no toast**, path turns `red_color_picker`, `wrongCount++` | `L4` |
| Lockout | `wrongCount == 5`: persist `isPrivateLock=true` and go to Forgot Password (§5). On every open, `isPrivateLock == true` goes straight to Forgot Password. Whether the fingerprint branch also checks it first is ASSUMED A2; ours checks it **before** showing any biometric prompt (the safe reading), PENDING-DEVICE | `L3`, `L5` |
| Fingerprint | when `isFinger`: androidx `BiometricPrompt` on open; title "Private Vault" `private_vault` (ASSUMED A4), subtitle "Unlock your private vault." `unlock_vault`, negative button "Cancel" `cancel`; cancel or error falls back to the PIN/pattern UI. Error wording is the androidx `fingerprint_error_*` English already in `VaultBiometric` | `L11` |
| After success, no recovery question saved | always open Recovery Question (§4) for a result | `L8` |
| After success, question saved | TYPE 0 + `IS_OPEN_VAULT`: replace this screen with the vault (Back from the vault does not return to the lock). TYPE 0 + not open: return `ISPASS=true` to the caller (§8). TYPE 1: after the **old** PIN/pattern verifies, reset to setup ("Set PIN", toolbar title **"Private Vault"**) and return `ISPASS=true` after the new one is saved. TYPE 2: after unlock, open Recovery Question. Exactly where `type` flips from 1 to 0 and when the toolbar title swaps is ASSUMED A3, PENDING-DEVICE | `L8`, `L9` |
| After Forgot Password result OK | clear stored password, `isPassword=false`, `passwordType=0`, back to setup ("Set PIN") | `L10` |
| Side effect | opening the lock sets `isVaultHintToolTip=true` (already in `GalleryNavHost.kt:358`) | `L12` |

---

## 2. Vault media (`R272/layout/activity_vault_media.xml`, `VM` — #33, VLT-01)

| Element | Value | Reference |
|---|---|---|
| Screen | `backgroundColor` | root |
| Toolbar | `SharedMediaToolbar`, title "Private Vault" `private_vault`; end `ic_settings` (settings) when not selecting. Selecting: Close, title "%d item(s) selected" `items_selected`, "Select all" `select_all` / "Unselect all" `unselect_all` (`GalleryText.BodyMediumMedium` `colorPrimary`, padding 5.sdp) | `toolBarLayout` |
| Grid | **3 columns**, padding 2.sdp h / 4.sdp v, date header rows (`item_date`: Today / Yesterday / `dd MMM yyyy`, as Phase 3) spanning the width, cells `item_grid_gallery` (Phase 3/5 cell: square, margin 1.sdp, `ic_check` / `ic_un_check` in selection mode, video duration row, GIF/RAW/SVG label). Tap opens the viewer in vault mode (Phase 5); long-press starts selection | `imageRecycler`, `i0/f` |
| Selection bar | `MediaSelectionBar` with **three slots: Share, Unhide, Delete**. Favorite, Add to and Create album are not shown. New `MediaAction.UNHIDE`: `ic_unhide` + "Unhide" `unhide`, width 63.sdp (A5: the three hidden slots are inferred, low risk) | `layout_select_media_bottom` stub, `VM` 11060-11180 |
| Unhide dialog | `HideMediaDialog` (`dialog_hide` card): title "Unhide items" `unhide_items`, message "Are you sure you want to unhide items?" `unhide_item_permission`, Cancel / Ok | `oa/n` |
| Unhide progress + result | `ProgressTextDialog` "Unhiding" `unhiding`, then toast "Media files unhidden successfully." `selected_media_files_successfully_unhide`; the restored files must appear in Photos at once (re-index MediaStore, D6) | `VM` 13389-13560 |
| Delete dialog | `DeleteMediaDialog` (`dialog_delete`), `showMoveToBin = false` (A6): "Are you sure you want to delete N items?" (`deleted_title` + N + `deleted_items`), No / Yes | `oa/j` |
| Delete progress + result | `ProgressTextDialog` "Deleting" `deleting`, then toast "Media files deleted successfully." `selected_media_files_successfully_deleted` | `VM` |
| Empty (no items) | centred column: text "Our Private Vault feature lets you store your most sensitive photos and videos securely." `hide_photo_private_permission`, `GalleryText.BodyMediumRegular` `graySubTitle` (#605F60), centred, margin h 20.sdp, marginTop 5.sdp, line spacing +3.sdp; below it button "Add Items" `add_item`: `btn_permission` background, `GalleryText.BodyMediumSemiBold` `onlyWhite`, padding 40.sdp h / 11.sdp v, marginTop 20.sdp. Shown only after the first query returns (never before) | `emptyMessageLayout`, `btnAddItem` |
| FAB | shown only when items exist: `ic_add_icon` tinted white on `dark_blue` (#0066FF), margin end 24.sdp, bottom 36.sdp above the bottom stack; content description `submit` | `addVaultButton` |
| Add Items | button and FAB both open the picker (§6) with TYPE=2, no album, multi-select | `VM.o000000o` |
| Loading | full-screen `colorPrimary` spinner layer (elevation 10.sdp, clickable, blocks touches) while a file action runs | `progressBar` |
| First open | if `isPrivateToastShow` is set: clear it and show the §7 toast (long) | `VM` 14990-15110 |
| Back | clears the selection first; else leave the vault. Leaving the vault clears the decrypted view cache (`clearViewCache()`, D6) | `VM` |

Not built (D2): the `hiddenFolderSync` indeterminate bar, the "Hidden Folder" title and the album cards.
Ours: drop the `AlertDialog`s, `"N Selected"`, the Material FAB, the literal empty text and custom button.

---

## 3. Settings (`R272/layout/activity_vault_setting.xml`, `VS` — #34, VLT-01)

| Element | Value | Reference |
|---|---|---|
| Screen | `backgroundColor`, scrolls | `NestedScrollView` |
| Toolbar | `SharedMediaToolbar`, title "Privacy Settings" `privacy_settings` | S1 |
| Row (all four) | flat row (no card, no divider), padding 10.sdp all sides, borderless ripple; leading icon padding 6.sdp at intrinsic size; title `GalleryText.BodyMediumMedium` `titleColor`, marginStart 5.sdp after the icon; subtitle `GalleryText.BodySmallRegular` `titleColor` below it, marginStart 5.sdp; trailing control end-aligned | `RelativeLayout` rows |
| Row 1 | `ic_change_password`, "Change Password" `change_password`, "Set a new passcode" `set_a_new_passcode`, trailing `ic_inside` (padding 10.sdp) | `changePassword` |
| Row 2 | `ic_pattern`, "Show Pattern" `show_pattern`, "Display your unique pattern" `display_your_unique_pattern`, trailing switch (vertical padding 10.sdp). Default **on** | `showPattern` |
| Row 3 | `ic_fingerprint`, "Unlock with Fingerprint" `unlock_with_fingerprint`, "Use biometrics for security" `use_biometrics_for_security`, trailing switch. Default off (A9) | `fingerLay` |
| Row 4 | `ic_change_recovery_question`, "Change Recovery Question" `change_recovery_question`, "Choose another recovery question" `choose_another_recovery_question`, trailing `ic_inside` | `recoveryQuestion` |
| Switch | `SwitchCompat` look (Material 3 `Switch` as Phase 6 Keep One Photo: thumb `colorPrimary` when on) | `SwitchCompat` |

| Behaviour | Value | Reference |
|---|---|---|
| Change Password | open Lock TYPE=1, `IS_OPEN_VAULT=false`; on OK toast "Password changed successfully." `password_changed_successfully` | S4, S5 |
| Change Recovery Question | open Lock TYPE=2, `IS_OPEN_VAULT=false` (unlock first); on OK toast "Recovery question changed successfully." `recovery_question_changed_successfully`; stay on Settings (ours today navigates to the vault: remove) | S4, S5 |
| Show Pattern switch | writes the show-pattern preference; **default true** (ours defaults false: fix; existing untouched installs flip, accepted, no release exists) | S3 |
| Fingerprint switch | turning on checks `BiometricManager.canAuthenticate`; if unusable, the switch reverts and the matching error shows ("No fingerprints enrolled." / "This device does not have a fingerprint sensor" / "Fingerprint hardware not available.", androidx `fingerprint_error_*`) | S6 |

---

## 4. Recovery question (`R272/layout/activity_vault_recovery_question.xml`, `item_question.xml`, `VR` — #35, VLT-01)

| Element | Value | Reference |
|---|---|---|
| Toolbar | `SharedMediaToolbar`, title "Set Security Questions" `set_security_questions_title` | R1 |
| Heading | "Set Security Question!" `set_security_question`, `GalleryText.BodyXXLargeSemiBold` `black`, centred, marginTop 30.sdp | |
| Sub | "The security question lets you reset your password if you forgot in future" `set_security_question_subtitle`, `GalleryText.BodyLargeRegular` `security_question_bottom_color` (#1E3354 / #FFFFFF), centred, margin h 20.sdp, marginTop 4.sdp | |
| Steps | row marginTop 30.sdp, centred: circles 36.sdp x 36.sdp with digits 1, 2, 3 (`GalleryText.BodyXLargeMedium`), `ic_recover_arrow` between them (margin h 5.sdp). Current/done step: background `ic_recover_select`, digit `onlyWhite`; pending: `ic_recover_unselect`, digit `titleColor` | `recoverCount1..3` |
| Prompt | "Answer at least 3 Question" `answer_at_least_Question`, `GalleryText.BodyXLargeMedium` `titleColor`, centred, marginTop 30.sdp | |
| Question selector | frame background `ic_recover_spinner_bg`, margin h 16.sdp, marginTop 20.sdp; closed row and popup rows use `item_question` (`GalleryText.BodySmallMedium` `titleColor`, padding 15.sdp h / 10.sdp v); popup background `ic_recover_spinner_bg`. **Replaces** Material `DropdownMenu` with a popup that visually matches | `questionSpinner`, `SpinnerItemStyle` |
| Answer field | background `ic_recover_answer_bg`, padding 15.sdp h / 10.sdp v, margin h 16.sdp, marginTop 10.sdp; hint "Your answer" `your_answer` `hintLanguageColor` (#501D242E / #50FFFFFF); text `GalleryText.BodyMediumRegular` `black`; one line | `securityAnswer` |
| Button | `btn_permission` background, full width margin h 16.sdp, marginTop 30.sdp, padding v 10.sdp, `GalleryText.BodyLargeSemiBold` `onlyWhite`, centred; label "Next" `next` on steps 1-2 and "Submit" `submit` on step 3 (A8, PENDING-DEVICE) | `nextBtn` |

| Behaviour | Value | Reference |
|---|---|---|
| Question source | 7 questions: `security_question1`..`7` ("What was your childhood nickname?", "What is your pet name?", "Who is your favorite teacher?", "Who is your favorite actor?", "Who is your favorite actress?", "Who is your favorite cricketer?", "Who is your favorite footballer?"). Free choice, not fixed | R2 |
| Next | empty (trimmed) answer: toast "Please enter an answer!" `please_enter_answer`. Else store `{id, question, answer}`, **remove the chosen question from the list**, clear the field, advance the step. Ours hashes the stored answer (kept) | R2 |
| After step 3 | `isQuestion=true`, hide the keyboard, return OK to the caller | R2 |
| Back | returns without saving (Lock/Settings treat as cancelled) | |

Ours: drop the three fixed questions and the invented "Please select different questions" toast.

---

## 5. Forgot password (`R272/layout/activity_vault_forgot_password.xml`, `VF` — #36, VLT-01)

| Element | Value | Reference |
|---|---|---|
| Toolbar | `SharedMediaToolbar`, title "Forgot Password" `forgot_password` (ours text is "Forgot Password?": fix the English) | F1 |
| Title | "Password Recovery Option" `password_recovery_option`, `GalleryText.BodyXXLargeSemiBold` `black`, centred, marginTop 40.sdp | |
| Sub | "Please answer these questions to reset your password." `password_recovery_questions`, `GalleryText.BodyLargeRegular` `titleColor`, centred, margin h 40.sdp, marginTop 8.sdp | |
| Selector | same as §4, margin h 16.sdp, marginTop 60.sdp; lists the three stored questions (localised by stored id 1..10, `security_question1`..`10`) | `questionSpinner` |
| Answer field | same as §4, marginTop 10.sdp | `securityAnswer` |
| Button | "Confirm" `confirm`, `btn_permission`, `GalleryText.BodyLargeSemiBold` `onlyWhite`, padding 95.sdp h / 10.sdp v, centred, marginTop 30.sdp, marginBottom 5.sdp | `confirmBtn` |

| Behaviour | Value | Reference |
|---|---|---|
| Empty answer | toast "Please write an answer" `please_write_an_answer` | F2 |
| Wrong answer | toast "Enter the correct security answer." `enter_correct_security_answer` | F2 |
| Right answer | return OK (Lock clears the password and returns to setup, §1.1). **Deliberate deviation D3:** trimmed, case-insensitive hashed comparison (2.7.2 is exact, case-sensitive, untrimmed) | F2, D3 |
| Parse failure | toast "An issue occurred!" `something_went_wrong` | F2 |
| Back | returns to the Lock screen, not to the vault (verify on device, 07-RESEARCH.md Pitfall 7). No extra escape is invented | |

---

## 6. Add Items picker (`R272/layout/activity_select.xml`, `item_picker_folder.xml`, `item_selected.xml`, `IS` TYPE=2 — #37, VLT-02)
> **Superseded in part by 07-07-PLAN.md (decoded 2.7.2 smali, 2026-11-24):** the grid is 4 columns (`GridLayoutManager(ctx, 4)`), cells use `item_grid_gallery`, the folder row is always shown and the "Choose media" title is hidden; only the selected tray and the Add button toggle at count > 0. Where this section disagrees, 07-07 wins. Device confirmation pending (P4).


Replaces `VaultAddMediaScreen`'s Material chrome; keep its data flow. This is **not** `activity_picker_screen` (D1).

| Element | Value | Reference |
|---|---|---|
| Screen | `backgroundColor` | `mainLayout` |
| Toolbar | `SharedMediaToolbar`, title "Choose media" `select_items`, Back | `toolBarLayout`, P3 |
| Folder row | `ic_back`-free row (its own back is redundant with the toolbar): height `actionBarSize` (56.dp), padding 4.sdp, `backgroundColor`; folder name (default "All Photos" `all_photos`) `GalleryText.BodyLargeSemiBold` at **14.ssp** `titleColor`, 1 line ellipsize, marginStart 9.sdp, marginEnd 10.sdp, weight 1; while folders load a 20.sdp `colorPrimary` spinner, then arrow `ic_title_arrow_up` (40.sdp x 30.sdp, padding h 15.sdp, `titleColor`; rotates to `ic_title_arrow_down` while the folder list is open). Add button at the end: "Add" `internal_select_add`, `bg_rounded_red` tinted `colorPrimary`, `GalleryText.BodyLargeSemiBold` at 10.ssp `onlyWhite`, padding 19.sdp h / 6.sdp v, margin h 12.sdp; **shown only while >= 1 item is selected**. **PENDING-DEVICE:** smali `IS.o000OOo` shows this row and the Add button when the selection count is above 0 and hides both at 0; the reference gives no other folder entry point in the layout, so ours shows the row from the moment folders load (folder switching stays reachable) and the Add button only with a selection | `toolbar`, `toolbarTitle`, `btnNext`, `loaderFolders`, `ivFolderNameArrow` |
| Divider | 1.sdp `toolbar_bottom_border` (#C7D2E3 / #2A2828) under the row | |
| Media grid | padding 2.sdp h / 5.sdp top / 20.sdp bottom, `clipToPadding=false`, marginTop 2.sdp; cell = Phase 3/5 selectable cell (`ic_check` / `ic_un_check`, video duration row). Column count and the exact cell (adapter `qa/l`) not isolated: keep our existing 3 columns, **PENDING-DEVICE** (P4) | `selectRecycler` |
| Folder list | replaces the grid while open; background `imagePickerFolderBGColor` (#EDEDED / #212121), padding 2.sdp h / 20.sdp bottom. Row `item_picker_folder`: padding 12.sdp h, marginTop 10.sdp; cover 60.sdp x 60.sdp radius 8.sdp centre-crop (placeholder `image_loading_albums`); name `GalleryText.BodyLargeSemiBold` `titleColor`, 1 line, marginTop 6.sdp, marginStart 7.sdp; count at 14.ssp `GalleryText.BodyXLargeMedium` `picker_folder_subtitle_color` (#505050), width 100.sdp, marginTop 2.sdp; trailing `ad_ic_right_arrow` 35.sdp, padding 8.sdp. Tap selects the folder and closes the list | `folderRecycler`, `item_picker_folder` |
| Selection tray | bottom, shown while >= 1 selected: background `imagePickerSelectionsBGColor` (#FFFFFF / #1E1E1E), padding v 5.sdp; header row 40.sdp tall: `ic_close` (36.sdp, padding 6.sdp, marginStart 5.sdp, tint `black`) clears the selection, count "%d item(s) selected" `items_selected` (`GalleryText.BodyLargeSemiBold` 13.ssp `titleColor`, marginStart 9.sdp, weight 1), `ic_arrow_up` (36.sdp, padding 6.sdp, marginEnd 5.sdp) expands/collapses the thumbnail strip. Strip (hidden by default): horizontal list margin 7.sdp, each thumb `item_selected` 60.sdp x 60.sdp, inner 55.sdp x 55.sdp radius 7.sdp centre-crop, remove badge `bg_circle` 16.sdp tinted `black` with `ic_close` tinted `white` (padding 2.sdp, elevation 2.sdp) at the top-end | `llSelectedMediaView`, `reNext`, `rvSelectedMedia` |
| Empty | centred: `ic_empty_img`; "No items" `no_items` `GalleryText.BodyLargeSemiBold` `titleColor` marginTop 20.sdp / bottom 10.sdp; "No media files here." `no_media_file` `GalleryText.BodySmallRegular` `titleColor` centred, margin h 50.sdp. Never shown before the first query returns | `emptyLayout` |
| Loading | full-screen `colorPrimary` spinner layer (elevation 10.sdp) while the list loads and while hiding runs | `progressBar` |

| Behaviour | Value | Reference |
|---|---|---|
| Add tap | with 0 selected: toast "Please select media." `please_select_items`; else hide the selected files (encrypt, then remove the source; D6 order), progress, toast "Media files hidden successfully." `selected_media_files_successfully_hide`, return to the vault | P3 |
| Selection | cell tap toggles; the title in the toolbar stays "Choose media"; the count lives in the tray | P3 |
| Back | closes the folder list first, then leaves the picker without hiding | |

---

## 7. First-time secured toast (`R272/layout/custom_toast_layout.xml`)

Shown once, long, over the vault, when `isPrivateToastShow` is set after the first lock + recovery setup, then cleared.

| Element | Value |
|---|---|
| Container | horizontal, centred, background `ic_toast_bg`, padding 8.sdp, width match parent (inside the toast window) |
| Icon | `ic_secure` at intrinsic size, marginEnd 5.sdp |
| Text | "Your photo is secured with us" `toast_private`, `GalleryText.BodyMediumMedium` `onlyWhite` |

Position and gravity (system toast default vs centred) are not in the layout: PENDING-DEVICE; default to the Compose toast host's bottom position used elsewhere in the app.

---

## 8. Add to Private lock gate (interaction contract, D5)

Every Add to Private entry point must check `isVaultLockSet()` (PIN or pattern stored, including legacy plain values) before showing the Hide dialog:

| State | Behaviour |
|---|---|
| Lock exists | show `HideMediaDialog` ("Hide" copy from Phase 3), then hide |
| No lock | open Lock with TYPE=0, `IS_OPEN_VAULT=false` (setup, then Recovery Question), on RESULT_OK show `HideMediaDialog`, then hide. Back from setup cancels; nothing is hidden |

Entry points (`MediaListActions.kt:147`, `HomeViewModel.kt:356`, `MediaViewerScreen.kt:535`): Home selection, media list, Favorites, Animated, Search colour, viewer. After a hide on any of them, re-index MediaStore (D6). Navigation result key `vault_ready` via `savedStateHandle` (pattern not yet exercised in this repo, A10).

---

## Spacing Scale

Phase 1 scale applies (sdp units: 2, 4, 8, 12, 16, 24, 32, 48). Values used here: 2.sdp (list padding), 4.sdp (grid v padding, folder row padding, sub margin), 8.sdp (forgot sub margin, toast padding), 16.sdp (spinner / field / button margins, forgot link padding), 24.sdp (FAB end margin), 32/48 not used.

Exceptions (parity overrides multiples of 4, per Phase 1): 1, 3, 5, 6, 7, 9, 10, 11, 15, 19, 20, 30, 33, 35, 36, 40, 47, 50, 60, 63, 95, 260 sdp; unscaled 1.5.dp (PIN underline), 3.dp (pattern line), 12.dp (pattern dot), 28.dp (activated dot), 56.dp (`actionBarSize`). Never round them.

---

## Typography

All Inter, `includeFontPadding = false`, natural line height (Phase 1 rule).

| Role | Size | Weight | Line Height | Where |
|------|------|--------|-------------|-------|
| Body | 11ssp | 400 Regular | natural (~1.21) | empty copy, answer field (`BodyMediumRegular`); 10ssp/400 settings subtitles (`BodySmallRegular`) |
| Label | 11ssp | 500 Medium | natural (~1.21) | settings titles, select all, toast (`BodyMediumMedium`); 10ssp/500 question rows (`BodySmallMedium`) |
| Heading | 13ssp | 600 SemiBold | natural (~1.21) | forgot link, switch link, buttons, picker folder names, tray count (`BodyLargeSemiBold`) |
| Display | 16ssp | 600 SemiBold | natural (~1.21) | screen headings (`BodyXXLargeSemiBold`) |

Exception (parity, as Phases 1, 3-6): 11ssp/600 (`BodyMediumSemiBold` Add Items), 13ssp/400 (`BodyLargeRegular` subtitles), 14ssp/500 (`BodyXLargeMedium` step title, step circles, folder count), 14ssp/600 (`pin_style` PIN digit, folder-row title), 10ssp/600 (Add button), 18.sdp/500 (keypad digit, sdp in the reference). Weights in use: 400, 500, 600. The section tables are binding.

---

## Color

Phase 1 roles, applied here:

| Role | Value | Usage |
|------|-------|-------|
| Dominant (60%) | `backgroundColor` #FFFFFF / #0C0C0C | every vault screen |
| Secondary (30%) | `imagePickerFolderBGColor` #EDEDED / #212121; `imagePickerSelectionsBGColor` #FFFFFF / #1E1E1E; `vault_divider_color` #BFD4E4 / #FFFFFF; `toolbar_bottom_border` #C7D2E3 / #2A2828; `ic_toast_bg` | folder list, selection tray, PIN underline, picker divider, toast |
| Accent (10%) | `colorPrimary` / `dark_blue` / `ic_lock_color` #0066FF (white at night) | reserved list below |
| Destructive | `red_color_picker` #ED0A3F | wrong-pattern path only (no destructive button in this phase; Delete uses `DeleteMediaDialog`) |

Accent reserved for: the vault FAB (`dark_blue`), the Add button and Add Items button (`btn_permission`/`colorPrimary`), "Forgot password?" and the switch link (`ic_lock_color`), "Select all"/"Unselect all", loading spinners, the settings switches when on, the picker folder-row spinner.
Not for titles, icons, row text or the PIN digits.

Fixed non-theme colours from the reference: `patternColor` #1E3354 / #FFFFFF (pattern regular), `green_color_picker` #01A368 (pattern success), `security_question_bottom_color` #1E3354 / #FFFFFF, `hintLanguageColor` #501D242E / #50FFFFFF, `graySubTitle` #605F60, `picker_folder_subtitle_color` #505050, `black` #000000 / #FFFFFF (headings, PIN title), `onlyWhite` #FFFFFF.

**Colours to add** (missing in `values/colors.xml` / `values-night`; run `python -I scripts/ref_res.py check-colors`): `red_color_picker`, `security_question_bottom_color`, `toolbar_bottom_border`, `imagePickerFolderBGColor`, `imagePickerSelectionsBGColor`, `picker_folder_subtitle_color` (verify each is absent first; research confirmed the first three). Already present: `vault_divider_color`, `ic_lock_color`, `patternColor`, `green_color_picker`, `hintLanguageColor`, `graySubTitle`, `dark_blue`, `black`.

**Drawables to add** (copy from `R272/drawable/`, only if absent): required `ic_secure`, `ic_toast_bg`, `ic_recover_spinner_bg`, `ic_recover_answer_bg`, `ic_arrow_up`; picker `ad_ic_right_arrow`, `bg_circle`, `ic_title_arrow_up`, `ic_title_arrow_down`, `image_loading_albums`; check `ic_unhide`, `ic_add_icon`, `ic_switch_pattern`, `ic_switch_pin`, `ic_recover_select`, `ic_recover_unselect`, `ic_recover_arrow`, `ic_change_password`, `ic_pattern`, `ic_fingerprint`, `ic_change_recovery_question`, `ic_inside`, `ic_lock`, `ic_backspace`, `ic_confirm`, `ic_settings`, `btn_permission`, `bg_rounded_red`, `ic_empty_img`, `ic_check`, `ic_un_check`, `ic_close`. Not needed here: `ic_rotation_arrow_mark`, `ic_gallery_collage`, `bg_corner` (collage picker, Phase 8).

---

## Copywriting Contract

Every visible string is a reference key; translations come from the Phase 1 sync (`python -I scripts/ref_res.py sync-strings`, then `check-strings`). Edit the four existing keys and run the sync in the same step (07-RESEARCH.md Pitfall 8).
No hard-coded English remains in `ui/vault` (every literal in the Phase 7 files, plus `VaultBiometric` title/subtitle, the `"N Selected"` and `"Vault Settings"` strings).

| Element | Copy (reference key) |
|---------|------|
| Primary CTA | "Add Items" (`add_item`, vault empty state); "Add" (`internal_select_add`, picker); "Next" / "Submit" (`next` / `submit`, recovery question); "Confirm" (`confirm`, forgot password) |
| Empty state heading | vault: "Our Private Vault feature lets you store your most sensitive photos and videos securely." (`hide_photo_private_permission`); picker: "No items" (`no_items`) |
| Empty state body | picker: "No media files here." (`no_media_file`) |
| Error state | "Please enter your pin." `please_enter_pin`; "The confirm password does not match!" `password_confirm`; "The confirm pattern does not match!" `pattern_confirm`; "The pattern must utilise four dots." `pattern_min_4_dots`; "Incorrect password!" `wrong_password` (PIN only; wrong pattern shows no toast); "Please enter an answer!" `please_enter_answer`; "Please write an answer" `please_write_an_answer`; "Enter the correct security answer." `enter_correct_security_answer`; "An issue occurred!" `something_went_wrong`; "Please select media." `please_select_items`; fingerprint errors from androidx `fingerprint_error_*` |
| Destructive confirmation | Delete from the vault: `dialog_delete` — "Are you sure you want to delete N items?" (`deleted_title` + N + `deleted_items`), No / Yes (no Recycle Bin checkbox). Unhide: "Unhide items" / "Are you sure you want to unhide items?" (`unhide_items` / `unhide_item_permission`), Cancel / Ok. Lock-out and password reset have no confirmation dialog (reference) |

Titles and labels: "Private Vault" `private_vault`; "Change Password" `change_password`; "Set PIN" `set_pin`; "Re-enter PIN" `re_enter_pin`; "Enter your PIN" `enter_pin`; "Draw Your Pattern" `draw_your_pattern`; "Re-draw your pattern" `re_draw_your_pattern`; "Switch to Pattern" `switch_to_pattern`; "Switch to PIN" `switch_to_pin`; "Forgot password?" `forgot_password_lock`; "Forgot Password" `forgot_password`; "Privacy Settings" `privacy_settings`; "Choose media" `select_items`; "%d item(s) selected" `items_selected`; "Set Security Questions" `set_security_questions_title`; "Password Recovery Option" `password_recovery_option`; "Unlock your private vault." `unlock_vault`; "Cancel" `cancel`.

Success / progress copy: "Unhiding" `unhiding`; "Deleting" `deleting`; "Media files unhidden successfully." `selected_media_files_successfully_unhide`; "Media files deleted successfully." `selected_media_files_successfully_deleted`; "Media files hidden successfully." `selected_media_files_successfully_hide`; "Password changed successfully." `password_changed_successfully`; "Recovery question changed successfully." `recovery_question_changed_successfully`; "Your photo is secured with us" `toast_private`.

**Keys to import** (missing in `values/strings.xml`; copy from `R272/values/strings.xml`, then sync translations; grep each before adding): `set_pin`, `re_enter_pin`, `draw_your_pattern`, `re_draw_your_pattern`, `forgot_password_lock`, `hide_photo_private_permission`, `add_item`, `submit`, `change_password`, `set_a_new_passcode`, `show_pattern`, `display_your_unique_pattern`, `unlock_with_fingerprint`, `use_biometrics_for_security`, `change_recovery_question`, `choose_another_recovery_question`, `set_security_question`, `set_security_question_subtitle`, `set_security_questions_title`, `answer_at_least_Question`, `your_answer`, `next`, `password_recovery_option`, `password_recovery_questions`, `confirm`, `privacy_settings`, `password_confirm`, `pattern_confirm`, `pattern_min_4_dots`, `wrong_password`, `please_enter_pin`, `password_changed_successfully`, `recovery_question_changed_successfully`, `enter_correct_security_answer`, `please_enter_answer`, `please_write_an_answer`, `toast_private`, `unlock_vault`, `unhiding`, `selected_media_files_successfully_unhide`, `select_items`, `internal_select_add`, `security_question1`..`10`, the array `security_question` (7 items, `arrays.xml`), the three `fingerprint_error_*`.
**Fix the English of existing keys:** `enter_pin` -> "Enter your PIN", `switch_to_pattern` -> "Switch to Pattern", `switch_to_pin` -> "Switch to PIN", `forgot_password` -> "Forgot Password".
Known deviations recorded for the checklist: no Hidden Folder section (D2), trimmed case-insensitive hashed answer match (D3), hashed PIN storage (not user-visible), Home lock-icon shared-element transition not built.

---

## UI Considerations

Applicable state considerations resolved: 18 covered, 9 backstop (PENDING-DEVICE), 3 unresolved (planner traces)

| Category | Element(s) | Status | Resolution / Reason |
|----------|------------|--------|---------------------|
| populated | lock PIN | ✅ covered | Lock shows the lock icon, "Enter your PIN", four underlined digit slots, a 3 x 4 keypad with backspace and check, and "Forgot password?" only if a recovery question exists |
| populated | lock pattern | ✅ covered | Pattern view 260 sdp tall with the path shown unless Show Pattern is off; success flashes green, error red |
| interaction | PIN setup | ✅ covered | "Set PIN" then "Re-enter PIN"; mismatch toast and restart; fewer than 4 digits on the check-mark gives "Please enter your pin." |
| interaction | pattern setup | ✅ covered | Fewer than four dots gives the four-dots toast; mismatch gives the pattern toast and returns to "Draw Your Pattern" |
| error | wrong entry | ✅ covered | Wrong PIN toasts "Incorrect password!"; wrong pattern shows only the red path; no shake in either |
| behaviour | lockout | ✅ covered | The fifth wrong attempt stores the lock-out flag and opens Forgot Password; every later open goes there until a right answer resets the lock |
| behaviour | change password | ✅ covered | Settings > Change Password verifies the old PIN or pattern with the toolbar "Change Password", then shows "Set PIN"; Settings toasts on success |
| behaviour | add to private, no lock | ✅ covered | The first Add to Private from any of the six screens opens setup and the recovery question, then the Hide dialog; Back cancels without hiding |
| populated | vault grid | ✅ covered | Three columns under Today / Yesterday / date headers, FAB bottom-right; selection bar shows Share, Unhide, Delete |
| empty | vault | ✅ covered | An empty vault shows the secure-storage sentence and an "Add Items" button, no FAB, never before the first query returns |
| selection | vault | ✅ covered | Toolbar reads "N item(s) selected" with Select all / Unselect all; Unhide and Delete ask first, then show a progress dialog and a success toast |
| populated | settings | ✅ covered | Four flat rows with icon, title and subtitle; Show Pattern is on and Fingerprint is off on a fresh install |
| error | fingerprint unusable | ✅ covered | Turning Unlock with Fingerprint on with no enrolled fingerprint reverts the switch and shows the androidx message |
| populated | recovery question | ✅ covered | Three numbered steps, a question selector that drops each used question, an answer field, Next / Submit; an empty answer gives "Please enter an answer!" |
| populated | forgot password | ✅ covered | Selector over the three stored questions, answer field and Confirm; empty gives "Please write an answer", wrong gives "Enter the correct security answer." |
| populated | picker | ✅ covered | "Choose media" toolbar, folder row with "All Photos" and arrow, grid with check circles, selection tray with count and thumbnails, Add button once something is selected |
| empty | picker | ✅ covered | "No items" and "No media files here." under `ic_empty_img` |
| visual | first-time toast | ✅ covered | The first vault open after setup shows a one-time toast with a shield icon and "Your photo is secured with us" |
| format | assumption A1 | 🧪 backstop | Pattern setup minimum is `< 4` dots: confirm on device |
| behaviour | fingerprint vs lockout (A2) | 🧪 backstop | Confirm on device that fingerprint cannot bypass the lock-out after five wrong attempts |
| behaviour | Change Password type flip (A3) | 🧪 backstop | Confirm the toolbar title swap and when the new-password step starts |
| visual | biometric prompt title (A4) | 🧪 backstop | Confirm the title text against 2.7.2 |
| visual | recovery button label (A8) | 🧪 backstop | Confirm "Next" then "Submit" on step 3 |
| behaviour | picker row visibility (P4) | 🧪 backstop | Confirm when the folder row and Add button show, the grid column count, and the cell layout against 2.7.2 |
| visual | toast gravity | 🧪 backstop | Position of the secured toast on device |
| media | thumbnails | 🧪 backstop | Decrypt speed on a large vault; unhide visible in Photos at once on MIUI |
| locale / rtl / dark | all screens | 🧪 backstop | Long German, Russian and Tamil strings on the buttons (Confirm at 95.sdp h padding, switch link, Add Items); RTL mirrors rows, not the PIN keypad order; light and dark side-by-side with 2.7.2 |
| behaviour | where `type` flips (L8) | ⚠ unresolved | Planner traces `VL.o00000OO` type-1 branch and the PIN/pattern submit methods before building the TYPE=1 transition |
| behaviour | picker details (P4) | ⚠ unresolved | Planner reads `IS.onCreate` and adapter `qa/l`, or a device capture, for column count and cell layout |
| behaviour | Back from Forgot | ⚠ unresolved | Verify on device that Back returns to the lock, not the vault |

---

## Registry Safety

| Registry | Blocks Used | Safety Gate |
|----------|-------------|-------------|
| none (Android, no shadcn) | – | not applicable |

Asset provenance gate (Phase 1 rule): copy only from `R272/res`; no ad-SDK resources; brand-art decision of 2026-11-13 stands. `ic_secure` and `ic_toast_bg` are generic security art, not brand art.
Device rule: no device this phase; nothing is marked verified, and every PENDING-DEVICE row stays open until a phone confirms it.

---

## Open questions (autonomous, resolved by best judgment; revisit on device)

| # | Question | Resolution |
|---|---|---|
| Q1 | Picker layout named by VLT-02 | Resolved by CONTEXT D1: `activity_select` |
| Q2 | Hidden Folder section | Resolved by CONTEXT D2: not built, recorded deviation |
| Q3 | Answer matching | Resolved by CONTEXT D3: keep ours |
| Q4 | Shake on wrong entry | Resolved by CONTEXT D4: removed |
| Q5 | Folder row visibility in the picker | Interim: shown from folder load, Add only with selection (PENDING-DEVICE) |
| Q6 | Pattern minimum, fingerprint-lockout order, type flip, biometric title, recovery button label, toast gravity | Interim values in §1.1, §4, §7 (A1-A4, A8) PENDING-DEVICE |

---

## Re-runnable provenance (2026-11-24)

- Layouts: `R272/layout/activity_vault_lock.xml`, `activity_vault_media.xml`, `activity_vault_setting.xml`, `activity_vault_recovery_question.xml`, `activity_vault_forgot_password.xml`, `activity_select.xml`, `item_question.xml`, `item_picker_folder.xml`, `item_selected.xml`, `custom_toast_layout.xml`, `dialog_hide.xml`, `dialog_delete.xml`, `layout_select_media_bottom.xml`. Compact view: strip `xmlns` and collapse whitespace with `sed`.
- Strings and arrays: `R272/values/strings.xml`, `R272/values/arrays.xml` (`security_question`), colours `R272/values/colors.xml` + `values-night/colors.xml`.
- Picker visibility: `IS.o000OOo` (line 2713, selection-changed handler using `items_selected` 0x7f130775) sets binding fields `SJowARcXwM` (LinearLayoutCompat, the folder-row `toolbar`) and `HISPj7KHQ7` (AppCompatTextView, `btnNext`) visible at count > 0 (lines 2922-2939) and gone at 0 (lines 4452-4470); `IS.onCreate` (line 4942) toggles the selection tray `OooO0o`.
- Behaviour rules L/M/S/R/F/P: see 07-RESEARCH.md "Reference behaviour contract" for smali line ranges.
- Missing keys/assets in ours: `grep -q 'name="<key>"' app/src/main/res/values/strings.xml`; `ls app/src/main/res/drawable*/<name>.*`; `python -I scripts/ref_res.py check-colors`.

---

## Checker Sign-Off

- [ ] Dimension 1 Copywriting: pending
- [ ] Dimension 2 Visuals: pending
- [ ] Dimension 3 Color: pending
- [ ] Dimension 4 Typography: pending
- [ ] Dimension 5 Spacing: pending
- [ ] Dimension 6 Registry Safety: pending
- [ ] Dimension 7 Inventory Provenance: pending

**Approval:** pending (gsd-ui-checker)
