package com.quizango.gallery

import com.quizango.gallery.ui.discovery.StoryQueue
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class StoryQueueTest {
    @Test
    fun nextStoryUntilTheLast() {
        StoryQueue.stories = listOf("recent" to "Recent", "random" to "Random", "/a" to "A")
        assertEquals("random" to "Random", StoryQueue.next("recent"))
        assertEquals("/a" to "A", StoryQueue.next("random"))
        assertNull(StoryQueue.next("/a")) // last story closes
        assertNull(StoryQueue.next("vault")) // not from the row
    }
}
