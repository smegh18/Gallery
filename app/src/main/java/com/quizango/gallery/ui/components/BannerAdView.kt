package com.quizango.gallery.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.quizango.gallery.data.ads.AdConstants

/**
 * Ad container stub: ads are excluded from reconstruction per user rules
 * ("Zero ad networks or telemetry code in the reconstructed app").
 */
@Composable
fun BannerAdView(
    modifier: Modifier = Modifier,
    adUnitId: String = AdConstants.TEST_BANNER_ID
) {
    // Zero-overhead no-op: ads strictly excluded
}
