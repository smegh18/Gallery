package com.quizango.gallery

import com.quizango.gallery.domain.model.MediaItem
import com.quizango.gallery.domain.model.MediaType
import com.quizango.gallery.ui.navigation.Screen
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import com.quizango.gallery.R
import com.quizango.gallery.ui.viewer.CONTROLS_HIDE_MS
import com.quizango.gallery.ui.viewer.SEEK_STEP_MS
import com.quizango.gallery.ui.viewer.TapZone
import com.quizango.gallery.ui.viewer.VideoPlaylist
import com.quizango.gallery.ui.viewer.VideoRepeat
import com.quizango.gallery.ui.viewer.playlistStep
import com.quizango.gallery.ui.viewer.seekTarget
import com.quizango.gallery.ui.viewer.tapZone
import com.quizango.gallery.ui.viewer.videoErrorMessage
import com.quizango.gallery.ui.viewer.playsInline
import com.quizango.gallery.ui.viewer.videoPlaylist
import com.quizango.gallery.ui.viewer.leaveWithVideoTrigger
import com.quizango.gallery.ui.viewer.recordVideoUsageAccessTrigger
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import java.net.URLDecoder

class VideoPlayerTest {

    private fun item(path: String, type: MediaType) = MediaItem(
        id = 1, filePath = path, fileName = File(path).name, fileSize = 1, folderName = "v", folderPath = "/v",
        dateModified = 1, dateTaken = 1, mediaType = type
    )

    @Test
    fun `videos up to 60 s with a plain path play inline`() {
        assertTrue(playsInline("/v/a.mp4", 60_000L))
        assertTrue(playsInline("/v/a.mp4", 0L))
        assertFalse(playsInline("/v/a.mp4", 60_001L))
        assertFalse(playsInline("content://media/external/video/1", 1_000L))
        assertFalse(playsInline("FILE:///sdcard/a.mp4", 1_000L))
    }

    @Test
    fun `playlist keeps only videos in source order and selects the tapped one`() {
        val list = listOf(
            item("/v/1.mp4", MediaType.VIDEO),
            item("/v/2.jpg", MediaType.IMAGE),
            item("/v/3.mp4", MediaType.VIDEO),
            item("/v/4.gif", MediaType.ANIMATED),
            item("/v/5.mp4", MediaType.VIDEO)
        )
        assertEquals(VideoPlaylist(listOf("/v/1.mp4", "/v/3.mp4", "/v/5.mp4"), 1), videoPlaylist(list, "/v/3.mp4"))
        // A path outside the list (a decrypted vault copy) plays alone
        assertEquals(VideoPlaylist(listOf("/cache/x.mp4"), 0), videoPlaylist(list, "/cache/x.mp4"))
    }

    @Test
    fun `route round-trips path and source without a playlist`() {
        val path = "/storage/emulated/0/Movies/a b&c=d?.mp4"
        val route = Screen.VideoPlayer.createRoute(path, "filter:favorites", isRecycle = false, isVault = true)
        assertTrue(route.startsWith("video?"))
        val args = route.substringAfter('?').split('&').associate { it.substringBefore('=') to it.substringAfter('=') }
        assertEquals(setOf("path", "folder", "isRecycle", "isVault"), args.keys)
        assertEquals(path, URLDecoder.decode(args["path"], "UTF-8"))
        assertEquals("filter:favorites", URLDecoder.decode(args["folder"], "UTF-8"))
        assertEquals("true", args["isVault"])
    }

    @Test
    fun `repeat cycles No Repeat, Repeat One, Loop and back`() {
        assertEquals(VideoRepeat.REPEAT_ONE, VideoRepeat.NO_REPEAT.next())
        assertEquals(VideoRepeat.LOOP, VideoRepeat.REPEAT_ONE.next())
        assertEquals(VideoRepeat.NO_REPEAT, VideoRepeat.LOOP.next())
        assertEquals(listOf(Player.REPEAT_MODE_OFF, Player.REPEAT_MODE_ONE, Player.REPEAT_MODE_ALL), VideoRepeat.entries.map { it.exoMode })
        // isNoRepeat is saved as 0 / 1 / 2; anything else falls back to No Repeat
        assertEquals(VideoRepeat.LOOP, VideoRepeat.fromSaved(2))
        assertEquals(VideoRepeat.NO_REPEAT, VideoRepeat.fromSaved(7))
    }

    @Test
    fun `side double-taps seek 10 s clamped to the video and controls hide after 3 s`() {
        assertEquals(10_000L, SEEK_STEP_MS)
        assertEquals(3_000L, CONTROLS_HIDE_MS)
        assertEquals(0L, seekTarget(4_000L, -SEEK_STEP_MS, 90_000L))
        assertEquals(90_000L, seekTarget(85_000L, SEEK_STEP_MS, 90_000L))
        assertEquals(30_000L, seekTarget(20_000L, SEEK_STEP_MS, 90_000L))
        assertEquals(0L, seekTarget(5_000L, SEEK_STEP_MS, -1L)) // duration unknown (C.TIME_UNSET is negative)
        assertEquals(TapZone.BACK, tapZone(100f, 900f))
        assertEquals(TapZone.CENTRE, tapZone(450f, 900f))
        assertEquals(TapZone.FORWARD, tapZone(800f, 900f))
    }

    @Test
    fun `previous and next refuse the playlist ends`() {
        assertNull(playlistStep(0, -1, 3))
        assertNull(playlistStep(2, 1, 3))
        assertEquals(1, playlistStep(0, 1, 3))
        assertEquals(1, playlistStep(2, -1, 3))
        assertNull(playlistStep(0, 1, 1))
    }

    @Test
    fun `errors map to the reference copy`() {
        assertEquals(R.string.video_not_found, videoErrorMessage(false, PlaybackException.ERROR_CODE_UNSPECIFIED))
        assertEquals(R.string.video_not_found, videoErrorMessage(true, PlaybackException.ERROR_CODE_IO_FILE_NOT_FOUND))
        assertEquals(R.string.this_video_format_not_supported, videoErrorMessage(true, PlaybackException.ERROR_CODE_PARSING_CONTAINER_UNSUPPORTED))
        assertEquals(R.string.this_video_format_not_supported, videoErrorMessage(true, PlaybackException.ERROR_CODE_DECODING_FORMAT_UNSUPPORTED))
        assertEquals(R.string.something_went_wrong, videoErrorMessage(true, PlaybackException.ERROR_CODE_IO_UNSPECIFIED))
    }

    @Test
    fun `player Back records the video usage trigger once, before navigating`() {
        val calls = mutableListOf<String>()
        leaveWithVideoTrigger(recordsVideo = true, navigate = { calls += "navigate" }, trigger = { calls += "trigger" })
        assertEquals(listOf("trigger", "navigate"), calls)
        assertEquals(Unit, recordVideoUsageAccessTrigger()) // Phase 12 owns the dialog; the hook does nothing yet
    }
}
