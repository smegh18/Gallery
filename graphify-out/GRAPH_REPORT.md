# Graph Report - Gallery  (2026-11-03)

## Corpus Check
- 111 files · ~83,118 words
- Verdict: corpus is large enough that graph structure adds value.
- Unclassified: 270 file(s) not represented in the graph (top: .xml 262, .properties 2, (none) 1)

## Summary
- 1091 nodes · 3075 edges · 88 communities (40 shown, 48 thin omitted)
- Extraction: 99% EXTRACTED · 1% INFERRED · 0% AMBIGUOUS · INFERRED: 32 edges (avg confidence: 0.92)
- Token cost: 0 input · 0 output

## Graph Freshness
- Built from commit: `7f3dd6cd`
- Run `git rev-parse HEAD` and compare to check if the graph is stale.
- Run `graphify update .` after code changes (no API cost).

## Community Hubs (Navigation)
- Dp
- GalleryDao
- GalleryDao
- GalleryNavHost.kt
- MediaViewerScreen.kt
- Test
- MediaItem
- Screen
- Activity
- ImageSegmentationHelper
- ImageCropView.kt
- Converters
- MediaType
- AppPreferences
- Test
- PhotoEditorScreen.kt
- CleanerTest.kt
- Test
- RecycleScreen
- MediaStoreThumbnailFetcher.kt
- VaultRepository
- HomeViewModel
- GalleryRulerView.kt
- VaultMediaScreen
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
- 1. Intentionally Excluded Proprietary Subsystems
- Final Readiness & Reconstruction Sign-off Report
- AlbumDetailScreen
- RecycleTest.kt
- CropRatio
- StorageCleanPhase
- NativeAdCard.kt
- Test
- final-readiness-report.md
- GalleryDrawer
- GalleryTopBar
- VaultRecoveryQuestionScreen
- VaultSettingScreen
- gradlew
- FavoritesScreen
- DuplicateCompleteScreen
- CollageStep
- StickerScreen
- WallpaperScreen
- AnimatedMediaScreen
- AboutScreen
- Screen.kt
- VaultRecoveryQuestionScreen
- VaultSettingScreen
- ColorSplash
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
- `DISC-004: Navigation Drawer Alignment` --references--> `GalleryDrawer()`  [INFERRED]
  discrepancy-register.md → app/src/main/java/com/quizango/gallery/ui/components/NavigationComponents.kt
- `1. Primary User Journey Screens` --references--> `GalleryDrawer()`  [INFERRED]
  screen-inventory.md → app/src/main/java/com/quizango/gallery/ui/components/NavigationComponents.kt
- `3. Navigation Drawer` --references--> `GalleryDrawer()`  [INFERRED]
  screenshot-comparison-report.md → app/src/main/java/com/quizango/gallery/ui/components/NavigationComponents.kt
- `Technical Stack` --references--> `GalleryDatabase`  [INFERRED]
  .planning/PROJECT.md → app/src/main/java/com/quizango/gallery/data/local/database/GalleryDatabase.kt

## Import Cycles
- None detected.

## Hyperedges (group relationships)
- **Application Launcher Icons** — app_src_main_res_mipmap_hdpi_ic_launcher_gallery_round, app_src_main_res_mipmap_hdpi_ic_launcher_round_gallery, app_src_main_res_mipmap_mdpi_ic_launcher_gallery, app_src_main_res_mipmap_xhdpi_ic_launcher_gallery, app_src_main_res_mipmap_xhdpi_ic_launcher_gallery_round, app_src_main_res_mipmap_xhdpi_ic_launcher_round_gallery, app_src_main_res_mipmap_xxhdpi_ic_launcher_gallery, app_src_main_res_mipmap_xxhdpi_ic_launcher_gallery_round [EXTRACTED 1.00]

## Communities (88 total, 48 thin omitted)

### Community 0 - "Dp"
Cohesion: 0.08
Nodes (3): StickerItem, MediaTypeItem, LanguageOption

### Community 1 - "GalleryDao"
Cohesion: 0.07
Nodes (13): GalleryDao, VaultDao, MediaEntity, VaultEntity, GalleryDatabase, Feature Tracker, Evidence Classification Key, Feature Matrix (+5 more)

### Community 3 - "GalleryNavHost.kt"
Cohesion: 0.07
Nodes (11): DuplicateCompleteScreen(), DuplicateWaitingScreen(), ScreenShortScreen(), CollageMakerScreen(), StickerScreen(), OrganizerRecycleScreen(), QuickOrganizerScreen(), SearchColorScreen() (+3 more)

### Community 4 - "MediaViewerScreen.kt"
Cohesion: 0.11
Nodes (9): BottomActionButton(), captureVideoFrame(), DetailSheetRow(), MediaViewerScreen(), PagerMediaItemView(), saveCapturedFrameToGallery(), shareMedia(), SuggestionPill() (+1 more)

### Community 5 - "Test"
Cohesion: 0.20
Nodes (3): formatDuration(), formatFileSize(), MediaViewerTest

### Community 6 - "MediaItem"
Cohesion: 0.06
Nodes (16): MediaStoreScanner, MediaRepository, MediaRepositoryImpl, Album, MediaItem, MomentGroup, MomentsScreen(), AlbumCard() (+8 more)

### Community 7 - "Screen"
Cohesion: 0.07
Nodes (30): About, AnimatedMedia, BurstPhotos, Cleaner, CollageMaker, Duplicates, DuplicateWaiting, Favorites (+22 more)

### Community 9 - "ImageSegmentationHelper"
Cohesion: 0.06
Nodes (10): BlurBgScreen(), ColorSplashScreen(), ImageSegmentationHelper, ColorPicker, CustomImage, Gallery, RemoveBgChoice, RemoveBgScreen() (+2 more)

### Community 10 - "ImageCropView.kt"
Cohesion: 0.11
Nodes (12): AdjustParam, BRIGHTNESS, CONTRAST, SATURATION, SHARPEN, EditorTab, ADJUST, BRUSH (+4 more)

### Community 11 - "Converters"
Cohesion: 0.10
Nodes (11): Converters, ConvertersTest, Current Phase: Phase J Completed (Full Implementation, Unit Tests, and Build Verification), Project State, 1. Build Verification Summary, 2. Test Suite Execution Details, 3. AAPT Resource Compilation & Vector Verification, 4. Verification Conclusion (+3 more)

### Community 12 - "MediaType"
Cohesion: 0.16
Nodes (6): MediaType, ANIMATED, BURST, IMAGE, SCREENSHOT, VIDEO

### Community 13 - "AppPreferences"
Cohesion: 0.14
Nodes (4): CropGeometry, drawCornerBrackets(), drawEdgeBars(), ImageCropView()

### Community 14 - "Test"
Cohesion: 0.09
Nodes (4): AppPreferences, Keys, LanguageScreen(), VaultForgotPasswordScreen()

### Community 15 - "PhotoEditorScreen.kt"
Cohesion: 0.21
Nodes (13): AdjustPanel(), applyAllTransformations(), BrushPanel(), CropPanel(), DrawnPath, FilterPanel(), PhotoEditorScreen(), PlacedSticker (+5 more)

### Community 16 - "CleanerTest.kt"
Cohesion: 0.15
Nodes (9): formatVideoDuration(), formatVideoSize(), LargeVideoScreen(), formatStorageBytes(), JunkCategory, JunkCategoryItem, ReleaseOptionRow(), RemoveJunkScreen() (+1 more)

### Community 20 - "VaultRepository"
Cohesion: 0.23
Nodes (4): VaultRepository, VaultRepositoryImpl, VaultItem, toVaultItem()

### Community 21 - "HomeViewModel"
Cohesion: 0.06
Nodes (14): SortDirection, ASCENDING, DESCENDING, SortField, DATE_MODIFIED, DATE_TAKEN, NAME, SIZE (+6 more)

### Community 23 - "VaultMediaScreen"
Cohesion: 0.40
Nodes (3): shareSelectedVaultMedia(), VaultGridItem(), VaultMediaScreen()

### Community 24 - "HomeScreen"
Cohesion: 0.24
Nodes (6): GalleryBottomNav(), ExploreHeroCard(), ExploreTab(), HomeScreen(), SelectionActionItem(), shareSelectedMedia()

### Community 25 - "DuplicateTest"
Cohesion: 0.17
Nodes (3): GalleryApplication, ThemeScreen(), applyNightMode()

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
Cohesion: 0.15
Nodes (4): PermissionScreen(), checkStoragePermissions(), SplashScreen(), WavyProgressIndicator()

### Community 31 - "Detailed Discrepancy Records"
Cohesion: 0.29
Nodes (7): Detailed Discrepancy Records, DISC-001: Explore Tab Structure & Content, DISC-002: Albums Tab Top Feature Bar & Media Types, DISC-004: Navigation Drawer Alignment, DISC-006: Reference Vector Drawables Missing, DISC-008: App Icons & Shortcuts, DISC-009: Cloud Sync & Third-Party Ad SDKs

### Community 34 - "Test"
Cohesion: 0.22
Nodes (8): 1. Architectural Integrity & Separation of Concerns, 2. Security & Privacy Audit, 3. Lifecycle & Memory Management, 4. Static Analysis & Tooling Status, Independent Code Review & Architecture Audit, Positive Findings, Positive Findings, Positive Findings

### Community 35 - "Test"
Cohesion: 0.25
Nodes (8): 1. Home Dashboard: Albums Tab, 2. Home Dashboard: Explore Tab, 3. Navigation Drawer, 5. System Cleaner Screen, Design System Tokens Summary, Evaluation Methodology, Screen-by-Screen Layout Comparison, Screenshot & UI Layout Comparison Report

### Community 36 - "1. Intentionally Excluded Proprietary Subsystems"
Cohesion: 0.22
Nodes (8): 1. Intentionally Excluded Proprietary Subsystems, 2. Inferred Behavioral Nuances, INFERRED-01: System Cache File Calculation, INFERRED-02: Duplicate Photo Detection Heuristics, UNKNOWN-01: Commercial Ad Mediation & Banners, UNKNOWN-02: Third-Party Telemetry & Analytics, UNKNOWN-03: Google Play In-App Billing, Unresolved Issues & Boundary Classifications

### Community 37 - "Final Readiness & Reconstruction Sign-off Report"
Cohesion: 0.33
Nodes (6): 1. Executive Summary, 2. Artifact & Cryptographic Baseline, 3. Discrepancy Resolution & Audit Summary, 4. Verification & Quality Gates, 5. Final Sign-off, Final Readiness & Reconstruction Sign-off Report

### Community 38 - "AlbumDetailScreen"
Cohesion: 0.11
Nodes (12): AlbumDetailScreen(), shareSelectedMedia(), formatGroupHeader(), getMidnightBoundaries(), getTodayMidnight(), PhotoGridItem(), PhotosTab(), AlbumDetailTest (+4 more)

### Community 40 - "CropRatio"
Cohesion: 0.22
Nodes (9): CropRatio, FIVE_FOUR, FOUR_FIVE, FOUR_THREE, FREE, NINE_SIXTEEN, ONE_ONE, SIXTEEN_NINE (+1 more)

### Community 44 - "final-readiness-report.md"
Cohesion: 0.40
Nodes (3): Discrepancy Register, Discrepancy Register: Forensic Audit & Reconstruction, Issue Summary

### Community 45 - "GalleryDrawer"
Cohesion: 0.83
Nodes (3): DrawerItemData, DrawerRow(), GalleryDrawer()

### Community 46 - "GalleryTopBar"
Cohesion: 0.50
Nodes (3): GalleryTopBar(), DISC-003: Home Toolbar & Vault Trigger, 4. Home Top App Bar

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
  These have ≤1 connection - possible missing edges or undocumented components. (Counts symbols only; 356 node(s) total have ≤1 connection when file, concept and rationale nodes are included.)
- **48 thin communities (<3 nodes) omitted from report** — run `graphify query` to explore isolated nodes.

## Suggested Questions
_Questions this graph is uniquely positioned to answer:_

- **Why does `MediaRepository` connect `MediaItem` to `Dp`, `BurstPhotoScreen`, `Test`, `GalleryNavHost.kt`, `MediaViewerScreen.kt`, `AlbumDetailScreen`, `ImageSegmentationHelper`, `PhotoEditorScreen.kt`, `CleanerTest.kt`, `FavoritesScreen`, `RecycleScreen`, `HomeViewModel`, `AnimatedMediaScreen`, `DuplicateTest`, `CropHandle`?**
  _High betweenness centrality (0.133) - this node is a cross-community bridge._
- **Are the 2 inferred relationships involving `GalleryNavHost()` (e.g. with `Positive Findings` and `DISC-007: Duplicate Photo Finder Navigation`) actually correct?**
  _`GalleryNavHost()` has 2 INFERRED edges - model-reasoned connections that need verification._
- **What connects `IMAGE`, `VIDEO`, `SCREENSHOT` to the rest of the system?**
  _78 weakly-connected nodes found - possible documentation gaps or missing edges._
- **Should `Dp` be split into smaller, more focused modules?**
  _Cohesion score 0.08158883521202362 - nodes in this community are weakly interconnected._
- **Why does `MediaItem` connect `MediaItem` to `Dp`, `GalleryDao`, `FavoritesTest`, `GalleryNavHost.kt`, `MediaViewerScreen.kt`, `Test`, `AlbumDetailScreen`, `RecycleTest.kt`, `MediaType`, `CleanerTest.kt`, `RecycleScreen`, `VaultRepository`, `HomeViewModel`, `DuplicateTest`?**
  _High betweenness centrality (0.120) - this node is a cross-community bridge._
- **Should `GalleryDao` be split into smaller, more focused modules?**
  _Cohesion score 0.07049180327868852 - nodes in this community are weakly interconnected._
- **Why does `Screen` connect `Screen` to `Dp`, `MediaViewerScreen.kt`, `Test`, `PhotoEditorScreen.kt`, `CleanerTest.kt`, `DuplicateTest`, `SplashScreen.kt`, `AlbumDetailScreen`, `RecycleTest.kt`, `StorageCleanPhase`, `Test`, `MediaViewer`, `SearchColor`, `StoryView`, `Wallpaper`, `app/build.gradle.kts`, `build.gradle.kts`, `settings.gradle.kts`, `RemoveBg`, `SearchColor`, `StoryView`, `Wallpaper`?**
  _High betweenness centrality (0.112) - this node is a cross-community bridge._