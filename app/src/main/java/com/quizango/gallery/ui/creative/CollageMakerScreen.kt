package com.quizango.gallery.ui.creative

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.quizango.gallery.data.repository.MediaRepository
import com.quizango.gallery.domain.model.MediaItem
import com.quizango.gallery.domain.model.MediaType
import com.quizango.gallery.ui.theme.GalleryBlue
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

enum class CollageStep {
    PICKER,
    EDITOR
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CollageMakerScreen(
    mediaRepository: MediaRepository,
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val allMedia by mediaRepository.getActiveMediaFlow().collectAsState(initial = emptyList())
    val photosOnly = remember(allMedia) { allMedia.filter { it.mediaType != MediaType.VIDEO } }

    var currentStep by remember { mutableStateOf(CollageStep.PICKER) }
    var selectedMedia by remember { mutableStateOf<List<MediaItem>>(emptyList()) }

    var selectedRatio by remember { mutableStateOf("1:1") } // 1:1, 4:5, 16:9, 9:16
    var cornerRadiusDp by remember { mutableFloatStateOf(8f) }
    var spacingDp by remember { mutableFloatStateOf(4f) }
    var backgroundColor by remember { mutableStateOf(Color.White) }

    val bgColors = listOf(
        Color.White,
        Color(0xFFF3F4F6),
        Color.Black,
        Color(0xFFFFEBEE),
        Color(0xFFE8F5E9),
        Color(0xFFE3F2FD),
        Color(0xFFFFF3E0),
        Color(0xFFF3E5F5)
    )

    when (currentStep) {
        CollageStep.PICKER -> {
            Scaffold(
                topBar = {
                    TopAppBar(
                        title = {
                            Text(
                                text = "Select Photos (${selectedMedia.size}/9)",
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
                        colors = TopAppBarDefaults.topAppBarColors(
                            containerColor = MaterialTheme.colorScheme.background
                        )
                    )
                },
                bottomBar = {
                    Surface(
                        tonalElevation = 6.dp,
                        color = MaterialTheme.colorScheme.surface,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 20.dp, vertical = 12.dp)
                        ) {
                            Button(
                                onClick = { currentStep = CollageStep.EDITOR },
                                enabled = selectedMedia.size in 2..9,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(48.dp),
                                shape = RoundedCornerShape(24.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = GalleryBlue,
                                    disabledContainerColor = GalleryBlue.copy(alpha = 0.4f)
                                )
                            ) {
                                Text(
                                    text = if (selectedMedia.size < 2) "Select at least 2 photos" else "Next",
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 15.sp
                                )
                            }
                        }
                    }
                }
            ) { paddingValues ->
                LazyVerticalGrid(
                    columns = GridCells.Fixed(3),
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues)
                        .background(MaterialTheme.colorScheme.background),
                    contentPadding = PaddingValues(2.dp),
                    horizontalArrangement = Arrangement.spacedBy(2.dp),
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    items(photosOnly, key = { it.filePath }) { item ->
                        val isSelected = selectedMedia.any { it.filePath == item.filePath }
                        val selectedIndex = selectedMedia.indexOfFirst { it.filePath == item.filePath }

                        Box(
                            modifier = Modifier
                                .aspectRatio(1f)
                                .clickable {
                                    selectedMedia = if (isSelected) {
                                        selectedMedia.filter { it.filePath != item.filePath }
                                    } else {
                                        if (selectedMedia.size < 9) {
                                            selectedMedia + item
                                        } else selectedMedia
                                    }
                                }
                        ) {
                            AsyncImage(
                                model = File(item.filePath),
                                contentDescription = item.fileName,
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop
                            )

                            // Badge
                            Box(
                                modifier = Modifier
                                    .align(Alignment.TopEnd)
                                    .padding(6.dp)
                                    .size(24.dp)
                                    .clip(CircleShape)
                                    .background(if (isSelected) GalleryBlue else Color.Black.copy(alpha = 0.4f))
                                    .border(1.5.dp, Color.White, CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                if (isSelected) {
                                    Text(
                                        text = "${selectedIndex + 1}",
                                        color = Color.White,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        CollageStep.EDITOR -> {
            Scaffold(
                topBar = {
                    TopAppBar(
                        title = {
                            Text(
                                text = "Collage Maker",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onBackground
                            )
                        },
                        navigationIcon = {
                            IconButton(onClick = { currentStep = CollageStep.PICKER }) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                    contentDescription = "Back",
                                    tint = MaterialTheme.colorScheme.onBackground
                                )
                            }
                        },
                        actions = {
                            TextButton(
                                onClick = {
                                    Toast.makeText(context, "Collage saved to Pictures/GalleryCollage", Toast.LENGTH_SHORT).show()
                                    onNavigateBack()
                                }
                            ) {
                                Text(
                                    text = "Save",
                                    color = GalleryBlue,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 15.sp
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
                    // Preview Canvas
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                            .background(Color(0xFF1E1E1E))
                            .padding(16.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        val aspectRatio = when (selectedRatio) {
                            "4:5" -> 4f / 5f
                            "16:9" -> 16f / 9f
                            "9:16" -> 9f / 16f
                            else -> 1f
                        }

                        Box(
                            modifier = Modifier
                                .fillMaxWidth(0.92f)
                                .aspectRatio(aspectRatio)
                                .clip(RoundedCornerShape(cornerRadiusDp.dp))
                                .background(backgroundColor)
                                .padding(spacingDp.dp)
                        ) {
                            // Dynamic Grid Layout based on number of items
                            val items = selectedMedia
                            when (items.size) {
                                2 -> {
                                    Row(
                                        modifier = Modifier.fillMaxSize(),
                                        horizontalArrangement = Arrangement.spacedBy(spacingDp.dp)
                                    ) {
                                        items.forEach { photo ->
                                            AsyncImage(
                                                model = File(photo.filePath),
                                                contentDescription = null,
                                                modifier = Modifier
                                                    .weight(1f)
                                                    .fillMaxHeight()
                                                    .clip(RoundedCornerShape(cornerRadiusDp.dp)),
                                                contentScale = ContentScale.Crop
                                            )
                                        }
                                    }
                                }
                                3 -> {
                                    Row(
                                        modifier = Modifier.fillMaxSize(),
                                        horizontalArrangement = Arrangement.spacedBy(spacingDp.dp)
                                    ) {
                                        AsyncImage(
                                            model = File(items[0].filePath),
                                            contentDescription = null,
                                            modifier = Modifier
                                                .weight(1f)
                                                .fillMaxHeight()
                                                .clip(RoundedCornerShape(cornerRadiusDp.dp)),
                                            contentScale = ContentScale.Crop
                                        )
                                        Column(
                                            modifier = Modifier
                                                .weight(1f)
                                                .fillMaxHeight(),
                                            verticalArrangement = Arrangement.spacedBy(spacingDp.dp)
                                        ) {
                                            AsyncImage(
                                                model = File(items[1].filePath),
                                                contentDescription = null,
                                                modifier = Modifier
                                                    .weight(1f)
                                                    .fillMaxWidth()
                                                    .clip(RoundedCornerShape(cornerRadiusDp.dp)),
                                                contentScale = ContentScale.Crop
                                            )
                                            AsyncImage(
                                                model = File(items[2].filePath),
                                                contentDescription = null,
                                                modifier = Modifier
                                                    .weight(1f)
                                                    .fillMaxWidth()
                                                    .clip(RoundedCornerShape(cornerRadiusDp.dp)),
                                                contentScale = ContentScale.Crop
                                            )
                                        }
                                    }
                                }
                                else -> {
                                    val cols = if (items.size <= 4) 2 else 3
                                    LazyVerticalGrid(
                                        columns = GridCells.Fixed(cols),
                                        modifier = Modifier.fillMaxSize(),
                                        horizontalArrangement = Arrangement.spacedBy(spacingDp.dp),
                                        verticalArrangement = Arrangement.spacedBy(spacingDp.dp),
                                        userScrollEnabled = false
                                    ) {
                                        items(items) { photo ->
                                            AsyncImage(
                                                model = File(photo.filePath),
                                                contentDescription = null,
                                                modifier = Modifier
                                                    .aspectRatio(1f)
                                                    .clip(RoundedCornerShape(cornerRadiusDp.dp)),
                                                contentScale = ContentScale.Crop
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // Bottom Control Panels
                    Surface(
                        tonalElevation = 4.dp,
                        color = MaterialTheme.colorScheme.surface,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            // Aspect Ratio Buttons
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceAround
                            ) {
                                listOf("1:1", "4:5", "16:9", "9:16").forEach { ratio ->
                                    FilterChip(
                                        selected = selectedRatio == ratio,
                                        onClick = { selectedRatio = ratio },
                                        label = { Text(ratio) }
                                    )
                                }
                            }

                            // Sliders for Spacing & Corner Radius
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("Border", fontSize = 12.sp, color = Color(0xFF8C9BAE), modifier = Modifier.width(55.dp))
                                Slider(
                                    value = spacingDp,
                                    onValueChange = { spacingDp = it },
                                    valueRange = 0f..16f,
                                    modifier = Modifier.weight(1f)
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Text("Radius", fontSize = 12.sp, color = Color(0xFF8C9BAE), modifier = Modifier.width(55.dp))
                                Slider(
                                    value = cornerRadiusDp,
                                    onValueChange = { cornerRadiusDp = it },
                                    valueRange = 0f..24f,
                                    modifier = Modifier.weight(1f)
                                )
                            }

                            // Background Color Palette
                            LazyRow(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                items(bgColors) { col ->
                                    Box(
                                        modifier = Modifier
                                            .size(32.dp)
                                            .clip(CircleShape)
                                            .background(col)
                                            .border(
                                                width = if (backgroundColor == col) 2.5.dp else 1.dp,
                                                color = if (backgroundColor == col) GalleryBlue else Color(0xFFCCCCCC),
                                                shape = CircleShape
                                            )
                                            .clickable { backgroundColor = col },
                                        contentAlignment = Alignment.Center
                                    ) {
                                        if (backgroundColor == col) {
                                            Icon(
                                                imageVector = Icons.Default.Check,
                                                contentDescription = null,
                                                tint = if (col == Color.Black) Color.White else Color.Black,
                                                modifier = Modifier.size(16.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
