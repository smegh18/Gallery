package albums.gallery.photo.folder.picasa.app.web.gallery.ui.album

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import albums.gallery.photo.folder.picasa.app.web.gallery.data.repository.MediaRepository
import albums.gallery.photo.folder.picasa.app.web.gallery.data.repository.VaultRepository
import albums.gallery.photo.folder.picasa.app.web.gallery.domain.model.MediaItem
import albums.gallery.photo.folder.picasa.app.web.gallery.ui.components.GalleryTopBar
import albums.gallery.photo.folder.picasa.app.web.gallery.ui.home.PhotoGridItem
import albums.gallery.photo.folder.picasa.app.web.gallery.ui.navigation.Screen
import albums.gallery.photo.folder.picasa.app.web.gallery.ui.theme.GalleryBlue
import kotlinx.coroutines.launch

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

    Scaffold(
        topBar = {
            GalleryTopBar(
                title = bucketName,
                canNavigateBack = true,
                onNavigateBack = onNavigateBack,
                isSelectionMode = isSelectionMode,
                selectedCount = selectedPaths.size,
                onSelectAllClick = {
                    selectedPaths = mediaItems.map { it.filePath }.toSet()
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
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 24.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceAround,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(onClick = {
                            // Share
                        }) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(Icons.Default.Share, contentDescription = "Share", tint = GalleryBlue)
                                Text("Share", style = MaterialTheme.typography.labelSmall)
                            }
                        }
                        IconButton(onClick = {
                            scope.launch {
                                selectedPaths.forEach { path ->
                                    mediaRepository.moveToTrash(path)
                                }
                                selectedPaths = emptySet()
                            }
                        }) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(Icons.Default.Delete, contentDescription = "Delete", tint = MaterialTheme.colorScheme.error)
                                Text("Delete", style = MaterialTheme.typography.labelSmall)
                            }
                        }
                        IconButton(onClick = {
                            scope.launch {
                                val toVault = mediaItems.filter { it.filePath in selectedPaths }
                                toVault.forEach { vaultRepository.moveToVault(it) }
                                selectedPaths = emptySet()
                            }
                        }) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(Icons.Default.Lock, contentDescription = "Vault", tint = GalleryBlue)
                                Text("Vault", style = MaterialTheme.typography.labelSmall)
                            }
                        }
                    }
                }
            }
        }
    ) { paddingValues ->
        if (mediaItems.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Album is Empty",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            LazyVerticalGrid(
                columns = GridCells.Fixed(4),
                contentPadding = PaddingValues(2.dp),
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
            ) {
                items(mediaItems, key = { it.filePath }) { item ->
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
