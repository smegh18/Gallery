package com.quizango.gallery.ui.favorites

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
import com.quizango.gallery.ui.browse.FAVORITES_ACTIONS
import com.quizango.gallery.ui.browse.SharedMediaList
import com.quizango.gallery.ui.browse.rememberMediaListActions
import com.quizango.gallery.ui.home.ALBUM_FAVORITES
import com.quizango.gallery.ui.navigation.Screen

/** FavoriteActivity (§10): the shared list frame over the favourites; unfavourited items leave the list at once. */
@Composable
fun FavoritesScreen(
    mediaRepository: MediaRepository,
    vaultRepository: VaultRepository,
    onNavigate: (String) -> Unit,
    onNavigateBack: () -> Unit
) {
    val items by remember { mediaRepository.getFavoritesFlow() }
        .collectAsState<List<MediaItem>, List<MediaItem>?>(initial = null)
    var selected by remember { mutableStateOf(emptySet<String>()) }
    val onAction = rememberMediaListActions(mediaRepository, vaultRepository, items.orEmpty(), selected, onNavigate) { selected = emptySet() }

    SharedMediaList(
        title = stringResource(R.string.favorites),
        items = items,
        selected = selected,
        onSelectedChange = { selected = it },
        onBack = onNavigateBack,
        onOpen = { onNavigate(viewerRouteFor(it, ALBUM_FAVORITES)) },
        actions = FAVORITES_ACTIONS,
        onAction = onAction,
        emptyTitle = R.string.no_favorites,
        emptyBody = R.string.no_favorites_empty,
        onPro = { onNavigate(Screen.Pro.route) }
    )
}
