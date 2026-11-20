package com.quizango.gallery.ui.home

import android.text.format.DateFormat
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.calculateCentroid
import androidx.compose.foundation.gestures.calculateZoom
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.ui.geometry.isSpecified
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.IntOffset
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.layout
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.platform.LocalContext
import androidx.annotation.DrawableRes
import androidx.core.content.ContextCompat
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.quizango.gallery.R
import com.quizango.gallery.domain.model.MediaItem
import com.quizango.gallery.domain.model.MediaType
import com.quizango.gallery.ui.theme.GalleryText
import com.quizango.gallery.ui.theme.sdp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import kotlin.math.roundToInt

/** One line of the Photos grid (2.7.2 PA view types). */
internal sealed interface PhotoRow {
    data object Ad : PhotoRow
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

/** The ad slot first (PF puts it at index 0), then a header per calendar day in first-seen order, each followed by that day's media. */
internal fun buildPhotoRows(media: List<MediaItem>): List<PhotoRow> = if (media.isEmpty()) emptyList() else buildList {
    add(PhotoRow.Ad)
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

/** Fast-scroll thumb position 0..1 for the first visible index; 0 when everything fits. */
internal fun thumbFraction(firstVisible: Int, visibleCount: Int, total: Int): Float =
    if (total <= visibleCount) 0f else (firstVisible.toFloat() / (total - visibleCount)).coerceIn(0f, 1f)

/** Inverse of [thumbFraction], so the thumb doesn't jump when the drag ends. */
internal fun indexForThumbFraction(fraction: Float, visibleCount: Int, total: Int): Int = when {
    total <= visibleCount -> 0
    fraction >= 1f -> total - 1
    else -> (fraction.coerceIn(0f, 1f) * (total - visibleCount)).roundToInt()
}

// S272/z/t: the step fires mid-gesture once the span ratio since the last step reaches 1.12 (spread, fewer columns)
// or 0.90 (pinch, more columns); a release never steps, it springs back.
private const val PINCH_IN_STEP = 1.12f
private const val PINCH_OUT_STEP = 0.90f
// S272/ai/n BsUTWEAMAI: elastic (ratio - 1) * 0.18 + 1, kept in 0.95..1.05, when there is no step that way
private const val ELASTIC_DAMPING = 0.18f
private const val ELASTIC_MIN = 0.95f
private const val ELASTIC_MAX = 1.05f
private const val ELASTIC_RETURN_MS = 200 // ai/n OooOOo0 elastic branch
// S272/ai/n constructor PathInterpolator(0.18, 0, 0.08, 1), used by every pinch animation
private val PinchEasing = CubicBezierEasing(0.18f, 0f, 0.08f, 1f)

/** Column count once the span ratio since the last step is [zoom]: spread gives fewer columns, kept in 3..5. */
internal fun nextColumnCount(current: Int, zoom: Float): Int = clampColumnCount(
    when {
        zoom >= PINCH_IN_STEP -> current - 1
        zoom <= PINCH_OUT_STEP -> current + 1
        else -> current
    }
)

/** S272/ai/n: how far the preview is toward the next column count, (ratio - 1) / 0.12 spreading, (1 - ratio) / 0.10 pinching. */
internal fun pinchProgress(zoom: Float): Float =
    if (zoom >= 1f) ((zoom - 1f) / (PINCH_IN_STEP - 1f)).coerceIn(0f, 1f)
    else ((1f - zoom) / (1f - PINCH_OUT_STEP)).coerceIn(0f, 1f)

/** S272/ai/n OooO0Oo (finish) and OooOOo0 (spring back): 140 ms plus 60 ms times the distance still to cover. */
internal fun pinchSettleMs(remaining: Float): Int = (140f + 60f * remaining.coerceIn(0f, 1f)).toInt()

/** Live preview scale: the cells follow the fingers and reach the next count's cell size at the step; elastic at 3 and 5. */
internal fun pinchPreviewScale(current: Int, zoom: Float): Float {
    val target = if (zoom >= 1f) current - 1 else current + 1
    if (target != clampColumnCount(target)) {
        return ((zoom - 1f) * ELASTIC_DAMPING + 1f).coerceIn(ELASTIC_MIN, ELASTIC_MAX)
    }
    return 1f + pinchProgress(zoom) * (current.toFloat() / target - 1f)
}

@Composable
fun PhotosTab(
    mediaList: List<MediaItem>,
    isLoading: Boolean,
    columnCount: Int,
    selectedPaths: Set<String>,
    isSelectionMode: Boolean,
    onMediaClick: (MediaItem) -> Unit,
    onMediaLongClick: (MediaItem) -> Unit,
    onColumnCountChange: (Int) -> Unit
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
        val gridState = rememberLazyGridState()
        // Shown at once on commit; the saved value catches up through the preference flow
        // one state object: the pointerInput(Unit) gesture below keeps writing to the instance it captured
        var shownCount by remember { mutableIntStateOf(columnCount) }
        LaunchedEffect(columnCount) { shownCount = columnCount }
        val count by rememberUpdatedState(shownCount)
        val onCount by rememberUpdatedState(onColumnCountChange)
        val scale = remember { Animatable(1f) }
        var origin by remember { mutableStateOf(TransformOrigin.Center) }
        val scope = rememberCoroutineScope()
        val placement = tween<IntOffset>(pinchSettleMs(0f), easing = PinchEasing)

        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                // Initial pass, so a pinch is taken before the grid or pager can scroll; one finger is never consumed
                .pointerInput(Unit) {
                    awaitEachGesture {
                        awaitFirstDown(requireUnconsumed = false, pass = PointerEventPass.Initial)
                        var zoom = 1f // span ratio since the gesture began or since the last step
                        var cols = count
                        var pinching = false
                        do {
                            val event = awaitPointerEvent(PointerEventPass.Initial)
                            if (event.changes.count { it.pressed } >= 2) {
                                pinching = true
                                zoom *= event.calculateZoom()
                                val c = event.calculateCentroid()
                                if (c.isSpecified && size.width > 0 && size.height > 0) {
                                    origin = TransformOrigin(c.x / size.width, c.y / size.height)
                                }
                                val next = nextColumnCount(cols, zoom)
                                if (next != cols) {
                                    // ai/n OooOO0O: step now; the new layout takes over at the preview size and settles
                                    // while cells glide (animateItem). shortcut: no 120 ms / 48 px re-step guard (z/t);
                                    // re-basing the ratio already needs another 12% / 10% of finger travel.
                                    val from = cols
                                    val left = 1f - pinchProgress(zoom)
                                    cols = next
                                    zoom = 1f
                                    shownCount = next
                                    onCount(next)
                                    scope.launch {
                                        scale.snapTo(scale.value * next / from)
                                        scale.animateTo(1f, tween(pinchSettleMs(left), easing = PinchEasing))
                                    }
                                } else {
                                    val target = pinchPreviewScale(cols, zoom)
                                    scope.launch { scale.snapTo(target) }
                                }
                                event.changes.forEach { it.consume() }
                            }
                        } while (event.changes.any { it.pressed })
                        if (pinching) {
                            // z/i OooOOO0 always hands the release to ai/n OooOOo0: spring back, never step
                            val elastic = nextColumnCount(cols, if (zoom >= 1f) 2f else 0f) == cols
                            val ms = if (elastic) ELASTIC_RETURN_MS else pinchSettleMs(pinchProgress(zoom))
                            scope.launch { scale.animateTo(1f, tween(ms, easing = PinchEasing)) }
                        }
                    }
                }
        ) {
            LazyVerticalGrid(
                state = gridState,
                columns = GridCells.Fixed(shownCount),
                contentPadding = PaddingValues(start = 13.sdp, end = 13.sdp, bottom = 50.sdp),
                horizontalArrangement = Arrangement.spacedBy(4.sdp),
                modifier = Modifier.fillMaxSize().graphicsLayer {
                    scaleX = scale.value
                    scaleY = scale.value
                    transformOrigin = origin
                }
            ) {
                items(
                    count = rows.size,
                    key = { i ->
                        when (val row = rows[i]) {
                            PhotoRow.Ad -> "ad"
                            is PhotoRow.Header -> "header_${row.dayStart}"
                            is PhotoRow.Media -> row.item.id
                        }
                    },
                    span = { i -> GridItemSpan(if (rows[i] is PhotoRow.Media) 1 else maxLineSpan) },
                    contentType = { i -> rows[i]::class }
                ) { i ->
                    // headers move with the cells so a column change reflows as one piece
                    Box(Modifier.animateItem(fadeInSpec = null, placementSpec = placement, fadeOutSpec = null)) {
                        when (val row = rows[i]) {
                            PhotoRow.Ad -> PhotosAdRow()
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
            FastScrollThumb(gridState, trackHeightPx = constraints.maxHeight)
        }
    }
}

/**
 * RecyclerViewFastScroller thumb on the end edge: follows the scroll, dragging jumps the grid.
 * Never auto-hides (handleVisibilityDuration -1) and shows no date bubble (2.7.2 never binds one).
 * The thumb ImageView has no background, so its 8.sdp elevation casts no shadow: none is drawn here.
 */
@Composable
private fun BoxScope.FastScrollThumb(state: LazyGridState, trackHeightPx: Int) {
    if (!state.canScrollForward && !state.canScrollBackward) return
    val painter = painterResource(R.drawable.ic_scroll)
    val range = (trackHeightPx - painter.intrinsicSize.height).coerceAtLeast(0f)
    val scrollFraction = {
        val info = state.layoutInfo
        thumbFraction(state.firstVisibleItemIndex, info.visibleItemsInfo.size, info.totalItemsCount)
    }
    var dragFraction by remember { mutableStateOf<Float?>(null) }
    val scope = rememberCoroutineScope()
    Image(
        painter = painter,
        contentDescription = null,
        modifier = Modifier
            .align(Alignment.TopEnd)
            .offset { IntOffset(0, ((dragFraction ?: scrollFraction()) * range).roundToInt()) }
            .pointerInput(state, range) {
                detectVerticalDragGestures(
                    onDragStart = { dragFraction = scrollFraction() },
                    onDragEnd = { dragFraction = null },
                    onDragCancel = { dragFraction = null }
                ) { change, dy ->
                    change.consume()
                    if (range > 0f) {
                        val f = ((dragFraction ?: scrollFraction()) + dy / range).coerceIn(0f, 1f)
                        dragFraction = f
                        val info = state.layoutInfo
                        scope.launch { state.scrollToItem(indexForThumbFraction(f, info.visibleItemsInfo.size, info.totalItemsCount)) }
                    }
                }
            }
    )
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
                    Image(painterResource(R.drawable.ic_common_fav), contentDescription = stringResource(R.string.favorite), modifier = Modifier.size(17.sdp))
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

/** Ad slot (photos_tab_item_ad): the native-ad loading skeleton, drawn static with no text or click (D5: ads stay placeholders). */
@Composable
private fun PhotosAdRow() {
    val edge = 13.sdp
    Column(
        // PD gives ad rows no edge inset: widen past the grid's 13.sdp side padding
        modifier = Modifier.layout { measurable, constraints ->
            val inset = edge.roundToPx()
            val width = constraints.maxWidth + 2 * inset
            val placeable = measurable.measure(constraints.copy(minWidth = width, maxWidth = width))
            layout(constraints.maxWidth, placeable.height) { placeable.place(-inset, 0) }
        }
    ) {
        Column(modifier = Modifier.fillMaxWidth().padding(start = 18.sdp, end = 18.sdp, top = 10.sdp, bottom = 8.sdp)) {
            Box(modifier = Modifier.fillMaxWidth().height(22.sdp)) {
                DrawableBox(R.drawable.photos_tab_shimmer_block, Modifier.size(22.sdp))
                // title bar: icon end + 5.sdp to CTA start - 25.sdp, top-aligned with the icon
                DrawableBox(
                    R.drawable.photos_tab_shimmer_block,
                    Modifier.padding(start = 27.sdp, end = 75.sdp).fillMaxWidth().height(8.sdp)
                )
                // subtitle bar: title start to CTA start - 7.sdp, bottom-aligned with the icon
                DrawableBox(
                    R.drawable.photos_tab_shimmer_block,
                    Modifier.align(Alignment.BottomStart).padding(start = 27.sdp, end = 57.sdp).fillMaxWidth().height(7.sdp)
                )
                DrawableBox(R.drawable.photos_tab_ad_cta_bg, Modifier.align(Alignment.TopEnd).size(width = 50.sdp, height = 22.sdp))
            }
            Spacer(modifier = Modifier.height(7.sdp))
            Box(modifier = Modifier.fillMaxWidth().height(132.sdp).background(colorResource(R.color.photos_tab_shimmer)))
        }
        HorizontalDivider(thickness = 1.dp, color = colorResource(R.color.photos_tab_divider))
    }
}

/** Draws an XML shape drawable, which painterResource cannot load. */
@Composable
private fun DrawableBox(@DrawableRes id: Int, modifier: Modifier) {
    val drawable = ContextCompat.getDrawable(LocalContext.current, id) ?: return
    Box(modifier = modifier.drawBehind {
        drawIntoCanvas { canvas ->
            drawable.setBounds(0, 0, size.width.toInt(), size.height.toInt())
            drawable.draw(canvas.nativeCanvas)
        }
    })
}

/** fragment_all_media emptyLayout */
@Composable
private fun PhotosEmptyState() {
    Column(
        modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        val titleColor = colorResource(R.color.titleColor)
        Image(painter = painterResource(R.drawable.ic_empty_img), contentDescription = null)
        Text(
            text = stringResource(R.string.no_items),
            style = GalleryText.BodyLargeSemiBold,
            color = titleColor,
            modifier = Modifier.padding(top = 20.sdp, bottom = 10.sdp)
        )
        Text(
            text = stringResource(R.string.no_media_file),
            style = GalleryText.BodySmallRegular,
            color = titleColor,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 50.sdp)
        )
    }
}
