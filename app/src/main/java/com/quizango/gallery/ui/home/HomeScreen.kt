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
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.animation.core.Easing
import androidx.compose.animation.core.animate
import androidx.compose.animation.core.tween
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.unit.Velocity
import com.quizango.gallery.ui.theme.sdp
import kotlinx.coroutines.Job
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
import com.quizango.gallery.notification.QuickActionsNotification
import com.quizango.gallery.ui.components.AddToAlbumSheet
import com.quizango.gallery.ui.navigation.Screen
import com.quizango.gallery.ui.splash.checkStoragePermissions
import com.quizango.gallery.ui.vault.rememberVaultHideGate
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
    var showDeleteDialog by remember { mutableStateOf(false) }
    var showAddToSheet by remember { mutableStateOf(false) }
    // Paths the Hide dialog will move; set only once the vault lock gate has passed
    var hidePaths by remember { mutableStateOf<List<String>?>(null) }
    val hideGate = rememberVaultHideGate(onNavigate) { hidePaths = it }
    val shareTitle = stringResource(R.string.share)
    val copiedMessage = stringResource(R.string.items_copied_successfully)
    val movedMessage = stringResource(R.string.items_moved_successfully)
    val sameFolderMessage = stringResource(R.string.media_files_should_not_move_to_same_folder)
    // Folder picked for the selection (Add to, or a new album from it); dialog_select_add then asks Move or Copy
    var addTarget by rememberSaveable { mutableStateOf<String?>(null) }

    val openAlbumDialog = rememberAlbumSelectionDialogs(viewModel, uiState)

    var titleBounds by remember { mutableStateOf<androidx.compose.ui.geometry.Rect?>(null) }
    val albumSelecting = uiState.selectedAlbums.isNotEmpty()
    val anySelecting = uiState.isSelectionMode || albumSelecting
    val openedFromOutside = LocalOpenedFromOutside.current
    // HA.OoooOoO: Android 13+ asks for notifications as Home starts (not after an outside entry) and the vault hint
    // waits for the answer; once notifications may show, the Quick Actions notification is scheduled (bw/h 16)
    var notificationAskPending by rememberSaveable {
        mutableStateOf(!openedFromOutside && !QuickActionsNotification.allowed(context))
    }
    val notificationAsk = androidx.activity.compose.rememberLauncherForActivityResult(
        androidx.activity.result.contract.ActivityResultContracts.RequestPermission()
    ) { granted ->
        notificationAskPending = false
        if (granted) QuickActionsNotification.schedule(context)
    }
    LaunchedEffect(Unit) {
        if (notificationAskPending) notificationAsk.launch(android.Manifest.permission.POST_NOTIFICATIONS)
        else QuickActionsNotification.schedule(context)
    }
    val showVaultHint = !anySelecting && shouldShowVaultHint(
        uiState.sessionCount, uiState.vaultHintSeen, uiState.vaultHintClosedSession, checkStoragePermissions(context),
        openedFromOutside = openedFromOutside, startDialogShowing = notificationAskPending
    )

    // HA.Oooo0oO order: vault hint, then the ⋮ popup (its focusable window eats Back itself), then selection,
    // then exit. 2.7.2 ships app_exit = "direct" and was seen exiting straight away on the device.
    BackHandler {
        when {
            showVaultHint -> viewModel.closeVaultHint()
            uiState.isSelectionMode -> viewModel.clearSelection()
            albumSelecting -> viewModel.clearAlbumSelection()
            else -> (context as? Activity)?.finishAffinity()
        }
    }

    // activity_home bottomMainBar under HomeBottomBarBehavior: the tab bar follows the scroll, then settles.
    val barScope = rememberCoroutineScope()
    var barHeight by remember { mutableIntStateOf(0) }
    var barOffset by remember { mutableFloatStateOf(0f) }
    val settleJob = remember { arrayOfNulls<Job>(1) }
    fun settleBarTo(target: Float) {
        settleJob[0]?.cancel()
        settleJob[0] = barScope.launch {
            animate(barOffset, target, animationSpec = tween(BAR_SETTLE_MS, easing = Easing(::barSettleEasing))) { v, _ -> barOffset = v }
        }
    }
    val barScroll = remember(anySelecting) {
        object : NestedScrollConnection {
            override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
                if (anySelecting) return Offset.Zero
                settleJob[0]?.cancel()
                barOffset = (barOffset - available.y).coerceIn(0f, barHeight.toFloat())
                return Offset.Zero // the bar moves alongside the content, it never eats the scroll
            }

            override suspend fun onPostFling(consumed: Velocity, available: Velocity): Velocity {
                if (!anySelecting) settleBarTo(settledBarOffset(barOffset, barHeight.toFloat()))
                return Velocity.Zero
            }
        }
    }
    LaunchedEffect(uiState.selectedTab) { settleBarTo(0f) }
    LaunchedEffect(anySelecting) {
        if (anySelecting) {
            settleJob[0]?.cancel()
            barOffset = 0f
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
        ) {
            if (albumSelecting) {
                val folders = uiState.albumsList.filter(::albumIsSelectable)
                val allSelected = folders.isNotEmpty() && uiState.selectedAlbums.size == folders.size
                HomeSelectionTopBar(
                    count = uiState.selectedAlbums.size,
                    allSelected = allSelected,
                    onClose = { viewModel.clearAlbumSelection() },
                    onToggleAll = { if (allSelected) viewModel.clearAlbumSelection() else viewModel.selectAllAlbums() },
                    onInfo = { openAlbumDialog(AlbumDialog.INFO) },
                    infoAlpha = if (uiState.selectedAlbums.size > 1) 0f else 1f
                )
            } else if (uiState.isSelectionMode) {
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
                    onTitleLongPress = {
                        if (showVaultHint) viewModel.closeVaultHint() // HA.o000OO0o closes the hint first
                        onNavigate(Screen.VaultLock.route)
                    },
                    onPro = { onNavigate(Screen.Pro.route) },
                    onTitlePositioned = { titleBounds = it }
                )
            }
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .clipToBounds()
                    .nestedScroll(barScroll)
            ) {
                // Swipe left/right between tabs; kept in sync with the bottom bar
                val pagerState = androidx.compose.foundation.pager.rememberPagerState(initialPage = uiState.selectedTab) { 3 }
                LaunchedEffect(uiState.selectedTab) {
                    if (pagerState.currentPage != uiState.selectedTab) pagerState.animateScrollToPage(uiState.selectedTab)
                }
                // Turning swiping off mid-drag (long-press then move) cancels the drag and leaves the pager
                // between pages, so the grid draws half off-screen; ViewPager2 never rests there. Settle it.
                LaunchedEffect(anySelecting) {
                    if (anySelecting && pagerState.currentPageOffsetFraction != 0f) {
                        pagerState.animateScrollToPage(pagerState.currentPage)
                    }
                }
                LaunchedEffect(pagerState) {
                    androidx.compose.runtime.snapshotFlow { pagerState.settledPage }.collect { viewModel.setTab(it) }
                }
                androidx.compose.foundation.pager.HorizontalPager(
                    state = pagerState,
                    // No swiping while selecting, so a drag can't drop the selection
                    userScrollEnabled = !anySelecting,
                    beyondViewportPageCount = 2, // HA setOffscreenPageLimit(2)
                    modifier = Modifier.fillMaxSize()
                ) { page ->
                when (page) {
                    PAGE_ALBUMS -> AlbumsTab(
                        albumsList = uiState.albumsList,
                        isLoading = uiState.albumsList.isEmpty() && !uiState.initialSyncDone,
                        mediaList = uiState.mediaList,
                        favoriteAlbum = uiState.favoriteAlbum,
                        deletedAlbum = uiState.deletedAlbum,
                        selectedAlbums = uiState.selectedAlbums,
                        onAlbumClick = { album, title -> onNavigate(albumRoute(album, title)) },
                        onAlbumToggle = { viewModel.toggleAlbum(it.path) },
                        onStoryClick = { id -> onNavigate(Screen.StoryView.createRoute(id, "")) }
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
                        },
                        onColumnCountChange = viewModel::setColumnCount
                    )
                    PAGE_EXPLORE -> ExploreTab(
                        junkCleanedToday = uiState.junkCleanedToday,
                        showCollageNew = showsCollageNew(uiState.collageNewSeen, uiState.sessionCount),
                        onNavigate = onNavigate,
                        onCollageOpened = viewModel::markCollageNewSeen
                    )
                }
                }

                if (albumSelecting) {
                    AlbumSelectionBottomBar(viewModel, uiState, openAlbumDialog, Modifier.align(Alignment.BottomCenter))
                } else if (uiState.isSelectionMode) {
                    HomeSelectionBottomBar(
                        onShare = { shareSelectedMedia(context, uiState.selectedPaths, shareTitle) },
                        onFavorite = { viewModel.toggleFavoriteSelected() },
                        onAddTo = { showAddToSheet = true },
                        onHide = { hideGate(uiState.selectedPaths.toList()) },
                        onDelete = { showDeleteDialog = true },
                        onCreateAlbum = { showCreateAlbumDialog = true },
                        modifier = Modifier.align(Alignment.BottomCenter)
                    )
                } else {
                    HomeTabBar(
                        selectedPage = uiState.selectedTab,
                        onSelect = { viewModel.setTab(it) },
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .onSizeChanged { barHeight = it.height }
                            .graphicsLayer { translationY = barOffset }
                    )
                }

                // dataSync: 5.sdp bar pulled up 3.dp so a thin line shows (its -7.dp bottom margin only shrinks a slot nothing sits in here)
                if (uiState.isSyncing) {
                    val primary = colorResource(R.color.colorPrimary)
                    LinearProgressIndicator(
                        modifier = Modifier
                            .align(Alignment.TopCenter)
                            .offset(y = (-3).dp)
                            .fillMaxWidth()
                            .height(5.sdp),
                        color = primary,
                        trackColor = primary.copy(alpha = 0.3f) // framework indeterminate track: colorControlActivated at disabledAlpha
                    )
                }
            }
            // navBarBg: fixed strip behind the system navigation bar
            Spacer(
                Modifier
                    .fillMaxWidth()
                    .background(colorResource(R.color.home_shell_tab_bg))
                    .navigationBarsPadding()
            )
        }
        if (showVaultHint) {
            VaultHintOverlay(
                title = stringResource(HOME_TABS[uiState.selectedTab].title),
                titleBounds = titleBounds,
                onClose = { viewModel.closeVaultHint() },
                onOpenVault = { onNavigate(Screen.VaultLock.route) }
            )
        }
        // Drawn above the vault hint (2.7.2 elevations 10.sdp vs 9.sdp)
        if (uiState.selectedTab == PAGE_PHOTOS && !uiState.pinchTipSeen && checkStoragePermissions(context)) {
            PinchTutorialOverlay(onGotIt = { viewModel.dismissPinchTip() })
        }
    }

    if (showSortDialog) {
        SortDialog(
            current = uiState.sortOrder,
            onApply = {
                viewModel.setSortOrder(it)
                showSortDialog = false
            },
            onDismiss = { showSortDialog = false }
        )
    }

    hidePaths?.let { paths ->
        HideMediaDialog(
            onConfirm = {
                hidePaths = null
                viewModel.moveSelectedToVault(paths)
            },
            onDismiss = { hidePaths = null }
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
                addTarget = album.path
            },
            onDismiss = { showAddToSheet = false }
        )
    }

    addTarget?.let { target ->
        fun add(move: Boolean) {
            addTarget = null
            val message = if (move) movedMessage else copiedMessage
            val onDone = { done: Int -> if (done > 0) Toast.makeText(context, message, Toast.LENGTH_SHORT).show() }
            val started = viewModel.addSelectedToFolder(target, move, onDone)
            if (!started) Toast.makeText(context, sameFolderMessage, Toast.LENGTH_SHORT).show()
        }
        AddingMediaDialog(onMove = { add(move = true) }, onCopy = { add(move = false) }, onCancel = { addTarget = null })
    }

    if (showCreateAlbumDialog) {
        CreateAlbumDialog(
            onCreate = { name ->
                showCreateAlbumDialog = false
                val newDir = File(albumRoots().first(), name)
                try {
                    newDir.mkdirs()
                } catch (_: Exception) {}
                // Create album from the selection bar copies the selection in; the Albums + makes it empty.
                if (uiState.isSelectionMode) addTarget = newDir.absolutePath
                else viewModel.syncMedia()
            },
            onDismiss = { showCreateAlbumDialog = false }
        )
    }
}

// internal: the story viewer reuses it for its Share button
internal fun shareSelectedMedia(context: Context, paths: Set<String>, chooserTitle: String) {
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
