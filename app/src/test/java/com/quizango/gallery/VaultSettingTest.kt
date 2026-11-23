package com.quizango.gallery

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
}
