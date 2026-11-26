package com.quizango.gallery

import com.quizango.gallery.ui.settings.ABOUT_VERSION_CODE
import com.quizango.gallery.ui.settings.ABOUT_VERSION_NAME
import com.quizango.gallery.ui.settings.AboutLinks
import com.quizango.gallery.ui.settings.PAYWALL_SLIDES
import com.quizango.gallery.ui.settings.UsageToggle
import com.quizango.gallery.ui.settings.rateStar
import com.quizango.gallery.ui.settings.rateTitle
import com.quizango.gallery.ui.settings.usageSwitchOn
import com.quizango.gallery.ui.settings.usageToggle
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SettingsRowsTest {

    @Test
    fun usageRowTurnsOffAsksForAccessOrTurnsOn() {
        assertEquals(UsageToggle.TURN_OFF, usageToggle(checked = true, accessGranted = true))
        assertEquals(UsageToggle.TURN_ON, usageToggle(checked = false, accessGranted = true))
        assertEquals(UsageToggle.ASK_ACCESS, usageToggle(checked = false, accessGranted = false))
    }

    @Test
    fun usageSwitchNeedsBothTheChoiceAndAccess() {
        assertTrue(usageSwitchOn(enabled = true, accessGranted = true))
        assertFalse(usageSwitchOn(enabled = true, accessGranted = false)) // default on, but no usage access yet
        assertFalse(usageSwitchOn(enabled = false, accessGranted = true))
    }

    @Test
    fun rateDialogSaysSorryBelowThreeStars() {
        assertEquals(R.string.thanks_for_your_support_1, rateTitle(1))
        assertEquals(R.string.thanks_for_your_support_1, rateTitle(2))
        assertEquals(R.string.thanks_for_your_support, rateTitle(3))
        assertEquals(R.string.thanks_for_your_support, rateTitle(5))
        assertEquals(R.drawable.ic_rate_star_1, rateStar(1))
        assertEquals(R.drawable.ic_rate_star_5, rateStar(5))
        assertEquals(R.drawable.ic_rate_star_1, rateStar(0)) // the bar never goes below one star
    }

    @Test
    fun aboutUsesTheReferenceLinksAndVersion() {
        assertEquals("https://appsspecials.com/bestgallery/privacy", AboutLinks.PRIVACY_POLICY)
        assertEquals("https://appsspecials.com/bestgallery/terms", AboutLinks.TERMS)
        assertEquals("https://www.instagram.com/galleryphotovault/", AboutLinks.INSTAGRAM)
        assertEquals("2.7.2", ABOUT_VERSION_NAME)
        assertEquals(152, ABOUT_VERSION_CODE)
    }

    @Test
    fun paywallSliderHasTheSixStaticSlides() {
        assertEquals(6, PAYWALL_SLIDES.size)
        assertEquals(R.string.paywall_slider_ads_title to R.string.paywall_slider_ads_desc, PAYWALL_SLIDES.first())
        assertEquals(R.string.paywall_slider_recycle_title to R.string.paywall_slider_recycle_desc, PAYWALL_SLIDES.last())
    }
}
