package com.quizango.gallery

import com.quizango.gallery.data.local.JunkScanner
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import java.nio.file.Files

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
        assertEquals(listOf("a.log"), r[JunkScanner.LOG]!!.files.map { it.name })
        assertEquals(listOf("Empty"), r[JunkScanner.EMPTY_FOLDER]!!.files.map { it.name })
    }

    @Test
    fun cleanDeletesAndSkipsFoldersThatFilledUp() {
        val (root, cache) = tree()
        val r = JunkScanner.scan(root, listOf(cache)).associateBy { it.id }
        val empty = r[JunkScanner.EMPTY_FOLDER]!!.files.single()
        File(empty, "new.txt").writeText("x")                  // no longer empty
        val freed = JunkScanner.clean(r[JunkScanner.APK]!!.files + r[JunkScanner.APP_CACHE]!!.files + empty)
        assertEquals(130L, freed)
        assertFalse(File(root, "Download/app.apk").exists())
        assertTrue(empty.exists())
        assertTrue(File(root, "Android/data/x/other.apk").exists())
    }
}
