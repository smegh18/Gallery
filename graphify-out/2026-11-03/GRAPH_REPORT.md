# Graph Report - Gallery  (2026-11-02)

## Corpus Check
- 110 files · ~82,619 words
- Verdict: corpus is large enough that graph structure adds value.
- Unclassified: 270 file(s) not represented in the graph (top: .xml 262, .properties 2, (none) 1)

## Summary
- 1072 nodes · 3034 edges · 92 communities (35 shown, 57 thin omitted)
- Extraction: 99% EXTRACTED · 1% INFERRED · 0% AMBIGUOUS · INFERRED: 32 edges (avg confidence: 0.92)
- Token cost: 0 input · 0 output

## Graph Freshness
- Built from commit: `bc532e56`
- Run `git rev-parse HEAD` and compare to check if the graph is stale.
- Run `graphify update .` after code changes (no API cost).

## Community Hubs (Navigation)
- Dp
- GalleryDao
- GalleryDao
- GalleryNavHost.kt
- MediaViewerScreen.kt
- PhotoEditorScreen
- MediaItem
- Screen
- Activity
- ImageSegmentationHelper
- ImageCropView.kt
- Converters
- AppPreferences
- Test
- Converters
- CleanerTest.kt
- Test
- RecycleScreen
- VaultEntity
- VaultRepository
- HomeViewModel
- GalleryRulerView.kt
- GalleryDatabase
- HomeScreen
- DuplicateTest
- CropHandle
- DuplicateTest
- PhotoEditorScreen
- CropRatio
- SplashScreen.kt
- Detailed Discrepancy Records
- BurstPhotoScreen
- FavoritesTest
- Test
- Test
- final-readiness-report.md
- formatGroupHeader
- FavoritesScreen
- RecycleTest.kt
- CropRatio
- StorageCleanPhase
- NativeAdCard.kt
- Test
- OrganizerRecycleScreen
- GalleryDrawer
- Feature Matrix
- VaultRecoveryQuestionScreen
- VaultSettingScreen
- gradlew
- FavoritesScreen
- DuplicateCompleteScreen
- CollageStep
- StickerScreen
- VaultForgotPasswordScreen
- SearchColorScreen
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
- LanguageScreen

## God Nodes (most connected - your core abstractions)
1. `MediaItem` - 84 edges
2. `MediaRepository` - 64 edges
3. `Screen` - 60 edges
4. `GalleryNavHost()` - 48 edges
5. `AppPreferences` - 41 edges
6. `MediaType` - 30 edges
7. `GalleryDao` - 29 edges
8. `MediaEntity` - 25 edges
9. `MediaRepositoryImpl` - 24 edges
10. `HomeViewModel` - 24 edges

## Surprising Connections (you probably didn't know these)
- `3. Discrepancy Resolution & Audit Summary` --references--> `DuplicatePhotoScreen()`  [INFERRED]
  final-readiness-report.md → app/src/main/java/com/quizango/gallery/ui/cleaner/DuplicatePhotoScreen.kt
- `1. Primary User Journey Screens` --references--> `GalleryDrawer()`  [INFERRED]
  screen-inventory.md → app/src/main/java/com/quizango/gallery/ui/components/NavigationComponents.kt
- `Technical Stack` --references--> `GalleryDatabase`  [INFERRED]
  .planning/PROJECT.md → app/src/main/java/com/quizango/gallery/data/local/database/GalleryDatabase.kt
- `Feature Matrix` --references--> `MediaEntity`  [INFERRED]
  feature-tracker.md → app/src/main/java/com/quizango/gallery/data/local/database/entity/Entities.kt
- `Feature Matrix` --references--> `VaultEntity`  [INFERRED]
  feature-tracker.md → app/src/main/java/com/quizango/gallery/data/local/database/entity/Entities.kt

## Import Cycles
- None detected.

## Hyperedges (group relationships)
- **Application Launcher Icons** — app_src_main_res_mipmap_hdpi_ic_launcher_gallery_round, app_src_main_res_mipmap_hdpi_ic_launcher_round_gallery, app_src_main_res_mipmap_mdpi_ic_launcher_gallery, app_src_main_res_mipmap_xhdpi_ic_launcher_gallery, app_src_main_res_mipmap_xhdpi_ic_launcher_gallery_round, app_src_main_res_mipmap_xhdpi_ic_launcher_round_gallery, app_src_main_res_mipmap_xxhdpi_ic_launcher_gallery, app_src_main_res_mipmap_xxhdpi_ic_launcher_gallery_round [EXTRACTED 1.00]

## Communities (92 total, 57 thin omitted)

### Community 2 - "GalleryDao"
Cohesion: 0.09
Nodes (3): MainActivity, applyNightMode(), GalleryTheme()

### Community 3 - "GalleryNavHost.kt"
Cohesion: 0.09
Nodes (9): DuplicateWaitingScreen(), ScreenShortScreen(), CollageMakerScreen(), WallpaperScreen(), AnimatedMediaScreen(), StoryViewScreen(), GalleryNavHost(), WelcomeScreen() (+1 more)

### Community 4 - "MediaViewerScreen.kt"
Cohesion: 0.08
Nodes (13): BottomActionButton(), captureVideoFrame(), DetailSheetRow(), formatDuration(), formatFileSize(), MediaViewerScreen(), PagerMediaItemView(), saveCapturedFrameToGallery() (+5 more)

### Community 5 - "PhotoEditorScreen"
Cohesion: 0.53
Nodes (8): AdjustPanel(), BrushPanel(), CropPanel(), FilterPanel(), PhotoEditorScreen(), StickerPanel(), SubToolHeader(), TextPanel()

### Community 6 - "MediaItem"
Cohesion: 0.05
Nodes (17): MediaStoreScanner, MediaRepository, MediaRepositoryImpl, Album, MediaItem, MomentGroup, MomentsScreen(), AlbumCard() (+9 more)

### Community 7 - "Screen"
Cohesion: 0.07
Nodes (30): About, AnimatedMedia, BurstPhotos, Cleaner, CollageMaker, Duplicates, DuplicateWaiting, Favorites (+22 more)

### Community 9 - "ImageSegmentationHelper"
Cohesion: 0.11
Nodes (10): BlurBgScreen(), ColorSplashScreen(), ImageSegmentationHelper, ColorPicker, CustomImage, Gallery, RemoveBgChoice, RemoveBgScreen() (+2 more)

### Community 10 - "ImageCropView.kt"
Cohesion: 0.11
Nodes (12): AdjustParam, BRIGHTNESS, CONTRAST, SATURATION, SHARPEN, EditorTab, ADJUST, BRUSH (+4 more)

### Community 11 - "Converters"
Cohesion: 0.10
Nodes (11): Converters, ConvertersTest, Current Phase: Phase J Completed (Full Implementation, Unit Tests, and Build Verification), Project State, 1. Build Verification Summary, 2. Test Suite Execution Details, 3. AAPT Resource Compilation & Vector Verification, 4. Verification Conclusion (+3 more)

### Community 13 - "AppPreferences"
Cohesion: 0.14
Nodes (4): CropGeometry, drawCornerBrackets(), drawEdgeBars(), ImageCropView()

### Community 14 - "Test"
Cohesion: 0.10
Nodes (3): AppPreferences, Keys, ThemeScreen()

### Community 15 - "Converters"
Cohesion: 0.35
Nodes (5): applyAllTransformations(), DrawnPath, PlacedSticker, PlacedText, saveEditedBitmap()

### Community 16 - "CleanerTest.kt"
Cohesion: 0.15
Nodes (9): formatVideoDuration(), formatVideoSize(), LargeVideoScreen(), formatStorageBytes(), JunkCategory, JunkCategoryItem, ReleaseOptionRow(), RemoveJunkScreen() (+1 more)

### Community 20 - "VaultRepository"
Cohesion: 0.13
Nodes (9): VaultRepository, VaultRepositoryImpl, MediaType, ANIMATED, BURST, IMAGE, SCREENSHOT, VIDEO (+1 more)

### Community 21 - "HomeViewModel"
Cohesion: 0.06
Nodes (17): SortDirection, ASCENDING, DESCENDING, SortField, DATE_MODIFIED, DATE_TAKEN, NAME, SIZE (+9 more)

### Community 23 - "GalleryDatabase"
Cohesion: 0.17
Nodes (5): GalleryDatabase, Complete Android APK Reconstruction: Gallery App, Core Feature Scope, Project Overview, Technical Stack

### Community 24 - "HomeScreen"
Cohesion: 0.28
Nodes (5): ExploreHeroCard(), ExploreTab(), HomeScreen(), SelectionActionItem(), shareSelectedMedia()

### Community 26 - "CropHandle"
Cohesion: 0.22
Nodes (5): CleanerCategoryCard(), CleanerScreen(), DuplicatePhotoScreen(), DISC-005: System Cleaner Screen Structure, DISC-007: Duplicate Photo Finder Navigation

### Community 28 - "PhotoEditorScreen"
Cohesion: 0.20
Nodes (10): CropHandle, BOTTOM, BOTTOM_LEFT, BOTTOM_RIGHT, CENTER, LEFT, RIGHT, TOP (+2 more)

### Community 29 - "CropRatio"
Cohesion: 0.20
Nodes (10): EditorFilter, COOL, CYBERPUNK, FADE, GRAYSCALE, NONE, SEPIA, VINTAGE (+2 more)

### Community 30 - "SplashScreen.kt"
Cohesion: 0.21
Nodes (3): checkStoragePermissions(), SplashScreen(), WavyProgressIndicator()

### Community 31 - "Detailed Discrepancy Records"
Cohesion: 0.22
Nodes (9): Discrepancy Register, Detailed Discrepancy Records, DISC-001: Explore Tab Structure & Content, DISC-002: Albums Tab Top Feature Bar & Media Types, DISC-006: Reference Vector Drawables Missing, DISC-008: App Icons & Shortcuts, DISC-009: Cloud Sync & Third-Party Ad SDKs, Discrepancy Register: Forensic Audit & Reconstruction (+1 more)

### Community 34 - "Test"
Cohesion: 0.22
Nodes (8): 1. Architectural Integrity & Separation of Concerns, 2. Security & Privacy Audit, 3. Lifecycle & Memory Management, 4. Static Analysis & Tooling Status, Independent Code Review & Architecture Audit, Positive Findings, Positive Findings, Positive Findings

### Community 35 - "Test"
Cohesion: 0.17
Nodes (10): GalleryTopBar(), DISC-003: Home Toolbar & Vault Trigger, 1. Home Dashboard: Albums Tab, 2. Home Dashboard: Explore Tab, 4. Home Top App Bar, 5. System Cleaner Screen, Design System Tokens Summary, Evaluation Methodology (+2 more)

### Community 36 - "final-readiness-report.md"
Cohesion: 0.11
Nodes (14): 1. Executive Summary, 2. Artifact & Cryptographic Baseline, 3. Discrepancy Resolution & Audit Summary, 4. Verification & Quality Gates, 5. Final Sign-off, Final Readiness & Reconstruction Sign-off Report, 1. Intentionally Excluded Proprietary Subsystems, 2. Inferred Behavioral Nuances (+6 more)

### Community 38 - "FavoritesScreen"
Cohesion: 0.19
Nodes (8): AlbumDetailScreen(), shareSelectedMedia(), PhotoGridItem(), PhotosTab(), Detailed Route Specifications, Navigation Graph Topology, Navigation Map & Architecture, Special Transitions & Lifecycle Behaviors

### Community 40 - "CropRatio"
Cohesion: 0.22
Nodes (9): CropRatio, FIVE_FOUR, FOUR_FIVE, FOUR_THREE, FREE, NINE_SIXTEEN, ONE_ONE, SIXTEEN_NINE (+1 more)

### Community 45 - "GalleryDrawer"
Cohesion: 0.38
Nodes (6): DrawerItemData, DrawerRow(), GalleryBottomNav(), GalleryDrawer(), DISC-004: Navigation Drawer Alignment, 3. Navigation Drawer

### Community 46 - "Feature Matrix"
Cohesion: 0.50
Nodes (4): Feature Tracker, Evidence Classification Key, Feature Matrix, Feature Tracker: Observable Parity & Implementation Evidence

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
- **78 isolated node(s):** `IMAGE`, `VIDEO`, `SCREENSHOT`, `ANIMATED`, `BURST` (+73 more)
  These have ≤1 connection - possible missing edges or undocumented components. (Counts symbols only; 352 node(s) total have ≤1 connection when file, concept and rationale nodes are included.)
- **57 thin communities (<3 nodes) omitted from report** — run `graphify query` to explore isolated nodes.

## Suggested Questions
_Questions this graph is uniquely positioned to answer:_

- **Why does `MediaRepository` connect `MediaItem` to `Dp`, `BurstPhotoScreen`, `Editor`, `GalleryNavHost.kt`, `MediaViewerScreen.kt`, `PhotoEditorScreen`, `FavoritesScreen`, `Test`, `ImageSegmentationHelper`, `OrganizerRecycleScreen`, `Converters`, `CleanerTest.kt`, `FavoritesScreen`, `RecycleScreen`, `HomeViewModel`, `SearchColorScreen`, `DuplicateTest`, `CropHandle`?**
  _High betweenness centrality (0.124) - this node is a cross-community bridge._
- **Are the 2 inferred relationships involving `GalleryNavHost()` (e.g. with `Positive Findings` and `DISC-007: Duplicate Photo Finder Navigation`) actually correct?**
  _`GalleryNavHost()` has 2 INFERRED edges - model-reasoned connections that need verification._
- **What connects `IMAGE`, `VIDEO`, `SCREENSHOT` to the rest of the system?**
  _78 weakly-connected nodes found - possible documentation gaps or missing edges._
- **Should `Dp` be split into smaller, more focused modules?**
  _Cohesion score 0.08066770186335404 - nodes in this community are weakly interconnected._
- **Why does `Screen` connect `Screen` to `Dp`, `MediaViewerScreen.kt`, `Converters`, `CleanerTest.kt`, `DuplicateTest`, `SplashScreen.kt`, `formatGroupHeader`, `RecycleTest.kt`, `StorageCleanPhase`, `Test`, `MediaViewer`, `SearchColor`, `StoryView`, `Wallpaper`, `app/build.gradle.kts`, `build.gradle.kts`, `settings.gradle.kts`, `RemoveBg`, `SearchColor`, `StoryView`, `Wallpaper`?**
  _High betweenness centrality (0.119) - this node is a cross-community bridge._
- **Should `GalleryDao` be split into smaller, more focused modules?**
  _Cohesion score 0.08866995073891626 - nodes in this community are weakly interconnected._
- **Why does `MediaItem` connect `MediaItem` to `Dp`, `FavoritesTest`, `GalleryNavHost.kt`, `MediaViewerScreen.kt`, `formatGroupHeader`, `FavoritesScreen`, `RecycleTest.kt`, `CleanerTest.kt`, `CleanerTest.kt`, `RecycleScreen`, `VaultEntity`, `VaultRepository`, `HomeViewModel`, `DuplicateTest`?**
  _High betweenness centrality (0.112) - this node is a cross-community bridge._