package albums.gallery.photo.folder.picasa.app.web.gallery

import albums.gallery.photo.folder.picasa.app.web.gallery.domain.model.Album
import albums.gallery.photo.folder.picasa.app.web.gallery.domain.model.MediaItem
import albums.gallery.photo.folder.picasa.app.web.gallery.domain.model.MediaType
import albums.gallery.photo.folder.picasa.app.web.gallery.domain.model.SortOrder
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
}
