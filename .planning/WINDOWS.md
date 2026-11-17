---
schema_version: 1
open_count: 4
waived_count: 0
fixed_count: 5
total_count: 9
last_updated: 2026-11-17T14:20:00.000Z
---

# Broken Windows Ledger

> Cross-phase defect register. With `workflow.windows_enforce` enabled, `/gsd-ship` blocks while `open_count > 0`.
> Waive with `gsd-tools windows waive <id> "<reason>"` (reason required).
> Mark fixed with `gsd-tools windows fixed <id>`.

| id | phase | kind | file | line | description | status | reason | recorded_at | resolved_at |
|----|-------|------|------|------|-------------|--------|--------|-------------|-------------|
| 1 | 03 | deviation | app/src/main/java/com/quizango/gallery/ui/home/PhotosTab.kt |  | Pinch commit threshold is the plan's 1.2 fallback and commit/cancel = 280/200 ms is unconfirmed; check S272/ai/n.smali when the 2.7.2 decode is available | fixed | Resolved in 03-06 follow-up (22bbe4f): S272/z/t steps mid-gesture at span ratio 1.12 / 0.90; z/i OooOOO0 never steps on release, it springs back via ai/n OooOOo0; finish and spring-back take 140 + 60 x remaining ms (ai/n OooO0Oo / OooOOo0); 280 ms is ai/n's commit timeout and 200 ms the elastic return. | 2026-11-15T16:13:27.761Z | 2026-11-17T12:00:00.000Z |
| 2 | 03 | unrun-verify | app/src/main/java/com/quizango/gallery/ui/home/HomeOverlays.kt |  | 03-04 pinch gesture and tutorial not run on a device (Redmi has a differently signed build); backstop in 03-06 | open |  | 2026-11-15T16:13:28.879Z |  |
| 3 | 03 | unrun-verify | app/src/main/java/com/quizango/gallery/ui/home/HomeScreen.kt |  | 03-05 device Back check (vault hint closes on Back, 'Tap again to exit' appears) not run: Redmi has a differently signed build; backstop in 03-06 | fixed | Resolved on the Redmi in 03-06 (debug build 9ab0b01): with the vault hint up, Back closes it; Back on Home then slides up 'Tap again to exit'; Back again keeps Home; tapping the text closes the app. Screenshots 12/13 in the 03-06 device run. | 2026-11-15T16:29:12.157Z | 2026-11-17T14:20:00.000Z |
| 4 | 03 | deviation | app/src/main/java/com/quizango/gallery/ui/home/HomeOverlays.kt |  | Vault-hint arrow size (intrinsic 65x71dp) and copy-column bottom alignment guessed: layout_home_vault_hint.xml not on this machine; check against 2.7.2 in 03-06 | fixed | Resolved in 03-06: decoded 2.7.2 layout_home_vault_hint.xml has the arrow wrap_content (intrinsic 65x71dp, paddingBottom 6sdp) and the copy column bottom-aligned (sec_txt bottom_toBottomOf parent, title above it); HomeOverlays matches, and the device screenshot (11-ours-s4-vault-hint) shows the pill over the title with arrow and copy below. | 2026-11-15T16:29:13.314Z | 2026-11-17T14:20:00.000Z |

````json
[
  {
    "id": 1,
    "kind": "deviation",
    "phase": "03",
    "file": "app/src/main/java/com/quizango/gallery/ui/home/PhotosTab.kt",
    "line": null,
    "description": "Pinch commit threshold is the plan's 1.2 fallback and commit/cancel = 280/200 ms is unconfirmed; check S272/ai/n.smali when the 2.7.2 decode is available",
    "status": "fixed",
    "reason": "Resolved in 03-06 follow-up (22bbe4f): S272/z/t steps mid-gesture at span ratio 1.12 / 0.90; z/i OooOOO0 never steps on release, it springs back via ai/n OooOOo0; finish and spring-back take 140 + 60 x remaining ms (ai/n OooO0Oo / OooOOo0); 280 ms is ai/n's commit timeout and 200 ms the elastic return.",
    "recorded_at": "2026-11-15T16:13:27.761Z",
    "resolved_at": "2026-11-17T12:00:00.000Z"
  },
  {
    "id": 2,
    "kind": "unrun-verify",
    "phase": "03",
    "file": "app/src/main/java/com/quizango/gallery/ui/home/HomeOverlays.kt",
    "line": null,
    "description": "03-04 pinch gesture and tutorial not run on a device (Redmi has a differently signed build); backstop in 03-06",
    "status": "open",
    "reason": "",
    "recorded_at": "2026-11-15T16:13:28.879Z",
    "resolved_at": null
  },
  {
    "id": 3,
    "kind": "unrun-verify",
    "phase": "03",
    "file": "app/src/main/java/com/quizango/gallery/ui/home/HomeScreen.kt",
    "line": null,
    "description": "03-05 device Back check (vault hint closes on Back, 'Tap again to exit' appears) not run: Redmi has a differently signed build; backstop in 03-06",
    "status": "fixed",
    "reason": "Resolved on the Redmi in 03-06 (debug build 9ab0b01): with the vault hint up, Back closes it; Back on Home then slides up 'Tap again to exit'; Back again keeps Home; tapping the text closes the app. Screenshots 12/13 in the 03-06 device run.",
    "recorded_at": "2026-11-15T16:29:12.157Z",
    "resolved_at": "2026-11-17T14:20:00.000Z"
  },
  {
    "id": 4,
    "kind": "deviation",
    "phase": "03",
    "file": "app/src/main/java/com/quizango/gallery/ui/home/HomeOverlays.kt",
    "line": null,
    "description": "Vault-hint arrow size (intrinsic 65x71dp) and copy-column bottom alignment guessed: layout_home_vault_hint.xml not on this machine; check against 2.7.2 in 03-06",
    "status": "fixed",
    "reason": "Resolved in 03-06: decoded 2.7.2 layout_home_vault_hint.xml has the arrow wrap_content (intrinsic 65x71dp, paddingBottom 6sdp) and the copy column bottom-aligned (sec_txt bottom_toBottomOf parent, title above it); HomeOverlays matches, and the device screenshot (11-ours-s4-vault-hint) shows the pill over the title with arrow and copy below.",
    "recorded_at": "2026-11-15T16:29:13.314Z",
    "resolved_at": "2026-11-17T14:20:00.000Z"
  },
  {
    "id": 5,
    "kind": "deferred",
    "phase": "03",
    "file": "app/src/main/java/com/quizango/gallery/ui/home/HomeViewModel.kt",
    "line": null,
    "description": "CodeRabbit PR #1: possible one-frame 'No items' flash between the first sync finishing and the first list emission; simple fix can strand the spinner on an empty library. Check on the phone in 03-06",
    "status": "open",
    "reason": "",
    "recorded_at": "2026-11-17T05:20:02.000Z",
    "resolved_at": null
  },
  {
    "id": 6,
    "kind": "deferred",
    "phase": "03",
    "file": "app/src/main/java/com/quizango/gallery/ui/components/NavigationComponents.kt",
    "line": null,
    "description": "CodeRabbit PR #1: hardcoded 'Back' contentDescription has no 2.7.2 string key; handle in Phase 5",
    "status": "open",
    "reason": "",
    "recorded_at": "2026-11-17T05:20:02.000Z",
    "resolved_at": null
  },
  {
    "id": 7,
    "kind": "deferred",
    "phase": "03",
    "file": "app/src/main/res/",
    "line": null,
    "description": "CodeRabbit PR #1: 12 translation findings (bn delete title order + sort labels, fil sort_by, hi copy toast, pt zoom_out in Russian, mr last_modified + add_to_title, my album prompts + change_number, tr/zh RAW, ta got_it) kept as imported 2.7.2 text; confirm each matches the decoded 2.7.2 APK in 03-06",
    "status": "fixed",
    "reason": "Resolved in 03-06: ref_res.py render() of every language against the decoded 2.7.2 APK differs from our values-xx files only in the \t vs \t spelling of a tab (the known check-strings note), so all 12 flagged strings are 2.7.2's own text (e.g. ar ad_to_private 'ضافة إلى الخاصة' is identical in 2.7.2).",
    "recorded_at": "2026-11-17T05:20:02.000Z",
    "resolved_at": "2026-11-17T14:20:00.000Z"
  },
  {
    "id": 8,
    "kind": "deferred",
    "phase": "03",
    "file": "app/src/main/java/com/quizango/gallery/ui/home/HomeScreen.kt",
    "line": null,
    "description": "CodeRabbit PR #1 re-review: copying into the selection's own folder (Add-to, or Create album with that folder's name) is a silent no-op that clears the selection; match what decoded 2.7.2 does (message string or hidden folder) in 03-06",
    "status": "fixed",
    "reason": "Resolved in 03-06 follow-up (fa8f435): after picking an album, dialog_select_add asks Move or Copy (S272/oa/w). Move into the folder holding every selected item shows 'Media files should not be moved to the same folder.' (ca/br Oooo0O0); Copy uses s9/k.OooOOO naming, so copying into the same folder makes name(1).ext.",
    "recorded_at": "2026-11-17T05:43:18.000Z",
    "resolved_at": "2026-11-17T12:00:00.000Z"
  },
  {
    "id": 9,
    "kind": "deferred",
    "phase": "03",
    "file": "app/src/main/java/com/quizango/gallery/ui/splash/SplashScreen.kt",
    "line": null,
    "description": "CodeRabbit PR #1 re-review: session count can double-increment if the Activity is recreated during the pending DataStore write in Splash; fix by owning the increment in a retained ViewModel if it shows up",
    "status": "open",
    "reason": "",
    "recorded_at": "2026-11-17T05:43:18.000Z",
    "resolved_at": null
  }
]
````
