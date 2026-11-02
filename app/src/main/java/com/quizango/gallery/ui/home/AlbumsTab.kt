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
import com.quizango.gallery.ui.theme.albumOptionBg
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
    onAlbumClick: (Album) -> Unit,
    onFeatureClick: (String) -> Unit = {},
    onMediaTypeClick: (String) -> Unit = {}
) {
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
                val recentAlbum = albumsList.firstOrNull { it.coverPath.isNotEmpty() }
                val randomAlbum = albumsList.filter { it.coverPath.isNotEmpty() }.let {
                    if (it.size > 1) it[1] else recentAlbum
                }
                val otherAlbumsWithCover = albumsList.filter { it.coverPath.isNotEmpty() && it != recentAlbum && it != randomAlbum }

                LazyRow(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 2.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Story 1: Recent
                    item {
                        StoryCircleItem(
                            title = "Recent",
                            imagePath = recentAlbum?.coverPath ?: "",
                            gradientColors = listOf(Color(0xFFFF3093), Color(0xFFFF5252), Color(0xFFFFC701)),
                            onClick = { recentAlbum?.let(onAlbumClick) }
                        )
                    }

                    // Story 2: Random
                    item {
                        StoryCircleItem(
                            title = "Random",
                            imagePath = randomAlbum?.coverPath ?: "",
                            gradientColors = listOf(Color(0xFF0072FF), Color(0xFF00C6FF)),
                            onClick = { randomAlbum?.let(onAlbumClick) }
                        )
                    }

                    // Additional dynamic stories if available
                    items(otherAlbumsWithCover) { album ->
                        StoryCircleItem(
                            title = album.name,
                            imagePath = album.coverPath,
                            gradientColors = listOf(Color(0xFF8E2DE2), Color(0xFF4A00E0)),
                            onClick = { onAlbumClick(album) }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))
            }
        }

        // Divider below top container
        item {
            HorizontalDivider(
                color = MaterialTheme.colorScheme.outline,
                thickness = 0.5.dp,
                modifier = Modifier.fillMaxWidth()
            )
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
            val chunkedAlbums = albumsList.chunked(3)
            items(chunkedAlbums) { rowAlbums ->
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
                    .padding(horizontal = 12.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.albumOptionBg)
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
                        Icon(
                            painter = painterResource(id = mediaType.iconRes),
                            contentDescription = mediaType.title,
                            tint = Color.Unspecified,
                            modifier = Modifier.size(28.dp)
                        )
                        Spacer(modifier = Modifier.width(14.dp))
                        Text(
                            text = mediaType.title,
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onBackground,
                            fontSize = 14.sp,
                            modifier = Modifier.weight(1f)
                        )
                        Icon(
                            painter = painterResource(id = R.drawable.ic_right_arrow),
                            contentDescription = null,
                            tint = if (MaterialTheme.colorScheme.isDark) Color.White else Color(0xFF8C9BAE),
                            modifier = Modifier.size(14.dp)
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
            .width(66.dp)
            .clickable(onClick = onClick)
            .padding(vertical = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            painter = painterResource(id = feature.iconRes),
            contentDescription = feature.name,
            tint = Color.Unspecified,
            modifier = Modifier.size(38.dp)
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
                    model = ImageRequest.Builder(context)
                        .data(File(album.coverPath))
                        .crossfade(true)
                        .build(),
                    contentDescription = album.name,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
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
            maxLines = 1,
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
    imagePath: String,
    placeholderRes: Int = 0,
    gradientColors: List<Color>,
    onClick: () -> Unit
) {
    val context = LocalContext.current

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
                .background(MaterialTheme.colorScheme.background)
                .padding(2.dp)
                .clip(CircleShape),
            contentAlignment = Alignment.Center
        ) {
            if (imagePath.isNotEmpty()) {
                AsyncImage(
                    model = ImageRequest.Builder(context)
                        .data(File(imagePath))
                        .crossfade(true)
                        .build(),
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
