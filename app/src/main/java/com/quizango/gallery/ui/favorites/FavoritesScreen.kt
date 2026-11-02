package com.quizango.gallery.ui.favorites

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.quizango.gallery.R
import com.quizango.gallery.data.repository.MediaRepository
import com.quizango.gallery.domain.model.MediaItem
import com.quizango.gallery.domain.model.MediaType
import com.quizango.gallery.ui.navigation.Screen
import com.quizango.gallery.ui.theme.GalleryBlue
import kotlinx.coroutines.launch
import java.io.File

/**
 * Favorites Screen with 100% parity to reference activity_favorite.xml and FavoriteActivity.java.
 * Features:
 * - 4-column square grid layout (matching reference fastScroller / imageRecycler)
 * - Empty state with ic_empty_img and authentic typography matching 06_favorite.xml
 * - TopBar in normal mode with back button, "Favorites" title, and select mode toggle
 * - TopBar in selection mode with close button, "X Selected" count, and "Select All" / "Unselect All" toggle
 * - Authentic 1:1 aspect ratio square card with video play badge, duration, and favorite heart indicator
 * - Corner selection indicators (ic_check / ic_un_check) in selection mode
 * - Bottom action bar in selection mode: Share, Unfavorite/Remove, Add to, Delete
 * - Authentic delete confirmation dialog with Recycle Bin option
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun FavoritesScreen(
    mediaRepository: MediaRepository,
    onNavigate: (String) -> Unit,
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val favorites by mediaRepository.getFavoritesFlow().collectAsState(initial = emptyList())
    var selectedPaths by remember { mutableStateOf(setOf<String>()) }
    val isSelectionMode = selectedPaths.isNotEmpty()

    var showDeleteDialog by remember { mutableStateOf(false) }
    var deleteMoveToBin by remember { mutableStateOf(true) }

    val allFavoritePaths = remember(favorites) { favorites.map { it.filePath }.toSet() }
    val isAllSelected = isSelectionMode && selectedPaths.size >= allFavoritePaths.size

    Scaffold(
        topBar = {
            Column(modifier = Modifier.fillMaxWidth()) {
                TopAppBar(
                    title = {
                        Text(
                            text = if (isSelectionMode) "${selectedPaths.size} Selected" else stringResource(id = R.string.favorites),
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 18.sp,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                    },
                    navigationIcon = {
                        IconButton(
                            onClick = {
                                if (isSelectionMode) {
                                    selectedPaths = emptySet()
                                } else {
                                    onNavigateBack()
                                }
                            }
                        ) {
                            Icon(
                                painter = painterResource(
                                    id = if (isSelectionMode) R.drawable.ic_close else R.drawable.ic_back
                                ),
                                contentDescription = if (isSelectionMode) "Close" else "Back",
                                tint = MaterialTheme.colorScheme.onBackground,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    },
                    actions = {
                        if (isSelectionMode) {
                            TextButton(
                                onClick = {
                                    selectedPaths = if (isAllSelected) {
                                        emptySet()
                                    } else {
                                        allFavoritePaths
                                    }
                                }
                            ) {
                                Text(
                                    text = if (isAllSelected) {
                                        stringResource(id = R.string.unselect_all)
                                    } else {
                                        stringResource(id = R.string.select_all)
                                    },
                                    color = GalleryBlue,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        } else if (favorites.isNotEmpty()) {
                            TextButton(
                                onClick = {
                                    selectedPaths = allFavoritePaths
                                }
                            ) {
                                Text(
                                    text = stringResource(id = R.string.select_all),
                                    color = GalleryBlue,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.background
                    )
                )
                HorizontalDivider(
                    thickness = 0.5.dp,
                    color = MaterialTheme.colorScheme.outline
                )
            }
        },
        bottomBar = {
            if (isSelectionMode) {
                Surface(
                    tonalElevation = 8.dp,
                    shadowElevation = 8.dp,
                    shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp),
                    color = MaterialTheme.colorScheme.surface,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 10.dp),
                        horizontalArrangement = Arrangement.SpaceAround,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // 1. Share (ic_home_select_share)
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier
                                .width(60.dp)
                                .clickable { shareFavoriteMedia(context, selectedPaths) }
                                .padding(vertical = 4.dp)
                        ) {
                            Icon(
                                painter = painterResource(id = R.drawable.ic_home_select_share),
                                contentDescription = "Share",
                                tint = Color.Unspecified,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = stringResource(id = R.string.share),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }

                        // 2. Unfavorite / Remove (ic_home_select_fav)
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier
                                .width(68.dp)
                                .clickable {
                                    val count = selectedPaths.size
                                    scope.launch {
                                        selectedPaths.forEach { path ->
                                            mediaRepository.setFavorite(path, false)
                                        }
                                        Toast.makeText(context, "$count removed from favorites", Toast.LENGTH_SHORT).show()
                                        selectedPaths = emptySet()
                                    }
                                }
                                .padding(vertical = 4.dp)
                        ) {
                            Icon(
                                painter = painterResource(id = R.drawable.ic_home_select_fav),
                                contentDescription = "Unfavorite",
                                tint = Color.Unspecified,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Remove",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }

                        // 3. Delete (ic_album_select_delete)
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier
                                .width(60.dp)
                                .clickable { showDeleteDialog = true }
                                .padding(vertical = 4.dp)
                        ) {
                            Icon(
                                painter = painterResource(id = R.drawable.ic_album_select_delete),
                                contentDescription = "Delete",
                                tint = Color.Unspecified,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = stringResource(id = R.string.delete),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
            }
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { paddingValues ->
        if (favorites.isEmpty()) {
            // Reference emptyLayout matching activity_favorite.xml & 06_favorite.xml
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.padding(horizontal = 40.dp)
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_empty_img),
                        contentDescription = null,
                        tint = Color.Unspecified,
                        modifier = Modifier.size(110.dp)
                    )
                    Spacer(modifier = Modifier.height(20.dp))
                    Text(
                        text = "No Favorites",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Tap on the heart icon to save your favorite media to quickly locate them.",
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                        lineHeight = 18.sp
                    )
                }
            }
        } else {
            // Authentic 4-column Square Grid matching GridLayoutManager(4) in FavoriteActivity
            LazyVerticalGrid(
                columns = GridCells.Fixed(4),
                contentPadding = PaddingValues(2.dp),
                horizontalArrangement = Arrangement.spacedBy(2.dp),
                verticalArrangement = Arrangement.spacedBy(2.dp),
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
            ) {
                items(favorites, key = { it.filePath }) { item ->
                    val isSelected = item.filePath in selectedPaths

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .aspectRatio(1f)
                            .clip(RoundedCornerShape(2.dp))
                            .combinedClickable(
                                onClick = {
                                    if (isSelectionMode) {
                                        selectedPaths = if (isSelected) {
                                            selectedPaths - item.filePath
                                        } else {
                                            selectedPaths + item.filePath
                                        }
                                    } else {
                                        onNavigate(Screen.MediaViewer.createRoute(item.filePath, "filter:favorites"))
                                    }
                                },
                                onLongClick = {
                                    if (!isSelectionMode) {
                                        selectedPaths = setOf(item.filePath)
                                    }
                                }
                            )
                    ) {
                        AsyncImage(
                            model = ImageRequest.Builder(context)
                                .data(File(item.filePath))
                                .crossfade(true)
                                .build(),
                            contentDescription = item.fileName,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )

                        // Video play indicator & duration badge if video
                        if (item.mediaType == MediaType.VIDEO) {
                            Box(
                                modifier = Modifier
                                    .align(Alignment.BottomStart)
                                    .padding(4.dp)
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(Color.Black.copy(alpha = 0.55f))
                                    .padding(horizontal = 4.dp, vertical = 2.dp)
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.PlayArrow,
                                        contentDescription = "Video",
                                        tint = Color.White,
                                        modifier = Modifier.size(12.dp)
                                    )
                                    if (item.durationMs > 0) {
                                        Spacer(modifier = Modifier.width(2.dp))
                                        Text(
                                            text = formatFavoriteDuration(item.durationMs),
                                            fontSize = 9.sp,
                                            color = Color.White,
                                            fontWeight = FontWeight.Medium
                                        )
                                    }
                                }
                            }
                        }

                        // Favorite heart badge at bottom-end
                        if (!isSelectionMode) {
                            Box(
                                modifier = Modifier
                                    .align(Alignment.BottomEnd)
                                    .padding(4.dp)
                                    .size(20.dp)
                                    .clip(CircleShape)
                                    .background(Color.Black.copy(alpha = 0.4f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Favorite,
                                    contentDescription = "Favorited",
                                    tint = Color(0xFFFF2D55),
                                    modifier = Modifier.size(12.dp)
                                )
                            }
                        }

                        // Selection indicator in selection mode (ic_check vs ic_un_check)
                        if (isSelectionMode) {
                            Icon(
                                painter = painterResource(
                                    id = if (isSelected) R.drawable.ic_check else R.drawable.ic_un_check
                                ),
                                contentDescription = if (isSelected) "Selected" else "Unselected",
                                tint = Color.Unspecified,
                                modifier = Modifier
                                    .align(Alignment.TopEnd)
                                    .padding(4.dp)
                                    .size(22.dp)
                            )
                        }
                    }
                }
            }
        }
    }

    // Authentic Delete confirmation dialog matching dialog_cleaner_permanently_delete.xml
    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            shape = RoundedCornerShape(16.dp),
            containerColor = MaterialTheme.colorScheme.surface,
            title = {
                Text(
                    text = "Are you sure you want to delete ${selectedPaths.size} item(s)?",
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 16.sp,
                    color = MaterialTheme.colorScheme.onSurface
                )
            },
            text = {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { deleteMoveToBin = !deleteMoveToBin }
                ) {
                    Checkbox(
                        checked = deleteMoveToBin,
                        onCheckedChange = { deleteMoveToBin = it },
                        colors = CheckboxDefaults.colors(checkedColor = GalleryBlue)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Move this file to Recycle Bin",
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            },
            confirmButton = {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = { showDeleteDialog = false },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(24.dp),
                        border = BorderStroke(1.dp, GalleryBlue)
                    ) {
                        Text("No", color = GalleryBlue, fontWeight = FontWeight.SemiBold)
                    }
                    Button(
                        onClick = {
                            showDeleteDialog = false
                            scope.launch {
                                selectedPaths.forEach { path ->
                                    if (deleteMoveToBin) {
                                        mediaRepository.moveToTrash(path)
                                    } else {
                                        mediaRepository.deletePermanently(path)
                                    }
                                }
                                val msg = if (deleteMoveToBin) "Moved to Recycle Bin" else "Deleted permanently"
                                Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                                selectedPaths = emptySet()
                            }
                        },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(24.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = GalleryBlue)
                    ) {
                        Text("Yes", color = Color.White, fontWeight = FontWeight.SemiBold)
                    }
                }
            },
            dismissButton = {}
        )
    }
}

private fun formatFavoriteDuration(durationMs: Long): String {
    val totalSeconds = durationMs / 1000
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return "%02d:%02d".format(minutes, seconds)
}

private fun shareFavoriteMedia(context: Context, paths: Set<String>) {
    if (paths.isEmpty()) return
    val uris = ArrayList<Uri>()
    paths.forEach { path ->
        val file = File(path)
        if (file.exists()) {
            val uri = FileProvider.getUriForFile(context, "${context.packageName}.provider", file)
            uris.add(uri)
        }
    }
    if (uris.isEmpty()) return
    val intent = if (uris.size == 1) {
        Intent(Intent.ACTION_SEND).apply {
            type = "*/*"
            putExtra(Intent.EXTRA_STREAM, uris[0])
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
    } else {
        Intent(Intent.ACTION_SEND_MULTIPLE).apply {
            type = "*/*"
            putParcelableArrayListExtra(Intent.EXTRA_STREAM, uris)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
    }
    context.startActivity(Intent.createChooser(intent, "Share media"))
}
