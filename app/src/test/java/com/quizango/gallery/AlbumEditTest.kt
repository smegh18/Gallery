package com.quizango.gallery

import com.quizango.gallery.data.local.datastore.albumCoverEntry
import com.quizango.gallery.data.local.datastore.parseAlbumCovers
import com.quizango.gallery.data.repository.movedPath
import com.quizango.gallery.domain.model.Album
import com.quizango.gallery.domain.model.MediaItem
import com.quizango.gallery.ui.home.AlbumNameCheck
import com.quizango.gallery.ui.home.albumNameExists
import com.quizango.gallery.ui.home.checkAlbumName
import com.quizango.gallery.ui.home.withCovers
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.nio.file.Files

class AlbumEditTest {
    private fun album(path: String) =
        Album(id = path, name = path, path = path, coverPath = "$path/newest.jpg", itemCount = 3, coverId = 1L)

    private fun item(id: Long, path: String, folder: String, video: Boolean = false) = MediaItem(
        id = id, filePath = path, fileName = path.substringAfterLast('/'), fileSize = 1, folderName = "x",
        folderPath = folder, dateModified = 0, dateTaken = 0,
        mediaType = if (video) com.quizango.gallery.domain.model.MediaType.VIDEO else com.quizango.gallery.domain.model.MediaType.IMAGE
    )

    @Test
    fun coverEntriesRoundTripAndBadEntriesAreIgnored() {
        assertEquals(mapOf("/a" to "/a/x.jpg"), parseAlbumCovers(setOf(albumCoverEntry("/a", "/a/x.jpg"))))
        assertEquals(emptyMap<String, String>(), parseAlbumCovers(setOf("no separator here")))
    }

    @Test
    fun coverOverridesTheNewestItemOnlyWhileTheFileIsInTheFolder() {
        val media = listOf(item(1, "/a/newest.jpg", "/a"), item(7, "/a/picked.mp4", "/a", video = true), item(9, "/b/z.jpg", "/b"))
        val albums = listOf(album("/a"), album("/b"), album("/c"))
        val covers = mapOf(
            "/a" to "/a/picked.mp4", // in the folder: wins, keeps its video flag
            "/b" to "/a/newest.jpg", // file belongs to another folder: ignored
            "/c" to "/c/gone.jpg" // file no longer exists: ignored
        )
        val out = withCovers(albums, covers, media)
        assertEquals("/a/picked.mp4", out[0].coverPath)
        assertEquals(7L, out[0].coverId)
        assertTrue(out[0].isVideoCover)
        assertEquals("/b/newest.jpg", out[1].coverPath)
        assertEquals("/c/newest.jpg", out[2].coverPath)
        assertFalse(out[2].isVideoCover)
    }

    @Test
    fun movedPathRewritesTheFolderItsTreeAndNothingElse() {
        val old = "/s/Pictures/Trip"
        val new = "/s/Pictures/Trips"
        assertEquals("/s/Pictures/Trips/a.jpg", movedPath("/s/Pictures/Trip/a.jpg", old, new))
        assertEquals("/s/Pictures/Trips/sub/b.jpg", movedPath("/s/Pictures/Trip/sub/b.jpg", old, new))
        assertEquals("/s/Pictures/Trips", movedPath(old, old, new))
        assertEquals("/s/Pictures/Trip2/a.jpg", movedPath("/s/Pictures/Trip2/a.jpg", old, new))
    }

    @Test
    fun renameNameRulesRefuseEmptyLongAndTakenNamesIgnoringCase() {
        assertEquals(AlbumNameCheck.EMPTY, checkAlbumName(""))
        assertEquals(AlbumNameCheck.TOO_LONG, checkAlbumName("a".repeat(50)))
        assertEquals(AlbumNameCheck.OK, checkAlbumName("a".repeat(49)))
        val parent = Files.createTempDirectory("rename").toFile()
        try {
            java.io.File(parent, "Trip").mkdir()
            assertTrue(albumNameExists("trip", listOf(parent))) // case-only rename
            assertTrue(albumNameExists("Trip", listOf(parent))) // unchanged
            assertFalse(albumNameExists("Trips", listOf(parent)))
        } finally {
            parent.deleteRecursively()
        }
    }
}
