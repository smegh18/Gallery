package com.quizango.gallery.ui.home

import androidx.compose.foundation.Image
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.annotation.DrawableRes
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import android.widget.Toast
import androidx.annotation.StringRes
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import coil.request.ImageRequest
import coil.compose.AsyncImage
import com.quizango.gallery.R
import com.quizango.gallery.ui.discovery.Story
import com.quizango.gallery.ui.discovery.StoryBuilder
import com.quizango.gallery.ui.discovery.StoryQueue
import com.quizango.gallery.domain.model.Album
import com.quizango.gallery.domain.model.MediaItem
import com.quizango.gallery.ui.navigation.Screen
import com.quizango.gallery.ui.theme.Inter
import com.quizango.gallery.ui.theme.sdp
import com.quizango.gallery.ui.theme.ssp
import java.text.NumberFormat
import java.util.Locale

internal const val ALBUM_FAVORITES = "filter:favorites" // the id getMediaByFolderFlow already knows
internal const val ALBUM_DELETED = "filter:deleted"

/** Favorite card (2.7.2 type 8): always shown, count 0 and card colour only when empty. */
internal fun favoritesCard(items: List<MediaItem>): Album {
    val cover = items.firstOrNull()
    return Album(
        id = ALBUM_FAVORITES, name = "", path = ALBUM_FAVORITES,
        coverPath = cover?.filePath ?: "", itemCount = items.size,
        coverId = cover?.id ?: 0L, isVideoCover = cover?.isVideo == true,
        titleRes = R.string.favorite
    )
}

/** Deleted card (2.7.2 type -2001): newest Recycle Bin item as cover. */
internal fun deletedCard(items: List<MediaItem>): Album {
    val cover = items.maxByOrNull { it.deletedDate }
    return Album(
        id = ALBUM_DELETED, name = "", path = ALBUM_DELETED,
        coverPath = cover?.filePath ?: "", itemCount = items.size,
        coverId = cover?.id ?: 0L, isVideoCover = cover?.isVideo == true,
        titleRes = R.string.album_tab_deleted
    )
}

/** 2.7.2 grid order (AA.OooO0oo): Recent, Favorite, All Video, Camera, Screenshots (when not empty), folders, Deleted. */
internal fun albumGrid(albums: List<Album>, favorite: Album, deleted: Album): List<Album> {
    fun virtual(id: String) = albums.firstOrNull { it.id == id }
    return listOfNotNull(virtual("filter:all_photos"), favorite) +
        listOf("filter:videos", "filter:camera", "filter:screenshots").mapNotNull { id -> virtual(id)?.takeIf { it.itemCount > 0 } } +
        albums.filter { !it.id.startsWith("filter:") } +
        deleted
}

/** Title string of a card; null for device folders, which show their folder name. */
internal fun albumCardTitle(album: Album): Int? = when (album.id) {
    "filter:all_photos" -> R.string.recent
    ALBUM_FAVORITES -> R.string.favorite
    "filter:videos" -> R.string.album_tab_all_video
    "filter:camera" -> R.string.camera
    "filter:screenshots" -> R.string.screenshot
    ALBUM_DELETED -> R.string.album_tab_deleted
    else -> null
}

/** Favorite and Deleted open their own screens; every other card opens the album media list. */
internal fun albumRoute(album: Album, listTitle: String): String = when (album.id) {
    ALBUM_FAVORITES -> Screen.Favorites.route
    ALBUM_DELETED -> Screen.RecycleBin.route
    else -> Screen.AlbumDetail.createRoute(album.id, listTitle)
}

/** Only device folders can be selected; the virtual cards (Recent, Favorite, All Video, ..., Deleted) all use "filter:" ids. */
internal fun albumIsSelectable(album: Album): Boolean = !album.id.startsWith("filter:")

/** Pin button label: "Unpin" only when the single selected album is pinned (AF.OooOo0O pinType 1). */
@StringRes
internal fun pinLabel(selected: Set<String>, pinned: Set<String>): Int =
    if (selected.size == 1 && selected.first() in pinned) R.string.unpin_to_top else R.string.pin_to_top

/** Pin, Change cover and Rename are dimmed once more than one album is selected (HA.handleEvents). */
internal fun singleAlbumActionAlpha(count: Int): Float = if (count > 1) 0.6f else 1f

/** List title of an album: the reference string for a virtual album, else the folder name. */
@Composable
internal fun albumTitle(album: Album): String =
    if (album.titleRes != 0) stringResource(album.titleRes) else album.name

@Composable
fun AlbumsTab(
    albumsList: List<Album>,
    favoriteAlbum: Album,
    deletedAlbum: Album,
    isLoading: Boolean,
    mediaList: List<MediaItem> = emptyList(),
    selectedAlbums: Set<String> = emptySet(),
    onAlbumClick: (Album, String) -> Unit,
    onAlbumToggle: (Album) -> Unit = {},
    onStoryClick: (id: String) -> Unit = {}
) {
    val context = LocalContext.current
    val selecting = selectedAlbums.isNotEmpty()
    fun toggleOrRefuse(album: Album) =
        if (albumIsSelectable(album)) onAlbumToggle(album)
        else Toast.makeText(context, R.string.cannot_select_system_folders, Toast.LENGTH_SHORT).show()
    if (isLoading) {
        // fragment_album: only a centred progress bar until the first sync has returned
        Box(
            modifier = Modifier.fillMaxSize().background(colorResource(R.color.album_tab_bg)),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator(color = colorResource(R.color.colorPrimary))
        }
        return
    }
    // Reference story rules (HomeViewModel): see StoryBuilder
    val stories = androidx.compose.runtime.remember(mediaList) { StoryBuilder.build(mediaList) }
    val row = androidx.compose.runtime.remember(stories) { storyRow(stories) }
    val rows = androidx.compose.runtime.remember(albumsList, favoriteAlbum, deletedAlbum) {
        albumGrid(albumsList, favoriteAlbum, deletedAlbum).chunked(3)
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(colorResource(R.color.album_tab_bg))
    ) {
        // Stories strip (storyRecycler): padding 7/9/7/10.sdp, no item spacing, no background
        item {
            StoryQueue.stories = stories // without the ad circle, so the viewer never plays it
            LazyRow(contentPadding = PaddingValues(start = 7.sdp, top = 9.sdp, end = 7.sdp, bottom = 10.sdp)) {
                items(row, key = { it?.id ?: "playzen" }) { story ->
                    StoryItem(story, onClick = { story?.let { onStoryClick(it.id) } })
                }
            }
            Spacer(Modifier.height(7.sdp)) // albumRecycler marginTop
        }

        // Album grid (3 columns): list padding 9.sdp, cards 4.sdp side and 20.sdp bottom margins
        items(rows, key = { row -> row.first().id }, contentType = { "album_row" }) { rowAlbums ->
            Row(Modifier.fillMaxWidth().padding(horizontal = 9.sdp)) {
                for (i in 0 until 3) {
                    val album = rowAlbums.getOrNull(i)
                    Box(Modifier.weight(1f).padding(start = 4.sdp, end = 4.sdp, bottom = 20.sdp)) {
                        if (album != null) {
                            val shown = albumCardTitle(album)?.let { stringResource(it) } ?: album.name
                            val listTitle = albumTitle(album)
                            HomeAlbumCard(
                                album = album,
                                title = shown,
                                selectionMark = if (selecting) album.path in selectedAlbums else null,
                                onClick = { if (selecting) toggleOrRefuse(album) else onAlbumClick(album, listTitle) },
                                onLongClick = { toggleOrRefuse(album) }
                            )
                        }
                    }
                }
            }
        }
    }
}

/** Corner badge of a card (bx/c.eyd3OXAZgV): Favorite, All Video, a pinned device folder; none otherwise. */
@DrawableRes
internal fun albumBadge(album: Album): Int? = when {
    album.id == ALBUM_FAVORITES -> R.drawable.album_tab_ic_favorite
    album.id == "filter:videos" -> R.drawable.album_tab_ic_video
    album.isPinned && !album.id.startsWith("filter:") -> R.drawable.ic_pin
    else -> null
}

/** AA.countFormat: the bare number with locale grouping, no unit word. */
internal fun albumCountText(count: Int, locale: Locale = Locale.getDefault()): String =
    NumberFormat.getIntegerInstance(locale).format(count)

/** item_home_album_card. [selectionMark] is null outside album selection, else whether this card is ticked. */
@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun HomeAlbumCard(album: Album, title: String, selectionMark: Boolean?, onClick: () -> Unit, onLongClick: () -> Unit) {
    Column(modifier = Modifier.fillMaxWidth().combinedClickable(onClick = onClick, onLongClick = onLongClick)) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1f)
                .clip(RoundedCornerShape(10.sdp))
                .background(colorResource(R.color.album_tab_card_placeholder))
        ) {
            // An album with no media shows only the card colour
            if (album.itemCount > 0 && album.coverPath.isNotEmpty()) {
                AsyncImage(
                    model = ImageRequest.Builder(LocalContext.current).data(album.coverUri).size(200, 200).build(),
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    placeholder = painterResource(R.drawable.image_loading_albums),
                    error = painterResource(R.drawable.image_loading_albums),
                    modifier = Modifier.fillMaxSize()
                )
            }
            if (album.id == ALBUM_DELETED) {
                Box(Modifier.fillMaxSize().background(colorResource(R.color.album_tab_deleted_dim)))
                Image(
                    painter = painterResource(R.drawable.album_tab_ic_trash),
                    contentDescription = null,
                    modifier = Modifier.align(Alignment.Center).size(width = 17.sdp, height = 22.sdp)
                )
            }
            albumBadge(album)?.let {
                Image(
                    painter = painterResource(it),
                    contentDescription = null,
                    modifier = Modifier.align(Alignment.BottomStart).padding(start = 6.sdp, bottom = 6.sdp).size(12.sdp)
                )
            }
            if (selectionMark != null) { // bx/c.DxDJysLV5r: top-end, 18.sdp, margin 5.sdp
                Image(
                    painter = painterResource(if (selectionMark) R.drawable.ic_check else R.drawable.ic_un_check),
                    contentDescription = null,
                    modifier = Modifier.align(Alignment.TopEnd).padding(5.sdp).size(18.sdp)
                )
            }
        }
        Text(
            text = title,
            fontFamily = Inter,
            fontWeight = FontWeight.Medium,
            fontSize = 9.ssp,
            lineHeight = 11.ssp, // natural line box (includeFontPadding=false in the reference)
            color = colorResource(R.color.album_tab_name),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(top = 6.sdp)
        )
        Text(
            text = albumCountText(album.itemCount),
            fontFamily = Inter,
            fontWeight = FontWeight.Normal,
            fontSize = 9.ssp,
            lineHeight = 11.ssp,
            color = colorResource(R.color.album_tab_count),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(top = 2.sdp)
        )
    }
}

/** The story row: the Playzen ad circle goes in at index 2 (clamped), only into a non-empty row (AF.OooOo). */
internal fun storyRow(stories: List<Story>): List<Story?> {
    if (stories.isEmpty()) return emptyList()
    return ArrayList<Story?>(stories).apply { add(minOf(2, size), null) }
}

/** item_stories: [story] null is the Playzen ad circle. */
@Composable
private fun StoryItem(story: Story?, onClick: () -> Unit) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val scale by animateFloatAsState(if (pressed) 0.9f else 1f, label = "storyRingPress") // InsLoadingView.onTouchEvent
    val ring = colorResource(R.color.album_tab_story_ring)
    Column(
        modifier = Modifier.width(64.sdp).clickable(interactionSource = interaction, indication = null, onClick = onClick),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(Modifier.size(61.sdp)) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer { scaleX = scale; scaleY = scale }
                    .drawBehind {
                        // InsLoadingView "unclicked": one solid ring, diameter 0.9, stroke 0.025 of the view
                        drawCircle(ring, radius = size.minDimension * 0.45f, style = Stroke(width = size.minDimension * 0.025f))
                    },
                contentAlignment = Alignment.Center
            ) {
                Box(Modifier.fillMaxSize(0.865f).clip(CircleShape)) {
                    if (story != null) {
                        AsyncImage(
                            model = story.items.first().contentUri,
                            contentDescription = null,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                    } else {
                        Image(
                            painter = painterResource(R.drawable.ic_story_playzen),
                            contentDescription = null,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                }
            }
            if (story == null) {
                Image(
                    painter = painterResource(R.drawable.ad_bg),
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize().padding(8.sdp)
                )
            }
        }
        Text(
            text = stringResource(story?.titleRes ?: R.string.playzen),
            fontFamily = Inter,
            fontWeight = FontWeight.Normal,
            fontSize = 8.ssp,
            lineHeight = 10.ssp,
            color = colorResource(R.color.album_tab_story_label),
            textAlign = TextAlign.Center,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.fillMaxWidth().padding(horizontal = 2.sdp).padding(top = 1.sdp)
        )
    }
}
