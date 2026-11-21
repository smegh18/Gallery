package com.quizango.gallery

import com.quizango.gallery.domain.model.Album
import com.quizango.gallery.ui.album.otherAlbums
import com.quizango.gallery.ui.home.HomeUiState
import com.quizango.gallery.ui.home.albumRoute
import com.quizango.gallery.ui.navigation.Screen
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class AllAlbumsTest {

    private fun album(id: String, count: Int = 3) = Album(id = id, name = id.substringAfterLast('/'), path = id, coverPath = "", itemCount = count)

    @Test
    fun otherAlbumsKeepsDeviceFoldersInRepositoryOrder() {
        val albums = listOf(
            album("filter:all_photos"), album("/sdcard/Pictures/Zed"), album("filter:videos"),
            album("/sdcard/DCIM/Trips"), album("filter:screenshots"), album("/sdcard/Download")
        )
        val shown = otherAlbums(HomeUiState(albumsList = albums, initialSyncDone = true))
        assertEquals(listOf("/sdcard/Pictures/Zed", "/sdcard/DCIM/Trips", "/sdcard/Download"), shown?.map { it.path })
    }

    @Test
    fun loadingUntilTheFirstSyncThenEmpty() {
        assertNull(otherAlbums(HomeUiState()))
        assertTrue(otherAlbums(HomeUiState(initialSyncDone = true))!!.isEmpty())
        // only virtual albums: nothing to show, not loading
        assertTrue(otherAlbums(HomeUiState(albumsList = listOf(album("filter:all_photos")), initialSyncDone = true))!!.isEmpty())
    }

    @Test
    fun tapOpensThatFoldersMediaList() {
        val folder = album("/sdcard/DCIM/Trips & more")
        assertEquals(Screen.AlbumDetail.createRoute(folder.path, folder.name), albumRoute(folder, folder.name))
    }
}
