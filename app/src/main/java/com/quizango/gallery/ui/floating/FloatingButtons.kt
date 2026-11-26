package com.quizango.gallery.ui.floating

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.Build
import android.provider.Settings
import androidx.core.content.ContextCompat
import androidx.core.content.edit
import com.quizango.gallery.MainActivity

/**
 * bo/d: the floating button switch and its state. 2.7.2 keeps it in SharedPreferences "assistive_touch_prefs" (same keys
 * here) because the overlay reads it synchronously from the service, where DataStore flows would not fit.
 */
object FloatingButtons {
    const val KEY_ENABLED = "assistive_touch_enabled"
    const val KEY_HINT_SHOWED = "assistive_touch_hint_showed"
    const val KEY_HOME_DIALOG_SESSION = "home_dialog_shown_session"
    const val KEY_INTRO_REPLAYED = "sidebar_intro_replayed"
    const val KEY_INTRO_SHOWN_AT = "sidebar_intro_shown_at"
    const val KEY_NUDGE_COUNT = "sidebar_nudge_count"
    const val KEY_NUDGE_DAY = "sidebar_nudge_day"
    const val KEY_SIDEBAR_ACTIONS = "sidebar_actions"
    const val KEY_HANDLE_Y = "sidebar_handle_y"
    const val KEY_ON_LEFT = "sidebar_on_left"
    const val DAY_MS = 86_400_000L

    /** MainActivity extra: a route the sidebar opens with Home under it (2.7.2 FLOATING_TARGET_INTENT through HomeActivity). */
    const val EXTRA_ROUTE = "floating_route"

    fun routeIntent(context: Context, route: String): Intent = Intent(context, MainActivity::class.java)
        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
        .putExtra(EXTRA_ROUTE, route)

    fun prefs(context: Context): SharedPreferences =
        context.applicationContext.getSharedPreferences("assistive_touch_prefs", Context.MODE_PRIVATE)

    fun available(): Boolean = floatingAvailable(Build.VERSION.SDK_INT, Build.BRAND, Build.MODEL)

    /** bo/d.HISPj7KHQ7: switched on and still allowed to draw over other apps. */
    fun isOn(context: Context): Boolean = prefs(context).getBoolean(KEY_ENABLED, false) && Settings.canDrawOverlays(context)

    /** bo/d.eyd3OXAZgV: called once the overlay permission is granted. */
    fun enable(context: Context) {
        prefs(context).edit { putBoolean(KEY_ENABLED, true) }
        start(context)
    }

    /** FloatingSettingActivity.o000oOoO (Always OFF). */
    fun disable(context: Context) {
        prefs(context).edit { putBoolean(KEY_ENABLED, false) }
        context.stopService(Intent(context, FloatingButtonService::class.java))
    }

    /** bo/d.OooO00o: start the service when on and not running yet; when off, make sure it is stopped. */
    fun start(context: Context): Boolean {
        val intent = Intent(context, FloatingButtonService::class.java)
        if (!isOn(context)) {
            context.stopService(intent)
            return false
        }
        if (FloatingButtonService.running) return true
        // Android 12+ refuses a foreground-service start from the background; callers are Home, Settings or boot
        return runCatching { ContextCompat.startForegroundService(context, intent) }.isSuccess
    }

    fun sidebarTools(context: Context): List<FloatingTool> = parseSidebarTools(prefs(context).getString(KEY_SIDEBAR_ACTIONS, null))

    fun saveSidebarTools(context: Context, tools: List<FloatingTool>) =
        prefs(context).edit { putString(KEY_SIDEBAR_ACTIONS, tools.toSidebarCsv()) }

    /** Whole days since the first intro was shown, or null before it ever was (bo/d.Wja3o2vx62). */
    fun daysSinceIntro(context: Context, now: Long = System.currentTimeMillis()): Long? {
        val at = prefs(context).getLong(KEY_INTRO_SHOWN_AT, 0L)
        return if (at == 0L) null else (now - at).coerceAtLeast(0) / DAY_MS
    }

    /** bo/d.SJowARcXwM: the first show time is kept. */
    fun markIntroShown(context: Context) {
        val p = prefs(context)
        if (p.getLong(KEY_INTRO_SHOWN_AT, 0L) == 0L) p.edit { putLong(KEY_INTRO_SHOWN_AT, System.currentTimeMillis()) }
    }
}
