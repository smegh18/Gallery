---
phase: 04-albums-stories-and-explore
plan: 02
subsystem: ui
tags: [compose, stories, ken-burns, pointer-input, tooltip]
status: complete
requires:
  - phase: 04-albums-stories-and-explore
    provides: shareSelectedMedia (internal), isStoriesEditToolTip prefs, story_edit_bg / popup_stories_edit_bg / btn_stories_edit_bg drawables, shimmer colours
provides:
  - 2.7.2 story viewer (3000 ms steps, alternating Ken Burns, press/tap/hold on halves, ad steps every 3rd photo, header/progress/Edit pill, 500 ms fade)
  - first-time "Edit your photo" tooltip
  - recordStoryUsageAccessTrigger no-op hook for Phase 12
affects: [04-06, 05 (VIEW-01 slideshow moves into the viewer), 12 (USG-01)]
tech-stack:
  added: []
  patterns: ["Ken Burns scale derived from the step's own paused progress, so zoom and bar pause together", "pure story helpers (storySteps, storyTouch, kenBurnsZoomIn) unit-tested on the JVM"]
key-files:
  created:
    - app/src/test/java/com/quizango/gallery/StoryViewTest.kt
  modified:
    - app/src/main/java/com/quizango/gallery/ui/discovery/StoryViewScreen.kt
    - app/src/main/java/com/quizango/gallery/ui/navigation/GalleryNavHost.kt
decisions:
  - "One Animatable per step (remember keyed by step and restart) drives both the progress segment and the zoom; no separate zoom animator to keep in sync"
  - "fromStoryRow = StoryQueue.find(path) != null; vault and viewer slideshow keep plain photo steps"
  - "Edit tooltip pref defaults to true in the nav host until DataStore loads, so it never flashes"
metrics:
  tasks: 3
  files: 3
  commits: 2
plan_head_before: af546c5c13ce0db64ff443bb1c50ba87c9d36884
plan_head_after: db5c9a3ea9106777a67f7f5e3a602a5a87c7e046
actuals:
  tokens: 9000
  tasks: 3
  commits: 2
---

# Phase 4 Plan 02: Stories viewer to 2.7.2 Summary

Story viewer rewritten to 2.7.2: 3000 ms steps with alternating accelerate-decelerate zoom, finger-down pause with half-screen tap / hold rule (mirrored in RTL), a static ad skeleton step after every 3rd row-story photo, the activity_story_view_photo header and grayTitle progress bar in sdp, a first-time Edit tooltip, and a no-op usage-access hook for Phase 12.

## What was built

- `StoryViewScreen.kt`: `STORY_STEP_MS`, `STORY_AD_EVERY`, `storySteps`, `StoryTouch` / `storyTouch`, `kenBurnsZoomIn`, `recordStoryUsageAccessTrigger`, private `StoryProgressBar`, `StoryHeader`, `StoryAdSkeleton`. The old tap detector, 3500 ms, 35% zone, gradient scrim, material-icons and English literals are gone. Share goes through `shareSelectedMedia` for the current photo only (nothing on an ad step).
- `GalleryNavHost.kt`: story route has its own 500 ms fade enter/exit/pop transitions, passes `fromStoryRow`, `editTipSeen` (initial true) and `onEditTipSeen` (writes `setStoriesEditToolTip(true)`).
- `StoryViewTest.kt`: step time, ad-step placement (6, 4, 2 photos, no-ads), touch rule (halves, RTL, 499 ms / 99.9 px vs 500 ms / 100 px), zoom alternation.

## Verification

- `StoryViewTest` passes; full `:app:testDebugUnitTest` and `:app:assembleDebug` pass.
- Grep gates: 3500=0, 0.35f=0, detectTapGestures=0, material.icons=0, verticalGradient=0, English literals=0, hook refs=3 with one shortcut comment, `tween(500)` on the story route=4.
- Device (Redmi 9, 720x1600): Recent story opens with the date title, white-on-grey segmented bar, close/share icons and the Edit pill; the tooltip appears above the pill and pauses playback; OK dismisses it and playback resumes. The 4th step shows the static skeleton with no date and no Edit pill. Back and Close paths not exercised on device beyond opening/closing the story.

## Deviations from Plan

**1. [Process] Tasks 1-3 committed as one feature commit**
- The three tasks all rewrite one file (`StoryViewScreen.kt`) and the nav host route; splitting would have meant writing the file three times. The RED test commit (59d080a) is separate; the GREEN code is db5c9a3. All three tasks' verify commands were run against the final state.

**2. [Judgement] Skeleton line heights**
- "One 20.ssp / 15.ssp text line tall" is rendered as 24.sdp / 18.sdp (about 1.2 line height); the CTA bar is 20.sdp padding + about 17 dp empty line = 37.sdp. 04-06 side-by-side should confirm.

## Known Stubs

- `recordStoryUsageAccessTrigger()` is intentionally empty until Phase 12 (USG-01); documented with the `shortcut:` comment.
- Ad step is a static skeleton by design (D5 placeholder), not a stub that blocks the goal.

## Threat Flags

None. T-04-03 (share only the current photo, none on ad step) and T-04-04 (vault path gets no ad steps, tooltip or hook; `onEditClick` stays null) are implemented as planned.

## Self-Check: PASSED

- StoryViewTest.kt, StoryViewScreen.kt, GalleryNavHost.kt present; commits 59d080a and db5c9a3 on branch worktree-agent-a52b777b98256b70a.
