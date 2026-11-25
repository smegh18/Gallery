package com.quizango.gallery

import com.quizango.gallery.ui.vault.LockEvent
import com.quizango.gallery.ui.vault.LockNext
import com.quizango.gallery.ui.vault.LockStage
import com.quizango.gallery.ui.vault.VaultLockFlow
import com.quizango.gallery.ui.vault.pinAfterSubmit
import com.quizango.gallery.ui.vault.settingsToast
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class VaultLockFlowTest {
    private fun setup(type: Int = 0, openVault: Boolean = true, hasQuestion: Boolean = true) =
        VaultLockFlow(type, openVault, lockSet = false, storedIsPattern = false, hasQuestion = hasQuestion)

    private fun verify(type: Int = 0, openVault: Boolean = true, pattern: Boolean = false, hasQuestion: Boolean = true) =
        VaultLockFlow(type, openVault, lockSet = true, storedIsPattern = pattern, hasQuestion = hasQuestion)

    private val never: (String) -> Boolean = { false }

    @Test
    fun pinSetupHoldsFirstEntryThenSavesOnMatch() {
        val f = setup()
        assertEquals(R.string.set_pin, f.stepTitle)
        assertEquals(LockEvent.Toast(R.string.please_enter_pin), f.submitPin("12", never))
        assertEquals(LockStage.SETUP_FIRST, f.stage)
        assertEquals(LockEvent.Clear, f.submitPin("1234", never))
        assertEquals(R.string.re_enter_pin, f.stepTitle)
        assertEquals(LockEvent.Save("1234", pattern = false), f.submitPin("1234", never))
    }

    @Test
    fun pinSetupMismatchRestartsAtSetPin() {
        val f = setup()
        f.submitPin("1234", never)
        assertEquals(LockEvent.Toast(R.string.password_confirm), f.submitPin("1235", never))
        assertEquals(R.string.set_pin, f.stepTitle)
        assertEquals(LockStage.SETUP_FIRST, f.stage)
    }

    @Test
    fun patternSetupNeedsFourDotsThenConfirms() {
        val f = setup().also { it.switchMode() }
        assertEquals(R.string.draw_your_pattern, f.stepTitle)
        assertEquals(LockEvent.Toast(R.string.pattern_min_4_dots), f.submitPattern("012", never))
        assertEquals(LockStage.SETUP_FIRST, f.stage)
        assertEquals(LockEvent.Clear, f.submitPattern("0123", never))
        assertEquals(R.string.re_draw_your_pattern, f.stepTitle)
        assertEquals(LockEvent.Save("0123", pattern = true), f.submitPattern("0123", never))
    }

    @Test
    fun patternSetupMismatchReturnsToDraw() {
        val f = setup().also { it.switchMode() }
        f.submitPattern("0123", never)
        assertEquals(LockEvent.Toast(R.string.pattern_confirm), f.submitPattern("0124", never))
        assertEquals(R.string.draw_your_pattern, f.stepTitle)
    }

    @Test
    fun verifyPinMatchesAndWrongToasts() {
        val f = verify()
        assertEquals(R.string.enter_pin, f.stepTitle)
        assertEquals(LockEvent.Success, f.submitPin("1234") { it == "1234" })
        assertEquals(LockEvent.Wrong(R.string.wrong_password, lockOut = false), f.submitPin("0000") { it == "1234" })
    }

    @Test
    fun wrongPatternHasNoToastAndNoDotMinimum() {
        val f = verify(pattern = true)
        assertEquals(LockEvent.Wrong(null, lockOut = false), f.submitPattern("01") { it == "0123" })
        assertEquals(LockEvent.Success, f.submitPattern("0123") { it == "0123" })
    }

    @Test
    fun lockOutOnExactlyTheFifthWrongAttempt() {
        val f = verify()
        repeat(4) { assertFalse((f.submitPin("0000", never) as LockEvent.Wrong).lockOut) }
        assertTrue((f.submitPin("0000", never) as LockEvent.Wrong).lockOut)
        assertFalse((f.submitPin("0000", never) as LockEvent.Wrong).lockOut)
    }

    @Test
    fun visibilityAndSwitch() {
        val v = verify(hasQuestion = true)
        assertTrue(v.showForgot)
        assertFalse(v.showSwitch)
        assertFalse(verify(hasQuestion = false).showForgot)
        val s = setup()
        assertFalse(s.showForgot)
        assertTrue(s.showSwitch)
        s.submitPin("1234", never)
        s.switchMode()
        assertTrue(s.isPattern)
        assertEquals(LockStage.SETUP_FIRST, s.stage)
        s.switchMode()
        assertFalse(s.isPattern)
        v.switchMode()
        assertEquals(LockStage.VERIFY, v.stage)
    }

    @Test
    fun toolbarAndStepTitles() {
        assertEquals(R.string.change_password, verify(type = 1).toolbarTitle)
        assertEquals(R.string.private_vault, verify(type = 0).toolbarTitle)
        assertEquals(R.string.private_vault, setup(type = 1).toolbarTitle)
        assertEquals(R.string.draw_your_pattern, verify(pattern = true).stepTitle)
    }

    @Test
    fun afterSuccessRouting() {
        assertEquals(LockNext.Recovery, verify(type = 0, hasQuestion = false).afterSuccess(false))
        assertEquals(LockNext.Recovery, verify(type = 1, hasQuestion = false).afterSuccess(false))
        assertEquals(LockNext.Vault, verify(type = 0, openVault = true).afterSuccess(false))
        assertEquals(LockNext.ResultOk(passwordSaved = false), verify(type = 0, openVault = false).afterSuccess(false))
        assertEquals(LockNext.ResultOk(passwordSaved = true), setup(openVault = false).afterSuccess(true))
        assertEquals(LockNext.Recovery, verify(type = 2).afterSuccess(false))
    }

    @Test
    fun changePasswordFlipsToTypeZeroSetupThenReturnsOk() {
        val f = verify(type = 1, openVault = false, pattern = true)
        assertEquals(LockNext.Stay, f.afterSuccess(false))
        assertEquals(0, f.type)
        assertEquals(LockStage.SETUP_FIRST, f.stage)
        assertFalse(f.isPattern)
        assertEquals(R.string.private_vault, f.toolbarTitle)
        assertEquals(R.string.set_pin, f.stepTitle)
        assertEquals(LockNext.ResultOk(passwordSaved = true), f.afterSuccess(true))
    }

    @Test
    fun resetAfterForgotReturnsToPinSetup() {
        val f = verify(pattern = true)
        repeat(3) { f.submitPattern("01", never) }
        f.resetAfterForgot()
        assertEquals(0, f.type)
        assertEquals(LockStage.SETUP_FIRST, f.stage)
        assertFalse(f.isPattern)
        assertEquals(R.string.set_pin, f.stepTitle)
        assertEquals(0, f.wrongCount)
    }

    @Test
    fun settingsToastChoice() {
        assertEquals(R.string.password_changed_successfully, settingsToast(1, true))
        assertEquals(R.string.password_changed_successfully, settingsToast(2, true))
        assertEquals(R.string.recovery_question_changed_successfully, settingsToast(2, false))
        assertNull(settingsToast(0, true))
        assertNull(settingsToast(1, false))
    }

    @Test
    fun aQuickSecondConfirmCannotSaveTheFirstSetupEntry() {
        val f = setup()
        val first = f.submitPin("1234", never)
        val left = pinAfterSubmit("1234", first)
        assertEquals("", left)
        // The second tap finds no digits: it toasts instead of saving "1234" as its own confirmation
        assertEquals(LockEvent.Toast(R.string.please_enter_pin), f.submitPin(left, never))
        assertEquals(LockStage.SETUP_CONFIRM, f.stage)
    }

    @Test
    fun everyConfirmOutcomeConsumesTheDigitsExceptATooShortPin() {
        assertEquals("12", pinAfterSubmit("12", LockEvent.Toast(R.string.please_enter_pin)))
        assertEquals("", pinAfterSubmit("1235", LockEvent.Toast(R.string.password_confirm)))
        assertEquals("", pinAfterSubmit("1234", LockEvent.Clear))
        assertEquals("", pinAfterSubmit("1234", LockEvent.Save("1234", pattern = false)))
        assertEquals("", pinAfterSubmit("1234", LockEvent.Success))
        assertEquals("", pinAfterSubmit("1234", LockEvent.Wrong(R.string.wrong_password, lockOut = false)))
    }
}
