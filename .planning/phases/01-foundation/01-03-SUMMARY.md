---
phase: 01-foundation
plan: 03
subsystem: ui-foundation
tags: [colors, night, sdp, drawables, theme]
requires: [01-01]
provides:
  - scripts/ref_res.py check-colors gate
  - 76 colour names and 76 night overrides from 2.7.2; 10 shared colours aligned
  - 26 values-swNNNdp/dimens.xml buckets (117 sdp/ssp entries each)
  - 106 2.7.2 drawables (83 new, 14 explore_* refreshed, 9 unchanged)
  - splash_bg window background
affects: [01-05, Phase 2+ ported screens]
tech-stack:
  added: []
  patterns: [reference XML copied byte-for-byte, verified by script gates]
key-files:
  created:
    - app/src/main/res/values-sw{330..1080}dp/dimens.xml
    - app/src/main/res/drawable/ (83 new files)
    - app/src/main/res/drawable-nodpi/{img_feature_promo_top.webp,ic_gs2_default_app.png,ic_gs2_success.png}
    - app/src/main/res/drawable-night-nodpi/img_feature_promo_top.webp
  modified:
    - scripts/ref_res.py
    - app/src/main/res/values/colors.xml
    - app/src/main/res/values-night/colors.xml
    - app/src/main/res/values/themes.xml
    - app/src/main/res/drawable/explore_* (14 overwritten)
  deleted:
    - app/src/main/res/drawable/launch_background.xml
    - app/src/main/res/drawable/app_icon_pre_splash.xml
decisions:
  - Reference raster brand/stock art copied as-is (user decision 2026-11-13), replace before any store release
  - ic_vault_with_bg gradients inlined with aapt:attr instead of the reference's decoded $-named drawables
metrics:
  tasks: 2
  commits: 2
  files: 160
status: complete
actuals:
  tokens: 45000
  tasks: 2
  commits: 2
plan_head_before: 9ff76d89d5883d071efa915347a676dc4a79d552
plan_head_after: 7ad7c435fb0d4317564b6206f429a60c20cd22a8
completed: 2026-11-13
---

# Phase 1 Plan 3: Colours, sdp buckets and 2.7.2 drawables Summary

All 2.7.2 colours (light and night), the 26 smallest-width sdp/ssp buckets and 106 drawables are in the app and build; cold start uses the 2.7.2 logo-free `splash_bg`.

## Commits

- 0c1df44 feat(01-03): colours with night overrides, 26 sdp buckets, check-colors gate
- 7ad7c43 feat(01-03): import 106 drawables, window background to splash_bg

## Gate results

- `check-colors`: exit 0, "178 light / 114 night names match 2.7.2" (failed with 69 missing / 10 differing / 28 missing night before the import)
- sw-bucket diff: 26 buckets, 0 differing (baseline 26)
- drawable parity: import set 106, 105 byte-identical, 1 intentional difference (ic_vault_with_bg, see below)
- ad-SDK drawables 0, non-schema URLs in res/drawable* 0
- APK gate (aapt2): `_13sdp` at sw360dp = 15.6 (1), explore_card_bg night #1a1b1f (1), home_shell_popup_bg night #2a2a2c (1)
- `:app:assembleDebug` and `:app:testDebugUnitTest` pass

## Deviations from Plan

**1. [Rule 1 - Bug in plan gate] Dimens regex misses ssp**
- The plan's `name="_(minus)?[0-9]+s[dp]p"` matches `sdp` and `spp`, not `ssp`, so it would have kept only 101 of 117 entries per bucket and left ssp text sizes unscaled. Used `s[ds]p`; the verify diff was run with the corrected pattern (0 of 26 differ). Files: all 26 dimens.xml. Commit 0c1df44.

**2. [Rule 3 - Blocking] ic_vault_with_bg references outlined gradient drawables**
- **Found during:** Task 2 build ("resource drawable/$ic_vault_with_bg__0 not found"). The plan stated imports reference no other drawable; the decoder splits aapt2 inline gradients into `$name__N.xml` files and the vector points at them.
- **Fix:** replaced the two `@drawable/$ic_vault_with_bg__N` fill references with the equivalent inline `<aapt:attr name="android:fillColor">` gradients (same colours, offsets, coordinates). This is the only import that is not byte-identical to 2.7.2 (105 of 106 are). The other 33 `$`-referencing reference drawables are not in the import set.
- **Files:** app/src/main/res/drawable/ic_vault_with_bg.xml. Commit 7ad7c43.

**3. [Counting] Plan said 98 new app drawables**: actual 83 new + 14 refreshed + 9 identical = 106; the import set total matches.

No file owned by plan 01-04 was touched. STATE.md and ROADMAP.md not edited per orchestrator instruction.

## Known Stubs

None. Reference brand art (feature-promo photos, gs2 illustrations) shipped as-is per user decision; pre-release replacement tracked by 01-05.

## Threat Flags

None.

## Self-Check: PASSED

check-colors, bucket diff, parity, ad/URL, APK gate, build and unit tests all pass; commits 0c1df44 and 7ad7c43 exist on branch worktree-agent-a2659ba4115177b2f.
