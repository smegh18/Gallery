package com.quizango.gallery.ui.vault

import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.annotation.StringRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.quizango.gallery.R
import com.quizango.gallery.data.repository.MediaRepository
import com.quizango.gallery.data.repository.VaultRepository
import com.quizango.gallery.domain.model.Album
import com.quizango.gallery.domain.model.MediaItem
import com.quizango.gallery.ui.browse.SharedMediaCell
import com.quizango.gallery.ui.home.albumTitle
import com.quizango.gallery.ui.home.drawableBackground
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

/** Runs [op] once per item of a confirmed vault batch, in order and past any failure; the paths it failed on, to retry. */
internal suspend fun <T> vaultBatchFailures(items: List<T>, path: (T) -> String, op: suspend (T) -> Boolean): List<String> =
    items.filterNot { op(it) }.map(path)

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
    var albumPath by remember { mutableStateOf<String?>(null) } // null = All Photos
    var showFolders by remember { mutableStateOf(false) }
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
            val failed = vaultBatchFailures(snapshot, MediaItem::filePath) { vaultRepository.moveToVault(it) }
            mediaRepository.syncMediaStore() // one re-index for the batch (07-02)
            if (failed.isEmpty()) {
                toast(R.string.selected_media_files_successfully_hide)
                onDone()
            } else {
                // 2.7.2 has no partial-result copy; the hidden ones leave the grid, the failed ones stay picked for a retry
                toast(R.string.something_went_wrong)
                picked = failed
                busy = false
            }
        }
    }

    BackHandler(enabled = showFolders) { showFolders = false } // closes the folder list first, then leaves without hiding
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
                        modifier = Modifier.size(35.sdp).clickable { if (showFolders) showFolders = false else onNavigateBack() }.padding(7.sdp)
                    )
                    Row(Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = title,
                            style = GalleryText.BodyLargeSemiBold.copy(fontSize = 14.ssp),
                            color = colorResource(R.color.titleColor),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier
                                .weight(1f, fill = false)
                                .clickable(enabled = albums != null) { showFolders = !showFolders }
                                .padding(start = 9.sdp, end = 10.sdp)
                        )
                        if (albums == null) CircularProgressIndicator(
                            color = colorResource(R.color.colorPrimary),
                            strokeWidth = 2.dp,
                            modifier = Modifier.size(20.sdp)
                        ) else Image(
                            painter = painterResource(
                                if (showFolders) R.drawable.ic_title_arrow_down
                                else R.drawable.ic_title_arrow_up
                            ),
                            contentDescription = stringResource(R.string.all_photos),
                            colorFilter = ColorFilter.tint(colorResource(R.color.titleColor)),
                            modifier = Modifier
                                .size(width = 40.sdp, height = 30.sdp)
                                .clickable { showFolders = !showFolders }
                                .padding(horizontal = 15.sdp)
                        )
                    }
                    if (barsVisible) Text(
                        text = stringResource(R.string.internal_select_add),
                        style = GalleryText.BodyLargeSemiBold.copy(fontSize = 10.ssp),
                        color = colorResource(R.color.onlyWhite),
                        modifier = Modifier
                            .padding(horizontal = 12.sdp)
                            .drawableBackground(R.drawable.bg_rounded_red, colorResource(R.color.colorPrimary).toArgb())
                            .clickable(onClick = ::add)
                            .padding(horizontal = 19.sdp, vertical = 6.sdp)
                    )
                }
                Box(Modifier.fillMaxWidth().height(1.sdp).background(colorResource(R.color.toolbar_bottom_border)))
            }
            Box(Modifier.weight(1f).fillMaxWidth().padding(top = 2.sdp)) {
                val list = media
                when {
                    showFolders -> albums?.let { all ->
                        FolderList(all, onPick = { path -> albumPath = path; showFolders = false })
                    }
                    list == null -> Unit // the loading layer covers the first query
                    list.isEmpty() -> PickerEmptyState()
                    else -> LazyVerticalGrid(
                        columns = GridCells.Fixed(4), // IS.onCreate GridLayoutManager(ctx, 4)
                        contentPadding = PaddingValues(start = 2.sdp, end = 2.sdp, top = 5.sdp, bottom = 20.sdp),
                        modifier = Modifier.fillMaxSize()
                    ) {
                        items(list, key = { it.filePath }) { item ->
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
            }
            if (barsVisible) SelectionTray(
                picked = picked,
                known = known,
                onClear = { picked = emptyList() },
                onRemove = { picked = togglePicked(picked, it) }
            )
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

/** emptyLayout: shown only once the first query has returned an empty folder. */
@Composable
internal fun PickerEmptyState() {
    Column(Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
        Image(painterResource(R.drawable.ic_empty_img), contentDescription = null)
        Text(
            text = stringResource(R.string.no_items),
            style = GalleryText.BodyLargeSemiBold,
            color = colorResource(R.color.titleColor),
            modifier = Modifier.padding(top = 20.sdp, bottom = 10.sdp)
        )
        Text(
            text = stringResource(R.string.no_media_file),
            style = GalleryText.BodySmallRegular,
            color = colorResource(R.color.titleColor),
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 50.sdp)
        )
    }
}

/** folderRecycler: All Photos first, then device folders and Videos; picking one closes the list. */
@Composable
private fun FolderList(albums: List<Album>, onPick: (String?) -> Unit) {
    val allPhotos = albums.firstOrNull { it.path == "filter:all_photos" }
    // All Photos here means every active item, photos and videos
    val allCount = albums.filter { it.path == "filter:all_photos" || it.path == "filter:videos" }.sumOf { it.itemCount }
    val folders = albums.filter { !it.path.startsWith("filter:") || it.path == "filter:videos" }
    LazyColumn(
        Modifier.fillMaxSize().background(colorResource(R.color.imagePickerFolderBGColor)),
        contentPadding = PaddingValues(start = 2.sdp, end = 2.sdp, bottom = 20.sdp)
    ) {
        item(key = "all_photos") {
            FolderRow(allPhotos, stringResource(R.string.all_photos), allCount) { onPick(null) }
        }
        items(folders, key = { it.path }) { album ->
            FolderRow(album, albumTitle(album), album.itemCount) { onPick(album.path) }
        }
    }
}

/** item_picker_folder: cover, name, count and the trailing arrow. */
@Composable
internal fun FolderRow(album: Album?, name: String, count: Int, onClick: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(top = 10.sdp)
            .clickable(onClick = onClick)
            .padding(horizontal = 12.sdp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        AsyncImage(
            model = album?.takeIf { it.itemCount > 0 && it.coverPath.isNotEmpty() }?.coverUri,
            contentDescription = null,
            placeholder = painterResource(R.drawable.image_loading_albums),
            error = painterResource(R.drawable.image_loading_albums),
            contentScale = ContentScale.Crop,
            modifier = Modifier.padding(1.sdp).size(60.sdp).clip(RoundedCornerShape(8.sdp))
        )
        Column(Modifier.weight(1f).padding(start = 5.sdp), verticalArrangement = Arrangement.Center) {
            Text(
                text = name,
                style = GalleryText.BodyLargeSemiBold,
                color = colorResource(R.color.titleColor),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.fillMaxWidth().padding(top = 6.sdp, start = 7.sdp)
            )
            Text(
                text = count.toString(),
                style = GalleryText.BodyXLargeMedium,
                color = colorResource(R.color.picker_folder_subtitle_color),
                modifier = Modifier.width(100.sdp).padding(top = 2.sdp, start = 7.sdp)
            )
        }
        Image(
            painter = painterResource(R.drawable.ad_ic_right_arrow),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.size(35.sdp).padding(8.sdp)
        )
    }
}

/** llSelectedMediaView: count header over an optional strip of picked thumbnails. */
@Composable
internal fun SelectionTray(
    picked: List<String>,
    known: Map<String, MediaItem>,
    onClear: () -> Unit,
    onRemove: (String) -> Unit
) {
    var showStrip by remember { mutableStateOf(false) } // rvSelectedMedia is hidden by default
    val count = stringResource(R.string.items_selected, picked.size)
    Column(
        Modifier
            .fillMaxWidth()
            .background(colorResource(R.color.imagePickerSelectionsBGColor))
            .padding(vertical = 5.sdp)
    ) {
        if (showStrip) LazyRow(Modifier.fillMaxWidth().padding(7.sdp)) {
            items(picked, key = { it }) { path ->
                Box(Modifier.padding(horizontal = 4.sdp).size(60.sdp).clickable { onRemove(path) }) {
                    AsyncImage(
                        model = known[path]?.contentUri,
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.align(Alignment.TopStart).padding(top = 5.sdp).size(55.sdp).clip(RoundedCornerShape(7.sdp))
                    )
                    Image(
                        painter = painterResource(R.drawable.ic_close),
                        contentDescription = stringResource(R.string.close),
                        colorFilter = ColorFilter.tint(colorResource(R.color.white)),
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .size(16.sdp)
                            .shadow(2.sdp, CircleShape)
                            .drawableBackground(R.drawable.bg_circle, colorResource(R.color.black).toArgb())
                            .padding(2.sdp)
                    )
                }
            }
        }
        Row(Modifier.fillMaxWidth().height(40.sdp), verticalAlignment = Alignment.CenterVertically) {
            Image(
                painter = painterResource(R.drawable.ic_close),
                contentDescription = stringResource(R.string.cancel),
                colorFilter = ColorFilter.tint(colorResource(R.color.black)),
                modifier = Modifier.padding(start = 5.sdp).size(36.sdp).clickable(onClick = onClear).padding(6.sdp)
            )
            Text(
                text = count,
                style = GalleryText.BodyLargeSemiBold.copy(fontSize = 13.ssp),
                color = colorResource(R.color.titleColor),
                modifier = Modifier.weight(1f).padding(start = 9.sdp)
            )
            Image(
                painter = painterResource(R.drawable.ic_arrow_up),
                contentDescription = count,
                colorFilter = ColorFilter.tint(colorResource(R.color.titleColor)),
                modifier = Modifier.padding(end = 5.sdp).size(36.sdp).clickable { showStrip = !showStrip }.padding(6.sdp)
            )
        }
    }
}
