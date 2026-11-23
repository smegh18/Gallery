package com.quizango.gallery.ui.vault

import androidx.annotation.StringRes
import com.quizango.gallery.R
import com.quizango.gallery.data.local.VaultSecret

sealed interface RecoveryEvent {
    data class Toast(@StringRes val res: Int) : RecoveryEvent
    data object Advanced : RecoveryEvent
    /** Three (question id, raw answer) entries in pick order. */
    data class Done(val entries: List<Pair<Int, String>>) : RecoveryEvent
}

/** activity_vault_recovery_question: three steps over the seven offered questions, a used one leaves the list. */
class RecoveryQuestionFlow {
    var step = 0
        private set
    var remaining: List<Int> = (1..7).toList()
        private set
    var selected = remaining.first()
        private set
    private val picked = mutableListOf<Pair<Int, String>>()

    @get:StringRes
    val buttonLabel get() = if (step == 2) R.string.submit else R.string.next

    fun select(id: Int) {
        if (id in remaining) selected = id
    }

    fun next(answer: String): RecoveryEvent {
        if (answer.isBlank()) return RecoveryEvent.Toast(R.string.please_enter_answer)
        picked += selected to answer
        if (picked.size == 3) return RecoveryEvent.Done(picked.toList())
        remaining = remaining - selected
        selected = remaining.first()
        step++
        return RecoveryEvent.Advanced
    }
}

private val QUESTION_RES = intArrayOf(
    R.string.security_question1, R.string.security_question2, R.string.security_question3,
    R.string.security_question4, R.string.security_question5, R.string.security_question6,
    R.string.security_question7, R.string.security_question8, R.string.security_question9,
    R.string.security_question10
)

/** A stored question is its reference id "1".."10"; older builds saved the question text, which maps to null. */
@StringRes
fun questionRes(stored: String): Int? = stored.toIntOrNull()?.let { QUESTION_RES.getOrNull(it - 1) }

sealed interface ForgotEvent {
    data class Toast(@StringRes val res: Int) : ForgotEvent
    data object Ok : ForgotEvent
}

/**
 * Forgot Password answer check. [stored] is the saved answer of the picked question, null when none are stored.
 * Deliberate deviation D3: 2.7.2 compares exactly; here the answer is trimmed and case-insensitive against the hash,
 * and an older plain answer is compared the same way.
 */
fun forgotAnswerEvent(answer: String, stored: String?): ForgotEvent = when {
    stored == null -> ForgotEvent.Toast(R.string.something_went_wrong)
    answer.isBlank() -> ForgotEvent.Toast(R.string.please_write_an_answer)
    matchesStored(answer, stored) -> ForgotEvent.Ok
    else -> ForgotEvent.Toast(R.string.enter_correct_security_answer)
}

private fun matchesStored(answer: String, stored: String): Boolean =
    if (VaultSecret.isHashed(stored)) VaultSecret.matches(stored, VaultSecret.normalizeAnswer(answer))
    else answer.trim().equals(stored.trim(), ignoreCase = true)
