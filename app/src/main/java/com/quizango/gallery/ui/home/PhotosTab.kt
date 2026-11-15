package com.quizango.gallery.ui.home

import android.text.format.DateFormat
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.quizango.gallery.R
import com.quizango.gallery.domain.model.MediaItem
import com.quizango.gallery.domain.model.MediaType
import com.quizango.gallery.ui.theme.GalleryText
import com.quizango.gallery.ui.theme.sdp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date

/** One line of the Photos grid (2.7.2 PA view types). */
internal sealed interface PhotoRow {
    data class Header(val dayStart: Long) : PhotoRow
    data class Media(val item: MediaItem) : PhotoRow
}

/** Local midnight of [millis]; MediaStore dates may be seconds, so values up to 10^10 are treated as seconds. */
internal fun dayStartOf(millis: Long): Long = Calendar.getInstance().apply {
    timeInMillis = if (millis > 10_000_000_000L) millis else millis * 1000L
    set(Calendar.HOUR_OF_DAY, 0)
    set(Calendar.MINUTE, 0)
    set(Calendar.SECOND, 0)
    set(Calendar.MILLISECOND, 0)
}.timeInMillis

/** A header per calendar day in first-seen order, each followed by that day's media. */
internal fun buildPhotoRows(media: List<MediaItem>): List<PhotoRow> = buildList {
    media.groupBy { dayStartOf(it.dateAdded) }.forEach { (day, items) ->
        add(PhotoRow.Header(day))
        items.forEach { add(PhotoRow.Media(it)) }
    }
}

internal enum class HeaderKind { TODAY, YESTERDAY, THIS_YEAR, OTHER_YEAR }

internal fun headerKind(dayStart: Long, now: Long): HeaderKind {
    val today = dayStartOf(now)
    val yesterday = Calendar.getInstance().apply { timeInMillis = today; add(Calendar.DAY_OF_YEAR, -1) }.timeInMillis
    val year = { t: Long -> Calendar.getInstance().apply { timeInMillis = t }.get(Calendar.YEAR) }
    return when {
        dayStart == today -> HeaderKind.TODAY
        dayStart == yesterday -> HeaderKind.YESTERDAY
        year(dayStart) == year(now) -> HeaderKind.THIS_YEAR
        else -> HeaderKind.OTHER_YEAR
    }
}

// R272 values/arrays.xml rawExtensions
private val RAW_EXTENSIONS = listOf(".dng", ".orf", ".nef", ".arw", ".rw2", ".cr2", ".cr3")

/** GIF / RAW / SVG corner label string for a file name; videos never get one. */
internal fun fileTypeLabel(fileName: String, isVideo: Boolean): Int? = when {
    isVideo -> null
    fileName.endsWith(".gif", ignoreCase = true) -> R.string.GIF
    RAW_EXTENSIONS.any { fileName.endsWith(it, ignoreCase = true) } -> R.string.RAW
    fileName.endsWith(".svg", ignoreCase = true) -> R.string.SVG
    else -> null
}

@Composable
fun PhotosTab(
    mediaList: List<MediaItem>,
    isLoading: Boolean,
    columnCount: Int,
    selectedPaths: Set<String>,
    isSelectionMode: Boolean,
    onMediaClick: (MediaItem) -> Unit,
    onMediaLongClick: (MediaItem) -> Unit
) {
    if (isLoading) {
        // fragment_all_media progressBar: no empty copy until the first sync has returned
        Box(
            modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator(color = colorResource(R.color.colorPrimary))
        }
    } else if (mediaList.isEmpty()) {
        PhotosEmptyState()
    } else {
        // Building rows for thousands of items off the main thread keeps the first frame fast
        val rows by produceState(emptyList<PhotoRow>(), mediaList) {
            value = withContext(Dispatchers.Default) { buildPhotoRows(mediaList) }
        }
        val locale = LocalConfiguration.current.locales[0]
        val dayFormats = remember(locale) {
            SimpleDateFormat(DateFormat.getBestDateTimePattern(locale, "dMMM"), locale) to
                SimpleDateFormat(DateFormat.getBestDateTimePattern(locale, "dMMMyyyy"), locale)
        }
        val now = System.currentTimeMillis()

        LazyVerticalGrid(
            columns = GridCells.Fixed(columnCount),
            contentPadding = PaddingValues(start = 13.sdp, end = 13.sdp, bottom = 50.sdp),
            horizontalArrangement = Arrangement.spacedBy(4.sdp),
            modifier = Modifier.fillMaxSize()
        ) {
            items(
                count = rows.size,
                key = { i ->
                    when (val row = rows[i]) {
                        is PhotoRow.Header -> "header_${row.dayStart}"
                        is PhotoRow.Media -> row.item.id
                    }
                },
                span = { i -> GridItemSpan(if (rows[i] is PhotoRow.Media) 1 else maxLineSpan) },
                contentType = { i -> rows[i]::class }
            ) { i ->
                when (val row = rows[i]) {
                    is PhotoRow.Header -> PhotosDateHeader(
                        when (headerKind(row.dayStart, now)) {
                            HeaderKind.TODAY -> stringResource(R.string.today)
                            HeaderKind.YESTERDAY -> stringResource(R.string.yesterday)
                            HeaderKind.THIS_YEAR -> dayFormats.first.format(Date(row.dayStart))
                            HeaderKind.OTHER_YEAR -> dayFormats.second.format(Date(row.dayStart))
                        }
                    )
                    is PhotoRow.Media -> {
                        val item = row.item
                        PhotosTabGridItem(
                            item = item,
                            isSelected = selectedPaths.contains(item.filePath),
                            isSelectionMode = isSelectionMode,
                            onClick = { if (isSelectionMode) onMediaLongClick(item) else onMediaClick(item) },
                            onLongClick = { onMediaLongClick(item) }
                        )
                    }
                }
            }
        }
    }
}

/** photos_tab_item_date */
@Composable
private fun PhotosDateHeader(label: String) {
    Text(
        text = label,
        style = GalleryText.BodySmallMedium,
        color = colorResource(R.color.photos_tab_header_text),
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
        modifier = Modifier.fillMaxWidth().padding(top = 8.sdp, bottom = 6.sdp)
    )
}

/** photos_tab_item_grid as bound by 2.7.2 PH: no duration text and no selection tint. */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun PhotosTabGridItem(
    item: MediaItem,
    isSelected: Boolean,
    isSelectionMode: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit
) {
    // 2.sdp above and below each card gives the 4.sdp row gap
    Box(modifier = Modifier.padding(vertical = 2.sdp)) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1f)
                .clip(RoundedCornerShape(3.sdp))
                .background(colorResource(R.color.photos_tab_thumb_bg))
                .then(if (isSelectionMode) Modifier.semantics { selected = isSelected } else Modifier)
                .combinedClickable(onClick = onClick, onLongClick = onLongClick)
        ) {
            AsyncImage(
                model = item.contentUri,
                contentDescription = item.fileName,
                placeholder = painterResource(R.drawable.image_loading_albums),
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
            val isVideo = item.mediaType == MediaType.VIDEO
            Row(modifier = Modifier.align(Alignment.BottomStart).padding(start = 3.sdp, bottom = 5.sdp)) {
                if (isVideo) {
                    Image(painterResource(R.drawable.ic_common_video), contentDescription = null, modifier = Modifier.size(17.sdp))
                }
                if (item.isFavorite) {
                    Image(painterResource(R.drawable.ic_common_fav), contentDescription = null, modifier = Modifier.size(17.sdp))
                }
            }
            fileTypeLabel(item.fileName, isVideo)?.let { label ->
                Text(
                    text = stringResource(label),
                    style = GalleryText.BodyXSmallSemiBold.copy(shadow = Shadow(color = Color(0xFF424242), blurRadius = 4f)),
                    color = colorResource(R.color.onlyWhite),
                    modifier = Modifier.align(Alignment.TopStart).padding(5.sdp)
                )
            }
            if (isSelectionMode) {
                Image(
                    painterResource(if (isSelected) R.drawable.ic_check else R.drawable.ic_un_check),
                    contentDescription = null,
                    modifier = Modifier.align(Alignment.TopEnd).padding(2.sdp).size(17.sdp)
                )
            }
        }
    }
}

@Composable
private fun PhotosEmptyState() {
    Box(
        modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.padding(horizontal = 40.dp)
        ) {
            Image(
                painter = painterResource(id = R.drawable.ic_empty_img),
                contentDescription = null,
                modifier = Modifier.size(160.dp)
            )
            Spacer(modifier = Modifier.height(24.dp))
            Text(
                text = stringResource(id = R.string.no_items),
                fontSize = 18.sp,
                color = MaterialTheme.colorScheme.onBackground
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = stringResource(id = R.string.no_media_file),
                fontSize = 13.5.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                lineHeight = 18.sp
            )
        }
    }
}
