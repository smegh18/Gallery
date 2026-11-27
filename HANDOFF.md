# HANDOFF: Gallery app, milestone v2.0 (reference 2.7.2 parity)

Updated 2026-11-25 by Claude Code. The session was paused mid-work so it can continue on another computer.

## What this project is
- Android gallery app in Kotlin, Jetpack Compose and Material 3, single module `app`, package `com.quizango.gallery`. Repo `E:/Gallery`, branch `main`, remote `https://github.com/smegh18/Gallery.git`.
- It rebuilds the reference app `albums.gallery.photo.folder.picasa.app.web.gallery`. The goal is **100% parity with reference 2.7.2**: every screen, flow, animation and behaviour.
- Planning uses GSD in `.planning/`: `ROADMAP.md` (13 phases), `REQUIREMENTS.md`, `STATE.md`, and `milestones/v2.7.2-parity/PARITY-CHECKLIST.md` (53 rows, each with Ported and Verified boxes).

## Active goal (user instruction, 2026-11-25)
"Skip artemis check and complete all rest of work." So: no Artemis or device checks. Finish Phases 8-12 (porting), then run Phase 13's quality gates without Artemis. Device acceptance (VER-01, VER-02, every Verified box) stays open for a later device session.

## Where things stand (2026-11-27)
- **Phases 1-12: complete on main.** All 53 parity-checklist rows are Ported; none Verified. Phases 8-12 were built by parallel builder agents in worktrees and merged; each has `.planning/phases/NN-*/NN-SUMMARY.md` with its deviations.
- **Quality gates (without Artemis):** unit tests (~397), assembleDebug, check-strings, check-colors pass. CodeRabbit ran on every phase; a nine-lens multi-agent review of Phases 9-12 confirmed 25 findings (incl. a vault PIN bypass via the exported MainActivity's `gs_route` extra and two crashes), all fixed (`.planning/phases/12-usage-access-and-cleanup/REVIEW-9-12.json`). CLR-01 sweep removed 100 unreferenced resources.
- **Open:** the device pass (Phase 13: VER-01/VER-02, the PENDING-DEVICE matrices in 06/07-LIVE-COMPARISON.md), and two user decisions: usage-access prompts ON (`USAGE_ACCESS_RC_ENABLE`; 2.7.2 offline default off) and the Quick Actions notification every 600 min (`INTERVAL_MINUTES`; 2.7.2 bundled default 0 = off).

## Next steps
1. Connect the phone and run the device pass with Artemis (Phase 13), fixing what differs.
2. Before a store build: replace the reference brand art (STATE.md list).

## Setup on a new computer
- Install JDK 17 or later, Android SDK 36 with build-tools 36.1.0 (for aapt2), Python 3, apktool 2.12.x, adb, and the CodeRabbit CLI (then `coderabbit auth login`).
- `local.properties` is untracked. It needs `sdk.dir=<Android SDK path>` and, optionally, `MAPS_API_KEY=` for the Place map.
- **The reference APK is not in git.** `reference/` is git-ignored, so copy `reference/gallery-2.7.2.apk` (41 MB) from the old computer, or pull it from the test phone (tooling-setup.md, step 1). `gallery.apk` (2.6.7) is tracked.
- Decode both versions in Git Bash. The scripts read these default paths:
  ```bash
  mkdir -p "$TEMP/ref272" && cp reference/gallery-2.7.2.apk "$TEMP/ref272/base.apk"
  (cd "$TEMP/ref272" && apktool d -f -o dec base.apk && "$LOCALAPPDATA/Android/Sdk/build-tools/36.1.0/aapt2.exe" dump resources base.apk > res272.txt)
  mkdir -p "$TEMP/ref267" && cp gallery.apk "$TEMP/ref267/base.apk"
  (cd "$TEMP/ref267" && apktool d -f -s -o dec base.apk)
  ```
- **Then fix the tab spelling, or `check-strings` falsely reports 20 languages as stale.** A fresh apktool decode writes a backslash followed by a real TAB where our files have `\t`:
  ```bash
  python -I -c "
  import glob, os
  for p in glob.glob(os.path.join(os.environ['TEMP'], 'ref272/dec/res/values*/strings.xml')):
      s = open(p, encoding='utf-8').read()
      open(p, 'w', encoding='utf-8', newline='').write(s.replace(chr(92) + chr(9), chr(92) + 't'))
  "
  python -I scripts/ref_res.py check-strings   # expect: strings: 24 languages up to date
  python -I scripts/ref_res.py check-colors    # expect: colors: 226 light / 134 night names match 2.7.2
  ```
- Reference layout in the decode: app screens are under `smali/albums/gallery/photo/folder/picasa/app/web/gallery/presentation/<feature>/`, with obfuscated helpers in short-named sibling packages across `smali`, `smali_classes2` and `smali_classes3`. The library screens are `com/collage/activities`, `com/canhub/cropper`, `com/grow/common/utilities/subscription` (the PayWall) and `com/baselibrary`. Resolve hex resource ids with `res/values/public.xml` or `res272.txt`.

## Rules the user set (follow them)
- **Coding style ("Ponytail"):** make the smallest complete change and reuse existing code and patterns. Add no speculative abstractions. New non-trivial logic gets one small unit test. Comment only the *why*.
- **Quality gate for every phase (VER-03):** unit tests, `assembleDebug`, CodeRabbit with no open findings, and an unslop audit of the changed screens.
- **Commits:** commit or push only when the user asks. Every commit message ends with `Co-Authored-By: Claude Opus 5.5 <noreply@anthropic.com>`.
- **Parity rule:** where 2.7.2 leaves a string in English, ours stays English. Port strings by their reference keys, then run `python -I scripts/ref_res.py sync-strings` for the translations.
- **Ads, billing and usage data:** ads stay placeholders, there is no real billing, and the usage-access prompts copy the UI only and send no data. Reference brand art must be replaced before any store build (tracked in STATE.md).
- **Talking to the user:** short and plain. Lead with the outcome, and confirm before anything destructive.

## Device testing (for the later device pass)
- Phone: Redmi M2006C3MII, Android 11, MIUI with system dark mode, wireless adb. Reference 2.7.2 is installed on it.
- Install with `adb install -r app/build/outputs/apk/debug/app-debug.apk`, then run `adb shell cmd package compile -m speed-profile -f com.quizango.gallery`. Without the compile step, a cold start takes about 10 s.
- Destructive checks run only inside a recorded disposable fixture folder such as `Pictures/ZZPhase7Test-*`. The procedures are in `07-09-SUMMARY.md` and `06-11-PLAN.md`.

## Gotchas learned
- **Android 11 drawable rules:** it crashes on `<item android:drawable="@null"/>` in layer-lists, and imported vectors with aapt-split gradients (`$name__0.xml`) must be inlined.
- **Theme:** the theme is applied once in `GalleryApplication.onCreate`. Re-applying it on every activity start causes an AppCompat recreate loop.
- **GSD worktree executors** start from the last pushed commit. Run `git merge --ff-only main` in each one and copy `local.properties` into it.
- **Disk space:** the old computer's C: drive had only about 3 GB free (Gradle caches and `%TEMP%` decodes live there).
