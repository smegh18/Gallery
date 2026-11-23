package com.quizango.gallery.ui.album

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import com.quizango.gallery.GalleryApplication
import com.quizango.gallery.R
import com.quizango.gallery.data.repository.MediaRepository
import com.quizango.gallery.data.repository.VaultRepository
import com.quizango.gallery.domain.model.MediaItem
import com.quizango.gallery.ui.browse.SharedMediaList
import com.quizango.gallery.ui.browse.allFavorite
import com.quizango.gallery.ui.browse.mediaListActions
import com.quizango.gallery.ui.browse.rememberMediaListActions
import com.quizango.gallery.ui.navigation.Screen
import com.quizango.gallery.ui.theme.GalleryText
import kotlinx.coroutines.launch

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
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val preferences = (context.applicationContext as GalleryApplication).preferences
    val stripeDismissed by preferences.isOrganizerStripeShown.collectAsState(initial = false)

    Column(Modifier.fillMaxSize()) {
        if (bucketId == "filter:all_photos" && !stripeDismissed) {
            Row(Modifier.fillMaxWidth().background(colorResource(R.color.organizer_stripe_color)).clickable {
                scope.launch { preferences.setOrganizerStripeShown(true); onNavigate(Screen.QuickOrganizer.route) }
            }.padding(horizontal = 12.dp, vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(painterResource(R.drawable.ic_organizer_white), contentDescription = null, tint = Color.Unspecified)
                Text(stringResource(R.string.organize_your_photos_quickly), style = GalleryText.BodySmallSemiBold, color = colorResource(R.color.whiteOnlyColor), maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f).padding(start = 12.dp))
                Icon(painterResource(R.drawable.ic_close_stripe), contentDescription = stringResource(R.string.cancel), tint = Color.Unspecified, modifier = Modifier.clickable { scope.launch { preferences.setOrganizerStripeShown(true) } }.padding(start = 8.dp))
            }
        }
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
}

/** The viewer opened on [item], paging through [source] (a folder path or a `filter:*` list). */
internal fun viewerRouteFor(item: MediaItem, source: String): String = Screen.MediaViewer.createRoute(item.filePath, source)
