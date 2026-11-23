package com.quizango.gallery.ui.home

import android.widget.Toast
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.quizango.gallery.R
import com.quizango.gallery.domain.model.MediaType
import com.quizango.gallery.ui.components.AddToAlbumSheet
import com.quizango.gallery.ui.components.MediaPicker
import java.io.File

/** The dialogs of the Phase 4 album selection (layout_select_album_bottom + top-bar Info). */
internal enum class AlbumDialog { ADD_TO, DELETE, CHANGE_COVER, RENAME, INFO }

/**
 * Draws the album-selection dialogs for [viewModel] and returns the opener; shared by the Albums tab and
 * Other Albums (AllAlbumActivity), so every folder mutation goes through the same validated HomeViewModel calls.
 */
@Composable
internal fun rememberAlbumSelectionDialogs(viewModel: HomeViewModel, uiState: HomeUiState): (AlbumDialog) -> Unit {
    val context = LocalContext.current
    var open by remember { mutableStateOf<AlbumDialog?>(null) }
    var renaming by remember { mutableStateOf<Int?>(null) } // item count while the rename runs
    // Folder picked for the selected albums' files; dialog_select_add then asks Move or Copy
    var addTarget by rememberSaveable { mutableStateOf<String?>(null) }
    val renameFailed = stringResource(R.string.something_went_wrong)
    val copiedMessage = stringResource(R.string.items_copied_successfully)
    val movedMessage = stringResource(R.string.items_moved_successfully)
    val sameFolderMessage = stringResource(R.string.media_files_should_not_move_to_same_folder)
    val close = { open = null }
    // Pin, Change cover, Rename and Info act on exactly one album
    val target = viewModel.changeCoverTarget()

    when (open) {
        AlbumDialog.CHANGE_COVER -> if (target == null) close() else {
            // 2.7.2 opens its in-app picker (InternalSelectMediaActivity, photos only) over the album's media
            Dialog(onDismissRequest = close, properties = DialogProperties(usePlatformDefaultWidth = false)) {
                MediaPicker(
                    title = target.name,
                    items = uiState.mediaList.filter { it.folderPath == target.path && it.mediaType != MediaType.VIDEO },
                    onBack = close,
                    onPick = { item ->
                        close()
                        viewModel.setSelectedAlbumCover(item)
                    }
                )
            }
        }
        AlbumDialog.RENAME -> if (target == null) close() else RenameAlbumDialog(
            current = target.name,
            parent = File(target.path).parentFile ?: File("/"),
            onSave = { name ->
                close()
                renaming = target.itemCount
                viewModel.renameSelectedAlbum(name) { ok ->
                    renaming = null
                    if (!ok) Toast.makeText(context, renameFailed, Toast.LENGTH_SHORT).show()
                }
            },
            onDismiss = close
        )
        AlbumDialog.INFO -> if (target == null) close() else {
            val albumMedia = remember(target.path, uiState.mediaList) { uiState.mediaList.filter { it.folderPath == target.path } }
            AlbumPropertyDialog(
                album = target,
                sizeBytes = remember(albumMedia) { albumMedia.sumOf { it.fileSize } },
                lastModified = remember(albumMedia) { albumMedia.maxOfOrNull { it.dateModified } ?: 0L },
                onDismiss = close
            )
        }
        AlbumDialog.DELETE -> DeleteMediaDialog(
            // Album Delete counts albums, as the top bar does (HA.count of the selection event)
            count = uiState.selectedAlbums.size,
            onConfirm = { checked ->
                close()
                viewModel.deleteSelectedAlbums(deleteActionFor(checked))
            },
            onDismiss = close
        )
        AlbumDialog.ADD_TO -> AddToAlbumSheet(
            albums = uiState.albumsList,
            previewItem = uiState.mediaList.firstOrNull { it.folderPath in uiState.selectedAlbums },
            selectedCount = uiState.albumsList.filter { it.path in uiState.selectedAlbums }.sumOf { it.itemCount },
            onAlbumClick = { album ->
                close()
                addTarget = album.path
            },
            onDismiss = close
        )
        null -> Unit
    }
    renaming?.let { ProgressTextDialog(stringResource(R.string.modifying_items, it)) }
    addTarget?.let { folder ->
        fun add(move: Boolean) {
            addTarget = null
            val message = if (move) movedMessage else copiedMessage
            val started = viewModel.addSelectedAlbumsToFolder(folder, move) { done ->
                if (done > 0) Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
            }
            if (!started) Toast.makeText(context, sameFolderMessage, Toast.LENGTH_SHORT).show()
        }
        AddingMediaDialog(onMove = { add(move = true) }, onCopy = { add(move = false) }, onCancel = { addTarget = null })
    }

    return { dialog ->
        // Single-album actions do nothing for a multi-selection (HA.handleEvents)
        val single = dialog == AlbumDialog.CHANGE_COVER || dialog == AlbumDialog.RENAME || dialog == AlbumDialog.INFO
        if (!single || viewModel.changeCoverTarget() != null) open = dialog
    }
}

/** layout_select_album_bottom wired to [viewModel]; [open] comes from [rememberAlbumSelectionDialogs]. */
@Composable
internal fun AlbumSelectionBottomBar(
    viewModel: HomeViewModel,
    uiState: HomeUiState,
    open: (AlbumDialog) -> Unit,
    modifier: Modifier = Modifier
) = AlbumSelectionBottomBar(
    pinLabel = pinLabel(uiState.selectedAlbums, uiState.albumsList.filter { it.isPinned }.map { it.path }.toSet()),
    singleActionAlpha = singleAlbumActionAlpha(uiState.selectedAlbums.size),
    onPin = viewModel::pinSelectedAlbum,
    onAddTo = { open(AlbumDialog.ADD_TO) },
    onDelete = { open(AlbumDialog.DELETE) },
    onChangeCover = { open(AlbumDialog.CHANGE_COVER) },
    onRename = { open(AlbumDialog.RENAME) },
    modifier = modifier
)
