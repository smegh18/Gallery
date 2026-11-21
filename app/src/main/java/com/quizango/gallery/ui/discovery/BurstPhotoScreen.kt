package com.quizango.gallery.ui.discovery

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
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.stringResource
import com.quizango.gallery.R
import com.quizango.gallery.data.repository.MediaRepository
import com.quizango.gallery.data.repository.burstKey
import com.quizango.gallery.domain.model.MediaItem
import com.quizango.gallery.ui.browse.AlbumSquareCard
import com.quizango.gallery.ui.browse.ListState
import com.quizango.gallery.ui.browse.SharedMediaToolbar
import com.quizango.gallery.ui.browse.listStateOf
import com.quizango.gallery.ui.home.PhotosEmptyState
import com.quizango.gallery.ui.navigation.Screen
import com.quizango.gallery.ui.theme.sdp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

internal class BurstGroup(val key: String, val frames: List<MediaItem>)

/**
 * BurstPhotoViewModel (bf/a): frames grouped by key in query order (newest first), groups sorted by their first
 * frame newest first (ax/u case 11), frame order kept. [frames] carry a non-empty key.
 */
internal fun burstGroups(frames: List<MediaItem>, keyOf: (MediaItem) -> String): List<BurstGroup> =
    frames.groupBy(keyOf).map { (key, group) -> BurstGroup(key, group) }
        .sortedByDescending { it.frames.first().dateModified }

/** Card title "Burst N" (getString(burst) + " " + position) and the media list of exactly that group. */
internal fun burstRoute(group: BurstGroup, title: String): String = Screen.AlbumDetail.createRoute("filter:burst:${group.key}", title)

/** BurstPhotoActivity (§9): one item_grid_square_album card per burst, opening that burst's frames only. */
@Composable
fun BurstPhotoScreen(
    mediaRepository: MediaRepository,
    onNavigate: (String) -> Unit,
    onNavigateBack: () -> Unit
) {
    val frames by remember { mediaRepository.getBurstPhotosFlow() }
        .collectAsState<List<MediaItem>, List<MediaItem>?>(initial = null)
    val groups by produceState<List<BurstGroup>?>(null, frames) {
        val all = frames ?: return@produceState
        value = withContext(Dispatchers.Default) { burstGroups(all) { it.burstKey() } }
    }
    val burst = stringResource(R.string.burst)

    Column(Modifier.fillMaxSize().background(colorResource(R.color.backgroundColor))) {
        SharedMediaToolbar(title = burst, onBack = onNavigateBack)
        Box(Modifier.weight(1f).fillMaxWidth()) {
            when (listStateOf(groups, 3)) {
                ListState.LOADING -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = colorResource(R.color.colorPrimary))
                }
                ListState.EMPTY -> PhotosEmptyState(R.string.no_items, R.string.no_media_file)
                ListState.CONTENT -> LazyVerticalGrid(
                    columns = GridCells.Fixed(3),
                    contentPadding = PaddingValues(4.sdp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    itemsIndexed(groups.orEmpty(), key = { _, group -> group.key }) { i, group ->
                        val title = "$burst ${i + 1}"
                        // be/b binds the single-cover face: first frame, title, bare count; never the 2x2 mosaic
                        AlbumSquareCard(
                            title = title,
                            count = group.frames.size,
                            cover = group.frames.first().contentUri,
                            onClick = { onNavigate(burstRoute(group, title)) },
                            modifier = Modifier.padding(end = 8.sdp, bottom = 8.sdp)
                        )
                    }
                }
            }
        }
        Spacer(Modifier.navigationBarsPadding())
    }
}
