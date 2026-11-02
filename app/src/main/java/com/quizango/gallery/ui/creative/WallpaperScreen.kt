package com.quizango.gallery.ui.creative

import android.app.WallpaperManager
import android.graphics.BitmapFactory
import android.os.Build
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.quizango.gallery.ui.theme.GalleryBlue
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WallpaperScreen(
    imagePath: String,
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var previewMode by remember { mutableStateOf("home") } // "home", "lock"
    var showApplyDialog by remember { mutableStateOf(false) }

    val currentTime = remember {
        SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date())
    }
    val currentDate = remember {
        SimpleDateFormat("EEE, MMM d", Locale.getDefault()).format(Date())
    }

    fun applyWallpaper(which: Int) {
        scope.launch {
            withContext(Dispatchers.IO) {
                try {
                    val wm = WallpaperManager.getInstance(context)
                    val bitmap = BitmapFactory.decodeFile(imagePath)
                    if (bitmap != null) {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                            wm.setBitmap(bitmap, null, true, which)
                        } else {
                            wm.setBitmap(bitmap)
                        }
                    }
                } catch (_: Exception) {}
            }
            Toast.makeText(context, "Wallpaper applied successfully!", Toast.LENGTH_SHORT).show()
            onNavigateBack()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Set Wallpaper",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = Color.White
                        )
                    }
                },
                actions = {
                    Button(
                        onClick = { showApplyDialog = true },
                        shape = RoundedCornerShape(20.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = GalleryBlue),
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 6.dp)
                    ) {
                        Text("Apply", color = Color.White, fontWeight = FontWeight.Bold)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Black.copy(alpha = 0.5f)
                )
            )
        },
        bottomBar = {
            Surface(
                color = Color.Black.copy(alpha = 0.6f),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    FilterChip(
                        selected = previewMode == "lock",
                        onClick = { previewMode = "lock" },
                        label = { Text("Lock Screen Preview") },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = GalleryBlue,
                            selectedLabelColor = Color.White,
                            labelColor = Color.White
                        )
                    )
                    FilterChip(
                        selected = previewMode == "home",
                        onClick = { previewMode = "home" },
                        label = { Text("Home Screen Preview") },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = GalleryBlue,
                            selectedLabelColor = Color.White,
                            labelColor = Color.White
                        )
                    )
                }
            }
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black)
        ) {
            // Wallpaper Background Image
            AsyncImage(
                model = File(imagePath),
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )

            // Preview Overlay
            if (previewMode == "lock") {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues)
                        .padding(top = 60.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = currentTime,
                        fontSize = 72.sp,
                        fontWeight = FontWeight.Light,
                        color = Color.White
                    )
                    Text(
                        text = currentDate,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color.White.copy(alpha = 0.85f)
                    )
                }
            } else {
                // Mock App Icons grid
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues)
                        .padding(horizontal = 24.dp, vertical = 60.dp),
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceAround
                    ) {
                        for (i in 1..4) {
                            Box(
                                modifier = Modifier
                                    .size(54.dp)
                                    .clip(RoundedCornerShape(14.dp))
                                    .background(Color.White.copy(alpha = 0.25f))
                            )
                        }
                    }

                    // Bottom dock
                    Surface(
                        color = Color.White.copy(alpha = 0.2f),
                        shape = RoundedCornerShape(20.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceAround
                        ) {
                            for (i in 1..4) {
                                Box(
                                    modifier = Modifier
                                        .size(48.dp)
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(Color.White.copy(alpha = 0.35f))
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    if (showApplyDialog) {
        AlertDialog(
            onDismissRequest = { showApplyDialog = false },
            title = { Text("Set as Wallpaper", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    TextButton(
                        onClick = {
                            showApplyDialog = false
                            applyWallpaper(WallpaperManager.FLAG_SYSTEM)
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Home Screen", fontSize = 16.sp, modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Start)
                    }
                    TextButton(
                        onClick = {
                            showApplyDialog = false
                            applyWallpaper(WallpaperManager.FLAG_LOCK)
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Lock Screen", fontSize = 16.sp, modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Start)
                    }
                    TextButton(
                        onClick = {
                            showApplyDialog = false
                            applyWallpaper(WallpaperManager.FLAG_SYSTEM or WallpaperManager.FLAG_LOCK)
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Both Screens", fontSize = 16.sp, modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Start)
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { showApplyDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}
