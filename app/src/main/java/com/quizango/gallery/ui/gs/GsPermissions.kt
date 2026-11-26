package com.quizango.gallery.ui.gs

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color as AndroidColor
import android.graphics.drawable.ColorDrawable
import android.net.Uri
import android.provider.Settings
import android.view.Gravity
import android.view.ViewGroup
import androidx.activity.ComponentActivity
import androidx.activity.compose.LocalActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.window.DialogWindowProvider
import androidx.core.content.ContextCompat
import androidx.core.util.Consumer
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.navigation.NavHostController
import androidx.navigation.compose.currentBackStackEntryAsState
import com.quizango.gallery.GalleryApplication
import com.quizango.gallery.R
import com.quizango.gallery.service.PackageMonitorService
import com.quizango.gallery.ui.navigation.Screen
import com.quizango.gallery.ui.permission.PermissionNeededDialog
import com.quizango.gallery.ui.theme.GalleryText
import com.quizango.gallery.ui.theme.sdp
import com.quizango.gallery.ui.theme.ssp
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

// 2.7.2 also asks for CALL_PHONE; it shares the Phone dialog with READ_PHONE_STATE, which alone delivers PHONE_STATE
internal fun phoneGranted(context: Context): Boolean =
    ContextCompat.checkSelfPermission(context, Manifest.permission.READ_PHONE_STATE) == PackageManager.PERMISSION_GRANTED

private fun overlaySettingsIntent(context: Context) =
    Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:${context.packageName}"))

private enum class Needed { PHONE, OVERLAY }

/**
 * 2.7.2 bu/c (the call permission helper): Phone first, then Display over other apps, each system page with its guide card.
 * A second Phone denial, or two overlay refusals, shows the permission-needed dialog that leads to the system page.
 */
@Composable
internal fun rememberGsPermissionFlow(onGranted: () -> Unit): () -> Unit {
    val context = LocalContext.current
    val activity = LocalActivity.current
    val prefs = (context.applicationContext as GalleryApplication).preferences
    val scope = rememberCoroutineScope()
    var overlayDenyCount by remember { mutableIntStateOf(0) }
    var needed by remember { mutableStateOf<Needed?>(null) }

    val overlayLauncher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) {
        val overlay = Settings.canDrawOverlays(context)
        val phone = phoneGranted(context)
        if (!overlay) overlayDenyCount++
        when {
            overlay && phone -> { overlayDenyCount = 0; onGranted() }
            overlay -> { overlayDenyCount = 0; needed = Needed.PHONE }
            phone -> if (overlayDenyCount >= 2) needed = Needed.OVERLAY
            else -> needed = Needed.PHONE
        }
    }
    val openOverlaySettings = {
        runCatching {
            overlayLauncher.launch(overlaySettingsIntent(context))
            activity?.let { PermissionGuideActivity.showLater(it, PermissionGuideActivity.GUIDE_OVERLAY) }
        }
        Unit
    }
    val appSettingsLauncher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) {
        when {
            !phoneGranted(context) -> needed = Needed.PHONE
            Settings.canDrawOverlays(context) -> onGranted()
            else -> openOverlaySettings()
        }
    }
    val phoneLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { ok ->
        scope.launch {
            if (ok) {
                prefs.setCallPermissionDenyCount(0)
                if (Settings.canDrawOverlays(context)) onGranted() else openOverlaySettings()
                return@launch
            }
            val denials = prefs.callPermissionDenyCount.first() + 1
            prefs.setCallPermissionDenyCount(denials)
            if (denials >= 2) {
                if (Settings.canDrawOverlays(context)) needed = Needed.PHONE else openOverlaySettings()
            }
        }
    }

    needed?.let { kind ->
        PermissionNeededDialog(
            onDismissRequest = { needed = null },
            onGrantAccess = {
                if (kind == Needed.OVERLAY) {
                    openOverlaySettings()
                } else runCatching {
                    appSettingsLauncher.launch(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:${context.packageName}")))
                    activity?.let { PermissionGuideActivity.showLater(it, PermissionGuideActivity.GUIDE_PHONE) }
                }
            }
        )
    }

    return {
        when {
            !phoneGranted(context) -> phoneLauncher.launch(Manifest.permission.READ_PHONE_STATE)
            Settings.canDrawOverlays(context) -> onGranted()
            overlayDenyCount >= 2 -> needed = Needed.OVERLAY
            else -> openOverlaySettings()
        }
    }
}

/** Settings "Quick Info" row (2.7.2 sett_call_setting): Gs Settings once Phone and overlay are on, else dialog_gs_settings first. */
@Composable
fun rememberQuickInfoSettingsOpener(onOpen: () -> Unit): () -> Unit {
    val context = LocalContext.current
    var showDialog by remember { mutableStateOf(false) }
    val flow = rememberGsPermissionFlow(onGranted = onOpen)
    if (showDialog) {
        GsSettingsDialog(onAllow = { showDialog = false; flow() }, onDismiss = { showDialog = false })
    }
    return { if (phoneGranted(context) && Settings.canDrawOverlays(context)) onOpen() else showDialog = true }
}

/** Settings "Install/Uninstall" row (2.7.2 sett_iu_setting): IU Settings once the overlay permission is on, else its system page. */
@Composable
fun rememberInstallSettingsOpener(onOpen: () -> Unit): () -> Unit {
    val context = LocalContext.current
    val activity = LocalActivity.current
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) {
        if (Settings.canDrawOverlays(context)) onOpen()
    }
    return {
        if (Settings.canDrawOverlays(context)) {
            onOpen()
        } else runCatching {
            launcher.launch(overlaySettingsIntent(context))
            activity?.let { PermissionGuideActivity.showLater(it, PermissionGuideActivity.GUIDE_OVERLAY) }
        }
    }
}

private fun Intent.takeGsRoute(): String? = getStringExtra(EXTRA_GS_ROUTE)?.also { removeExtra(EXTRA_GS_ROUTE) }

/**
 * App-wide Phase 11 effects, placed once in the NavHost: opens the route Quick Info, the install screen or the
 * "Quick Info disabled" notification asked for (Home first, as 2.7.2 reopens HomeActivity), and starts the
 * install/uninstall monitor on resume, as 2.7.2's HomeActivity.onResume does.
 */
@Composable
fun GsAppEffects(navController: NavHostController) {
    val context = LocalContext.current
    val activity = LocalActivity.current as? ComponentActivity ?: return
    val scope = rememberCoroutineScope()
    var pending by remember { mutableStateOf(activity.intent?.takeGsRoute()) }
    DisposableEffect(activity) {
        val listener = Consumer<Intent> { intent -> intent.takeGsRoute()?.let { pending = it } }
        activity.addOnNewIntentListener(listener)
        onDispose { activity.removeOnNewIntentListener(listener) }
    }
    val permissionFlow = rememberGsPermissionFlow(onGranted = {})
    val current = navController.currentBackStackEntryAsState().value?.destination?.route
    LaunchedEffect(pending, current) {
        val route = pending ?: return@LaunchedEffect
        if (current != Screen.Home.route) {
            // Under another screen: back to Home first; before Home exists (splash, onboarding) wait for it
            navController.popBackStack(Screen.Home.route, inclusive = false)
            return@LaunchedEffect
        }
        pending = null
        when (route) {
            GS_ROUTE_PERMISSION -> permissionFlow()
            Screen.Home.route -> Unit
            else -> navController.navigate(route) { launchSingleTop = true }
        }
    }
    LifecycleResumeEffect(Unit) {
        scope.launch { PackageMonitorService.startIfAllowed(context) }
        onPauseOrDispose { }
    }
}

/** dialog_gs_settings: bottom sheet over Settings asking to set up the after-call feature. */
@Composable
private fun GsSettingsDialog(onAllow: () -> Unit, onDismiss: () -> Unit) {
    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        val window = (LocalView.current.parent as? DialogWindowProvider)?.window
        SideEffect {
            window?.run {
                setGravity(Gravity.BOTTOM)
                setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
                setBackgroundDrawable(ColorDrawable(AndroidColor.TRANSPARENT))
                setWindowAnimations(R.style.dialog_window_animation)
            }
        }
        Column(
            Modifier
                .fillMaxWidth()
                .background(colorResource(R.color.bg_overlay_dialog_top), RoundedCornerShape(topStart = 30.dp, topEnd = 30.dp))
        ) {
            Box(Modifier.fillMaxWidth()) {
                Image(
                    painterResource(R.drawable.img_overlay_dialog), null,
                    contentScale = ContentScale.FillWidth,
                    modifier = Modifier.fillMaxWidth().padding(top = 25.sdp).padding(horizontal = 10.sdp)
                )
                Image(
                    painterResource(R.drawable.ic_close), stringResource(R.string.close),
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(top = 12.sdp, end = 8.sdp)
                        .clip(RoundedCornerShape(50))
                        .clickable(onClick = onDismiss)
                        .padding(5.sdp)
                )
            }
            Column(
                Modifier.fillMaxWidth().background(colorResource(R.color.backgroundColor)).padding(horizontal = 10.sdp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    stringResource(R.string.improve_your_call_experience), style = GalleryText.BodyXLargeMedium, fontSize = 13.ssp,
                    color = colorResource(R.color.explore_icon_color), textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth().padding(top = 20.sdp)
                )
                Text(
                    stringResource(R.string.enables_smarter_call_amp_app_experience), style = GalleryText.BodyMediumMedium,
                    fontSize = 12.ssp, color = colorResource(R.color.op_onboarding_desc_color), textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth().padding(top = 6.sdp, start = 8.sdp, end = 20.sdp)
                )
                Box(
                    Modifier
                        .padding(horizontal = 12.sdp)
                        .padding(top = 15.sdp)
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(30.sdp))
                        .background(colorResource(R.color.colorPrimary))
                        .clickable(onClick = onAllow)
                        .padding(vertical = 13.sdp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(stringResource(R.string.allow), style = GalleryText.BodyXLargeSemiBold, fontSize = 15.ssp, color = colorResource(R.color.onlyWhite))
                }
                Text(
                    stringResource(R.string.disable_setting), style = GalleryText.BodySmallRegular,
                    color = colorResource(R.color.op_onboarding_desc_color),
                    modifier = Modifier.padding(top = 5.sdp, bottom = 20.sdp).navigationBarsPadding()
                )
            }
        }
    }
}
