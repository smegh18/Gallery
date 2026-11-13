package com.quizango.gallery.ui.discovery

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.quizango.gallery.R
import com.quizango.gallery.data.local.MomentScanner
import com.quizango.gallery.data.repository.MediaRepository
import com.quizango.gallery.domain.model.Album
import com.quizango.gallery.domain.model.MediaType
import com.quizango.gallery.ui.home.AlbumCard
import com.quizango.gallery.ui.theme.GalleryBlue
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MomentsScreen(
    mediaRepository: MediaRepository,
    moments: MomentScanner,
    onMomentClick: (label: String) -> Unit,
    onNavigateBack: () -> Unit
) {
    val allMedia by mediaRepository.getActiveMediaFlow().collectAsState(initial = null)
    val labels by moments.labels.collectAsState()
    val progress by moments.progress.collectAsState()

    val photos = remember(allMedia) { allMedia?.filter { it.mediaType != MediaType.VIDEO } }
    LaunchedEffect(photos) { photos?.let { moments.scan(it) } }

    // Reference: one album per label, filling in while the scan runs
    val groups by produceState(emptyList<Album>(), photos, labels) {
        value = withContext(Dispatchers.Default) {
            MomentScanner.group(photos.orEmpty(), labels).map { (label, items) ->
                val cover = items.first()
                Album(
                    id = "filter:moment:$label",
                    name = label,
                    path = "filter:moment:$label",
                    coverPath = cover.filePath,
                    itemCount = items.size,
                    coverId = cover.id
                )
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Moments",
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
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(MaterialTheme.colorScheme.background)
        ) {
            val p = progress
            when {
                groups.isEmpty() && (p != null || photos == null) -> WaitingForScan(p)
                groups.isEmpty() -> EmptyMoments()
                else -> LazyVerticalGrid(
                    columns = GridCells.Fixed(3),
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    if (p != null) {
                        item(span = { GridItemSpan(maxLineSpan) }) { ScanningCard(p) }
                    }
                    items(groups, key = { it.id }) { album ->
                        AlbumCard(album = album, onClick = { onMomentClick(album.name) })
                    }
                }
            }
        }
    }
}

/** Reference header: "Scanning Photos...." with count, bar and hint. */
@Composable
private fun ScanningCard(p: MomentScanner.Progress) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .padding(14.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = "Scanning Photos....",
                fontSize = 15.sp,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.weight(1f)
            )
            Text(
                text = "${p.done}/${p.total}",
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Spacer(Modifier.height(10.dp))
        LinearProgressIndicator(
            progress = { if (p.total == 0) 0f else p.done.toFloat() / p.total },
            modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(4.dp)),
            color = GalleryBlue,
            trackColor = MaterialTheme.colorScheme.outlineVariant,
            // plain filling line: no M3 end dot or gap
            gapSize = 0.dp,
            drawStopIndicator = {}
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = "Scanning time depends on number of photos",
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun WaitingForScan(p: MomentScanner.Progress?) {
    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        if (p != null) ScanningCard(p)
        Spacer(Modifier.weight(1f))
        CircularProgressIndicator(color = GalleryBlue)
        Spacer(Modifier.height(16.dp))
        Text(
            text = "Kindly wait as we are scanning images. This won’t take long..",
            fontSize = 14.sp,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.weight(1f))
    }
}

@Composable
private fun EmptyMoments() {
    Column(
        modifier = Modifier.fillMaxSize().padding(32.dp),
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
            text = "Oops!",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground
        )
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = "No moment files here.",
            style = MaterialTheme.typography.bodySmall,
            color = Color(0xFF8C9BAE)
        )
    }
}
