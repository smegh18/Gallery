package com.quizango.gallery.data.local

import android.content.Context
import com.android.installreferrer.api.InstallReferrerClient
import com.android.installreferrer.api.InstallReferrerStateListener
import com.quizango.gallery.data.local.datastore.AppPreferences
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.coroutines.resume

/**
 * Reference 2.7.2 shows the onboarding screens (Language, Feature promo) only to campaign installs.
 * Play reports "utm_medium=organic" for normal installs; anything else, or no referrer, counts as a
 * campaign. Debug builds always take the campaign flow so it stays testable.
 */
object InstallSource {
    const val ORGANIC = "organic"
    const val CAMPAIGN = "campaign"

    fun classify(referrer: String?): String =
        if (referrer?.contains("utm_medium=organic") == true) ORGANIC else CAMPAIGN

    suspend fun isCampaign(context: Context, prefs: AppPreferences): Boolean {
        // Play answers "organic" even for adb installs, so debug builds always take the campaign flow
        if (context.applicationInfo.flags and android.content.pm.ApplicationInfo.FLAG_DEBUGGABLE != 0) return true
        prefs.installSource.first()?.let { return it == CAMPAIGN }
        val source = classify(withTimeoutOrNull(3_000) { referrer(context) })
        prefs.setInstallSource(source)
        return source == CAMPAIGN
    }

    private suspend fun referrer(context: Context): String? = suspendCancellableCoroutine { cont ->
        val client = InstallReferrerClient.newBuilder(context).build()
        client.startConnection(object : InstallReferrerStateListener {
            override fun onInstallReferrerSetupFinished(code: Int) {
                val value = if (code == InstallReferrerClient.InstallReferrerResponse.OK) {
                    runCatching { client.installReferrer.installReferrer }.getOrNull()
                } else null
                client.endConnection()
                if (cont.isActive) cont.resume(value)
            }

            override fun onInstallReferrerServiceDisconnected() {
                if (cont.isActive) cont.resume(null)
            }
        })
        cont.invokeOnCancellation { client.endConnection() }
    }
}
