---
phase: 05-viewing-and-browsing
plan: 03
subsystem: viewer
status: complete
tags: [viewer, video, exoplayer, VIEW-02]
requires: [05-02]
provides: [inline-short-video, VideoPlayerScreen, Screen.VideoPlayer, MediaRepository.saveVideoFrame, mediaSourceFlow]
affects: [05-06, 10]
tech-stack:
  added: []
  patterns: [one ExoPlayer per screen, paused on ON_PAUSE and released on dispose; the route carries the list source, never the playlist]
key-files:
  created:
    - app/src/main/java/com/quizango/gallery/ui/viewer/VideoPlayback.kt
    - app/src/main/java/com/quizango/gallery/ui/viewer/VideoPlayerScreen.kt
    - app/src/test/java/com/quizango/gallery/VideoPlayerTest.kt
  modified:
    - app/src/main/java/com/quizango/gallery/ui/viewer/MediaViewerScreen.kt
    - app/src/main/java/com/quizango/gallery/ui/viewer/ViewerChrome.kt
    - app/src/main/java/com/quizango/gallery/ui/viewer/ViewerDialogs.kt
    - app/src/main/java/com/quizango/gallery/ui/navigation/Screen.kt
    - app/src/main/java/com/quizango/gallery/ui/navigation/GalleryNavHost.kt
    - app/src/main/java/com/quizango/gallery/data/repository/MediaRepository.kt
    - app/src/main/java/com/quizango/gallery/data/local/datastore/AppPreferences.kt
    - app/src/main/AndroidManifest.xml
    - app/src/test/java/com/quizango/gallery/MediaViewerTest.kt
    - app/src/main/res/values*/strings.xml
decisions:
  - "Both players pause on ON_PAUSE rather than ON_STOP: it fires first on Back, on opening another screen and on Home, so audio stops before the navigation transition"
  - "MainActivity now handles orientation/screenSize changes in place (2.7.2's VideoPlayerActivity declares the same configChanges), so rotating the player keeps playback and state"
  - "The Video player route carries path + list source (folder, isRecycle, isVault); the player rebuilds the queue through the shared mediaSourceFlow"
  - "A vault video plays alone: the player gets the decrypted cache path, which is not in the vault list"
  - "Inline videos still auto-play while a slide show runs, so the 05-02 slide-show Mute keeps working"
requirements: [VIEW-02]
metrics:
  completed: 2026-11-21
actuals:
  tokens: 16000
  tasks: 3
  commits: 3
plan_head_before: 61380176caf1371ec832b0305355528f5b93a848
plan_head_after: 722cf83
---

# Phase 5 Plan 03: Inline short video and the Video player Summary

Videos up to 60 s now play inside the viewer. They start paused, the mute, play and speed controls sit in the pill dock, and the reference seek row shows while playing. Longer videos show the centre play button, which opens a new full-screen Video player. It queues the list's videos and has the 2.7.2 controls, gestures, track pickers and error copy. Both players stop their audio on page change, Back, Home and close.

## Done

### Task 1 (tracer): short/long split and the routed player, commit c72410b
- `VideoPlayback.kt` holds the split and queue rules:
  - `playsInline` matches `ch/m`: a video of at most 60 000 ms with a plain path plays inline. A longer one, or a `content://` / `file://` path, opens the player.
  - `videoPlaylist` keeps only the videos, in list order, with the tapped one selected. A path missing from the list plays alone.
- Inline viewer:
  - The player is created only for inline videos. It starts paused on the first frame and does not repeat.
  - Other video pages show `ic_full_video_play` (padding 30.sdp) over the thumbnail. A tap checks the file on IO with `isPlayableVideo`: a bad file shows `toast_corrupted_video`, a good one opens `Screen.VideoPlayer`.
- `Screen.VideoPlayer` route and NavHost destination. `mediaSourceFlow` was pulled out of `MediaViewerScreen`, so the viewer and the player read the same list.
- Imported 14 reference strings (`toast_corrupted_video`, `no_previous_video`, `no_next_video`, `no_repeat`, `repeat_one`, `loop`, `speed`, `capture_ss`, `ten_sec`, `paused`, `subtitle_not_available`, `audio_not_available`, `video_not_found`, `this_video_format_not_supported`) and synced them to 24 languages.

### Task 2: inline seek, speed, mute and capture, commit d9860b9
- Removed the Material slider and surface strip.
- `ViewerDock` now takes a `shortVideoRow`, laid out per §1.6:
  - mute: start, 18.sdp margin, 12.sdp padding
  - play: centre
  - speed: end
  - all three tinted white
- `ShortVideoSeekRow` shows only while the video plays and the chrome is up, and hides at once (§1.4). Its `VideoSeekBar` draws the `sb_progress_drawable` / `sb_thumb` look, tinted `shortVideoProgress`:
  - a 5.dp track at the drawable's #26 alpha
  - a 16.dp thumb
  - tap or drag to seek
  - mirrored in RTL

  Under it, the times use shadowed `BodyXSmallMedium`. `formatDuration` adds hours only when needed.
- `OptionsPopup` is the `popmenu_short_video_speed` card: 100.sdp wide with the five speeds and the select tick. The player's speed and track pickers reuse it.
- At the end the video stops. The next Play starts again from zero (`restartsFromZero`).
- Capture goes through the new `MediaRepository.saveVideoFrame`:
  - It saves `IMG_yyyyMMdd_<millis>.png` (the `VideoPlayerActivity` name format) to Pictures/Screenshots.
  - It scans the file and syncs.
  - It shows no toast on success or failure.
- The old player is now released before the next page's player is built, so two players never exist at once.

### Task 3: Video player controls, gestures and lifecycle, commit 722cf83
- Layout per §2.1:
  - Top bar (`videoPlayerColor`, 56.dp): Back, file name, Subtitle, Audio track, Details.
  - Quick card (`video_control_back`): four 52.sdp columns. Speed, Repeat (icon and label follow the current mode), Mute (label shows the current state: "Unmute" while sound is on, as in the reference), Capture ss.
  - Bottom panel (`video_bottom_back`): times with a white seek bar, then Lock · Previous · Play · Next · Rotate · Floating. Landscape adds Repeat and the ±10 s buttons.
- Gestures:
  - Tap toggles the controls.
  - Double-tap seeks by thirds: the left and right thirds seek ±10 s and show the fast-seek arrows for 600 ms. The centre pauses and shows the Paused chip for 1 s.
  - A vertical swipe on the left half sets window brightness; on the right half it sets music volume. The `video_control_back` pill shows a 5×80.sdp bar and hides 500 ms after the swipe.
- The controls auto-hide 3000 ms after the last interaction, and stay up while a picker is open. Lock hides everything except `ic_video_lock` at top-start.
- Repeat cycles No Repeat, Repeat One, Loop (REPEAT_MODE_OFF / ONE / ALL) and is saved as `isNoRepeat` (2.7.2 key) in `AppPreferences`.
- Previous and Next show `no_previous_video` / `no_next_video` at the ends of the queue.
- The subtitle and audio pickers list the supported Media3 tracks and apply a `TrackSelectionOverride`. With no tracks they show `subtitle_not_available` / `audio_not_available`. Subtitles are drawn in `video_subtitle_bg` 15.sdp above the panel, from `onCues`, and PlayerView's own subtitle view is hidden.
- Errors: a missing file shows `video_not_found`; an unsupported container, format or decoder shows `this_video_format_not_supported`; anything else shows `something_went_wrong`. After the toast the player closes.
- Lifecycle:
  - One `ExoPlayer` per screen: paused on `ON_PAUSE`, stopped on the Back button, released on dispose.
  - Orientation is `SCREEN_ORIENTATION_SENSOR` while the player is open, and Rotate locks portrait or landscape.
  - On leave, the previous orientation comes back and window brightness returns to `BRIGHTNESS_OVERRIDE_NONE`.
- Floating is visible and does nothing yet, with a `shortcut:` comment naming Phase 10. No overlay permission is requested.
- Details reuses `MediaDetailsDialog` (made internal). It fills the resolution from the player's video size when the row has none.
- TalkBack labels come from Media3's translated `exo_controls_*` descriptions where the reference has none. Lock, Unlock and Floating use new untranslatable `viewer_*` labels, as `viewer_back` already does.

## Verification
- `GRADLE_USER_HOME="$HOME/.gradle" ./gradlew.bat :app:testDebugUnitTest -q` passes: VideoPlayerTest has 7 tests, MediaViewerTest 23.
  - **VideoPlayerTest:**
    - the 60 000 / 60 001 ms boundary, plus content and file URIs
    - the queue derivation
    - the route round trip, with no playlist in the URI
    - the repeat cycle and saved values
    - the ±10 s clamp and tap thirds
    - the 3 s auto-hide
    - refusals at the playlist ends
    - the error copy mapping
  - **MediaViewerTest:**
    - hour formatting
    - the restart-from-zero end state
    - the five speed labels
    - the capture file name
- `./gradlew.bat :app:assembleDebug -q` passes.
- `python -I scripts/ref_res.py check-strings`: 24 languages up to date.
- The tracer gate ran in auto mode: the automated verify passed again before Tasks 2 and 3. The human check moved to the device list below.

## Not verified: device steps for the phase-end check
The phone was disconnected, so nothing ran on a device. On the Redmi:
1. **Short clip:** open a short clip (about 24 s). It must sit paused with mute, play and speed in the pill. Compare it with `ref272-viewer-short-video-paused.png`.
2. **Short clip playing:** press Play. The seek row appears, the heart turns into Capture, and the dock row matches `ref272-viewer-short-video-playing.png`.
3. **Short clip controls:** try all five speeds and mute. Let the clip end: it must stop, not loop. Pressing Play again restarts it.
4. **Capture:** Capture while playing. A new `IMG_yyyyMMdd_*.png` appears in Pictures/Screenshots and in our Photos tab, with no toast.
5. **Long video:** open one over 60 s. The centre button opens the player on that video, and Previous / Next walk the album's videos, toasting at the ends.
6. **Player controls:**
   - 10-second double-taps
   - the centre double-tap Paused chip
   - the repeat cycle (and that it is still set after reopening)
   - mute, speed, rotate (portrait and landscape layouts) and lock/unlock
   - brightness and volume swipes
   - the 3 s auto-hide
7. **Track pickers:** use a video with no subtitles (toast) and one with two audio tracks, if one is available.
8. **Audio stops:** check that the audio stops immediately on each of:
   - Home, in both players
   - Back, in both players
   - a page change in the viewer
   - closing the player while it plays
9. **After leaving the player:** the app's brightness and portrait orientation return to normal.
10. **Error toast:** a corrupt or renamed-away video shows `toast_corrupted_video` from the viewer, or the matching error toast in the player.

## Deviations from Plan

### Auto-fixed Issues
1. **[Rule 2 - Missing critical] Window state the player changes is now restored.** The swipe changes the brightness of the whole window, and our player shares MainActivity with the rest of the app, unlike 2.7.2's separate activity. Leaving the player now restores the brightness and the requested orientation (722cf83).
2. **[Rule 3 - Blocking] Rotation would have recreated the app.** MainActivity had no `configChanges`, so rotating the player would recreate the activity and lose the player. Added `orientation|screenSize|screenLayout|smallestScreenSize`, the same set the reference's VideoPlayerActivity declares (722cf83).
3. **[Rule 2 - Missing critical] Audio played on during the exit transition.** With only dispose and `ON_STOP`, audio kept playing through the 180-260 ms exit animation. Both players now pause on `ON_PAUSE`, and the player's Back button calls `stop()` first.

### Judgement calls
- The plan's "player teardown is idempotent" test was not written: `ExoPlayer.release()` cannot run in a JVM unit test. Release happens once, in the screen's `DisposableEffect`. The pure rules got their tests.
- The subtitle picker has no "Off" row, because the spec does not define the picker's rows. It lists the available tracks only.
- After Home and back, the player resumes paused. The reference resume behaviour was not checked.
- With No Repeat, ExoPlayer moves on to the next queued video at the end (REPEAT_MODE_OFF). Loop repeats the whole queue. This was not checked on the reference device.

## Known Stubs
- `VideoPlayerScreen` Floating button: an intentional no-op hook. The Phase 10 floating player owns it (UI-SPEC §2.2).

## Threat Flags
None beyond the plan's threat model:
- T-05-03: the file is checked before the player opens, Media3 errors map to the reference copy and close the player, and there is one player per screen.
- T-05-04: playback pauses on `ON_PAUSE`, the old page's player is released first, and every terminal path releases.

Capture writes only to the public Pictures/Screenshots folder, through the same java.io + scan path that copy uses.

## Not updated here
STATE.md, ROADMAP.md and REQUIREMENTS.md were not touched in this worktree; the orchestrator owns them during parallel waves.

## Self-Check: PASSED
- All three files in `key-files.created` exist.
- Commits c72410b, d9860b9 and 722cf83 are on the branch.
