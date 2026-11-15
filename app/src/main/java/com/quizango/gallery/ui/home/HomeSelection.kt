package com.quizango.gallery.ui.home

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.core.content.ContextCompat
import com.quizango.gallery.R
import com.quizango.gallery.ui.theme.GalleryText
import com.quizango.gallery.ui.theme.Inter
import com.quizango.gallery.ui.theme.sdp
import com.quizango.gallery.ui.theme.ssp

/** activity_home in selection mode (HA.o0000o0): backBtn, selectTitle, mediaSelectAllBtn. */
@Composable
internal fun HomeSelectionTopBar(
    count: Int,
    allSelected: Boolean,
    onClose: () -> Unit,
    onToggleAll: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(colorResource(R.color.backgroundColor))
            .statusBarsPadding()
            .height(42.sdp)
            .padding(end = 7.sdp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .padding(start = 5.sdp)
                .size(32.sdp)
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = ripple(bounded = false),
                    onClick = onClose
                )
                .padding(8.sdp)
        ) {
            Image(
                painter = painterResource(R.drawable.ic_close),
                contentDescription = stringResource(R.string.cancel),
                colorFilter = ColorFilter.tint(colorResource(R.color.home_shell_icon)),
                modifier = Modifier.fillMaxSize()
            )
        }
        Text(
            text = stringResource(R.string.items_selected, count),
            style = TextStyle(fontFamily = Inter, fontWeight = FontWeight.W600, fontSize = 12.ssp),
            color = colorResource(R.color.home_shell_title),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(start = 3.sdp).weight(1f)
        )
        Text(
            text = stringResource(if (allSelected) R.string.unselect_all else R.string.select_all),
            style = GalleryText.BodySmallMedium,
            color = colorResource(R.color.colorPrimary),
            modifier = Modifier
                .padding(end = 5.sdp)
                .clickable(onClick = onToggleAll)
                .padding(3.sdp)
        )
    }
}

/** layout_select_media_bottom: replaces the tab bar while selecting. */
@Composable
internal fun HomeSelectionBottomBar(
    onShare: () -> Unit,
    onFavorite: () -> Unit,
    onAddTo: () -> Unit,
    onHide: () -> Unit,
    onDelete: () -> Unit,
    onCreateAlbum: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .shadow(10.sdp, RoundedCornerShape(topStart = 16.sdp, topEnd = 16.sdp))
            .drawableBackground(R.drawable.select_album_bg),
        contentAlignment = Alignment.TopCenter
    ) {
        Row(
            modifier = Modifier
                .horizontalScroll(rememberScrollState())
                .height(IntrinsicSize.Min)
                .padding(horizontal = 5.sdp)
        ) {
            SelectionAction(50.sdp, R.drawable.ic_home_select_share, R.string.share, onShare)
            SelectionAction(55.sdp, R.drawable.ic_home_select_fav, R.string.favorite, onFavorite)
            SelectionAction(55.sdp, R.drawable.ic_home_select_add, R.string.add_to, onAddTo)
            SelectionAction(63.sdp, R.drawable.ic_home_select_hide, R.string.ad_to_private, onHide)
            SelectionAction(50.sdp, R.drawable.ic_album_select_delete, R.string.delete, onDelete)
            SelectionAction(60.sdp, R.drawable.ic_home_select_create_album, R.string.create_albums, onCreateAlbum)
        }
    }
}

@Composable
private fun SelectionAction(
    width: androidx.compose.ui.unit.Dp,
    @DrawableRes icon: Int,
    @StringRes label: Int,
    onClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .width(width)
            .fillMaxHeight()
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = ripple(bounded = false),
                onClick = onClick
            )
            .padding(vertical = 10.sdp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // The label names the action, so the icon has no content description.
        Image(painterResource(icon), contentDescription = null)
        Text(
            text = stringResource(label),
            style = GalleryText.BodyXSmallMedium,
            color = colorResource(R.color.titleColor),
            modifier = Modifier.padding(top = 4.sdp)
        )
    }
}

/** Draws an XML shape/ripple drawable as the background, which painterResource cannot load. */
@Composable
internal fun Modifier.drawableBackground(@DrawableRes id: Int): Modifier {
    // Resolved on each composition so night colours follow the theme (as PhotosTab's DrawableBox).
    val drawable = ContextCompat.getDrawable(LocalContext.current, id) ?: return this
    return drawBehind {
        drawIntoCanvas { canvas ->
            drawable.setBounds(0, 0, size.width.toInt(), size.height.toInt())
            drawable.draw(canvas.nativeCanvas)
        }
    }
}
