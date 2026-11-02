package com.quizango.gallery.ui.discovery

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.quizango.gallery.R
import com.quizango.gallery.data.repository.MediaRepository
import com.quizango.gallery.domain.model.MediaItem
import com.quizango.gallery.ui.home.formatGroupHeader
import com.quizango.gallery.ui.theme.GalleryBlue
import com.quizango.gallery.ui.theme.isDark
import com.quizango.gallery.ui.theme.settingsDivider
import java.io.File

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun SearchColorScreen(
    colorHex: String = "#FF0000",
    mediaRepository: MediaRepository,
    onMediaClick: (MediaItem) -> Unit,
    onNavigateBack: () -> Unit
) {
    val mediaItems by mediaRepository.searchByColorFlow(colorHex).collectAsState(initial = emptyList())
    val selectedPaths = remember { mutableStateListOf<String>() }
    var isSelectionMode by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            Column(modifier = Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.background)) {
                TopAppBar(
                    title = {
                        Text(
                            text = if (isSelectionMode) "${selectedPaths.size} Selected" else stringResource(R.string.media_color),
                            fontSize = 18.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                    },
                    navigationIcon = {
                        IconButton(onClick = {
                            if (isSelectionMode) {
                                selectedPaths.clear()
                                isSelectionMode = false
                            } else {
                                onNavigateBack()
                            }
                        }) {
                            Icon(
                                painter = painterResource(id = if (isSelectionMode) R.drawable.ic_close else R.drawable.ic_back),
                                contentDescription = if (isSelectionMode) "Close" else "Back",
                                tint = MaterialTheme.colorScheme.onBackground,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    },
                    actions = {
                        if (isSelectionMode) {
                            val allSelected = selectedPaths.size == mediaItems.size && mediaItems.isNotEmpty()
                            TextButton(onClick = {
                                if (allSelected) {
                                    selectedPaths.clear()
                                } else {
                                    selectedPaths.clear()
                                    selectedPaths.addAll(mediaItems.map { it.filePath })
                                }
                            }) {
                                Text(
                                    text = if (allSelected) "Deselect All" else stringResource(R.string.select_all),
                                    color = GalleryBlue,
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 14.sp
                                )
                            }
                        } else {
                            IconButton(
                                onClick = { /* PRO action */ },
                                modifier = Modifier.padding(end = 6.dp)
                            ) {
                                Icon(
                                    painter = painterResource(id = R.drawable.ic_pro_premium),
                                    contentDescription = "PRO",
                                    tint = Color.Unspecified,
                                    modifier = Modifier
                                        .height(25.dp)
                                        .width(56.dp)
                                )
                            }
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.background
                    )
                )
                HorizontalDivider(
                    thickness = 1.dp,
                    color = MaterialTheme.colorScheme.settingsDivider
                )
            }
        },
        bottomBar = {
            if (isSelectionMode) {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    color = MaterialTheme.colorScheme.surface,
                    shadowElevation = 8.dp
                ) {
                    Column {
                        HorizontalDivider(thickness = 0.5.dp, color = MaterialTheme.colorScheme.settingsDivider)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 10.dp),
                            horizontalArrangement = Arrangement.SpaceEvenly,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(
                                    painter = painterResource(id = R.drawable.ic_home_select_share),
                                    contentDescription = "Share",
                                    tint = Color.Unspecified,
                                    modifier = Modifier.size(22.dp)
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(text = "Share", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurface)
                            }
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(
                                    painter = painterResource(id = R.drawable.ic_album_select_delete),
                                    contentDescription = "Delete",
                                    tint = Color.Unspecified,
                                    modifier = Modifier.size(22.dp)
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(text = "Delete", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurface)
                            }
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(
                                    painter = painterResource(id = R.drawable.ic_more),
                                    contentDescription = "More",
                                    tint = Color.Unspecified,
                                    modifier = Modifier.size(22.dp)
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(text = "More", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurface)
                            }
                        }
                    }
                }
            }
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(MaterialTheme.colorScheme.background)
        ) {
            if (mediaItems.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_empty_img),
                            contentDescription = null,
                            tint = Color.Unspecified,
                            modifier = Modifier.size(110.dp)
                        )
                        Spacer(modifier = Modifier.height(20.dp))
                        Text(
                            text = stringResource(id = R.string.no_items),
                            fontSize = 17.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = stringResource(id = R.string.no_media_file),
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Normal,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            } else {
                val grouped = remember(mediaItems) {
                    mediaItems.groupBy { formatGroupHeader(it.dateAdded) }
                }

                LazyVerticalGrid(
                    columns = GridCells.Fixed(4),
                    contentPadding = PaddingValues(2.dp),
                    horizontalArrangement = Arrangement.spacedBy(2.dp),
                    verticalArrangement = Arrangement.spacedBy(2.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    grouped.forEach { (header, items) ->
                        item(span = { GridItemSpan(4) }) {
                            Text(
                                text = header,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onBackground,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 12.dp, vertical = 10.dp)
                            )
                        }

                        items(items, key = { it.filePath }) { item ->
                            val isSelected = selectedPaths.contains(item.filePath)

                            Box(
                                modifier = Modifier
                                    .aspectRatio(1f)
                                    .combinedClickable(
                                        onClick = {
                                            if (isSelectionMode) {
                                                if (isSelected) {
                                                    selectedPaths.remove(item.filePath)
                                                    if (selectedPaths.isEmpty()) isSelectionMode = false
                                                } else {
                                                    selectedPaths.add(item.filePath)
                                                }
                                            } else {
                                                onMediaClick(item)
                                            }
                                        },
                                        onLongClick = {
                                            isSelectionMode = true
                                            if (isSelected) {
                                                selectedPaths.remove(item.filePath)
                                                if (selectedPaths.isEmpty()) isSelectionMode = false
                                            } else {
                                                selectedPaths.add(item.filePath)
                                            }
                                        }
                                    )
                            ) {
                                AsyncImage(
                                    model = File(item.filePath),
                                    contentDescription = item.fileName,
                                    modifier = Modifier.fillMaxSize(),
                                    contentScale = ContentScale.Crop
                                )

                                if (isSelectionMode) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .background(if (isSelected) Color(0x330066FF) else Color.Transparent)
                                    )
                                    Box(
                                        modifier = Modifier
                                            .padding(4.dp)
                                            .size(20.dp)
                                            .align(Alignment.TopEnd)
                                            .clip(CircleShape)
                                            .background(if (isSelected) GalleryBlue else Color(0x66000000))
                                            .border(1.dp, Color.White, CircleShape),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        if (isSelected) {
                                            Icon(
                                                imageVector = Icons.Default.Check,
                                                contentDescription = null,
                                                tint = Color.White,
                                                modifier = Modifier.size(14.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

