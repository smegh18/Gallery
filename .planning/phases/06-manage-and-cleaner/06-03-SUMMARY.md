---
phase: 06-manage-and-cleaner
plan: 03
subsystem: cleaner
status: complete
tags: [cleaner, junk, parity, delete]
requires: [06-02, 06-12]
provides:
  - "JunkScanner.clean(paths): List<PathMutationOutcome> (absent = SUCCESS, folder that filled up or undeletable = FILE_FAILED)"
  - "RemoveJunkActivity results: tri-state selection, Log Files opt-in, Clean / Deep Clean, cleaning state with 800 ms minimum"
  - "JunkClearedActivity state; AppPreferences.setJunkCleanedForToday written only after a clean removed something"
  - "SystemCleanerRows / SYSTEM_CLEANER_ROWS shared by System Cleaner and Junk cleared"
affects: [06-11]
tech-stack:
  added: []
  patterns: ["frozen path snapshot at tap -> per-path outcomes -> failures stay ticked", "async(IO + NonCancellable) delete raced against a minimum display delay"]
key-files:
  created:
    - app/src/main/res/drawable/ic_check_new.xml
    - app/src/main/res/drawable/ic_uncheck_new.xml
    - app/src/main/res/drawable/ic_uncheck_new_sub.xml
    - app/src/main/res/drawable/ic_radio_foreground_new.xml
    - app/src/main/res/drawable/layout_progress_image_view.xml
  modified:
    - app/src/main/java/com/quizango/gallery/data/local/JunkScanner.kt
    - app/src/main/java/com/quizango/gallery/data/local/datastore/AppPreferences.kt
    - app/src/main/java/com/quizango/gallery/ui/cleaner/RemoveJunkScreen.kt
    - app/src/main/java/com/quizango/gallery/ui/cleaner/CleanerScreen.kt
    - app/src/main/java/com/quizango/gallery/ui/navigation/GalleryNavHost.kt
    - app/src/test/java/com/quizango/gallery/RemoveJunkTest.kt
    - app/src/test/java/com/quizango/gallery/JunkScannerTest.kt
    - app/src/test/java/com/quizango/gallery/CleanerTest.kt
decisions:
  - "Deep Clean cleans the ticked items, like Clean (decoded dx/g: btnDelete case 0xc -> rewarded ad -> case 7 -> OoooooO, the same clean as btnClean case 0xb). The plan's 'opens System Cleaner' contradicts the approved UI-SPEC §2.2 and the smali; the spec wins. The AD badge stays (Q2), no ad runs."
  - "Clean keeps its grey reference look at all times but does nothing at 0 ticked bytes (plan Task 2); Deep Clean dims to 0.6 and disables at 0 (RJ.OoooOO0). 2.7.2 would run an empty clean and show 'Finished'; we skip that so cleaned-today is never written without a delete."
  - "Partial failure still opens Junk cleared with only the removed bytes; failures stay ticked in the results. If every path fails, the results return with them ticked."
  - "The cleaning size counts down from the ticked size to 0 (RJ ValueAnimator.ofFloat(total, 0)); Junk cleared counts up 0 -> freed (dx/a). Both 800 ms with the exact AccelerateDecelerateInterpolator curve."
  - "Sizes are read once after the scan and kept in a map, so recomposition never stats files and freed bytes use the scanned sizes."
  - "Sub-row icon is the category icon (APK files: the app icon); img_remove_junk_sun_item and ic_item_not_expand were not imported because nothing draws them (empty rows show No Junk with no arrow)."
metrics:
  duration: "~70 min (about 25 min waiting on host commit memory)"
  completed: 2026-11-23
estimate:
  tokens: 85000
  tasks: 3
actuals:
  tokens: 20600
  tasks: 3
  commits: 3
plan_head_before: 5592039759c780aceae77a4eb7296e429e976404
plan_head_after: ce4ffae
---

# Phase 6 Plan 03: Junk results, cleaning and Junk cleared Summary

The junk flow now runs end to end the way 2.7.2 does. Results show five rows with tri-state checks, and Log Files start unticked. Clean or Deep Clean deletes exactly the paths that were ticked at the tap. The cleaning Lottie stays up for at least 800 ms, then `junk_done` plays and Junk cleared shows the bytes that were really freed. Cleaned-today is saved only after a delete has removed something.

## Tasks

| # | Task | Commit |
|---|------|--------|
| 1 | Tracer: results, tri-state selection, `clean` returns per-path outcomes (tracer re-verified before expanding) | 81647c9 |
| 2 | Clean / Deep Clean, cleaning state, 800 ms minimum, all-fail returns to results, button hint | 3bb18e0 |
| 3 | Junk cleared state, `setJunkCleanedForToday`, shared System Cleaner rows, navigation | ce4ffae |

## What changed
- `JunkScanner.clean(paths)` deduplicates its input and returns one `PathMutationOutcome` per path. A missing path counts as SUCCESS. A folder that is no longer empty, or a delete that fails or throws `SecurityException`, is FILE_FAILED.
- `RemoveJunkScreen.kt`. The pure helpers below are unit-tested:
  - `initialJunkSelection`, `junkCheckIcon`, `toggleJunkCategory` and `junkBytes`
  - `runJunkClean(snapshot, sizes, markCleaned)`, which returns `JunkCleanResult(removed, failed, freedBytes)`
  - `canStartJunkClean` and `phaseAfterJunkClean`
  - the `JunkRow` enum, which maps the scanner ids to the `dy/m` names and the `dy/g` icons
- **Results (§2.2):**
  - The `layout_progress_image_view` header animates from 350 to 300 dp over 500 ms. It shows "Storage Clean", the `storage_clean_bg_img` circle, the ticked size with a half-size unit (`formatCleanerSizeSpanned`) and "Junk Found".
  - Each row has a 22.sdp header with icon, title, arrow, size and check, or "No Junk" when empty. Sub-rows use 12.sdp text and `ic_check_new` / `ic_uncheck_new_sub`.
  - The bottom bar floats over the list: a 38.sdp bar holding Clean (`bg_remove_junk_clean_disabled`) and Deep Clean (`bg_remove_junk_deep_clean` + `deep_clean_ic`).
  - `remove_junk_button_animation` appears after 3000 ms and only while the ticked size is above 0.
- **Cleaning (§2.3):** `cleaner_lottie` (Crop, loop) runs with the size counting down in 35.sdp text and "Cleaning....". The transparent top bar has `ic_back_white` and "Remove Junk". The delete runs as `async(IO + NonCancellable)` alongside `delay(800)`. When something was removed, `junk_done` plays once and then Junk cleared opens.
- **Junk cleared (§2.4):**
  - `SharedMediaToolbar` titled "Remove Junk" (`tool_bar_remove_junk`), then `img_junk_cleared_done`
  - the freed size in Bold 18.ssp, counting up ("Finished" when 0)
  - "Junk files Cleaned", then "Release more space now"
  - the three System Cleaner rows: Duplicate Images, Screenshots and Large Video
  - The old Done button and all English literals are gone. Back pops the route. A row keeps Junk cleared underneath it (phase and freed bytes are saveable).
- Removed `formatStorageBytes`, `JunkCategory` and their two `CleanerTest` cases, plus the `onNavigateToCleaner` route parameter.

## Verification
- `:app:testDebugUnitTest` (full): 272 tests in 46 classes, 0 failures. `:app:assembleDebug`: OK. Run with `-Dorg.gradle.jvmargs=-Xmx2048m`, in-process Kotlin compile and a scratchpad init script setting test `maxHeapSize=512m` (no project files touched). Finished with `./gradlew.bat --stop`.
- `RemoveJunkTest` has 9 tests, all on temp-directory fixtures. They cover:
  - Log Files start unticked
  - the mixed icon, with bytes counted from ticked paths only
  - Clean deletes only the ticked document; the unticked one and an unselected `.tmp` survive; a filled folder stays ticked as failed
  - the start guard against zero bytes and repeat taps
  - all-fail returns to the results
  - cleaned-today is written only when something was removed
  - the release rows route to Duplicates, Screenshots and Large Videos
- `JunkScannerTest` checks the outcome order and statuses, deduplication, absent paths, and that a folder that filled up is not deleted.
- `python -I scripts/ref_res.py check-strings` passed ("24 languages up to date") after Task 1. Before the Task 3 run, the decoded reference `%TEMP%\ref272\dec` was deleted by a temp cleanup during this session, so the Task 3 re-run could not execute. Task 3 adds no string keys (all keys existed from 06-01), so the earlier pass still applies. Re-decode with apktool before the next string gate (see tooling-setup.md).

## Deviations from Plan

**1. [Spec precedence] Deep Clean cleans instead of opening System Cleaner**
- The plan's must-have says Deep Clean "opens System Cleaner directly". The approved UI-SPEC §2.2 says the opposite ("Clean / Deep Clean both start §2.3 for the checked items"), and so does the decoded `dx/g` (case 0xc → rewarded ad → case 7 → `OoooooO`, the same clean as case 0xb). User decision Q2 says "the action runs directly (no ad)". I implemented the spec: the AD badge is kept and the clean runs at once. If the plan wording was intended, it is a one-line change to `onClick` on the Deep Clean pill.

**2. [Plan over spec] Clean does nothing at 0 bytes**
- UI-SPEC says Clean is "always tappable". The plan's Task 2 test requires it to be disabled at 0. I kept the reference look and made it inert, so no empty "clean" can write cleaned-today.

**3. [Rule 3 - Environment] Host memory**
- The Gradle daemon crashed three times on commit-charge mmap failures, caused by other sessions' JVMs. A scratchpad script now waits for free commit memory before each gate run. Only the environment was affected. The JVM crash logs (`hs_err_pid*`, `replay_pid*`) were deleted, not committed.

Not unit-tested (no Compose UI test infrastructure in the repo): the visuals, the actual 800 ms on screen, the `junk_done` hand-off, the hint delay, and Back behaviour (no BackHandler steps to an earlier state: system and toolbar Back both call `popBackStack`).

## Known Stubs
None. Ads stay collapsed placeholders, per D5.

## Threat Flags
None beyond the plan's register:
- T-06-05 is mitigated by the frozen snapshot, the tri-state tests and the Log Files opt-in.
- T-06-06 is mitigated by outcome-based bytes and writing the preference after the delete.
- T-06-07 is mitigated by `canStartJunkClean` and the phase switch made in the tap handler.

## Device steps for phase-end acceptance (06-11)
1. Put disposable fixtures on the phone, for example `/sdcard/ZZJunkTest/a.tmp` (a few kB), `old1.pdf` and `old2.txt` with `touch -d 2024-01-01`, and an empty folder `/sdcard/ZZJunkTest/Empty`. Then open Explore → Scan Now.
2. In the results, check:
   - the header shrinks once from 350 to 300 dp
   - the size shows a half-size unit
   - Temp Files and Empty Folders are ticked, Log Files show `ic_uncheck_new`, and empty rows show "No Junk"
   - against `evidence/ref272-remove-junk.png`, in light and dark
3. Expand Log Files and tick only `old1.pdf`. The category icon must turn into the red mixed radio, and the header size must grow by that file only. After about 3 s, the button hint Lottie appears over the bar.
4. Untick everything. Deep Clean must dim to 60%, Clean must do nothing, and the hint must hide. Re-tick, then double-tap Clean fast: only one cleaning run may start.
5. Tap Clean. You should see the `cleaner_lottie` full screen and the size counting down for at least about 0.8 s, then `junk_done`, then Junk cleared with the freed size counting up, "Junk files Cleaned", "Release more space now" and the three rows. Check with `adb shell ls /sdcard/ZZJunkTest` that `old1.pdf` and `a.tmp` are gone and `old2.txt` still exists.
6. Explore → Scan Now must now open System Cleaner (cleaned-today is persisted). Back from Junk cleared must return to Explore or System Cleaner, never to the results or the scan. Open Duplicate Images from Junk cleared, then press Back: you return to Junk cleared.
7. Deep Clean: repeat with a new fixture. It must clean the same way, with no ad and the AD badge visible.
8. All-fail case, if feasible: make a ticked file undeletable (for example an SD-card file without a grant). The results come back with it still ticked, and Junk cleared does not open.
9. Remove `/sdcard/ZZJunkTest` afterwards.

## Self-Check: PASSED
- FOUND: RemoveJunkScreen.kt, JunkScanner.kt, AppPreferences.kt, CleanerScreen.kt, GalleryNavHost.kt, RemoveJunkTest.kt, JunkScannerTest.kt, CleanerTest.kt, ic_check_new.xml, ic_uncheck_new.xml, ic_uncheck_new_sub.xml, ic_radio_foreground_new.xml, layout_progress_image_view.xml
- FOUND commits: 81647c9, 3bb18e0, ce4ffae (ancestors of HEAD)
