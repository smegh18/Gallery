package com.quizango.gallery.ui.viewer

import androidx.compose.runtime.Composable
import com.quizango.gallery.domain.model.MediaItem
import com.quizango.gallery.ui.home.RenameAlbumDialog
import com.quizango.gallery.ui.navigation.Screen
import java.io.File

internal enum class ViewerDialogKind { RENAME }

/** An open viewer dialog and the page it was opened on; it never acts on any other page. */
internal data class ViewerDialog(val kind: ViewerDialogKind, val path: String)

/** The captured path, only while that page is still the current one (a sync or swipe may have moved the pager). */
internal fun ViewerDialog.targetIfCurrent(currentPath: String): String? = path.takeIf { it == currentPath }

/** More → Show on map: only for an item with a place (MF `getPlace()` non-empty). No Maps key needed. */
internal fun showOnMapRoute(item: MediaItem): String? =
    item.placeName.takeIf { it.isNotBlank() }?.let { Screen.AlbumDetail.createRoute("filter:place", it) }

/** dialog_rename for the current file: name without extension, the extension kept (ci/h). */
@Composable
internal fun FileRenameDialog(item: MediaItem, onSave: (String) -> Unit, onDismiss: () -> Unit) {
    val file = File(item.fileName)
    RenameAlbumDialog(
        current = file.nameWithoutExtension,
        parent = File(item.filePath).parentFile ?: File("/"),
        onSave = onSave,
        onDismiss = onDismiss,
        ext = file.extension
    )
}
