package com.quizango.gallery.ui.floating

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.stringResource
import com.airbnb.lottie.compose.LottieAnimation
import com.airbnb.lottie.compose.LottieCompositionSpec
import com.airbnb.lottie.compose.LottieConstants
import com.airbnb.lottie.compose.rememberLottieComposition
import com.quizango.gallery.R
import com.quizango.gallery.ui.theme.GalleryText
import com.quizango.gallery.ui.theme.sdp

/**
 * com.baselibrary GuideActivity, type 1: a see-through screen over the system "Display over other apps" page with the
 * dialog_overlay_guide_anime card at the bottom. Any touch closes it.
 */
class OverlayGuideActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            val composition = rememberLottieComposition(LottieCompositionSpec.RawRes(R.raw.anime_overlay))
            Box(
                Modifier
                    .fillMaxSize()
                    .pointerInput(Unit) { awaitEachGesture { awaitFirstDown(); finish() } },
                contentAlignment = Alignment.BottomCenter
            ) {
                Column(
                    Modifier
                        .navigationBarsPadding()
                        .fillMaxWidth()
                        .height(100.sdp)
                        .background(colorResource(R.color.colorPrimary), RoundedCornerShape(topStart = 20.sdp, topEnd = 20.sdp)),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        stringResource(R.string.gallery_allow_display),
                        style = GalleryText.BodyLargeSemiBold,
                        color = colorResource(R.color.white), // @color/white, dark in night mode as in 2.7.2
                        modifier = Modifier.padding(top = 12.sdp)
                    )
                    LottieAnimation(
                        composition.value,
                        iterations = LottieConstants.IterateForever,
                        contentScale = ContentScale.Fit,
                        modifier = Modifier.fillMaxWidth().weight(1f).padding(horizontal = 10.sdp)
                    )
                }
            }
        }
    }
}
