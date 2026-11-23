package com.quizango.gallery.ui.vault

import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.annotation.StringRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.quizango.gallery.R
import com.quizango.gallery.data.repository.MediaRepository
import com.quizango.gallery.data.repository.VaultRepository
import com.quizango.gallery.domain.model.Album
import com.quizango.gallery.domain.model.MediaItem
import com.quizango.gallery.ui.browse.SharedMediaCell
import com.quizango.gallery.ui.home.albumTitle
import com.quizango.gallery.ui.theme.GalleryText
import com.quizango.gallery.ui.theme.sdp
import com.quizango.gallery.ui.theme.ssp
import kotlinx.coroutines.launch

/** Tap toggles: a picked item leaves the list, a new one joins at the end, so Add hides in tap order. */
internal fun <T> togglePicked(picked: List<T>, item: T): List<T> = if (item in picked) picked - item else picked + item

/** `btnNext` and `llSelectedMediaView` are visible only while at least one item is picked (IS.o000OOo). */
internal fun pickerBarsVisible(pickedCount: Int): Boolean = pickedCount > 0

/** Toast to show for an Add tap, or null when there is something to hide. */
@StringRes
internal fun pickerAddEvent(pickedCount: Int): Int? = if (pickedCount == 0) R.string.please_select_items else null

/** activity_select (InternalSelectMediaActivity TYPE=2, #37): folder row, 4-column grid, Add. */
@Composable
fun VaultAddMediaScreen(
    mediaRepository: MediaRepository,
    vaultRepository: VaultRepository,
    onDone: () -> Unit,
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    // null until the first query returns, so the loading layer (and later the empty copy) never flashes the wrong state
    val albums by remember { mediaRepository.getAlbumsFlow() }.collectAsState(initial = null)
    val albumPath: String? = null // All Photos; the folder list switches it
    val media by remember(albumPath) {
        albumPath?.let { mediaRepository.getMediaByFolderFlow(it) } ?: mediaRepository.getActiveMediaFlow()
    }.collectAsState(initial = null)
    var picked by remember { mutableStateOf(emptyList<String>()) }
    // The selection survives a folder switch, so the picked items are remembered by path
    val known = remember { mutableMapOf<String, MediaItem>() }
    var busy by remember { mutableStateOf(false) }
    val title = albums?.firstOrNull { it.path == albumPath }?.let { albumTitle(it) } ?: stringResource(R.string.all_photos)
    val barsVisible = pickerBarsVisible(picked.size)

    fun toast(id: Int) = Toast.makeText(context, context.getString(id), Toast.LENGTH_SHORT).show()

    fun add() {
        if (busy) return
        pickerAddEvent(picked.size)?.let { return toast(it) }
        busy = true
        val snapshot = picked.mapNotNull { known[it] } // T-07-22: acts on the ordered snapshot taken at the tap
        scope.launch {
            val hidden = snapshot.count { vaultRepository.moveToVault(it) }
            mediaRepository.syncMediaStore() // one re-index for the batch (07-02)
            toast(if (hidden > 0) R.string.selected_media_files_successfully_hide else R.string.something_went_wrong)
            onDone()
        }
    }

    BackHandler(enabled = busy) { } // no leaving while files are being hidden

    Box(Modifier.fillMaxSize().background(colorResource(R.color.backgroundColor))) {
        Column(Modifier.fillMaxSize()) {
            Column(Modifier.fillMaxWidth().background(colorResource(R.color.backgroundColor)).statusBarsPadding()) {
                Row(
                    Modifier.fillMaxWidth().height(56.dp).padding(4.sdp), // ?actionBarSize
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Image(
                        painter = painterResource(R.drawable.ic_back),
                        contentDescription = stringResource(R.string.viewer_back),
                        colorFilter = ColorFilter.tint(colorResource(R.color.titleColor)),
                        modifier = Modifier.size(35.sdp).clickable(onClick = onNavigateBack).padding(7.sdp)
                    )
                    Row(Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = title,
                            style = GalleryText.BodyLargeSemiBold.copy(fontSize = 14.ssp),
                            color = colorResource(R.color.titleColor),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f, fill = false).padding(start = 9.sdp, end = 10.sdp)
                        )
                        if (albums == null) CircularProgressIndicator(
                            color = colorResource(R.color.colorPrimary),
                            strokeWidth = 2.dp,
                            modifier = Modifier.size(20.sdp)
                        ) else Image(
                            painter = painterResource(R.drawable.ic_title_arrow_up),
                            contentDescription = stringResource(R.string.all_photos),
                            colorFilter = ColorFilter.tint(colorResource(R.color.titleColor)),
                            modifier = Modifier.size(width = 40.sdp, height = 30.sdp).padding(horizontal = 15.sdp)
                        )
                    }
                    if (barsVisible) Text(
                        text = stringResource(R.string.internal_select_add),
                        style = GalleryText.BodyLargeSemiBold.copy(fontSize = 10.ssp),
                        color = colorResource(R.color.onlyWhite),
                        modifier = Modifier
                            .padding(horizontal = 12.sdp)
                            .background(colorResource(R.color.colorPrimary), RoundedCornerShape(50)) // bg_rounded_red tinted colorPrimary
                            .clickable(onClick = ::add)
                            .padding(horizontal = 19.sdp, vertical = 6.sdp)
                    )
                }
                Box(Modifier.fillMaxWidth().height(1.sdp).background(colorResource(R.color.toolbar_bottom_border)))
            }
            Box(Modifier.weight(1f).fillMaxWidth().padding(top = 2.sdp)) {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(4), // IS.onCreate GridLayoutManager(ctx, 4)
                    contentPadding = PaddingValues(start = 2.sdp, end = 2.sdp, top = 5.sdp, bottom = 20.sdp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(media.orEmpty(), key = { it.filePath }) { item ->
                        Box(
                            Modifier.clickable {
                                known[item.filePath] = item
                                picked = togglePicked(picked, item.filePath)
                            }
                        ) {
                            SharedMediaCell(
                                item = item,
                                isSelected = item.filePath in picked,
                                selecting = true,
                                onClick = {},
                                onLongClick = {}
                            )
                        }
                    }
                }
            }
            Box(Modifier.navigationBarsPadding())
        }

        // progressBar layer: blocks touches during the first query and while the batch is hidden
        if (media == null || busy) Box(
            Modifier
                .fillMaxSize()
                .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = {}),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator(color = colorResource(R.color.colorPrimary))
        }
    }
}
