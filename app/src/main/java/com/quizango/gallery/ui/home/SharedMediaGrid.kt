package com.quizango.gallery.ui.home

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.quizango.gallery.R
import com.quizango.gallery.domain.model.MediaItem
import com.quizango.gallery.domain.model.MediaType
import com.quizango.gallery.ui.theme.GalleryBlue
import com.quizango.gallery.ui.theme.Inter
import java.text.SimpleDateFormat
import java.util.*

/** Pre-2.7.2 grid item, header and duration helpers, used by Search by colour and Recycle Bin until their phases port them. */

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
                        fontFamily = Inter,
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
