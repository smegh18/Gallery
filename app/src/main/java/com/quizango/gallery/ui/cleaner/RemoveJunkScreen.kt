package com.quizango.gallery.ui.cleaner

import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.ui.text.style.TextOverflow
import androidx.core.graphics.drawable.toBitmap
import android.content.Context
import android.os.Environment
import android.os.StatFs
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.quizango.gallery.R
import com.quizango.gallery.ui.theme.cardBackground
import com.quizango.gallery.ui.theme.cardBorder
import com.quizango.gallery.ui.theme.isDark
import com.quizango.gallery.ui.theme.settingsDivider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

data class JunkCategory(
    val id: String,
    val name: String,
    val description: String,
    val sizeBytes: Long,
    val iconRes: Int,
    val isChecked: Boolean = true,
    val files: List<java.io.File> = emptyList()
) {
    constructor(id: String, name: String, sizeBytes: Long, iconRes: Int, isChecked: Boolean = true) :
        this(id = id, name = name, description = "", sizeBytes = sizeBytes, iconRes = iconRes, isChecked = isChecked)
}
typealias JunkCategoryItem = JunkCategory

enum class StorageCleanPhase {
    SCANNING,   // JunkScanActivity (Warm orange gradient #FF7000 to #FF8D01)
    OVERVIEW,   // RemoveJunkActivity (Red gradient #F94D3C to #EB1D08 with storage_clean_bg_img)
    CLEANING,   // Cleaning in-progress animation
    COMPLETED   // JunkClearedActivity (img_junk_cleared_done & Release More Space)
}

fun formatStorageBytes(bytes: Long): String {
    val kb = bytes / 1024.0
    val mb = kb / 1024.0
    val gb = mb / 1024.0
    return when {
        gb >= 1.0 -> String.format("%.2f GB", gb)
        mb >= 1.0 -> String.format("%.1f MB", mb)
        kb >= 1.0 -> String.format("%.1f kB", kb) // reference style, e.g. 57.9 kB
        bytes > 0 -> "$bytes B"
        else -> "0 B"
    }
}

/**
 * Storage Clean Suite with 100% visual and functional parity to reference APK
 * Matches:
 * 1. JunkScanActivity (Scanning animation with paths and warm orange gradient)
 * 2. RemoveJunkActivity (Red gradient collapsing header with storage_clean_bg_img,
 *    5 categories with checkmarks, Clean & Deep Clean pill buttons)
 * 3. JunkClearedActivity (Junk cleared celebration + Release More Space options)
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RemoveJunkScreen(
    onNavigateBack: () -> Unit,
    onNavigateToCleaner: () -> Unit = {},
    onNavigateToDuplicates: () -> Unit = {},
    onNavigateToScreenshots: () -> Unit = {},
    onNavigateToLargeVideos: () -> Unit = {}
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    // Saved so Back from a "Release more space" card returns to the result, not a new scan
    var currentPhase by rememberSaveable { mutableStateOf(StorageCleanPhase.SCANNING) }
    var scanningPath by remember { mutableStateOf("/storage/emulated/0") }
    var scanProgress by remember { mutableFloatStateOf(0f) }
    var totalFreedBytes by rememberSaveable { mutableLongStateOf(0L) }
    var totalStorageBytes by remember { mutableLongStateOf(0L) }
    var freeStorageBytes by remember { mutableLongStateOf(0L) }

    var categories by remember {
        mutableStateOf(
            listOf(
                JunkCategoryItem("app_cache", "Junk Files", 0L, R.drawable.ic_cleaner_junk_file),
                JunkCategoryItem("apk_files", "Obsolete APK Files", 0L, R.drawable.ic_cleaner_apk_file),
                JunkCategoryItem("temp_files", "Temp Files", 0L, R.drawable.ic_cleaner_temp_file),
                JunkCategoryItem("log_files", "Log Files", 0L, R.drawable.ic_cleaner_log_file),
                JunkCategoryItem("empty_folder", "Empty Folders", 0L, R.drawable.ic_cleaner_folder_file)
            )
        )
    }

    // Paths the user unticked inside an expanded category
    var excludedPaths by remember { mutableStateOf(setOf<String>()) }
    var expandedId by remember { mutableStateOf<String?>(null) }

    // Real scan (JunkScanner): every size shown comes from files that exist.
    LaunchedEffect(Unit) {
        if (currentPhase == StorageCleanPhase.COMPLETED) return@LaunchedEffect
        currentPhase = StorageCleanPhase.SCANNING
        val results = withContext(Dispatchers.IO) {
            try {
                val stat = StatFs(Environment.getDataDirectory().path)
                totalStorageBytes = stat.totalBytes
                freeStorageBytes = stat.availableBytes
            } catch (_: Exception) {}
            var last = 0L
            com.quizango.gallery.data.local.JunkScanner.scan(
                storageRoot = Environment.getExternalStorageDirectory(),
                appCacheDirs = listOfNotNull(context.cacheDir, context.externalCacheDir)
            ) { path ->
                val now = System.currentTimeMillis()
                if (now - last > 80) { scanningPath = path; last = now } // throttle UI updates
            }.associateBy { it.id }
        }
        categories = categories.map { c ->
            val files = results[c.id]?.files.orEmpty()
            val size = files.sumOf { it.length() }
            // Log files stay unticked by default, as in the reference
            c.copy(files = files, sizeBytes = size, isChecked = size > 0 && c.id != "log_files")
        }
        scanProgress = 1f
        delay(1200) // hold "Scanning Completed!" like the reference
        currentPhase = StorageCleanPhase.OVERVIEW
    }

    val selectedFiles = categories.filter { it.isChecked }.flatMap { c -> c.files.filter { it.path !in excludedPaths } }
    val totalSelectedJunk = selectedFiles.sumOf { it.length() }

    when (currentPhase) {
        StorageCleanPhase.SCANNING -> {
            // Reference JunkScanActivity layout: activity_junk_scan.xml with layout_progress_background (#FF7000 -> #FF8D01)
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            if (scanProgress >= 1f) listOf(Color(0xFFF74734), Color(0xFFEF2915))
                            else listOf(Color(0xFFFF7000), Color(0xFFFF8D01))
                        )
                    )
                    .statusBarsPadding()
            ) {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Toolbar matching activity_junk_scan.xml
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(onClick = onNavigateBack) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Back",
                                tint = Color.White
                            )
                        }
                        Text(
                            text = stringResource(R.string.remove_junk),
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }

                    Spacer(modifier = Modifier.weight(0.7f))

                    // Animated Scanning Radar / Circle
                    val infiniteTransition = rememberInfiniteTransition(label = "scanner_pulse")
                    val pulseScale by infiniteTransition.animateFloat(
                        initialValue = 0.90f,
                        targetValue = 1.10f,
                        animationSpec = infiniteRepeatable(
                            animation = tween(800, easing = FastOutSlowInEasing),
                            repeatMode = RepeatMode.Reverse
                        ),
                        label = "pulse"
                    )
                    val sweepAngle by infiniteTransition.animateFloat(
                        initialValue = 0f,
                        targetValue = 360f,
                        animationSpec = infiniteRepeatable(
                            animation = tween(1200, easing = LinearEasing),
                            repeatMode = RepeatMode.Restart
                        ),
                        label = "sweep"
                    )

                    Box(
                        modifier = Modifier
                            .size(180.dp)
                            .clip(CircleShape)
                            .background(Color.White.copy(alpha = 0.12f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Box(
                            modifier = Modifier
                                .size((140 * pulseScale).dp)
                                .clip(CircleShape)
                                .background(Color.White.copy(alpha = 0.20f)),
                            contentAlignment = Alignment.Center
                        ) {
                            if (scanProgress >= 1f) {
                                Box(
                                    modifier = Modifier.size(54.dp).clip(CircleShape).background(Color.White),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Check,
                                        contentDescription = null,
                                        tint = Color(0xFFF26A5E),
                                        modifier = Modifier.size(34.dp)
                                    )
                                }
                            } else {
                                CircularProgressIndicator(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .rotate(sweepAngle),
                                    color = Color.White,
                                    strokeWidth = 3.dp
                                )
                                Icon(
                                    painter = painterResource(id = R.drawable.system_cleaner_main_icon),
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(54.dp)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(36.dp))

                    if (scanProgress >= 1f) {
                        Text(
                            text = "Junk Files Found: ${formatStorageBytes(categories.sumOf { it.sizeBytes })}\nScanning Completed!",
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            textAlign = TextAlign.Center
                        )
                    } else {
                        Text(
                            text = "Scanning...",
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    if (scanProgress < 1f) Text(
                        text = scanningPath,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Normal,
                        color = Color.White.copy(alpha = 0.85f),
                        textAlign = TextAlign.Center,
                        maxLines = 1,
                        modifier = Modifier.padding(horizontal = 24.dp)
                    )

                    Spacer(modifier = Modifier.weight(1f))
                }
            }
        }

        StorageCleanPhase.OVERVIEW -> {
            // Reference RemoveJunkActivity layout: activity_remove_junk.xml
            // Red gradient header (#F94D3C -> #EB1D08) with circular sun container, category list, Clean & Deep Clean buttons
            Scaffold(
                bottomBar = {
                    Surface(
                        color = MaterialTheme.colorScheme.surface,
                        modifier = Modifier
                            .fillMaxWidth()
                            .navigationBarsPadding()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 14.dp),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            val isCleanEnabled = totalSelectedJunk > 0

                            // Clean Button (btnClean)
                            Button(
                                onClick = {
                                    if (isCleanEnabled) {
                                        scope.launch {
                                            currentPhase = StorageCleanPhase.CLEANING
                                            val toDelete = selectedFiles
                                            delay(1200)
                                            totalFreedBytes = withContext(Dispatchers.IO) {
                                                com.quizango.gallery.data.local.JunkScanner.clean(toDelete)
                                            }
                                            currentPhase = StorageCleanPhase.COMPLETED
                                        }
                                    }
                                },
                                enabled = isCleanEnabled,
                                modifier = Modifier
                                    .weight(1f)
                                    .height(48.dp),
                                shape = RoundedCornerShape(25.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (MaterialTheme.colorScheme.isDark) Color(0xFF272727) else Color(0xFFF0F1F8),
                                    disabledContainerColor = if (MaterialTheme.colorScheme.isDark) Color(0xFF1E1E1E) else Color(0xFFE2E4E8),
                                    disabledContentColor = Color(0xFF9EA3AE)
                                )
                            ) {
                                Text(
                                    text = stringResource(R.string.clean),
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 15.sp,
                                    color = if (isCleanEnabled && MaterialTheme.colorScheme.isDark) Color.White else Color(0xFFA7B1BD)
                                )
                            }

                            // Deep Clean Button (btnDelete with deep_clean_ic)
                            Button(
                                onClick = onNavigateToCleaner,
                                modifier = Modifier
                                    .weight(1f)
                                    .height(48.dp),
                                shape = RoundedCornerShape(25.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0B72F6))
                            ) {
                                Icon(
                                    painter = painterResource(id = R.drawable.deep_clean_ic),
                                    contentDescription = "Deep Clean",
                                    tint = Color.White,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = stringResource(R.string.deep_clean),
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White,
                                    fontSize = 15.sp
                                )
                            }
                        }
                    }
                }
            ) { paddingValues ->
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues)
                        .background(MaterialTheme.colorScheme.background)
                ) {
                    // Header with layout_progress_image_view (#F94D3C -> #EB1D08)
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(bottomStart = 16.dp, bottomEnd = 16.dp))
                                .background(
                                    Brush.verticalGradient(
                                        listOf(Color(0xFFF94D3C), Color(0xFFEB1D08))
                                    )
                                )
                                .statusBarsPadding()
                                .padding(bottom = 26.dp)
                        ) {
                            Column(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                // Toolbar
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 16.dp, vertical = 14.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    // Reference header has no back arrow; system back still works
                                    Text(
                                        text = stringResource(R.string.storage_clean),
                                        fontSize = 19.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White
                                    )
                                }

                                Spacer(modifier = Modifier.height(10.dp))

                                // Centered Sun Element (storage_clean_bg_img)
                                Box(
                                    modifier = Modifier.size(160.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Image(
                                        painter = painterResource(id = R.drawable.storage_clean_bg_img),
                                        contentDescription = null,
                                        modifier = Modifier.fillMaxSize()
                                    )

                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        val parts = formatStorageBytes(totalSelectedJunk).split(" ")
                                        Row(verticalAlignment = Alignment.Bottom) {
                                            Text(text = parts[0], fontSize = 30.sp, fontWeight = FontWeight.ExtraBold, color = Color.White)
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text(
                                                text = parts.getOrElse(1) { "" }, fontSize = 13.sp, fontWeight = FontWeight.Bold,
                                                color = Color.White, modifier = Modifier.padding(bottom = 5.dp)
                                            )
                                        }
                                        Spacer(modifier = Modifier.height(2.dp))
                                        Text(
                                            text = stringResource(R.string.junk_found),
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = Color.White.copy(alpha = 0.95f)
                                        )
                                    }
                                }
                            }
                        }
                    }

                    item {
                        Spacer(modifier = Modifier.height(16.dp))
                    }

                    // 5 Junk Categories matching item_remove_junk.xml (no dividers in reference)
                    categories.forEach { item ->
                        item(key = item.id) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable(enabled = item.sizeBytes > 0) {
                                        expandedId = if (expandedId == item.id) null else item.id
                                    }
                                    .padding(horizontal = 16.dp, vertical = 13.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    painter = painterResource(id = item.iconRes),
                                    contentDescription = item.name,
                                    tint = Color.Unspecified,
                                    modifier = Modifier.size(26.dp)
                                )
                                Spacer(modifier = Modifier.width(14.dp))
                                Text(
                                    text = item.name,
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onBackground,
                                    modifier = Modifier.weight(1f)
                                )
                                if (item.sizeBytes > 0) {
                                    Icon(
                                        imageVector = if (expandedId == item.id) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.onBackground,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Text(
                                        text = formatStorageBytes(item.sizeBytes),
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = MaterialTheme.colorScheme.onBackground,
                                        modifier = Modifier.padding(end = 12.dp)
                                    )
                                    JunkCheck(item.isChecked) {
                                        categories = categories.map {
                                            if (it.id == item.id) it.copy(isChecked = !it.isChecked) else it
                                        }
                                        excludedPaths = excludedPaths - item.files.map { it.path }.toSet()
                                    }
                                } else {
                                    Text(
                                        text = stringResource(R.string.no_junk),
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                        if (expandedId == item.id) {
                            // shortcut: shows the first 200 files only; page it if real users hit more.
                            items(item.files.take(200), key = { "${item.id}:${it.path}" }) { file ->
                                val checked = item.isChecked && file.path !in excludedPaths
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(start = 16.dp, end = 16.dp, top = 6.dp, bottom = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    JunkFileIcon(file, item.iconRes)
                                    Spacer(modifier = Modifier.width(14.dp))
                                    Text(
                                        text = if (item.id == "apk_files") file.nameWithoutExtension else file.name,
                                        fontSize = 14.sp,
                                        color = MaterialTheme.colorScheme.onBackground,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        modifier = Modifier.weight(1f)
                                    )
                                    Text(
                                        text = formatStorageBytes(file.length()),
                                        fontSize = 13.sp,
                                        color = MaterialTheme.colorScheme.onBackground,
                                        modifier = Modifier.padding(end = 12.dp)
                                    )
                                    JunkCheck(checked) {
                                        if (checked) {
                                            excludedPaths = excludedPaths + file.path
                                        } else {
                                            excludedPaths = excludedPaths - file.path
                                            if (!item.isChecked) categories = categories.map {
                                                if (it.id == item.id) it.copy(isChecked = true) else it
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    item {
                        Spacer(modifier = Modifier.height(24.dp))
                    }
                }
            }
        }

        StorageCleanPhase.CLEANING -> {
            // Cleaning in-progress view matching clAnimation in activity_remove_junk.xml
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            listOf(Color(0xFFFF7000), Color(0xFFFF8D01))
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.padding(24.dp)
                ) {
                    CircularProgressIndicator(
                        color = Color.White,
                        strokeWidth = 4.dp,
                        modifier = Modifier.size(64.dp)
                    )

                    Spacer(modifier = Modifier.height(24.dp))

                    Text(
                        text = "Cleaning...",
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = "Optimizing storage space",
                        fontSize = 13.sp,
                        color = Color.White.copy(alpha = 0.85f)
                    )
                }
            }
        }

        StorageCleanPhase.COMPLETED -> {
            // Reference JunkClearedActivity: activity_junk_cleared.xml
            Scaffold(
                topBar = {
                    TopAppBar(
                        title = {
                            Text(
                                text = stringResource(R.string.remove_junk),
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
                        colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
                    )
                },
                bottomBar = {
                    Surface(
                        color = MaterialTheme.colorScheme.surface,
                        modifier = Modifier
                            .fillMaxWidth()
                            .navigationBarsPadding()
                    ) {
                        Button(
                            onClick = onNavigateBack,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 12.dp)
                                .height(48.dp),
                            shape = RoundedCornerShape(25.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0066FF))
                        ) {
                            Text(
                                text = "Done",
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp
                            )
                        }
                    }
                }
            ) { paddingValues ->
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues)
                        .background(MaterialTheme.colorScheme.background)
                        .padding(horizontal = 16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Spacer(modifier = Modifier.height(24.dp))

                    Image(
                        painter = painterResource(id = R.drawable.img_junk_cleared_done),
                        contentDescription = "Junk Cleared",
                        modifier = Modifier.size(88.dp)
                    )

                    Spacer(modifier = Modifier.height(18.dp))

                    Text(
                        text = formatStorageBytes(totalFreedBytes),
                        fontSize = 28.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    Text(
                        text = "Your phone is in optimal condition",
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(modifier = Modifier.height(36.dp))

                    Text(
                        text = "Release more space",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground,
                        modifier = Modifier.align(Alignment.Start)
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    // Deep clean options matching reference activity_junk_cleared.xml
                    ReleaseOptionRow(
                        title = "Duplicate Images",
                        subtitle = "Find and remove identical photos.",
                        iconRes = R.drawable.img_system_cleaner_duplicate,
                        onClick = onNavigateToDuplicates
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    ReleaseOptionRow(
                        title = "Screenshots",
                        subtitle = "Manage and delete unwanted screenshots.",
                        iconRes = R.drawable.img_system_cleaner_screenshot,
                        onClick = onNavigateToScreenshots
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    ReleaseOptionRow(
                        title = "Large Video",
                        subtitle = "Free up storage space for more memories.",
                        iconRes = R.drawable.img_system_cleaner_large_video,
                        onClick = onNavigateToLargeVideos
                    )
                }
            }
        }
    }
}

@Composable
private fun ReleaseOptionRow(
    title: String,
    subtitle: String,
    iconRes: Int,
    onClick: () -> Unit
) {
    val shape = RoundedCornerShape(14.dp)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(MaterialTheme.colorScheme.cardBackground)
            .border(width = 1.dp, color = MaterialTheme.colorScheme.cardBorder, shape = shape)
            .clickable(onClick = onClick)
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Image(
            painter = painterResource(id = iconRes),
            contentDescription = title,
            modifier = Modifier.size(46.dp)
        )

        Spacer(modifier = Modifier.width(14.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = subtitle,
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1
            )
        }

        Icon(
            painter = painterResource(id = R.drawable.system_cleaner_banner_arrow),
            contentDescription = null,
            tint = Color.Unspecified,
            modifier = Modifier.size(20.dp)
        )
    }
}



@Composable
private fun JunkCheck(checked: Boolean, onToggle: () -> Unit) {
    Box(
        modifier = Modifier
            .size(22.dp)
            .clip(CircleShape)
            .background(if (checked) Color(0xFF0066FF) else Color.Transparent)
            .border(1.5.dp, if (checked) Color(0xFF0066FF) else Color(0xFFCCCCCC), CircleShape)
            .clickable(onClick = onToggle),
        contentAlignment = Alignment.Center
    ) {
        if (checked) {
            Icon(
                imageVector = Icons.Default.Check,
                contentDescription = "Selected",
                tint = Color.White,
                modifier = Modifier.size(13.dp)
            )
        }
    }
}

/** APKs show their app icon like the reference; other files show the category icon. */
@Composable
private fun JunkFileIcon(file: java.io.File, fallbackRes: Int) {
    val context = LocalContext.current
    val icon by produceState<android.graphics.drawable.Drawable?>(null, file) {
        if (file.name.endsWith(".apk", ignoreCase = true)) {
            value = withContext(Dispatchers.IO) {
                try {
                    val pm = context.packageManager
                    pm.getPackageArchiveInfo(file.path, 0)?.applicationInfo?.let { ai ->
                        ai.sourceDir = file.path
                        ai.publicSourceDir = file.path
                        ai.loadIcon(pm)
                    }
                } catch (_: Exception) {
                    null
                }
            }
        }
    }
    val d = icon
    if (d != null) {
        Image(
            bitmap = d.toBitmap(64, 64).asImageBitmap(),
            contentDescription = null,
            modifier = Modifier.size(32.dp).clip(RoundedCornerShape(6.dp))
        )
    } else {
        Icon(
            painter = painterResource(id = fallbackRes),
            contentDescription = null,
            tint = Color.Unspecified,
            modifier = Modifier.size(26.dp)
        )
    }
}
