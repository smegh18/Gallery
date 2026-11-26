package com.quizango.gallery.notification

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.job.JobInfo
import android.app.job.JobParameters
import android.app.job.JobScheduler
import android.app.job.JobService
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.widget.RemoteViews
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.core.app.NotificationCompat
import com.quizango.gallery.MainActivity
import com.quizango.gallery.R
import com.quizango.gallery.ui.navigation.EXTRA_FROM_SWAPPABLE_NOTIFICATION
import com.quizango.gallery.ui.navigation.EXTRA_NOTIFICATION_ACTION
import com.quizango.gallery.ui.navigation.EXTRA_NOTIFICATION_INTENT

/** The swappable notification's features: common_data.swappable_notification_options keys, label and icon. */
internal enum class QuickAction(val key: String, @StringRes val label: Int, @DrawableRes val icon: Int) {
    CAMERA("camera", R.string.str_notification_camera, R.drawable.ic_notification_camera),
    FAVORITE("favorite", R.string.str_notification_favorite, R.drawable.ic_notification_fav),
    CLEANER("cleaner", R.string.str_notification_cleaner, R.drawable.ic_notification_cleaner),
    MOMENTS("moments", R.string.str_notification_moment, R.drawable.ic_notification_moments),
    DUPLICATE("duplicate", R.string.str_notification_duplicate, R.drawable.ic_notification_duplicate),
    RECYCLE("recycle", R.string.str_notification_recycle, R.drawable.ic_notification_recycle);

    companion object {
        val DEFAULT_OPTIONS = listOf("camera", "favorite", "cleaner")

        /** g1/b.cWbN6pumKk: known keys in the given order, three slots at most. */
        fun features(options: List<String>): List<QuickAction> =
            options.mapNotNull { key -> entries.firstOrNull { it.key == key } }.take(3)
    }
}

/**
 * 2.7.2's "Quick Actions" notification (com.swappable_notification): notification_small/big_layout with up to three
 * feature buttons, each opening the app on that feature. Re-posted on a period, as 2.7.2's NotificationWorker.
 */
internal object QuickActionsNotification {
    const val CHANNEL_ID = "Quick Actions" // 2.7.2 uses the name as the channel id
    private const val NOTIFICATION_ID = 0x3dc
    private const val JOB_ID = 0x3dc

    // shortcut: 2.7.2 takes the period from remote config swappable_notification_timer (bundled default 0 = off,
    // live value unknown); this is its in-code fallback. Set it from a real config once one exists.
    private const val INTERVAL_MINUTES = 600L

    private val BUTTONS = listOf(R.id.btnFeature1, R.id.btnFeature2, R.id.btnFeature3)
    private val TEXTS = listOf(R.id.txtFeature1, R.id.txtFeature2, R.id.txtFeature3)
    private val ICONS = listOf(R.id.ivFeature1, R.id.ivFeature2, R.id.ivFeature3)

    /** Home calls this on each start once notifications may show (HA bw/h 16); the job is set up once. */
    fun schedule(context: Context) {
        val scheduler = context.getSystemService(JobScheduler::class.java) ?: return
        if (scheduler.getPendingJob(JOB_ID) != null) return
        post(context) // a new periodic WorkManager job runs right away
        scheduler.schedule(
            JobInfo.Builder(JOB_ID, ComponentName(context, QuickActionsJob::class.java))
                .setPeriodic(INTERVAL_MINUTES * 60_000L)
                .setPersisted(true)
                .build()
        )
    }

    /** HA.o000O00: POST_NOTIFICATIONS is only asked for on Android 13+. */
    fun allowed(context: Context): Boolean = Build.VERSION.SDK_INT < 33 ||
        context.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED

    fun post(context: Context) {
        if (!allowed(context)) return
        val manager = context.getSystemService(NotificationManager::class.java) ?: return
        if (Build.VERSION.SDK_INT >= 26) {
            manager.createNotificationChannel(
                NotificationChannel(CHANNEL_ID, CHANNEL_ID, NotificationManager.IMPORTANCE_DEFAULT).apply {
                    setSound(null, null)
                    enableVibration(false)
                }
            )
        }
        val small = RemoteViews(context.packageName, R.layout.notification_small_layout)
        val big = RemoteViews(context.packageName, R.layout.notification_big_layout)
        QuickAction.features(QuickAction.DEFAULT_OPTIONS).forEachIndexed { slot, action ->
            val label = context.getString(action.label)
            val tap = PendingIntent.getActivity(
                context,
                BUTTONS[slot],
                Intent(context, MainActivity::class.java)
                    .putExtra(EXTRA_NOTIFICATION_INTENT, label)
                    .putExtra(EXTRA_NOTIFICATION_ACTION, action.key)
                    .putExtra(EXTRA_FROM_SWAPPABLE_NOTIFICATION, true)
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP),
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
            )
            for (views in listOf(small, big)) {
                views.setImageViewResource(ICONS[slot], action.icon)
                views.setTextViewText(TEXTS[slot], label)
                views.setOnClickPendingIntent(BUTTONS[slot], tap)
                views.setViewVisibility(BUTTONS[slot], android.view.View.VISIBLE)
            }
        }
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.notification_icon)
            .setCustomContentView(small)
            .setCustomBigContentView(big)
            .setStyle(NotificationCompat.DecoratedCustomViewStyle())
            .setOnlyAlertOnce(true)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .build()
        manager.notify(NOTIFICATION_ID, notification)
    }
}

/** NotificationWorker stand-in: platform JobScheduler, so no WorkManager dependency. */
class QuickActionsJob : JobService() {
    override fun onStartJob(params: JobParameters?): Boolean {
        QuickActionsNotification.post(this)
        return false
    }

    override fun onStopJob(params: JobParameters?): Boolean = false
}
