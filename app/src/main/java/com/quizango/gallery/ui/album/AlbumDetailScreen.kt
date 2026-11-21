package com.quizango.gallery.ui.album

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.quizango.gallery.data.repository.MediaRepository
import com.quizango.gallery.data.repository.VaultRepository
import com.quizango.gallery.domain.model.MediaItem
import com.quizango.gallery.ui.browse.SharedMediaList
import com.quizango.gallery.ui.browse.allFavorite
import com.quizango.gallery.ui.browse.mediaListActions
import com.quizango.gallery.ui.browse.rememberMediaListActions
import com.quizango.gallery.ui.navigation.Screen

/**
 * MediaListActivity (§4): a device folder or a `filter:*` list on the shared list frame.
 * The organizer stripe slot under the toolbar belongs to Phase 6 (CLN-04).
 */
@Composable
fun AlbumDetailScreen(
    bucketId: String,
    bucketName: String,
    mediaRepository: MediaRepository,
    vaultRepository: VaultRepository,
    onNavigate: (String) -> Unit,
    onNavigateBack: () -> Unit
) {
    val items by remember(bucketId) { mediaRepository.getMediaByFolderFlow(bucketId) }
        .collectAsState<List<MediaItem>, List<MediaItem>?>(initial = null)
    var selected by remember { mutableStateOf(emptySet<String>()) }
    val list = items.orEmpty()
    val onAction = rememberMediaListActions(mediaRepository, vaultRepository, list, selected) { selected = emptySet() }

    SharedMediaList(
        title = bucketName,
        items = items,
        selected = selected,
        onSelectedChange = { selected = it },
        onBack = onNavigateBack,
        // bucketId scopes the viewer's pages to this list
        onOpen = { onNavigate(viewerRouteFor(it, bucketId)) },
        actions = mediaListActions(allFavorite(list, selected)),
        onAction = onAction,
        showPinchTip = true
    )
}

/** The viewer opened on [item], paging through [source] (a folder path or a `filter:*` list). */
internal fun viewerRouteFor(item: MediaItem, source: String): String = Screen.MediaViewer.createRoute(item.filePath, source)
