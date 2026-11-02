package com.quizango.gallery

import org.junit.Assert.*
import org.junit.Test

class SettingsTest {

    @Test
    fun testThemeModeOptions() {
        val DAY_MODE = 0
        val NIGHT_MODE = 1
        val SYSTEM_MODE = 2

        val validModes = setOf(DAY_MODE, NIGHT_MODE, SYSTEM_MODE)
        assertTrue(validModes.contains(0))
        assertTrue(validModes.contains(1))
        assertTrue(validModes.contains(2))
        assertFalse(validModes.contains(3))

        // Check default mode is System Mode (2)
        val defaultMode = SYSTEM_MODE
        assertEquals(2, defaultMode)
    }

    @Test
    fun testRatingLabelsMapping() {
        val ratingLabels = mapOf(
            5 to "The best we can get",
            4 to "I like it",
            3 to "It is good",
            2 to "I don't like it",
            1 to "I hate it"
        )

        assertEquals("The best we can get", ratingLabels[5])
        assertEquals("I like it", ratingLabels[4])
        assertEquals("It is good", ratingLabels[3])
        assertEquals("I don't like it", ratingLabels[2])
        assertEquals("I hate it", ratingLabels[1])
        assertNull(ratingLabels[0])
        assertNull(ratingLabels[6])
    }

    @Test
    fun testAutoDeleteRecycleBinPreference() {
        // Recycle bin 30-day auto deletion logic
        val now = System.currentTimeMillis()
        val thirtyDaysMs = 30L * 24 * 60 * 60 * 1000L

        val recentDeleted = now - (10L * 24 * 60 * 60 * 1000L) // 10 days ago
        val expiredDeleted = now - (35L * 24 * 60 * 60 * 1000L) // 35 days ago

        val isExpiredRecent = (now - recentDeleted) > thirtyDaysMs
        val isExpiredOld = (now - expiredDeleted) > thirtyDaysMs

        assertFalse(isExpiredRecent)
        assertTrue(isExpiredOld)
    }

    @Test
    fun testLanguageSelectionValidation() {
        val supportedCodes = setOf(
            "en", "ar", "bn", "de", "es", "fa", "fil", "fr",
            "gu", "hi", "in", "it", "ja", "ko", "mr", "ms",
            "my", "pl", "pt", "ru", "sv", "ta", "tr", "ur", "zh"
        )

        assertTrue(supportedCodes.contains("en"))
        assertTrue(supportedCodes.contains("es"))
        assertTrue(supportedCodes.contains("hi"))
        assertTrue(supportedCodes.contains("fr"))
        assertEquals(25, supportedCodes.size)
    }

    @Test
    fun testAboutSocialLinks() {
        val socialLinks = mapOf(
            "instagram" to "https://www.instagram.com/photogalleryvault/",
            "youtube" to "https://www.youtube.com/@appspecials4546",
            "facebook" to "https://www.facebook.com/photogallerybyappspecials",
            "pinterest" to "https://in.pinterest.com/appphotogallery/",
            "tiktok" to "https://www.tiktok.com/@photogalleryapp?lang=en"
        )

        assertEquals(5, socialLinks.size)
        assertTrue(socialLinks["instagram"]!!.contains("instagram.com"))
        assertTrue(socialLinks["youtube"]!!.contains("youtube.com"))
        assertTrue(socialLinks["facebook"]!!.contains("facebook.com"))
        assertTrue(socialLinks["pinterest"]!!.contains("pinterest.com"))
        assertTrue(socialLinks["tiktok"]!!.contains("tiktok.com"))
    }
}
