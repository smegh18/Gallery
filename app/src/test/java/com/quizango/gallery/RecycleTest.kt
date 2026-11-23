package com.quizango.gallery

import com.quizango.gallery.domain.model.MediaItem
import com.quizango.gallery.domain.model.MediaType
import com.quizango.gallery.data.local.PathMutationOutcome
import com.quizango.gallery.data.local.datastore.autoDeleteTrashValue
import com.quizango.gallery.ui.navigation.Screen
import com.quizango.gallery.ui.recycle.calculateRemainingDays
import com.quizango.gallery.ui.recycle.recycleRetentionThreshold
import com.quizango.gallery.ui.recycle.recycleSelectionAfterOutcomes
import com.quizango.gallery.ui.recycle.shouldShowRecycleDayStrip
import org.junit.Assert.*
import org.junit.Test

class RecycleTest {

    @Test
    fun testCalculateRemainingDays() {
        val now = 1730000000000L
        val oneDayMs = 24 * 60 * 60 * 1000L

        // Just deleted (0 elapsed days)
        assertEquals(30, calculateRemainingDays(deletedDate = now, currentTimeMs = now))

        // 0 or unset deletedDate
        assertEquals(30, calculateRemainingDays(deletedDate = 0L, currentTimeMs = now))

        // 10 days elapsed
        val tenDaysAgo = now - (10 * oneDayMs)
        assertEquals(20, calculateRemainingDays(deletedDate = tenDaysAgo, currentTimeMs = now))

        // 29 days elapsed
        val twentyNineDaysAgo = now - (29 * oneDayMs)
        assertEquals(1, calculateRemainingDays(deletedDate = twentyNineDaysAgo, currentTimeMs = now))

        // 45 days elapsed stays overdue: the reference has no lower clamp.
        val fortyFiveDaysAgo = now - (45 * oneDayMs)
        assertEquals(-15, calculateRemainingDays(deletedDate = fortyFiveDaysAgo, currentTimeMs = now))
    }

    @Test
    fun recycleRetentionUsesExactElapsedCutoffAndOnlyShowsWhileEnabled() {
        val now = 1_730_000_000_000L
        val day = 24 * 60 * 60 * 1000L
        assertEquals(now - 30 * day, recycleRetentionThreshold(now))
        assertFalse(shouldShowRecycleDayStrip(autoDeleteEnabled = false))
        assertTrue(shouldShowRecycleDayStrip(autoDeleteEnabled = true))
    }

    @Test
    fun absentAutoDeletePreferenceDefaultsOffButSavedValuesArePreserved() {
        assertFalse(autoDeleteTrashValue(null))
        assertTrue(autoDeleteTrashValue(true))
        assertFalse(autoDeleteTrashValue(false))
    }

    @Test
    fun recycleActionsKeepFailedPathsSelectedForRetry() {
        val selected = setOf("success", "file-failed", "metadata-failed")
        val outcomes = listOf(
            PathMutationOutcome("success", PathMutationOutcome.Status.SUCCESS),
            PathMutationOutcome("file-failed", PathMutationOutcome.Status.FILE_FAILED),
            PathMutationOutcome("metadata-failed", PathMutationOutcome.Status.METADATA_FAILED)
        )
        assertEquals(setOf("file-failed", "metadata-failed"), recycleSelectionAfterOutcomes(selected, outcomes))
    }

    @Test
    fun testRecycleSelectionToggle() {
        val item1 = "/storage/emulated/0/DCIM/Camera/IMG_01.jpg"
        val item2 = "/storage/emulated/0/DCIM/Camera/IMG_02.jpg"
        val item3 = "/storage/emulated/0/DCIM/Camera/VID_01.mp4"
        val allItems = listOf(item1, item2, item3)

        var selected = setOf<String>()
        assertTrue(selected.isEmpty())

        // Select one
        selected = selected + item1
        assertEquals(1, selected.size)
        assertTrue(item1 in selected)

        // Select All
        selected = allItems.toSet()
        assertEquals(3, selected.size)

        // Select All toggle: if all selected, deselect all
        val nextSelection = if (selected.size == allItems.size) emptySet() else allItems.toSet()
        assertTrue(nextSelection.isEmpty())
    }

    @Test
    fun testRecycleTargetFiltering() {
        val item1 = MediaItem(
            id = 1L,
            filePath = "/storage/emulated/0/DCIM/Camera/photo1.jpg",
            fileName = "photo1.jpg",
            fileSize = 1024L,
            folderName = "Camera",
            folderPath = "/storage/emulated/0/DCIM/Camera",
            dateModified = 1730000000L,
            dateTaken = 1730000000L,
            deletedDate = 1730000000L,
            mediaType = MediaType.IMAGE
        )

        val item2 = MediaItem(
            id = 2L,
            filePath = "/storage/emulated/0/DCIM/Camera/photo2.jpg",
            fileName = "photo2.jpg",
            fileSize = 2048L,
            folderName = "Camera",
            folderPath = "/storage/emulated/0/DCIM/Camera",
            dateModified = 1730000000L,
            dateTaken = 1730000000L,
            deletedDate = 1730000000L,
            mediaType = MediaType.IMAGE
        )

        val items = listOf(item1, item2)
        val selectedPaths = setOf(item1.filePath)

        val targetItems = items.filter { it.filePath in selectedPaths }
        assertEquals(1, targetItems.size)
        assertEquals(item1.filePath, targetItems[0].filePath)
    }

    @Test
    fun testRecycleBinNavigationRoute() {
        assertEquals("recycle", Screen.RecycleBin.route)
    }
}
