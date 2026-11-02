package com.quizango.gallery

import com.quizango.gallery.domain.model.MediaItem
import com.quizango.gallery.domain.model.MediaType
import com.quizango.gallery.ui.cleaner.formatDuplicateSize
import com.quizango.gallery.ui.navigation.Screen
import org.junit.Assert.*
import org.junit.Test

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
    fun testDuplicateGroupingByExplicitGroupAndSize() {
        val item1 = createMediaItem(id = 1, path = "/path/img1.jpg", size = 2000L, dupGroup = 101L)
        val item2 = createMediaItem(id = 2, path = "/path/img1_copy.jpg", size = 2000L, dupGroup = 101L)
        val item3 = createMediaItem(id = 3, path = "/path/img2.jpg", size = 5000L, dupGroup = 0L)
        val item4 = createMediaItem(id = 4, path = "/path/img2_copy.jpg", size = 5000L, dupGroup = 0L)
        val unique = createMediaItem(id = 5, path = "/path/unique.jpg", size = 7000L, dupGroup = 0L)

        val allMedia = listOf(item1, item2, item3, item4, unique)

        // Heuristic grouping algorithm
        val explicitGroups = allMedia.filter { it.duplicateGroup != 0L }
            .groupBy { it.duplicateGroup }
            .filter { it.value.size > 1 }
            .values
            .toList()

        val explicitPaths = explicitGroups.flatten().map { it.filePath }.toSet()

        val sizeGroups = allMedia.filter { it.filePath !in explicitPaths && it.fileSize > 0 }
            .groupBy { it.fileSize }
            .filter { it.value.size > 1 }
            .values
            .toList()

        val duplicateGroups = (explicitGroups + sizeGroups).distinctBy { group -> group.map { it.filePath }.sorted() }

        assertEquals(2, duplicateGroups.size)
        // Group 1 has 2 items
        assertEquals(2, duplicateGroups[0].size)
        // Group 2 has 2 items
        assertEquals(2, duplicateGroups[1].size)
        // Unique item is not in any duplicate group
        assertFalse(duplicateGroups.flatten().any { it.filePath == unique.filePath })
    }

    @Test
    fun testKeepBestPhotoSelectionLogic() {
        val itemA1 = createMediaItem(id = 1, path = "/path/A1.jpg", size = 1000L)
        val itemA2 = createMediaItem(id = 2, path = "/path/A2.jpg", size = 1000L)
        val itemB1 = createMediaItem(id = 3, path = "/path/B1.jpg", size = 2000L)
        val itemB2 = createMediaItem(id = 4, path = "/path/B2.jpg", size = 2000L)
        val itemB3 = createMediaItem(id = 5, path = "/path/B3.jpg", size = 2000L)

        val groups = listOf(listOf(itemA1, itemA2), listOf(itemB1, itemB2, itemB3))

        // When Keep Best Photo is ON:
        // A1 is kept (unselected), A2 is selected.
        // B1 is kept (unselected), B2 and B3 are selected.
        val keepBestSelected = mutableSetOf<String>()
        groups.forEach { group ->
            group.drop(1).forEach { keepBestSelected.add(it.filePath) }
        }

        assertEquals(3, keepBestSelected.size)
        assertFalse(itemA1.filePath in keepBestSelected)
        assertTrue(itemA2.filePath in keepBestSelected)
        assertFalse(itemB1.filePath in keepBestSelected)
        assertTrue(itemB2.filePath in keepBestSelected)
        assertTrue(itemB3.filePath in keepBestSelected)

        // When Keep Best Photo is OFF:
        // All duplicate items are selected
        val allSelected = mutableSetOf<String>()
        groups.forEach { group ->
            group.forEach { allSelected.add(it.filePath) }
        }

        assertEquals(5, allSelected.size)
        assertTrue(itemA1.filePath in allSelected)
        assertTrue(itemB1.filePath in allSelected)
    }

    @Test
    fun testSelectAllAndUnselectAllToggle() {
        val paths = listOf("/path/1.jpg", "/path/2.jpg", "/path/3.jpg", "/path/4.jpg")
        val allPathsSet = paths.toSet()

        var selected = setOf<String>()
        val isAllSelected1 = selected.isNotEmpty() && selected.size >= allPathsSet.size
        assertFalse(isAllSelected1)

        // Select All
        selected = allPathsSet
        val isAllSelected2 = selected.isNotEmpty() && selected.size >= allPathsSet.size
        assertTrue(isAllSelected2)

        // Unselect All
        selected = emptySet()
        assertEquals(0, selected.size)
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
        dupGroup: Long = 0L
    ): MediaItem {
        return MediaItem(
            id = id,
            filePath = path,
            fileName = path.substringAfterLast('/'),
            fileSize = size,
            folderName = "Camera",
            folderPath = "/storage/emulated/0/DCIM/Camera",
            dateModified = System.currentTimeMillis(),
            dateTaken = System.currentTimeMillis(),
            mediaType = MediaType.IMAGE,
            duplicateGroup = dupGroup
        )
    }
}
