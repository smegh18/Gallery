package com.quizango.gallery

import com.quizango.gallery.data.local.VaultCrypto
import com.quizango.gallery.data.repository.hideFileSafely
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.ByteArrayOutputStream
import java.io.File
import java.nio.file.Files
import javax.crypto.KeyGenerator

class VaultFilesTest {
    private val jvm = "AES/CBC/PKCS5Padding"
    private val key = KeyGenerator.getInstance("AES").apply { init(256) }.generateKey()
    private val dir: File = Files.createTempDirectory("vaultfiles").toFile()
    private val plain = ByteArray(5_000) { (it % 253).toByte() }

    private fun source(name: String = "photo.jpg") = File(dir, "src").apply { mkdirs() }.let { File(it, name).apply { writeBytes(plain) } }
    private fun vaultTarget() = File(dir, "vault").apply { mkdirs() }.let { File(it, "uuid_photo.jpg") }
    private val encrypt: (File, File) -> Unit = { s, t -> s.inputStream().use { i -> t.outputStream().use { VaultCrypto.encrypt(i, it, key, jvm) } } }
    private fun decrypt(f: File) = ByteArrayOutputStream().also { o -> f.inputStream().use { VaultCrypto.decrypt(it, o, key, jvm) } }.toByteArray()

    // ---- hide ----

    @Test
    fun hideInsertsRowBeforeDeletingSourceAndKeepsDecryptableCopy() = runBlocking {
        val src = source(); val target = vaultTarget(); val calls = mutableListOf<String>()
        val ok = hideFileSafely(src, target, encrypt,
            insertRow = { calls += "insert"; assertTrue(src.exists()); assertTrue(target.exists()) },
            deleteRow = { calls += "deleteRow" },
            deleteSource = { calls += "deleteSource"; it.delete() })
        assertTrue(ok)
        assertEquals(listOf("insert", "deleteSource"), calls)
        assertFalse(src.exists())
        assertArrayEquals(plain, decrypt(target))
    }

    @Test
    fun hideRowInsertFailureKeepsSourceAndDropsCiphertext() = runBlocking {
        val src = source(); val target = vaultTarget()
        val ok = hideFileSafely(src, target, encrypt, insertRow = { error("db") }, deleteRow = {})
        assertFalse(ok)
        assertTrue(src.exists()); assertFalse(target.exists())
    }

    @Test
    fun hideSourceDeleteFailureRemovesRowAndCiphertext() = runBlocking {
        val src = source(); val target = vaultTarget(); var rowDeleted = false
        val ok = hideFileSafely(src, target, encrypt, insertRow = {}, deleteRow = { rowDeleted = true }, deleteSource = { false })
        assertFalse(ok)
        assertTrue(rowDeleted)
        assertTrue(src.exists()); assertFalse(target.exists())
        assertArrayEquals(plain, src.readBytes())
    }

    @Test
    fun hideKeepsCiphertextIfRowCannotBeRemovedAfterSourceSurvives() = runBlocking {
        val src = source(); val target = vaultTarget()
        val ok = hideFileSafely(src, target, encrypt, insertRow = {}, deleteRow = { error("db") }, deleteSource = { false })
        assertFalse(ok)
        assertTrue(src.exists()); assertTrue(target.exists()) // row still valid, nothing lost
    }

    @Test
    fun hideEncryptionFailureKeepsSourceAndLeavesNoCiphertext() = runBlocking {
        val src = source(); val target = vaultTarget(); var inserted = false
        val failing: (File, File) -> Unit = { _, t -> t.writeBytes(byteArrayOf(1)); error("boom") }
        val ok = hideFileSafely(src, target, failing, insertRow = { inserted = true }, deleteRow = {})
        assertFalse(ok)
        assertFalse(inserted)
        assertTrue(src.exists()); assertFalse(target.exists())
    }
}
