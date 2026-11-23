package com.quizango.gallery

import com.quizango.gallery.data.local.datastore.showPatternValue
import com.quizango.gallery.ui.vault.VaultGate
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class VaultGateTest {
    @Test
    fun showPatternDefaultsToOnAndKeepsSavedValue() {
        assertTrue(showPatternValue(null))
        assertFalse(showPatternValue(false))
        assertTrue(showPatternValue(true))
    }

    @Test
    fun lockSetNeedsAnyNonEmptySecret() {
        assertFalse(VaultGate.lockSet(null, null))
        assertFalse(VaultGate.lockSet("", ""))
        assertFalse(VaultGate.lockSet(null, ""))
        assertTrue(VaultGate.lockSet("v1:salt:hash", null))
        assertTrue(VaultGate.lockSet("1234", ""))
        assertTrue(VaultGate.lockSet("", "0123"))
    }

    @Test
    fun setupFinishedNeedsLockAndQuestion() {
        assertTrue(VaultGate.setupFinished(lockSet = true, hasQuestion = true))
        assertFalse(VaultGate.setupFinished(lockSet = true, hasQuestion = false))
        assertFalse(VaultGate.setupFinished(lockSet = false, hasQuestion = true))
    }
}
