---
phase: 05-viewing-and-browsing
plan: 01
status: complete
completed: 2026-11-21
---

# 05-01 Summary: viewer tracer (resources, zoom, glass chrome)

Built by Codex and finished by Claude Code.

## Done
- Imported the Phase 5 resource contract from decoded 2.7.2:
  - 52 drawables
  - 15 colours (light and night)
  - the strings that were missing, synced to all 24 languages
  - `scripts/ref_res.py` updated to cover them

  `check-strings` and `check-colors` pass.
- `ViewerZoom.kt` sets the max zoom per image: 2× landscape, 5× portrait, with separate GIF/WebP limits. Double-tap zooms to that max. The pointer handler refreshes when the decoded image size or the container size changes, so zoom limits are never out of date. Tested in `ViewerZoomTest`.
- `ViewerChrome.kt` adds the glass top bar (back, rotate, heart, details). `MediaViewerScreen.kt` now uses it in place of the 2.6.7 top bar.

## Verification
- `:app:testDebugUnitTest` and `:app:assembleDebug` pass.
- Device check on the Redmi over adb passed: open, double-tap zoom, pan while zoomed without changing page, paging after zoom-out, and Back. See `05-01-DEVICE-CHECK.md`. Two Artemis runs stalled before this and were stopped.

## Left for later plans
- The dock, actions, dialogs and slide show belong to 05-02.
- Pinch was not exercised on the device.
