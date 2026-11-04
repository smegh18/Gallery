@file:kotlin.OptIn(
    androidx.compose.foundation.ExperimentalFoundationApi::class,
    androidx.media3.common.util.UnstableApi::class
)

package com.quizango.gallery.ui.viewer

import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.widget.Toast
import androidx.annotation.OptIn
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.gestures.rememberTransformableState
import androidx.compose.foundation.gestures.transformable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Folder
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.text.SimpleDateFormat
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
    var showSuggestionStrip by remember { mutableStateOf(!isRecycle && !isVault) }
    var showInfoDialog by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    var showPermanentDeleteDialog by remember { mutableStateOf(false) }
    var deleteMoveToBin by remember { mutableStateOf(true) }
    var showRenameDialog by remember { mutableStateOf(false) }
    var renameInput by remember { mutableStateOf("") }
    var showAddToDialog by remember { mutableStateOf(false) }
    var showMoreMenu by remember { mutableStateOf(false) }
    var vaultConfirm by remember { mutableStateOf<String?>(null) } // "hide" | "unhide"
    var rotationAngle by remember { mutableFloatStateOf(0f) }

    // Video Playback State
    var currentExoPlayer by remember { mutableStateOf<ExoPlayer?>(null) }
    var isVideoPlaying by remember { mutableStateOf(false) }
    var isVideoMuted by remember { mutableStateOf(false) }
    var videoSpeed by remember { mutableFloatStateOf(1.0f) }
    var videoPositionMs by remember { mutableLongStateOf(0L) }
    var videoDurationMs by remember { mutableLongStateOf(0L) }
    var showSpeedMenu by remember { mutableStateOf(false) }

    // Collect media items for this album/folder, recycle bin, vault, or all media
    val mediaList by remember(folderPath, isRecycle, isVault) {
        when {
            isRecycle -> mediaRepository.getRecycleBinFlow()
            isVault -> vaultRepository.getVaultItemsFlow().map { list -> list.map { it.toMediaItem() } }
            folderPath == "filter:favorites" -> mediaRepository.getFavoritesFlow()
            folderPath.isNotEmpty() -> mediaRepository.getMediaByFolderFlow(folderPath)
            else -> mediaRepository.getActiveMediaFlow()
        }
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

    val displayList = remember(mediaList, fallbackItem) {
        if (mediaList.isNotEmpty()) {
            if (mediaList.any { it.filePath == filePath }) {
                mediaList
            } else {
                listOf(fallbackItem) + mediaList
            }
        } else {
            listOf(fallbackItem)
        }
    }

    val targetIndex = remember(displayList, filePath) {
        val idx = displayList.indexOfFirst { it.filePath == filePath }
        if (idx >= 0) idx else 0
    }

    val pagerState = key(isListReady) {
        rememberPagerState(
            initialPage = targetIndex,
            pageCount = { displayList.size }
        )
    }

    val currentItem = displayList.getOrElse(pagerState.currentPage.coerceIn(0, displayList.lastIndex)) { fallbackItem }

    var isCurrentPageZoomed by remember { mutableStateOf(false) }

    // Reset rotation, zoom and suggestions when swiping to another item
    LaunchedEffect(pagerState.currentPage) {
        rotationAngle = 0f
        isCurrentPageZoomed = false
        showSuggestionStrip = !isRecycle && !isVault
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

    // Video Player lifecycle for active page
    LaunchedEffect(currentItem.filePath, currentViewPath) {
        if (currentItem.mediaType == MediaType.VIDEO && currentViewPath != null) {
            val player = ExoPlayer.Builder(context).build().apply {
                val exoMedia = ExoMediaItem.fromUri(Uri.fromFile(File(currentViewPath)))
                setMediaItem(exoMedia)
                repeatMode = ExoPlayer.REPEAT_MODE_ONE
                prepare()
                playWhenReady = true
            }
            // The effect can run twice while the media list loads; only the newest player may play
            currentExoPlayer?.release()
            currentExoPlayer = player
            isVideoPlaying = true
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

    // Formatted date and time matching reference: "28 Oct", "03:04"
    val epochTime = remember(currentItem.dateAdded, currentItem.dateModified) {
        val epoch = if (currentItem.dateAdded > 0) currentItem.dateAdded else currentItem.dateModified
        val millis = if (epoch > 10_000_000_000L) epoch else epoch * 1000L
        Date(millis)
    }
    val dateText = remember(epochTime) {
        SimpleDateFormat("dd MMM", Locale.getDefault()).format(epochTime)
    }
    val timeText = remember(epochTime) {
        SimpleDateFormat("hh:mm", Locale.getDefault()).format(epochTime)
    }

    // More menu shared by the normal and vault bottom bars (vault: Rename, Set as Wallpaper, Print)
    val moreMenu: @Composable () -> Unit = {
                                        DropdownMenu(
                                            expanded = showMoreMenu,
                                            onDismissRequest = { showMoreMenu = false },
                                            modifier = Modifier
                                                .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(16.dp))
                                                .padding(vertical = 4.dp)
                                        ) {
                                            if (!isVault) DropdownMenuItem(
                                                text = {
                                                    Text(
                                                        stringResource(R.string.add_to),
                                                        color = MaterialTheme.colorScheme.onSurface,
                                                        fontSize = 14.sp,
                                                        fontWeight = FontWeight.Medium
                                                    )
                                                },
                                                onClick = {
                                                    showMoreMenu = false
                                                    showAddToDialog = true
                                                },
                                                leadingIcon = {
                                                    Icon(
                                                        painterResource(id = R.drawable.ic_add),
                                                        contentDescription = null,
                                                        tint = MaterialTheme.colorScheme.onSurface,
                                                        modifier = Modifier.size(20.dp)
                                                    )
                                                }
                                            )
                                            DropdownMenuItem(
                                                text = {
                                                    Text(
                                                        stringResource(R.string.rename),
                                                        color = MaterialTheme.colorScheme.onSurface,
                                                        fontSize = 14.sp,
                                                        fontWeight = FontWeight.Medium
                                                    )
                                                },
                                                onClick = {
                                                    showMoreMenu = false
                                                    renameInput = File(currentItem.filePath).nameWithoutExtension
                                                    showRenameDialog = true
                                                },
                                                leadingIcon = {
                                                    Icon(
                                                        painterResource(id = R.drawable.ic_rename),
                                                        contentDescription = null,
                                                        tint = MaterialTheme.colorScheme.onSurface,
                                                        modifier = Modifier.size(20.dp)
                                                    )
                                                }
                                            )
                                            DropdownMenuItem(
                                                text = {
                                                    Text(
                                                        stringResource(R.string.slide_show),
                                                        color = MaterialTheme.colorScheme.onSurface,
                                                        fontSize = 14.sp,
                                                        fontWeight = FontWeight.Medium
                                                    )
                                                },
                                                onClick = {
                                                    showMoreMenu = false
                                                    onNavigate(Screen.StoryView.createRoute(if (isVault) "vault" else folderPath.ifEmpty { "all" }, "Slide show"))
                                                },
                                                leadingIcon = {
                                                    Icon(
                                                        painterResource(id = R.drawable.ic_slideshow),
                                                        contentDescription = null,
                                                        tint = MaterialTheme.colorScheme.onSurface,
                                                        modifier = Modifier.size(20.dp)
                                                    )
                                                }
                                            )
                                            if (currentItem.mediaType.isImage) {
                                                DropdownMenuItem(
                                                    text = {
                                                        Text(
                                                            stringResource(R.string.set_as_wallpaper),
                                                            color = MaterialTheme.colorScheme.onSurface,
                                                            fontSize = 14.sp,
                                                            fontWeight = FontWeight.Medium
                                                        )
                                                    },
                                                    onClick = {
                                                        showMoreMenu = false
                                                        onNavigate(Screen.Wallpaper.createRoute(currentViewPath ?: currentItem.filePath))
                                                    },
                                                    leadingIcon = {
                                                        Icon(
                                                            painterResource(id = R.drawable.ic_set_as),
                                                            contentDescription = null,
                                                            tint = MaterialTheme.colorScheme.onSurface,
                                                            modifier = Modifier.size(20.dp)
                                                        )
                                                    }
                                                )
                                                DropdownMenuItem(
                                                    text = {
                                                        Text(
                                                            stringResource(R.string.print),
                                                            color = MaterialTheme.colorScheme.onSurface,
                                                            fontSize = 14.sp,
                                                            fontWeight = FontWeight.Medium
                                                        )
                                                    },
                                                    onClick = {
                                                        showMoreMenu = false
                                                        try {
                                                            val printHelper = androidx.print.PrintHelper(context)
                                                            printHelper.scaleMode = androidx.print.PrintHelper.SCALE_MODE_FIT
                                                            val bitmap = BitmapFactory.decodeFile(currentViewPath ?: currentItem.filePath)
                                                            if (bitmap != null) {
                                                                printHelper.printBitmap("Print ${currentItem.fileName}", bitmap)
                                                            }
                                                        } catch (_: Exception) {}
                                                    },
                                                    leadingIcon = {
                                                        Icon(
                                                            painterResource(id = R.drawable.ic_print),
                                                            contentDescription = null,
                                                            tint = MaterialTheme.colorScheme.onSurface,
                                                            modifier = Modifier.size(20.dp)
                                                        )
                                                    }
                                                )
                                            }
                                        }
    }

    // Reference: tapping hides the bars and shows the photo full screen on black
    val view = androidx.compose.ui.platform.LocalView.current
    DisposableEffect(showControls) {
        val window = (context as? android.app.Activity)?.window
        if (window != null) {
            val controller = androidx.core.view.WindowCompat.getInsetsController(window, view)
            if (showControls) {
                controller.show(androidx.core.view.WindowInsetsCompat.Type.systemBars())
            } else {
                controller.systemBarsBehavior =
                    androidx.core.view.WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
                controller.hide(androidx.core.view.WindowInsetsCompat.Type.systemBars())
            }
        }
        onDispose {
            if (window != null) {
                androidx.core.view.WindowCompat.getInsetsController(window, view)
                    .show(androidx.core.view.WindowInsetsCompat.Type.systemBars())
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(if (showControls) MaterialTheme.colorScheme.background else Color.Black)
    ) {
        // Main Media Horizontal Pager
        HorizontalPager(
            state = pagerState,
            key = { index -> displayList.getOrNull(index)?.filePath ?: index },
            userScrollEnabled = !isCurrentPageZoomed,
            modifier = Modifier.fillMaxSize()
        ) { page ->
            val item = displayList.getOrNull(page) ?: fallbackItem
            PagerMediaItemView(
                item = item,
                displayPath = viewPath(item),
                rotationAngle = if (page == pagerState.currentPage) rotationAngle else 0f,
                exoPlayer = if (page == pagerState.currentPage) currentExoPlayer else null,
                onToggleControls = { showControls = !showControls },
                onDismiss = onNavigateBack,
                onZoomChange = { zoomed ->
                    if (page == pagerState.currentPage) {
                        isCurrentPageZoomed = zoomed
                    }
                }
            )
        }

        // Top Toolbar (Matches activity_media_full_screen.xml toolbarLay)
        AnimatedVisibility(
            visible = showControls,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier.align(Alignment.TopCenter)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surface)
                    .statusBarsPadding()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                        .padding(start = 4.dp, end = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_back),
                            contentDescription = "Back",
                            tint = Color.Unspecified
                        )
                    }

                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .padding(start = 6.dp)
                    ) {
                        Text(
                            text = dateText,
                            color = MaterialTheme.colorScheme.onSurface,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = timeText,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            maxLines = 1
                        )
                    }

                    // Video Frame Screenshot Capture Button (Reference captureBtn / ic_camera_ss_short)
                    if (currentItem.mediaType == MediaType.VIDEO) {
                        IconButton(onClick = {
                            scope.launch(Dispatchers.IO) {
                                val bitmap = captureVideoFrame(currentItem.filePath, videoPositionMs)
                                if (bitmap != null) {
                                    val saved = saveCapturedFrameToGallery(context, bitmap, currentItem.fileName)
                                    if (saved != null) {
                                        mediaRepository.syncMediaStore()
                                        withContext(Dispatchers.Main) {
                                            Toast.makeText(context, "Screenshot captured & saved to Gallery", Toast.LENGTH_SHORT).show()
                                        }
                                    }
                                } else {
                                    withContext(Dispatchers.Main) {
                                        Toast.makeText(context, "Failed to capture screenshot", Toast.LENGTH_SHORT).show()
                                    }
                                }
                            }
                        }) {
                            Icon(
                                painter = painterResource(id = R.drawable.ic_camera_ss_short),
                                contentDescription = "Capture Screenshot",
                                tint = Color.Unspecified,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }

                    // Rotate Button (Only for images and non-recycle mode, matching reference)
                    if (currentItem.mediaType.isImage && !isRecycle) {
                        IconButton(onClick = { rotationAngle = (rotationAngle + 90f) % 360f }) {
                            Icon(
                                painter = painterResource(id = R.drawable.ic_media_rotate),
                                contentDescription = "Rotate",
                                tint = Color.Unspecified
                            )
                        }
                    }

                    // Favorite Button (Red fill if favorite, outline otherwise; hidden in recycle and vault)
                    if (!isRecycle && !isVault) {
                        IconButton(onClick = {
                            scope.launch {
                                mediaRepository.setFavorite(currentItem.filePath, !currentItem.isFavorite)
                            }
                        }) {
                            Icon(
                                painter = painterResource(
                                    id = if (currentItem.isFavorite) R.drawable.ic_fav_fill else R.drawable.ic_favorite
                                ),
                                contentDescription = "Favorite",
                                tint = Color.Unspecified
                            )
                        }
                    }

                    // Property / Info Details Button (not in the reference vault viewer)
                    if (!isVault) IconButton(onClick = { showInfoDialog = true }) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_property),
                            contentDescription = "Details",
                            tint = Color.Unspecified
                        )
                    }
                }

                HorizontalDivider(color = MaterialTheme.colorScheme.settingsDivider, thickness = 1.dp)
            }
        }

        // Bottom Overlays (Suggestion Pill Strip + Video Scrubber / Controls + Bottom Action Bar)
        AnimatedVisibility(
            visible = showControls,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier.align(Alignment.BottomCenter)
        ) {
            Column(
                modifier = Modifier.fillMaxWidth()
            ) {
                // Floating Suggestion Pill Strip (Matches item_suggestion_layout.xml)
                if (showSuggestionStrip && currentItem.mediaType.isImage && !isRecycle && !isVault) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 8.dp)
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Close button pill
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(CircleShape)
                                .background(Color(0xFF242730))
                                .clickable { showSuggestionStrip = false },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                painter = painterResource(id = R.drawable.ic_close),
                                contentDescription = "Close suggestions",
                                tint = Color.White,
                                modifier = Modifier.size(12.dp)
                            )
                        }

                        // Edit suggestions
                        SuggestionPill(iconRes = R.drawable.ic_edit_small_crop, label = stringResource(R.string.crop)) {
                            onNavigate(Screen.Editor.createRoute(currentItem.filePath, initialTab = "crop"))
                        }
                        SuggestionPill(iconRes = R.drawable.ic_edit_small_draw, label = stringResource(R.string.markup)) {
                            onNavigate(Screen.Editor.createRoute(currentItem.filePath, initialTab = "brush"))
                        }
                        SuggestionPill(iconRes = R.drawable.ic_edit_small_adjust, label = stringResource(R.string.adjust)) {
                            onNavigate(Screen.Editor.createRoute(currentItem.filePath, initialTab = "adjust"))
                        }
                        SuggestionPill(iconRes = R.drawable.ic_edit_small_filter, label = stringResource(R.string.filter)) {
                            onNavigate(Screen.Editor.createRoute(currentItem.filePath, initialTab = "filter"))
                        }
                        SuggestionPill(iconRes = R.drawable.ic_edit_small_remove_bg, label = stringResource(R.string.remove_background)) {
                            onNavigate(Screen.RemoveBg.createRoute(currentItem.filePath))
                        }
                        SuggestionPill(iconRes = R.drawable.ic_edit_small_blur_bg, label = stringResource(R.string.blur_background)) {
                            onNavigate(Screen.BlurBg.createRoute(currentItem.filePath))
                        }
                        SuggestionPill(iconRes = R.drawable.ic_edit_small_color_splash, label = stringResource(R.string.color_splash)) {
                            onNavigate(Screen.ColorSplash.createRoute(currentItem.filePath))
                        }
                    }
                }

                // Video Scrubber & Playback Controls (Matches activity_media_full_screen.xml progressLay & videoControlBottom)
                if (currentItem.mediaType == MediaType.VIDEO) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(MaterialTheme.colorScheme.surface)
                            .padding(top = 6.dp)
                    ) {
                        // Progress / Scrubber Bar (videoSeek)
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp)
                        ) {
                            Slider(
                                value = if (videoDurationMs > 0) videoPositionMs.toFloat().coerceIn(0f, videoDurationMs.toFloat()) else 0f,
                                onValueChange = { newPos ->
                                    videoPositionMs = newPos.toLong()
                                    currentExoPlayer?.seekTo(newPos.toLong())
                                },
                                valueRange = 0f..(if (videoDurationMs > 0) videoDurationMs.toFloat() else 1f),
                                colors = SliderDefaults.colors(
                                    thumbColor = GalleryBlue,
                                    activeTrackColor = GalleryBlue,
                                    inactiveTrackColor = MaterialTheme.colorScheme.cardBorder
                                ),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(24.dp)
                            )
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 4.dp),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = formatDuration(videoPositionMs),
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = formatDuration(if (videoDurationMs > 0) videoDurationMs else currentItem.durationMs),
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        // Video Playback Controls (shortMuteBtn, shortPlayBtn, shortSpeedBtn)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 32.dp, vertical = 6.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // 1. Mute / Unmute
                            IconButton(
                                onClick = {
                                    if (isVideoMuted) {
                                        currentExoPlayer?.volume = 1f
                                        isVideoMuted = false
                                    } else {
                                        currentExoPlayer?.volume = 0f
                                        isVideoMuted = true
                                    }
                                }
                            ) {
                                Icon(
                                    painter = painterResource(
                                        id = if (isVideoMuted) R.drawable.ic_short_video_mute else R.drawable.ic_short_video_unmute
                                    ),
                                    contentDescription = if (isVideoMuted) "Unmute" else "Mute",
                                    tint = Color.Unspecified,
                                    modifier = Modifier.size(26.dp)
                                )
                            }

                            // 2. Play / Pause
                            IconButton(
                                onClick = {
                                    if (currentExoPlayer?.isPlaying == true) {
                                        currentExoPlayer?.pause()
                                        isVideoPlaying = false
                                    } else {
                                        currentExoPlayer?.play()
                                        isVideoPlaying = true
                                    }
                                },
                                modifier = Modifier
                                    .size(46.dp)
                                    .clip(CircleShape)
                                    .background(GalleryBlue.copy(alpha = 0.12f))
                            ) {
                                Icon(
                                    painter = painterResource(
                                        id = if (isVideoPlaying) R.drawable.ic_short_video_pause else R.drawable.ic_short_video_play
                                    ),
                                    contentDescription = if (isVideoPlaying) "Pause" else "Play",
                                    tint = Color.Unspecified,
                                    modifier = Modifier.size(26.dp)
                                )
                            }

                            // 3. Speed Menu
                            Box {
                                IconButton(onClick = { showSpeedMenu = true }) {
                                    Icon(
                                        painter = painterResource(id = R.drawable.ic_short_video_speed),
                                        contentDescription = "Playback Speed",
                                        tint = Color.Unspecified,
                                        modifier = Modifier.size(26.dp)
                                    )
                                }

                                DropdownMenu(
                                    expanded = showSpeedMenu,
                                    onDismissRequest = { showSpeedMenu = false },
                                    modifier = Modifier
                                        .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(16.dp))
                                        .padding(vertical = 4.dp)
                                        .width(130.dp)
                                ) {
                                    val speeds = listOf(0.25f, 0.5f, 1.0f, 1.5f, 2.0f)
                                    speeds.forEach { speed ->
                                        DropdownMenuItem(
                                            text = {
                                                Row(
                                                    modifier = Modifier.fillMaxWidth(),
                                                    horizontalArrangement = Arrangement.SpaceBetween,
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    Text(
                                                        text = "${speed}x",
                                                        color = MaterialTheme.colorScheme.onSurface,
                                                        fontSize = 14.sp,
                                                        fontWeight = if (videoSpeed == speed) FontWeight.Bold else FontWeight.Medium
                                                    )
                                                    if (videoSpeed == speed) {
                                                        Icon(
                                                            painter = painterResource(id = R.drawable.ic_short_video_speed_select),
                                                            contentDescription = "Selected",
                                                            tint = Color.Unspecified,
                                                            modifier = Modifier.size(16.dp)
                                                        )
                                                    }
                                                }
                                            },
                                            onClick = {
                                                videoSpeed = speed
                                                currentExoPlayer?.setPlaybackSpeed(speed)
                                                showSpeedMenu = false
                                            }
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // Bottom Action Bar (Matches item_bottom_option_layout.xml: normal, recycler, or hidden action)
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    color = MaterialTheme.colorScheme.surface,
                    shadowElevation = 8.dp
                ) {
                    Column(
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        HorizontalDivider(color = MaterialTheme.colorScheme.settingsDivider, thickness = 1.dp)

                        when {
                            isRecycle -> {
                                // Mode B: Recycle Bin (recyclerBottomAction)
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .navigationBarsPadding()
                                        .padding(vertical = 10.dp),
                                    horizontalArrangement = Arrangement.SpaceAround,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    BottomActionButton(
                                        iconRes = R.drawable.ic_restore,
                                        label = stringResource(R.string.restore)
                                    ) {
                                        scope.launch {
                                            mediaRepository.restoreFromTrash(currentItem.filePath)
                                            Toast.makeText(context, "Restored to Gallery", Toast.LENGTH_SHORT).show()
                                            onNavigateBack()
                                        }
                                    }
                                    BottomActionButton(
                                        iconRes = R.drawable.ic_delete,
                                        label = stringResource(R.string.delete)
                                    ) {
                                        showPermanentDeleteDialog = true
                                    }
                                }
                            }
                            isVault -> {
                                // Mode C: Vault / Hidden (hiddenBottomAction)
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .navigationBarsPadding()
                                        .padding(vertical = 10.dp),
                                    horizontalArrangement = Arrangement.SpaceAround,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    BottomActionButton(
                                        iconRes = R.drawable.ic_share,
                                        label = stringResource(R.string.share)
                                    ) {
                                        currentViewPath?.let { shareMedia(context, it) }
                                    }
                                    BottomActionButton(
                                        iconRes = R.drawable.ic_unhide,
                                        label = stringResource(R.string.unhide)
                                    ) {
                                        vaultConfirm = "unhide"
                                    }
                                    BottomActionButton(
                                        iconRes = R.drawable.ic_delete,
                                        label = stringResource(R.string.delete)
                                    ) {
                                        showPermanentDeleteDialog = true
                                    }
                                    Box {
                                        BottomActionButton(
                                            iconRes = R.drawable.ic_more,
                                            label = stringResource(R.string.more)
                                        ) {
                                            showMoreMenu = true
                                        }
                                        moreMenu()
                                    }
                                }
                            }
                            else -> {
                                // Mode A: Normal Gallery / Album (normalBottomAction)
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .navigationBarsPadding()
                                        .padding(vertical = 10.dp),
                                    horizontalArrangement = Arrangement.SpaceAround,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    // 1. Share
                                    BottomActionButton(
                                        iconRes = R.drawable.ic_share,
                                        label = stringResource(R.string.share)
                                    ) {
                                        currentViewPath?.let { shareMedia(context, it) }
                                    }

                                    // 2. Private (Vault)
                                    BottomActionButton(
                                        iconRes = R.drawable.ic_hide,
                                        label = stringResource(R.string.private_bottom)
                                    ) {
                                        vaultConfirm = "hide"
                                    }

                                    // 3. Edit (Images) / Add to (Videos)
                                    if (currentItem.mediaType.isImage) {
                                        BottomActionButton(
                                            iconRes = R.drawable.ic_main_edit,
                                            label = stringResource(R.string.edit)
                                        ) {
                                            onNavigate(Screen.Editor.createRoute(currentItem.filePath))
                                        }
                                    } else {
                                        BottomActionButton(
                                            iconRes = R.drawable.ic_add,
                                            label = stringResource(R.string.add_to)
                                        ) {
                                            showAddToDialog = true
                                        }
                                    }

                                    // 4. Delete
                                    BottomActionButton(
                                        iconRes = R.drawable.ic_delete,
                                        label = stringResource(R.string.delete)
                                    ) {
                                        showDeleteDialog = true
                                    }

                                    // 5. More Options
                                    Box {
                                        BottomActionButton(
                                            iconRes = R.drawable.ic_more,
                                            label = stringResource(R.string.more)
                                        ) {
                                            showMoreMenu = true
                                        }

                                        moreMenu()
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // 1. Delete Confirmation Dialog (Matches dialog_delete.xml)
    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = {
                Text(
                    text = stringResource(R.string.delete_dialog_permission),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            },
            text = {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { deleteMoveToBin = !deleteMoveToBin }
                        .padding(vertical = 4.dp)
                ) {
                    Checkbox(
                        checked = deleteMoveToBin,
                        onCheckedChange = { deleteMoveToBin = it },
                        colors = CheckboxDefaults.colors(
                            checkedColor = GalleryBlue,
                            checkmarkColor = Color.White
                        )
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = stringResource(R.string.delete_dialog_move),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        showDeleteDialog = false
                        scope.launch {
                            if (deleteMoveToBin) {
                                mediaRepository.moveToTrash(currentItem.filePath)
                                Toast.makeText(context, "Moved to Recycle Bin", Toast.LENGTH_SHORT).show()
                            } else {
                                mediaRepository.deletePermanently(currentItem.filePath)
                                Toast.makeText(context, "Deleted permanently", Toast.LENGTH_SHORT).show()
                            }
                            onNavigateBack()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = GalleryBlue),
                    shape = RoundedCornerShape(20.dp),
                    modifier = Modifier.height(42.dp).width(110.dp)
                ) {
                    Text(stringResource(R.string.yes), color = Color.White, fontWeight = FontWeight.SemiBold)
                }
            },
            dismissButton = {
                OutlinedButton(
                    onClick = { showDeleteDialog = false },
                    border = BorderStroke(1.dp, GalleryBlue),
                    shape = RoundedCornerShape(20.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = GalleryBlue),
                    modifier = Modifier.height(42.dp).width(110.dp)
                ) {
                    Text(stringResource(R.string.no), color = GalleryBlue, fontWeight = FontWeight.SemiBold)
                }
            },
            containerColor = MaterialTheme.colorScheme.surface,
            shape = RoundedCornerShape(20.dp)
        )
    }

    // 2. Permanent Delete Dialog (for Recycle and Vault modes)
    if (showPermanentDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showPermanentDeleteDialog = false },
            title = {
                Text(
                    text = "Delete Permanently",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            },
            text = {
                Text(
                    text = "Are you sure you want to permanently delete this item? This action cannot be undone.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showPermanentDeleteDialog = false
                        scope.launch {
                            if (isRecycle) {
                                mediaRepository.deletePermanently(currentItem.filePath)
                                Toast.makeText(context, "Deleted permanently", Toast.LENGTH_SHORT).show()
                            } else if (isVault) {
                                vaultRepository.deleteFromVault(currentItem.toVaultItem())
                                Toast.makeText(context, "Deleted permanently from vault", Toast.LENGTH_SHORT).show()
                            }
                            onNavigateBack()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE11A1A)),
                    shape = RoundedCornerShape(20.dp),
                    modifier = Modifier.height(42.dp).width(110.dp)
                ) {
                    Text("Delete", color = Color.White, fontWeight = FontWeight.SemiBold)
                }
            },
            dismissButton = {
                OutlinedButton(
                    onClick = { showPermanentDeleteDialog = false },
                    border = BorderStroke(1.dp, GalleryBlue),
                    shape = RoundedCornerShape(20.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = GalleryBlue),
                    modifier = Modifier.height(42.dp).width(110.dp)
                ) {
                    Text(stringResource(R.string.cancel), color = GalleryBlue, fontWeight = FontWeight.SemiBold)
                }
            },
            containerColor = MaterialTheme.colorScheme.surface,
            shape = RoundedCornerShape(20.dp)
        )
    }

    // 3. Rename Dialog (Matches dialog_rename.xml)
    // Reference confirmation sheets for hiding / unhiding
    vaultConfirm?.let { mode ->
        val hide = mode == "hide"
        ModalBottomSheet(
            onDismissRequest = { vaultConfirm = null },
            containerColor = MaterialTheme.colorScheme.surface,
            dragHandle = null,
            // Reference sheet is inset from the screen edges
            modifier = Modifier.navigationBarsPadding().padding(start = 9.dp, end = 9.dp, bottom = 9.dp),
            shape = RoundedCornerShape(24.dp)
        ) {
            Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 18.dp, vertical = 18.dp)) {
                Text(
                    text = if (hide) "Hide Media" else "Unhide items",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = if (hide) "Are you sure you want to hide media?" else "Are you sure you want to unhide items?",
                    fontSize = 16.sp,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(28.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(24.dp), modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp)) {
                    OutlinedButton(
                        onClick = { vaultConfirm = null },
                        modifier = Modifier.weight(1f).height(40.dp),
                        shape = RoundedCornerShape(20.dp)
                    ) { Text("Cancel", color = MaterialTheme.colorScheme.onSurface, fontSize = 16.sp) }
                    Button(
                        onClick = {
                            vaultConfirm = null
                            scope.launch {
                                val ok = if (hide) vaultRepository.moveToVault(currentItem)
                                else vaultRepository.restoreFromVault(currentItem.toVaultItem())
                                if (ok) {
                                    Toast.makeText(
                                        context,
                                        if (hide) "Media files hidden successfully." else "Media files unhidden successfully.",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                    onNavigateBack()
                                }
                            }
                        },
                        modifier = Modifier.weight(1f).height(40.dp),
                        shape = RoundedCornerShape(20.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = GalleryBlue)
                    ) { Text("Ok", color = Color.White, fontSize = 16.sp) }
                }
                Spacer(modifier = Modifier.navigationBarsPadding().height(8.dp))
            }
        }
    }

    if (showRenameDialog) {
        AlertDialog(
            onDismissRequest = { showRenameDialog = false },
            title = {
                Text(
                    text = stringResource(R.string.rename),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            },
            text = {
                TextField(
                    value = renameInput,
                    onValueChange = { renameInput = it },
                    placeholder = { Text(stringResource(R.string.enter_name), color = MaterialTheme.colorScheme.textTertiary) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = Color.Transparent,
                        unfocusedContainerColor = Color.Transparent,
                        focusedIndicatorColor = GalleryBlue,
                        unfocusedIndicatorColor = MaterialTheme.colorScheme.settingsDivider,
                        focusedTextColor = MaterialTheme.colorScheme.onSurface,
                        unfocusedTextColor = MaterialTheme.colorScheme.onSurface
                    )
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        val trimmed = renameInput.trim()
                        if (trimmed.isNotEmpty()) {
                            scope.launch {
                                val success = if (isVault) vaultRepository.renameInVault(currentItem.toVaultItem(), trimmed)
                                    else mediaRepository.renameMedia(currentItem.filePath, trimmed)
                                if (success) {
                                    Toast.makeText(context, "Renamed successfully", Toast.LENGTH_SHORT).show()
                                } else {
                                    Toast.makeText(context, "Could not rename file", Toast.LENGTH_SHORT).show()
                                }
                            }
                        }
                        showRenameDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = GalleryBlue),
                    shape = RoundedCornerShape(20.dp),
                    modifier = Modifier.height(42.dp).width(110.dp)
                ) {
                    Text(stringResource(R.string.save), color = Color.White, fontWeight = FontWeight.SemiBold)
                }
            },
            dismissButton = {
                OutlinedButton(
                    onClick = { showRenameDialog = false },
                    border = BorderStroke(1.dp, GalleryBlue),
                    shape = RoundedCornerShape(20.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = GalleryBlue),
                    modifier = Modifier.height(42.dp).width(110.dp)
                ) {
                    Text(stringResource(R.string.cancel), color = GalleryBlue, fontWeight = FontWeight.SemiBold)
                }
            },
            containerColor = MaterialTheme.colorScheme.surface,
            shape = RoundedCornerShape(20.dp)
        )
    }

    // 4. Add to Album Bottom Sheet (Matches dialog_select_album.xml)
    if (showAddToDialog) {
        val albums by mediaRepository.getAlbumsFlow().collectAsState(initial = emptyList())
        ModalBottomSheet(
            onDismissRequest = { showAddToDialog = false },
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
            containerColor = MaterialTheme.colorScheme.surface,
            shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp),
            dragHandle = {
                Box(
                    modifier = Modifier
                        .padding(top = 12.dp, bottom = 8.dp)
                        .width(40.dp)
                        .height(4.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(MaterialTheme.colorScheme.cardBorder)
                )
            }
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 8.dp)
            ) {
                Text(
                    text = stringResource(R.string.add_to),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.padding(bottom = 12.dp)
                )

                if (albums.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(140.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "No Albums Found",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                } else {
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(3),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 340.dp)
                    ) {
                        items(albums) { album ->
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(8.dp))
                                    .clickable {
                                        showAddToDialog = false
                                        scope.launch {
                                            val ok = mediaRepository.copyMediaToFolder(currentItem.filePath, album.path)
                                            if (ok) {
                                                Toast.makeText(context, "Added to ${album.name}", Toast.LENGTH_SHORT).show()
                                            }
                                        }
                                    }
                                    .padding(4.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(80.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(MaterialTheme.colorScheme.albumOptionBg),
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (album.coverPath.isNotEmpty()) {
                                        AsyncImage(
                                            model = ImageRequest.Builder(context).data(File(album.coverPath)).build(),
                                            contentDescription = null,
                                            contentScale = ContentScale.Crop,
                                            modifier = Modifier.fillMaxSize()
                                        )
                                    } else {
                                        Icon(
                                            imageVector = Icons.Outlined.Folder,
                                            contentDescription = null,
                                            tint = GalleryBlue,
                                            modifier = Modifier.size(36.dp)
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = album.name,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = FontWeight.Medium,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }

    // 5. Details Bottom Sheet (Matches dialog_property.xml)
    if (showInfoDialog) {
        ModalBottomSheet(
            onDismissRequest = { showInfoDialog = false },
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
            containerColor = MaterialTheme.colorScheme.surface,
            shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp),
            dragHandle = {
                Box(
                    modifier = Modifier
                        .padding(top = 12.dp, bottom = 8.dp)
                        .width(40.dp)
                        .height(4.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(MaterialTheme.colorScheme.cardBorder)
                )
            }
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 8.dp)
                    .navigationBarsPadding()
            ) {
                Text(
                    text = stringResource(R.string.details),
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.padding(bottom = 8.dp)
                )

                HorizontalDivider(color = MaterialTheme.colorScheme.settingsDivider, thickness = 1.dp)

                Spacer(modifier = Modifier.height(6.dp))

                DetailSheetRow(stringResource(R.string.name), currentItem.fileName)
                DetailSheetRow(stringResource(R.string.path), currentItem.filePath)
                DetailSheetRow(stringResource(R.string.size), formatFileSize(currentItem.fileSize))

                val dimensionText = remember(currentItem) {
                    if (currentItem.dimension.isNotEmpty()) {
                        currentItem.dimension
                    } else if (currentItem.mediaType.isImage) {
                        try {
                            val opts = BitmapFactory.Options().apply { inJustDecodeBounds = true }
                            BitmapFactory.decodeFile(currentItem.filePath, opts)
                            if (opts.outWidth > 0 && opts.outHeight > 0) "${opts.outWidth}x${opts.outHeight}px" else ""
                        } catch (_: Exception) { "" }
                    } else ""
                }
                if (dimensionText.isNotEmpty()) {
                    DetailSheetRow(stringResource(R.string.resolution), dimensionText)
                }

                if (currentItem.mediaType == MediaType.VIDEO && (videoDurationMs > 0 || currentItem.durationMs > 0)) {
                    val duration = if (videoDurationMs > 0) videoDurationMs else currentItem.durationMs
                    DetailSheetRow("Duration", "${duration / 1000} seconds")
                }

                val dateStr = remember(currentItem.dateAdded, currentItem.dateModified) {
                    val epoch = if (currentItem.dateAdded > 0) currentItem.dateAdded else currentItem.dateModified
                    val millis = if (epoch > 10_000_000_000L) epoch else epoch * 1000L
                    SimpleDateFormat("dd/MM/yyyy hh:mm a", Locale.getDefault()).format(Date(millis))
                }
                DetailSheetRow(stringResource(R.string.taken_on), dateStr)

                Spacer(modifier = Modifier.height(16.dp))

                Button(
                    onClick = { showInfoDialog = false },
                    modifier = Modifier
                        .align(Alignment.End)
                        .padding(bottom = 12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = GalleryBlue),
                    shape = RoundedCornerShape(20.dp),
                    contentPadding = PaddingValues(horizontal = 40.dp, vertical = 10.dp)
                ) {
                    Text(stringResource(R.string.ok), color = Color.White, fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }
}

@Composable
private fun SuggestionPill(
    iconRes: Int,
    label: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(16.dp))
            .background(Color(0xFF242730))
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 7.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Icon(
            painter = painterResource(id = iconRes),
            contentDescription = label,
            tint = Color.White,
            modifier = Modifier.size(15.dp)
        )
        Text(
            text = label,
            color = Color.White,
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium
        )
    }
}

@Composable
private fun BottomActionButton(
    iconRes: Int,
    label: String,
    onClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick
            )
            .padding(horizontal = 8.dp, vertical = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            painter = painterResource(id = iconRes),
            contentDescription = label,
            tint = Color.Unspecified,
            modifier = Modifier.size(22.dp)
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = label,
            color = MaterialTheme.colorScheme.onSurface,
            fontSize = 11.sp,
            fontWeight = FontWeight.Medium,
            maxLines = 1
        )
    }
}

@OptIn(UnstableApi::class, ExperimentalFoundationApi::class)
@Composable
private fun PagerMediaItemView(
    item: MediaItem,
    displayPath: String?,
    rotationAngle: Float,
    exoPlayer: ExoPlayer?,
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
        val newScale = (scale * zoomChange).coerceIn(1f, ViewerZoom.MAX_SCALE)
        scale = newScale
        if (newScale <= 1.05f) {
            scale = 1f
            offset = Offset.Zero
        } else {
            offset = clampOffset(offset + offsetChange, newScale)
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
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .clip(CircleShape)
                        .background(Color.Black.copy(alpha = 0.5f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_short_video_play),
                        contentDescription = "Play",
                        tint = Color.White,
                        modifier = Modifier.size(28.dp)
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
                .pointerInput(item.filePath) {
                    detectTapGestures(
                        onTap = { onToggleControls() },
                        onDoubleTap = { tap ->
                            if (scale > 1.05f) {
                                scale = 1f
                                offset = Offset.Zero
                            } else {
                                scale = ViewerZoom.DOUBLE_TAP_SCALE
                                offset = ViewerZoom.offsetForTap(tap, scale, fitted, containerF)
                            }
                        }
                    )
                },
            contentAlignment = Alignment.Center
        ) {
            AsyncImage(
                model = ImageRequest.Builder(context)
                    .data(displayPath?.let { File(it) })
                    .crossfade(false)
                    .build(),
                contentDescription = item.fileName,
                contentScale = ContentScale.Fit,
                onSuccess = { imageSize = it.painter.intrinsicSize },
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer(
                        rotationZ = rotationAngle,
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

@Composable
private fun DetailSheetRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 10.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Top
    ) {
        Text(
            text = label,
            fontSize = 14.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(1f)
        )
        Text(
            text = value,
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(2f),
            maxLines = 3,
            overflow = TextOverflow.Ellipsis
        )
    }
}

private fun formatFileSize(bytes: Long): String {
    if (bytes <= 0) return "0 B"
    val kb = bytes / 1024.0
    val mb = kb / 1024.0
    val gb = mb / 1024.0
    return when {
        gb >= 1.0 -> String.format(Locale.US, "%.2f GB", gb)
        mb >= 1.0 -> String.format(Locale.US, "%.2f MB", mb)
        kb >= 1.0 -> String.format(Locale.US, "%.2f KB", kb)
        else -> "$bytes B"
    }
}

private fun shareMedia(context: Context, path: String) {
    try {
        val file = File(path)
        if (!file.exists()) return
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.provider", file)
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "*/*"
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(intent, "Share Media"))
    } catch (_: Exception) {}
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

private fun captureVideoFrame(filePath: String, positionMs: Long): Bitmap? {
    return try {
        val retriever = MediaMetadataRetriever()
        retriever.setDataSource(filePath)
        val frame = retriever.getFrameAtTime(
            positionMs * 1000L,
            MediaMetadataRetriever.OPTION_CLOSEST
        )
        retriever.release()
        frame
    } catch (_: Exception) {
        null
    }
}

private fun saveCapturedFrameToGallery(context: Context, bitmap: Bitmap, originalFileName: String): String? {
    val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
    val baseName = originalFileName.substringBeforeLast(".")
    val newFileName = "FRAME_${baseName}_$timeStamp.jpg"

    return try {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val contentValues = ContentValues().apply {
                put(MediaStore.MediaColumns.DISPLAY_NAME, newFileName)
                put(MediaStore.MediaColumns.MIME_TYPE, "image/jpeg")
                put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_PICTURES + "/Gallery")
            }
            val uri = context.contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)
            if (uri != null) {
                context.contentResolver.openOutputStream(uri)?.use { stream ->
                    bitmap.compress(Bitmap.CompressFormat.JPEG, 95, stream)
                }
                uri.toString()
            } else null
        } else {
            val picturesDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES)
            val galleryDir = File(picturesDir, "Gallery").also { if (!it.exists()) it.mkdirs() }
            val file = File(galleryDir, newFileName)
            file.outputStream().use { stream ->
                bitmap.compress(Bitmap.CompressFormat.JPEG, 95, stream)
            }
            file.absolutePath
        }
    } catch (_: Exception) {
        null
    }
}

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
