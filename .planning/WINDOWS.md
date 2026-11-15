---
schema_version: 1
open_count: 2
waived_count: 0
fixed_count: 0
total_count: 2
last_updated: 2026-11-15T16:13:28.879Z
---

# Broken Windows Ledger

> Cross-phase defect register. With `workflow.windows_enforce` enabled, `/gsd-ship` blocks while `open_count > 0`.
> Waive with `gsd-tools windows waive <id> "<reason>"` (reason required).
> Mark fixed with `gsd-tools windows fixed <id>`.

| id | phase | kind | file | line | description | status | reason | recorded_at | resolved_at |
|----|-------|------|------|------|-------------|--------|--------|-------------|-------------|
| 1 | 03 | deviation | app/src/main/java/com/quizango/gallery/ui/home/PhotosTab.kt |  | Pinch commit threshold is the plan's 1.2 fallback and commit/cancel = 280/200 ms is unconfirmed; check S272/ai/n.smali when the 2.7.2 decode is available | open |  | 2026-11-15T16:13:27.761Z |  |
| 2 | 03 | unrun-verify | app/src/main/java/com/quizango/gallery/ui/home/HomeOverlays.kt |  | 03-04 pinch gesture and tutorial not run on a device (Redmi has a differently signed build); backstop in 03-06 | open |  | 2026-11-15T16:13:28.879Z |  |

````json
[
  {
    "id": 1,
    "kind": "deviation",
    "phase": "03",
    "file": "app/src/main/java/com/quizango/gallery/ui/home/PhotosTab.kt",
    "line": null,
    "description": "Pinch commit threshold is the plan's 1.2 fallback and commit/cancel = 280/200 ms is unconfirmed; check S272/ai/n.smali when the 2.7.2 decode is available",
    "status": "open",
    "reason": "",
    "recorded_at": "2026-11-15T16:13:27.761Z",
    "resolved_at": null
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
  }
]
````
