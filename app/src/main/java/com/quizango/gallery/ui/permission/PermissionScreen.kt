package com.quizango.gallery.ui.permission

import android.Manifest
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import com.airbnb.lottie.compose.LottieAnimation
import com.airbnb.lottie.compose.LottieCompositionSpec
import com.airbnb.lottie.compose.LottieConstants
import com.airbnb.lottie.compose.animateLottieCompositionAsState
import com.airbnb.lottie.compose.rememberLottieComposition
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.quizango.gallery.R
import com.quizango.gallery.ui.navigation.Screen
import com.quizango.gallery.ui.splash.checkStoragePermissions
import com.quizango.gallery.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PermissionScreen(
    onNavigate: (String) -> Unit
) {
    val context = LocalContext.current
    var hasPermission by remember { mutableStateOf(checkStoragePermissions(context)) }
    var showAboutDialog by remember { mutableStateOf(false) }

    val permissionsToRequest = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        arrayOf(
            Manifest.permission.READ_MEDIA_IMAGES,
            Manifest.permission.READ_MEDIA_VIDEO
        )
    } else {
        arrayOf(
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.WRITE_EXTERNAL_STORAGE
        )
    }

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { result ->
        val granted = result.values.all { it }
        if (granted) {
            hasPermission = true
            onNavigate(Screen.Home.route)
        }
    }

    LaunchedEffect(Unit) {
        if (hasPermission) {
            onNavigate(Screen.Home.route)
        }
    }

    val permissionTextColor = MaterialTheme.colorScheme.onSurface
    val permissionGrayColor = MaterialTheme.colorScheme.onSurfaceVariant

    Scaffold(
        containerColor = MaterialTheme.colorScheme.screenBackground
    ) { paddingValues ->
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(MaterialTheme.colorScheme.screenBackground)
        ) {
            val screenHeight = maxHeight
            val guidelinePercent = 0.35f
            val guidelineTop = screenHeight * guidelinePercent

            // Top Decorative Graphic (app:layout_constraintTop_toTopOf="parent", alpha 0.7)
            Image(
                painter = painterResource(id = R.drawable.ic_permission_top),
                contentDescription = null,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(guidelineTop)
                    .alpha(0.7f),
                contentScale = ContentScale.FillWidth
            )

            // Main Content Area below guideline (matching ll_bottomview in activity_permission.xml)
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(top = guidelineTop)
                    .navigationBarsPadding(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // 1. Top Card (ConstraintLayout with ic_permission_top_dialog: width 235sdp (~300dp), marginTop 20sdp (~24dp))
                Box(
                    modifier = Modifier
                        .padding(top = 10.dp)
                        .width(300.dp)
                        .clip(RoundedCornerShape(topStart = 25.dp, topEnd = 25.dp))
                        .border(
                            width = 9.dp,
                            color = Color(0xFFCCE0FF),
                            shape = RoundedCornerShape(topStart = 25.dp, topEnd = 25.dp)
                        )
                        .background(MaterialTheme.colorScheme.surface)
                        .padding(horizontal = 10.dp, vertical = 15.dp)
                ) {
                    Column {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                painter = painterResource(id = R.drawable.ic_expand_arrow),
                                contentDescription = null,
                                tint = permissionTextColor,
                                modifier = Modifier
                                    .size(24.dp)
                                    .rotate(270f)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = stringResource(id = R.string.all_files_access),
                                fontSize = 14.5.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = permissionTextColor
                            )
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Image(
                                painter = painterResource(id = R.drawable.app_icon_splash),
                                contentDescription = null,
                                modifier = Modifier
                                    .size(45.dp)
                                    .clip(RoundedCornerShape(9.dp))
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = stringResource(id = R.string.app_name),
                                fontSize = 16.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = permissionTextColor
                            )
                        }

                        Spacer(modifier = Modifier.height(16.dp))
                    }
                }

                // 2. Simulated Toggle Card (rl_permission: height 80dp, marginTop -25dp, marginHorizontal 6dp, background ic_permission_bg)
                // AND 3. hand_lottie (width 60dp, height 60dp, marginTop -53dp -> top at 80 - 53 = 27dp, marginEnd 28dp)
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 6.dp)
                        .offset(y = (-25).dp)
                        .height(80.dp)
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.ic_permission_bg),
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.FillBounds
                    )

                    Text(
                        text = stringResource(id = R.string.allow_access_to_manage_all_files),
                        color = permissionTextColor,
                        fontSize = 14.5.sp,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier
                            .align(Alignment.CenterStart)
                            .padding(start = 20.dp, end = 75.dp)
                    )

                    // Authentic Lottie animated switch toggle & hand pointer (Reference @raw/permission)
                    // In activity_permission.xml: hand_lottie follows rl_permission with marginTop="-53dp", layout_gravity="end", marginEnd="@dimen/_25sdp"
                    val composition by rememberLottieComposition(LottieCompositionSpec.RawRes(R.raw.permission))
                    val progress by animateLottieCompositionAsState(
                        composition = composition,
                        iterations = LottieConstants.IterateForever
                    )

                    LottieAnimation(
                        composition = composition,
                        progress = { progress },
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(end = 34.dp)
                            .offset(y = 19.dp)
                            .size(60.dp)
                    )
                }

                // 4. Flexible Spacer
                Spacer(modifier = Modifier.weight(1f))

                // 5. Title: "Permission Required"
                Text(
                    text = stringResource(id = R.string.permission_required),
                    fontSize = 23.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = permissionTextColor,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(5.dp))

                // 6. Subtitle: "Allow Storage Permission \nto access your Photos and Videos."
                Text(
                    text = stringResource(id = R.string.allow_storage_permission),
                    fontSize = 13.5.sp,
                    color = permissionGrayColor,
                    textAlign = TextAlign.Center,
                    lineHeight = 19.sp
                )

                Spacer(modifier = Modifier.height(18.dp))

                // 7. Primary CTA Button: "Grant Access" (btnAllow: height 52dp, marginHorizontal 15dp)
                Button(
                    onClick = {
                        launcher.launch(permissionsToRequest)
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                        .height(52.dp),
                    shape = RoundedCornerShape(26.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = GalleryBlue)
                ) {
                    Text(
                        text = stringResource(id = R.string.grant_access),
                        fontSize = 16.5.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color.White
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                // 8. Footer Text: "About this permission"
                Text(
                    text = stringResource(id = R.string.about_this_permission),
                    modifier = Modifier
                        .clickable { showAboutDialog = true }
                        .padding(vertical = 8.dp, horizontal = 16.dp),
                    style = TextStyle(
                        textDecoration = TextDecoration.Underline,
                        color = permissionTextColor,
                        fontSize = 13.5.sp,
                        fontWeight = FontWeight.SemiBold
                    ),
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(16.dp))
            }

            // About This Permission Modal Bottom Sheet
            if (showAboutDialog) {
                ModalBottomSheet(
                    onDismissRequest = { showAboutDialog = false },
                    containerColor = MaterialTheme.colorScheme.surface,
                    shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
                    dragHandle = null
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp)
                            .padding(top = 16.dp, bottom = 32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Box(modifier = Modifier.fillMaxWidth()) {
                            IconButton(
                                onClick = { showAboutDialog = false },
                                modifier = Modifier.align(Alignment.TopEnd)
                            ) {
                                Icon(
                                    painter = painterResource(id = R.drawable.ic_permission_close),
                                    contentDescription = "Close",
                                    tint = Color.Unspecified,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                        }

                        Image(
                            painter = painterResource(id = R.drawable.permission_about_img),
                            contentDescription = null,
                            modifier = Modifier.size(54.dp)
                        )

                        Spacer(modifier = Modifier.height(18.dp))

                        Text(
                            text = stringResource(id = R.string.about_permission_title),
                            fontSize = 18.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = permissionTextColor,
                            textAlign = TextAlign.Start,
                            modifier = Modifier.fillMaxWidth()
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        Text(
                            text = stringResource(id = R.string.about_permission_description),
                            fontSize = 13.sp,
                            color = permissionGrayColor,
                            lineHeight = 19.sp,
                            textAlign = TextAlign.Start,
                            modifier = Modifier.fillMaxWidth()
                        )

                        Spacer(modifier = Modifier.height(24.dp))

                        Button(
                            onClick = {
                                showAboutDialog = false
                                val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                                    data = Uri.fromParts("package", context.packageName, null)
                                }
                                context.startActivity(intent)
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(54.dp),
                            shape = RoundedCornerShape(27.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = GalleryBlue)
                        ) {
                            Text(
                                text = stringResource(id = R.string.open_settings),
                                fontSize = 16.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Color.White
                            )
                        }
                    }
                }
            }
        }
    }
}
