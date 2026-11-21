package com.quizango.gallery.ui.viewer

import android.media.MediaMetadataRetriever
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import com.quizango.gallery.R
import com.quizango.gallery.domain.model.MediaItem
import java.io.File

/** ch/m: a video up to 60 s (0xea60) with a plain path plays inside the viewer. */
internal const val SHORT_VIDEO_MAX_MS = 60_000L

/** Longer videos and content:// / file:// inputs open the Video player instead (ch/m onExternalVideoPlay). */
internal fun playsInline(path: String, durationMs: Long): Boolean =
    durationMs <= SHORT_VIDEO_MAX_MS &&
        !path.startsWith("content://", ignoreCase = true) &&
        !path.startsWith("file://", ignoreCase = true)

/** The Video player's queue: the list's videos in list order, starting at the tapped one. */
internal data class VideoPlaylist(val paths: List<String>, val start: Int)

/** MF.Oooooo0: only videos, source order kept; a path outside the list (a vault cache copy) plays alone. */
internal fun videoPlaylist(items: List<MediaItem>, path: String): VideoPlaylist {
    val paths = items.filter { it.isVideo }.map { it.filePath }
    val start = paths.indexOf(path)
    return if (start >= 0) VideoPlaylist(paths, start) else VideoPlaylist(listOf(path), 0)
}

/** The tap on ic_full_video_play checks the file can be read as a video before opening the player. */
internal fun isPlayableVideo(path: String): Boolean {
    if (!File(path).isFile) return false
    val retriever = MediaMetadataRetriever()
    return try {
        retriever.setDataSource(path)
        retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_HAS_VIDEO) == "yes"
    } catch (_: Exception) {
        false
    } finally {
        retriever.release()
    }
}

/** popmenu_short_video_speed / popmenu_video_speed rows. */
internal val VIDEO_SPEEDS = listOf(0.25f, 0.5f, 1.0f, 1.5f, 2.0f)

internal fun speedLabel(speed: Float): String = "${speed}x"

/** The inline view has no repeat: it stops at the end, and the next Play starts again from zero. */
internal fun restartsFromZero(playbackState: Int): Boolean = playbackState == Player.STATE_ENDED

/** VideoPlayerActivity timings: controls auto-hide (0xbb8), ±10 s seek (0x2710), seek arrows (0x258), Paused chip (0x3e8), swipe pill (0x1f4). */
internal const val CONTROLS_HIDE_MS = 3_000L
internal const val SEEK_STEP_MS = 10_000L
internal const val SEEK_FEEDBACK_MS = 600L
internal const val PAUSED_CHIP_MS = 1_000L
internal const val SWIPE_PILL_MS = 500L

internal fun seekTarget(positionMs: Long, deltaMs: Long, durationMs: Long): Long =
    (positionMs + deltaMs).coerceIn(0L, durationMs.coerceAtLeast(0L))

/** Previous / Next index, or null at either end of the queue (no_previous_video / no_next_video). */
internal fun playlistStep(index: Int, delta: Int, size: Int): Int? = (index + delta).takeIf { it in 0 until size }

/** Double-tap thirds of the video area: left seeks back, right seeks forward, the centre pauses. */
internal enum class TapZone { BACK, CENTRE, FORWARD }

internal fun tapZone(x: Float, width: Float): TapZone = when {
    x < width / 3 -> TapZone.BACK
    x > width * 2 / 3 -> TapZone.FORWARD
    else -> TapZone.CENTRE
}

/** isNoRepeat values 0 / 1 / 2; the button cycles No Repeat → Repeat One → Loop. */
internal enum class VideoRepeat(@DrawableRes val icon: Int, @StringRes val label: Int, val exoMode: Int) {
    NO_REPEAT(R.drawable.ic_video_no_repeat, R.string.no_repeat, Player.REPEAT_MODE_OFF),
    REPEAT_ONE(R.drawable.ic_video_repeat, R.string.repeat_one, Player.REPEAT_MODE_ONE),
    LOOP(R.drawable.ic_video_loop, R.string.loop, Player.REPEAT_MODE_ALL);

    fun next(): VideoRepeat = entries[(ordinal + 1) % entries.size]

    companion object {
        fun fromSaved(value: Int): VideoRepeat = entries.getOrElse(value) { NO_REPEAT }
    }
}

/** The reference error copy: missing file, unsupported format, anything else. */
@StringRes
internal fun videoErrorMessage(fileExists: Boolean, errorCode: Int): Int = when {
    !fileExists || errorCode == PlaybackException.ERROR_CODE_IO_FILE_NOT_FOUND -> R.string.video_not_found
    errorCode == PlaybackException.ERROR_CODE_DECODING_FORMAT_UNSUPPORTED ||
        errorCode == PlaybackException.ERROR_CODE_PARSING_CONTAINER_UNSUPPORTED ||
        errorCode == PlaybackException.ERROR_CODE_DECODER_INIT_FAILED -> R.string.this_video_format_not_supported
    else -> R.string.something_went_wrong
}
