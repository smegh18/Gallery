package com.quizango.gallery.ui.discovery

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DeleteOutline
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
fun QuickOrganizerScreen(
    mediaRepository: MediaRepository,
    onNavigateToRecycle: () -> Unit,
    onNavigateBack: () -> Unit
) {
    val scope = rememberCoroutineScope()
    val allMedia by mediaRepository.getActiveMediaFlow().collectAsState(initial = emptyList())
    var currentItemIndex by remember { mutableIntStateOf(0) }
    var trashSet by remember { mutableStateOf(setOf<String>()) }

    val unorganizedItems = remember(allMedia, trashSet) {
        allMedia.filter { it.filePath !in trashSet }
    }

    val currentItem = unorganizedItems.getOrNull(currentItemIndex)

    fun keepCurrent() {
        if (currentItemIndex < unorganizedItems.size - 1) {
            currentItemIndex++
        }
    }

    fun trashCurrent() {
        currentItem?.let { item ->
            trashSet = trashSet + item.filePath
            scope.launch {
                mediaRepository.moveToTrash(item.filePath)
            }
            if (currentItemIndex >= unorganizedItems.size - 1 && currentItemIndex > 0) {
                currentItemIndex--
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Unorganized",
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
                    IconButton(onClick = onNavigateToRecycle) {
                        Icon(
                            imageVector = Icons.Default.DeleteOutline,
                            contentDescription = "Organizer Trash",
                            tint = MaterialTheme.colorScheme.onBackground
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(MaterialTheme.colorScheme.background)
        ) {
            if (unorganizedItems.isEmpty() || currentItem == null) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.padding(32.dp)
                    ) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_empty_img),
                            contentDescription = null,
                            tint = Color.Unspecified,
                            modifier = Modifier.size(90.dp)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "All caught up!",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Your library has been completely organized.",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFF8C9BAE)
                        )
                    }
                }
            } else {
                // Card View of Current Photo
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .padding(horizontal = 24.dp, vertical = 12.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(Color.Black),
                    contentAlignment = Alignment.Center
                ) {
                    AsyncImage(
                        model = File(currentItem.filePath),
                        contentDescription = currentItem.fileName,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Fit
                    )
                }

                // Controls: Keep (Check) vs Trash (Cross/Delete)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 12.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Trash / Discard Button
                    FilledIconButton(
                        onClick = { trashCurrent() },
                        modifier = Modifier.size(56.dp),
                        colors = IconButtonDefaults.filledIconButtonColors(containerColor = Color(0xFFFF3B30))
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Trash",
                            tint = Color.White,
                            modifier = Modifier.size(28.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(36.dp))

                    // Keep Button
                    FilledIconButton(
                        onClick = { keepCurrent() },
                        modifier = Modifier.size(56.dp),
                        colors = IconButtonDefaults.filledIconButtonColors(containerColor = Color(0xFF34C759))
                    ) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = "Keep",
                            tint = Color.White,
                            modifier = Modifier.size(28.dp)
                        )
                    }
                }

                // Subtitle
                Text(
                    text = "You can select a photo to jump to",
                    fontSize = 12.sp,
                    color = Color(0xFF8C9BAE),
                    modifier = Modifier
                        .align(Alignment.CenterHorizontally)
                        .padding(bottom = 6.dp)
                )

                // Bottom Filmstrip
                LazyRow(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 12.dp),
                    contentPadding = PaddingValues(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    itemsIndexed(unorganizedItems) { idx, item ->
                        val isCurrent = idx == currentItemIndex

                        Box(
                            modifier = Modifier
                                .size(56.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .border(
                                    width = if (isCurrent) 2.5.dp else 1.dp,
                                    color = if (isCurrent) GalleryBlue else Color.Transparent,
                                    shape = RoundedCornerShape(8.dp)
                                )
                                .clickable { currentItemIndex = idx }
                        ) {
                            AsyncImage(
                                model = File(item.filePath),
                                contentDescription = null,
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop
                            )
                        }
                    }
                }
            }
        }
    }
}
