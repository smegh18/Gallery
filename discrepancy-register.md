# Discrepancy Register: Forensic Audit & Reconstruction

This register documents all identified discrepancies between the reference Android APK (`albums.gallery.photo.folder.picasa.app.web.gallery` v2.6.7, SHA-256 `a4700dae2133b911cd36e77b4bbc110e8269038ed4c29d932094fd0fe6bf7134`) and the reconstructed native Kotlin/Compose implementation.

---

## Issue Summary

| ID | Screen / Feature | Severity | Status | Summary |
|---|---|---|---|---|
| **DISC-001** | Explore Tab Structure & Content | HIGH | **RESOLVED** | Generic 2-column grid vs. reference 3 hero cards (`fragment_explore.xml`) |
| **DISC-002** | Albums Tab Top Feature Bar | HIGH | **RESOLVED** | Missing horizontal carousel with 10 features & Media Types list (`fragment_album.xml`) |
| **DISC-003** | Home Toolbar & Vault Trigger | MEDIUM | **RESOLVED** | Missing `ic_home_lock` vault trigger and exact reference action icons (`activity_home.xml`) |
| **DISC-004** | Navigation Drawer Alignment | HIGH | **RESOLVED** | Generic Material icons vs. exact reference drawables & "Go Pro" banner (`drawer_layout.xml`) |
| **DISC-005** | System Cleaner Structure | MEDIUM | **RESOLVED** | Simulated categories vs. exact reference `item_remove_junk` + 3 category cards (`activity_system_cleaner.xml`) |
| **DISC-006** | Reference Vector Drawables | HIGH | **RESOLVED** | Missing 35+ reference vector drawables in reconstructed `res/drawable` |
| **DISC-007** | Duplicate Photo Finder Navigation | MEDIUM | **RESOLVED** | Cleaner screen lacked direct link to Duplicate Photo Finder |
| **DISC-008** | App Icons & Shortcuts | LOW | **RESOLVED** | Generic Android robot icons vs. authentic Gallery launcher & shortcuts (`shortcuts.xml`) |
| **DISC-009** | Cloud Sync & Firebase Backend | LOW | **CLASSIFIED (UNKNOWN)** | Proprietary third-party ad/analytics SDKs omitted by design per privacy/clean reconstruction |

---

## Detailed Discrepancy Records

### DISC-001: Explore Tab Structure & Content
- **Screen / Feature**: Explore Tab (`ExploreTab.kt`)
- **Reference Evidence**:
  - `fragment_explore.xml` contains a single-column `NestedScrollView` with 3 prominent rounded hero cards (`layout_shape`):
    1. `item_system_cleaner` with icon `system_cleaner_main_icon`, title `@string/tool_bar_system_cleaner`, subtitle `@string/txt_cleaner_sub`, and orange arrow `system_cleaner_banner_arrow_new_ornage`.
    2. `item_organizer` with icon `ic_organier_explore`, title `@string/organizer_explore_title`, subtitle `@string/organizer_explore_sub_title`, and arrow `system_cleaner_banner_arrow_new`.
    3. `item_collage_maker` with icon `ic_collage_explore`, title `@string/collage_maker_explore_title`, subtitle `@string/collage_maker_explore_sub_title`, "New" badge (`album_collage_new`), and arrow `system_cleaner_banner_arrow_new`.
- **Current Implementation**: A 2-column grid displaying 6 generic cards ("System Cleaner", "Duplicate Finder", "Private Vault", "Recycle Bin", "Favorites", "Photo Editor") with standard Material icons.
- **Exact Discrepancy**: Layout was completely mismatched in geometry (2-column grid vs. vertical card stack), content (6 items vs. 3 hero cards), and iconography.
- **Severity**: HIGH
- **Root Cause**: Initial scaffold used generic explore dashboard template instead of inspecting `fragment_explore.xml`.
- **Files Affected**:
  - `app/src/main/java/albums/gallery/photo/folder/picasa/app/web/gallery/ui/home/ExploreTab.kt`
- **Required Correction**: Refactored `ExploreTab.kt` to render exactly the 3 hero cards with authentic vector assets and typography matching `fragment_explore.xml`.
- **Acceptance Criteria**: Explore tab renders 3 full-width rounded cards with matching icons, subtitles, and click routing to Cleaner, Duplicates, and Editor.
- **Test Required**: UI preview / layout verification; Gradle assemble check.
- **Status**: **RESOLVED**

---

### DISC-002: Albums Tab Top Feature Bar & Media Types
- **Screen / Feature**: Albums Tab (`AlbumsTab.kt`)
- **Reference Evidence**:
  - `fragment_album.xml` and `AlbumsFragment.java` (lines 2620–2675):
    - `rvFeaturesLists`: Horizontal carousel of 10 items: Vault, Favorites, Cleaner, Duplicate, Recycle Bin, Collage, Editor, Videos, Place, Moments.
    - `mediaTitle` and `media_type_bg`: Media Types section at bottom containing 4 rows: Portrait (`ic_image_portrait`), High Definition (`ic_image_high_definition`), Burst (`ic_image_burst`), Animated (`ic_image_animated`).
- **Current Implementation**: Only contained a 3-column album grid without the top features carousel or the media types list.
- **Exact Discrepancy**: Top 10-feature navigation bar was missing; Media Types section was missing.
- **Severity**: HIGH
- **Root Cause**: Screen was implemented focusing solely on the Room album query without referencing `AlbumsFragment.java`.
- **Files Affected**:
  - `app/src/main/java/albums/gallery/photo/folder/picasa/app/web/gallery/ui/home/AlbumsTab.kt`
  - `app/src/main/java/albums/gallery/photo/folder/picasa/app/web/gallery/ui/home/HomeScreen.kt`
- **Required Correction**: Added `HOME_TOP_FEATURES` horizontal scroll row and `MEDIA_TYPES` bottom card with exact vector drawables and navigation routing.
- **Acceptance Criteria**: Top carousel renders all 10 features with correct drawables; clicks route to corresponding features; Media Types list renders 4 items.
- **Test Required**: Automated assemble & unit test run.
- **Status**: **RESOLVED**

---

### DISC-003: Home Toolbar & Vault Trigger
- **Screen / Feature**: Main Dashboard Top Bar (`GalleryTopBar`)
- **Reference Evidence**:
  - `activity_home.xml`: Toolbar features menu button with unread red dot (`ivDot`), bold uppercase "GALLERY" title in `@color/colorPrimary` (#3478F6), lock icon (`ic_home_lock`) adjacent to title (which triggers Vault / hint overlay), and action buttons: sort (`ic_short`), camera (`ic_camera`), search (`ic_toolbar_search`), and Pro (`ic_pro_new`).
- **Current Implementation**: Top bar used generic Material icons and simple "Albums"/"Photos"/"Explore" dynamic titles without lock trigger or Pro icon.
- **Exact Discrepancy**: Title was lowercase dynamic text; lock icon was missing; action icons were generic Material vector icons.
- **Severity**: MEDIUM
- **Root Cause**: Generic Material 3 `TopAppBar` scaffolding without applying reference layout attributes from `activity_home.xml`.
- **Files Affected**:
  - `app/src/main/java/albums/gallery/photo/folder/picasa/app/web/gallery/ui/components/NavigationComponents.kt`
  - `app/src/main/java/albums/gallery/photo/folder/picasa/app/web/gallery/ui/home/HomeScreen.kt`
- **Required Correction**: Updated `GalleryTopBar` to display bold uppercase "GALLERY" with `ic_home_lock` icon that navigates to Vault, red dot on menu icon, and authentic action drawables (`ic_short`, `ic_camera`, `ic_toolbar_search`, `ic_pro_new`).
- **Acceptance Criteria**: Matches reference toolbar appearance and actions; clicking lock icon navigates to Vault.
- **Test Required**: Compile and navigation verification.
- **Status**: **RESOLVED**

---

### DISC-004: Navigation Drawer Alignment
- **Screen / Feature**: Navigation Drawer (`GalleryDrawer`)
- **Reference Evidence**:
  - `drawer_layout.xml`: Header with "Go Pro" banner (`layout_settings_premium`), followed by:
    - Favorites (`ic_favorite`)
    - Create Album (`ic_create_album`)
    - Recycle Bin (`ic_delete`)
    - System Cleaner (`ic_drawer_cleaner`)
    - Duplicate Photos (`ic_duplicate`)
    - Private Vault (`ic_unhide`)
    - AI Photo Editor (`ic_drawer_editor`)
    - Change App Language (`ic_app_language`)
    - Theme Mode (`ic_theme_mode`)
    - Settings (`ic_settings`)
    - About Us (`ic_about`)
    - Follow Us social links (`ic_instagram`, `ic_youtube`, `ic_facebook`).
- **Current Implementation**: Generic drawer items with Material outline icons and no Go Pro banner or social footer.
- **Exact Discrepancy**: Missing visual identity, Go Pro banner, reference vector drawables, and social links.
- **Severity**: HIGH
- **Root Cause**: Rapid prototyping using default Compose `NavigationDrawerItem` icons.
- **Files Affected**:
  - `app/src/main/java/albums/gallery/photo/folder/picasa/app/web/gallery/ui/components/NavigationComponents.kt`
- **Required Correction**: Reconstructed `GalleryDrawer` with authentic "Go Premium" card, reference drawables, right chevron arrows, divider, and social links footer.
- **Acceptance Criteria**: Drawer items match reference ordering, drawables, and chevron indicators.
- **Test Required**: Build and interaction test.
- **Status**: **RESOLVED**

---

### DISC-005: System Cleaner Screen Structure
- **Screen / Feature**: System Cleaner (`CleanerScreen.kt`)
- **Reference Evidence**:
  - `activity_system_cleaner.xml`: "Remove Junk" card with `ic_remove_junk_list`, description, and red rounded "Scan Now" button; list items for Duplicate Photos (`img_system_cleaner_duplicate`), Large Videos (`img_system_cleaner_large_video`), and Screenshots (`img_system_cleaner_screenshot`).
- **Current Implementation**: Generic circular storage gauge with simulated progress delay and static junk categories.
- **Exact Discrepancy**: Visual layout did not match reference layout; lacked direct category routing.
- **Severity**: MEDIUM
- **Root Cause**: Custom UI design created instead of inspecting `activity_system_cleaner.xml`.
- **Files Affected**:
  - `app/src/main/java/albums/gallery/photo/folder/picasa/app/web/gallery/ui/cleaner/CleanerScreen.kt`
- **Required Correction**: Redesigned `CleanerScreen.kt` to match `activity_system_cleaner.xml` with "Remove Junk" card, live cache calculation, real cache clearing, and category navigation.
- **Acceptance Criteria**: Screen displays "Remove Junk" hero card and 3 subcategory cards; "Scan Now" initiates scan/cleaning; Duplicates card navigates to `DuplicatePhotoScreen`.
- **Test Required**: Unit test and assemble verification.
- **Status**: **RESOLVED**

---

### DISC-006: Reference Vector Drawables Missing
- **Screen / Feature**: Application Resources (`res/drawable`)
- **Reference Evidence**:
  - Reference APK contains over 300 vector drawables in `.analysis/apktool/res/drawable/`.
- **Current Implementation**: App only had 5 drawables.
- **Exact Discrepancy**: Icons were substituted with generic Material icons.
- **Severity**: HIGH
- **Root Cause**: Asset extraction was incomplete during initial project setup.
- **Files Affected**:
  - `app/src/main/res/drawable/`
  - `app/src/main/res/values/colors.xml`
- **Required Correction**: Copied 35+ essential vector drawables from reference APK and added missing referenced colors (`titleColor`, `subtitleColor`, `localization_search_icon`, `albumOptionBackColor`, `svgArrowColor`).
- **Acceptance Criteria**: All copied XML drawables link and compile without AAPT errors.
- **Test Required**: `./gradlew assembleDebug`.
- **Status**: **RESOLVED**

---

### DISC-007: Duplicate Photo Finder Navigation
- **Screen / Feature**: Navigation Routing
- **Reference Evidence**:
  - Reference app links System Cleaner to Duplicate Photo Finder (`DuplicateWaitingActivity`).
- **Current Implementation**: `DuplicatePhotoScreen` existed but was not reachable from `CleanerScreen`.
- **Exact Discrepancy**: Missing navigation route callback.
- **Severity**: MEDIUM
- **Root Cause**: Cleaner screen was isolated from Duplicates route.
- **Files Affected**:
  - `app/src/main/java/albums/gallery/photo/folder/picasa/app/web/gallery/ui/navigation/GalleryNavHost.kt`
  - `app/src/main/java/albums/gallery/photo/folder/picasa/app/web/gallery/ui/cleaner/CleanerScreen.kt`
- **Required Correction**: Wired `onNavigateToDuplicates` parameter in `CleanerScreen` and `GalleryNavHost`.
- **Acceptance Criteria**: Tapping Duplicate Photos inside System Cleaner opens `DuplicatePhotoScreen`.
- **Test Required**: Code review & compilation.
- **Status**: **RESOLVED**

---

### DISC-008: App Icons & Shortcuts
- **Screen / Feature**: Application Manifest & Launcher
- **Reference Evidence**:
  - Reference manifest defines `ic_launcher_gallery` and `shortcuts.xml` (`ic_shortcut_delete`, `ic_shortcut_duplicate`, `ic_shortcut_fav`, `ic_shortcut_hide`).
- **Current Implementation**: Previously lacked authentic adaptive foreground vectors and shortcut drawables.
- **Exact Discrepancy**: Generic launcher icon.
- **Severity**: LOW
- **Root Cause**: Placeholder launcher icons.
- **Files Affected**:
  - `app/src/main/res/drawable/ic_launcher_gallery_foreground.xml`
  - `app/src/main/res/drawable/ic_shortcut_*.xml`
- **Required Correction**: Copied authentic vector assets from reference APK.
- **Acceptance Criteria**: Launcher and app shortcuts use authentic vector artwork.
- **Test Required**: Compilation verification.
- **Status**: **RESOLVED**

---

### DISC-009: Cloud Sync & Third-Party Ad SDKs
- **Screen / Feature**: Backend / Ads / Tracking
- **Reference Evidence**:
  - Reference APK contains Google AdMob, Facebook Audience Network, AppLovin, and Firebase Analytics SDKs.
- **Current Implementation**: Pure offline, privacy-first implementation without ad SDKs.
- **Exact Discrepancy**: Ad banners and interstitial triggers are omitted.
- **Severity**: LOW
- **Root Cause**: Deliberate architectural decision to deliver a clean, privacy-respecting native reconstruction.
- **Files Affected**: `app/build.gradle.kts`
- **Required Correction**: Kept omitted; classified as UNKNOWN/INTENTIONALLY EXCLUDED third-party dependency.
- **Acceptance Criteria**: App functions completely offline without ad tracking crashes.
- **Status**: **CLASSIFIED (INTENTIONALLY OMITTED)**
