package com.quizango.gallery

import com.quizango.gallery.data.local.MomentScanner
import com.quizango.gallery.domain.model.MediaItem
import org.junit.Assert.assertEquals
import org.junit.Test

class MomentScannerTest {
    private fun photo(path: String) = MediaItem(
        id = 0, filePath = path, fileName = path, fileSize = 0, folderName = "", folderPath = "",
        dateModified = 0, dateTaken = 0
    )

    @Test
    fun labelFileRoundTrips() {
        val text = MomentScanner.line("/a\tb.jpg", "Dog") + MomentScanner.line("/c.jpg", "")
        assertEquals(mapOf("/a b.jpg" to "Dog", "/c.jpg" to ""), MomentScanner.parse(text.lines()))
    }

    @Test
    fun groupsBiggestFirstAndSkipsUnlabelled() {
        val photos = listOf(photo("1"), photo("2"), photo("3"), photo("4"))
        val labels = mapOf("1" to "Sky", "2" to "Dog", "3" to "Dog", "4" to "")
        val groups = MomentScanner.group(photos, labels)
        assertEquals(listOf("Dog" to 2, "Sky" to 1), groups.map { it.first to it.second.size })
    }
}
