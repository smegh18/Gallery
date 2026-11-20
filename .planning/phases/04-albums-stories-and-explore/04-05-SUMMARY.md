---
phase: 04-albums-stories-and-explore
plan: 05
subsystem: ui
tags: [compose, albums, rename, change-cover, properties, datastore, room]
requires:
  - phase: 04-04
    provides: AlbumSelectionBottomBar, HomeSelectionTopBar, selectedAlbums state
provides:
  - Change cover via shared MediaPicker and persisted albumCover preference
  - On-disk folder rename with RenameAlbumDialog, RenameProgressDialog, and Room transaction row rewrite
  - AlbumPropertyDialog and album info button in selection top bar
affects: [04-06]
tech-stack:
  added: []
  patterns: ["snapshot-then-transaction row migration with Room withTransaction", "shared MediaPicker component between Editor and Home cover selection", "NUL-separated Datastore key-value pairs for folder covers"]
key-files:
  created:
    - app/src/main/java/com/quizango/gallery/ui/components/MediaPicker.kt
    - app/src/test/java/com/quizango/gallery/AlbumEditTest.kt
    - app/src/test/java/com/quizango/gallery/RenameFolderTest.kt
  modified:
    - app/src/main/java/com/quizango/gallery/data/local/datastore/AppPreferences.kt
    - app/src/main/java/com/quizango/gallery/data/repository/MediaRepository.kt
    - app/src/main/java/com/quizango/gallery/GalleryApplication.kt
    - app/src/main/java/com/quizango/gallery/ui/home/HomeDialogs.kt
    - app/src/main/java/com/quizango/gallery/ui/home/HomeScreen.kt
    - app/src/main/java/com/quizango/gallery/ui/home/HomeSelection.kt
    - app/src/main/java/com/quizango/gallery/ui/home/HomeViewModel.kt
    - app/src/main/java/com/quizango/gallery/ui/editor/PhotoEditorScreen.kt
    - app/src/main/res/values/strings.xml (and 24 localized variants)
decisions:
  - "Dimmed Rename and Change cover still act on the first selected album when several are selected, matching reference 2.7.2 behaviour (AF.smali get(0))"
  - "Rename folder rewrites all rows in one atomic Room transaction preserving favorites and deletedDate flags before deleting old paths"
metrics:
  duration: ~60 min
  completed: 2026-11-20
status: complete
actuals:
  tasks: 3
  commits: 3
---

# Phase 4 Plan 05: Change cover, Rename and Album Info Summary

Album selection parity (checklist #8 / ALB-01) is completed: Change cover through a shared in-app MediaPicker with persisted cover override, on-disk folder rename with progress dialog and atomic Room database row migration, and the top-bar album info details dialog.

## What was built

- **Task 1 (ab91154):**
  - Moved private photo picker out of `PhotoEditorScreen.kt` into shared `ui/components/MediaPicker.kt`.
  - Added `albumCovers` Flow and `setAlbumCover` in `AppPreferences` using reference key `albumCover`.
  - Extended `HomeViewModel.albums` flow via `withCovers` to override `Album.coverPath`, `coverId`, and `isVideoCover` while the cover file remains in the folder.
  - Added `changeCoverTarget()` and `setSelectedAlbumCover()` in `HomeViewModel`; wired Change cover in `HomeScreen` to launch `MediaPicker`.
  - Added unit test cases in `AlbumEditTest.kt`.
- **Task 2 (30d7264):**
  - `MediaRepository.renameFolder` and `renameFolderRows`: snapshots database rows before `File.renameTo()`, rewrites all rows (subfolders, favorites, Recycle Bin items) in an atomic Room transaction (`database.withTransaction`), deletes old rows, and triggers `MediaStoreScanner.scanFiles`.
  - Injected transaction runner in `GalleryApplication` backed by Room transaction.
  - Added `moveAlbumKeys` in `AppPreferences` to migrate `pinAlbum` and `albumCover` entries on rename.
  - `RenameAlbumDialog` per `dialog_rename.xml` with 2.7.2 validation rules (empty ignored, 50+ chars refused, existing sibling name refused case-insensitively).
  - `RenameProgressDialog` per `dialog_album_rename_progress.xml` showing `modifying_items`.
  - Created `RenameFolderTest.kt` verifying all 4 guard paths and tree row moves on a real temporary directory with in-memory fake DAO.
- **Task 3 (b8ebe83):**
  - Updated `HomeSelectionTopBar` in `HomeSelection.kt` to add `onInfo` button (`ic_property`, 32.sdp box, 7.sdp padding, borderless ripple, contentDescription `details`), with `infoAlpha` (set to 0f when multiple albums are selected).
  - Created `AlbumPropertyDialog` in `HomeDialogs.kt` matching `dialog_album_property.xml` (name, path, size formatted via `formatFileSize`, total files, last modified formatted as `dd/MM/yyyy hh:mm a`, and OK button).
  - Wired `onInfo` in `HomeScreen.kt` to display `AlbumPropertyDialog` for the single selected album.

## Reference findings & Parity decisions

- In 2.7.2 (`AF.smali ~12039-12180` and `~13727-13900`), tapping Change cover or Rename when multiple albums are selected acts on the first selected album (`get(0)`), even though the icons are dimmed (alpha 0.6). We preserve this exact parity.
- `AlbumPropertyDialog` uses `dialog_album_property.xml` labels and formats size with `Formatter.formatFileSize` and date with `SimpleDateFormat("dd/MM/yyyy hh:mm a", Locale.ENGLISH)` matching `oa/e.smali`.

## Gate results

- `./gradlew.bat :app:testDebugUnitTest -q`: 0 errors, all unit tests passed.
- `./gradlew.bat :app:assembleDebug -q`: clean compile, 0 errors.
- Automated pattern checks for `MediaPicker`, `withTransaction`, `renameFolder`, `ic_property`, `total_files`, and `AlbumPropertyDialog` all passed.
