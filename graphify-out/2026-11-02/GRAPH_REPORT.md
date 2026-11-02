# Graph Report - Gallery  (2026-11-02)

## Corpus Check
- 110 files · ~82,676 words
- Verdict: corpus is large enough that graph structure adds value.
- Unclassified: 270 file(s) not represented in the graph (top: .xml 262, .properties 2, (none) 1)

## Summary
- 1065 nodes · 3042 edges · 74 communities (35 shown, 39 thin omitted)
- Extraction: 99% EXTRACTED · 1% INFERRED · 0% AMBIGUOUS · INFERRED: 32 edges (avg confidence: 0.92)
- Token cost: 0 input · 0 output

## Graph Freshness
- Built from commit: `4700abb0`
- Run `git rev-parse HEAD` and compare to check if the graph is stale.
- Run `graphify update .` after code changes (no API cost).

## Community Hubs (Navigation)
- Dp
- formatGroupHeader
- GalleryDao
- VaultRepository
- MediaViewerScreen.kt
- ExploreTab.kt
- GalleryNavHost.kt
- Screen
- MediaItem
- SplashScreen.kt
- ImageCropView.kt
- Test
- CleanerTest.kt
- AppPreferences
- Test
- Converters
- HomeViewModel
- AlbumDetailScreen
- HomeScreen
- Independent Code Review & Architecture Audit
- Detailed Discrepancy Records
- EditorTab
- Screen-by-Screen Layout Comparison
- PhotoEditorTest.kt
- 1. Intentionally Excluded Proprietary Subsystems
- DuplicateTest
- CropHandle
- PhotoEditorScreen
- CropRatio
- FavoritesTest
- AlbumsTab.kt
- RecycleTest.kt
- MediaType
- Test
- Test
- Test
- BannerAdView.kt
- FavoritesScreen
- SettingsScreen
- DuplicatePhotoScreen
- StorageCleanPhase
- NativeAdCard.kt
- GalleryDrawer
- RecycleScreen
- AboutScreen
- VaultLockScreen
- VaultRecoveryQuestionScreen
- VaultSettingScreen
- Android APK Reconstruction
- DuplicateCompleteScreen
- CollageStep
- StickerScreen
- OrganizerRecycleScreen
- VaultMediaScreen
- Screen Inventory: Reference APK vs Reconstructed Implementation
- WallpaperScreen
- LanguageScreen
- AlbumDetail
- BlurBg
- ColorSplash
- DuplicateComplete
- Editor
- MediaViewer
- RemoveBg
- SearchColor
- StoryView
- Wallpaper
- Reconstruction Roadmap

## God Nodes (most connected - your core abstractions)
1. `MediaItem` - 82 edges
2. `MediaRepository` - 64 edges
3. `Screen` - 60 edges
4. `GalleryNavHost()` - 48 edges
5. `AppPreferences` - 41 edges
6. `GalleryDao` - 29 edges
7. `MediaType` - 29 edges
8. `MediaEntity` - 25 edges
9. `MediaRepositoryImpl` - 24 edges
10. `HomeViewModel` - 24 edges

## Surprising Connections (you probably didn't know these)
- `4. Home Top App Bar` --references--> `GalleryTopBar()`  [INFERRED]
  screenshot-comparison-report.md → app/src/main/java/com/quizango/gallery/ui/components/NavigationComponents.kt
- `1. Primary User Journey Screens` --references--> `GalleryDrawer()`  [INFERRED]
  screen-inventory.md → app/src/main/java/com/quizango/gallery/ui/components/NavigationComponents.kt
- `Technical Stack` --references--> `GalleryDatabase`  [INFERRED]
  .planning/PROJECT.md → app/src/main/java/com/quizango/gallery/data/local/database/GalleryDatabase.kt
- `Positive Findings` --references--> `MediaRepository`  [INFERRED]
  code-review-summary.md → app/src/main/java/com/quizango/gallery/data/repository/MediaRepository.kt
- `Positive Findings` --references--> `VaultRepository`  [INFERRED]
  code-review-summary.md → app/src/main/java/com/quizango/gallery/data/repository/VaultRepository.kt

## Import Cycles
- None detected.

## Communities (74 total, 39 thin omitted)

### Community 2 - "GalleryDao"
Cohesion: 0.07
Nodes (12): GalleryDao, VaultDao, MediaEntity, VaultEntity, GalleryDatabase, Evidence Classification Key, Feature Matrix, Feature Tracker: Observable Parity & Implementation Evidence (+4 more)

### Community 3 - "VaultRepository"
Cohesion: 0.23
Nodes (3): VaultRepository, VaultRepositoryImpl, VaultItem

### Community 4 - "MediaViewerScreen.kt"
Cohesion: 0.12
Nodes (11): BottomActionButton(), captureVideoFrame(), DetailSheetRow(), formatFileSize(), MediaViewerScreen(), PagerMediaItemView(), saveCapturedFrameToGallery(), shareMedia() (+3 more)

### Community 5 - "ExploreTab.kt"
Cohesion: 0.07
Nodes (3): AdManager, MainActivity, GalleryTheme()

### Community 6 - "GalleryNavHost.kt"
Cohesion: 0.07
Nodes (11): DuplicateWaitingScreen(), ScreenShortScreen(), CollageMakerScreen(), AnimatedMediaScreen(), BurstPhotoScreen(), QuickOrganizerScreen(), SearchColorScreen(), StoryViewScreen() (+3 more)

### Community 7 - "Screen"
Cohesion: 0.07
Nodes (30): About, AnimatedMedia, BurstPhotos, Cleaner, CollageMaker, Duplicates, DuplicateWaiting, Favorites (+22 more)

### Community 8 - "MediaItem"
Cohesion: 0.05
Nodes (9): MediaStoreScanner, MediaRepository, MediaRepositoryImpl, MediaItem, GalleryApplication, MomentGroup, MomentsScreen(), applyNightMode() (+1 more)

### Community 9 - "SplashScreen.kt"
Cohesion: 0.07
Nodes (7): BlurBgScreen(), ColorSplashScreen(), GalleryRulerView(), ImageSegmentationHelper, checkStoragePermissions(), SplashScreen(), WavyProgressIndicator()

### Community 10 - "ImageCropView.kt"
Cohesion: 0.13
Nodes (4): CropGeometry, drawCornerBrackets(), drawEdgeBars(), ImageCropView()

### Community 12 - "CleanerTest.kt"
Cohesion: 0.15
Nodes (9): formatVideoDuration(), formatVideoSize(), LargeVideoScreen(), formatStorageBytes(), JunkCategory, JunkCategoryItem, ReleaseOptionRow(), RemoveJunkScreen() (+1 more)

### Community 13 - "AppPreferences"
Cohesion: 0.10
Nodes (3): AppPreferences, Keys, ThemeScreen()

### Community 15 - "Converters"
Cohesion: 0.10
Nodes (11): Converters, ConvertersTest, Current Phase: Phase J Completed (Full Implementation, Unit Tests, and Build Verification), Project State, 1. Build Verification Summary, 2. Test Suite Execution Details, 3. AAPT Resource Compilation & Vector Verification, 4. Verification Conclusion (+3 more)

### Community 16 - "HomeViewModel"
Cohesion: 0.07
Nodes (14): SortDirection, ASCENDING, DESCENDING, SortField, DATE_MODIFIED, DATE_TAKEN, NAME, SIZE (+6 more)

### Community 17 - "AlbumDetailScreen"
Cohesion: 0.18
Nodes (8): AlbumDetailScreen(), shareSelectedMedia(), PhotoGridItem(), PhotosTab(), Detailed Route Specifications, Navigation Graph Topology, Navigation Map & Architecture, Special Transitions & Lifecycle Behaviors

### Community 18 - "HomeScreen"
Cohesion: 0.25
Nodes (5): GalleryTopBar(), HomeScreen(), SelectionActionItem(), shareSelectedMedia(), DISC-003: Home Toolbar & Vault Trigger

### Community 19 - "Independent Code Review & Architecture Audit"
Cohesion: 0.22
Nodes (8): 1. Architectural Integrity & Separation of Concerns, 2. Security & Privacy Audit, 3. Lifecycle & Memory Management, 4. Static Analysis & Tooling Status, Independent Code Review & Architecture Audit, Positive Findings, Positive Findings, Positive Findings

### Community 20 - "Detailed Discrepancy Records"
Cohesion: 0.22
Nodes (8): Detailed Discrepancy Records, DISC-001: Explore Tab Structure & Content, DISC-002: Albums Tab Top Feature Bar & Media Types, DISC-006: Reference Vector Drawables Missing, DISC-008: App Icons & Shortcuts, DISC-009: Cloud Sync & Third-Party Ad SDKs, Discrepancy Register: Forensic Audit & Reconstruction, Issue Summary

### Community 21 - "EditorTab"
Cohesion: 0.15
Nodes (12): AdjustParam, BRIGHTNESS, CONTRAST, SATURATION, SHARPEN, EditorTab, ADJUST, BRUSH (+4 more)

### Community 22 - "Screen-by-Screen Layout Comparison"
Cohesion: 0.22
Nodes (8): 1. Home Dashboard: Albums Tab, 2. Home Dashboard: Explore Tab, 4. Home Top App Bar, 5. System Cleaner Screen, Design System Tokens Summary, Evaluation Methodology, Screen-by-Screen Layout Comparison, Screenshot & UI Layout Comparison Report

### Community 23 - "PhotoEditorTest.kt"
Cohesion: 0.11
Nodes (15): applyAllTransformations(), DrawnPath, EditorFilter, COOL, CYBERPUNK, FADE, GRAYSCALE, NONE (+7 more)

### Community 24 - "1. Intentionally Excluded Proprietary Subsystems"
Cohesion: 0.22
Nodes (8): 1. Intentionally Excluded Proprietary Subsystems, 2. Inferred Behavioral Nuances, INFERRED-01: System Cache File Calculation, INFERRED-02: Duplicate Photo Detection Heuristics, UNKNOWN-01: Commercial Ad Mediation & Banners, UNKNOWN-02: Third-Party Telemetry & Analytics, UNKNOWN-03: Google Play In-App Billing, Unresolved Issues & Boundary Classifications

### Community 26 - "CropHandle"
Cohesion: 0.20
Nodes (10): CropHandle, BOTTOM, BOTTOM_LEFT, BOTTOM_RIGHT, CENTER, LEFT, RIGHT, TOP (+2 more)

### Community 28 - "PhotoEditorScreen"
Cohesion: 0.53
Nodes (8): AdjustPanel(), BrushPanel(), CropPanel(), FilterPanel(), PhotoEditorScreen(), StickerPanel(), SubToolHeader(), TextPanel()

### Community 29 - "CropRatio"
Cohesion: 0.22
Nodes (9): CropRatio, FIVE_FOUR, FOUR_FIVE, FOUR_THREE, FREE, NINE_SIXTEEN, ONE_ONE, SIXTEEN_NINE (+1 more)

### Community 31 - "AlbumsTab.kt"
Cohesion: 0.09
Nodes (22): Album, ColorPicker, CustomImage, Gallery, RemoveBgChoice, RemoveBgScreen(), SolidColor, Transparent (+14 more)

### Community 33 - "MediaType"
Cohesion: 0.25
Nodes (6): MediaType, ANIMATED, BURST, IMAGE, SCREENSHOT, VIDEO

### Community 38 - "FavoritesScreen"
Cohesion: 0.33
Nodes (3): FavoritesScreen(), formatFavoriteDuration(), shareFavoriteMedia()

### Community 39 - "SettingsScreen"
Cohesion: 0.53
Nodes (4): RateUsDialog(), SettingDivider(), SettingNavRow(), SettingsScreen()

### Community 40 - "DuplicatePhotoScreen"
Cohesion: 0.12
Nodes (11): CleanerCategoryCard(), CleanerScreen(), DuplicatePhotoScreen(), DISC-005: System Cleaner Screen Structure, DISC-007: Duplicate Photo Finder Navigation, 1. Executive Summary, 2. Artifact & Cryptographic Baseline, 3. Discrepancy Resolution & Audit Summary (+3 more)

### Community 41 - "StorageCleanPhase"
Cohesion: 0.40
Nodes (5): StorageCleanPhase, CLEANING, COMPLETED, OVERVIEW, SCANNING

### Community 43 - "GalleryDrawer"
Cohesion: 0.38
Nodes (6): DrawerItemData, DrawerRow(), GalleryBottomNav(), GalleryDrawer(), DISC-004: Navigation Drawer Alignment, 3. Navigation Drawer

### Community 50 - "Android APK Reconstruction"
Cohesion: 0.33
Nodes (5): Android APK Reconstruction, Architecture, Objective, Rules, Verification

### Community 52 - "CollageStep"
Cohesion: 0.67
Nodes (3): CollageStep, EDITOR, PICKER

### Community 55 - "VaultMediaScreen"
Cohesion: 0.40
Nodes (3): shareSelectedVaultMedia(), VaultGridItem(), VaultMediaScreen()

### Community 56 - "Screen Inventory: Reference APK vs Reconstructed Implementation"
Cohesion: 0.33
Nodes (5): 1. Primary User Journey Screens, 2. Reference APK Specialized / Nested Screens, 3. Third-Party & Proprietary Frameworks (Excluded by Design), Screen Inventory: Reference APK vs Reconstructed Implementation, Summary Metrics

## Knowledge Gaps
- **66 isolated node(s):** `IMAGE`, `VIDEO`, `SCREENSHOT`, `ANIMATED`, `BURST` (+61 more)
  These have ≤1 connection - possible missing edges or undocumented components. (Counts symbols only; 346 node(s) total have ≤1 connection when file, concept and rationale nodes are included.)
- **39 thin communities (<3 nodes) omitted from report** — run `graphify query` to explore isolated nodes.

## Suggested Questions
_Questions this graph is uniquely positioned to answer:_

- **Why does `Screen` connect `Screen` to `Dp`, `formatGroupHeader`, `MediaViewerScreen.kt`, `SplashScreen.kt`, `Test`, `CleanerTest.kt`, `DuplicateTest`, `AlbumsTab.kt`, `RecycleTest.kt`, `Test`, `Test`, `Screen.kt`, `AlbumDetail`, `BlurBg`, `ColorSplash`, `DuplicateComplete`, `Editor`, `MediaViewer`, `RemoveBg`, `SearchColor`, `StoryView`, `Wallpaper`?**
  _High betweenness centrality (0.128) - this node is a cross-community bridge._
- **Are the 2 inferred relationships involving `GalleryNavHost()` (e.g. with `Positive Findings` and `DISC-007: Duplicate Photo Finder Navigation`) actually correct?**
  _`GalleryNavHost()` has 2 INFERRED edges - model-reasoned connections that need verification._
- **What connects `IMAGE`, `VIDEO`, `SCREENSHOT` to the rest of the system?**
  _66 weakly-connected nodes found - possible documentation gaps or missing edges._
- **Should `Dp` be split into smaller, more focused modules?**
  _Cohesion score 0.07021346469622332 - nodes in this community are weakly interconnected._
- **Why does `MediaItem` connect `MediaItem` to `Dp`, `MediaType`, `GalleryDao`, `VaultRepository`, `MediaViewerScreen.kt`, `formatGroupHeader`, `GalleryNavHost.kt`, `RecycleTest.kt`, `Test`, `RecycleScreen`, `CleanerTest.kt`, `HomeViewModel`, `AlbumDetailScreen`, `DuplicateTest`, `FavoritesTest`, `AlbumsTab.kt`?**
  _High betweenness centrality (0.125) - this node is a cross-community bridge._
- **Should `GalleryDao` be split into smaller, more focused modules?**
  _Cohesion score 0.07103825136612021 - nodes in this community are weakly interconnected._
- **Why does `MediaRepository` connect `MediaItem` to `Dp`, `MediaViewerScreen.kt`, `GalleryNavHost.kt`, `FavoritesScreen`, `DuplicatePhotoScreen`, `SplashScreen.kt`, `CleanerTest.kt`, `RecycleScreen`, `HomeViewModel`, `AlbumDetailScreen`, `Independent Code Review & Architecture Audit`, `OrganizerRecycleScreen`, `PhotoEditorScreen`, `AlbumsTab.kt`?**
  _High betweenness centrality (0.115) - this node is a cross-community bridge._