package com.quizango.gallery.ui.browse

import androidx.activity.compose.BackHandler
import androidx.annotation.StringRes
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.zIndex
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.onLongClick
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.quizango.gallery.GalleryApplication
import com.quizango.gallery.R
import com.quizango.gallery.domain.model.MediaItem
import com.quizango.gallery.domain.model.MediaType
import com.quizango.gallery.ui.home.FastScrollThumb
import com.quizango.gallery.ui.home.HeaderKind
import com.quizango.gallery.ui.home.PINCH_MORPH_MS
import com.quizango.gallery.ui.home.PhotoRow
import com.quizango.gallery.ui.home.PhotosEmptyState
import com.quizango.gallery.ui.home.PinchEasing
import com.quizango.gallery.ui.home.PinchTutorialOverlay
import com.quizango.gallery.ui.home.buildPhotoRows
import com.quizango.gallery.ui.home.clampColumnCount
import com.quizango.gallery.ui.home.fileTypeLabel
import com.quizango.gallery.ui.home.formatDuration
import com.quizango.gallery.ui.home.headerKind
import com.quizango.gallery.ui.home.pinchColumnSteps
import com.quizango.gallery.ui.theme.GalleryText
import com.quizango.gallery.ui.theme.Inter
import com.quizango.gallery.ui.theme.sdp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/** Rows of an MA list (S272/ka/d): day headers and media, no ad row. */
internal fun sharedMediaRows(media: List<MediaItem>): List<PhotoRow> = buildPhotoRows(media, withAd = false)

/** Lazy keys: media by file path (unique per row), headers by day. */
internal fun sharedRowKey(row: PhotoRow): Any = when (row) {
    PhotoRow.Ad -> "ad"
    is PhotoRow.Header -> "header_${row.dayStart}"
    is PhotoRow.Media -> row.item.filePath
}

/** Headers (and an ad row) take the whole line at any column count. */
internal fun sharedRowSpan(row: PhotoRow, maxLineSpan: Int): Int = if (row is PhotoRow.Media) 1 else maxLineSpan

// ka/d formats with DD_MMM_YYYY in Locale.ENGLISH
private val HEADER_FORMAT = SimpleDateFormat("dd MMM yyyy", Locale.ENGLISH)

/** item_date label: Today / Yesterday, otherwise dd MMM yyyy (ka/a). Null means a fixed string resource. */
internal fun sharedHeaderDate(dayStart: Long, now: Long): String? = when (headerKind(dayStart, now)) {
    HeaderKind.TODAY, HeaderKind.YESTERDAY -> null
    else -> synchronized(HEADER_FORMAT) { HEADER_FORMAT.format(Date(dayStart)) }
}

/** The paths from [anchor] to [current] inclusive in display order, both clamped to the list (DragMultiSelectHelper). */
internal fun rangePaths(paths: List<String>, anchor: Int, current: Int): Set<String> {
    if (paths.isEmpty()) return emptySet()
    val a = anchor.coerceIn(0, paths.lastIndex)
    val b = current.coerceIn(0, paths.lastIndex)
    return paths.subList(minOf(a, b), maxOf(a, b) + 1).toSet()
}

internal fun isAllSelected(selected: Set<String>, paths: List<String>): Boolean =
    paths.isNotEmpty() && selected.containsAll(paths)

/** Select all / Unselect all. */
internal fun toggleAll(selected: Set<String>, paths: List<String>): Set<String> =
    if (isAllSelected(selected, paths)) emptySet() else paths.toSet()

/** Drops selected paths no longer in the list; the same instance when nothing went away. */
internal fun pruneSelection(selected: Set<String>, present: Set<String>): Set<String> =
    if (present.containsAll(selected)) selected else selected.filterTo(LinkedHashSet()) { it in present }

// R272 anim/item_animation_fall_down via layoutAnimation: 150 ms per item, each starting 15 % later
private const val ENTRANCE_MS = 150
private const val ENTRANCE_STAGGER_MS = 22L

/**
 * The §3 list frame shared by Media list, Favorites, Animated and Search colour (2.7.2's one adapter `ka/d`):
 * layout_toolbar, a day-grouped grid on the shared `mediaColCount`, loading / empty states and the selection bar.
 * Hosts own the data and every action; [items] is null until the first query returns.
 */
@Composable
internal fun SharedMediaList(
    title: String,
    items: List<MediaItem>?,
    selected: Set<String>,
    onSelectedChange: (Set<String>) -> Unit,
    onBack: () -> Unit,
    onOpen: (MediaItem) -> Unit,
    actions: List<MediaAction>,
    onAction: (MediaAction) -> Unit,
    @StringRes emptyTitle: Int = R.string.no_items,
    @StringRes emptyBody: Int = R.string.no_media_file,
    showPinchTip: Boolean = false // pinchToZoomInStub: only activity_media_list has it
) {
    val prefs = (LocalContext.current.applicationContext as GalleryApplication).preferences
    val scope = rememberCoroutineScope()
    val savedCount by remember { prefs.mediaColCount }.collectAsState(initial = null)
    val tipSeen by remember { prefs.isPinchInOutToolTip }.collectAsState(initial = true)
    val selecting = selected.isNotEmpty()
    val currentSelected by rememberUpdatedState(selected)

    // Rows can vanish under a selection (unfavourite, delete, a sync): keep only what is still listed
    LaunchedEffect(items) {
        val present = items?.mapTo(HashSet()) { it.filePath } ?: return@LaunchedEffect
        val pruned = pruneSelection(currentSelected, present)
        if (pruned !== currentSelected) onSelectedChange(pruned)
    }
    BackHandler(enabled = selecting) { onSelectedChange(emptySet()) }

    Box(Modifier.fillMaxSize().background(colorResource(R.color.backgroundColor))) {
        Column(Modifier.fillMaxSize()) {
            val paths = remember(items) { items?.map { it.filePath }.orEmpty() }
            SharedMediaToolbar(
                title = if (selecting) stringResource(R.string.items_selected, selected.size) else title,
                selecting = selecting,
                allSelected = isAllSelected(selected, paths),
                onBack = onBack,
                onClose = { onSelectedChange(emptySet()) },
                onToggleAll = { onSelectedChange(toggleAll(selected, paths)) }
            )
            Box(Modifier.weight(1f).fillMaxWidth()) {
                val count = savedCount
                when {
                    items == null || count == null -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = colorResource(R.color.colorPrimary))
                    }
                    items.isEmpty() -> PhotosEmptyState(emptyTitle, emptyBody)
                    else -> SharedMediaGrid(
                        items = items,
                        columnCount = count,
                        selected = selected,
                        onSelectedChange = onSelectedChange,
                        onOpen = onOpen,
                        onColumnCountChange = { scope.launch { prefs.setMediaColCount(clampColumnCount(it)) } }
                    )
                }
            }
            if (selecting) MediaSelectionBar(actions, onAction)
            Spacer(Modifier.navigationBarsPadding())
        }
        if (showPinchTip && !tipSeen && !items.isNullOrEmpty()) {
            PinchTutorialOverlay(onGotIt = { scope.launch { prefs.setPinchInOutToolTip(true) } })
        }
    }
}

/** layout_toolbar: back + title, or close + count + Select all / Unselect all while selecting. */
@Composable
private fun SharedMediaToolbar(
    title: String,
    selecting: Boolean,
    allSelected: Boolean,
    onBack: () -> Unit,
    onClose: () -> Unit,
    onToggleAll: () -> Unit
) {
    Column(
        Modifier
            .fillMaxWidth()
            .zIndex(1f)
            .shadow(2.sdp)
            .background(colorResource(R.color.backgroundColor))
            .statusBarsPadding()
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().height(55.dp).padding(start = 4.sdp, end = 10.sdp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Image(
                painter = painterResource(if (selecting) R.drawable.ic_close else R.drawable.ic_back),
                contentDescription = stringResource(if (selecting) R.string.cancel else R.string.viewer_back),
                modifier = Modifier
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = ripple(bounded = false),
                        onClick = if (selecting) onClose else onBack
                    )
                    .padding(6.sdp) // ic_back is autoMirrored
            )
            Text(
                text = title,
                style = GalleryText.BodyLargeSemiBold,
                color = colorResource(R.color.titleColor),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f).padding(start = 4.sdp)
            )
            if (selecting) Text(
                text = stringResource(if (allSelected) R.string.unselect_all else R.string.select_all),
                style = GalleryText.BodyMediumMedium,
                color = colorResource(R.color.colorPrimary),
                modifier = Modifier.clickable(onClick = onToggleAll).padding(5.sdp)
            )
        }
        // toolbarDevider: 1.dp of the 56.dp ?actionBarSize
        HorizontalDivider(thickness = 1.dp, color = colorResource(R.color.actionbarDividerColor))
    }
}

@Composable
private fun SharedMediaGrid(
    items: List<MediaItem>,
    columnCount: Int,
    selected: Set<String>,
    onSelectedChange: (Set<String>) -> Unit,
    onOpen: (MediaItem) -> Unit,
    onColumnCountChange: (Int) -> Unit
) {
    // Grouping thousands of items off the main thread keeps the first frame fast
    val rows by produceState<List<PhotoRow>?>(null, items) {
        value = withContext(Dispatchers.Default) { sharedMediaRows(items) }
    }
    val shownRows = rows ?: return
    // Display order, which day grouping can differ from query order: range selection follows what the user sees
    val media = remember(shownRows) { shownRows.filterIsInstance<PhotoRow.Media>().map { it.item } }
    val order = remember(media) { media.withIndex().associate { it.value.filePath to it.index } }
    val now = remember(items) { System.currentTimeMillis() }
    val gridState = rememberLazyGridState()
    // Shown at once on a pinch step; the saved value catches up through the preference flow
    var shownCount by remember { mutableIntStateOf(columnCount) }
    LaunchedEffect(columnCount) { shownCount = columnCount }
    val count by rememberUpdatedState(shownCount)
    val onCount by rememberUpdatedState(onColumnCountChange)
    val currentSelected by rememberUpdatedState(selected)
    val currentChange by rememberUpdatedState(onSelectedChange)
    val currentOpen by rememberUpdatedState(onOpen)
    val currentMedia by rememberUpdatedState(media)
    val currentOrder by rememberUpdatedState(order)
    val padding = PaddingValues(horizontal = 2.sdp, vertical = 4.sdp)
    val layoutDirection = LocalLayoutDirection.current
    val placement = tween<IntOffset>(PINCH_MORPH_MS, easing = PinchEasing)
    // layoutAnimation runs on the first fill only
    var entered by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        delay(1500)
        entered = true
    }

    fun tap(path: String) {
        val sel = currentSelected
        if (sel.isEmpty()) currentMedia.getOrNull(currentOrder[path] ?: -1)?.let(currentOpen)
        else currentChange(if (path in sel) sel - path else sel + path)
    }

    BoxWithConstraints(
        Modifier
            .fillMaxSize()
            .pinchColumnSteps({ count }) { next ->
                shownCount = next
                onCount(next)
            }
            // Outer: a tap only fires when the long-press drag below did not consume the up
            .pointerInput(gridState) {
                detectTapGestures(onTap = { pos -> gridState.mediaPathAt(pos, padding, layoutDirection, this)?.let(::tap) })
            }
            .pointerInput(gridState) {
                var anchor = -1
                var base = emptySet<String>()
                detectDragGesturesAfterLongPress(
                    onDragStart = { pos ->
                        val path = gridState.mediaPathAt(pos, padding, layoutDirection, this) ?: return@detectDragGesturesAfterLongPress
                        anchor = currentOrder[path] ?: -1
                        base = currentSelected
                        currentChange(base + path)
                    },
                    onDragEnd = { anchor = -1 },
                    onDragCancel = { anchor = -1 },
                    onDrag = { change, _ ->
                        if (anchor < 0) return@detectDragGesturesAfterLongPress
                        // shortcut: no auto-scroll at the edges while drag-selecting; add it if long ranges are needed
                        val path = gridState.mediaPathAt(change.position, padding, layoutDirection, this)
                        val index = path?.let { currentOrder[it] } ?: return@detectDragGesturesAfterLongPress
                        currentChange(base + rangePaths(currentMedia.map { it.filePath }, anchor, index))
                    }
                )
            }
    ) {
        LazyVerticalGrid(
            state = gridState,
            columns = GridCells.Fixed(shownCount),
            contentPadding = padding,
            modifier = Modifier.fillMaxSize()
        ) {
            items(
                count = shownRows.size,
                key = { sharedRowKey(shownRows[it]) },
                span = { GridItemSpan(sharedRowSpan(shownRows[it], maxLineSpan)) },
                contentType = { shownRows[it]::class }
            ) { i ->
                val entrance = if (entered) Modifier else Modifier.fallDown(i)
                Box(entrance.animateItem(fadeInSpec = null, placementSpec = placement, fadeOutSpec = null)) {
                    when (val row = shownRows[i]) {
                        PhotoRow.Ad -> Unit
                        is PhotoRow.Header -> Text(
                            text = sharedHeaderDate(row.dayStart, now) ?: stringResource(
                                if (headerKind(row.dayStart, now) == HeaderKind.TODAY) R.string.today else R.string.yesterday
                            ),
                            style = GalleryText.BodySmallMedium,
                            color = colorResource(R.color.black),
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 5.sdp, vertical = 6.sdp)
                        )
                        is PhotoRow.Media -> {
                            val path = row.item.filePath
                            SharedMediaCell(
                                item = row.item,
                                isSelected = path in selected,
                                selecting = selected.isNotEmpty(),
                                onClick = { tap(path) },
                                onLongClick = { currentChange(currentSelected + path) }
                            )
                        }
                    }
                }
            }
        }
        FastScrollThumb(gridState, trackHeightPx = constraints.maxHeight)
    }
}

/** The media path under [pos] (grid coordinates), or null over a header, padding or empty space. */
private fun LazyGridState.mediaPathAt(
    pos: Offset,
    padding: PaddingValues,
    layoutDirection: LayoutDirection,
    density: androidx.compose.ui.unit.Density
): String? {
    // Item offsets exclude the content padding (LazyGrid places them at offset + (start, top) padding) and, in RTL,
    // count from the end edge (LazyGridMeasuredItem.position mirrors them before placeRelative)
    val fromStart = if (layoutDirection == LayoutDirection.Rtl) layoutInfo.viewportSize.width - pos.x else pos.x
    val x = fromStart - with(density) { padding.calculateStartPadding(layoutDirection).toPx() }
    val y = pos.y - with(density) { padding.calculateTopPadding().toPx() }
    return layoutInfo.visibleItemsInfo.firstOrNull {
        x >= it.offset.x && x < it.offset.x + it.size.width && y >= it.offset.y && y < it.offset.y + it.size.height
    }?.key as? String
}

/** item_animation_fall_down: fade in, scale 105 % to 100 % and drop in, staggered by [order]. */
@Composable
private fun Modifier.fallDown(order: Int): Modifier {
    val progress = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        delay(order * ENTRANCE_STAGGER_MS)
        progress.animateTo(1f, tween(ENTRANCE_MS))
    }
    return graphicsLayer {
        val p = progress.value
        alpha = p
        scaleX = 1.05f - 0.05f * p
        scaleY = scaleX
        translationY = -0.2f * size.height * (1f - p)
    }
}

/** item_grid_gallery as bound by ka/c. Taps come from the grid's gesture handler; semantics expose them to TalkBack. */
@Composable
private fun SharedMediaCell(
    item: MediaItem,
    isSelected: Boolean,
    selecting: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit
) {
    Box(
        Modifier
            .padding(0.5.dp)
            .fillMaxWidth()
            .aspectRatio(1f)
            .semantics(mergeDescendants = true) {
                onClick { onClick(); true }
                onLongClick { onLongClick(); true }
                if (selecting) selected = isSelected
            }
    ) {
        AsyncImage(
            model = item.contentUri,
            contentDescription = item.fileName,
            placeholder = painterResource(R.drawable.image_loading_albums),
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
        )
        val white = colorResource(R.color.onlyWhite)
        val isVideo = item.mediaType == MediaType.VIDEO
        if (isVideo) Row(
            modifier = Modifier.align(Alignment.BottomStart).padding(start = 3.sdp, end = 3.sdp, bottom = 3.sdp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Image(painterResource(R.drawable.ic_media_video), contentDescription = null)
            Text(
                text = if (item.durationMs > 0) formatDuration(item.durationMs) else "00:00",
                style = GalleryText.BodyXSmallMediumGrid.copy(shadow = Shadow(Color.Black, Offset(2f, 2f), 4f)),
                color = white,
                maxLines = 1,
                modifier = Modifier.padding(horizontal = 3.sdp)
            )
        }
        fileTypeLabel(item.fileName, isVideo)?.let { label ->
            Text(
                text = stringResource(label),
                style = TextStyle(fontFamily = Inter, fontWeight = FontWeight.W500, fontSize = 12.sp, shadow = Shadow(Color.Black, blurRadius = 4f)),
                color = white,
                modifier = Modifier.align(Alignment.TopStart).padding(6.sdp)
            )
        }
        if (item.isFavorite) Image(
            painterResource(R.drawable.ic_common_fav),
            contentDescription = stringResource(R.string.favorite),
            modifier = Modifier.align(Alignment.BottomEnd).padding(1.sdp)
        )
        if (selecting) Image(
            painterResource(if (isSelected) R.drawable.ic_check else R.drawable.ic_un_check),
            contentDescription = null,
            modifier = Modifier.align(Alignment.TopEnd).padding(1.sdp)
        )
    }
}
