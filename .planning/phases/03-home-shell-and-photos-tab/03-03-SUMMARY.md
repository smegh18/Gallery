---
phase: 03-home-shell-and-photos-tab
plan: 03
subsystem: ui-home
tags: [compose, selection-mode, delete-dialog, nested-scroll, parity-2.7.2]
requires:
  - 03-01 (HomeShell.kt, HomeTopBar / HomeTabBar, Phase 3 strings / colours / drawables)
provides:
  - HomeSelection.kt (HomeSelectionTopBar, HomeSelectionBottomBar, DeleteMediaDialog, DeleteAction, deleteActionFor, MOVE_TO_RECYCLE_BIN_DEFAULT, Modifier.drawableBackground)
  - ui/components/AddToAlbumSheet.kt (shared by Home and AlbumDetail)
  - HomeViewModel.deleteSelected(action), addSelectedToFolder(path, onDone)
  - HomeShell.settledBarOffset, barSettleEasing, BAR_SETTLE_MS; HomeTabBar(modifier)
affects: [03-04, 03-06, phase 4 (Albums / Explore bottom padding under the overlaid bar), phase 5 (AlbumDetail port)]
tech-stack:
  added: []
  patterns: [pure internal mapping/easing helpers unit-tested, NestedScrollConnection-driven bar offset, XML shape/ripple drawables drawn via ContextCompat in drawBehind]
key-files:
  created:
    - app/src/main/java/com/quizango/gallery/ui/home/HomeSelection.kt
    - app/src/main/java/com/quizango/gallery/ui/components/AddToAlbumSheet.kt
  modified:
    - app/src/main/java/com/quizango/gallery/ui/home/HomeScreen.kt
    - app/src/main/java/com/quizango/gallery/ui/home/HomeShell.kt
    - app/src/main/java/com/quizango/gallery/ui/home/HomeViewModel.kt
    - app/src/main/java/com/quizango/gallery/ui/album/AlbumDetailScreen.kt
    - app/src/test/java/com/quizango/gallery/HomeShellTest.kt
decisions:
  - "deleteSelected is internal because DeleteAction is internal (Kotlin forbids a public function exposing an internal type)"
  - "Delete dialog: the whole checkbox row toggles (Role.Checkbox), because the reference checkbox has minWidth/minHeight 0 and would otherwise be a tiny touch target"
  - "Home Scaffold replaced by Column(top bar, clipped content box, navBarBg strip); the tab bar and selection bar overlay the pager"
status: complete
commits: 5
metrics:
  duration: ~45 min
  completed: 2026-11-15
actuals:
  tokens: 13500
  tasks: 3
  commits: 5
---

# Phase 3 Plan 3: Selection mode, delete confirmation and bottom-bar behaviour Summary

Long-press on Photos now shows the 2.7.2 selection top bar (close, "%d item(s) selected", Select all / Unselect all) and the six-action `select_album_bg` bottom sheet. Delete asks first through a port of dialog_delete, with Move to Recycle Bin checked by default. Add to opens a shared add-to-album sheet and copies the selection into the album, and Create album from the selection copies the selection into the new album. The tab bar slides away with the scroll and settles shown or hidden in a 220 ms ease-out-cubic animation, a thin colorPrimary line runs along the top while syncing, and a fixed home_shell_tab_bg strip sits behind the system navigation bar.

## Tasks

| Task | RED | GREEN | Result |
|------|-----|-------|--------|
| 1 Selection top bar and bottom bar | n/a | 5caf234 | HomeSelectionTopBar / HomeSelectionBottomBar (widths 50/55/55/63/50/60.sdp, 10.sdp elevation, horizontal scroll, borderless ripple, no icon cd); Share chooser title from `R.string.share`; old `SelectionActionItem` and GalleryTopBar use on Home removed |
| 2 Delete dialog, Add to, Create album | f19aa36 | c3b9277 | DeleteAction / deleteActionFor / MOVE_TO_RECYCLE_BIN_DEFAULT; DeleteMediaDialog from the 2.6.7 dialog_delete dump; `moveSelectedToTrash` replaced by `deleteSelected(action)`; `addSelectedToFolder`; AddToAlbumSheet moved to ui/components on `add_to_title` / `no_items` / `items`; AlbumDetail keeps its copy logic and toast |
| 3 Hide on scroll, sync line, nav-bar strip | fa34ef0 | 519fadc | settledBarOffset / barSettleEasing / BAR_SETTLE_MS; NestedScrollConnection (onPreScroll follows, onPostFling settles); snap back on tab change; pinned at 0 while selecting; 5.sdp colorPrimary sync line pulled up 3.dp |

## Verification (actual results)

- RED runs: both failed at `:app:compileDebugUnitTestKotlin` on unresolved references (`DeleteAction` / `deleteActionFor` / `MOVE_TO_RECYCLE_BIN_DEFAULT`; `settledBarOffset` / `barSettleEasing`).
- Grep gates: Task 1 `selection markers=8/8, hard-coded chooser title=0`; Task 2 `delete/add-to markers=10/10, old no-confirm trash fn=0`, and the moved sheet has no `"Add to"` / `"No albums"` literal; Task 3 `bar-behaviour markers=5/5, old blue sync bar=0`.
- `./gradlew.bat :app:testDebugUnitTest --tests "com.quizango.gallery.HomeShellTest" -q` exit 0 after each GREEN step. HomeShellTest: 11 tests, 0 failures.
- Final `./gradlew.bat :app:testDebugUnitTest :app:assembleDebug`: `BUILD SUCCESSFUL in 18s`, exit 0. Full unit suite: 127 tests, 0 failures, 0 errors. app-debug.apk rebuilt.
- Device: **not run.** The wireless Redmi (M2006C3MII) was connected and idle on the launcher. `adb install -r` failed with `INSTALL_FAILED_UPDATE_INCOMPATIBLE` because the build on the phone has a different signature. Installing would mean uninstalling first, which wipes the app's data on the phone, so I did not do it. 03-06 has to install it (uninstall first, or sign the build with the same key) before the backstop checks.

## Deviations from Plan

1. **[Rule 3 - Blocking] Reference decode not on this machine.** R272/S272 and E:/Gallery/local.properties are missing, and the worktree setup does not apply because this is a sequential run on main. I took the dialog_delete values from `aapt2 dump xmltree gallery.apk` (2.6.7; UI-SPEC section 3 says the layout is unchanged in 2.7.2) and resolved their IDs with `aapt2 dump resources`: card dialogBack r14.sdp; column paddingVertical 5.sdp; title BodyLargeMedium titleColor with margins 15.sdp and top 18.sdp; the recycle row has paddingHorizontal 10.sdp and a checkbox with marginStart 6.dp tinted fastScrollerColor, beside BodyMediumRegular text set to 2 lines with marginStart 5.sdp; the button row has marginTop 15.sdp, padding 26.sdp horizontal and 10.sdp vertical; No and Yes are BodyMediumSemiBold, weight 1, 5.sdp gap and 10.sdp vertical padding. layout_select_media_bottom was dumped the same way and matches the plan. The Android SDK is at D:/Android/Sdk, not at the ANDROID_HOME path.
2. **[Rule 3] `deleteSelected` is `internal`.** DeleteAction is internal, as the plan asks, and a public function cannot expose it. The grep gate `fun deleteSelected(` still matches.
3. **Delete dialog checkbox row.** The whole row is `toggleable(role = Checkbox)` and the Checkbox itself has `onCheckedChange = null` with the minimum interactive size at 0 (the reference has minWidth/minHeight 0). So tapping the "Move to Recycle Bin" text also toggles it. In the reference only the box toggles. I made this choice for accessibility. Revert it if strict parity matters more.
4. **Ripple drawables.** `help_delete_cancel_bg`, `btn_permission` and `select_album_bg` are drawn with ContextCompat in `drawBehind` (new `Modifier.drawableBackground` in HomeSelection.kt). A RippleDrawable drawn this way only paints its static layer, so the press ripple comes from Compose `clickable`, clipped to a pill shape.
5. **Sync line track colour.** `trackColor = colorPrimary @ 0.3 alpha`, which approximates the framework's indeterminate horizontal track (colorControlActivated at disabledAlpha). I did not check this against a 2.7.2 screen. Compare it in 03-06.
6. **Root layout.** HomeScreen's Scaffold is now a Column, so the system-bar handling moved to the bars: the top bar keeps `statusBarsPadding` and the navBarBg strip has `navigationBarsPadding`. The Scaffold's horizontal system-bar insets (landscape side navigation bar) no longer apply.

## Notes for 03-06 device comparison

- Bar offset: `barOffset = (barOffset - dy).coerceIn(0, tabBarHeight)` on every nested pre-scroll (drag and fling), and nothing is consumed. onPostFling starts the settle, and it runs after every drag release, including zero-velocity ones. A new scroll cancels a settle that is still running.
- Albums and Explore have no bottom padding for the overlaid bar yet (Phase 4). A short, non-scrolling Albums page can leave its last row under the tab bar.
- The selection bar is not translated. In selection mode the offset is forced to 0 and scrolls are ignored.

## Known Stubs

None.

## Threat Flags

None new. T-03-06: every Home delete goes through DeleteMediaDialog, the default is Move to Recycle Bin, No and outside tap delete nothing, and the no-confirmation function is gone. T-03-07: the existing trim-and-ignore-blank rule and `File(Pictures, name)` are unchanged. The rule does not reject `..` or `/` in the name, so a name like `../x` creates the folder outside Pictures. This was already true before this plan, and the plan said to keep the rule as is. Worth tightening when the create-album dialog is ported.

## Self-Check: PASSED

- HomeSelection.kt, AddToAlbumSheet.kt and this SUMMARY are present. Commits 5caf234, f19aa36, c3b9277, fa34ef0 and 519fadc are on main.
- STATE.md and ROADMAP.md were not touched.
