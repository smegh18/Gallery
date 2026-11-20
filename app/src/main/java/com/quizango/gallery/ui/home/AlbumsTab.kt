package com.quizango.gallery.ui.home

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.request.ImageRequest
import coil.compose.AsyncImage
import com.quizango.gallery.R
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
import com.quizango.gallery.ui.theme.topContainerBg

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
    onAlbumClick: (Album, String) -> Unit,
    onStoryClick: (path: String, title: String) -> Unit = { _, _ -> }
) {
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
    val rows = androidx.compose.runtime.remember(albumsList, favoriteAlbum, deletedAlbum) {
        albumGrid(albumsList, favoriteAlbum, deletedAlbum).chunked(3)
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(colorResource(R.color.album_tab_bg))
    ) {
        // Stories strip (Reference: storyRecycler in fragment_album.xml)
        item {
            StoryQueue.stories = stories
            LazyRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 2.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(stories, key = { "story_${it.id}" }) { story ->
                    StoryCircleItem(
                        title = story.title,
                        imageModel = story.items.first().contentUri,
                        gradientColors = when (story.id) {
                            "recent" -> listOf(Color(0xFFFF3093), Color(0xFFFF5252), Color(0xFFFFC701), Color(0xFFFF3093))
                            "random" -> listOf(Color(0xFF0072FF), Color(0xFF00C6FF), Color(0xFF0072FF))
                            "favorite" -> listOf(Color(0xFFFF416C), Color(0xFFFF4B2B), Color(0xFFFF416C))
                            else -> listOf(Color(0xFF8E2DE2), Color(0xFF4A00E0), Color(0xFF8E2DE2))
                        },
                        onClick = { onStoryClick(story.id, story.title) }
                    )
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
                            HomeAlbumCard(album = album, title = shown, onClick = { onAlbumClick(album, listTitle) })
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

/** item_home_album_card. */
@Composable
private fun HomeAlbumCard(album: Album, title: String, onClick: () -> Unit) {
    Column(modifier = Modifier.fillMaxWidth().clickable(onClick = onClick)) {
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

@Composable
private fun StoryCircleItem(
    title: String,
    imageModel: Any?,
    placeholderRes: Int = 0,
    gradientColors: List<Color>,
    onClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .width(76.dp)
            .clickable(onClick = onClick)
            .padding(vertical = 2.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(66.dp)
                .clip(CircleShape)
                .background(Brush.sweepGradient(gradientColors))
                .padding(2.5.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.topContainerBg)
                .padding(2.dp)
                .clip(CircleShape),
            contentAlignment = Alignment.Center
        ) {
            if (imageModel != null) {
                AsyncImage(
                    model = imageModel,
                    contentDescription = title,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            } else if (placeholderRes != 0) {
                Image(
                    painter = painterResource(id = placeholderRes),
                    contentDescription = title,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                )
            }
        }

        Spacer(modifier = Modifier.height(4.dp))

        Text(
            text = title,
            fontSize = 11.5.sp,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onBackground,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center
        )
    }
}
