package com.quizango.gallery.ui.components

import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.annotation.StringRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.quizango.gallery.R
import com.quizango.gallery.data.repository.MediaRepository
import com.quizango.gallery.domain.model.Album
import com.quizango.gallery.domain.model.MediaItem
import com.quizango.gallery.domain.model.MediaType
import com.quizango.gallery.ui.browse.SharedMediaCell
import com.quizango.gallery.ui.home.albumTitle
import com.quizango.gallery.ui.home.drawableBackground
import com.quizango.gallery.ui.theme.GalleryText
import com.quizango.gallery.ui.theme.sdp
import com.quizango.gallery.ui.theme.ssp
import com.quizango.gallery.ui.vault.FolderRow
import com.quizango.gallery.ui.vault.PickerEmptyState
import com.quizango.gallery.ui.vault.SelectionTray
import com.quizango.gallery.ui.vault.togglePicked

/** InternalSelectMediaActivity TYPE 4 (collage): Add needs 2 to 15 photos (IS.o0O0O00). */
internal const val COLLAGE_MIN = 2
internal const val COLLAGE_MAX = 15

/** Toast for a TYPE 4 Add tap, or null when the picked count is in range. */
@StringRes
internal fun collageAddError(count: Int): Int? = when {
    count == 0 -> R.string.please_select_items
    count < COLLAGE_MIN || count > COLLAGE_MAX -> R.string.collage_limit_error
    else -> null
}

/** IS.oo0o0Oo: once 15 photos are picked a tap on a new one is refused with collage_limit_error. */
internal fun collageTapAllowed(picked: List<String>, path: String) = path in picked || picked.size < COLLAGE_MAX

/**
 * activity_select picking photos (InternalSelectMediaActivity TYPE 3 is_single, TYPE 4 collage): folder row, folder list
 * and a 4-column grid of photos. Single mode returns the tapped photo; [multi] adds check circles, the tray and Add.
 */
@Composable
fun PhotoPicker(mediaRepository: MediaRepository, multi: Boolean, onBack: () -> Unit, onPicked: (List<MediaItem>) -> Unit) {
    val context = LocalContext.current
    val media by remember { mediaRepository.getActiveMediaFlow() }.collectAsState(initial = null)
    val albums by remember { mediaRepository.getAlbumsFlow() }.collectAsState(initial = null)
    val photos = remember(media) { media?.filter { it.mediaType != MediaType.VIDEO } } // TYPE 3/4 query images only
    // Device folders in album order, counted by their photos; folders holding only videos drop out
    val folders = remember(albums, photos) {
        val byFolder = photos?.groupBy { it.folderPath } ?: return@remember null
        albums?.filter { !it.path.startsWith("filter:") }?.mapNotNull { a -> byFolder[a.path]?.let { a.copy(itemCount = it.size) } }
    }
    var folder by rememberSaveable { mutableStateOf<String?>(null) } // null = All Photos
    var showFolders by remember { mutableStateOf(false) }
    var picked by remember { mutableStateOf(emptyList<String>()) }
    val known = remember { mutableMapOf<String, MediaItem>() }
    val shown = remember(photos, folder) { photos?.let { all -> folder?.let { f -> all.filter { it.folderPath == f } } ?: all } }
    val title = folders?.firstOrNull { it.path == folder }?.let { albumTitle(it) } ?: stringResource(R.string.all_photos)

    fun toast(@StringRes id: Int) = Toast.makeText(context, context.getString(id, COLLAGE_MIN, COLLAGE_MAX), Toast.LENGTH_SHORT).show()

    fun tap(item: MediaItem) {
        if (!multi) return onPicked(listOf(item))
        if (!collageTapAllowed(picked, item.filePath)) return toast(R.string.collage_limit_error)
        known[item.filePath] = item
        picked = togglePicked(picked, item.filePath)
    }

    fun add() {
        collageAddError(picked.size)?.let { return toast(it) }
        onPicked(picked.mapNotNull { known[it] })
    }

    BackHandler(enabled = showFolders) { showFolders = false }

    PickerFrame(
        title = title,
        loading = folders == null,
        folderArrow = if (folders == null) null else showFolders,
        onTitleClick = { showFolders = !showFolders },
        showAdd = multi && picked.isNotEmpty(),
        onAdd = ::add,
        onBack = { if (showFolders) showFolders = false else onBack() },
        busy = shown == null,
        tray = {
            if (multi && picked.isNotEmpty()) SelectionTray(
                picked = picked,
                known = known,
                onClear = { picked = emptyList() },
                onRemove = { picked = togglePicked(picked, it) }
            )
        }
    ) {
        val list = shown
        when {
            showFolders -> folders?.let { all ->
                LazyColumn(
                    Modifier.fillMaxSize().background(colorResource(R.color.imagePickerFolderBGColor)),
                    contentPadding = PaddingValues(start = 2.sdp, end = 2.sdp, bottom = 20.sdp)
                ) {
                    item(key = "all_photos") {
                        val newest = photos?.firstOrNull()
                        val cover = newest?.let { Album(id = "", name = "", path = "", coverPath = it.filePath, itemCount = photos.size, coverId = it.id) }
                        FolderRow(cover, stringResource(R.string.all_photos), photos?.size ?: 0) { folder = null; showFolders = false }
                    }
                    items(all, key = { it.path }) { album ->
                        FolderRow(album, albumTitle(album), album.itemCount) { folder = album.path; showFolders = false }
                    }
                }
            }
            list == null -> Unit
            list.isEmpty() -> PickerEmptyState()
            else -> PhotoGrid(list, selecting = multi, isPicked = { it.filePath in picked }, onTap = ::tap)
        }
    }
}

/** activity_select over a fixed list (album Change cover): the album name as title and a single tap pick. */
@Composable
fun MediaPicker(title: String, items: List<MediaItem>, onBack: () -> Unit, onPick: (MediaItem) -> Unit) {
    PickerFrame(
        title = title, loading = false, folderArrow = null, onTitleClick = null,
        showAdd = false, onAdd = {}, onBack = onBack, busy = false, tray = {}
    ) {
        if (items.isEmpty()) PickerEmptyState()
        else PhotoGrid(items, selecting = false, isPicked = { false }, onTap = onPick)
    }
}

/** The activity_select frame: `toolbar` folder row (Back, title, loader or arrow, Add), divider, content, tray, progress layer. */
@Composable
private fun PickerFrame(
    title: String,
    loading: Boolean,
    folderArrow: Boolean?,
    onTitleClick: (() -> Unit)?,
    showAdd: Boolean,
    onAdd: () -> Unit,
    onBack: () -> Unit,
    busy: Boolean,
    tray: @Composable () -> Unit,
    content: @Composable () -> Unit
) {
    Box(Modifier.fillMaxSize().background(colorResource(R.color.backgroundColor))) {
        Column(Modifier.fillMaxSize()) {
            Column(Modifier.fillMaxWidth().statusBarsPadding()) {
                Row(
                    Modifier.fillMaxWidth().height(56.dp).padding(4.sdp), // ?actionBarSize
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Image(
                        painter = painterResource(R.drawable.ic_back),
                        contentDescription = stringResource(R.string.viewer_back),
                        colorFilter = ColorFilter.tint(colorResource(R.color.titleColor)),
                        modifier = Modifier.size(35.sdp).clickable(onClick = onBack).padding(7.sdp)
                    )
                    Row(Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically) {
                        // toolbarTitle: the folder arrow is its drawableEnd (drawablePadding 10sdp); ivFolderNameArrow stays gone
                        Row(
                            Modifier
                                .weight(1f, fill = false)
                                .then(if (onTitleClick != null && !loading) Modifier.clickable(onClick = onTitleClick) else Modifier)
                                .padding(start = 9.sdp, end = 10.sdp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = title,
                                style = GalleryText.BodyLargeSemiBold.copy(fontSize = 14.ssp),
                                color = colorResource(R.color.titleColor),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f, fill = false)
                            )
                            if (!loading && folderArrow != null && onTitleClick != null) Image(
                                painter = painterResource(if (folderArrow) R.drawable.ic_title_arrow_up else R.drawable.ic_title_arrow_down),
                                contentDescription = stringResource(R.string.all_photos),
                                colorFilter = ColorFilter.tint(colorResource(R.color.titleColor)),
                                modifier = Modifier.padding(start = 10.sdp)
                            )
                        }
                        if (loading) CircularProgressIndicator(
                            color = colorResource(R.color.colorPrimary),
                            strokeWidth = 2.dp,
                            modifier = Modifier.size(20.sdp)
                        )
                    }
                    if (showAdd) Text(
                        text = stringResource(R.string.internal_select_add),
                        style = GalleryText.BodyLargeSemiBold.copy(fontSize = 10.ssp),
                        color = colorResource(R.color.onlyWhite),
                        modifier = Modifier
                            .padding(horizontal = 12.sdp)
                            .drawableBackground(R.drawable.bg_rounded_red, colorResource(R.color.colorPrimary).toArgb())
                            .clickable(onClick = onAdd)
                            .padding(horizontal = 19.sdp, vertical = 6.sdp)
                    )
                }
                Box(Modifier.fillMaxWidth().height(1.sdp).background(colorResource(R.color.toolbar_bottom_border)))
            }
            Box(Modifier.weight(1f).fillMaxWidth().padding(top = 2.sdp)) { content() }
            tray()
            Box(Modifier.navigationBarsPadding())
        }
        // progressBar layer: blocks touches until the first query returns
        if (busy) Box(
            Modifier
                .fillMaxSize()
                .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = {}),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator(color = colorResource(R.color.colorPrimary))
        }
    }
}

/** selectRecycler: GridLayoutManager(ctx, 4) of item_grid_gallery cells. */
@Composable
private fun PhotoGrid(items: List<MediaItem>, selecting: Boolean, isPicked: (MediaItem) -> Boolean, onTap: (MediaItem) -> Unit) {
    LazyVerticalGrid(
        columns = GridCells.Fixed(4),
        contentPadding = PaddingValues(start = 2.sdp, end = 2.sdp, top = 5.sdp, bottom = 20.sdp),
        modifier = Modifier.fillMaxSize()
    ) {
        items(items, key = { it.filePath }) { item ->
            Box(Modifier.clickable { onTap(item) }) {
                SharedMediaCell(item = item, isSelected = isPicked(item), selecting = selecting, onClick = {}, onLongClick = {})
                // item_image CornerLayout stroke: single-pick cells show a ~12% black frame over the photo (measured #E0E0E0 on white)
                if (!selecting) Box(Modifier.matchParentSize().padding(0.5.dp).border(1.25.dp, Color(0x1F000000)))
            }
        }
    }
}
