package com.quizango.gallery.ui.navigation

import com.quizango.gallery.notification.QuickAction
import com.quizango.gallery.ui.discovery.placeRoute

/** Where the app lands when opened from outside (deep link, notification, shortcut); Home always sits under it. */
internal sealed interface Entry {
    data class Open(val route: String) : Entry
    data object Camera : Entry
    data class Browse(val url: String) : Entry
}

/** DeepLinkActivity intent filter: https://api.bestgalleryapp.com/Activity?activity=<name>&key=<value>. */
internal const val DEEP_LINK_HOST = "api.bestgalleryapp.com"
internal const val DEEP_LINK_PATH_PREFIX = "/Activity"

/** swappable_notification extras (NotificationWorker): the action key decides the screen. */
internal const val EXTRA_NOTIFICATION_ACTION = "key_notification_action"
internal const val EXTRA_NOTIFICATION_INTENT = "key_notification_intent"
internal const val EXTRA_FROM_SWAPPABLE_NOTIFICATION = "is_from_swappable_notification"

/** res/xml/shortcuts.xml extra. */
internal const val EXTRA_SHORTCUT_ROUTE = "route"

internal fun isDeepLink(scheme: String?, host: String?, path: String?): Boolean =
    scheme == "https" && host == DEEP_LINK_HOST && path.orEmpty().startsWith(DEEP_LINK_PATH_PREFIX)

/**
 * g1/b.DxDJysLV5r: the `activity` name (exact, case-sensitive) picks the screen; unknown or empty opens Home (null).
 * ExternalURL opens `key` in the browser, and only an http(s) link: everything here comes from outside the app.
 */
internal fun deepLinkEntry(activity: String?, key: String?): Entry? {
    if (activity == "ExternalURL") {
        val url = key?.trim().orEmpty()
        val scheme = url.substringBefore("://", "").lowercase()
        return if ((scheme == "https" || scheme == "http") && url.length > scheme.length + 3) Entry.Browse(url) else null
    }
    val route = when (activity) {
        "PayWallActivity" -> Screen.Pro.route
        "DuplicatePhotoActivity" -> Screen.DuplicateWaiting.route
        "RecycleActivity" -> Screen.RecycleBin.route
        "FavoriteActivity" -> Screen.Favorites.route
        "BurstPhotoActivity" -> Screen.BurstPhotos.route
        "ThemeActivity" -> Screen.Theme.route
        "SearchActivity" -> Screen.Search.route
        "AppLanguageActivity" -> Screen.Language.route
        "AllAlbumActivity" -> Screen.AllAlbums.route
        "MapActivity" -> placeRoute()
        "VaultLockActivity" -> Screen.VaultLock.route
        "MomentsActivity" -> Screen.Moments.route
        // 2.7.2 picks a photo on Home first, then opens the tool; our editor opens on its own picker
        "PhotoEditActivity", "RemoveBgActivity", "ColorSplashActivity", "BlurBgActivity" -> Screen.Editor.createRoute()
        "AnimatedMediaListActivity" -> Screen.AnimatedMedia.route
        "InternalSelectMediaActivity" -> Screen.CollageMaker.route
        "OrganizerActivity" -> Screen.QuickOrganizer.route
        else -> null // HomeActivity, RateUs, unknown names
    }
    return route?.let(Entry::Open)
}

/** HA.o000O0oo for the Quick Actions keys; Cleaner opens the scan until junk was cleaned today. */
internal fun notificationEntry(action: String?, junkCleanedToday: Boolean): Entry? = when (action) {
    QuickAction.CAMERA.key -> Entry.Camera
    QuickAction.FAVORITE.key -> Entry.Open(Screen.Favorites.route)
    QuickAction.CLEANER.key -> Entry.Open(if (junkCleanedToday) Screen.Cleaner.route else Screen.RemoveJunk.route)
    QuickAction.MOMENTS.key -> Entry.Open(Screen.Moments.route)
    QuickAction.DUPLICATE.key -> Entry.Open(Screen.DuplicateWaiting.route)
    QuickAction.RECYCLE.key -> Entry.Open(Screen.RecycleBin.route)
    else -> null
}

/** Launcher shortcuts (2.7.2 targets those activities directly). */
internal fun shortcutEntry(value: String?): Entry? = when (value) {
    "favorites" -> Screen.Favorites.route
    "recycle" -> Screen.RecycleBin.route
    "vault" -> Screen.VaultLock.route
    "duplicates" -> Screen.DuplicateWaiting.route
    else -> null
}?.let(Entry::Open)
