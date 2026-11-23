package com.quizango.gallery.ui.discovery

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.res.stringResource
import com.quizango.gallery.R
import com.quizango.gallery.data.repository.MediaRepository
import com.quizango.gallery.data.repository.VaultRepository
import com.quizango.gallery.domain.model.MediaItem
import com.quizango.gallery.ui.album.viewerRouteFor
import com.quizango.gallery.ui.browse.SharedMediaList
import com.quizango.gallery.ui.browse.allFavorite
import com.quizango.gallery.ui.browse.mediaListActions
import com.quizango.gallery.ui.browse.rememberMediaListActions

/** The viewer pages through the same animated rows (getMediaByFolderFlow resolves it with getAnimatedMediaFlow's filter). */
internal const val ANIMATED_SOURCE = "filter:animated"

/**
 * AnimatedMediaListActivity (§8) on the shared list frame: six selection slots, empty no_items / no_media_file.
 * 2.7.2 fills it from a background AnimatedFindService with a scanning header; ours flags GIFs at MediaStore sync,
 * so the list is complete when the first query returns and the frame's spinner covers the wait.
 */
@Composable
fun AnimatedMediaScreen(
    mediaRepository: MediaRepository,
    vaultRepository: VaultRepository,
    onNavigate: (String) -> Unit,
    onNavigateBack: () -> Unit
) {
    val items by remember { mediaRepository.getAnimatedMediaFlow() }
        .collectAsState<List<MediaItem>, List<MediaItem>?>(initial = null)
    var selected by remember { mutableStateOf(emptySet<String>()) }
    val list = items.orEmpty()
    val onAction = rememberMediaListActions(mediaRepository, vaultRepository, list, selected, onNavigate) { selected = emptySet() }

    SharedMediaList(
        title = stringResource(R.string.animated),
        items = items,
        selected = selected,
        onSelectedChange = { selected = it },
        onBack = onNavigateBack,
        onOpen = { onNavigate(viewerRouteFor(it, ANIMATED_SOURCE)) },
        actions = mediaListActions(allFavorite(list, selected)),
        onAction = onAction
    )
}
