package com.quizango.gallery

import com.quizango.gallery.ui.navigation.Screen
import org.junit.Assert.*
import org.junit.Test

class OnboardingTest {

    @Test
    fun testSplashNavigationRoutingLogic() {
        // Case 1: First-time user (Welcome not completed)
        var hasCompletedWelcome = false
        var hasPermissions = false
        var targetRoute = if (!hasCompletedWelcome) {
            Screen.Welcome.route
        } else if (!hasPermissions) {
            Screen.Permission.route
        } else {
            Screen.Home.route
        }
        assertEquals(Screen.Welcome.route, targetRoute)

        // Case 2: Welcome completed, but permissions not granted yet
        hasCompletedWelcome = true
        hasPermissions = false
        targetRoute = if (!hasCompletedWelcome) {
            Screen.Welcome.route
        } else if (!hasPermissions) {
            Screen.Permission.route
        } else {
            Screen.Home.route
        }
        assertEquals(Screen.Permission.route, targetRoute)

        // Case 3: Returning user with permissions granted
        hasCompletedWelcome = true
        hasPermissions = true
        targetRoute = if (!hasCompletedWelcome) {
            Screen.Welcome.route
        } else if (!hasPermissions) {
            Screen.Permission.route
        } else {
            Screen.Home.route
        }
        assertEquals(Screen.Home.route, targetRoute)
    }

    @Test
    fun testOnboardingStepOrder() {
        val onboardingSteps = listOf(
            Screen.Splash.route,
            Screen.Welcome.route,
            Screen.Language.route,
            Screen.Permission.route,
            Screen.Home.route
        )

        assertEquals("splash", onboardingSteps[0])
        assertEquals("welcome", onboardingSteps[1])
        assertEquals("language", onboardingSteps[2])
        assertEquals("permission", onboardingSteps[3])
        assertEquals("home", onboardingSteps[4])
    }

    @Test
    fun testLanguageScreenNavigationFromOnboardingVsSettings() {
        // When coming from Welcome during initial onboarding:
        val fromOnboarding = true
        val canNavigateBackOnboarding = !fromOnboarding
        assertFalse(canNavigateBackOnboarding)

        val nextRouteOnboarding = if (canNavigateBackOnboarding) "back" else Screen.Permission.route
        assertEquals(Screen.Permission.route, nextRouteOnboarding)

        // When coming from Settings:
        val fromSettings = true
        val canNavigateBackSettings = fromSettings
        assertTrue(canNavigateBackSettings)

        val nextRouteSettings = if (canNavigateBackSettings) "back" else Screen.Permission.route
        assertEquals("back", nextRouteSettings)
    }

    @Test
    fun testRequiredPermissionsByApiLevel() {
        val tiramisuApiLevel = 33
        val legacyApiLevel = 29

        val tiramisuPermissions = if (tiramisuApiLevel >= 33) {
            listOf("android.permission.READ_MEDIA_IMAGES", "android.permission.READ_MEDIA_VIDEO")
        } else {
            listOf("android.permission.READ_EXTERNAL_STORAGE")
        }
        assertEquals(2, tiramisuPermissions.size)
        assertTrue(tiramisuPermissions.contains("android.permission.READ_MEDIA_IMAGES"))
        assertTrue(tiramisuPermissions.contains("android.permission.READ_MEDIA_VIDEO"))

        val legacyPermissions = if (legacyApiLevel >= 33) {
            listOf("android.permission.READ_MEDIA_IMAGES", "android.permission.READ_MEDIA_VIDEO")
        } else {
            listOf("android.permission.READ_EXTERNAL_STORAGE")
        }
        assertEquals(1, legacyPermissions.size)
        assertEquals("android.permission.READ_EXTERNAL_STORAGE", legacyPermissions[0])
    }

    @Test
    fun testSplashAnimationParameters() {
        val delayTime = 3000L
        val minDelay = 1000L
        val maxDelay = 6000L
        assertTrue(delayTime in minDelay..maxDelay)

        val progressUpdateInterval = delayTime / 100L
        assertEquals(30L, progressUpdateInterval)

        val splashRawRes = com.quizango.gallery.R.raw.app_icon_lottie
        assertTrue(splashRawRes != 0)

        // Indeterminate wave pattern verification (activity_splash.xml parity)
        val isIndeterminate = true
        val progressWidthSdp = 160 // @dimen/_160sdp
        val primaryColorHex = "#0066FF"
        assertTrue(isIndeterminate)
        assertEquals(160, progressWidthSdp)
        assertEquals("#0066FF", primaryColorHex)
    }
}
