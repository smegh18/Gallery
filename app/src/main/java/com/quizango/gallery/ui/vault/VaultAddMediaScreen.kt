package com.quizango.gallery.ui.vault

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.quizango.gallery.R
import com.quizango.gallery.data.repository.MediaRepository
import com.quizango.gallery.data.repository.VaultRepository
import com.quizango.gallery.domain.model.MediaType
import com.quizango.gallery.ui.theme.GalleryBlue
import kotlinx.coroutines.launch

/** Reference InternalSelectMediaActivity opened from the vault's "Add Items". */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VaultAddMediaScreen(
    mediaRepository: MediaRepository,
    vaultRepository: VaultRepository,
    onDone: () -> Unit,
    onNavigateBack: () -> Unit
) {
    val scope = rememberCoroutineScope()
    val albums by mediaRepository.getAlbumsFlow().collectAsState(initial = emptyList())
    var albumPath by remember { mutableStateOf<String?>(null) } // null = All Photos
    val media by remember(albumPath) {
        albumPath?.let { mediaRepository.getMediaByFolderFlow(it) } ?: mediaRepository.getActiveMediaFlow()
    }.collectAsState(initial = emptyList())
    var selected by remember { mutableStateOf(setOf<String>()) }
    var menuOpen by remember { mutableStateOf(false) }
    var busy by remember { mutableStateOf(false) }
    val title = albums.firstOrNull { it.path == albumPath }?.name ?: "All Photos"

    Scaffold(
        topBar = {
            Column {
                TopAppBar(
                    title = {
                        Box {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.clickable { menuOpen = true }
                            ) {
                                Text(title, fontSize = 20.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onBackground)
                                Icon(Icons.Default.ArrowDropDown, contentDescription = "Select album", tint = MaterialTheme.colorScheme.onBackground)
                            }
                            DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                                DropdownMenuItem(text = { Text("All Photos") }, onClick = { albumPath = null; menuOpen = false })
                                albums.filter { !it.path.startsWith("filter:") || it.path == "filter:videos" }.forEach { a ->
                                    DropdownMenuItem(text = { Text(a.name) }, onClick = { albumPath = a.path; menuOpen = false })
                                }
                            }
                        }
                    },
                    navigationIcon = {
                        IconButton(onClick = onNavigateBack) {
                            Icon(painterResource(id = R.drawable.ic_back), contentDescription = "Back", tint = MaterialTheme.colorScheme.onBackground)
                        }
                    },
                    actions = {
                        if (selected.isNotEmpty()) {
                            Button(
                                onClick = {
                                    if (busy) return@Button
                                    busy = true
                                    scope.launch {
                                        media.filter { it.filePath in selected }.forEach { vaultRepository.moveToVault(it) }
                                        onDone()
                                    }
                                },
                                shape = RoundedCornerShape(50),
                                colors = ButtonDefaults.buttonColors(containerColor = GalleryBlue),
                                modifier = Modifier.padding(end = 12.dp).height(40.dp)
                            ) { Text("Add", color = Color.White, fontSize = 18.sp) }
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background),
                    expandedHeight = 56.dp
                )
                HorizontalDivider(thickness = 1.dp, color = MaterialTheme.colorScheme.outline)
            }
        },
        bottomBar = {
            if (selected.isNotEmpty()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                        .navigationBarsPadding()
                        .height(60.dp)
                        .padding(horizontal = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = { selected = emptySet() }) {
                        Icon(Icons.Default.Close, contentDescription = "Clear selection", tint = MaterialTheme.colorScheme.onBackground)
                    }
                    Text(
                        "${selected.size} item(s) selected",
                        fontSize = 18.sp,
                        color = MaterialTheme.colorScheme.onBackground,
                        modifier = Modifier.weight(1f).padding(start = 12.dp)
                    )
                    Icon(Icons.Default.KeyboardArrowUp, contentDescription = null, tint = MaterialTheme.colorScheme.onBackground)
                }
            }
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        LazyVerticalGrid(
            columns = GridCells.Fixed(4),
            contentPadding = PaddingValues(3.dp),
            modifier = Modifier.padding(padding).fillMaxSize()
        ) {
            items(media, key = { it.filePath }) { item ->
                val isSel = item.filePath in selected
                Box(
                    modifier = Modifier
                        .padding(1.dp)
                        .aspectRatio(1f)
                        .then(if (isSel) Modifier.border(1.5.dp, GalleryBlue) else Modifier)
                        .clickable { selected = if (isSel) selected - item.filePath else selected + item.filePath }
                ) {
                    AsyncImage(model = item.contentUri, contentDescription = item.fileName, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(8.dp)
                            .size(18.dp)
                            .clip(CircleShape)
                            .background(if (isSel) GalleryBlue else Color.Transparent)
                            .border(1.5.dp, Color.White, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        if (isSel) Icon(Icons.Default.Check, contentDescription = null, tint = Color.White, modifier = Modifier.size(12.dp))
                    }
                    if (item.mediaType == MediaType.VIDEO) {
                        // Reference: play icon + duration
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.align(Alignment.BottomStart).padding(8.dp)
                        ) {
                            Icon(
                                painterResource(id = R.drawable.ic_media_video),
                                contentDescription = null,
                                tint = Color.Unspecified,
                                modifier = Modifier.size(15.dp)
                            )
                            Spacer(Modifier.width(6.dp))
                            Text(formatDurationShort(item.durationMs), color = Color.White, fontSize = 12.sp)
                        }
                    }
                }
            }
        }
    }
}

private fun formatDurationShort(ms: Long): String {
    val s = ms / 1000
    return "%02d:%02d".format(s / 60, s % 60)
}
