package com.quizango.gallery

import com.quizango.gallery.domain.model.MediaItem
import com.quizango.gallery.domain.model.MediaType
import com.quizango.gallery.ui.discovery.STORY_STEP_MS
import com.quizango.gallery.ui.discovery.StoryTouch
import com.quizango.gallery.ui.discovery.kenBurnsZoomIn
import com.quizango.gallery.ui.discovery.storySteps
import com.quizango.gallery.ui.discovery.storyTouch
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class StoryViewTest {
    private fun photos(n: Int) = (1..n).map {
        MediaItem(id = it.toLong(), filePath = "p$it", fileName = "p$it", fileSize = 0, folderName = "", folderPath = "",
            dateModified = 0, dateTaken = 0, mediaType = MediaType.IMAGE)
    }

    private fun shape(steps: List<MediaItem?>) = steps.map { it?.filePath ?: "ad" }

    @Test
    fun stepIsThreeSeconds() = assertEquals(3000, STORY_STEP_MS)

    @Test
    fun adStepFollowsEveryThirdPhoto() {
        assertEquals(listOf("p1", "p2", "p3", "ad", "p4", "p5", "p6", "ad"), shape(storySteps(photos(6), true)))
        assertEquals(listOf("p1", "p2", "p3", "ad", "p4"), shape(storySteps(photos(4), true)))
        assertEquals(listOf("p1", "p2"), shape(storySteps(photos(2), true)))
        assertTrue(storySteps(photos(6), false).none { it == null })
    }

    @Test
    fun tapGoesToTheHalfUnderTheFinger() {
        assertEquals(StoryTouch.PREVIOUS, storyTouch(120, 10f, 100f, 720f, rtl = false))
        assertEquals(StoryTouch.NEXT, storyTouch(120, 10f, 600f, 720f, rtl = false))
        assertEquals(StoryTouch.NEXT, storyTouch(120, 10f, 100f, 720f, rtl = true))
        assertEquals(StoryTouch.PREVIOUS, storyTouch(120, 10f, 600f, 720f, rtl = true))
    }

    @Test
    fun holdOrDragResumesWithoutMoving() {
        assertEquals(StoryTouch.PREVIOUS, storyTouch(499, 99.9f, 100f, 720f, rtl = false))
        assertEquals(StoryTouch.RESUME, storyTouch(500, 0f, 100f, 720f, rtl = false))
        assertEquals(StoryTouch.RESUME, storyTouch(120, 100f, 600f, 720f, rtl = false))
    }

    @Test
    fun zoomAlternatesStartingWithZoomIn() {
        assertTrue(kenBurnsZoomIn(0))
        assertFalse(kenBurnsZoomIn(1))
        assertTrue(kenBurnsZoomIn(2))
    }
}
