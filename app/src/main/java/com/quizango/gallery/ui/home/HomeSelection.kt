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
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.LocalMinimumInteractiveComponentSize
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
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
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.core.content.ContextCompat
import com.quizango.gallery.R
import com.quizango.gallery.ui.theme.GalleryText
import com.quizango.gallery.ui.theme.Inter
import com.quizango.gallery.ui.theme.sdp
import com.quizango.gallery.ui.theme.ssp

internal enum class DeleteAction { MOVE_TO_RECYCLE_BIN, DELETE_PERMANENTLY }

/** The only place the dialog's checkbox state turns into a delete kind. */
internal fun deleteActionFor(moveToRecycleBinChecked: Boolean): DeleteAction =
    if (moveToRecycleBinChecked) DeleteAction.MOVE_TO_RECYCLE_BIN else DeleteAction.DELETE_PERMANENTLY

/** S272/oa/j.smali onCreate checks the box. */
internal const val MOVE_TO_RECYCLE_BIN_DEFAULT = true

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
    width: Dp,
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

/** dialog_delete (S272/oa/j.smali): No and outside tap delete nothing; Yes passes the checkbox state. */
@Composable
internal fun DeleteMediaDialog(
    count: Int,
    onConfirm: (moveToRecycleBinChecked: Boolean) -> Unit,
    onDismiss: () -> Unit
) {
    var moveToBin by rememberSaveable { mutableStateOf(MOVE_TO_RECYCLE_BIN_DEFAULT) }
    val titleColor = colorResource(R.color.titleColor)
    val accent = colorResource(R.color.fastScrollerColor)
    val title = if (count == 1) stringResource(R.string.delete_dialog_permission)
    else stringResource(R.string.deleted_title) + " " + count + " " + stringResource(R.string.deleted_items)
    Dialog(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(colorResource(R.color.dialogBack), RoundedCornerShape(14.sdp))
                .padding(vertical = 5.sdp)
        ) {
            Text(
                text = title,
                style = GalleryText.BodyLargeMedium,
                color = titleColor,
                modifier = Modifier.padding(start = 15.sdp, end = 15.sdp, top = 18.sdp, bottom = 15.sdp)
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .toggleable(value = moveToBin, role = Role.Checkbox, onValueChange = { moveToBin = it })
                    .padding(horizontal = 10.sdp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // minWidth/minHeight 0 in the reference; the whole row is the touch target instead
                CompositionLocalProvider(LocalMinimumInteractiveComponentSize provides 0.dp) {
                    Checkbox(
                        checked = moveToBin,
                        onCheckedChange = null,
                        colors = CheckboxDefaults.colors(checkedColor = accent, uncheckedColor = accent),
                        modifier = Modifier.padding(start = 6.dp)
                    )
                }
                Text(
                    text = stringResource(R.string.delete_dialog_move),
                    style = GalleryText.BodyMediumRegular,
                    color = titleColor,
                    minLines = 2,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(start = 5.sdp)
                )
            }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 15.sdp)
                    .padding(horizontal = 26.sdp, vertical = 10.sdp)
            ) {
                DialogButton(R.string.no, accent, R.drawable.help_delete_cancel_bg, Modifier.weight(1f).padding(end = 5.sdp), onDismiss)
                DialogButton(
                    R.string.yes, colorResource(R.color.onlyWhite), R.drawable.btn_permission,
                    Modifier.weight(1f).padding(start = 5.sdp)
                ) { onConfirm(moveToBin) }
            }
        }
    }
}

@Composable
private fun DialogButton(@StringRes label: Int, textColor: androidx.compose.ui.graphics.Color, @DrawableRes background: Int, modifier: Modifier, onClick: () -> Unit) {
    Text(
        text = stringResource(label),
        style = GalleryText.BodyMediumSemiBold,
        color = textColor,
        textAlign = TextAlign.Center,
        modifier = modifier
            .clip(RoundedCornerShape(50))
            .drawableBackground(background)
            .clickable(onClick = onClick)
            .padding(vertical = 10.sdp)
    )
}
