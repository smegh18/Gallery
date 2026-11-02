package com.quizango.gallery.ui.recycle

import android.widget.Toast
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.quizango.gallery.R
import com.quizango.gallery.data.local.datastore.AppPreferences
import com.quizango.gallery.data.repository.MediaRepository
import com.quizango.gallery.domain.model.MediaItem
import com.quizango.gallery.domain.model.MediaType
import com.quizango.gallery.ui.home.formatDuration
import com.quizango.gallery.ui.navigation.Screen
import com.quizango.gallery.ui.theme.GalleryBlue
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.io.File

/**
 * Calculates remaining retention days (out of 30 days) before auto-deletion.
 */
fun calculateRemainingDays(deletedDate: Long, currentTimeMs: Long = System.currentTimeMillis()): Int {
    if (deletedDate <= 0L) return 30
    val elapsedMs = currentTimeMs - deletedDate
    val elapsedDays = (elapsedMs / (24 * 60 * 60 * 1000L)).toInt()
    return maxOf(1, 30 - elapsedDays)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecycleScreen(
    mediaRepository: MediaRepository,
    appPreferences: AppPreferences? = null,
    onNavigate: (String) -> Unit = {},
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val recycleItems by mediaRepository.getRecycleBinFlow().collectAsState(initial = emptyList())
    var selectedPaths by remember { mutableStateOf(setOf<String>()) }
    var showDeleteConfirmDialog by remember { mutableStateOf(false) }
    var showRestoreConfirmDialog by remember { mutableStateOf(false) }
    var showEmptyBinConfirmDialog by remember { mutableStateOf(false) }

    var autoDeleteEnabled by remember { mutableStateOf(true) }

    LaunchedEffect(appPreferences) {
        appPreferences?.let { prefs ->
            autoDeleteEnabled = prefs.autoDeleteTrash.first()
        }
    }

    val isSelectionMode = selectedPaths.isNotEmpty()

    Scaffold(
        topBar = {
            Column(modifier = Modifier.fillMaxWidth()) {
                TopAppBar(
                    title = {
                        Text(
                            text = if (isSelectionMode) "${selectedPaths.size} Selected" else "Recycle Bin",
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 18.sp,
                            color = Color(0xFF1E3354)
                        )
                    },
                    navigationIcon = {
                        IconButton(
                            onClick = {
                                if (isSelectionMode) {
                                    selectedPaths = emptySet()
                                } else {
                                    onNavigateBack()
                                }
                            }
                        ) {
                            Icon(
                                painter = painterResource(id = if (isSelectionMode) R.drawable.ic_close else R.drawable.ic_back),
                                contentDescription = if (isSelectionMode) "Close" else "Back",
                                tint = Color.Unspecified,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    },
                    actions = {
                        if (isSelectionMode) {
                            TextButton(
                                onClick = {
                                    selectedPaths = if (selectedPaths.size == recycleItems.size) {
                                        emptySet()
                                    } else {
                                        recycleItems.map { it.filePath }.toSet()
                                    }
                                }
                            ) {
                                Text(
                                    text = if (selectedPaths.size == recycleItems.size) "Deselect" else "Select All",
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 14.sp,
                                    color = GalleryBlue
                                )
                            }
                        } else if (recycleItems.isNotEmpty()) {
                            IconButton(onClick = { showEmptyBinConfirmDialog = true }) {
                                Icon(
                                    painter = painterResource(id = R.drawable.ic_delete),
                                    contentDescription = "Empty Recycle Bin",
                                    tint = Color.Unspecified,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.background
                    )
                )
                HorizontalDivider(thickness = 1.dp, color = Color(0xFFE4E6E9))
            }
        },
        bottomBar = {
            if (isSelectionMode) {
                Surface(
                    tonalElevation = 8.dp,
                    shadowElevation = 8.dp,
                    shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp),
                    color = MaterialTheme.colorScheme.surface,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 48.dp, vertical = 12.dp),
                        horizontalArrangement = Arrangement.SpaceAround,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // 1. Restore Action (matching RecycleActivity layoutSelectMediaBottom)
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier
                                .clickable { showRestoreConfirmDialog = true }
                                .padding(8.dp)
                        ) {
                            Icon(
                                painter = painterResource(id = R.drawable.ic_restore),
                                contentDescription = "Restore",
                                tint = Color.Unspecified,
                                modifier = Modifier.size(26.dp)
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Restore",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium,
                                color = Color(0xFF1E3354)
                            )
                        }

                        // 2. Permanently Delete Action (matching RecycleActivity layoutSelectMediaBottom)
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier
                                .clickable { showDeleteConfirmDialog = true }
                                .padding(8.dp)
                        ) {
                            Icon(
                                painter = painterResource(id = R.drawable.ic_album_select_delete),
                                contentDescription = "Delete",
                                tint = Color.Unspecified,
                                modifier = Modifier.size(26.dp)
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Delete",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium,
                                color = Color(0xFF1E3354)
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
            // Auto-delete Card matching activity_recycle.xml autoDeleteLayout
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 8.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color(0xFFE8F2FF))
                    .padding(horizontal = 14.dp, vertical = 8.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "Recycle bin items will be removed after 30 days.",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Normal,
                        color = Color(0xFF1E3354),
                        modifier = Modifier.weight(1f)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Switch(
                        checked = autoDeleteEnabled,
                        onCheckedChange = { isChecked ->
                            autoDeleteEnabled = isChecked
                            scope.launch {
                                appPreferences?.setAutoDeleteTrash(isChecked)
                            }
                        },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.White,
                            checkedTrackColor = GalleryBlue,
                            uncheckedThumbColor = Color.White,
                            uncheckedTrackColor = Color(0xFFBFD4E4)
                        )
                    )
                }
            }

            if (recycleItems.isEmpty()) {
                // Empty Layout matching activity_recycle.xml emptyLayout
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
                            text = "No items",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color(0xFF1E3354)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "No deleted files here. When you remove items from your phone, they'll appear here,",
                            fontSize = 13.sp,
                            color = Color(0xFF707C93),
                            textAlign = TextAlign.Center,
                            lineHeight = 18.sp
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
                    items(recycleItems, key = { it.filePath }) { item ->
                        val isSelected = item.filePath in selectedPaths
                        RecycleGridItem(
                            item = item,
                            isSelected = isSelected,
                            isSelectionMode = isSelectionMode,
                            onClick = {
                                if (isSelectionMode) {
                                    selectedPaths = if (isSelected) {
                                        selectedPaths - item.filePath
                                    } else {
                                        selectedPaths + item.filePath
                                    }
                                } else {
                                    onNavigate(Screen.MediaViewer.createRoute(item.filePath, isRecycle = true))
                                }
                            },
                            onLongClick = {
                                if (!isSelectionMode) {
                                    selectedPaths = setOf(item.filePath)
                                }
                            }
                        )
                    }
                }
            }
        }

        // 1. Restore Confirmation Dialog matching dialog_restore_hide.xml
        if (showRestoreConfirmDialog) {
            Dialog(onDismissRequest = { showRestoreConfirmDialog = false }) {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(22.dp)
                    ) {
                        Text(
                            text = "Restore items",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color(0xFF1E3354)
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = "Are you sure you want to restore items?",
                            fontSize = 14.sp,
                            color = Color(0xFF707C93),
                            lineHeight = 20.sp
                        )
                        Spacer(modifier = Modifier.height(24.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            // Cancel button (matching help_delete_cancel_bg)
                            Button(
                                onClick = { showRestoreConfirmDialog = false },
                                modifier = Modifier
                                    .weight(1f)
                                    .height(44.dp),
                                shape = RoundedCornerShape(22.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color(0xFFF2F4F7),
                                    contentColor = Color(0xFF707C93)
                                )
                            ) {
                                Text("Cancel", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                            }

                            // Restore button (matching btn_permission)
                            Button(
                                onClick = {
                                    val targets = recycleItems.filter { it.filePath in selectedPaths }
                                    scope.launch {
                                        targets.forEach { mediaRepository.restoreFromTrash(it.filePath) }
                                        selectedPaths = emptySet()
                                        showRestoreConfirmDialog = false
                                        Toast.makeText(context, "Media files restored successfully.", Toast.LENGTH_SHORT).show()
                                    }
                                },
                                modifier = Modifier
                                    .weight(1f)
                                    .height(44.dp),
                                shape = RoundedCornerShape(22.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = GalleryBlue,
                                    contentColor = Color.White
                                )
                            ) {
                                Text("Restore", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                            }
                        }
                    }
                }
            }
        }

        // 2. Delete Confirmation Dialog matching dialog_delete.xml
        if (showDeleteConfirmDialog) {
            Dialog(onDismissRequest = { showDeleteConfirmDialog = false }) {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(22.dp)
                    ) {
                        Text(
                            text = "Delete items",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color(0xFF1E3354)
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = "All items will be permanently deleted from your device and cannot be recovered.",
                            fontSize = 14.sp,
                            color = Color(0xFF707C93),
                            lineHeight = 20.sp
                        )
                        Spacer(modifier = Modifier.height(24.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            // Cancel button (matching help_delete_cancel_bg)
                            Button(
                                onClick = { showDeleteConfirmDialog = false },
                                modifier = Modifier
                                    .weight(1f)
                                    .height(44.dp),
                                shape = RoundedCornerShape(22.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color(0xFFF2F4F7),
                                    contentColor = Color(0xFF707C93)
                                )
                            ) {
                                Text("Cancel", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                            }

                            // Delete button (matching permanent delete action)
                            Button(
                                onClick = {
                                    val targets = recycleItems.filter { it.filePath in selectedPaths }
                                    scope.launch {
                                        mediaRepository.deletePermanently(targets.map { it.filePath })
                                        selectedPaths = emptySet()
                                        showDeleteConfirmDialog = false
                                        Toast.makeText(context, "Media files deleted successfully.", Toast.LENGTH_SHORT).show()
                                    }
                                },
                                modifier = Modifier
                                    .weight(1f)
                                    .height(44.dp),
                                shape = RoundedCornerShape(22.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color(0xFFE11A1A),
                                    contentColor = Color.White
                                )
                            ) {
                                Text("Delete", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                            }
                        }
                    }
                }
            }
        }

        // 3. Empty Bin Confirm Dialog
        if (showEmptyBinConfirmDialog) {
            Dialog(onDismissRequest = { showEmptyBinConfirmDialog = false }) {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(22.dp)
                    ) {
                        Text(
                            text = "Empty Recycle Bin",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color(0xFF1E3354)
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = "Are you sure you want to permanently delete all items in the Recycle Bin?",
                            fontSize = 14.sp,
                            color = Color(0xFF707C93),
                            lineHeight = 20.sp
                        )
                        Spacer(modifier = Modifier.height(24.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Button(
                                onClick = { showEmptyBinConfirmDialog = false },
                                modifier = Modifier
                                    .weight(1f)
                                    .height(44.dp),
                                shape = RoundedCornerShape(22.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color(0xFFF2F4F7),
                                    contentColor = Color(0xFF707C93)
                                )
                            ) {
                                Text("Cancel", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                            }

                            Button(
                                onClick = {
                                    scope.launch {
                                        mediaRepository.deletePermanently(recycleItems.map { it.filePath })
                                        showEmptyBinConfirmDialog = false
                                        Toast.makeText(context, "Recycle Bin emptied.", Toast.LENGTH_SHORT).show()
                                    }
                                },
                                modifier = Modifier
                                    .weight(1f)
                                    .height(44.dp),
                                shape = RoundedCornerShape(22.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color(0xFFE11A1A),
                                    contentColor = Color.White
                                )
                            ) {
                                Text("Empty", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                            }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun RecycleGridItem(
    item: MediaItem,
    isSelected: Boolean,
    isSelectionMode: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit
) {
    val context = LocalContext.current
    val remainingDays = calculateRemainingDays(item.deletedDate)

    Box(
        modifier = Modifier
            .aspectRatio(1f)
            .padding(1.5.dp)
            .clip(RoundedCornerShape(3.dp))
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick
            )
    ) {
        AsyncImage(
            model = ImageRequest.Builder(context)
                .data(File(item.filePath))
                .crossfade(true)
                .build(),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
        )

        // Video Duration badge (matching item_grid_recycle_media.xml playBtn + duration)
        if (item.mediaType == MediaType.VIDEO) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(start = 4.dp, bottom = 22.dp)
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_media_video),
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(14.dp)
                )
                Spacer(modifier = Modifier.width(3.dp))
                Text(
                    text = formatDuration(item.durationMs),
                    color = Color.White,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        }

        // Remaining Day Count bar (matching item_grid_recycle_media.xml dayCount)
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .background(Color.Black.copy(alpha = 0.55f))
                .padding(vertical = 3.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "$remainingDays days left",
                color = Color.White,
                fontSize = 10.sp,
                fontWeight = FontWeight.Medium
            )
        }

        // Selection Checkbox overlay (matching item_grid_recycle_media.xml selected)
        if (isSelectionMode) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(if (isSelected) GalleryBlue.copy(alpha = 0.25f) else Color.Transparent)
            )
            Icon(
                painter = painterResource(
                    id = if (isSelected) R.drawable.circle_checked else R.drawable.circle_unchecked
                ),
                contentDescription = if (isSelected) "Selected" else "Unselected",
                tint = if (isSelected) GalleryBlue else Color.White,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(6.dp)
                    .size(22.dp)
            )
        }
    }
}
