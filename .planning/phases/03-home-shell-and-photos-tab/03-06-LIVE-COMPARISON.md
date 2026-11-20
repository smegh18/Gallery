# Phase 3 live comparison report

Date: 2026-11-19

## Setup

- Same physical Redmi M2006C3MII, Android 11, display 720x1600.
- Reference: `albums.gallery.photo.folder.picasa.app.web.gallery`, version 2.7.2, versionCode 152.
- Debug build: `com.quizango.gallery`, versionName 2.7.0, versionCode 150.
- Both apps were viewed on the same phone. Comparison screenshots are under the Codex session's `device-comparison` directory.
- The reference app showed its first-run Quick Access Floating Button prompt; it was closed with its X. The optional feature was not enabled. The temporary system dark-mode change was restored to the original light setting.

## Screenshots

- Reference Photos, light: [screen.png](C:/Users/sankh/.codex/visualizations/2026/11/19/01a1f22c-1c12-7a52-a79f-5a14b88d2ed2/device-comparison/reference-photos/screen.png)
- Our Photos, light: [screen.png](C:/Users/sankh/.codex/visualizations/2026/11/19/01a1f22c-1c12-7a52-a79f-5a14b88d2ed2/device-comparison/our-photos-settled/screen.png)
- Reference Albums: [screen.png](C:/Users/sankh/.codex/visualizations/2026/11/19/01a1f22c-1c12-7a52-a79f-5a14b88d2ed2/device-comparison/reference-albums-capture/screen.png)
- Our Albums: [screen.png](C:/Users/sankh/.codex/visualizations/2026/11/19/01a1f22c-1c12-7a52-a79f-5a14b88d2ed2/device-comparison/our-albums/screen.png)
- Reference Explore: [screen.png](C:/Users/sankh/.codex/visualizations/2026/11/19/01a1f22c-1c12-7a52-a79f-5a14b88d2ed2/device-comparison/reference-explore/screen.png)
- Our Explore: [screen.png](C:/Users/sankh/.codex/visualizations/2026/11/19/01a1f22c-1c12-7a52-a79f-5a14b88d2ed2/device-comparison/our-explore/screen.png)
- Reference Photos, dark: [screen.png](C:/Users/sankh/.codex/visualizations/2026/11/19/01a1f22c-1c12-7a52-a79f-5a14b88d2ed2/device-comparison/reference-photos-dark/screen.png)
- Our Photos, dark: [screen.png](C:/Users/sankh/.codex/visualizations/2026/11/19/01a1f22c-1c12-7a52-a79f-5a14b88d2ed2/device-comparison/our-photos-dark/screen.png)
- Reference Photos, after scroll: [screen.png](C:/Users/sankh/.codex/visualizations/2026/11/19/01a1f22c-1c12-7a52-a79f-5a14b88d2ed2/device-comparison/reference-photos-scrolled/screen.png)
- Our Photos, after scroll: [screen.png](C:/Users/sankh/.codex/visualizations/2026/11/19/01a1f22c-1c12-7a52-a79f-5a14b88d2ed2/device-comparison/our-photos-scrolled/screen.png)
- Our Photos in German: [screen.png](C:/Users/sankh/.codex/visualizations/2026/11/19/01a1f22c-1c12-7a52-a79f-5a14b88d2ed2/device-comparison/our-photos-de/screen.png)
- Our Photos in Arabic: [screen.png](C:/Users/sankh/.codex/visualizations/2026/11/19/01a1f22c-1c12-7a52-a79f-5a14b88d2ed2/device-comparison/our-photos-ar/screen.png)
- Our Arabic selection bar: [screen.png](C:/Users/sankh/.codex/visualizations/2026/11/19/01a1f22c-1c12-7a52-a79f-5a14b88d2ed2/device-comparison/our-selection-ar/screen.png)
- Arabic selection bar after horizontal scroll: [screen.png](C:/Users/sankh/.codex/visualizations/2026/11/19/01a1f22c-1c12-7a52-a79f-5a14b88d2ed2/device-comparison/our-selection-ar-scrolled/screen.png)

## Findings

### Phase 3: Home shell and Photos

- **Matches observed:** Photos / Album / Explore tab order, tab labels, header titles, major toolbar actions, three-column photo grid, Today section, and tab bar placement are close on the same device. The shared photo thumbnails are largely in the same order. Dark mode keeps the title, grid, icons, and tab labels legible in both builds.
- **Candidate mismatch:** our Photos screen shows a blue double-arrow fast-scroll affordance at the right edge when idle and after a matching grid swipe. It is not visible in either reference Photos capture. Both builds scroll the grid and hide the bottom tab bar. The reference also showed a sticky bottom ad during scroll; that is intentionally outside the clone's ad scope.
- **Expected ad difference:** reference screenshots loaded Google and banner ads; our build shows the reserved top ad area as a blank skeleton and omits the reference's bottom banner. The project decision is to keep ad placeholders and not serve real ads.
- **Data caveat:** the apps do not report identical album counts/names (reference Recent 125, our All Photos 124; other folders also differ). This can reflect each app's media indexing/filtering; the screenshots do not establish a rendering defect.

### Phase 4 screens, outside this phase's acceptance

- Albums top bar and overall collection-grid structure are similar. The reference includes Recent / Favorite / All Video / Camera / Screenshots / ZZ0306Move / ZZDupTest / Download; our build shows All Photos / Videos / Camera / Screenshots / 0 / ZZDupTest / Gallery / Download / VideoDownloader. Counts and some thumbnails differ.
- Explore is visibly different: reference has Free up space, AI Editor / Collage, Library links (Vault / Favorites / Moments / Place), and Manage entries; our build has System Cleaner, Quick Organize, and Collage Maker. The roadmap assigns Albums and Explore content to Phase 4.

## Safe interactions performed

- Opened Photos overflow: Sort / Recycle Bin / Settings. Opened Sort and cancelled without applying a sort.
- Opened Create Album and cancelled without creating an album.
- Opened the vault lock through the title long-press and backed out without setting a PIN.
- Switched tabs by tap and by horizontal swipe; scrolled the grid. No photo was opened, moved, deleted, or edited.
- App locale checks: German, Russian, Tamil and Arabic. Home titles and bottom-tab labels remained on one line; the selection-count header remained on one line in all four. Arabic mirrored the header and tabs; horizontally scrolling its selection action bar revealed the clipped Create Album action. Selection mode was cancelled without activating an action. English was restored.

## Not verified

- A follow-up ARTEMIS Pro run (trace `4b84772c-5797-48ef-8b25-6468583987a2`) stalled before producing a plan or verification; it was stopped. Its first observed hierarchy was Android Overview, not Gallery. Gallery was relaunched with REA afterward.
- Source inspection confirms our Photos implementation handles pinch gestures and our Home Back handler presents the exit popup, but this is code evidence, not real-device verification or reference parity evidence.
- The live hierarchy while Gallery was foreground showed Photos header, 3-column grid and bottom tabs. It contained thumbnail filenames such as `resume_test29.png` through `resume_test16.png`; no media was opened or mutated.
- Selection, Favorite, Delete/Recycle Bin, Add to album/private, picker contents, pinch-column behavior, and exit behavior remain unverified on device. The reference-specific answers (exit popup versus direct exit, fast-scroll date bubble, Add to Private confirmation, and Add/Create Album picker parity) remain open.
- Delete/favorite/move/add-to-private actions were not invoked because no isolated synthetic test media was prepared. These require a synthetic item and must not be tested against personal photos.

## Overall

The Phase 3 Home shell and basic Photos layout are close. The persistent fast-scroll arrows are the one candidate Phase 3 visual discrepancy from this pass. Ads are an intentional project difference. The visible Albums and Explore content differences belong to Phase 4. Phase 3 remains in progress: a device pass must still verify exit behavior, date bubble behavior, picker parity, pinch columns, and safe selection actions; media-mutating outcomes require synthetic test media.

## Follow-up checkpoint attempt (2026-11-19)

- A fresh ARTEMIS diagnostic with end-to-end probe again passed required checks (5/5) and observed the authorized Redmi device. Two Pro runs and one short Flash run stalled at initialization with no execution steps; all were stopped. They are not counted as verification.
- REA relaunched Gallery following an Overview-screen escape and confirmed Photos / Today / 3-column grid / bottom tabs. English/light was restored and no selection was active. Fresh read-only capture: [gallery-home.png](evidence/gallery-home.png).
- No photo was opened, selected, edited, deleted, favorited, moved, shared, or added to the vault during this follow-up.
- Phase completion remains blocked on the plan's live phone checkpoint. Outstanding device comparisons: exit behavior in both apps; fast-scroll bubble/drag and pinch interaction; selection toolbar and delete confirmation; Add to Private confirmation; Add/Create Album picker parity; dark-mode selection/delete confirmation. Destructive outcomes must use synthetic test media.

## Direct ADB / UIAutomator follow-up (2026-11-19)

- ADB UIAutomator hierarchy and shell input were used after ARTEMIS task startup delays. The debug build's Home Back showed “Tap again to exit”; pressing Back dismissed it. On reference 2.7.2 Home, one Back press returned directly to our previously open Gallery task and no exit-popup text was present: reference behavior is direct exit, unlike the current build. Per the plan, carry this difference to Phase 13.
- After one ordinary vertical swipe in reference Photos, screenshot evidence showed the grid scrolled and the bottom tabs were hidden behind the sticky ad. No date bubble or fast-scroll thumb was visible. Screenshot: [reference-scrolled.png](evidence/reference-scrolled.png). This does not test dragging a fast-scroll thumb.
- Touchscreen calibration was read through `getevent -lp` (720×1600, multitouch slots 0–9), but Android denied raw `sendevent` with Permission denied; no raw touch was delivered. The temporary pinch script was removed from host and device.
- An experimental UI Automator pinch test compiled, but its first draft depended on existing phone filenames and a Compose implementation-class selector. CodeRabbit identified these as reliability risks, so the test and UI Automator dependency were removed before commit. Gradle's connected test had already failed with `INSTALL_FAILED_USER_RESTRICTED` installing the instrumentation APK; direct instrumentation could not start because the debug target package was missing, and MIUI also denied reinstalling that app. The temporary test package was removed. The phone currently has the reference app; the debug package is not installed for user 0.
- No photos or albums were selected or changed in these follow-up checks.

## Remaining Phase 3 device checks

- Pinch-to-change-columns on-device, fast-scroller drag behavior, selection toolbar and delete confirmation, Add to Private confirmation, Add/Create Album picker parity, and dark-mode selection/delete confirmation.
- Revisit automated pinch verification with a deterministic synthetic-media fixture and a device-verified accessibility target. MIUI currently returns `INSTALL_FAILED_USER_RESTRICTED` when installing the debug/test APKs. Do not exercise destructive actions on personal media; use a disposable synthetic image.
