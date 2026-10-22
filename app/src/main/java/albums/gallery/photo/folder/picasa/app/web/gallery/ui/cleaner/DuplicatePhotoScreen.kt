package albums.gallery.photo.folder.picasa.app.web.gallery.ui.cleaner

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
import coil.compose.AsyncImage
import coil.request.ImageRequest
import albums.gallery.photo.folder.picasa.app.web.gallery.R
import albums.gallery.photo.folder.picasa.app.web.gallery.data.repository.MediaRepository
import albums.gallery.photo.folder.picasa.app.web.gallery.domain.model.MediaItem
import albums.gallery.photo.folder.picasa.app.web.gallery.ui.theme.GalleryBlue
import kotlinx.coroutines.launch
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DuplicatePhotoScreen(
    mediaRepository: MediaRepository,
    onNavigateBack: () -> Unit
) {
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val allMedia by mediaRepository.getActiveMediaFlow().collectAsState(initial = emptyList())
    var selectedDuplicates by remember { mutableStateOf(setOf<String>()) }
    var keepBestPhotoEnabled by remember { mutableStateOf(true) }

    // Group media by identical file size or similar name
    val duplicateGroups = remember(allMedia) {
        allMedia.groupBy { "${it.fileSize}_${it.fileName.takeLast(10)}" }
            .filter { it.value.size > 1 }
            .values
            .toList()
    }

    LaunchedEffect(duplicateGroups, keepBestPhotoEnabled) {
        val autoSelected = mutableSetOf<String>()
        duplicateGroups.forEach { group ->
            if (keepBestPhotoEnabled) {
                // Keep the first, mark the others as duplicates
                group.drop(1).forEach { autoSelected.add(it.filePath) }
            } else {
                group.forEach { autoSelected.add(it.filePath) }
            }
        }
        selectedDuplicates = autoSelected
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Duplicate Photos",
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 18.sp,
                        color = Color(0xFF1E3354)
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_back),
                            contentDescription = "Back",
                            tint = Color.Unspecified,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        bottomBar = {
            if (selectedDuplicates.isNotEmpty()) {
                Surface(
                    tonalElevation = 8.dp,
                    color = MaterialTheme.colorScheme.surface,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp, vertical = 12.dp)
                    ) {
                        // Reference delBtnLayout with ic_delete_duplicate_btn_selected_bg
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(50.dp)
                                .clip(RoundedCornerShape(25.dp))
                                .background(Color(0xFFE11A1A))
                                .clickable {
                                    scope.launch {
                                        selectedDuplicates.forEach { mediaRepository.moveToTrash(it) }
                                        selectedDuplicates = emptySet()
                                    }
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "Delete (${selectedDuplicates.size})",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }
                    }
                }
            }
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Keep Best Photo row matching keepPhotoLayout in activity_duplicate_photo.xml
            if (duplicateGroups.isNotEmpty()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Keep best photo",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color(0xFF1E3354),
                        modifier = Modifier.weight(1f)
                    )
                    Switch(
                        checked = keepBestPhotoEnabled,
                        onCheckedChange = { keepBestPhotoEnabled = it },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.White,
                            checkedTrackColor = GalleryBlue
                        )
                    )
                }

                HorizontalDivider(
                    color = Color(0xFFE4E6E9),
                    thickness = 0.5.dp
                )
            }

            if (duplicateGroups.isEmpty()) {
                // Empty layout matching activity_duplicate_photo.xml
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .weight(1f),
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
                            modifier = Modifier.size(100.dp)
                        )
                        Spacer(modifier = Modifier.height(20.dp))
                        Text(
                            text = "No duplicate photos found",
                            fontSize = 17.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color(0xFF1E3354)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Your gallery is clean and organized.",
                            fontSize = 13.sp,
                            color = Color(0xFF707C93),
                            textAlign = TextAlign.Center
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .weight(1f),
                    contentPadding = PaddingValues(14.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    items(duplicateGroups) { group ->
                        DuplicateGroupCard(
                            group = group,
                            selectedDuplicates = selectedDuplicates,
                            onToggleSelection = { path ->
                                selectedDuplicates = if (path in selectedDuplicates) {
                                    selectedDuplicates - path
                                } else {
                                    selectedDuplicates + path
                                }
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun DuplicateGroupCard(
    group: List<MediaItem>,
    selectedDuplicates: Set<String>,
    onToggleSelection: (String) -> Unit
) {
    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(
                text = "${group.size} Similar Photos",
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color(0xFF707C93)
            )

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                group.forEachIndexed { index, item ->
                    val isSelected = item.filePath in selectedDuplicates
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .aspectRatio(1f)
                            .clip(RoundedCornerShape(8.dp))
                            .clickable { onToggleSelection(item.filePath) }
                    ) {
                        AsyncImage(
                            model = ImageRequest.Builder(LocalContext.current)
                                .data(File(item.filePath))
                                .crossfade(true)
                                .build(),
                            contentDescription = null,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )

                        if (index == 0) {
                            Box(
                                modifier = Modifier
                                    .align(Alignment.TopStart)
                                    .background(GalleryBlue)
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = "BEST",
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                            }
                        }

                        Icon(
                            painter = painterResource(
                                id = if (isSelected) R.drawable.circle_checked else R.drawable.circle_unchecked
                            ),
                            contentDescription = null,
                            tint = if (isSelected) GalleryBlue else Color.White,
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .padding(4.dp)
                                .size(20.dp)
                        )
                    }
                }
            }
        }
    }
}
