package com.quizango.gallery

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import com.quizango.gallery.ui.viewer.ViewerZoom
import org.junit.Assert.assertEquals
import org.junit.Test

class ViewerZoomTest {
    private val screen = Size(1000f, 2000f)

    @Test
    fun fitsWidePhotoToWidth() {
        assertEquals(Size(1000f, 500f), ViewerZoom.fittedSize(Size(2000f, 1000f), screen))
    }

    @Test
    fun clampStopsAtPhotoEdgesNotScreenEdges() {
        val fitted = Size(1000f, 500f) // wide photo, letterboxed
        val c = ViewerZoom.clamp(Offset(9999f, 9999f), 2f, fitted, screen)
        assertEquals(500f, c.x, 0.01f) // (1000*2 - 1000)/2
        assertEquals(0f, c.y, 0.01f)   // 500*2 = 1000 < 2000: no vertical pan
    }

    @Test
    fun doubleTapZoomsTowardTap() {
        val fitted = Size(1000f, 2000f)
        val o = ViewerZoom.offsetForTap(Offset(250f, 1000f), 1.75f, fitted, screen)
        assertEquals(187.5f, o.x, 0.01f) // (500-250)*0.75, within max 375
        assertEquals(0f, o.y, 0.01f)
    }
}
