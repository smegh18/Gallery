package com.quizango.gallery

import com.quizango.gallery.domain.model.MediaItem
import com.quizango.gallery.ui.album.viewerRouteFor
import com.quizango.gallery.ui.browse.ListState
import com.quizango.gallery.ui.browse.MediaAction
import com.quizango.gallery.ui.browse.listStateOf
import com.quizango.gallery.ui.discovery.DEFAULT_SEARCH_COLOR
import com.quizango.gallery.ui.discovery.colorFromArg
import com.quizango.gallery.ui.discovery.colorSource
import com.quizango.gallery.ui.discovery.searchColorActions
import com.quizango.gallery.ui.navigation.Screen
import com.quizango.gallery.ui.search.SEARCH_SWATCH_HEX
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.net.URLDecoder

class SearchColorTest {

    @Test
    fun actionSlotsAreShareFavoriteOrUnfavoritePrivateDeleteInOrder() {
        assertEquals(
            listOf(MediaAction.SHARE, MediaAction.FAVORITE, MediaAction.PRIVATE, MediaAction.DELETE),
            searchColorActions(allFavorite = false)
        )
        assertEquals(
            listOf(MediaAction.SHARE, MediaAction.UNFAVORITE, MediaAction.PRIVATE, MediaAction.DELETE),
            searchColorActions(allFavorite = true)
        )
        // No Add to / Create album slot on this screen
        assertFalse(searchColorActions(false).any { it == MediaAction.ADD_TO || it == MediaAction.CREATE_ALBUM })
    }

    @Test
    fun selectedColorIsDecodedExactlyFromTheRoute() {
        SEARCH_SWATCH_HEX.forEach { hex ->
            val route = Screen.SearchColor.createRoute(hex)
            assertEquals(hex, colorFromArg(route.substringAfter("color=")))   // still encoded (%23...)
            assertEquals(hex, colorFromArg(hex))                              // already decoded by the nav host
        }
        assertEquals("#007AFF", colorFromArg("%23007aff")) // picker colours are upper-cased
    }

    @Test
    fun malformedColorArgsFallBackToTheRouteDefaultInsteadOfReachingTheFilter() {
        assertEquals(DEFAULT_SEARCH_COLOR, colorFromArg(null))
        assertEquals(DEFAULT_SEARCH_COLOR, colorFromArg(""))
        assertEquals(DEFAULT_SEARCH_COLOR, colorFromArg("red"))
        assertEquals(DEFAULT_SEARCH_COLOR, colorFromArg("#12345"))
        assertEquals(DEFAULT_SEARCH_COLOR, colorFromArg("#GGGGGG"))
        assertEquals(DEFAULT_SEARCH_COLOR, colorFromArg("100%"))
    }

    @Test
    fun viewerRoutesKeepTheColorListContext() {
        assertEquals("filter:color:#FF00E5", colorSource("#FF00E5"))
        val item = MediaItem(id = 1, filePath = "/x/1.jpg", fileName = "1.jpg", fileSize = 1, folderName = "x", folderPath = "/x", dateModified = 1, dateTaken = 1)
        val route = viewerRouteFor(item, colorSource("#FF00E5"))
        assertEquals(Screen.MediaViewer.createRoute("/x/1.jpg", "filter:color:#FF00E5"), route)
        val folder = route.substringAfter("folder=").substringBefore("&")
        assertEquals("filter:color:#FF00E5", URLDecoder.decode(folder, "UTF-8"))
    }

    @Test
    fun loadingDiffersFromTheNoItemsState() {
        // null = the first query has not returned: spinner, never the empty copy
        assertEquals(ListState.LOADING, listStateOf(null, 3))
        assertEquals(ListState.LOADING, listStateOf(emptyList<MediaItem>(), null))
        assertEquals(ListState.EMPTY, listStateOf(emptyList<MediaItem>(), 3))
        assertTrue(listStateOf(listOf(1), 3) == ListState.CONTENT)
    }
}
