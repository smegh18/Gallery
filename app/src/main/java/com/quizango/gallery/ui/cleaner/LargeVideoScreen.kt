package com.quizango.gallery.ui.cleaner

import android.widget.Toast
import androidx.annotation.ColorRes
import androidx.annotation.StringRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.quizango.gallery.R
import com.quizango.gallery.data.local.PathMutationOutcome
import com.quizango.gallery.data.local.failedPaths
import com.quizango.gallery.data.repository.MediaRepository
import com.quizango.gallery.domain.model.MediaItem
import com.quizango.gallery.ui.browse.ListState
import com.quizango.gallery.ui.browse.SharedMediaToolbar
import com.quizango.gallery.ui.browse.listStateOf
import com.quizango.gallery.ui.browse.pruneSelection
import com.quizango.gallery.ui.browse.sharedHeaderDate
import com.quizango.gallery.ui.browse.sharedMediaRows
import com.quizango.gallery.ui.browse.sharedRowKey
import com.quizango.gallery.ui.browse.sharedRowSpan
import com.quizango.gallery.ui.browse.toggleAll
import com.quizango.gallery.ui.home.HeaderKind
import com.quizango.gallery.ui.home.PhotoRow
import com.quizango.gallery.ui.home.PhotosEmptyState
import com.quizango.gallery.ui.home.ProgressTextDialog
import com.quizango.gallery.ui.home.drawableBackground
import com.quizango.gallery.ui.home.headerKind
import com.quizango.gallery.ui.theme.Inter
import com.quizango.gallery.ui.theme.sdp
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

fun formatVideoSize(bytes: Long): String {
    val kb = bytes / 1024.0
    val mb = kb / 1024.0
    val gb = mb / 1024.0
    return when {
        gb >= 1.0 -> String.format("%.2f GB", gb)
        mb >= 1.0 -> String.format("%.1f MB", mb)
        kb >= 1.0 -> String.format("%.0f KB", kb)
        else -> "$bytes B"
    }
}

/** What differs between LargeVideoActivity and ScreenShortActivity; the cell, header and dialog layouts are shared. */
internal enum class CleanerMediaKind(
    @StringRes val title: Int,
    val columns: Int,
    @StringRes val empty: Int,
    @StringRes val confirmTitle: Int,
    @StringRes val confirmBody: Int,
    @ColorRes val background: Int,
    val showsSize: Boolean // dt/a shows txt_file_size; the screenshot cell (b0/b) keeps it gone
) {
    LARGE_VIDEO(
        R.string.tool_bar_large_video, 3, R.string.cleaner_video_empty_layout_text,
        R.string.permanent_delete_video_title, R.string.permanent_delete_video_sub_title, R.color.white, true
    )
}

/** btn_video_del / btn_cleaner_del text: "Delete Selected", "Delete (N)", or "Delete All (N)" when everything is picked. */
internal fun cleanerDeleteLabel(selected: Int, total: Int, deleteSelected: String, delete: String, deleteAll: String): String = when {
    selected == 0 -> deleteSelected
    selected == total -> "$deleteAll ($selected)"
    else -> "$delete ($selected)"
}

/** Deletes exactly the confirmed [snapshot] for good; the paths that failed are returned to stay selected for a retry. */
internal suspend fun runCleanerDelete(
    snapshot: List<String>,
    delete: suspend (List<String>) -> List<PathMutationOutcome>
): Set<String> = delete(snapshot).failedPaths().toSet()

@Composable
fun LargeVideoScreen(mediaRepository: MediaRepository, onNavigateBack: () -> Unit) {
    val flow = remember { mediaRepository.getLargeVideosFlow() }
    CleanerMediaScreen(CleanerMediaKind.LARGE_VIDEO, flow.collectAsState(initial = null).value, mediaRepository, onNavigateBack)
}

/**
 * activity_large_video / activity_screen_short: layout_toolbar with Select all, a day-grouped grid that is always
 * in pick mode, the Delete button, and dialog_cleaner_permanently_delete. [items] is null until the first query returns.
 */
@Composable
internal fun CleanerMediaScreen(
    kind: CleanerMediaKind,
    items: List<MediaItem>?,
    mediaRepository: MediaRepository,
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var selected by remember { mutableStateOf(emptySet<String>()) }
    // Frozen when Delete is tapped, so the dialog confirms exactly what was shown
    var pending by remember { mutableStateOf<List<String>?>(null) }
    var deleting by remember { mutableStateOf(false) }
    val paths = remember(items) { items.orEmpty().map { it.filePath } }

    // Deleted files leave the grid: drop them from the selection
    LaunchedEffect(paths) { selected = pruneSelection(selected, paths.toSet()) }

    Box(Modifier.fillMaxSize().background(colorResource(kind.background))) {
        Column(Modifier.fillMaxSize()) {
            SharedMediaToolbar(
                title = if (selected.isEmpty()) stringResource(kind.title) else stringResource(R.string.items_selected, selected.size),
                allSelected = selected.size == paths.size,
                onBack = onNavigateBack,
                onToggleAll = { selected = toggleAll(selected, paths) },
                showToggleAll = paths.isNotEmpty()
            )
            when (listStateOf(items, kind.columns)) {
                ListState.LOADING -> Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = colorResource(R.color.colorPrimary))
                }
                ListState.EMPTY -> Box(Modifier.weight(1f).fillMaxWidth()) {
                    PhotosEmptyState(kind.empty, body = null, titleHorizontalPadding = 53.sdp)
                }
                ListState.CONTENT -> CleanerMediaGrid(
                    kind = kind,
                    items = items.orEmpty(),
                    selected = selected,
                    onToggle = { p -> selected = if (p in selected) selected - p else selected + p },
                    modifier = Modifier.weight(1f)
                )
            }
            Spacer(Modifier.navigationBarsPadding())
        }
        if (paths.isNotEmpty()) CleanerDeleteButton(
            text = cleanerDeleteLabel(
                selected.size, paths.size,
                stringResource(R.string.delete_selected), stringResource(R.string.delete), stringResource(R.string.delete_all)
            ),
            enabled = selected.isNotEmpty(),
            onClick = { pending = paths.filter { it in selected } },
            modifier = Modifier.align(Alignment.BottomCenter)
        )
    }

    pending?.let { snapshot ->
        CleanerDeleteDialog(
            title = kind.confirmTitle,
            subtitle = kind.confirmBody,
            onConfirm = {
                pending = null
                deleting = true
                scope.launch {
                    // A half-finished delete would leave rows for removed files; finish it even if the screen goes
                    val failed = withContext(NonCancellable) { runCleanerDelete(snapshot, mediaRepository::deletePermanently) }
                    selected = failed
                    deleting = false
                    val message = if (failed.isEmpty()) R.string.selected_media_files_successfully_deleted else R.string.something_went_wrong
                    Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
                }
            },
            onDismiss = { pending = null }
        )
    }
    if (deleting) ProgressTextDialog(stringResource(R.string.deleting))
}

@Composable
private fun CleanerMediaGrid(
    kind: CleanerMediaKind,
    items: List<MediaItem>,
    selected: Set<String>,
    onToggle: (String) -> Unit,
    modifier: Modifier
) {
    val rows = remember(items) { sharedMediaRows(items) }
    val now = remember(items) { System.currentTimeMillis() }
    LazyVerticalGrid(columns = GridCells.Fixed(kind.columns), modifier = modifier.fillMaxSize()) {
        items(
            count = rows.size,
            key = { sharedRowKey(rows[it]) },
            span = { GridItemSpan(sharedRowSpan(rows[it], maxLineSpan)) },
            contentType = { rows[it]::class }
        ) { i ->
            when (val row = rows[i]) {
                PhotoRow.Ad -> Unit
                // item_screenshot_main: 12 sp Inter Regular black
                is PhotoRow.Header -> Text(
                    text = sharedHeaderDate(row.dayStart, now) ?: stringResource(
                        if (headerKind(row.dayStart, now) == HeaderKind.TODAY) R.string.today else R.string.yesterday
                    ),
                    style = TextStyle(fontFamily = Inter, fontWeight = FontWeight.Normal, fontSize = 12.sp),
                    color = colorResource(R.color.black),
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 5.sdp, vertical = 4.sdp)
                )
                is PhotoRow.Media -> CleanerMediaCell(row.item, row.item.filePath in selected, kind.showsSize) { onToggle(row.item.filePath) }
            }
        }
    }
}

/** item_large_video: square centre-crop thumbnail, check top end, the file size pill on large videos; tap toggles. */
@Composable
private fun CleanerMediaCell(item: MediaItem, isSelected: Boolean, showsSize: Boolean, onToggle: () -> Unit) {
    Box(
        Modifier
            .padding(1.sdp)
            .fillMaxWidth()
            .aspectRatio(1f)
            .toggleable(value = isSelected, role = Role.Checkbox, onValueChange = { onToggle() })
    ) {
        AsyncImage(
            model = item.contentUri,
            contentDescription = item.fileName,
            placeholder = painterResource(R.drawable.image_loading_albums),
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
        )
        Image(
            painter = painterResource(if (isSelected) R.drawable.ic_check else R.drawable.ic_un_check),
            contentDescription = null,
            modifier = Modifier.align(Alignment.TopEnd).padding(1.sdp)
        )
        if (showsSize) Text(
            text = formatCleanerSize(item.fileSize),
            style = TextStyle(fontFamily = Inter, fontWeight = FontWeight.Bold, fontSize = 13.sp),
            color = colorResource(R.color.onlyWhite),
            maxLines = 1,
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(start = 5.sdp, bottom = 5.sdp)
                .drawableBackground(R.drawable.large_video_duration)
                .padding(horizontal = 8.sdp, vertical = 5.sdp)
        )
    }
}

/** btn_video_del: 38.sdp pill, btn_delete_disable grey and inert until something is picked, then dark_blue. */
@Composable
private fun CleanerDeleteButton(text: String, enabled: Boolean, onClick: () -> Unit, modifier: Modifier) {
    Box(
        modifier
            .navigationBarsPadding()
            .padding(start = 15.sdp, end = 15.sdp, bottom = 10.sdp)
            .fillMaxWidth()
            .height(38.sdp)
            .clip(RoundedCornerShape(25.dp))
            .background(colorResource(if (enabled) R.color.dark_blue else R.color.btn_delete_disable))
            .clickable(enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            style = TextStyle(fontFamily = Inter, fontWeight = FontWeight.Bold, fontSize = 16.sp),
            color = colorResource(R.color.onlyWhite),
            textAlign = TextAlign.Center,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}
