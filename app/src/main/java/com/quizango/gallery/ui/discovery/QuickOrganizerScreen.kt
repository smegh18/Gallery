package com.quizango.gallery.ui.discovery

import android.os.Environment
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.PointerInputScope
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.pointerInteropFilter
import androidx.compose.ui.input.pointer.positionChange
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.platform.LocalContext
import coil.compose.AsyncImage
import com.quizango.gallery.GalleryApplication
import com.quizango.gallery.R
import com.quizango.gallery.data.local.PathMutationOutcome
import com.quizango.gallery.data.repository.MediaRepository
import com.quizango.gallery.domain.model.Album
import com.quizango.gallery.domain.model.MediaItem
import com.quizango.gallery.ui.home.CreateAlbumDialog
import com.quizango.gallery.ui.home.albumRoots
import com.quizango.gallery.ui.theme.GalleryText
import com.airbnb.lottie.compose.LottieAnimation
import com.airbnb.lottie.compose.LottieConstants
import com.airbnb.lottie.compose.animateLottieCompositionAsState
import com.airbnb.lottie.compose.rememberLottieComposition
import kotlinx.coroutines.launch
import java.io.File

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun QuickOrganizerScreen(
    mediaRepository: MediaRepository,
    onNavigateToRecycle: () -> Unit,
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val app = context.applicationContext as GalleryApplication
    val allMedia by mediaRepository.getActiveMediaFlow().collectAsState(initial = emptyList())
    val albumRows by mediaRepository.getAlbumsFlow().collectAsState(initial = emptyList())
    val mediaColumnCount by app.preferences.mediaColCount.collectAsState(initial = 3)
    val skipOrganizerDefaults by app.preferences.isOrgSkipToolTip.collectAsState(initial = false)
    val organizerStep by app.preferences.organizerStepFlow.collectAsState(initial = 1)
    var selectedPath by rememberSaveable { mutableStateOf<String?>(null) }
    var actionState by remember { mutableStateOf(OrganizerActionState()) }
    var busy by remember { mutableStateOf(false) }
    var expanded by rememberSaveable { mutableStateOf(false) }
    var showCreateAlbum by remember { mutableStateOf(false) }
    var showBackDeletePrompt by remember { mutableStateOf(false) }
    var organizerDeletePaths by remember { mutableStateOf(emptyList<String>()) }
    var isMediaLoaded by remember { mutableStateOf(false) }
    var feedback by remember { mutableStateOf<String?>(null) }
    val recycleItems by mediaRepository.getRecycleBinFlow().collectAsState(initial = emptyList())
    val organizerOwnedPaths by app.preferences.organizerBinPaths.collectAsState(initial = emptySet())
    LaunchedEffect(allMedia) { isMediaLoaded = true }

    fun requestBack() {
        val ownedRecycle = recycleItems.filter { it.filePath in organizerOwnedPaths }
        if (organizerStep == 7 && actionState.completedPaths.isNotEmpty() && ownedRecycle.isNotEmpty()) {
            organizerDeletePaths = ownedRecycle.map { it.filePath }.distinct()
            showBackDeletePrompt = true
        } else onNavigateBack()
    }

    fun swipeTutorial(swipe: OrganizerSwipe) {
        val next = nextOrganizerTutorialStep(organizerStep, swipe)
        if (next != organizerStep) scope.launch { app.preferences.setOrganizerStepFlow(next) }
    }

    val visibleItems = remember(allMedia, actionState.completedPaths) { allMedia.filterNot { it.filePath in actionState.completedPaths } }
    LaunchedEffect(allMedia, selectedPath) {
        val selected = selectedPath
        val valid = visibleItems.mapTo(HashSet()) { it.filePath }
        if (selected == null || selected !in valid) selectedPath = visibleItems.firstOrNull()?.filePath
    }
    val currentIndex = visibleItems.indexOfFirst { it.filePath == selectedPath }.coerceAtLeast(0)
    val currentItem = visibleItems.getOrNull(currentIndex)
    val pagerState = rememberPagerState(initialPage = currentIndex) { visibleItems.size }
    LaunchedEffect(currentIndex, visibleItems.size) {
        if (visibleItems.isNotEmpty() && pagerState.currentPage != currentIndex) pagerState.scrollToPage(currentIndex)
    }
    LaunchedEffect(pagerState.currentPage, visibleItems) {
        visibleItems.getOrNull(pagerState.currentPage)?.let { selectedPath = it.filePath }
    }
    LaunchedEffect(feedback) {
        feedback?.let { Toast.makeText(context, it, Toast.LENGTH_SHORT).show(); feedback = null }
    }

    // The reference's empty folder list gets these two display entries unless the persisted skip hint is set.
    val realFolders = remember(albumRows) { albumRows.filter { it.path != "filter:all_photos" && it.path != "filter:videos" && it.path != "filter:camera" && it.path != "filter:screenshots" && it.path.startsWith("/") } }
    val albums = remember(realFolders, skipOrganizerDefaults) {
        if (realFolders.isEmpty() && !skipOrganizerDefaults) listOf(
            Album("organizer:photos", "Photos", File(Environment.getExternalStorageDirectory(), "Pictures").path, "", 0),
            Album("organizer:family", "Family", File(Environment.getExternalStorageDirectory(), "Pictures/Family").path, "", 0)
        ) else realFolders
    }
    val canUndo = actionState.history.isNotEmpty() && !busy

    fun advanceAfter(path: String) {
        selectedPath = nextOrganizerPath(visibleItems.map { it.filePath }, path)
    }
    suspend fun commitMutation(action: OrganizerAction, outcome: PathMutationOutcome) {
        actionState = recordOrganizerAction(actionState, action, outcome)
        if (outcome.succeeded) {
            if (action is OrganizerAction.Trash) app.preferences.addOrganizerBinPaths(setOf(action.path))
            advanceAfter(action.path)
        }
        else feedback = context.getString(R.string.organizer_move_failed)
    }
    fun skipCurrent() {
        val item = currentItem ?: return
        val action = OrganizerAction.Skip(item.filePath)
        actionState = actionState.copy(history = actionState.history + action, completedPaths = actionState.completedPaths + item.filePath)
        advanceAfter(item.filePath)
    }
    fun moveCurrentToTrash() {
        val item = currentItem ?: return
        if (busy) return
        busy = true
        scope.launch {
            try { commitMutation(OrganizerAction.Trash(item.filePath), mediaRepository.moveToTrash(item.filePath)) }
            finally { busy = false }
        }
    }
    fun undo() {
        val action = actionState.history.lastOrNull() ?: run { feedback = context.getString(R.string.org_no_actions_to_undo); return }
        if (busy) return
        if (action is OrganizerAction.Skip) {
            actionState = actionState.copy(history = actionState.history.dropLast(1), currentPath = action.path, completedPaths = actionState.completedPaths - action.path)
            selectedPath = action.path
            return
        }
        busy = true
        scope.launch {
            try {
                val outcome = when (action) {
                    is OrganizerAction.Trash -> mediaRepository.restoreFromTrash(action.path)
                    is OrganizerAction.Move -> {
                        if (mediaRepository.moveMediaToFolder(action.path, organizerMoveUndoFolder(action))) PathMutationOutcome(action.path, PathMutationOutcome.Status.SUCCESS)
                        else PathMutationOutcome(action.path, PathMutationOutcome.Status.FILE_FAILED)
                    }
                    is OrganizerAction.Skip -> error("Handled above")
                }
                actionState = undoOrganizerAction(actionState, action, outcome)
                if (outcome.succeeded) {
                    selectedPath = if (action is OrganizerAction.Move) action.source else action.path
                } else feedback = context.getString(R.string.organizer_move_failed)
            } finally { busy = false }
        }
    }
    fun moveCurrentTo(album: Album) {
        val item = currentItem ?: return
        if (item.folderPath == album.path) { feedback = context.getString(R.string.media_files_should_not_move_to_same_folder); return }
        if (busy) return
        busy = true
        scope.launch {
            try {
                val movedPath = mediaRepository.moveMediaToFolderWithResult(item.filePath, album.path)
                val path = movedPath ?: item.filePath
                val result = PathMutationOutcome(path, if (movedPath != null) PathMutationOutcome.Status.SUCCESS else PathMutationOutcome.Status.FILE_FAILED)
                commitMutation(organizerMoveAction(item.filePath, path), result)
            } finally { busy = false }
        }
    }
    // 2.7.2 cr/c: leaving the album dialog at step 3 without a new album closes the organizer; reopening shows the dialog again
    fun dismissCreateAlbum() {
        showCreateAlbum = false
        if (organizerStep == 3) onNavigateBack()
    }
    LaunchedEffect(organizerStep) { if (organizerStep == 3) showCreateAlbum = true }

    fun createAlbum(name: String) {
        val folder = File(albumRoots().first(), name)
        val success = try { folder.mkdirs() } catch (_: Exception) { false }
        // Toasted here, not through feedback: a failure at step 3 leaves the screen in the same tap
        Toast.makeText(context, if (success) R.string.org_folder_created else R.string.org_folder_create_failed, Toast.LENGTH_SHORT).show()
        if (!success) return dismissCreateAlbum()
        showCreateAlbum = false
        scope.launch {
            // Saved before the sync, like 2.7.2 o000oOoO, so leaving mid-sync cannot strand the tutorial at step 3
            val next = organizerTutorialStepAfterCreate(organizerStep, true)
            if (next != organizerStep) app.preferences.setOrganizerStepFlow(next)
            mediaRepository.syncMediaStore()
        }
    }

    fun openCreateAlbum() {
        if (organizerStep == 2) scope.launch { app.preferences.setOrganizerStepFlow(3) }
        showCreateAlbum = true
    }

    fun tapAlbum(album: Album, index: Int) {
        val next = organizerTutorialStepAfterAlbumTap(organizerStep, index)
        if (next != organizerStep) scope.launch { app.preferences.setOrganizerStepFlow(next) }
        moveCurrentTo(album)
    }

    BackHandler { requestBack() }
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(Modifier.clickable { expanded = !expanded }, verticalAlignment = Alignment.CenterVertically) {
                        Text(stringResource(R.string.unorganized), style = GalleryText.BodyLargeSemiBold, color = colorResource(R.color.titleColor))
                        Icon(painterResource(R.drawable.ic_down_arrow), contentDescription = stringResource(R.string.org_dropdown), modifier = Modifier.size(24.dp))
                    }
                },
                navigationIcon = { IconButton(onClick = ::requestBack) { Icon(painterResource(R.drawable.ic_back), contentDescription = null, tint = colorResource(R.color.titleColor)) } },
                actions = {
                    IconButton(onClick = ::undo, enabled = !busy) { Icon(painterResource(if (canUndo) R.drawable.ic_undo_checked else R.drawable.ic_undo_unchecked), contentDescription = stringResource(R.string.undo)) }
                    IconButton(onClick = onNavigateToRecycle) { Icon(painterResource(R.drawable.ic_delete), contentDescription = stringResource(R.string.organizer_recycle_bin), tint = colorResource(R.color.titleColor)) }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.White)
            )
        }
    ) { insets ->
        Column(Modifier.fillMaxSize().padding(insets).background(Color.White)) {
            if (expanded) {
                Row(Modifier.fillMaxWidth().background(colorResource(R.color.shimmer_color)).padding(horizontal = 10.dp, vertical = 5.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(painterResource(R.drawable.ic_information), contentDescription = null, modifier = Modifier.size(16.dp), tint = Color.Unspecified)
                    Text(stringResource(R.string.you_can_select_a_photo_to_jump_to), style = GalleryText.BodyXSmallSemiBold, color = colorResource(R.color.black), modifier = Modifier.padding(start = 10.dp))
                }
                LazyVerticalGrid(columns = GridCells.Fixed(mediaColumnCount.coerceIn(2, 8)), modifier = Modifier.weight(1f).fillMaxWidth().padding(top = 10.dp)) {
                    items(allMedia, key = { it.filePath }) { media ->
                        AsyncImage(model = media.contentUri, contentDescription = media.fileName, modifier = Modifier.fillMaxWidth().aspectRatio(1f).clickable { selectedPath = media.filePath; actionState = actionState.copy(completedPaths = actionState.completedPaths - media.filePath); expanded = false }, contentScale = ContentScale.Crop)
                    }
                }
            } else if (!isMediaLoaded) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(Modifier.padding(bottom = 8.dp), color = colorResource(R.color.colorPrimary))
                }
            } else if (visibleItems.isEmpty()) {
                Column(Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                    Icon(painterResource(R.drawable.ic_empty_img), contentDescription = null, tint = Color.Unspecified, modifier = Modifier.size(90.dp))
                    Text(stringResource(R.string.no_items), style = MaterialTheme.typography.titleMedium, color = colorResource(R.color.titleColor))
                    Text(stringResource(R.string.no_media_file), style = MaterialTheme.typography.bodySmall, color = colorResource(R.color.fastScrollerColor))
                }
            } else {
                HorizontalPager(state = pagerState, modifier = Modifier.weight(1f).fillMaxWidth(), userScrollEnabled = !busy && organizerStep == 7) { page ->
                    val item = visibleItems.getOrNull(page)
                    if (item != null) {
                        Box(Modifier.fillMaxSize().pointerInput(item.filePath, busy, organizerStep) {
                            // Before step 7 the tutorial overlay takes every touch, so only step 7 swipes reach the photo
                            detectOrganizerSwipe { swipe ->
                                if (organizerStep == 7) when (swipe) {
                                    OrganizerSwipe.UP -> moveCurrentToTrash()
                                    OrganizerSwipe.LEFT -> skipCurrent()
                                    else -> Unit
                                }
                            }
                        }, contentAlignment = Alignment.Center) {
                            AsyncImage(model = item.contentUri, contentDescription = item.fileName, modifier = Modifier.fillMaxSize().padding(10.dp), contentScale = ContentScale.Fit)
                            if (busy) CircularProgressIndicator(Modifier.align(Alignment.Center), color = colorResource(R.color.colorPrimary))
                        }
                    }
                }
                LazyRow(Modifier.fillMaxWidth().shadow(12.dp).background(Color.White).padding(vertical = 4.dp), contentPadding = PaddingValues(horizontal = 8.dp), horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                    item {
                        OrganizerAlbumCell(name = stringResource(R.string.org_create_album), cover = null, create = true, selected = false, enabled = organizerStep == 7 || organizerStep == 2, onClick = ::openCreateAlbum)
                    }
                    items(albums.size, key = { albums[it].path }) { index ->
                        val album = albums[index]
                        val currentFolder = currentItem?.folderPath == album.path
                        OrganizerAlbumCell(name = album.name.ifBlank { stringResource(album.titleRes) }, cover = album.coverUri, create = false, selected = currentFolder, enabled = organizerStep == 7 || organizerStep == 4, onClick = { tapAlbum(album, index + 1) })
                    }
                }
            }
            Spacer(Modifier.navigationBarsPadding())
        }
    }
    OrganizerTutorialOverlay(
        step = organizerStep,
        onClose = { scope.launch { app.preferences.setOrganizerStepFlow(7) } },
        onOrganizeNow = { scope.launch { app.preferences.setOrganizerStepFlow(2) } },
        onCreateAlbum = ::openCreateAlbum,
        onAlbumTap = { albums.firstOrNull()?.let { tapAlbum(it, 1) } },
        onSwipe = ::swipeTutorial
    )
    if (showCreateAlbum) CreateAlbumDialog(onCreate = ::createAlbum, onDismiss = ::dismissCreateAlbum)
    if (showBackDeletePrompt) {
        AlertDialog(
            onDismissRequest = { showBackDeletePrompt = false; onNavigateBack() },
            title = { Text(stringResource(R.string.organizer_dialog_title)) },
            text = { Text(stringResource(R.string.organizer_deleting_select_item)) },
            confirmButton = {
                TextButton(enabled = !busy, onClick = {
                    val snapshot = organizerDeletePaths
                    busy = true
                    scope.launch {
                        try {
                            val outcomes = mediaRepository.deletePermanently(snapshot)
                            val succeeded = outcomes.filter { it.succeeded }.map { it.path }.toSet()
                            app.preferences.removeOrganizerBinPaths(succeeded)
                            actionState = actionState.copy(completedPaths = actionState.completedPaths - succeeded)
                            val failed = outcomes.filterNot { it.succeeded }.map { it.path }
                            if (failed.isEmpty()) {
                                feedback = context.getString(R.string.organizer_deleted_count, succeeded.size)
                                showBackDeletePrompt = false
                                onNavigateBack()
                            } else {
                                organizerDeletePaths = failed
                                feedback = context.getString(R.string.organizer_move_failed)
                            }
                        } finally { busy = false }
                    }
                }) { Text(stringResource(R.string.permanently_delete), color = colorResource(R.color.cleaner_red)) }
            },
            dismissButton = { TextButton(onClick = { showBackDeletePrompt = false; onNavigateBack() }) { Text(stringResource(R.string.cancel)) } }
        )
    }
}

/** One swipe per finished drag; every drag counts from zero, so a cancelled one never adds to the next. */
private suspend fun PointerInputScope.detectOrganizerSwipe(onSwipe: (OrganizerSwipe) -> Unit) {
    var dx = 0f
    var dy = 0f
    detectDragGestures(onDragStart = { dx = 0f; dy = 0f }, onDragEnd = { onSwipe(organizerSwipeFromDelta(dx, dy)) }) { change, drag ->
        change.consume()
        dx += drag.x
        dy += drag.y
    }
}

@Composable
private fun OrganizerAlbumCell(name: String, cover: Any?, create: Boolean, selected: Boolean, enabled: Boolean = true, onClick: () -> Unit) {
    Column(Modifier.width(78.dp).clickable(enabled = enabled, onClick = onClick).padding(8.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        AsyncImage(model = if (create) R.drawable.organizer_create_album else cover ?: R.drawable.ic_album_placeholder, contentDescription = name, modifier = Modifier.size(48.dp).clip(RoundedCornerShape(8.dp)), contentScale = ContentScale.Crop)
        Text(name, fontSize = 12.sp, fontWeight = FontWeight.Medium, color = colorResource(R.color.black), maxLines = 2, overflow = TextOverflow.Ellipsis, textAlign = TextAlign.Center, modifier = Modifier.padding(top = 5.dp))
    }
}

@Composable
private fun OrganizerTutorialOverlay(
    step: Int,
    onClose: () -> Unit,
    onOrganizeNow: () -> Unit,
    onCreateAlbum: () -> Unit,
    onAlbumTap: () -> Unit,
    onSwipe: (OrganizerSwipe) -> Unit
) {
    if (step !in 1..6) return
    // 2.7.2 clThree/clFour (cr/f): the full-screen step 5/6 layers take the swipe themselves
    Box(Modifier.fillMaxSize().background(Color(0xCC000000)).clickable(enabled = true, onClick = {}).pointerInput(step) {
        if (step in 5..6) detectOrganizerSwipe(onSwipe)
    }) {
        when (step) {
            1 -> Column(Modifier.align(Alignment.Center).fillMaxWidth().padding(horizontal = 30.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Icon(painterResource(R.drawable.ic_heart_left_org), null, Modifier.size(36.dp, 54.dp))
                    IconButton(onClick = onClose) { Icon(painterResource(R.drawable.ic_close_new_org), contentDescription = null, modifier = Modifier.size(24.dp)) }
                }
                val composition by rememberLottieComposition(com.airbnb.lottie.compose.LottieCompositionSpec.RawRes(R.raw.quick_oraganize))
                val progress by animateLottieCompositionAsState(composition, iterations = LottieConstants.IterateForever)
                LottieAnimation(composition, { progress }, Modifier.size(width = 250.dp, height = 330.dp))
                Button(onClick = onOrganizeNow, shape = RoundedCornerShape(100), modifier = Modifier.fillMaxWidth().padding(vertical = 24.dp)) {
                    Text(stringResource(R.string.organize_now).uppercase(), color = Color.White, fontWeight = FontWeight.Bold)
                }
            }
            2 -> TutorialCard(Modifier.align(Alignment.BottomCenter), stringResource(R.string.str_step_1), R.drawable.organizer_create_album, stringResource(R.string.org_create_album), onCreateAlbum)
            4 -> TutorialCard(Modifier.align(Alignment.BottomCenter), stringResource(R.string.str_step_2), R.drawable.ic_album_placeholder, stringResource(R.string.org_album_name_photo), onAlbumTap)
            5 -> TutorialGesturePrompt(Modifier.align(Alignment.Center), R.raw.hand_left_arrow, stringResource(R.string.str_step_3))
            6 -> TutorialGesturePrompt(Modifier.align(Alignment.Center), R.raw.hand_upside_arrow, stringResource(R.string.str_step_4))
        }
    }
}

@Composable
private fun TutorialCard(modifier: Modifier, message: String, icon: Int, label: String, action: () -> Unit) {
    Column(modifier.fillMaxWidth().background(Color(0xCC000000)).padding(bottom = 8.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Text(message, color = Color.White, style = GalleryText.BodyXXLargeSemiBold, textAlign = TextAlign.Center)
        Row(Modifier.fillMaxWidth().padding(12.dp).background(colorResource(R.color.dialogBack), RoundedCornerShape(topStart = 12.dp, topEnd = 12.dp)).clickable(onClick = action).padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(painterResource(icon), null)
            Text(label, color = colorResource(R.color.titleColor), modifier = Modifier.padding(start = 8.dp))
        }
    }
}

@Composable
private fun TutorialGesturePrompt(modifier: Modifier, raw: Int, message: String) {
    val composition by rememberLottieComposition(com.airbnb.lottie.compose.LottieCompositionSpec.RawRes(raw))
    val progress by animateLottieCompositionAsState(composition, iterations = LottieConstants.IterateForever)
    Column(modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        LottieAnimation(composition, { progress }, Modifier.size(100.dp))
        Text(message, color = Color.White, style = GalleryText.BodyXXLargeSemiBold, textAlign = TextAlign.Center)
    }
}
