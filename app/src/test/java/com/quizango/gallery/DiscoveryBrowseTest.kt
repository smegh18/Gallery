package com.quizango.gallery

import com.quizango.gallery.data.local.MomentScanner
import com.quizango.gallery.ui.browse.MediaAction
import com.quizango.gallery.ui.browse.mediaListActions
import com.quizango.gallery.ui.discovery.ANIMATED_SOURCE
import com.quizango.gallery.ui.discovery.MomentsState
import com.quizango.gallery.ui.discovery.momentRoute
import com.quizango.gallery.ui.discovery.momentsState
import com.quizango.gallery.ui.discovery.showsScanHeader
import com.quizango.gallery.ui.navigation.Screen
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DiscoveryBrowseTest {

    @Test
    fun momentsStates() {
        // photos or groups not built yet: wait, never "Oops!"
        assertEquals(MomentsState.WAIT, momentsState(groupCount = null, settled = false, scanning = false))
        // first scan, nothing found yet
        assertEquals(MomentsState.WAIT, momentsState(groupCount = 0, settled = true, scanning = true))
        // groups built from stale labels while the scan already finished: still wait
        assertEquals(MomentsState.WAIT, momentsState(groupCount = 0, settled = false, scanning = false))
        // active scan with groups shows cards (header on top)
        assertEquals(MomentsState.CONTENT, momentsState(groupCount = 2, settled = false, scanning = true))
        // scan done, none
        assertEquals(MomentsState.EMPTY, momentsState(groupCount = 0, settled = true, scanning = false))
        assertEquals(MomentsState.CONTENT, momentsState(groupCount = 5, settled = true, scanning = false))
    }

    @Test
    fun scanHeaderNeedsACount() {
        assertFalse(showsScanHeader(null))
        assertFalse(showsScanHeader(MomentScanner.Progress(0, 0))) // labels still loading
        assertFalse(showsScanHeader(MomentScanner.Progress(0, 40)))
        assertTrue(showsScanHeader(MomentScanner.Progress(1, 40)))
    }

    @Test
    fun momentCardOpensItsLabelledList() {
        assertEquals(Screen.AlbumDetail.createRoute("filter:moment:Food & drink", "Food & drink"), momentRoute("Food & drink"))
        assertTrue(momentRoute("a/b").startsWith("album/filter%3Amoment%3Aa%2Fb/"))
    }

    @Test
    fun animatedUsesTheSixMediaListSlotsAndItsOwnSource() {
        assertEquals(
            listOf(MediaAction.SHARE, MediaAction.FAVORITE, MediaAction.ADD_TO, MediaAction.PRIVATE, MediaAction.DELETE, MediaAction.CREATE_ALBUM),
            mediaListActions(allFavorite = false)
        )
        assertEquals("filter:animated", ANIMATED_SOURCE)
    }
}
