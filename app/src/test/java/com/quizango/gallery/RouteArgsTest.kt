package com.quizango.gallery

import com.quizango.gallery.domain.model.MediaItem
import com.quizango.gallery.ui.album.viewerRouteFor
import com.quizango.gallery.ui.discovery.colorFromArg
import com.quizango.gallery.ui.navigation.Screen
import com.quizango.gallery.ui.search.viewAllArgs
import com.quizango.gallery.ui.viewer.viewerDisplayList
import com.quizango.gallery.ui.viewer.viewerTargetIndex
import org.junit.Assert.assertEquals
import org.junit.Test
import java.net.URI

/**
 * What the nav host hands a destination for a query argument: androidx.navigation 2.8.8 reads query arguments
 * with Uri.getQueryParameters (percent-decoded, "+" kept) in NavDeepLink. java.net.URI.getQuery decodes the same way.
 */
internal fun navArg(route: String, name: String): String {
    val raw = route.substringAfter('?').split('&').first { it.startsWith("$name=") }.substringAfter("=")
    return URI("app://x/?$raw").query
}

class RouteArgsTest {
    private val awkward = listOf("c++", "100%", "50% off", "%41", "a+b c", "São Paulo & co/50%", "x=y&z")

    @Test
    fun searchQueryReachesViewAllUnchangedAfterTheNavigationDecode() {
        awkward.forEach { q ->
            val args = viewAllArgs("1", navArg(Screen.SearchViewAll.createRoute(1, q), "query"))
            assertEquals(q, args.second)
        }
    }

    @Test
    fun viewerPathAndFolderSurviveTheNavigationDecode() {
        val path = "/sdcard/DCIM/50% off+a b.jpg"
        val route = Screen.MediaViewer.createRoute(path, "filter:place:Rio & co")
        assertEquals(path, navArg(route, "path"))
        assertEquals("filter:place:Rio & co", navArg(route, "folder"))
    }

    @Test
    fun colorRouteStillResolvesTheSelectedColor() {
        assertEquals("#00FF00", colorFromArg(navArg(Screen.SearchColor.createRoute("#00ff00"), "color")))
    }
}

/** The browse-to-viewer tracer: a tapped list item, the route it builds, the arguments the viewer receives, the page it opens on. */
class BrowseToViewerTracerTest {
    private fun item(i: Int, name: String) = MediaItem(
        id = i.toLong(), filePath = "/storage/emulated/0/DCIM/$name", fileName = name, fileSize = 1,
        folderName = "DCIM", folderPath = "/storage/emulated/0/DCIM", dateModified = 1, dateTaken = i.toLong()
    )

    @Test
    fun everyTappedItemOpensItsOwnPageEvenWithAwkwardFileNames() {
        val album = listOf("a.jpg", "50% off.jpg", "c+d.jpg", "%41.jpg", "e f&g.jpg", "clip.mp4").mapIndexed(::item)
        album.forEachIndexed { tapped, it ->
            val route = viewerRouteFor(it, "filter:favorites")
            val path = navArg(route, "path")
            val source = navArg(route, "folder")
            assertEquals("filter:favorites", source)
            val pages = viewerDisplayList(album, path, it) { true }
            assertEquals(album, pages)
            assertEquals(tapped, viewerTargetIndex(pages, path))
        }
    }

    @Test
    fun anOpenedFileMissingFromTheListIsAddedOnlyWhileItStillExists() {
        val album = listOf(item(1, "a.jpg"), item(2, "b.jpg"))
        val opened = item(9, "new.jpg")
        assertEquals(listOf(opened) + album, viewerDisplayList(album, opened.filePath, opened) { true })
        assertEquals(album, viewerDisplayList(album, opened.filePath, opened) { false })
        assertEquals(listOf(opened), viewerDisplayList(emptyList(), opened.filePath, opened) { true })
        assertEquals(0, viewerTargetIndex(album, opened.filePath))
    }
}
