package com.quizango.gallery.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.airbnb.lottie.compose.LottieAnimation
import com.airbnb.lottie.compose.LottieCompositionSpec
import com.airbnb.lottie.compose.LottieConstants
import com.airbnb.lottie.compose.rememberLottieComposition
import com.quizango.gallery.R
import com.quizango.gallery.ui.theme.GalleryText
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
