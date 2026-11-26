package com.quizango.gallery

import android.app.WallpaperManager
import com.quizango.gallery.ui.creative.ImageSegmentationHelper
import com.quizango.gallery.ui.creative.WALLPAPER_BOTH
import com.quizango.gallery.ui.creative.WALLPAPER_HOME
import com.quizango.gallery.ui.creative.WALLPAPER_LOCK
import com.quizango.gallery.ui.creative.rulerLabel
import com.quizango.gallery.ui.creative.showsGuide
import com.quizango.gallery.ui.creative.splashIntensity
import com.quizango.gallery.ui.creative.wallpaperFlag
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class AiToolsTest {
    @Test
    fun theGuideShowsOnlyOnceTheFlagIsKnownToBeFalse() {
        assertTrue(showsGuide(false))
        assertFalse(showsGuide(true))
        assertFalse(showsGuide(null)) // still loading: neither the guide nor processing yet
    }

    @Test
    fun rulerReadsZeroToHundredAndSplashIntensityFollowsIt() {
        assertEquals(0, rulerLabel(0))
        assertEquals(100, rulerLabel(25))
        assertEquals(1f, splashIntensity(25), 0f)
        assertEquals(1f - (25 - 10) * 0.04f, splashIntensity(10), 1e-6f) // e9/c's formula
        assertEquals(0f, splashIntensity(0), 0f)
    }

    @Test
    fun splashMatrixIsIdentityAtZeroAndRedEverywhereAtFull() {
        val identity = floatArrayOf(1f, 0f, 0f, 0f, 0f, 0f, 1f, 0f, 0f, 0f, 0f, 0f, 1f, 0f, 0f, 0f, 0f, 0f, 1f, 0f)
        val mono = floatArrayOf(1f, 0f, 0f, 0f, 0f, 1f, 0f, 0f, 0f, 0f, 1f, 0f, 0f, 0f, 0f, 0f, 0f, 0f, 1f, 0f)
        assertArrayEquals(identity, ImageSegmentationHelper.splashMatrixValues(0f), 0f)
        assertArrayEquals(mono, ImageSegmentationHelper.splashMatrixValues(1f), 0f)
    }

    @Test
    fun wallpaperChoicesMapToLockHomeAndBoth() {
        assertEquals(WallpaperManager.FLAG_LOCK, wallpaperFlag(WALLPAPER_LOCK))
        assertEquals(WallpaperManager.FLAG_SYSTEM, wallpaperFlag(WALLPAPER_HOME))
        assertEquals(WallpaperManager.FLAG_SYSTEM or WallpaperManager.FLAG_LOCK, wallpaperFlag(WALLPAPER_BOTH))
    }

    @Test
    fun theSubjectIsTrimmedToItsNonTransparentBox() {
        val px = IntArray(4 * 3) // 4 wide, 3 high, all transparent
        assertNull(ImageSegmentationHelper.opaqueBounds(px, 4, 3))
        px[1 * 4 + 1] = 0x40FF0000 // faint but not transparent
        px[2 * 4 + 2] = 0xFF00FF00.toInt()
        assertArrayEquals(intArrayOf(1, 1, 2, 2), ImageSegmentationHelper.opaqueBounds(px, 4, 3))
    }
}
