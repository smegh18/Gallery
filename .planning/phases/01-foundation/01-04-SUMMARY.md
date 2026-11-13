---
phase: 01-foundation
plan: 04
subsystem: i18n
tags: [strings, translations, 2.7.2, recycle, vault]
requires: [01-01]
provides:
  - 72 new/changed 2.7.2 string resources in values/strings.xml (271 keys total)
  - 24 regenerated values-<lang>/strings.xml with 2.7.2 translations
  - Recycle Bin and Private Vault select toggles on R.string.select_all / unselect_all
affects: [all later ported screens]
key-files:
  modified:
    - app/src/main/res/values/strings.xml
    - app/src/main/res/values-{24 codes}/strings.xml (generated)
    - app/src/main/java/com/quizango/gallery/ui/recycle/RecycleScreen.kt
    - app/src/main/java/com/quizango/gallery/ui/vault/VaultMediaScreen.kt
    - tooling-setup.md
decisions:
  - about_privacy_policy added as resource only (URL), wired to no UI
metrics:
  tasks: 2
  commits: 2
status: complete
requirements: [FND-02]
actuals:
  tokens: 30000
  tasks: 2
  commits: 2
plan_head_before: 9ff76d8
plan_head_after: 2ccce92
completed: 2026-11-13
---

# Phase 1 Plan 4: Full 2.7.2 string set, translated Summary

All 72 missing new/changed 2.7.2 English strings copied verbatim by script from the reference XML, 24 translation files regenerated with `sync-strings`, and the Recycle/Vault select toggles moved to `R.string.select_all` / `unselect_all`.

## Tasks

| Task | Name | Commit |
|------|------|--------|
| 1 | Import 72 new and changed 2.7.2 English strings | 62dcd32 |
| 2 | Toggles on string resources, translations regenerated, tooling note | 2ccce92 |

## Verification

- English parity gate: 83 checked, 0 differ from 2.7.2; English file has 271 keys.
- SDK/Crashlytics key guard: 0 in values/strings.xml.
- `python -I scripts/ref_res.py check-strings`: 24 languages up to date.
- Spot checks: de close=1, ru select_all=1, de album_tab_memories=0, bn gs2_installing_title=0.
- No hardcoded "Select All"/"Deselect" left in RecycleScreen.kt / VaultMediaScreen.kt.
- `:app:testDebugUnitTest` and `:app:assembleDebug` succeed (after each task).

## Deviations from Plan

None - plan executed as written. The import was a throwaway script (kept in the scratchpad, not committed) that extracted raw `<string>` lines from R272, so values are byte-identical to the reference. No edit to scripts/ref_res.py was needed.

## Known Stubs

None. about_privacy_policy is an intentionally unwired resource (Phase 9).

## Threat Flags

None.

## Self-Check: PASSED

Commits 62dcd32 and 2ccce92 are on the branch; strings.xml, the 24 translation files, both screens and tooling-setup.md exist and are committed.
