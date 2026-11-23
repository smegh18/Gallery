package com.quizango.gallery.ui.cleaner

import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.quizango.gallery.R
import com.quizango.gallery.data.local.DuplicateFinder
import com.quizango.gallery.data.repository.MediaRepository
import com.quizango.gallery.domain.model.MediaItem
import com.quizango.gallery.ui.browse.ListState
import com.quizango.gallery.ui.browse.SharedMediaToolbar
import com.quizango.gallery.ui.browse.fallDown
import com.quizango.gallery.ui.browse.listStateOf
import com.quizango.gallery.ui.browse.pruneSelection
import com.quizango.gallery.ui.home.DeleteMediaDialog
import com.quizango.gallery.ui.home.FastScrollThumb
import com.quizango.gallery.ui.home.PhotosEmptyState
import com.quizango.gallery.ui.home.drawableBackground
import com.quizango.gallery.ui.theme.GalleryText
import com.quizango.gallery.ui.theme.sdp
import kotlinx.coroutines.delay

/** A confirmed duplicate delete (DP 1284-1406 `oa/j` Yes): the frozen photos and the Move to Recycle Bin choice. */
data class DuplicateDeleteRequest(val items: List<MediaItem>, val moveToRecycleBin: Boolean) {
    val paths: List<String> get() = items.map { it.filePath }
}

/** The selected photos still listed, in display order; null means nothing to delete (please_select_items). */
internal fun selectedDuplicates(groups: List<List<MediaItem>>, selected: Set<String>): List<MediaItem>? =
    groups.flatten().filter { it.filePath in selected }.takeIf { it.isNotEmpty() }

/** Keep One Photo on (DP 322-660): every photo except the first of each group. */
internal fun keepOneSelection(groups: List<List<MediaItem>>): Set<String> =
    groups.flatMapTo(LinkedHashSet()) { group -> group.drop(1).map { it.filePath } }

/** item_duplicate_main title (DP 2179-2300): "Group 1 (2 Images)", numbered from 1. */
internal fun duplicateGroupLabel(group: String, index: Int, count: Int, images: String): String =
    "$group ${index + 1} ($count $images)"

/**
 * DuplicatePhotoActivity (activity_duplicate_photo, item_duplicate_main, item_duplicate_sub): layout_toolbar,
 * the Keep One Photo switch (off on open), groups in 4 columns with a fast-scroll thumb, and Delete Selected
 * while anything is selected. Grouping is the reference fingerprint ([DuplicateFinder.group], cached after the scan).
 */
@Composable
fun DuplicatePhotoScreen(
    mediaRepository: MediaRepository,
    duplicateFinder: DuplicateFinder,
    onNavigateBack: () -> Unit,
    onDelete: (DuplicateDeleteRequest) -> Unit
) {
    val context = LocalContext.current
    val allMedia by remember { mediaRepository.getActiveMediaFlow() }.collectAsState(initial = null)
    // null until the first grouping is in, so the empty copy never flashes first
    val groups by produceState<List<List<MediaItem>>?>(null, allMedia) {
        val media = allMedia ?: return@produceState
        value = DuplicateFinder.group(media, duplicateFinder.fingerprints(media))
    }
    var selected by remember { mutableStateOf(emptySet<String>()) }
    var keepOne by remember { mutableStateOf(false) }
    // Frozen when Delete Selected is tapped; later selection changes cannot alter what the dialog confirms
    var pending by remember { mutableStateOf<List<MediaItem>?>(null) }

    // Deleted or changed files leave their group: drop them from the selection
    LaunchedEffect(groups) {
        val present = groups?.flatMapTo(HashSet()) { g -> g.map { it.filePath } } ?: return@LaunchedEffect
        selected = pruneSelection(selected, present)
    }

    Box(Modifier.fillMaxSize().background(colorResource(R.color.backgroundColor))) {
        Column(Modifier.fillMaxSize()) {
            SharedMediaToolbar(title = stringResource(R.string.duplicate_images), onBack = onNavigateBack)
            val shown = groups
            when (listStateOf(shown, 4)) {
                ListState.LOADING -> Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = colorResource(R.color.colorPrimary))
                }
                ListState.EMPTY -> Box(Modifier.weight(1f).fillMaxWidth()) {
                    PhotosEmptyState(R.string.cleaner_duplicate_empty_layout_text, R.string.no_duplicate_files, titleHorizontalPadding = 53.sdp)
                }
                ListState.CONTENT -> {
                    KeepOneRow(keepOne) { on ->
                        keepOne = on
                        selected = if (on) keepOneSelection(shown.orEmpty()) else emptySet()
                    }
                    DuplicateGrid(
                        groups = shown.orEmpty(),
                        selected = selected,
                        onToggle = { path -> selected = if (path in selected) selected - path else selected + path },
                        modifier = Modifier.weight(1f)
                    )
                }
            }
            Spacer(Modifier.navigationBarsPadding())
        }
        if (selected.isNotEmpty()) DeleteSelectedButton(
            count = selected.size,
            onClick = {
                val snapshot = selectedDuplicates(groups.orEmpty(), selected)
                if (snapshot == null) Toast.makeText(context, context.getString(R.string.please_select_items), Toast.LENGTH_SHORT).show()
                else pending = snapshot
            },
            modifier = Modifier.align(Alignment.BottomCenter)
        )
    }

    pending?.let { items ->
        DeleteMediaDialog(
            count = items.size,
            customTitle = stringResource(R.string.deleted_title) + " " + items.size + " " + stringResource(R.string.deleted_items),
            onConfirm = { toBin ->
                pending = null
                onDelete(DuplicateDeleteRequest(items, toBin))
            },
            onDismiss = { pending = null }
        )
    }
}

/** keepPhotoLayout: "Keep One Photo" and the SwitchCompat (thumb colorPrimary when on). */
@Composable
private fun KeepOneRow(checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    val primary = colorResource(R.color.colorPrimary)
    Row(
        modifier = Modifier.fillMaxWidth().padding(top = 5.sdp).padding(horizontal = 10.sdp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = stringResource(R.string.keep_best_photo),
            style = GalleryText.BodyMediumMedium,
            color = colorResource(R.color.titleColor),
            modifier = Modifier.weight(1f)
        )
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = primary,
                checkedTrackColor = primary.copy(alpha = 0.3f),
                checkedBorderColor = Color.Transparent
            )
        )
    }
}

@Composable
private fun DuplicateGrid(
    groups: List<List<MediaItem>>,
    selected: Set<String>,
    onToggle: (String) -> Unit,
    modifier: Modifier
) {
    val gridState = rememberLazyGridState()
    val groupWord = stringResource(R.string.group)
    val imagesWord = stringResource(R.string.images)
    // layout_animation runs on the first fill only, as in SharedMediaList
    var entered by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        delay(1500)
        entered = true
    }
    BoxWithConstraints(modifier.fillMaxWidth()) {
        LazyVerticalGrid(
            state = gridState,
            columns = GridCells.Fixed(4),
            contentPadding = PaddingValues(start = 2.sdp, end = 2.sdp, bottom = 50.sdp),
            modifier = Modifier.fillMaxSize()
        ) {
            var order = 0
            groups.forEachIndexed { index, group ->
                val headerOrder = order
                order += group.size + 1
                item(key = "group:" + group.first().filePath, span = { GridItemSpan(maxLineSpan) }) {
                    Text(
                        text = duplicateGroupLabel(groupWord, index, group.size, imagesWord),
                        style = GalleryText.BodySmallMedium,
                        color = colorResource(R.color.black),
                        modifier = (if (entered) Modifier else Modifier.fallDown(headerOrder))
                            .fillMaxWidth()
                            .padding(horizontal = 5.sdp, vertical = 4.sdp)
                    )
                }
                items(group.size, key = { group[it].filePath }) { i ->
                    val item = group[i]
                    DuplicateCell(
                        item = item,
                        isSelected = item.filePath in selected,
                        onToggle = { onToggle(item.filePath) },
                        modifier = if (entered) Modifier else Modifier.fallDown(headerOrder + 1 + i)
                    )
                }
            }
        }
        FastScrollThumb(gridState, trackHeightPx = constraints.maxHeight)
    }
}

/** item_duplicate_sub: square card radius 3.sdp, check top end, the folder name as a shadowed caption. */
@Composable
private fun DuplicateCell(item: MediaItem, isSelected: Boolean, onToggle: () -> Unit, modifier: Modifier) {
    Box(
        modifier
            .padding(1.sdp)
            .fillMaxWidth()
            .aspectRatio(1f)
            .clip(RoundedCornerShape(3.sdp))
            .toggleable(value = isSelected, role = Role.Checkbox, onValueChange = { onToggle() })
    ) {
        AsyncImage(
            model = item.contentUri,
            contentDescription = item.fileName,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
        )
        Image(
            painter = painterResource(if (isSelected) R.drawable.ic_check else R.drawable.ic_un_check),
            contentDescription = null,
            modifier = Modifier.align(Alignment.TopEnd).padding(1.sdp)
        )
        Text(
            text = item.folderName,
            style = GalleryText.BodyXSmallMedium.copy(shadow = Shadow(Color.Black, Offset(2f, 2f), 4f)),
            color = colorResource(R.color.onlyWhite),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .padding(start = 10.sdp, end = 10.sdp, top = 6.sdp, bottom = 3.sdp)
        )
    }
}

/** delBtnLayout: "Delete Selected (N items)" and the `ic_ads_left` badge (kept by decision Q2; no ad is shown). */
@Composable
private fun DeleteSelectedButton(count: Int, onClick: () -> Unit, modifier: Modifier) {
    Row(
        modifier = modifier
            .navigationBarsPadding()
            .padding(start = 15.sdp, end = 15.sdp, bottom = 10.sdp)
            .fillMaxWidth()
            .height(40.sdp)
            .clip(RoundedCornerShape(25.dp))
            .drawableBackground(R.drawable.ic_delete_duplicate_btn_selected_bg)
            .clickable(onClick = onClick)
            .padding(horizontal = 10.sdp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = deleteSelectedLabel(stringResource(R.string.delete_selected), count, stringResource(R.string.items)),
            style = GalleryText.BodyXLargeSemiBold,
            color = colorResource(R.color.onlyWhite),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f, fill = false)
        )
        Image(
            painter = painterResource(R.drawable.ic_ads_left),
            contentDescription = null,
            modifier = Modifier.padding(start = 8.dp)
        )
    }
}

/** DP 546-651: "Delete Selected (19 items)". */
internal fun deleteSelectedLabel(deleteSelected: String, count: Int, items: String): String = "$deleteSelected ($count $items)"

fun formatDuplicateSize(bytes: Long): String {
    return when {
        bytes >= 1024L * 1024L * 1024L -> "%.2f GB".format(bytes.toDouble() / (1024 * 1024 * 1024))
        bytes >= 1024L * 1024L -> "%.1f MB".format(bytes.toDouble() / (1024 * 1024))
        bytes >= 1024L -> "%d KB".format(bytes / 1024)
        else -> "$bytes B"
    }
}
