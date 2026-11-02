# Graph Report - Gallery  (2026-11-02)

## Corpus Check
- cluster-only mode — file stats not available

## Summary
- 976 nodes · 2942 edges · 73 communities (25 shown, 48 thin omitted)
- Extraction: 100% EXTRACTED · 0% INFERRED · 0% AMBIGUOUS · INFERRED: 9 edges (avg confidence: 0.85)
- Token cost: 0 input · 0 output

## Graph Freshness
- Built from commit: `8a86c15b`
- Run `git rev-parse HEAD` and compare to check if the graph is stale.
- Run `graphify update .` after code changes (no API cost).

## Community Hubs (Navigation)
- Dp
- HomeScreen
- GalleryDao
- MediaType
- MediaViewerScreen.kt
- ExploreTab.kt
- GalleryNavHost.kt
- Screen
- MediaItem
- ImageSegmentationHelper
- ImageCropView.kt
- CleanerTest.kt
- AppPreferences
- Test
- Converters
- HomeViewModel.kt
- HomeViewModel
- GalleryRulerView.kt
- SplashScreen.kt
- MainActivity.kt
- EditorTab
- MediaRepositoryImpl
- PhotoEditorTest.kt
- PhotoEditorScreen.kt
- DuplicateTest
- CropHandle
- EditorFilter
- PhotoEditorScreen
- CropRatio
- FavoritesTest
- Album
- RecycleTest.kt
- DomainModelTest.kt
- Test
- Test
- MediaRepository.kt
- BannerAdView.kt
- FavoritesScreen
- SettingsScreen
- DuplicatePhotoScreen
- StorageCleanPhase
- NativeAdCard.kt
- MomentsScreen
- RecycleScreen
- AboutScreen
- VaultLockScreen
- VaultRecoveryQuestionScreen
- VaultSettingScreen
- CleanerScreen
- DuplicateCompleteScreen
- CollageStep
- StickerScreen
- OrganizerRecycleScreen
- QuickOrganizerScreen
- SearchColorScreen
- ThemeScreen
- VaultForgotPasswordScreen
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

## God Nodes (most connected - your core abstractions)
1. `MediaItem` - 82 edges
2. `MediaRepository` - 63 edges
3. `Screen` - 60 edges
4. `GalleryNavHost()` - 46 edges
5. `AppPreferences` - 41 edges
6. `GalleryDao` - 29 edges
7. `MediaType` - 29 edges
8. `HomeViewModel` - 24 edges
9. `MediaEntity` - 24 edges
10. `MediaRepositoryImpl` - 24 edges

## Surprising Connections (you probably didn't know these)
- `SearchScreen()` --calls--> `SolidColor`  [INFERRED]
  app/src/main/java/com/quizango/gallery/ui/search/SearchScreen.kt → app/src/main/java/com/quizango/gallery/ui/creative/RemoveBgScreen.kt
- `HomeScreen()` --calls--> `ExploreTab()`  [INFERRED]
  app/src/main/java/com/quizango/gallery/ui/home/HomeScreen.kt → app/src/main/java/com/quizango/gallery/ui/home/ExploreTab.kt
- `ScreenShortScreen()` --calls--> `formatVideoSize()`  [INFERRED]
  app/src/main/java/com/quizango/gallery/ui/cleaner/ScreenShortScreen.kt → app/src/main/java/com/quizango/gallery/ui/cleaner/LargeVideoScreen.kt
- `BlurBgScreen()` --calls--> `GalleryRulerView()`  [INFERRED]
  app/src/main/java/com/quizango/gallery/ui/creative/BlurBgScreen.kt → app/src/main/java/com/quizango/gallery/ui/creative/GalleryRulerView.kt
- `ColorSplashScreen()` --calls--> `GalleryRulerView()`  [INFERRED]
  app/src/main/java/com/quizango/gallery/ui/creative/ColorSplashScreen.kt → app/src/main/java/com/quizango/gallery/ui/creative/GalleryRulerView.kt

## Import Cycles
- None detected.

## Communities (73 total, 48 thin omitted)

### Community 0 - "Dp"
Cohesion: 0.08
Nodes (3): StickerItem, MediaTypeItem, LanguageOption

### Community 1 - "HomeScreen"
Cohesion: 0.06
Nodes (21): AlbumDetailScreen(), shareSelectedMedia(), DrawerItemData, DrawerRow(), GalleryBottomNav(), GalleryDrawer(), GalleryTopBar(), AlbumCard() (+13 more)

### Community 2 - "GalleryDao"
Cohesion: 0.09
Nodes (5): GalleryDao, VaultDao, MediaEntity, VaultEntity, GalleryDatabase

### Community 3 - "MediaType"
Cohesion: 0.06
Nodes (16): VaultRepository, VaultRepositoryImpl, MediaType, ANIMATED, BURST, IMAGE, SCREENSHOT, VIDEO (+8 more)

### Community 4 - "MediaViewerScreen.kt"
Cohesion: 0.08
Nodes (12): BottomActionButton(), captureVideoFrame(), DetailSheetRow(), formatDuration(), formatFileSize(), MediaViewerScreen(), PagerMediaItemView(), saveCapturedFrameToGallery() (+4 more)

### Community 5 - "ExploreTab.kt"
Cohesion: 0.09
Nodes (7): AdManager, ExploreTab(), FeatureCard(), ManageRow(), SectionLabel(), TileButton(), GalleryTheme()

### Community 6 - "GalleryNavHost.kt"
Cohesion: 0.08
Nodes (10): DuplicateWaitingScreen(), ScreenShortScreen(), CollageMakerScreen(), WallpaperScreen(), AnimatedMediaScreen(), BurstPhotoScreen(), StoryViewScreen(), GalleryNavHost() (+2 more)

### Community 7 - "Screen"
Cohesion: 0.07
Nodes (30): About, AnimatedMedia, BurstPhotos, Cleaner, CollageMaker, Duplicates, DuplicateWaiting, Favorites (+22 more)

### Community 9 - "ImageSegmentationHelper"
Cohesion: 0.11
Nodes (10): BlurBgScreen(), ColorSplashScreen(), ImageSegmentationHelper, ColorPicker, CustomImage, Gallery, RemoveBgChoice, RemoveBgScreen() (+2 more)

### Community 10 - "ImageCropView.kt"
Cohesion: 0.12
Nodes (4): CropGeometry, drawCornerBrackets(), drawEdgeBars(), ImageCropView()

### Community 12 - "CleanerTest.kt"
Cohesion: 0.15
Nodes (9): formatVideoDuration(), formatVideoSize(), LargeVideoScreen(), formatStorageBytes(), JunkCategory, JunkCategoryItem, ReleaseOptionRow(), RemoveJunkScreen() (+1 more)

### Community 13 - "AppPreferences"
Cohesion: 0.10
Nodes (3): AppPreferences, Keys, WelcomeScreen()

### Community 16 - "HomeViewModel.kt"
Cohesion: 0.17
Nodes (9): SortDirection, ASCENDING, DESCENDING, SortField, DATE_MODIFIED, DATE_TAKEN, NAME, SIZE (+1 more)

### Community 19 - "SplashScreen.kt"
Cohesion: 0.21
Nodes (3): checkStoragePermissions(), SplashScreen(), WavyProgressIndicator()

### Community 21 - "EditorTab"
Cohesion: 0.15
Nodes (12): AdjustParam, BRIGHTNESS, CONTRAST, SATURATION, SHARPEN, EditorTab, ADJUST, BRUSH (+4 more)

### Community 23 - "PhotoEditorTest.kt"
Cohesion: 0.24
Nodes (3): DrawnPath, PlacedSticker, PlacedText

### Community 26 - "CropHandle"
Cohesion: 0.20
Nodes (10): CropHandle, BOTTOM, BOTTOM_LEFT, BOTTOM_RIGHT, CENTER, LEFT, RIGHT, TOP (+2 more)

### Community 27 - "EditorFilter"
Cohesion: 0.20
Nodes (10): EditorFilter, COOL, CYBERPUNK, FADE, GRAYSCALE, NONE, SEPIA, VINTAGE (+2 more)

### Community 28 - "PhotoEditorScreen"
Cohesion: 0.53
Nodes (8): AdjustPanel(), BrushPanel(), CropPanel(), FilterPanel(), PhotoEditorScreen(), StickerPanel(), SubToolHeader(), TextPanel()

### Community 29 - "CropRatio"
Cohesion: 0.22
Nodes (9): CropRatio, FIVE_FOUR, FOUR_FIVE, FOUR_THREE, FREE, NINE_SIXTEEN, ONE_ONE, SIXTEEN_NINE (+1 more)

### Community 31 - "Album"
Cohesion: 0.36
Nodes (5): Album, HomeUiState, SearchAlbumCard(), SearchMediaCard(), SearchScreen()

### Community 38 - "FavoritesScreen"
Cohesion: 0.33
Nodes (3): FavoritesScreen(), formatFavoriteDuration(), shareFavoriteMedia()

### Community 39 - "SettingsScreen"
Cohesion: 0.53
Nodes (4): RateUsDialog(), SettingDivider(), SettingNavRow(), SettingsScreen()

### Community 41 - "StorageCleanPhase"
Cohesion: 0.40
Nodes (5): StorageCleanPhase, CLEANING, COMPLETED, OVERVIEW, SCANNING

### Community 52 - "CollageStep"
Cohesion: 0.67
Nodes (3): CollageStep, EDITOR, PICKER

## Knowledge Gaps
- **66 isolated node(s):** `Screenshots`, `Search`, `Settings`, `Splash`, `Stickers` (+61 more)
  These have ≤1 connection - possible missing edges. (Counts symbols only; 294 node(s) total have ≤1 connection when file, concept and rationale nodes are included.)
- **48 thin communities (<3 nodes) omitted from report** — run `graphify query` to explore isolated nodes.

## Suggested Questions
_Questions this graph is uniquely positioned to answer:_

- **Why does `Screen` connect `Screen` to `Dp`, `HomeScreen`, `MediaType`, `MediaViewerScreen.kt`, `ExploreTab.kt`, `CleanerTest.kt`, `SplashScreen.kt`, `PhotoEditorScreen.kt`, `DuplicateTest`, `RecycleTest.kt`, `Test`, `Screen.kt`, `AlbumDetail`, `BlurBg`, `ColorSplash`, `DuplicateComplete`, `Editor`, `MediaViewer`, `RemoveBg`, `SearchColor`, `StoryView`, `Wallpaper`?**
  _High betweenness centrality (0.134) - this node is a cross-community bridge._
- **What connects `Screenshots`, `Search`, `Settings` to the rest of the system?**
  _66 weakly-connected nodes found - possible missing edges._
- **Should `Dp` be split into smaller, more focused modules?**
  _Cohesion score 0.08020857296219615 - nodes in this community are weakly interconnected._
- **Why does `MediaItem` connect `MediaItem` to `Dp`, `HomeScreen`, `GalleryDao`, `MediaType`, `MediaViewerScreen.kt`, `GalleryNavHost.kt`, `ImageSegmentationHelper.kt`, `CleanerTest.kt`, `HomeViewModel.kt`, `HomeViewModel`, `MediaRepositoryImpl`, `DuplicateTest`, `FavoritesTest`, `Album`, `RecycleTest.kt`, `DomainModelTest.kt`, `MediaRepository.kt`, `MomentsScreen`, `RecycleScreen`?**
  _High betweenness centrality (0.130) - this node is a cross-community bridge._
- **Should `HomeScreen` be split into smaller, more focused modules?**
  _Cohesion score 0.06108597285067873 - nodes in this community are weakly interconnected._
- **Why does `MediaRepository` connect `MediaItem` to `Dp`, `HomeScreen`, `MediaType`, `MediaViewerScreen.kt`, `GalleryNavHost.kt`, `ImageSegmentationHelper`, `CleanerTest.kt`, `HomeViewModel.kt`, `HomeViewModel`, `MediaRepositoryImpl`, `PhotoEditorScreen.kt`, `PhotoEditorScreen`, `Album`, `MediaRepository.kt`, `FavoritesScreen`, `DuplicatePhotoScreen`, `MomentsScreen`, `RecycleScreen`, `OrganizerRecycleScreen`, `QuickOrganizerScreen`, `SearchColorScreen`?**
  _High betweenness centrality (0.107) - this node is a cross-community bridge._
- **Should `GalleryDao` be split into smaller, more focused modules?**
  _Cohesion score 0.09254901960784313 - nodes in this community are weakly interconnected._