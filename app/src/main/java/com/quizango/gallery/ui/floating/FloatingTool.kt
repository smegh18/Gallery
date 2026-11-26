package com.quizango.gallery.ui.floating

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import com.quizango.gallery.R
import com.quizango.gallery.ui.discovery.placeRoute
import com.quizango.gallery.ui.navigation.Screen
import kotlin.math.abs

/**
 * bo/w: the twelve quick tools in 2.7.2 order. [key] is what sidebar_actions stores; [tileColor] is the tile
 * behind the white icon (hard-coded ARGB in 2.7.2, no colour resource); Gallery shows the app logo instead.
 */
enum class FloatingTool(val key: String, @DrawableRes val icon: Int, @StringRes val label: Int, val tileColor: Long) {
    GALLERY("gallery", R.drawable.ic_launcher_gallery_foreground, R.string.gallery, 0),
    VAULT("vault", R.drawable.ic_at_lock, R.string.private_vault, 0xFF7B61FF),
    FAVORITE("favorite", R.drawable.ic_at_fav, R.string.favorite, 0xFFFF4D6A),
    MOMENTS("moments", R.drawable.ic_at_story, R.string.moment, 0xFFFF9500),
    RECYCLE_BIN("recycle_bin", R.drawable.ic_at_recycle, R.string.recycle_bin, 0xFF34C759),
    SEARCH("search", R.drawable.home_shell_ic_search, R.string.search, 0xFF0A84FF),
    CAMERA("camera", R.drawable.home_shell_ic_camera, R.string.camera, 0xFF5E5CE6),
    DUPLICATE("duplicate", R.drawable.explore_tab_ic_duplicate, R.string.duplicate_finder, 0xFF00B8A9),
    ORGANIZE("organize", R.drawable.explore_tab_ic_organize, R.string.organizer_explore_title, 0xFFAF52DE),
    PLACES("places", R.drawable.explore_tab_ic_place, R.string.places, 0xFF30B0C7),
    CLEANER("cleaner", R.drawable.explore_tab_ic_cleaner, R.string.str_home_cleaner, 0xFFFF6B35),
    SETTINGS("settings", R.drawable.home_shell_ic_settings, R.string.settings, 0xFF8E8E93)
}

/** bo/w DEFAULT_SIDEBAR and MAX_SIDEBAR_ACTIONS. */
val DEFAULT_SIDEBAR = listOf(FloatingTool.GALLERY, FloatingTool.VAULT, FloatingTool.FAVORITE, FloatingTool.MOMENTS, FloatingTool.RECYCLE_BIN)
const val MAX_SIDEBAR_TOOLS = 6

/** bo/d.DxDJysLV5r: unknown keys dropped, repeats removed, at most six; nothing saved, or nothing left, gives the default set. */
fun parseSidebarTools(csv: String?): List<FloatingTool> {
    if (csv == null) return DEFAULT_SIDEBAR
    val tools = csv.split(',').mapNotNull { key -> FloatingTool.entries.firstOrNull { it.key == key.trim() } }
    return tools.distinct().take(MAX_SIDEBAR_TOOLS).ifEmpty { DEFAULT_SIDEBAR }
}

fun List<FloatingTool>.toSidebarCsv(): String = joinToString(",") { it.key }

/** bo/s edit mode, sidebar item: null when refused, the sidebar keeps at least one tool ("Keep at least one tool"). */
fun removeSidebarTool(tools: List<FloatingTool>, tool: FloatingTool): List<FloatingTool>? =
    if (tools.size <= 1) null else tools - tool

/** bo/s edit mode, All tools item: an added tool goes last; null when six are already there ("You can add up to 6 tools"). */
fun addSidebarTool(tools: List<FloatingTool>, tool: FloatingTool): List<FloatingTool>? = when {
    tool in tools -> tools
    tools.size >= MAX_SIDEBAR_TOOLS -> null
    else -> tools + tool
}

/** Where a tool opens in the app (bo/s targets); Gallery restarts the app and Camera opens the camera app instead. */
fun FloatingTool.route(junkCleanedToday: Boolean): String? = when (this) {
    FloatingTool.GALLERY, FloatingTool.CAMERA -> null
    FloatingTool.VAULT -> Screen.VaultLock.route
    FloatingTool.FAVORITE -> Screen.Favorites.route
    FloatingTool.MOMENTS -> Screen.Moments.route
    FloatingTool.RECYCLE_BIN -> Screen.RecycleBin.route
    FloatingTool.SEARCH -> Screen.Search.route
    FloatingTool.DUPLICATE -> Screen.DuplicateWaiting.route
    FloatingTool.ORGANIZE -> Screen.QuickOrganizer.route
    FloatingTool.PLACES -> placeRoute()
    FloatingTool.CLEANER -> if (junkCleanedToday) Screen.Cleaner.route else Screen.RemoveJunk.route
    FloatingTool.SETTINGS -> Screen.Settings.route
}

/** MainActivity is exported: its floating_route extra may only name a route some tool opens, never e.g. vault_media. */
fun isSidebarRoute(route: String): Boolean =
    FloatingTool.entries.any { route == it.route(junkCleanedToday = true) || route == it.route(junkCleanedToday = false) }

/** default_remote_config floating_button_controller.home_dialog_sessions: launch number -> forcefully. */
val HOME_OFFER_SESSIONS = mapOf(1 to false, 4 to true, 7 to false, 10 to true)

/** HomeActivity.o0ooOOo: null when Home does not offer the button this launch, else whether the offer is forceful. */
fun homeOffer(session: Int, shownSession: Int, isOn: Boolean, available: Boolean): Boolean? =
    if (isOn || !available || shownSession == session) null else HOME_OFFER_SESSIONS[session]

/** common_data.overlay_unsupported_devices (default remote config), as lower-case brand_model. */
val OVERLAY_UNSUPPORTED = setOf("poco_2312bpc51h", "redmi_23129rn51h", "redmi_23129rn51x", "poco_2302epcc4i", "motorola_moto e13")

/** The feature exists from Android 9 on, except on the devices 2.7.2 lists as unable to draw overlays. */
fun floatingAvailable(sdkInt: Int, brand: String, model: String): Boolean =
    sdkInt >= 28 && "${brand}_$model".lowercase() !in OVERLAY_UNSUPPORTED

/** bo/l OooOOO: a dropped handle snaps to the side its centre is on. */
fun snapsLeft(x: Int, width: Int, screenWidth: Int): Boolean = x + width / 2 < screenWidth / 2

/** bo/l OooOO0: the saved y, or centred between the bars, kept between the status bar and the navigation bar. */
fun clampHandleY(saved: Int, top: Int, bottom: Int, height: Int): Int {
    val y = if (saved < 0) (top + bottom - height) / 2 else saved
    return y.coerceIn(top, maxOf(top, bottom - height))
}

/** bo/s OooOO0O: a fling over 800 px/s goes its way (positive opens); a slower release opens when over 35 % shows. */
fun opensOnRelease(velocity: Float, openFraction: Float): Boolean =
    if (abs(velocity) > 800f) velocity > 0f else openFraction > 0.35f

/** bo/l OooOOo: the intro runs again once, a week after the first (2.7.2 never writes sidebar_opened, so it always does). */
fun replaysIntro(daysSinceIntro: Long?, replayed: Boolean): Boolean = !replayed && daysSinceIntro != null && daysSinceIntro >= 7

/** bo/l: from the day after the intro the handle wiggles on show, at most three times and once a day. */
fun nudges(daysSinceIntro: Long?, count: Int, lastDay: Long, today: Long): Boolean =
    daysSinceIntro != null && daysSinceIntro >= 1 && count < 3 && lastDay != today
