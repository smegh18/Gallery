package com.quizango.gallery

import com.quizango.gallery.domain.model.MediaItem
import com.quizango.gallery.ui.browse.MediaAction
import com.quizango.gallery.ui.browse.allFavorite
import com.quizango.gallery.ui.browse.mediaListActions
import com.quizango.gallery.ui.browse.sharedHeaderDate
import com.quizango.gallery.ui.browse.sharedMediaRows
import com.quizango.gallery.ui.home.PhotoRow
import com.quizango.gallery.ui.home.clampColumnCount
import com.quizango.gallery.ui.home.dayStartOf
import com.quizango.gallery.ui.home.nextColumnCount
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Calendar

class SharedMediaListTest {

    private fun at(y: Int, m: Int, d: Int, h: Int) = Calendar.getInstance().apply {
        clear()
        set(y, m - 1, d, h, 0)
    }.timeInMillis

    private fun media(id: Long, taken: Long, fav: Boolean = false) = MediaItem(
        id = id, filePath = "/p/$id.jpg", fileName = "$id.jpg", fileSize = 1L,
        folderName = "p", folderPath = "/p", dateModified = 0L, dateTaken = taken, isFavorite = fav
    )

    @Test
    fun rowsGroupByDayWithoutAnAdRowAndKeepMediaOrder() {
        val now = at(2026, 6, 15, 12)
        val today1 = media(1, at(2026, 6, 15, 10))
        val today2 = media(2, at(2026, 6, 15, 8))
        val yesterday = media(3, at(2026, 6, 14, 9))
        val old = media(4, at(2024, 1, 5, 9))
        val rows = sharedMediaRows(listOf(today1, today2, yesterday, old))

        assertTrue(rows.none { it == PhotoRow.Ad })
        assertEquals(
            listOf(
                PhotoRow.Header(dayStartOf(today1.dateAdded)), PhotoRow.Media(today1), PhotoRow.Media(today2),
                PhotoRow.Header(dayStartOf(yesterday.dateAdded)), PhotoRow.Media(yesterday),
                PhotoRow.Header(dayStartOf(old.dateAdded)), PhotoRow.Media(old)
            ),
            rows
        )
        // Today / Yesterday come from string resources; every other day is dd MMM yyyy in English (ka/a)
        assertNull(sharedHeaderDate(dayStartOf(today1.dateAdded), now))
        assertNull(sharedHeaderDate(dayStartOf(yesterday.dateAdded), now))
        assertEquals("05 Jan 2024", sharedHeaderDate(dayStartOf(old.dateAdded), now))
        assertEquals("01 Mar 2026", sharedHeaderDate(dayStartOf(at(2026, 3, 1, 12)), now)) // same year keeps the year
        assertTrue(sharedMediaRows(emptyList()).isEmpty())
    }

    @Test
    fun pinchStepsOneColumnWithinThreeToFive() {
        assertEquals(4, nextColumnCount(3, 0.85f))
        assertEquals(5, nextColumnCount(4, 0.5f)) // one step per re-based ratio, never two
        assertEquals(5, nextColumnCount(5, 0.5f))
        assertEquals(3, nextColumnCount(3, 2f))
        assertEquals(3, clampColumnCount(1))
        assertEquals(5, clampColumnCount(9))
    }

    @Test
    fun favoriteSlotTurnsToUnfavoriteOnlyWhenEverySelectedItemIsAFavorite() {
        val fav = media(1, 1L, fav = true)
        val plain = media(2, 1L)
        val items = listOf(fav, plain)
        assertTrue(allFavorite(items, setOf(fav.filePath)))
        assertFalse(allFavorite(items, setOf(fav.filePath, plain.filePath)))
        assertFalse(allFavorite(items, emptySet()))
        assertEquals(
            listOf(MediaAction.SHARE, MediaAction.FAVORITE, MediaAction.ADD_TO, MediaAction.PRIVATE, MediaAction.DELETE, MediaAction.CREATE_ALBUM),
            mediaListActions(allFavorite = false)
        )
        assertEquals(MediaAction.UNFAVORITE, mediaListActions(allFavorite = true)[1])
    }
}
