---
phase: 06-manage-and-cleaner
plan: 01
subsystem: cleaner
status: complete
tags: [cleaner, strings, parity, formatter]
requires: []
provides:
  - "Restyled System Cleaner (UI-SPEC §1) with CleanerEntry order/targets"
  - "Phase 6 reference string keys, dup_delete_files_removed plural, 3 colors"
  - "formatCleanerSize / formatCleanerSizeSpanned (s9/k)"
affects: [06-02, 06-03, 06-04, 06-05, 06-06, 06-07, 06-08, 06-09, 06-10, 06-11]
tech-stack:
  added: []
  patterns: ["SharedMediaToolbar + drawableBackground(layout_shape) cards", "raw-text key import from R272 then ref_res.py sync-strings"]
key-files:
  created:
    - app/src/main/java/com/quizango/gallery/ui/cleaner/CleanerFormatters.kt
    - app/src/main/res/drawable/layout_shape.xml
    - app/src/main/res/values/plurals.xml
  modified:
    - app/src/main/java/com/quizango/gallery/ui/cleaner/CleanerScreen.kt
    - app/src/test/java/com/quizango/gallery/CleanerTest.kt
    - app/src/main/res/values/strings.xml
    - app/src/main/res/values/colors.xml
    - app/src/main/res/values-*/strings.xml (24 generated locales)
decisions:
  - "Size formatter uses Locale.US symbols in every locale, as s9/k does (no locale parameter needed)"
  - "Unit index clamped at TB; the reference would index out of bounds from 1 PiB"
  - "Organizer code literals stored as translatable=false keys org_folder_created, org_folder_create_failed, org_no_actions_to_undo, org_dropdown"
  - "System Cleaner Scan now uses the layout's key scan_now (\"Scan Now\"), not explore_scan_now"
metrics:
  duration: "~60 min (about 25 min lost waiting on host memory)"
  completed: 2026-11-23
estimate:
  tokens: 75000
  tasks: 3
actuals:
  tokens: 32700
  tasks: 3
  commits: 3
plan_head_before: aa11853d9dd4eb12e3b3c942329882ebf2962c41
plan_head_after: 5b3fb31d7f47c246e5b293cce18ea4649202d732
---

# Phase 6 Plan 01: System Cleaner entry, Phase 6 copy and size formatter Summary

System Cleaner now uses the 2.7.2 layout: `layout_toolbar`, `layout_shape` stroke cards, reference keys and the red Scan Now button. The 44 Phase 6 reference strings, the plural and the 3 colors are imported. One `s9/k` size formatter (1024-based, `#,##0.#`, US symbols) is ready for the later slices.

## Tasks

| # | Task | Commit |
|---|------|--------|
| 1 | Tracer: System Cleaner restyle, CleanerEntry order/targets test | 547c6c0 |
| 2 | Phase 6 strings (44 keys + 4 literal keys), plural, colors, synced locales | cabec0f |
| 3 | `formatCleanerSize` / `formatCleanerSizeSpanned` with tests | 5b3fb31 |

## What changed
- `CleanerScreen.kt`: the Material TopAppBar, `ArrowBack` glyph, 16.dp filled cards and English literals are gone. The screen now uses `SharedMediaToolbar`, `drawableBackground(R.drawable.layout_shape)` cards with 14.sdp padding, intrinsic-size icons, a top-anchored title and subtitle (BodyLargeSemiBold / BodySmallMedium), and a Scan Now pill (82.sdp, MaterialButton insets, `cleaner_red`). The `CleanerEntry` enum fixes the order Remove Junk, Duplicate Images, Screenshots, Large Video. `pick()` maps each entry to the existing callbacks, so the NavHost needs no change. Both the card and Scan Now open the junk scan.
- `values/strings.xml`: the UI-SPEC "Keys to import" list was copied as raw text from R272, which keeps escapes, quotes, the fixed "1.40 GB" `junk_scan_completed_message` and the multi-line `str_step_4`. The organizer code literals were added as `translatable="false"`. The new `values/plurals.xml` holds `dup_delete_files_removed`.
- `values/colors.xml`: added `color_success_lottie_blue` #ff0066ff, `blackTran` #90000000 and `organizer_stripe_color` #ff358f38. None of them has a night override in 2.7.2.
- `CleanerFormatters.kt`: pure ports of `s9/k.OooO0o0` and `OooO0Oo` (the unit drawn at 0.5em). The old screen-local formatters (`formatStorageBytes`, `formatVideoSize`, `formatDuplicateSize`) stay for now. Per the plan, their owning slices switch them over.

## Verification
- `:app:testDebugUnitTest` (full) passes. `:app:assembleDebug` passes.
- `ref_res.py check-strings` reports 24 languages up to date. `check-colors` reports 216 light / 129 night matching. `processDebugResources` passes.
- The locale diffs are pure additions.

## Deviations from Plan

### Auto-fixed Issues

**1. [Rule 1 - Bug] Clamp size unit index at TB**
- **Found during:** Task 3
- **Issue:** 2.7.2 indexes `units[5]` for sizes of 1 PiB or more, which throws ArrayIndexOutOfBounds.
- **Fix:** `coerceAtMost(lastIndex)`, so 1 PiB renders "1,024 TB" (tested).
- **Commit:** 5b3fb31

**2. [Rule 3 - Blocking] Gradle run with a 2 GB daemon heap**
- **Issue:** Parallel executors' daemons used up the host's commit charge, and the test JVM and the daemon crashed with mmap failures.
- **Fix:** Ran the gates with `-Dorg.gradle.jvmargs=-Xmx2048m`. This affects the environment only. No project files changed.

Also noted: the plan's Task 3 said to take a Locale dependency. The reference hard-codes `Locale.US` symbols, so the formatter does too, and a test checks the output under `Locale.GERMANY`.

## Known Stubs
None.

## Device steps for phase-end acceptance (06-11)
- Open Explore → System Cleaner. Check the toolbar and the layout_shape cards in light and dark against 2.7.2 (#F2F2F2 / #1E1E1E stroke).
- Tap the Remove Junk card and the Scan Now button: both must open the junk scan. Tap Duplicate Images, Screenshots and Large Video: each must open its flow. Back must return to System Cleaner, then to Explore.
- Long locales (de, ru, ta): Scan Now must ellipsize inside 82.sdp without clipping. Check the RTL mirror in ar.

## Self-Check: PASSED
- FOUND: CleanerFormatters.kt, layout_shape.xml, plurals.xml
- FOUND commits: 547c6c0, cabec0f, 5b3fb31
