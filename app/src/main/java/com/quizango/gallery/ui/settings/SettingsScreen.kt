package com.quizango.gallery.ui.settings

import android.Manifest
import android.app.AppOpsManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color as AndroidColor
import android.graphics.drawable.ColorDrawable
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.Uri
import android.os.Build
import android.os.Process
import android.provider.Settings
import android.view.Gravity
import android.view.ViewGroup
import android.widget.Toast
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.window.DialogWindowProvider
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import com.quizango.gallery.R
import com.quizango.gallery.data.local.InstallSource
import com.quizango.gallery.data.local.datastore.AppPreferences
import com.quizango.gallery.ui.browse.SharedMediaToolbar
import com.quizango.gallery.ui.home.drawableBackground
import com.quizango.gallery.ui.navigation.Screen
import com.quizango.gallery.ui.theme.GalleryText
import com.quizango.gallery.ui.theme.Inter
import com.quizango.gallery.ui.theme.sdp
import kotlinx.coroutines.launch

// shortcut: no-op until Phase 12 (USG-01) wires dialog_usage_access_settings
internal fun recordSettingsUsageAccessTrigger() = Unit

internal enum class UsageToggle { TURN_OFF, TURN_ON, ASK_ACCESS }

/** SettingActivity.OoooOOo: a checked switch turns sharing off; otherwise usage access decides between on and the prompt. */
internal fun usageToggle(checked: Boolean, accessGranted: Boolean): UsageToggle = when {
    checked -> UsageToggle.TURN_OFF
    accessGranted -> UsageToggle.TURN_ON
    else -> UsageToggle.ASK_ACCESS
}

/** Oooooo0: the switch is on only while sharing is allowed and usage access is granted. */
internal fun usageSwitchOn(enabled: Boolean, accessGranted: Boolean): Boolean = enabled && accessGranted

/** common_data.overlay_unsupported_devices default, as "brand_model" in lower case (PermissionsKt.OooO0OO). */
internal val OVERLAY_UNSUPPORTED_DEVICES =
    setOf("poco_2312bpc51h", "redmi_23129rn51h", "redmi_23129rn51x", "poco_2302epcc4i", "motorola_moto e13")

/** Oooooo: the floating-button row needs Android 9+ and a device that can draw over other apps. */
internal fun floatingRowVisible(sdk: Int, brand: String, model: String): Boolean =
    sdk >= 28 && "${brand}_$model".lowercase().trim() !in OVERLAY_UNSUPPORTED_DEVICES

/** dialog_rate_us (jt/e.HISPj7KHQ7): 1-2 stars say sorry, 3-5 thank; the big star art follows the rating. */
@StringRes
internal fun rateTitle(rating: Int): Int = if (rating <= 2) R.string.thanks_for_your_support_1 else R.string.thanks_for_your_support

@DrawableRes
internal fun rateStar(rating: Int): Int = RATE_STARS[rating.coerceIn(1, 5) - 1]

private val RATE_STARS = listOf(
    R.drawable.ic_rate_star_1, R.drawable.ic_rate_star_2, R.drawable.ic_rate_star_3, R.drawable.ic_rate_star_4, R.drawable.ic_rate_star_5
)

// 2.7.2 has no string resource for this share text (dl/b case 12)
private fun shareText(packageName: String) =
    " ".repeat(24) + "\nLet me recommend you this application\n\nhttps://play.google.com/store/apps/details?id=$packageName"

// shortcut: 2.7.2's Play games / Quiz rows open playzen.io (reference brand, omitted); they open the Play Store instead
private const val GAMES_URL = "https://play.google.com/store/apps"

/** s9/k.Oooo000: Wi-Fi, mobile or ethernet. */
private fun networkAvailable(context: Context): Boolean {
    val cm = context.getSystemService(ConnectivityManager::class.java) ?: return false
    val caps = cm.getNetworkCapabilities(cm.activeNetwork) ?: return false
    return caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) || caps.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) ||
        caps.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET)
}

/** PermissionsKt.DxDJysLV5r: usage access (android:get_usage_stats) is allowed for this app. */
private fun usageAccessGranted(context: Context): Boolean = runCatching {
    val ops = context.getSystemService(AppOpsManager::class.java)
    @Suppress("DEPRECATION")
    val mode = if (Build.VERSION.SDK_INT >= 29) ops.unsafeCheckOpNoThrow(AppOpsManager.OPSTR_GET_USAGE_STATS, Process.myUid(), context.packageName)
    else ops.checkOpNoThrow(AppOpsManager.OPSTR_GET_USAGE_STATS, Process.myUid(), context.packageName)
    if (mode == AppOpsManager.MODE_DEFAULT) context.checkCallingOrSelfPermission(Manifest.permission.PACKAGE_USAGE_STATS) == PackageManager.PERMISSION_GRANTED
    else mode == AppOpsManager.MODE_ALLOWED
}.getOrDefault(false)

private fun granted(context: Context, permission: String) =
    context.checkSelfPermission(permission) == PackageManager.PERMISSION_GRANTED

private fun open(context: Context, url: String) {
    runCatching { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url))) }
        .onFailure { Toast.makeText(context, R.string.browser_not_available, Toast.LENGTH_SHORT).show() }
}

/**
 * activity_setting (SettingActivity) for a user without the ad-free purchase: the Go Ads Free banner, then the rows in
 * 2.7.2 order. My Subscription (premium only) and Revoke consent (UMP privacy form, no consent SDK here) stay hidden,
 * as 2.7.2 hides them for that user; their trailing dividers stay, as in the layout.
 */
@Composable
fun SettingsScreen(
    appPreferences: AppPreferences,
    onNavigate: (String) -> Unit,
    onNavigateBack: () -> Unit,
    onOpenQuickInfoSettings: () -> Unit,
    onOpenFloatingSettings: () -> Unit,
    onOpenInstallSettings: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val autoDelete by appPreferences.autoDeleteTrash.collectAsState(initial = false)
    val insightsEnabled by appPreferences.appInsightsEnabled.collectAsState(initial = true)
    var showRateUs by remember { mutableStateOf(false) }
    // GS and IU rows show only to campaign installs (s9/ao.OoooO with the default organic_user_controller)
    var campaign by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { campaign = InstallSource.isCampaign(context, appPreferences) }
    // onResume re-reads what the user can change outside the app: network, overlay, phone permissions, usage access
    var resumes by remember { mutableIntStateOf(0) }
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { resumes++ }
    val online = remember(resumes) { networkAvailable(context) }
    val canDrawOverlays = remember(resumes) { Settings.canDrawOverlays(context) }
    val phoneGranted = remember(resumes) {
        granted(context, Manifest.permission.CALL_PHONE) && granted(context, Manifest.permission.READ_PHONE_STATE)
    }
    val usageAccess = remember(resumes) { usageAccessGranted(context) }
    var awaitingUsageAccess by remember { mutableStateOf(false) }
    // dl/e case 0: access granted from the prompt turns sharing back on
    LaunchedEffect(usageAccess) {
        if (awaitingUsageAccess && usageAccess) {
            awaitingUsageAccess = false
            appPreferences.setAppInsightsEnabled(true)
        }
    }
    val usageOn = usageSwitchOn(insightsEnabled, usageAccess)
    val floatingVisible = floatingRowVisible(Build.VERSION.SDK_INT, Build.BRAND.orEmpty(), Build.MODEL.orEmpty())

    fun toggleUsage() {
        when (usageToggle(usageOn, usageAccess)) {
            UsageToggle.TURN_OFF -> {
                scope.launch { appPreferences.setAppInsightsEnabled(false) }
                Toast.makeText(context, R.string.setting_usage_data_off_toast, Toast.LENGTH_SHORT).show()
            }
            UsageToggle.TURN_ON -> scope.launch { appPreferences.setAppInsightsEnabled(true) }
            UsageToggle.ASK_ACCESS -> {
                awaitingUsageAccess = true
                recordSettingsUsageAccessTrigger()
            }
        }
    }

    fun share() = runCatching {
        val send = Intent(Intent.ACTION_SEND)
            .setType("text/plain")
            .putExtra(Intent.EXTRA_SUBJECT, context.getString(R.string.app_name))
            .putExtra(Intent.EXTRA_TEXT, shareText(context.packageName))
        context.startActivity(Intent.createChooser(send, context.getString(R.string.choose_one)))
    }

    Column(Modifier.fillMaxSize().background(colorResource(R.color.backgroundColor))) {
        SharedMediaToolbar(title = stringResource(R.string.settings), onBack = onNavigateBack)
        Column(
            Modifier
                .fillMaxWidth()
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(top = 3.dp + 5.sdp, bottom = 10.sdp)
                .navigationBarsPadding()
        ) {
            GoProBanner(onClick = { onNavigate(Screen.Pro.route) })
            SettingRow(R.drawable.ic_theme_mode, R.string.theme_mode, Modifier.padding(top = 5.sdp)) { onNavigate(Screen.Theme.route) }
            SettingDivider()
            if (campaign && online) {
                SettingRow(R.drawable.ic_phone, R.string.gs_feature, dot = !(phoneGranted && canDrawOverlays), dotCentered = false,
                    onClick = onOpenQuickInfoSettings)
                SettingDivider()
            }
            SettingRow(R.drawable.ic_unhide, R.string.private_vault) { onNavigate(Screen.VaultLock.route) }
            SettingDivider()
            SettingRow(R.drawable.ic_app_language, R.string.app_languages) { onNavigate(Screen.Language.route) }
            SettingDivider()
            if (floatingVisible) {
                SettingRow(R.drawable.ic_floating_btn, R.string.quick_access_settings, onClick = onOpenFloatingSettings)
                SettingDivider()
            }
            if (campaign) SettingRow(R.drawable.ic_iu_control, R.string.iu_monitoring, dot = !canDrawOverlays, dotCentered = true,
                onClick = onOpenInstallSettings)
            SettingDivider()
            SettingRow(R.drawable.ic_about, R.string.about_us) { onNavigate(Screen.About.route) }
            SettingDivider()
            SettingDivider() // revokeLay's divider stays under the hidden row
            SettingRow(R.drawable.ic_rate, R.string.rate_us_with_star, arrow = false) {
                if (networkAvailable(context)) showRateUs = true
                else Toast.makeText(context, R.string.please_turn_on_internet_and_try_again, Toast.LENGTH_SHORT).show()
            }
            SettingDivider()
            SettingRow(R.drawable.ic_setting_share, R.string.share, arrow = false) { share() }
            SettingDivider()
            SettingRow(R.drawable.ic_game_setting, R.string.play_games, arrow = false) { open(context, GAMES_URL) }
            SettingDivider() // gameDivider: quizzone_setting.enable (default true)
            SettingRow(R.drawable.ic_quiz_setting, R.string.quiz, arrow = false) { open(context, GAMES_URL) }
            SettingDivider()
            Text(
                text = stringResource(R.string.recycle_bin),
                style = GalleryText.BodyMediumMedium,
                color = colorResource(R.color.drawer_bottom_trans_text),
                modifier = Modifier.padding(start = 12.sdp, top = 10.sdp)
            )
            SwitchRow(R.drawable.ic_delete, R.string.delete_photos_after_days, checked = autoDelete, tint = false,
                // dl/c case 0 flips the switch; turning it on only saves the choice (the Recycle Bin purges on open, Phase 6)
                onToggle = { scope.launch { appPreferences.setAutoDeleteTrash(!autoDelete) } })
            SwitchRow(R.drawable.ic_setting_usage_data, R.string.setting_usage_data_title, checked = usageOn, tint = true, onToggle = ::toggleUsage)
        }
    }

    if (showRateUs) RateUsDialog(
        onDismiss = { showRateUs = false },
        onRate = { rating ->
            showRateUs = false
            if (rating == 5) {
                val pkg = context.packageName
                runCatching { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=$pkg"))) }
                    .onFailure { open(context, "https://play.google.com/store/apps/details?id=$pkg") }
            }
        }
    )
}

/** layout_settings_premium (goProBanner). */
@Composable
private fun GoProBanner(onClick: () -> Unit) {
    val line = colorResource(R.color.premium_line)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(5.sdp)
            .drawableBackground(R.drawable.premium_bg)
            .clickable(remember { MutableInteractionSource() }, ripple(bounded = false), role = Role.Button, onClick = onClick),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Image(painterResource(R.drawable.premium), contentDescription = null, modifier = Modifier.padding(10.sdp).size(50.sdp))
        Column(Modifier.weight(1f).padding(top = 4.sdp, bottom = 4.sdp, end = 10.sdp)) {
            Text(stringResource(R.string.go_ads_free), style = GalleryText.BodyXLargeBold, color = line)
            Text(
                text = stringResource(R.string.unlock_everything_amp_enjoy_the_ultimate_ad_free_experience),
                style = GalleryText.BodyXSmallRegular.copy(fontSize = with(androidx.compose.ui.platform.LocalDensity.current) { 9.sdp.toSp() }),
                color = line,
                modifier = Modifier.padding(top = 3.sdp, end = 10.sdp)
            )
        }
    }
}

/** One activity_setting row: icon, BodyLargeMedium title, optional arrow; [dot] is ic_red_dot on the icon's top edge. */
@Composable
private fun SettingRow(
    @DrawableRes icon: Int,
    @StringRes title: Int,
    modifier: Modifier = Modifier,
    arrow: Boolean = true,
    dot: Boolean = false,
    dotCentered: Boolean = false, // ivDotIu sits on the icon's end edge, ivDot inside it
    onClick: () -> Unit
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 10.sdp)
            .clickable(remember { MutableInteractionSource() }, ripple(bounded = false), role = Role.Button, onClick = onClick)
            .padding(vertical = 15.sdp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(Modifier.padding(horizontal = 5.sdp)) {
            Image(painterResource(icon), contentDescription = null)
            if (dot) Image(
                painter = painterResource(R.drawable.ic_red_dot),
                contentDescription = null,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .offset(x = if (dotCentered) 4.sdp else 0.dp, y = (-4).sdp)
                    .size(8.sdp)
            )
        }
        Text(
            text = stringResource(title),
            style = GalleryText.BodyLargeMedium,
            color = colorResource(R.color.titleColor),
            modifier = Modifier.weight(1f).padding(start = 10.sdp)
        )
        if (arrow) Image(painterResource(R.drawable.ic_drawer_right_arrow), contentDescription = null)
    }
}

@Composable
private fun SettingDivider() {
    Box(Modifier.fillMaxWidth().padding(horizontal = 10.sdp).height(1.dp).background(colorResource(R.color.settings_divider)))
}

/** deleteLay / usageDataLay: the whole row toggles; the usage icon is tinted titleColor. */
@Composable
private fun SwitchRow(@DrawableRes icon: Int, @StringRes title: Int, checked: Boolean, tint: Boolean, onToggle: () -> Unit) {
    val label = stringResource(title)
    val primary = colorResource(R.color.colorPrimary)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 10.sdp, end = 10.sdp, bottom = 15.sdp)
            .clickable(remember { MutableInteractionSource() }, indication = null, role = Role.Switch, onClick = onToggle)
            .padding(vertical = 5.sdp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Image(
            painter = painterResource(icon),
            contentDescription = null,
            colorFilter = if (tint) ColorFilter.tint(colorResource(R.color.titleColor)) else null,
            modifier = Modifier.padding(horizontal = 5.sdp)
        )
        Text(
            text = label,
            style = GalleryText.BodyLargeMedium,
            color = colorResource(R.color.titleColor),
            modifier = Modifier.weight(1f).padding(start = 10.sdp)
        )
        Switch(
            checked = checked,
            onCheckedChange = { onToggle() },
            colors = SwitchDefaults.colors(
                checkedThumbColor = primary,
                checkedTrackColor = primary.copy(alpha = 0.3f),
                checkedBorderColor = Color.Transparent
            ),
            modifier = Modifier.semantics { contentDescription = label }
        )
    }
}

/**
 * dialog_rate_us (jt/e): a bottom sheet with the big star (ic_rate_star_N) over the ellipse, title, subtitle,
 * five stars (5 to start, at least 1) and RATE US. Five stars opens the store page; fewer just close it.
 */
@Composable
private fun RateUsDialog(onDismiss: () -> Unit, onRate: (Int) -> Unit) {
    var rating by remember { mutableIntStateOf(5) }
    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        val window = (LocalView.current.parent as? DialogWindowProvider)?.window
        SideEffect {
            window?.run {
                setGravity(Gravity.BOTTOM)
                setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
                setBackgroundDrawable(ColorDrawable(AndroidColor.TRANSPARENT))
            }
        }
        Box(Modifier.fillMaxWidth()) {
            Column(
                Modifier
                    .padding(top = 80.dp)
                    .fillMaxWidth()
                    .drawableBackground(R.drawable.rate_us_bg),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Image(
                    painter = painterResource(R.drawable.star_bg_ellipse),
                    contentDescription = null,
                    contentScale = ContentScale.FillBounds,
                    modifier = Modifier.fillMaxWidth().height(140.dp)
                )
                Text(
                    text = stringResource(rateTitle(rating)),
                    style = TextStyle(fontFamily = Inter, fontWeight = FontWeight.W500, fontSize = 18.sp),
                    color = colorResource(R.color.black),
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(top = 15.dp)
                )
                Text(
                    text = stringResource(R.string.we_would_be_very_grateful_if_you_can_rate_us),
                    style = TextStyle(fontFamily = Inter, fontWeight = FontWeight.W600, fontSize = 14.sp),
                    color = colorResource(R.color.rate_desc_color),
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(top = 12.dp)
                )
                Row(Modifier.padding(top = 10.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    for (star in 1..5) Image(
                        painter = painterResource(if (star <= rating) R.drawable.ic_fill_star else R.drawable.ic_unfill_star),
                        contentDescription = null,
                        modifier = Modifier
                            .size(35.dp)
                            .clickable(remember { MutableInteractionSource() }, null, role = Role.RadioButton) { rating = star }
                    )
                }
                Row(Modifier.padding(top = 10.dp).width(215.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = stringResource(R.string.the_best_we_can_get),
                        style = TextStyle(fontFamily = Inter, fontWeight = FontWeight.W600, fontSize = 16.sp),
                        color = colorResource(R.color.rate_best_text),
                        textAlign = TextAlign.Center,
                        maxLines = 1,
                        modifier = Modifier.weight(1f)
                    )
                    Image(painterResource(R.drawable.star_arrow), contentDescription = null, modifier = Modifier.padding(end = 10.dp).size(20.dp))
                }
                Box(
                    Modifier
                        .padding(start = 15.dp, end = 15.dp, top = 15.dp, bottom = 25.dp)
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(colorResource(R.color.colorPrimary))
                        .clickable(role = Role.Button) { onRate(rating) }
                        .padding(vertical = 12.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = stringResource(R.string.rate_us).uppercase(),
                        style = TextStyle(fontFamily = Inter, fontWeight = FontWeight.Bold, fontSize = 16.sp),
                        color = colorResource(R.color.onlyWhite)
                    )
                }
            }
            Image(
                painter = painterResource(rateStar(rating)),
                contentDescription = null,
                modifier = Modifier.align(Alignment.TopCenter).padding(top = 5.dp).size(150.dp)
            )
        }
    }
}
