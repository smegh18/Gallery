package com.quizango.gallery.ui.home

import android.graphics.Color as AndroidColor
import android.graphics.drawable.ColorDrawable
import android.view.Gravity
import android.view.ViewGroup
import androidx.annotation.StringRes
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.LocalMinimumInteractiveComponentSize
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.window.DialogWindowProvider
import com.quizango.gallery.R
import com.quizango.gallery.domain.model.SortDirection
import com.quizango.gallery.domain.model.SortField
import com.quizango.gallery.domain.model.SortOrder
import com.quizango.gallery.ui.theme.GalleryText
import com.quizango.gallery.ui.theme.sdp

/**
 * 2.7.2 style CommonDialog as its dialogs set it up (bz/f, bz/e, oa/j, oa/n, oa/w onCreate): 95% of the screen wide,
 * gravity BOTTOM lifted 10dp, transparent window, slide up / down. [cancelable] false is oa/w's setCancelable(false).
 */
@Composable
internal fun CommonDialog(onDismissRequest: () -> Unit, cancelable: Boolean = true, content: @Composable () -> Unit) {
    Dialog(
        onDismissRequest = onDismissRequest,
        properties = DialogProperties(
            dismissOnBackPress = cancelable,
            dismissOnClickOutside = cancelable,
            usePlatformDefaultWidth = false
        )
    ) {
        val window = (LocalView.current.parent as? DialogWindowProvider)?.window
        val lift = with(LocalDensity.current) { 10.dp.roundToPx() }
        SideEffect {
            window?.run {
                setGravity(Gravity.BOTTOM)
                setLayout((context.resources.displayMetrics.widthPixels * 0.95f).toInt(), ViewGroup.LayoutParams.WRAP_CONTENT)
                attributes = attributes.apply { y = lift }
                setBackgroundDrawable(ColorDrawable(AndroidColor.TRANSPARENT))
                setWindowAnimations(R.style.dialog_window_animation)
            }
        }
        content()
    }
}

private val SORT_FIELDS = listOf(
    R.string.last_modified to SortField.DATE_MODIFIED,
    R.string.filename to SortField.NAME,
    R.string.file_size to SortField.SIZE,
    R.string.created_date to SortField.DATE_TAKEN
)
private val SORT_DIRECTIONS = listOf(
    R.string.ascending_order to SortDirection.ASCENDING,
    R.string.descending_order to SortDirection.DESCENDING
)

/** dialog_sort (S272/bz/f): two radio groups, each closed by a divider, then Cancel / OK. Outside tap and Back cancel. */
@Composable
internal fun SortDialog(current: SortOrder, onApply: (SortOrder) -> Unit, onDismiss: () -> Unit) {
    var field by rememberSaveable { mutableStateOf(current.field) }
    var direction by rememberSaveable { mutableStateOf(current.direction) }
    CommonDialog(onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(colorResource(R.color.dialogBack), RoundedCornerShape(14.sdp))
                .padding(vertical = 15.sdp)
        ) {
            Text(
                text = stringResource(R.string.sort_by),
                style = GalleryText.BodyXLargeSemiBold,
                color = colorResource(R.color.black),
                modifier = Modifier.padding(horizontal = 16.sdp)
            )
            Column(Modifier.padding(top = 5.sdp).padding(horizontal = 16.sdp)) {
                SORT_FIELDS.forEach { (label, value) -> SortRadio(label, field == value) { field = value } }
                SortDivider()
            }
            Column(Modifier.padding(horizontal = 16.sdp)) {
                SORT_DIRECTIONS.forEach { (label, value) -> SortRadio(label, direction == value) { direction = value } }
                SortDivider()
            }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 15.sdp)
                    .padding(horizontal = 15.sdp, vertical = 5.sdp)
            ) {
                DialogButton(
                    R.string.cancel, colorResource(R.color.fastScrollerColor), R.drawable.help_delete_cancel_bg,
                    Modifier.weight(1f).padding(horizontal = 8.sdp), onDismiss
                )
                DialogButton(
                    R.string.ok, colorResource(R.color.onlyWhite), R.drawable.btn_permission,
                    Modifier.weight(1f).padding(horizontal = 8.sdp)
                ) { onApply(SortOrder(field, direction)) }
            }
        }
    }
}

/** RadioButton with sort_selector text (colorPrimary when checked) and bz/f's tint: dark_blue / radioButtonUncheckedColor. */
@Composable
private fun SortRadio(@StringRes label: Int, selected: Boolean, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .selectable(selected = selected, role = Role.RadioButton, onClick = onClick)
            .padding(vertical = 10.sdp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // The row is the touch target, as the AppCompat RadioButton is
        CompositionLocalProvider(LocalMinimumInteractiveComponentSize provides 0.dp) {
            RadioButton(
                selected = selected,
                onClick = null,
                colors = RadioButtonDefaults.colors(
                    selectedColor = colorResource(R.color.dark_blue),
                    unselectedColor = colorResource(R.color.radioButtonUncheckedColor)
                )
            )
        }
        Text(
            text = stringResource(label),
            style = GalleryText.BodyMediumMedium,
            color = colorResource(if (selected) R.color.colorPrimary else R.color.titleColor),
            modifier = Modifier.padding(start = 8.dp)
        )
    }
}

@Composable
private fun SortDivider() {
    Spacer(
        Modifier
            .padding(vertical = 5.sdp)
            .fillMaxWidth()
            .height(1.dp)
            .background(colorResource(R.color.dividerColor))
    )
}
