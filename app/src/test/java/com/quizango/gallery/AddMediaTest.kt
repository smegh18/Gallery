package com.quizango.gallery

import com.quizango.gallery.data.repository.copyIntoFolder
import com.quizango.gallery.data.repository.moveIntoFolder
import com.quizango.gallery.data.repository.uniqueTargetFile
import com.quizango.gallery.ui.home.allInFolder
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import java.nio.file.Files

class AddMediaTest {

    @Test
    fun freeTargetNames() { // S272/s9/k.OooOOO
        val dir = Files.createTempDirectory("album").toFile()
        assertEquals(File(dir, "a.jpg"), uniqueTargetFile(dir, "a.jpg"))
        File(dir, "a.jpg").writeText("1")
        assertEquals(File(dir, "a(1).jpg"), uniqueTargetFile(dir, "a.jpg"))
        File(dir, "a(1).jpg").writeText("2")
        assertEquals(File(dir, "a(2).jpg"), uniqueTargetFile(dir, "a.jpg"))
        File(dir, "b.c.jpg").writeText("3")
        assertEquals(File(dir, "b.c(1).jpg"), uniqueTargetFile(dir, "b.c.jpg"))
        File(dir, "noext").writeText("4")
        assertEquals(File(dir, "noext(1)"), uniqueTargetFile(dir, "noext")) // 2.7.2 would loop forever here
        assertEquals(File(dir, "dot.jpg"), uniqueTargetFile(dir, ".dot.jpg")) // 2.7.2 drops a leading dot
        dir.deleteRecursively()
    }

    @Test
    fun copyIntoOwnFolderMakesADuplicate() {
        val dir = Files.createTempDirectory("album").toFile()
        val src = File(dir, "a.jpg").apply { writeText("photo") }
        val copy = copyIntoFolder(src, dir)
        assertEquals(File(dir, "a(1).jpg"), copy)
        assertEquals("photo", src.readText()) // the original is never overwritten
        assertEquals("photo", copy!!.readText())
        dir.deleteRecursively()
    }

    @Test
    fun copyNeverOverwritesADifferentFileOfTheSameName() {
        val from = Files.createTempDirectory("from").toFile()
        val to = Files.createTempDirectory("to").toFile()
        val src = File(from, "a.jpg").apply { writeText("new") }
        File(to, "a.jpg").writeText("old")
        assertEquals(File(to, "a(1).jpg"), copyIntoFolder(src, to))
        assertEquals("old", File(to, "a.jpg").readText())
        from.deleteRecursively(); to.deleteRecursively()
    }

    @Test
    fun moveCopiesThenDeletesTheSource() {
        val from = Files.createTempDirectory("from").toFile()
        val to = Files.createTempDirectory("to").toFile()
        val src = File(from, "a.jpg").apply { writeText("photo") }
        val moved = moveIntoFolder(src, to)
        assertEquals(File(to, "a.jpg"), moved)
        assertFalse(src.exists())
        assertEquals("photo", moved!!.readText())
        val there = File(to, "b.jpg").apply { writeText("x") }
        assertEquals(there, moveIntoFolder(there, to)) // already in that folder: left alone, not renamed
        assertTrue(there.exists())
        from.deleteRecursively(); to.deleteRecursively()
    }

    @Test
    fun moveIntoTheFolderThatHoldsEverything() { // S272/ca/br Oooo0O0
        assertTrue(allInFolder(listOf("/s/DCIM/Camera/a.jpg", "/s/DCIM/Camera/b.jpg"), "/s/DCIM/Camera"))
        assertTrue(allInFolder(listOf("/s/DCIM/Camera/a.jpg"), "/s/DCIM/Camera/"))
        assertFalse(allInFolder(listOf("/s/DCIM/Camera/a.jpg", "/s/Pictures/X/b.jpg"), "/s/DCIM/Camera"))
    }
}
