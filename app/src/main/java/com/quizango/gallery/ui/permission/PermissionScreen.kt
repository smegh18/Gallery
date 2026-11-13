package com.quizango.gallery.ui.permission

import android.Manifest
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
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
import com.quizango.gallery.ui.navigation.Screen
import com.quizango.gallery.ui.splash.checkStoragePermissions
import com.quizango.gallery.ui.theme.GalleryText
import com.quizango.gallery.ui.theme.sdp
import com.quizango.gallery.ui.theme.ssp
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/**
 * Permission screen reconstructed with 100% visual and behavioral parity
 * against reference APK 2.7.2 `activity_permission.xml`.
 */
@Composable
fun PermissionScreen(
    appPreferences: AppPreferences? = null,
    onNavigate: (String) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var hasPermission by remember { mutableStateOf(checkStoragePermissions(context)) }
    var showDialog by remember { mutableStateOf(false) }

    fun navigateNext() {
        if (appPreferences != null) {
            scope.launch {
                val completed = appPreferences.hasCompletedOnboarding.first()
                if (!completed) {
                    onNavigate(Screen.FeaturePromo.route)
                } else {
                    onNavigate(Screen.Home.route)
                }
            }
        } else {
            onNavigate(Screen.FeaturePromo.route)
        }
    }

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { result ->
        if (result.values.all { it }) {
            hasPermission = true
            navigateNext()
        }
    }

    val allFilesLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) {
        if (checkStoragePermissions(context)) {
            hasPermission = true
            navigateNext()
        }
    }

    fun requestPermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            try {
                val intent = Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION).apply {
                    data = Uri.parse("package:${context.packageName}")
                }
                allFilesLauncher.launch(intent)
            } catch (e: Exception) {
                val intent = Intent(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION)
                allFilesLauncher.launch(intent)
            }
        } else {
            launcher.launch(
                arrayOf(
                    Manifest.permission.READ_EXTERNAL_STORAGE,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE
                )
            )
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
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Top Image (ic_permission_top with alpha 0.7)
            Image(
                painter = painterResource(id = R.drawable.ic_permission_top),
                contentDescription = null,
                contentScale = ContentScale.FillWidth,
                modifier = Modifier
                    .fillMaxWidth()
                    .alpha(0.7f)
            )

            // Mock Settings Dialog Card (ic_permission_top_dialog background, 235sdp width)
            Box(
                modifier = Modifier
                    .padding(top = 20.sdp)
                    .width(235.sdp)
                    .clip(RoundedCornerShape(topStart = 25.dp, topEnd = 25.dp))
                    .background(colorResource(R.color.dialogBack))
                    .padding(horizontal = 10.dp, vertical = 15.dp)
            ) {
                Column {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(horizontal = 8.sdp, vertical = 8.sdp)
                    ) {
                        Image(
                            painter = painterResource(id = R.drawable.ic_expand_arrow),
                            contentDescription = null,
                            modifier = Modifier
                                .size(30.dp)
                                .rotate(270f)
                                .padding(4.dp)
                        )
                        Spacer(modifier = Modifier.width(4.sdp))
                        Text(
                            text = stringResource(id = R.string.all_files_access),
                            style = GalleryText.BodyMediumSemiBold,
                            color = colorResource(R.color.per_text_color),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(horizontal = 8.sdp, vertical = 12.sdp)
                    ) {
                        Image(
                            painter = painterResource(id = R.drawable.ic_logo),
                            contentDescription = null,
                            modifier = Modifier.height(45.dp)
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

            // Toggle Bar (ic_permission_bg, 80dp height, -25dp offset overlap)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 5.sdp)
                    .offset(y = (-25).dp)
                    .height(80.dp)
                    .zIndex(1f)
            ) {
                Image(
                    painter = painterResource(id = R.drawable.ic_permission_bg),
                    contentDescription = null,
                    contentScale = ContentScale.FillBounds,
                    modifier = Modifier.fillMaxSize()
                )

                Text(
                    text = stringResource(id = R.string.allow_access_to_manage_all_files),
                    style = GalleryText.BodyXLargeSemiBold,
                    fontSize = 12.ssp,
                    color = colorResource(R.color.per_text_color),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier
                        .align(Alignment.CenterStart)
                        .padding(start = 18.sdp, end = 65.sdp)
                )

                // Hand Lottie: 50sdp, -53dp offset
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
                        .offset(y = (-28).dp)
                        .padding(end = 25.sdp)
                        .size(50.sdp)
                        .zIndex(2f)
                )
            }

            // Headings & Text
            Text(
                text = stringResource(id = R.string.permission_required),
                style = GalleryText.BodyXXLargeSemiBold,
                fontSize = 18.ssp,
                color = colorResource(R.color.per_text_color),
                modifier = Modifier.padding(bottom = 5.dp)
            )

            Text(
                text = stringResource(id = R.string.allow_storage_permission),
                style = GalleryText.BodyMediumMedium,
                fontSize = 11.ssp,
                color = colorResource(R.color.gray_permission),
                modifier = Modifier.padding(top = 3.sdp, bottom = 5.sdp)
            )

            // Grant Access Button
            Button(
                onClick = { requestPermissions() },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 15.sdp, vertical = 12.sdp)
                    .height(50.sdp),
                shape = RoundedCornerShape(25.sdp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = colorResource(R.color.colorPrimary)
                )
            ) {
                Text(
                    text = stringResource(id = R.string.grant_access),
                    style = GalleryText.BodyXLargeSemiBold,
                    color = colorResource(R.color.onlyWhite)
                )
            }

            // About this permission link
            Text(
                text = stringResource(id = R.string.about_this_permission),
                style = GalleryText.BodySmallSemiBold,
                color = colorResource(R.color.titleColor),
                modifier = Modifier
                    .clickable { showDialog = true }
                    .padding(vertical = 12.sdp)
            )

            Spacer(modifier = Modifier.navigationBarsPadding())
        }

        if (showDialog) {
            PermissionNeededDialog(
                onDismissRequest = { showDialog = false },
                onGrantAccess = { requestPermissions() }
            )
        }
    }
}
