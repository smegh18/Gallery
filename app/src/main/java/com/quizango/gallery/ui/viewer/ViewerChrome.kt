package com.quizango.gallery.ui.viewer

import androidx.annotation.StringRes
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.quizango.gallery.R

internal const val CHROME_ANIMATION_MS = 150

data class ViewerChromeControls(
    val rotate: Boolean,
    val favorite: Boolean,
    val capture: Boolean,
    val details: Boolean
)

/** Visibility rules from the reference viewer toolbar. */
fun viewerChromeControls(
    isVideo: Boolean,
    isRecycle: Boolean,
    isVault: Boolean,
    isAnimated: Boolean,
    isHeif: Boolean,
    isShortVideoPlaying: Boolean
): ViewerChromeControls = ViewerChromeControls(
    rotate = !isVideo && !isRecycle && !isVault && !isAnimated && !isHeif,
    favorite = !isRecycle && !isVault && !isShortVideoPlaying,
    capture = isShortVideoPlaying,
    details = !isRecycle && !isVault
)

@Composable
fun ViewerTopChrome(
    visible: Boolean,
    controls: ViewerChromeControls,
    isFavorite: Boolean,
    onBack: () -> Unit,
    onRotate: () -> Unit,
    onFavorite: () -> Unit,
    onCapture: () -> Unit,
    onDetails: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(modifier.fillMaxWidth()) {
        if (!visible) {
            Box(
                Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .height(24.dp)
                    .background(Brush.verticalGradient(listOf(Color(0x73000000), Color.Transparent)))
            )
        }
        AnimatedVisibility(
            visible = visible,
            enter = slideInVertically(animationSpec = tween(CHROME_ANIMATION_MS)) { -it } + fadeIn(tween(CHROME_ANIMATION_MS)),
            exit = slideOutVertically(animationSpec = tween(CHROME_ANIMATION_MS)) { -it } + fadeOut(tween(CHROME_ANIMATION_MS)),
            modifier = Modifier.align(Alignment.TopCenter)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .height(56.dp)
                    .padding(horizontal = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                ViewerGlassButton(R.drawable.ic_back, R.string.viewer_back, onBack)
                Spacer(Modifier.weight(1f))
                if (controls.rotate) ViewerGlassButton(R.drawable.ic_media_rotate, R.string.rotate, onRotate)
                if (controls.favorite) ViewerGlassButton(
                    if (isFavorite) R.drawable.ic_fav_fill else R.drawable.ic_favorite,
                    R.string.favorite,
                    onFavorite
                )
                if (controls.capture) ViewerGlassButton(R.drawable.ic_camera_ss_short, R.string.viewer_capture, onCapture)
                if (controls.details) ViewerGlassButton(R.drawable.ic_property, R.string.details, onDetails)
            }
        }
    }
}

@Composable
private fun ViewerGlassButton(@androidx.annotation.DrawableRes icon: Int, @StringRes label: Int, onClick: () -> Unit) {
    IconButton(
        onClick = onClick,
        modifier = Modifier
            .size(36.dp)
            .clip(androidx.compose.foundation.shape.CircleShape)
            .background(Color(0x8C1C1D21))
    ) {
        Icon(
            painter = painterResource(icon),
            contentDescription = stringResource(label),
            tint = Color.Unspecified,
            modifier = Modifier.size(22.dp)
        )
    }
}
