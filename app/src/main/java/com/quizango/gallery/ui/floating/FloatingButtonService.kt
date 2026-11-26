package com.quizango.gallery.ui.floating

import android.app.Activity
import android.app.ActivityManager
import android.app.Application
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.content.pm.ServiceInfo
import android.content.res.Configuration
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import com.quizango.gallery.MainActivity
import com.quizango.gallery.R
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.cancel

/**
 * FloatingButtonService (2.7.2): a specialUse foreground service that keeps the sidebar handle over other apps.
 * The handle shows only while the app is in the background and goes away as soon as one of our screens starts.
 */
class FloatingButtonService : Service() {

    companion object {
        @Volatile
        var running = false
            private set
        private const val CHANNEL_ID = "floating_button_channel"
        private const val NOTIFICATION_ID = 1101
        // ProcessLifecycleOwner reports the stop 700 ms late, then 2.7.2 waits BACKGROUND_CHECK_DELAY (500 ms)
        private const val SHOW_DELAY_MS = 1200L
    }

    private val main = Handler(Looper.getMainLooper())
    private val scope = MainScope()
    private var handle: SidebarHandle? = null
    private val startedActivities = HashSet<Int>()
    // What the process looked like when the service was created, until the first activity callback says otherwise
    private var assumeForeground = false
    private val appVisible get() = startedActivities.isNotEmpty() || assumeForeground
    private val showWhenAway = Runnable { if (!appVisible) updateHandle() }

    private val activityCallbacks = object : Application.ActivityLifecycleCallbacks {
        override fun onActivityStarted(activity: Activity) {
            assumeForeground = false
            startedActivities += System.identityHashCode(activity)
            main.removeCallbacks(showWhenAway)
            removeHandle()
        }

        override fun onActivityStopped(activity: Activity) {
            assumeForeground = false
            startedActivities -= System.identityHashCode(activity)
            if (!appVisible) main.postDelayed(showWhenAway, SHOW_DELAY_MS)
        }

        override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) = Unit
        override fun onActivityResumed(activity: Activity) = Unit
        override fun onActivityPaused(activity: Activity) = Unit
        override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) = Unit
        override fun onActivityDestroyed(activity: Activity) = Unit
    }

    override fun onBind(intent: Intent?) = null

    override fun onCreate() {
        super.onCreate()
        running = true
        // Started from Home or Settings our screen is on top (importance 100); from boot or a restart nothing is
        val state = ActivityManager.RunningAppProcessInfo().also { ActivityManager.getMyMemoryState(it) }
        assumeForeground = state.importance <= ActivityManager.RunningAppProcessInfo.IMPORTANCE_FOREGROUND
        application.registerActivityLifecycleCallbacks(activityCallbacks)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (!startForegroundNow()) return START_NOT_STICKY
        if (!FloatingButtons.isOn(this)) {
            removeHandle()
            stopSelf()
            return START_NOT_STICKY
        }
        updateHandle()
        return START_STICKY
    }

    private fun startForegroundNow(): Boolean = runCatching {
        ServiceCompat.startForeground(
            this, NOTIFICATION_ID, notification(),
            if (Build.VERSION.SDK_INT >= 34) ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE else 0
        )
    }.onFailure { stopSelf() }.isSuccess

    private fun notification(): Notification {
        val title = getString(R.string.quick_access_floating_button_title)
        if (Build.VERSION.SDK_INT >= 26) {
            getSystemService(NotificationManager::class.java)
                .createNotificationChannel(NotificationChannel(CHANNEL_ID, title, NotificationManager.IMPORTANCE_LOW))
        }
        // 2.7.2 opens the splash: the app restarts fresh
        val open = PendingIntent.getActivity(
            this, NOTIFICATION_ID,
            Intent(this, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK),
            PendingIntent.FLAG_IMMUTABLE
        )
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.notification_icon)
            .setContentTitle(getString(R.string.app_name))
            .setContentText(title)
            .setContentIntent(open)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setOngoing(true)
            .build()
    }

    private fun updateHandle() {
        when {
            !FloatingButtons.isOn(this) -> {
                removeHandle()
                stopSelf()
            }
            appVisible -> removeHandle()
            handle == null -> handle = SidebarHandle(this, scope).takeIf {
                it.show(firstTime = !FloatingButtons.prefs(this).getBoolean(FloatingButtons.KEY_HINT_SHOWED, false))
            }
        }
    }

    private fun removeHandle() {
        handle?.remove()
        handle = null
    }

    /** Rotation and screen-size changes move the edges: rebuild the handle against the new bounds. */
    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        if (handle != null) {
            removeHandle()
            updateHandle()
        }
    }

    override fun onDestroy() {
        running = false
        application.unregisterActivityLifecycleCallbacks(activityCallbacks)
        main.removeCallbacksAndMessages(null)
        removeHandle()
        scope.cancel()
        super.onDestroy()
    }
}
