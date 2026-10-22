package albums.gallery.photo.folder.picasa.app.web.gallery.ui.home

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.MediaStore
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import albums.gallery.photo.folder.picasa.app.web.gallery.data.ads.AdConstants
import albums.gallery.photo.folder.picasa.app.web.gallery.data.ads.AdManager
import albums.gallery.photo.folder.picasa.app.web.gallery.domain.model.Album
import albums.gallery.photo.folder.picasa.app.web.gallery.domain.model.MediaItem
import albums.gallery.photo.folder.picasa.app.web.gallery.domain.model.SortOrder
import albums.gallery.photo.folder.picasa.app.web.gallery.ui.components.BannerAdView
import albums.gallery.photo.folder.picasa.app.web.gallery.ui.components.GalleryBottomNav
import albums.gallery.photo.folder.picasa.app.web.gallery.ui.components.GalleryDrawer
import albums.gallery.photo.folder.picasa.app.web.gallery.ui.components.GalleryTopBar
import albums.gallery.photo.folder.picasa.app.web.gallery.ui.navigation.Screen
import albums.gallery.photo.folder.picasa.app.web.gallery.ui.theme.GalleryBlue
import kotlinx.coroutines.launch
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    viewModel: HomeViewModel,
    onNavigate: (String) -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    var showSortDialog by remember { mutableStateOf(false) }
    var showCreateAlbumDialog by remember { mutableStateOf(false) }
    var newAlbumName by remember { mutableStateOf("") }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            GalleryDrawer(
                onNavigate = { route ->
                    when (route) {
                        "favorites" -> onNavigate(Screen.Favorites.route)
                        "recycle" -> onNavigate(Screen.RecycleBin.route)
                        "duplicates" -> onNavigate(Screen.Duplicates.route)
                        "vault" -> onNavigate(Screen.VaultLock.route)
                        "cleaner" -> onNavigate(Screen.Cleaner.route)
                        "editor" -> onNavigate(Screen.Editor.createRoute())
                        "language" -> onNavigate(Screen.Language.route)
                        "theme" -> onNavigate(Screen.Theme.route)
                        "settings" -> onNavigate(Screen.Settings.route)
                    }
                },
                onCreateAlbumClick = {
                    showCreateAlbumDialog = true
                },
                onCloseDrawer = {
                    scope.launch { drawerState.close() }
                }
            )
        }
    ) {
        Scaffold(
            topBar = {
                val title = when (uiState.selectedTab) {
                    0 -> "Albums"
                    1 -> "Photos"
                    else -> "Explore"
                }

                GalleryTopBar(
                    title = title,
                    onOpenDrawer = {
                        scope.launch { drawerState.open() }
                    },
                    onSearchClick = {
                        onNavigate(Screen.Search.route)
                    },
                    onLockClick = {
                        onNavigate(Screen.VaultLock.route)
                    },
                    onProClick = {
                        // Pro feature
                    },
                    onSortClick = if (uiState.selectedTab == 1) { { showSortDialog = true } } else null,
                    onCameraClick = {
                        val cameraIntent = Intent(MediaStore.INTENT_ACTION_STILL_IMAGE_CAMERA)
                        if (cameraIntent.resolveActivity(context.packageManager) != null) {
                            context.startActivity(cameraIntent)
                        }
                    },
                    isSelectionMode = uiState.isSelectionMode,
                    selectedCount = uiState.selectedPaths.size,
                    onSelectAllClick = { viewModel.selectAll() },
                    onCloseSelection = { viewModel.clearSelection() }
                )
            },
            bottomBar = {
                if (uiState.isSelectionMode) {
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
                                shareSelectedMedia(context, uiState.selectedPaths)
                            }) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Icon(Icons.Default.Share, contentDescription = "Share", tint = GalleryBlue)
                                    Text("Share", style = MaterialTheme.typography.labelSmall)
                                }
                            }
                            IconButton(onClick = {
                                viewModel.moveSelectedToTrash()
                            }) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Icon(Icons.Default.Delete, contentDescription = "Delete", tint = MaterialTheme.colorScheme.error)
                                    Text("Delete", style = MaterialTheme.typography.labelSmall)
                                }
                            }
                            IconButton(onClick = {
                                viewModel.moveSelectedToVault()
                            }) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Icon(Icons.Default.Lock, contentDescription = "Vault", tint = GalleryBlue)
                                    Text("Vault", style = MaterialTheme.typography.labelSmall)
                                }
                            }
                        }
                    }
                } else {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        BannerAdView(adUnitId = AdConstants.TEST_BANNER_ID)
                        GalleryBottomNav(
                            selectedTab = uiState.selectedTab,
                            onTabSelected = { viewModel.setTab(it) }
                        )
                    }
                }
            }
        ) { paddingValues ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
            ) {
                when (uiState.selectedTab) {
                    0 -> AlbumsTab(
                        albumsList = uiState.albumsList,
                        onAlbumClick = { album ->
                            val activity = context as? Activity
                            if (activity != null) {
                                AdManager.showInterstitial(activity, AdConstants.EVENT_ALBUM_CLICK) {
                                    onNavigate(Screen.AlbumDetail.createRoute(album.id, album.name))
                                }
                            } else {
                                onNavigate(Screen.AlbumDetail.createRoute(album.id, album.name))
                            }
                        },
                        onFeatureClick = { featureId ->
                            when (featureId) {
                                "vault" -> onNavigate(Screen.VaultLock.route)
                                "favorites" -> onNavigate(Screen.Favorites.route)
                                "cleaner" -> onNavigate(Screen.Cleaner.route)
                                "duplicates" -> onNavigate(Screen.Duplicates.route)
                                "recycle" -> onNavigate(Screen.RecycleBin.route)
                                "collage", "editor" -> onNavigate(Screen.Editor.createRoute())
                                "videos" -> onNavigate(Screen.AlbumDetail.createRoute("filter:videos", "Videos"))
                                "place" -> onNavigate(Screen.AlbumDetail.createRoute("filter:place", "Places"))
                                "moments" -> onNavigate(Screen.AlbumDetail.createRoute("filter:moments", "Moments"))
                            }
                        },
                        onMediaTypeClick = { typeId ->
                            when (typeId) {
                                "portrait" -> onNavigate(Screen.AlbumDetail.createRoute("filter:portrait", "Portraits"))
                                "high_definition" -> onNavigate(Screen.AlbumDetail.createRoute("filter:hd", "High Definition"))
                                "burst" -> onNavigate(Screen.AlbumDetail.createRoute("filter:burst", "Burst"))
                                "animated" -> onNavigate(Screen.AlbumDetail.createRoute("filter:animated", "Animated"))
                                else -> viewModel.setTab(1)
                            }
                        }
                    )
                    1 -> PhotosTab(
                        mediaList = uiState.mediaList,
                        columnCount = uiState.columnCount,
                        selectedPaths = uiState.selectedPaths,
                        isSelectionMode = uiState.isSelectionMode,
                        onMediaClick = { item ->
                            onNavigate(Screen.MediaViewer.createRoute(item.filePath, ""))
                        },
                        onMediaLongClick = { item ->
                            viewModel.toggleSelection(item.filePath)
                        },
                        onRefresh = { viewModel.syncMedia() }
                    )
                    2 -> ExploreTab(
                        onNavigate = onNavigate
                    )
                }

                if (uiState.isSyncing) {
                    LinearProgressIndicator(
                        modifier = Modifier
                            .fillMaxWidth()
                            .align(Alignment.TopCenter),
                        color = GalleryBlue
                    )
                }
            }
        }
    }

    if (showSortDialog) {
        AlertDialog(
            onDismissRequest = { showSortDialog = false },
            title = { Text("Sort Media By") },
            text = {
                Column {
                    val options = listOf(
                        "Date (Newest first)" to SortOrder.DATE_DESC,
                        "Date (Oldest first)" to SortOrder.DATE_ASC,
                        "Name (A-Z)" to SortOrder.NAME_ASC,
                        "Name (Z-A)" to SortOrder.NAME_DESC,
                        "Size (Largest first)" to SortOrder.SIZE_DESC,
                        "Size (Smallest first)" to SortOrder.SIZE_ASC
                    )
                    options.forEach { (label, order) ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp)
                        ) {
                            RadioButton(
                                selected = uiState.sortOrder == order,
                                onClick = {
                                    viewModel.setSortOrder(order)
                                    showSortDialog = false
                                }
                            )
                            Text(
                                text = label,
                                style = MaterialTheme.typography.bodyMedium,
                                modifier = Modifier.padding(start = 8.dp)
                            )
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showSortDialog = false }) {
                    Text("Close")
                }
            }
        )
    }

    if (showCreateAlbumDialog) {
        AlertDialog(
            onDismissRequest = {
                showCreateAlbumDialog = false
                newAlbumName = ""
            },
            title = {
                Text(
                    text = "Create an album",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF1E3354)
                )
            },
            text = {
                Column {
                    Text(
                        text = "Album name",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFF707C93),
                        modifier = Modifier.padding(bottom = 6.dp)
                    )
                    OutlinedTextField(
                        value = newAlbumName,
                        onValueChange = { newAlbumName = it },
                        placeholder = { Text("Enter album name") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp)
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val trimmed = newAlbumName.trim()
                        if (trimmed.isNotEmpty()) {
                            try {
                                val picturesDir = android.os.Environment.getExternalStoragePublicDirectory(android.os.Environment.DIRECTORY_PICTURES)
                                val newDir = File(picturesDir, trimmed)
                                if (!newDir.exists()) {
                                    newDir.mkdirs()
                                }
                                viewModel.syncMedia()
                            } catch (_: Exception) {}
                        }
                        showCreateAlbumDialog = false
                        newAlbumName = ""
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = GalleryBlue),
                    shape = RoundedCornerShape(20.dp)
                ) {
                    Text("OK", color = Color.White, fontWeight = FontWeight.SemiBold)
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    showCreateAlbumDialog = false
                    newAlbumName = ""
                }) {
                    Text("Cancel", color = Color(0xFF707C93), fontWeight = FontWeight.Medium)
                }
            },
            containerColor = Color.White,
            shape = RoundedCornerShape(16.dp)
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
                val uri = FileProvider.getUriForFile(
                    context,
                    "${context.packageName}.provider",
                    file
                )
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
