package com.quizango.gallery.ui.discovery

import android.content.Intent
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import coil.compose.AsyncImage
import com.quizango.gallery.domain.model.MediaItem
import java.io.File

@Composable
fun StoryViewScreen(
    storyTitle: String,
    mediaItems: List<MediaItem>,
    onEditClick: ((String) -> Unit)? = null,
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current

    if (mediaItems.isEmpty()) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator(color = Color.White)
        }
        LaunchedEffect(Unit) {
            kotlinx.coroutines.delay(800)
            if (mediaItems.isEmpty()) {
                onNavigateBack()
            }
        }
        return
    }

    var currentIndex by remember { mutableIntStateOf(0) }
    var isPaused by remember { mutableStateOf(false) }
    var resetTrigger by remember { mutableIntStateOf(0) }

    val currentItem = mediaItems.getOrNull(currentIndex) ?: mediaItems.first()
    val progressAnimatable = remember { Animatable(0f) }

    // Reset progress when item changes or explicitly restarted
    LaunchedEffect(currentIndex, resetTrigger) {
        progressAnimatable.snapTo(0f)
    }

    // Auto advancement timer with smooth pause/resume progress
    LaunchedEffect(currentIndex, isPaused, resetTrigger) {
        if (!isPaused) {
            val remaining = (1f - progressAnimatable.value).coerceIn(0f, 1f)
            val duration = (remaining * 3500).toInt()
            if (duration > 0) {
                progressAnimatable.animateTo(
                    targetValue = 1f,
                    animationSpec = tween(durationMillis = duration, easing = LinearEasing)
                )
            }
            if (currentIndex < mediaItems.size - 1) {
                currentIndex++
            } else {
                onNavigateBack()
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .pointerInput(Unit) {
                detectTapGestures(
                    onPress = {
                        isPaused = true
                        tryAwaitRelease()
                        isPaused = false
                    },
                    onTap = { offset ->
                        if (offset.x < size.width * 0.35f) {
                            if (currentIndex > 0) {
                                currentIndex--
                            } else {
                                resetTrigger++
                            }
                        } else {
                            if (currentIndex < mediaItems.size - 1) {
                                currentIndex++
                            } else {
                                onNavigateBack()
                            }
                        }
                    }
                )
            }
    ) {
        // Image
        AsyncImage(
            model = File(currentItem.filePath),
            contentDescription = null,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Fit
        )

        // Top Controls: Segmented Progress Bar & Title & Close
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    androidx.compose.ui.graphics.Brush.verticalGradient(
                        listOf(Color.Black.copy(alpha = 0.65f), Color.Black.copy(alpha = 0.25f), Color.Transparent)
                    )
                )
                .statusBarsPadding()
                .padding(horizontal = 12.dp, vertical = 8.dp)
        ) {
            // Segmented Progress Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                for (i in mediaItems.indices) {
                    val progress = when {
                        i < currentIndex -> 1f
                        i == currentIndex -> progressAnimatable.value
                        else -> 0f
                    }

                    LinearProgressIndicator(
                        progress = { progress },
                        modifier = Modifier
                            .weight(1f)
                            .height(3.dp)
                            .clip(RoundedCornerShape(1.5.dp)),
                        color = Color.White,
                        trackColor = Color.White.copy(alpha = 0.35f)
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = storyTitle,
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )

                IconButton(onClick = onNavigateBack) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Close",
                        tint = Color.White
                    )
                }
            }
        }

        // Bottom Bar matching activity_story_view_photo.xml
        Surface(
            color = Color.Black.copy(alpha = 0.5f),
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .navigationBarsPadding()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 32.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.SpaceAround,
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (onEditClick != null) IconButton(onClick = {
                    onEditClick(currentItem.filePath)
                }) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Default.Edit, contentDescription = "Edit", tint = Color.White)
                        Text("Edit", color = Color.White, fontSize = 11.sp)
                    }
                }

                IconButton(onClick = {
                    try {
                        val file = File(currentItem.filePath)
                        val uri = FileProvider.getUriForFile(context, "${context.packageName}.provider", file)
                        val shareIntent = Intent(Intent.ACTION_SEND).apply {
                            type = "image/*"
                            putExtra(Intent.EXTRA_STREAM, uri)
                            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                        }
                        context.startActivity(Intent.createChooser(shareIntent, "Share Story"))
                    } catch (_: Exception) {}
                }) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Default.Share, contentDescription = "Share", tint = Color.White)
                        Text("Share", color = Color.White, fontSize = 11.sp)
                    }
                }
            }
        }
    }
}
