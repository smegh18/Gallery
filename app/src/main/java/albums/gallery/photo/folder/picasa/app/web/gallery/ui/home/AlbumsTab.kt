package albums.gallery.photo.folder.picasa.app.web.gallery.ui.home

import android.content.Intent
import android.net.Uri
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
import albums.gallery.photo.folder.picasa.app.web.gallery.R
import albums.gallery.photo.folder.picasa.app.web.gallery.domain.model.Album
import albums.gallery.photo.folder.picasa.app.web.gallery.ui.components.NativeAdCard
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

private val TopContainerBg = Color(0xFF161718)

@Composable
fun AlbumsTab(
    albumsList: List<Album>,
    onAlbumClick: (Album) -> Unit,
    onFeatureClick: (String) -> Unit = {},
    onMediaTypeClick: (String) -> Unit = {}
) {
    val context = LocalContext.current

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0C0C0C)),
        contentPadding = PaddingValues(bottom = 24.dp)
    ) {
        // 1. Top Section (Features Carousel + Stories Strip with background #161718)
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(TopContainerBg)
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

                Spacer(modifier = Modifier.height(4.dp))

                // Stories Strip (Reference: llStoryView / storyRecycler in fragment_album.xml)
                val recentAlbum = albumsList.firstOrNull { it.coverPath.isNotEmpty() }
                val randomAlbum = albumsList.filter { it.coverPath.isNotEmpty() }.let {
                    if (it.size > 1) it[1] else recentAlbum
                }

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
                            showAdBadge = false,
                            onClick = { recentAlbum?.let(onAlbumClick) }
                        )
                    }

                    // Story 2: Random
                    item {
                        StoryCircleItem(
                            title = "Random",
                            imagePath = randomAlbum?.coverPath ?: "",
                            gradientColors = listOf(Color(0xFF0072FF), Color(0xFF00C6FF)),
                            showAdBadge = false,
                            onClick = { randomAlbum?.let(onAlbumClick) }
                        )
                    }

                    // Story 3: Sponsored Game Story (Subway Surfers with AD badge)
                    item {
                        StoryCircleItem(
                            title = "Subway Sur...",
                            imagePath = "",
                            placeholderRes = R.drawable.ic_story_playzen,
                            gradientColors = listOf(Color(0xFF00E5FF), Color(0xFF2979FF)),
                            showAdBadge = false,
                            onClick = {
                                val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store/apps"))
                                try { context.startActivity(intent) } catch (_: Exception) {}
                            }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))
            }
        }

        // Spacer before Albums Grid
        item {
            Spacer(modifier = Modifier.height(6.dp))
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
                            tint = Color(0xFF7A8389)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "No Albums Found",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = Color(0xFF7A8389)
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
                        .padding(horizontal = 10.dp, vertical = 5.dp),
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

        // Native Ad Banner (Reference: flAd in fragment_album.xml)
        item {
            Spacer(modifier = Modifier.height(6.dp))
            Box(modifier = Modifier.padding(horizontal = 10.dp)) {
                NativeAdCard(onAdClick = {
                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store/apps"))
                    try { context.startActivity(intent) } catch (_: Exception) {}
                })
            }
        }

        // 3. Media Types Header & List (Reference mediaTitle & media_type_bg)
        item {
            Spacer(modifier = Modifier.height(14.dp))
            Text(
                text = "Media Types",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                fontSize = 16.sp,
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
            )
            Spacer(modifier = Modifier.height(4.dp))

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 10.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(Color(0xFF1E1E1E))
            ) {
                MEDIA_TYPES.forEachIndexed { index, mediaType ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onMediaTypeClick(mediaType.id) }
                            .padding(horizontal = 14.dp, vertical = 12.dp),
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
                            color = Color.White,
                            fontSize = 14.sp,
                            modifier = Modifier.weight(1f)
                        )
                        Icon(
                            painter = painterResource(id = R.drawable.ic_right_arrow),
                            contentDescription = null,
                            tint = Color.Unspecified,
                            modifier = Modifier.size(16.dp)
                        )
                    }

                    if (index < MEDIA_TYPES.size - 1) {
                        HorizontalDivider(
                            color = Color(0xFF2A2A2A),
                            thickness = 1.dp,
                            modifier = Modifier.padding(horizontal = 12.dp)
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
            .width(72.dp)
            .clickable(onClick = onClick)
            .padding(vertical = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            painter = painterResource(id = feature.iconRes),
            contentDescription = feature.name,
            tint = Color.Unspecified,
            modifier = Modifier.size(36.dp)
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
            color = Color.White
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
                .clip(RoundedCornerShape(10.dp))
                .background(Color(0xFF1E1E1E))
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
            fontSize = 13.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            color = Color.White
        )

        Spacer(modifier = Modifier.height(2.dp))

        Text(
            text = "${album.itemCount}",
            style = MaterialTheme.typography.labelSmall,
            color = Color(0xFF7A8389),
            fontSize = 11.sp
        )
    }
}

@Composable
private fun StoryCircleItem(
    title: String,
    imagePath: String,
    placeholderRes: Int = 0,
    gradientColors: List<Color>,
    showAdBadge: Boolean = false,
    onClick: () -> Unit
) {
    val context = LocalContext.current

    Column(
        modifier = Modifier
            .width(82.dp)
            .clickable(onClick = onClick)
            .padding(vertical = 2.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(72.dp)
                .clip(CircleShape)
                .background(Brush.linearGradient(gradientColors))
                .padding(2.5.dp)
                .clip(CircleShape)
                .background(TopContainerBg)
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
                        .background(Color(0xFF242424))
                )
            }

            // Reference ad_bg overlay badge
            if (showAdBadge) {
                Image(
                    painter = painterResource(id = R.drawable.ad_bg),
                    contentDescription = "AD",
                    modifier = Modifier.fillMaxSize()
                )
            }
        }

        Spacer(modifier = Modifier.height(4.dp))

        Text(
            text = title,
            fontSize = 11.5.sp,
            fontWeight = FontWeight.Medium,
            color = Color.White,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center
        )
    }
}
