package albums.gallery.photo.folder.picasa.app.web.gallery.ui.album

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import albums.gallery.photo.folder.picasa.app.web.gallery.R
import albums.gallery.photo.folder.picasa.app.web.gallery.data.ads.AdConstants
import albums.gallery.photo.folder.picasa.app.web.gallery.data.repository.MediaRepository
import albums.gallery.photo.folder.picasa.app.web.gallery.data.repository.VaultRepository
import albums.gallery.photo.folder.picasa.app.web.gallery.domain.model.MediaItem
import albums.gallery.photo.folder.picasa.app.web.gallery.ui.components.BannerAdView
import albums.gallery.photo.folder.picasa.app.web.gallery.ui.components.GalleryTopBar
import albums.gallery.photo.folder.picasa.app.web.gallery.ui.home.PhotoGridItem
import albums.gallery.photo.folder.picasa.app.web.gallery.ui.navigation.Screen
import albums.gallery.photo.folder.picasa.app.web.gallery.ui.theme.GalleryBlue
import kotlinx.coroutines.launch
import java.io.File

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
                            shareSelectedMedia(context, selectedPaths)
                        }) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(
                                    painter = painterResource(id = R.drawable.ic_share),
                                    contentDescription = "Share",
                                    tint = GalleryBlue,
                                    modifier = Modifier.size(20.dp)
                                )
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
                                Icon(
                                    painter = painterResource(id = R.drawable.ic_delete),
                                    contentDescription = "Delete",
                                    tint = MaterialTheme.colorScheme.error,
                                    modifier = Modifier.size(20.dp)
                                )
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
                                Icon(
                                    painter = painterResource(id = R.drawable.ic_hide),
                                    contentDescription = "Vault",
                                    tint = GalleryBlue,
                                    modifier = Modifier.size(20.dp)
                                )
                                Text("Vault", style = MaterialTheme.typography.labelSmall)
                            }
                        }
                    }
                }
            } else {
                BannerAdView(adUnitId = AdConstants.TEST_BANNER_ID)
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
                    modifier = Modifier.size(64.dp)
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "No Items",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFF1E3354)
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "There is no media file available",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFF707C93)
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
