package com.quizango.gallery.ui.browse

import android.widget.Toast
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.platform.LocalContext
import com.quizango.gallery.R
import com.quizango.gallery.data.repository.MediaRepository
import com.quizango.gallery.data.repository.VaultRepository
import com.quizango.gallery.domain.model.MediaItem
import com.quizango.gallery.ui.components.AddToAlbumSheet
import com.quizango.gallery.ui.home.AddingMediaDialog
import com.quizango.gallery.ui.home.CreateAlbumDialog
import com.quizango.gallery.ui.home.DeleteAction
import com.quizango.gallery.ui.home.DeleteMediaDialog
import com.quizango.gallery.ui.home.HideMediaDialog
import com.quizango.gallery.ui.home.SelectionAction
import com.quizango.gallery.ui.home.albumRoots
import com.quizango.gallery.ui.home.allInFolder
import com.quizango.gallery.ui.home.deleteActionFor
import com.quizango.gallery.ui.home.drawableBackground
import com.quizango.gallery.ui.home.shareSelectedMedia
import com.quizango.gallery.ui.theme.sdp
import kotlinx.coroutines.launch
import java.io.File

/** One slot of layout_select_media_bottom; widths are the Phase 3 per-slot values. */
internal enum class MediaAction(@DrawableRes val icon: Int, @StringRes val label: Int, val widthSdp: Int) {
    SHARE(R.drawable.ic_home_select_share, R.string.share, 50),
    FAVORITE(R.drawable.ic_home_select_fav, R.string.favorite, 55),
    UNFAVORITE(R.drawable.ic_fav_fill, R.string.unfavorite, 55),
    ADD_TO(R.drawable.ic_home_select_add, R.string.add_to, 55),
    PRIVATE(R.drawable.ic_home_select_hide, R.string.ad_to_private, 63),
    DELETE(R.drawable.ic_album_select_delete, R.string.delete, 50),
    CREATE_ALBUM(R.drawable.ic_home_select_create_album, R.string.create_albums, 60)
}

/** True when every selected item is already a favourite, so the slot reads Unfavorite (§3.4). */
internal fun allFavorite(items: List<MediaItem>, selected: Set<String>): Boolean =
    items.filter { it.filePath in selected }.let { picked -> picked.isNotEmpty() && picked.all { it.isFavorite } }

/** Media list and Animated (§3.4): six slots. */
internal fun mediaListActions(allFavorite: Boolean): List<MediaAction> = listOf(
    MediaAction.SHARE,
    if (allFavorite) MediaAction.UNFAVORITE else MediaAction.FAVORITE,
    MediaAction.ADD_TO,
    MediaAction.PRIVATE,
    MediaAction.DELETE,
    MediaAction.CREATE_ALBUM
)

/** Favorites (§10): four slots, never Add to or Create album. */
internal val FAVORITES_ACTIONS = listOf(MediaAction.SHARE, MediaAction.UNFAVORITE, MediaAction.PRIVATE, MediaAction.DELETE)

/** layout_select_media_bottom with only [actions] shown, in order. */
@Composable
internal fun MediaSelectionBar(actions: List<MediaAction>, onAction: (MediaAction) -> Unit, modifier: Modifier = Modifier) {
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
            actions.forEach { action ->
                SelectionAction(Modifier.width(action.widthSdp.sdp), action.icon, action.label, { onAction(action) })
            }
        }
    }
}

private sealed interface PendingDialog {
    val items: List<MediaItem>
    data class Delete(override val items: List<MediaItem>) : PendingDialog
    data class Hide(override val items: List<MediaItem>) : PendingDialog
    data class AddTo(override val items: List<MediaItem>) : PendingDialog
    data class CreateAlbum(override val items: List<MediaItem>) : PendingDialog
    data class MoveOrCopy(override val items: List<MediaItem>, val folder: String) : PendingDialog
}

/**
 * Runs the §3.4 bar for a list host and draws its dialogs; returns the handler for [MediaSelectionBar].
 * Every operation acts on the items selected when the slot was tapped, never on list indices, so rows
 * vanishing meanwhile (sync, unfavourite) cannot retarget it. [onDone] clears the selection.
 */
@Composable
internal fun rememberMediaListActions(
    mediaRepository: MediaRepository,
    vaultRepository: VaultRepository,
    items: List<MediaItem>,
    selected: Set<String>,
    onDone: () -> Unit
): (MediaAction) -> Unit {
    val context = LocalContext.current
    // The screen's scope: an operation keeps running when the selection (and the bar) goes away
    val scope = rememberCoroutineScope()
    var pending by remember { mutableStateOf<PendingDialog?>(null) }
    fun toast(@StringRes id: Int) = Toast.makeText(context, context.getString(id), Toast.LENGTH_SHORT).show()

    when (val dialog = pending) {
        is PendingDialog.Delete -> DeleteMediaDialog(
            count = dialog.items.size,
            onConfirm = { checked ->
                pending = null
                val paths = dialog.items.map { it.filePath }
                scope.launch {
                    when (deleteActionFor(checked)) {
                        DeleteAction.MOVE_TO_RECYCLE_BIN -> paths.forEach { mediaRepository.moveToTrash(it) }
                        DeleteAction.DELETE_PERMANENTLY -> mediaRepository.deletePermanently(paths)
                    }
                    toast(R.string.selected_media_files_successfully_deleted)
                    onDone()
                }
            },
            onDismiss = { pending = null }
        )
        is PendingDialog.Hide -> HideMediaDialog(
            onConfirm = {
                pending = null
                scope.launch {
                    dialog.items.forEach { vaultRepository.moveToVault(it) }
                    mediaRepository.syncMediaStore()
                    toast(R.string.selected_media_files_successfully_hide)
                    onDone()
                }
            },
            onDismiss = { pending = null }
        )
        is PendingDialog.AddTo -> {
            val albums by remember { mediaRepository.getAlbumsFlow() }.collectAsState(initial = emptyList())
            AddToAlbumSheet(
                albums = albums,
                previewItem = dialog.items.firstOrNull(),
                selectedCount = dialog.items.size,
                onAlbumClick = { album -> pending = PendingDialog.MoveOrCopy(dialog.items, album.path) },
                onDismiss = { pending = null }
            )
        }
        is PendingDialog.CreateAlbum -> CreateAlbumDialog(
            onCreate = { name ->
                val dir = File(albumRoots().first(), name)
                runCatching { dir.mkdirs() }
                pending = PendingDialog.MoveOrCopy(dialog.items, dir.absolutePath)
            },
            onDismiss = { pending = null }
        )
        is PendingDialog.MoveOrCopy -> {
            fun add(move: Boolean) {
                pending = null
                val paths = dialog.items.map { it.filePath }
                if (move && allInFolder(paths, dialog.folder)) {
                    toast(R.string.media_files_should_not_move_to_same_folder)
                    return
                }
                scope.launch {
                    val done = paths.count {
                        if (move) mediaRepository.moveMediaToFolder(it, dialog.folder) else mediaRepository.copyMediaToFolder(it, dialog.folder)
                    }
                    mediaRepository.syncMediaStore()
                    if (done > 0) toast(if (move) R.string.selected_media_files_successfully_moved else R.string.selected_media_files_successfully_copied)
                    onDone()
                }
            }
            AddingMediaDialog(onMove = { add(move = true) }, onCopy = { add(move = false) }, onCancel = { pending = null })
        }
        null -> Unit
    }

    return { action ->
        val picked = items.filter { it.filePath in selected }
        if (picked.isNotEmpty()) when (action) {
            MediaAction.SHARE -> shareSelectedMedia(context, picked.map { it.filePath }.toSet(), context.getString(R.string.share))
            MediaAction.FAVORITE, MediaAction.UNFAVORITE -> scope.launch {
                picked.forEach { mediaRepository.setFavorite(it.filePath, action == MediaAction.FAVORITE) }
                onDone()
            }
            MediaAction.ADD_TO -> pending = PendingDialog.AddTo(picked)
            MediaAction.PRIVATE -> pending = PendingDialog.Hide(picked)
            MediaAction.DELETE -> pending = PendingDialog.Delete(picked)
            MediaAction.CREATE_ALBUM -> pending = PendingDialog.CreateAlbum(picked)
        }
    }
}
