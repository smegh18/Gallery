# Complete Android APK Reconstruction: Gallery App

## Project Overview
Reconstruct the authorized reference Android application (`gallery.apk`, package `albums.gallery.photo.folder.picasa.app.web.gallery`, version 2.6.7) in a clean, modern native Android project with maximum achievable behavioral and visual fidelity.

## Technical Stack
- **Language**: Kotlin 2.x
- **UI Framework**: Jetpack Compose (with Material 3 & custom design tokens for exact visual parity)
- **Architecture**: MVVM with Unidirectional Data Flow (UDF)
- **Local Persistence**: Room Database (`GalleryDatabase`, version 5 schema parity)
- **Preferences**: Jetpack DataStore / SharedPreferences
- **Asynchronous Flow**: Kotlin Coroutines & Flow
- **Dependency Injection**: Hilt / Kotlin-inject or clean factory pattern
- **Image Loading**: Coil (Compose-first high-performance image and video thumbnail loader)
- **Testing**: JUnit 5, MockK, AndroidX Test, Compose UI Testing

## Core Feature Scope
1. **Onboarding & Permissions**: Splash, Language selector, Welcome screens, Granular Storage & Notification permissions.
2. **Dashboard**: Photos timeline with date grouping, Albums grid, Explore tab with utilities & carousel.
3. **Media Fullscreen Viewer**: Pinch-to-zoom, swipe pager, info dialog, favorites toggle, rotate, video player with speed & timeline controls.
4. **Recycle Bin**: In-app Trash with 30-day retention and restore / permanent delete.
5. **Private Vault**: PIN & Pattern lock, biometric prompt, security question recovery, hidden encrypted storage.
6. **Duplicate Cleaner**: Hash-based grouping, smart auto-selection ("Keep Best Photo"), bulk deletion.
7. **System Cleaner**: Cache, screenshots, large videos, junk cleaning flow.
8. **Photo Editor**: Crop/Rotate, GPU shaders, sliders (brightness, contrast, saturation), brush drawing, text overlays, stickers.
9. **Personalization & Settings**: Light/Dark/System theme, language switching, sort orders, trash policy.
