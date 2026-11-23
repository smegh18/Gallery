package com.quizango.gallery.ui.vault

/** Lock checks shared by every Add to Private entry point (rules L5, M6). */
internal object VaultGate {
    /** A lock counts as set when either secret is non-empty, including plain values saved by older builds. */
    fun lockSet(pin: String?, pattern: String?) = !pin.isNullOrEmpty() || !pattern.isNullOrEmpty()

    fun setupFinished(lockSet: Boolean, hasQuestion: Boolean) = lockSet && hasQuestion
}
