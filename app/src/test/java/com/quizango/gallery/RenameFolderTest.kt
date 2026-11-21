package com.quizango.gallery

import com.quizango.gallery.data.local.database.dao.GalleryDao
import com.quizango.gallery.data.local.database.entity.MediaEntity
import com.quizango.gallery.data.repository.renameFolderRows
import kotlinx.coroutines.flow.Flow

import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.io.File
import java.nio.file.Files

/** In-memory rows keyed by filePath; only the members renameFolderRows calls are real. */
private class FakeDao(rows: List<MediaEntity>) : GalleryDao {
    val rows = rows.associateBy { it.filePath }.toMutableMap()
    override suspend fun getAllMedia() = rows.values.toList()
    override suspend fun insertAll(media: List<MediaEntity>) { media.forEach { rows[it.filePath] = it } }
    override suspend fun deleteByPaths(paths: List<String>) { paths.forEach { rows.remove(it) } }

    override fun getAllActiveMediaFlow(): Flow<List<MediaEntity>> = flowOf(emptyList())
    override suspend fun getAllActiveMedia(): List<MediaEntity> = TODO()
    override fun getMediaByFolderFlow(folderPath: String): Flow<List<MediaEntity>> = flowOf(emptyList())
    override fun getFavoritesFlow(): Flow<List<MediaEntity>> = flowOf(emptyList())
    override fun getRecycleBinFlow(): Flow<List<MediaEntity>> = flowOf(emptyList())
    override suspend fun getMediaByPath(path: String): MediaEntity? = TODO()
    override fun getVideosFlow(): Flow<List<MediaEntity>> = flowOf(emptyList())
    override fun getScreenshotsFlow(): Flow<List<MediaEntity>> = flowOf(emptyList())
    override fun getDuplicatesFlow(): Flow<List<MediaEntity>> = flowOf(emptyList())
    override fun getGeotaggedMediaFlow(): Flow<List<MediaEntity>> = flowOf(emptyList())
    override suspend fun insert(media: MediaEntity) = TODO()
    override suspend fun update(media: MediaEntity) = TODO()
    override suspend fun delete(media: MediaEntity) = TODO()
    override suspend fun deleteByPath(path: String) = TODO()
    override suspend fun moveToTrash(path: String, timestamp: Long) = TODO()
    override suspend fun restoreFromTrash(path: String) = TODO()
    override suspend fun setFavorite(path: String, favorite: Boolean) = TODO()
    override suspend fun purgeExpiredTrash(thresholdTimestamp: Long) = TODO()
}

class RenameFolderTest {
    private lateinit var root: File
    private lateinit var trip: File
    private lateinit var rows: List<MediaEntity>
    private var transactions = 0
    private val inTransaction: suspend (suspend () -> Unit) -> Unit = { block -> transactions++; block() }

    private fun row(file: File, folder: File, rel: String, favourite: Boolean = false, deleted: Long = 0L) = MediaEntity(
        filePath = file.path.replace('\\', '/'), id = file.name.hashCode().toLong(), fileName = file.name,
        folderName = folder.name, folderPath = folder.path.replace('\\', '/'),
        relativePath = rel, isFavorite = favourite, deletedDate = deleted,
        duplicateHash = "h-" + file.name, place = "Paris", colorList = listOf(1, 2)
    )

    @Before
    fun setUp() {
        root = Files.createTempDirectory("renametest").toFile()
        trip = File(root, "Trip").apply { mkdirs() }
        val sub = File(trip, "sub").apply { mkdirs() }
        val sibling = File(root, "Trip2").apply { mkdirs() }
        rows = listOf(
            row(File(trip, "a.jpg"), trip, "Pictures/Trip/", favourite = true),
            row(File(trip, "bin.jpg"), trip, "Pictures/Trip/", deleted = 123L),
            row(File(sub, "b.jpg"), sub, "Pictures/Trip/sub/"),
            row(File(sibling, "c.jpg"), sibling, "Pictures/Trip2/")
        )
        rows.forEach { File(it.filePath).writeText("x") }
    }

    @After
    fun tearDown() {
        root.deleteRecursively()
    }

    private fun rename(dao: FakeDao, name: String, renameDir: (File, File) -> Boolean = { f, t -> f.renameTo(t) }, folder: File = trip) =
        runBlocking { renameFolderRows(dao, folder, name, inTransaction, renameDir) }

    private fun assertUntouched(dao: FakeDao) {
        assertTrue(trip.isDirectory)
        assertEquals(rows.associateBy { it.filePath }, dao.rows)
        assertEquals(0, transactions)
    }

    @Test
    fun aSiblingWithTheTargetNameIgnoringCaseRefusesAndChangesNothing() {
        File(root, "TRIPS").mkdirs()
        val dao = FakeDao(rows)
        assertNull(rename(dao, "trips"))
        assertUntouched(dao)
    }

    @Test
    fun anUnchangedOrCaseOnlyNameIsRefused() {
        val dao = FakeDao(rows)
        assertNull(rename(dao, "Trip"))
        assertNull(rename(dao, "trip"))
        assertUntouched(dao)
    }

    @Test
    fun aMissingSourceFolderRefusesAndChangesNothing() {
        val dao = FakeDao(rows)
        assertNull(rename(dao, "Trips", folder = File(root, "Gone")))
        assertUntouched(dao)
    }

    @Test
    fun aFailedDirectoryRenameLeavesFolderAndRowsAsTheyWere() {
        val dao = FakeDao(rows)
        assertNull(rename(dao, "Trips", renameDir = { _, _ -> false }))
        assertUntouched(dao)
    }

    @Test
    fun aFailingTransactionPutsTheFolderBack() {
        val dao = FakeDao(rows)
        val failed = runCatching {
            runBlocking { renameFolderRows(dao, trip, "Trips", { throw IllegalStateException("db") }) }
        }
        assertTrue(failed.isFailure)
        assertTrue(trip.isDirectory)
        assertFalse(File(root, "Trips").exists())
        assertEquals(rows.associateBy { it.filePath }, dao.rows)
    }

    @Test
    fun successMovesTheWholeTreeWithEveryFlagKept() {
        val dao = FakeDao(rows)
        val result = rename(dao, "Trips")
        assertNotNull(result)
        val newDir = File(root, "Trips")
        assertTrue(newDir.isDirectory)
        assertFalse(trip.exists())
        assertEquals(1, transactions)

        val base = root.path.replace('\\', '/')
        val a = dao.rows["$base/Trips/a.jpg"]!!
        val bin = dao.rows["$base/Trips/bin.jpg"]!!
        val b = dao.rows["$base/Trips/sub/b.jpg"]!!
        assertTrue(a.isFavorite)
        assertEquals(123L, bin.deletedDate)
        listOf(a to rows[0], bin to rows[1], b to rows[2]).forEach { (moved, old) ->
            assertEquals(old.copy(filePath = moved.filePath, folderPath = moved.folderPath, folderName = moved.folderName, relativePath = moved.relativePath), moved)
        }
        // folderName changes only for rows directly in the renamed folder
        assertEquals("Trips", a.folderName)
        assertEquals("$base/Trips", a.folderPath)
        assertEquals("sub", b.folderName)
        assertEquals("$base/Trips/sub", b.folderPath)
        assertEquals("Pictures/Trips/", a.relativePath)
        assertEquals("Pictures/Trips/sub/", b.relativePath)
        // nothing left under the old paths; the prefix sibling is untouched
        assertTrue(dao.rows.keys.none { it.startsWith("$base/Trip/") })
        assertEquals(rows[3], dao.rows["$base/Trip2/c.jpg"])
        assertEquals(4, dao.rows.size)
        // the returned lists are what the MediaStore scan needs
        assertEquals(3, result!!.first.size)
        assertEquals(3, result.second.size)
        assertTrue(result.second.all { it.startsWith("$base/Trips/") })
    }

    @Test
    fun aSyncRunningRightAfterTheRenameDoesNotLoseFlags() {
        val dao = FakeDao(rows)
        // Simulates syncMediaStore between renameTo and the transaction: old rows dropped (files gone), new rows default
        val result = rename(dao, "Holiday", renameDir = { from, to ->
            from.renameTo(to).also { ok ->
                if (ok) rows.filter { it.filePath.startsWith(trip.path.replace('\\', '/') + "/") }.forEach { old ->
                    dao.rows.remove(old.filePath)
                    val path = old.filePath.replace("/Trip/", "/Holiday/")
                    dao.rows[path] = old.copy(filePath = path, isFavorite = false, deletedDate = 0L)
                }
            }
        })
        assertTrue(result != null)
        val fav = dao.rows.values.single { it.fileName == "a.jpg" }
        val bin = dao.rows.values.single { it.fileName == "bin.jpg" }
        assertTrue(fav.isFavorite)
        assertEquals(123L, bin.deletedDate)
    }
}
