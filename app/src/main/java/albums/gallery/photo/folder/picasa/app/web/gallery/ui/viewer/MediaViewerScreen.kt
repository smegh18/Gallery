package albums.gallery.photo.folder.picasa.app.web.gallery.ui.viewer

import android.app.WallpaperManager
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.widget.Toast
import androidx.annotation.OptIn
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.rememberTransformableState
import androidx.compose.foundation.gestures.transformable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.MoreVert
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
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
import albums.gallery.photo.folder.picasa.app.web.gallery.R
import albums.gallery.photo.folder.picasa.app.web.gallery.data.ads.AdConstants
import albums.gallery.photo.folder.picasa.app.web.gallery.data.repository.MediaRepository
import albums.gallery.photo.folder.picasa.app.web.gallery.data.repository.VaultRepository
import albums.gallery.photo.folder.picasa.app.web.gallery.domain.model.MediaItem
import albums.gallery.photo.folder.picasa.app.web.gallery.domain.model.MediaType
import albums.gallery.photo.folder.picasa.app.web.gallery.ui.components.BannerAdView
import albums.gallery.photo.folder.picasa.app.web.gallery.ui.navigation.Screen
import albums.gallery.photo.folder.picasa.app.web.gallery.ui.theme.GalleryBlue
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

@OptIn(UnstableApi::class)
@ExperimentalMaterial3Api
@Composable
fun MediaViewerScreen(
    filePath: String,
    mediaRepository: MediaRepository,
    vaultRepository: VaultRepository,
    onNavigate: (String) -> Unit,
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var showControls by remember { mutableStateOf(true) }
    var showInfoDialog by remember { mutableStateOf(false) }
    var showWallpaperDialog by remember { mutableStateOf(false) }
    var showMoreMenu by remember { mutableStateOf(false) }
    var rotationAngle by remember { mutableFloatStateOf(0f) }

    // Read media item from repository flow or fallback
    val allMedia by mediaRepository.getActiveMediaFlow().collectAsState(initial = emptyList())
    val currentItem = allMedia.find { it.filePath == filePath } ?: remember(filePath) {
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

    // Zoom and pan state
    var scale by remember { mutableFloatStateOf(1f) }
    var offset by remember { mutableStateOf(Offset.Zero) }
    val transformState = rememberTransformableState { zoomChange, offsetChange, _ ->
        scale = (scale * zoomChange).coerceIn(1f, 5f)
        if (scale == 1f) {
            offset = Offset.Zero
        } else {
            offset += offsetChange
        }
    }

    // Video player setup
    var exoPlayer by remember { mutableStateOf<ExoPlayer?>(null) }

    DisposableEffect(currentItem) {
        if (currentItem.mediaType == MediaType.VIDEO) {
            val player = ExoPlayer.Builder(context).build().apply {
                val mediaItem = ExoMediaItem.fromUri(Uri.fromFile(File(currentItem.filePath)))
                setMediaItem(mediaItem)
                prepare()
                playWhenReady = true
            }
            exoPlayer = player
        }
        onDispose {
            exoPlayer?.release()
            exoPlayer = null
        }
    }

    // Formatted date and time for reference-matching toolbar
    val epochTime = remember(currentItem.dateAdded, currentItem.dateModified) {
        val epoch = if (currentItem.dateAdded > 0) currentItem.dateAdded else currentItem.dateModified
        Date(epoch * 1000L)
    }
    val dateText = remember(epochTime) {
        SimpleDateFormat("MMM dd, yyyy", Locale.getDefault()).format(epochTime)
    }
    val timeText = remember(epochTime) {
        SimpleDateFormat("hh:mm a", Locale.getDefault()).format(epochTime)
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .pointerInput(Unit) {
                detectTapGestures(
                    onTap = { showControls = !showControls },
                    onDoubleTap = {
                        scale = if (scale > 1f) 1f else 2.5f
                        offset = Offset.Zero
                    }
                )
            }
    ) {
        // Media Content
        if (currentItem.mediaType == MediaType.VIDEO) {
            exoPlayer?.let { player ->
                AndroidView(
                    factory = { ctx ->
                        PlayerView(ctx).apply {
                            this.player = player
                            useController = true
                        }
                    },
                    modifier = Modifier.fillMaxSize()
                )
            }
        } else {
            AsyncImage(
                model = ImageRequest.Builder(context)
                    .data(File(currentItem.filePath))
                    .crossfade(true)
                    .build(),
                contentDescription = currentItem.fileName,
                contentScale = ContentScale.Fit,
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer(
                        rotationZ = rotationAngle,
                        scaleX = scale,
                        scaleY = scale,
                        translationX = offset.x,
                        translationY = offset.y
                    )
                    .transformable(state = transformState)
            )
        }

        // Top Control Overlay (Reference activity_media_full_screen.xml toolbarLay)
        AnimatedVisibility(
            visible = showControls,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier.align(Alignment.TopCenter)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.Black.copy(alpha = 0.65f))
                    .statusBarsPadding()
                    .padding(horizontal = 8.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onNavigateBack) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = Color.White
                    )
                }

                Column(
                    modifier = Modifier
                        .weight(1f)
                        .padding(horizontal = 6.dp)
                ) {
                    Text(
                        text = dateText,
                        color = Color.White,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = timeText,
                        color = Color(0xFFB0B0B0),
                        style = MaterialTheme.typography.bodySmall,
                        maxLines = 1
                    )
                }

                // Rotate Button
                IconButton(onClick = { rotationAngle = (rotationAngle + 90f) % 360f }) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_media_rotate),
                        contentDescription = "Rotate",
                        tint = Color.White
                    )
                }

                // Favorite Button
                IconButton(onClick = {
                    scope.launch {
                        mediaRepository.setFavorite(currentItem.filePath, !currentItem.isFavorite)
                    }
                }) {
                    Icon(
                        imageVector = if (currentItem.isFavorite) Icons.Default.Favorite else Icons.Outlined.FavoriteBorder,
                        contentDescription = "Favorite",
                        tint = if (currentItem.isFavorite) Color(0xFFFF4D4F) else Color.White
                    )
                }

                // Details Button
                IconButton(onClick = { showInfoDialog = true }) {
                    Icon(
                        imageVector = Icons.Outlined.Info,
                        contentDescription = "Details",
                        tint = Color.White
                    )
                }
            }
        }

        // Bottom Control Overlay (Reference item_bottom_option_layout.xml + global_ad_framelayout)
        AnimatedVisibility(
            visible = showControls,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier.align(Alignment.BottomCenter)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.Black.copy(alpha = 0.75f))
                    .navigationBarsPadding()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceAround,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // 1. Share
                    IconButton(onClick = { shareMedia(context, currentItem.filePath) }) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(Icons.Default.Share, contentDescription = "Share", tint = Color.White)
                            Spacer(modifier = Modifier.height(2.dp))
                            Text("Share", color = Color.White, fontSize = 11.sp)
                        }
                    }

                    // 2. Private / Vault
                    IconButton(onClick = {
                        scope.launch {
                            vaultRepository.moveToVault(currentItem)
                            Toast.makeText(context, "Moved to Private Vault", Toast.LENGTH_SHORT).show()
                            onNavigateBack()
                        }
                    }) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(Icons.Default.Lock, contentDescription = "Private", tint = Color.White)
                            Spacer(modifier = Modifier.height(2.dp))
                            Text("Private", color = Color.White, fontSize = 11.sp)
                        }
                    }

                    // 3. Edit (Images only)
                    if (currentItem.mediaType == MediaType.IMAGE) {
                        IconButton(onClick = {
                            onNavigate(Screen.Editor.createRoute(currentItem.filePath))
                        }) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(Icons.Default.Edit, contentDescription = "Edit", tint = Color.White)
                                Spacer(modifier = Modifier.height(2.dp))
                                Text("Edit", color = Color.White, fontSize = 11.sp)
                            }
                        }
                    }

                    // 4. Delete
                    IconButton(onClick = {
                        scope.launch {
                            mediaRepository.moveToTrash(currentItem.filePath)
                            Toast.makeText(context, "Moved to Recycle Bin", Toast.LENGTH_SHORT).show()
                            onNavigateBack()
                        }
                    }) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(Icons.Default.Delete, contentDescription = "Delete", tint = Color(0xFFFF4D4F))
                            Spacer(modifier = Modifier.height(2.dp))
                            Text("Delete", color = Color(0xFFFF4D4F), fontSize = 11.sp)
                        }
                    }

                    // 5. More Options (Wallpaper, Details, Rotate)
                    Box {
                        IconButton(onClick = { showMoreMenu = true }) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(
                                    painter = painterResource(id = R.drawable.ic_more),
                                    contentDescription = "More",
                                    tint = Color.White
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text("More", color = Color.White, fontSize = 11.sp)
                            }
                        }

                        DropdownMenu(
                            expanded = showMoreMenu,
                            onDismissRequest = { showMoreMenu = false }
                        ) {
                            if (currentItem.mediaType == MediaType.IMAGE) {
                                DropdownMenuItem(
                                    text = { Text("Set as wallpaper") },
                                    onClick = {
                                        showMoreMenu = false
                                        showWallpaperDialog = true
                                    },
                                    leadingIcon = {
                                        Icon(Icons.Default.Wallpaper, contentDescription = null)
                                    }
                                )
                            }
                            DropdownMenuItem(
                                text = { Text("Rotate clockwise") },
                                onClick = {
                                    showMoreMenu = false
                                    rotationAngle = (rotationAngle + 90f) % 360f
                                },
                                leadingIcon = {
                                    Icon(painterResource(id = R.drawable.ic_media_rotate), contentDescription = null)
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Details") },
                                onClick = {
                                    showMoreMenu = false
                                    showInfoDialog = true
                                },
                                leadingIcon = {
                                    Icon(Icons.Outlined.Info, contentDescription = null)
                                }
                            )
                        }
                    }
                }

                // Reference Banner Ad: rl_banner_ad with global_ad_framelayout
                BannerAdView(adUnitId = AdConstants.BANNER_FULLSCREEN_MEDIA)
            }
        }
    }

    // Set as Wallpaper Dialog (Reference dialog_set_wallpaper.xml)
    if (showWallpaperDialog) {
        AlertDialog(
            onDismissRequest = { showWallpaperDialog = false },
            title = {
                Text(
                    text = "Set as wallpaper",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    TextButton(
                        onClick = {
                            showWallpaperDialog = false
                            scope.launch(Dispatchers.IO) {
                                applyWallpaper(context, currentItem.filePath, WallpaperManager.FLAG_LOCK)
                            }
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "Lock screen",
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    TextButton(
                        onClick = {
                            showWallpaperDialog = false
                            scope.launch(Dispatchers.IO) {
                                applyWallpaper(context, currentItem.filePath, WallpaperManager.FLAG_SYSTEM)
                            }
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "Home screen",
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    TextButton(
                        onClick = {
                            showWallpaperDialog = false
                            scope.launch(Dispatchers.IO) {
                                applyWallpaper(
                                    context,
                                    currentItem.filePath,
                                    WallpaperManager.FLAG_SYSTEM or WallpaperManager.FLAG_LOCK
                                )
                            }
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "Home and lock screens",
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { showWallpaperDialog = false }) {
                    Text("Cancel", color = GalleryBlue)
                }
            },
            shape = RoundedCornerShape(14.dp)
        )
    }

    // Media Details Bottom Sheet (Reference dialog_property.xml)
    if (showInfoDialog) {
        ModalBottomSheet(
            onDismissRequest = { showInfoDialog = false },
            containerColor = MaterialTheme.colorScheme.surface,
            dragHandle = {
                Box(
                    modifier = Modifier
                        .padding(top = 10.dp, bottom = 6.dp)
                        .size(width = 40.dp, height = 4.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(Color(0xFFDADADA))
                )
            }
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp)
                    .padding(bottom = 32.dp)
            ) {
                Text(
                    text = "Details",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.padding(bottom = 16.dp)
                )

                DetailSheetRow("Name", currentItem.fileName)
                DetailSheetRow("Path", currentItem.filePath)
                DetailSheetRow("Size", formatFileSize(currentItem.fileSize))

                val dimensionText = remember(currentItem.filePath) {
                    if (currentItem.dimension.isNotEmpty()) {
                        currentItem.dimension
                    } else if (currentItem.mediaType == MediaType.IMAGE) {
                        try {
                            val opts = BitmapFactory.Options().apply { inJustDecodeBounds = true }
                            BitmapFactory.decodeFile(currentItem.filePath, opts)
                            if (opts.outWidth > 0 && opts.outHeight > 0) "${opts.outWidth} x ${opts.outHeight}" else ""
                        } catch (_: Exception) { "" }
                    } else ""
                }
                if (dimensionText.isNotEmpty()) {
                    DetailSheetRow("Resolution", dimensionText)
                }

                if (currentItem.mediaType == MediaType.VIDEO && currentItem.durationMs > 0) {
                    DetailSheetRow("Duration", "${currentItem.durationMs / 1000} seconds")
                }

                val dateStr = remember(currentItem.dateAdded, currentItem.dateModified) {
                    val epoch = if (currentItem.dateAdded > 0) currentItem.dateAdded else currentItem.dateModified
                    SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date(epoch * 1000L))
                }
                DetailSheetRow("Taken on", dateStr)

                Spacer(modifier = Modifier.height(20.dp))

                Button(
                    onClick = { showInfoDialog = false },
                    modifier = Modifier.align(Alignment.End),
                    colors = ButtonDefaults.buttonColors(containerColor = GalleryBlue),
                    shape = RoundedCornerShape(20.dp),
                    contentPadding = PaddingValues(horizontal = 32.dp, vertical = 10.dp)
                ) {
                    Text("OK", color = Color.White, fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }
}

@Composable
private fun DetailSheetRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Top
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = Color(0xFF5C7A90),
            modifier = Modifier.weight(1f)
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium,
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

private fun applyWallpaper(context: Context, filePath: String, flag: Int) {
    try {
        val wallpaperManager = WallpaperManager.getInstance(context)
        val bitmap = BitmapFactory.decodeFile(filePath)
        if (bitmap == null) {
            Toast.makeText(context, "Unable to load image", Toast.LENGTH_SHORT).show()
            return
        }
        val desiredHeight = wallpaperManager.desiredMinimumHeight.takeIf { it > 0 } ?: bitmap.height
        val scaleFactor = desiredHeight.toFloat() / bitmap.height.toFloat()
        val scaledWidth = (bitmap.width * scaleFactor).toInt().coerceAtLeast(1)
        val scaledBitmap = Bitmap.createScaledBitmap(bitmap, scaledWidth, desiredHeight, true)

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N) {
            wallpaperManager.setBitmap(scaledBitmap, null, true, flag)
        } else {
            wallpaperManager.setBitmap(scaledBitmap)
        }
        Toast.makeText(context, "Wallpaper applied successfully", Toast.LENGTH_SHORT).show()
    } catch (e: Exception) {
        Toast.makeText(context, "Could not set wallpaper: ${e.message}", Toast.LENGTH_SHORT).show()
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
