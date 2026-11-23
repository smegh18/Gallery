package com.quizango.gallery

import com.quizango.gallery.data.local.JunkScanner
import com.quizango.gallery.ui.cleaner.JUNK_SCAN_HOLD_MS
import com.quizango.gallery.ui.cleaner.junkScanStatus
import org.junit.Assert.assertEquals
import org.junit.Test
import java.io.File
import java.nio.file.Files
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
}
