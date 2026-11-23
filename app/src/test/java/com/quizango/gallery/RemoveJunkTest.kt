package com.quizango.gallery

import com.quizango.gallery.data.local.JunkScanner
import com.quizango.gallery.ui.cleaner.JUNK_CLEAN_MIN_MS
import com.quizango.gallery.ui.cleaner.JUNK_SCAN_HOLD_MS
import com.quizango.gallery.ui.cleaner.JunkCleanResult
import com.quizango.gallery.ui.cleaner.StorageCleanPhase
import com.quizango.gallery.ui.cleaner.canStartJunkClean
import com.quizango.gallery.ui.cleaner.phaseAfterJunkClean
import com.quizango.gallery.ui.cleaner.initialJunkSelection
import com.quizango.gallery.ui.cleaner.junkBytes
import com.quizango.gallery.ui.cleaner.junkCheckIcon
import com.quizango.gallery.ui.cleaner.junkScanStatus
import com.quizango.gallery.ui.cleaner.runJunkClean
import com.quizango.gallery.ui.cleaner.toggleJunkCategory
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import java.nio.file.Files
import java.util.Calendar
import javax.xml.parsers.DocumentBuilderFactory

class RemoveJunkTest {

    @Test
    fun scanPostsReferenceMilestonesAndHoldsDone() {
        val root = Files.createTempDirectory("storage").toFile()
        File(root, "a.tmp").writeBytes(ByteArray(5))
        val steps = mutableListOf<Int>()
        JunkScanner.scan(root, emptyList(), onProgress = { steps += it })
        assertEquals(listOf(20, 40, 60, 80, 100), steps)
        assertEquals(2500L, JUNK_SCAN_HOLD_MS)
    }

    @Test
    fun doneTextIsTheFixedReferenceString() {
        assertEquals(R.string.scanning, junkScanStatus(false))
        assertEquals(R.string.junk_scan_completed_message, junkScanStatus(true))
        // aapt drops the surrounding quotes and keeps the newline; no format argument, so no measured size
        val doc = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(File("src/main/res/values/strings.xml"))
        val nodes = doc.getElementsByTagName("string")
        val raw = (0 until nodes.length).map { nodes.item(it) }
            .single { it.attributes.getNamedItem("name").nodeValue == "junk_scan_completed_message" }.textContent
        assertEquals("Junk Files Found: 1.40 GB\nScanning Completed!", raw.removeSurrounding("\""))
    }

    /** Temp root with one temp file and two documents older than a year (Log Files). */
    private fun fixture(): Triple<File, Long, List<com.quizango.gallery.data.local.JunkScanResult>> {
        val now = Calendar.getInstance().apply { set(2026, Calendar.JUNE, 15, 12, 0, 0) }.timeInMillis
        val old = now - 400L * 24 * 3600 * 1000
        val root = Files.createTempDirectory("storage").toFile()
        File(root, "a.tmp").writeBytes(ByteArray(7))
        File(root, "old1.pdf").apply { writeBytes(ByteArray(11)); setLastModified(old) }
        File(root, "old2.txt").apply { writeBytes(ByteArray(13)); setLastModified(old) }
        return Triple(root, now, JunkScanner.scan(root, emptyList(), now = now))
    }

    @Test
    fun logFilesStartUntickedAndOtherFoundRowsTicked() {
        val (root, _, results) = fixture()
        val selected = initialJunkSelection(results)
        assertEquals(setOf(File(root, "a.tmp").path), selected)
        val logs = results.single { it.id == JunkScanner.LOG }.files.map { it.path }
        assertEquals(2, logs.size)
        assertEquals(R.drawable.ic_uncheck_new, junkCheckIcon(logs, selected))
        assertEquals(R.drawable.ic_check_new, junkCheckIcon(listOf(File(root, "a.tmp").path), selected))
    }

    @Test
    fun partialChildSelectionIsMixedAndCountsOnlyTickedBytes() {
        val (root, _, results) = fixture()
        val sizes = results.flatMap { it.files }.associate { it.path to it.length() }
        val logs = results.single { it.id == JunkScanner.LOG }.files.map { it.path }
        val pdf = File(root, "old1.pdf").path
        val selected = initialJunkSelection(results) + pdf
        assertEquals(R.drawable.ic_radio_foreground_new, junkCheckIcon(logs, selected))
        assertEquals(7L + 11L, junkBytes(selected, sizes))
        // Mixed -> all, all -> none; other categories untouched
        val all = toggleJunkCategory(logs, selected)
        assertEquals(R.drawable.ic_check_new, junkCheckIcon(logs, all))
        assertEquals(initialJunkSelection(results), toggleJunkCategory(logs, all))
    }

    @Test
    fun cleanDeletesOnlyTheTickedDocumentAndKeepsFailuresTicked() {
        val (root, _, results) = fixture()
        val sizes = results.flatMap { it.files }.associate { it.path to it.length() }
        val pdf = File(root, "old1.pdf").path
        val full = File(root, "Full").apply { mkdirs(); File(this, "x").writeText("x") }.path // a folder that filled up
        val result = runJunkClean(listOf(pdf, full), sizes + (full to 4096L))
        assertEquals(setOf(pdf), result.removed)
        assertEquals(setOf(full), result.failed)
        assertEquals(11L, result.freedBytes)
        assertFalse(File(pdf).exists())
        assertTrue(File(root, "old2.txt").exists())   // unticked document untouched
        assertTrue(File(root, "a.tmp").exists())      // not in the snapshot
    }

    @Test
    fun cleanStartsOnlyFromResultsWithTickedBytes() {
        assertFalse(canStartJunkClean(StorageCleanPhase.OVERVIEW, 0))
        assertTrue(canStartJunkClean(StorageCleanPhase.OVERVIEW, 1))
        // A second tap after the first moved to CLEANING (or later) is ignored
        for (p in listOf(StorageCleanPhase.SCANNING, StorageCleanPhase.CLEANING, StorageCleanPhase.COMPLETED)) {
            assertFalse(canStartJunkClean(p, 1024))
        }
        assertEquals(800L, JUNK_CLEAN_MIN_MS)
    }

    @Test
    fun everyPathFailingReturnsToResultsInsteadOfCleared() {
        assertEquals(StorageCleanPhase.OVERVIEW, phaseAfterJunkClean(JunkCleanResult(emptySet(), setOf("/a"), 0)))
        assertEquals(StorageCleanPhase.COMPLETED, phaseAfterJunkClean(JunkCleanResult(setOf("/b"), setOf("/a"), 5)))
    }
}
