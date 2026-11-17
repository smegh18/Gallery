package com.quizango.gallery

import com.quizango.gallery.ui.home.AlbumNameCheck
import com.quizango.gallery.ui.home.albumNameExists
import com.quizango.gallery.ui.home.checkAlbumName
import com.quizango.gallery.ui.home.filterAlbumNameEdit
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import java.nio.file.Files

class AlbumDialogTest {

    @Test
    fun okButtonChecks() { // S272/bz/e: empty does nothing, 50 or more is refused, the field itself stops at 50
        assertEquals(AlbumNameCheck.EMPTY, checkAlbumName(""))
        assertEquals(AlbumNameCheck.OK, checkAlbumName("a".repeat(49)))
        assertEquals(AlbumNameCheck.TOO_LONG, checkAlbumName("a".repeat(50)))
    }

    @Test
    fun inputFilterRejectsSymbolOnlyInsertions() { // S272/ua/a
        assertEquals("Trip", filterAlbumNameEdit("Tri", "Trip"))
        assertEquals("Trip", filterAlbumNameEdit("Trip", "Trip/")) // a typed "/" would make a sub-folder
        assertEquals("Trip", filterAlbumNameEdit("Trip", "Tr:ip"))
        assertEquals("Trip 1", filterAlbumNameEdit("Trip", "Trip 1"))
        assertEquals("Trip a/b", filterAlbumNameEdit("Trip", "Trip a/b")) // mixed paste passes, as in 2.7.2
        assertEquals("Tri", filterAlbumNameEdit("Trip", "Tri")) // deleting is never filtered
        assertEquals(50, filterAlbumNameEdit("", "x".repeat(60)).length) // LengthFilter(50)
    }

    @Test
    fun existsInEitherRootIgnoringCase() { // S272/s9/g: Pictures and Pictures/Gallery/Organizer
        val pictures = Files.createTempDirectory("pictures").toFile()
        val organizer = File(pictures, "Gallery/Organizer").apply { mkdirs() }
        File(pictures, "Trips").mkdirs()
        File(organizer, "Pets").mkdirs()
        File(pictures, "notes.txt").writeText("x")
        val roots = listOf(pictures, organizer)
        assertTrue(albumNameExists("Trips", roots))
        assertTrue(albumNameExists("trips", roots))
        assertTrue(albumNameExists("PETS", roots))
        assertTrue(albumNameExists("notes.txt", roots)) // any file with that exact name blocks it too
        assertFalse(albumNameExists("Food", roots))
        pictures.deleteRecursively()
    }
}
