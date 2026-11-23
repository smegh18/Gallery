package com.quizango.gallery

import com.quizango.gallery.domain.model.MediaItem
import com.quizango.gallery.domain.model.MediaType
import com.quizango.gallery.domain.model.VaultItem
import com.quizango.gallery.ui.home.AlbumNameCheck
import com.quizango.gallery.ui.home.checkAlbumName
import com.quizango.gallery.ui.home.fileNameWithExt
import com.quizango.gallery.ui.viewer.DockAction as d
import com.quizango.gallery.ui.viewer.MoreAction
import com.quizango.gallery.ui.viewer.SLIDESHOW_DEFAULT_SECONDS
import com.quizango.gallery.ui.viewer.SUGGESTION_DELAY_MS
import com.quizango.gallery.ui.viewer.nextSlidePage
import com.quizango.gallery.ui.viewer.nextSlideSeconds
import com.quizango.gallery.ui.viewer.slideshowAllowed
import com.quizango.gallery.ui.viewer.Suggestion
import com.quizango.gallery.ui.viewer.dockActions
import com.quizango.gallery.ui.viewer.editBlockedMessage
import com.quizango.gallery.ui.viewer.suggestionsEligible
import com.quizango.gallery.ui.viewer.ViewerDialog
import com.quizango.gallery.ui.viewer.ViewerDialogKind
import com.quizango.gallery.ui.viewer.formatDuration
import com.quizango.gallery.ui.viewer.moreActions
import com.quizango.gallery.ui.viewer.showOnMapRoute
import com.quizango.gallery.ui.discovery.placeRoute
import com.quizango.gallery.ui.viewer.targetIfCurrent
import com.quizango.gallery.ui.navigation.Screen
import com.quizango.gallery.data.repository.captureFileName
import com.quizango.gallery.ui.viewer.VIDEO_SPEEDS
import com.quizango.gallery.ui.viewer.restartsFromZero
import com.quizango.gallery.ui.viewer.speedLabel
import androidx.media3.common.Player
import com.quizango.gallery.ui.viewer.leaveWithVideoTrigger
import org.junit.Assert.*
import org.junit.Test
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

class MediaViewerTest {

    private fun photo(path: String = "/p/IMG_1.jpg", place: String = "", lat: Float = 0f, lng: Float = 0f) = MediaItem(
        id = 1, filePath = path, fileName = File(path).name, fileSize = 1, folderName = "p", folderPath = "/p",
        dateModified = 1, dateTaken = 1, placeName = place, latitude = lat, longitude = lng
    )

    @Test
    fun moreActionsKeepReferenceOrder() {
        assertEquals(
            listOf(MoreAction.ADD_TO, MoreAction.RENAME, MoreAction.SLIDE_SHOW, MoreAction.WALLPAPER, MoreAction.PRINT, MoreAction.SHOW_ON_MAP),
            moreActions(isVault = false, isVideo = false, hasPlace = true)
        )
        assertEquals(
            listOf(MoreAction.ADD_TO, MoreAction.RENAME, MoreAction.SLIDE_SHOW, MoreAction.WALLPAPER, MoreAction.PRINT),
            moreActions(isVault = false, isVideo = false, hasPlace = false)
        )
        assertEquals(listOf(MoreAction.RENAME, MoreAction.WALLPAPER, MoreAction.PRINT), moreActions(isVault = true, isVideo = false, hasPlace = true))
        // Device-checked on 2.7.2: a video page's More has only Rename and Slide show
        assertEquals(listOf(MoreAction.RENAME, MoreAction.SLIDE_SHOW), moreActions(isVault = false, isVideo = true, hasPlace = false))
    }

    @Test
    fun dialogNeverActsOnAnotherPage() {
        val open = ViewerDialog(ViewerDialogKind.RENAME, "/p/IMG_1.jpg")
        assertEquals("/p/IMG_1.jpg", open.targetIfCurrent("/p/IMG_1.jpg"))
        // Swiped (or the list moved) between opening and Save
        assertNull(open.targetIfCurrent("/p/IMG_2.jpg"))
    }

    @Test
    fun renameRefusesFiftyCharactersAndKeepsExtension() {
        assertEquals(AlbumNameCheck.OK, checkAlbumName("a".repeat(49)))
        assertEquals(AlbumNameCheck.TOO_LONG, checkAlbumName("a".repeat(50)))
        assertEquals(AlbumNameCheck.EMPTY, checkAlbumName(""))
        assertEquals("beach.jpg", fileNameWithExt("beach", "jpg"))
        assertEquals("notes", fileNameWithExt("notes", ""))
    }

    @Test
    fun dockRowsPerMode() {
        assertEquals(listOf(d.SHARE, d.PRIVATE, d.EDIT, d.DELETE, d.MORE), dockActions(isRecycle = false, isVault = false, isVideo = false))
        assertEquals(listOf(d.SHARE, d.PRIVATE, d.ADD_TO, d.DELETE, d.MORE), dockActions(isRecycle = false, isVault = false, isVideo = true))
        assertEquals(listOf(d.RESTORE, d.DELETE), dockActions(isRecycle = true, isVault = false, isVideo = false))
        assertEquals(listOf(d.SHARE, d.UNHIDE, d.DELETE, d.MORE), dockActions(isRecycle = false, isVault = true, isVideo = false))
    }

    @Test
    fun suggestionsOnlyForStillNormalPhotos() {
        assertEquals(1_500L, SUGGESTION_DELAY_MS)
        assertTrue(suggestionsEligible(photo(), isRecycle = false, isVault = false))
        assertFalse(suggestionsEligible(photo().copy(mediaType = MediaType.VIDEO), isRecycle = false, isVault = false))
        assertFalse(suggestionsEligible(photo().copy(isAnimated = true), isRecycle = false, isVault = false))
        assertFalse(suggestionsEligible(photo(), isRecycle = true, isVault = false))
        assertFalse(suggestionsEligible(photo(), isRecycle = false, isVault = true))
        assertEquals(Suggestion.REMOVE_BG, Suggestion.entries.first())
        assertEquals(R.string.blur_background_guide, Suggestion.BLUR_BG.label)
    }

    @Test
    fun editRefusesAnimatedAndCorrupt() {
        assertEquals(R.string.toast_animated_image, editBlockedMessage(isAnimated = true, decodable = true))
        assertEquals(R.string.toast_corrupted_image, editBlockedMessage(isAnimated = false, decodable = false))
        assertNull(editBlockedMessage(isAnimated = false, decodable = true))
    }

    @Test
    fun slideshowTwoSecondsFromCurrentPageAndStopsAtEnd() {
        assertEquals(2, SLIDESHOW_DEFAULT_SECONDS)
        assertEquals(3, nextSlidePage(current = 2, lastIndex = 5))
        assertNull(nextSlidePage(current = 5, lastIndex = 5))
        // Duration tap cycles 2 → 3 → 4 → 5 → 1
        assertEquals(listOf(3, 4, 5, 1, 2), generateSequence(nextSlideSeconds(2), ::nextSlideSeconds).take(5).toList())
        assertTrue(slideshowAllowed(isRecycle = false, isVault = false))
        assertFalse(slideshowAllowed(isRecycle = true, isVault = false))
        assertFalse(slideshowAllowed(isRecycle = false, isVault = true))
    }

    @Test
    fun showOnMapOnlyWithPlace() {
        assertNull(showOnMapRoute(photo()))
        assertNull(showOnMapRoute(photo(place = "  ")))
        assertEquals(placeRoute("Paris"), showOnMapRoute(photo(place = "Paris")))
        assertEquals(placeRoute("48.8566,2.3522"), showOnMapRoute(photo(lat = 48.8566f, lng = 2.3522f)))
    }

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
        // Hours only when needed
        assertEquals("59:59", formatDuration(3_599_000L))
        assertEquals("01:00:00", formatDuration(3_600_000L))
    }

    @Test
    fun `inline video stops at the end and the next Play restarts from zero`() {
        assertTrue(restartsFromZero(Player.STATE_ENDED))
        assertFalse(restartsFromZero(Player.STATE_READY))
        assertFalse(restartsFromZero(Player.STATE_BUFFERING))
    }

    @Test
    fun `speeds are the five reference rows`() {
        assertEquals(listOf("0.25x", "0.5x", "1.0x", "1.5x", "2.0x"), VIDEO_SPEEDS.map(::speedLabel))
    }

    @Test
    fun `capture file is IMG_yyyyMMdd_ millis png`() {
        val millis = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.US).parse("2026-11-21 10:30")!!.time
        assertEquals("IMG_20261121_$millis.png", captureFileName(millis))
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

    @Test
    fun viewerExitRecordsTheVideoTriggerOnlyAfterAVideoWasShown() {
        val calls = mutableListOf<String>()
        leaveWithVideoTrigger(recordsVideo = true, navigate = { calls += "navigate" }, trigger = { calls += "trigger" })
        assertEquals(listOf("trigger", "navigate"), calls)
        calls.clear()
        leaveWithVideoTrigger(recordsVideo = false, navigate = { calls += "navigate" }, trigger = { calls += "trigger" })
        assertEquals(listOf("navigate"), calls)
    }
}
