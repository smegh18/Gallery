package com.quizango.gallery.ui.floating

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

/** The floating part of 2.7.2's IUBootReceiver: after a reboot or an app update the button comes back if it is on. */
class FloatingBootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED || intent.action == Intent.ACTION_MY_PACKAGE_REPLACED) {
            FloatingButtons.start(context)
        }
    }
}
