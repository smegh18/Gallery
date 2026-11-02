# Graph Report - Gallery  (2026-11-02)

## Corpus Check
- cluster-only mode — file stats not available

## Summary
- 1083 nodes · 3049 edges · 91 communities (36 shown, 55 thin omitted)
- Extraction: 99% EXTRACTED · 1% INFERRED · 0% AMBIGUOUS · INFERRED: 34 edges (avg confidence: 0.92)
- Token cost: 0 input · 0 output

## Graph Freshness
- Built from commit: `00a31f22`
- Run `git rev-parse HEAD` and compare to check if the graph is stale.
- Run `graphify update .` after code changes (no API cost).

## Community Hubs (Navigation)
- Dp
- GalleryDao
- GalleryDao
- GalleryNavHost.kt
- MediaViewerScreen.kt
- ExploreTab.kt
- GalleryNavHost.kt
- Screen
- AlbumsTab.kt
- ImageSegmentationHelper
- ImageCropView.kt
- Converters
- AppPreferences
- Test
- Converters
- CleanerTest.kt
- Test
- HomeViewModel
- Test
- VaultRepository
- EditorTab
- GalleryRulerView.kt
- MediaRepositoryImpl
- HomeScreen
- DuplicateTest
- CropHandle
- DuplicateTest
- PhotoEditorScreen
- CropRatio
- FavoritesTest
- Detailed Discrepancy Records
- RecycleTest.kt
- FavoritesTest
- Test
- Test
- 1. Intentionally Excluded Proprietary Subsystems
- BannerAdView.kt
- FavoritesScreen
- SettingsScreen
- DomainModelTest
- StorageCleanPhase
- NativeAdCard.kt
- Test
- RecycleScreen
- AboutScreen
- VaultLockScreen
- VaultRecoveryQuestionScreen
- VaultSettingScreen
- gradlew
- FavoritesScreen
- DuplicateCompleteScreen
- CollageStep
- StickerScreen
- MomentsScreen
- RecycleScreen
- AboutScreen
- Screen.kt
- VaultRecoveryQuestionScreen
- VaultSettingScreen
- BlurBg
- ColorSplash
- DuplicateComplete
- Editor
- RemoveBg
- SearchColor
- StoryView
- Wallpaper
- app/build.gradle.kts
- build.gradle.kts
- settings.gradle.kts
- RemoveBg
- SearchColor
- StoryView
- Wallpaper
- About Gallery Logo
- Launcher Icon
- Round Launcher Icon (HDPI)
- Round Launcher Icon (HDPI PNG)
- Launcher Icon (MDPI)
- Launcher Icon (XHDPI)
- Round Launcher Icon (XHDPI)
- Round Launcher Icon (XHDPI PNG)
- Launcher Icon (XXHDPI)
- Round Launcher Icon (XXHDPI)
- Ralph Progress & Architecture Notes

## God Nodes (most connected - your core abstractions)
1. `MediaItem` - 82 edges
2. `MediaRepository` - 64 edges
3. `Screen` - 60 edges
4. `GalleryNavHost()` - 48 edges
5. `AppPreferences` - 41 edges
6. `GalleryDao` - 29 edges
7. `MediaType` - 29 edges
8. `MediaEntity` - 24 edges
9. `HomeViewModel` - 24 edges
10. `MediaRepositoryImpl` - 24 edges

## Surprising Connections (you probably didn't know these)
- `DISC-004: Navigation Drawer Alignment` --references--> `GalleryDrawer()`  [INFERRED]
  discrepancy-register.md → app/src/main/java/com/quizango/gallery/ui/components/NavigationComponents.kt
- `1. Primary User Journey Screens` --references--> `GalleryDrawer()`  [INFERRED]
  screen-inventory.md → app/src/main/java/com/quizango/gallery/ui/components/NavigationComponents.kt
- `3. Navigation Drawer` --references--> `GalleryDrawer()`  [INFERRED]
  screenshot-comparison-report.md → app/src/main/java/com/quizango/gallery/ui/components/NavigationComponents.kt
- `3. Discrepancy Resolution & Audit Summary` --references--> `DuplicatePhotoScreen()`  [INFERRED]
  final-readiness-report.md → app/src/main/java/com/quizango/gallery/ui/cleaner/DuplicatePhotoScreen.kt
- `Technical Stack` --references--> `GalleryDatabase`  [INFERRED]
  .planning/PROJECT.md → app/src/main/java/com/quizango/gallery/data/local/database/GalleryDatabase.kt

## Import Cycles
- None detected.

## Hyperedges (group relationships)
- **Application Launcher Icons** — app_src_main_res_mipmap_hdpi_ic_launcher_gallery_round, app_src_main_res_mipmap_hdpi_ic_launcher_round_gallery, app_src_main_res_mipmap_mdpi_ic_launcher_gallery, app_src_main_res_mipmap_xhdpi_ic_launcher_gallery, app_src_main_res_mipmap_xhdpi_ic_launcher_gallery_round, app_src_main_res_mipmap_xhdpi_ic_launcher_round_gallery, app_src_main_res_mipmap_xxhdpi_ic_launcher_gallery, app_src_main_res_mipmap_xxhdpi_ic_launcher_gallery_round [EXTRACTED 1.00]

## Communities (91 total, 55 thin omitted)

### Community 1 - "GalleryDao"
Cohesion: 0.07
Nodes (13): GalleryDao, VaultDao, MediaEntity, VaultEntity, GalleryDatabase, Feature Tracker, Evidence Classification Key, Feature Matrix (+5 more)

### Community 2 - "GalleryDao"
Cohesion: 0.07
Nodes (4): AdManager, MainActivity, applyNightMode(), GalleryTheme()

### Community 3 - "GalleryNavHost.kt"
Cohesion: 0.07
Nodes (11): DuplicateWaitingScreen(), ScreenShortScreen(), CollageMakerScreen(), WallpaperScreen(), AnimatedMediaScreen(), BurstPhotoScreen(), OrganizerRecycleScreen(), StoryViewScreen() (+3 more)

### Community 4 - "MediaViewerScreen.kt"
Cohesion: 0.11
Nodes (11): BottomActionButton(), captureVideoFrame(), DetailSheetRow(), formatFileSize(), MediaViewerScreen(), PagerMediaItemView(), saveCapturedFrameToGallery(), shareMedia() (+3 more)

### Community 5 - "ExploreTab.kt"
Cohesion: 0.10
Nodes (4): SearchColorScreen(), formatGroupHeader(), AlbumDetailTest, HomeTest

### Community 7 - "Screen"
Cohesion: 0.07
Nodes (30): About, AnimatedMedia, BurstPhotos, Cleaner, CollageMaker, Duplicates, DuplicateWaiting, Favorites (+22 more)

### Community 8 - "AlbumsTab.kt"
Cohesion: 0.14
Nodes (15): Album, AlbumCard(), AlbumsTab(), FeatureItemView(), MediaTypeItem, StoryCircleItem(), TopFeatureItem, ExploreTab() (+7 more)

### Community 9 - "ImageSegmentationHelper"
Cohesion: 0.11
Nodes (10): BlurBgScreen(), ColorSplashScreen(), ImageSegmentationHelper, ColorPicker, CustomImage, Gallery, RemoveBgChoice, RemoveBgScreen() (+2 more)

### Community 10 - "ImageCropView.kt"
Cohesion: 0.08
Nodes (21): AdjustParam, BRIGHTNESS, CONTRAST, SATURATION, SHARPEN, CropRatio, FIVE_FOUR, FOUR_FIVE (+13 more)

### Community 11 - "Converters"
Cohesion: 0.10
Nodes (11): Converters, ConvertersTest, Current Phase: Phase J Completed (Full Implementation, Unit Tests, and Build Verification), Project State, 1. Build Verification Summary, 2. Test Suite Execution Details, 3. AAPT Resource Compilation & Vector Verification, 4. Verification Conclusion (+3 more)

### Community 13 - "AppPreferences"
Cohesion: 0.13
Nodes (4): CropGeometry, drawCornerBrackets(), drawEdgeBars(), ImageCropView()

### Community 14 - "Test"
Cohesion: 0.09
Nodes (4): AppPreferences, Keys, LanguageScreen(), VaultForgotPasswordScreen()

### Community 15 - "Converters"
Cohesion: 0.23
Nodes (13): AdjustPanel(), applyAllTransformations(), BrushPanel(), CropPanel(), DrawnPath, FilterPanel(), PhotoEditorScreen(), PlacedSticker (+5 more)

### Community 16 - "CleanerTest.kt"
Cohesion: 0.15
Nodes (9): formatVideoDuration(), formatVideoSize(), LargeVideoScreen(), formatStorageBytes(), JunkCategory, JunkCategoryItem, ReleaseOptionRow(), RemoveJunkScreen() (+1 more)

### Community 20 - "VaultRepository"
Cohesion: 0.23
Nodes (3): VaultRepository, VaultRepositoryImpl, VaultItem

### Community 21 - "EditorTab"
Cohesion: 0.19
Nodes (10): SortDirection, ASCENDING, DESCENDING, SortField, DATE_MODIFIED, DATE_TAKEN, NAME, SIZE (+2 more)

### Community 24 - "HomeScreen"
Cohesion: 0.25
Nodes (7): DrawerItemData, DrawerRow(), GalleryBottomNav(), GalleryDrawer(), HomeScreen(), SelectionActionItem(), shareSelectedMedia()

### Community 26 - "CropHandle"
Cohesion: 0.22
Nodes (5): CleanerCategoryCard(), CleanerScreen(), DuplicatePhotoScreen(), DISC-005: System Cleaner Screen Structure, DISC-007: Duplicate Photo Finder Navigation

### Community 28 - "PhotoEditorScreen"
Cohesion: 0.20
Nodes (10): CropHandle, BOTTOM, BOTTOM_LEFT, BOTTOM_RIGHT, CENTER, LEFT, RIGHT, TOP (+2 more)

### Community 29 - "CropRatio"
Cohesion: 0.20
Nodes (10): EditorFilter, COOL, CYBERPUNK, FADE, GRAYSCALE, NONE, SEPIA, VINTAGE (+2 more)

### Community 30 - "FavoritesTest"
Cohesion: 0.22
Nodes (4): PermissionScreen(), checkStoragePermissions(), SplashScreen(), WavyProgressIndicator()

### Community 31 - "Detailed Discrepancy Records"
Cohesion: 0.20
Nodes (10): Discrepancy Register, Detailed Discrepancy Records, DISC-001: Explore Tab Structure & Content, DISC-002: Albums Tab Top Feature Bar & Media Types, DISC-004: Navigation Drawer Alignment, DISC-006: Reference Vector Drawables Missing, DISC-008: App Icons & Shortcuts, DISC-009: Cloud Sync & Third-Party Ad SDKs (+2 more)

### Community 32 - "RecycleTest.kt"
Cohesion: 0.22
Nodes (5): AlbumDetailScreen(), shareSelectedMedia(), GalleryTopBar(), DISC-003: Home Toolbar & Vault Trigger, 4. Home Top App Bar

### Community 34 - "Test"
Cohesion: 0.22
Nodes (8): 1. Architectural Integrity & Separation of Concerns, 2. Security & Privacy Audit, 3. Lifecycle & Memory Management, 4. Static Analysis & Tooling Status, Independent Code Review & Architecture Audit, Positive Findings, Positive Findings, Positive Findings

### Community 35 - "Test"
Cohesion: 0.22
Nodes (8): 1. Home Dashboard: Albums Tab, 2. Home Dashboard: Explore Tab, 3. Navigation Drawer, 5. System Cleaner Screen, Design System Tokens Summary, Evaluation Methodology, Screen-by-Screen Layout Comparison, Screenshot & UI Layout Comparison Report

### Community 36 - "1. Intentionally Excluded Proprietary Subsystems"
Cohesion: 0.22
Nodes (8): 1. Intentionally Excluded Proprietary Subsystems, 2. Inferred Behavioral Nuances, INFERRED-01: System Cache File Calculation, INFERRED-02: Duplicate Photo Detection Heuristics, UNKNOWN-01: Commercial Ad Mediation & Banners, UNKNOWN-02: Third-Party Telemetry & Analytics, UNKNOWN-03: Google Play In-App Billing, Unresolved Issues & Boundary Classifications

### Community 37 - "BannerAdView.kt"
Cohesion: 0.25
Nodes (6): MediaType, ANIMATED, BURST, IMAGE, SCREENSHOT, VIDEO

### Community 38 - "FavoritesScreen"
Cohesion: 0.32
Nodes (6): PhotoGridItem(), PhotosTab(), Detailed Route Specifications, Navigation Graph Topology, Navigation Map & Architecture, Special Transitions & Lifecycle Behaviors

### Community 44 - "RecycleScreen"
Cohesion: 0.25
Nodes (6): 1. Executive Summary, 2. Artifact & Cryptographic Baseline, 3. Discrepancy Resolution & Audit Summary, 4. Verification & Quality Gates, 5. Final Sign-off, Final Readiness & Reconstruction Sign-off Report

### Community 46 - "VaultLockScreen"
Cohesion: 0.33
Nodes (4): formatDuration(), shareSelectedVaultMedia(), VaultGridItem(), VaultMediaScreen()

### Community 47 - "VaultRecoveryQuestionScreen"
Cohesion: 0.29
Nodes (7): App Icon Splash, Screen Inventory, 1. Primary User Journey Screens, 2. Reference APK Specialized / Nested Screens, 3. Third-Party & Proprietary Frameworks (Excluded by Design), Screen Inventory: Reference APK vs Reconstructed Implementation, Summary Metrics

### Community 48 - "VaultSettingScreen"
Cohesion: 0.33
Nodes (5): Android APK Reconstruction, Architecture, Objective, Rules, Verification

### Community 50 - "FavoritesScreen"
Cohesion: 0.33
Nodes (3): FavoritesScreen(), formatFavoriteDuration(), shareFavoriteMedia()

### Community 51 - "DuplicateCompleteScreen"
Cohesion: 0.53
Nodes (4): RateUsDialog(), SettingDivider(), SettingNavRow(), SettingsScreen()

### Community 52 - "CollageStep"
Cohesion: 0.40
Nodes (5): StorageCleanPhase, CLEANING, COMPLETED, OVERVIEW, SCANNING

### Community 62 - "ColorSplash"
Cohesion: 0.67
Nodes (3): CollageStep, EDITOR, PICKER

## Knowledge Gaps
- **78 isolated node(s):** `Screenshots`, `Search`, `Settings`, `Splash`, `Stickers` (+73 more)
  These have ≤1 connection - possible missing edges or undocumented components. (Counts symbols only; 356 node(s) total have ≤1 connection when file, concept and rationale nodes are included.)
- **55 thin communities (<3 nodes) omitted from report** — run `graphify query` to explore isolated nodes.

## Suggested Questions
_Questions this graph is uniquely positioned to answer:_

- **Why does `Screen` connect `Screen` to `Dp`, `MediaViewerScreen.kt`, `ExploreTab.kt`, `AlbumsTab.kt`, `Converters`, `CleanerTest.kt`, `Test`, `DuplicateTest`, `SettingsScreen`, `StorageCleanPhase`, `Test`, `MediaViewer`, `SearchColor`, `StoryView`, `Wallpaper`, `app/build.gradle.kts`, `build.gradle.kts`, `settings.gradle.kts`, `RemoveBg`, `SearchColor`, `StoryView`, `Wallpaper`?**
  _High betweenness centrality (0.138) - this node is a cross-community bridge._
- **Are the 2 inferred relationships involving `GalleryNavHost()` (e.g. with `Positive Findings` and `DISC-007: Duplicate Photo Finder Navigation`) actually correct?**
  _`GalleryNavHost()` has 2 INFERRED edges - model-reasoned connections that need verification._
- **What connects `Screenshots`, `Search`, `Settings` to the rest of the system?**
  _78 weakly-connected nodes found - possible documentation gaps or missing edges._
- **Should `Dp` be split into smaller, more focused modules?**
  _Cohesion score 0.08113207547169811 - nodes in this community are weakly interconnected._
- **Why does `MediaItem` connect `GalleryNavHost.kt` to `Dp`, `GalleryDao`, `GalleryNavHost.kt`, `MediaViewerScreen.kt`, `ExploreTab.kt`, `AlbumsTab.kt`, `CleanerTest.kt`, `CleanerTest.kt`, `HomeViewModel`, `Test`, `VaultRepository`, `EditorTab`, `MediaRepositoryImpl`, `DuplicateTest`, `FavoritesTest`, `BannerAdView.kt`, `FavoritesScreen`, `SettingsScreen`, `DomainModelTest`, `AboutScreen`, `MomentsScreen`, `RecycleScreen`?**
  _High betweenness centrality (0.134) - this node is a cross-community bridge._
- **Should `GalleryDao` be split into smaller, more focused modules?**
  _Cohesion score 0.07231638418079096 - nodes in this community are weakly interconnected._
- **Why does `MediaRepository` connect `GalleryNavHost.kt` to `Dp`, `GalleryNavHost.kt`, `MediaViewerScreen.kt`, `ExploreTab.kt`, `AlbumsTab.kt`, `ImageSegmentationHelper`, `Converters`, `CleanerTest.kt`, `HomeViewModel`, `EditorTab`, `MediaRepositoryImpl`, `DuplicateTest`, `CropHandle`, `RecycleTest.kt`, `Test`, `AboutScreen`, `FavoritesScreen`, `MomentsScreen`, `RecycleScreen`, `Editor`?**
  _High betweenness centrality (0.110) - this node is a cross-community bridge._