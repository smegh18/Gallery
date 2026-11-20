package com.quizango.gallery

import com.quizango.gallery.domain.model.Album
import com.quizango.gallery.domain.model.MediaItem
import com.quizango.gallery.ui.home.ALBUM_DELETED
import com.quizango.gallery.ui.home.ALBUM_FAVORITES
import com.quizango.gallery.ui.home.albumCardTitle
import com.quizango.gallery.ui.home.albumGrid
import com.quizango.gallery.ui.home.albumRoute
import com.quizango.gallery.ui.home.deletedCard
import com.quizango.gallery.ui.home.favoritesCard
import com.quizango.gallery.ui.navigation.Screen
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class AlbumsTabTest {
    private fun album(id: String, count: Int = 5) =
        Album(id = id, name = id, path = id, coverPath = "/c/$id", itemCount = count)

    private fun item(path: String, deleted: Long = 0L) = MediaItem(
        id = 0, filePath = path, fileName = path, fileSize = 0, folderName = "", folderPath = "",
        dateModified = 0, dateTaken = 0, deletedDate = deleted
    )

    private val favorite = favoritesCard(emptyList())
    private val deleted = deletedCard(emptyList())

    @Test
    fun gridOrderFollowsTheReference() {
        val albums = listOf(
            album("filter:all_photos"), album("filter:videos"), album("filter:camera"), album("filter:screenshots"),
            album("/a"), album("/b")
        )
        assertEquals(
            listOf("filter:all_photos", ALBUM_FAVORITES, "filter:videos", "filter:camera", "filter:screenshots", "/a", "/b", ALBUM_DELETED),
            albumGrid(albums, favorite, deleted).map { it.id }
        )
    }

    @Test
    fun emptyVirtualAlbumsAreDroppedButFavoriteAndDeletedStay() {
        val albums = listOf(
            album("filter:all_photos"), album("filter:videos", 0), album("filter:camera", 0), album("filter:screenshots", 0), album("/a")
        )
        assertEquals(
            listOf("filter:all_photos", ALBUM_FAVORITES, "/a", ALBUM_DELETED),
            albumGrid(albums, favorite, deleted).map { it.id }
        )
        assertEquals(0, favorite.itemCount)
        assertEquals(0, deleted.itemCount)
    }

    @Test
    fun cardTitles() {
        assertEquals(R.string.recent, albumCardTitle(album("filter:all_photos")))
        assertEquals(R.string.favorite, albumCardTitle(favorite))
        assertEquals(R.string.album_tab_all_video, albumCardTitle(album("filter:videos")))
        assertEquals(R.string.camera, albumCardTitle(album("filter:camera")))
        assertEquals(R.string.screenshot, albumCardTitle(album("filter:screenshots")))
        assertEquals(R.string.album_tab_deleted, albumCardTitle(deleted))
        assertNull(albumCardTitle(album("/storage/emulated/0/Download")))
    }

    @Test
    fun routes() {
        assertEquals(Screen.Favorites.route, albumRoute(favorite, ""))
        assertEquals(Screen.RecycleBin.route, albumRoute(deleted, ""))
        assertEquals(Screen.AlbumDetail.createRoute("filter:all_photos", "All Photos"), albumRoute(album("filter:all_photos"), "All Photos"))
        assertEquals(Screen.AlbumDetail.createRoute("/a", "Download"), albumRoute(album("/a"), "Download"))
    }

    @Test
    fun deletedCardCoverIsTheNewestTrashedItem() {
        val card = deletedCard(listOf(item("old", 10), item("newest", 30), item("mid", 20)))
        assertEquals("newest", card.coverPath)
        assertEquals(3, card.itemCount)
        assertEquals(ALBUM_DELETED, card.id)
    }
}
