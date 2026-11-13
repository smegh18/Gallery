---
phase: 01-foundation
plan: 01
subsystem: ui-foundation
tags: [i18n, theme, typography, inter, sdp, compose]
requires: []
provides:
  - scripts/ref_res.py (sync-strings, check-strings)
  - 24 generated values-<lang>/strings.xml
  - Inter FontFamily, GalleryTypography on Inter, GalleryText (23 reference Body* styles)
  - Int.sdp / Int.ssp (sdpFactor)
affects: [01-03, 01-04, all later ported screens]
tech-stack:
  added: [Inter fonts (SIL OFL, byte-identical to 2.7.2)]
  patterns: [reference XML -> generated res via stdlib Python; ssp-scaled text styles]
key-files:
  created:
    - scripts/ref_res.py
    - app/src/main/res/values-{24 codes}/strings.xml
    - app/src/main/res/font/if_inter_{light,regular,medium,semi_bold,bold}.ttf
    - app/src/main/java/com/quizango/gallery/ui/theme/Dimens.kt
    - app/src/test/java/com/quizango/gallery/ThemeTest.kt
    - app/src/test/java/com/quizango/gallery/DimensTest.kt
  modified:
    - app/src/main/res/values/strings.xml
    - app/src/main/res/values/colors.xml
    - app/src/main/java/com/quizango/gallery/ui/components/NavigationComponents.kt
    - app/src/main/java/com/quizango/gallery/ui/theme/{Color,Theme,Type}.kt
    - six screens with explicit TextStyle (Story, Photos, Welcome, Permission, Search, VaultRecoveryQuestion)
decisions:
  - Keys skipped by the sync (kept English): app_name, permanent_delete_Image_title, permanent_delete_Image_sub_title
  - All 24 LanguageScreen codes generated (UI-SPEC said 23, it missed fil)
metrics:
  tasks: 2
  commits: 2
  files: 40 (plus 5 binaries)
status: complete
actuals:
  tokens: 65000
  tasks: 2
  commits: 2
plan_head_before: c5a6ed6052edd7ef1406de70a5441ad4f5b77c2e
plan_head_after: 26935fe2c1d8cf84e280d62db13cecb97f9849ac
completed: 2026-11-13
---

# Phase 1 Plan 1: Tracer slice and sdp/ssp + reference text styles Summary

Reference-import pipeline proven end to end (2.7.2 XML -> generated translations -> APK), plus colorPrimary #0066FF in both modes, Inter app-wide, Int.sdp/Int.ssp and the 23 GalleryText styles.

## Tasks

| Task | Name | Commit |
|------|------|--------|
| 1 | Tracer: Home select toggle in 2.7.2 wording, accent, Inter, device language | 9551f83 |
| 2 | sdp/ssp helper, 23 reference text styles, Inter on 6 explicit TextStyle sites | 26935fe |

## Verification

- ThemeTest (1 test) and DimensTest (1 test, 7 buckets) pass; full `:app:testDebugUnitTest` passes.
- `:app:assembleDebug` succeeds.
- `python -I scripts/ref_res.py check-strings`: 24 languages up to date.
- APK gate: `select_all(de)=1 colorPrimary_0066ff=1 inter_fonts=5`.
- GalleryText name diff against R272 `values/styles.xml` is empty (23 names).
- The five Inter fonts are byte-identical to R272 (`cmp`).
- Tracer gate: `<verify>` re-run end to end after Task 1, passed before Task 2 started.

## Deviations from Plan

**1. [Rule 3 - Blocking] Worktree lacked the plan files.** The worktree branch was based on b5264c7, which predates the `.planning/phase 01` commits on main. Fast-forwarded the worktree branch to main (c5a6ed6, docs-only commits) so the plan and UI-SPEC were readable. No code was altered by this.

**2. TDD RED run not performed separately for DimensTest.** Test and implementation were written back to back and committed together in the Task 2 commit; the test passes against the implementation. Dimens.kt is three trivial declarations.

No other deviations. ThemeTest runs on the JVM, so the plan's fallback (removing it) was not needed.

## Known Stubs

None.

## Notes for later plans

- `check-strings` compares in text mode, so CRLF checkouts (git autocrlf on Windows) compare equal to the LF files the script writes.
- Generated files carry 150-161 strings per language (bn 152, tr/zh 160); keys the reference does not translate fall back to English like in 2.7.2. No format-specifier mismatches were reported.
- Reference brand art untouched (user decision).

## Threat Flags

None.

## Self-Check: PASSED

Files created exist (ref_res.py, Dimens.kt, ThemeTest.kt, DimensTest.kt, 5 fonts, 24 strings files); commits 9551f83 and 26935fe are on the branch.
