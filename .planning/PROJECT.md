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

## Current Milestone: v2.0 Reference 2.7.2 parity

**Goal:** Bring the app from reference 2.6.7 parity to 100% parity with reference 2.7.2 (versionCode 152): every screen, flow, animation and behaviour.

**Target features:**
- Port 30 new and 40 changed layouts across the 53 screens in `.planning/milestones/v2.7.2-parity/PARITY-CHECKLIST.md` (Home shell; Photos, Albums and Explore tabs; viewer; stories; editor; collage; settings)
- Remove what 2.7.2 dropped: the Welcome screen, the Albums feature carousel and the unused Lotties
- New features:
  - Feature-promo onboarding step
  - Floating quick-tools sidebar
  - After-call "Quick Info" and install/update/uninstall screens
  - Usage-access prompts and setting
- Shared assets: 107 drawables, 85 strings with translations, theme diff

**Approach:**
- Stage 1: port from decompiled 2.7.2 (REA apktool backend, see `tooling-setup.md`).
- Stage 2: verify each screen side by side on the phone with Artemis.
- Quality gates per phase: unit tests, assembleDebug, CodeRabbit, unslop audit.
- Ads stay placeholders.

## Evolution

This document evolves at phase transitions and milestone boundaries.

**After each phase transition** (via `/gsd-transition`):
1. Requirements invalidated? → Move to Out of Scope with reason
2. Requirements validated? → Move to Validated with phase reference
3. New requirements emerged? → Add to Active
4. Decisions to log? → Add to Key Decisions
5. "What This Is" still accurate? → Update if drifted

**After each milestone** (via `/gsd-complete-milestone`):
1. Full review of all sections
2. Core Value check — still the right priority?
3. Audit Out of Scope — reasons still valid?
4. Update Context with current state

---
*Last updated: 2026-11-13 after starting milestone v2.0*
