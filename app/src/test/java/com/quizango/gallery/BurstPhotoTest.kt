package com.quizango.gallery

import com.quizango.gallery.data.repository.burstGroupKey
import com.quizango.gallery.data.repository.burstKey
import com.quizango.gallery.domain.model.MediaItem
import com.quizango.gallery.domain.model.MediaType
import com.quizango.gallery.ui.discovery.burstGroups
import com.quizango.gallery.ui.discovery.burstRoute
import com.quizango.gallery.ui.navigation.Screen
import org.junit.Assert.assertEquals
import org.junit.Test

class BurstPhotoTest {

    private val camera = "/storage/emulated/0/DCIM/Camera"

    private fun frame(name: String, date: Long, folder: String = camera, type: MediaType = MediaType.IMAGE) = MediaItem(
        id = date, filePath = "$folder/$name", fileName = name, fileSize = 1, folderName = "Camera",
        folderPath = folder, dateModified = date, dateTaken = date, mediaType = type
    )

    private fun key(item: MediaItem, maker: String = "Google") = item.burstKey(maker)

    @Test
    fun keyIsThePathUpToTheLastUnderscoreAndCoverJoinsItsBurst() {
        val a = frame("IMG_20240101_120000_BURST001.jpg", 1)
        val cover = frame("IMG_20240101_120000_BURST002_COVER.jpg", 2)
        assertEquals("$camera/IMG_20240101_120000", key(a))
        assertEquals(key(a), key(cover))
    }

    @Test
    fun onlyCameraBurstJpegsGetAKey() {
        assertEquals("", key(frame("IMG_20240101_120000.jpg", 1))) // no marker
        assertEquals("", key(frame("IMG_20240101_120000_BURST001.png", 1))) // not a JPEG
        assertEquals("", key(frame("IMG_20240101_120000_BURST001.jpg", 1, folder = "/storage/emulated/0/Pictures")))
        assertEquals("", key(frame("VID_20240101_120000_BURST001.jpg", 1, type = MediaType.VIDEO)))
        // Xiaomi names its frames TIMEBURST; a plain BURST name is not one of its bursts
        assertEquals("$camera/IMG_20240101_120000", key(frame("IMG_20240101_120000_TIMEBURST1.jpg", 1), "Xiaomi"))
        assertEquals("", key(frame("IMG_20240101_120000.jpg", 1), "Xiaomi"))
        assertEquals("Cshot1", burstGroupKey("$camera/Cshot/Cshot1/IMG1.jpg", "IMG1.jpg", "$camera/Cshot/Cshot1", true, "OPPO"))
    }

    @Test
    fun groupsKeepFramesTogetherNewestGroupFirstFrameOrderKept() {
        // query order: newest first, two bursts interleaved
        val frames = listOf(
            frame("IMG_20240102_090000_BURST003.jpg", 60),
            frame("IMG_20240101_120000_BURST002.jpg", 50),
            frame("IMG_20240102_090000_BURST002.jpg", 40),
            frame("IMG_20240101_120000_BURST001.jpg", 30),
            frame("IMG_20240102_090000_BURST001.jpg", 20)
        )
        val groups = burstGroups(frames) { key(it) }
        assertEquals(listOf("$camera/IMG_20240102_090000", "$camera/IMG_20240101_120000"), groups.map { it.key })
        assertEquals(listOf(60L, 40L, 20L), groups[0].frames.map { it.dateModified })
        assertEquals(listOf(50L, 30L), groups[1].frames.map { it.dateModified })
    }

    @Test
    fun cardOpensOnlyItsOwnBurst() {
        val frames = listOf(frame("IMG_1_BURST002.jpg", 4), frame("IMG_2_BURST001.jpg", 3), frame("IMG_1_BURST001.jpg", 2))
        val group = burstGroups(frames) { key(it) }.first()
        assertEquals(Screen.AlbumDetail.createRoute("filter:burst:$camera/IMG_1", "Burst 1"), burstRoute(group, "Burst 1"))
        // what MediaRepository resolves for filter:burst:<key>
        assertEquals(listOf("IMG_1_BURST002.jpg", "IMG_1_BURST001.jpg"), frames.filter { key(it) == group.key }.map { it.fileName })
    }
}
