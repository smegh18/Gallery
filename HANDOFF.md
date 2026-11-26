# HANDOFF: Gallery app, milestone v2.0 (reference 2.7.2 parity)

Updated 2026-11-25 by Claude Code. The session was paused mid-work so it can continue on another computer.

## What this project is
- Android gallery app in Kotlin, Jetpack Compose and Material 3, single module `app`, package `com.quizango.gallery`. Repo `E:/Gallery`, branch `main`, remote `https://github.com/smegh18/Gallery.git`.
- It rebuilds the reference app `albums.gallery.photo.folder.picasa.app.web.gallery`. The goal is **100% parity with reference 2.7.2**: every screen, flow, animation and behaviour.
- Planning uses GSD in `.planning/`: `ROADMAP.md` (13 phases), `REQUIREMENTS.md`, `STATE.md`, and `milestones/v2.7.2-parity/PARITY-CHECKLIST.md` (53 rows, each with Ported and Verified boxes).

## Active goal (user instruction, 2026-11-25)
"Skip artemis check and complete all rest of work." So: no Artemis or device checks. Finish Phases 8-12 (porting), then run Phase 13's quality gates without Artemis. Device acceptance (VER-01, VER-02, every Verified box) stays open for a later device session.

## Where things stand (2026-11-26)
- **Phases 1-7:** complete (6-7 on automated gates; device pass deferred to Phase 13).
- **Phases 9, 10, 11, 12:** built by parallel builder agents, merged into main and wired together (Settings rows to Floating/Gs/IU settings, Settings usage switch to the usage-access sheet, Home start order: notification ask, then usage check, then floating offer). Each has `.planning/phases/NN-*/NN-SUMMARY.md`. 385 unit tests and assembleDebug passed at the Phase 10 merge (`1a183ae`).
- **Review of 9-12:** CodeRabbit (2 runs) plus a nine-lens multi-agent review with 3 skeptics per finding. CodeRabbit findings: CR9-1 fixed, CR9-2 resolved by the wiring, CR10-1 fixed, CR10-2 and CR10-3..14 rejected (2.7.2 shows no app label; translations are 2.7.2's own text). The workflow confirmed 25 findings (full JSON copied to `.planning/phases/12-usage-access-and-cleanup/REVIEW-9-12.json`), including 2 critical security bugs (the exported MainActivity followed any `gs_route` extra, so the vault could be opened without the PIN) and 2 critical crashes (painterResource on shape drawables in Pro and Settings). All 25 are fixed (gates passed); CodeRabbit has not re-reviewed the fixes.
- **Phase 8 (Create and Edit):** its builder was still working at the pause. Its partial work is pushed on branch `phase-08-wip` (see `.planning/phases/08-create-and-edit/08-SUMMARY.md` on that branch). Finish it, merge into main (expect conflicts in strings/colours/NavHost/Screen; keep both sides, drop identical duplicates, regenerate translations with sync-strings), then review it the same way.
- **Left after that:** CLR-01 cleanup sweep (script idea: list res files nothing references, rerun after Phase 8), Phase 13 gates without Artemis, then update ROADMAP/REQUIREMENTS/PARITY-CHECKLIST (rows #10-#12, #38-#53 Ported) and STATE.
- Decisions to confirm with the user: usage-access prompts are ON (`USAGE_ACCESS_RC_ENABLE`; 2.7.2 offline defaults are off); Quick Actions notification repeats every 600 min (`INTERVAL_MINUTES`; 2.7.2 bundled default 0 = off).

## Next steps
1. Finish the review fixes, then Phase 8, merge, review.
2. CLR-01 sweep, gates (tests, assembleDebug, check-strings, check-colors, CodeRabbit), docs, commit, push.

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
