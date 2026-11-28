package com.quizango.gallery.ui.discovery

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.layout.layout
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.airbnb.lottie.compose.LottieAnimation
import com.airbnb.lottie.compose.LottieCompositionSpec
import com.airbnb.lottie.compose.LottieConstants
import com.airbnb.lottie.compose.rememberLottieComposition
import com.quizango.gallery.R
import com.quizango.gallery.data.local.MomentScanner
import com.quizango.gallery.data.repository.MediaRepository
import com.quizango.gallery.domain.model.Album
import com.quizango.gallery.domain.model.MediaItem
import com.quizango.gallery.domain.model.MediaType
import com.quizango.gallery.ui.browse.AlbumSquareCard
import com.quizango.gallery.ui.browse.SharedMediaToolbar
import com.quizango.gallery.ui.home.PhotosEmptyState
import com.quizango.gallery.ui.navigation.Screen
import androidx.compose.material3.MaterialTheme
import com.quizango.gallery.ui.theme.GalleryText
import com.quizango.gallery.ui.theme.isDark
import com.quizango.gallery.ui.theme.sdp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

internal enum class MomentsState { WAIT, EMPTY, CONTENT }

/**
 * Cards once any moment exists; the scanner wait while a scan runs or the groups are not yet built from the
 * current photos and labels; "Oops!" only after both settle, so the empty copy never flashes first.
 */
internal fun momentsState(groupCount: Int?, settled: Boolean, scanning: Boolean): MomentsState = when {
    (groupCount ?: 0) > 0 -> MomentsState.CONTENT
    scanning || !settled -> MomentsState.WAIT
    else -> MomentsState.EMPTY
}

/** momentsScanningLayout shows once the scan has a count (MO / ga/g: max > 0 and progress > 0). */
internal fun showsScanHeader(p: MomentScanner.Progress?): Boolean = p != null && p.total > 0 && p.done > 0

/** A moment opens the media list titled with its label; the filter re-resolves the label against current rows. */
internal fun momentRoute(label: String): String = Screen.AlbumDetail.createRoute("filter:moment:$label", label)

private class MomentGroups(val photos: List<MediaItem>, val labels: Map<String, String>, val albums: List<Album>)

/** MomentsActivity (§6): MomentScanner labels, shown as item_search_sub cards. */
@Composable
fun MomentsScreen(
    mediaRepository: MediaRepository,
    moments: MomentScanner,
    onMomentClick: (label: String) -> Unit,
    onNavigateBack: () -> Unit
) {
    val allMedia by mediaRepository.getActiveMediaFlow().collectAsState(initial = null)
    val labels by moments.labels.collectAsState()
    val progress by moments.progress.collectAsState()

    val photos = remember(allMedia) { allMedia?.filter { it.mediaType != MediaType.VIDEO } }
    LaunchedEffect(photos) { photos?.let { moments.scan(it) } }

    // One album per label, filling in while the scan runs
    val built by produceState<MomentGroups?>(null, photos, labels) {
        val p = photos ?: return@produceState
        value = withContext(Dispatchers.Default) {
            MomentGroups(p, labels, MomentScanner.group(p, labels).map { (label, items) ->
                val cover = items.first()
                Album(
                    id = "filter:moment:$label", name = label, path = "filter:moment:$label",
                    coverPath = cover.filePath, itemCount = items.size, coverId = cover.id
                )
            })
        }
    }
    val groups = built?.albums
    val settled = built?.let { it.photos === photos && it.labels === labels } == true

    Column(Modifier.fillMaxSize().background(colorResource(R.color.backgroundColor))) {
        SharedMediaToolbar(title = stringResource(R.string.moment), onBack = onNavigateBack)
        progress?.takeIf(::showsScanHeader)?.let { ScanningHeader(it) }
        Box(Modifier.weight(1f).fillMaxWidth()) {
            when (momentsState(groups?.size, settled, progress != null)) {
                MomentsState.WAIT -> ScannerWait()
                MomentsState.EMPTY -> PhotosEmptyState(R.string.oops, R.string.no_moment_files_found)
                MomentsState.CONTENT -> LazyVerticalGrid(
                    columns = GridCells.Fixed(3),
                    contentPadding = PaddingValues(horizontal = 2.sdp, vertical = 6.sdp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(groups.orEmpty(), key = { it.id }) { album ->
                        AlbumSquareCard(
                            title = album.name,
                            count = album.itemCount,
                            cover = album.coverUri,
                            titleTop = 4.sdp, // item_search_sub
                            onClick = { onMomentClick(album.name) },
                            modifier = Modifier.padding(4.sdp)
                        )
                    }
                }
            }
        }
        Spacer(Modifier.navigationBarsPadding())
    }
}

/** momentsScanningLayout: "Scanning Photos...." with done/total, a determinate bar and the hint. */
@Composable
private fun ScanningHeader(p: MomentScanner.Progress) {
    val titleColor = colorResource(R.color.titleColor)
    Column(Modifier.fillMaxWidth().padding(top = 10.sdp)) {
        Row(Modifier.fillMaxWidth().padding(top = 10.sdp, start = 10.sdp, end = 10.sdp)) {
            Text(stringResource(R.string.scanning_photos), style = GalleryText.BodySmallRegular, color = titleColor, modifier = Modifier.weight(1f))
            Text(
                "${p.done}/${p.total}",
                style = GalleryText.BodyLargeSemiBold,
                color = titleColor,
                // the bar is layout_below the title only, so the taller count adds no height
                modifier = Modifier.layout { m, c -> val pl = m.measure(c); layout(pl.width, 0) { pl.place(0, 0) } }
            )
        }
        LinearProgressIndicator(
            progress = { p.done.toFloat() / p.total },
            color = colorResource(R.color.colorPrimary),
            // framework disabledAlpha on the track layer: 0.26 light, 0.30 dark
            trackColor = colorResource(R.color.seekbar_color_bg).copy(alpha = if (MaterialTheme.colorScheme.isDark) 0.3f else 0.26f),
            strokeCap = StrokeCap.Butt,
            gapSize = 0.dp,
            drawStopIndicator = {},
            // progressBarStyleHorizontal: a 4.dp track centred in its 16.dp minimum height
            modifier = Modifier.fillMaxWidth().padding(horizontal = 10.sdp, vertical = 5.sdp).padding(vertical = 6.dp).height(4.dp)
        )
        Text(
            stringResource(R.string.scanning_time),
            style = GalleryText.BodyXSmallRegular,
            color = titleColor,
            modifier = Modifier.align(Alignment.CenterHorizontally).padding(bottom = 5.sdp)
        )
    }
}

/** rl_scanner_lottie_view: the looping scanner and "Kindly wait..." while nothing is found yet. */
@Composable
private fun ScannerWait() {
    val composition by rememberLottieComposition(LottieCompositionSpec.RawRes(R.raw.moment_scanner_lottie))
    Column(Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
        LottieAnimation(composition, iterations = LottieConstants.IterateForever, modifier = Modifier.size(140.sdp))
        Text(
            stringResource(R.string.moment_lottie_text),
            style = GalleryText.BodySmallMedium,
            color = colorResource(R.color.titleColor),
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 10.sdp, start = 20.sdp, end = 20.sdp)
        )
    }
}
