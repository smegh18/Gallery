package com.quizango.gallery

import com.quizango.gallery.data.local.VaultCrypto
import com.quizango.gallery.data.local.VaultSecret
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.File
import java.nio.file.Files
import javax.crypto.KeyGenerator

class VaultCryptoTest {
    private val jvm = "AES/CBC/PKCS5Padding"
    private val key = KeyGenerator.getInstance("AES").apply { init(256) }.generateKey()

    @Test
    fun encryptDecryptRoundTripAndHidesContent() {
        val plain = ByteArray(200_001) { (it % 251).toByte() }
        val enc = ByteArrayOutputStream().also { VaultCrypto.encrypt(ByteArrayInputStream(plain), it, key, jvm) }.toByteArray()
        val f: File = Files.createTempFile("v", ".bin").toFile().apply { writeBytes(enc) }
        assertTrue(VaultCrypto.isEncrypted(f))
        assertFalse(String(enc, Charsets.ISO_8859_1).contains(String(plain.copyOf(64), Charsets.ISO_8859_1)))
        val dec = ByteArrayOutputStream().also { VaultCrypto.decrypt(ByteArrayInputStream(enc), it, key, jvm) }.toByteArray()
        assertArrayEquals(plain, dec)
    }

    @Test
    fun plainLegacyFileIsNotEncrypted() {
        val f = Files.createTempFile("p", ".jpg").toFile().apply { writeBytes(ByteArray(100) { 7 }) }
        assertFalse(VaultCrypto.isEncrypted(f))
    }

    @Test
    fun secretHashing() {
        val h = VaultSecret.hash("1234")
        assertTrue(VaultSecret.isHashed(h))
        assertFalse(h.contains("1234"))
        assertTrue(VaultSecret.matches(h, "1234"))
        assertFalse(VaultSecret.matches(h, "1235"))
        assertTrue(VaultSecret.matches("4321", "4321"))   // legacy plain PIN
        assertFalse(VaultSecret.matches(null, ""))          // nothing saved never matches
        assertFalse(VaultSecret.matches("", ""))
        val a = VaultSecret.hash(VaultSecret.normalizeAnswer("  Fluffy "))
        assertTrue(VaultSecret.matches(a, VaultSecret.normalizeAnswer("fluffy")))
    }
}
