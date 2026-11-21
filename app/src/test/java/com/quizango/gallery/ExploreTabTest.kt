package com.quizango.gallery

import com.quizango.gallery.ui.discovery.placeRoute
import com.quizango.gallery.ui.home.ExploreItem
import com.quizango.gallery.ui.home.exploreRoute
import com.quizango.gallery.ui.home.manageRows
import com.quizango.gallery.ui.home.showsCollageNew
import com.quizango.gallery.ui.home.showsDuplicateFinder
import com.quizango.gallery.ui.navigation.Screen
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ExploreTabTest {

    private fun route(item: ExploreItem, junk: Boolean = false) = exploreRoute(item, junk)

    @Test
    fun scanNowOpensRemoveJunkUnlessJunkWasClearedToday() {
        assertEquals(Screen.RemoveJunk.route, route(ExploreItem.SCAN, junk = false))
        assertEquals(Screen.Cleaner.route, route(ExploreItem.SCAN, junk = true))
    }

    @Test
    fun everyOtherItemOpensItsReferenceTarget() {
        assertEquals(Screen.Editor.createRoute(), route(ExploreItem.AI_EDITOR))
        assertEquals(Screen.CollageMaker.route, route(ExploreItem.COLLAGE))
        assertEquals(Screen.VaultLock.route, route(ExploreItem.VAULT))
        assertEquals(Screen.Favorites.route, route(ExploreItem.FAVORITES))
        assertEquals(Screen.Moments.route, route(ExploreItem.MOMENTS))
        assertEquals(placeRoute(), route(ExploreItem.PLACE))
        assertEquals(Screen.DuplicateWaiting.route, route(ExploreItem.DUPLICATE_FINDER))
        assertEquals(Screen.QuickOrganizer.route, route(ExploreItem.QUICK_ORGANIZE))
        assertEquals(Screen.RecycleBin.route, route(ExploreItem.RECYCLE_BIN))
        assertEquals(Screen.Settings.route, route(ExploreItem.SETTINGS))
    }

    @Test
    fun allElevenItemsAreCovered() {
        assertEquals(11, ExploreItem.values().size)
        ExploreItem.values().forEach { route(it) }
    }

    @Test
    fun collageBadgeShowsUntilOpenedOrFourthLaunch() {
        assertTrue(showsCollageNew(seen = false, session = 1))
        assertTrue(showsCollageNew(seen = false, session = 3))
        assertFalse(showsCollageNew(seen = false, session = 4))
        assertFalse(showsCollageNew(seen = true, session = 1))
    }

    @Test
    fun duplicateFinderNeedsAndroid10AndThreeGbRam() {
        assertTrue(showsDuplicateFinder(sdkInt = 29, totalMemBytes = 3L shl 30))
        assertFalse(showsDuplicateFinder(sdkInt = 28, totalMemBytes = 4L shl 30))
        assertFalse(showsDuplicateFinder(sdkInt = 33, totalMemBytes = 3L * 1024 * 1024 * 1024 - 1))
    }

    @Test
    fun manageRowsDropDuplicateFinderWhenHidden() {
        assertEquals(
            listOf(ExploreItem.DUPLICATE_FINDER, ExploreItem.QUICK_ORGANIZE, ExploreItem.RECYCLE_BIN, ExploreItem.SETTINGS),
            manageRows(showDuplicate = true)
        )
        assertEquals(
            listOf(ExploreItem.QUICK_ORGANIZE, ExploreItem.RECYCLE_BIN, ExploreItem.SETTINGS),
            manageRows(showDuplicate = false)
        )
    }
}
