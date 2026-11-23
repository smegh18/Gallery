---
phase: 07-private-vault
plan: 07
subsystem: ui
tags: [compose, vault, picker, activity_select, parity, requirements]
requires:
  - phase: 07-02
    provides: rollback-safe moveToVault and MediaStore re-index
provides:
  - VaultAddMediaScreen ported to activity_select (InternalSelectMediaActivity TYPE=2, #37)
  - togglePicked / pickerBarsVisible / pickerAddEvent pure helpers with tests
  - VLT-02 wording corrected to activity_select in REQUIREMENTS, ROADMAP and checklist row 37 (D1)
affects: [07-08, 07-09, 08]
status: complete
actuals:
  tokens: 7000
  tasks: 3
  commits: 3
tech-stack:
  added: []
  patterns: [selection kept as an ordered list of paths plus a path-to-item map so it survives folder switches]
key-files:
  created:
    - app/src/test/java/com/quizango/gallery/VaultPickerStateTest.kt
  modified:
    - app/src/main/java/com/quizango/gallery/ui/vault/VaultAddMediaScreen.kt
    - app/src/main/java/com/quizango/gallery/ui/browse/SharedMediaList.kt
    - app/src/main/java/com/quizango/gallery/ui/home/HomeSelection.kt
    - .planning/REQUIREMENTS.md
    - .planning/ROADMAP.md
    - .planning/milestones/v2.7.2-parity/PARITY-CHECKLIST.md
key-decisions:
  - "The folder row replaces layout_toolbar entirely (no 'Choose media' title); only the Add button and the tray toggle at count > 0, per the planner's smali trace (supersedes UI-SPEC section 6)"
  - "All Photos in the folder list counts photos plus videos and clears the folder filter, matching the old 'All Photos' menu entry"
requirements-completed: [VLT-02]
---

# Phase 7 Plan 07: Add Items picker Summary

The vault's Add Items screen is now 2.7.2's `activity_select`: a folder row (Back, folder name, arrow, Add), a 4-column `item_grid_gallery` grid with check circles, the folder list, a selection tray and the reference empty state, and Add hides the batch through 07-02's safe order. VLT-02's text now names the right layout everywhere.

## Commits
- b01c8d2 feat(07-07): vault Add Items tracer - activity_select folder row, 4-column grid, safe batch hide (Task 1, tracer, with VaultPickerStateTest)
- ffc3386 feat(07-07): picker folder list, selection tray, empty state and Back order (Task 2)
- ec9cdf0 docs(07-07): correct VLT-02 to InternalSelectMediaActivity / activity_select (D1) (Task 3)

## Verification (run on this machine)
- Tracer gate (before Task 2): `./gradlew.bat :app:testDebugUnitTest --tests "com.quizango.gallery.VaultPickerStateTest" -q` -> exit 0 (3 tests); `./gradlew.bat :app:assembleDebug -q` -> exit 0. Tracer verified, expansion proceeded. (First test run failed to compile on the `collectAsState<...>` type arguments; fixed before the commit.)
- Task 2: `./gradlew.bat :app:assembleDebug -q` -> exit 0 (after adding the `lazy.items` import).
- Final tree: `./gradlew.bat :app:testDebugUnitTest --tests "com.quizango.gallery.VaultPickerStateTest" -q` -> exit 0; `./gradlew.bat :app:assembleDebug -q` -> exit 0; `./gradlew.bat :app:testDebugUnitTest -q` (whole suite) -> exit 0.
- Task 3: the plan's python D1 verify script prints OK (VLT-02 line, ROADMAP criterion 4, row 37 with both boxes still unchecked).
- Acceptance greps: banned patterns (TopAppBar / DropdownMenu / Icons. / raw .sp / literal "All Photos") 0; `GridCells.Fixed(4)` 1; `syncMediaStore()` 1; `selected_media_files_successfully_hide` 1; reference drawables 6 (>= 6); reference colours 4 (>= 4); `no_items|no_media_file|items_selected` 3 (>= 3); `BackHandler` 3 (>= 1).

## Behaviour now
- Toolbar: only the folder row (56.dp + status inset, padding 4.sdp): `ic_back` (35.sdp), folder title (BodyLargeSemiBold 14.ssp, one line, ellipsis), a 20.sdp colorPrimary spinner until the albums query returns, then `ic_title_arrow_up` (`_down` while the list is open), and the Add pill (`bg_rounded_red` tinted colorPrimary, 10.ssp onlyWhite) only with at least one pick. 1.sdp `toolbar_bottom_border` underneath.
- Grid: 4 columns, padding 2/5/2/20.sdp, marginTop 2.sdp; every cell shows the check circle (`ic_check` / `ic_un_check`), video duration row and GIF/RAW/SVG label (the shared cell).
- Folder list: replaces the grid on `imagePickerFolderBGColor`; "All Photos" first, then device folders and Videos, with cover (60.sdp, radius 8.sdp), name, count and `ad_ic_right_arrow`. Choosing a folder closes the list and keeps the selection (picked items are remembered by path).
- Tray (only with a selection): `imagePickerSelectionsBGColor`; `ic_close` clears, "N item(s) selected", `ic_arrow_up` toggles a horizontal strip of 60.sdp thumbs, each with a black `bg_circle` badge holding a white `ic_close` that removes it.
- Empty folder: `ic_empty_img`, "No items", "No media files here." only after the first query has returned; before that the colorPrimary spinner layer covers the screen.
- Back: closes the folder list first, then leaves without hiding; swallowed while a hide is running.
- Add: with nothing picked, toast "Please select media."; else the spinner layer blocks input, `moveToVault` runs per picked item in tap order on the ordered snapshot, one `mediaRepository.syncMediaStore()`, toast "Media files hidden successfully." (or "An issue occurred!" when nothing was hidden), then back to the vault.

## Deviations from Plan

**1. [Rule 3 - Blocking/reuse] Shared helpers touched** - `SharedMediaCell` in `SharedMediaList.kt` changed from `private` to `internal` so the picker reuses the Phase 3/5 cell instead of a copy, and `drawableBackground` in `HomeSelection.kt` gained an optional `tint` (reference `backgroundTint`) so `bg_rounded_red` and `bg_circle` are drawn as the reference drawables (painterResource cannot load shape drawables). Neither file is in the plan's `files_modified`; both changes are source-compatible and the whole suite passes.

**2. Folder-list entry point** - the plan's Task 1 had no folder opening; it is added in Task 2 as planned. Tapping the title or the arrow opens the list (the loader is not tappable). The title is also tappable only once albums have loaded.

No other deviations.

## Decisions / accepted limits
- Each folder switch re-queries with a null initial value, so the loading layer shows briefly after choosing a folder (assumed to match the reference's loader; PENDING-DEVICE).
- The tray arrow does not rotate while the strip is open; the smali trace gave no rotation, so none was invented.
- The All Photos count is photos plus videos (the folder it opens shows both); cover is the first photo.
- `shortcut:` a batch that is interrupted (process death mid-hide) is covered by 07-02's per-file rollback; there is no batch-level undo.
- The plan's `requirements: [VLT-02]` checkbox in REQUIREMENTS.md is left unchecked (the 07-02 summary also claims VLT-02); ticking it is for the phase verification once the device checks run.

## Known Stubs
None.

## Pending device checks (all PENDING-DEVICE, no phone, Artemis down)
- Vault > Add Items, pick two disposable fixtures, Add: both appear in the vault and vanish from Photos at once (MIUI re-index).
- Folder-row look and visibility timing (row always visible, Add and tray only with a selection), 4-column grid, cell and tray look side by side with 2.7.2, light and dark (P4).
- Folder list open/close, switch folders with a selection in place, tray strip expand and remove, empty folder, Back order, long folder names, loader after a folder switch.

## Threat Flags
None. T-07-22 (ordered snapshot, 07-02 safe order) and T-07-23 (input-blocking layer, repository work on IO) are mitigated as planned; T-07-24 (scoped edits, boxes asserted unchanged) holds.

## Self-Check: PASSED
Commits b01c8d2, ffc3386, ec9cdf0 present; VaultPickerStateTest.kt and the modified files exist; no file deletions in any commit.
