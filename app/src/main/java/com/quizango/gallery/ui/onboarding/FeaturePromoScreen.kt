package com.quizango.gallery.ui.onboarding

import android.Manifest
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.LocalActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import com.airbnb.lottie.compose.LottieAnimation
import com.airbnb.lottie.compose.LottieCompositionSpec
import com.airbnb.lottie.compose.LottieConstants
import com.airbnb.lottie.compose.animateLottieCompositionAsState
import com.airbnb.lottie.compose.rememberLottieComposition
import com.quizango.gallery.R
import com.quizango.gallery.data.local.datastore.AppPreferences
import com.quizango.gallery.ui.gs.PermissionGuideActivity
import com.quizango.gallery.ui.theme.GalleryText
import com.quizango.gallery.ui.theme.sdp
import com.quizango.gallery.ui.theme.ssp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Feature Promo screen ("Get more from your Gallery") matching reference 2.7.2
 * `activity_feature_promo.xml` (checklist #5, ONB-05).
 */
@Composable
fun FeaturePromoScreen(
    appPreferences: AppPreferences,
    onNavigateToHome: () -> Unit
) {
    val context = LocalContext.current
    val activity = LocalActivity.current
    val scope = rememberCoroutineScope()
    var showSkipButton by remember { mutableStateOf(false) }

    // "Not now" button appears after 2 seconds per reference postDelayed
    LaunchedEffect(Unit) {
        delay(2000)
        showSkipButton = true
    }

    val overlayLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) {
        val hasOverlay = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            Settings.canDrawOverlays(context)
        } else {
            true
        }
        if (hasOverlay) {
            scope.launch {
                appPreferences.setOnboardingCompleted(true)
                onNavigateToHome()
            }
        }
    }

    val phonePermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            // After phone permission granted, check overlay permission
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !Settings.canDrawOverlays(context)) {
                val intent = Intent(
                    Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                    Uri.parse("package:${context.packageName}")
                )
                overlayLauncher.launch(intent)
                activity?.let { PermissionGuideActivity.showLater(it, PermissionGuideActivity.GUIDE_OVERLAY) } // Phase 11: 2.7.2 overlay guide
            } else {
                scope.launch {
                    appPreferences.setOnboardingCompleted(true)
                    onNavigateToHome()
                }
            }
        } else {
            // Denied: don't leave the user stuck on this screen
            scope.launch {
                appPreferences.setOnboardingCompleted(true)
                onNavigateToHome()
            }
        }
    }

    fun handleEnableFeatures() {
        phonePermissionLauncher.launch(Manifest.permission.READ_PHONE_STATE)
    }

    fun handleSkip() {
        scope.launch {
            appPreferences.setOnboardingCompleted(true)
            onNavigateToHome()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(colorResource(R.color.ic_text_button))
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
        ) {
            // Top Hero Image (img_feature_promo_top, 1125x1005 webp)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(280.dp)
            ) {
                Image(
                    painter = painterResource(id = R.drawable.img_feature_promo_top),
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    alignment = Alignment.TopCenter,
                    modifier = Modifier.fillMaxSize()
                )
            }

            // Bottom Sheet Container (bg_onb_feature_sheet, 24dp top corners, 28dp overlap onto hero)
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .offset(y = (-28).dp)
                    .shadow(elevation = 6.sdp, shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)),
                shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
                color = colorResource(R.color.ic_text_button)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.sdp)
                        .padding(top = 16.sdp, bottom = 10.sdp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Mock Settings Dialog Card (ic_permission_top_dialog background, 210sdp width)
                    Box(
                        modifier = Modifier
                            .width(210.sdp)
                            .clip(RoundedCornerShape(topStart = 25.dp, topEnd = 25.dp))
                            .background(colorResource(R.color.dialogBack))
                            .padding(start = 10.dp, top = 12.dp, end = 10.dp, bottom = 26.dp)
                    ) {
                        Column {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(start = 6.sdp, top = 4.sdp)
                            ) {
                                Image(
                                    painter = painterResource(id = R.drawable.ic_expand_arrow),
                                    contentDescription = null,
                                    modifier = Modifier
                                        .size(26.dp)
                                        .rotate(270f)
                                        .padding(4.dp)
                                )
                                Spacer(modifier = Modifier.width(4.sdp))
                                Text(
                                    text = stringResource(id = R.string.onb_feature_mock_title),
                                    style = GalleryText.BodyMediumSemiBold,
                                    color = colorResource(R.color.per_text_color),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }

                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(start = 6.sdp, top = 8.sdp)
                            ) {
                                Image(
                                    painter = painterResource(id = R.drawable.ic_logo),
                                    contentDescription = null,
                                    modifier = Modifier.height(36.dp)
                                )
                                Spacer(modifier = Modifier.width(8.sdp))
                                Text(
                                    text = stringResource(id = R.string.app_name),
                                    style = GalleryText.BodyLargeSemiBold,
                                    color = colorResource(R.color.per_text_color)
                                )
                            }
                        }
                    }

                    // Toggle Bar (ic_permission_bg, 64dp height, -22dp offset overlap)
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .offset(y = (-22).dp)
                            .height(64.dp)
                            .zIndex(1f)
                    ) {
                        Image(
                            painter = painterResource(id = R.drawable.ic_permission_bg),
                            contentDescription = null,
                            contentScale = ContentScale.FillBounds,
                            modifier = Modifier.fillMaxSize()
                        )

                        Text(
                            text = stringResource(id = R.string.onb_feature_mock_toggle),
                            style = GalleryText.BodyXLargeSemiBold,
                            fontSize = 11.ssp,
                            color = colorResource(R.color.per_text_color),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier
                                .align(Alignment.CenterStart)
                                .padding(start = 16.sdp, end = 65.sdp)
                        )

                        // Hand Lottie: 46sdp, -40dp offset
                        val composition by rememberLottieComposition(LottieCompositionSpec.RawRes(R.raw.permission))
                        val progress by animateLottieCompositionAsState(
                            composition = composition,
                            iterations = LottieConstants.IterateForever
                        )
                        LottieAnimation(
                            composition = composition,
                            progress = { progress },
                            modifier = Modifier
                                .align(Alignment.CenterEnd)
                                .offset(y = 46.sdp / 2 - 8.dp) // top at row bottom - 40dp (64dp row), as in 2.7.2
                                .padding(end = 18.sdp)
                                .size(46.sdp)
                                .zIndex(2f)
                        )
                    }

                    // Heading: "Get more from your Gallery" (17ssp, BodyXXLargeSemiBold, color black)
                    Text(
                        text = stringResource(id = R.string.onb_feature_title),
                        style = GalleryText.BodyXXLargeSemiBold,
                        fontSize = 17.ssp,
                        color = colorResource(R.color.black),
                        textAlign = TextAlign.Center,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 14.sdp)
                    )

                    // Subtitle: "Turn on these features to reach your photos faster"
                    Text(
                        text = stringResource(id = R.string.onb_feature_subtitle),
                        style = GalleryText.BodyMediumRegular,
                        fontSize = 11.ssp,
                        color = colorResource(R.color.op_onboarding_ac_prem_desc),
                        textAlign = TextAlign.Center,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 3.sdp)
                    )

                    // Chips Row ("After-call", "Shortcut", "App tips")
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 10.sdp),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        FeatureChip(
                            iconRes = R.drawable.ic_gs_call,
                            textRes = R.string.onb_feature_chip_call
                        )
                        Spacer(modifier = Modifier.width(6.sdp))
                        FeatureChip(
                            iconRes = R.drawable.ic_floating_btn,
                            textRes = R.string.onb_feature_chip_shortcut
                        )
                        Spacer(modifier = Modifier.width(6.sdp))
                        FeatureChip(
                            iconRes = R.drawable.explore_tab_ic_cleaner,
                            textRes = R.string.onb_feature_chip_tips
                        )
                    }

                    // Permission Note
                    Text(
                        text = stringResource(id = R.string.onb_feature_permission_note),
                        style = GalleryText.BodyMediumRegular,
                        fontSize = 10.ssp,
                        color = colorResource(R.color.op_onboarding_desc_color),
                        textAlign = TextAlign.Center,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 10.sdp)
                    )

                    // "Enable features" Button (15ssp, BodyXLargeSemiBold, btn_permission)
                    Button(
                        onClick = { handleEnableFeatures() },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 10.sdp)
                            .height(50.sdp),
                        shape = RoundedCornerShape(25.sdp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = colorResource(R.color.colorPrimary)
                        )
                    ) {
                        Text(
                            text = stringResource(id = R.string.onb_feature_enable),
                            style = GalleryText.BodyXLargeSemiBold,
                            fontSize = 15.ssp,
                            color = colorResource(R.color.onlyWhite)
                        )
                    }

                    // "Not now" Skip Button (appears after 2s delay)
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(40.dp)
                            .padding(top = 2.sdp),
                        contentAlignment = Alignment.Center
                    ) {
                        if (showSkipButton) {
                            Text(
                                text = stringResource(id = R.string.onb_feature_skip),
                                style = GalleryText.BodyMediumRegular,
                                fontSize = 12.ssp,
                                color = colorResource(R.color.op_onboarding_desc_color),
                                modifier = Modifier
                                    .clickable { handleSkip() }
                                    .padding(8.sdp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.navigationBarsPadding())
                }
            }
        }
    }
}

@Composable
private fun FeatureChip(
    iconRes: Int,
    textRes: Int
) {
    Surface(
        shape = CircleShape,
        color = colorResource(R.color.iu_primary_soft)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(start = 8.sdp, end = 10.sdp, top = 6.dp, bottom = 6.dp)
        ) {
            Icon(
                painter = painterResource(id = iconRes),
                contentDescription = null,
                tint = colorResource(R.color.iu_primary),
                modifier = Modifier.size(14.sdp)
            )
            Spacer(modifier = Modifier.width(4.sdp))
            Text(
                text = stringResource(id = textRes),
                style = GalleryText.BodySmallMedium,
                color = colorResource(R.color.iu_primary),
                maxLines = 1
            )
        }
    }
}
