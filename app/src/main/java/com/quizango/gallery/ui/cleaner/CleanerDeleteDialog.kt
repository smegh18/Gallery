package com.quizango.gallery.ui.cleaner

import androidx.annotation.StringRes
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.quizango.gallery.R
import com.quizango.gallery.ui.home.CommonDialog
import com.quizango.gallery.ui.theme.GalleryText
import com.quizango.gallery.ui.theme.sdp

private val PILL = RoundedCornerShape(25.dp)

/**
 * dialog_cleaner_permanently_delete (S272/dw/b): title, subtitle, red "Permanently delete" and outlined "Cancel".
 * Files go for good (no Recycle Bin), as the copy says.
 */
@Composable
internal fun CleanerDeleteDialog(@StringRes title: Int, @StringRes subtitle: Int, onConfirm: () -> Unit, onDismiss: () -> Unit) {
    CommonDialog(onDismiss) {
        Column(
            Modifier
                .fillMaxWidth()
                .background(colorResource(R.color.dialogBack), RoundedCornerShape(14.sdp))
                .padding(horizontal = 20.sdp, vertical = 10.sdp)
        ) {
            Text(
                text = stringResource(title),
                style = GalleryText.BodyXLargeBold,
                color = colorResource(R.color.ic_cleaner_expand_arrow_color),
                modifier = Modifier.padding(top = 5.sdp)
            )
            Text(
                text = stringResource(subtitle),
                style = GalleryText.BodyMediumMedium,
                color = colorResource(R.color.cleaner_delete_sub_text),
                modifier = Modifier.padding(top = 10.sdp)
            )
            DialogPill(
                text = stringResource(R.string.permanently_delete),
                textColor = colorResource(R.color.onlyWhite),
                modifier = Modifier.padding(top = 18.sdp).background(colorResource(R.color.cleaner_red), PILL),
                onClick = onConfirm
            )
            // permanent_btn_border: 1 dp #969696 pill outline
            DialogPill(
                text = stringResource(R.string.cancel),
                textColor = colorResource(R.color.item_cleaner_sub_txt),
                modifier = Modifier.padding(top = 7.sdp).border(1.dp, Color(0xFF969696), PILL),
                onClick = onDismiss
            )
        }
    }
}

@Composable
private fun DialogPill(text: String, textColor: Color, modifier: Modifier, onClick: () -> Unit) {
    Box(
        modifier.fillMaxWidth().height(40.sdp).clip(PILL).clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(text = text, style = GalleryText.BodyLargeSemiBold, color = textColor)
    }
}
