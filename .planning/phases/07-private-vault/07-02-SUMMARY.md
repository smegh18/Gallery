---
phase: 07-private-vault
plan: 02
subsystem: data
tags: [vault, data-safety, mediastore, encryption]
requires: []
provides:
  - hideFileSafely / restoreFileSafely (rollback-safe hide and collision-safe restore)
  - MediaStore re-index after hide and restore
affects: [07-06, 07-07, 07-08]
status: complete
actuals:
  tokens: 14000
  tasks: 2
  commits: 2
tech-stack:
  added: []
  patterns: [injectable I/O steps so file-safety order is unit-testable on the JVM]
key-files:
  created:
    - app/src/test/java/com/quizango/gallery/VaultFilesTest.kt
  modified:
    - app/src/main/java/com/quizango/gallery/data/repository/VaultRepository.kt
    - app/src/main/java/com/quizango/gallery/GalleryApplication.kt
key-decisions:
  - "A failed row delete during hide rollback keeps the ciphertext so a row never points at a missing file"
  - "Post-hide DB/scan failures never turn a successful hide into a reported failure"
requirements-completed: [VLT-01, VLT-02]
---

# Phase 7 Plan 02: Safe hide and restore Summary

The vault repository now hides in the order encrypt, insert row, delete source (rolling back row and ciphertext when the source survives), restores under a free name via `uniqueTargetFile` (never overwriting), and asks MediaStore to re-index the old or restored path.

## Commits
- 5338ff2 fix(07-02): hide inserts vault row before deleting source, rolls back, re-indexes MediaStore (Task 1, tracer)
- 6482efb fix(07-02): restore never overwrites, rolls back, and re-indexes MediaStore (Task 2)

## Verification (run on this machine)
- `./gradlew.bat :app:testDebugUnitTest --tests "com.quizango.gallery.VaultFilesTest" -q` -> exit 0 after Task 1 (5/5), `:app:assembleDebug -q` -> exit 0 (tracer gate passed before expansion).
- `./gradlew.bat :app:testDebugUnitTest --tests "com.quizango.gallery.VaultFilesTest" --tests "com.quizango.gallery.VaultCryptoTest" -q` -> exit 0; VaultFilesTest 10/10, VaultCryptoTest 3/3.
- `./gradlew.bat :app:assembleDebug -q` -> exit 0 on the final tree.
- Acceptance greps: `File(originalFolder, vaultItem.fileName)` 0 hits; `uniqueTargetFile(` 1; `scanFiles(` 2; `VaultRepositoryImpl(this, database.vaultDao(), database.galleryDao(), scanner)` present; `insertRow()` precedes `deleteSource(` in hideFileSafely.

## Behaviour now
- Hide: encrypt failure, row insert failure, or source that survives delete (false, throw, or still exists) all leave the original intact; row and ciphertext are removed (ciphertext kept only if the row itself cannot be removed, so no dangling row).
- Restore: creates the folder if missing; target is `createNewFile`-claimed so a race cannot overwrite; decrypt failure removes the partial target; if the vault file cannot be deleted the restored copy is removed and the item stays (row kept); legacy plain vault files still restore.
- After hide `scanFiles(old path)`, after restore `scanFiles(restored path)`; scan or Room-gallery cleanup failures are swallowed (CancellationException rethrown) because the file is already safe.

## Deviations from Plan
None. Task 1 was committed with the restore half of the file still in its old form (the full file was split locally so each commit is verified on its own), then Task 2 completed it.

## Decisions / accepted limits
- The accepted crash residual (item in both places after a crash between row insert and source delete) is tolerated: a later hide of the same source never fails (new UUID ciphertext), but it creates a second vault row for the same photo. I did not dedupe because the only safe dedupe is a full content comparison of old ciphertext vs source, which is heavier than the problem; the result is a duplicate entry, never a lost file.
- `restoreFromVault` still returns false if `vaultDao.deleteByPaths` throws after the vault file is already gone (file restored, row left dangling). No file is lost; a retry returns false because the vault file is missing.
- Batch callers must still run `mediaRepository.syncMediaStore()` after a batch (07-06, 07-07, 07-08).

## Known Stubs
None.

## Pending device checks
PENDING-DEVICE: hide/unhide on a real MIUI device (gallery row disappears and reappears immediately after hide and unhide). Covered here only by JVM temp-dir tests and the build; `scanFiles` itself is not exercised by tests.

## Threat Flags
None.

## Self-Check: PASSED
Commits 5338ff2 and 6482efb present; VaultFilesTest.kt exists; no unexpected file deletions in either commit.
