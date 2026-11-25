package com.quizango.gallery

import com.quizango.gallery.ui.vault.pickerAddEvent
import com.quizango.gallery.ui.vault.pickerBarsVisible
import com.quizango.gallery.ui.vault.togglePicked
import com.quizango.gallery.ui.vault.vaultBatchFailures
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class VaultPickerStateTest {
    @Test
    fun toggleKeepsTapOrder() {
        assertEquals(listOf("a"), togglePicked(emptyList(), "a"))
        assertEquals(listOf("b"), togglePicked(listOf("a", "b"), "a"))
        assertEquals(listOf("b", "a"), togglePicked(togglePicked(emptyList(), "b"), "a"))
    }

    @Test
    fun addButtonAndTrayShowOnlyWithASelection() {
        assertFalse(pickerBarsVisible(0))
        assertTrue(pickerBarsVisible(1))
        assertTrue(pickerBarsVisible(5))
    }

    @Test
    fun addWithNothingPickedToastsPleaseSelect() {
        assertEquals(R.string.please_select_items, pickerAddEvent(0))
        assertNull(pickerAddEvent(1))
        assertNull(pickerAddEvent(2))
    }

    @Test
    fun aVaultBatchTriesEveryItemOnceAndReturnsTheFailuresInOrder() = runBlocking {
        val tried = mutableListOf<String>()
        val failed = vaultBatchFailures(listOf("a", "b", "c", "d"), { "/$it" }) { tried += it; it != "b" && it != "d" }
        assertEquals(listOf("a", "b", "c", "d"), tried) // a failure does not stop the rest
        assertEquals(listOf("/b", "/d"), failed) // these stay picked; only an empty list is a full success
        assertTrue(vaultBatchFailures(listOf("a"), { it }) { true }.isEmpty())
    }
}
