package com.quizango.gallery

import com.quizango.gallery.domain.model.MediaItem
import com.quizango.gallery.domain.model.MediaType
import com.quizango.gallery.ui.cleaner.JunkCategory
import com.quizango.gallery.ui.cleaner.formatStorageBytes
import com.quizango.gallery.ui.cleaner.formatVideoDuration
import com.quizango.gallery.ui.cleaner.formatVideoSize
import com.quizango.gallery.ui.navigation.Screen
import org.junit.Assert.*
import org.junit.Test

class CleanerTest {

    @Test
    fun testFormatVideoDuration() {
        assertEquals("00:00", formatVideoDuration(0L))
        assertEquals("00:45", formatVideoDuration(45000L))
        assertEquals("01:30", formatVideoDuration(90000L))
        assertEquals("10:05", formatVideoDuration(605000L))
    }

    @Test
    fun testFormatStorageBytes() {
        assertEquals("500 B", formatStorageBytes(500L))
        assertEquals("12.0 kB", formatStorageBytes(12 * 1024L))
        assertEquals("45.0 MB", formatStorageBytes(45 * 1024 * 1024L))
        assertEquals("1.50 GB", formatStorageBytes((1.5 * 1024 * 1024 * 1024L).toLong()))
    }

    @Test
    fun testFormatVideoSize() {
        assertEquals("500 B", formatVideoSize(500L))
        assertEquals("500 KB", formatVideoSize(500 * 1024L))
        assertEquals("1.0 MB", formatVideoSize(1024 * 1024L))
        assertEquals("70.0 MB", formatVideoSize(70 * 1024 * 1024L))
        assertEquals("2.00 GB", formatVideoSize(2L * 1024 * 1024 * 1024L))
    }

    @Test
    fun testCleanerSelectionToggle() {
        val path1 = "/storage/emulated/0/DCIM/Camera/VID_01.mp4"
        val path2 = "/storage/emulated/0/DCIM/Camera/VID_02.mp4"
        val path3 = "/storage/emulated/0/Pictures/Screenshots/SS_01.png"
        val allPaths = listOf(path1, path2, path3)

        var selected = setOf<String>()
        assertTrue(selected.isEmpty())

        // Toggle single item on
        selected = selected + path1
        assertEquals(1, selected.size)
        assertTrue(path1 in selected)

        // Toggle single item off
        selected = selected - path1
        assertTrue(selected.isEmpty())

        // Select All
        selected = allPaths.toSet()
        assertEquals(3, selected.size)

        // Select all toggle behavior (if all selected -> deselect all)
        val isAllSelected = selected.size == allPaths.size
        val nextSelection = if (isAllSelected) emptySet() else allPaths.toSet()
        assertTrue(nextSelection.isEmpty())
    }

    @Test
    fun testJunkCategorySumCalculation() {
        val categories = listOf(
            JunkCategory("app_cache", "App Cache", "Cache", 50 * 1024 * 1024L, 0, isChecked = true),
            JunkCategory("apk_files", "APK Files", "APK", 20 * 1024 * 1024L, 0, isChecked = false),
            JunkCategory("temp_files", "Temporary Files", "Temp", 30 * 1024 * 1024L, 0, isChecked = true)
        )

        val totalSelected = categories.filter { it.isChecked }.sumOf { it.sizeBytes }
        val expected = (50 + 30) * 1024 * 1024L
        assertEquals(expected, totalSelected)
    }

    @Test
    fun testLargeVideoFiltering() {
        val video1 = MediaItem(
            id = 1L,
            filePath = "/storage/emulated/0/DCIM/Camera/VID_SMALL.mp4",
            fileName = "VID_SMALL.mp4",
            fileSize = 5 * 1024 * 1024L, // 5MB
            folderName = "Camera",
            folderPath = "/storage/emulated/0/DCIM/Camera",
            dateModified = 1730000000L,
            dateTaken = 1730000000L,
            mediaType = MediaType.VIDEO,
            durationMs = 30000L
        )

        val video2 = MediaItem(
            id = 2L,
            filePath = "/storage/emulated/0/DCIM/Camera/VID_LARGE.mp4",
            fileName = "VID_LARGE.mp4",
            fileSize = 75 * 1024 * 1024L, // 75MB
            folderName = "Camera",
            folderPath = "/storage/emulated/0/DCIM/Camera",
            dateModified = 1730000000L,
            dateTaken = 1730000000L,
            mediaType = MediaType.VIDEO,
            durationMs = 120000L
        )

        val list = listOf(video1, video2)
        val minThreshold = 10 * 1024 * 1024L // 10MB
        val largeVideos = list.filter { it.fileSize >= minThreshold }

        assertEquals(1, largeVideos.size)
        assertEquals("VID_LARGE.mp4", largeVideos[0].fileName)
    }

    @Test
    fun testCleanerSuiteNavigationRoutes() {
        assertEquals("cleaner", Screen.Cleaner.route)
        assertEquals("cleaner/large_videos", Screen.LargeVideos.route)
        assertEquals("cleaner/screenshots", Screen.Screenshots.route)
        assertEquals("cleaner/remove_junk", Screen.RemoveJunk.route)
    }
}
