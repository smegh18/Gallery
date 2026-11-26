package com.quizango.gallery.ui.components

import android.Manifest
import android.app.AppOpsManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Process
import android.provider.Settings
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.StringRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.TextUnitType
import androidx.compose.ui.unit.em
import androidx.compose.ui.window.DialogWindowProvider
import androidx.core.view.WindowCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
import com.quizango.gallery.GalleryApplication
import com.quizango.gallery.R
import com.quizango.gallery.ui.home.drawableBackground
import com.quizango.gallery.ui.theme.Inter
import com.quizango.gallery.ui.theme.isDark
import com.quizango.gallery.ui.theme.sdp
import com.quizango.gallery.ui.theme.ssp
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

// 2.7.2 usage-access prompts (S272/ta/c dialog, ta/d placements, ta/f controller). Only the prompt is ported (D4):
// no usage data is read, and Allow only opens the system Usage Access screen. Remote config is not fetched, so
// ta/a's "usage_access_controller" uses 2.7.2's in-app defaults (R272/xml/default_remote_config.xml): the
// home_enable, video, story and recycle switches are on, and the session lists below come from there.

/**
 * RC "enable". 2.7.2's in-app default is false: its Home, video, story and Recycle Bin prompts appear only once its
 * live remote config turns them on (the Settings one never checks it). USG-01 asks for them, so they are on here.
 * False also hides the Settings usage row (2.7.2 SettingActivity sets usageDataLay GONE).
 */
internal const val USAGE_ACCESS_RC_ENABLE = true

/** RC "feature_sessions" (video, story, Recycle Bin, Settings); ta/d's own fallback is 2, 3, 4, 6, 8, 10, 12. */
private val FEATURE_SESSIONS = setOf(1, 2, 3, 5, 8, 10, 14)

/** ta/d placements: all five dialog_usage_access_* layouts are identical; only the title differs. */
enum class UsageAccessKind(@StringRes val title: Int, val sessions: Set<Int>, val waitsForHome: Boolean) {
    HOME(R.string.usage_access_home_title, setOf(1, 3, 5, 9, 13), false), // RC "home_sessions" (fallback 1, 2, 4, 6, 8)
    VIDEO(R.string.usage_access_video_title, FEATURE_SESSIONS, true),
    STORY(R.string.usage_access_story_title, FEATURE_SESSIONS, true),
    RECYCLE(R.string.usage_access_recycle_title, FEATURE_SESSIONS, true),
    SETTINGS(R.string.usage_access_settings_title, FEATURE_SESSIONS, false)
}

/** -1 in the last-shown keys: that prompt never showed. */
internal const val USAGE_NEVER_SHOWN = -1

/** What ta/f reads before a prompt. [shownSession] is the key the non-Home prompts share. */
internal data class UsagePromptState(
    val session: Int,
    val granted: Boolean,
    val insightsEnabled: Boolean,
    val homeShownSession: Int,
    val shownSession: Int
)

/** ta/f.BsUTWEAMAI rules, in its order (each skip has its own log line there). */
internal fun usageAccessAllowed(kind: UsageAccessKind, s: UsagePromptState): Boolean = when {
    s.granted -> false // "permission already granted"
    !s.insightsEnabled -> false // "user opted out in Settings"
    !USAGE_ACCESS_RC_ENABLE -> false // "rc enable=false"
    kind.waitsForHome && s.homeShownSession == USAGE_NEVER_SHOWN -> false // "home popup not shown yet"
    s.session !in kind.sessions -> false
    else -> (if (kind == UsageAccessKind.HOME) s.homeShownSession else s.shownSession) != s.session // "already shown in session"
}

/** ta/f.R7N8DF4OVS on Home: the queued prompt if its rules pass, else the Home one. */
internal fun pickHomeUsagePrompt(queued: UsageAccessKind?, s: UsagePromptState): UsageAccessKind? =
    listOfNotNull(queued, UsageAccessKind.HOME).firstOrNull { usageAccessAllowed(it, s) }

/** ta/f.pendingForHome: the prompt a story, video or Recycle Bin exit leaves for Home's next resume. */
object UsageAccessQueue {
    @Volatile private var queued: UsageAccessKind? = null

    fun queueForHome(kind: UsageAccessKind) {
        queued = kind
    }

    fun take(): UsageAccessKind? = queued.also { queued = null }
}

/** PermissionsKt.DxDJysLV5r: AppOps GET_USAGE_STATS for this uid; MODE_DEFAULT falls back to the permission grant. */
fun isUsageAccessGranted(context: Context): Boolean = try {
    val appOps = context.getSystemService(Context.APP_OPS_SERVICE) as AppOpsManager
    val mode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        appOps.unsafeCheckOpNoThrow(AppOpsManager.OPSTR_GET_USAGE_STATS, Process.myUid(), context.packageName)
    } else {
        @Suppress("DEPRECATION")
        appOps.checkOpNoThrow(AppOpsManager.OPSTR_GET_USAGE_STATS, Process.myUid(), context.packageName)
    }
    if (mode == AppOpsManager.MODE_DEFAULT) {
        context.checkCallingOrSelfPermission(Manifest.permission.PACKAGE_USAGE_STATS) == PackageManager.PERMISSION_GRANTED
    } else {
        mode == AppOpsManager.MODE_ALLOWED
    }
} catch (_: Exception) {
    false
}

/** ta/f.Wja3o2vx62: this app's Usage Access page, else the whole list; false when neither opens. */
internal fun openUsageAccessSettings(packageName: String, launch: (Intent) -> Unit): Boolean =
    listOf(
        Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS, Uri.parse("package:$packageName")),
        Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS)
    ).any { runCatching { launch(it) }.isSuccess }

/** HA posts its check 0x2bc ms after onResume, and again after Home regains window focus. */
internal const val HOME_USAGE_CHECK_DELAY_MS = 700L

/**
 * HomeActivity's usage-access check (HA.OoooOoo): [HOME_USAGE_CHECK_DELAY_MS] after Home resumes, once Home has window
 * focus and no prompt is up, the prompt a story, video or Recycle Bin exit queued is tried, then the Home one. Returns
 * true while a prompt is up: Home holds its vault hint until it closes (isUsageAccessPopupShowingOnHome). [onChecked]
 * runs once a check has run, shown or not: Home holds the floating-button offer until then, as HA does.
 */
@Composable
fun homeUsageAccessPrompt(onChecked: () -> Unit = {}): Boolean {
    val context = LocalContext.current
    val latestOnChecked by rememberUpdatedState(onChecked)
    val preferences = (context.applicationContext as GalleryApplication).preferences
    var shown by rememberSaveable { mutableStateOf<UsageAccessKind?>(null) }
    val windowInfo = LocalWindowInfo.current
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    LaunchedEffect(lifecycle) {
        lifecycle.repeatOnLifecycle(Lifecycle.State.RESUMED) {
            delay(HOME_USAGE_CHECK_DELAY_MS)
            while (shown == null && !windowInfo.isWindowFocused) { // another dialog or popup is up: HA retries once focus is back
                snapshotFlow { windowInfo.isWindowFocused }.first { it }
                delay(HOME_USAGE_CHECK_DELAY_MS)
            }
            if (shown != null) { // a prompt is already up (restored); the queued one waits
                latestOnChecked()
                return@repeatOnLifecycle
            }
            val session = preferences.sessionCount.first()
            val state = UsagePromptState(
                session = session,
                granted = isUsageAccessGranted(context),
                insightsEnabled = preferences.appInsightsEnabled.first(),
                homeShownSession = preferences.usageAccessShownSession(home = true).first(),
                shownSession = preferences.usageAccessShownSession(home = false).first()
            )
            pickHomeUsagePrompt(UsageAccessQueue.take(), state)?.let { kind ->
                preferences.setUsageAccessShownSession(home = kind == UsageAccessKind.HOME, session)
                shown = kind
            }
            latestOnChecked()
        }
    }
    shown?.let { kind ->
        UsageAccessDialog(
            kind,
            onAllow = {
                shown = null
                openUsageAccessSettings(context.packageName, context::startActivity)
            },
            onDismiss = { shown = null }
        )
    }
    return shown != null
}

/**
 * Settings "Share app usage stats" switched on without Usage Access (SettingActivity.OoooOOo): the SETTINGS sheet, with
 * no session rules. After Not now, Back, a swipe or the return from the system screen, a grant turns app_insights_enabled
 * on, then [onDone] runs so Settings can refresh its switch (dl/e case 0). Returns the call that opens the sheet; it does
 * nothing while access is already granted.
 */
@Composable
fun rememberSettingsUsageAccessPrompt(onDone: () -> Unit): () -> Unit {
    val context = LocalContext.current
    val preferences = (context.applicationContext as GalleryApplication).preferences
    val scope = rememberCoroutineScope()
    val latestOnDone by rememberUpdatedState(onDone)
    var showing by rememberSaveable { mutableStateOf(false) }
    val finish: () -> Unit = {
        scope.launch {
            if (isUsageAccessGranted(context)) preferences.setAppInsightsEnabled(true)
            latestOnDone()
        }
    }
    val settings = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { finish() }
    if (showing) {
        UsageAccessDialog(
            UsageAccessKind.SETTINGS,
            onAllow = {
                showing = false
                if (!openUsageAccessSettings(context.packageName, settings::launch)) finish()
            },
            onDismiss = {
                showing = false
                finish()
            }
        )
    }
    return remember(context) { { if (!isUsageAccessGranted(context)) showing = true } }
}

/**
 * dialog_usage_access_* in ta/c's BottomSheetDialog: full width at the bottom, dim 0.8, cancelable (Back, outside tap,
 * swipe down), white navigation-bar icons. Allow runs [onAllow] at once (ta/c drops its dismiss listener first);
 * Not now and every cancel end in [onDismiss].
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UsageAccessDialog(kind: UsageAccessKind, onAllow: () -> Unit, onDismiss: () -> Unit) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scope = rememberCoroutineScope()
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        sheetMaxWidth = Dp.Unspecified, // ta/c: design_bottom_sheet MATCH_PARENT, no side margins
        shape = RectangleShape,
        containerColor = Color.Transparent, // the layout draws bg_usage_sheet itself
        scrimColor = Color.Black.copy(alpha = 0.8f), // ta/c setDimAmount(0.8f)
        dragHandle = null,
        contentWindowInsets = { WindowInsets.navigationBars } // the sheet sits above the bar; the dim shows under it
    ) {
        val window = (LocalView.current.parent as? DialogWindowProvider)?.window
        val dark = MaterialTheme.colorScheme.isDark
        SideEffect {
            window?.let {
                WindowCompat.getInsetsController(it, it.decorView).run {
                    isAppearanceLightStatusBars = !dark // Home's own status bar, which ta/c's sheet never covers
                    isAppearanceLightNavigationBars = false // ta/c: light navigation bars off
                }
            }
        }
        UsageAccessSheet(
            kind,
            onAllow = onAllow,
            onNotNow = { scope.launch { sheetState.hide() }.invokeOnCompletion { onDismiss() } }
        )
    }
}

@Composable
@ReadOnlyComposable
private fun usageText(size: Int, bold: Boolean = false) =
    TextStyle(fontFamily = Inter, fontWeight = if (bold) FontWeight.W700 else FontWeight.W400, fontSize = size.ssp)

/** ta/c opens RC common_data.social_media_links.privacy_policy (this in-app default; about_privacy_policy if empty). */
private const val USAGE_PRIVACY_POLICY_URL = "https://appsspecials.com/bestgallery/privacy"

private val USAGE_POINTS = listOf(
    R.string.usage_access_point_media,
    R.string.usage_access_point_personal,
    R.string.usage_access_point_control
)

@Composable
private fun UsageAccessSheet(kind: UsageAccessKind, onAllow: () -> Unit, onNotNow: () -> Unit) {
    val context = LocalContext.current
    val appName = stringResource(R.string.app_name_local)
    val accent = colorResource(R.color.usage_settings_accent)
    val titleColor = colorResource(R.color.titleColor)
    val subtitle = colorResource(R.color.usage_subtitle)
    val desc = usageText(11)
    val descExtra = with(LocalDensity.current) { 2.sdp.toSp() } // lineSpacingExtra
    val noRipple = remember { MutableInteractionSource() } // plain TextView clicks, no pressed state
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .drawableBackground(R.drawable.bg_usage_sheet, colorResource(R.color.dialogBack).toArgb())
            .padding(start = 22.sdp, top = 10.sdp, end = 22.sdp, bottom = 18.sdp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(Modifier.padding(bottom = 16.sdp).size(38.sdp, 5.sdp).drawableBackground(R.drawable.bg_usage_grabber))
        Image(
            painter = painterResource(R.drawable.ic_usage_hero_settings),
            contentDescription = null,
            contentScale = ContentScale.Inside, // centerInside: the 30dp glyph is never scaled up
            modifier = Modifier.size(60.sdp).drawableBackground(R.drawable.bg_usage_icon_settings).padding(15.sdp)
        )
        Text(
            text = stringResource(R.string.usage_access_settings_eyebrow),
            style = usageText(9, bold = true),
            color = accent,
            letterSpacing = 0.08.em,
            modifier = Modifier
                .padding(top = 14.sdp)
                .drawableBackground(R.drawable.bg_usage_eyebrow_settings)
                .padding(horizontal = 12.sdp, vertical = 4.sdp)
        )
        Text(
            text = stringResource(kind.title, appName),
            style = usageText(17, bold = true),
            color = titleColor,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth().padding(top = 10.sdp)
        )
        Text(
            text = stringResource(R.string.usage_access_desc, appName),
            style = desc.copy(lineHeight = TextUnit(desc.fontSize.value * 1.2f + descExtra.value, TextUnitType.Sp)),
            color = subtitle,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth().padding(top = 8.sdp)
        )
        Column(Modifier.fillMaxWidth().padding(top = 16.sdp)) {
            USAGE_POINTS.forEachIndexed { index, point ->
                Row(
                    modifier = Modifier.fillMaxWidth().padding(top = if (index == 0) 0.sdp else 9.sdp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Image(
                        painter = painterResource(R.drawable.ic_usage_check),
                        contentDescription = null,
                        modifier = Modifier
                            .size(18.sdp)
                            .drawableBackground(R.drawable.bg_usage_check_circle, accent.toArgb())
                            .padding(3.sdp)
                    )
                    Text(
                        text = stringResource(point),
                        style = usageText(10),
                        color = titleColor,
                        modifier = Modifier.fillMaxWidth().padding(start = 10.sdp)
                    )
                }
            }
        }
        Text(
            text = stringResource(R.string.usage_access_next_step, appName),
            style = usageText(10),
            color = subtitle,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth().padding(top = 16.sdp)
        )
        Text(
            text = stringResource(R.string.privacy_policy),
            style = usageText(10, bold = true),
            color = accent,
            textDecoration = TextDecoration.Underline,
            modifier = Modifier
                .padding(top = 12.sdp)
                .clickable(interactionSource = noRipple, indication = null) {
                    runCatching { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(USAGE_PRIVACY_POLICY_URL))) }
                        .onFailure { Toast.makeText(context, R.string.browser_not_available, Toast.LENGTH_SHORT).show() }
                }
                .padding(4.sdp)
        )
        Text(
            text = stringResource(R.string.usage_access_allow),
            style = usageText(13, bold = true),
            color = colorResource(R.color.onlyWhite),
            textAlign = TextAlign.Center,
            modifier = Modifier
                .padding(top = 14.sdp)
                .fillMaxWidth()
                .height(44.sdp)
                .drawableBackground(R.drawable.bg_usage_allow_settings)
                .clickable(interactionSource = noRipple, indication = null, onClick = onAllow)
                .wrapContentHeight()
        )
        Text(
            text = stringResource(R.string.usage_access_not_now),
            style = usageText(12),
            color = titleColor,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .padding(top = 10.sdp)
                .fillMaxWidth()
                .height(42.sdp)
                .drawableBackground(R.drawable.bg_usage_not_now)
                .clickable(interactionSource = noRipple, indication = null, onClick = onNotNow)
                .wrapContentHeight()
        )
    }
}
