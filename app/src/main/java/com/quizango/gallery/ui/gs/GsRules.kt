package com.quizango.gallery.ui.gs

import android.content.Intent
import java.util.Locale

/** What 2.7.2's GsSetupCardReceiver does once a call ends (PHONE_STATE IDLE). */
internal enum class CallEndAction { OVERLAY_DISABLED_NOTICE, WAIT_FOR_UNLOCK, SHOW }

internal fun callEndAction(canDrawOverlays: Boolean, appInForeground: Boolean, keyguardLocked: Boolean): CallEndAction = when {
    !canDrawOverlays && !appInForeground -> CallEndAction.OVERLAY_DISABLED_NOTICE
    keyguardLocked -> CallEndAction.WAIT_FOR_UNLOCK
    else -> CallEndAction.SHOW
}

// TelephonyManager.EXTRA_STATE_* values; those fields are not compile-time constants
internal const val STATE_RINGING = "RINGING"
internal const val STATE_OFFHOOK = "OFFHOOK"
internal const val STATE_IDLE = "IDLE"

/** New (gsCallStartMs, gsCallEndMs) after a PHONE_STATE change; null for any other state. Only RINGING clears the start. */
internal fun callTimes(state: String?, start: Long, now: Long): Pair<Long, Long>? = when (state) {
    STATE_RINGING -> 0L to 0L
    STATE_OFFHOOK -> (if (start == 0L) now else start) to 0L
    STATE_IDLE -> start to now
    else -> null
}

/** "mm:ss" for "Duration · %1$s": minutes wrap at the hour as in 2.7.2; an unanswered call or a clock jump reads 00:00. */
internal fun callDurationText(startMs: Long, endMs: Long): String {
    if (startMs == 0L || endMs < startMs) return "00:00"
    val seconds = (endMs - startMs) / 1000
    return String.format(Locale.getDefault(), "%02d:%02d", seconds % 3600 / 60, seconds % 60)
}

/** 2.7.2 PackageEventInfo.eventType (1 install, 2 update, 3 uninstall). */
internal enum class PackageEvent { INSTALL, UPDATE, UNINSTALL }

/** An update arrives as REMOVED and ADDED (both with EXTRA_REPLACING) then REPLACED: only REPLACED counts. */
internal fun packageEvent(action: String?, replacing: Boolean): PackageEvent? = when {
    action == Intent.ACTION_PACKAGE_REPLACED -> PackageEvent.UPDATE
    replacing -> null
    action == Intent.ACTION_PACKAGE_ADDED -> PackageEvent.INSTALL
    action == Intent.ACTION_PACKAGE_REMOVED -> PackageEvent.UNINSTALL
    else -> null
}

/**
 * 2.7.2's in-memory guards (PackageMonitorReceiver statics): the same action and package within 5 s is a duplicate,
 * and an update screen shows at most once per gs2_screen_cooldown_minutes (30). Times are elapsedRealtime, and the
 * last-shown time starts at 0 as in 2.7.2, so no update screen shows in the first 30 minutes after boot.
 */
internal class IuThrottle(private val duplicateWindowMs: Long = 5_000, private val updateCooldownMs: Long = 30 * 60_000L) {
    private var lastKey: String? = null
    private var lastEventAt = 0L
    private var lastUpdateShownAt = 0L

    @Synchronized
    fun isDuplicate(action: String, packageName: String, now: Long): Boolean {
        val key = "$action|$packageName"
        if (key == lastKey && now - lastEventAt < duplicateWindowMs) return true
        lastKey = key
        lastEventAt = now
        return false
    }

    @Synchronized
    fun takeUpdateSlot(now: Long): Boolean {
        if (now - lastUpdateShownAt < updateCooldownMs) return false
        lastUpdateShownAt = now
        return true
    }
}
