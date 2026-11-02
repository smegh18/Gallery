package com.quizango.gallery

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import com.quizango.gallery.ui.editor.AdjustParam
import com.quizango.gallery.ui.editor.BRUSH_COLORS
import com.quizango.gallery.ui.editor.CropRatio
import com.quizango.gallery.ui.editor.DrawnPath
import com.quizango.gallery.ui.editor.EditorFilter
import com.quizango.gallery.ui.editor.EditorTab
import com.quizango.gallery.ui.editor.PlacedSticker
import com.quizango.gallery.ui.editor.PlacedText
import com.quizango.gallery.ui.editor.STICKER_LIST
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Unit tests verifying Photo Editor models, enums, parameters, palettes, and structures.
 */
class PhotoEditorTest {

    @Test
    fun testEditorTabsCompleteness() {
        val tabs = EditorTab.values()
        assertEquals(6, tabs.size)
        assertEquals("Crop", EditorTab.CROP.label)
        assertEquals("Adjust", EditorTab.ADJUST.label)
        assertEquals("Filter", EditorTab.FILTER.label)
        assertEquals("Brush", EditorTab.BRUSH.label)
        assertEquals("Sticker", EditorTab.STICKER.label)
        assertEquals("Text", EditorTab.TEXT.label)

        tabs.forEach { tab ->
            assertTrue(tab.iconRes != 0)
        }
    }

    @Test
    fun testAdjustParamsCompleteness() {
        val params = AdjustParam.values()
        assertEquals(4, params.size)
        assertEquals("Brightness", AdjustParam.BRIGHTNESS.label)
        assertEquals("Contrast", AdjustParam.CONTRAST.label)
        assertEquals("Saturation", AdjustParam.SATURATION.label)
        assertEquals("Sharpen", AdjustParam.SHARPEN.label)

        params.forEach { param ->
            assertTrue(param.iconRes != 0)
        }
    }

    @Test
    fun testEditorFiltersCompleteness() {
        val filters = EditorFilter.values()
        assertTrue(filters.size >= 9)
        assertEquals("Original", EditorFilter.NONE.label)
        assertEquals("B&W", EditorFilter.GRAYSCALE.label)
        assertEquals("Sepia", EditorFilter.SEPIA.label)
        assertEquals("Vintage", EditorFilter.VINTAGE.label)
        assertEquals("Cool", EditorFilter.COOL.label)
        assertEquals("Warm", EditorFilter.WARM.label)
        assertEquals("Vivid", EditorFilter.VIVID.label)
        assertEquals("Fade", EditorFilter.FADE.label)
        assertEquals("Neon", EditorFilter.CYBERPUNK.label)
    }

    @Test
    fun testCropRatiosCompleteness() {
        val ratios = CropRatio.values()
        assertTrue(ratios.size >= 8)
        assertEquals("Free", CropRatio.FREE.label)
        assertEquals(0f, CropRatio.FREE.aspectX, 0.001f)

        assertEquals("1:1", CropRatio.ONE_ONE.label)
        assertEquals(1f, CropRatio.ONE_ONE.aspectX / CropRatio.ONE_ONE.aspectY, 0.001f)

        assertEquals("4:5", CropRatio.FOUR_FIVE.label)
        assertEquals(0.8f, CropRatio.FOUR_FIVE.aspectX / CropRatio.FOUR_FIVE.aspectY, 0.001f)

        assertEquals("Story", CropRatio.NINE_SIXTEEN.label)
        assertEquals(9f / 16f, CropRatio.NINE_SIXTEEN.aspectX / CropRatio.NINE_SIXTEEN.aspectY, 0.001f)

        assertEquals("Cover", CropRatio.SIXTEEN_NINE.label)
        assertEquals(16f / 9f, CropRatio.SIXTEEN_NINE.aspectX / CropRatio.SIXTEEN_NINE.aspectY, 0.001f)

        assertEquals("5:4", CropRatio.FIVE_FOUR.label)
        assertEquals(1.25f, CropRatio.FIVE_FOUR.aspectX / CropRatio.FIVE_FOUR.aspectY, 0.001f)

        assertEquals("4:3", CropRatio.FOUR_THREE.label)
        assertEquals(4f / 3f, CropRatio.FOUR_THREE.aspectX / CropRatio.FOUR_THREE.aspectY, 0.001f)

        assertEquals("3:4", CropRatio.THREE_FOUR.label)
        assertEquals(0.75f, CropRatio.THREE_FOUR.aspectX / CropRatio.THREE_FOUR.aspectY, 0.001f)
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
    fun testBrushColorsPalette() {
        assertEquals(10, BRUSH_COLORS.size)
        // Red, Orange, Yellow, Green, Cyan, Blue, Purple, Pink, White, Black
        assertEquals(Color(0xFFE11A1A), BRUSH_COLORS[0])
        assertEquals(Color(0xFF247BF7), BRUSH_COLORS[5])
        assertEquals(Color(0xFFFFFFFF), BRUSH_COLORS[8])
        assertEquals(Color(0xFF000000), BRUSH_COLORS[9])
    }

    @Test
    fun testStickerListNonEmpty() {
        assertTrue(STICKER_LIST.isNotEmpty())
        assertTrue(STICKER_LIST.contains("✨"))
        assertTrue(STICKER_LIST.contains("❤️"))
        assertTrue(STICKER_LIST.contains("⭐"))
        assertTrue(STICKER_LIST.contains("🔥"))
    }

    @Test
    fun testDrawnPathModel() {
        val points = listOf(Offset(10f, 20f), Offset(30f, 40f))
        val path = DrawnPath(points = points, color = Color.Red, strokeWidth = 15f)
        assertEquals(2, path.points.size)
        assertEquals(Color.Red, path.color)
        assertEquals(15f, path.strokeWidth, 0.001f)
    }

    @Test
    fun testPlacedStickerModel() {
        val sticker = PlacedSticker(sticker = "🎉", offset = Offset(150f, 250f))
        assertEquals("🎉", sticker.sticker)
        assertEquals(150f, sticker.offset.x, 0.001f)
        assertEquals(250f, sticker.offset.y, 0.001f)
        assertTrue(sticker.id != 0L)
    }

    @Test
    fun testPlacedTextModel() {
        val text = PlacedText(text = "Hello Gallery", color = Color.Yellow, fontSize = 28f, opacity = 0.9f)
        assertEquals("Hello Gallery", text.text)
        assertEquals(Color.Yellow, text.color)
        assertEquals(28f, text.fontSize, 0.001f)
        assertEquals(0.9f, text.opacity, 0.001f)
        assertTrue(text.id != 0L)
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

    @Test
    fun testRemoveBgChoiceTypes() {
        val transparent = com.quizango.gallery.ui.creative.RemoveBgChoice.Transparent
        val colorPicker = com.quizango.gallery.ui.creative.RemoveBgChoice.ColorPicker
        val gallery = com.quizango.gallery.ui.creative.RemoveBgChoice.Gallery
        val solid = com.quizango.gallery.ui.creative.RemoveBgChoice.SolidColor(Color(0xFF9E9E9E), 0)

        assertTrue(transparent is com.quizango.gallery.ui.creative.RemoveBgChoice)
        assertTrue(colorPicker is com.quizango.gallery.ui.creative.RemoveBgChoice)
        assertTrue(gallery is com.quizango.gallery.ui.creative.RemoveBgChoice)
        assertEquals(0, solid.id)
        assertEquals(Color(0xFF9E9E9E), solid.color)
    }

    @Test
    fun testCreativeRulerParameters() {
        val minBlur = 0
        val maxBlur = 100
        val defaultBlur = 25
        assertTrue(defaultBlur in minBlur..maxBlur)

        val minSplash = 0
        val maxSplash = 100
        val defaultSplash = 0
        assertTrue(defaultSplash in minSplash..maxSplash)
    }
}
