package albums.gallery.photo.folder.picasa.app.web.gallery.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
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
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 24.dp)
    ) {
        // 1. Top Features Carousel (Reference rvFeaturesLists)
        item {
            LazyRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surface)
                    .padding(vertical = 8.dp),
                contentPadding = PaddingValues(horizontal = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                items(HOME_TOP_FEATURES) { feature ->
                    FeatureItemView(
                        feature = feature,
                        onClick = { onFeatureClick(feature.id) }
                    )
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
        }

        // 2. Albums Grid (3 columns reconstructed in LazyColumn chunks)
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
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
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
                        .padding(horizontal = 10.dp, vertical = 6.dp),
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
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
            )
            Spacer(modifier = Modifier.height(6.dp))

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 10.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(Color(0xFFF3F7FB))
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
                            modifier = Modifier.size(32.dp)
                        )
                        Spacer(modifier = Modifier.width(14.dp))
                        Text(
                            text = mediaType.title,
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.SemiBold,
                            color = Color(0xFF1E3354),
                            modifier = Modifier.weight(1f)
                        )
                        Icon(
                            painter = painterResource(id = R.drawable.ic_right_arrow),
                            contentDescription = null,
                            tint = Color(0xFF354052),
                            modifier = Modifier.size(16.dp)
                        )
                    }

                    if (index < MEDIA_TYPES.size - 1) {
                        HorizontalDivider(
                            color = Color(0xFFE4E6E9),
                            thickness = 1.dp,
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
            .width(62.dp)
            .clickable(onClick = onClick)
            .padding(vertical = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            painter = painterResource(id = feature.iconRes),
            contentDescription = feature.name,
            tint = Color.Unspecified,
            modifier = Modifier.size(34.dp)
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = feature.name,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Medium,
            fontSize = 11.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurface
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
                Icon(
                    imageVector = Icons.Outlined.Folder,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier
                        .size(44.dp)
                        .align(Alignment.Center)
                )
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        Text(
            text = album.name,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            color = MaterialTheme.colorScheme.onBackground
        )

        Text(
            text = "${album.itemCount} items",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontSize = 11.sp
        )
    }
}
