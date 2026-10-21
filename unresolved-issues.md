# Unresolved Issues & Boundary Classifications

In strict adherence to reverse-engineering ethics and the `android-apk-reconstruction` skill requirements:
> "Never invent API contracts, backend rules, or security behavior. Separate observed behavior from inferred behavior."

This document records the exact boundaries between observed local behavior and intentionally unresolved or omitted proprietary subsystems.

---

## 1. Intentionally Excluded Proprietary Subsystems

### UNKNOWN-01: Commercial Ad Mediation & Banners
- **Reference Evidence**: Decompiled sources contain integrations with Google AdMob (`com.google.android.gms.ads.*`), AppLovin MAX (`com.applovin.*`), and Facebook Audience Network (`com.facebook.ads.*`).
- **Classification**: **UNKNOWN (Proprietary External Network)**
- **Reconstruction Status**: **INTENTIONALLY EXCLUDED**
- **Rationale**: Reconstructing commercial ad mediation introduces unnecessary third-party dependencies, battery drain, tracking libraries, and network overhead. The core gallery features function cleanly and natively without them.

### UNKNOWN-02: Third-Party Telemetry & Analytics
- **Reference Evidence**: Reference APK includes Firebase Analytics (`com.google.firebase.analytics.*`) and OneSignal Push Notifications (`com.onesignal.*`).
- **Classification**: **UNKNOWN (Proprietary External Server)**
- **Reconstruction Status**: **INTENTIONALLY EXCLUDED**
- **Rationale**: An offline gallery application does not require proprietary user telemetry or push notification servers. Eliminating these enhances user privacy and offline reliability.

### UNKNOWN-03: Google Play In-App Billing
- **Reference Evidence**: `com.android.billingclient.api.BillingClient` in reference bytecode for Pro subscriptions.
- **Classification**: **UNKNOWN (Google Play Billing Backend)**
- **Reconstruction Status**: **UNRESOLVED / LOCAL PRO OVERRIDE**
- **Rationale**: Real monetary billing requires authorized Google Play Merchant credentials and cannot be spoofed. The reconstructed app preserves the "Go Premium" drawer banner and toolbar badge for visual parity while bypassing restrictive paywalls for local utility features.

---

## 2. Inferred Behavioral Nuances

### INFERRED-01: System Cache File Calculation
- **Reference Evidence**: `SystemCleanerActivity.java` invokes internal package manager methods and storage stats helpers to calculate cache.
- **Classification**: **INFERRED**
- **Reconstruction Status**: **IMPLEMENTED WITH SAFE LOCAL APIS**
- **Rationale**: Modern Android permissions restrict apps from clearing arbitrary system-wide caches without system root or specialized permissions. The reconstructed cleaner safely inspects and purges internal application cache, thumbnail caches, and temporary files via standard `File` APIs.

### INFERRED-02: Duplicate Photo Detection Heuristics
- **Reference Evidence**: Reference app analyzes file sizes and names in `DuplicateWaitingActivity`.
- **Classification**: **INFERRED**
- **Reconstruction Status**: **IMPLEMENTED**
- **Rationale**: Exact perceptual hashing algorithms (e.g. pHash) in native C++ libraries were abstracted; implemented using robust size and name-signature clustering with automatic selection of redundant copies.
