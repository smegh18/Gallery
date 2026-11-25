package com.quizango.gallery.ui.vault

import androidx.annotation.StringRes
import com.quizango.gallery.R

/** What the lock screen must do after one submitted PIN or pattern. */
internal sealed interface LockEvent {
    data class Toast(@StringRes val res: Int) : LockEvent
    /** Wipe the entered PIN or drawn pattern (the pattern pad clears 30 ms later like the reference, the PIN at once). */
    data object Clear : LockEvent
    /** Persist [value] (the screen hashes it) as the PIN or pattern lock, then call [VaultLockFlow.afterSuccess]. */
    data class Save(val value: String, val pattern: Boolean) : LockEvent
    /** The stored lock matched; call [VaultLockFlow.afterSuccess]. */
    data object Success : LockEvent
    /** A wrong unlock; [toast] is null for a wrong pattern, [lockOut] is true on exactly the fifth miss. */
    data class Wrong(@StringRes val toast: Int?, val lockOut: Boolean) : LockEvent
}

/** Where to go once a verify or setup finished. */
internal sealed interface LockNext {
    data object Recovery : LockNext
    data object Vault : LockNext
    data class ResultOk(val passwordSaved: Boolean) : LockNext
    /** Change Password: the old lock verified, the same screen now sets the new PIN. */
    data object Stay : LockNext
}

internal enum class LockStage { VERIFY, SETUP_FIRST, SETUP_CONFIRM }

/**
 * The PIN digits left once a Confirm was handled. Every outcome consumes them at once, so a second quick Confirm cannot
 * submit the same digits again (the first setup entry would save as its own confirmation); a too-short PIN keeps them.
 */
internal fun pinAfterSubmit(entered: String, event: LockEvent): String =
    if (event is LockEvent.Toast && entered.length < 4) entered else ""

/**
 * Pure model of the 2.7.2 VaultLockActivity (rules L2-L4, L6-L8, L10).
 * [type]: 0 unlock or first setup, 1 Change Password, 2 Change Recovery Question.
 * [matches] is injected by the caller so stored hashes stay inside VaultSecret.
 */
internal class VaultLockFlow(
    type: Int,
    private val openVault: Boolean,
    lockSet: Boolean,
    storedIsPattern: Boolean,
    private val hasQuestion: Boolean,
    wrongCount: Int = 0
) {
    var type = type
        private set
    var stage = if (lockSet) LockStage.VERIFY else LockStage.SETUP_FIRST
        private set
    var isPattern = lockSet && storedIsPattern
        private set
    // Seeded so the count survives the lock leaving composition under Forgot Password
    var wrongCount = wrongCount
        private set
    private var first: String? = null

    val toolbarTitle: Int
        @StringRes get() = if (type == 1 && stage == LockStage.VERIFY) R.string.change_password else R.string.private_vault
    val stepTitle: Int
        @StringRes get() = when (stage) {
            LockStage.VERIFY -> if (isPattern) R.string.draw_your_pattern else R.string.enter_pin
            LockStage.SETUP_FIRST -> if (isPattern) R.string.draw_your_pattern else R.string.set_pin
            LockStage.SETUP_CONFIRM -> if (isPattern) R.string.re_draw_your_pattern else R.string.re_enter_pin
        }
    val showForgot get() = stage == LockStage.VERIFY && hasQuestion
    val showSwitch get() = stage != LockStage.VERIFY

    fun submitPin(value: String, matches: (String) -> Boolean): LockEvent {
        if (value.length != 4) return LockEvent.Toast(R.string.please_enter_pin)
        return when (stage) {
            LockStage.VERIFY -> verify(value, matches, R.string.wrong_password)
            LockStage.SETUP_FIRST -> {
                first = value
                stage = LockStage.SETUP_CONFIRM
                LockEvent.Clear
            }
            LockStage.SETUP_CONFIRM -> confirm(value, false, R.string.password_confirm)
        }
    }

    fun submitPattern(value: String, matches: (String) -> Boolean): LockEvent = when (stage) {
        // Only a new pattern needs four dots; unlocking accepts any length (L4)
        LockStage.VERIFY -> verify(value, matches, null)
        LockStage.SETUP_FIRST ->
            if (value.length < 4) LockEvent.Toast(R.string.pattern_min_4_dots)
            else {
                first = value
                stage = LockStage.SETUP_CONFIRM
                LockEvent.Clear
            }
        LockStage.SETUP_CONFIRM -> confirm(value, true, R.string.pattern_confirm)
    }

    private fun verify(value: String, matches: (String) -> Boolean, @StringRes wrongToast: Int?): LockEvent {
        if (matches(value)) return LockEvent.Success
        wrongCount++
        return LockEvent.Wrong(wrongToast, lockOut = wrongCount == 5)
    }

    private fun confirm(value: String, pattern: Boolean, @StringRes mismatch: Int): LockEvent {
        if (value == first) return LockEvent.Save(value, pattern)
        restartSetup()
        return LockEvent.Toast(mismatch)
    }

    private fun restartSetup() {
        first = null
        stage = LockStage.SETUP_FIRST
    }

    /** "Switch to Pattern" / "Switch to PIN": only while setting up, and the first entry starts over. */
    fun switchMode() {
        if (stage == LockStage.VERIFY) return
        isPattern = !isPattern
        restartSetup()
    }

    /** [passwordSaved] is true when the lock was just written, false after a plain unlock. */
    fun afterSuccess(passwordSaved: Boolean): LockNext {
        if (!hasQuestion) return LockNext.Recovery
        if (type == 2) return LockNext.Recovery
        if (type == 1 && stage == LockStage.VERIFY) {
            // A3: the old lock verified, so the same screen becomes the type-0 "Set PIN" setup
            type = 0
            isPattern = false
            restartSetup()
            return LockNext.Stay
        }
        return if (type == 0 && openVault) LockNext.Vault else LockNext.ResultOk(passwordSaved)
    }

    /** L10: a right Forgot Password answer wipes the lock and returns to PIN setup. */
    fun resetAfterForgot() {
        type = 0
        isPattern = false
        wrongCount = 0
        restartSetup()
    }
}

/** S5: which toast Settings shows after the lock screen returned RESULT_OK; null means none. */
@StringRes
internal fun settingsToast(initialType: Int, passwordSaved: Boolean): Int? = when {
    (initialType == 1 || initialType == 2) && passwordSaved -> R.string.password_changed_successfully
    initialType == 2 -> R.string.recovery_question_changed_successfully
    else -> null
}
