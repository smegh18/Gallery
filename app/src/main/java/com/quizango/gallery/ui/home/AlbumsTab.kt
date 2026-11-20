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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.quizango.gallery.R
import com.quizango.gallery.ui.discovery.StoryBuilder
import com.quizango.gallery.ui.discovery.StoryQueue
import com.quizango.gallery.domain.model.Album
import com.quizango.gallery.domain.model.MediaItem
import com.quizango.gallery.ui.navigation.Screen
import com.quizango.gallery.ui.theme.sdp
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
                            AlbumCard(album = album.copy(name = shown), onClick = { onAlbumClick(album, listTitle) })
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun AlbumCard(
    album: Album,
    onClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1f)
                .clip(RoundedCornerShape(12.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant)
        ) {
            if (album.coverPath.isNotEmpty()) {
                AsyncImage(
                    model = album.coverUri,
                    contentDescription = album.name,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
                if (album.isVideoCover) {
                    Icon(
                        imageVector = Icons.Filled.Videocam,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier
                            .align(Alignment.BottomStart)
                            .padding(10.dp)
                            .size(22.dp)
                    )
                }
            } else {
                Image(
                    painter = painterResource(id = R.drawable.image_loading_albums),
                    contentDescription = album.name,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        Text(
            text = album.name,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold,
            fontSize = 13.5.sp,
            maxLines = 2, // reference wraps long names
            overflow = TextOverflow.Ellipsis,
            color = MaterialTheme.colorScheme.onBackground
        )

        Spacer(modifier = Modifier.height(2.dp))

        Text(
            text = "${album.itemCount}",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontSize = 11.sp,
            fontWeight = FontWeight.Medium
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
