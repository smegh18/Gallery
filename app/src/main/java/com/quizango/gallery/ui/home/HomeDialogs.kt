package com.quizango.gallery.ui.home

import android.graphics.Color as AndroidColor
import android.graphics.drawable.ColorDrawable
import android.view.Gravity
import android.os.Environment
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.Toast
import androidx.annotation.StringRes
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextAlign
import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
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
import androidx.compose.ui.AbsoluteAlignment
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.shadow
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
                setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE) // bz/e: field stays above the keyboard
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
                    Modifier.weight(1f).padding(horizontal = 8.sdp), onClick = onDismiss
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

/**
 * dialog_select_add (S272/oa/w), shown after an album is picked for the selection: "Adding Media", Move, Copy, and a
 * Cancel pill on the right. Not cancelable: Back and outside taps do nothing (setCancelable(false)).
 */
@Composable
internal fun AddingMediaDialog(onMove: () -> Unit, onCopy: () -> Unit, onCancel: () -> Unit) {
    val titleColor = colorResource(R.color.titleColor)
    CommonDialog(onCancel, cancelable = false) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .shadow(2.sdp, RoundedCornerShape(14.sdp))
                .background(colorResource(R.color.dialogBack), RoundedCornerShape(14.sdp))
                .padding(vertical = 16.sdp)
        ) {
            Text(
                text = stringResource(R.string.adding_media),
                style = GalleryText.BodyXLargeSemiBold,
                color = titleColor,
                modifier = Modifier.padding(start = 16.sdp)
            )
            AddingMediaRow(R.string.move, Modifier.padding(top = 10.sdp), onMove)
            Spacer(
                Modifier
                    .padding(horizontal = 12.sdp)
                    .fillMaxWidth()
                    .height(1.dp)
                    .background(colorResource(R.color.dividerColor))
            )
            AddingMediaRow(R.string.copy, Modifier, onCopy)
            // layout_gravity="right" is absolute, so it stays right in RTL too
            DialogButton(
                R.string.cancel, colorResource(R.color.fastScrollerColor), R.drawable.help_delete_cancel_bg,
                Modifier
                    .align(AbsoluteAlignment.Right)
                    .padding(top = 12.sdp, bottom = 2.sdp, end = 20.sdp),
                horizontalPadding = 40.sdp,
                onClick = onCancel
            )
        }
    }
}

@Composable
private fun AddingMediaRow(@StringRes label: Int, modifier: Modifier, onClick: () -> Unit) {
    Text(
        text = stringResource(label),
        style = GalleryText.BodyLargeMedium,
        color = colorResource(R.color.titleColor),
        modifier = modifier
            .padding(start = 15.sdp)
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.sdp, vertical = 10.sdp)
    )
}

/**
 * dialog_hide (S272/oa/n) as Home opens it (HomeActivity.o000Ooo): "Hide Media" / "Are you sure you want to hide
 * media?", Cancel / Ok. Back and outside taps cancel.
 */
@Composable
internal fun HideMediaDialog(onConfirm: () -> Unit, onDismiss: () -> Unit) {
    val titleColor = colorResource(R.color.titleColor)
    CommonDialog(onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(colorResource(R.color.dialogBack), RoundedCornerShape(14.sdp))
                .padding(vertical = 5.sdp)
        ) {
            Text(
                text = stringResource(R.string.hide_media),
                style = GalleryText.BodyXLargeSemiBold,
                color = titleColor,
                modifier = Modifier.padding(start = 15.sdp, top = 10.sdp)
            )
            Text(
                text = stringResource(R.string.hide_item_permission),
                style = GalleryText.BodyMediumRegular,
                color = titleColor,
                modifier = Modifier.padding(horizontal = 15.sdp).padding(vertical = 15.sdp)
            )
            Row(Modifier.fillMaxWidth().padding(horizontal = 15.sdp, vertical = 10.sdp)) {
                DialogButton(
                    R.string.cancel, colorResource(R.color.fastScrollerColor), R.drawable.help_delete_cancel_bg,
                    Modifier.weight(1f).padding(horizontal = 10.sdp), onClick = onDismiss
                )
                DialogButton(
                    R.string.ok, colorResource(R.color.onlyWhite), R.drawable.btn_permission,
                    Modifier.weight(1f).padding(horizontal = 10.sdp), onClick = onConfirm
                )
            }
        }
    }
}

private const val ALBUM_NAME_MAX = 50

/** S272/ua/a: an insertion made only of these symbols is dropped, so "/" or ":" can't be typed into a folder name. */
private val ALBUM_NAME_REJECT = Regex(
    "^[@#\\\\\$%/:?\"<|'+!\\[\\]>=×÷*~`;?§✓•√π¶∆£€¢^°{}" +
        "©®™℅₹؟؛¥]*\$"
)

internal enum class AlbumNameCheck { EMPTY, TOO_LONG, OK }

/** S272/bz/e OK click on the trimmed name: empty does nothing; `if-lt 0x32`, so exactly 50 characters is refused too. */
internal fun checkAlbumName(trimmed: String): AlbumNameCheck = when {
    trimmed.isEmpty() -> AlbumNameCheck.EMPTY
    trimmed.length >= ALBUM_NAME_MAX -> AlbumNameCheck.TOO_LONG
    else -> AlbumNameCheck.OK
}

/** The name field's filters (S272/bz/e): ua/a on the inserted text (trimmed when longer than 1), then LengthFilter(50). */
internal fun filterAlbumNameEdit(old: String, new: String): String {
    val start = old.commonPrefixWith(new).length
    val end = new.length - old.substring(start).commonSuffixWith(new.substring(start)).length
    val inserted = new.substring(start, end)
    val chunk = if (inserted.length > 1) inserted.trim() else inserted
    return if (inserted.isNotEmpty() && ALBUM_NAME_REJECT.matches(chunk)) old else new.take(ALBUM_NAME_MAX)
}

/** S272/s9/g: a file with exactly that name, or a folder with that name in any case, in one of [roots]. */
internal fun albumNameExists(name: String, roots: List<File>): Boolean = roots.any { root ->
    File(root, name).exists() ||
        root.listFiles()?.any { it.isDirectory && it.name.equals(name, ignoreCase = true) } == true
}

/** S272/ua/c albumPath and organizerPath; new albums go in the first. */
internal fun albumRoots(): List<File> {
    val pictures = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES)
    return listOf(pictures, File(pictures, "Gallery/Organizer"))
}

/**
 * dialog_create_album (S272/bz/e): underlined EditTextTheme field with the keyboard up, word capitals, 50-character limit.
 * OK is tinted dividerColor until there is text (bz/c), refuses names of 50+ characters or names already taken, and
 * otherwise hands over the trimmed name; the caller closes the dialog.
 */
@Composable
internal fun CreateAlbumDialog(onCreate: (String) -> Unit, onDismiss: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var name by rememberSaveable { mutableStateOf("") }
    var checking by remember { mutableStateOf(false) }
    var focused by remember { mutableStateOf(false) }
    val focus = remember { FocusRequester() }
    val keyboard = LocalSoftwareKeyboardController.current
    LaunchedEffect(Unit) {
        focus.requestFocus()
        keyboard?.show()
    }
    val titleColor = colorResource(R.color.titleColor)
    // EditTextTheme: colorControlNormal album_dialog, colorControlActivated dark_blue
    val lineColor = colorResource(if (focused) R.color.dark_blue else R.color.album_dialog)
    val tooLong = stringResource(R.string.album_name_cannot_exceed_more_than_50_characters)
    val taken = stringResource(R.string.album_name_already_exists)
    CommonDialog(onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(colorResource(R.color.dialogBack), RoundedCornerShape(16.sdp))
                .padding(vertical = 5.sdp)
        ) {
            Text(
                text = stringResource(R.string.create_an_album),
                style = GalleryText.BodyXLargeSemiBold,
                color = titleColor,
                modifier = Modifier.padding(start = 19.sdp, top = 15.sdp)
            )
            Text(
                text = stringResource(R.string.album_name),
                style = GalleryText.BodySmallRegular,
                color = titleColor,
                modifier = Modifier.padding(start = 19.sdp, top = 15.sdp)
            )
            val textStyle = GalleryText.BodyMediumMedium.copy(color = colorResource(R.color.black))
            BasicTextField(
                value = name,
                onValueChange = { name = filterAlbumNameEdit(name, it) },
                singleLine = true,
                textStyle = textStyle,
                cursorBrush = SolidColor(colorResource(R.color.dark_blue)),
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.sdp)
                    .focusRequester(focus)
                    .onFocusChanged { focused = it.isFocused }
                    // abc_edit_text_material: 4dp inset line, 1dp, 2dp while focused
                    .drawBehind {
                        val stroke = (if (focused) 2.dp else 1.dp).toPx()
                        val y = size.height - stroke / 2
                        drawLine(lineColor, Offset(4.dp.toPx(), y), Offset(size.width - 4.dp.toPx(), y), stroke)
                    }
                    .padding(horizontal = 4.dp, vertical = 10.sdp),
                decorationBox = { field ->
                    if (name.isEmpty()) {
                        Text(stringResource(R.string.enter_album_name), style = textStyle, color = colorResource(R.color.hintLanguageColor))
                    }
                    field()
                }
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 5.sdp)
                    .padding(horizontal = 19.sdp, vertical = 10.sdp)
            ) {
                DialogButton(
                    R.string.cancel, colorResource(R.color.fastScrollerColor), R.drawable.help_delete_cancel_bg,
                    Modifier.weight(1f).padding(end = 10.sdp), onClick = onDismiss
                )
                Text(
                    text = stringResource(R.string.caller_ok),
                    style = GalleryText.BodyMediumSemiBold,
                    color = colorResource(R.color.onlyWhite),
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .weight(1f)
                        .padding(start = 10.sdp)
                        .clip(RoundedCornerShape(50))
                        // btn_permission under bz/c's background tint
                        .background(colorResource(if (name.isBlank()) R.color.dividerColor else R.color.colorPrimary))
                        .clickable {
                            val trimmed = name.trim()
                            when (checkAlbumName(trimmed)) {
                                AlbumNameCheck.EMPTY -> Unit
                                AlbumNameCheck.TOO_LONG -> Toast.makeText(context, tooLong, Toast.LENGTH_SHORT).show()
                                AlbumNameCheck.OK -> if (!checking) {
                                    checking = true
                                    scope.launch {
                                        val exists = withContext(Dispatchers.IO) { albumNameExists(trimmed, albumRoots()) }
                                        checking = false
                                        if (exists) Toast.makeText(context, taken, Toast.LENGTH_SHORT).show() else onCreate(trimmed)
                                    }
                                }
                            }
                        }
                        .padding(vertical = 10.sdp)
                )
            }
        }
    }
}
