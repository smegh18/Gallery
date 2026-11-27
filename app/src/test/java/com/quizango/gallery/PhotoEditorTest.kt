package com.quizango.gallery

import androidx.compose.ui.geometry.Offset
import com.quizango.gallery.ui.editor.AdjustParam
import com.quizango.gallery.ui.editor.CropPreset
import com.quizango.gallery.ui.editor.EditTool
import com.quizango.gallery.ui.editor.TextHit
import com.quizango.gallery.ui.editor.adjustAmount
import com.quizango.gallery.ui.editor.editRefusal
import com.quizango.gallery.ui.editor.editTools
import com.quizango.gallery.ui.editor.firstTooltip
import com.quizango.gallery.ui.editor.initialTool
import com.quizango.gallery.ui.editor.isSuggestionTool
import com.quizango.gallery.ui.editor.rotatedBy
import com.quizango.gallery.ui.editor.sampleSizeFor
import com.quizango.gallery.ui.editor.savedImageFile
import com.quizango.gallery.ui.editor.textHit
import com.quizango.gallery.ui.editor.workingCopyName
import com.quizango.gallery.ui.editor.zoomedScale
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/** PhotoEditActivity rules (#38): tool strip, suggestion entry, crop presets, adjust scale, files, text sticker hits. */
class PhotoEditorTest {

    @Test
    fun aiToolsLeadTheStripOnlyWhenAvailable() {
        assertEquals(EditTool.entries, editTools(aiAvailable = true))
        assertEquals(listOf(EditTool.CROP, EditTool.ADJUST, EditTool.FILTER, EditTool.BRUSH, EditTool.STICKER, EditTool.TEXT), editTools(aiAvailable = false))
    }

    @Test
    fun oneTooltipAtATimeRemoveBgFirst() {
        val seen = mutableSetOf<String>()
        assertEquals(EditTool.REMOVE_BG, firstTooltip(true, true) { it in seen })
        seen += "isRemoveEditToolTip"
        assertEquals(EditTool.COLOR_SPLASH, firstTooltip(true, true) { it in seen })
        seen += listOf("isColorEditToolTip", "isBlurEditToolTip")
        assertNull(firstTooltip(true, true) { it in seen })
        assertNull(firstTooltip(false, true) { false })
        assertNull(firstTooltip(true, false) { false })
    }

    @Test
    fun suggestionRoutesOpenTheirTool() {
        assertEquals(EditTool.CROP, initialTool("crop"))
        assertEquals(EditTool.BRUSH, initialTool("Markup"))
        assertEquals(EditTool.FILTER, initialTool("filter"))
        assertNull(initialTool(null))
        assertTrue(isSuggestionTool("crop") && isSuggestionTool("brush"))
        assertFalse(isSuggestionTool("adjust") || isSuggestionTool(null))
    }

    @Test
    fun cropPresetsKeepTheShippedAspects() {
        assertEquals(0 to 0, CropPreset.FREE.aspectX to CropPreset.FREE.aspectY)
        assertEquals(4 to 3, CropPreset.IG_4_5.aspectX to CropPreset.IG_4_5.aspectY) // 2.7.2 quirk, kept
        assertEquals(4 to 5, CropPreset.INSTA_STORY.aspectX to CropPreset.INSTA_STORY.aspectY)
        assertEquals(16 to 9, CropPreset.YOUTUBE.aspectX to CropPreset.YOUTUBE.aspectY)
    }

    @Test
    fun adjustRulerMapsToGpuImageAmounts() {
        assertEquals(1f, adjustAmount(AdjustParam.BRIGHTNESS.max), 0.0001f)
        assertEquals(1f, adjustAmount(AdjustParam.CONTRAST.default), 0.0001f)
        assertEquals(-1f, adjustAmount(AdjustParam.SHARPEN.min), 0.0001f)
    }

    @Test
    fun filesAndRefusals() {
        assertEquals("image.jpg", workingCopyName("IMG_1.jpg"))
        val pictures = File("/pics")
        assertEquals(File("/dcim/cam", "Image_5.png"), savedImageFile("/dcim/cam", pictures, 5))
        assertEquals(File(pictures, "Image_5.png"), savedImageFile(null, pictures, 5))
        assertEquals(R.string.toast_file, editRefusal("a.SVG", true))
        assertEquals(R.string.heif_image_toast, editRefusal("a.heic", true))
        assertEquals(R.string.toast_corrupted_image, editRefusal("a.jpg", false))
        assertNull(editRefusal("a.jpg", true))
        assertEquals(1, sampleSizeFor(4000, 3000, 4096))
        assertEquals(2, sampleSizeFor(8000, 3000, 4096))
    }

    @Test
    fun textStickerCornersOnlyWhenSelected() {
        val c = Offset(100f, 100f)
        assertEquals(TextHit.CLOSE, textHit(Offset(52f, 82f), c, 100f, 40f, 0f, 10f, selected = true))
        assertEquals(TextHit.ZOOM, textHit(Offset(148f, 118f), c, 100f, 40f, 0f, 10f, selected = true))
        assertEquals(TextHit.BODY, textHit(Offset(52f, 82f), c, 100f, 40f, 0f, 10f, selected = false))
        assertNull(textHit(Offset(200f, 100f), c, 100f, 40f, 0f, 10f, selected = true))
        // turned 90 degrees the sticker is tall, so a point above the centre is inside
        assertEquals(TextHit.BODY, textHit(Offset(100f, 60f), c, 100f, 40f, 90f, 10f, selected = false))
    }

    @Test
    fun zoomAndRotateFollowTheFinger() {
        val c = Offset(0f, 0f)
        assertEquals(2f, zoomedScale(1f, c, Offset(10f, 0f), Offset(20f, 0f)), 0.001f)
        assertEquals(6f, zoomedScale(1f, c, Offset(1f, 0f), Offset(100f, 0f)), 0.001f) // clamped
        assertEquals(90f, rotatedBy(0f, c, Offset(10f, 0f), Offset(0f, 10f)), 0.001f)
    }

    @Test
    fun testCropGeometryAspectCropRectCalculation() {
        val bounds = androidx.compose.ui.geometry.Rect(0f, 0f, 1000f, 1000f)

        // Free mode should return identical bounds
        val freeRect = com.quizango.gallery.ui.editor.crop.CropGeometry.calculateAspectCropRect(bounds, 0f, 0f)
        assertEquals(bounds, freeRect)

        // 1:1 on 1000x1000 square should match square exactly
        val squareRect = com.quizango.gallery.ui.editor.crop.CropGeometry.calculateAspectCropRect(bounds, 1f, 1f)
        assertEquals(1000f, squareRect.width, 0.01f)
        assertEquals(1000f, squareRect.height, 0.01f)

        // 16:9 on 1000x1000 square
        val wideRect = com.quizango.gallery.ui.editor.crop.CropGeometry.calculateAspectCropRect(bounds, 16f, 9f)
        assertEquals(1000f, wideRect.width, 0.01f)
        assertEquals(1000f / (16f / 9f), wideRect.height, 0.1f)
        // Check centered vertically
        assertEquals((1000f - wideRect.height) / 2f, wideRect.top, 0.1f)

        // 9:16 on 1000x1000 square
        val tallRect = com.quizango.gallery.ui.editor.crop.CropGeometry.calculateAspectCropRect(bounds, 9f, 16f)
        assertEquals(1000f, tallRect.height, 0.01f)
        assertEquals(1000f * (9f / 16f), tallRect.width, 0.1f)
        // Check centered horizontally
        assertEquals((1000f - tallRect.width) / 2f, tallRect.left, 0.1f)
    }

    @Test
    fun testCropGeometryHitTesting() {
        val cropRect = androidx.compose.ui.geometry.Rect(100f, 100f, 500f, 500f)
        val radius = 30f

        // Corners
        assertEquals(
            com.quizango.gallery.ui.editor.crop.CropHandle.TOP_LEFT,
            com.quizango.gallery.ui.editor.crop.CropGeometry.hitTest(Offset(105f, 105f), cropRect, radius)
        )
        assertEquals(
            com.quizango.gallery.ui.editor.crop.CropHandle.TOP_RIGHT,
            com.quizango.gallery.ui.editor.crop.CropGeometry.hitTest(Offset(495f, 105f), cropRect, radius)
        )
        assertEquals(
            com.quizango.gallery.ui.editor.crop.CropHandle.BOTTOM_LEFT,
            com.quizango.gallery.ui.editor.crop.CropGeometry.hitTest(Offset(105f, 495f), cropRect, radius)
        )
        assertEquals(
            com.quizango.gallery.ui.editor.crop.CropHandle.BOTTOM_RIGHT,
            com.quizango.gallery.ui.editor.crop.CropGeometry.hitTest(Offset(495f, 495f), cropRect, radius)
        )

        // Edges
        assertEquals(
            com.quizango.gallery.ui.editor.crop.CropHandle.TOP,
            com.quizango.gallery.ui.editor.crop.CropGeometry.hitTest(Offset(300f, 105f), cropRect, radius)
        )
        assertEquals(
            com.quizango.gallery.ui.editor.crop.CropHandle.BOTTOM,
            com.quizango.gallery.ui.editor.crop.CropGeometry.hitTest(Offset(300f, 495f), cropRect, radius)
        )
        assertEquals(
            com.quizango.gallery.ui.editor.crop.CropHandle.LEFT,
            com.quizango.gallery.ui.editor.crop.CropGeometry.hitTest(Offset(105f, 300f), cropRect, radius)
        )
        assertEquals(
            com.quizango.gallery.ui.editor.crop.CropHandle.RIGHT,
            com.quizango.gallery.ui.editor.crop.CropGeometry.hitTest(Offset(495f, 300f), cropRect, radius)
        )

        // Center
        assertEquals(
            com.quizango.gallery.ui.editor.crop.CropHandle.CENTER,
            com.quizango.gallery.ui.editor.crop.CropGeometry.hitTest(Offset(300f, 300f), cropRect, radius)
        )

        // Outside
        assertEquals(
            null,
            com.quizango.gallery.ui.editor.crop.CropGeometry.hitTest(Offset(50f, 50f), cropRect, radius)
        )
    }

    @Test
    fun testCropGeometryCenterDragClamping() {
        val bounds = androidx.compose.ui.geometry.Rect(0f, 0f, 1000f, 1000f)
        val initial = androidx.compose.ui.geometry.Rect(100f, 100f, 500f, 500f) // width=400, height=400

        // Move right by 200, down by 200
        val moved = com.quizango.gallery.ui.editor.crop.CropGeometry.updateCropRect(
            currentRect = initial,
            handle = com.quizango.gallery.ui.editor.crop.CropHandle.CENTER,
            dragDelta = Offset(200f, 200f),
            bounds = bounds,
            aspectX = 0f,
            aspectY = 0f
        )
        assertEquals(300f, moved.left, 0.01f)
        assertEquals(300f, moved.top, 0.01f)
        assertEquals(700f, moved.right, 0.01f)
        assertEquals(700f, moved.bottom, 0.01f)

        // Drag far beyond right/bottom bounds -> should clamp cleanly
        val clamped = com.quizango.gallery.ui.editor.crop.CropGeometry.updateCropRect(
            currentRect = initial,
            handle = com.quizango.gallery.ui.editor.crop.CropHandle.CENTER,
            dragDelta = Offset(1500f, 1500f),
            bounds = bounds,
            aspectX = 0f,
            aspectY = 0f
        )
        assertEquals(600f, clamped.left, 0.01f)
        assertEquals(600f, clamped.top, 0.01f)
        assertEquals(1000f, clamped.right, 0.01f)
        assertEquals(1000f, clamped.bottom, 0.01f)
        assertEquals(400f, clamped.width, 0.01f)
        assertEquals(400f, clamped.height, 0.01f)
    }

    @Test
    fun testCropGeometryFixedRatioCornerDrag() {
        val bounds = androidx.compose.ui.geometry.Rect(0f, 0f, 1000f, 1000f)
        val initial = androidx.compose.ui.geometry.Rect(200f, 200f, 600f, 600f) // 1:1, w=400, h=400

        // Drag BOTTOM_RIGHT by dx=100, dy=100
        val enlarged = com.quizango.gallery.ui.editor.crop.CropGeometry.updateCropRect(
            currentRect = initial,
            handle = com.quizango.gallery.ui.editor.crop.CropHandle.BOTTOM_RIGHT,
            dragDelta = Offset(100f, 100f),
            bounds = bounds,
            aspectX = 1f,
            aspectY = 1f
        )
        assertEquals(200f, enlarged.left, 0.01f)
        assertEquals(200f, enlarged.top, 0.01f)
        assertEquals(500f, enlarged.width, 0.01f)
        assertEquals(500f, enlarged.height, 0.01f)
        assertEquals(1f, enlarged.width / enlarged.height, 0.001f)
    }

    @Test
    fun testCreativeRoutes() {
        val path = "/sdcard/Pictures/sample.jpg"
        val removeBgRoute = com.quizango.gallery.ui.navigation.Screen.RemoveBg.createRoute(path)
        assertTrue(removeBgRoute.startsWith("creative/remove_bg?path="))
        assertTrue(removeBgRoute.contains("sample.jpg"))

        val blurBgRoute = com.quizango.gallery.ui.navigation.Screen.BlurBg.createRoute(path)
        assertTrue(blurBgRoute.startsWith("creative/blur_bg?path="))
        assertTrue(blurBgRoute.contains("sample.jpg"))

        val colorSplashRoute = com.quizango.gallery.ui.navigation.Screen.ColorSplash.createRoute(path)
        assertTrue(colorSplashRoute.startsWith("creative/color_splash?path="))
        assertTrue(colorSplashRoute.contains("sample.jpg"))
    }
}
