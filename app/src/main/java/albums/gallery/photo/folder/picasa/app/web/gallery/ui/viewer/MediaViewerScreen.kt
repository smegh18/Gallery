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
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.rememberTransformableState
import androidx.compose.foundation.gestures.transformable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.Folder
import androidx.compose.material.icons.outlined.Info
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
import androidx.compose.ui.text.style.TextAlign
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
import albums.gallery.photo.folder.picasa.app.web.gallery.domain.model.Album
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
    folderPath: String = "",
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
    var showDeleteDialog by remember { mutableStateOf(false) }
    var deleteMoveToBin by remember { mutableStateOf(true) }
    var showRenameDialog by remember { mutableStateOf(false) }
    var renameInput by remember { mutableStateOf("") }
    var showAddToDialog by remember { mutableStateOf(false) }
    var showMoreMenu by remember { mutableStateOf(false) }
    var rotationAngle by remember { mutableFloatStateOf(0f) }

    // Collect media items for this album/folder or all media
    val mediaList by remember(folderPath) {
        if (folderPath.isNotEmpty()) {
            mediaRepository.getMediaByFolderFlow(folderPath)
        } else {
            mediaRepository.getActiveMediaFlow()
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

    val displayList = remember(mediaList, fallbackItem) {
        if (mediaList.isNotEmpty()) mediaList else listOf(fallbackItem)
    }

    val initialIndex = remember(displayList, filePath) {
        val idx = displayList.indexOfFirst { it.filePath == filePath }
        if (idx >= 0) idx else 0
    }

    val pagerState = rememberPagerState(initialPage = initialIndex) {
        displayList.size
    }

    val currentItem = displayList.getOrElse(pagerState.currentPage.coerceIn(0, displayList.lastIndex)) { fallbackItem }

    // Reset rotation when swiping to another item
    LaunchedEffect(pagerState.currentPage) {
        rotationAngle = 0f
    }

    // Formatted date and time for reference-matching toolbar
    val epochTime = remember(currentItem.dateAdded, currentItem.dateModified) {
        val epoch = if (currentItem.dateAdded > 0) currentItem.dateAdded else currentItem.dateModified
        val millis = if (epoch > 10_000_000_000L) epoch else epoch * 1000L
        Date(millis)
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
                    onTap = { showControls = !showControls }
                )
            }
    ) {
        // Horizontal Pager for swiping between media
        HorizontalPager(
            state = pagerState,
            modifier = Modifier.fillMaxSize()
        ) { page ->
            val item = displayList.getOrNull(page) ?: fallbackItem
            PagerMediaItemView(
                item = item,
                rotationAngle = if (page == pagerState.currentPage) rotationAngle else 0f,
                onToggleControls = { showControls = !showControls }
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
                        painter = painterResource(id = R.drawable.ic_property),
                        contentDescription = "Details",
                        tint = Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }

        // Bottom Control Overlay (Reference item_bottom_option_layout.xml)
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
                        .padding(horizontal = 8.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.SpaceAround,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // 1. Share
                    IconButton(onClick = { shareMedia(context, currentItem.filePath) }) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                painter = painterResource(id = R.drawable.ic_share),
                                contentDescription = "Share",
                                tint = Color.White,
                                modifier = Modifier.size(20.dp)
                            )
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
                            Icon(
                                painter = painterResource(id = R.drawable.ic_hide),
                                contentDescription = "Private",
                                tint = Color.White,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text("Private", color = Color.White, fontSize = 11.sp)
                        }
                    }

                    // 3. Edit (Images) / Add to (Videos) - 5 standard buttons matching reference normalBottomAction
                    if (currentItem.mediaType == MediaType.IMAGE) {
                        IconButton(onClick = {
                            onNavigate(Screen.Editor.createRoute(currentItem.filePath))
                        }) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(
                                    painter = painterResource(id = R.drawable.ic_main_edit),
                                    contentDescription = "Edit",
                                    tint = Color.White,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text("Edit", color = Color.White, fontSize = 11.sp)
                            }
                        }
                    } else {
                        IconButton(onClick = { showAddToDialog = true }) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(
                                    painter = painterResource(id = R.drawable.ic_add),
                                    contentDescription = "Add to",
                                    tint = Color.White,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text("Add to", color = Color.White, fontSize = 11.sp)
                            }
                        }
                    }

                    // 4. Delete (Opens reference dialog_delete.xml prompt)
                    IconButton(onClick = { showDeleteDialog = true }) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                painter = painterResource(id = R.drawable.ic_delete),
                                contentDescription = "Delete",
                                tint = Color(0xFFFF4D4F),
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text("Delete", color = Color(0xFFFF4D4F), fontSize = 11.sp)
                        }
                    }

                    // 5. More Options (Add to, Wallpaper, Rename, Details)
                    Box {
                        IconButton(onClick = { showMoreMenu = true }) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(
                                    painter = painterResource(id = R.drawable.ic_more),
                                    contentDescription = "More",
                                    tint = Color.White,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text("More", color = Color.White, fontSize = 11.sp)
                            }
                        }

                        DropdownMenu(
                            expanded = showMoreMenu,
                            onDismissRequest = { showMoreMenu = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("Add to") },
                                onClick = {
                                    showMoreMenu = false
                                    showAddToDialog = true
                                },
                                leadingIcon = {
                                    Icon(painterResource(id = R.drawable.ic_add), contentDescription = null, modifier = Modifier.size(18.dp))
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Rename") },
                                onClick = {
                                    showMoreMenu = false
                                    renameInput = File(currentItem.filePath).nameWithoutExtension
                                    showRenameDialog = true
                                },
                                leadingIcon = {
                                    Icon(painterResource(id = R.drawable.ic_rename), contentDescription = null, modifier = Modifier.size(18.dp))
                                }
                            )
                            if (currentItem.mediaType == MediaType.IMAGE) {
                                DropdownMenuItem(
                                    text = { Text("Set as wallpaper") },
                                    onClick = {
                                        showMoreMenu = false
                                        showWallpaperDialog = true
                                    },
                                    leadingIcon = {
                                        Icon(painterResource(id = R.drawable.ic_set_as), contentDescription = null, modifier = Modifier.size(18.dp))
                                    }
                                )
                            }
                            DropdownMenuItem(
                                text = { Text("Details") },
                                onClick = {
                                    showMoreMenu = false
                                    showInfoDialog = true
                                },
                                leadingIcon = {
                                    Icon(painterResource(id = R.drawable.ic_property), contentDescription = null, modifier = Modifier.size(18.dp))
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

    // 1. Delete Confirmation Dialog (Reference dialog_delete.xml)
    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = {
                Text(
                    text = "Do you want to delete this media?",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFF1E3354)
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
                        colors = CheckboxDefaults.colors(checkedColor = GalleryBlue)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Move to Recycle Bin",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color(0xFF1E3354)
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
                    shape = RoundedCornerShape(20.dp)
                ) {
                    Text("Yes", color = Color.White, fontWeight = FontWeight.SemiBold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) {
                    Text("No", color = Color(0xFF707C93), fontWeight = FontWeight.Medium)
                }
            },
            containerColor = Color.White,
            shape = RoundedCornerShape(16.dp)
        )
    }

    // 2. Rename Dialog (Reference dialog_rename.xml)
    if (showRenameDialog) {
        AlertDialog(
            onDismissRequest = { showRenameDialog = false },
            title = {
                Text(
                    text = "Rename",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF1E3354)
                )
            },
            text = {
                OutlinedTextField(
                    value = renameInput,
                    onValueChange = { renameInput = it },
                    placeholder = { Text("Enter name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp)
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        val trimmed = renameInput.trim()
                        if (trimmed.isNotEmpty()) {
                            scope.launch {
                                val success = mediaRepository.renameMedia(currentItem.filePath, trimmed)
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
                    shape = RoundedCornerShape(20.dp)
                ) {
                    Text("Save", color = Color.White, fontWeight = FontWeight.SemiBold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showRenameDialog = false }) {
                    Text("Cancel", color = Color(0xFF707C93), fontWeight = FontWeight.Medium)
                }
            },
            containerColor = Color.White,
            shape = RoundedCornerShape(16.dp)
        )
    }

    // 3. Add to Album Bottom Sheet (Reference dialog_select_album.xml)
    if (showAddToDialog) {
        val albums by mediaRepository.getAlbumsFlow().collectAsState(initial = emptyList())
        ModalBottomSheet(
            onDismissRequest = { showAddToDialog = false },
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
            containerColor = Color.White,
            shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 12.dp)
            ) {
                Text(
                    text = "Add to Album",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF1E3354),
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
                            color = Color(0xFF707C93),
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
                                        .background(Color(0xFFF3F7FB)),
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
                                    color = Color(0xFF1E3354)
                                )
                            }
                        }
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }

    // 4. Set as Wallpaper Dialog (Reference dialog_set_wallpaper.xml)
    if (showWallpaperDialog) {
        AlertDialog(
            onDismissRequest = { showWallpaperDialog = false },
            title = {
                Text(
                    text = "Set as wallpaper",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF1E3354)
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
                            color = Color(0xFF1E3354)
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
                            color = Color(0xFF1E3354)
                        )
                    }

                    TextButton(
                        onClick = {
                            showWallpaperDialog = false
                            scope.launch(Dispatchers.IO) {
                                applyWallpaper(context, currentItem.filePath, WallpaperManager.FLAG_LOCK or WallpaperManager.FLAG_SYSTEM)
                            }
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "Both screen",
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.Medium,
                            color = Color(0xFF1E3354)
                        )
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { showWallpaperDialog = false }) {
                    Text("Cancel", color = Color(0xFF707C93), fontWeight = FontWeight.Medium)
                }
            },
            containerColor = Color.White,
            shape = RoundedCornerShape(16.dp)
        )
    }

    // 5. Details Bottom Sheet (Reference dialog_property.xml)
    if (showInfoDialog) {
        ModalBottomSheet(
            onDismissRequest = { showInfoDialog = false },
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
            containerColor = Color.White,
            shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 12.dp)
            ) {
                Text(
                    text = "Details",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF1E3354),
                    modifier = Modifier.padding(bottom = 12.dp)
                )

                HorizontalDivider(color = Color(0xFFE4E6E9), thickness = 1.dp)

                Spacer(modifier = Modifier.height(10.dp))

                DetailSheetRow("Name", currentItem.fileName)
                DetailSheetRow("Path", currentItem.filePath)
                DetailSheetRow("File Size", formatFileSize(currentItem.fileSize))

                val dimensionText = remember(currentItem) {
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
                    val millis = if (epoch > 10_000_000_000L) epoch else epoch * 1000L
                    SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date(millis))
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

@OptIn(UnstableApi::class)
@Composable
private fun PagerMediaItemView(
    item: MediaItem,
    rotationAngle: Float,
    onToggleControls: () -> Unit
) {
    val context = LocalContext.current
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

    if (item.mediaType == MediaType.VIDEO) {
        var exoPlayer by remember(item.filePath) { mutableStateOf<ExoPlayer?>(null) }
        DisposableEffect(item.filePath) {
            val player = ExoPlayer.Builder(context).build().apply {
                val mediaItem = ExoMediaItem.fromUri(Uri.fromFile(File(item.filePath)))
                setMediaItem(mediaItem)
                prepare()
                playWhenReady = true
            }
            exoPlayer = player
            onDispose {
                player.release()
            }
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(Unit) {
                    detectTapGestures(onTap = { onToggleControls() })
                },
            contentAlignment = Alignment.Center
        ) {
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
        }
    } else {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(Unit) {
                    detectTapGestures(
                        onTap = { onToggleControls() },
                        onDoubleTap = {
                            scale = if (scale > 1f) 1f else 2.5f
                            offset = Offset.Zero
                        }
                    )
                },
            contentAlignment = Alignment.Center
        ) {
            AsyncImage(
                model = ImageRequest.Builder(context)
                    .data(File(item.filePath))
                    .crossfade(true)
                    .build(),
                contentDescription = item.fileName,
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
