package com.quizango.gallery.data.local

import java.io.File
import java.io.InputStream
import java.io.OutputStream
import java.security.KeyStore
import java.security.MessageDigest
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.CipherInputStream
import javax.crypto.CipherOutputStream
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.IvParameterSpec

/**
 * AES-256-CBC file encryption for the Private Vault.
 * File layout: "QGV1" magic, 16-byte IV, ciphertext.
 * CBC (not GCM) because Keystore GCM decryption buffers the whole file, which fails on large videos.
 */
object VaultCrypto {
    private val MAGIC = "QGV1".toByteArray()
    private const val KEY_ALIAS = "gallery_vault_key"

    /** Android Keystore transformation; JVM tests pass "AES/CBC/PKCS5Padding" (same padding, other name). */
    const val ANDROID_TRANSFORMATION = "AES/CBC/PKCS7Padding"

    /** Key lives in the Android Keystore and never leaves it. */
    fun keystoreKey(): SecretKey {
        val ks = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
        (ks.getKey(KEY_ALIAS, null) as? SecretKey)?.let { return it }
        val gen = KeyGenerator.getInstance("AES", "AndroidKeyStore")
        gen.init(
            android.security.keystore.KeyGenParameterSpec.Builder(
                KEY_ALIAS,
                android.security.keystore.KeyProperties.PURPOSE_ENCRYPT or
                    android.security.keystore.KeyProperties.PURPOSE_DECRYPT
            )
                .setKeySize(256)
                .setBlockModes(android.security.keystore.KeyProperties.BLOCK_MODE_CBC)
                .setEncryptionPaddings(android.security.keystore.KeyProperties.ENCRYPTION_PADDING_PKCS7)
                .build()
        )
        return gen.generateKey()
    }

    fun encrypt(input: InputStream, output: OutputStream, key: SecretKey, transformation: String = ANDROID_TRANSFORMATION) {
        val cipher = Cipher.getInstance(transformation)
        cipher.init(Cipher.ENCRYPT_MODE, key) // provider picks a random IV
        output.write(MAGIC)
        output.write(cipher.iv)
        CipherOutputStream(output, cipher).use { input.copyTo(it) }
    }

    /** True for files written by [encrypt]; older vault files are plain copies. */
    fun isEncrypted(file: File): Boolean {
        if (file.length() < MAGIC.size + 16) return false
        val head = ByteArray(MAGIC.size)
        file.inputStream().use { it.read(head) }
        return head.contentEquals(MAGIC)
    }

    fun decrypt(input: InputStream, output: OutputStream, key: SecretKey, transformation: String = ANDROID_TRANSFORMATION) {
        val head = ByteArray(MAGIC.size).also { readFully(input, it) }
        require(head.contentEquals(MAGIC)) { "Not a vault file" }
        val iv = ByteArray(16).also { readFully(input, it) }
        val cipher = Cipher.getInstance(transformation)
        cipher.init(Cipher.DECRYPT_MODE, key, IvParameterSpec(iv))
        CipherInputStream(input, cipher).use { it.copyTo(output) }
    }

    private fun readFully(input: InputStream, buf: ByteArray) {
        var off = 0
        while (off < buf.size) {
            val n = input.read(buf, off, buf.size - off)
            require(n > 0) { "Truncated vault file" }
            off += n
        }
    }
}

/** Salted SHA-256 for the vault PIN and recovery answers. Stored as "sha256$<salt hex>$<hash hex>". */
object VaultSecret {
    private const val PREFIX = "sha256$"

    fun normalizeAnswer(answer: String) = answer.trim().lowercase()

    fun hash(value: String, salt: ByteArray = ByteArray(16).also { SecureRandom().nextBytes(it) }): String {
        val digest = MessageDigest.getInstance("SHA-256").apply { update(salt) }.digest(value.toByteArray())
        return PREFIX + salt.toHex() + "$" + digest.toHex()
    }

    fun isHashed(stored: String?) = stored?.startsWith(PREFIX) == true

    /** Accepts both hashed values and plain ones saved by older versions. */
    fun matches(stored: String?, input: String): Boolean {
        if (stored.isNullOrEmpty()) return false
        if (!isHashed(stored)) return MessageDigest.isEqual(stored.toByteArray(), input.toByteArray())
        val parts = stored.removePrefix(PREFIX).split("$")
        if (parts.size != 2) return false
        return MessageDigest.isEqual(hash(input, parts[0].hexToBytes()).toByteArray(), stored.toByteArray())
    }

    private fun ByteArray.toHex() = joinToString("") { "%02x".format(it) }
    private fun String.hexToBytes() = chunked(2).map { it.toInt(16).toByte() }.toByteArray()
}
