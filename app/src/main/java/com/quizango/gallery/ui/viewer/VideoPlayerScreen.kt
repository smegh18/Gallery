package com.quizango.gallery.ui.viewer

import android.app.Activity
import android.content.Context
import android.content.pm.ActivityInfo
import android.content.res.Configuration
import android.media.AudioManager
import android.net.Uri
import android.provider.Settings
import android.view.View
import android.view.WindowManager
import android.widget.Toast
import androidx.annotation.DrawableRes
import androidx.annotation.OptIn
import androidx.annotation.StringRes
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.activity.compose.BackHandler
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.media3.common.C
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.TrackSelectionOverride
import androidx.media3.common.Tracks
import androidx.media3.common.text.CueGroup
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import com.quizango.gallery.R
import com.quizango.gallery.data.local.datastore.AppPreferences
import com.quizango.gallery.data.repository.MediaRepository
import com.quizango.gallery.data.repository.VaultRepository
import com.quizango.gallery.domain.model.MediaItem
import com.quizango.gallery.domain.model.MediaType
import com.quizango.gallery.ui.home.drawableBackground
import com.quizango.gallery.ui.theme.GalleryText
import com.quizango.gallery.ui.theme.sdp
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.io.File
import java.util.Locale
import kotlin.math.roundToInt
import androidx.media3.common.MediaItem as ExoMediaItem

private enum class PlayerMenu { SPEED, SUBTITLE, AUDIO }

// shortcut: no-op; Phase 12 (USG-01) shows dialog_usage_access_video here, where 2.7.2 records ta/d.VIDEO.
internal fun recordVideoUsageAccessTrigger() = Unit

/** Back from the player, or from a viewer that showed a video: the usage-access trigger first, then navigation. */
internal fun leaveWithVideoTrigger(recordsVideo: Boolean, navigate: () -> Unit, trigger: () -> Unit = ::recordVideoUsageAccessTrigger) {
    if (recordsVideo) trigger()
    navigate()
}

/** One selectable Media3 track for the subtitle / audio pickers. */
private class TrackChoice(val group: Tracks.Group, val index: Int, val label: String)

/** 2.7.2 greys out and disables CC when the file has no playable subtitle track. */
internal fun hasSubtitleTrack(groups: List<Tracks.Group>): Boolean =
    groups.any { it.type == C.TRACK_TYPE_TEXT && it.isSupported }

/** VideoPlayerActivity (§2): one player for the screen, queued with the source list's videos. */
@OptIn(UnstableApi::class)
@Composable
fun VideoPlayerScreen(
    filePath: String,
    folderPath: String,
    isRecycle: Boolean,
    isVault: Boolean,
    mediaRepository: MediaRepository,
    vaultRepository: VaultRepository,
    preferences: AppPreferences,
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val activity = context as? Activity
    val scope = rememberCoroutineScope()
    val white = colorResource(R.color.onlyWhite)
    val player = remember { ExoPlayer.Builder(context).build() }

    var playing by remember { mutableStateOf(false) }
    var buffering by remember { mutableStateOf(false) }
    var positionMs by remember { mutableLongStateOf(0L) }
    var durationMs by remember { mutableLongStateOf(0L) }
    var currentPath by remember { mutableStateOf(filePath) }
    var subtitle by remember { mutableStateOf("") }
    var muted by remember { mutableStateOf(false) }
    var speed by remember { mutableFloatStateOf(1f) }
    var repeat by remember { mutableStateOf(VideoRepeat.NO_REPEAT) }
    var showControls by remember { mutableStateOf(true) }
    var locked by remember { mutableStateOf(false) }
    var interaction by remember { mutableIntStateOf(0) }
    var menu by remember { mutableStateOf<PlayerMenu?>(null) }
    var trackChoices by remember { mutableStateOf<List<TrackChoice>>(emptyList()) }
    var hasSubtitles by remember { mutableStateOf(false) }
    var details by remember { mutableStateOf(false) }
    var seekShown by remember { mutableStateOf<TapZone?>(null) }
    var seekTick by remember { mutableIntStateOf(0) }
    var pausedChip by remember { mutableIntStateOf(0) }
    var swipeVolume by remember { mutableStateOf<Boolean?>(null) } // null: no pill, true: volume, false: brightness
    var swipeLevel by remember { mutableFloatStateOf(0f) }
    var swiping by remember { mutableStateOf(false) }

    fun toast(@StringRes message: Int) = Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
    fun touch() { interaction++ }
    fun close() {
        // Audio stops now, not after the exit transition; the dispose below releases the player
        player.stop()
        onNavigateBack()
    }
    // VideoPlayerActivity.Oooo0oO: Back does nothing while locked, otherwise records the video trigger and leaves
    fun back() {
        if (locked) return
        player.stop()
        leaveWithVideoTrigger(recordsVideo = true, navigate = onNavigateBack)
    }
    BackHandler(onBack = ::back)

    DisposableEffect(player) {
        val listener = object : Player.Listener {
            override fun onIsPlayingChanged(isPlaying: Boolean) { playing = isPlaying }
            override fun onPlaybackStateChanged(playbackState: Int) { buffering = playbackState == Player.STATE_BUFFERING }
            override fun onMediaItemTransition(mediaItem: ExoMediaItem?, reason: Int) {
                mediaItem?.mediaId?.let { currentPath = it }
                subtitle = ""
            }
            override fun onTracksChanged(tracks: Tracks) { hasSubtitles = hasSubtitleTrack(tracks.groups) }
            override fun onCues(cueGroup: CueGroup) {
                subtitle = cueGroup.cues.joinToString("\n") { it.text?.toString().orEmpty() }.trim()
            }
            override fun onPlayerError(error: PlaybackException) {
                toast(videoErrorMessage(File(currentPath).exists(), error.errorCode))
                close()
            }
        }
        player.addListener(listener)
        onDispose {
            player.removeListener(listener)
            player.release()
        }
    }
    // ON_PAUSE comes first on Back and on Home, so audio stops before any transition
    LifecycleEventEffect(Lifecycle.Event.ON_PAUSE) { player.pause() }

    // Sensor orientation while open (manifest "sensor"); brightness set by the swipe is window-wide, so both are undone
    DisposableEffect(activity) {
        val before = activity?.requestedOrientation
        activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR
        onDispose {
            if (activity != null && before != null) activity.requestedOrientation = before
            activity?.window?.let { w ->
                w.attributes = w.attributes.apply { screenBrightness = WindowManager.LayoutParams.BRIGHTNESS_OVERRIDE_NONE }
            }
        }
    }
    val landscape = LocalConfiguration.current.orientation == Configuration.ORIENTATION_LANDSCAPE

    val source by produceState<List<MediaItem>?>(null, filePath) {
        value = mediaSourceFlow(mediaRepository, vaultRepository, folderPath, isRecycle, isVault).first()
    }
    LaunchedEffect(source) {
        val items = source ?: return@LaunchedEffect
        if (!File(filePath).exists()) {
            toast(R.string.video_not_found)
            close()
            return@LaunchedEffect
        }
        val queue = videoPlaylist(items, filePath)
        player.setMediaItems(
            queue.paths.map { ExoMediaItem.Builder().setUri(Uri.fromFile(File(it))).setMediaId(it).build() },
            queue.start,
            0L
        )
        player.repeatMode = repeat.exoMode
        player.prepare()
        player.playWhenReady = true
    }
    LaunchedEffect(Unit) { repeat = VideoRepeat.fromSaved(preferences.isNoRepeat.first()) }
    LaunchedEffect(repeat) { player.repeatMode = repeat.exoMode }

    LaunchedEffect(player) {
        while (true) {
            positionMs = player.currentPosition.coerceAtLeast(0L)
            durationMs = player.duration.coerceAtLeast(0L)
            delay(200)
        }
    }
    // Controls hide 3 s after the last interaction; an open picker keeps them up
    LaunchedEffect(showControls, interaction, menu) {
        if (showControls && menu == null) {
            delay(CONTROLS_HIDE_MS)
            showControls = false
        }
    }
    LaunchedEffect(seekTick) {
        delay(SEEK_FEEDBACK_MS)
        seekShown = null
    }
    LaunchedEffect(pausedChip) { if (pausedChip > 0) { delay(PAUSED_CHIP_MS); pausedChip = 0 } }
    LaunchedEffect(swiping) { if (!swiping) { delay(SWIPE_PILL_MS); swipeVolume = null } }

    fun seekBy(delta: Long) {
        player.seekTo(seekTarget(player.currentPosition, delta, player.duration.coerceAtLeast(0L)))
        touch()
    }
    fun step(delta: Int) {
        val to = playlistStep(player.currentMediaItemIndex, delta, player.mediaItemCount)
        if (to == null) toast(if (delta < 0) R.string.no_previous_video else R.string.no_next_video) else player.seekTo(to, 0L)
        touch()
    }
    fun togglePlay() {
        if (player.isPlaying) {
            player.pause()
        } else {
            if (restartsFromZero(player.playbackState)) player.seekTo(0L)
            player.play()
        }
        touch()
    }
    fun openTracks(type: Int, kind: PlayerMenu, @StringRes none: Int) {
        val choices = player.currentTracks.groups.filter { it.type == type }.flatMap { g ->
            (0 until g.length).filter { g.isTrackSupported(it) }.map { i ->
                val f = g.getTrackFormat(i)
                TrackChoice(g, i, f.label ?: f.language?.let { Locale(it).displayLanguage }?.takeIf { it.isNotBlank() } ?: "${i + 1}")
            }
        }
        if (choices.isEmpty()) {
            toast(none)
        } else {
            trackChoices = choices
            menu = kind
        }
        touch()
    }
    fun pickTrack(type: Int, choice: TrackChoice) {
        player.trackSelectionParameters = player.trackSelectionParameters.buildUpon()
            .setTrackTypeDisabled(type, false)
            .setOverrideForType(TrackSelectionOverride(choice.group.mediaTrackGroup, choice.index))
            .build()
        menu = null
    }
    fun brightnessNow(): Float {
        val set = activity?.window?.attributes?.screenBrightness ?: -1f
        return if (set >= 0f) set
        else Settings.System.getInt(context.contentResolver, Settings.System.SCREEN_BRIGHTNESS, 128) / 255f
    }
    val audio = remember { context.getSystemService(Context.AUDIO_SERVICE) as AudioManager }
    val maxVolume = remember { audio.getStreamMaxVolume(AudioManager.STREAM_MUSIC).coerceAtLeast(1) }

    Box(Modifier.fillMaxSize().background(colorResource(R.color.video_player_background))) {
        AndroidView(
            factory = {
                PlayerView(it).apply {
                    useController = false
                    this.player = player
                    // Subtitles are drawn below in video_subtitle_bg instead
                    subtitleView?.visibility = View.GONE
                }
            },
            modifier = Modifier.fillMaxSize()
        )

        // Gesture layer: tap toggles, double-tap thirds seek / pause, vertical swipes set brightness (left) and volume (right)
        Box(
            Modifier
                .fillMaxSize()
                .pointerInput(locked) {
                    detectTapGestures(
                        onTap = {
                            showControls = !showControls
                            touch()
                        },
                        onDoubleTap = { tap ->
                            if (locked) return@detectTapGestures
                            when (val zone = tapZone(tap.x, size.width.toFloat())) {
                                TapZone.BACK, TapZone.FORWARD -> {
                                    seekBy(if (zone == TapZone.BACK) -SEEK_STEP_MS else SEEK_STEP_MS)
                                    seekShown = zone
                                    seekTick++
                                }
                                TapZone.CENTRE -> if (player.isPlaying) {
                                    player.pause()
                                    pausedChip++
                                } else {
                                    togglePlay()
                                }
                            }
                        }
                    )
                }
                .pointerInput(locked) {
                    if (locked) return@pointerInput
                    detectVerticalDragGestures(
                        onDragStart = { start ->
                            val volume = start.x > size.width / 2
                            swipeVolume = volume
                            swipeLevel = if (volume) audio.getStreamVolume(AudioManager.STREAM_MUSIC) / maxVolume.toFloat() else brightnessNow()
                            swiping = true
                        },
                        onDragEnd = { swiping = false },
                        onDragCancel = { swiping = false }
                    ) { change, dy ->
                        change.consume()
                        swipeLevel = (swipeLevel - dy / (size.height * 0.6f)).coerceIn(0f, 1f)
                        if (swipeVolume == true) {
                            audio.setStreamVolume(AudioManager.STREAM_MUSIC, (swipeLevel * maxVolume).roundToInt(), 0)
                        } else {
                            activity?.window?.let { w ->
                                w.attributes = w.attributes.apply { screenBrightness = swipeLevel.coerceAtLeast(0.01f) }
                            }
                        }
                    }
                }
        )

        if (buffering) CircularProgressIndicator(color = white, modifier = Modifier.align(Alignment.Center))

        seekShown?.let { zone ->
            val back = zone == TapZone.BACK
            Column(
                Modifier.align(if (back) Alignment.CenterStart else Alignment.CenterEnd).padding(horizontal = 25.sdp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(painterResource(if (back) R.drawable.ic_video_fastbackward else R.drawable.ic_video_fastforward), null, tint = Color.Unspecified)
                Text(stringResource(R.string.ten_sec), style = GalleryText.BodySmallMedium, color = white)
            }
        }
        if (pausedChip > 0) {
            Row(
                Modifier.align(Alignment.Center).drawableBackground(R.drawable.video_pause_back).padding(horizontal = 8.sdp, vertical = 3.sdp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(painterResource(R.drawable.ic_video_center_pause), null, tint = Color.Unspecified)
                Text(stringResource(R.string.paused), style = GalleryText.BodyMediumMedium, color = white, modifier = Modifier.padding(start = 2.sdp))
            }
        }
        swipeVolume?.let { volume ->
            Column(
                Modifier
                    .align(if (volume) Alignment.CenterEnd else Alignment.CenterStart)
                    .padding(horizontal = 10.sdp)
                    .drawableBackground(R.drawable.video_control_back)
                    .padding(start = 3.sdp, end = 3.sdp, top = 10.sdp, bottom = 8.sdp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(Modifier.size(5.sdp, 80.sdp).clip(RoundedCornerShape(50)).background(white.copy(alpha = 0.3f))) {
                    Box(Modifier.fillMaxWidth().fillMaxHeight(swipeLevel).align(Alignment.BottomCenter).background(white))
                }
                Icon(
                    painterResource(
                        when {
                            !volume -> R.drawable.ic_video_brightness
                            swipeLevel <= 0f -> R.drawable.ic_video_stripe_mute
                            else -> R.drawable.ic_video_stripe_unmute
                        }
                    ),
                    null,
                    tint = Color.Unspecified,
                    modifier = Modifier.padding(top = 8.sdp)
                )
            }
        }

        // Subtitle line 15.sdp above the bottom panel, then the panel itself
        Column(Modifier.align(Alignment.BottomCenter).fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
            if (subtitle.isNotEmpty()) {
                Text(
                    subtitle,
                    style = GalleryText.BodyLargeMedium,
                    color = white,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .padding(bottom = 15.sdp)
                        .drawableBackground(R.drawable.video_subtitle_bg)
                        .padding(horizontal = 5.sdp, vertical = 2.sdp)
                )
            }
            AnimatedVisibility(visible = showControls && !locked, enter = fadeIn(), exit = fadeOut()) {
                Column(
                    Modifier
                        .fillMaxWidth()
                        .drawableBackground(R.drawable.video_bottom_back)
                        .navigationBarsPadding()
                        .padding(top = 12.sdp, bottom = 16.sdp)
                ) {
                    Row(Modifier.fillMaxWidth().padding(horizontal = 10.sdp), verticalAlignment = Alignment.CenterVertically) {
                        Text(formatDuration(positionMs), style = GalleryText.BodyXSmallMedium, color = white, textAlign = TextAlign.Center, modifier = Modifier.widthIn(min = 34.sdp))
                        VideoSeekBar(positionMs, durationMs, white, { player.seekTo(it); positionMs = it; touch() }, Modifier.weight(1f))
                        Text(formatDuration(durationMs), style = GalleryText.BodyXSmallMedium, color = white, textAlign = TextAlign.Center, modifier = Modifier.widthIn(min = 34.sdp))
                    }
                    Box(Modifier.fillMaxWidth().padding(top = 12.sdp)) {
                        Row(Modifier.align(Alignment.CenterStart), verticalAlignment = Alignment.CenterVertically) {
                            PlayerButton(R.drawable.ic_video_unlock, R.string.viewer_lock, 5.sdp) { locked = true; touch() }
                            if (landscape) {
                                PlayerButton(repeat.icon, repeat.label, 6.sdp, Modifier.padding(start = 12.sdp)) {
                                    repeat = repeat.next()
                                    scope.launch { preferences.setNoRepeat(repeat.ordinal) }
                                    touch()
                                }
                            }
                        }
                        Row(Modifier.align(Alignment.Center), verticalAlignment = Alignment.CenterVertically) {
                            if (landscape) PlayerButton(R.drawable.ic_video_ten_backward, R.string.ten_sec, 5.sdp, Modifier.padding(end = 22.sdp)) { seekBy(-SEEK_STEP_MS) }
                            PlayerButton(R.drawable.ic_video_previous, androidx.media3.ui.R.string.exo_controls_previous_description, 5.sdp) { step(-1) }
                            PlayerButton(if (playing) R.drawable.ic_video_pause else R.drawable.ic_video_play, if (playing) PAUSE_LABEL else PLAY_LABEL, 2.sdp, tinted = false) { togglePlay() }
                            PlayerButton(R.drawable.ic_video_next, androidx.media3.ui.R.string.exo_controls_next_description, 5.sdp) { step(1) }
                            if (landscape) PlayerButton(R.drawable.ic_video_ten_forward, R.string.ten_sec, 5.sdp, Modifier.padding(start = 25.sdp)) { seekBy(SEEK_STEP_MS) }
                        }
                        Row(Modifier.align(Alignment.CenterEnd), verticalAlignment = Alignment.CenterVertically) {
                            PlayerButton(R.drawable.ic_video_rotate, R.string.rotate, 5.sdp, Modifier.padding(end = 8.sdp)) {
                                activity?.requestedOrientation =
                                    if (landscape) ActivityInfo.SCREEN_ORIENTATION_PORTRAIT else ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
                                touch()
                            }
                            // shortcut: the floating popup player (overlay permission) is Phase 10; this hook stays a no-op until then
                            PlayerButton(R.drawable.ic_video_floating, R.string.viewer_floating, 5.sdp, Modifier.padding(end = 8.sdp)) { touch() }
                        }
                    }
                }
            }
        }

        // Top bar and the four-column quick-control card
        AnimatedVisibility(
            visible = showControls && !locked,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier.align(Alignment.TopStart)
        ) {
            Column {
                Row(
                    Modifier
                        .fillMaxWidth()
                        .background(colorResource(R.color.videoPlayerColor))
                        .statusBarsPadding()
                        .height(56.dp)
                        .padding(horizontal = 4.sdp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    PlayerButton(R.drawable.ic_back, R.string.viewer_back, 6.sdp, onClick = ::back)
                    Text(
                        File(currentPath).name,
                        style = GalleryText.BodyLargeSemiBold,
                        color = white,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f).padding(start = 4.sdp)
                    )
                    Box(Modifier.padding(start = 6.sdp)) {
                        PlayerButton(
                            R.drawable.ic_subtitle, R.string.subtitle, 3.sdp,
                            Modifier.alpha(if (hasSubtitles) 1f else 0.5f), tinted = false, enabled = hasSubtitles
                        ) {
                            player.pause() // 2.7.2 pauses before showing the Subtitle dialog (p0/d) and does not resume
                            openTracks(C.TRACK_TYPE_TEXT, PlayerMenu.SUBTITLE, R.string.subtitle_not_available)
                        }
                    }
                    Box(Modifier.padding(start = 4.sdp)) {
                        PlayerButton(R.drawable.ic_audio_track, R.string.audio_track, 3.sdp, tinted = false) {
                            openTracks(C.TRACK_TYPE_AUDIO, PlayerMenu.AUDIO, R.string.audio_not_available)
                        }
                        if (menu == PlayerMenu.AUDIO) TrackPopup(trackChoices, { pickTrack(C.TRACK_TYPE_AUDIO, it) }) { menu = null }
                    }
                    PlayerButton(R.drawable.ic_video_property, R.string.details, 3.sdp, Modifier.padding(start = 4.sdp)) { details = true; touch() }
                }
                Row(
                    Modifier
                        .padding(top = 10.sdp, start = 10.sdp, end = 10.sdp)
                        .drawableBackground(R.drawable.video_control_back)
                ) {
                    Box {
                        QuickControl(R.drawable.ic_video_speed, R.string.speed) { menu = PlayerMenu.SPEED; touch() }
                        if (menu == PlayerMenu.SPEED) {
                            OptionsPopup(
                                options = VIDEO_SPEEDS.map(::speedLabel),
                                selected = VIDEO_SPEEDS.indexOf(speed),
                                position = AnchoredPopup(above = false, end = false),
                                background = colorResource(R.color.videoSpeedBackColor),
                                textColor = white,
                                selectIcon = R.drawable.ic_video_speed_select,
                                onPick = {
                                    speed = VIDEO_SPEEDS[it]
                                    player.setPlaybackSpeed(speed)
                                    menu = null
                                },
                                onDismiss = { menu = null }
                            )
                        }
                    }
                    QuickControl(repeat.icon, repeat.label) {
                        repeat = repeat.next()
                        scope.launch { preferences.setNoRepeat(repeat.ordinal) }
                        touch()
                    }
                    // Device-checked on 2.7.2: names the action, "Mute" with the crossed speaker while sound is on
                    QuickControl(if (muted) R.drawable.ic_video_unmute else R.drawable.ic_video_mute, if (muted) R.string.unmute else R.string.mute) {
                        muted = !muted
                        player.volume = if (muted) 0f else 1f
                        touch()
                    }
                    QuickControl(R.drawable.ic_video_ss, R.string.capture_ss, Modifier.padding(end = 8.sdp)) {
                        // 2.7.2 shows no toast for either outcome
                        val path = currentPath
                        val at = player.currentPosition
                        scope.launch { mediaRepository.saveVideoFrame(path, at) }
                        touch()
                    }
                }
            }
        }

        if (locked && showControls) {
            PlayerButton(
                R.drawable.ic_video_lock,
                R.string.viewer_unlock,
                4.sdp,
                Modifier
                    .align(Alignment.TopStart)
                    .statusBarsPadding()
                    .padding(start = 26.sdp, top = 26.sdp)
                    .drawableBackground(R.drawable.video_control_back),
                ripple = false
            ) {
                locked = false
                touch()
            }
        }
    }

    if (menu == PlayerMenu.SUBTITLE) {
        SubtitleDialog(
            trackChoices,
            onDisable = {
                player.trackSelectionParameters = player.trackSelectionParameters.buildUpon()
                    .setTrackTypeDisabled(C.TRACK_TYPE_TEXT, true).build()
                menu = null
            },
            onPick = { pickTrack(C.TRACK_TYPE_TEXT, it) }
        ) { menu = null }
    }
    if (details) {
        val item = source?.firstOrNull { it.filePath == currentPath } ?: fileVideoItem(currentPath)
        val size = player.videoSize
        MediaDetailsDialog(
            item.copy(dimension = item.dimension.ifEmpty { if (size.width > 0) "${size.width}x${size.height}" else "" }),
            onDismiss = { details = false }
        )
    }
}

private fun fileVideoItem(path: String): MediaItem {
    val file = File(path)
    return MediaItem(
        id = 0,
        filePath = path,
        fileName = file.name,
        fileSize = file.length(),
        folderName = file.parentFile?.name ?: "",
        folderPath = file.parent ?: "",
        dateModified = file.lastModified() / 1000,
        dateTaken = file.lastModified() / 1000,
        mediaType = MediaType.VIDEO
    )
}

/** An activity_video_player icon button on custom_video_ripple; [tinted] mirrors the layout's onlyWhite tint. */
@Composable
private fun PlayerButton(
    @DrawableRes icon: Int,
    @StringRes label: Int,
    padding: androidx.compose.ui.unit.Dp,
    modifier: Modifier = Modifier,
    tinted: Boolean = true,
    ripple: Boolean = true,
    enabled: Boolean = true,
    onClick: () -> Unit
) {
    Icon(
        painterResource(icon),
        stringResource(label),
        tint = if (tinted) colorResource(R.color.onlyWhite) else Color.Unspecified,
        modifier = modifier
            .clickable(
                enabled = enabled,
                interactionSource = remember { MutableInteractionSource() },
                indication = if (ripple) ripple(bounded = false, color = colorResource(R.color.ripple_color_video)) else null,
                onClick = onClick
            )
            .padding(padding)
    )
}

/** A 52.sdp quick-control column: icon over a BodyXSmallMedium label. */
@Composable
private fun QuickControl(@DrawableRes icon: Int, @StringRes label: Int, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Column(
        modifier
            .padding(horizontal = 1.sdp)
            .width(52.sdp)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = ripple(bounded = false, color = colorResource(R.color.ripple_color_video)),
                onClick = onClick
            )
            .padding(vertical = 10.sdp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(painterResource(icon), null, tint = Color.Unspecified)
        Text(
            stringResource(label),
            style = GalleryText.BodyXSmallMedium,
            color = colorResource(R.color.onlyWhite),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(top = 4.sdp)
        )
    }
}

/** Subtitle / audio picker in the popmenu_video_speed card style, the selected track ticked. */
/** dialog_audio_track titled Subtitle (p0/d): a bottom card, Disable first, then the tracks; the ticked row is the current one. */
@Composable
private fun SubtitleDialog(choices: List<TrackChoice>, onDisable: () -> Unit, onPick: (TrackChoice) -> Unit, onDismiss: () -> Unit) {
    val white = colorResource(R.color.onlyWhite)
    val picked = choices.indexOfFirst { it.group.isTrackSelected(it.index) }
    com.quizango.gallery.ui.home.CommonDialog(onDismiss) {
        Column(
            Modifier
                .fillMaxWidth()
                .background(colorResource(R.color.videoPlayerColor), RoundedCornerShape(14.sdp))
                .padding(bottom = 5.sdp)
        ) {
            Text(stringResource(R.string.subtitle), style = GalleryText.BodyXXLargeSemiBold, color = white,
                modifier = Modifier.padding(start = 12.sdp, top = 12.sdp))
            Column(Modifier.padding(vertical = 12.sdp)) {
                val rows = listOf(stringResource(R.string.disable)) + choices.map { it.label }
                rows.forEachIndexed { i, label ->
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .clickable { if (i == 0) onDisable() else onPick(choices[i - 1]) }
                            .padding(horizontal = 12.sdp, vertical = 6.sdp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(label, style = GalleryText.BodyMediumMedium, color = white, modifier = Modifier.weight(1f))
                        if (i == picked + 1) Icon(painterResource(R.drawable.ic_video_speed_select), null, tint = Color.Unspecified)
                    }
                }
            }
        }
    }
}

@Composable
private fun TrackPopup(choices: List<TrackChoice>, onPick: (TrackChoice) -> Unit, onDismiss: () -> Unit) {
    OptionsPopup(
        options = choices.map { it.label },
        selected = choices.indexOfFirst { it.group.isTrackSelected(it.index) },
        position = AnchoredPopup(above = false, end = true),
        background = colorResource(R.color.videoSpeedBackColor),
        textColor = colorResource(R.color.onlyWhite),
        selectIcon = R.drawable.ic_video_speed_select,
        onPick = { onPick(choices[it]) },
        onDismiss = onDismiss
    )
}
