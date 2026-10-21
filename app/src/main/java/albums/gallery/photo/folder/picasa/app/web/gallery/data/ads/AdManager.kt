package albums.gallery.photo.folder.picasa.app.web.gallery.data.ads

import android.app.Activity
import android.content.Context
import android.os.SystemClock
import android.util.Log
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.MobileAds
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback
import com.google.android.gms.ads.rewarded.RewardedAd
import com.google.android.gms.ads.rewarded.RewardedAdLoadCallback
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Production-quality Advertising Manager for Gallery application.
 *
 * Implements:
 * 1. Safe lifecycle handling and asynchronous loading.
 * 2. Frequency capping and cooldowns between interstitials.
 * 3. Graceful degradation: never blocks user navigation on load failure or dismissal.
 * 4. Official Google Mobile Ads test units for zero production violation risk.
 */
object AdManager {
    private const val TAG = "AdManager"

    private val isInitialized = AtomicBoolean(false)
    private var interstitialAd: InterstitialAd? = null
    private var isInterstitialLoading = false
    private var lastInterstitialTimeMs: Long = 0L

    private var rewardedAd: RewardedAd? = null
    private var isRewardedLoading = false

    fun initialize(context: Context) {
        if (isInitialized.compareAndSet(false, true)) {
            try {
                MobileAds.initialize(context) { status ->
                    Log.d(TAG, "MobileAds initialized: $status")
                    loadInterstitial(context)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error initializing MobileAds", e)
            }
        }
    }

    /**
     * Preloads an interstitial ad using Google Mobile Ads test unit.
     */
    fun loadInterstitial(context: Context) {
        if (interstitialAd != null || isInterstitialLoading) return
        isInterstitialLoading = true

        val adRequest = AdRequest.Builder().build()
        InterstitialAd.load(
            context,
            AdConstants.TEST_INTERSTITIAL_ID,
            adRequest,
            object : InterstitialAdLoadCallback() {
                override fun onAdLoaded(ad: InterstitialAd) {
                    interstitialAd = ad
                    isInterstitialLoading = false
                    Log.d(TAG, "Interstitial ad loaded successfully")
                }

                override fun onAdFailedToLoad(loadAdError: LoadAdError) {
                    interstitialAd = null
                    isInterstitialLoading = false
                    Log.w(TAG, "Interstitial ad failed to load: ${loadAdError.message}")
                }
            }
        )
    }

    /**
     * Shows an interstitial ad if eligible and loaded.
     * Always calls [onDismissed] to ensure normal user navigation continues uninterrupted.
     */
    fun showInterstitial(
        activity: Activity,
        triggerEvent: String,
        onDismissed: () -> Unit
    ) {
        val now = SystemClock.elapsedRealtime()
        val cooldownPassed = (now - lastInterstitialTimeMs) >= AdConstants.INTERSTITIAL_COOLDOWN_MS

        val currentAd = interstitialAd
        if (currentAd != null && cooldownPassed) {
            currentAd.fullScreenContentCallback = object : FullScreenContentCallback() {
                override fun onAdDismissedFullScreenContent() {
                    Log.d(TAG, "Interstitial dismissed on event: $triggerEvent")
                    interstitialAd = null
                    lastInterstitialTimeMs = SystemClock.elapsedRealtime()
                    loadInterstitial(activity.applicationContext)
                    onDismissed()
                }

                override fun onAdFailedToShowFullScreenContent(adError: AdError) {
                    Log.w(TAG, "Interstitial failed to show: ${adError.message}")
                    interstitialAd = null
                    loadInterstitial(activity.applicationContext)
                    onDismissed()
                }

                override fun onAdShowedFullScreenContent() {
                    Log.d(TAG, "Interstitial showing for event: $triggerEvent")
                }
            }
            currentAd.show(activity)
        } else {
            // Not eligible or not loaded: continue immediately without blocking
            if (currentAd == null && !isInterstitialLoading) {
                loadInterstitial(activity.applicationContext)
            }
            onDismissed()
        }
    }

    /**
     * Preloads a rewarded ad.
     */
    fun loadRewarded(context: Context) {
        if (rewardedAd != null || isRewardedLoading) return
        isRewardedLoading = true

        val adRequest = AdRequest.Builder().build()
        RewardedAd.load(
            context,
            AdConstants.TEST_REWARDED_ID,
            adRequest,
            object : RewardedAdLoadCallback() {
                override fun onAdLoaded(ad: RewardedAd) {
                    rewardedAd = ad
                    isRewardedLoading = false
                    Log.d(TAG, "Rewarded ad loaded successfully")
                }

                override fun onAdFailedToLoad(loadAdError: LoadAdError) {
                    rewardedAd = null
                    isRewardedLoading = false
                    Log.w(TAG, "Rewarded ad failed to load: ${loadAdError.message}")
                }
            }
        )
    }

    /**
     * Shows a rewarded ad with completion callback.
     */
    fun showRewarded(
        activity: Activity,
        onRewardEarned: () -> Unit,
        onDismissed: () -> Unit
    ) {
        val currentAd = rewardedAd
        if (currentAd != null) {
            var rewardEarned = false
            currentAd.fullScreenContentCallback = object : FullScreenContentCallback() {
                override fun onAdDismissedFullScreenContent() {
                    rewardedAd = null
                    loadRewarded(activity.applicationContext)
                    if (rewardEarned) {
                        onRewardEarned()
                    }
                    onDismissed()
                }

                override fun onAdFailedToShowFullScreenContent(adError: AdError) {
                    rewardedAd = null
                    loadRewarded(activity.applicationContext)
                    onDismissed()
                }
            }
            currentAd.show(activity) { _ ->
                rewardEarned = true
            }
        } else {
            loadRewarded(activity.applicationContext)
            onDismissed()
        }
    }
}
