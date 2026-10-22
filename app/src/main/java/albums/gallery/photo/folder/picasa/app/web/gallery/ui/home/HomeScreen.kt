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
import albums.gallery.photo.folder.picasa.app.web.gallery.domain.model.SortDirection
import albums.gallery.photo.folder.picasa.app.web.gallery.domain.model.SortField
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
                        "about" -> onNavigate(Screen.About.route)
                        "games", "more_apps" -> {
                            val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store/apps"))
                            try { context.startActivity(intent) } catch (_: Exception) {}
                        }
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
                    onCameraClick = if (uiState.selectedTab == 0) {
                        {
                            val cameraIntent = Intent(MediaStore.INTENT_ACTION_STILL_IMAGE_CAMERA)
                            if (cameraIntent.resolveActivity(context.packageManager) != null) {
                                context.startActivity(cameraIntent)
                            }
                        }
                    } else null,
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
        var tempField by remember { mutableStateOf(uiState.sortOrder.field) }
        var tempDirection by remember { mutableStateOf(uiState.sortOrder.direction) }

        AlertDialog(
            onDismissRequest = { showSortDialog = false },
            title = {
                Text(
                    text = "Sort by",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF1E3354)
                )
            },
            text = {
                Column {
                    val fieldOptions = listOf(
                        "Last Modified" to SortField.DATE_MODIFIED,
                        "File Name" to SortField.NAME,
                        "File Size" to SortField.SIZE,
                        "Created Date" to SortField.DATE_TAKEN
                    )
                    fieldOptions.forEach { (label, field) ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { tempField = field }
                                .padding(vertical = 2.dp)
                        ) {
                            RadioButton(
                                selected = tempField == field,
                                onClick = { tempField = field },
                                colors = RadioButtonDefaults.colors(selectedColor = GalleryBlue)
                            )
                            Text(
                                text = label,
                                style = MaterialTheme.typography.bodyMedium,
                                color = if (tempField == field) GalleryBlue else Color(0xFF1E3354),
                                fontWeight = if (tempField == field) FontWeight.SemiBold else FontWeight.Normal,
                                modifier = Modifier.padding(start = 8.dp)
                            )
                        }
                    }

                    HorizontalDivider(
                        color = Color(0xFFE4E6E9),
                        thickness = 1.dp,
                        modifier = Modifier.padding(vertical = 6.dp)
                    )

                    val directionOptions = listOf(
                        "Ascending Order" to SortDirection.ASCENDING,
                        "Descending Order" to SortDirection.DESCENDING
                    )
                    directionOptions.forEach { (label, direction) ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { tempDirection = direction }
                                .padding(vertical = 2.dp)
                        ) {
                            RadioButton(
                                selected = tempDirection == direction,
                                onClick = { tempDirection = direction },
                                colors = RadioButtonDefaults.colors(selectedColor = GalleryBlue)
                            )
                            Text(
                                text = label,
                                style = MaterialTheme.typography.bodyMedium,
                                color = if (tempDirection == direction) GalleryBlue else Color(0xFF1E3354),
                                fontWeight = if (tempDirection == direction) FontWeight.SemiBold else FontWeight.Normal,
                                modifier = Modifier.padding(start = 8.dp)
                            )
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.setSortOrder(SortOrder(tempField, tempDirection))
                        showSortDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = GalleryBlue),
                    shape = RoundedCornerShape(20.dp)
                ) {
                    Text("Ok", color = Color.White, fontWeight = FontWeight.SemiBold)
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showSortDialog = false }
                ) {
                    Text("Cancel", color = Color(0xFF707C93), fontWeight = FontWeight.Medium)
                }
            },
            containerColor = Color.White,
            shape = RoundedCornerShape(16.dp)
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
