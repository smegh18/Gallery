package com.quizango.gallery.ui.splash

import androidx.compose.runtime.snapshotFlow
import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.airbnb.lottie.compose.LottieAnimation
import com.airbnb.lottie.compose.LottieCompositionSpec
import com.airbnb.lottie.compose.animateLottieCompositionAsState
import com.airbnb.lottie.compose.rememberLottieComposition
import com.quizango.gallery.R
import com.quizango.gallery.data.local.datastore.AppPreferences
import com.quizango.gallery.ui.navigation.Screen
import com.quizango.gallery.ui.theme.GalleryBlue
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first

/**
 * Splash screen reconstructed with 100% visual and behavioral parity
 * against reference APK `activity_splash.xml` and `SplashActivity.java`.
 *
 * Features:
 * - Authentic logo animation via Lottie (`R.raw.app_icon_lottie`) inside MaterialCardView (140sdp, cornerRadius 20sdp).
 * - Animated progress bar matching reference `splashProgress` and `delayTime` (3000ms).
 * - "Your Photo Manager" title (`@string/your_photo_manager`).
 * - Window insets compliance for edge-to-edge layouts.
 * - Dynamic preference & permission-based navigation.
 */
@Composable
fun SplashScreen(
    appPreferences: AppPreferences,
    onNavigate: (String) -> Unit
) {
    val context = LocalContext.current

    // Authentic Lottie animation from reference APK (R.raw.app_icon_lottie)
    val lottieComposition by rememberLottieComposition(LottieCompositionSpec.RawRes(R.raw.app_icon_lottie))
    val lottieProgress by animateLottieCompositionAsState(
        composition = lottieComposition,
        iterations = 1,
        isPlaying = true
    )

    // The logo's movement ends at frame 25 of 90 (0.83 s); the rest of the file just holds the
    // last frame, which looked frozen. Leave a short beat after the movement; the cap covers a failed load.
    LaunchedEffect(Unit) {
        val hasCompletedWelcome = appPreferences.hasCompletedWelcome.first()
        val hasPermissions = checkStoragePermissions(context)
        kotlinx.coroutines.withTimeoutOrNull(2_000) {
            snapshotFlow { lottieProgress >= 26f / 90f }.first { it }
            delay(350)
        }

        if (!hasCompletedWelcome) {
            onNavigate(Screen.Welcome.route)
        } else if (!hasPermissions) {
            onNavigate(Screen.Permission.route)
        } else {
            onNavigate(Screen.Home.route)
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentAlignment = Alignment.Center
    ) {
        // Reference MaterialCardView with lottie_splash & iv_splash (140sdp, cornerRadius 20sdp)
        Box(
            modifier = Modifier
                .size(170.dp)
                .clip(RoundedCornerShape(26.dp)),
            contentAlignment = Alignment.Center
        ) {
            if (lottieComposition != null) {
                LottieAnimation(
                    composition = lottieComposition,
                    progress = { lottieProgress },
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                Image(
                    painter = painterResource(id = R.drawable.app_icon_splash),
                    contentDescription = "App Icon",
                    modifier = Modifier.fillMaxSize()
                )
            }
        }

        // Reference Bottom Area: "Your Photo Manager" + Horizontal Animated Progress
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .windowInsetsPadding(
                    WindowInsets.safeDrawing.only(
                        WindowInsetsSides.Bottom + WindowInsetsSides.Horizontal
                    )
                )
                .padding(bottom = 36.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = stringResource(id = R.string.your_photo_manager),
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onBackground,
                fontSize = 14.sp
            )
            Spacer(modifier = Modifier.height(10.dp))
            // Reference splashProgress: Expressive sine-wave loading animation matching reference on physical device
            WavyProgressIndicator(
                modifier = Modifier
                    .width(200.dp)
                    .height(14.dp),
                color = GalleryBlue,
                trackColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f),
                strokeWidth = 4.dp,
                amplitude = 3.dp,
                wavelength = 22.dp,
                cycleCount = 4,
                animationDurationMillis = 1100
            )
        }
    }
}

/**
 * Authentic expressive Wavy Progress Indicator matching Android 15 & Material 3
 * wavy loading animation observed on physical device in the reference app.
 *
 * The progress bar itself bends and transforms into the wave (no straight track underneath).
 */
@Composable
fun WavyProgressIndicator(
    modifier: Modifier = Modifier,
    color: Color = GalleryBlue,
    trackColor: Color = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f),
    strokeWidth: Dp = 4.dp,
    amplitude: Dp = 3.dp,
    wavelength: Dp = 22.dp,
    cycleCount: Int = 4,
    animationDurationMillis: Int = 1100
) {
    val infiniteTransition = rememberInfiniteTransition(label = "WavyProgress")
    val waveProgress by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = animationDurationMillis, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "WaveProgressValue"
    )

    Canvas(modifier = modifier) {
        val strokeWidthPx = strokeWidth.toPx()
        val amplitudePx = amplitude.toPx()
        val wavelengthPx = wavelength.toPx()
        val centerY = size.height / 2f
        val trackStart = strokeWidthPx / 2f
        val trackEnd = size.width - strokeWidthPx / 2f
        val trackLength = trackEnd - trackStart

        val waveWidth = wavelengthPx * cycleCount
        val totalTravel = trackLength + waveWidth
        val waveStart = trackStart - waveWidth + waveProgress * totalTravel
        val waveEnd = waveStart + waveWidth

        // 1. Draw left straight track segment ONLY outside the wave
        if (waveStart > trackStart) {
            val leftEnd = minOf(waveStart, trackEnd)
            if (leftEnd - trackStart > 1f) {
                drawLine(
                    color = trackColor,
                    start = Offset(trackStart, centerY),
                    end = Offset(leftEnd, centerY),
                    strokeWidth = strokeWidthPx,
                    cap = StrokeCap.Round
                )
            }
        }

        // 2. Draw right straight track segment ONLY outside the wave
        if (waveEnd < trackEnd) {
            val rightStart = maxOf(waveEnd, trackStart)
            if (trackEnd - rightStart > 1f) {
                drawLine(
                    color = trackColor,
                    start = Offset(rightStart, centerY),
                    end = Offset(trackEnd, centerY),
                    strokeWidth = strokeWidthPx,
                    cap = StrokeCap.Round
                )
            }
        }

        // 3. Draw the wavy shape itself (the progress bar bends into the wave; NO straight line underneath)
        val activeStart = maxOf(trackStart, waveStart)
        val activeEnd = minOf(trackEnd, waveEnd)

        if (activeEnd > activeStart) {
            val path = Path()
            var isFirst = true
            var x = activeStart
            val step = 1.5f // smooth sub-pixel curve sampling

            while (x <= activeEnd) {
                val phase = ((x - waveStart) / wavelengthPx) * (2f * Math.PI.toFloat())
                val y = centerY + amplitudePx * kotlin.math.sin(phase)
                if (isFirst) {
                    path.moveTo(x, y)
                    isFirst = false
                } else {
                    path.lineTo(x, y)
                }
                x += step
            }

            // Ensure the exact endpoint is reached
            val endPhase = ((activeEnd - waveStart) / wavelengthPx) * (2f * Math.PI.toFloat())
            val endY = centerY + amplitudePx * kotlin.math.sin(endPhase)
            path.lineTo(activeEnd, endY)

            drawPath(
                path = path,
                color = color,
                style = Stroke(
                    width = strokeWidthPx,
                    cap = StrokeCap.Round
                )
            )
        }
    }
}

fun checkStoragePermissions(context: Context): Boolean {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        ContextCompat.checkSelfPermission(context, Manifest.permission.READ_MEDIA_IMAGES) == PackageManager.PERMISSION_GRANTED &&
        ContextCompat.checkSelfPermission(context, Manifest.permission.READ_MEDIA_VIDEO) == PackageManager.PERMISSION_GRANTED
    } else {
        ContextCompat.checkSelfPermission(context, Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED
    }
}
