# Tooling setup: reading the reference app

How to get readable code and resources from the reference app (`albums.gallery.photo.folder.picasa.app.web.gallery`) when checking parity.

## What is set up

| Tool | Status | Notes |
|---|---|---|
| REA MCP, apktool backend | Works | `C:\apktool\apktool.BAT`, version 2.12.1, found on PATH. Decodes resources and bytecode (smali). |
| REA MCP, jadx backend | Not set up | `search_android_classes`, `inspect_android_package` and other jadx tools fail until `REA_JADX_MCP_JAR` points to a `jadx-headless-mcp` 0.7.1 JAR (plus a JDK). REA does not download it. |
| aapt2 | Works | `%LOCALAPPDATA%\Android\Sdk\build-tools\36.1.0\aapt2.exe`. Resolves resource ids (strings, integers, dimens, drawables). |
| adb | Works | Phone connected over wireless debugging. |

## Which reference version

- `gallery.apk` in this repo is version 2.6.7, the version the clone was built from.
- The phone has version 2.7.0, which is what manual testing compares against. Its design differs in places (Explore tab, bottom bar, Albums tab).
- Pull the version on the phone when an issue comes from comparing on the device.

## Steps (Git Bash)

1. Pull the installed APK from the phone:
   ```bash
   mkdir -p "$TEMP/ref270" && cd "$TEMP/ref270"
   for p in $(adb shell pm path albums.gallery.photo.folder.picasa.app.web.gallery | sed 's/package://' | tr -d '\r'); do MSYS_NO_PATHCONV=1 adb pull "$p"; done
   ```
2. Decode bytecode only (fast), then resources only:
   ```bash
   /c/apktool/apktool.BAT d -f -r -o smali base.apk     # smali code
   /c/apktool/apktool.BAT d -f -s -o resdec base.apk    # res/ XML: layouts, drawables, styles
   ```
3. Dump the resource table so ids in smali (`0x7f13....`) can be looked up:
   ```bash
   "$LOCALAPPDATA/Android/Sdk/build-tools/36.1.0/aapt2.exe" dump resources base.apk > res270.txt
   grep -A1 "resource 0x7f13094f" res270.txt     # -> string/recent "Recent"
   ```
4. Find code by the strings or ids a screen uses, for example `grep -rl '"STORIES"' smali/`.

App code is under `smali/smali/albums/gallery/photo/folder/picasa/app/web/gallery/`. Class and method names are obfuscated, but Android, Kotlin and model calls (`getLastDate`, `Calendar.add`, `subList`) stay readable.

## Notes from use

- **Home stories:** the rules are in `presentation/home/viewmodel/HomeViewModel.smali` (`OooOoO0` builds the row), and the row labels are in `bw/j.smali`. They are mirrored in `app/src/main/java/com/quizango/gallery/ui/discovery/StoryBuilder.kt`.
- **Story screen:** the layout is `res/layout/activity_story_view_photo.xml`. Its icons (`ic_story_close`, `ic_share_story`, `ic_edit`) are copied into `app/src/main/res/drawable/`.
- **Screen comparison:** `adb exec-out screencap -p > file.png`. The reference shows full-screen ads: close them, and never tap Install.
