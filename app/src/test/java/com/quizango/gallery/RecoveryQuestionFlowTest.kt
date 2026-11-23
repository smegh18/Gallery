package com.quizango.gallery

import com.quizango.gallery.data.local.VaultSecret
import com.quizango.gallery.ui.vault.ForgotEvent
import com.quizango.gallery.ui.vault.RecoveryEvent
import com.quizango.gallery.ui.vault.RecoveryQuestionFlow
import com.quizango.gallery.ui.vault.forgotAnswerEvent
import com.quizango.gallery.ui.vault.questionRes
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class RecoveryQuestionFlowTest {
    @Test
    fun startsOnFirstOfSevenQuestions() {
        val f = RecoveryQuestionFlow()
        assertEquals((1..7).toList(), f.remaining)
        assertEquals(1, f.selected)
        assertEquals(0, f.step)
    }

    @Test
    fun blankAnswerToastsAndKeepsStep() {
        val f = RecoveryQuestionFlow()
        assertEquals(RecoveryEvent.Toast(R.string.please_enter_answer), f.next("  "))
        assertEquals(0, f.step)
        assertEquals(7, f.remaining.size)
    }

    @Test
    fun usedQuestionLeavesTheChoicesAndNextIsSelected() {
        val f = RecoveryQuestionFlow()
        f.select(2)
        assertEquals(RecoveryEvent.Advanced, f.next("Rex"))
        assertEquals(listOf(1, 3, 4, 5, 6, 7), f.remaining)
        assertEquals(1, f.selected)
        assertEquals(1, f.step)
    }

    @Test
    fun threeAnswersFinishWithDistinctIdsInPickOrder() {
        val f = RecoveryQuestionFlow()
        f.select(5); f.next("a")
        f.select(2); f.next("b")
        f.select(7)
        assertEquals(RecoveryEvent.Done(listOf(5 to "a", 2 to "b", 7 to "c")), f.next("c"))
    }

    @Test
    fun cannotSelectAUsedQuestion() {
        val f = RecoveryQuestionFlow()
        f.select(3); f.next("a")
        f.select(3)
        assertEquals(1, f.selected)
    }

    @Test
    fun buttonLabelIsNextThenSubmit() {
        val f = RecoveryQuestionFlow()
        assertEquals(R.string.next, f.buttonLabel)
        f.next("a")
        assertEquals(R.string.next, f.buttonLabel)
        f.next("b")
        assertEquals(R.string.submit, f.buttonLabel)
    }

    @Test
    fun questionResMapsIdsAndLeavesLegacyText() {
        assertEquals(R.string.security_question3, questionRes("3"))
        assertEquals(R.string.security_question10, questionRes("10"))
        assertNull(questionRes("What is your pet name?"))
        assertNull(questionRes("11"))
    }

    private val hash = VaultSecret.hash(VaultSecret.normalizeAnswer("rex"))

    @Test
    fun forgotEmptyAnswerToastsPleaseWrite() {
        assertEquals(ForgotEvent.Toast(R.string.please_write_an_answer), forgotAnswerEvent("", hash))
        assertEquals(ForgotEvent.Toast(R.string.please_write_an_answer), forgotAnswerEvent("   ", hash))
    }

    @Test
    fun forgotMatchesTrimmedCaseInsensitiveAgainstHash() {
        assertEquals(ForgotEvent.Ok, forgotAnswerEvent(" Rex ", hash))
        assertEquals(ForgotEvent.Toast(R.string.enter_correct_security_answer), forgotAnswerEvent("rax", hash))
    }

    @Test
    fun forgotLegacyPlainAnswerMatchesCaseInsensitively() {
        assertEquals(ForgotEvent.Ok, forgotAnswerEvent("rex", "Rex"))
        assertEquals(ForgotEvent.Toast(R.string.enter_correct_security_answer), forgotAnswerEvent("rax", "Rex"))
    }

    @Test
    fun forgotWithoutStoredAnswerIsAnIssue() {
        assertEquals(ForgotEvent.Toast(R.string.something_went_wrong), forgotAnswerEvent("rex", null))
    }
}
