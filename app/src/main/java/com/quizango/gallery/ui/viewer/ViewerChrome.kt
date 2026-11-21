package com.quizango.gallery.ui.viewer

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupPositionProvider
import androidx.compose.ui.window.PopupProperties
import com.quizango.gallery.ui.theme.GalleryText
import com.quizango.gallery.ui.theme.sdp
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

/** popmenu_media_more rows in reference order. */
internal enum class MoreAction(@DrawableRes val icon: Int, @StringRes val label: Int) {
    ADD_TO(R.drawable.ic_add, R.string.add_to),
    RENAME(R.drawable.ic_rename, R.string.rename),
    SLIDE_SHOW(R.drawable.ic_slideshow, R.string.slide_show),
    WALLPAPER(R.drawable.ic_set_as, R.string.set_as_wallpaper),
    PRINT(R.drawable.ic_print, R.string.print),
    SHOW_ON_MAP(R.drawable.ic_map, R.string.show_on_map)
}

/**
 * MF More popup per type: normal shows everything, Show on map only with a place; the vault (type 2) hides Add to and
 * Show on map. Slide show stays out of the vault, and Wallpaper / Print need a still image to decode.
 */
internal fun moreActions(isVault: Boolean, isVideo: Boolean, hasPlace: Boolean): List<MoreAction> = buildList {
    if (!isVault) add(MoreAction.ADD_TO)
    add(MoreAction.RENAME)
    if (!isVault) add(MoreAction.SLIDE_SHOW)
    if (!isVideo) {
        add(MoreAction.WALLPAPER)
        add(MoreAction.PRINT)
    }
    if (!isVault && hasPlace) add(MoreAction.SHOW_ON_MAP)
}

/** Puts the popup's bottom on the anchor's top, end-aligned with it (Gravity.BOTTOM above the dock). */
private object AboveAnchorEnd : PopupPositionProvider {
    override fun calculatePosition(
        anchorBounds: IntRect,
        windowSize: IntSize,
        layoutDirection: LayoutDirection,
        popupContentSize: IntSize
    ): IntOffset {
        val x = if (layoutDirection == LayoutDirection.Ltr) anchorBounds.right - popupContentSize.width else anchorBounds.left
        return IntOffset(x, anchorBounds.top - popupContentSize.height)
    }
}

/** popmenu_media_more: dialogBack card, radius 16.sdp, 160.sdp wide, anchored above the dock it is placed in. */
@Composable
internal fun ViewerMoreCard(actions: List<MoreAction>, onAction: (MoreAction) -> Unit, onDismiss: () -> Unit) {
    Popup(popupPositionProvider = AboveAnchorEnd, onDismissRequest = onDismiss, properties = PopupProperties(focusable = true)) {
        Column(
            Modifier
                .width(160.sdp)
                .shadow(1.dp, RoundedCornerShape(16.sdp))
                .background(colorResource(R.color.dialogBack), RoundedCornerShape(16.sdp))
                .padding(horizontal = 8.sdp, vertical = 5.sdp)
        ) {
            actions.forEach { action ->
                Row(
                    Modifier
                        .fillMaxWidth()
                        .clickable { onAction(action) }
                        .padding(vertical = 5.sdp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(painterResource(action.icon), null, tint = Color.Unspecified, modifier = Modifier.padding(5.sdp))
                    Text(
                        stringResource(action.label),
                        style = GalleryText.BodySmallMedium,
                        color = colorResource(R.color.titleColor),
                        maxLines = 1,
                        modifier = Modifier.padding(start = 5.sdp)
                    )
                }
            }
        }
    }
}
