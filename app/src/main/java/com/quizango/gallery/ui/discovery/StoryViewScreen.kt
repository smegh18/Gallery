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
import androidx.compose.foundation.clickable
import androidx.compose.foundation.shape.CircleShape
import com.quizango.gallery.R
import androidx.compose.ui.res.painterResource

/** The Albums tab's story row, so the viewer plays the same photos and a finished story opens the next one. */
object StoryQueue {
    var stories: List<Story> = emptyList()

    fun find(id: String): Story? = stories.firstOrNull { it.id == id }

    fun next(id: String): Story? {
        val i = stories.indexOfFirst { it.id == id }
        return if (i < 0) null else stories.getOrNull(i + 1)
    }
}

/** Reference story title: the photo's date, e.g. "04 Nov 2026". */
private fun storyDate(item: MediaItem): String {
    val millis = if (item.dateTaken > 0) item.dateTaken else item.dateModified
    return java.text.SimpleDateFormat("dd MMM yyyy", java.util.Locale.getDefault()).format(java.util.Date(millis))
}

@Composable
fun StoryViewScreen(
    storyTitle: String,
    mediaItems: List<MediaItem>,
    onEditClick: ((String) -> Unit)? = null,
    onNavigateBack: () -> Unit,
    // Reference: after the last photo, play the next story in the row
    onFinished: () -> Unit = onNavigateBack
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
        val latest by rememberUpdatedState(mediaItems) // items may still be loading
        LaunchedEffect(Unit) {
            kotlinx.coroutines.delay(800)
            if (latest.isEmpty()) {
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
                onFinished()
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
                                onFinished()
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
                        trackColor = Color.White.copy(alpha = 0.35f),
                        // plain filling line: no M3 end dot or gap
                        gapSize = 0.dp,
                        drawStopIndicator = {}
                    )
                }
            }

            // Reference header (activity_story_view_photo): close, photo date, share
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 12.dp)
                    .height(28.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_story_close),
                    contentDescription = "Close",
                    tint = Color.Unspecified,
                    modifier = Modifier
                        .clip(CircleShape)
                        .clickable(onClick = onNavigateBack)
                        .padding(5.dp)
                )
                Text(
                    text = storyDate(currentItem),
                    color = Color.White,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 13.sp,
                    style = androidx.compose.ui.text.TextStyle(
                        shadow = androidx.compose.ui.graphics.Shadow(Color(0x80000000), androidx.compose.ui.geometry.Offset(2f, 2f), 4f)
                    ),
                    modifier = Modifier
                        .padding(start = 16.dp)
                        .weight(1f)
                )
                Icon(
                    painter = painterResource(R.drawable.ic_share_story),
                    contentDescription = "Share",
                    tint = Color.Unspecified,
                    modifier = Modifier
                        .clip(CircleShape)
                        .clickable {
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
                        }
                        .padding(5.dp)
                )
            }
        }

        // Reference Edit pill: #DD000000, 6dp corners, 32dp high, 30dp above the bottom
        if (onEditClick != null) Row(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(bottom = 30.dp)
                .height(32.dp)
                .widthIn(min = 80.dp)
                .clip(RoundedCornerShape(6.dp))
                .background(Color(0xDD000000))
                .clickable { onEditClick(currentItem.filePath) }
                .padding(start = 14.dp, end = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Icon(painter = painterResource(R.drawable.ic_edit), contentDescription = null, tint = Color.Unspecified)
            Text(
                text = "Edit",
                color = Color.White,
                fontWeight = FontWeight.SemiBold,
                fontSize = 13.sp,
                maxLines = 1,
                modifier = Modifier.padding(start = 6.dp)
            )
        }
    }
}
