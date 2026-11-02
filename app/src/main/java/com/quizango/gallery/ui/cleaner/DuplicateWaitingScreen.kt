package com.quizango.gallery.ui.cleaner

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.airbnb.lottie.compose.LottieAnimation
import com.airbnb.lottie.compose.LottieCompositionSpec
import com.airbnb.lottie.compose.animateLottieCompositionAsState
import com.airbnb.lottie.compose.rememberLottieComposition
import com.quizango.gallery.R
import com.quizango.gallery.ui.theme.GalleryBlue
import kotlinx.coroutines.delay

/**
 * Duplicate Waiting Screen reflecting 100% visual and structural parity with
 * reference activity_duplicate_waiting.xml and DuplicateWaitingActivity.java.
 * Background: color_success_lottie_blue (#0066FF).
 * Phase 1: Scanning with progress indicator, "Scanning...", and "This may take a while on large libraries."
 * Phase 2: Scanning Completed with success animation and auto-transition to duplicate results.
 */
@Composable
fun DuplicateWaitingScreen(
    onScanFinished: () -> Unit
) {
    var progress by remember { mutableFloatStateOf(0f) }
    var isScanComplete by remember { mutableStateOf(false) }

    val successComposition by rememberLottieComposition(LottieCompositionSpec.RawRes(R.raw.cleaner_done_lottie))
    val lottieProgress by animateLottieCompositionAsState(
        composition = successComposition,
        isPlaying = isScanComplete
    )

    LaunchedEffect(Unit) {
        // Smooth scanning progression matching reference SERVICE_IDLE_CHECK / MIN_SCREEN_MS
        for (i in 1..100) {
            progress = i / 100f
            delay(18)
        }
        isScanComplete = true
        delay(900)
        onScanFinished()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(GalleryBlue),
        contentAlignment = Alignment.Center
    ) {
        AnimatedVisibility(
            visible = !isScanComplete,
            enter = fadeIn(),
            exit = fadeOut()
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 32.dp)
            ) {
                CircularProgressIndicator(
                    color = Color.White,
                    strokeWidth = 3.5.dp,
                    modifier = Modifier.size(52.dp)
                )

                Spacer(modifier = Modifier.height(24.dp))

                Text(
                    text = stringResource(id = R.string.scanning),
                    fontSize = 20.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color.White
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = stringResource(id = R.string.dup_media_wait_hint),
                    fontSize = 14.sp,
                    color = Color.White.copy(alpha = 0.85f),
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(28.dp))

                LinearProgressIndicator(
                    progress = { progress },
                    modifier = Modifier
                        .fillMaxWidth(0.72f)
                        .height(6.dp)
                        .clip(RoundedCornerShape(3.dp)),
                    color = Color.White,
                    trackColor = Color.White.copy(alpha = 0.3f)
                )

                Spacer(modifier = Modifier.height(10.dp))

                Text(
                    text = "${(progress * 100).toInt()}%",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color.White.copy(alpha = 0.9f)
                )
            }
        }

        AnimatedVisibility(
            visible = isScanComplete,
            enter = fadeIn(),
            exit = fadeOut()
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 32.dp)
            ) {
                LottieAnimation(
                    composition = successComposition,
                    progress = { lottieProgress },
                    modifier = Modifier.size(140.dp)
                )

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = stringResource(id = R.string.scan_complete),
                    fontSize = 20.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color.White,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}
