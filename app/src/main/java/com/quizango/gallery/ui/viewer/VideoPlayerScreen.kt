package com.quizango.gallery.ui.viewer

import android.net.Uri
import androidx.annotation.OptIn
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.media3.common.MediaItem as ExoMediaItem
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import com.quizango.gallery.R
import com.quizango.gallery.data.repository.MediaRepository
import com.quizango.gallery.data.repository.VaultRepository
import com.quizango.gallery.ui.theme.GalleryText
import com.quizango.gallery.ui.theme.sdp
import kotlinx.coroutines.flow.first
import java.io.File

/** VideoPlayerActivity: one player for the screen, queued with the source list's videos. */
@OptIn(UnstableApi::class)
@Composable
fun VideoPlayerScreen(
    filePath: String,
    folderPath: String,
    isRecycle: Boolean,
    isVault: Boolean,
    mediaRepository: MediaRepository,
    vaultRepository: VaultRepository,
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val player = remember { ExoPlayer.Builder(context).build() }
    DisposableEffect(player) { onDispose { player.release() } }
    // ON_PAUSE comes first on Back and on Home, so audio stops before any transition
    LifecycleEventEffect(Lifecycle.Event.ON_PAUSE) { player.pause() }

    val playlist by produceState<VideoPlaylist?>(null, filePath) {
        value = videoPlaylist(mediaSourceFlow(mediaRepository, vaultRepository, folderPath, isRecycle, isVault).first(), filePath)
    }
    LaunchedEffect(playlist) {
        val queue = playlist ?: return@LaunchedEffect
        player.setMediaItems(queue.paths.map { ExoMediaItem.fromUri(Uri.fromFile(File(it))) }, queue.start, 0L)
        player.prepare()
        player.playWhenReady = true
    }

    fun close() {
        player.stop()
        onNavigateBack()
    }

    Box(Modifier.fillMaxSize().background(colorResource(R.color.video_player_background))) {
        AndroidView(
            factory = { PlayerView(it).apply { useController = false; this.player = player } },
            modifier = Modifier.fillMaxSize()
        )
        Row(
            Modifier
                .fillMaxWidth()
                .background(colorResource(R.color.videoPlayerColor))
                .statusBarsPadding()
                .height(56.dp)
                .padding(horizontal = 4.sdp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = ::close) {
                Icon(painterResource(R.drawable.ic_back), stringResource(R.string.viewer_back), tint = colorResource(R.color.onlyWhite))
            }
            Text(
                File(filePath).name,
                style = GalleryText.BodyLargeSemiBold,
                color = colorResource(R.color.onlyWhite),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f).padding(start = 4.sdp)
            )
        }
    }
}
