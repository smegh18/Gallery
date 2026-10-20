# Reconstruction Roadmap

## Phase Overview

- [x] **Phase A: Environment and Tool Audit**
  - Verify Java, Android SDK, ADB, Apktool, REA MCP, GSD, Reconstruction Skill.
  - Deliverable: `ENVIRONMENT_AUDIT.md`.
- [x] **Phase B: Static APK Analysis**
  - Decompile resources, manifest, and smali via Apktool & REA.
  - Map all 57 activities, Room database schemas, data models, APIs, and design system tokens.
  - Deliverable: `APK_ANALYSIS_REPORT.md`.
- [ ] **Phase C: Runtime Investigation & Baseline**
  - Run reference app on emulator/device (pending target availability).
  - Capture reference screenshots of reachable screens.
- [ ] **Phase D: Architecture Decision Records (ADRs) & Specifications**
  - Define package boundaries, state machines, repository interfaces, and Compose theme mapping.
- [ ] **Phase E: Android Project Foundation**
  - Scaffold clean Gradle root, settings, app module, build configurations, and Gradle wrapper.
- [ ] **Phase F: Design System & Shared UI Tokens**
  - Color palette, typography, shapes, common toolbars, dialogs, and navigation scaffolds.
- [ ] **Phase G: Feature Wave 1 - Splash, Onboarding & Storage Permissions**
- [ ] **Phase H: Feature Wave 2 - Room Database & MediaStore Repository Engine**
- [ ] **Phase I: Feature Wave 3 - Main Dashboard (Timeline, Albums, Explore, Drawer)**
- [ ] **Phase J: Feature Wave 4 - Full-Screen Media Viewer & Video Player**
- [ ] **Phase K: Feature Wave 5 - Recycle Bin / Trash Retention**
- [ ] **Phase L: Feature Wave 6 - Private Vault (PIN/Pattern & Encrypted Storage)**
- [ ] **Phase M: Feature Wave 7 - Duplicate Photo Finder & System Cleaner**
- [ ] **Phase N: Feature Wave 8 - Photo Editor & Creative Tools**
- [ ] **Phase O: Automated Testing Suite (Unit, Integration & UI Tests)**
- [ ] **Phase P: Code Review, Parity Verification & Release Report**
