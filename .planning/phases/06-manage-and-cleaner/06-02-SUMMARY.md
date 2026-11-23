---
phase: 06-manage-and-cleaner
plan: 02
subsystem: cleaner
status: complete
tags: [cleaner, junk, scanner, parity]
requires: [06-01]
provides:
  - "JunkScanner with the decoded 2.7.2 predicates: exact lowercase tmp and apk, old documents in LOG, injectable now, PROGRESS_STEPS"
  - "JunkScanActivity scan/done state in RemoveJunkScreen (JunkScanState, JUNK_SCAN_HOLD_MS, junkScanStatus)"
  - "Imported drawables ic_back_white, layout_progress_background, layout_complete_background"
affects: [06-03, 06-11]
tech-stack:
  added: []
  patterns: ["drawableBackground(shape xml) for full-screen reference gradients", "LaunchedEffect job.ensureActive inside the IO walk to stop a stale scan"]
key-files:
  created:
    - app/src/test/java/com/quizango/gallery/RemoveJunkTest.kt
    - app/src/main/res/drawable/ic_back_white.xml
    - app/src/main/res/drawable/layout_progress_background.xml
    - app/src/main/res/drawable/layout_complete_background.xml
  modified:
    - app/src/main/java/com/quizango/gallery/data/local/JunkScanner.kt
    - app/src/main/java/com/quizango/gallery/ui/cleaner/RemoveJunkScreen.kt
    - app/src/test/java/com/quizango/gallery/JunkScannerTest.kt
decisions:
  - "APK match is the exact lowercase extension apk (dr/c case 2), like temp; UP.APK is no longer junk"
  - "Old-document extensions are lowercased (aw/b uses toLowerCase(Locale.ROOT)) and lastModified must be strictly older than now minus one calendar year"
  - "The Android/ subtree under the storage root stays skipped (other apps' data), as the findings' rejected cases require"
  - "One storage walk fills the four storage categories, so 40/60/80/100 post together after it; 20 posts after app cache"
  - "Path readout uses end ellipsis: Compose 1.7.8 has no MiddleEllipsis (shortcut comment in code)"
metrics:
  duration: "~75 min (about 35 min waiting on host commit memory)"
  completed: 2026-11-23
estimate:
  tokens: 80000
  tasks: 3
actuals:
  tokens: 6600
  tasks: 3
  commits: 3
plan_head_before: ec50c202231146cca0c830e8c2d0d97fbb34d14c
plan_head_after: 3410f9ef60ef2a05f6d2e10e988b00858e0c762f
---

# Phase 6 Plan 02: Junk scanner rules and JunkScanActivity state Summary

The junk scanner now uses the decoded 2.7.2 rules. Temp Files accept only `.tmp` (lowercase), APK files only `.apk` (lowercase), and Log Files list documents older than one year. The scan screen matches JunkScanActivity: orange gradient and scanner Lottie while scanning, then the red done state with the fixed "Junk Files Found: 1.40 GB / Scanning Completed!" text, held for 2.5 s before the results open.

## Tasks

| # | Task | Commit |
|---|------|--------|
| 1 | Tracer: exact `tmp` predicate, stable empty categories, progress milestones | e7ac44b |
| 2 | Old-document Log Files rule with injectable `now`, exact `apk` | 91b04b6 |
| 3 | JunkScanActivity scan/done state, 2500 ms hold, stale-scan guard | 3410f9e |

## What changed
- `JunkScanner.kt`: `ext()` ports `ex/j` (the text after the last `.`, `""` when there is none). The rules are now `ext == "apk"`, `ext == "tmp"`, and old documents (`doc docx pdf txt csv xls xlsx ppt pptx xml`, lowercased, `lastModified < Calendar(now).add(YEAR, -1)`). The `LOG` id is kept and documented as the "Log Files" display category. App cache paths are de-duplicated. A `SecurityException` during the walk keeps what was already found. `onProgress` posts `PROGRESS_STEPS` (20 after app cache, then 40/60/80/100 after the storage walk). Classification never deletes anything.
- `RemoveJunkScreen.kt`: the SCANNING branch is now `JunkScanState`, which shows:
  - `layout_progress_background` and the `scan_cleaner_lottie` loop while scanning
  - `layout_complete_background`, `done_cleaner_lottie` (played once) and `junk_scan_completed_message` at 100
  - the `ic_back_white` toolbar 56.dp tall at 24.sdp, titled `remove_junk` in BodyXXLargeSemiBold
  - the path readout (BodyMediumRegular, 6.sdp below the status)

  `JUNK_SCAN_HOLD_MS = 2500`. The scan's `onPath` calls `job.ensureActive()`, so leaving the screen stops the IO walk. Cancelling the `LaunchedEffect` also discards the results. Log Files stay unticked (Q6). The old hand-built "Junk Files Found: <measured size>" literal, the M3 ArrowBack, the pulse/sweep circle and the `.sp` sizes in this state are gone.

## Verification
- `:app:testDebugUnitTest` (full) passes: 253 tests, 0 failures. `:app:assembleDebug` passes. Both gates ran with `-Dorg.gradle.jvmargs=-Xmx2048m`, followed by `./gradlew.bat --stop`.
- `JunkScannerTest` (6 tests) covers:
  - `a.tmp` is accepted with its real bytes; `a.temp`, `b.TMP`, `c.Tmp`, `tmp`, `d.`, the directory `dir.tmp` and `Android/data/x/e.tmp` are rejected
  - null, missing and file-as-root inputs return the five empty categories
  - every old-document extension, including uppercase `PDF`, is accepted one second before the cutoff; files exactly at the cutoff, one second after it, `.log` and `.jpg` are rejected
  - categories keep their order with no duplicate paths; `UP.APK` is rejected
- `RemoveJunkTest` covers the 20/40/60/80/100 milestones, the 2500 ms hold constant, `junkScanStatus`, and that `values/strings.xml` holds exactly `Junk Files Found: 1.40 GB\nScanning Completed!` with no format argument.
- All fixtures live under `Files.createTempDirectory`. Nothing touches real media.

## Deviations from Plan

### Auto-fixed Issues

**1. [Rule 3 - Blocking] Progress milestones added to the scanner in Task 1**
- **Issue:** Task 3's milestones need an `onProgress` on `JunkScanner`, which is outside Task 3's file list. The UI edit had to compile at every commit.
- **Fix:** Added `onProgress`/`PROGRESS_STEPS` to the scanner in the Task 1 commit.
- **Commit:** e7ac44b

**2. [Rule 1 - Parity] APK predicate made exact lowercase**
- **Found during:** Task 2 (decoded `dr/c` case 2 compares the extension with `"apk"` exactly, the same way as `tmp`).
- **Fix:** `ext == "apk"`, covered by a test.
- **Commit:** 91b04b6

**3. [Rule 3 - Blocking] Gradle under host memory pressure**
- The test JVM and the daemon crashed with commit-charge mmap failures while parallel executors were running. I waited for memory to free up and re-ran with the same flags. Only the environment was affected; no project files changed.

Not unit-tested, because the repo has no coroutines-test or Compose UI test infrastructure for this and adding a dependency is out of scope:
- the hold timing itself (the 2500 ms constant is tested)
- Back from the scan state (it calls `onNavigateBack` = `popBackStack`, with no BackHandler that steps to an earlier phase)

## Known Stubs
None. The result, cleaning and cleared states are unchanged and belong to 06-03.

## Threat Flags
None. The traversal surface is unchanged except for the narrower predicates. T-06-03 is mitigated by Dispatchers.IO, a throttled path readout and cancellation that stops the walk. T-06-04 is mitigated by tests and the unticked default.

## Device steps for phase-end acceptance (06-11)
- Put disposable fixtures on the phone, for example `/sdcard/ZZJunkTest/a.tmp`, `a.temp`, `b.TMP` and an old `old.pdf` (`touch -d 2024-01-01`). Open Explore → Scan Now / System Cleaner → Remove Junk.
- Watch the scan: orange gradient, scanner Lottie looping, "Scanning..." and the path readout. Then the red done state with `done_cleaner_lottie` and exactly "Junk Files Found: 1.40 GB / Scanning Completed!", held about 2.5 s before results. Compare against `evidence/ref272-junk-scan.png`.
- The results must show only `a.tmp` under Temp Files and `old.pdf` under Log Files, unticked.
- Press Back mid-scan, both toolbar and system Back: you must return to the caller (Explore or System Cleaner) with no flash of results or another junk state.
- Long paths: the readout ellipsizes at the end, not in the middle (the known shortcut). Check light and dark (the gradients have no night variant in 2.7.2) and the RTL back arrow in ar (autoMirrored).
- Remove `/sdcard/ZZJunkTest` afterwards.

## Self-Check: PASSED
- FOUND: JunkScanner.kt, RemoveJunkScreen.kt, JunkScannerTest.kt, RemoveJunkTest.kt, ic_back_white.xml, layout_progress_background.xml, layout_complete_background.xml
- FOUND commits: e7ac44b, 91b04b6, 3410f9e
