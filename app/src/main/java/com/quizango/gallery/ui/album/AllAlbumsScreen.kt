package com.quizango.gallery.ui.album

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.stringResource
import com.quizango.gallery.R
import com.quizango.gallery.domain.model.Album
import com.quizango.gallery.ui.browse.AlbumSquareCard
import com.quizango.gallery.ui.browse.ListState
import com.quizango.gallery.ui.browse.SharedMediaToolbar
import com.quizango.gallery.ui.browse.listStateOf
import com.quizango.gallery.ui.home.AlbumDialog
import com.quizango.gallery.ui.home.AlbumSelectionBottomBar
import com.quizango.gallery.ui.home.HomeUiState
import com.quizango.gallery.ui.home.HomeViewModel
import com.quizango.gallery.ui.home.PhotosEmptyState
import com.quizango.gallery.ui.home.albumIsSelectable
import com.quizango.gallery.ui.home.albumRoute
import com.quizango.gallery.ui.home.rememberAlbumSelectionDialogs
import com.quizango.gallery.ui.theme.sdp

/** AllAlbumActivity's list: the device folders in repository order, no virtual albums; null until the first sync. */
internal fun otherAlbums(state: HomeUiState): List<Album>? =
    if (state.albumsList.isEmpty() && !state.initialSyncDone) null else state.albumsList.filter(::albumIsSelectable)

/**
 * "Other Albums" (§5, activity_all_album). 2.7.2 reaches it only from notifications / deep links (Phase 9 wires those);
 * selection reuses the Phase 4 album actions of [viewModel], so pin/add/delete/cover/rename/info behave as on the Albums tab.
 */
@Composable
fun AllAlbumsScreen(
    viewModel: HomeViewModel,
    onNavigate: (String) -> Unit,
    onNavigateBack: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val albums = otherAlbums(uiState)
    val selected = uiState.selectedAlbums
    val selecting = selected.isNotEmpty()
    val openDialog = rememberAlbumSelectionDialogs(viewModel, uiState)
    BackHandler(enabled = selecting) { viewModel.clearAlbumSelection() }

    Column(Modifier.fillMaxSize().background(colorResource(R.color.backgroundColor))) {
        val allSelected = !albums.isNullOrEmpty() && selected.size == albums.size
        SharedMediaToolbar(
            title = if (selecting) stringResource(R.string.items_selected, selected.size) else stringResource(R.string.other_albums),
            selecting = selecting,
            allSelected = allSelected,
            onBack = onNavigateBack,
            onClose = viewModel::clearAlbumSelection,
            onToggleAll = { if (allSelected) viewModel.clearAlbumSelection() else viewModel.selectAllAlbums() },
            onInfo = { openDialog(AlbumDialog.INFO) }.takeIf { selected.size == 1 }
        )
        Box(Modifier.weight(1f).fillMaxWidth()) {
            when (listStateOf(albums, 3)) {
                ListState.LOADING -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = colorResource(R.color.colorPrimary))
                }
                ListState.EMPTY -> PhotosEmptyState(R.string.no_items, body = null)
                ListState.CONTENT -> LazyVerticalGrid(
                    columns = GridCells.Fixed(3),
                    contentPadding = PaddingValues(horizontal = 2.sdp, vertical = 4.sdp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    // item_ad_all_album rows stay collapsed (D5), so none are added
                    items(albums.orEmpty(), key = { it.path }) { album ->
                        AlbumSquareCard(
                            title = album.name,
                            count = album.itemCount,
                            cover = album.coverUri,
                            selectionMark = if (selecting) album.path in selected else null,
                            onClick = {
                                if (selecting) viewModel.toggleAlbum(album.path) else onNavigate(albumRoute(album, album.name))
                            },
                            onLongClick = { viewModel.toggleAlbum(album.path) },
                            modifier = Modifier.padding(end = 8.sdp, bottom = 8.sdp)
                        )
                    }
                }
            }
        }
        if (selecting) AlbumSelectionBottomBar(viewModel, uiState, openDialog)
        Spacer(Modifier.navigationBarsPadding())
    }
}
