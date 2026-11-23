package com.quizango.gallery

import com.quizango.gallery.data.local.DuplicateFinder
import com.quizango.gallery.domain.model.MediaItem
import com.quizango.gallery.domain.model.MediaType
import com.quizango.gallery.ui.browse.ListState
import com.quizango.gallery.ui.browse.listStateOf
import com.quizango.gallery.ui.browse.pruneSelection
import com.quizango.gallery.ui.cleaner.duplicateGroupLabel
import com.quizango.gallery.ui.cleaner.formatDuplicateSize
import com.quizango.gallery.ui.cleaner.keepOneSelection
import com.quizango.gallery.ui.cleaner.runDuplicateWaiting
import com.quizango.gallery.ui.navigation.Screen
import org.junit.Assert.*
import kotlinx.coroutines.runBlocking
import org.junit.Test
import java.io.File

class DuplicateTest {

    @Test
    fun testFormatDuplicateSize() {
        assertEquals("500 B", formatDuplicateSize(500L))
        assertEquals("500 KB", formatDuplicateSize(500 * 1024L))
        assertEquals("1.0 MB", formatDuplicateSize(1024 * 1024L))
        assertEquals("15.5 MB", formatDuplicateSize((15.5 * 1024 * 1024L).toLong()))
        assertEquals("1.50 GB", formatDuplicateSize((1.5 * 1024 * 1024 * 1024L).toLong()))
    }

    @Test
    fun groupLabelsAreOneBased() {
        assertEquals("Group 1 (2 Images)", duplicateGroupLabel("Group", 0, 2, "Images"))
        assertEquals("Group 4 (3 Images)", duplicateGroupLabel("Group", 3, 3, "Images"))
    }

    @Test
    fun keepOneSelectsEverythingButTheFirstOfEachGroup() {
        val a = listOf(createMediaItem(1, "/A1.jpg", 1), createMediaItem(2, "/A2.jpg", 1))
        val b = listOf(createMediaItem(3, "/B1.jpg", 2), createMediaItem(4, "/B2.jpg", 2), createMediaItem(5, "/B3.jpg", 2))
        assertEquals(setOf("/A2.jpg", "/B2.jpg", "/B3.jpg"), keepOneSelection(listOf(a, b)))
        assertEquals(emptySet<String>(), keepOneSelection(emptyList()))
    }

    @Test
    fun pathKeysStayUniqueAndSelectionSurvivesAFlowUpdate() {
        val items = (1L..5L).map { createMediaItem(it, "/p$it.jpg", it, date = it) }
        val prints = mapOf("/p1.jpg" to "x", "/p2.jpg" to "x", "/p3.jpg" to "y", "/p4.jpg" to "y", "/p5.jpg" to "y")
        val before = DuplicateFinder.group(items, prints)
        val paths = before.flatten().map { it.filePath }
        assertEquals(paths.size, paths.toSet().size)
        val selected = keepOneSelection(before)
        // p5 is deleted elsewhere: its group shrinks, the other selections stay
        val after = DuplicateFinder.group(items.filter { it.filePath != "/p5.jpg" }, prints)
        val present = after.flatten().map { it.filePath }.toSet()
        assertEquals(selected - "/p5.jpg", pruneSelection(selected, present))
    }

    @Test
    fun loadingEmptyAndGroupedStatesStayDistinct() {
        assertEquals(ListState.LOADING, listStateOf(null, 4))
        assertEquals(ListState.EMPTY, listStateOf(emptyList<List<MediaItem>>(), 4))
        assertEquals(ListState.CONTENT, listStateOf(listOf(listOf(createMediaItem(1, "/a", 1))), 4))
    }

    @Test
    fun waitingScansOnceThenDoneThenOpensResultsWhenSuccessEnds() = runBlocking {
        val log = mutableListOf<String>()
        runDuplicateWaiting(
            scan = { log += "scan" },
            onDone = { log += "done" },
            playSuccess = { log += "success" },
            onFinished = { log += "results" }
        )
        assertEquals(listOf("scan", "done", "success", "results"), log)
    }

    @Test
    fun waitingHasNoTimerAndNoSeparateCompleteScreen() {
        val waiting = File("src/main/java/com/quizango/gallery/ui/cleaner/DuplicateWaitingScreen.kt").readText()
        assertFalse(waiting.contains("delay("))
        val screens = File("src/main/java/com/quizango/gallery/ui/navigation/Screen.kt").readText()
        assertFalse(screens.contains("DuplicateScanningComplete"))
    }

    @Test
    fun testDuplicateCompleteRoute() {
        val route = Screen.DuplicateComplete.createRoute(5, 10485760L)
        assertEquals("duplicates/complete?count=5&size=10485760", route)
    }

    private fun createMediaItem(
        id: Long,
        path: String,
        size: Long,
        dupGroup: Long = 0L,
        date: Long = System.currentTimeMillis()
    ): MediaItem {
        return MediaItem(
            id = id,
            filePath = path,
            fileName = path.substringAfterLast('/'),
            fileSize = size,
            folderName = "Camera",
            folderPath = "/storage/emulated/0/DCIM/Camera",
            dateModified = date,
            dateTaken = date,
            mediaType = MediaType.IMAGE,
            duplicateGroup = dupGroup
        )
    }
}
