package com.quizango.gallery

import com.quizango.gallery.ui.creative.collage.CellEdit
import com.quizango.gallery.ui.creative.collage.CollageBg
import com.quizango.gallery.ui.creative.collage.CollageState
import com.quizango.gallery.ui.creative.collage.usedPaths
import com.quizango.gallery.ui.editor.replaceFile
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Assume.assumeFalse
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.IOException

/** Phase 8 review fixes: working PNGs are replaced only after a complete write, and the collage cache keeps only used photos. */
class EditSafetyTest {

    @get:Rule
    val tmp = TemporaryFolder()

    @Test
    fun failedWriteKeepsThePreviousFileAndLeavesNoTemp() {
        val file = tmp.newFile("image.png").apply { writeText("old") }
        assertFalse(replaceFile(file) { it.write("par".toByteArray()); false })
        assertFalse(replaceFile(file) { it.write("par".toByteArray()); throw IOException("disk full") })
        assertEquals("old", file.readText())
        assertEquals(listOf("image.png"), tmp.root.list()!!.toList())
    }

    @Test
    fun successfulWriteCreatesTheFile() {
        val file = tmp.root.resolve("image.png")
        assertTrue(replaceFile(file) { it.write("new".toByteArray()); true })
        assertEquals("new", file.readText())
        assertEquals(listOf("image.png"), tmp.root.list()!!.toList())
    }

    @Test
    fun successfulWriteReplacesTheFile() {
        // rename over an existing file is the POSIX (Android) behaviour; Windows hosts refuse it
        assumeFalse(System.getProperty("os.name").startsWith("Windows"))
        val file = tmp.newFile("image.png").apply { writeText("old") }
        assertTrue(replaceFile(file) { it.write("new".toByteArray()); true })
        assertEquals("new", file.readText())
    }

    @Test
    fun collageCacheKeepsCellsBackgroundAndRollbackPhotos() {
        val now = CollageState(listOf(CellEdit("a"), CellEdit("c")), background = CollageBg.Image("bg"))
        val before = CollageState(listOf(CellEdit("a"), CellEdit("b")))
        assertEquals(setOf("a", "c", "bg"), usedPaths(now))
        assertEquals(setOf("a", "c", "bg", "b"), usedPaths(now, before, null))
        assertEquals(setOf("a", "b"), usedPaths(before.copy(background = CollageBg.Solid(0))))
    }
}
