package com.quizango.gallery

import com.quizango.gallery.ui.vault.fingerprintToggle
import com.quizango.gallery.ui.vault.vaultSettingRows
import org.junit.Assert.assertEquals
import org.junit.Test

class VaultSettingTest {
    @Test
    fun rowsAreInReferenceOrder() {
        assertEquals(
            listOf(
                Triple(R.drawable.ic_change_password, R.string.change_password, R.string.set_a_new_passcode),
                Triple(R.drawable.ic_pattern, R.string.show_pattern, R.string.display_your_unique_pattern),
                Triple(R.drawable.ic_fingerprint, R.string.unlock_with_fingerprint, R.string.use_biometrics_for_security),
                Triple(R.drawable.ic_change_recovery_question, R.string.change_recovery_question, R.string.choose_another_recovery_question)
            ),
            vaultSettingRows
        )
    }

    @Test
    fun turningOnWithoutUsableFingerprintWritesNothingAndToasts() {
        val reason = androidx.biometric.R.string.fingerprint_error_no_fingerprints
        assertEquals(null to reason, fingerprintToggle(enable = true, reason = reason))
    }

    @Test
    fun turningOnWhenUsableSaves() {
        assertEquals(true to null, fingerprintToggle(enable = true, reason = null))
    }

    @Test
    fun turningOffAlwaysSaves() {
        assertEquals(false to null, fingerprintToggle(enable = false, reason = androidx.biometric.R.string.fingerprint_error_hw_not_present))
    }
}
