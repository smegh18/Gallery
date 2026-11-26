package com.quizango.gallery.ui.floating

import android.content.Intent
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.ModalBottomSheetProperties
import androidx.compose.material3.SheetValue
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.core.content.edit
import com.quizango.gallery.MainActivity
import com.quizango.gallery.R
import com.quizango.gallery.ui.splash.checkStoragePermissions
import com.quizango.gallery.ui.theme.GalleryText
import com.quizango.gallery.ui.theme.sdp
import com.quizango.gallery.ui.theme.sdpFactor
import kotlin.math.cos
import kotlin.math.sin

/**
 * bo/p.R7N8DF4OVS: with the overlay permission the button turns on at once. Without it "Display over other apps" opens,
 * the guide (dialog_overlay_guide_anime) comes up over it a second later, and the app returns by itself as soon as the
 * permission is granted (ua/l polls every 200 ms). The returned function starts the flow.
 */
// shortcut: 2.7.2 first asks CALL_PHONE + READ_PHONE_STATE for its after-call feature when that is eligible; that belongs to Phase 11, prepend it here
@Composable
fun rememberOverlayPermissionFlow(onGranted: () -> Unit, onDenied: () -> Unit): () -> Unit {
    val context = LocalContext.current
    val granted by rememberUpdatedState(onGranted)
    val denied by rememberUpdatedState(onDenied)
    val poll = remember { Handler(Looper.getMainLooper()) }
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) {
        poll.removeCallbacksAndMessages(null)
        if (Settings.canDrawOverlays(context)) {
            FloatingButtons.enable(context)
            granted()
        } else denied()
    }
    DisposableEffect(Unit) { onDispose { poll.removeCallbacksAndMessages(null) } }
    return remember {
        start@{
            if (Settings.canDrawOverlays(context)) {
                FloatingButtons.enable(context)
                granted()
                return@start
            }
            val intent = Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:${context.packageName}"))
            if (intent.resolveActivity(context.packageManager) == null || runCatching { launcher.launch(intent) }.isFailure) {
                denied()
                return@start
            }
            poll.postDelayed(object : Runnable {
                override fun run() {
                    if (!Settings.canDrawOverlays(context)) {
                        poll.postDelayed(this, 200)
                        return
                    }
                    runCatching {
                        context.startActivity(
                            Intent(context, MainActivity::class.java).addFlags(
                                Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP or
                                    Intent.FLAG_ACTIVITY_REORDER_TO_FRONT or Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED
                            )
                        )
                    }
                }
            }, 200)
            poll.postDelayed({
                if (!Settings.canDrawOverlays(context)) runCatching { context.startActivity(Intent(context, OverlayGuideActivity::class.java)) }
            }, 1000)
        }
    }
}

/** HomeActivity's floating-button offer for this launch: its dialog, then the permission flow. */
@Stable
class FloatingButtonOffer internal constructor(internal val forcefully: Boolean?) {
    internal var dialogUp by mutableStateOf(forcefully != null)
    internal var flowUp by mutableStateOf(false)

    /** The vault hint waits while this is true (2.7.2 isVaultHintShowPending). */
    val isShowing get() = dialogUp || flowUp
}

/**
 * Home's start: the button resumes if it is on; otherwise launches 1, 4, 7 and 10 offer it (4 and 10 forcefully),
 * once per launch and only once the media permission is there (HomeActivity.o00Ooo).
 */
@Composable
fun rememberFloatingButtonOffer(session: Int): FloatingButtonOffer {
    val context = LocalContext.current
    LaunchedEffect(Unit) { FloatingButtons.start(context) }
    val offer = remember(session) {
        val shown = FloatingButtons.prefs(context).getInt(FloatingButtons.KEY_HOME_DIALOG_SESSION, -1)
        FloatingButtonOffer(
            homeOffer(session, shown, FloatingButtons.isOn(context), FloatingButtons.available())
                ?.takeIf { checkStoragePermissions(context) }
        )
    }
    LaunchedEffect(offer) {
        if (offer.forcefully != null) FloatingButtons.prefs(context).edit { putInt(FloatingButtons.KEY_HOME_DIALOG_SESSION, session) }
    }
    return offer
}

/** Shows the offer's dialog; a refused forceful offer comes straight back (HomeActivity.o00oO0O). */
@Composable
fun FloatingButtonOfferDialog(offer: FloatingButtonOffer) {
    val forcefully = offer.forcefully ?: return
    val enable = rememberOverlayPermissionFlow(
        onGranted = { offer.flowUp = false },
        onDenied = {
            offer.flowUp = false
            if (forcefully) offer.dialogUp = true
        }
    )
    if (offer.dialogUp) FloatingButtonDialog(
        forcefully,
        onAllow = {
            offer.dialogUp = false
            offer.flowUp = true
            enable()
        },
        onClose = { offer.dialogUp = false }
    )
}

/**
 * SettingActivity's floating row: with the overlay permission it opens Quick Access Settings; without it the offer
 * dialog (never forceful) comes first and Allow goes on to the screen once the permission is granted. Hosts the dialog.
 */
@Composable
fun rememberFloatingSettingsEntry(onOpenSettings: () -> Unit): () -> Unit {
    val context = LocalContext.current
    val open by rememberUpdatedState(onOpenSettings)
    var dialog by remember { mutableStateOf(false) }
    val enable = rememberOverlayPermissionFlow(onGranted = { open() }, onDenied = {})
    if (dialog) FloatingButtonDialog(
        forcefully = false,
        onAllow = {
            dialog = false
            enable()
        },
        onClose = { dialog = false }
    )
    return { if (Settings.canDrawOverlays(context)) open() else dialog = true }
}

/** Tool icons round the close button of dialog_floating_button_popup: (drawable, size sdp, angle from 12 o'clock, radius sdp). */
private val RING = listOf(
    Triple(R.drawable.ic_story_with_bg, 40, 280f to 87f),
    Triple(R.drawable.ic_recyclebin_with_bg, 40, 316f to 77f),
    Triple(R.mipmap.ic_launcher_round_gallery, 50, 360f to 70f),
    Triple(R.drawable.ic_vault_with_bg, 40, 44f to 77f),
    Triple(R.drawable.ic_fav_with_bg, 40, 80f to 87f)
)

/**
 * bo/c (dialog_floating_button_popup): an expanded bottom sheet over a 60 % dim. A forceful one has no close button
 * and cannot be swiped, tapped or backed away.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun FloatingButtonDialog(forcefully: Boolean, onAllow: () -> Unit, onClose: () -> Unit) {
    ModalBottomSheet(
        onDismissRequest = onClose,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true, confirmValueChange = { !forcefully || it != SheetValue.Hidden }),
        shape = RoundedCornerShape(topStart = 18.sdp, topEnd = 18.sdp),
        containerColor = colorResource(R.color.callercad_screen_bg),
        scrimColor = Color.Black.copy(alpha = 0.6f),
        dragHandle = null,
        properties = ModalBottomSheetProperties(shouldDismissOnBackPress = !forcefully)
    ) {
        Column(Modifier.fillMaxWidth()) {
            Box(Modifier.fillMaxWidth()) {
                // clTop: the ring wraps to 160 sdp (20 padding, the logo's top 70 sdp above the close button's centre)
                Box(Modifier.fillMaxWidth().height(160.sdp)) {
                    Image(painterResource(R.drawable.bg_dialog_access_float), null, Modifier.matchParentSize(), contentScale = ContentScale.FillBounds)
                    Image(
                        painterResource(R.drawable.ic_close_with_bg), null,
                        Modifier.align(Alignment.TopCenter).offset(y = (CENTER_Y - 17.5f).sdp).size(35.sdp)
                    )
                    RING.forEach { (icon, size, polar) ->
                        val (angle, radius) = polar
                        val rad = Math.toRadians(angle.toDouble())
                        Image(
                            painterResource(icon), null,
                            Modifier
                                .align(Alignment.TopCenter)
                                .offset(x = (radius * sin(rad)).toFloat().sdp, y = (CENTER_Y - radius * cos(rad) - size / 2.0).toFloat().sdp)
                                .size(size.sdp)
                        )
                    }
                }
                if (!forcefully) Image(
                    painterResource(R.drawable.ic_story_close),
                    stringResource(R.string.close_button),
                    Modifier
                        .align(Alignment.TopEnd)
                        .clip(RoundedCornerShape(50))
                        .clickable(remember { MutableInteractionSource() }, ripple(bounded = false)) { onClose() }
                        .padding(10.sdp),
                    colorFilter = ColorFilter.tint(colorResource(R.color.radioButtonUncheckedColor))
                )
            }
            Text(
                stringResource(R.string.quick_access_floating_button_title),
                style = GalleryText.BodyXLargeBold,
                color = colorResource(R.color.titleColor),
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth().padding(top = 10.sdp)
            )
            Text(
                stringResource(R.string.quick_access_floating_button_description),
                style = GalleryText.BodyMediumMedium,
                color = colorResource(R.color.iu_setting_sub_text),
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth().padding(top = 10.sdp).padding(horizontal = 15.sdp)
            )
            Box(
                Modifier
                    .padding(top = 10.sdp)
                    .padding(horizontal = 16.sdp)
                    .fillMaxWidth()
                    .height(40.sdp)
                    .clip(RoundedCornerShape(25.sdp))
                    .background(colorResource(R.color.dark_blue))
                    .clickable { onAllow() },
                contentAlignment = Alignment.Center
            ) {
                Text(stringResource(R.string.allow), style = GalleryText.BodyXLargeSemiBold, color = colorResource(R.color.onlyWhite))
            }
            Text(
                stringResource(R.string.floating_button_settings_disclaimer),
                style = GalleryText.BodyXSmallRegular,
                color = colorResource(R.color.iu_setting_sub_text),
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth().padding(horizontal = 30.sdp).padding(top = 10.sdp, bottom = 17.sdp)
            )
        }
    }
}

/** The close button's centre, sdp from the top of clTop. */
private const val CENTER_Y = 115f

/** Float sdp for the ring's trigonometry (ui/theme has Int.sdp only). */
private val Float.sdp: Dp
    @Composable @ReadOnlyComposable
    get() = (this * sdpFactor(LocalConfiguration.current.smallestScreenWidthDp)).dp
