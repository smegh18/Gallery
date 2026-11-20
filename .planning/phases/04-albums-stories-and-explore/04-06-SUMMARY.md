---
phase: 04-albums-stories-and-explore
plan: 06
subsystem: verification
tags: [coderabbit, unslop, audit, quality-gates, parity, adb, preferences]
requires:
  - phase: 04-01
  - phase: 04-02
  - phase: 04-03
  - phase: 04-04
  - phase: 04-05
provides:
  - Phase 4 quality gate verification (VER-03)
  - CodeRabbit review resolution and unslop audit documentation
  - PARITY-CHECKLIST rows #8, #9, and #15 ticked Ported
  - STATE.md brand-art list updated with ic_story_playzen and Playzen label
  - Device test harness with synthetic album ZZPhase4Test and reset first-run states
affects: [05-01]
tech-stack:
  added: []
  patterns: ["external python preferences protobuf helper for deterministic state resets", "TalkBack accessibility semantics for selectable cards", "transaction-isolated Room row migrations"]
key-files:
  created:
    - .planning/phases/04-albums-stories-and-explore/04-06-SUMMARY.md
  modified:
    - .planning/milestones/v2.7.2-parity/PARITY-CHECKLIST.md
    - .planning/STATE.md
    - app/src/main/java/com/quizango/gallery/data/repository/MediaRepository.kt
    - app/src/main/java/com/quizango/gallery/ui/components/MediaPicker.kt
    - app/src/main/java/com/quizango/gallery/ui/home/AlbumsTab.kt
    - app/src/main/java/com/quizango/gallery/ui/home/HomeViewModel.kt
decisions:
  - "CodeRabbit findings 1-4 fixed directly; findings 5-13 (translations in values-*/strings.xml) are byte-identical reference imports from 2.7.2 and kept for reference parity"
  - "All unslop audit findings for Phase 4 surfaces accepted as intentional 2.7.2 reference parity (colors, spacing, typography, layouts)"
metrics:
  duration: ~45 min
  completed: 2026-11-20
status: in-progress
actuals:
  tasks: 2 of 3 complete (Task 3 human checkpoint pending)
  commits: 3
---

# Phase 4 Plan 06: Quality Gates & Verification Summary

Phase 4 (Albums, Stories and Explore) quality gates have been executed: unit tests, `assembleDebug`, reference parity scripts, CodeRabbit review fixes, unslop audit, checklist updates, brand-art tracking, and on-device test setup.

---

## 1. CodeRabbit Review

Base commit: `2e7f4a1f09f128a8eacc1a713c46f426ce977b15` (Plan 04-01 start).
Initial review completed with 13 findings.

### Actionable Code Findings (Fixed in `96df88d` & `40b55ff`)
1. **`MediaRepository.kt` (line 69)**: Moved `dao.getAllMedia().filter { ... }` snapshot read inside the `inTransaction` block during `renameFolderRows` to ensure concurrent updates to favorites or deleted flags are captured atomically right before row replacement.
2. **`AlbumsTab.kt` (line 215)**: Added `Modifier.semantics { selected = selectionMark }` on `HomeAlbumCard` when `selectionMark != null` so TalkBack / accessibility services convey album selection state.
3. **`MediaPicker.kt` (line 50)**: Replaced hardcoded `"Back"` string with localized `stringResource(R.string.cancel)`.
4. **`HomeViewModel.kt` (line 308 & 321)**: Added `mediaSelectionGen` and `albumSelectionGen` generation tracking to prevent asynchronous copy/move/delete operations from clearing newer selections if selection was closed and reopened while an operation was in flight. Follow-up fix in `40b55ff` increments generation on every selection mutation (`toggleSelection`, `selectAll`, `toggleAlbum`, `selectAllAlbums`, `setTab`) to ensure partial selection changes during in-flight operations are also protected.

### Reference Translation Findings (Kept for 2.7.2 Parity)
Findings 5–13 suggested grammatical or contextual adjustments to translated strings in `values-*/strings.xml` (`pin`, `unpintotop`, `recent`, `screenshot`, etc. in Filipino, Persian, Marathi, Tamil, Urdu, Swedish).
- **Disposition:** Kept as-is for 2.7.2 parity.
- **Reason:** All localized strings are synchronized directly from reference 2.7.2 decompiled resources (`scripts/ref_res.py check-strings`). Modifying them would violate the project requirement of byte-identical parity with reference 2.7.2.

---

## 2. Unslop Audit (Static & Code Inspection)

Audited surfaces:
- **Albums Tab:**
  - Grid & Cards: 3-column album cards matching `item_home_album_card.xml` with 10.sdp corner radius, placeholder color, and badge overlays.
  - Deleted Album Card: `#66000000` dark dim overlay (`album_tab_deleted_dim`) with centered trash icon (`album_tab_ic_trash`).
  - Accepted Parity: Card aspect ratio (1:1), typography (13.ssp regular, 11.ssp subtitle), and layout strictly match 2.7.2.
- **Story Row & Playzen Circle:**
  - Story circles with 2.dp `#0066FF` ring stroke and 0.95f press-scale animation.
  - Playzen circle: reference cross-promo entry.
  - Accepted Parity: Playzen art tracked for pre-release replacement in `STATE.md`; layout and animations match 2.7.2.
- **Stories Viewer:**
  - Fullscreen zoom animation (3000 ms duration, 1.0f -> 1.08f scale), left/right split tap navigation, touch-and-hold pause.
  - Header: Close icon, date text, share action.
  - Edit Pill & Tooltip: `story_edit_bg` and `popup_stories_edit_bg` with `Got it` button.
  - Ad Step: 2.7.2 ad frame skeleton after 3rd photo.
  - Accepted Parity: All timing, layout, and overlay assets match 2.7.2.
- **Explore Tab:**
  - Hero banner: `#0066FF` / `#1F4FD6` gradient, clean disk space typography, "Clean Now" button.
  - Create section (AI Editor, Collage with red `#FF1414` "New" badge).
  - Library section (Vault, Favorites, Moments, Place).
  - Ad card placeholder.
  - Manage rows (Duplicate Finder, Quick Organize, Recycle Bin, Settings) with 1.dp dividers.
  - Accepted Parity: Layout hierarchy, tile dimensions, icon assets, and navigation routing match 2.7.2.
- **Album Selection:**
  - Selection top bar: close button, selection counter, select-all, and album property info button.
  - Album selection bottom bar: 5 actions (Pin to top, Delete, Add to, Change cover, Rename) with alpha 0.6 dimming when multiple albums are selected.
  - Dialogs: `RenameAlbumDialog`, `RenameProgressDialog`, and `AlbumPropertyDialog` (`dialog_album_property.xml`).
  - Accepted Parity: Actions and dialog layouts faithfully reflect 2.7.2.

---

## 3. Automated Gate Results

1. **Unit Tests & Compilation:**
   - `./gradlew.bat :app:testDebugUnitTest -q`: Passed (0 errors, 0 failures).
   - `./gradlew.bat :app:assembleDebug -q`: Clean build.
2. **Resource & Color Parity:**
   - `python -I scripts/ref_res.py check-strings`: "strings: 24 languages up to date".
   - `python -I scripts/ref_res.py check-colors`: "colors: 198 light / 125 night names match 2.7.2".
3. **Plan 04-01 Specific Resource & Removal Gates:**
   - Legacy carousel references in `AlbumsTab.kt` / `HomeScreen.kt`: 0.
   - Reference drawables byte-comparison (`ic_pin`, `story_edit_bg`, `popup_stories_edit_bg`, `btn_stories_edit_bg`, `ic_album_select_pinned`, `ic_album_select_cover`, `ad_icon_bg_stroke_1`, `ad_btn_bg_white`): 0 mismatches.
   - Checklist rows ported: 3/3 (Rows #8, #9, #15).
   - Brand art list: `ic_story_playzen` tracked in `STATE.md`.

---

## 4. Device Setup & First-Run State Preparation

- **Target Device:** Redmi M2006C3MII (`adb-QG55IJKZHQF6OF6L-aT7pYW._adb-tls-connect._tcp`), Android 11.
- **APK Installed & Compiled:**
  - `adb install -r app/build/outputs/apk/debug/app-debug.apk`: Success.
  - `adb shell cmd package compile -m speed-profile -f com.quizango.gallery`: Success.
- **Disposable Test Album (`ZZPhase4Test`):**
  - Generated 6 distinct solid-color PNGs in `/sdcard/Pictures/ZZPhase4Test/`.
  - Scanned into MediaStore via `MEDIA_SCANNER_SCAN_FILE`.
  - Confirmed 6 distinct rows present in MediaStore image provider.
- **Preferences Backup & Reset:**
  - Backup created at `C:/Users/megh/AppData/Local/Temp/gallery-prefs/backup.preferences_pb` (270 bytes).
  - First-run preferences reset via `prefs_pb.py`:
    `sessionCount=1 isStoriesEditToolTip=false collageMakerNewButtonExplore=false`
  - Read-back verified; app left force-stopped so launch 2 triggers the first-run tooltips and "New" badge.

---

## 5. Checkpoint Task 3: Side-by-Side Verification Instructions

Please launch the app on the connected test phone and verify against reference 2.7.2:

1. **Albums Tab (Launch 2):**
   - Story row: Recent, Memories, Playzen circles with `#0066FF` rings.
   - Tap and hold a circle: slight scale-down animation. Tap Playzen: no-op.
   - Grid: Recent, Favorite, All Video, Camera, Screenshots (if present), folders, Deleted last with dark overlay.
2. **Stories Viewer:**
   - Tap a story: slow zoom over 3000 ms, split tap (left=prev, right=next), long press pauses.
   - First photo: "Edit your photo" tooltip appears; tap OK.
   - After photo 3: ad outline card appears.
3. **Explore Tab:**
   - Free up space hero, Create (AI Editor, Collage with red "New" badge), Library, Manage rows.
   - Tap Collage: verify "New" badge dismisses on next visit.
4. **Album Selection (on `ZZPhase4Test` only):**
   - Long-press `ZZPhase4Test`: selection top/bottom bars appear.
   - Pin to top / Unpin: moves first with pin badge.
   - Change cover: launches picker; selected photo becomes cover.
   - Info icon: `AlbumPropertyDialog` details match.
   - Rename: renames to `ZZPhase4Test2` with progress dialog.
   - Delete: moves to Recycle Bin; empty from Recycle Bin.
5. **Theme & Locale:**
   - Dark mode: progress bar `#7A8389`, hero `#1F4FD6`.
   - German, Russian, Tamil: one-line ellipsis.
   - Arabic: RTL mirrored layouts.
