package com.quizango.gallery.ui.search

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.rememberAsyncImagePainter
import com.quizango.gallery.R
import com.quizango.gallery.data.repository.MediaRepository
import com.quizango.gallery.domain.model.Album
import com.quizango.gallery.domain.model.MediaItem
import com.quizango.gallery.domain.model.MediaType
import com.quizango.gallery.ui.theme.GalleryBlue
import com.quizango.gallery.ui.theme.cardBackground
import com.quizango.gallery.ui.theme.isDark
import com.quizango.gallery.ui.theme.searchBarBg
import com.quizango.gallery.ui.theme.settingsDivider
import java.io.File

val SEARCH_COLORS = listOf(
    Color(0xFFF2F2F2),
    Color(0xFF000000),
    Color(0xFFFF0000),
    Color(0xFFFFE500),
    Color(0xFFFF7A00),
    Color(0xFFFF00E5),
    Color(0xFF9E00FF),
    Color(0xFF0066FF),
    Color(0xFF00C2FF),
    Color(0xFF008625)
)

private val SEARCH_COLORS_HEX = listOf(
    "#F2F2F2",
    "#000000",
    "#FF0000",
    "#FFE500",
    "#FF7A00",
    "#FF00E5",
    "#9E00FF",
    "#0066FF",
    "#00C2FF",
    "#008625"
)

@Composable
fun SearchScreen(
    mediaRepository: MediaRepository,
    onMediaClick: (MediaItem) -> Unit,
    onAlbumClick: ((Album) -> Unit)? = null,
    onNavigateBack: () -> Unit,
    onNavigateToColorSearch: ((String) -> Unit)? = null
) {
    val allMedia by mediaRepository.getActiveMediaFlow().collectAsState(initial = emptyList())
    val allAlbums by mediaRepository.getAlbumsFlow().collectAsState(initial = emptyList())

    var searchQuery by remember { mutableStateOf("") }
    var selectedColorIndex by remember { mutableStateOf<Int?>(null) }
    var expandedAlbumsViewAll by remember { mutableStateOf(false) }
    var expandedMediaViewAll by remember { mutableStateOf(false) }

    // Filter albums according to search query
    val filteredAlbums = remember(allAlbums, searchQuery) {
        if (searchQuery.isBlank()) {
            allAlbums
        } else {
            allAlbums.filter { album ->
                album.name.contains(searchQuery, ignoreCase = true)
            }
        }
    }

    // Filter media items according to search query and selected color
    val filteredMedia = remember(allMedia, searchQuery, selectedColorIndex) {
        allMedia.filter { item ->
            val matchesQuery = if (searchQuery.isBlank()) {
                true
            } else {
                item.fileName.contains(searchQuery, ignoreCase = true) ||
                    item.folderPath.contains(searchQuery, ignoreCase = true)
            }

            val matchesColor = if (selectedColorIndex == null) {
                true
            } else {
                (Math.abs(item.filePath.hashCode()) % SEARCH_COLORS.size) == selectedColorIndex
            }

            matchesQuery && matchesColor
        }
    }

    val photosList = remember(filteredMedia) {
        filteredMedia.filter { it.mediaType == MediaType.IMAGE }
    }

    val videosList = remember(filteredMedia) {
        filteredMedia.filter { it.mediaType == MediaType.VIDEO }
    }

    val isSearchActive = searchQuery.isNotBlank() || selectedColorIndex != null
    val hasResults = filteredAlbums.isNotEmpty() || filteredMedia.isNotEmpty()

    Scaffold(
        topBar = {
            // Reference layout_search_toolbar.xml
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.background)
                    .statusBarsPadding()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                        .padding(horizontal = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = onNavigateBack,
                        modifier = Modifier.size(40.dp)
                    ) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_back),
                            contentDescription = "Back",
                            tint = MaterialTheme.colorScheme.onBackground,
                            modifier = Modifier.size(22.dp)
                        )
                    }

                    // Search Input Box (search_back: @color/sticker_search_color #F3F6FC, radius 16sdp ~ 20dp)
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(40.dp)
                            .clip(RoundedCornerShape(20.dp))
                            .background(MaterialTheme.colorScheme.searchBarBg)
                            .padding(horizontal = 14.dp),
                        contentAlignment = Alignment.CenterStart
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            BasicTextField(
                                value = searchQuery,
                                onValueChange = { searchQuery = it },
                                singleLine = true,
                                textStyle = TextStyle(
                                    fontSize = 14.sp,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    fontWeight = FontWeight.Medium
                                ),
                                cursorBrush = SolidColor(GalleryBlue),
                                modifier = Modifier.weight(1f),
                                decorationBox = { innerTextField ->
                                    if (searchQuery.isEmpty()) {
                                        Text(
                                            text = "Search",
                                            fontSize = 14.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                    innerTextField()
                                }
                            )

                            if (searchQuery.isNotEmpty()) {
                                IconButton(
                                    onClick = { searchQuery = "" },
                                    modifier = Modifier.size(20.dp)
                                ) {
                                    Icon(
                                        painter = painterResource(id = R.drawable.ic_close),
                                        contentDescription = "Clear",
                                        tint = MaterialTheme.colorScheme.onBackground,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            } else {
                                Icon(
                                    painter = painterResource(id = R.drawable.ic_search_common),
                                    contentDescription = "Search",
                                    tint = MaterialTheme.colorScheme.onBackground,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.width(6.dp))
                }

                HorizontalDivider(
                    color = MaterialTheme.colorScheme.settingsDivider,
                    thickness = 1.dp
                )
            }
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { paddingValues ->
        if (isSearchActive && !hasResults) {
            // Reference activity_search.xml: emptyLayout
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.padding(horizontal = 40.dp)
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_empty_img),
                        contentDescription = null,
                        tint = Color.Unspecified,
                        modifier = Modifier.size(100.dp)
                    )
                    Spacer(modifier = Modifier.height(20.dp))
                    Text(
                        text = "Oops!",
                        fontSize = 17.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "No search results found",
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
            ) {
                // 1. Colors Section (Reference activity_search.xml: colorLay)
                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 10.dp)
                    ) {
                        Text(
                            text = "Colors",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onBackground,
                            modifier = Modifier.padding(horizontal = 14.dp)
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        LazyRow(
                            modifier = Modifier.fillMaxWidth(),
                            contentPadding = PaddingValues(horizontal = 14.dp),
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            item {
                                // Color picker dropper icon (Reference ic_search_color_picker @id/searchColorPick)
                                Box(
                                    modifier = Modifier
                                        .size(32.dp)
                                        .clickable {
                                            if (onNavigateToColorSearch != null) {
                                                onNavigateToColorSearch("#FF0000")
                                            } else {
                                                selectedColorIndex = null
                                            }
                                        },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        painter = painterResource(id = R.drawable.ic_search_color_picker),
                                        contentDescription = "Color Picker",
                                        tint = Color.Unspecified,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }

                            itemsIndexed(SEARCH_COLORS) { index, color ->
                                val isSelected = selectedColorIndex == index
                                Box(
                                    modifier = Modifier
                                        .size(24.dp)
                                        .clip(CircleShape)
                                        .background(color)
                                        .border(
                                            width = if (isSelected) 2.5.dp else 1.dp,
                                            color = if (isSelected) GalleryBlue else if (index == 0) Color(0xFFD0D7DE) else Color.White,
                                            shape = CircleShape
                                        )
                                        .clickable {
                                            if (onNavigateToColorSearch != null) {
                                                onNavigateToColorSearch(SEARCH_COLORS_HEX[index])
                                            } else {
                                                selectedColorIndex = if (isSelected) null else index
                                            }
                                        }
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        HorizontalDivider(
                            color = MaterialTheme.colorScheme.settingsDivider,
                            thickness = 0.5.dp,
                            modifier = Modifier.padding(horizontal = 14.dp)
                        )
                    }
                }

                // 2. Albums Section (Reference item_search_main.xml: Albums)
                if (filteredAlbums.isNotEmpty()) {
                    item {
                        Column(modifier = Modifier.fillMaxWidth()) {
                            // Section Header with "View All"
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 14.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Albums",
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onBackground,
                                    modifier = Modifier.weight(1f)
                                )
                                Text(
                                    text = if (expandedAlbumsViewAll) "Collapse" else "View All",
                                    fontSize = 13.5.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = GalleryBlue,
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(4.dp))
                                        .clickable {
                                            expandedAlbumsViewAll = !expandedAlbumsViewAll
                                        }
                                        .padding(horizontal = 4.dp, vertical = 2.dp)
                                )
                            }

                            // 3-Column Grid of Albums (Reference item_search_sub.xml)
                            val displayAlbums = if (expandedAlbumsViewAll) {
                                filteredAlbums
                            } else {
                                filteredAlbums.take(6)
                            }

                            val chunkedAlbums = displayAlbums.chunked(3)
                            chunkedAlbums.forEach { rowAlbums ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 10.dp),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    rowAlbums.forEach { album ->
                                        Box(modifier = Modifier.weight(1f)) {
                                            SearchAlbumCard(
                                                album = album,
                                                onClick = {
                                                    if (onAlbumClick != null) {
                                                        onAlbumClick(album)
                                                    }
                                                }
                                            )
                                        }
                                    }
                                    // Fill empty slots in row
                                    repeat(3 - rowAlbums.size) {
                                        Spacer(modifier = Modifier.weight(1f))
                                    }
                                }
                                Spacer(modifier = Modifier.height(8.dp))
                            }

                            HorizontalDivider(
                                color = MaterialTheme.colorScheme.settingsDivider,
                                thickness = 0.5.dp,
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
                            )
                        }
                    }
                }

                // 3. Photos Section (Reference item_search_main.xml: Photos)
                if (photosList.isNotEmpty()) {
                    item {
                        Column(modifier = Modifier.fillMaxWidth()) {
                            // Section Header with "View All"
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 14.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Photos",
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onBackground,
                                    modifier = Modifier.weight(1f)
                                )
                                Text(
                                    text = if (expandedMediaViewAll) "Collapse" else "View All",
                                    fontSize = 13.5.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = GalleryBlue,
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(4.dp))
                                        .clickable {
                                            expandedMediaViewAll = !expandedMediaViewAll
                                        }
                                        .padding(horizontal = 4.dp, vertical = 2.dp)
                                )
                            }

                            // 3-Column Grid of Photos
                            val displayPhotos = if (expandedMediaViewAll) {
                                photosList
                            } else {
                                photosList.take(6)
                            }

                            val chunkedPhotos = displayPhotos.chunked(3)
                            chunkedPhotos.forEach { rowPhotos ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 10.dp),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    rowPhotos.forEach { mediaItem ->
                                        Box(modifier = Modifier.weight(1f)) {
                                            SearchMediaCard(
                                                item = mediaItem,
                                                onClick = { onMediaClick(mediaItem) }
                                            )
                                        }
                                    }
                                    repeat(3 - rowPhotos.size) {
                                        Spacer(modifier = Modifier.weight(1f))
                                    }
                                }
                                Spacer(modifier = Modifier.height(8.dp))
                            }

                            HorizontalDivider(
                                color = MaterialTheme.colorScheme.settingsDivider,
                                thickness = 0.5.dp,
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
                            )
                        }
                    }
                }

                // 4. Videos Section (if any videos exist)
                if (videosList.isNotEmpty()) {
                    item {
                        Column(modifier = Modifier.fillMaxWidth()) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 14.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Videos",
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onBackground,
                                    modifier = Modifier.weight(1f)
                                )
                            }

                            val chunkedVideos = videosList.take(6).chunked(3)
                            chunkedVideos.forEach { rowVideos ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 10.dp),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    rowVideos.forEach { mediaItem ->
                                        Box(modifier = Modifier.weight(1f)) {
                                            SearchMediaCard(
                                                item = mediaItem,
                                                onClick = { onMediaClick(mediaItem) }
                                            )
                                        }
                                    }
                                    repeat(3 - rowVideos.size) {
                                        Spacer(modifier = Modifier.weight(1f))
                                    }
                                }
                                Spacer(modifier = Modifier.height(8.dp))
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * Reconstructs item_search_sub.xml for Album items:
 * - Square thumbnail with RoundedCornerShape(10.dp)
 * - Album title in @color/titleColor (#1E3354)
 * - Item count in @color/countTitle (#707C93)
 */
@Composable
private fun SearchAlbumCard(
    album: Album,
    onClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .clickable(onClick = onClick)
            .padding(4.dp)
    ) {
        // SquareCardLayout
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1f)
                .clip(RoundedCornerShape(10.dp))
                .background(MaterialTheme.colorScheme.cardBackground),
            contentAlignment = Alignment.Center
        ) {
            if (album.coverPath.isNotEmpty()) {
                Image(
                    painter = rememberAsyncImagePainter(model = coil.request.ImageRequest.Builder(androidx.compose.ui.platform.LocalContext.current).data(File(album.coverPath)).size(300, 300).allowRgb565(true).build()),
                    contentDescription = album.name,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                Icon(
                    painter = painterResource(id = R.drawable.ic_create_album),
                    contentDescription = null,
                    tint = Color(0xFF99A4B6),
                    modifier = Modifier.size(36.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(4.dp))

        Text(
            text = album.name,
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(horizontal = 2.dp)
        )

        Text(
            text = "${album.itemCount}",
            fontSize = 11.5.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            modifier = Modifier.padding(horizontal = 2.dp)
        )
    }
}

/**
 * Reconstructs item_search_sub.xml for individual Media items:
 * - Square thumbnail with RoundedCornerShape(10.dp)
 * - File title and info
 */
@Composable
private fun SearchMediaCard(
    item: MediaItem,
    onClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .clickable(onClick = onClick)
            .padding(4.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1f)
                .clip(RoundedCornerShape(10.dp))
                .background(MaterialTheme.colorScheme.cardBackground),
            contentAlignment = Alignment.Center
        ) {
            Image(
                painter = rememberAsyncImagePainter(model = coil.request.ImageRequest.Builder(androidx.compose.ui.platform.LocalContext.current).data(File(item.filePath)).size(300, 300).allowRgb565(true).build()),
                contentDescription = item.fileName,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        }

        Spacer(modifier = Modifier.height(4.dp))

        Text(
            text = item.fileName,
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(horizontal = 2.dp)
        )
    }
}

