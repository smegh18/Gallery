package com.quizango.gallery

import com.quizango.gallery.domain.model.MediaItem
import com.quizango.gallery.domain.model.MediaType
import com.quizango.gallery.notification.QuickAction
import com.quizango.gallery.ui.external.PickKind
import com.quizango.gallery.ui.external.pickKind
import com.quizango.gallery.ui.external.pickable
import com.quizango.gallery.ui.navigation.Entry
import com.quizango.gallery.ui.navigation.Screen
import com.quizango.gallery.ui.navigation.deepLinkEntry
import com.quizango.gallery.ui.navigation.isDeepLink
import com.quizango.gallery.ui.navigation.notificationEntry
import com.quizango.gallery.ui.navigation.shortcutEntry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ExternalEntryTest {

    @Test
    fun deepLinkMatchesOnlyTheReferenceFilter() {
        assertTrue(isDeepLink("https", "api.bestgalleryapp.com", "/Activity"))
        assertTrue(isDeepLink("https", "api.bestgalleryapp.com", "/Activity/open"))
        assertFalse(isDeepLink("http", "api.bestgalleryapp.com", "/Activity"))
        assertFalse(isDeepLink("https", "evil.example.com", "/Activity"))
        assertFalse(isDeepLink("https", "api.bestgalleryapp.com", "/other"))
        assertFalse(isDeepLink(null, null, null))
    }

    @Test
    fun deepLinkNamesPickTheirScreen() {
        assertEquals(Entry.Open(Screen.Pro.route), deepLinkEntry("PayWallActivity", ""))
        assertEquals(Entry.Open(Screen.RecycleBin.route), deepLinkEntry("RecycleActivity", null))
        assertEquals(Entry.Open(Screen.Favorites.route), deepLinkEntry("FavoriteActivity", null))
        assertEquals(Entry.Open(Screen.DuplicateWaiting.route), deepLinkEntry("DuplicatePhotoActivity", null))
        assertEquals(Entry.Open(Screen.VaultLock.route), deepLinkEntry("VaultLockActivity", null))
        assertEquals(Entry.Open(Screen.Theme.route), deepLinkEntry("ThemeActivity", null))
        assertEquals(Entry.Open(Screen.Editor.createRoute()), deepLinkEntry("RemoveBgActivity", null))
        // Home, the rate dialog, wrong case and unknown names all stay on Home
        listOf("HomeActivity", "RateUs", "recycleactivity", "Nope", "", null).forEach { assertNull(it, deepLinkEntry(it, null)) }
    }

    @Test
    fun externalUrlOpensOnlyWebLinks() {
        assertEquals(Entry.Browse("https://example.com/a"), deepLinkEntry("ExternalURL", " https://example.com/a "))
        assertEquals(Entry.Browse("http://example.com"), deepLinkEntry("ExternalURL", "http://example.com"))
        listOf("javascript:alert(1)", "file:///sdcard/x", "intent://x#Intent;end", "https://", "", null)
            .forEach { assertNull(it, deepLinkEntry("ExternalURL", it)) }
    }

    @Test
    fun notificationActionsOpenTheirFeature() {
        assertEquals(Entry.Camera, notificationEntry("camera", false))
        assertEquals(Entry.Open(Screen.Favorites.route), notificationEntry("favorite", false))
        assertEquals(Entry.Open(Screen.RemoveJunk.route), notificationEntry("cleaner", junkCleanedToday = false))
        assertEquals(Entry.Open(Screen.Cleaner.route), notificationEntry("cleaner", junkCleanedToday = true))
        assertEquals(Entry.Open(Screen.Moments.route), notificationEntry("moments", false))
        assertEquals(Entry.Open(Screen.DuplicateWaiting.route), notificationEntry("duplicate", false))
        assertEquals(Entry.Open(Screen.RecycleBin.route), notificationEntry("recycle", false))
        assertNull(notificationEntry("Camera", false)) // keys, not the translated labels
        assertNull(notificationEntry(null, false))
    }

    @Test
    fun shortcutsOpenTheirScreen() {
        assertEquals(Entry.Open(Screen.Favorites.route), shortcutEntry("favorites"))
        assertEquals(Entry.Open(Screen.RecycleBin.route), shortcutEntry("recycle"))
        assertEquals(Entry.Open(Screen.VaultLock.route), shortcutEntry("vault"))
        assertEquals(Entry.Open(Screen.DuplicateWaiting.route), shortcutEntry("duplicates"))
        assertNull(shortcutEntry("settings"))
    }

    @Test
    fun quickActionsTakeKnownOptionsUpToThree() {
        assertEquals(listOf(QuickAction.CAMERA, QuickAction.FAVORITE, QuickAction.CLEANER), QuickAction.features(QuickAction.DEFAULT_OPTIONS))
        assertEquals(
            listOf(QuickAction.RECYCLE, QuickAction.MOMENTS, QuickAction.CAMERA),
            QuickAction.features(listOf("recycle", "editor", "moments", "camera", "duplicate"))
        )
        assertEquals(emptyList<QuickAction>(), QuickAction.features(emptyList()))
    }

    @Test
    fun pickKindFollowsTheCallersType() {
        assertEquals(PickKind.IMAGES, pickKind("image/*"))
        assertEquals(PickKind.IMAGES, pickKind("image/jpeg"))
        assertEquals(PickKind.IMAGES, pickKind("vnd.android.cursor.dir/image"))
        assertEquals(PickKind.VIDEOS, pickKind("video/*"))
        assertEquals(PickKind.VIDEOS, pickKind("vnd.android.cursor.dir/video"))
        assertEquals(PickKind.ALL, pickKind("*/*"))
        assertEquals(PickKind.ALL, pickKind(null))
    }

    @Test
    fun onlyMediaStoreRowsOfTheAskedKindArePickable() {
        fun item(id: Long, type: MediaType) = MediaItem(id, "/p/$id", "$id", 1, "p", "/p", dateModified = 1, dateTaken = 1, mediaType = type)
        assertTrue(pickable(item(1, MediaType.IMAGE), PickKind.IMAGES))
        assertTrue(pickable(item(2, MediaType.SCREENSHOT), PickKind.IMAGES))
        assertFalse(pickable(item(3, MediaType.VIDEO), PickKind.IMAGES))
        assertTrue(pickable(item(3, MediaType.VIDEO), PickKind.VIDEOS))
        assertTrue(pickable(item(4, MediaType.VIDEO), PickKind.ALL))
        assertFalse(pickable(item(0, MediaType.IMAGE), PickKind.ALL)) // no MediaStore id, no content URI to hand out
    }
}
