package com.quizango.gallery.ui.home

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.MediaStore
import androidx.activity.compose.BackHandler
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import androidx.compose.foundation.Image
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import com.quizango.gallery.R
import com.quizango.gallery.domain.model.MediaItem
import com.quizango.gallery.domain.model.SortDirection
import com.quizango.gallery.domain.model.SortField
import com.quizango.gallery.domain.model.SortOrder
import com.quizango.gallery.ui.components.GalleryBottomNav
import com.quizango.gallery.ui.components.GalleryDrawer
import com.quizango.gallery.ui.components.GalleryTopBar
import com.quizango.gallery.ui.navigation.Screen
import com.quizango.gallery.ui.theme.GalleryBlue
import com.quizango.gallery.ui.theme.settingsDivider
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
    var showMoreMenu by remember { mutableStateOf(false) }
    var newAlbumName by remember { mutableStateOf("") }
    
    BackHandler(enabled = drawerState.isOpen) {
        scope.launch { drawerState.close() }
    }
    
    BackHandler(enabled = uiState.isSelectionMode) {
        viewModel.clearSelection()
    }

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
                GalleryTopBar(
                    title = if (uiState.selectedTab == 2) "Explore" else "GALLERY",
                    onOpenDrawer = {
                        scope.launch { drawerState.open() }
                    },
                    onSearchClick = {
                        onNavigate(Screen.Search.route)
                    },
                    onSortClick = if (uiState.selectedTab == 1) {
                        { showSortDialog = true }
                    } else null,
                    onCameraClick = if (uiState.selectedTab == 0) {
                        {
                            val cameraIntent = Intent(MediaStore.INTENT_ACTION_STILL_IMAGE_CAMERA)
                            if (cameraIntent.resolveActivity(context.packageManager) != null) {
                                context.startActivity(cameraIntent)
                            }
                        }
                    } else null,
                    onAddAlbumClick = null,
                    onProClick = if (uiState.selectedTab != 2) {
                        { onNavigate(Screen.About.route) }
                    } else null,
                    onLockClick = if (uiState.selectedTab != 2) {
                        { onNavigate(Screen.VaultLock.route) }
                    } else null,
                    isSelectionMode = uiState.isSelectionMode,
                    selectedCount = uiState.selectedPaths.size,
                    isAllSelected = uiState.selectedPaths.size == uiState.mediaList.size && uiState.mediaList.isNotEmpty(),
                    onSelectAllClick = {
                        if (uiState.selectedPaths.size == uiState.mediaList.size && uiState.mediaList.isNotEmpty()) {
                            viewModel.clearSelection()
                        } else {
                            viewModel.selectAll()
                        }
                    },
                    onCloseSelection = { viewModel.clearSelection() }
                )
            },
            bottomBar = {
                if (uiState.isSelectionMode) {
                    Surface(
                        shadowElevation = 8.dp,
                        color = MaterialTheme.colorScheme.surface,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .navigationBarsPadding()
                                .horizontalScroll(rememberScrollState())
                                .padding(horizontal = 8.dp, vertical = 6.dp),
                            horizontalArrangement = Arrangement.SpaceEvenly,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            SelectionActionItem(
                                iconRes = R.drawable.ic_home_select_share,
                                label = stringResource(id = R.string.share),
                                onClick = { shareSelectedMedia(context, uiState.selectedPaths) }
                            )
                            SelectionActionItem(
                                iconRes = R.drawable.ic_home_select_fav,
                                label = stringResource(id = R.string.favorite),
                                onClick = { viewModel.toggleFavoriteSelected() }
                            )
                            SelectionActionItem(
                                iconRes = R.drawable.ic_home_select_add,
                                label = stringResource(id = R.string.add_to),
                                onClick = { showCreateAlbumDialog = true }
                            )
                            SelectionActionItem(
                                iconRes = R.drawable.ic_home_select_hide,
                                label = stringResource(id = R.string.ad_to_private),
                                onClick = { viewModel.moveSelectedToVault() }
                            )
                            SelectionActionItem(
                                iconRes = R.drawable.ic_album_select_delete,
                                label = stringResource(id = R.string.delete),
                                onClick = { viewModel.moveSelectedToTrash() }
                            )
                            SelectionActionItem(
                                iconRes = R.drawable.ic_home_select_create_album,
                                label = stringResource(id = R.string.create_albums),
                                onClick = { showCreateAlbumDialog = true }
                            )
                        }
                    }
                } else {
                    GalleryBottomNav(
                        selectedTab = uiState.selectedTab,
                        onTabSelected = { viewModel.setTab(it) }
                    )
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
                        mediaList = uiState.mediaList,
                        onAlbumClick = { album ->
                            onNavigate(Screen.AlbumDetail.createRoute(album.id, album.name))
                        },
                        onStoryClick = { path, title ->
                            onNavigate(Screen.StoryView.createRoute(path, title))
                        },
                        onFeatureClick = { featureId ->
                            when (featureId) {
                                "vault" -> onNavigate(Screen.VaultLock.route)
                                "favorites" -> onNavigate(Screen.Favorites.route)
                                "cleaner" -> onNavigate(Screen.RemoveJunk.route)
                                "duplicates" -> onNavigate(Screen.Duplicates.route)
                                "recycle" -> onNavigate(Screen.RecycleBin.route)
                                "collage" -> onNavigate(Screen.CollageMaker.route)
                                "editor" -> onNavigate(Screen.Editor.createRoute())
                                "videos" -> onNavigate(Screen.AlbumDetail.createRoute("filter:videos", "Videos"))
                                "place" -> onNavigate(Screen.AlbumDetail.createRoute("filter:place", "Geographic photo"))
                                "moments" -> onNavigate(Screen.Moments.route)
                            }
                        },
                        onMediaTypeClick = { typeId ->
                            when (typeId) {
                                "portrait" -> onNavigate(Screen.AlbumDetail.createRoute("filter:portrait", "Portraits"))
                                "high_definition" -> onNavigate(Screen.AlbumDetail.createRoute("filter:hd", "High Definition"))
                                "burst" -> onNavigate(Screen.BurstPhotos.route)
                                "animated" -> onNavigate(Screen.AnimatedMedia.route)
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
                    color = MaterialTheme.colorScheme.onSurface
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
                                color = if (tempField == field) GalleryBlue else MaterialTheme.colorScheme.onSurface,
                                fontWeight = if (tempField == field) FontWeight.SemiBold else FontWeight.Normal,
                                modifier = Modifier.padding(start = 8.dp)
                            )
                        }
                    }

                    HorizontalDivider(
                        color = MaterialTheme.colorScheme.settingsDivider,
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
                                color = if (tempDirection == direction) GalleryBlue else MaterialTheme.colorScheme.onSurface,
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
                    Text("Cancel", color = MaterialTheme.colorScheme.onSurfaceVariant, fontWeight = FontWeight.Medium)
                }
            },
            containerColor = MaterialTheme.colorScheme.surface,
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
                    color = MaterialTheme.colorScheme.onSurface
                )
            },
            text = {
                Column {
                    Text(
                        text = "Album name",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(bottom = 6.dp)
                    )
                    OutlinedTextField(
                        value = newAlbumName,
                        onValueChange = { newAlbumName = it },
                        placeholder = { Text("Enter album name") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = GalleryBlue,
                            unfocusedBorderColor = MaterialTheme.colorScheme.settingsDivider,
                            cursorColor = GalleryBlue,
                            focusedTextColor = MaterialTheme.colorScheme.onSurface,
                            unfocusedTextColor = MaterialTheme.colorScheme.onSurface
                        )
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
                    Text("Cancel", color = MaterialTheme.colorScheme.onSurfaceVariant, fontWeight = FontWeight.Medium)
                }
            },
            containerColor = MaterialTheme.colorScheme.surface,
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

@Composable
private fun SelectionActionItem(
    iconRes: Int,
    label: String,
    onClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 8.dp, vertical = 6.dp)
    ) {
        Image(
            painter = painterResource(id = iconRes),
            contentDescription = label,
            modifier = Modifier.size(24.dp)
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = label,
            fontSize = 11.sp,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1
        )
    }
}
