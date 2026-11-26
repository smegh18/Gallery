package com.quizango.gallery.ui.iu

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.animation.AccelerateDecelerateInterpolator
import android.view.animation.OvershootInterpolator
import android.widget.Toast
import androidx.activity.addCallback
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.annotation.StringRes
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.toBitmap
import com.quizango.gallery.GalleryApplication
import com.quizango.gallery.R
import com.quizango.gallery.ui.components.NativeAdCard
import com.quizango.gallery.ui.gs.IuSettingScreen
import com.quizango.gallery.ui.gs.PackageEvent
import com.quizango.gallery.ui.gs.appRouteIntent
import com.quizango.gallery.ui.navigation.Screen
import com.quizango.gallery.ui.theme.GalleryText
import com.quizango.gallery.ui.theme.GalleryTheme
import com.quizango.gallery.ui.theme.sdp
import com.quizango.gallery.ui.theme.ssp
import kotlinx.coroutines.delay

/**
 * 2.7.2 InstallUninstallActivity: a 4 s progress card for the installed, updated or removed app, then the complete state
 * and, 1.5 s later, Open (install, update) or Done. A separate task started from the monitor service, which is why it
 * is an activity. The gear opens IU settings in place: 2.7.2's IU screen is noHistory, so Back there leaves the app too.
 */
class InstallUninstallActivity : AppCompatActivity() {
    private var completed = false
    private val showSettings = mutableStateOf(false)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // 2.7.2 closes a restored instance instead of replaying the scan
        if (savedInstanceState != null) {
            finishAndRemoveTask()
            return
        }
        val event = PackageEvent.entries.firstOrNull { it.name == intent.getStringExtra(EXTRA_EVENT) }
        val packageName = intent.getStringExtra(EXTRA_PACKAGE)
        if (event == null || packageName == null) {
            finish()
            return
        }
        enableEdgeToEdge()
        // Back does nothing while the progress runs
        onBackPressedDispatcher.addCallback(this) { if (completed || showSettings.value) finishAndRemoveTask() }
        val app = application as GalleryApplication
        setContent {
            val themeMode by app.preferences.themeMode.collectAsState(initial = 2)
            GalleryTheme(darkTheme = if (themeMode == 2) isSystemInDarkTheme() else themeMode == 1) {
                if (showSettings.value) {
                    IuSettingScreen(app.preferences, onNavigateBack = ::finishAndRemoveTask)
                } else {
                    InstallUninstallScreen(
                        event = event,
                        packageName = packageName,
                        onCompleted = { completed = true },
                        onSettings = { showSettings.value = true },
                        onVault = {
                            startActivity(appRouteIntent(this, Screen.VaultLock.route))
                            finish()
                        },
                        onAction = { openApp(event, packageName) }
                    )
                }
            }
        }
    }

    private fun openApp(event: PackageEvent, packageName: String) {
        if (event != PackageEvent.UNINSTALL) {
            val launch = packageManager.getLaunchIntentForPackage(packageName)?.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            if (launch == null || runCatching { startActivity(launch) }.isFailure) {
                Toast.makeText(this, R.string.iu_launch_failed, Toast.LENGTH_SHORT).show()
            }
        }
        finish()
    }

    companion object {
        private const val EXTRA_PACKAGE = "package_name"
        private const val EXTRA_EVENT = "package_event"

        internal fun intent(context: Context, packageName: String, event: PackageEvent): Intent =
            Intent(context, InstallUninstallActivity::class.java)
                .putExtra(EXTRA_PACKAGE, packageName)
                .putExtra(EXTRA_EVENT, event.name)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_CLEAR_TASK)
    }
}

/** Texts per event: header title and subtitle and progress title while scanning, then the complete header and button. */
internal data class IuTexts(
    @StringRes val title: Int,
    @StringRes val subtitle: Int,
    @StringRes val progressTitle: Int,
    @StringRes val doneTitle: Int,
    @StringRes val doneSubtitle: Int,
    @StringRes val button: Int
)

internal fun iuTexts(event: PackageEvent): IuTexts = when (event) {
    PackageEvent.UPDATE -> IuTexts(
        R.string.gs2_updating_title, R.string.gs2_progress_desc_update, R.string.gs2_progress_title_update,
        R.string.gs2_update_complete, R.string.gs2_update_complete_desc, R.string.app_scan_open
    )
    PackageEvent.INSTALL -> IuTexts(
        R.string.gs2_installing_title, R.string.gs2_progress_desc_install, R.string.gs2_setting_things_up,
        R.string.gs2_installation_complete, R.string.gs2_you_can_start_using_the_app_now, R.string.app_scan_open
    )
    PackageEvent.UNINSTALL -> IuTexts(
        R.string.gs2_uninstalling_title, R.string.gs2_progress_desc_uninstall, R.string.gs2_cleaning_up,
        R.string.gs2_removal_complete, R.string.gs2_your_app_uninstalled_successfully, R.string.app_scan_done
    )
}

private enum class IuPhase { PROGRESS, STATUS, BUTTON }

@Composable
private fun InstallUninstallScreen(
    event: PackageEvent,
    packageName: String,
    onCompleted: () -> Unit,
    onSettings: () -> Unit,
    onVault: () -> Unit,
    onAction: () -> Unit
) {
    val context = LocalContext.current
    val texts = iuTexts(event)
    val progress = remember { Animatable(0f) }
    val success = remember { Animatable(0f) }
    var phase by remember { mutableStateOf(IuPhase.PROGRESS) }
    LaunchedEffect(Unit) {
        // ValueAnimator 0..100 over 4 s with its default accelerate-decelerate curve
        val curve = AccelerateDecelerateInterpolator()
        progress.animateTo(100f, tween(4000, easing = { curve.getInterpolation(it) }))
        phase = IuPhase.STATUS
        onCompleted()
        val overshoot = OvershootInterpolator()
        success.animateTo(1f, tween(350, easing = { overshoot.getInterpolation(it) }))
        delay(1500 - 350)
        phase = IuPhase.BUTTON
    }
    val appIcon = remember(packageName) {
        if (event == PackageEvent.UNINSTALL) null
        else (runCatching { context.packageManager.getApplicationIcon(packageName) }.getOrNull()
            ?: ContextCompat.getDrawable(context, android.R.drawable.sym_def_app_icon))?.toBitmap()?.asImageBitmap()
    }
    val primaryText = colorResource(R.color.iu_text_primary)

    Column(
        Modifier
            .fillMaxSize()
            .background(colorResource(R.color.gs2_background))
            .verticalScroll(rememberScrollState())
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(bottom = 16.sdp)
    ) {
        Box(Modifier.fillMaxWidth().padding(top = 30.sdp, bottom = 10.sdp)) {
            Image(
                painterResource(R.drawable.ic_iu_setting), stringResource(R.string.settings),
                colorFilter = ColorFilter.tint(primaryText),
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(top = 4.sdp, end = 12.sdp)
                    .clickable(remember { MutableInteractionSource() }, ripple(bounded = false), onClick = onSettings)
                    .padding(3.sdp)
            )
            Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                Box(Modifier.padding(top = 28.sdp).size(92.sdp), contentAlignment = Alignment.Center) {
                    if (phase == IuPhase.PROGRESS) {
                        val iconModifier = Modifier.size(62.sdp)
                        if (appIcon != null) {
                            Image(appIcon, null, iconModifier.clip(RoundedCornerShape(22)))
                        } else {
                            Image(painterResource(R.drawable.ic_gs2_default_app), null, iconModifier)
                        }
                    } else {
                        Image(
                            painterResource(R.drawable.ic_gs2_success), null,
                            Modifier.fillMaxSize().graphicsLayer {
                                val scale = 0.6f + 0.4f * success.value
                                scaleX = scale
                                scaleY = scale
                                alpha = success.value.coerceIn(0f, 1f)
                            }
                        )
                    }
                }
                Text(
                    stringResource(if (phase == IuPhase.PROGRESS) texts.title else texts.doneTitle),
                    style = GalleryText.BodyLargeSemiBold, fontSize = 15.ssp, color = primaryText, textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 20.sdp).padding(top = 10.sdp)
                )
                Text(
                    stringResource(if (phase == IuPhase.PROGRESS) texts.subtitle else texts.doneSubtitle),
                    style = GalleryText.BodyMediumRegular, fontSize = 11.ssp, color = colorResource(R.color.gs2_subtitle),
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 20.sdp).padding(top = 5.sdp)
                )
            }
        }
        // custom_card_view: 2.7.2 shows this vault promo where the top ad fails to load; ours never loads one
        VaultCard(onVault)
        Box(Modifier.padding(horizontal = 12.sdp).padding(top = 12.sdp).fillMaxWidth()) {
            val statusShape = RoundedCornerShape(10.sdp)
            when (phase) {
                IuPhase.PROGRESS -> Column(
                    Modifier.fillMaxWidth().background(colorResource(R.color.gs2_status_bg), statusShape)
                        .padding(horizontal = 12.sdp, vertical = 10.sdp)
                ) {
                    Text(
                        stringResource(texts.progressTitle), style = GalleryText.BodyMediumRegular, fontSize = 11.ssp,
                        color = primaryText, maxLines = 1, overflow = TextOverflow.Ellipsis
                    )
                    Row(Modifier.padding(top = 8.sdp), verticalAlignment = Alignment.CenterVertically) {
                        LinearProgressIndicator(
                            progress = { progress.value / 100f },
                            color = colorResource(R.color.iu_primary),
                            trackColor = colorResource(R.color.gs2_progress_track),
                            strokeCap = StrokeCap.Round,
                            gapSize = 0.dp,
                            drawStopIndicator = {},
                            modifier = Modifier.weight(1f).height(7.dp).padding(end = 10.sdp)
                        )
                        Text(
                            "${progress.value.toInt()}%", style = GalleryText.BodyLargeSemiBold, fontSize = 10.ssp,
                            color = primaryText, textAlign = TextAlign.End, modifier = Modifier.widthIn(min = 20.sdp)
                        )
                    }
                }
                IuPhase.STATUS -> Row(
                    Modifier.fillMaxWidth().background(colorResource(R.color.gs2_status_bg), statusShape)
                        .padding(horizontal = 12.sdp, vertical = 11.sdp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Image(painterResource(R.drawable.ic_gs2_check_fill), null, Modifier.size(20.sdp))
                    Text(
                        stringResource(R.string.gs2_all_set), style = GalleryText.BodyMediumRegular, fontSize = 13.ssp,
                        color = primaryText, maxLines = 1, overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f).padding(start = 8.sdp)
                    )
                }
                IuPhase.BUTTON -> Text(
                    stringResource(texts.button), style = GalleryText.BodyLargeSemiBold, fontSize = 13.ssp,
                    color = colorResource(R.color.onlyWhite), textAlign = TextAlign.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.sdp))
                        .background(colorResource(R.color.gs2_open_btn_bg))
                        .clickable(onClick = onAction)
                        .padding(vertical = 11.sdp)
                )
            }
        }
        NativeAdCard(Modifier.padding(horizontal = 8.sdp).padding(top = 12.sdp))
    }
}

@Composable
private fun VaultCard(onClick: () -> Unit) {
    val shape = RoundedCornerShape(14.sdp)
    Row(
        Modifier
            .padding(horizontal = 12.sdp)
            .padding(top = 16.sdp)
            .fillMaxWidth()
            .height(IntrinsicSize.Min)
            .clip(shape)
            .border(1.dp, colorResource(R.color.iu_stroke), shape)
            .clickable(onClick = onClick)
            .padding(10.dp)
    ) {
        Image(painterResource(R.drawable.ic_iu_private), null, Modifier.align(Alignment.CenterVertically))
        Column(Modifier.weight(1f).fillMaxHeight().padding(start = 10.sdp)) {
            Text(
                stringResource(R.string.private_gallery_vault), style = GalleryText.BodyMediumMedium, fontSize = 10.ssp,
                color = colorResource(R.color.iu_text_primary), maxLines = 2, overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(top = 6.dp)
            )
            Text(
                stringResource(R.string.smart_org), style = GalleryText.BodyMediumRegular, fontSize = 9.ssp,
                color = colorResource(R.color.iu_text_secondary), maxLines = 2, overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(top = 4.dp)
            )
            Spacer(Modifier.weight(1f))
            Text(
                stringResource(R.string.try_it_now), style = GalleryText.BodySmallMedium, fontSize = 11.ssp,
                color = colorResource(R.color.onlyWhite), textAlign = TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .background(colorResource(R.color.iu_primary), RoundedCornerShape(100.dp))
                    .padding(vertical = 5.sdp)
            )
            Spacer(Modifier.weight(1f))
        }
    }
}
