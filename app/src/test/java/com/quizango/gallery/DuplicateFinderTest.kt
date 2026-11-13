package com.quizango.gallery

import com.quizango.gallery.data.local.DuplicateFinder
import com.quizango.gallery.domain.model.MediaItem
import org.junit.Assert.assertEquals
import org.junit.Test

class DuplicateFinderTest {
    private fun photo(path: String, size: Long, date: Long = 0) = MediaItem(
        id = 1, filePath = path, fileName = path, fileSize = size, folderName = "", folderPath = "",
        dateModified = date, dateTaken = date
    )

    @Test
    fun groupsOnlySameFingerprintNotSameSize() {
        val a = photo("a", 100, 1); val b = photo("b", 100, 2); val c = photo("c", 999, 3); val d = photo("d", 5)
        // a and b have the same size but different pictures; a and c are the same picture
        val groups = DuplicateFinder.group(listOf(a, b, c, d), mapOf("a" to "x", "b" to "y", "c" to "x"))
        assertEquals(listOf(listOf("c", "a")), groups.map { g -> g.map { it.filePath } })
    }

    @Test
    fun cacheLinesRoundTripAndHexIsPadded() {
        val k = DuplicateFinder.key(photo("/p\t1.jpg", 10, 20))
        assertEquals(mapOf("/p 1.jpg|10|20" to "ab"), DuplicateFinder.parse(DuplicateFinder.line(k, "ab").lines()))
        assertEquals("000f10ff", DuplicateFinder.hex(byteArrayOf(0, 15, 16, -1)))
    }
}
