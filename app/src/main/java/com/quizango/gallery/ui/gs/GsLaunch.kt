package com.quizango.gallery.ui.gs

import android.Manifest
import android.app.ActivityManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.PixelFormat
import android.media.RingtoneManager
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.view.View
import android.view.WindowManager
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.quizango.gallery.MainActivity
import com.quizango.gallery.R
import com.quizango.gallery.ui.navigation.Screen
import com.quizango.gallery.ui.navigation.encodeRouteArg

/** MainActivity extra: a NavHost route to open once Home is reached (see [GsAppEffects]). */
internal const val EXTRA_GS_ROUTE = "gs_route"
/** [EXTRA_GS_ROUTE] value that runs the call + overlay permission flow (2.7.2 key_notification_intent "gs_permission"). */
internal const val GS_ROUTE_PERMISSION = "gs_permission"
/** 2.7.2 FROM_GS: MainActivity counts an entry from Quick Info or the install screen as an outside entry. */
internal const val EXTRA_FROM_GS = "from_gs"

private val GS_ROUTES = setOf(
    GS_ROUTE_PERMISSION, Screen.Home.route, Screen.Settings.route, Screen.VaultLock.route,
    Screen.Favorites.route, Screen.Moments.route, Screen.CollageMaker.route
)
private val GS_ALBUM_PREFIXES = listOf("filter:videos", "filter:camera", "filter:screenshots")
    .map { "album/${encodeRouteArg(it)}/" }

/** The [EXTRA_GS_ROUTE] values Quick Info, the install screen and the notification send; never a vault content route. */
internal fun isGsRoute(route: String): Boolean =
    route in GS_ROUTES || GS_ALBUM_PREFIXES.any { prefix ->
        route.startsWith(prefix) && route.length > prefix.length && route.substring(prefix.length).none { it == '/' || it == '?' }
    }

/** 2.7.2 posts both Quick Info notifications under this one id. */
internal const val GS_NOTIFICATION_ID = 129
private const val GS_CHANNEL_ID = "GS_CARD_CHANNEL_ID"

/** Opens the app on [route] the way 2.7.2 opens HomeActivity (NEW_TASK | CLEAR_TOP). */
internal fun appRouteIntent(context: Context, route: String): Intent =
    Intent(context, MainActivity::class.java)
        .putExtra(EXTRA_GS_ROUTE, route)
        .putExtra(EXTRA_FROM_GS, true)
        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)

internal fun quickInfoIntent(context: Context): Intent =
    Intent(context, QuickInfoActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP)

internal fun isAppInForeground(): Boolean =
    ActivityManager.RunningAppProcessInfo().also { ActivityManager.getMyMemoryState(it) }.importance ==
        ActivityManager.RunningAppProcessInfo.IMPORTANCE_FOREGROUND

internal fun isNetworkAvailable(context: Context): Boolean {
    val cm = context.getSystemService(ConnectivityManager::class.java) ?: return false
    return cm.getNetworkCapabilities(cm.activeNetwork)?.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) == true
}

private fun canNotify(context: Context): Boolean =
    (Build.VERSION.SDK_INT < 33 ||
        ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED) &&
        NotificationManagerCompat.from(context).areNotificationsEnabled()

/** 2.7.2 br/b: high-importance "Quick Info Card" channel, default sound, big text, auto-cancel. False when notifications are off. */
internal fun postGsNotification(context: Context, title: String, text: String, contentIntent: PendingIntent): Boolean {
    if (!canNotify(context)) return false
    if (Build.VERSION.SDK_INT >= 26) {
        val channel = NotificationChannel(GS_CHANNEL_ID, context.getString(R.string.gs1_notif_channel_name), NotificationManager.IMPORTANCE_HIGH)
        channel.description = context.getString(R.string.gs1_notif_channel_desc)
        context.getSystemService(NotificationManager::class.java)?.createNotificationChannel(channel)
    }
    val notification = NotificationCompat.Builder(context, GS_CHANNEL_ID)
        .setSmallIcon(R.drawable.ic_gs_photo) // shortcut: generic glyph instead of 2.7.2's brand icon, swap for our own small icon when one exists
        .setContentTitle(title)
        .setContentText(text)
        .setStyle(NotificationCompat.BigTextStyle().bigText(text))
        .setPriority(NotificationCompat.PRIORITY_HIGH)
        .setSound(RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION))
        .setContentIntent(contentIntent)
        .setAutoCancel(true)
        .build()
    return runCatching { NotificationManagerCompat.from(context).notify(GS_NOTIFICATION_ID, notification) }.isSuccess
}

/** "Quick Info disabled": the call ended while the overlay permission is off; a tap opens the app and asks for it again. */
internal fun postOverlayDisabledNotification(context: Context) {
    val pending = PendingIntent.getActivity(
        context, 1, appRouteIntent(context, GS_ROUTE_PERMISSION),
        PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
    )
    postGsNotification(
        context,
        context.getString(R.string.gs1_overlay_disabled_title),
        context.getString(R.string.gs1_overlay_disabled_text, context.getString(R.string.app_name_local)),
        pending
    )
}

/** "Conversation ended / See details" while the screen is locked; a tap opens Quick Info. */
internal fun postCallEndedNotification(context: Context) {
    val pending = PendingIntent.getActivity(
        context, 0, quickInfoIntent(context),
        PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
    )
    postGsNotification(context, context.getString(R.string.gs1_call_ended_title), context.getString(R.string.gs1_call_ended_text), pending)
}

/**
 * 2.7.2's background launch (bs/d, FloatingPopupManager): a 1 px transparent overlay window gives the app a visible
 * window, so an activity may start from the background on Android 10+. Needs the overlay permission; the window goes
 * 1 s after the launch.
 */
internal object OverlayLauncher {
    private val handler = Handler(Looper.getMainLooper())
    private var anchor: View? = null
    private val release = Runnable { remove() }

    /** Called while a call rings or is answered, so the screen can start the moment it ends. */
    fun hold(context: Context) {
        handler.removeCallbacks(release)
        if (anchor != null || Build.VERSION.SDK_INT < 26 || !Settings.canDrawOverlays(context)) return
        val app = context.applicationContext
        val view = View(app)
        val params = WindowManager.LayoutParams(
            1, 1, WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE,
            PixelFormat.TRANSLUCENT
        )
        runCatching {
            app.getSystemService(WindowManager::class.java).addView(view, params)
            anchor = view
        }
    }

    fun remove() {
        handler.removeCallbacks(release)
        val view = anchor ?: return
        anchor = null
        runCatching { view.context.getSystemService(WindowManager::class.java).removeView(view) }
    }

    /** Starts [intent] right away while the window is up, else after 500 ms of it (2.7.2 POPUP_DURATION). */
    fun launch(context: Context, intent: Intent) {
        val held = anchor?.isAttachedToWindow == true
        if (!held) hold(context)
        handler.postDelayed({
            runCatching { context.startActivity(intent) }
            handler.postDelayed(release, 1000)
        }, if (held) 0L else 500L)
    }
}
