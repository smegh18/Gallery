package com.quizango.gallery.ui.gs

import android.app.KeyguardManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.telephony.TelephonyManager
import androidx.core.content.ContextCompat
import com.quizango.gallery.GalleryApplication
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/**
 * 2.7.2 GsSetupCardReceiver: PHONE_STATE (delivered only with READ_PHONE_STATE granted) records when a call was answered
 * and ended, and at the end shows Quick Info, waits for unlock first, or posts "Quick Info disabled" without the overlay
 * permission. No caller number is read: 2.7.2 always shows "Private Number".
 */
class GsSetupCardReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != TelephonyManager.ACTION_PHONE_STATE_CHANGED) return
        val state = intent.getStringExtra(TelephonyManager.EXTRA_STATE)
        val app = context.applicationContext as GalleryApplication
        val pending = goAsync()
        CoroutineScope(Dispatchers.Main.immediate).launch {
            try {
                handle(app, state)
            } finally {
                pending.finish()
            }
        }
    }

    private suspend fun handle(app: GalleryApplication, state: String?) {
        val prefs = app.preferences
        if (!prefs.afterCallScreenEnabled.first()) return
        // Each change arrives more than once (with and without the number, once per SIM): act on the first
        if (state == lastPhoneState) return
        lastPhoneState = state
        val (start, _) = prefs.gsCallTimes.first()
        val (newStart, newEnd) = callTimes(state, start, System.currentTimeMillis()) ?: return
        prefs.setGsCallTimes(newStart, newEnd)
        if (state != STATE_IDLE) {
            cancelUnlockWait(app)
            OverlayLauncher.hold(app)
            return
        }
        val keyguardLocked = app.getSystemService(KeyguardManager::class.java)?.isKeyguardLocked == true
        when (callEndAction(Settings.canDrawOverlays(app), isAppInForeground(), keyguardLocked)) {
            CallEndAction.OVERLAY_DISABLED_NOTICE -> {
                OverlayLauncher.remove()
                postOverlayDisabledNotification(app)
            }
            CallEndAction.WAIT_FOR_UNLOCK -> {
                postCallEndedNotification(app)
                waitForUnlock(app)
            }
            CallEndAction.SHOW -> if (isAppInForeground()) {
                OverlayLauncher.remove()
                app.startActivity(quickInfoIntent(app))
            } else {
                OverlayLauncher.launch(app, quickInfoIntent(app))
            }
        }
    }

    companion object {
        private var lastPhoneState: String? = null
        private var unlockReceiver: BroadcastReceiver? = null

        /** 2.7.2: Quick Info starts 300 ms after the user unlocks (USER_PRESENT), once. */
        private fun waitForUnlock(app: Context) {
            if (unlockReceiver != null) return
            val receiver = object : BroadcastReceiver() {
                override fun onReceive(context: Context, intent: Intent) {
                    if (intent.action != Intent.ACTION_USER_PRESENT) return
                    cancelUnlockWait(app)
                    Handler(Looper.getMainLooper()).postDelayed({ OverlayLauncher.launch(app, quickInfoIntent(app)) }, 300)
                }
            }
            unlockReceiver = receiver
            runCatching {
                ContextCompat.registerReceiver(app, receiver, IntentFilter(Intent.ACTION_USER_PRESENT), ContextCompat.RECEIVER_NOT_EXPORTED)
            }.onFailure { unlockReceiver = null }
        }

        private fun cancelUnlockWait(app: Context) {
            val receiver = unlockReceiver ?: return
            unlockReceiver = null
            runCatching { app.unregisterReceiver(receiver) }
        }
    }
}
