package com.quizango.gallery

import com.quizango.gallery.ui.navigation.Screen
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Unit tests verifying Onboarding Flow 2.7.2 requirements and preferences (Plan 02-05).
 */
class OnboardingFlowTest {

    @Test
    fun testScreenRoutesMatch272Requirements() {
        assertEquals("splash", Screen.Splash.route)
        assertEquals("language", Screen.Language.route)
        assertEquals("permission", Screen.Permission.route)
        assertEquals("feature_promo", Screen.FeaturePromo.route)
        assertEquals("home", Screen.Home.route)
    }

    @Test
    fun testOnboardingStringsExist() {
        assertTrue(R.string.onb_feature_title != 0)
        assertTrue(R.string.onb_feature_subtitle != 0)
        assertTrue(R.string.onb_feature_enable != 0)
        assertTrue(R.string.onb_feature_skip != 0)
        assertTrue(R.string.why_this_permission_needed != 0)
        assertTrue(R.string.call_permission_need != 0)
        assertTrue(R.string.open_setting_dialog != 0)
        assertTrue(R.string.tap_permission_dialog != 0)
        assertTrue(R.string.select_phone_call_dialog != 0)
        assertTrue(R.string.tap_allow_dialog != 0)
    }

    @Test
    fun testOnboardingDrawablesExist() {
        assertTrue(R.drawable.ic_floating_btn != 0)
        assertTrue(R.drawable.ic_permission_needed != 0)
        assertTrue(R.drawable.ic_logo != 0)
    }

    @Test
    fun testOnboardingColorsExist() {
        assertTrue(R.color.per_text_color != 0)
        assertTrue(R.color.gray_permission != 0)
        assertTrue(R.color.radioButtonUncheckedColor != 0)
        assertTrue(R.color.op_onboarding_desc_color != 0)
        assertTrue(R.color.op_onboarding_ac_prem_desc != 0)
    }
}
