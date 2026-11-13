package com.quizango.gallery

import com.quizango.gallery.ui.theme.GalleryTypography
import com.quizango.gallery.ui.theme.Inter
import org.junit.Assert.assertEquals
import org.junit.Test

class ThemeTest {

    @Test
    fun everyTypographySlotUsesInter() {
        val t = GalleryTypography
        val slots = mapOf(
            "displayLarge" to t.displayLarge, "displayMedium" to t.displayMedium, "displaySmall" to t.displaySmall,
            "headlineLarge" to t.headlineLarge, "headlineMedium" to t.headlineMedium, "headlineSmall" to t.headlineSmall,
            "titleLarge" to t.titleLarge, "titleMedium" to t.titleMedium, "titleSmall" to t.titleSmall,
            "bodyLarge" to t.bodyLarge, "bodyMedium" to t.bodyMedium, "bodySmall" to t.bodySmall,
            "labelLarge" to t.labelLarge, "labelMedium" to t.labelMedium, "labelSmall" to t.labelSmall
        )
        assertEquals(15, slots.size)
        slots.forEach { (name, style) -> assertEquals(name, Inter, style.fontFamily) }
    }
}
