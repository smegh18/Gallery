# Phase 6 Live Comparison Matrix

Date prepared: 2026-11-23 19:20 Asia/Calcutta

This matrix is a test plan and evidence ledger. Automated gates have passed; device entries are intentionally marked **PENDING** until screenshots and observations are recorded on the authorized device with a disposable fixture root. No Phase 6 Ported or Verified checklist boxes have been changed.

## Shared gate evidence

- Full unit suite: PASS — 297 tests, 48 suites, 0 failures/errors (`:app:testDebugUnitTest -q`).
- Debug assembly: PASS (`:app:assembleDebug -q`).
- Strings: PASS — 24 languages current (`python -I scripts/ref_res.py check-strings`).
- Colors: PASS — 216 light / 129 night names match (`python -I scripts/ref_res.py check-colors`).
- Diff check: PASS (`git diff --check`).
- Device: authorized `M2006C3MII` is visible to ADB. No recorded disposable fixture path exists yet.

## Row and state matrix

| Checklist row | Reference states to compare | Automated evidence | Device evidence / current result |
|---|---|---|---|
| #25 System Cleaner | Entry toolbar/cards; row order Duplicate Images, Screenshots, Large Video; card and Scan Now routing | CleanerTest and integrated suite; build/resources PASS | PENDING — screenshots and taps not run |
| #26 Junk flow | scan start/progress/done; five result categories; default selections including unticked old documents; Clean vs Deep Clean; cleaning and cleared states; one disposable fixture only | JunkScannerTest, CleanerTest, RemoveJunk tests; integrated suite PASS | PENDING — disposable `.tmp` and old-document fixture not recorded; no cleanup run |
| #27 Large Videos | 70 MiB boundary; headers/cells; select, cancel, confirmation and result | LargeVideo tests and integrated suite PASS | PENDING — boundary fixtures and safe delete not run |
| #28 Screenshots | all four screenshot directory families; 4-column cells; selection/dialog and delete | Screenshot tests and integrated suite PASS | PENDING — directory-family fixtures not recorded |
| #29 Duplicate flow | waiting/done timing; grouped images; keep-one; confirmation; Recycle/permanent choice; process minimum; partial failures; complete count/bytes; route closure | DuplicateTest and integrated suite PASS | PENDING — disposable duplicate pair not recorded; no destructive action run |
| #30 Recycle | fresh default off; saved true/false; conditional day strip; viewer; restore; selected delete; empty all; Back trigger | RecycleTest and integrated suite PASS | PENDING — saved-value setup and disposable recycle media not exercised |
| #31 Quick Organizer and Organizer Recycle | stripe visibility/close/tap; first-entry tutorial and resume; album create/move; same-folder refusal; left skip/up trash; undo; jump grid; owner-filtered bin; restore/delete; Back prompt | OrganizerTest, AlbumDetailTest, MediaRepositoryMutationTest and integrated suite PASS | PENDING — organizer state reset, fixture albums/media, screenshots and delete/restore not run |

## Required cross-cutting comparison states

| Dimension | Coverage | Status |
|---|---|---|
| Light theme | Rows #25–#31 reference screenshots and representative interactions | PENDING |
| Dark theme | Rows #25–#31 contrast, surfaces, icon tint, selection and dialogs | PENDING |
| Arabic RTL | Toolbar ordering, text wrapping, action alignment, grid order and dialogs | PENDING |
| Fresh install preferences | Recycle auto-delete off; organizer step 1; stripe visible; organizer defaults | Unit defaults checked where pure helpers exist; device reset PENDING |
| Saved preferences | Recycle saved on/off; tutorial resumption/complete; stripe dismissed; skip-tooltip behavior | Pure/unit coverage partial; device persistence PENDING |
| Small library | Empty/loading/populated transitions and selection reconciliation | Automated helper/screen tests PASS; device PENDING |
| Large library | Scroll, grid responsiveness, selection and pager stability | PENDING |
| Destructive flows | Clean, permanent delete, recycle, restore, organizer bin delete | Repository outcomes tested; device confinement to fixture root PENDING |

## Fixture and evidence ledger

- Required root: `Pictures/ZZPhase6Test-<unique-id>` on the authorized test device.
- Recorded fixture root: **none yet**.
- Screenshot directory: **not created**.
- No `Clean`, permanent delete, restore, empty-bin or organizer delete action has been executed on the phone during this gate.
- Before any destructive confirmation, inspect every selected target and prove its path is under the recorded fixture root. If any target is outside, cancel and stop.

## Acceptance status

Automated gates are green. The blocking human device checkpoint remains open. After the matrix passes with captured screenshots/timings and bounded destructive fixtures, mark only numeric rows #25–#31 as Ported `[x]`; leave Verified `[ ]`. Until then, leave the parity checklist unchanged.
