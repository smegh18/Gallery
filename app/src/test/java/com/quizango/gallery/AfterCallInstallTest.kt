package com.quizango.gallery

import android.content.Intent
import com.quizango.gallery.domain.model.Album
import com.quizango.gallery.domain.model.MediaItem
import com.quizango.gallery.ui.gs.CallEndAction
import com.quizango.gallery.ui.gs.IuThrottle
import com.quizango.gallery.ui.gs.PackageEvent
import com.quizango.gallery.ui.gs.STATE_IDLE
import com.quizango.gallery.ui.gs.STATE_OFFHOOK
import com.quizango.gallery.ui.gs.STATE_RINGING
import com.quizango.gallery.ui.gs.callDurationText
import com.quizango.gallery.ui.gs.callEndAction
import com.quizango.gallery.ui.gs.callTimes
import com.quizango.gallery.ui.gs.gsAlbumRoute
import com.quizango.gallery.ui.gs.gsAlbums
import com.quizango.gallery.ui.gs.GS_ROUTE_PERMISSION
import com.quizango.gallery.ui.gs.isGsRoute
import com.quizango.gallery.ui.gs.packageEvent
import com.quizango.gallery.ui.iu.iuTexts
import com.quizango.gallery.ui.navigation.Screen
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class AfterCallInstallTest {

    @Test
    fun callEndShowsOrWaitsOrNotifies() {
        // Background without the overlay permission: only the "Quick Info disabled" notification
        assertEquals(CallEndAction.OVERLAY_DISABLED_NOTICE, callEndAction(canDrawOverlays = false, appInForeground = false, keyguardLocked = false))
        assertEquals(CallEndAction.OVERLAY_DISABLED_NOTICE, callEndAction(canDrawOverlays = false, appInForeground = false, keyguardLocked = true))
        assertEquals(CallEndAction.WAIT_FOR_UNLOCK, callEndAction(canDrawOverlays = true, appInForeground = false, keyguardLocked = true))
        assertEquals(CallEndAction.SHOW, callEndAction(canDrawOverlays = true, appInForeground = false, keyguardLocked = false))
        // The app on screen can show it without the overlay permission
        assertEquals(CallEndAction.SHOW, callEndAction(canDrawOverlays = false, appInForeground = true, keyguardLocked = false))
    }

    @Test
    fun callTimesFollowPhoneStates() {
        assertEquals(0L to 0L, callTimes(STATE_RINGING, 500L, 900L))
        assertEquals(900L to 0L, callTimes(STATE_OFFHOOK, 0L, 900L))
        // 2.7.2 keeps an earlier start on OFFHOOK; only RINGING clears it
        assertEquals(500L to 0L, callTimes(STATE_OFFHOOK, 500L, 900L))
        assertEquals(500L to 900L, callTimes(STATE_IDLE, 500L, 900L))
        assertNull(callTimes(null, 500L, 900L))
    }

    @Test
    fun callDurationWrapsAtTheHour() {
        assertEquals("00:00", callDurationText(0L, 90_000L)) // missed or rejected: never answered
        assertEquals("00:00", callDurationText(5_000L, 1_000L))
        assertEquals("01:15", callDurationText(1_000L, 76_000L))
        assertEquals("02:05", callDurationText(1L, 3_725_001L)) // 1 h 2 min 5 s shows 02:05 as in 2.7.2
    }

    @Test
    fun packageEventsKeepOnlyTheFinalBroadcast() {
        assertEquals(PackageEvent.INSTALL, packageEvent(Intent.ACTION_PACKAGE_ADDED, replacing = false))
        assertEquals(PackageEvent.UNINSTALL, packageEvent(Intent.ACTION_PACKAGE_REMOVED, replacing = false))
        assertEquals(PackageEvent.UPDATE, packageEvent(Intent.ACTION_PACKAGE_REPLACED, replacing = true))
        // The REMOVED + ADDED pair of an update is skipped
        assertNull(packageEvent(Intent.ACTION_PACKAGE_REMOVED, replacing = true))
        assertNull(packageEvent(Intent.ACTION_PACKAGE_ADDED, replacing = true))
        assertNull(packageEvent(Intent.ACTION_PACKAGE_FULLY_REMOVED, replacing = false))
        assertNull(packageEvent(null, replacing = false))
    }

    @Test
    fun throttleDropsDuplicatesAndSpacesUpdates() {
        val throttle = IuThrottle()
        assertFalse(throttle.isDuplicate("add", "a.b", 10_000L))
        assertTrue(throttle.isDuplicate("add", "a.b", 14_999L))
        assertFalse(throttle.isDuplicate("add", "a.b", 15_000L))
        assertFalse(throttle.isDuplicate("add", "c.d", 15_001L))

        val minute = 60_000L
        assertFalse(throttle.takeUpdateSlot(29 * minute)) // 2.7.2 counts from boot: none in the first 30 min
        assertTrue(throttle.takeUpdateSlot(30 * minute))
        assertFalse(throttle.takeUpdateSlot(45 * minute))
        assertTrue(throttle.takeUpdateSlot(60 * minute))
    }

    @Test
    fun installScreenTextsPerEvent() {
        assertEquals(R.string.app_scan_open, iuTexts(PackageEvent.INSTALL).button)
        assertEquals(R.string.app_scan_open, iuTexts(PackageEvent.UPDATE).button)
        assertEquals(R.string.app_scan_done, iuTexts(PackageEvent.UNINSTALL).button)
        assertEquals(R.string.gs2_updating_title, iuTexts(PackageEvent.UPDATE).title)
        assertEquals(R.string.gs2_setting_things_up, iuTexts(PackageEvent.INSTALL).progressTitle)
        assertEquals(R.string.gs2_your_app_uninstalled_successfully, iuTexts(PackageEvent.UNINSTALL).doneSubtitle)
    }

    @Test
    fun quickInfoAlbumsShowOnlyFilledSmartAlbums() {
        fun album(id: String, count: Int, cover: String) = Album(id, "", id, cover, count)
        val albums = listOf(
            album("filter:all_photos", 4, "/p/1.jpg"),
            album("filter:videos", 0, ""),
            album("filter:camera", 2, "/DCIM/Camera/2.jpg"),
            album("filter:screenshots", 1, "/Pictures/Screenshots/3.png"),
            album("/storage/Download", 9, "/storage/Download/4.jpg")
        )
        val favorite = MediaItem(id = 0, filePath = "/p/1.jpg", fileName = "1.jpg", fileSize = 1, folderName = "p", folderPath = "/p", dateModified = 1, dateTaken = 1)

        val cards = gsAlbums(albums, listOf(favorite))
        assertEquals(listOf("filter:all_photos", "filter:camera", "filter:screenshots", Screen.Favorites.route), cards.map { it.id })
        assertEquals(File("/p/1.jpg"), cards.last().cover)
        assertEquals(listOf("filter:all_photos", "filter:camera", "filter:screenshots"), gsAlbums(albums, emptyList()).map { it.id })

        assertEquals(Screen.Home.route, gsAlbumRoute(cards[0], "All Photos"))
        assertEquals(Screen.AlbumDetail.createRoute("filter:camera", "Camera"), gsAlbumRoute(cards[1], "Camera"))
        assertEquals(Screen.Favorites.route, gsAlbumRoute(cards[3], "Favorite"))
    }

    @Test
    fun theGsRouteExtraOnlyOpensRoutesTheAppItselfSends() {
        listOf(
            GS_ROUTE_PERMISSION, Screen.Home.route, Screen.Settings.route, Screen.VaultLock.route,
            Screen.Favorites.route, Screen.Moments.route, Screen.CollageMaker.route,
            Screen.AlbumDetail.createRoute("filter:videos", "Videos"),
            Screen.AlbumDetail.createRoute("filter:camera", "Camera"),
            Screen.AlbumDetail.createRoute("filter:screenshots", "Screen / shot?")
        ).forEach { assertTrue(it, isGsRoute(it)) }
        // MainActivity is exported: vault content, vault settings, recovery and unknown routes are refused
        listOf(
            Screen.VaultMedia.route, Screen.VaultSetting.route, "vault/recovery_question", "vault/settings",
            Screen.VaultLockFor.createRoute(1, true), "x", "",
            Screen.AlbumDetail.createRoute("123", "Private"), "album/filter%3Avideos/", "album/filter%3Avideos/a/b"
        ).forEach { assertFalse(it, isGsRoute(it)) }
    }
}
