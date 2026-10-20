# Android APK Reconstruction

## Objective

Reconstruct the externally observable functionality and visual
behavior of an authorized reference Android APK in a clean,
maintainable native Android project.

## Rules

1. Treat the reference APK as read-only evidence.
2. Never assume that decompiled code represents the original source.
3. Record evidence and uncertainty before implementing behavior.
4. Separate observed behavior from inferred behavior.
5. Never invent API contracts, backend rules, or security behavior.
6. Preserve observable behavior while improving architecture.
7. Implement features incrementally.
8. Build and test after each meaningful implementation step.
9. Compare reconstructed screens against reference screenshots.
10. Verify navigation, persistence, error handling, and lifecycle
    behavior, not just visual appearance.
11. Do not leave placeholder screens or nonfunctional controls.
12. Do not suppress errors to make tests pass.
13. Do not hardcode credentials or sensitive user data.
14. Keep dependencies minimal and justify additional libraries.
15. Do not claim a feature is complete without evidence.

## Architecture

Prefer:

- Kotlin.
- Jetpack Compose for new UI, where appropriate.
- MVVM with unidirectional data flow.
- ViewModels and lifecycle-aware state collection.
- Coroutines and Flow.
- Repository interfaces.
- Explicit data, domain, and presentation boundaries.
- Dependency injection where justified.
- Room for suitable local relational persistence.
- DataStore for suitable application preferences.
- Retrofit/OkHttp or an appropriate existing networking stack.

Do not introduce abstractions without a clear purpose.

Use Views/XML where they provide a better compatibility path
or are required by the reference behavior.

## Verification

For every feature, define:

- Reference behavior.
- Acceptance criteria.
- Positive scenarios.
- Negative scenarios.
- Loading, empty, and error states.
- Navigation behavior.
- Persistence requirements.
- Automated tests.
- Screenshot comparison requirements.

A successful compilation alone is not sufficient for completion.