package com.quizango.gallery

import com.quizango.gallery.domain.model.SortDirection
import com.quizango.gallery.domain.model.SortField
import com.quizango.gallery.domain.model.SortOrder
import com.quizango.gallery.ui.home.HOME_TOP_FEATURES
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
}
