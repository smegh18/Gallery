package com.quizango.gallery.ui.album

import com.quizango.gallery.ui.theme.isDark
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Environment
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.quizango.gallery.R
import com.quizango.gallery.data.repository.MediaRepository
import com.quizango.gallery.data.repository.VaultRepository
import com.quizango.gallery.domain.model.Album
import com.quizango.gallery.domain.model.MediaItem
import com.quizango.gallery.ui.components.GalleryTopBar
import com.quizango.gallery.ui.home.PhotoGridItem
import com.quizango.gallery.ui.home.formatGroupHeader
import com.quizango.gallery.ui.navigation.Screen
import com.quizango.gallery.ui.theme.GalleryBlue
import com.quizango.gallery.ui.theme.cardBackground
import com.quizango.gallery.ui.theme.cardBorder
import kotlinx.coroutines.launch
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AlbumDetailScreen(
    bucketId: String,
    bucketName: String,
    mediaRepository: MediaRepository,
    vaultRepository: VaultRepository,
    onNavigate: (String) -> Unit,
    onNavigateBack: () -> Unit
) {
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val mediaItems by mediaRepository.getMediaByFolderFlow(bucketId).collectAsState(initial = emptyList())
    var selectedPaths by remember { mutableStateOf(setOf<String>()) }
    val isSelectionMode = selectedPaths.isNotEmpty()

    var showDeleteDialog by remember { mutableStateOf(false) }
    var deleteMoveToBin by remember { mutableStateOf(true) }
    var showAddToDialog by remember { mutableStateOf(false) }
    var showCreateAlbumDialog by remember { mutableStateOf(false) }
    var newAlbumName by remember { mutableStateOf("") }
    val albums by mediaRepository.getAlbumsFlow().collectAsState(initial = emptyList())

    Scaffold(
        topBar = {
            GalleryTopBar(
                title = bucketName,
                canNavigateBack = true,
                onNavigateBack = onNavigateBack,
                onSearchClick = { onNavigate(Screen.Search.route) },
                isSelectionMode = isSelectionMode,
                selectedCount = selectedPaths.size,
                onSelectAllClick = {
                    selectedPaths = if (selectedPaths.size == mediaItems.size) {
                        emptySet()
                    } else {
                        mediaItems.map { it.filePath }.toSet()
                    }
                },
                onCloseSelection = {
                    selectedPaths = emptySet()
                }
            )
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
                    val scrollState = rememberScrollState()
                    val anyNotFav = remember(selectedPaths, mediaItems) {
                        mediaItems.any { it.filePath in selectedPaths && !it.isFavorite }
                    }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(scrollState)
                            .padding(horizontal = 8.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceAround,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // 1. Share (ic_home_select_share)
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier
                                .width(56.dp)
                                .clickable { shareSelectedMedia(context, selectedPaths) }
                                .padding(vertical = 4.dp)
                        ) {
                            Icon(
                                painter = painterResource(id = R.drawable.ic_home_select_share),
                                contentDescription = "Share",
                                tint = Color.Unspecified,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Share",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }

                        // 2. Favorite (ic_home_select_fav / ic_fav_fill)
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier
                                .width(56.dp)
                                .clickable {
                                    scope.launch {
                                        selectedPaths.forEach { path ->
                                            mediaRepository.setFavorite(path, anyNotFav)
                                        }
                                        val msg = if (anyNotFav) "Added to Favorites" else "Removed from Favorites"
                                        Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                                        selectedPaths = emptySet()
                                    }
                                }
                                .padding(vertical = 4.dp)
                        ) {
                            Icon(
                                painter = painterResource(
                                    id = if (anyNotFav) R.drawable.ic_home_select_fav else R.drawable.ic_fav_fill
                                ),
                                contentDescription = if (anyNotFav) "Favorite" else "Unfavorite",
                                tint = Color.Unspecified,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = if (anyNotFav) "Favorite" else "Unfavorite",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }

                        // 3. Add to (ic_home_select_add)
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier
                                .width(56.dp)
                                .clickable { showAddToDialog = true }
                                .padding(vertical = 4.dp)
                        ) {
                            Icon(
                                painter = painterResource(id = R.drawable.ic_home_select_add),
                                contentDescription = "Add to",
                                tint = Color.Unspecified,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Add to",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }

                        // 4. Add to Private (Vault) (ic_home_select_hide)
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier
                                .width(70.dp)
                                .clickable {
                                    scope.launch {
                                        val toVault = mediaItems.filter { it.filePath in selectedPaths }
                                        toVault.forEach { vaultRepository.moveToVault(it) }
                                        mediaRepository.syncMediaStore()
                                        Toast.makeText(context, "Added to Private Vault", Toast.LENGTH_SHORT).show()
                                        selectedPaths = emptySet()
                                    }
                                }
                                .padding(vertical = 4.dp)
                        ) {
                            Icon(
                                painter = painterResource(id = R.drawable.ic_home_select_hide),
                                contentDescription = "Add to Private",
                                tint = Color.Unspecified,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Add to Private",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }

                        // 5. Delete (ic_album_select_delete)
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier
                                .width(56.dp)
                                .clickable { showDeleteDialog = true }
                                .padding(vertical = 4.dp)
                        ) {
                            Icon(
                                painter = painterResource(id = R.drawable.ic_album_select_delete),
                                contentDescription = "Delete",
                                tint = Color.Unspecified,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Delete",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }

                        // 6. Create album (ic_home_select_create_album)
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier
                                .width(70.dp)
                                .clickable {
                                    newAlbumName = ""
                                    showCreateAlbumDialog = true
                                }
                                .padding(vertical = 4.dp)
                        ) {
                            Icon(
                                painter = painterResource(id = R.drawable.ic_home_select_create_album),
                                contentDescription = "Create album",
                                tint = Color.Unspecified,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Create album",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
            }
        }
    ) { paddingValues ->
        if (mediaItems.isEmpty()) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_empty_img),
                    contentDescription = null,
                    tint = Color.Unspecified,
                    modifier = Modifier.size(80.dp)
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "No items",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onBackground,
                    fontSize = 18.sp
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "No media files here.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 14.sp
                )
            }
        } else {
            val grouped by produceState(emptyMap<String, List<MediaItem>>(), mediaItems) {
                value = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Default) {
                    mediaItems.groupBy { formatGroupHeader(it.dateAdded) }
                }
            }

            LazyVerticalGrid(
                columns = GridCells.Fixed(4),
                contentPadding = PaddingValues(2.dp),
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
            ) {
                grouped.forEach { (header, items) ->
                    item(span = { GridItemSpan(4) }) {
                        Text(
                            text = header,
                            // Reference date headers: regular weight, black in light mode
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Normal,
                            color = if (MaterialTheme.colorScheme.isDark) Color.White else Color.Black,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 8.dp, vertical = 8.dp)
                        )
                    }

                    items(items, key = { it.filePath }) { item ->
                        val isSelected = selectedPaths.contains(item.filePath)
                        PhotoGridItem(
                            item = item,
                            isSelected = isSelected,
                            isSelectionMode = isSelectionMode,
                            onClick = {
                                if (isSelectionMode) {
                                    selectedPaths = if (isSelected) selectedPaths - item.filePath else selectedPaths + item.filePath
                                } else {
                                    onNavigate(Screen.MediaViewer.createRoute(item.filePath, bucketId))
                                }
                            },
                            onLongClick = {
                                selectedPaths = if (isSelected) selectedPaths - item.filePath else selectedPaths + item.filePath
                            }
                        )
                    }
                }
            }
        }
    }

    // Delete Confirmation Dialog (matching dialog_delete.xml)
    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            shape = RoundedCornerShape(16.dp),
            title = {
                Text(
                    text = "Are you sure you want to delete ${selectedPaths.size} item(s)?",
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 16.sp,
                    color = MaterialTheme.colorScheme.onSurface
                )
            },
            text = {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { deleteMoveToBin = !deleteMoveToBin }
                ) {
                    Checkbox(
                        checked = deleteMoveToBin,
                        onCheckedChange = { deleteMoveToBin = it },
                        colors = CheckboxDefaults.colors(checkedColor = GalleryBlue)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Move this file to Recycle Bin",
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            },
            confirmButton = {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = { showDeleteDialog = false },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(24.dp),
                        border = BorderStroke(1.dp, GalleryBlue)
                    ) {
                        Text("No", color = GalleryBlue, fontWeight = FontWeight.SemiBold)
                    }
                    Button(
                        onClick = {
                            showDeleteDialog = false
                            scope.launch {
                                selectedPaths.forEach { path ->
                                    if (deleteMoveToBin) {
                                        mediaRepository.moveToTrash(path)
                                    } else {
                                        mediaRepository.deletePermanently(path)
                                    }
                                }
                                val msg = if (deleteMoveToBin) "Moved to Recycle Bin" else "Deleted permanently"
                                Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                                selectedPaths = emptySet()
                            }
                        },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(24.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = GalleryBlue)
                    ) {
                        Text("Yes", color = Color.White, fontWeight = FontWeight.SemiBold)
                    }
                }
            },
            dismissButton = {}
        )
    }

    // Add to Album Bottom Sheet (matching dialog_select_album.xml)
    if (showAddToDialog) {
        val firstSelectedItem = mediaItems.firstOrNull { it.filePath in selectedPaths }
        ModalBottomSheet(
            onDismissRequest = { showAddToDialog = false },
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
            containerColor = MaterialTheme.colorScheme.surface,
            shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp),
            dragHandle = {
                Box(
                    modifier = Modifier
                        .padding(vertical = 12.dp)
                        .width(40.dp)
                        .height(4.dp)
                        .background(Color(0xFFD9D9D9), RoundedCornerShape(2.dp))
                )
            }
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 32.dp)
            ) {
                // Header with thumbnail preview and count badge + title "Add to"
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    Box(
                        modifier = Modifier.size(44.dp)
                    ) {
                        if (firstSelectedItem != null) {
                            AsyncImage(
                                model = ImageRequest.Builder(context)
                                    .data(File(firstSelectedItem.filePath))
                                    .size(120, 120)
                                    .allowRgb565(true)
                                    .build(),
                                contentDescription = null,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier
                                    .size(38.dp)
                                    .align(Alignment.BottomStart)
                                    .background(Color.LightGray, RoundedCornerShape(4.dp))
                            )
                        } else {
                            Box(
                                modifier = Modifier
                                    .size(38.dp)
                                    .align(Alignment.BottomStart)
                                    .background(Color.LightGray, RoundedCornerShape(4.dp))
                            )
                        }
                        Box(
                            modifier = Modifier
                                .size(20.dp)
                                .align(Alignment.TopEnd)
                                .background(GalleryBlue, RoundedCornerShape(10.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "${selectedPaths.size}",
                                color = Color.White,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = "Add to",
                        fontSize = 17.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                if (albums.isEmpty()) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(32.dp)
                    ) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_empty_img),
                            contentDescription = null,
                            tint = Color.Unspecified,
                            modifier = Modifier.size(56.dp)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "No albums",
                            color = MaterialTheme.colorScheme.onSurface,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 15.sp
                        )
                    }
                } else {
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(3),
                        contentPadding = PaddingValues(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 340.dp)
                    ) {
                        items(albums, key = { it.id }) { album ->
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier
                                    .padding(6.dp)
                                    .clickable {
                                        showAddToDialog = false
                                        scope.launch {
                                            selectedPaths.forEach { path ->
                                                mediaRepository.copyMediaToFolder(path, album.path)
                                            }
                                            mediaRepository.syncMediaStore()
                                            Toast.makeText(context, "Added to ${album.name}", Toast.LENGTH_SHORT).show()
                                            selectedPaths = emptySet()
                                        }
                                    }
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(72.dp)
                                        .background(MaterialTheme.colorScheme.cardBackground, RoundedCornerShape(12.dp)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        painter = painterResource(id = R.drawable.ic_albums_fill),
                                        contentDescription = album.name,
                                        tint = GalleryBlue,
                                        modifier = Modifier.size(36.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = album.name,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    maxLines = 1
                                )
                                Text(
                                    text = "${album.itemCount} items",
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    // Create Album Dialog (matching dialog_create_album.xml)
    if (showCreateAlbumDialog) {
        AlertDialog(
            onDismissRequest = { showCreateAlbumDialog = false },
            shape = RoundedCornerShape(16.dp),
            title = {
                Text(
                    text = "Create an Album",
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 18.sp,
                    color = MaterialTheme.colorScheme.onSurface
                )
            },
            text = {
                Column {
                    Text(
                        text = "Album Name",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    OutlinedTextField(
                        value = newAlbumName,
                        onValueChange = { newAlbumName = it },
                        placeholder = { Text("Enter album name", color = Color(0xFFB0B7C3), fontSize = 14.sp) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = GalleryBlue,
                            unfocusedBorderColor = MaterialTheme.colorScheme.cardBorder,
                            cursorColor = GalleryBlue,
                            focusedTextColor = MaterialTheme.colorScheme.onSurface,
                            unfocusedTextColor = MaterialTheme.colorScheme.onSurface
                        )
                    )
                }
            },
            confirmButton = {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = { showCreateAlbumDialog = false },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(24.dp),
                        border = BorderStroke(1.dp, GalleryBlue)
                    ) {
                        Text("Cancel", color = GalleryBlue, fontWeight = FontWeight.SemiBold)
                    }
                    Button(
                        onClick = {
                            if (newAlbumName.isNotBlank()) {
                                val targetName = newAlbumName.trim()
                                showCreateAlbumDialog = false
                                scope.launch {
                                    val picturesDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES)
                                    val newDir = File(picturesDir, targetName).also { if (!it.exists()) it.mkdirs() }
                                    selectedPaths.forEach { path ->
                                        mediaRepository.copyMediaToFolder(path, newDir.absolutePath)
                                    }
                                    mediaRepository.syncMediaStore()
                                    Toast.makeText(context, "Album '$targetName' created", Toast.LENGTH_SHORT).show()
                                    selectedPaths = emptySet()
                                }
                            }
                        },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(24.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = GalleryBlue)
                    ) {
                        Text("OK", color = Color.White, fontWeight = FontWeight.SemiBold)
                    }
                }
            },
            dismissButton = {}
        )
    }
}

private fun shareSelectedMedia(context: Context, paths: Set<String>) {
    if (paths.isEmpty()) return
    try {
        val uris = ArrayList<Uri>()
        paths.forEach { path ->
            val file = File(path)
            if (file.exists()) {
                val uri = FileProvider.getUriForFile(context, "${context.packageName}.provider", file)
                uris.add(uri)
            }
        }
        val shareIntent = if (uris.size == 1) {
            Intent(Intent.ACTION_SEND).apply {
                type = "*/*"
                putExtra(Intent.EXTRA_STREAM, uris.first())
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
        } else {
            Intent(Intent.ACTION_SEND_MULTIPLE).apply {
                type = "*/*"
                putParcelableArrayListExtra(Intent.EXTRA_STREAM, uris)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
        }
        context.startActivity(Intent.createChooser(shareIntent, "Share Media"))
    } catch (_: Exception) {}
}
