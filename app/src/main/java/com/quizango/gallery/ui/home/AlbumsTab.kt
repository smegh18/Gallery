package com.quizango.gallery.ui.home

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Folder
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.HorizontalDivider
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.quizango.gallery.R
import com.quizango.gallery.domain.model.Album
import com.quizango.gallery.domain.model.MediaItem
import com.quizango.gallery.domain.model.MediaType
import com.quizango.gallery.ui.theme.albumOptionBg
import com.quizango.gallery.ui.theme.mediaTypeCardBg
import com.quizango.gallery.ui.theme.cardBorder
import com.quizango.gallery.ui.theme.isDark
import com.quizango.gallery.ui.theme.topContainerBg
import java.io.File

data class TopFeatureItem(
    val id: String,
    val name: String,
    val iconRes: Int
)

val HOME_TOP_FEATURES = listOf(
    TopFeatureItem("vault", "Vault", R.drawable.ic_home_top_lock),
    TopFeatureItem("favorites", "Favorites", R.drawable.ic_home_top_favorite),
    TopFeatureItem("cleaner", "Cleaner", R.drawable.ic_home_top_cleaner),
    TopFeatureItem("duplicates", "Duplicate", R.drawable.ic_home_top_duplicate),
    TopFeatureItem("recycle", "Recycle Bin", R.drawable.ic_home_top_recyclebin),
    TopFeatureItem("collage", "Collage", R.drawable.ic_home_top_collage),
    TopFeatureItem("editor", "Editor", R.drawable.ic_home_top_editor),
    TopFeatureItem("videos", "Videos", R.drawable.ic_home_top_videos),
    TopFeatureItem("place", "Place", R.drawable.ic_home_top_map),
    TopFeatureItem("moments", "Moments", R.drawable.ic_home_top_moment)
)

data class MediaTypeItem(
    val id: String,
    val title: String,
    val iconRes: Int
)

val MEDIA_TYPES = listOf(
    MediaTypeItem("portrait", "Portrait", R.drawable.ic_image_portrait),
    MediaTypeItem("high_definition", "High Definition", R.drawable.ic_image_high_definition),
    MediaTypeItem("burst", "Burst", R.drawable.ic_image_burst),
    MediaTypeItem("animated", "Animated", R.drawable.ic_image_animated)
)

@Composable
fun AlbumsTab(
    albumsList: List<Album>,
    mediaList: List<MediaItem> = emptyList(),
    onAlbumClick: (Album) -> Unit,
    onStoryClick: (path: String, title: String) -> Unit = { _, _ -> },
    onFeatureClick: (String) -> Unit = {},
    onMediaTypeClick: (String) -> Unit = {}
) {
    val storiesData = androidx.compose.runtime.remember(mediaList, albumsList) {
        val nonVideoMedia = mediaList.filter { it.mediaType != MediaType.VIDEO }
        // Reference builds stories from photos only; no photos means no Recent/Random story.
        val recent = nonVideoMedia.firstOrNull()?.contentUri
        val random = nonVideoMedia.asSequence().shuffled().firstOrNull()?.contentUri
        val fav = nonVideoMedia.firstOrNull { it.isFavorite }?.contentUri
        val folders = nonVideoMedia.map { it.folderPath }.toSet()
        val folderNames = nonVideoMedia.map { it.folderName }.toSet()
        val albums = albumsList.filter { album ->
            album.coverPath.isNotEmpty() && (album.path in folders || album.name in folderNames)
        }
        data class StoriesData(val recent: Any?, val random: Any?, val fav: Any?, val albums: List<Album>)
        StoriesData(recent, random, fav, albums)
    }

    val chunkedAlbums = androidx.compose.runtime.remember(albumsList) { albumsList.chunked(3) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentPadding = PaddingValues(bottom = 24.dp)
    ) {
        // 1. Top Section (Features Carousel + Stories Strip with dynamic topContainerBg)
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.topContainerBg)
                    .padding(vertical = 4.dp)
            ) {
                // Features Carousel (Reference rvFeaturesLists)
                LazyRow(
                    modifier = Modifier.fillMaxWidth(),
                    contentPadding = PaddingValues(horizontal = 6.dp),
                    horizontalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    items(HOME_TOP_FEATURES) { feature ->
                        FeatureItemView(
                            feature = feature,
                            onClick = { onFeatureClick(feature.id) }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))

                // Stories Strip (Reference: llStoryView / storyRecycler in fragment_album.xml)
                LazyRow(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 2.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Story 1: Recent
                    if (storiesData.recent != null) {
                        item(key = "story_recent") {
                            StoryCircleItem(
                                title = "Recent",
                                imageModel = storiesData.recent,
                                gradientColors = listOf(Color(0xFFFF3093), Color(0xFFFF5252), Color(0xFFFFC701), Color(0xFFFF3093)),
                                onClick = { onStoryClick("recent", "Recent") }
                            )
                        }
                    }

                    // Story 2: Random
                    if (storiesData.random != null) {
                        item(key = "story_random") {
                            StoryCircleItem(
                                title = "Random",
                                imageModel = storiesData.random,
                                gradientColors = listOf(Color(0xFF0072FF), Color(0xFF00C6FF), Color(0xFF0072FF)),
                                onClick = { onStoryClick("random", "Random") }
                            )
                        }
                    }

                    // Story 3: Favorite (if available)
                    if (storiesData.fav != null) {
                        item(key = "story_fav") {
                            StoryCircleItem(
                                title = "Favorite",
                                imageModel = storiesData.fav,
                                gradientColors = listOf(Color(0xFFFF416C), Color(0xFFFF4B2B), Color(0xFFFF416C)),
                                onClick = { onStoryClick("favorite", "Favorites") }
                            )
                        }
                    }

                    // Dynamic Album Stories
                    items(storiesData.albums, key = { "album_story_${it.id}" }) { album ->
                        StoryCircleItem(
                            title = album.name,
                            imageModel = album.coverUri,
                            gradientColors = listOf(Color(0xFF8E2DE2), Color(0xFF4A00E0), Color(0xFF8E2DE2)),
                            onClick = { onStoryClick(album.id, album.name) }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))
            }
        }

        // Reference has no divider below the top container, only the colour change
        item {
            Spacer(modifier = Modifier.height(10.dp))
        }

        // 2. Albums Grid (3 columns)
        if (albumsList.isEmpty()) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center,
                        modifier = Modifier.padding(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Folder,
                            contentDescription = null,
                            modifier = Modifier.size(56.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "No Albums Found",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        } else {
            items(chunkedAlbums, key = { row -> row.firstOrNull()?.id ?: row.hashCode() }, contentType = { "album_row" }) { rowAlbums ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    for (i in 0 until 3) {
                        if (i < rowAlbums.size) {
                            val album = rowAlbums[i]
                            Box(modifier = Modifier.weight(1f)) {
                                AlbumCard(album = album, onClick = { onAlbumClick(album) })
                            }
                        } else {
                            Spacer(modifier = Modifier.weight(1f))
                        }
                    }
                }
            }
        }

        // 3. Media Types Header & List (Reference mediaTitle & media_type_bg)
        item {
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "Media Types",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground,
                fontSize = 16.sp,
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 4.dp)
            )
            Spacer(modifier = Modifier.height(6.dp))

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 10.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.mediaTypeCardBg)
                    .border(0.5.dp, MaterialTheme.colorScheme.cardBorder, RoundedCornerShape(12.dp))
            ) {
                MEDIA_TYPES.forEachIndexed { index, mediaType ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onMediaTypeClick(mediaType.id) }
                            .padding(horizontal = 14.dp, vertical = 13.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Reference draws each icon on a rounded tile (media_type_bg)
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(MaterialTheme.colorScheme.albumOptionBg),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                painter = painterResource(id = mediaType.iconRes),
                                contentDescription = mediaType.title,
                                tint = Color.Unspecified,
                                modifier = Modifier.size(28.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(14.dp))
                        Text(
                            text = mediaType.title,
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onBackground,
                            fontSize = 16.sp,
                            modifier = Modifier.weight(1f)
                        )
                        Icon(
                            painter = painterResource(id = R.drawable.ic_right_arrow),
                            contentDescription = null,
                            tint = if (MaterialTheme.colorScheme.isDark) Color.White else Color(0xFF8C9BAE),
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    if (index < MEDIA_TYPES.size - 1) {
                        HorizontalDivider(
                            color = MaterialTheme.colorScheme.outline,
                            thickness = 0.5.dp,
                            modifier = Modifier.padding(horizontal = 14.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun FeatureItemView(
    feature: TopFeatureItem,
    onClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .width(64.dp)
            .clickable(onClick = onClick)
            .padding(vertical = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            painter = painterResource(id = feature.iconRes),
            contentDescription = feature.name,
            tint = Color.Unspecified,
            // Pill vectors are 51x34dp; reference draws them at intrinsic size.
            modifier = Modifier.size(width = 51.dp, height = 34.dp)
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = feature.name,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Medium,
            fontSize = 10.5.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onBackground
        )
    }
}

@Composable
fun AlbumCard(
    album: Album,
    onClick: () -> Unit
) {
    val context = LocalContext.current

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
