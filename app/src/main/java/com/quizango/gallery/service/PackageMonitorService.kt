package com.quizango.gallery.service

import android.app.KeyguardManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import android.os.SystemClock
import android.provider.Settings
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import androidx.core.content.ContextCompat
import com.quizango.gallery.GalleryApplication
import com.quizango.gallery.MainActivity
import com.quizango.gallery.R
import com.quizango.gallery.ui.gs.IuThrottle
import com.quizango.gallery.ui.gs.OverlayLauncher
import com.quizango.gallery.ui.gs.PackageEvent
import com.quizango.gallery.ui.gs.isAppInForeground
import com.quizango.gallery.ui.gs.isNetworkAvailable
import com.quizango.gallery.ui.gs.packageEvent
import com.quizango.gallery.ui.iu.InstallUninstallActivity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/**
 * 2.7.2 PackageMonitorService: a special-use foreground service whose runtime receiver turns app installs, updates and
 * uninstalls into the install/uninstall screen. Runtime-registered because Android 8+ no longer delivers these
 * broadcasts to manifest receivers.
 */
class PackageMonitorService : Service() {
    private var receiver: BroadcastReceiver? = null

    override fun onCreate() {
        super.onCreate()
        if (!enterForeground()) return
        val packageReceiver = PackageReceiver()
        val filter = IntentFilter().apply {
            addAction(Intent.ACTION_PACKAGE_ADDED)
            addAction(Intent.ACTION_PACKAGE_REMOVED)
            addAction(Intent.ACTION_PACKAGE_REPLACED)
            addDataScheme("package")
        }
        // System broadcasts still reach a not-exported receiver
        runCatching { ContextCompat.registerReceiver(this, packageReceiver, filter, ContextCompat.RECEIVER_NOT_EXPORTED) }
            .onSuccess { receiver = packageReceiver }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (!enterForeground()) return START_NOT_STICKY
        // A sticky restart can come after the overlay permission was revoked
        if (!Settings.canDrawOverlays(this)) {
            stopSelf(startId)
            return START_NOT_STICKY
        }
        return START_STICKY
    }

    override fun onDestroy() {
        receiver?.let { runCatching { unregisterReceiver(it) } }
        receiver = null
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun enterForeground(): Boolean = try {
        if (Build.VERSION.SDK_INT >= 26) {
            val channel = NotificationChannel(CHANNEL_ID, getString(R.string.iu_service_channel_name), NotificationManager.IMPORTANCE_LOW)
            getSystemService(NotificationManager::class.java)?.createNotificationChannel(channel)
        }
        val open = PendingIntent.getActivity(
            this, 1001, Intent(this, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_gs_photo) // shortcut: generic glyph instead of 2.7.2's brand icon, swap for our own small icon when one exists
            .setContentTitle(getString(R.string.app_name))
            .setContentText(getString(R.string.iu_service_notification_text))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .setContentIntent(open)
            .build()
        val type = if (Build.VERSION.SDK_INT >= 34) ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE else 0
        ServiceCompat.startForeground(this, 1, notification, type)
        true
    } catch (e: Exception) {
        // Android 12+ refuses a background start without an exemption
        stopSelf()
        false
    }

    /** 2.7.2 PackageMonitorReceiver, in its order of checks. */
    private class PackageReceiver : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            val action = intent.action ?: return
            val packageName = intent.data?.schemeSpecificPart ?: return
            if (packageName == context.packageName || !isNetworkAvailable(context)) return
            val event = packageEvent(action, intent.getBooleanExtra(Intent.EXTRA_REPLACING, false)) ?: return
            if (throttle.isDuplicate(action, packageName, SystemClock.elapsedRealtime())) return
            if (context.getSystemService(KeyguardManager::class.java)?.isKeyguardLocked == true) return
            if (!Settings.canDrawOverlays(context)) return
            if (event == PackageEvent.UPDATE && !throttle.takeUpdateSlot(SystemClock.elapsedRealtime())) return
            val app = context.applicationContext as GalleryApplication
            val pending = goAsync()
            CoroutineScope(Dispatchers.Main.immediate).launch {
                try {
                    if (!app.preferences.iuAlwaysOn.first()) return@launch
                    val screen = InstallUninstallActivity.intent(app, packageName, event)
                    if (isAppInForeground()) app.startActivity(screen) else OverlayLauncher.launch(app, screen)
                } finally {
                    pending.finish()
                }
            }
        }
    }

    companion object {
        private const val CHANNEL_ID = "PackageMonitorChannel"
        private val throttle = IuThrottle()

        /** 2.7.2 IUServiceKeeper: runs only with the overlay permission and while the IU setting is Always ON. */
        suspend fun startIfAllowed(context: Context) {
            val app = context.applicationContext as GalleryApplication
            if (!Settings.canDrawOverlays(app) || !app.preferences.iuAlwaysOn.first()) return
            runCatching { ContextCompat.startForegroundService(app, Intent(app, PackageMonitorService::class.java)) }
        }

        fun stop(context: Context) {
            context.stopService(Intent(context, PackageMonitorService::class.java))
        }
    }
}

/** 2.7.2 IUBootReceiver: restarts the monitor after a reboot or an update of this app. */
class IuBootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED && intent.action != Intent.ACTION_MY_PACKAGE_REPLACED) return
        val pending = goAsync()
        CoroutineScope(Dispatchers.Main.immediate).launch {
            try {
                PackageMonitorService.startIfAllowed(context)
            } finally {
                pending.finish()
            }
        }
    }
}
