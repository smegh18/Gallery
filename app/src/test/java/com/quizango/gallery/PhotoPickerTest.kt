package com.quizango.gallery

import com.quizango.gallery.ui.components.collageAddError
import com.quizango.gallery.ui.components.collageTapAllowed
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class PhotoPickerTest {
    @Test
    fun collageAddNeedsTwoToFifteenPhotos() {
        assertEquals(R.string.please_select_items, collageAddError(0))
        assertEquals(R.string.collage_limit_error, collageAddError(1))
        assertNull(collageAddError(2))
        assertNull(collageAddError(15))
        assertEquals(R.string.collage_limit_error, collageAddError(16))
    }

    @Test
    fun aSixteenthPhotoIsRefusedButUnpickingStaysAllowed() {
        val fifteen = (1..15).map { "/p$it" }
        assertTrue(collageTapAllowed(fifteen.dropLast(1), "/new"))
        assertFalse(collageTapAllowed(fifteen, "/new"))
        assertTrue(collageTapAllowed(fifteen, "/p3"))
    }
}
