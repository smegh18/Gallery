package com.quizango.gallery

import com.quizango.gallery.data.local.PathMutationOutcome
import com.quizango.gallery.data.local.PathMutationOutcome.Status
import com.quizango.gallery.data.local.failedPaths
import com.quizango.gallery.data.local.successfulPaths
import com.quizango.gallery.data.repository.deleteFileIfPresent
import com.quizango.gallery.data.repository.deleteFilesThenRows
import com.quizango.gallery.data.repository.updateRows
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.io.File
import java.nio.file.Files

class MediaRepositoryMutationTest {
    private lateinit var root: File
    private val calls = mutableListOf<String>()
    private val rows = mutableSetOf<String>()

    private fun file(name: String) = File(root, name).apply { writeText("x") }.path

    private val loggedFileDelete: (String) -> Boolean = { calls += "file:$it"; deleteFileIfPresent(it) }
    private val loggedRowDelete: suspend (List<String>) -> Unit = { chunk ->
        chunk.forEach { assertFalse("row deleted before its file", File(it).exists()) }
        calls += chunk.map { "row:$it" }
        rows -= chunk.toSet()
    }

    @Before
    fun setUp() {
        root = Files.createTempDirectory("mutationtest").toFile()
    }

    @After
    fun tearDown() {
        root.deleteRecursively()
    }

    @Test
    fun existingFileIsDeletedBeforeItsRow() = runBlocking {
        val a = file("a.jpg").also { rows += it }
        val out = deleteFilesThenRows(listOf(a), loggedFileDelete, loggedRowDelete)
        assertEquals(listOf("file:$a", "row:$a"), calls)
        assertEquals(listOf(PathMutationOutcome(a, Status.SUCCESS)), out)
        assertFalse(File(a).exists())
        assertTrue(rows.isEmpty())
    }

    @Test
    fun failedFileDeleteKeepsTheRow() = runBlocking {
        val a = file("a.jpg").also { rows += it }
        val b = file("b.jpg").also { rows += it }
        val out = deleteFilesThenRows(listOf(a, b), { p -> if (p == a) false else throw SecurityException() }, loggedRowDelete)
        assertEquals(listOf(Status.FILE_FAILED, Status.FILE_FAILED), out.map { it.status })
        assertEquals(listOf(a, b), out.failedPaths())
        assertEquals(setOf(a, b), rows)
        assertTrue(calls.none { it.startsWith("row:") })
    }

    @Test
    fun nonEmptyFolderIsNotTreatedAsGone() {
        val dir = File(root, "d").apply { mkdirs() }
        File(dir, "x.jpg").writeText("x")
        assertFalse(deleteFileIfPresent(dir.path))
    }

    @Test
    fun absentFileStillCleansItsStaleRow() = runBlocking {
        val gone = File(root, "gone.jpg").path.also { rows += it }
        val out = deleteFilesThenRows(listOf(gone), loggedFileDelete, loggedRowDelete).single()
        assertEquals(Status.SUCCESS, out.status)
        assertTrue(rows.isEmpty())
    }

    @Test
    fun rowFailureAfterFileDeleteIsRecoveredByRetry() = runBlocking {
        val a = file("a.jpg").also { rows += it }
        val failed = deleteFilesThenRows(listOf(a), loggedFileDelete) { throw IllegalStateException("db") }.single()
        assertEquals(Status.METADATA_FAILED, failed.status)
        assertFalse(File(a).exists())
        assertEquals(setOf(a), rows) // the row is not claimed as removed
        val retried = deleteFilesThenRows(listOf(a), loggedFileDelete, loggedRowDelete).single()
        assertEquals(Status.SUCCESS, retried.status)
        assertTrue(rows.isEmpty())
    }

    @Test
    fun repeatedPathsAreMutatedOnceInInputOrder() = runBlocking {
        val a = file("a.jpg")
        val b = file("b.jpg")
        val out = deleteFilesThenRows(listOf(b, a, b), loggedFileDelete, loggedRowDelete)
        assertEquals(listOf(b, a), out.map { it.path })
        assertEquals(listOf("file:$b", "file:$a", "row:$b", "row:$a"), calls)
    }

    @Test
    fun singleAndBulkShareOneContract() = runBlocking {
        val a = file("a.jpg")
        val stuck = file("stuck.jpg")
        val out = deleteFilesThenRows(listOf(a, stuck), { it != stuck && deleteFileIfPresent(it) }, loggedRowDelete)
        assertEquals(listOf(a), out.successfulPaths())
        assertEquals(listOf(stuck), out.failedPaths())
        assertEquals(deleteFilesThenRows(listOf(stuck), { false }, loggedRowDelete).single(), out[1])
    }

    @Test
    fun trashAndRestoreReportEachRowAndKeepInputIdentity() = runBlocking {
        val a = file("a.jpg")
        val b = file("b.jpg")
        val out = updateRows(listOf(a, b, a)) {
            if (it == b) throw IllegalStateException("db")
            calls += "row:$it"
        }
        assertEquals(listOf(PathMutationOutcome(a, Status.SUCCESS), PathMutationOutcome(b, Status.METADATA_FAILED)), out)
        assertEquals(listOf(a), out.successfulPaths())
        assertEquals(listOf(b), out.failedPaths())
        assertEquals(listOf("row:$a"), calls)
        assertTrue(File(a).exists() && File(b).exists()) // trash never touches the file
    }
}
