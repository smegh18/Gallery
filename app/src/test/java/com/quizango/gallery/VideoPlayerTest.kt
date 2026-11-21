package com.quizango.gallery

import com.quizango.gallery.domain.model.MediaItem
import com.quizango.gallery.domain.model.MediaType
import com.quizango.gallery.ui.navigation.Screen
import com.quizango.gallery.ui.viewer.VideoPlaylist
import com.quizango.gallery.ui.viewer.playsInline
import com.quizango.gallery.ui.viewer.videoPlaylist
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
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
}
