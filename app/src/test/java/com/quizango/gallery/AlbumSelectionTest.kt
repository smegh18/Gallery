package com.quizango.gallery

import com.quizango.gallery.domain.model.Album
import com.quizango.gallery.ui.home.ALBUM_DELETED
import com.quizango.gallery.ui.home.ALBUM_FAVORITES
import com.quizango.gallery.ui.home.albumIsSelectable
import com.quizango.gallery.ui.home.pinLabel
import com.quizango.gallery.ui.home.singleAlbumActionAlpha
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
}
