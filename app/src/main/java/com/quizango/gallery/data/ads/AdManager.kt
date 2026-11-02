package com.quizango.gallery.data.ads

import android.app.Activity
import android.content.Context

/**
 * AdManager clean stub ensuring zero ad network bloat, no third-party tracking,
 * and immediate non-blocking execution per clean reconstruction principles.
 */
object AdManager {
    fun initialize(context: Context) {
        // No-op: monetization SDKs excluded per user requirements
    }

    fun loadInterstitial(context: Context) {
        // No-op
    }

    fun showInterstitial(
        activity: Activity?,
        eventKey: String = "",
        onDismissOrSkipped: () -> Unit
    ) {
        onDismissOrSkipped()
    }

    fun showRewarded(
        activity: Activity?,
        onRewardEarned: () -> Unit,
        onDismissOrSkipped: () -> Unit
    ) {
        onRewardEarned()
        onDismissOrSkipped()
    }
}
