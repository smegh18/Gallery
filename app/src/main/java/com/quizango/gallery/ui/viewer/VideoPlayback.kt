package com.quizango.gallery.ui.viewer

import android.media.MediaMetadataRetriever
import androidx.media3.common.Player
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
