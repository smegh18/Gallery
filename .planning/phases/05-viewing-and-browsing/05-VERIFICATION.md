---
phase: 05-viewing-and-browsing
verified: 2026-11-23
status: passed
score: 5/5 success criteria
---

# Phase 5: Viewing and Browsing Verification

**Goal:** Opening, playing, listing and searching media feels the same as 2.7.2.

| # | Success criterion | Status | Evidence |
|---|---|---|---|
| 1 | The viewer swipes, zooms and double-taps like 2.7.2; the options row matches and works | ✓ | 05-01 / 05-02; `ViewerZoomTest` and `MediaViewerTest`; device rows #13 plus the normal, recycle and vault viewer states (05-LIVE-COMPARISON) |
| 2 | The video player matches 2.7.2 and closing it stops the audio | ✓ | 05-03; `VideoPlayerTest`; device row #14, short and long video, and the audio-ownership row; subtitle dialog fixed in c18d9a7 |
| 3 | The media list and all-albums screens match and open the right media | ✓ | 05-04 / 05-05; `SharedMediaListTest` and `AllAlbumsTest`; device rows #16 and #17 (Other Albums through the debug link) |
| 4 | Moments, Place, Animated, Burst and Favorites match, including empty states | ✓ | 05-05 / 05-06; device rows #18-#22; Place GPS read fixed in ab84704 |
| 5 | Search, Search all and Search by colour match | ✓ | 05-07; `SearchScreenTest`; device rows #23 and #24 |

**Gates:**
- `:app:testDebugUnitTest` and `:app:assembleDebug` pass.
- `check-strings` and `check-colors` pass.
- The device pass on the Redmi was side by side with 2.7.2, and the user ran the remaining checks.

**Not run or carried over:**
- **CodeRabbit:** the CLI and MCP aren't available (the MCP needs `/mcp` authorization). A manual audit is recorded in 05-08 instead.
- **Live map:** there is no Maps key, which is an accepted state.
- **Layout to Phase 13:** small layout differences (Subtitle dialog width, Place sheet height, player bottom-row spacing).
- **Animated:** there is no scanning service for it (we only flag GIFs during sync).
- **UI-SPEC §9 Burst:** the spec's mosaic description is wrong. The 2.7.2 code uses a single cover, and the port follows the code.
- **Debug links:** the debug-only Explore links (fed36c2) were removed on 2026-11-27 in the side-by-side pass; Phase 9's ExternalEntry routes reach these screens.
