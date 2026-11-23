package com.quizango.gallery

import com.quizango.gallery.data.local.PathMutationOutcome
import com.quizango.gallery.data.local.PathMutationOutcome.Status
import com.quizango.gallery.data.repository.LARGE_VIDEO_MIN_BYTES
import com.quizango.gallery.data.repository.largeVideos
import com.quizango.gallery.domain.model.MediaItem
import com.quizango.gallery.domain.model.MediaType
import com.quizango.gallery.ui.browse.sharedMediaRows
import com.quizango.gallery.ui.browse.sharedRowSpan
import com.quizango.gallery.ui.cleaner.CleanerMediaKind
import com.quizango.gallery.ui.cleaner.cleanerDeleteLabel
import com.quizango.gallery.ui.cleaner.runCleanerDelete
import com.quizango.gallery.ui.home.PhotoRow
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CleanerMediaTest {
    private val day = 86_400_000L
    private val base = 1_730_000_000_000L

    private fun media(name: String, size: Long, date: Long, type: MediaType = MediaType.VIDEO, folder: String = "/storage/emulated/0/DCIM/Camera") =
        MediaItem(0, "$folder/$name", name, size, folder.substringAfterLast('/'), folder, dateModified = date, dateTaken = date, mediaType = type)

    @Test
    fun `large videos start at exactly 70 MiB and list newest first`() {
        assertEquals(73_400_320L, LARGE_VIDEO_MIN_BYTES)
        val list = listOf(
            media("old.mp4", LARGE_VIDEO_MIN_BYTES, base - day),
            media("under.mp4", LARGE_VIDEO_MIN_BYTES - 1, base),
            media("new.mp4", LARGE_VIDEO_MIN_BYTES * 2, base),
            media("big.jpg", LARGE_VIDEO_MIN_BYTES * 3, base, MediaType.IMAGE)
        )
        assertEquals(listOf("new.mp4", "old.mp4"), largeVideos(list).map { it.fileName })
    }

    @Test
    fun `day headers span the whole row in a three column grid`() {
        val rows = sharedMediaRows(listOf(media("a.mp4", 1, base), media("b.mp4", 1, base - 2 * day)))
        assertEquals(listOf(PhotoRow.Header::class, PhotoRow.Media::class, PhotoRow.Header::class, PhotoRow.Media::class), rows.map { it::class })
        val columns = CleanerMediaKind.LARGE_VIDEO.columns
        assertEquals(3, columns)
        assertEquals(3, sharedRowSpan(rows[0], columns))
        assertEquals(1, sharedRowSpan(rows[1], columns))
    }

    @Test
    fun `delete label follows the selection count, never a size`() {
        val label = { n: Int -> cleanerDeleteLabel(n, 3, "Delete Selected", "Delete", "Delete All") }
        assertEquals("Delete Selected", label(0))
        assertEquals("Delete (2)", label(2))
        assertEquals("Delete All (3)", label(3))
    }

    @Test
    fun `confirmed delete sends only the snapshot and keeps the failures selected`() = runBlocking {
        val sent = mutableListOf<List<String>>()
        val failed = runCleanerDelete(listOf("/a", "/b", "/c")) { paths ->
            sent += paths
            listOf(
                PathMutationOutcome("/a", Status.SUCCESS),
                PathMutationOutcome("/b", Status.FILE_FAILED),
                PathMutationOutcome("/c", Status.METADATA_FAILED)
            )
        }
        assertEquals(listOf(listOf("/a", "/b", "/c")), sent)
        assertEquals(setOf("/b", "/c"), failed)
        assertTrue(runCleanerDelete(listOf("/a")) { listOf(PathMutationOutcome("/a", Status.SUCCESS)) }.isEmpty())
    }
}
