# HANDOFF: Gallery app, milestone v2.0 (reference 2.7.2 parity)

Written 2026-11-13 by Claude Code for Antigravity to continue this work.

## What this project is
- Android gallery app in Kotlin, Jetpack Compose and Material 3, single module `app`, package `com.quizango.gallery`. Repo `E:/Gallery`, branch `main`, remote `https://github.com/smegh18/Gallery.git`.
- It is a rebuild of a reference app, `albums.gallery.photo.folder.picasa.app.web.gallery`. Our app matched reference **2.6.7**. The current goal is **100% parity with reference 2.7.2**: every screen, flow, animation and behaviour.

## Where things stand
- Main is ahead of origin with Phase 3 complete and Phase 4 implemented through Plan 04-06 (quality gates passed, device checkpoint prepped).
- Planning is done with GSD (get-shit-done) in `.planning/`:
  - `PROJECT.md`: milestone goal and approach.
  - `REQUIREMENTS.md`: 46 requirement IDs with traceability.
  - `ROADMAP.md`: 13 phases.
  - `STATE.md`: current position.
  - `milestones/v2.7.2-parity/PARITY-CHECKLIST.md`: **the master list of 53 screens**, each with Ported and Verified boxes, plus the decisions and flows to verify. Rows #1-#10 and #15 are marked **Ported**.
  - `phases/01-foundation/`: Complete and verified.
  - `phases/02-onboarding/`: Complete and verified.
  - `phases/03-home-shell-and-photos-tab/`: Complete and verified.
  - `phases/04-albums-stories-and-explore/`: Plans 04-01 to 04-06 implemented. Automated gates, CodeRabbit review, unslop audit, and device harness (`ZZPhase4Test` with 6 images, reset first-run states) are ready. Task 3 on-device human verification pending.
- **Phase 4 Summary:**
  - Plan 04-01: Removed legacy carousel/media-type lists, imported 2.7.2 drawables and colors, rebuilt `HomeAlbumCard`.
  - Plan 04-02: 2.7.2 story row with blue rings, press-scale animation, and Playzen cross-promo circle.
  - Plan 04-03: Fullscreen Stories viewer with slow zoom (3000 ms), split-tap navigation, hold-to-pause, Edit tooltip, and ad outline placeholder.
  - Plan 04-04: Rebuilt Explore tab matching 2.7.2 sections; album selection mode with 5-action bottom bar and pin to top.
  - Plan 04-05: Change cover via shared `MediaPicker`, on-disk folder rename with progress dialog and atomic Room row migration, and `AlbumPropertyDialog`.
  - Plan 04-06: CodeRabbit review fixes (atomicity, accessibility semantics, localized cancel, selection generation tracking), unslop audit, checklist ticks (rows #8, #9, #15), Playzen brand-art listing, and on-device test album setup.

## Next step
- Complete Task 3 on-device verification for Phase 4 on Redmi M2006C3MII.
- Remove disposable test album `ZZPhase4Test` from `/sdcard/Pictures/` via ADB.
- Mark Phase 4 complete and kick off **Phase 5: Full-Screen Media Viewer and Video Player (VIE-01, VIE-02, VIE-03)**.

## How to read the reference app (source of truth)
See `tooling-setup.md`.
- **Reference APK:** `reference/gallery-2.7.2.apk`. It is git-ignored, so it exists only on this computer. The phone has 2.7.2 installed.
- **Decoded copies:** `%TEMP%/ref272/dec` holds the 2.7.2 smali and resources. `%TEMP%/ref267/dec` holds 2.6.7. They live in `C:/Users/megh/AppData/Local/Temp/`. If they are gone, re-decode with apktool (`C:/apktool/apktool.BAT d -f -o dec base.apk`).
- **Decompiling:** use apktool for smali, and aapt2 at `%LOCALAPPDATA%/Android/Sdk/build-tools/36.1.0/aapt2.exe` for `dump resources` to resolve `0x7f…` ids. jadx is NOT installed. REA's jadx backend is not configured, but its apktool backend works.
- `scripts/ref_res.py`:
  - `sync-strings` pulls the 2.7.2 translations for keys added to `values/strings.xml`.
  - `check-strings` and `check-colors` are parity gates.
  - Always run it as `python -I`.

## Rules the user set (follow them)
- **Coding style ("Ponytail"):** make the smallest complete change and reuse existing code and patterns. No speculative abstractions. New non-trivial logic gets one small unit test. Comment only the *why*.
- **Quality gate for every phase:**
  - `./gradlew.bat :app:testDebugUnitTest -q`
  - `./gradlew.bat :app:assembleDebug -q`
  - a CodeRabbit review (`coderabbit review --committed --base-commit <phase-start> --agent`). The free tier allows about 3 reviews per window, and more than 150 files gets split with `--dir`.
  - an unslop audit of the changed screens
- **Commit message trailer:** every commit message ends with `Co-Authored-By: Claude Opus 5.5 <noreply@anthropic.com>`. This was the convention so far, so adjust it if you sign your own commits. Only commit or push when the user asks; the user usually says "Commit and push".
- **Parity rule:** where 2.7.2 leaves a string in English, ours stays English.
- **Brand art:** the reference brand art (launcher icons, splash, about logo, `app_icon_lottie`, promo photos) is kept for now by user decision. It must be replaced before any store build; this is tracked in STATE.md.
- **Ads and billing:** stay placeholders. Usage-access prompts copy the UI only and send no data.
- **Talking to the user:** short and plain. Lead with the outcome, and confirm before anything destructive. Never tap Install on reference-app ads.

## Device testing
- **Phone:** Redmi M2006C3MII, Android 11, wireless adb (`adb devices`). It runs **MIUI with system dark mode**.
- **Our app:** debug build installed. `adb install -r app/build/outputs/apk/debug/app-debug.apk` works because it uses the same debug key. **Then run `adb shell cmd package compile -m speed-profile -f com.quizango.gallery`**: otherwise every launch takes about 10 s with a black screen, because ART verifies uncompiled debug dex. Store installs are compiled at install time. Launch with `adb shell monkey -p com.quizango.gallery -c android.intent.category.LAUNCHER 1`. The first launch after an install takes about 10 s.
- **Commands:**
  - screenshots: `adb exec-out screencap -p > x.png`
  - Git Bash needs `MSYS_NO_PATHCONV=1` for `/sdcard` paths
- **Leftover test data:** the folder `Pictures/ZZDupTest` on the phone is test data from the duplicate check. The user can delete it.
- **Artemis MCP:** the Android automation tool, planned for Phase 13 side-by-side verification.

## Deferred items (already assigned to later phases)
- **Phase 3:** the All Media photo grid draws in only half the screen width. Also, "No Albums Found" flashes during the first load.
- **Phase 6:** the hard-coded "N freed" text and the `sp` sizes on the duplicate delete-complete screen.
- **Phase 13:** clipping and RTL checks in ar, ru and ta.
- **Phases 2-9:** about 175 UI labels are hard-coded in English. Each screen phase switches its labels to reference string resources.
- **Settings and floating screens:** the in-app Dark theme now reaches XML through AppCompat DayNight. Re-check it when porting those screens.

## Gotchas learned
- **GSD worktree executors** are created from the last **pushed** commit. Start each one with `git merge --ff-only main`, then copy `local.properties` (untracked, holds `sdk.dir`) into the worktree. Remove leftover worktree folders with a `\\?\` path, because of Windows path-length limits.
- **Android 11 drawable crash:** it rejects `<item android:drawable="@null"/>` in layer-lists, which is why `splash_bg` was edited. Imported vectors with aapt-split gradients (`$name__0.xml`) must be inlined.
- **Theme application:** do not re-apply the theme on every activity start, because it causes an AppCompat recreate loop. It is applied once in `GalleryApplication.onCreate`.
