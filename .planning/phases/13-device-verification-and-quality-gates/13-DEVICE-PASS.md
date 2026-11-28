# Phase 13 device pass (session 1, 2026-11-27)

Device: Redmi M2006C3MII (`cattail`), Android 11, MIUI, wireless adb. Our debug build installed fresh (the old copy had a different signature, so it was uninstalled with the user's OK; its data and vault were wiped). Driven with adb screenshots, UI dumps and taps; Artemis was ready but used only for a probe (Gemini free tier is capped at about 20 requests a day).

This was a **smoke pass of our app on the device** (does each new flow open, work and not crash), not the side-by-side light/dark comparison with 2.7.2 that ticks **Verified**. No Verified box was ticked.

## Results

| Area | What was run | Result |
|---|---|---|
| Onboarding (#1-#5) | Fresh install: Splash, Permission (All files access), Language, Feature promo; Enable features asked Phone then opened Display over other apps with the guide card | PASS |
| Home (#6, #7) | Home with the pinch tutorial; sessions counted | PASS |
| Collage (#40) | Explore > Collage opens the picker directly (no collage home), 2 photos > Add > collage screen with Layout/Ratio/Background/Border/Brush/Text > Save > saved screen with share targets; the file appears first in the picker | PASS |
| Photo editor (#38) | Explore > AI Editor > pick a photo | **FAIL, fixed**: every photo was refused as "Unable to edit corrupted image" (a `?.let { ...; null } ?:` bug turned every successful decode into the failure). Fixed in `5f219c9`; after the fix the editor opens with the 2.7.2 tool strip, a filter applies, Save writes `Image_<millis>.png` and MediaStore indexes it |
| Settings (#46) | Rows in 2.7.2 order, red dots, no crash (the review's red-dot crash fix holds) | PASS |
| Pro (#49) | Opens, no crash, "No Data Found" + REFRESH, no buy button | PASS |
| Quick Access (#11, #12) | Settings > Quick Access Settings: offer dialog when overlay is off, Allow > guide card > granted > Floating settings (Always ON / Always OFF). Home: edge handle over the launcher, two-step intro ("Put it where you like"), long-press drag then snap back to the edge, swipe opens the sidebar (Gallery, Private Vault, Favorite, Moments, Recycle Bin, Edit), Private Vault opens the vault lock (Set PIN), never the vault grid | PASS |
| Usage access (#6, USG-01) | Home prompt "Make Gallery better for you" on session 9 (also recorded on session 3); Not now dismisses without blocking Home | PASS |
| After-call Quick Info (#50) | Needs a real phone call to end | NOT RUN |
| Install/uninstall screens (#51) | Needs another app installed, updated or uninstalled on the phone | NOT RUN |

## Device notes
- MIUI reset the overlay permission (`appops SYSTEM_ALERT_WINDOW`) to "ignore" after each reinstall when it was granted with adb; granting it through the system toggle holds. The floating service then stops and the handle disappears, by design.
- MIUI's edge back-gesture zone swallows touches in the last ~12 px of the screen edge; the sidebar handle opens with a swipe starting inside the handle (about 20 px in), as a finger naturally does.
- `ComposeOverlay.add()` now logs a warning when an overlay window cannot be added (it failed silently before).

## Side-by-side with 2.7.2 (2026-11-27/28, Redmi M2006C3MII, 27 screens)
Both apps driven through the same 27 screens by script and captured at 720x1600; each pair compared by an agent and every reported difference re-checked by a second agent trying to refute it.
- **Light:** 2 of 27 matched at the start, 19 in the last full run; Pro, Quick Access, After Call, Install/Uninstall and Quick Organize matched in later re-checks.
- **Dark:** 13 of 27 on the first pass; the six screens fixed and re-checked all match except a 2px title offset on Quick Access.
- **Root cause found:** TextView draws `textSize` at whole pixels (`getDimensionPixelSize`), Compose kept fractions (9ssp = 21.6px). `Int.ssp` now rounds to whole px, which fixed narrow or 1px-low text on many screens at once.
- **Left on purpose:** 1px text offsets on a few titles (would move every screen), the story ring's anti-aliased edge, the Go Ads Free subtitle breaking one word later, the Pro note's uneven word gaps (2.7.2's JustifiedTextView), the dark Quick Access title (light matches with the same code), and the saved first-launch album order (`myAlbums` pref) not ported.
- **Not a difference:** the thin grey bar at x≈706-712, y≈705-860 on reference captures is 2.7.2's own floating Quick Access edge handle (overlay granted on the test phone), not a scrollbar.
- **Reference install:** the Play-pulled `gallery-2.7.2.apk` is a base split only and crashes at the splash; it runs with the `split_config.xxhdpi.apk` from a full 2.7.2 install (kept in `reference/2.7.2-splits/`, git-ignored). Play auto-update must be off or it moves to 2.7.5.

## Still open for Phase 13
- 16 rows ticked Verified in PARITY-CHECKLIST (2026-11-28). Still to compare side by side: the rest of rows 29-31 (Duplicates scan/delete, Recycle with items, Quick Organize past the intro), and rows not in the 27-screen run (onboarding 1-5, floating sidebar 11, video and story viewers 14-15, album and media lists 16-17, Animated/Burst 20-21, color search 24, other cleaners 25 and 27-28, vault 33-37, editor and creative tools 38-45, notifications 52-53).
- Quick Info after a real call; install/uninstall screens after a real install.
- The PENDING-DEVICE matrices in `06-LIVE-COMPARISON.md` and `07-LIVE-COMPARISON.md`.
