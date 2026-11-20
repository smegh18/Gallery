package com.quizango.gallery

import com.quizango.gallery.ui.home.ExploreItem
import com.quizango.gallery.ui.home.exploreRoute
import com.quizango.gallery.ui.navigation.Screen
import org.junit.Assert.assertEquals
import org.junit.Test

class ExploreTabTest {

    private fun route(item: ExploreItem, junk: Boolean = false) = exploreRoute(item, junk, "Place")

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
        assertEquals(Screen.AlbumDetail.createRoute("filter:place", "Place"), route(ExploreItem.PLACE))
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
}
