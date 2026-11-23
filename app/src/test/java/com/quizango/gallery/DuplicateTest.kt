package com.quizango.gallery

import com.quizango.gallery.data.local.DuplicateFinder
import com.quizango.gallery.domain.model.MediaItem
import com.quizango.gallery.domain.model.MediaType
import com.quizango.gallery.ui.browse.ListState
import com.quizango.gallery.ui.browse.listStateOf
import com.quizango.gallery.ui.browse.pruneSelection
import com.quizango.gallery.ui.cleaner.DuplicateDeleteRequest
import com.quizango.gallery.ui.cleaner.DuplicateDeleteProcessCoordinator
import com.quizango.gallery.ui.cleaner.deleteSelectedLabel
import com.quizango.gallery.ui.cleaner.duplicateGroupLabel
import com.quizango.gallery.ui.cleaner.formatDuplicateSize
import com.quizango.gallery.ui.cleaner.keepOneSelection
import com.quizango.gallery.ui.cleaner.runDuplicateWaiting
import com.quizango.gallery.ui.cleaner.selectedDuplicates
import com.quizango.gallery.ui.navigation.Screen
import com.quizango.gallery.data.local.PathMutationOutcome
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
    fun nothingSelectedOrOnlyStalePathsMeansPleaseSelect() {
        val groups = listOf(listOf(createMediaItem(1, "/a", 1), createMediaItem(2, "/b", 1)))
        assertNull(selectedDuplicates(groups, emptySet()))
        assertNull(selectedDuplicates(groups, setOf("/gone")))
    }

    @Test
    fun deleteButtonShowsCountAndKeepsTheAdBadge() {
        assertEquals("Delete Selected (19 items)", deleteSelectedLabel("Delete Selected", 19, "items"))
        assertTrue(File("src/main/res/drawable/ic_ads_left.xml").exists())
        assertTrue(File("src/main/java/com/quizango/gallery/ui/cleaner/DuplicatePhotoScreen.kt").readText().contains("R.drawable.ic_ads_left"))
    }

    @Test
    fun confirmationCarriesFrozenPathsAndRecycleChoiceWithoutDeleting() {
        val groups = listOf(
            listOf(createMediaItem(1, "/a1", 1), createMediaItem(2, "/a2", 1)),
            listOf(createMediaItem(3, "/b1", 2), createMediaItem(4, "/b2", 2))
        )
        var selected = setOf("/b2", "/a2")
        val snapshot = selectedDuplicates(groups, selected)!!
        selected = selected + "/a1" // a later change must not reach the confirmed request
        val toBin = DuplicateDeleteRequest(snapshot, moveToRecycleBin = true)
        assertEquals(listOf("/a2", "/b2"), toBin.paths)
        assertTrue(toBin.moveToRecycleBin)
        assertFalse(DuplicateDeleteRequest(snapshot, moveToRecycleBin = false).moveToRecycleBin)
        assertEquals(3, selected.size)
        // The results screen only hands the request on; deleting belongs to the process step
        val screen = File("src/main/java/com/quizango/gallery/ui/cleaner/DuplicatePhotoScreen.kt").readText()
        assertFalse(screen.contains("moveToTrash") || screen.contains("deletePermanently"))
    }

    @Test
    fun confirmedRequestCopiesItsPathsBeforeTheProcessStarts() {
        val original = mutableListOf(createMediaItem(1, "/first.jpg", 1))
        val request = DuplicateDeleteRequest(original, moveToRecycleBin = true)
        original += createMediaItem(2, "/later.jpg", 1)

        assertEquals(listOf("/first.jpg"), request.paths)
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

    @Test
    fun deleteProcessUsesSuccessfulOutcomesOnlyAndLeavesFailuresForRetry() = runBlocking {
        val first = createMediaItem(1, "/first.jpg", 100)
        val failed = createMediaItem(2, "/failed.jpg", 200)
        val request = DuplicateDeleteRequest(listOf(first, failed), moveToRecycleBin = true)
        var calls = 0
        val waits = mutableListOf<Long>()
        var now = 10L
        val coordinator = DuplicateDeleteProcessCoordinator(request, nowMillis = { now })

        val result = coordinator.run(
            mutate = {
                calls += 1
                now = 1_010L
                listOf(
                    PathMutationOutcome("/first.jpg", PathMutationOutcome.Status.SUCCESS),
                    PathMutationOutcome("/failed.jpg", PathMutationOutcome.Status.METADATA_FAILED)
                )
            },
            wait = { waits += it }
        )
        val repeated = coordinator.run(mutate = { error("must not mutate twice") }, wait = { error("must not wait twice") })

        assertEquals(1, calls)
        assertEquals(listOf(2_000L), waits)
        assertEquals(listOf(first), result.successfulItems)
        assertEquals(listOf(failed), result.failedItems)
        assertEquals(100L, result.freedBytes)
        assertEquals(result, repeated)
    }

    @Test
    fun deleteProcessTreatsMissingOrFailedOutcomesAsUnsuccessful() = runBlocking {
        val first = createMediaItem(1, "/first.jpg", 100)
        val missing = createMediaItem(2, "/missing.jpg", 200)
        val result = DuplicateDeleteProcessCoordinator(
            DuplicateDeleteRequest(listOf(first, missing), moveToRecycleBin = false),
            nowMillis = { 0L }
        ).run(
            mutate = { listOf(PathMutationOutcome("/first.jpg", PathMutationOutcome.Status.FILE_FAILED)) },
            wait = {}
        )

        assertTrue(result.successfulItems.isEmpty())
        assertEquals(listOf(first, missing), result.failedItems)
        assertEquals(0L, result.freedBytes)
    }

    @Test
    fun cleanupCompleteUsesReferencePluralAndFormattedFreedCopy() {
        val screen = File("src/main/java/com/quizango/gallery/ui/cleaner/DuplicateCompleteScreen.kt").readText()
        val plurals = File("src/main/res/values/plurals.xml").readText()

        assertTrue(plurals.contains("<item quantity=\"one\">%d file removed</item>"))
        assertTrue(plurals.contains("<item quantity=\"other\">%d files removed</item>"))
        assertTrue(screen.contains("getQuantityString(R.plurals.dup_delete_files_removed"))
        assertTrue(screen.contains("stringResource(R.string.dup_delete_space_freed"))
        assertFalse(screen.contains("formatDuplicateSize(freedSizeBytes)} freed"))
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
