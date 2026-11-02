package com.quizango.gallery.ui.discovery

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.quizango.gallery.R
import com.quizango.gallery.data.repository.MediaRepository
import com.quizango.gallery.domain.model.MediaItem
import com.quizango.gallery.ui.theme.GalleryBlue
import kotlinx.coroutines.launch
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OrganizerRecycleScreen(
    mediaRepository: MediaRepository,
    onNavigateBack: () -> Unit
) {
    val scope = rememberCoroutineScope()
    val trashList by mediaRepository.getRecycleBinFlow().collectAsState(initial = emptyList())
    var selectedItem by remember { mutableStateOf<MediaItem?>(null) }
    var showActionDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Organizer Bin",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = MaterialTheme.colorScheme.onBackground
                        )
                    }
                },
                actions = {
                    if (trashList.isNotEmpty()) {
                        TextButton(
                            onClick = {
                                val paths = trashList.map { it.filePath }
                                scope.launch {
                                    mediaRepository.deletePermanently(paths)
                                }
                            }
                        ) {
                            Text(
                                text = "Empty Bin",
                                color = Color(0xFFFF3B30),
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(MaterialTheme.colorScheme.background)
        ) {
            if (trashList.isEmpty()) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_empty_img),
                        contentDescription = null,
                        tint = Color.Unspecified,
                        modifier = Modifier.size(90.dp)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "Organizer bin is empty",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Photos you discard during quick organization will appear here.",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFF8C9BAE),
                        textAlign = TextAlign.Center
                    )
                }
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(3),
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(2.dp),
                    horizontalArrangement = Arrangement.spacedBy(2.dp),
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    items(trashList, key = { it.filePath }) { item ->
                        Box(
                            modifier = Modifier
                                .aspectRatio(1f)
                                .clickable {
                                    selectedItem = item
                                    showActionDialog = true
                                }
                        ) {
                            AsyncImage(
                                model = File(item.filePath),
                                contentDescription = item.fileName,
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop
                            )
                        }
                    }
                }
            }
        }
    }

    if (showActionDialog && selectedItem != null) {
        val target = selectedItem!!
        AlertDialog(
            onDismissRequest = {
                showActionDialog = false
                selectedItem = null
            },
            title = { Text(target.fileName, maxLines = 1, fontWeight = FontWeight.Bold) },
            text = { Text("Choose an action for this photo.") },
            confirmButton = {
                Button(
                    onClick = {
                        showActionDialog = false
                        scope.launch {
                            mediaRepository.restoreFromTrash(target.filePath)
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = GalleryBlue)
                ) {
                    Icon(Icons.Default.Restore, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Restore")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        showActionDialog = false
                        scope.launch {
                            mediaRepository.deletePermanently(target.filePath)
                        }
                    }
                ) {
                    Text("Delete Permanently", color = Color(0xFFFF3B30))
                }
            }
        )
    }
}
