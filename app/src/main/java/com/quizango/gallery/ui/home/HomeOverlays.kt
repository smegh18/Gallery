package com.quizango.gallery.ui.home

import android.graphics.Color as AndroidColor
import android.graphics.drawable.ColorDrawable
import android.view.Gravity
import android.view.ViewGroup
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.paint
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.layout
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInWindow
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.window.DialogWindowProvider
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.onLongClick
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.airbnb.lottie.compose.LottieAnimation
import com.airbnb.lottie.compose.LottieCompositionSpec
import com.airbnb.lottie.compose.LottieConstants
import com.airbnb.lottie.compose.rememberLottieComposition
import com.quizango.gallery.R
import com.quizango.gallery.ui.theme.GalleryText
import com.quizango.gallery.ui.theme.Inter
import com.quizango.gallery.ui.theme.sdp

/**
 * layout_pinch_zoom_in_out: full-Home tooltip_bg scrim that takes every touch; only GOT IT closes it.
 * Draw it as the last child of the Home root so it covers the top and bottom bars.
 */
@Composable
fun PinchTutorialOverlay(onGotIt: () -> Unit) {
    val white = colorResource(R.color.onlyWhite)
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(colorResource(R.color.tooltip_bg))
            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) {}
    ) {
        // Equal space above and below keeps the Lottie centred (centerInParent) with the text hanging under it
        Column(modifier = Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally) {
            Spacer(Modifier.weight(1f))
            val composition by rememberLottieComposition(LottieCompositionSpec.RawRes(R.raw.zoom_in_out_gesture))
            LottieAnimation(composition, iterations = LottieConstants.IterateForever, modifier = Modifier.size(140.sdp))
            Column(modifier = Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = stringResource(R.string.zoom_out),
                    style = GalleryText.BodyXLargeSemiBold,
                    color = white,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(start = 40.sdp, end = 40.sdp, top = 5.sdp)
                )
                Text(
                    text = stringResource(R.string.change_number),
                    style = GalleryText.BodyLargeSemiBold,
                    color = white,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(start = 40.sdp, end = 40.sdp, top = 5.sdp)
                )
            }
        }
        // AppCompatButton with no vertical padding: framework minimum 88 x 48dp, text centred
        Text(
            text = stringResource(R.string.got_it),
            style = GalleryText.BodyMediumSemiBold,
            color = white,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(bottom = 15.sdp)
                .clip(RoundedCornerShape(35.sdp))
                .drawableBackground(R.drawable.pinch_in_out_btn)
                .clickable(onClick = onGotIt)
                .defaultMinSize(minWidth = 88.dp, minHeight = 48.dp)
                .padding(horizontal = 35.sdp)
                .wrapContentSize(Alignment.Center)
        )
    }
}


/**
 * layout_home_vault_hint: hint_home scrim over the whole Home (elevation 9.sdp, under the pinch tutorial).
 * The pill repeats [title] so its text lands on [titleBounds] (window coordinates of the top-bar title, HA.Oooooo).
 */
@Composable
fun VaultHintOverlay(title: String, titleBounds: Rect?, onClose: () -> Unit, onOpenVault: () -> Unit) {
    val haptic = LocalHapticFeedback.current
    val vaultLabel = stringResource(R.string.vault_hint_title)
    val white = colorResource(R.color.onlyWhite)
    var origin by remember { mutableStateOf(Offset.Zero) }
    Box(
        modifier = Modifier
            .fillMaxSize()
            .onGloballyPositioned { origin = it.positionInWindow() }
            .background(colorResource(R.color.hint_home))
            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = onClose) // S272/bw/i case 1
    ) {
        val textInsetX = 6.sdp
        val textInsetY = 4.sdp
        val fallbackStart = 40.sdp
        val fallbackTop = 5.sdp
        Column(
            modifier = Modifier.layout { measurable, constraints ->
                val rtl = layoutDirection == LayoutDirection.Rtl
                val insetX = textInsetX.roundToPx()
                // Edge the block starts from, in this box's coordinates: left edge in LTR, right edge in RTL
                val startEdge = when {
                    titleBounds == null -> if (rtl) constraints.maxWidth - fallbackStart.roundToPx() else fallbackStart.roundToPx()
                    rtl -> (titleBounds.right - origin.x).toInt() + insetX
                    else -> (titleBounds.left - origin.x).toInt() - insetX
                }.coerceIn(0, constraints.maxWidth)
                val top = titleBounds?.let { (it.top - origin.y).toInt() - textInsetY.roundToPx() } ?: fallbackTop.roundToPx()
                val room = if (rtl) startEdge else constraints.maxWidth - startEdge
                val placeable = measurable.measure(constraints.copy(minWidth = 0, minHeight = 0, maxWidth = room))
                layout(constraints.maxWidth, constraints.maxHeight) {
                    placeable.place(if (rtl) startEdge - placeable.width else startEdge, top)
                }
            }
        ) {
            Row(
                modifier = Modifier
                    .heightIn(min = 24.sdp)
                    .paint(painterResource(R.drawable.ic_hint_home), contentScale = ContentScale.FillBounds)
                    // the raw long-press below is invisible to TalkBack; expose the same action
                    .semantics { onLongClick(label = vaultLabel) { onClose(); onOpenVault(); true } }
                    .pointerInput(onOpenVault) {
                        // S272/bw/l: long-press = long-pressing the title; a short tap on the pill does nothing
                        detectTapGestures(onLongPress = {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            onClose()
                            onOpenVault()
                        })
                    }
                    .padding(horizontal = textInsetX, vertical = textInsetY),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = title,
                    style = GalleryText.BodyXXLargeSemiBold,
                    color = Color(0xFF1F3355), // hard-coded -0xe0ccab in both themes
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(end = 10.sdp) // room for the lock glyph in ic_hint_home
                )
            }
            Row(modifier = Modifier.padding(start = 16.sdp)) {
                Image(
                    painter = painterResource(R.drawable.ic_vault_hint_arrow),
                    contentDescription = null,
                    modifier = Modifier.padding(bottom = 6.sdp)
                )
                Column(modifier = Modifier.align(Alignment.Bottom)) {
                    Text(
                        text = stringResource(R.string.vault_hint_title),
                        style = GalleryText.BodyMediumMedium,
                        color = white,
                        modifier = Modifier.padding(top = 4.sdp)
                    )
                    Text(
                        text = stringResource(R.string.vault_hint_description),
                        style = GalleryText.BodySmallRegular,
                        color = white
                    )
                }
            }
        }
    }
}

/**
 * dialog_exit through S272/jt/a: bottom sheet window, full width, wrap height, transparent background, system dim.
 * Cancelable, so Back and an outside tap call [onDismiss]; the text calls [onExit]. The ad frame stays empty (D5).
 */
@Composable
fun HomeExitPopup(onExit: () -> Unit, onDismiss: () -> Unit) {
    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        val window = (LocalView.current.parent as? DialogWindowProvider)?.window
        SideEffect {
            window?.run {
                setGravity(Gravity.BOTTOM)
                setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
                setBackgroundDrawable(ColorDrawable(AndroidColor.TRANSPARENT))
            }
        }
        // _15sdp is a dp dimension used as textSize
        val textSize = with(LocalDensity.current) { 15.sdp.toSp() }
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .drawableBackground(R.drawable.exit_rectangle) // 25.dp top corners, unscaled
                .padding(4.dp)
        ) {
            Text(
                text = stringResource(R.string.tap_again_to_exit),
                style = TextStyle(fontFamily = Inter, fontWeight = FontWeight.W400, fontSize = textSize),
                color = Color(0xFF9A9A9A),
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(55.dp)
                    .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = onExit)
                    .wrapContentSize(Alignment.Center)
            )
        }
    }
}
