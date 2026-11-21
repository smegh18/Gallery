package com.quizango.gallery.ui.viewer

import android.graphics.BitmapFactory
import android.text.format.Formatter
import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import com.quizango.gallery.R
import com.quizango.gallery.domain.model.Album
import com.quizango.gallery.domain.model.MediaItem
import com.quizango.gallery.ui.components.AddToAlbumSheet
import com.quizango.gallery.ui.home.AddingMediaDialog
import com.quizango.gallery.ui.home.DeleteMediaDialog
import com.quizango.gallery.ui.home.HideMediaDialog
import com.quizango.gallery.ui.home.PropertyDialog
import com.quizango.gallery.ui.home.RenameAlbumDialog
import com.quizango.gallery.ui.navigation.Screen
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

internal enum class ViewerDialogKind { DETAILS, RENAME, DELETE, RECYCLE_DELETE, VAULT_DELETE, RESTORE, HIDE, UNHIDE, ADD_TO }

/**
 * An open viewer dialog and the page it was opened on; it never acts on any other page.
 * [folder] is the album picked in the Add to sheet, before the Move / Copy choice.
 */
internal data class ViewerDialog(val kind: ViewerDialogKind, val path: String, val folder: String = "")

/** The captured path, only while that page is still the current one (a sync or swipe may have moved the pager). */
internal fun ViewerDialog.targetIfCurrent(currentPath: String): String? = path.takeIf { it == currentPath }

/** More → Show on map: only for an item with a place (MF `getPlace()` non-empty). No Maps key needed. */
internal fun showOnMapRoute(item: MediaItem): String? =
    item.placeName.takeIf { it.isNotBlank() }?.let { Screen.AlbumDetail.createRoute("filter:place", it) }

/** Edit refuses animated and undecodable images with the reference toasts; null when the editor may open. */
@StringRes
internal fun editBlockedMessage(isAnimated: Boolean, decodable: Boolean): Int? = when {
    isAnimated -> R.string.toast_animated_image
    !decodable -> R.string.toast_corrupted_image
    else -> null
}

/** Whether BitmapFactory can read the image's header (a corrupt file cannot). */
internal fun isDecodable(path: String): Boolean {
    val opts = BitmapFactory.Options().apply { inJustDecodeBounds = true }
    BitmapFactory.decodeFile(path, opts)
    return opts.outWidth > 0 && opts.outHeight > 0
}

/** The dialog [dialog] asks for, drawn with the Phase 3 surfaces whose copy the reference viewer reuses. */
@Composable
internal fun ViewerDialogHost(
    dialog: ViewerDialog,
    item: MediaItem,
    albums: List<Album>,
    onConfirm: () -> Unit,
    onDelete: (moveToRecycleBin: Boolean) -> Unit,
    onRename: (String) -> Unit,
    onPickAlbum: (String) -> Unit,
    onAdd: (move: Boolean) -> Unit,
    onDismiss: () -> Unit
) {
    when (dialog.kind) {
        ViewerDialogKind.DETAILS -> MediaDetailsDialog(item, onDismiss)
        ViewerDialogKind.RENAME -> {
            val file = File(item.fileName)
            RenameAlbumDialog(
                current = file.nameWithoutExtension,
                parent = File(item.filePath).parentFile ?: File("/"),
                onSave = onRename,
                onDismiss = onDismiss,
                ext = file.extension
            )
        }
        ViewerDialogKind.DELETE -> DeleteMediaDialog(count = 1, onConfirm = onDelete, onDismiss = onDismiss)
        ViewerDialogKind.VAULT_DELETE -> DeleteMediaDialog(
            count = 1,
            onConfirm = { onConfirm() },
            onDismiss = onDismiss,
            customTitle = stringResource(R.string.deleted_item_permission),
            showMoveToBin = false
        )
        ViewerDialogKind.RECYCLE_DELETE -> HideMediaDialog(onConfirm, onDismiss, R.string.delete_items, R.string.delete_items_description)
        ViewerDialogKind.RESTORE -> HideMediaDialog(onConfirm, onDismiss, R.string.restore_items, R.string.restore_item_permission)
        ViewerDialogKind.HIDE -> HideMediaDialog(onConfirm, onDismiss)
        ViewerDialogKind.UNHIDE -> HideMediaDialog(onConfirm, onDismiss, R.string.unhide_items, R.string.unhide_item_permission)
        ViewerDialogKind.ADD_TO -> if (dialog.folder.isEmpty()) {
            AddToAlbumSheet(albums = albums, previewItem = item, selectedCount = 1, onAlbumClick = { onPickAlbum(it.path) }, onDismiss = onDismiss)
        } else {
            AddingMediaDialog(onMove = { onAdd(true) }, onCopy = { onAdd(false) }, onCancel = onDismiss)
        }
    }
}

/** dialog_property / dialog_video_property: Name, Path, Size, Resolution, Taken on. */
@Composable
internal fun MediaDetailsDialog(item: MediaItem, onDismiss: () -> Unit) {
    val context = LocalContext.current
    val resolution = remember(item.filePath, item.dimension) {
        item.dimension.ifEmpty {
            if (item.isVideo) "" else BitmapFactory.Options().apply { inJustDecodeBounds = true }
                .also { BitmapFactory.decodeFile(item.filePath, it) }
                .let { if (it.outWidth > 0) "${it.outWidth}x${it.outHeight}" else "" }
        }
    }
    val takenOn = remember(item.dateAdded) {
        val millis = if (item.dateAdded > 10_000_000_000L) item.dateAdded else item.dateAdded * 1000L
        SimpleDateFormat("dd/MM/yyyy hh:mm a", Locale.ENGLISH).format(Date(millis))
    }
    PropertyDialog(
        rows = listOf(
            R.string.name to item.fileName,
            R.string.path to item.filePath,
            R.string.size to Formatter.formatFileSize(context, item.fileSize),
            R.string.resolution to resolution,
            R.string.taken_on to takenOn
        ),
        onDismiss = onDismiss
    )
}
