package com.quizango.gallery

import com.quizango.gallery.domain.model.MediaItem
import com.quizango.gallery.domain.model.MediaType
import org.junit.Assert.*
import org.junit.Test

class FavoritesTest {

    @Test
    fun testFavoriteFiltering() {
        val item1 = createMediaItem(id = 1, path = "/path/1.jpg", isFav = true)
        val item2 = createMediaItem(id = 2, path = "/path/2.jpg", isFav = false)
        val item3 = createMediaItem(id = 3, path = "/path/3.jpg", isFav = true, isTrash = true)
        val item4 = createMediaItem(id = 4, path = "/path/4.jpg", isFav = true)

        val allMedia = listOf(item1, item2, item3, item4)

        // Only active (not in trash) favorites should be shown
        val activeFavorites = allMedia.filter { it.isFavorite && it.deletedDate == 0L }

        assertEquals(2, activeFavorites.size)
        assertTrue(activeFavorites.any { it.filePath == item1.filePath })
        assertTrue(activeFavorites.any { it.filePath == item4.filePath })
        assertFalse(activeFavorites.any { it.filePath == item2.filePath })
        assertFalse(activeFavorites.any { it.filePath == item3.filePath })
    }

    @Test
    fun testToggleFavoriteLogic() {
        val originalItem = createMediaItem(id = 1, path = "/path/photo.jpg", isFav = false)
        val favoritedItem = originalItem.copy(isFavorite = true)

        assertTrue(favoritedItem.isFavorite)

        val unfavoritedItem = favoritedItem.copy(isFavorite = false)
        assertFalse(unfavoritedItem.isFavorite)
    }

    @Test
    fun testBatchUnfavoriteLogic() {
        val item1 = createMediaItem(id = 1, path = "/path/1.jpg", isFav = true)
        val item2 = createMediaItem(id = 2, path = "/path/2.jpg", isFav = true)
        val item3 = createMediaItem(id = 3, path = "/path/3.jpg", isFav = true)

        val selectedPaths = setOf(item1.filePath, item3.filePath)

        val remainingFavorites = listOf(item1, item2, item3).map { item ->
            if (item.filePath in selectedPaths) item.copy(isFavorite = false) else item
        }.filter { it.isFavorite }

        assertEquals(1, remainingFavorites.size)
        assertEquals(item2.filePath, remainingFavorites[0].filePath)
    }

    @Test
    fun testSelectAllAndUnselectAll() {
        val paths = listOf("/path/1.jpg", "/path/2.jpg", "/path/3.jpg")
        val allPathsSet = paths.toSet()

        var selected = setOf<String>()
        assertFalse(selected.isNotEmpty() && selected.size >= allPathsSet.size)

        // Select All
        selected = allPathsSet
        assertTrue(selected.isNotEmpty() && selected.size >= allPathsSet.size)

        // Unselect All
        selected = emptySet()
        assertEquals(0, selected.size)
    }

    @Test
    fun testFormatFavoriteDuration() {
        fun formatDuration(ms: Long): String {
            val totalSeconds = ms / 1000
            val minutes = totalSeconds / 60
            val seconds = totalSeconds % 60
            return "%02d:%02d".format(minutes, seconds)
        }

        assertEquals("00:00", formatDuration(0L))
        assertEquals("00:35", formatDuration(35000L))
        assertEquals("02:15", formatDuration(135000L))
        assertEquals("12:05", formatDuration(725000L))
    }

    private fun createMediaItem(
        id: Long,
        path: String,
        isFav: Boolean = false,
        isTrash: Boolean = false
    ): MediaItem {
        return MediaItem(
            id = id,
            filePath = path,
            fileName = path.substringAfterLast('/'),
            fileSize = 1024L,
            folderName = "Camera",
            folderPath = "/storage/emulated/0/DCIM/Camera",
            dateModified = System.currentTimeMillis(),
            dateTaken = System.currentTimeMillis(),
            mediaType = MediaType.IMAGE,
            isFavorite = isFav,
            deletedDate = if (isTrash) System.currentTimeMillis() else 0L
        )
    }
}
