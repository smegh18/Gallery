package com.quizango.gallery.ui.editor

import androidx.annotation.DrawableRes
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import com.quizango.gallery.R
import com.quizango.gallery.ui.home.CommonDialog
import com.quizango.gallery.ui.home.drawableBackground
import com.quizango.gallery.ui.theme.GalleryText
import com.quizango.gallery.ui.theme.sdp

/**
 * dialog_discard_changes (S272/cw/b): Back with unsaved edits in the editor and the Remove BG / Blur / Color Splash tools.
 * No and Back keep editing; Yes leaves without saving.
 */
@Composable
internal fun DiscardChangesDialog(onDiscard: () -> Unit, onDismiss: () -> Unit) {
    CommonDialog(onDismiss) {
        Column(
            Modifier
                .fillMaxWidth()
                .background(colorResource(R.color.dialogBack), RoundedCornerShape(14.sdp))
                .padding(vertical = 5.sdp)
        ) {
            Text(
                text = stringResource(R.string.discard_changes),
                style = GalleryText.BodyXLargeSemiBold,
                color = colorResource(R.color.titleColor),
                modifier = Modifier.padding(start = 15.sdp, top = 10.sdp)
            )
            Text(
                text = stringResource(R.string.discard_permission),
                style = GalleryText.BodyMediumRegular,
                color = colorResource(R.color.titleColor),
                modifier = Modifier.padding(15.sdp)
            )
            Row(Modifier.fillMaxWidth().padding(horizontal = 5.sdp, vertical = 10.sdp)) {
                DialogButton(R.string.no, R.drawable.help_delete_cancel_bg, colorResource(R.color.fastScrollerColor), onDismiss)
                DialogButton(R.string.yes, R.drawable.btn_permission, colorResource(R.color.onlyWhite), onDiscard)
            }
        }
    }
}

@Composable
private fun RowScope.DialogButton(text: Int, @DrawableRes background: Int, color: Color, onClick: () -> Unit) {
    Text(
        text = stringResource(text),
        style = GalleryText.BodyMediumSemiBold,
        color = color,
        textAlign = TextAlign.Center,
        modifier = Modifier
            .weight(1f)
            .padding(horizontal = 10.sdp)
            .drawableBackground(background)
            .clip(RoundedCornerShape(50))
            .clickable(onClick = onClick)
            .padding(vertical = 10.sdp)
    )
}
