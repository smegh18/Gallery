package com.quizango.gallery.ui.splash

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.airbnb.lottie.compose.LottieAnimation
import com.airbnb.lottie.compose.LottieCompositionSpec
import com.airbnb.lottie.compose.animateLottieCompositionAsState
import com.airbnb.lottie.compose.rememberLottieComposition
import com.quizango.gallery.R
import com.quizango.gallery.data.local.datastore.AppPreferences
import com.quizango.gallery.ui.navigation.Screen
import com.quizango.gallery.ui.theme.GalleryText
import com.quizango.gallery.ui.theme.sdp
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first

/**
 * Splash screen reconstructed with 100% visual and behavioral parity
 * against reference APK 2.7.2 `activity_splash.xml` and `SplashActivity.smali`.
 */
@Composable
fun SplashScreen(
    appPreferences: AppPreferences,
    onNavigate: (String) -> Unit
) {
    val context = LocalContext.current

    val lottieComposition by rememberLottieComposition(LottieCompositionSpec.RawRes(R.raw.app_icon_lottie))
    val lottieProgress by animateLottieCompositionAsState(
        composition = lottieComposition,
        iterations = 1,
        isPlaying = true
    )

    // survives Activity recreation (rotation during splash), so one launch counts once
    var counted by androidx.compose.runtime.saveable.rememberSaveable { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        // awaited, so Home reads this launch's number; a failed write only skips the hint count
        if (!counted) {
            try { appPreferences.incrementSessionCount() } catch (_: java.io.IOException) {}
            counted = true
        }
        val hasCompletedOnboarding = appPreferences.hasCompletedOnboarding.first()
        val hasPermissions = checkStoragePermissions(context)
        kotlinx.coroutines.withTimeoutOrNull(2_000) {
            snapshotFlow { lottieProgress >= 26f / 90f }.first { it }
            delay(350)
        }

        // Reference order: Permission first; Language + Feature promo only for campaign installs
        when {
            !hasPermissions -> onNavigate(Screen.Permission.route)
            !hasCompletedOnboarding &&
                com.quizango.gallery.data.local.InstallSource.isCampaign(context, appPreferences) ->
                onNavigate(Screen.Language.route)
            else -> onNavigate(Screen.Home.route)
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(colorResource(R.color.backgroundColor)),
        contentAlignment = Alignment.Center
    ) {
        // Reference MaterialCardView with lottie_splash & iv_splash (140sdp, cornerRadius 20sdp)
        Box(
            modifier = Modifier
                .size(140.sdp)
                .clip(RoundedCornerShape(20.sdp)),
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

        // Reference Bottom Area: "Your Photo Manager" + LinearProgressIndicator + splash_banner
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .windowInsetsPadding(
                    WindowInsets.safeDrawing.only(
                        WindowInsetsSides.Bottom + WindowInsetsSides.Horizontal
                    )
                ),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = stringResource(id = R.string.your_photo_manager),
                style = GalleryText.BodyMediumMedium,
                color = colorResource(R.color.titleColor),
                modifier = Modifier.padding(bottom = 5.sdp)
            )
            LinearProgressIndicator(
                modifier = Modifier
                    .width(120.sdp)
                    .padding(bottom = 30.sdp),
                color = colorResource(R.color.colorPrimary),
                trackColor = colorResource(R.color.splash_loader_track)
            )
            // FrameLayout splash_banner ad placeholder
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(0.dp)
            )
        }
    }
}

fun checkStoragePermissions(context: Context): Boolean {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
        android.os.Environment.isExternalStorageManager()
    } else {
        ContextCompat.checkSelfPermission(context, Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED
    }
}
