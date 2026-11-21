package com.quizango.gallery.ui.viewer

import android.media.MediaMetadataRetriever
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
