package com.quizango.gallery

import com.quizango.gallery.domain.model.Album
import com.quizango.gallery.domain.model.MediaItem
import com.quizango.gallery.ui.home.albumMediaPaths
import com.quizango.gallery.ui.home.ALBUM_DELETED
import com.quizango.gallery.ui.home.ALBUM_FAVORITES
import com.quizango.gallery.ui.home.albumGrid
import com.quizango.gallery.ui.home.albumIsSelectable
import com.quizango.gallery.ui.home.deletedCard
import com.quizango.gallery.ui.home.favoritesCard
import com.quizango.gallery.ui.home.pinLabel
import com.quizango.gallery.ui.home.singleAlbumActionAlpha
import com.quizango.gallery.ui.home.withPins
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AlbumSelectionTest {
    private fun album(id: String, pinned: Boolean = false) =
        Album(id = id, name = id, path = id, coverPath = "/c/$id", itemCount = 5, isPinned = pinned)

    @Test
    fun onlyDeviceFoldersAreSelectable() {
        assertTrue(albumIsSelectable(album("/sdcard/Pictures/a")))
        listOf("filter:all_photos", "filter:videos", "filter:camera", "filter:screenshots", ALBUM_FAVORITES, ALBUM_DELETED)
            .forEach { assertFalse(it, albumIsSelectable(album(it))) }
    }

    @Test
    fun pinLabelReadsUnpinOnlyForOnePinnedAlbum() {
        assertEquals(R.string.unpin_to_top, pinLabel(setOf("/a"), setOf("/a")))
        assertEquals(R.string.pin_to_top, pinLabel(setOf("/a"), emptySet()))
        assertEquals(R.string.pin_to_top, pinLabel(setOf("/a", "/b"), setOf("/a", "/b")))
    }

    @Test
    fun severalSelectedAlbumsDimTheSingleAlbumActions() {
        assertEquals(1f, singleAlbumActionAlpha(1), 0f)
        assertEquals(0.6f, singleAlbumActionAlpha(2), 0f)
    }

    @Test
    fun pinnedFoldersSortFirstAmongFolders() {
        val albums = withPins(
            listOf(album("filter:all_photos"), album("/a"), album("/b"), album("/c")),
            pinned = setOf("/c")
        )
        val grid = albumGrid(albums, favoritesCard(emptyList()), deletedCard(emptyList())).map { it.id }
        assertEquals(listOf("filter:all_photos", ALBUM_FAVORITES, "/c", "/a", "/b", ALBUM_DELETED), grid)
    }

    @Test
    fun withPinsMarksOnlyFolders() {
        val marked = withPins(listOf(album("filter:camera"), album("/c"), album("/d")), pinned = setOf("/c", "filter:camera"))
        assertEquals(listOf(false, true, false), marked.map { it.isPinned })
    }

    private fun item(path: String, folder: String) = MediaItem(
        id = 0, filePath = path, fileName = path, fileSize = 0, folderName = "", folderPath = folder,
        dateModified = 0, dateTaken = 0
    )

    @Test
    fun albumMediaPathsCollectsEveryFileOfTheSelectedFolders() {
        val media = listOf(item("/a/1.jpg", "/a"), item("/a/2.jpg", "/a"), item("/b/3.jpg", "/b"))
        assertEquals(listOf("/a/1.jpg", "/a/2.jpg"), albumMediaPaths(setOf("/a"), media))
        assertEquals(emptyList<String>(), albumMediaPaths(emptySet(), media))
    }
}
