package albums.gallery.photo.folder.picasa.app.web.gallery.ui.search

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import albums.gallery.photo.folder.picasa.app.web.gallery.R
import albums.gallery.photo.folder.picasa.app.web.gallery.data.repository.MediaRepository
import albums.gallery.photo.folder.picasa.app.web.gallery.domain.model.MediaItem
import albums.gallery.photo.folder.picasa.app.web.gallery.domain.model.MediaType
import albums.gallery.photo.folder.picasa.app.web.gallery.ui.home.PhotoGridItem
import albums.gallery.photo.folder.picasa.app.web.gallery.ui.theme.GalleryBlue

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

@Composable
fun SearchScreen(
    mediaRepository: MediaRepository,
    onMediaClick: (MediaItem) -> Unit,
    onNavigateBack: () -> Unit
) {
    val allMedia by mediaRepository.getActiveMediaFlow().collectAsState(initial = emptyList())
    var searchQuery by remember { mutableStateOf("") }
    var selectedColorIndex by remember { mutableStateOf<Int?>(null) }
    var selectedFilterChip by remember { mutableStateOf("All") }

    val filteredList = remember(allMedia, searchQuery, selectedColorIndex, selectedFilterChip) {
        allMedia.filter { item ->
            val matchesQuery = if (searchQuery.isBlank()) {
                true
            } else {
                item.fileName.contains(searchQuery, ignoreCase = true) ||
                    item.folderPath.contains(searchQuery, ignoreCase = true)
            }

            val matchesType = when (selectedFilterChip) {
                "Photos" -> item.mediaType == MediaType.IMAGE
                "Videos" -> item.mediaType == MediaType.VIDEO
                else -> true
            }

            val matchesColor = if (selectedColorIndex == null) {
                true
            } else {
                (Math.abs(item.filePath.hashCode()) % SEARCH_COLORS.size) == selectedColorIndex
            }

            matchesQuery && matchesType && matchesColor
        }
    }

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
                        .padding(horizontal = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_back),
                            contentDescription = "Back",
                            tint = Color.Unspecified,
                            modifier = Modifier.size(24.dp)
                        )
                    }

                    // Search Input Box (search_back background)
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(40.dp)
                            .clip(RoundedCornerShape(20.dp))
                            .background(Color(0xFFF3F6FC))
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
                                    color = Color(0xFF1E3354),
                                    fontWeight = FontWeight.Medium
                                ),
                                cursorBrush = SolidColor(GalleryBlue),
                                modifier = Modifier.weight(1f),
                                decorationBox = { innerTextField ->
                                    if (searchQuery.isEmpty()) {
                                        Text(
                                            text = "Search",
                                            fontSize = 14.sp,
                                            color = Color(0xFF99A4B6)
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
                                        tint = Color.Unspecified,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            } else {
                                Icon(
                                    painter = painterResource(id = R.drawable.ic_search_common),
                                    contentDescription = "Search",
                                    tint = Color.Unspecified,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.width(6.dp))
                }

                HorizontalDivider(
                    color = Color(0xFFE4E6E9),
                    thickness = 1.dp
                )
            }
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Colors Section matching activity_search.xml
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 10.dp)
            ) {
                Text(
                    text = "Colors",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFF1E3354),
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
                        // Color picker reset / wheel icon
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .clickable { selectedColorIndex = null },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                painter = painterResource(id = R.drawable.ic_search_color_picker),
                                contentDescription = "All Colors",
                                tint = Color.Unspecified,
                                modifier = Modifier.size(32.dp)
                            )
                        }
                    }

                    itemsIndexed(SEARCH_COLORS) { index, color ->
                        val isSelected = selectedColorIndex == index
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(CircleShape)
                                .background(color)
                                .border(
                                    width = if (isSelected) 3.dp else 1.dp,
                                    color = if (isSelected) GalleryBlue else Color(0xFFD0D7DE),
                                    shape = CircleShape
                                )
                                .clickable {
                                    selectedColorIndex = if (isSelected) null else index
                                }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                HorizontalDivider(
                    color = Color(0xFFE4E6E9),
                    thickness = 0.5.dp,
                    modifier = Modifier.padding(horizontal = 14.dp)
                )
            }

            // Filter Chips (All, Photos, Videos)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                listOf("All", "Photos", "Videos").forEach { chip ->
                    val isSelected = selectedFilterChip == chip
                    FilterChip(
                        selected = isSelected,
                        onClick = { selectedFilterChip = chip },
                        label = { Text(chip) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = GalleryBlue.copy(alpha = 0.15f),
                            selectedLabelColor = GalleryBlue
                        )
                    )
                }
            }

            // Results Grid or Empty Layout
            if (filteredList.isEmpty()) {
                // Empty Layout matching activity_search.xml emptyLayout
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .weight(1f),
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
                            color = Color(0xFF1E3354)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "No results found for your search",
                            fontSize = 13.sp,
                            color = Color(0xFF707C93),
                            textAlign = TextAlign.Center
                        )
                    }
                }
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(3),
                    contentPadding = PaddingValues(2.dp),
                    modifier = Modifier
                        .fillMaxSize()
                        .weight(1f)
                ) {
                    items(filteredList, key = { it.filePath }) { item ->
                        PhotoGridItem(
                            item = item,
                            isSelected = false,
                            isSelectionMode = false,
                            onClick = { onMediaClick(item) },
                            onLongClick = {}
                        )
                    }
                }
            }
        }
    }
}
