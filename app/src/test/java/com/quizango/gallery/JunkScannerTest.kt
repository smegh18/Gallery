package com.quizango.gallery

import com.quizango.gallery.data.local.JunkScanner
import com.quizango.gallery.data.local.PathMutationOutcome
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import java.nio.file.Files
import java.util.Calendar

class JunkScannerTest {

    private fun tree(): Pair<File, File> {
        val root = Files.createTempDirectory("storage").toFile()
        File(root, "Download").mkdirs()                       // protected, empty
        File(root, "Download/app.apk").writeBytes(ByteArray(100))
        File(root, "Android/data/x").mkdirs()
        File(root, "Android/data/x/other.apk").writeBytes(ByteArray(50)) // other app data: skipped
        File(root, "Notes/a.log").apply { parentFile!!.mkdirs(); writeBytes(ByteArray(10)) }
        File(root, "Notes/b.tmp").writeBytes(ByteArray(20))
        File(root, "Old/Empty").mkdirs()                       // real empty folder
        File(root, "DCIM").mkdirs()                            // protected, empty
        val cache = Files.createTempDirectory("cache").toFile()
        File(cache, "c.bin").writeBytes(ByteArray(30))
        return root to cache
    }

    @Test
    fun scanReportsOnlyRealFiles() {
        val (root, cache) = tree()
        val r = JunkScanner.scan(root, listOf(cache)).associateBy { it.id }
        assertEquals(30L, r[JunkScanner.APP_CACHE]!!.sizeBytes)
        assertEquals(listOf("app.apk"), r[JunkScanner.APK]!!.files.map { it.name })
        assertEquals(listOf("b.tmp"), r[JunkScanner.TEMP]!!.files.map { it.name })
        assertTrue(r[JunkScanner.LOG]!!.files.isEmpty())           // .log is not an old document
        assertEquals(listOf("Empty"), r[JunkScanner.EMPTY_FOLDER]!!.files.map { it.name })
    }

    @Test
    fun tempFilesAcceptOnlyLowercaseTmpExtension() {
        val root = Files.createTempDirectory("storage").toFile()
        File(root, "a.tmp").writeBytes(ByteArray(7))
        listOf("a.temp", "b.TMP", "c.Tmp", "tmp", "d.").forEach { File(root, it).writeBytes(ByteArray(3)) }
        File(root, "dir.tmp/inner").mkdirs()                    // directory named .tmp: never a temp file
        File(root, "Android/data/x").mkdirs()
        File(root, "Android/data/x/e.tmp").writeBytes(ByteArray(3)) // other app data: skipped
        val temp = JunkScanner.scan(root, emptyList()).single { it.id == JunkScanner.TEMP }
        assertEquals(listOf(File(root, "a.tmp")), temp.files)
        assertEquals(7L, temp.sizeBytes)
    }

    @Test
    fun missingRootsGiveStableEmptyCategories() {
        val ids = listOf(JunkScanner.APP_CACHE, JunkScanner.APK, JunkScanner.TEMP, JunkScanner.LOG, JunkScanner.EMPTY_FOLDER)
        val gone = File(Files.createTempDirectory("gone").toFile(), "missing")
        val aFile = Files.createTempFile("root", ".tmp").toFile()
        for (r in listOf(JunkScanner.scan(null, emptyList()), JunkScanner.scan(gone, listOf(gone)), JunkScanner.scan(aFile, emptyList()))) {
            assertEquals(ids, r.map { it.id })
            assertTrue(r.all { it.files.isEmpty() })
        }
    }

    @Test
    fun logFilesHoldOnlyDocumentsOlderThanOneYear() {
        val now = Calendar.getInstance().apply { set(2026, Calendar.JUNE, 15, 12, 0, 0); set(Calendar.MILLISECOND, 0) }.timeInMillis
        val cutoff = Calendar.getInstance().apply { timeInMillis = now; add(Calendar.YEAR, -1) }.timeInMillis
        val root = Files.createTempDirectory("storage").toFile()
        fun doc(name: String, modified: Long) = File(root, name).apply { writeBytes(ByteArray(4)); setLastModified(modified) }
        val old = listOf("doc", "docx", "pdf", "txt", "csv", "xls", "xlsx", "ppt", "pptx", "xml", "PDF")
            .mapIndexed { i, ext -> doc("d$i.$ext", cutoff - 1000) }
        doc("new.pdf", cutoff + 1000)
        doc("edge.pdf", cutoff)                                  // not strictly older
        doc("old.log", cutoff - 1000)
        doc("old.jpg", cutoff - 1000)
        val logs = JunkScanner.scan(root, emptyList(), now = now).single { it.id == JunkScanner.LOG }
        assertEquals(old.toSet(), logs.files.toSet())
    }

    @Test
    fun categoriesKeepReferenceOrderAndMatchOnlyTheirOwnFiles() {
        val (root, cache) = tree()
        File(root, "Download/UP.APK").writeBytes(ByteArray(9))   // dr/c: exact lowercase "apk"
        val r = JunkScanner.scan(root, listOf(cache, cache))
        assertEquals(
            listOf(JunkScanner.APP_CACHE, JunkScanner.APK, JunkScanner.TEMP, JunkScanner.LOG, JunkScanner.EMPTY_FOLDER),
            r.map { it.id }
        )
        val all = r.flatMap { it.files }
        assertEquals(all.size, all.toSet().size)
        assertEquals(listOf("c.bin"), r[0].files.map { it.name })
        assertEquals(listOf("app.apk"), r[1].files.map { it.name })
    }

    @Test
    fun appCacheCountsOnlyNonEmptyTopLevelFiles() {
        val (root, cache) = tree()
        File(cache, "empty.bin").createNewFile()
        File(cache, "image_cache").mkdirs()
        File(cache, "image_cache/nested.bin").writeBytes(ByteArray(5))
        val r = JunkScanner.scan(root, listOf(cache)).associateBy { it.id }
        assertEquals(listOf("c.bin"), r[JunkScanner.APP_CACHE]!!.files.map { it.name })
    }

    @Test
    fun cleanReportsPerPathOutcomesAndSkipsFoldersThatFilledUp() {
        val (root, cache) = tree()
        val r = JunkScanner.scan(root, listOf(cache)).associateBy { it.id }
        val empty = r[JunkScanner.EMPTY_FOLDER]!!.files.single()
        File(empty, "new.txt").writeText("x")                  // no longer empty
        val apk = File(root, "Download/app.apk").path
        val gone = File(root, "never-there.tmp").path           // already absent: counts as done
        val out = JunkScanner.clean(listOf(apk, empty.path, gone, apk))
        assertEquals(listOf(apk, empty.path, gone), out.map { it.path })
        assertEquals(
            listOf(PathMutationOutcome.Status.SUCCESS, PathMutationOutcome.Status.FILE_FAILED, PathMutationOutcome.Status.SUCCESS),
            out.map { it.status }
        )
        assertFalse(File(apk).exists())
        assertTrue(empty.exists())
        assertTrue(File(root, "Android/data/x/other.apk").exists())
    }
}
