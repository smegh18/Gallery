# Phase 6 narrow reference findings

Resolved from the locally decoded Gallery 2.7.2 APK on 2026-11-23. These findings close only the three implementation questions left open by `06-UI-SPEC.md`; the approved UI specification remains the visual and behavioral contract.

## Organizer intro persisted steps

Evidence: `presentation/organizer/view/OrganizerActivity.smali`, its click/gesture listeners `cr/e.smali`, `cr/c.smali`, `be/a.smali`, and the album adapter `cq/c.smali`.

- `organizerStepFlow` defaults to `1`; `7` means complete.
- Value `1` renders `clFirst` (hearts, `quick_oraganize`, ORGANIZE NOW). The close action writes `7` immediately and removes the overlay.
- ORGANIZE NOW writes `2` and renders `clTwo` (Tap to Create Album).
- Tapping Create Album writes `3` and opens the create-album flow. Value `3` is transitional rather than a distinct visible instruction.
- A successful album creation while the saved value is `3` writes `4` and renders `txtAlbumListTitle` (Tap to add photos in the album). At value `4`, the album adapter overlays `hand_click` on album position 1 until that album is tapped.
- Tapping the prompted album writes `5` and renders `clThree` (Swipe left to skip).
- Performing the left gesture writes `6` and renders `clFour` (Swipe up to move to recycle bin).
- Performing the up gesture writes `7` and removes the tutorial overlay.
- `isOrgSkipToolTip` has one decoded read and no decoded writer. It defaults false. When the album list otherwise contains only Create Album, false seeds the Photos and Family defaults; true suppresses those defaults. Organizer itself never changes the flag.

## All Photos organizer stripe routing

Evidence: `presentation/medialist/view/activity/MediaListActivity.smali`, stripe click listener `ck/a.smali`, close listener `be/a.smali`, Albums result handler `ca/at.smali`, and `presentation/home/view/activity/HomeActivity.smali`.

- Both stripe actions persist `isOrganizerStripeShown = true`.
- Close only hides the stripe.
- Stripe tap calls `setResult(0x309)` (decimal 777) and finishes MediaListActivity.
- The Albums result handler converts 777 into event `ga/ak`; HomeActivity receives that event and starts OrganizerActivity.
- The Compose equivalent is therefore direct navigation from the `filter:all_photos` stripe to `Screen.QuickOrganizer` after persisting dismissal. It does not need a generic AlbumDetail result API. Other album/filter lists never show the stripe.

## Junk Temp Files predicate

Evidence: the temp traversal launched by `presentation/systemcleaner/removejunk/viewmodel/RemoveJunkViewModel` through `dz/c.smali`; predicate discriminator 4 in `smali_classes3/.../dr/c.smali`; extension helper `smali_classes3/.../ex/j.smali`.

- The pass recursively traverses shared external storage and evaluates files only.
- It extracts the substring after the final `.` from the file name and accepts it only when that extension equals the lowercase string `tmp`.
- There is no age condition and no `.temp` alternative. With no lowercasing in the decoded helper/predicate, uppercase or mixed-case extensions do not match.
- The reconstruction must replace its current case-insensitive `.tmp` / `.temp` rule with this exact predicate. Tests must include accepted `a.tmp` and rejected `a.temp`, `a.TMP`, extensionless, directory, and Android-subtree cases.
