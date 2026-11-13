---
phase: 01-foundation
plan: 05
subsystem: quality-gates
tags: [coderabbit, unslop, release, device-check]
requires: [01-01, 01-02, 01-03, 01-04]
provides:
  - Phase 1 quality gate results (CodeRabbit, unslop, tests, debug and release builds, parity gates)
  - launch crash fix for splash_bg on Android 11
  - STATE.md pending todo for the reference brand art
affects: [Phase 2 onward]
key-files:
  modified:
    - app/src/main/res/drawable/splash_bg.xml
    - scripts/ref_res.py
    - .planning/STATE.md
status: complete
requirements: [FND-01, FND-02, FND-03, FND-04]
---

# Phase 1 Plan 5: Quality gates and device check (draft, awaiting the on-device check)

Tasks 1 and 2 are done; Task 3 (on-device check in light, dark and other languages) is waiting for the user. This file is the draft the checkpoint refers to.

## Commits

- b244d3a fix(01-05): splash_bg drops its empty @null item, which crashed launch on Android 11
- 932425e fix(01-05): ref_res keeps unindexed placeholder order, check-colors rejects missing reference colours
- 2c9a0cf fix(01-05): ref_res placeholder check also recognises relative %< specifiers
- 17e5034 fix(01-05): ref_res compares which argument each placeholder binds, incl. relative %<

## Deviations from Plan

**1. [Rule 1 - Bug] The app crashed on launch on the test phone (Android 11, API 30).**
- **Found during:** Task 2 install, first launch (device gate, not caught by any build or unit test).
- **Issue:** `splash_bg` (imported byte-identical from 2.7.2 in plan 01-03) holds `<item android:drawable="@null"/>`. API 30 rejects it when inflating the window background: `XmlPullParserException: <item> tag requires a 'drawable' attribute or child tag` -> `Resources$NotFoundException: drawable/splash_bg` at `MainActivity.onCreate`. The item draws nothing.
- **Fix:** removed that empty item; the logo-free `backgroundColor` fill stays. `splash_bg.xml` is now the second intentional difference from 2.7.2 (with `ic_vault_with_bg`); the 01-03 drawable-parity gate now expects exactly those two.
- **Verified:** rebuilt, installed, app starts to Albums with no FATAL EXCEPTION; Home selection top bar reads "Select all" / "Unselect all" in the new blue.
- **Commit:** b244d3a

**2. CodeRabbit review scope.** `coderabbit review --committed --base-commit c5a6ed6` fails with "Too many files" (184 files, limit 150), so the same base was reviewed in `--dir` scopes: `app/src/main/java` (14 files), `app/src/test` (2), `scripts` (1), `.planning` (4), `app/src/main/res/drawable` (95), `res/values` (3), `res/values-night` (1), `res/raw` (15). Not reviewed individually: the 24 generated `values-<lang>/strings.xml`, 26 generated `values-swNNNdp/dimens.xml`, 5 Inter font binaries and 4 `drawable-nodpi` rasters. They are covered by `check-strings`, the sw-bucket diff and byte-comparison with the reference, and by the review of `scripts/ref_res.py` which generates them. The CLI also limits free reviews to 3 per window.

## CodeRabbit result

| Round | Scope | Findings | Action |
|-------|-------|----------|--------|
| 1 | app/src/main/java, app/src/test | 0 | none |
| 1 | scripts | 2 (major: placeholder argument order, minor: missing reference colours.xml) | fixed in 932425e |
| 2 | scripts | 1 (minor: `%<` relative specifier) | fixed in 2c9a0cf |
| 2 | res/drawable, res/values, res/values-night, res/raw, java, test, .planning | 0 | none |
| 3 | scripts (`--fresh`) | 1 (major: `%<s` before its antecedent accepted) | fixed in 17e5034 |
| 4 | scripts (`--fresh`) | CLI free-review rate limit hit ("Rate limit exceeded", 41 minutes) | see final line below |

Nothing was "kept for 2.7.2 parity": every finding was in our own script.

**Final status:** every finding from rounds 1-3 is fixed. The round-4 re-check of `scripts/ref_res.py` (after 17e5034) did not run: CodeRabbit's free-review rate limit was hit, and the orchestrator stopped the run instead of waiting 41 minutes. Re-run `coderabbit review` on `scripts/` at the start of Phase 2.

## Gate results (final tree)

- `:app:testDebugUnitTest`, `:app:assembleDebug`, `:app:assembleRelease` (R8, resource shrinking, lintVitalRelease): all succeed.
- 01-02 gates: debug APK `dm_success=1 retired_cleaner_done=0 app_setings=1`; raw parity 0 differing, 23 files; retired/ad raw 0, URL files 0.
- 01-03 gates: `check-colors` 178 light / 114 night names match 2.7.2; sw buckets 26, 0 differing; `_13sdp` at sw360 = 15.6, explore_card_bg night #1a1b1f, home_shell_popup_bg night #2a2a2c; drawable import set 106, 2 intentionally different (ic_vault_with_bg, splash_bg); ad-SDK drawables 0, non-schema URLs 0; windowBackground is `splash_bg`.
- `check-strings`: 24 languages up to date (unchanged after the placeholder-check hardening, so no translation had an argument-order mismatch).
- Release APK (aapt2): `release dm_success=1 release inter_fonts=5`.
- Reference privacy-policy string (`R.string.about_privacy_policy`) not referenced from Kotlin.
- STATE.md has the brand-art pending todo. No store build until the reference brand art is replaced (T-01-14).
- `adb install -r` of the debug APK: Success.

## unslop audit (audit only, nothing edited)

Scope: the UI Phase 1 changed. The app is native Android (Compose), so the audit reads the code and the screens on the phone; the web-specific signs (gradient text, glows, hero scene) do not apply.

| Surface | Finding | Status |
|---------|---------|--------|
| Home selection top bar | "Select all" / "Unselect all" from 2.7.2 strings, sentence case, accent #0066FF text button, no emoji, no icon decoration | accepted: parity |
| Recycle Bin and Vault selection toggles | Same strings; SemiBold 14sp button in `GalleryBlue` | accepted: parity |
| Duplicate delete-complete screen | dm_success Lottie, 2.7.2 copy; "N freed" is a hard-coded English string at DuplicateCompleteScreen.kt:194 and sizes are plain `sp` (not `ssp`) | open for later: pre-existing, Phase 6 ports this screen (string resource and ssp sizes) |
| Inter typography on Home, Albums, Viewer, Settings | Inter everywhere, weights 400-700 as in 2.7.2 (700 headings are reference parity, not an unslop choice) | accepted: parity; on-device clipping check is checkpoint steps 1, 3, 7 |
| Accent colour | One accent #0066FF in light and dark, as 2.7.2 | accepted: parity |
| Photo grid (All Media) | Photos draw in about the left half of the width with empty space to the right (seen with 90 photos in selection mode) | open: not a Phase 1 change (no grid code touched); looks like a pre-existing layout issue for Phase 3 |
| Brand art (launcher icons, splash, promo images) | reference art kept by decision | accepted per user decision 2026-11-13, tracked in STATE.md |

No emoji, exclamation marks, hue gradients, glows or looping decorative motion were introduced by Phase 1.

## Pending: Task 3 results

To be filled after the user's answer: steps 1-7 approval, the step 8 observation (XML icons in app-Dark with phone in light) and the decision on the open unslop items above.

## Known Stubs

None.

## Threat Flags

None.

## Device check (user, 2026-11-13): approved after fixes

The user ran checks 1-5. Checks 3-5 failed at first. Fixed in 6d5a159:
- Theme: MainActivity is now an AppCompatActivity on a DayNight theme, so Day/Night reaches XML resources too. The saved theme is applied once at startup, which ends the restart loop. Force dark is off, so MIUI no longer darkens Day mode.
- Language: the chosen language is applied through AppCompat locales and kept across restarts. locales_config is added.
- Delete animation (dm_success.lottie): plays on the device (Android 11).

Known follow-up: about 175 UI labels are hardcoded in English. Each one switches to the reference's string when its screen is ported (Phases 2-9).
