---
phase: 03-home-shell-and-photos-tab
plan: 06
type: verification
status: complete
last_updated: 2026-11-19
---

# Phase 3 Plan 06: Verification Gates (in progress)

## Completed gates

- **CodeRabbit:** The committed Phase 3 review completed with 2 minor findings. Verified and fixed both: empty `MediaStoreScanner.scanFiles()` now returns immediately; fast-scroll mapping now uses the viewport range and sends the bottom endpoint to the final item. The final review of the working-tree fixes completed with 0 findings.
- **Unit tests:** `:app:testDebugUnitTest` passed on the final tree (full suite, 5m 24s). The focused fast-scroll regression also passed.
- **Debug build:** `:app:assembleDebug` passed on the final tree (5m 17s).
- **Reference resources:** `check-strings` reports 24 languages up to date; `check-colors` reports 188 light and 120 night names matching 2.7.2. The 31-key English comparison reports 0 differences. All 8 imported drawables are byte-identical to the decoded reference; all 4 Phase 3 colors are present.
- **Legacy/resource checks:** obsolete drawer/nav/brand references = 0; hard-coded selection title/close descriptions = 0. The built APK contains exactly one German `albums` string and one `ic_hint_home` drawable.
- **Unslop audit (static):** No generic visual slop found in the Phase 3 Home shell, Photos grid, selection surfaces, dialogs, or overlays. The blue accent, tutorial Lottie, popup/selection shadows, and non-4dp sizing are intentional 2.7.2 parity choices. The Albums-tab story gradient belongs to Phase 4 and was not changed. This was a source/resource audit; no live Home screenshot was available during the audit.

## Changes made

- Add-to destinations now expose the real Camera and Screenshots folders while excluding synthetic video/filter collections.
- Fast-scroll tests cover viewport-aware endpoints, fitting grids, and the inverse drag mapping.
- Empty MediaStore scan requests now return without waiting for the timeout.
- Regenerated locale resources with `scripts/ref_res.py sync-strings` and restored byte-exact 2.7.2 drawable XML.

## Device checkpoint (human comparison pending)

- ADB reports one authorized, unlocked physical Redmi M2006C3MII (`adb-QG55IJKZHQF6OF6L-aT7pYW._adb-tls-connect._tcp`), Android 11. ARTEMIS diagnostic is ready (5/5 required checks), and the replacement Gemini key verifies successfully. Gemini returned temporary 503/504 availability errors for interaction tasks, so a precisely observed ADB tap was used for the tutorial controls.
- The debug APK was installed with `adb install -r` after user approval; installation succeeded without uninstalling, preserving private app data. Package metadata still reports versionName 2.7.0 (the debug build metadata is unchanged). `cmd package compile -m speed-profile -f` returned Success.
- The first-run pinch tutorial was dismissed using the on-screen GOT IT button. The next hint, “Hold Gallery to open Private Vault,” was dismissed by tapping the empty scrim area, matching `VaultHintOverlay`'s outside-tap handler. ARTEMIS hierarchy and REA screenshot confirmed the unobscured Home screen: Photos header, Today group, three-column photo grid, and Photos / Albums / Explore bottom tabs. No photo was opened or changed.
- Safe interaction checks: Photos → Albums → Explore tab navigation worked; Albums presents “Album” with a Create Album action; Photos overflow showed Sort / Recycle Bin / Settings; Albums overflow showed Recycle Bin / Settings; Sort and Create Album dialogs opened and were cancelled without saving. Long-pressing the Photos title opened the Private Vault lock screen; Back returned without setting a PIN. Dark mode was enabled temporarily and the Albums screen remained legible; the original light mode (`ui_night_mode=1`) was restored and Photos was reselected. Temporary screenshots were deleted after inspection.
- Live comparison and locale follow-up: reference 2.7.2 and debug 2.7.0 were captured on the same device; both light/dark Photos and all three tabs were compared. Horizontal tab swipes work both ways. German, Russian, Tamil and Arabic labels were checked; selection counts remain one line, Arabic mirrors the header/tabs, and the RTL selection bar scrolls to reveal Create Album. English and light mode were restored. Full observations and screenshots are in [03-06-LIVE-COMPARISON.md](03-06-LIVE-COMPARISON.md).
- Remaining-check attempts: ARTEMIS diagnostics and device probe passed (5/5 required; screenshot + 69 hierarchy elements), but Pro traces `4b84772c-5797-48ef-8b25-6468583987a2` and `2d190df2-313c-42f4-ad16-ccd8967b727d` stayed initializing with no task plan; first observed state of the former was Android Overview. A short Flash trace `abe92067-94e6-4d73-aa54-d8352369a797` also did not begin before it was stopped. REA relaunched Gallery after the first attempt; final hierarchy confirmed Photos, Today, three-column grid and bottom tabs, English/light, with no selection. No media was opened or changed. These are failed attempts, not verification passes.
- Before the current gate, the plan's preference helper self-checked, updated only the four Home tutorial/session entries while Gallery was force-stopped, and read back exactly: `sessionCount=3 isPinchInOutToolTip=false isVaultHintToolTip=false vaultHintClosedSession=0`. The authorized session-4 launch then displayed both expected hints.
- Latest DataStore readback after the session-4 verification was `sessionCount=4 isPinchInOutToolTip=true isVaultHintToolTip=true vaultHintClosedSession=4`. The phone is restored to light mode and English, with the app on Photos at the grid top and no selection active. Phase 3 remains in progress: live checkpoint requirements (including selection/delete confirmation, pinch, fast-scroller, exit behavior, and dialog/picker parity) are not all verified; ARTEMIS task execution stalled despite diagnostics/probe passing. No checklist Verified boxes were ticked. Do not test favorite/delete/move/Add to Private outcomes on personal media; use a disposable synthetic item.

- Direct ADB/UIAutomator follow-up: our Home Back shows the exit sheet; reference Home Back exits directly (handoff to Phase 13 per plan). Reference grid swipe hid bottom tabs; no date bubble/thumb was visually present in the saved scroll screenshot. Added pinch instrumentation test compiles, but execution is blocked by MIUI INSTALL_FAILED_USER_RESTRICTED; raw sendevent is denied by Android. Current user 0 has reference installed, debug package absent; temporary test package removed. No media modified.

## Device check closed (2026-11-20)
- **Artemis Pro run b48563a6** (read-only, nothing deleted or moved):
  - grid: 3 columns across the full width, date headers, video badge;
  - the tab bar hides on scroll and returns;
  - the fast-scroll handle shows with no date bubble;
  - selection bar: ✕ / "1 item(s) selected" / Select all, and Share, Favorite, Add to, Add to Private, Delete, Create album;
  - Add to shows the album sheet;
  - Add to Private asks "Hide Media / Are you sure you want to hide media?".
- **Fixed after the run:**
  - The delete prompt now reads 2.7.2's "Are you sure you want to delete 1 item?" (8465f2f).
  - Back on Home exits directly, as 2.7.2 does; the user decided, and the exit sheet is removed (db1fe85).
  - Pinch: recorded 2.7.2 side by side. It steps the column count with a ~200 ms cell glide and no whole-grid zoom. Ours now matches, and the user approved it (18193d5).
- **Open questions answered:**
  - exit popup → direct exit;
  - fast-scroll bubble → none, as in 2.7.2;
  - Add to Private → confirms first, as in 2.7.2;
  - Add to → album sheet.
- **Approved** by the user on the device.
