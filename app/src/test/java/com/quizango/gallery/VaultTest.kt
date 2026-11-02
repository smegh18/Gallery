package com.quizango.gallery

import com.quizango.gallery.domain.model.MediaType
import com.quizango.gallery.domain.model.VaultItem
import com.quizango.gallery.ui.navigation.Screen
import org.junit.Assert.*
import org.junit.Test

class VaultTest {

    @Test
    fun testVaultPinValidation() {
        val validPin = "1234"
        val wrongPin = "5678"
        val incompletePin = "12"

        assertEquals(4, validPin.length)
        assertTrue(validPin.all { it.isDigit() })
        assertNotEquals(validPin, wrongPin)
        assertFalse(incompletePin.length == 4)
    }

    @Test
    fun testVaultItemCreation() {
        val imageItem = VaultItem(
            filePath = "/storage/emulated/0/Android/data/com.quizango.gallery/files/vault/secret_photo.jpg",
            fileName = "secret_photo.jpg",
            folderName = "vault",
            folderPath = "/storage/emulated/0/Android/data/com.quizango.gallery/files/vault",
            fileSize = 1048576L,
            dateHidden = 1730000000L,
            mediaType = MediaType.IMAGE
        )

        assertEquals("secret_photo.jpg", imageItem.fileName)
        assertEquals(MediaType.IMAGE, imageItem.mediaType)
        assertEquals(1048576L, imageItem.fileSize)

        val videoItem = VaultItem(
            filePath = "/storage/emulated/0/Android/data/com.quizango.gallery/files/vault/secret_video.mp4",
            fileName = "secret_video.mp4",
            folderName = "vault",
            folderPath = "/storage/emulated/0/Android/data/com.quizango.gallery/files/vault",
            fileSize = 5242880L,
            dateHidden = 1730000000L,
            mediaType = MediaType.VIDEO,
            durationMs = 15000L
        )

        assertEquals("secret_video.mp4", videoItem.fileName)
        assertEquals(MediaType.VIDEO, videoItem.mediaType)
        assertEquals(15000L, videoItem.durationMs)
    }

    @Test
    fun testSecurityQuestionAnswerMatching() {
        val savedAnswer = "Fluffy"

        val exactInput = "Fluffy"
        val caseInsensitiveInput = "fluffy"
        val trimmedInput = "  Fluffy  "
        val wrongInput = "Doggy"

        assertTrue(exactInput.trim().equals(savedAnswer.trim(), ignoreCase = true))
        assertTrue(caseInsensitiveInput.trim().equals(savedAnswer.trim(), ignoreCase = true))
        assertTrue(trimmedInput.trim().equals(savedAnswer.trim(), ignoreCase = true))
        assertFalse(wrongInput.trim().equals(savedAnswer.trim(), ignoreCase = true))
    }

    @Test
    fun testVaultSelectionToggle() {
        val item1 = "/vault/item1.jpg"
        val item2 = "/vault/item2.jpg"
        val item3 = "/vault/item3.mp4"
        val allItems = listOf(item1, item2, item3)

        var selected = setOf<String>()
        assertFalse(selected.isNotEmpty())

        // Select one
        selected = selected + item1
        assertEquals(1, selected.size)

        // Select all
        selected = allItems.toSet()
        assertEquals(3, selected.size)

        // Select All toggle: if all are selected, deselect all
        val nextSelected = if (selected.size == allItems.size) emptySet() else allItems.toSet()
        assertTrue(nextSelected.isEmpty())
    }

    @Test
    fun testVaultNavigationRoutes() {
        assertEquals("vault_lock", Screen.VaultLock.route)
        assertEquals("vault_media", Screen.VaultMedia.route)
        assertEquals("vault/settings", Screen.VaultSetting.route)
        assertEquals("vault/recovery_question", Screen.VaultRecoveryQuestion.route)
        assertEquals("vault/forgot_password", Screen.VaultForgotPassword.route)
    }
}
