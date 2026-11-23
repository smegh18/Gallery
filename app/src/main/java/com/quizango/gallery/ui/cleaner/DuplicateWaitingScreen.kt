package com.quizango.gallery.ui.cleaner

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.airbnb.lottie.compose.LottieAnimation
import com.airbnb.lottie.compose.LottieCompositionSpec
import com.airbnb.lottie.compose.rememberLottieAnimatable
import com.airbnb.lottie.compose.rememberLottieComposition
import com.quizango.gallery.R
import com.quizango.gallery.ui.theme.GalleryText
import com.quizango.gallery.ui.theme.sdp

/**
 * DuplicateWaitingActivity (activity_duplicate_waiting): blue screen with a spinner while the fingerprint scan runs,
 * then the done state (`clDoneAnimation`) plays `success` once and the results replace this screen (`ak/h`).
 * The done state is a state, not a screen: DuplicateScanningCompleteActivity is not built (Q4).
 */
@Composable
fun DuplicateWaitingScreen(
    scan: suspend () -> Unit,
    onScanFinished: () -> Unit
) {
    var done by remember { mutableStateOf(false) }
    val compositionResult = rememberLottieComposition(LottieCompositionSpec.RawRes(R.raw.success))
    val animatable = rememberLottieAnimatable()
    val finish by rememberUpdatedState(onScanFinished)

    LaunchedEffect(Unit) {
        runDuplicateWaiting(
            scan = scan,
            onDone = { done = true },
            // A composition that fails to load must not strand the user on this screen
            playSuccess = { runCatching { compositionResult.await() }.getOrNull()?.let { animatable.animate(it) } },
            onFinished = finish
        )
    }

    val white = colorResource(R.color.onlyWhite)
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(colorResource(R.color.color_success_lottie_blue))
            .padding(14.sdp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.fillMaxWidth()
        ) {
            if (!done) {
                CircularProgressIndicator(color = white, modifier = Modifier.size(48.dp))
                Text(
                    text = stringResource(R.string.scanning),
                    style = GalleryText.BodyLargeSemiBold,
                    color = white,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth().padding(top = 18.sdp)
                )
                Text(
                    text = stringResource(R.string.dup_media_wait_hint),
                    style = GalleryText.BodyMediumRegular,
                    color = white,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth().padding(top = 8.sdp)
                )
            } else {
                LottieAnimation(
                    composition = compositionResult.value,
                    progress = { animatable.progress },
                    modifier = Modifier.size(150.sdp)
                )
                Text(
                    text = stringResource(R.string.scan_complete),
                    style = GalleryText.BodyLargeSemiBold,
                    color = white,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 10.sdp)
                )
            }
        }
    }
}

/** DW order: the real scan, then the done state, then `success` once; the results open when it ends, never on a timer. */
internal suspend fun runDuplicateWaiting(
    scan: suspend () -> Unit,
    onDone: () -> Unit,
    playSuccess: suspend () -> Unit,
    onFinished: () -> Unit
) {
    scan()
    onDone()
    playSuccess()
    onFinished()
}
