# Phase 7: Private Vault — Context

Decided 2026-11-24 by the orchestrator on the user's standing instruction to make best-judgment calls autonomously (see 07-RESEARCH.md "Open questions"). The user may revisit any of these.

## Locked decisions

- **D1 — VLT-02 / checklist #37 picker:** the vault's Add Items picker ports 2.7.2's `InternalSelectMediaActivity` (TYPE=2, `activity_select.xml`). The requirement's mention of `activity_picker_screen` is a mislabel; that layout is the collage picker and belongs to Phase 8. Update REQUIREMENTS.md/checklist wording accordingly.
- **D2 — Hidden Folder section:** not built in Phase 7. Record as a known deviation from 2.7.2.
- **D3 — Forgot Password answer match:** keep our trimmed, case-insensitive hashed comparison (safer for users than 2.7.2's exact match). Record as a deliberate deviation.
- **D4 — Wrong-entry shake animation:** remove it (not in 2.7.2; core value is indistinguishable behaviour).
- **D5 — Lock gate:** every Add to Private entry point must check that a lock exists and route through first-time setup (2.7.2 behaviour), never hide into an unlocked vault.
- **D6 — Data safety outranks parity:** fix the VaultRepository bugs (overwrite on restore, delete-before-insert, missing MediaStore re-index, lingering decrypted cache) even where 2.7.2 behaves the same.
- **D7 — Device work:** no phone is available. Every device check is a PENDING-DEVICE item; nothing is faked. Unresolved `[OPEN]` items take the most reference-consistent guess and are flagged in the plan summary.
