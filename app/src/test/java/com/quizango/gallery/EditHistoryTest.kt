package com.quizango.gallery

import com.quizango.gallery.ui.editor.EditHistory
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class EditHistoryTest {
    @Test
    fun undoRedoAndBranching() {
        val h = EditHistory<String>()
        h.record("a"); h.record("a"); h.record("b"); h.record("c")
        assertEquals("b", h.undo())
        assertEquals("a", h.undo())
        assertNull(h.undo())
        assertEquals("b", h.redo())
        h.record("x") // new edit drops "c"
        assertFalse(h.canRedo)
        assertEquals("b", h.undo())
        assertEquals("x", h.redo())
    }

    @Test
    fun limitDropsOldest() {
        val h = EditHistory<Int>(limit = 3)
        (1..5).forEach { h.record(it) }
        assertEquals(4, h.undo())
        assertEquals(3, h.undo())
        assertFalse(h.canUndo)
        assertTrue(h.canRedo)
    }
}
