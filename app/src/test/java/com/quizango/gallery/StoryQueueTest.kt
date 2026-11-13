package com.quizango.gallery

import com.quizango.gallery.domain.model.MediaItem
import com.quizango.gallery.domain.model.MediaType
import com.quizango.gallery.ui.discovery.Story
import com.quizango.gallery.ui.discovery.StoryBuilder
import com.quizango.gallery.ui.discovery.StoryQueue
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class StoryQueueTest {
    private val day = 24L * 60 * 60 * 1000
    private val now = 1_800_000_000_000L

    private fun photo(path: String, age: Long, folder: String = "/storage/emulated/0/DCIM/Camera", fav: Boolean = false, type: MediaType = MediaType.IMAGE) =
        MediaItem(id = 0, filePath = path, fileName = path, fileSize = 0, folderName = "", folderPath = folder,
            dateModified = now - age, dateTaken = now - age, mediaType = type, isFavorite = fav)

    @Test
    fun nextStoryUntilTheLast() {
        StoryQueue.stories = listOf(Story("recent", "Recent", emptyList()), Story("random", "Random", emptyList()))
        assertEquals("random", StoryQueue.next("recent")?.id)
        assertNull(StoryQueue.next("random")) // last story closes
        assertNull(StoryQueue.next("vault")) // not from the row
    }

    @Test
    fun referenceStoryRules() {
        val media = (1..30).map { photo("old$it", 40 * day + it) } +
            listOf(
                photo("new1", 1 * day), photo("new2", 2 * day, fav = true),
                photo("shot", 1 * day, folder = "/storage/emulated/0/Pictures/Screenshots"),
                photo("vid", 1 * day, type = MediaType.VIDEO),
                photo("lastYear", 365 * day - 2 * day)
            )
        val stories = StoryBuilder.build(media, now)
        assertEquals(listOf("recent", "random", "favorite", "timeline"), stories.map { it.id })
        val recent = stories[0].items.map { it.filePath }
        assertEquals(listOf("new1", "new2"), recent) // last 10 days, newest first, no screenshots or videos
        assertTrue(stories.all { it.items.size <= 15 })
        assertEquals(listOf("lastYear"), stories[3].items.map { it.filePath })
        assertTrue(stories[1].items.none { it.filePath == "shot" || it.filePath == "vid" })
    }
}
