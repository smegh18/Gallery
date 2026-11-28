package com.quizango.gallery.ui.search

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.view.WindowManager
import androidx.annotation.StringRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import com.quizango.gallery.R
import com.quizango.gallery.data.local.MomentScanner
import com.quizango.gallery.data.repository.MediaRepository
import com.quizango.gallery.domain.model.Album
import com.quizango.gallery.domain.model.MediaItem
import com.quizango.gallery.domain.model.MediaType
import com.quizango.gallery.ui.browse.AlbumSquareCard
import com.quizango.gallery.ui.discovery.groupPlaces
import com.quizango.gallery.ui.discovery.momentRoute
import com.quizango.gallery.ui.discovery.placeMediaRoute
import com.quizango.gallery.ui.home.PhotosEmptyState
import com.quizango.gallery.ui.home.drawableBackground
import com.quizango.gallery.ui.navigation.Screen
import com.quizango.gallery.ui.theme.GalleryText
import com.quizango.gallery.ui.theme.sdp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** The search preview shows three cards per section before View All (2.7.2 dg.g.getItemCount caps isShort at 3). */
internal const val SEARCH_PREVIEW_LIMIT = 3

/** Result sections in screen order; [type] is the reference event type (Albums 1, Places 2, Moments 3). */
internal enum class SearchSection(val type: Int, @StringRes val title: Int) {
    ALBUMS(1, R.string.albums),
    PLACES(2, R.string.places),
    MOMENTS(3, R.string.moment);

    companion object {
        /** Anything outside the three types (a hand-edited or stale route) is null, never a crash. */
        fun of(type: Int?): SearchSection? = entries.firstOrNull { it.type == type }
    }
}

/** One item_search_sub card. [route] is the media list it opens; the cover is resolved only when drawn. */
internal class SearchCard(
    val title: String,
    val count: Int,
    val route: String,
    private val album: Album? = null,
    private val item: MediaItem? = null
) {
    val cover: Any? get() = album?.coverUri ?: item?.contentUri
}

internal typealias SearchIndex = Map<SearchSection, List<SearchCard>>

/**
 * Every card of every section, unfiltered. Albums are real device folders only (no All Photos / Videos / Camera /
 * Screenshots); places and moments use the same grouping and filter keys as their browse screens.
 */
internal fun buildSearchIndex(albums: List<Album>, media: List<MediaItem>, labels: Map<String, String>): SearchIndex {
    val photos = media.filter { it.mediaType != MediaType.VIDEO }
    return mapOf(
        SearchSection.ALBUMS to albums.filter { !it.id.startsWith("filter:") }.map {
            SearchCard(it.name, it.itemCount, Screen.AlbumDetail.createRoute(it.id, it.name), album = it)
        },
        SearchSection.PLACES to groupPlaces(media).map {
            SearchCard(it.key, it.items.size, placeMediaRoute(it), item = it.items.first())
        },
        SearchSection.MOMENTS to MomentScanner.group(photos, labels).map { (label, items) ->
            SearchCard(label, items.size, momentRoute(label), item = items.first())
        }
    )
}

/** Cards of [section] whose name contains [query] (case-insensitive); a blank query keeps all. */
internal fun sectionCards(index: SearchIndex, section: SearchSection?, query: String): List<SearchCard> {
    val needle = query.trim()
    return index[section].orEmpty().filter { needle.isEmpty() || it.title.contains(needle, ignoreCase = true) }
}

/** The non-empty sections for [query], in Albums, Places, Moments order. */
internal fun searchResults(index: SearchIndex, query: String): List<Pair<SearchSection, List<SearchCard>>> =
    SearchSection.entries.map { it to sectionCards(index, it, query) }.filter { it.second.isNotEmpty() }

internal fun searchPreview(cards: List<SearchCard>): List<SearchCard> = cards.take(SEARCH_PREVIEW_LIMIT)

/**
 * The search index once media, albums and moment labels are in (null before, so "Oops!" never flashes first).
 * Starts or continues the on-device moment scan so Moments results do not depend on visiting Moments first.
 */
@Composable
internal fun rememberSearchIndex(mediaRepository: MediaRepository, moments: MomentScanner): SearchIndex? {
    val media by remember { mediaRepository.getActiveMediaFlow() }
        .collectAsState<List<MediaItem>, List<MediaItem>?>(initial = null)
    val albums by remember { mediaRepository.getAlbumsFlow() }
        .collectAsState<List<Album>, List<Album>?>(initial = null)
    val labels by moments.labels.collectAsState()
    LaunchedEffect(media) {
        media?.let { all -> moments.scan(all.filter { it.mediaType != MediaType.VIDEO }) }
    }
    val index by produceState<SearchIndex?>(null, media, albums, labels) {
        val m = media ?: return@produceState
        val a = albums ?: return@produceState
        value = withContext(Dispatchers.Default) { buildSearchIndex(a, m, labels) }
    }
    return index
}

/** SearchActivity (§11): Colors, then Albums / Places / Moments previews that filter as the user types. */
@Composable
fun SearchScreen(
    mediaRepository: MediaRepository,
    moments: MomentScanner,
    onNavigate: (String) -> Unit,
    onNavigateBack: () -> Unit
) {
    // Saved, so Back from View All returns to the same query
    var query by rememberSaveable { mutableStateOf("") }
    var picking by remember { mutableStateOf(false) }
    val index = rememberSearchIndex(mediaRepository, moments)
    val results = remember(index, query) { index?.let { searchResults(it, query) } }
    AdjustNothingWhileShown()

    Column(Modifier.fillMaxSize().background(colorResource(R.color.backgroundColor))) {
        SearchToolbar(query = query, onQueryChange = { query = it }, onBack = onNavigateBack)
        Box(Modifier.weight(1f).fillMaxWidth()) {
            LazyColumn(Modifier.fillMaxSize()) {
                item { ColorsRow(onPick = { picking = true }, onColor = { onNavigate(Screen.SearchColor.createRoute(it)) }) }
                results?.let { sections ->
                    if (sections.isEmpty()) item {
                        Box(Modifier.fillParentMaxHeight(0.7f).fillMaxWidth()) {
                            PhotosEmptyState(R.string.oops, R.string.search_deleted)
                        }
                    }
                    sections.forEach { (section, cards) ->
                        item(key = section.type) {
                            SectionBlock(
                                section = section,
                                cards = searchPreview(cards),
                                onViewAll = { onNavigate(Screen.SearchViewAll.createRoute(section.type, query)) },
                                onOpen = { onNavigate(it.route) }
                            )
                        }
                    }
                }
            }
            if (results == null) Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = colorResource(R.color.colorPrimary))
            }
        }
        Spacer(Modifier.navigationBarsPadding())
    }

    if (picking) SearchColorPickerDialog(
        onChosen = { hex ->
            picking = false
            onNavigate(Screen.SearchColor.createRoute(hex))
        },
        onDismiss = { picking = false }
    )
}

/** layout_search_toolbar: back, the `search_back` field with hint, clear or search icon, divider. Also the sticker picker's. */
@Composable
internal fun SearchToolbar(query: String, onQueryChange: (String) -> Unit, onBack: () -> Unit, hint: Int = R.string.search) {
    val keyboard = LocalSoftwareKeyboardController.current
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
                painter = painterResource(R.drawable.ic_back),
                contentDescription = stringResource(R.string.viewer_back),
                modifier = Modifier
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = ripple(bounded = false),
                        onClick = onBack
                    )
                    .padding(6.sdp)
            )
            Row(
                modifier = Modifier
                    .weight(1f)
                    .padding(start = 4.sdp, end = 10.sdp)
                    .drawableBackground(R.drawable.search_back),
                verticalAlignment = Alignment.CenterVertically
            ) {
                BasicTextField(
                    value = query,
                    onValueChange = onQueryChange,
                    singleLine = true,
                    textStyle = GalleryText.BodySmallMedium.copy(color = colorResource(R.color.black)),
                    cursorBrush = SolidColor(colorResource(R.color.colorPrimary)),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                    keyboardActions = KeyboardActions(onSearch = { keyboard?.hide() }),
                    // searchEdt: marginHorizontal 10 + paddingHorizontal 10, paddingVertical 7
                    modifier = Modifier.weight(1f).padding(horizontal = 20.sdp, vertical = 7.sdp),
                    decorationBox = { field ->
                        Box(contentAlignment = Alignment.CenterStart) {
                            if (query.isEmpty()) Text(
                                stringResource(hint),
                                style = GalleryText.BodySmallMedium,
                                color = colorResource(R.color.hintLanguageColor),
                                maxLines = 1
                            )
                            field()
                        }
                    }
                )
                if (query.isEmpty()) Image(
                    painter = painterResource(R.drawable.ic_search_common),
                    contentDescription = stringResource(R.string.search),
                    modifier = Modifier.padding(end = 10.sdp)
                ) else Image(
                    painter = painterResource(R.drawable.ic_close),
                    contentDescription = stringResource(R.string.cancel),
                    modifier = Modifier
                        .padding(end = 10.sdp)
                        .size(19.sdp)
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = ripple(bounded = false),
                            onClick = { onQueryChange("") }
                        )
                        .padding(1.sdp)
                )
            }
        }
        HorizontalDivider(thickness = 1.dp, color = colorResource(R.color.actionbarDividerColor))
    }
}

/** colorLay: the "Colors" header, the picker button and the ten swatches, then the divider. */
@Composable
private fun ColorsRow(onPick: () -> Unit, onColor: (String) -> Unit) {
    Column(Modifier.fillMaxWidth()) {
        Text(
            text = stringResource(R.string.colors),
            style = GalleryText.BodyLargeMedium,
            color = colorResource(R.color.black),
            modifier = Modifier.padding(top = 10.sdp, start = 5.sdp)
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(start = 4.sdp, end = 4.sdp, top = 5.sdp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Image(
                painter = painterResource(R.drawable.ic_search_color_picker),
                contentDescription = stringResource(R.string.choose_color),
                modifier = Modifier
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = ripple(bounded = false),
                        onClick = onPick
                    )
                    .padding(6.sdp)
            )
            SEARCH_SWATCH_HEX.forEach { hex ->
                Box(
                    Modifier
                        .padding(4.sdp)
                        .size(20.sdp)
                        .clip(CircleShape)
                        .semantics { contentDescription = hex }
                        .clickable { onColor(hex) }
                        // MaterialCardView: the fill sits inside the 1sdp stroke, so no colour bleeds past its edge
                        .border(1.sdp, colorResource(R.color.onlyWhite), CircleShape)
                        .padding(1.sdp)
                        .background(colorOfHex(hex), CircleShape)
                )
            }
        }
        SectionDivider()
    }
}

/** item_search_main: title with View All, up to three item_search_sub cards in 3 columns, divider. */
@Composable
private fun SectionBlock(section: SearchSection, cards: List<SearchCard>, onViewAll: () -> Unit, onOpen: (SearchCard) -> Unit) {
    Column(Modifier.fillMaxWidth()) {
        Row(Modifier.fillMaxWidth().padding(top = 10.sdp), verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = stringResource(section.title),
                style = GalleryText.BodyMediumSemiBold,
                color = colorResource(R.color.black),
                modifier = Modifier.weight(1f).padding(start = 5.sdp)
            )
            Text(
                text = stringResource(R.string.view_all),
                style = GalleryText.BodyMediumMedium,
                color = colorResource(R.color.colorPrimary),
                modifier = Modifier.padding(end = 10.sdp).clickable(onClick = onViewAll)
            )
        }
        Column(Modifier.fillMaxWidth().padding(top = 5.sdp, start = 2.sdp, end = 2.sdp)) {
            cards.chunked(3).forEach { row ->
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Start) {
                    row.forEach { card ->
                        SearchCardView(card, { onOpen(card) }, Modifier.weight(1f))
                    }
                    // Keep a short last row on the 3-column grid
                    repeat(3 - row.size) { Spacer(Modifier.weight(1f)) }
                }
            }
        }
        SectionDivider()
    }
}

/** item_search_sub (§11.3): margin 4.sdp around the square cover, title and count. */
@Composable
internal fun SearchCardView(card: SearchCard, onClick: () -> Unit, modifier: Modifier = Modifier) {
    AlbumSquareCard(
        title = card.title,
        count = card.count,
        cover = card.cover,
        titleTop = 4.sdp,
        onClick = onClick,
        modifier = modifier.padding(4.sdp)
    )
}

@Composable
private fun SectionDivider() {
    HorizontalDivider(
        thickness = 0.5.dp,
        color = colorResource(R.color.dividerColor),
        modifier = Modifier.padding(start = 8.sdp, end = 8.sdp, top = 10.sdp)
    )
}

private fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}

/** SearchActivity is `adjustNothing`: the keyboard overlays the page instead of resizing it. Restored on leaving. */
@Composable
private fun AdjustNothingWhileShown() {
    val window = LocalContext.current.findActivity()?.window ?: return
    DisposableEffect(window) {
        val previous = window.attributes.softInputMode
        window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_NOTHING)
        onDispose { window.setSoftInputMode(previous) }
    }
}
