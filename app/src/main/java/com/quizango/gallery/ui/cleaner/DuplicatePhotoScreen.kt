package com.quizango.gallery.ui.cleaner

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.quizango.gallery.R
import com.quizango.gallery.data.repository.MediaRepository
import com.quizango.gallery.domain.model.MediaItem
import com.quizango.gallery.ui.theme.GalleryBlue
import kotlinx.coroutines.launch
import java.io.File

/**
 * Duplicate Photo Screen with 100% parity to reference activity_duplicate_photo.xml,
 * item_duplicate_main.xml, item_duplicate_sub.xml, and DuplicatePhotoActivity.java.
 * Features:
 * - 4-column square grid layout (GridLayoutManager spanCount=4)
 * - Group header spanning 4 columns ("Group X (Y images)")
 * - "Keep best photo" toggle preserving the original unselected by default
 * - Top-right "Select All" / "Unselect All" toggle
 * - Authentic ic_check / ic_un_check corner selection indicators
 * - Bottom red delete bar (#E11A1A) with live size & count indicators
 * - Authentic permanent delete confirmation dialog
 * - Seamless transition to DuplicateCompleteScreen
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DuplicatePhotoScreen(
    mediaRepository: MediaRepository,
    onNavigateBack: () -> Unit,
    onNavigateToComplete: ((count: Int, sizeBytes: Long) -> Unit)? = null
) {
    val scope = rememberCoroutineScope()
    val allMedia by mediaRepository.getActiveMediaFlow().collectAsState(initial = emptyList())
    var selectedDuplicates by remember { mutableStateOf(setOf<String>()) }
    var keepBestPhotoEnabled by remember { mutableStateOf(true) }
    var showDeleteDialog by remember { mutableStateOf(false) }

    // Duplicate grouping algorithm:
    // 1. Group by explicit duplicateGroup if set > 0
    // 2. Otherwise group by identical non-zero fileSize
    val duplicateGroups: List<List<MediaItem>> = remember(allMedia) {
        val explicitGroups = allMedia.filter { it.duplicateGroup != 0L }
            .groupBy { it.duplicateGroup }
            .filter { it.value.size > 1 }
            .values
            .toList()

        val explicitPaths = explicitGroups.flatten().map { it.filePath }.toSet()

        val sizeGroups = allMedia.filter { it.filePath !in explicitPaths && it.fileSize > 0 }
            .groupBy { it.fileSize }
            .filter { it.value.size > 1 }
            .values
            .toList()

        (explicitGroups + sizeGroups).distinctBy { group -> group.map { it.filePath }.sorted() }
    }

    // Default selection logic:
    // If keepBestPhotoEnabled is true, keep the 1st photo in each group unselected and select the rest.
    // If keepBestPhotoEnabled is false, select all items across all duplicate groups.
    LaunchedEffect(duplicateGroups, keepBestPhotoEnabled) {
        val autoSelected = mutableSetOf<String>()
        duplicateGroups.forEach { group ->
            if (keepBestPhotoEnabled) {
                group.drop(1).forEach { autoSelected.add(it.filePath) }
            } else {
                group.forEach { autoSelected.add(it.filePath) }
            }
        }
        selectedDuplicates = autoSelected
    }

    val allDuplicatePaths = remember(duplicateGroups) {
        duplicateGroups.flatten().map { it.filePath }.toSet()
    }

    val isAllSelected = selectedDuplicates.isNotEmpty() && selectedDuplicates.size >= allDuplicatePaths.size

    val selectedItems = remember(allMedia, selectedDuplicates) {
        allMedia.filter { it.filePath in selectedDuplicates }
    }
    val selectedBytes = remember(selectedItems) {
        selectedItems.sumOf { it.fileSize }
    }

    Scaffold(
        topBar = {
            Column(modifier = Modifier.fillMaxWidth()) {
                TopAppBar(
                    title = {
                        Text(
                            text = stringResource(id = R.string.duplicate_images),
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 18.sp,
                            color = Color(0xFF1E3354)
                        )
                    },
                    navigationIcon = {
                        IconButton(onClick = onNavigateBack) {
                            Icon(
                                painter = painterResource(id = R.drawable.ic_back),
                                contentDescription = "Back",
                                tint = Color.Unspecified,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    },
                    actions = {
                        if (duplicateGroups.isNotEmpty()) {
                            TextButton(
                                onClick = {
                                    selectedDuplicates = if (isAllSelected) {
                                        emptySet()
                                    } else {
                                        allDuplicatePaths
                                    }
                                }
                            ) {
                                Text(
                                    text = if (isAllSelected) {
                                        stringResource(id = R.string.unselect_all)
                                    } else {
                                        stringResource(id = R.string.select_all)
                                    },
                                    color = GalleryBlue,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.background
                    )
                )
                HorizontalDivider(
                    thickness = 0.5.dp,
                    color = Color(0x1A071427)
                )
            }
        },
        bottomBar = {
            if (selectedDuplicates.isNotEmpty()) {
                Surface(
                    tonalElevation = 6.dp,
                    color = MaterialTheme.colorScheme.surface,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 12.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(Color(0xFFE11A1A))
                                .clickable { showDeleteDialog = true },
                            contentAlignment = Alignment.Center
                        ) {
                            val sizeText = if (selectedBytes > 0) " (${formatDuplicateSize(selectedBytes)})" else " (${selectedDuplicates.size})"
                            Text(
                                text = "${stringResource(id = R.string.delete_selected)}$sizeText",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }
                    }
                }
            }
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            if (duplicateGroups.isNotEmpty()) {
                // Keep Best Photo row matching keepPhotoLayout in activity_duplicate_photo.xml
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = stringResource(id = R.string.keep_best_photo),
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color(0xFF1E3354),
                        modifier = Modifier.weight(1f)
                    )
                    Switch(
                        checked = keepBestPhotoEnabled,
                        onCheckedChange = { keepBestPhotoEnabled = it },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.White,
                            checkedTrackColor = GalleryBlue
                        )
                    )
                }

                HorizontalDivider(
                    thickness = 0.5.dp,
                    color = Color(0x1A071427)
                )
            }

            if (duplicateGroups.isEmpty()) {
                // Authentic Empty State matching activity_duplicate_photo.xml
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
                            modifier = Modifier.size(110.dp)
                        )
                        Spacer(modifier = Modifier.height(20.dp))
                        Text(
                            text = stringResource(id = R.string.cleaner_duplicate_empty_layout_text),
                            fontSize = 17.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color(0xFF1E3354),
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = stringResource(id = R.string.no_duplicate_files),
                            fontSize = 13.sp,
                            color = Color(0xFF707C93),
                            textAlign = TextAlign.Center
                        )
                    }
                }
            } else {
                // Authentic 4-column Grid matching GridLayoutManager(4) in DuplicatePhotoActivity
                LazyVerticalGrid(
                    columns = GridCells.Fixed(4),
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                    modifier = Modifier
                        .fillMaxSize()
                        .weight(1f)
                ) {
                    duplicateGroups.forEachIndexed { groupIndex, group ->
                        // Group Header spanning full 4 columns (item_duplicate_main.xml)
                        item(
                            span = { GridItemSpan(4) },
                            key = "header_$groupIndex"
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 4.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Group ${groupIndex + 1} (${group.size} images)",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = Color(0xFF1E3354),
                                    modifier = Modifier.weight(1f)
                                )
                                val redundantSize = group.drop(1).sumOf { it.fileSize }
                                if (redundantSize > 0) {
                                    Text(
                                        text = formatDuplicateSize(redundantSize),
                                        fontSize = 12.sp,
                                        color = Color(0xFF707C93)
                                    )
                                }
                            }
                        }

                        // Duplicate items spanning 1 column each (item_duplicate_sub.xml)
                        items(
                            count = group.size,
                            key = { "${group[it].filePath}_${groupIndex}_$it" },
                            span = { GridItemSpan(1) }
                        ) { itemIndex ->
                            val item = group[itemIndex]
                            val isSelected = item.filePath in selectedDuplicates
                            val isBest = itemIndex == 0 && keepBestPhotoEnabled

                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .aspectRatio(1f)
                                    .clip(RoundedCornerShape(4.dp))
                                    .clickable {
                                        selectedDuplicates = if (isSelected) {
                                            selectedDuplicates - item.filePath
                                        } else {
                                            selectedDuplicates + item.filePath
                                        }
                                    }
                            ) {
                                AsyncImage(
                                    model = ImageRequest.Builder(LocalContext.current)
                                        .data(File(item.filePath))
                                        .crossfade(true)
                                        .build(),
                                    contentDescription = item.fileName,
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.fillMaxSize()
                                )

                                // BEST badge on the first image when keepBestPhotoEnabled is active
                                if (isBest) {
                                    Box(
                                        modifier = Modifier
                                            .align(Alignment.TopStart)
                                            .padding(3.dp)
                                            .clip(RoundedCornerShape(2.dp))
                                            .background(GalleryBlue)
                                            .padding(horizontal = 4.dp, vertical = 1.dp)
                                    ) {
                                        Text(
                                            text = "BEST",
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color.White
                                        )
                                    }
                                }

                                // Selection check indicator (ic_check vs ic_un_check)
                                Icon(
                                    painter = painterResource(
                                        id = if (isSelected) R.drawable.ic_check else R.drawable.ic_un_check
                                    ),
                                    contentDescription = if (isSelected) "Selected" else "Unselected",
                                    tint = Color.Unspecified,
                                    modifier = Modifier
                                        .align(Alignment.TopEnd)
                                        .padding(3.dp)
                                        .size(20.dp)
                                )

                                // Bottom shadow with file size label (item_duplicate_sub.xml)
                                Box(
                                    modifier = Modifier
                                        .align(Alignment.BottomCenter)
                                        .fillMaxWidth()
                                        .background(
                                            Brush.verticalGradient(
                                                listOf(Color.Transparent, Color.Black.copy(alpha = 0.65f))
                                            )
                                        )
                                        .padding(horizontal = 4.dp, vertical = 2.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = formatDuplicateSize(item.fileSize),
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = Color.White,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Permanent delete confirmation dialog matching dialog_cleaner_permanently_delete.xml
    if (showDeleteDialog) {
        val isSingle = selectedDuplicates.size == 1
        val dialogTitle = if (isSingle) {
            stringResource(id = R.string.permanent_delete_duplicate_single_title)
        } else {
            stringResource(id = R.string.permanent_delete_duplicate_title)
        }

        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = {
                Text(
                    text = dialogTitle,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    color = Color(0xFF1E3354)
                )
            },
            text = {
                Text(
                    text = stringResource(id = R.string.permanent_delete_duplicate_sub_title),
                    fontSize = 14.sp,
                    color = Color(0xFF707C93)
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDeleteDialog = false
                        val count = selectedDuplicates.size
                        val totalBytes = selectedBytes
                        val toDelete = selectedDuplicates.toList()
                        scope.launch {
                            toDelete.forEach { path ->
                                mediaRepository.moveToTrash(path)
                            }
                            selectedDuplicates = emptySet()
                            onNavigateToComplete?.invoke(count, totalBytes)
                        }
                    }
                ) {
                    Text(
                        text = stringResource(id = R.string.clean),
                        color = Color(0xFFE11A1A),
                        fontWeight = FontWeight.Bold
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) {
                    Text(
                        text = stringResource(id = R.string.cancel),
                        color = Color(0xFF707C93),
                        fontWeight = FontWeight.Medium
                    )
                }
            },
            containerColor = MaterialTheme.colorScheme.surface,
            shape = RoundedCornerShape(14.dp)
        )
    }
}

fun formatDuplicateSize(bytes: Long): String {
    return when {
        bytes >= 1024L * 1024L * 1024L -> "%.2f GB".format(bytes.toDouble() / (1024 * 1024 * 1024))
        bytes >= 1024L * 1024L -> "%.1f MB".format(bytes.toDouble() / (1024 * 1024))
        bytes >= 1024L -> "%d KB".format(bytes / 1024)
        else -> "$bytes B"
    }
}
