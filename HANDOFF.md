# HANDOFF: Gallery app, milestone v2.0 (reference 2.7.2 parity)

Updated 2026-11-25 by Claude Code. The session was paused mid-work so it can continue on another computer.

## What this project is
- Android gallery app in Kotlin, Jetpack Compose and Material 3, single module `app`, package `com.quizango.gallery`. Repo `E:/Gallery`, branch `main`, remote `https://github.com/smegh18/Gallery.git`.
- It rebuilds the reference app `albums.gallery.photo.folder.picasa.app.web.gallery`. The goal is **100% parity with reference 2.7.2**: every screen, flow, animation and behaviour.
- Planning uses GSD in `.planning/`: `ROADMAP.md` (13 phases), `REQUIREMENTS.md`, `STATE.md`, and `milestones/v2.7.2-parity/PARITY-CHECKLIST.md` (53 rows, each with Ported and Verified boxes).

## Active goal (user instruction, 2026-11-25)
"Skip artemis check and complete all rest of work." So: no Artemis or device checks. Finish Phases 8-12 (porting), then run Phase 13's quality gates without Artemis. Device acceptance (VER-01, VER-02, every Verified box) stays open for a later device session.

## Where things stand
- **Phases 1-7: complete.** Phases 6 and 7 were closed on 2026-11-25 on automated gates only. Their device matrices (`06-LIVE-COMPARISON.md`, `07-LIVE-COMPARISON.md`) stay PENDING-DEVICE and move to Phase 13. Checklist rows 25-37 are ticked Ported, not Verified (see the note above "Phase 7 known deviations" in the checklist).
- **CodeRabbit on Phases 6-7.** The CLI is installed now. Running `coderabbit review --committed --base-commit 04c2447 --dir app --agent` found 11 major issues: the expired-trash purge racing a restore, Organizer drag and tutorial bugs, the junk list capped at 200 and partial-clean handling, a Duplicate Delete Process pop race, and vault batch results being ignored, plus double-submit races in Forgot Password and PIN confirm. 10 are fixed. CR-6 was rejected because 2.7.2 opens Junk Cleared after every clean, and our port only reports the bytes it actually removed. Unit tests (346) and `assembleDebug` pass after the fixes. Details are in the "CodeRabbit review 2026-11-25" section of `06-GATE-FINDINGS.md` and `07-GATE-FINDINGS.md`. Two smaller issues the fixer noticed are still open, so they come first:
  - Vault Unhide still toasts success when only some items were restored, and it clears the whole selection. This is the same kind of bug as CR-10, in `ui/vault/VaultMediaScreen.kt`.
  - `syncMediaStore` writes back the `deletedDate` it read at the start of the sync without holding `trashLock`, so a restore during a sync can be overwritten. This is in `data/repository/MediaRepository.kt`.
  - CodeRabbit has not re-reviewed the fixes.
- **Phases 8-12: not started.** Research for all five began but was stopped at the pause before any file was saved, so it has to be redone. Write one file per phase: `.planning/phases/08-create-and-edit/08-RESEARCH.md`, `09-settings-and-system/09-RESEARCH.md`, `10-floating-sidebar/10-RESEARCH.md`, `11-after-call-and-install-screens/11-RESEARCH.md` and `12-usage-access-and-cleanup/12-RESEARCH.md`. The five are independent, so they can run in parallel. The brief for each: read the phase section in ROADMAP (including any "Carried in" items), its requirement IDs and checklist rows, and the decoded reference (below). Compare that with our code, then write sections 1-7: scope, reference contract per screen, ours versus reference, resources to import, pitfalls, plan breakdown with file ownership and waves, and assumptions.

## Next steps
1. Set up the new computer (next section) and check that the gates pass.
2. Finish any CodeRabbit finding marked open, then rerun the gates.
3. Research Phases 8-12 (see above), then plan and build them in order. For each phase:
   - run `./gradlew.bat :app:testDebugUnitTest -q`, `./gradlew.bat :app:assembleDebug -q`, `python -I scripts/ref_res.py check-strings` and `check-colors`;
   - run a CodeRabbit review (`coderabbit review --committed --base-commit <phase start> --dir app --agent`; the free tier allows 3 reviews per rolling hour) and an unslop audit of the changed screens;
   - tick Ported in PARITY-CHECKLIST.md, but not Verified.
4. Phase 13 without Artemis: confirm the VER-03 gates for every phase, then update ROADMAP, REQUIREMENTS and STATE.

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
