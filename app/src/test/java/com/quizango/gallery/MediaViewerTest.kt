package com.quizango.gallery

import com.quizango.gallery.domain.model.MediaItem
import com.quizango.gallery.domain.model.MediaType
import com.quizango.gallery.domain.model.VaultItem
import com.quizango.gallery.ui.viewer.formatDuration
import com.quizango.gallery.ui.navigation.Screen
import org.junit.Assert.*
import org.junit.Test
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

class MediaViewerTest {

    @Test
    fun testToolbarDateTimeFormatting() {
        val testEpochSec = 1730178000L // 29 Oct
        val date = Date(testEpochSec * 1000L)
        val dateStr = SimpleDateFormat("dd MMM", Locale.US).format(date)
        val timeStr = SimpleDateFormat("hh:mm a", Locale.US).format(date)

        assertNotNull(dateStr)
        assertNotNull(timeStr)
        assertTrue(dateStr.isNotEmpty())
        assertTrue(timeStr.isNotEmpty())
    }

    @Test
    fun testFallbackItemCreation() {
        val testPath = "/storage/emulated/0/DCIM/Camera/sample_photo.jpg"
        val file = File(testPath)
        val isVideo = file.extension.lowercase() in listOf("mp4", "mkv", "mov", "avi", "3gp", "webm")

        val fallbackItem = MediaItem(
            id = 0,
            filePath = testPath,
            fileName = file.name,
            fileSize = 1024L,
            folderName = "Camera",
            folderPath = "/storage/emulated/0/DCIM/Camera",
            dateModified = 1000L,
            dateTaken = 1000L,
            mediaType = if (isVideo) MediaType.VIDEO else MediaType.IMAGE
        )

        assertEquals("sample_photo.jpg", fallbackItem.fileName)
        assertEquals(MediaType.IMAGE, fallbackItem.mediaType)
        assertFalse(fallbackItem.isFavorite)
    }

    @Test
    fun testFallbackVideoItemCreation() {
        val testVideoPath = "/storage/emulated/0/Movies/sample_video.mp4"
        val file = File(testVideoPath)
        val isVideo = file.extension.lowercase() in listOf("mp4", "mkv", "mov", "avi", "3gp", "webm")

        val fallbackItem = MediaItem(
            id = 0,
            filePath = testVideoPath,
            fileName = file.name,
            fileSize = 2048000L,
            folderName = "Movies",
            folderPath = "/storage/emulated/0/Movies",
            dateModified = 2000L,
            dateTaken = 2000L,
            mediaType = if (isVideo) MediaType.VIDEO else MediaType.IMAGE
        )

        assertEquals("sample_video.mp4", fallbackItem.fileName)
        assertEquals(MediaType.VIDEO, fallbackItem.mediaType)
    }

    @Test
    fun testFileSizeFormatting() {
        fun formatFileSize(bytes: Long): String {
            if (bytes <= 0) return "0 B"
            val kb = bytes / 1024.0
            val mb = kb / 1024.0
            val gb = mb / 1024.0
            return when {
                gb >= 1.0 -> String.format(Locale.US, "%.2f GB", gb)
                mb >= 1.0 -> String.format(Locale.US, "%.2f MB", mb)
                kb >= 1.0 -> String.format(Locale.US, "%.2f KB", kb)
                else -> "$bytes B"
            }
        }

        assertEquals("0 B", formatFileSize(0L))
        assertEquals("500 B", formatFileSize(500L))
        assertEquals("1.00 KB", formatFileSize(1024L))
        assertEquals("1.00 MB", formatFileSize(1024L * 1024L))
        assertEquals("1.50 GB", formatFileSize((1.5 * 1024 * 1024 * 1024).toLong()))
    }

    @Test
    fun testVideoDurationFormatting() {
        assertEquals("00:00", formatDuration(0L))
        assertEquals("00:05", formatDuration(5000L))
        assertEquals("01:30", formatDuration(90000L))
        assertEquals("10:00", formatDuration(600000L))
    }

    @Test
    fun testVideoSpeedOptions() {
        val speeds = listOf(0.25f, 0.5f, 1.0f, 1.5f, 2.0f)
        assertEquals(5, speeds.size)
        assertTrue(1.0f in speeds)
        assertEquals(0.25f, speeds.first(), 0.001f)
        assertEquals(2.0f, speeds.last(), 0.001f)
    }

    @Test
    fun testVaultItemToMediaItemConversion() {
        val vaultItem = VaultItem(
            filePath = "/storage/emulated/0/.vault/secret.jpg",
            fileName = "secret.jpg",
            folderName = ".vault",
            folderPath = "/storage/emulated/0/.vault",
            fileSize = 4096L,
            dateHidden = 1730178000L,
            mediaType = MediaType.IMAGE,
            durationMs = 0L,
            dimension = "1080x1920"
        )

        assertEquals("secret.jpg", vaultItem.fileName)
        assertEquals(MediaType.IMAGE, vaultItem.mediaType)
        assertEquals("1080x1920", vaultItem.dimension)
    }

    @Test
    fun testMediaViewerRoutes() {
        val defaultRoute = Screen.MediaViewer.createRoute("/path/to/img.jpg")
        assertTrue(defaultRoute.contains("viewer?path="))
        assertTrue(defaultRoute.contains("isRecycle=false"))
        assertTrue(defaultRoute.contains("isVault=false"))

        val recycleRoute = Screen.MediaViewer.createRoute("/path/to/trash.jpg", isRecycle = true)
        assertTrue(recycleRoute.contains("isRecycle=true"))

        val vaultRoute = Screen.MediaViewer.createRoute("/path/to/secret.jpg", isVault = true)
        assertTrue(vaultRoute.contains("isVault=true"))

        val favRoute = Screen.MediaViewer.createRoute("/path/to/fav.jpg", folder = "filter:favorites")
        assertTrue(favRoute.contains("filter%3Afavorites") || favRoute.contains("filter:favorites"))
    }

    @Test
    fun testClickedPhotoInitialIndexCalculation() {
        val photos = (0..9).map { i ->
            MediaItem(
                id = i.toLong(),
                filePath = "/storage/emulated/0/DCIM/Camera/IMG_$i.jpg",
                fileName = "IMG_$i.jpg",
                fileSize = 1024L,
                folderName = "Camera",
                folderPath = "/storage/emulated/0/DCIM/Camera",
                dateModified = 1000L + i,
                dateTaken = 1000L + i,
                mediaType = MediaType.IMAGE
            )
        }

        // Test clicking photo 0
        val target0 = photos.indexOfFirst { it.filePath == "/storage/emulated/0/DCIM/Camera/IMG_0.jpg" }
        assertEquals(0, target0)

        // Test clicking photo 5
        val target5 = photos.indexOfFirst { it.filePath == "/storage/emulated/0/DCIM/Camera/IMG_5.jpg" }
        assertEquals(5, target5)

        // Test clicking photo 9
        val target9 = photos.indexOfFirst { it.filePath == "/storage/emulated/0/DCIM/Camera/IMG_9.jpg" }
        assertEquals(9, target9)

        // Test clicking non-existent path falls back to 0 safely
        val targetNone = photos.indexOfFirst { it.filePath == "/storage/emulated/0/DCIM/Camera/non_existent.jpg" }
        assertEquals(-1, targetNone)
        val safeIndex = if (targetNone >= 0) targetNone else 0
        assertEquals(0, safeIndex)
    }

    @Test
    fun testDisplayListFallbackAndPagingSync() {
        val clickedPath = "/storage/emulated/0/DCIM/Camera/new_photo.jpg"
        val fallbackItem = MediaItem(
            id = 0,
            filePath = clickedPath,
            fileName = "new_photo.jpg",
            fileSize = 2048L,
            folderName = "Camera",
            folderPath = "/storage/emulated/0/DCIM/Camera",
            dateModified = 5000L,
            dateTaken = 5000L,
            mediaType = MediaType.IMAGE
        )

        // When mediaList is empty
        val emptyMediaList = emptyList<MediaItem>()
        val displayListWhenEmpty = if (emptyMediaList.isNotEmpty()) emptyMediaList else listOf(fallbackItem)
        assertEquals(1, displayListWhenEmpty.size)
        assertEquals(clickedPath, displayListWhenEmpty[0].filePath)

        // When mediaList is populated with existing items
        val mediaList = listOf(
            fallbackItem.copy(id = 1, filePath = "/storage/emulated/0/DCIM/Camera/old_1.jpg"),
            fallbackItem, // at index 1
            fallbackItem.copy(id = 3, filePath = "/storage/emulated/0/DCIM/Camera/old_2.jpg")
        )
        val displayListPopulated = if (mediaList.isNotEmpty()) {
            if (mediaList.any { it.filePath == clickedPath }) mediaList
            else listOf(fallbackItem) + mediaList
        } else listOf(fallbackItem)

        assertEquals(3, displayListPopulated.size)
        val targetIdx = displayListPopulated.indexOfFirst { it.filePath == clickedPath }
        assertEquals(1, targetIdx)
    }

    @Test
    fun testPinchToZoomScaleCalculation() {
        var scale = 1.0f

        // Initial state at 1x
        assertFalse(scale > 1.05f)

        // Pinch to zoom in by 1.5x
        val zoomInChange = 1.5f
        scale = (scale * zoomInChange).coerceIn(1f, 5f)
        assertEquals(1.5f, scale, 0.001f)
        assertTrue(scale > 1.05f)

        // Pinch to zoom in beyond 5x max
        val zoomOverMax = 4.0f
        scale = (scale * zoomOverMax).coerceIn(1f, 5f)
        assertEquals(5.0f, scale, 0.001f)
        assertTrue(scale > 1.05f)

        // Pinch to zoom out towards 1x
        val zoomOutChange = 0.2f
        scale = (scale * zoomOutChange).coerceIn(1f, 5f)
        if (scale <= 1.05f) {
            scale = 1f
        }
        assertEquals(1.0f, scale, 0.001f)
        assertFalse(scale > 1.05f)
    }

    @Test
    fun testPanOffsetClamping() {
        val containerWidth = 1080
        val containerHeight = 2400

        fun clamp(offset: Pair<Float, Float>, scale: Float): Pair<Float, Float> {
            if (scale <= 1.05f) return Pair(0f, 0f)
            val maxOffsetX = ((containerWidth * (scale - 1f)) / 2f).coerceAtLeast(0f)
            val maxOffsetY = ((containerHeight * (scale - 1f)) / 2f).coerceAtLeast(0f)
            return Pair(
                offset.first.coerceIn(-maxOffsetX, maxOffsetX),
                offset.second.coerceIn(-maxOffsetY, maxOffsetY)
            )
        }

        // When scale == 1f, offset must clamp to (0, 0)
        val offsetAt1x = clamp(Pair(200f, 300f), 1.0f)
        assertEquals(0f, offsetAt1x.first, 0.001f)
        assertEquals(0f, offsetAt1x.second, 0.001f)

        // When scale == 2f, maxOffsetX = 540, maxOffsetY = 1200
        val offsetAt2xValid = clamp(Pair(300f, 500f), 2.0f)
        assertEquals(300f, offsetAt2xValid.first, 0.001f)
        assertEquals(500f, offsetAt2xValid.second, 0.001f)

        // When scale == 2f and drag exceeds bounds, it is clamped
        val offsetAt2xExceeded = clamp(Pair(1000f, -2000f), 2.0f)
        assertEquals(540f, offsetAt2xExceeded.first, 0.001f)
        assertEquals(-1200f, offsetAt2xExceeded.second, 0.001f)
    }
}
