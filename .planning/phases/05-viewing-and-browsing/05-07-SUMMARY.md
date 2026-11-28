---
phase: 05-viewing-and-browsing
plan: 07
subsystem: ui
tags: [search, compose, color-picker, shared-media-list, navigation]
requires:
  - phase: 05-06
    provides: groupPlaces / placeMediaRoute (filter:place:<key>), shared item cards
  - phase: 05-05
    provides: momentRoute, AlbumSquareCard (item_search_sub), MomentScanner grouping
  - phase: 05-04
    provides: SharedMediaList frame and the shared selection actions
provides:
  - Search (SearchActivity 2.7.2) with Colors row and Albums / Places / Moments previews
  - Choose color dialog starting at #007AFF
  - Separate Search View All route
  - Media Color on the shared list frame with four actions
affects: [phase-05-verification]
tech-stack:
  added: []
  patterns: ["Pure search index (buildSearchIndex / sectionCards) shared by Search and View All", "Route args decoded once with a non-throwing fallback"]
key-files:
  created:
    - app/src/main/java/com/quizango/gallery/ui/search/SearchColorPicker.kt
    - app/src/main/java/com/quizango/gallery/ui/search/SearchViewAllScreen.kt
    - app/src/test/java/com/quizango/gallery/SearchScreenTest.kt
    - app/src/test/java/com/quizango/gallery/SearchColorTest.kt
  modified:
    - app/src/main/java/com/quizango/gallery/ui/search/SearchScreen.kt
    - app/src/main/java/com/quizango/gallery/ui/discovery/SearchColorScreen.kt
    - app/src/main/java/com/quizango/gallery/ui/navigation/Screen.kt
    - app/src/main/java/com/quizango/gallery/ui/navigation/GalleryNavHost.kt
    - app/src/main/res/values*/strings.xml (24 languages)
key-decisions:
  - "Search results are one pure SearchIndex (Albums / Places / Moments card lists) built off the main thread; preview = first 3 (side-by-side 2026-11-27; was 6), View All = the whole filtered list, both from the same function"
  - "View All carries only section type and query in the route and recomputes from current data; unknown or malformed section = empty state, never a crash"
  - "Search keeps its query in rememberSaveable so Back from View All returns to the unchanged query"
  - "adjustNothing is applied by switching the window soft-input mode while Search is shown and restoring it on leave, because the app is one activity with adjustResize"
  - "Media Color route colour is decoded once and accepted only as #RRGGBB (upper-cased), else the route default #FF0000"
requirements-completed: [VIEW-03]
duration: n/a
completed: 2026-11-23
status: complete
actuals:
  tasks: 3
  commits: 3
---

# Phase 5 Plan 07: Search, View All and Media Color Summary

Search now shows Colors followed by Albums, Places and Moments previews (three cards each, filtered as you type), opens a separate View All screen, and sends swatches or a Choose color dialog choice to Media Color, which is the shared media list with its four reference actions.

## Tasks

| Task | Commit | Notes |
|------|--------|-------|
| 1 Tracer: query, section cards, media lists | 5510394 | Search rebuilt per UI-SPEC 11.1-11.3; Choose color dialog; strings imported |
| 2 Search View All route | fe5b304 | SearchViewAllScreen and the pure route-arg parser, tests |
| 3 Media Color on the shared frame | 6f01d64 | SearchColorScreen rewrite, SearchColorTest; the NavHost destinations for View All and Color land here because they need the Task 3 screen signature |

Task 1 also holds the `Screen.SearchViewAll` route constant and the Search NavHost wiring; the destination itself arrives with Task 3's NavHost commit. Between those two commits View All had a route but no destination (not shipped, everything is committed together locally).

## What was built

- **Search (`SearchScreen.kt`)**: toolbar per 11.1 (55.dp bar + 1.dp divider, `search_back` field, hint `search`, IME Search, clear / search icons), Colors header with the picker button and ten swatches, then Albums, Places, Moments sections each only when non-empty, three `item_search_sub` cards, "View All", 0.5.dp dividers. Albums are real device folders only (no `filter:*` virtual albums). Place cards use `groupPlaces` / `placeMediaRoute`, moment cards use `MomentScanner.group` / `momentRoute`, so they open the same media lists as Places and Moments. The moment scan is started from Search. Empty state is `ic_empty_img` + `oops` + `search_deleted`; a centred spinner covers the page until media and albums have emitted.
- **Choose color (`SearchColorPicker.kt`)**: Compose dialog with preview and hue / saturation / value sliders, starts at #007AFF, Cancel / Ok; Ok opens Media Color with `#RRGGBB`.
- **View All (`SearchViewAllScreen.kt`, `Screen.SearchViewAll`)**: `SharedMediaToolbar` titled with the section name, 3-column grid (2.sdp / 4.sdp padding), spinner while loading, Oops + `search_deleted` when empty. No Collapse, no in-place expand, no Photos / Videos sections anywhere in Search.
- **Media Color (`SearchColorScreen.kt`)**: `SharedMediaList` titled `media_color`; slots Share, Favorite / Unfavorite, Add to Private, Delete in that order using the shared real favourite / vault / delete behaviour; viewer taps use `filter:color:<hex>`, which `getMediaByFolderFlow` already resolves, so the pager stays within the colour result. The old PRO icon, `ic_more`, Material check, fixed 4 columns and English labels are gone.
- **Strings**: `choose_color`, `colors`, `places`, `view_all`, `search_deleted` imported from the 2.7.2 reference and synced to 24 languages.

## Verification

- `./gradlew.bat :app:testDebugUnitTest --tests "com.quizango.gallery.SearchScreenTest" --tests "com.quizango.gallery.SearchColorTest" --tests "com.quizango.gallery.SharedMediaListTest" :app:assembleDebug -q` : exit 0 (SearchScreenTest 11, SearchColorTest 5, SharedMediaListTest 6; 0 failures, 0 skipped; assembleDebug passes)
- `python -I scripts/ref_res.py check-strings` : 24 languages up to date
- Full unit suite not re-run in this plan.

## Deviations from Plan

**1. [Rule 3 - Blocking] Missing import, test constructor argument**
- **Found during:** build after Task 1 / Task 3
- **Issue:** `SearchScreen.kt` lacked `androidx.compose.ui.unit.dp` (commit 5510394 does not compile on its own); `SearchColorTest` MediaItem lacked `dateTaken`.
- **Fix:** import added in fe5b304, `dateTaken` in 6f01d64. The final tree builds.

**2. TDD RED commits skipped.** Tests and code were written together and verified once (a cold Gradle run is 3-5 minutes here and a compile-failing RED run proves nothing). No `test(...)` RED commit exists for the three `tdd="true"` tasks.

**3. Tracer feedback gate.** Autonomous run: the tracer's `<verify>` (SearchScreenTest + assembleDebug) was first run after Tasks 1-3 were written, not between Task 1 and Task 2. It passes for the final tree.

## Decisions made without asking (user AFK)

- A blank query shows every section (previews), not nothing. "Oops!" shows whenever the source is loaded and no section has a card, including a blank query on a library with no folders / places / moments; the Colors row stays visible above it.
- Place card titles are the place key (name, else rounded coordinates), matching the Place screen and `filter:place:<key>`.
- Places include videos (like the Place screen); Moments are photos only (like Moments).
- Search does not auto-focus the field or open the keyboard on entry (2.7.2 behaviour not confirmed).
- Double decoding: the app decodes route args with `URLDecoder` itself. For the free-form query and colour this is made non-throwing (`runCatching`, already-decoded text such as `100%` is kept). A typed `+` may come back as a space if Navigation had already decoded the argument; unverified on device.
- Search Color keeps the repository's existing colour matching unchanged (as the plan says), which only recognises the ten swatch colours. A custom colour from the picker that is not one of the ten shows the "No items" empty state.

## Pending device verification (Artemis MCP disconnected, `adb devices` shows no device)

- Type a query matching one album, one place and one moment: each section filters; each card opens the right media list
- A section with more than three matches shows three; View All opens a titled 3-column screen with every match; Back returns to Search with the same query
- Keyboard does not resize the page (adjustNothing override, restored when leaving Search)
- Swatch taps and the custom Choose color Ok open Media Color; Cancel and outside-tap only close the dialog
- Media Color: pinch columns, select items, only the four actions appear; a result opens a viewer pager limited to that colour
- Moment results fill in while the scan runs; no "Oops!" flash on first open
- Dialog and Search toolbar look (slider dialog is a plain Material 3 AlertDialog, not the reference's colour-picker library) and swatch and field dimensions are unchecked visually
- Whether the nav host already decodes `%` sequences in the `query` and `color` arguments

## Known Stubs

None. Not recorded in `.planning/WINDOWS.md`; the device checks above are unrun-verify items for the orchestrator to track.

## Threat notes

- T-05-12: section is a typed whitelist (`SearchSection.of`), the query is URL-encoded in the route and decoded without throwing, results are recomputed from the current repositories, and the query is only compared against titles, never used as a path.
- T-05-13: Media Color uses the shared validated selection actions (immutable item snapshot per tap, shared delete / hide confirmations); the colour argument must match `#RRGGBB` before it reaches `filter:color:` or the repository.

## Threat Flags

None beyond the plan's threat model.

## Self-Check: PASSED

Commits 5510394, fe5b304, 6f01d64 exist; SearchColorPicker.kt, SearchViewAllScreen.kt, SearchScreenTest.kt and SearchColorTest.kt exist. STATE.md and ROADMAP.md not touched by this plan (STATE.md was already modified before the run and is left unstaged).
