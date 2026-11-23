package com.quizango.gallery

import com.quizango.gallery.domain.model.MediaItem
import com.quizango.gallery.domain.model.MediaType
import com.quizango.gallery.ui.cleaner.CleanerEntry
import com.quizango.gallery.ui.cleaner.formatCleanerSize
import com.quizango.gallery.ui.cleaner.formatCleanerSizeSpanned
import com.quizango.gallery.ui.navigation.Screen
import org.junit.Assert.*
import org.junit.Test
import java.util.Locale

class CleanerTest {

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

    @Test
    fun systemCleanerEntriesKeepReferenceOrderAndTargets() {
        val e = CleanerEntry.entries
        assertEquals(
            listOf(R.string.remove_junk, R.string.duplicate_images, R.string.tool_bar_screenshot, R.string.tool_bar_large_video),
            e.map { it.title }
        )
        assertEquals(
            listOf(R.string.txt_cleaner_sub, R.string.txt_sub_duplicate, R.string.txt_sub_screenshot, R.string.txt_sub_large_video),
            e.map { it.subtitle }
        )
        assertEquals(
            listOf(Screen.RemoveJunk, Screen.DuplicateWaiting, Screen.Screenshots, Screen.LargeVideos),
            e.map { it.pick(Screen.RemoveJunk, Screen.DuplicateWaiting, Screen.Screenshots, Screen.LargeVideos) }
        )
    }

    @Test
    fun cleanerSizeMatchesReferenceFormatter() {
        val k = 1024L
        assertEquals("0 B", formatCleanerSize(0))
        assertEquals("0 B", formatCleanerSize(-5))
        assertEquals("1,023 B", formatCleanerSize(1023))
        assertEquals("1 kB", formatCleanerSize(k))
        assertEquals("54.5 kB", formatCleanerSize(55808))
        assertEquals("1.2 kB", formatCleanerSize(1280)) // DecimalFormat HALF_EVEN, one decimal at most
        assertEquals("23.2 MB", formatCleanerSize((23.2 * k * k).toLong()))
        assertEquals("1 GB", formatCleanerSize(k * k * k))
        assertEquals("1.5 GB", formatCleanerSize(3 * k * k * k / 2))
        assertEquals("2,048 TB", formatCleanerSize(2048 * k * k * k * k))
        assertEquals("1,024 TB", formatCleanerSize(k * k * k * k * k))
    }

    @Test
    fun cleanerSizeIgnoresDisplayLocale() {
        val saved = Locale.getDefault()
        try {
            Locale.setDefault(Locale.GERMANY)
            assertEquals("1,023 B", formatCleanerSize(1023))
            assertEquals("23.2 MB", formatCleanerSize((23.2 * 1024 * 1024).toLong()))
        } finally {
            Locale.setDefault(saved)
        }
    }

    @Test
    fun cleanerSizeSpannedHalvesOnlyTheUnit() {
        val s = formatCleanerSizeSpanned(55808)
        assertEquals("54.5 kB", s.text)
        assertEquals(listOf(5 to 7), s.spanStyles.map { it.start to it.end })
    }
}
