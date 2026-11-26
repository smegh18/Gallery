package com.quizango.gallery

import com.quizango.gallery.ui.discovery.placeRoute
import com.quizango.gallery.ui.floating.DEFAULT_SIDEBAR
import com.quizango.gallery.ui.floating.FloatingTool
import com.quizango.gallery.ui.floating.MAX_SIDEBAR_TOOLS
import com.quizango.gallery.ui.floating.addSidebarTool
import com.quizango.gallery.ui.floating.clampHandleY
import com.quizango.gallery.ui.floating.floatingAvailable
import com.quizango.gallery.ui.floating.homeOffer
import com.quizango.gallery.ui.floating.isSidebarRoute
import com.quizango.gallery.ui.floating.nudges
import com.quizango.gallery.ui.floating.opensOnRelease
import com.quizango.gallery.ui.floating.parseSidebarTools
import com.quizango.gallery.ui.floating.removeSidebarTool
import com.quizango.gallery.ui.floating.replaysIntro
import com.quizango.gallery.ui.floating.route
import com.quizango.gallery.ui.floating.snapsLeft
import com.quizango.gallery.ui.floating.toSidebarCsv
import com.quizango.gallery.ui.navigation.Screen
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class FloatingSidebarTest {

    @Test
    fun defaultSidebarIsTheFive2_7_2Tools() {
        assertEquals(
            listOf(FloatingTool.GALLERY, FloatingTool.VAULT, FloatingTool.FAVORITE, FloatingTool.MOMENTS, FloatingTool.RECYCLE_BIN),
            DEFAULT_SIDEBAR
        )
        assertEquals(DEFAULT_SIDEBAR, parseSidebarTools(null))
        assertEquals(12, FloatingTool.entries.size)
    }

    @Test
    fun savedToolsDropUnknownAndRepeatedKeysAndKeepAtMostSix() {
        assertEquals(listOf(FloatingTool.SEARCH, FloatingTool.CAMERA), parseSidebarTools("search, camera,nope,search"))
        assertEquals(DEFAULT_SIDEBAR, parseSidebarTools(""))
        assertEquals(DEFAULT_SIDEBAR, parseSidebarTools("nothing,known"))
        val all = FloatingTool.entries.toSidebarCsv()
        assertEquals(FloatingTool.entries.take(MAX_SIDEBAR_TOOLS), parseSidebarTools(all))
        val saved = listOf(FloatingTool.CLEANER, FloatingTool.GALLERY, FloatingTool.PLACES)
        assertEquals(saved, parseSidebarTools(saved.toSidebarCsv()))
    }

    @Test
    fun removingTheLastToolIsRefused() {
        assertNull(removeSidebarTool(listOf(FloatingTool.VAULT), FloatingTool.VAULT))
        assertEquals(listOf(FloatingTool.GALLERY), removeSidebarTool(listOf(FloatingTool.GALLERY, FloatingTool.VAULT), FloatingTool.VAULT))
    }

    @Test
    fun addingGoesLastUpToSixAndIgnoresToolsAlreadyThere() {
        val five = DEFAULT_SIDEBAR
        val six = addSidebarTool(five, FloatingTool.SEARCH)!!
        assertEquals(five + FloatingTool.SEARCH, six)
        assertNull(addSidebarTool(six, FloatingTool.CAMERA))
        assertEquals(six, addSidebarTool(six, FloatingTool.SEARCH))
        assertEquals(five, addSidebarTool(five, FloatingTool.VAULT))
    }

    @Test
    fun everyToolOpensItsReferenceTarget() {
        assertNull(FloatingTool.GALLERY.route(false))
        assertNull(FloatingTool.CAMERA.route(false))
        assertEquals(Screen.VaultLock.route, FloatingTool.VAULT.route(false))
        assertEquals(Screen.Favorites.route, FloatingTool.FAVORITE.route(false))
        assertEquals(Screen.Moments.route, FloatingTool.MOMENTS.route(false))
        assertEquals(Screen.RecycleBin.route, FloatingTool.RECYCLE_BIN.route(false))
        assertEquals(Screen.Search.route, FloatingTool.SEARCH.route(false))
        assertEquals(Screen.DuplicateWaiting.route, FloatingTool.DUPLICATE.route(false))
        assertEquals(Screen.QuickOrganizer.route, FloatingTool.ORGANIZE.route(false))
        assertEquals(placeRoute(), FloatingTool.PLACES.route(false))
        assertEquals(Screen.RemoveJunk.route, FloatingTool.CLEANER.route(false))
        assertEquals(Screen.Cleaner.route, FloatingTool.CLEANER.route(true))
        assertEquals(Screen.Settings.route, FloatingTool.SETTINGS.route(false))
    }

    @Test
    fun homeOffersTheButtonOnLaunches1To10AndForcefullyOn4And10() {
        assertEquals(false, homeOffer(1, -1, isOn = false, available = true))
        assertEquals(true, homeOffer(4, 1, isOn = false, available = true))
        assertEquals(false, homeOffer(7, 4, isOn = false, available = true))
        assertEquals(true, homeOffer(10, 7, isOn = false, available = true))
        assertNull(homeOffer(2, -1, isOn = false, available = true))
        assertNull(homeOffer(4, 4, isOn = false, available = true)) // once per launch
        assertNull(homeOffer(4, -1, isOn = true, available = true))
        assertNull(homeOffer(4, -1, isOn = false, available = false))
    }

    @Test
    fun featureNeedsAndroid9AndASupportedDevice() {
        assertFalse(floatingAvailable(27, "google", "Pixel 7"))
        assertTrue(floatingAvailable(28, "google", "Pixel 7"))
        assertFalse(floatingAvailable(34, "Redmi", "23129RN51X"))
        assertFalse(floatingAvailable(34, "motorola", "moto e13"))
    }

    @Test
    fun droppedHandleSnapsToTheSideItsCentreIsOn() {
        assertTrue(snapsLeft(x = 400, width = 120, screenWidth = 1080))
        assertFalse(snapsLeft(x = 500, width = 120, screenWidth = 1080))
    }

    @Test
    fun handleHeightIsSavedOrCentredAndStaysBetweenTheBars() {
        assertEquals((80 + 2200 - 300) / 2, clampHandleY(-1, top = 80, bottom = 2200, height = 300))
        assertEquals(80, clampHandleY(10, top = 80, bottom = 2200, height = 300))
        assertEquals(1900, clampHandleY(2100, top = 80, bottom = 2200, height = 300))
        assertEquals(1000, clampHandleY(1000, top = 80, bottom = 2200, height = 300))
        assertEquals(80, clampHandleY(500, top = 80, bottom = 300, height = 300)) // screen shorter than the handle
    }

    @Test
    fun releaseOpensOnAFastInwardFlingOrWhenMoreThanAThirdShows() {
        assertTrue(opensOnRelease(velocity = 900f, openFraction = 0.1f))
        assertFalse(opensOnRelease(velocity = -900f, openFraction = 0.9f))
        assertTrue(opensOnRelease(velocity = 200f, openFraction = 0.36f))
        assertFalse(opensOnRelease(velocity = 799f, openFraction = 0.35f))
    }

    @Test
    fun introReplaysOnceAfterAWeekAndNudgesAtMostThreeTimesOnceADay() {
        assertFalse(replaysIntro(null, replayed = false))
        assertFalse(replaysIntro(6, replayed = false))
        assertTrue(replaysIntro(7, replayed = false))
        assertFalse(replaysIntro(30, replayed = true))
        assertFalse(nudges(0, count = 0, lastDay = -1, today = 100))
        assertTrue(nudges(1, count = 2, lastDay = 99, today = 100))
        assertFalse(nudges(1, count = 3, lastDay = 99, today = 100))
        assertFalse(nudges(5, count = 1, lastDay = 100, today = 100))
    }

    @Test
    fun theFloatingRouteExtraOnlyOpensRoutesASidebarToolOpens() {
        assertTrue(isSidebarRoute(Screen.VaultLock.route))
        assertTrue(isSidebarRoute(Screen.RemoveJunk.route))
        assertTrue(isSidebarRoute(Screen.Cleaner.route))
        // MainActivity is exported: anything else, like the vault grid past its lock, is refused
        assertFalse(isSidebarRoute(Screen.VaultMedia.route))
        assertFalse(isSidebarRoute(Screen.VaultSetting.route))
        assertFalse(isSidebarRoute(""))
    }
}
