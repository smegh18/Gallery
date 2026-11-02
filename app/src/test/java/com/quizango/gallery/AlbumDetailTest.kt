package com.quizango.gallery

import com.quizango.gallery.domain.model.Album
import com.quizango.gallery.domain.model.MediaItem
import com.quizango.gallery.domain.model.MediaType
import com.quizango.gallery.ui.home.formatGroupHeader
import com.quizango.gallery.ui.navigation.Screen
import org.junit.Assert.*
import org.junit.Test
import java.io.File

class AlbumDetailTest {

    private fun createSampleMediaItem(
        id: Long,
        name: String,
        isFavorite: Boolean = false,
        dateAdded: Long = 1730178000L
    ): MediaItem {
        return MediaItem(
            id = id,
            filePath = "/storage/emulated/0/DCIM/Camera/$name",
            fileName = name,
            fileSize = 2048000L,
            folderName = "Camera",
            folderPath = "/storage/emulated/0/DCIM/Camera",
            dateModified = dateAdded,
            dateTaken = dateAdded,
            isFavorite = isFavorite,
            mediaType = if (name.endsWith(".mp4")) MediaType.VIDEO else MediaType.IMAGE
        )
    }

    @Test
    fun testAlbumDetailGrouping() {
        val item1 = createSampleMediaItem(1, "photo1.jpg", dateAdded = 1730178000L) // 29 Oct
        val item2 = createSampleMediaItem(2, "photo2.jpg", dateAdded = 1730178000L)
        val item3 = createSampleMediaItem(3, "video1.mp4", dateAdded = 1729000000L)

        val items = listOf(item1, item2, item3)
        val grouped = items.groupBy { formatGroupHeader(it.dateAdded) }

        assertEquals(2, grouped.size)
        assertTrue(grouped.values.any { it.size == 2 })
        assertTrue(grouped.values.any { it.size == 1 })
    }

    @Test
    fun testSelectionModeToggle() {
        val items = listOf(
            createSampleMediaItem(1, "img1.jpg"),
            createSampleMediaItem(2, "img2.jpg"),
            createSampleMediaItem(3, "img3.jpg")
        )

        var selectedPaths = setOf<String>()
        assertFalse(selectedPaths.isNotEmpty())

        // Select first item
        selectedPaths = selectedPaths + items[0].filePath
        assertEquals(1, selectedPaths.size)
        assertTrue(selectedPaths.isNotEmpty())

        // Select all items
        selectedPaths = items.map { it.filePath }.toSet()
        assertEquals(3, selectedPaths.size)

        // Select All toggle: if all are selected, deselect all (matching reference app)
        val nextSelection = if (selectedPaths.size == items.size) {
            emptySet()
        } else {
            items.map { it.filePath }.toSet()
        }
        assertTrue(nextSelection.isEmpty())
    }

    @Test
    fun testFavoriteToggleLogic() {
        val favItem = createSampleMediaItem(1, "fav.jpg", isFavorite = true)
        val nonFavItem = createSampleMediaItem(2, "unfav.jpg", isFavorite = false)

        val items = listOf(favItem, nonFavItem)

        // Case 1: selection includes a non-favorite item -> action should be "Favorite"
        val selectedCase1 = setOf(favItem.filePath, nonFavItem.filePath)
        val anyNotFavCase1 = items.any { it.filePath in selectedCase1 && !it.isFavorite }
        assertTrue(anyNotFavCase1)

        // Case 2: selection contains only favorites -> action should be "Unfavorite"
        val selectedCase2 = setOf(favItem.filePath)
        val anyNotFavCase2 = items.any { it.filePath in selectedCase2 && !it.isFavorite }
        assertFalse(anyNotFavCase2)
    }

    @Test
    fun testAlbumNavigationRoutes() {
        val bucketId = "100"
        val bucketName = "Camera"

        val route = Screen.AlbumDetail.createRoute(bucketId, bucketName)
        assertEquals("album/100/Camera", route)

        val mediaRoute = Screen.MediaViewer.createRoute("/sdcard/DCIM/Camera/photo.jpg", bucketId)
        assertTrue(mediaRoute.startsWith("viewer?path="))
        assertTrue(mediaRoute.contains("folder=100"))
    }

    @Test
    fun testAlbumModelAttributes() {
        val album = Album(
            id = "camera_bucket",
            name = "Camera",
            path = "/storage/emulated/0/DCIM/Camera",
            coverPath = "/storage/emulated/0/DCIM/Camera/IMG_001.jpg",
            itemCount = 42
        )

        assertEquals("camera_bucket", album.id)
        assertEquals("Camera", album.name)
        assertEquals(42, album.itemCount)
        assertEquals("/storage/emulated/0/DCIM/Camera/IMG_001.jpg", album.coverPath)
    }
}
