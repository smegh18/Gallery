package com.quizango.gallery.ui.vault

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.quizango.gallery.R
import com.quizango.gallery.data.repository.VaultRepository
import com.quizango.gallery.domain.model.MediaType
import com.quizango.gallery.domain.model.VaultItem
import com.quizango.gallery.ui.home.formatDuration
import com.quizango.gallery.ui.navigation.Screen
import com.quizango.gallery.ui.theme.GalleryBlue
import kotlinx.coroutines.launch
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VaultMediaScreen(
    vaultRepository: VaultRepository,
    onNavigate: (String) -> Unit = {},
    onNavigateBack: () -> Unit,
    onNavigateToSettings: () -> Unit = {}
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val vaultItems by vaultRepository.getVaultItemsFlow().collectAsState(initial = emptyList())
    var selectedPaths by remember { mutableStateOf(setOf<String>()) }
    var showDeleteConfirmDialog by remember { mutableStateOf(false) }
    var showUnhideConfirmDialog by remember { mutableStateOf(false) }

    val isSelectionMode = selectedPaths.isNotEmpty()

    Scaffold(
        topBar = {
            Column(modifier = Modifier.fillMaxWidth()) {
                TopAppBar(
                    title = {
                        Text(
                            text = if (isSelectionMode) "${selectedPaths.size} Selected" else "Private Vault",
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
                                painter = painterResource(id = if (isSelectionMode) R.drawable.ic_close else R.drawable.ic_back),
                                contentDescription = if (isSelectionMode) "Close" else "Back",
                                tint = MaterialTheme.colorScheme.onBackground,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    },
                    actions = {
                        if (isSelectionMode && vaultItems.isNotEmpty()) {
                            TextButton(
                                onClick = {
                                    selectedPaths = if (selectedPaths.size == vaultItems.size) {
                                        emptySet()
                                    } else {
                                        vaultItems.map { it.filePath }.toSet()
                                    }
                                }
                            ) {
                                Text(
                                    text = if (selectedPaths.size == vaultItems.size) "Deselect" else "Select All",
                                    fontWeight = FontWeight.SemiBold,
                                    color = GalleryBlue,
                                    fontSize = 14.sp
                                )
                            }
                        } else if (!isSelectionMode) {
                            IconButton(onClick = onNavigateToSettings) {
                                Icon(
                                    painter = painterResource(id = R.drawable.ic_settings),
                                    contentDescription = "Vault Settings",
                                    tint = MaterialTheme.colorScheme.onBackground,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.background
                    ),
                    expandedHeight = 56.dp
                )
                HorizontalDivider(thickness = 1.dp, color = MaterialTheme.colorScheme.outline)
            }
        },
        floatingActionButton = {
            if (!isSelectionMode && vaultItems.isNotEmpty()) {
                FloatingActionButton(
                    onClick = { onNavigate(Screen.VaultAdd.route) },
                    containerColor = GalleryBlue,
                    contentColor = Color.White,
                    shape = CircleShape,
                    modifier = Modifier.padding(bottom = 8.dp, end = 8.dp)
                ) {
                    Icon(imageVector = Icons.Default.Add, contentDescription = "Add to Vault", modifier = Modifier.size(28.dp))
                }
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
                            .padding(horizontal = 24.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceAround,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // 1. Share Action
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier
                                .clickable { shareSelectedVaultMedia(context, selectedPaths) }
                                .padding(vertical = 4.dp, horizontal = 12.dp)
                        ) {
                            Icon(
                                painter = painterResource(id = R.drawable.ic_home_select_share),
                                contentDescription = "Share",
                                tint = Color.Unspecified,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Share",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }

                        // 2. Unhide Action (ic_unhide)
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier
                                .clickable { showUnhideConfirmDialog = true }
                                .padding(vertical = 4.dp, horizontal = 12.dp)
                        ) {
                            Icon(
                                painter = painterResource(id = R.drawable.ic_unhide),
                                contentDescription = "Unhide",
                                tint = Color.Unspecified,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Unhide",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }

                        // 3. Delete Action
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier
                                .clickable { showDeleteConfirmDialog = true }
                                .padding(vertical = 4.dp, horizontal = 12.dp)
                        ) {
                            Icon(
                                painter = painterResource(id = R.drawable.ic_album_select_delete),
                                contentDescription = "Delete",
                                tint = Color.Unspecified,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Delete",
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
        if (vaultItems.isEmpty()) {
            // Reference emptyMessageLayout matching activity_vault_media.xml
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.padding(horizontal = 24.dp)
                ) {
                    // Reference empty state: text and "Add Items" only (no icon)
                    Text(
                        text = "Our Private Vault feature lets you store your most sensitive photos and videos securely.",
                        fontSize = 15.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                        lineHeight = 20.sp
                    )
                    Spacer(modifier = Modifier.height(24.dp))
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(24.dp))
                            .background(GalleryBlue)
                            .clickable { onNavigate(Screen.VaultAdd.route) }
                            .size(width = 159.dp, height = 41.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Add Items",
                            color = Color.White,
                            fontWeight = FontWeight.Medium,
                            fontSize = 18.sp
                        )
                    }
                }
            }
        } else {
            LazyVerticalGrid(
                columns = GridCells.Fixed(3),
                contentPadding = PaddingValues(2.dp),
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
            ) {
                // Reference groups vault items under date headers ("Today", "Yesterday", "04 Jul 2026")
                vaultItems.groupBy { vaultDateLabel(it.dateHidden) }.forEach { (label, group) ->
                item(key = "header_$label", span = { androidx.compose.foundation.lazy.grid.GridItemSpan(maxLineSpan) }) {
                    Text(
                        text = label,
                        fontSize = 15.sp,
                        color = MaterialTheme.colorScheme.onBackground,
                        modifier = Modifier.padding(start = 3.dp, top = 6.dp, bottom = 8.dp)
                    )
                }
                items(group, key = { it.filePath }) { item ->
                    val isSelected = item.filePath in selectedPaths
                    // Vault files are encrypted; thumbnails come from a decrypted cache copy
                    val viewFile by produceState<File?>(null, item.filePath) { value = vaultRepository.viewFile(item) }
                    VaultGridItem(
                        item = item,
                        viewFile = viewFile,
                        isSelected = isSelected,
                        isSelectionMode = isSelectionMode,
                        onClick = {
                            if (isSelectionMode) {
                                selectedPaths = if (isSelected) {
                                    selectedPaths - item.filePath
                                } else {
                                    selectedPaths + item.filePath
                                }
                            } else {
                                onNavigate(Screen.MediaViewer.createRoute(item.filePath, isVault = true))
                            }
                        },
                        onLongClick = {
                            if (!isSelectionMode) {
                                selectedPaths = setOf(item.filePath)
                            }
                        }
                    )
                }
                }
            }
        }

        // Unhide Confirmation Dialog
        if (showUnhideConfirmDialog) {
            AlertDialog(
                onDismissRequest = { showUnhideConfirmDialog = false },
                shape = RoundedCornerShape(16.dp),
                containerColor = MaterialTheme.colorScheme.surface,
                title = {
                    Text(
                        text = "Unhide Items",
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 16.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                },
                text = {
                    Text(
                        text = "Are you sure you want to restore these ${selectedPaths.size} item(s) back to the public gallery?",
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                },
                confirmButton = {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 8.dp, vertical = 4.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OutlinedButton(
                            onClick = { showUnhideConfirmDialog = false },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(24.dp),
                            border = BorderStroke(1.dp, GalleryBlue)
                        ) {
                            Text("Cancel", color = GalleryBlue, fontWeight = FontWeight.SemiBold)
                        }
                        Button(
                            onClick = {
                                val targets = vaultItems.filter { it.filePath in selectedPaths }
                                scope.launch {
                                    targets.forEach { vaultRepository.restoreFromVault(it) }
                                    Toast.makeText(context, "Restored ${targets.size} item(s) to gallery", Toast.LENGTH_SHORT).show()
                                    selectedPaths = emptySet()
                                    showUnhideConfirmDialog = false
                                }
                            },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(24.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = GalleryBlue)
                        ) {
                            Text("Unhide", color = Color.White, fontWeight = FontWeight.SemiBold)
                        }
                    }
                },
                dismissButton = {}
            )
        }

        // Permanent Delete Confirmation Dialog
        if (showDeleteConfirmDialog) {
            AlertDialog(
                onDismissRequest = { showDeleteConfirmDialog = false },
                shape = RoundedCornerShape(16.dp),
                containerColor = MaterialTheme.colorScheme.surface,
                title = {
                    Text(
                        text = "Delete from Vault",
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 16.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                },
                text = {
                    Text(
                        text = "Are you sure you want to permanently delete these ${selectedPaths.size} item(s)? This action cannot be undone.",
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                },
                confirmButton = {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 8.dp, vertical = 4.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OutlinedButton(
                            onClick = { showDeleteConfirmDialog = false },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(24.dp),
                            border = BorderStroke(1.dp, GalleryBlue)
                        ) {
                            Text("Cancel", color = GalleryBlue, fontWeight = FontWeight.SemiBold)
                        }
                        Button(
                            onClick = {
                                val targets = vaultItems.filter { it.filePath in selectedPaths }
                                scope.launch {
                                    targets.forEach { vaultRepository.deleteFromVault(it) }
                                    Toast.makeText(context, "Permanently deleted ${targets.size} item(s)", Toast.LENGTH_SHORT).show()
                                    selectedPaths = emptySet()
                                    showDeleteConfirmDialog = false
                                }
                            },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(24.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF4D4F))
                        ) {
                            Text("Delete", color = Color.White, fontWeight = FontWeight.SemiBold)
                        }
                    }
                },
                dismissButton = {}
            )
        }
    }
}

@Composable
private fun VaultGridItem(
    item: VaultItem,
    viewFile: File?,
    isSelected: Boolean,
    isSelectionMode: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit
) {
    val context = LocalContext.current

    Box(
        modifier = Modifier
            .aspectRatio(1f)
            .padding(1.dp)
            .clickable(onClick = onClick)
    ) {
        AsyncImage(
            model = ImageRequest.Builder(context)
                .data(viewFile)
                .size(300, 300)
                .allowRgb565(true)
                .build(),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
        )

        if (item.mediaType == MediaType.VIDEO) {
            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .background(Color.Black.copy(alpha = 0.6f))
                    .padding(horizontal = 4.dp, vertical = 2.dp)
            ) {
                Text(
                    text = formatDuration(item.durationMs),
                    color = Color.White,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        }

        if (isSelectionMode) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(if (isSelected) GalleryBlue.copy(alpha = 0.35f) else Color.Transparent)
            )
            Icon(
                painter = painterResource(
                    id = if (isSelected) R.drawable.ic_check else R.drawable.ic_un_check
                ),
                contentDescription = if (isSelected) "Selected" else "Not selected",
                tint = Color.Unspecified,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(4.dp)
                    .size(24.dp)
            )
        }
    }
}

private fun shareSelectedVaultMedia(context: Context, paths: Set<String>) {
    if (paths.isEmpty()) return
    try {
        val uris = ArrayList<Uri>()
        paths.forEach { path ->
            val file = File(path)
            if (file.exists()) {
                val uri = FileProvider.getUriForFile(context, "${context.packageName}.provider", file)
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


/** "Today", "Yesterday" or "dd MMM yyyy" for a timestamp in seconds or milliseconds. */
internal fun vaultDateLabel(epoch: Long): String {
    val millis = if (epoch in 1..9_999_999_999L) epoch * 1000 else epoch
    val day = java.util.Calendar.getInstance().apply { timeInMillis = millis }
    val today = java.util.Calendar.getInstance()
    fun sameDay(a: java.util.Calendar, b: java.util.Calendar) =
        a.get(java.util.Calendar.YEAR) == b.get(java.util.Calendar.YEAR) &&
            a.get(java.util.Calendar.DAY_OF_YEAR) == b.get(java.util.Calendar.DAY_OF_YEAR)
    if (sameDay(day, today)) return "Today"
    today.add(java.util.Calendar.DAY_OF_YEAR, -1)
    if (sameDay(day, today)) return "Yesterday"
    return java.text.SimpleDateFormat("dd MMM yyyy", java.util.Locale.getDefault()).format(java.util.Date(millis))
}
