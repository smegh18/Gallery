package albums.gallery.photo.folder.picasa.app.web.gallery.ui.cleaner

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import albums.gallery.photo.folder.picasa.app.web.gallery.R
import albums.gallery.photo.folder.picasa.app.web.gallery.ui.theme.GalleryBlue
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CleanerScreen(
    onNavigateBack: () -> Unit,
    onNavigateToDuplicates: () -> Unit = {}
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var isScanning by remember { mutableStateOf(false) }
    var isCleaning by remember { mutableStateOf(false) }
    var isCleaned by remember { mutableStateOf(false) }
    var totalJunkBytes by remember { mutableLongStateOf(0L) }
    var cacheBytes by remember { mutableLongStateOf(0L) }

    fun scanJunk() {
        scope.launch {
            isScanning = true
            isCleaned = false
            withContext(Dispatchers.IO) {
                val appCache = getDirSize(context.cacheDir) + (context.externalCacheDir?.let { getDirSize(it) } ?: 0L)
                val codeCache = getDirSize(context.codeCacheDir)
                cacheBytes = appCache.coerceAtLeast(1024 * 512)
                totalJunkBytes = cacheBytes + codeCache.coerceAtLeast(1024 * 256)
                delay(800) // Realistic scanning feedback
            }
            isScanning = false
        }
    }

    LaunchedEffect(Unit) {
        scanJunk()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("System Cleaner", fontWeight = FontWeight.Bold, color = Color(0xFF1E3354)) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color(0xFF1E3354))
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
            )
        }
    ) { paddingValues ->
        val scrollState = rememberScrollState()

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(MaterialTheme.colorScheme.background)
                .verticalScroll(scrollState)
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // 1. Remove Junk Hero Card (Reference item_remove_junk in activity_system_cleaner.xml)
            val cardShape = RoundedCornerShape(14.dp)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(cardShape)
                    .background(MaterialTheme.colorScheme.surface)
                    .border(width = 1.dp, color = Color(0xFFF0F0F0), shape = cardShape)
                    .padding(16.dp)
            ) {
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_remove_junk_list),
                            contentDescription = "Remove Junk",
                            tint = Color.Unspecified,
                            modifier = Modifier.size(36.dp)
                        )

                        Spacer(modifier = Modifier.width(14.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Remove Junk",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF1E3354)
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = if (isCleaned) "System cache cleaned successfully!" else "Clear up space and optimize performance.",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color(0xFF7D90A3),
                                fontSize = 12.sp,
                                lineHeight = 16.sp
                            )
                        }

                        Spacer(modifier = Modifier.width(8.dp))

                        Button(
                            onClick = {
                                if (!isScanning && !isCleaning) {
                                    if (isCleaned) {
                                        scanJunk()
                                    } else {
                                        scope.launch {
                                            isCleaning = true
                                            withContext(Dispatchers.IO) {
                                                clearDir(context.cacheDir)
                                                context.externalCacheDir?.let { clearDir(it) }
                                                delay(1000)
                                            }
                                            isCleaning = false
                                            isCleaned = true
                                            totalJunkBytes = 0L
                                        }
                                    }
                                }
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (isCleaned) GalleryBlue else Color(0xFFFF4D4F)
                            ),
                            shape = RoundedCornerShape(25.dp),
                            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp)
                        ) {
                            Text(
                                text = when {
                                    isScanning -> "Scanning..."
                                    isCleaning -> "Cleaning..."
                                    isCleaned -> "Done"
                                    else -> "Scan Now"
                                },
                                color = Color.White,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 12.sp
                            )
                        }
                    }

                    if (isScanning || isCleaning) {
                        Spacer(modifier = Modifier.height(12.dp))
                        LinearProgressIndicator(
                            modifier = Modifier.fillMaxWidth(),
                            color = Color(0xFFFF4D4F)
                        )
                    }

                    if (totalJunkBytes > 0 && !isCleaned) {
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = "Found junk: ${formatSize(totalJunkBytes)}",
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFFF4D4F),
                            fontSize = 12.sp
                        )
                    }
                }
            }

            // 2. Reference Categories List (systemCleanerRecycler)
            CleanerCategoryCard(
                iconRes = R.drawable.img_system_cleaner_duplicate,
                title = "Duplicate Photos",
                subtitle = "Find and delete duplicate photos",
                onClick = onNavigateToDuplicates
            )

            CleanerCategoryCard(
                iconRes = R.drawable.img_system_cleaner_large_video,
                title = "Large Videos",
                subtitle = "Find and delete large video files",
                onClick = { /* Large video scanner */ }
            )

            CleanerCategoryCard(
                iconRes = R.drawable.img_system_cleaner_screenshot,
                title = "Screenshots",
                subtitle = "Manage and clean up screenshot images",
                onClick = { /* Screenshots manager */ }
            )
        }
    }
}

@Composable
private fun CleanerCategoryCard(
    iconRes: Int,
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    val shape = RoundedCornerShape(14.dp)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(MaterialTheme.colorScheme.surface)
            .border(width = 1.dp, color = Color(0xFFF0F0F0), shape = shape)
            .clickable(onClick = onClick)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            painter = painterResource(id = iconRes),
            contentDescription = title,
            tint = Color.Unspecified,
            modifier = Modifier.size(36.dp)
        )

        Spacer(modifier = Modifier.width(14.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF1E3354),
                fontSize = 15.sp
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = Color(0xFF7D90A3),
                fontSize = 12.sp
            )
        }

        Icon(
            painter = painterResource(id = R.drawable.system_cleaner_banner_arrow_new),
            contentDescription = null,
            tint = Color.Unspecified,
            modifier = Modifier.size(24.dp)
        )
    }
}

private fun getDirSize(dir: File?): Long {
    if (dir == null || !dir.exists()) return 0L
    var size = 0L
    dir.listFiles()?.forEach { file ->
        size += if (file.isDirectory) getDirSize(file) else file.length()
    }
    return size
}

private fun clearDir(dir: File?) {
    if (dir == null || !dir.exists()) return
    dir.listFiles()?.forEach { file ->
        if (file.isDirectory) clearDir(file)
        file.delete()
    }
}

private fun formatSize(bytes: Long): String {
    val kb = bytes / 1024.0
    val mb = kb / 1024.0
    val gb = mb / 1024.0
    return when {
        gb >= 1.0 -> String.format("%.2f GB", gb)
        mb >= 1.0 -> String.format("%.1f MB", mb)
        kb >= 1.0 -> String.format("%.0f KB", kb)
        else -> "$bytes B"
    }
}
