package com.quizango.gallery

import com.quizango.gallery.domain.model.MediaItem
import com.quizango.gallery.ui.album.viewerRouteFor
import com.quizango.gallery.ui.browse.FAVORITES_ACTIONS
import com.quizango.gallery.ui.browse.ListState
import com.quizango.gallery.ui.browse.MediaAction
import com.quizango.gallery.ui.browse.listStateOf
import com.quizango.gallery.ui.browse.pruneSelection
import com.quizango.gallery.ui.home.ALBUM_FAVORITES
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

class FavoritesTest {

    private fun media(id: Long, fav: Boolean = true) = MediaItem(
        id = id, filePath = "/p/$id.jpg", fileName = "$id.jpg", fileSize = 1L,
        folderName = "p", folderPath = "/p", dateModified = 1L, dateTaken = 1L, isFavorite = fav
    )

    @Test
    fun favoritesShowsShareUnfavoritePrivateDeleteOnly() {
        assertEquals(listOf(MediaAction.SHARE, MediaAction.UNFAVORITE, MediaAction.PRIVATE, MediaAction.DELETE), FAVORITES_ACTIONS)
        assertFalse(MediaAction.ADD_TO in FAVORITES_ACTIONS)
        assertFalse(MediaAction.CREATE_ALBUM in FAVORITES_ACTIONS)
    }

    @Test
    fun unfavoritedRowsLeaveTheListAndTheSelection() {
        val a = media(1)
        val b = media(2)
        val c = media(3)
        val selected = setOf(a.filePath, c.filePath)
        // getFavoritesFlow re-emits without the unfavourited rows
        val after = listOf(a, b, c).map { if (it.filePath in selected) it.copy(isFavorite = false) else it }.filter { it.isFavorite }
        assertEquals(listOf(b), after)
        assertEquals(emptySet<String>(), pruneSelection(selected, after.mapTo(HashSet()) { it.filePath }))
        // a partial removal keeps the rest; nothing removed keeps the same set instance (no extra recomposition)
        assertEquals(setOf(b.filePath), pruneSelection(setOf(a.filePath, b.filePath), setOf(b.filePath)))
        val kept = setOf(b.filePath)
        assertSame(kept, pruneSelection(kept, setOf(a.filePath, b.filePath)))
    }

    @Test
    fun loadingAndEmptyAreDistinct() {
        assertEquals(ListState.LOADING, listStateOf(null, 3))
        assertEquals(ListState.LOADING, listStateOf(emptyList<MediaItem>(), null)) // column count not read yet
        assertEquals(ListState.EMPTY, listStateOf(emptyList<MediaItem>(), 3))
        assertEquals(ListState.CONTENT, listStateOf(listOf(media(1)), 4))
    }

    @Test
    fun viewerPagesThroughTheFavorites() {
        assertTrue(viewerRouteFor(media(1), ALBUM_FAVORITES).contains("folder=filter%3Afavorites"))
    }
}
