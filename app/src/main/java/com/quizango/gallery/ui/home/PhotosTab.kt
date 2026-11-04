package com.quizango.gallery.ui.home

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.outlined.Circle
import androidx.compose.material.icons.outlined.PhotoLibrary
import androidx.compose.material3.*
import androidx.compose.runtime.getValue
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.foundation.Image
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.quizango.gallery.R
import com.quizango.gallery.domain.model.MediaItem
import com.quizango.gallery.domain.model.MediaType
import com.quizango.gallery.ui.theme.GalleryBlue
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun PhotosTab(
    mediaList: List<MediaItem>,
    columnCount: Int,
    selectedPaths: Set<String>,
    isSelectionMode: Boolean,
    onMediaClick: (MediaItem) -> Unit,
    onMediaLongClick: (MediaItem) -> Unit,
    onRefresh: () -> Unit
) {
    if (mediaList.isEmpty()) {
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
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = stringResource(id = R.string.no_media_file),
                    fontSize = 13.5.sp,
                    fontWeight = FontWeight.Normal,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                    lineHeight = 18.sp
                )
            }
        }
    } else {
        // Group items chronologically (remembered with todayMidnight to invalidate on day change)
        val todayMidnight = getTodayMidnight()
        // Grouping thousands of items off the main thread keeps the first frame fast
        val grouped by androidx.compose.runtime.produceState(emptyMap<String, List<MediaItem>>(), mediaList, todayMidnight) {
            value = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Default) {
                mediaList.groupBy { formatGroupHeader(it.dateAdded) }
            }
        }

        LazyVerticalGrid(
            columns = GridCells.Fixed(columnCount),
            contentPadding = PaddingValues(2.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            grouped.forEach { (header, items) ->
                item(
                    span = { GridItemSpan(columnCount) },
                    key = "header_" + header,
                    contentType = "header"
                ) {
                    Text(
                        text = header,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 10.dp)
                    )
                }

                items(
                    items = items,
                    key = { it.id },
                    contentType = { "photo" }
                ) { item ->
                    val isSelected = selectedPaths.contains(item.filePath)
                    PhotoGridItem(
                        item = item,
                        isSelected = isSelected,
                        isSelectionMode = isSelectionMode,
                        onClick = {
                            if (isSelectionMode) {
                                onMediaLongClick(item)
                            } else {
                                onMediaClick(item)
                            }
                        },
                        onLongClick = { onMediaLongClick(item) }
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun PhotoGridItem(
    item: MediaItem,
    isSelected: Boolean,
    isSelectionMode: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit
) {
    val context = LocalContext.current

    Box(
        modifier = Modifier
            .aspectRatio(1f)
            .padding(0.5.dp)
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick
            )
    ) {
        AsyncImage(
            model = item.contentUri,
            contentDescription = item.fileName,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
        )

        // Overlay for video badge or duration (Reference item_grid_gallery.xml)
        if (item.mediaType == MediaType.VIDEO) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(start = 4.dp, bottom = 4.dp)
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_media_video),
                    contentDescription = "Video",
                    tint = Color.Unspecified,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(3.dp))
                Text(
                    text = formatDuration(item.durationMs),
                    color = Color.White,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium,
                    style = androidx.compose.ui.text.TextStyle(
                        shadow = androidx.compose.ui.graphics.Shadow(
                            color = Color.Black,
                            offset = androidx.compose.ui.geometry.Offset(2f, 2f),
                            blurRadius = 4f
                        )
                    )
                )
            }
        }

        // Favorite icon badge (Reference item_grid_gallery.xml layout_gravity="end|bottom")
        if (item.isFavorite) {
            Icon(
                painter = painterResource(id = R.drawable.ic_common_fav),
                contentDescription = "Favorite",
                tint = Color.Unspecified,
                modifier = Modifier
                    .size(20.dp)
                    .align(Alignment.BottomEnd)
                    .padding(2.dp)
            )
        }

        // Selection overlay and badge (Reference item_grid_gallery.xml layout_gravity="end")
        if (isSelectionMode) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(if (isSelected) GalleryBlue.copy(alpha = 0.35f) else Color.Transparent)
            )

            Icon(
                painter = painterResource(
                    id = if (isSelected) R.drawable.ic_check else R.drawable.ic_un_check
                ),
                contentDescription = if (isSelected) "Selected" else "Not selected",
                tint = Color.Unspecified,
                modifier = Modifier
                    .size(24.dp)
                    .align(Alignment.TopEnd)
                    .padding(2.dp)
            )
        }
    }
}

private var cachedTodayMidnight = 0L
private var cachedYesterdayMidnight = 0L
private var lastMidnightCalcTime = 0L
private val headerDateFormat = SimpleDateFormat("dd MMM yyyy", Locale.ENGLISH)

@Synchronized
private fun getMidnightBoundaries(): Pair<Long, Long> {
    val now = System.currentTimeMillis()
    if (now - lastMidnightCalcTime > 60_000L) {
        val cal = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        cachedTodayMidnight = cal.timeInMillis
        cal.add(Calendar.DAY_OF_YEAR, -1)
        cachedYesterdayMidnight = cal.timeInMillis
        lastMidnightCalcTime = now
    }
    return Pair(cachedTodayMidnight, cachedYesterdayMidnight)
}

fun getTodayMidnight(): Long = getMidnightBoundaries().first

fun formatGroupHeader(timestamp: Long): String {
    val millis = if (timestamp > 10_000_000_000L) timestamp else timestamp * 1000L
    val (todayMidnight, yesterdayMidnight) = getMidnightBoundaries()
    val tomorrowMidnight = todayMidnight + 86_400_000L
    return when {
        millis in todayMidnight until tomorrowMidnight -> "Today"
        millis in yesterdayMidnight until todayMidnight -> "Yesterday"
        else -> synchronized(headerDateFormat) { headerDateFormat.format(Date(millis)) }
    }
}

fun formatDuration(durationMs: Long): String {
    if (durationMs <= 0) return "0:00"
    val totalSec = durationMs / 1000
    val minutes = totalSec / 60
    val seconds = totalSec % 60
    return if (minutes >= 60) {
        val hours = minutes / 60
        val remMin = minutes % 60
        String.format(Locale.US, "%d:%02d:%02d", hours, remMin, seconds)
    } else {
        String.format(Locale.US, "%02d:%02d", minutes, seconds)
    }
}
