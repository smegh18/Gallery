package com.quizango.gallery

import com.quizango.gallery.domain.model.SortDirection
import com.quizango.gallery.domain.model.SortField
import com.quizango.gallery.domain.model.SortOrder
import com.quizango.gallery.R
import com.quizango.gallery.domain.model.MediaItem
import com.quizango.gallery.domain.model.MediaType
import com.quizango.gallery.ui.home.HOME_TOP_FEATURES
import com.quizango.gallery.ui.home.HeaderKind
import com.quizango.gallery.ui.home.PhotoRow
import com.quizango.gallery.ui.home.buildPhotoRows
import com.quizango.gallery.ui.home.dayStartOf
import com.quizango.gallery.ui.home.fileTypeLabel
import com.quizango.gallery.ui.home.headerKind
import com.quizango.gallery.ui.home.indexForThumbFraction
import com.quizango.gallery.ui.home.nextColumnCount
import com.quizango.gallery.ui.home.pinchPreviewScale
import com.quizango.gallery.ui.home.thumbFraction
import com.quizango.gallery.ui.home.MEDIA_TYPES
import com.quizango.gallery.ui.home.formatDuration
import com.quizango.gallery.ui.home.formatGroupHeader
import org.junit.Assert.*
import org.junit.Test
import java.util.Calendar

class HomeTest {

    @Test
    fun testFormatDuration() {
        assertEquals("0:00", formatDuration(0L))
        assertEquals("0:00", formatDuration(-100L))
        assertEquals("00:05", formatDuration(5000L))
        assertEquals("01:05", formatDuration(65000L))
        assertEquals("10:00", formatDuration(600000L))
        assertEquals("1:01:05", formatDuration(3665000L))
    }

    @Test
    fun testFormatGroupHeader() {
        val now = System.currentTimeMillis()
        assertEquals("Today", formatGroupHeader(now))

        val oneDayAgo = Calendar.getInstance().apply {
            add(Calendar.DAY_OF_YEAR, -1)
        }.timeInMillis
        assertEquals("Yesterday", formatGroupHeader(oneDayAgo))

        val olderDate = Calendar.getInstance().apply {
            set(2024, Calendar.JANUARY, 15, 12, 0, 0)
        }.timeInMillis
        val formatted = formatGroupHeader(olderDate)
        assertTrue(formatted.contains("Jan"))
        assertTrue(formatted.contains("2024"))
    }

    @Test
    fun testTopFeaturesList() {
        assertEquals(10, HOME_TOP_FEATURES.size)
        val ids = HOME_TOP_FEATURES.map { it.id }
        assertEquals(
            listOf("vault", "favorites", "cleaner", "duplicates", "recycle", "collage", "editor", "videos", "place", "moments"),
            ids
        )
    }

    @Test
    fun testMediaTypesList() {
        assertEquals(4, MEDIA_TYPES.size)
        val ids = MEDIA_TYPES.map { it.id }
        assertEquals(
            listOf("portrait", "high_definition", "burst", "animated"),
            ids
        )
    }

    @Test
    fun testSortOrder() {
        val defaultSort = SortOrder.DATE_DESC
        assertEquals(SortField.DATE_MODIFIED, defaultSort.field)
        assertEquals(SortDirection.DESCENDING, defaultSort.direction)

        val nameAsc = SortOrder(SortField.NAME, SortDirection.ASCENDING)
        assertEquals(SortField.NAME, nameAsc.field)
        assertEquals(SortDirection.ASCENDING, nameAsc.direction)

        val sizeDesc = SortOrder(SortField.SIZE, SortDirection.DESCENDING)
        assertEquals(SortField.SIZE, sizeDesc.field)
        assertEquals(SortDirection.DESCENDING, sizeDesc.direction)
    }

    @Test
    fun testHomeTabs() {
        val tabNames = listOf("Albums", "All Media", "Explore")
        assertEquals(3, tabNames.size)
        assertEquals("Albums", tabNames[0])
        assertEquals("All Media", tabNames[1])
        assertEquals("Explore", tabNames[2])
    }
    @Test
    fun testFeaturesAndMediaTypesResourcesValid() {
        HOME_TOP_FEATURES.forEach { feature ->
            assertTrue(feature.name.isNotBlank())
            assertTrue(feature.iconRes != 0)
        }
        MEDIA_TYPES.forEach { mediaType ->
            assertTrue(mediaType.title.isNotBlank())
            assertTrue(mediaType.iconRes != 0)
        }
    }

    private fun at(y: Int, m: Int, d: Int, h: Int): Long =
        Calendar.getInstance().apply { clear(); set(y, m - 1, d, h, 0, 0) }.timeInMillis

    private fun media(id: Long, taken: Long) = MediaItem(
        id = id, filePath = "/p/$id.jpg", fileName = "$id.jpg", fileSize = 1L,
        folderName = "p", folderPath = "/p", dateModified = 0L, dateTaken = taken
    )

    @Test
    fun testBuildPhotoRows() {
        assertTrue(buildPhotoRows(emptyList()).isEmpty()) // no lone ad row
        val a1 = media(1, at(2026, 6, 15, 10))
        val a2 = media(2, at(2026, 6, 15, 8))
        val b = media(3, at(2026, 6, 14, 9) / 1000) // seconds are normalised
        val dayA = dayStartOf(a1.dateAdded)
        val dayB = dayStartOf(b.dateAdded)
        assertEquals(at(2026, 6, 15, 0), dayA)
        assertEquals(at(2026, 6, 14, 0), dayB)
        assertEquals(
            listOf(PhotoRow.Ad, PhotoRow.Header(dayA), PhotoRow.Media(a1), PhotoRow.Media(a2), PhotoRow.Header(dayB), PhotoRow.Media(b)),
            buildPhotoRows(listOf(a1, a2, b))
        )
    }

    @Test
    fun testHeaderKind() {
        val now = at(2026, 6, 15, 12)
        assertEquals(HeaderKind.TODAY, headerKind(dayStartOf(at(2026, 6, 15, 8)), now))
        assertEquals(HeaderKind.YESTERDAY, headerKind(dayStartOf(at(2026, 6, 14, 23)), now))
        assertEquals(HeaderKind.THIS_YEAR, headerKind(dayStartOf(at(2026, 3, 1, 12)), now))
        assertEquals(HeaderKind.OTHER_YEAR, headerKind(dayStartOf(at(2025, 12, 31, 12)), now))
    }

    @Test
    fun testFileTypeLabel() {
        assertEquals(R.string.GIF, fileTypeLabel("a.GIF", false))
        assertEquals(R.string.RAW, fileTypeLabel("b.dng", false))
        assertEquals(R.string.RAW, fileTypeLabel("c.CR3", false))
        assertEquals(R.string.SVG, fileTypeLabel("d.svg", false))
        assertNull(fileTypeLabel("e.jpg", false))
        assertNull(fileTypeLabel("f.gif", true))
    }

    @Test
    fun testFastScrollMath() {
        assertEquals(0f, thumbFraction(0, 10, 100), 0f)
        assertEquals(1f, thumbFraction(90, 10, 100), 0f)
        assertEquals(0.5f, thumbFraction(45, 10, 100), 0f)
        assertEquals(0f, thumbFraction(0, 10, 8), 0f) // list fits: no division by zero
        assertEquals(0, indexForThumbFraction(0f, 100))
        assertEquals(99, indexForThumbFraction(1f, 100))
        assertEquals(50, indexForThumbFraction(0.5f, 100))
        assertEquals(99, indexForThumbFraction(2f, 100))
    }

    @Test
    fun testNextColumnCount() {
        assertEquals(3, nextColumnCount(3, 1.3f)) // already the fewest
        assertEquals(3, nextColumnCount(4, 1.3f)) // spread: fewer, bigger cells
        assertEquals(5, nextColumnCount(4, 0.7f)) // pinch: more columns
        assertEquals(5, nextColumnCount(5, 0.7f)) // already the most
        assertEquals(4, nextColumnCount(4, 1.05f)) // dead zone
        assertEquals(4, nextColumnCount(4, 0.95f))
        assertEquals(1.05f, pinchPreviewScale(3, 2f), 0f) // elastic at the fewest columns
        assertEquals(0.95f, pinchPreviewScale(5, 0.5f), 0f) // elastic at the most
        assertEquals(4f / 3f, pinchPreviewScale(4, 2f), 1e-6f) // up to the 3-column cell size
        assertEquals(1.1f, pinchPreviewScale(4, 1.1f), 0f)
    }
}
