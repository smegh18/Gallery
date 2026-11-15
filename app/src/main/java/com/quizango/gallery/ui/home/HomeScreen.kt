package com.quizango.gallery.ui.home

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.MediaStore
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.stringResource
import com.quizango.gallery.R
import com.quizango.gallery.domain.model.MediaItem
import com.quizango.gallery.domain.model.SortDirection
import com.quizango.gallery.domain.model.SortField
import com.quizango.gallery.domain.model.SortOrder
import com.quizango.gallery.ui.components.AddToAlbumSheet
import com.quizango.gallery.ui.navigation.Screen
import com.quizango.gallery.ui.theme.GalleryBlue
import com.quizango.gallery.ui.theme.GalleryText
import com.quizango.gallery.ui.theme.settingsDivider
import kotlinx.coroutines.launch
import java.io.File

@Composable
fun HomeScreen(
    viewModel: HomeViewModel,
    onNavigate: (String) -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    var showSortDialog by remember { mutableStateOf(false) }
    var showCreateAlbumDialog by remember { mutableStateOf(false) }
    var newAlbumName by remember { mutableStateOf("") }
    var showDeleteDialog by remember { mutableStateOf(false) }
    var showAddToSheet by remember { mutableStateOf(false) }
    val shareTitle = stringResource(R.string.share)
    val copiedMessage = stringResource(R.string.items_copied_successfully)
    val onCopied: (Int) -> Unit = { Toast.makeText(context, copiedMessage, Toast.LENGTH_SHORT).show() }

    BackHandler(enabled = uiState.isSelectionMode) {
        viewModel.clearSelection()
    }

    Scaffold(
        topBar = {
            if (uiState.isSelectionMode) {
                val allSelected = uiState.selectedPaths.size == uiState.mediaList.size && uiState.mediaList.isNotEmpty()
                HomeSelectionTopBar(
                    count = uiState.selectedPaths.size,
                    allSelected = allSelected,
                    onClose = { viewModel.clearSelection() },
                    onToggleAll = { if (allSelected) viewModel.clearSelection() else viewModel.selectAll() }
                )
            } else {
                HomeTopBar(
                    page = uiState.selectedTab,
                    onSearch = { onNavigate(Screen.Search.route) },
                    onCamera = {
                        val cameraIntent = Intent(MediaStore.INTENT_ACTION_STILL_IMAGE_CAMERA)
                        if (cameraIntent.resolveActivity(context.packageManager) != null) {
                            context.startActivity(cameraIntent)
                        }
                    },
                    onAddAlbum = { showCreateAlbumDialog = true },
                    onMoreRow = { row ->
                        when (row) {
                            MoreMenuRow.SORT -> showSortDialog = true
                            MoreMenuRow.RECYCLE_BIN -> onNavigate(Screen.RecycleBin.route)
                            MoreMenuRow.SETTINGS -> onNavigate(Screen.Settings.route)
                        }
                    },
                    onTitleLongPress = { onNavigate(Screen.VaultLock.route) }
                )
            }
        },
        bottomBar = {
            if (uiState.isSelectionMode) {
                Column {
                    HomeSelectionBottomBar(
                        onShare = { shareSelectedMedia(context, uiState.selectedPaths, shareTitle) },
                        onFavorite = { viewModel.toggleFavoriteSelected() },
                        onAddTo = { showAddToSheet = true },
                        onHide = { viewModel.moveSelectedToVault() },
                        onDelete = { showDeleteDialog = true },
                        onCreateAlbum = { showCreateAlbumDialog = true }
                    )
                    Spacer(Modifier.fillMaxWidth().background(colorResource(R.color.home_shell_tab_bg)).navigationBarsPadding())
                }
            } else {
                HomeTabBar(uiState.selectedTab) { viewModel.setTab(it) }
            }
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Swipe left/right between tabs; kept in sync with the bottom bar
            val pagerState = androidx.compose.foundation.pager.rememberPagerState(initialPage = uiState.selectedTab) { 3 }
            LaunchedEffect(uiState.selectedTab) {
                if (pagerState.currentPage != uiState.selectedTab) pagerState.animateScrollToPage(uiState.selectedTab)
            }
            // Turning swiping off mid-drag (long-press then move) cancels the drag and leaves the pager
            // between pages, so the grid draws half off-screen; ViewPager2 never rests there. Settle it.
            LaunchedEffect(uiState.isSelectionMode) {
                if (uiState.isSelectionMode && pagerState.currentPageOffsetFraction != 0f) {
                    pagerState.animateScrollToPage(pagerState.currentPage)
                }
            }
            LaunchedEffect(pagerState) {
                androidx.compose.runtime.snapshotFlow { pagerState.settledPage }.collect { viewModel.setTab(it) }
            }
            androidx.compose.foundation.pager.HorizontalPager(
                state = pagerState,
                // No swiping while selecting, so a drag can't drop the selection
                userScrollEnabled = !uiState.isSelectionMode,
                beyondViewportPageCount = 2, // HA setOffscreenPageLimit(2)
                modifier = Modifier.fillMaxSize()
            ) { page ->
            when (page) {
                PAGE_ALBUMS -> AlbumsTab(
                    albumsList = uiState.albumsList,
                    isLoading = uiState.albumsList.isEmpty() && !uiState.initialSyncDone,
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
                            "duplicates" -> onNavigate(Screen.DuplicateWaiting.route) // reference scans first
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
                            else -> viewModel.setTab(PAGE_PHOTOS)
                        }
                    }
                )
                PAGE_PHOTOS -> PhotosTab(
                    mediaList = uiState.mediaList,
                    isLoading = uiState.mediaList.isEmpty() && !uiState.initialSyncDone,
                    columnCount = uiState.columnCount,
                    selectedPaths = uiState.selectedPaths,
                    isSelectionMode = uiState.isSelectionMode,
                    onMediaClick = { item ->
                        onNavigate(Screen.MediaViewer.createRoute(item.filePath, ""))
                    },
                    onMediaLongClick = { item ->
                        viewModel.toggleSelection(item.filePath)
                    }
                )
                PAGE_EXPLORE -> ExploreTab(
                    onNavigate = onNavigate
                )
            }
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

    if (showSortDialog) {
        var tempField by remember { mutableStateOf(uiState.sortOrder.field) }
        var tempDirection by remember { mutableStateOf(uiState.sortOrder.direction) }

        AlertDialog(
            onDismissRequest = { showSortDialog = false },
            title = {
                Text(
                    text = stringResource(R.string.sort_by),
                    style = GalleryText.BodyXLargeSemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            },
            text = {
                Column {
                    val fieldOptions = listOf(
                        R.string.last_modified to SortField.DATE_MODIFIED,
                        R.string.filename to SortField.NAME,
                        R.string.file_size to SortField.SIZE,
                        R.string.created_date to SortField.DATE_TAKEN
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
                                text = stringResource(label),
                                style = GalleryText.BodyMediumMedium,
                                color = if (tempField == field) GalleryBlue else MaterialTheme.colorScheme.onSurface,
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
                        R.string.ascending_order to SortDirection.ASCENDING,
                        R.string.descending_order to SortDirection.DESCENDING
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
                                text = stringResource(label),
                                style = GalleryText.BodyMediumMedium,
                                color = if (tempDirection == direction) GalleryBlue else MaterialTheme.colorScheme.onSurface,
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
                    Text(stringResource(R.string.ok), style = GalleryText.BodyMediumSemiBold, color = Color.White)
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showSortDialog = false }
                ) {
                    Text(stringResource(R.string.cancel), style = GalleryText.BodyMediumSemiBold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            },
            containerColor = MaterialTheme.colorScheme.surface,
            shape = RoundedCornerShape(16.dp)
        )
    }

    if (showDeleteDialog) {
        DeleteMediaDialog(
            count = uiState.selectedPaths.size,
            onConfirm = { checked ->
                showDeleteDialog = false
                viewModel.deleteSelected(deleteActionFor(checked))
            },
            onDismiss = { showDeleteDialog = false }
        )
    }

    if (showAddToSheet) {
        AddToAlbumSheet(
            albums = uiState.albumsList,
            previewItem = uiState.mediaList.firstOrNull { it.filePath in uiState.selectedPaths },
            selectedCount = uiState.selectedPaths.size,
            onAlbumClick = { album ->
                showAddToSheet = false
                viewModel.addSelectedToFolder(album.path, onCopied)
            },
            onDismiss = { showAddToSheet = false }
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
                    text = stringResource(R.string.create_an_album),
                    style = GalleryText.BodyXLargeSemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            },
            text = {
                Column {
                    Text(
                        text = stringResource(R.string.album_name),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(bottom = 6.dp)
                    )
                    OutlinedTextField(
                        value = newAlbumName,
                        onValueChange = { newAlbumName = it },
                        placeholder = { Text(stringResource(R.string.enter_album_name)) },
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
                                // Create album from the selection bar copies the selection in; the Albums + makes it empty.
                                if (uiState.isSelectionMode) viewModel.addSelectedToFolder(newDir.absolutePath, onCopied)
                                else viewModel.syncMedia()
                            } catch (_: Exception) {}
                        }
                        showCreateAlbumDialog = false
                        newAlbumName = ""
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = GalleryBlue),
                    shape = RoundedCornerShape(20.dp)
                ) {
                    Text(stringResource(R.string.caller_ok), style = GalleryText.BodyMediumSemiBold, color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    showCreateAlbumDialog = false
                    newAlbumName = ""
                }) {
                    Text(stringResource(R.string.cancel), style = GalleryText.BodyMediumSemiBold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            },
            containerColor = MaterialTheme.colorScheme.surface,
            shape = RoundedCornerShape(16.dp)
        )
    }
}

private fun shareSelectedMedia(context: Context, paths: Set<String>, chooserTitle: String) {
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
        context.startActivity(Intent.createChooser(shareIntent, chooserTitle))
    } catch (_: Exception) {}
}
