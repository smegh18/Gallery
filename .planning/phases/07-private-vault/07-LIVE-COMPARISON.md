# Phase 7 Live Comparison Matrix

Date prepared: 2026-11-24 (IST)

This matrix is a test plan and evidence ledger. Automated gates passed; every device entry is **PENDING-DEVICE** (D7): no adb device is attached and the Artemis MCP server failed to connect (`CONNECT_TIMEOUT`) when this was written. Nothing here is marked verified. No Phase 7 Ported or Verified checklist box has been changed.

## Shared gate evidence

Full detail in `07-GATE-FINDINGS.md`.

- Full unit suite: PASS, 54 suites, 342 tests, 0 failures/errors/skipped (`./gradlew.bat :app:testDebugUnitTest -q`).
- Debug assembly: PASS (`./gradlew.bat :app:assembleDebug -q`).
- Strings: PASS, 24 languages current (`python -I scripts/ref_res.py check-strings`).
- Colours: PASS, 226 light / 134 night names match 2.7.2 (`python -I scripts/ref_res.py check-colors`).
- Diff check: PASS (`git diff --check`).
- CodeRabbit: NOT RUN (CLI not installed); manual review found no defect. Still owed.
- Device: `adb devices` lists none. No fixture root recorded.

## Row matrix (checklist rows #32-#37)

| Row | Route / reference | States to compare against 2.7.2 | Automated evidence | Device result |
|---|---|---|---|---|
| #32 VaultLockActivity | Explore > Vault; `VaultLock`, `VaultLockFor(type, openVault)` | PIN and pattern unlock; set PIN, re-enter; pattern set, redraw; Show Pattern off; wrong entry; fifth wrong attempt; fingerprint; change mode; light and dark; RTL | VaultLockFlowTest, VaultGateTest, VaultTest; build; resources PASS | PENDING-DEVICE |
| #33 VaultMediaActivity | `VaultMedia` | empty (no FAB), populated three columns under date headers, FAB, selection (Share, Unhide, Delete), unhide and delete dialogs with progress and toast, secured toast, Share sends a decrypted image, decrypted cache cleared on leaving | VaultFilesTest, SharedMediaListTest, VaultTest; build | PENDING-DEVICE |
| #34 VaultSettingActivity | vault settings icon > Privacy Settings | four flat rows; Change Password; Change Recovery Question; Show Pattern on by default; fingerprint switch with and without enrolled finger or sensor | VaultSettingTest; build | PENDING-DEVICE |
| #35 VaultRecoveryQuestionActivity | after first lock setup, or Settings > Change Recovery Question | three numbered steps, selector drops used questions, empty answer toast, Next then Submit, system Back wipes a half-made lock | RecoveryQuestionFlowTest; build | PENDING-DEVICE |
| #36 VaultForgotPasswordActivity | fifth wrong attempt or "Forgot password?" | selector over the three stored questions, empty / wrong / right answer, Back after lockout versus Back after the link, right answer then Set PIN | RecoveryQuestionFlowTest, VaultLockFlowTest, VaultTest; build | PENDING-DEVICE |
| #37 InternalSelectMediaActivity (`activity_select`) | vault > Add Items / FAB | folder row ("All Photos", arrow), folder list, four-column grid, check circles, selection tray with count and thumbnails, Add after a selection, empty folder, loader after a folder switch | VaultPickerStateTest; build | PENDING-DEVICE |

## UI-SPEC "UI Considerations" state matrix

| State | What to do on the device | Result |
|---|---|---|
| Lock PIN populated | lock icon, "Enter your PIN", four underlined slots, 3 x 4 keypad with backspace and check, "Forgot password?" only with a recovery question | PENDING-DEVICE |
| Lock pattern populated | pattern view 260 sdp, path hidden when Show Pattern off, green on success, red on error | PENDING-DEVICE |
| PIN setup | "Set PIN", "Re-enter PIN", mismatch toast and restart, fewer than four digits on the check gives "Please enter your pin." | PENDING-DEVICE |
| Pattern setup | fewer than four dots gives the four-dots toast; mismatch toast and back to "Draw Your Pattern" | PENDING-DEVICE |
| Wrong entry | wrong PIN toasts "Incorrect password!"; wrong pattern shows only the red path; no shake | PENDING-DEVICE |
| Lockout across process death | five wrong PINs open Forgot; kill the app (`adb shell am force-stop com.quizango.gallery`) and reopen the vault: still Forgot; fingerprint does not open it | PENDING-DEVICE |
| Change password | Settings > Change Password: verify old secret under toolbar "Change Password", then "Set PIN"; toast on return; stays on Settings | PENDING-DEVICE |
| Add to private, no lock, six entry points | after Clear data: Photos, an album, Favorites, Animated, Search by colour and the viewer each walk lock setup, recovery question, then the Hide dialog; Back during setup (lock screen and question screen, button and system Back) hides nothing | PENDING-DEVICE |
| Vault grid | three columns, Today / Yesterday / date headers, FAB bottom-right | PENDING-DEVICE |
| Vault empty | secure-storage sentence and "Add Items" button, no FAB, nothing shown before the first query returns | PENDING-DEVICE |
| Vault selection | "N item(s) selected", Select all / Unselect all, Share / Unhide / Delete; confirmations then progress then toast | PENDING-DEVICE |
| Settings | four rows, Show Pattern on and Fingerprint off on a fresh install; subtitles | PENDING-DEVICE |
| Fingerprint unusable | switch on with no enrolled finger or no sensor reverts and shows the androidx message | PENDING-DEVICE |
| Recovery question | three steps, question selector, answer field, soft keyboard | PENDING-DEVICE |
| Forgot password | selector, answer field, Confirm; "Please write an answer", "Enter the correct security answer." | PENDING-DEVICE |
| Forgot, Back from both cases | Back after the lockout closes the lock; Back after the link returns to the lock (the "unresolved" row in UI-SPEC) | PENDING-DEVICE |
| Picker populated | "Choose media" toolbar, folder row, four-column grid, check circles, tray, Add | PENDING-DEVICE |
| Picker empty | "No items" and "No media files here." under `ic_empty_img` | PENDING-DEVICE |
| First-time toast | first vault open after setup shows "Your photo is secured with us" once; reopening does not | PENDING-DEVICE |

## Backstops (assumptions and device-only checks)

| Backstop | Check | Result |
|---|---|---|
| A1 pattern minimum (smali-confirmed `< 4`) | draw three dots on setup, draw four dots | PENDING-DEVICE |
| A2 fingerprint after lockout | after five wrong attempts fingerprint must not open the vault | PENDING-DEVICE |
| A3 Change Password toolbar swap and new-password step (smali-resolved) | title and step order against 2.7.2 | PENDING-DEVICE |
| A4 biometric prompt title and the 600 ms delay | title text against 2.7.2 | PENDING-DEVICE |
| A8 recovery button | reads Next, Next, then Submit | PENDING-DEVICE |
| P4 picker folder row, 4-column grid, cell and tray visibility (smali-resolved) | when the folder row, Add and tray show; column count; cell layout | PENDING-DEVICE |
| Secured-toast gravity | position and duration against 2.7.2 (ours: bottom, nav-bar inset plus 64.dp) | PENDING-DEVICE |
| PIN slot glyph | filled dot U+25CF versus the reference glyph (`shortcut:` in `VaultLockScreen.kt`) | PENDING-DEVICE |
| Pattern dot and path tint and sizes | unselected 12.dp, selected 28.dp, path 3.dp (guessed from XML) | PENDING-DEVICE |
| Biometric prompt delay and cancel | prompt shows after 600 ms; cancel leaves the PIN screen | PENDING-DEVICE |
| Recovery selector drop arrow | does the reference selector show an arrow (ours shows none) | PENDING-DEVICE |
| Settings rows | is the whole row (not only the switch) tappable in 2.7.2 for the two switch rows | PENDING-DEVICE |
| Thumbnails and MIUI | decrypt speed on a large vault; hide and unhide visible in Photos at once on MIUI | PENDING-DEVICE |
| German, Russian, Tamil | long strings on Confirm, switch link, Add Items, settings subtitles, selectors | PENDING-DEVICE |
| Arabic RTL | rows mirror, PIN slots and keypad stay left to right | PENDING-DEVICE |
| Light and dark | every vault screen side by side with 2.7.2 | PENDING-DEVICE |
| Picker loader and tray arrow | loader after a folder switch; tray arrow does not rotate | PENDING-DEVICE |
| Rotation / process death mid-setup | lock entry restarts (accepted limit); lockout counter survives | PENDING-DEVICE |

## Deviation ledger

Recorded as known deviations from 2.7.2 (also noted in `PARITY-CHECKLIST.md`):

1. **D2** No Hidden Folder section in the vault (not built; no bar or album cards).
2. **D3** Forgot Password uses a trimmed, case-insensitive match against hashed answers (2.7.2 matches exactly).
3. PIN, pattern and recovery answers are stored hashed (salted, constant-time compare); the reference stores them differently.
4. Home lock-icon shared-element transition (`vault_click`) is not built.
5. The secured toast is an in-app overlay (shield icon, `ic_toast_bg`), not a system `Toast`, so its position is ours to match.
6. UI-SPEC section 6 correction: the picker folder row is the toolbar (title, loader, arrow), and the grid has four columns (D1: `activity_select`).
7. No shake animation on wrong entry (D4).
8. Share from the vault decrypts to a cache copy first (fixed 07-06, the old code shared ciphertext).
9. Accepted limits: crash between row insert and source delete can leave a duplicate vault row (never a lost file); a hide interrupted mid-batch has no batch undo.

## Fixture root and Wave 0 reference captures

Required root: `Pictures/ZZPhase7Test-<unique-id>` on the test device.
Recorded fixture root: **none yet.**
Screenshot directory: **not created.**

How to record the fixture root when a phone is attached (do this before any hide, unhide or delete):

1. `adb devices` shows one authorized device, then pick an id: `ROOT=ZZPhase7Test-$(date +%Y%m%d-%H%M)`.
2. `adb shell mkdir -p /sdcard/Pictures/$ROOT` and `adb push <local disposable jpg/png/gif/mp4 files> /sdcard/Pictures/$ROOT/`. Include: two or more plain photos with distinct names, one fixture to hide, then push a second file with the same name into the root before unhiding (no-overwrite check), one animated GIF or WebP (Animated entry point), one strongly coloured image (Search by colour), and a short video.
3. Index them: `adb shell am broadcast -a android.intent.action.MEDIA_SCANNER_SCAN_FILE -d file:///sdcard/Pictures/$ROOT/<file>` per file (or reboot the media scanner by opening the Photos tab).
4. Record the root: write the `ROOT` value and `adb shell ls -l /sdcard/Pictures/$ROOT` output into this section (replace "none yet"), plus the device serial and `adb shell getprop ro.build.version.release`.
5. Guard rule: before every Hide, Unhide, Delete confirmation, read the selected item's path and confirm it starts with `/storage/emulated/0/Pictures/<ROOT>/`. If anything outside the root is selected, cancel and stop.
6. Before step 6 of the device pass (Clear data), unhide every fixture first; clearing app data deletes the encrypted vault files, so anything still hidden is lost.
7. Install both builds: reference 2.7.2 (package `albums.gallery.photo.folder.picasa.app.web.gallery`) and ours: `adb install -r app/build/outputs/apk/debug/app-debug.apk` (application id `com.quizango.gallery`; a differently signed earlier build needs `adb uninstall com.quizango.gallery` first).

Wave 0 reference capture list (take on 2.7.2 first with `adb exec-out screencap -p > <name>.png`, then repeat on ours):

1. Lock: set PIN, re-enter PIN, unlock PIN
2. Lock: draw pattern, redraw pattern, unlock pattern, error (red) pattern
3. Recovery question steps 1, 2, 3 (button Next then Submit), selector open
4. Forgot password (selector open, wrong answer toast)
5. Vault empty, vault populated, vault selecting, unhide and delete dialogs
6. Privacy Settings (fresh install)
7. Picker: folder row, folder list, grid with selection, tray expanded, empty
8. Secured toast on first vault open
9. Biometric prompt
10. Light, dark, and one Arabic RTL variant of the lock, vault and settings screens

## Acceptance status

Automated gates are green and the blocking human device checkpoint (07-09 Task 3) remains open. When the device pass passes with captured screenshots and fixture-confined destructive actions, tick Ported only for numeric rows #32-#37 in `PARITY-CHECKLIST.md`, leave Verified `[ ]`, and touch no other row. Until then the checklist stays unchanged.
