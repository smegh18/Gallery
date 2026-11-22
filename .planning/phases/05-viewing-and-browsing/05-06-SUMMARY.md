---
phase: 05-viewing-and-browsing
plan: 06
subsystem: ui
tags: [maps, play-services-maps, compose, location-permission, place]
requires:
  - phase: 05-05
    provides: AlbumDetail virtual filters, shared toolbar and card patterns
provides:
  - Place screen (MapActivity parity) with optional Maps key
  - filter:place:<key> media filter and pure place grouping contracts
  - Runtime fine/coarse location flow and viewer Show on map route
affects: [05-07, search]
tech-stack:
  added: ["com.google.android.gms:play-services-maps:20.1.0"]
  patterns: ["Manifest key from untracked local.properties, blank default", "Marker icons drawn with Canvas bitmaps"]
key-files:
  created:
    - app/src/main/java/com/quizango/gallery/ui/discovery/PlaceScreen.kt
    - app/src/test/java/com/quizango/gallery/PlaceScreenTest.kt
  modified:
    - app/build.gradle.kts
    - app/src/main/AndroidManifest.xml
    - app/src/main/java/com/quizango/gallery/ui/discovery/BrowseGroups.kt
    - app/src/main/java/com/quizango/gallery/data/repository/MediaRepository.kt
    - app/src/main/java/com/quizango/gallery/ui/home/ExploreTab.kt
    - app/src/main/java/com/quizango/gallery/ui/navigation/GalleryNavHost.kt
    - app/src/main/java/com/quizango/gallery/ui/navigation/Screen.kt
    - app/src/main/java/com/quizango/gallery/ui/viewer/ViewerDialogs.kt
    - app/src/main/java/com/quizango/gallery/ui/viewer/MediaViewerScreen.kt
    - app/src/test/java/com/quizango/gallery/MediaViewerTest.kt
    - app/src/test/java/com/quizango/gallery/ExploreTabTest.kt
key-decisions:
  - "A marker tap selects the place (camera animates, sheet scrolls to its card); a card tap opens that place's media list"
  - "Viewer Show on map keys on placeKey (name, else rounded coordinates), so coordinate-only photos are covered and a blank name with zero coordinates hides the row"
  - "Marker icons are Canvas bitmaps (thumbnail in a backgroundColor bubble, small bottom-start corner, name below) because a map marker cannot be a composable"
  - "Permanent denial = a result with no grant and no rationale; the same Allow tap then opens app settings with the home_permission_location toast"
requirements-completed: [VIEW-01, VIEW-03]
duration: n/a
completed: 2026-11-23
status: complete
actuals:
  tasks: 3
  commits: 4
---

# Phase 5 Plan 06: Google Maps Place screen Summary

Explore Place and viewer Show on map now open a Maps-SDK screen with photo markers over a draggable 3-column place sheet; it works with no Maps key (blank map area, live sheet) and asks for location only from the Allow stripe.

## Tasks

| Task | Commit | Notes |
|------|--------|-------|
| 1 Tracer: Explore -> Place with optional local key | 04caac9 | Verified, not redone: SDK 20.1.0 pinned, `MAPS_API_KEY` from `local.properties` (gitignored, confirmed) into manifest metadata, blank default; route, PlaceScreen, `filter:place` and `filter:place:<key>` all present; assembleDebug passes |
| 2 Markers, cards, scoped media | be1ca27 (wip helper), 5633fcc | Mosaic cards, Canvas photo markers, marker/sheet sync, PlaceScreenTest |
| 3 Permission and viewer Show on map | 5633fcc (stripe and permission flow, same file as Task 2), 8f86d55 | Viewer route and visibility, tests |

Task 2 and 3's PlaceScreen.kt work was written together and is in one commit (5633fcc); the viewer part is its own commit (8f86d55).

## Verification

- `./gradlew.bat :app:testDebugUnitTest --tests com.quizango.gallery.PlaceScreenTest --tests com.quizango.gallery.ExploreTabTest --tests com.quizango.gallery.MediaViewerTest -q` : exit 0 (PlaceScreenTest 11, ExploreTabTest 6, MediaViewerTest 23; 0 failures)
- `./gradlew.bat :app:assembleDebug -q` : exit 0, MAPS_API_KEY absent
- Full unit suite not re-run in this plan.

## Deviations from Plan

None in logic. Task 1 had already been done in a previous session and was only checked.

## Pending device verification (Artemis MCP disconnected, `adb devices` shows no device)

- No key: Explore -> Place does not crash, map area is blank, sheet loads and cards open scoped media
- With a local key: tiles render, markers show photo + name, marker tap animates camera and scrolls the sheet, My Location dot appears only after grant
- Revoke location: stripe shows; deny once, deny again (settings + toast), grant fine or coarse (stripe disappears)
- Viewer More on a geotagged item shows Show on map and opens that place; non-geotagged item has no row
- Marker bitmap proportions (60.sdp thumbnail, 6.sdp padding, 10.ssp label) are unchecked visually

## Known Stubs

None. Not recorded in the ledger: the device checks above are unrun-verify items for the orchestrator to track.

## Threat notes

- T-05-09: no permission request on entry; My Location only after grant; no analytics or upload.
- T-05-10: key read from untracked `local.properties`, never logged, no `AIza` string in tracked files. For a real key, restrict it in Google Cloud Console to the Maps SDK for Android plus this app's package name and signing SHA-1.
- T-05-11: place keys are URL-encoded in routes and only compared against current repository rows, never used as paths.

## Threat Flags

None beyond the plan's threat model.

## Self-Check: PASSED

Commits 04caac9, be1ca27, 5633fcc, 8f86d55 exist; PlaceScreen.kt and PlaceScreenTest.kt exist. STATE.md and ROADMAP.md untouched.
