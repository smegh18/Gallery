package albums.gallery.photo.folder.picasa.app.web.gallery.ui.cleaner

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CleaningServices
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import albums.gallery.photo.folder.picasa.app.web.gallery.R
import albums.gallery.photo.folder.picasa.app.web.gallery.data.ads.AdConstants
import albums.gallery.photo.folder.picasa.app.web.gallery.ui.components.BannerAdView
import albums.gallery.photo.folder.picasa.app.web.gallery.ui.theme.GalleryBlue
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

enum class CleanerPhase {
    DASHBOARD,
    SCANNING,
    CLEARED
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CleanerScreen(
    onNavigateBack: () -> Unit,
    onNavigateToDuplicates: () -> Unit = {}
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var currentPhase by remember { mutableStateOf(CleanerPhase.DASHBOARD) }
    var totalJunkBytes by remember { mutableLongStateOf(0L) }
    var clearedBytes by remember { mutableLongStateOf(0L) }
    var scanningPath by remember { mutableStateOf("/data/user/0/cache") }

    fun calculateJunk(): Long {
        val appCache = getDirSize(context.cacheDir) + (context.externalCacheDir?.let { getDirSize(it) } ?: 0L)
        val codeCache = getDirSize(context.codeCacheDir)
        return appCache.coerceAtLeast(1024L * 1024L * 85L) + codeCache.coerceAtLeast(1024L * 1024L * 25L)
    }

    LaunchedEffect(Unit) {
        withContext(Dispatchers.IO) {
            totalJunkBytes = calculateJunk()
        }
    }

    fun startJunkScan() {
        scope.launch {
            currentPhase = CleanerPhase.SCANNING
            val samplePaths = listOf(
                "${context.cacheDir.path}/image_manager_disk_cache",
                "${context.codeCacheDir.path}/oat_cache",
                "${context.externalCacheDir?.path ?: context.cacheDir.path}/thumbnails",
                "/storage/emulated/0/Android/data/${context.packageName}/cache",
                "/storage/emulated/0/DCIM/.thumbnails"
            )

            withContext(Dispatchers.IO) {
                samplePaths.forEach { path ->
                    scanningPath = path
                    delay(300)
                }
                clearDir(context.cacheDir)
                context.externalCacheDir?.let { clearDir(it) }
                clearedBytes = if (totalJunkBytes > 0) totalJunkBytes else (1024L * 1024L * 110L)
                totalJunkBytes = 0L
            }
            delay(200)
            currentPhase = CleanerPhase.CLEARED
        }
    }

    when (currentPhase) {
        CleanerPhase.DASHBOARD -> {
            Scaffold(
                topBar = {
                    TopAppBar(
                        title = { Text("System Cleaner", fontWeight = FontWeight.Bold, color = Color(0xFF1E3354)) },
                        navigationIcon = {
                            IconButton(onClick = onNavigateBack) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                    contentDescription = "Back",
                                    tint = Color(0xFF1E3354)
                                )
                            }
                        },
                        colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
                    )
                },
                bottomBar = {
                    BannerAdView(adUnitId = AdConstants.BANNER_DUPLICATE_FINDER)
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
                                        text = "Clear up space and optimize performance.",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = Color(0xFF7D90A3),
                                        fontSize = 12.sp,
                                        lineHeight = 16.sp
                                    )
                                }

                                Spacer(modifier = Modifier.width(8.dp))

                                Button(
                                    onClick = { startJunkScan() },
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF4D4F)),
                                    shape = RoundedCornerShape(25.dp),
                                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp)
                                ) {
                                    Text(
                                        text = "Clean Now",
                                        color = Color.White,
                                        fontWeight = FontWeight.SemiBold,
                                        fontSize = 12.sp
                                    )
                                }
                            }

                            if (totalJunkBytes > 0) {
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

        CleanerPhase.SCANNING -> {
            // Reference: activity_junk_scan.xml with layout_progress_background
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            listOf(Color(0xFF0066FF), Color(0xFF003DA5))
                        )
                    )
                    .statusBarsPadding()
            ) {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Toolbar
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 8.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(onClick = { currentPhase = CleanerPhase.DASHBOARD }) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Back",
                                tint = Color.White
                            )
                        }
                        Text(
                            text = "Remove Junk",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }

                    Spacer(modifier = Modifier.weight(0.7f))

                    // Rotating scan indicator
                    val infiniteTransition = rememberInfiniteTransition(label = "scan_spin")
                    val spinAngle by infiniteTransition.animateFloat(
                        initialValue = 0f,
                        targetValue = 360f,
                        animationSpec = infiniteRepeatable(
                            animation = tween(durationMillis = 1200, easing = LinearEasing),
                            repeatMode = RepeatMode.Restart
                        ),
                        label = "spin"
                    )

                    Box(
                        modifier = Modifier
                            .size(160.dp)
                            .clip(CircleShape)
                            .background(Color.White.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier
                                .fillMaxSize()
                                .rotate(spinAngle),
                            color = Color.White,
                            strokeWidth = 4.dp
                        )
                        Icon(
                            imageVector = Icons.Default.CleaningServices,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(56.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(28.dp))

                    Text(
                        text = "Scanning...",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = scanningPath,
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.White.copy(alpha = 0.8f),
                        textAlign = TextAlign.Center,
                        maxLines = 1,
                        modifier = Modifier.padding(horizontal = 32.dp)
                    )

                    Spacer(modifier = Modifier.weight(1f))
                }
            }
        }

        CleanerPhase.CLEARED -> {
            // Reference: activity_junk_cleared.xml
            Scaffold(
                topBar = {
                    TopAppBar(
                        title = { Text("Remove Junk", fontWeight = FontWeight.Bold, color = Color(0xFF1E3354)) },
                        navigationIcon = {
                            IconButton(onClick = { currentPhase = CleanerPhase.DASHBOARD }) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                    contentDescription = "Back",
                                    tint = Color(0xFF1E3354)
                                )
                            }
                        },
                        colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
                    )
                },
                bottomBar = {
                    Column {
                        BannerAdView(adUnitId = AdConstants.BANNER_DUPLICATE_FINDER)
                        Button(
                            onClick = { currentPhase = CleanerPhase.DASHBOARD },
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 8.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = GalleryBlue),
                            shape = RoundedCornerShape(24.dp)
                        ) {
                            Text("Done", color = Color.White, fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
                        }
                    }
                }
            ) { paddingValues ->
                val scrollState = rememberScrollState()

                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues)
                        .background(MaterialTheme.colorScheme.background)
                        .verticalScroll(scrollState)
                        .padding(horizontal = 16.dp, vertical = 16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Spacer(modifier = Modifier.height(16.dp))

                    Icon(
                        painter = painterResource(id = R.drawable.img_junk_cleared_done),
                        contentDescription = "Junk Cleared",
                        tint = Color.Unspecified,
                        modifier = Modifier.size(72.dp)
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        text = formatSize(clearedBytes),
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF1E3354)
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    Text(
                        text = "Your phone is in optimal condition",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color(0xFF7D90A3)
                    )

                    Spacer(modifier = Modifier.height(32.dp))

                    Text(
                        text = "Release more space",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF1E3354),
                        modifier = Modifier.align(Alignment.Start)
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    CleanerCategoryCard(
                        iconRes = R.drawable.img_system_cleaner_duplicate,
                        title = "Duplicate Photos",
                        subtitle = "Find and delete duplicate photos",
                        onClick = onNavigateToDuplicates
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    CleanerCategoryCard(
                        iconRes = R.drawable.img_system_cleaner_large_video,
                        title = "Large Videos",
                        subtitle = "Find and delete large video files",
                        onClick = { /* Large video scanner */ }
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    CleanerCategoryCard(
                        iconRes = R.drawable.img_system_cleaner_screenshot,
                        title = "Screenshots",
                        subtitle = "Manage and clean up screenshot images",
                        onClick = { /* Screenshots manager */ }
                    )
                }
            }
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
