package com.quizango.gallery

import com.quizango.gallery.ui.theme.sdpFactor
import org.junit.Assert.assertEquals
import org.junit.Test

class DimensTest {

    @Test
    fun factorFollowsReferenceSmallestWidthBuckets() {
        val expected = mapOf(
            299 to 1.0f, 320 to 1.0f, 330 to 1.1f, 360 to 1.2f, 411 to 1.3f, 600 to 2.0f, 1200 to 3.6f
        )
        expected.forEach { (sw, factor) -> assertEquals("sw=$sw", factor, sdpFactor(sw), 0.0001f) }
    }
}
