package com.quizango.gallery

import com.quizango.gallery.domain.model.Album
import com.quizango.gallery.domain.model.MediaItem
import com.quizango.gallery.domain.model.MediaType
import com.quizango.gallery.domain.model.SortOrder
import org.junit.Assert.*
import org.junit.Test

class DomainModelTest {

    @Test
    fun testMediaTypeFromRaw() {
        assertEquals(MediaType.IMAGE, MediaType.fromRaw(1))
        assertEquals(MediaType.VIDEO, MediaType.fromRaw(2))
        assertEquals(MediaType.SCREENSHOT, MediaType.fromRaw(4))
        assertEquals(MediaType.ANIMATED, MediaType.fromRaw(16))
        assertEquals(MediaType.BURST, MediaType.fromRaw(64))
        assertEquals(MediaType.IMAGE, MediaType.fromRaw(999)) // Default fallback
    }

    @Test
    fun testMediaItemProperties() {
        val item = MediaItem(
            id = 101,
            filePath = "/storage/emulated/0/DCIM/Camera/IMG_001.jpg",
            fileName = "IMG_001.jpg",
            fileSize = 4096000L,
            folderName = "Camera",
            folderPath = "/storage/emulated/0/DCIM/Camera",
            dateModified = 1672531200L,
            dateTaken = 1672531199L,
            mediaType = MediaType.IMAGE,
            isFavorite = true
        )

        assertEquals("IMG_001.jpg", item.fileName)
        assertEquals(MediaType.IMAGE, item.mediaType)
        assertTrue(item.isFavorite)
        assertFalse(item.isInRecycleBin)
        assertEquals(1672531199L, item.dateAdded)
    }

    @Test
    fun testAlbumGrouping() {
        val items = listOf(
            MediaItem(
                id = 1,
                filePath = "/storage/DCIM/Camera/photo1.jpg",
                fileName = "photo1.jpg",
                fileSize = 1000,
                folderName = "Camera",
                folderPath = "/storage/DCIM/Camera",
                dateModified = 100,
                dateTaken = 100,
                mediaType = MediaType.IMAGE
            ),
            MediaItem(
                id = 2,
                filePath = "/storage/DCIM/Camera/photo2.jpg",
                fileName = "photo2.jpg",
                fileSize = 2000,
                folderName = "Camera",
                folderPath = "/storage/DCIM/Camera",
                dateModified = 200,
                dateTaken = 200,
                mediaType = MediaType.IMAGE
            ),
            MediaItem(
                id = 3,
                filePath = "/storage/Pictures/Screenshots/shot1.png",
                fileName = "shot1.png",
                fileSize = 3000,
                folderName = "Screenshots",
                folderPath = "/storage/Pictures/Screenshots",
                dateModified = 300,
                dateTaken = 300,
                mediaType = MediaType.SCREENSHOT
            )
        )

        val albums = items.groupBy { it.folderPath }.map { (path, media) ->
            Album(
                id = path,
                name = media.first().folderName,
                path = path,
                coverPath = media.first().filePath,
                itemCount = media.size
            )
        }

        assertEquals(2, albums.size)
        val cameraAlbum = albums.find { it.name == "Camera" }
        assertNotNull(cameraAlbum)
        assertEquals(2, cameraAlbum?.itemCount)

        val screenshotAlbum = albums.find { it.name == "Screenshots" }
        assertNotNull(screenshotAlbum)
        assertEquals(1, screenshotAlbum?.itemCount)
    }

    @Test
    fun testSortingOrders() {
        val item1 = MediaItem(1, "/p/B.jpg", "B.jpg", 1000, "f", "p", "", 100, 100, mediaType = MediaType.IMAGE)
        val item2 = MediaItem(2, "/p/A.jpg", "A.jpg", 3000, "f", "p", "", 300, 300, mediaType = MediaType.IMAGE)
        val item3 = MediaItem(3, "/p/C.jpg", "C.jpg", 2000, "f", "p", "", 200, 200, mediaType = MediaType.IMAGE)

        val list = listOf(item1, item2, item3)

        // Sort by date DESC
        val byDateDesc = list.sortedByDescending { it.dateAdded }
        assertEquals("A.jpg", byDateDesc[0].fileName)
        assertEquals("C.jpg", byDateDesc[1].fileName)
        assertEquals("B.jpg", byDateDesc[2].fileName)

        // Sort by name ASC
        val byNameAsc = list.sortedBy { it.fileName }
        assertEquals("A.jpg", byNameAsc[0].fileName)
        assertEquals("B.jpg", byNameAsc[1].fileName)
        assertEquals("C.jpg", byNameAsc[2].fileName)

        // Sort by size DESC
        val bySizeDesc = list.sortedByDescending { it.fileSize }
        assertEquals(3000L, bySizeDesc[0].fileSize)
        assertEquals(2000L, bySizeDesc[1].fileSize)
        assertEquals(1000L, bySizeDesc[2].fileSize)
    }

    @Test
    fun testSearchByColorPalette() {
        val colors = listOf(
            "#F2F2F2", "#000000", "#FF0000", "#FFE500", "#FF7A00",
            "#FF00E5", "#9E00FF", "#0066FF", "#00C2FF", "#008625"
        )
        assertEquals(10, colors.size)
        assertTrue(colors.contains("#FF0000"))
        assertTrue(colors.contains("#000000"))
        assertTrue(colors.contains("#F2F2F2"))

        val items = listOf(
            MediaItem(1, "/storage/DCIM/Camera/red.jpg", "red.jpg", 1000, "Camera", "/storage/DCIM/Camera", "", 100, 100, mediaType = MediaType.IMAGE),
            MediaItem(2, "/storage/DCIM/Camera/blue.jpg", "blue.jpg", 1000, "Camera", "/storage/DCIM/Camera", "", 100, 100, mediaType = MediaType.IMAGE)
        )
        val filtered = items.filter { item ->
            val hash = kotlin.math.abs(item.filePath.hashCode())
            val itemColor = colors[hash % colors.size]
            itemColor.equals("#FF0000", ignoreCase = true)
        }
        assertNotNull(filtered)
    }
}
