@file:kotlin.OptIn(
    androidx.compose.foundation.ExperimentalFoundationApi::class,
    androidx.media3.common.util.UnstableApi::class
)

package com.quizango.gallery.ui.viewer

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.annotation.OptIn
import androidx.annotation.StringRes
import androidx.activity.compose.BackHandler
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animate
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.gestures.rememberTransformableState
import androidx.compose.foundation.gestures.transformable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.FileProvider
import androidx.media3.common.MediaItem as ExoMediaItem
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.quizango.gallery.R
import com.quizango.gallery.data.repository.MediaRepository
import com.quizango.gallery.data.repository.VaultRepository
import com.quizango.gallery.domain.model.MediaItem
import com.quizango.gallery.domain.model.MediaType
import com.quizango.gallery.domain.model.VaultItem
import com.quizango.gallery.ui.navigation.Screen
import com.quizango.gallery.ui.theme.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.util.*

@OptIn(UnstableApi::class)
@ExperimentalMaterial3Api
@Composable
fun MediaViewerScreen(
    filePath: String,
    folderPath: String = "",
    isRecycle: Boolean = false,
    isVault: Boolean = false,
    mediaRepository: MediaRepository,
    vaultRepository: VaultRepository,
    onNavigate: (String) -> Unit,
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var showControls by remember { mutableStateOf(true) }
    var showMoreMenu by remember { mutableStateOf(false) }
    // Slide show runs inside the viewer (MF slideControl), from the current page
    var slideshow by remember { mutableStateOf(false) }
    var slideSeconds by remember { mutableIntStateOf(SLIDESHOW_DEFAULT_SECONDS) }
    var slideMuted by remember { mutableStateOf(false) }
    // Bumped after Rotate rewrites the file, so the page reloads it
    var imageVersion by remember { mutableIntStateOf(0) }

    // Video Playback State
    var currentExoPlayer by remember { mutableStateOf<ExoPlayer?>(null) }
    var isVideoPlaying by remember { mutableStateOf(false) }
    var isVideoMuted by remember { mutableStateOf(false) }
    var videoSpeed by remember { mutableFloatStateOf(1.0f) }
    var videoPositionMs by remember { mutableLongStateOf(0L) }
    var videoDurationMs by remember { mutableLongStateOf(0L) }

    // Collect media items for this album/folder, recycle bin, vault, or all media
    val mediaList by remember(folderPath, isRecycle, isVault) {
        mediaSourceFlow(mediaRepository, vaultRepository, folderPath, isRecycle, isVault)
    }.collectAsState(initial = emptyList())

    val fallbackItem = remember(filePath) {
        val file = File(filePath)
        val isVideo = file.extension.lowercase() in listOf("mp4", "mkv", "mov", "avi", "3gp", "webm")
        MediaItem(
            id = 0,
            filePath = filePath,
            fileName = file.name,
            fileSize = file.length(),
            folderName = file.parentFile?.name ?: "",
            folderPath = file.parent ?: "",
            dateModified = file.lastModified() / 1000,
            dateTaken = file.lastModified() / 1000,
            mediaType = if (isVideo) MediaType.VIDEO else MediaType.IMAGE
        )
    }

    val isListReady = mediaList.isNotEmpty()

    // Once the opened file has been listed, its absence means it left the list (Recycle Bin, vault), not "not indexed yet"
    val openedListed = remember(filePath) { booleanArrayOf(false) }
    val displayList = remember(mediaList, fallbackItem) {
        if (mediaList.any { it.filePath == filePath }) openedListed[0] = true
        viewerDisplayList(mediaList, filePath, fallbackItem) { !openedListed[0] && File(filePath).exists() }
    }

    val targetIndex = remember(displayList, filePath) { viewerTargetIndex(displayList, filePath) }

    val pagerState = key(isListReady) {
        rememberPagerState(
            initialPage = targetIndex,
            pageCount = { displayList.size }
        )
    }

    val currentItem = displayList.getOrElse(pagerState.currentPage.coerceIn(0, displayList.lastIndex)) { fallbackItem }

    var isCurrentPageZoomed by remember { mutableStateOf(false) }

    // Reset zoom when swiping to another item
    LaunchedEffect(pagerState.currentPage) {
        isCurrentPageZoomed = false
    }

    // Vault files are encrypted: show/share decrypted cache copies, keyed by the encrypted path
    val vaultViewPaths = remember { mutableStateMapOf<String, String>() }
    LaunchedEffect(mediaList, isVault) {
        if (isVault) {
            val items = if (mediaList.isEmpty()) listOf(fallbackItem) else mediaList
            for (m in items) {
                if (m.filePath !in vaultViewPaths) {
                    vaultRepository.viewFile(m.toVaultItem())?.let { vaultViewPaths[m.filePath] = it.path }
                }
            }
        }
    }
    fun viewPath(m: MediaItem): String? = if (isVault) vaultViewPaths[m.filePath] else m.filePath
    val currentViewPath = viewPath(currentItem)

    // Only short videos get an inline player; longer ones open the Video player (§1.8)
    val isInlineVideo = currentItem.isVideo && playsInline(currentItem.filePath, currentItem.durationMs)

    // Video Player lifecycle for active page
    // isListReady: device-checked, the player made for the pre-list fallback page was released during the first load
    LaunchedEffect(currentItem.filePath, currentViewPath, isInlineVideo, isListReady) {
        if (isInlineVideo && currentViewPath != null) {
            // The effect can run twice while the media list loads; the old player goes before a new one exists
            currentExoPlayer?.release()
            // Starts paused on the first frame (the slide show plays it) and stops at the end: no inline repeat
            val player = ExoPlayer.Builder(context).build().apply {
                val exoMedia = ExoMediaItem.fromUri(Uri.fromFile(File(currentViewPath)))
                setMediaItem(exoMedia)
                prepare()
                playWhenReady = slideshow
            }
            currentExoPlayer = player
            videoPositionMs = 0L
            videoDurationMs = currentItem.durationMs
            isVideoPlaying = false
            isVideoMuted = false
            videoSpeed = 1.0f

            try {
                while (currentExoPlayer === player) {
                    videoPositionMs = player.currentPosition.coerceAtLeast(0L)
                    val dur = player.duration
                    if (dur > 0) videoDurationMs = dur
                    isVideoPlaying = player.isPlaying
                    delay(200)
                }
            } finally {
                // Runs when the screen closes or another item is shown: each player releases itself,
                // so an earlier one can't keep playing audio in the background.
                player.release()
                if (currentExoPlayer === player) currentExoPlayer = null
            }
        } else {
            currentExoPlayer?.release()
            currentExoPlayer = null
            isVideoPlaying = false
            videoPositionMs = 0L
            videoDurationMs = 0L
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            currentExoPlayer?.release()
            currentExoPlayer = null
        }
    }
    // ON_PAUSE comes first on Back, on opening another screen and on Home, so audio stops before any transition
    LifecycleEventEffect(Lifecycle.Event.ON_PAUSE) { currentExoPlayer?.pause() }

    // One open dialog at a time, bound to the page it was opened on; a page change closes it
    var dialog by remember { mutableStateOf<ViewerDialog?>(null) }
    var busy by remember { mutableStateOf(false) }
    LaunchedEffect(currentItem.filePath) {
        dialog = null
        showMoreMenu = false
    }

    fun open(kind: ViewerDialogKind) {
        dialog = ViewerDialog(kind, currentItem.filePath)
    }

    // Runs a confirmed action on the page its dialog was opened for, or not at all
    fun confirm(opened: ViewerDialog, action: suspend (MediaItem) -> Unit) {
        dialog = null
        val item = currentItem.takeIf { opened.targetIfCurrent(it.filePath) != null } ?: return
        scope.launch {
            busy = true
            try {
                action(item)
            } finally {
                busy = false
            }
        }
    }

    fun toast(@StringRes message: Int) = Toast.makeText(context, message, Toast.LENGTH_SHORT).show()

    // ic_full_video_play: check the file plays (corrupt → toast), then open the Video player on the list's videos
    fun playExternal(path: String?) {
        if (path == null) return
        scope.launch {
            val ok = withContext(Dispatchers.IO) { isPlayableVideo(path) }
            if (ok) onNavigate(Screen.VideoPlayer.createRoute(path, folderPath, isRecycle, isVault))
            else toast(R.string.toast_corrupted_video)
        }
    }

    fun onDockAction(action: DockAction) {
        when (action) {
            DockAction.SHARE -> currentViewPath?.let { shareMedia(context, it, currentItem.isVideo) }
            DockAction.PRIVATE -> open(ViewerDialogKind.HIDE)
            DockAction.EDIT -> {
                val blocked = editBlockedMessage(currentItem.isAnimatedMedia, isDecodable(currentItem.filePath))
                if (blocked != null) toast(blocked) else onNavigate(Screen.Editor.createRoute(currentItem.filePath))
            }
            DockAction.ADD_TO -> open(ViewerDialogKind.ADD_TO)
            DockAction.DELETE -> open(
                when {
                    isRecycle -> ViewerDialogKind.RECYCLE_DELETE
                    isVault -> ViewerDialogKind.VAULT_DELETE
                    else -> ViewerDialogKind.DELETE
                }
            )
            DockAction.MORE -> showMoreMenu = true
            DockAction.RESTORE -> open(ViewerDialogKind.RESTORE)
            DockAction.UNHIDE -> open(ViewerDialogKind.UNHIDE)
        }
    }

    fun onMoreAction(action: MoreAction) {
        showMoreMenu = false
        when (action) {
            MoreAction.ADD_TO -> open(ViewerDialogKind.ADD_TO)
            MoreAction.RENAME -> open(ViewerDialogKind.RENAME)
            MoreAction.SLIDE_SHOW -> {
                val viewable = currentItem.isVideo || currentViewPath?.let(::isDecodable) == true
                if (slideshowAllowed(isRecycle, isVault) && viewable) {
                    slideSeconds = SLIDESHOW_DEFAULT_SECONDS
                    showControls = false
                    slideshow = true
                } else {
                    toast(R.string.something_went_wrong)
                }
            }
            MoreAction.WALLPAPER -> onNavigate(Screen.Wallpaper.createRoute(currentViewPath ?: currentItem.filePath))
            MoreAction.PRINT -> try {
                val printHelper = androidx.print.PrintHelper(context)
                printHelper.scaleMode = androidx.print.PrintHelper.SCALE_MODE_FIT
                // The Uri overload decodes off the main thread
                printHelper.printBitmap(currentItem.fileName, Uri.fromFile(File(currentViewPath ?: currentItem.filePath)))
            } catch (_: Exception) {
                toast(R.string.something_went_wrong)
            }
            MoreAction.SHOW_ON_MAP -> showOnMapRoute(currentItem)?.let(onNavigate)
        }
    }

    // More card shared by the normal and vault docks
    val moreMenu: @Composable () -> Unit = {
        if (showMoreMenu) {
            ViewerMoreCard(
                actions = moreActions(isVault = isVault, isVideo = currentItem.isVideo, hasPlace = showOnMapRoute(currentItem) != null),
                onAction = ::onMoreAction,
                onDismiss = { showMoreMenu = false }
            )
        }
    }

    // Edit suggestions: 1500 ms after a still page settles, hidden with the chrome, ✕ closes them for this page
    var suggestionsClosed by remember(currentItem.filePath) { mutableStateOf(false) }
    var suggestionsShown by remember { mutableStateOf(false) }
    val suggestionsAllowed = showControls && !slideshow && !suggestionsClosed && suggestionsEligible(currentItem, isRecycle, isVault)
    LaunchedEffect(currentItem.filePath, suggestionsAllowed, pagerState.isScrollInProgress) {
        suggestionsShown = false
        if (suggestionsAllowed && !pagerState.isScrollInProgress) {
            delay(SUGGESTION_DELAY_MS)
            suggestionsShown = true
        }
    }

    fun stopSlideshow() {
        slideshow = false
        showControls = true
    }
    // One timer, restarted by each page turn; it stops on the last page (MF runnable)
    LaunchedEffect(slideshow, pagerState.currentPage, slideSeconds) {
        if (!slideshow) return@LaunchedEffect
        delay(slideSeconds * 1000L)
        val next = nextSlidePage(pagerState.currentPage, displayList.lastIndex)
        if (next == null) stopSlideshow() else pagerState.animateScrollToPage(next)
    }
    // A changed list (sync, delete elsewhere) or leaving the app ends it, as does Back
    LaunchedEffect(displayList) { if (slideshow) stopSlideshow() }
    LifecycleEventEffect(Lifecycle.Event.ON_STOP) { if (slideshow) stopSlideshow() }
    BackHandler(enabled = slideshow) { stopSlideshow() }
    LaunchedEffect(currentExoPlayer, slideshow, slideMuted) {
        currentExoPlayer?.volume = if ((slideshow && slideMuted) || (!slideshow && isVideoMuted)) 0f else 1f
    }

    // 2.7.2 remains edge-to-edge while leaving Android system bars visible.
    val view = androidx.compose.ui.platform.LocalView.current
    DisposableEffect(Unit) {
        val window = (context as? android.app.Activity)?.window
        if (window != null) {
            val controller = androidx.core.view.WindowCompat.getInsetsController(window, view)
            controller.show(androidx.core.view.WindowInsetsCompat.Type.systemBars())
        }
        onDispose {
            view.keepScreenOn = false
            if (window != null) {
                androidx.core.view.WindowCompat.getInsetsController(window, view)
                    .show(androidx.core.view.WindowInsetsCompat.Type.systemBars())
            }
        }
    }

    // FLAG_KEEP_SCREEN_ON only while the slide show runs
    LaunchedEffect(slideshow) { view.keepScreenOn = slideshow }

    Box(
        modifier = Modifier
            .fillMaxSize()
            // 2.7.2: backgroundColor while the chrome shows, fullImageBackColor once it hides
            .background(colorResource(if (showControls) R.color.backgroundColor else R.color.fullImageBackColor))
    ) {
        // Main Media Horizontal Pager
        HorizontalPager(
            state = pagerState,
            key = { index -> displayList.getOrNull(index)?.filePath ?: index },
            userScrollEnabled = !isCurrentPageZoomed && !slideshow,
            modifier = Modifier.fillMaxSize()
        ) { page ->
            val item = displayList.getOrNull(page) ?: fallbackItem
            PagerMediaItemView(
                item = item,
                displayPath = viewPath(item),
                version = if (page == pagerState.currentPage) imageVersion else 0,
                exoPlayer = if (page == pagerState.currentPage) currentExoPlayer else null,
                onPlayExternal = { playExternal(viewPath(item)) },
                onToggleControls = { showControls = !showControls },
                onDismiss = onNavigateBack,
                onZoomChange = { zoomed ->
                    if (page == pagerState.currentPage) {
                        isCurrentPageZoomed = zoomed
                    }
                }
            )
        }

        ViewerTopChrome(
            visible = showControls,
            controls = viewerChromeControls(
                isVideo = currentItem.isVideo,
                isRecycle = isRecycle,
                isVault = isVault,
                isAnimated = currentItem.isAnimatedMedia,
                isHeif = currentItem.fileName.endsWith(".heif", ignoreCase = true),
                isShortVideoPlaying = isInlineVideo && isVideoPlaying
            ),
            isFavorite = currentItem.isFavorite,
            onBack = onNavigateBack,
            onRotate = {
                val item = currentItem
                if (!busy) scope.launch {
                    busy = true
                    val ok = mediaRepository.rotateImage(item.filePath)
                    busy = false
                    if (ok) imageVersion++ else toast(R.string.couldn_t_rotate_image)
                }
            },
            onFavorite = { scope.launch { mediaRepository.setFavorite(currentItem.filePath, !currentItem.isFavorite) } },
            onCapture = {
                // 2.7.2 shows no toast for either outcome
                val path = currentViewPath
                val at = currentExoPlayer?.currentPosition ?: 0L
                if (path != null) scope.launch { mediaRepository.saveVideoFrame(path, at) }
            },
            onDetails = { open(ViewerDialogKind.DETAILS) },
            modifier = Modifier.align(Alignment.TopCenter)
        )

        // Bottom overlays: the inline seek row and suggestion pills (hide at once), then the dock (slides with the chrome)
        Column(modifier = Modifier.align(Alignment.BottomCenter).fillMaxWidth()) {
            if (showControls && isInlineVideo && isVideoPlaying) {
                ShortVideoSeekRow(videoPositionMs, videoDurationMs) {
                    videoPositionMs = it
                    currentExoPlayer?.seekTo(it)
                }
            }
            if (suggestionsShown) {
                SuggestionStrip(
                    onClose = { suggestionsClosed = true },
                    onPick = { onNavigate(suggestionRoute(it, currentItem.filePath)) }
                )
            }
            AnimatedVisibility(
                visible = showControls,
                enter = slideInVertically(animationSpec = tween(CHROME_ANIMATION_MS)) { it } + fadeIn(tween(CHROME_ANIMATION_MS)),
                exit = slideOutVertically(animationSpec = tween(CHROME_ANIMATION_MS)) { it } + fadeOut(tween(CHROME_ANIMATION_MS))
            ) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    ViewerDock(
                        actions = dockActions(isRecycle = isRecycle, isVault = isVault, isVideo = currentItem.isVideo),
                        onAction = ::onDockAction,
                        moreMenu = moreMenu,
                        shortVideoRow = if (!isInlineVideo) null else {
                            {
                                ShortVideoRow(
                                    muted = isVideoMuted,
                                    playing = isVideoPlaying,
                                    speed = videoSpeed,
                                    onToggleMute = {
                                        isVideoMuted = !isVideoMuted
                                        currentExoPlayer?.volume = if (isVideoMuted) 0f else 1f
                                    },
                                    onTogglePlay = {
                                        currentExoPlayer?.let { p ->
                                            val wasPlaying = p.isPlaying
                                            if (wasPlaying) {
                                                p.pause()
                                            } else {
                                                if (restartsFromZero(p.playbackState)) p.seekTo(0L)
                                                p.play()
                                            }
                                            isVideoPlaying = !wasPlaying
                                        }
                                    },
                                    onSpeed = { speed ->
                                        videoSpeed = speed
                                        currentExoPlayer?.setPlaybackSpeed(speed)
                                    }
                                )
                            }
                        }
                    )
                }
            }
        }

        if (slideshow) {
            SlideshowControls(
                seconds = slideSeconds,
                muted = slideMuted,
                onClose = ::stopSlideshow,
                onToggleMute = { slideMuted = !slideMuted },
                onCycleSeconds = { slideSeconds = nextSlideSeconds(slideSeconds) }
            )
        }

        if (busy) {
            CircularProgressIndicator(color = colorResource(R.color.colorPrimary), modifier = Modifier.align(Alignment.Center))
        }
    }

    // Collected outside the dialog branch: collecting inside it crashed the Recycle Bin viewer (ClassCastException in the slot table)
    val albums by remember { mediaRepository.getAlbumsFlow() }.collectAsState(initial = emptyList())
    dialog?.let { opened ->
        ViewerDialogHost(
            dialog = opened,
            item = currentItem,
            albums = albums,
            onConfirm = {
                confirm(opened) { item ->
                    val ok = when (opened.kind) {
                        ViewerDialogKind.RECYCLE_DELETE -> { mediaRepository.deletePermanently(item.filePath); true }
                        ViewerDialogKind.VAULT_DELETE -> vaultRepository.deleteFromVault(item.toVaultItem())
                        ViewerDialogKind.RESTORE -> { mediaRepository.restoreFromTrash(item.filePath); true }
                        ViewerDialogKind.HIDE -> vaultRepository.moveToVault(item)
                        ViewerDialogKind.UNHIDE -> vaultRepository.restoreFromVault(item.toVaultItem())
                        else -> false
                    }
                    if (!ok) toast(R.string.something_went_wrong)
                    // 2.7.2 stays open on the next page; only the last page closes the viewer
                    else if (displayList.size <= 1) onNavigateBack()
                }
            },
            onDelete = { moveToBin ->
                confirm(opened) { item ->
                    if (moveToBin) mediaRepository.moveToTrash(item.filePath) else mediaRepository.deletePermanently(item.filePath)
                    if (displayList.size <= 1) onNavigateBack()
                }
            },
            onRename = { name ->
                confirm(opened) { item ->
                    val ok = if (isVault) vaultRepository.renameInVault(item.toVaultItem(), name)
                    else mediaRepository.renameMedia(item.filePath, name)
                    // No success toast in the reference; a failed rename must not look like one
                    if (!ok) toast(R.string.something_went_wrong)
                }
            },
            onPickAlbum = { folder -> if (opened.targetIfCurrent(currentItem.filePath) != null) dialog = opened.copy(folder = folder) },
            onAdd = { move ->
                confirm(opened) { item ->
                    if (move && item.folderPath == opened.folder) {
                        toast(R.string.media_files_should_not_move_to_same_folder)
                    } else {
                        val ok = if (move) mediaRepository.moveMediaToFolder(item.filePath, opened.folder)
                        else mediaRepository.copyMediaToFolder(item.filePath, opened.folder)
                        toast(if (!ok) R.string.something_went_wrong else if (move) R.string.items_moved_successfully else R.string.items_copied_successfully)
                    }
                }
            },
            onDismiss = { dialog = null }
        )
    }
}

@OptIn(UnstableApi::class, ExperimentalFoundationApi::class)
@Composable
private fun PagerMediaItemView(
    item: MediaItem,
    displayPath: String?,
    version: Int,
    exoPlayer: ExoPlayer?,
    onPlayExternal: () -> Unit,
    onToggleControls: () -> Unit,
    onDismiss: () -> Unit = {},
    onZoomChange: (Boolean) -> Unit = {}
) {
    val context = LocalContext.current
    var scale by remember(item.filePath) { mutableFloatStateOf(1f) }
    var offset by remember(item.filePath) { mutableStateOf(Offset.Zero) }
    var containerSize by remember { mutableStateOf(IntSize.Zero) }
    var imageSize by remember(item.filePath) { mutableStateOf(androidx.compose.ui.geometry.Size.Zero) }
    var dismissDrag by remember(item.filePath) { mutableFloatStateOf(0f) }

    val isZoomed = scale > 1.05f

    LaunchedEffect(isZoomed) {
        onZoomChange(isZoomed)
    }

    val containerF = androidx.compose.ui.geometry.Size(containerSize.width.toFloat(), containerSize.height.toFloat())
    // shortcut: ignores 90/270 rotation when fitting; pan limits are approximate on rotated photos.
    val fitted = ViewerZoom.fittedSize(imageSize, containerF)
    val maxZoom = ViewerZoom.maxZoomFor(
        width = imageSize.width,
        height = imageSize.height,
        isAnimated = item.isAnimated || item.mediaType == MediaType.ANIMATED
    )
    val scope = rememberCoroutineScope()

    fun clampOffset(proposedOffset: Offset, currentScale: Float): Offset {
        if (containerSize.width == 0 || currentScale <= 1.05f) return Offset.Zero
        return ViewerZoom.clamp(proposedOffset, currentScale, fitted, containerF)
    }

    // Swipe down (when not zoomed) closes the viewer, like the reference
    val dismissModifier = Modifier.pointerInput(item.filePath) {
        detectVerticalDragGestures(
            onDragEnd = {
                if (dismissDrag > 220f) onDismiss() else dismissDrag = 0f
            },
            onDragCancel = { dismissDrag = 0f }
        ) { change, dy ->
            if (scale <= 1.05f) {
                change.consume()
                dismissDrag = (dismissDrag + dy).coerceAtLeast(0f)
            }
        }
    }
    // Drag feedback for photos only: a graphicsLayer stops the video SurfaceView from drawing
    val dismissVisual = Modifier.graphicsLayer(
        translationY = dismissDrag,
        alpha = 1f - (dismissDrag / 1200f).coerceIn(0f, 0.6f)
    )

    val transformState = rememberTransformableState { zoomChange, offsetChange, _ ->
        val newScale = (scale * zoomChange).coerceIn(ViewerZoom.FIT_SCALE, maxZoom * ViewerZoom.OVERZOOM_FACTOR)
        scale = newScale
        if (newScale <= 1.05f) {
            scale = 1f
            offset = Offset.Zero
        } else {
            offset = clampOffset(offset + offsetChange, newScale)
        }
    }

    LaunchedEffect(transformState.isTransformInProgress) {
        if (!transformState.isTransformInProgress && scale > maxZoom) {
            scale = maxZoom
            offset = clampOffset(offset, scale)
        }
    }

    if (item.mediaType == MediaType.VIDEO) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .then(dismissModifier)
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = { onToggleControls() }
                ),
            contentAlignment = Alignment.Center
        ) {
            if (exoPlayer != null) {
                AndroidView(
                    factory = { ctx ->
                        PlayerView(ctx).apply {
                            this.player = exoPlayer
                            useController = false
                            isClickable = false
                            isFocusable = false
                            setOnTouchListener { _, _ -> false }
                        }
                    },
                    update = { view ->
                        if (view.player != exoPlayer) {
                            view.player = exoPlayer
                        }
                    },
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                // Video thumbnail fallback when swiped away
                AsyncImage(
                    model = ImageRequest.Builder(context)
                        .data(displayPath?.let { File(it) })
                        .crossfade(false)
                        .build(),
                    contentDescription = item.fileName,
                    contentScale = ContentScale.Fit,
                    modifier = Modifier.fillMaxSize()
                )
                if (!playsInline(item.filePath, item.durationMs)) {
                    Icon(
                        painter = painterResource(R.drawable.ic_full_video_play),
                        contentDescription = stringResource(androidx.media3.ui.R.string.exo_controls_play_description),
                        tint = Color.Unspecified,
                        modifier = Modifier
                            .clip(CircleShape)
                            .clickable(onClick = onPlayExternal)
                            .padding(30.sdp)
                    )
                }
            }
        }
    } else {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .onSizeChanged { containerSize = it }
                .then(dismissModifier)
                .then(dismissVisual)
                .pointerInput(item.filePath, maxZoom, fitted, containerF) {
                    detectTapGestures(
                        onTap = { onToggleControls() },
                        onDoubleTap = { tap ->
                            val start = scale
                            val target = if (scale > 1.05f) ViewerZoom.FIT_SCALE else maxZoom
                            scope.launch {
                                animate(start, target, animationSpec = tween(ViewerZoom.DOUBLE_TAP_DURATION_MS)) { value, _ ->
                                    scale = value
                                    offset = if (target == ViewerZoom.FIT_SCALE) {
                                        Offset.Zero
                                    } else {
                                        ViewerZoom.offsetForTap(tap, value, fitted, containerF)
                                    }
                                }
                            }
                        }
                    )
                },
            contentAlignment = Alignment.Center
        ) {
            AsyncImage(
                model = ImageRequest.Builder(context)
                    .data(displayPath?.let { File(it) })
                    // A new version is a new request; Coil's file key carries the rotated file's new mtime
                    .setParameter("version", version)
                    .crossfade(false)
                    .build(),
                contentDescription = item.fileName,
                contentScale = ContentScale.Fit,
                onSuccess = { imageSize = it.painter.intrinsicSize },
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer(
                        scaleX = scale,
                        scaleY = scale,
                        translationX = offset.x,
                        translationY = offset.y
                    )
                    .transformable(
                        state = transformState,
                        canPan = { isZoomed },
                        enabled = true
                    )
                    .then(
                        if (isZoomed) {
                            Modifier.pointerInput(item.filePath, isZoomed) {
                                detectDragGestures { change, dragAmount ->
                                    change.consume()
                                    offset = clampOffset(offset + dragAmount, scale)
                                }
                            }
                        } else Modifier
                    )
            )
        }
    }
}

private fun shareMedia(context: Context, path: String, isVideo: Boolean) {
    try {
        val file = File(path)
        if (!file.exists()) return
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.provider", file)
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = if (isVideo) "video/*" else "image/*"
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(intent, null))
    } catch (_: Exception) {}
}

/** The viewer's pages: the source list, plus the opened file when the list lacks it but it is still on disk. */
internal fun viewerDisplayList(
    mediaList: List<MediaItem>,
    filePath: String,
    fallbackItem: MediaItem,
    fileExists: () -> Boolean
): List<MediaItem> = when {
    mediaList.isEmpty() -> listOf(fallbackItem)
    // A renamed or removed opened file must not come back as a stale extra page
    mediaList.any { it.filePath == filePath } || !fileExists() -> mediaList
    else -> listOf(fallbackItem) + mediaList
}

/** The page of the opened file, or the first page when it is not in the list. */
internal fun viewerTargetIndex(displayList: List<MediaItem>, filePath: String): Int =
    displayList.indexOfFirst { it.filePath == filePath }.coerceAtLeast(0)

/** The list a viewer (or the Video player it opens) pages through. */
internal fun mediaSourceFlow(
    mediaRepository: MediaRepository,
    vaultRepository: VaultRepository,
    folderPath: String,
    isRecycle: Boolean,
    isVault: Boolean
): Flow<List<MediaItem>> = when {
    isRecycle -> mediaRepository.getRecycleBinFlow()
    isVault -> vaultRepository.getVaultItemsFlow().map { list -> list.map { it.toMediaItem() } }
    folderPath == "filter:favorites" -> mediaRepository.getFavoritesFlow()
    folderPath.isNotEmpty() -> mediaRepository.getMediaByFolderFlow(folderPath)
    else -> mediaRepository.getActiveMediaFlow()
}

private fun VaultItem.toMediaItem() = MediaItem(
    id = 0,
    filePath = filePath,
    fileName = fileName,
    fileSize = fileSize,
    folderName = folderName,
    folderPath = folderPath,
    dateModified = dateHidden,
    dateTaken = dateHidden,
    mediaType = mediaType,
    durationMs = durationMs,
    dimension = dimension
)

private fun MediaItem.toVaultItem() = VaultItem(
    filePath = filePath,
    fileName = fileName,
    folderName = folderName,
    folderPath = folderPath,
    fileSize = fileSize,
    dateHidden = if (dateModified > 0) dateModified else System.currentTimeMillis() / 1000,
    mediaType = mediaType,
    durationMs = durationMs,
    dimension = dimension
)

internal fun formatDuration(durationMs: Long): String {
    if (durationMs <= 0) return "00:00"
    val totalSec = durationMs / 1000
    val hours = totalSec / 3600
    val minutes = (totalSec % 3600) / 60
    val seconds = totalSec % 60
    return if (hours > 0) {
        String.format(Locale.US, "%02d:%02d:%02d", hours, minutes, seconds)
    } else {
        String.format(Locale.US, "%02d:%02d", minutes, seconds)
    }
}
