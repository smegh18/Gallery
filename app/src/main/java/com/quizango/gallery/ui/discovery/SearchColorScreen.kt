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
import com.quizango.gallery.ui.browse.MediaAction
import com.quizango.gallery.ui.browse.SharedMediaList
import com.quizango.gallery.ui.browse.allFavorite
import com.quizango.gallery.ui.browse.rememberMediaListActions

internal const val DEFAULT_SEARCH_COLOR = "#FF0000"

private val HEX_COLOR = Regex("#[0-9A-Fa-f]{6}")

/**
 * The colour from the route: decoded once, kept only when it is a `#RRGGBB` value (upper-cased, as the picker and
 * swatches produce it), else the route default. A malformed argument never reaches the repository filter.
 */
internal fun colorFromArg(arg: String?): String {
    val decoded = arg.orEmpty()
    return if (HEX_COLOR.matches(decoded)) decoded.uppercase() else DEFAULT_SEARCH_COLOR
}

/** The list the viewer pages through for [colorHex]: getMediaByFolderFlow resolves it with searchByColorFlow. */
internal fun colorSource(colorHex: String): String = "filter:color:$colorHex"

/** Search colour (§3.4): Share, Favorite / Unfavorite, Add to Private, Delete; never Add to or Create album. */
internal fun searchColorActions(allFavorite: Boolean): List<MediaAction> = listOf(
    MediaAction.SHARE,
    if (allFavorite) MediaAction.UNFAVORITE else MediaAction.FAVORITE,
    MediaAction.PRIVATE,
    MediaAction.DELETE
)

/**
 * SearchPhotoColorActivity (§12) on the shared list frame, titled "Media Color". Which photos match a colour is
 * unchanged repository behaviour (searchByColorFlow); this screen only presents and acts on the result.
 */
@Composable
fun SearchColorScreen(
    colorHex: String,
    mediaRepository: MediaRepository,
    vaultRepository: VaultRepository,
    onNavigate: (String) -> Unit,
    onNavigateBack: () -> Unit
) {
    val items by remember(colorHex) { mediaRepository.searchByColorFlow(colorHex) }
        .collectAsState<List<MediaItem>, List<MediaItem>?>(initial = null)
    var selected by remember { mutableStateOf(emptySet<String>()) }
    val list = items.orEmpty()
    val onAction = rememberMediaListActions(mediaRepository, vaultRepository, list, selected) { selected = emptySet() }

    SharedMediaList(
        title = stringResource(R.string.media_color),
        items = items,
        selected = selected,
        onSelectedChange = { selected = it },
        onBack = onNavigateBack,
        onOpen = { onNavigate(viewerRouteFor(it, colorSource(colorHex))) },
        actions = searchColorActions(allFavorite(list, selected)),
        onAction = onAction
    )
}
