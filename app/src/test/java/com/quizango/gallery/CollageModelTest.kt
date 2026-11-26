package com.quizango.gallery

import com.quizango.gallery.ui.creative.collage.COLLAGE_BG_COLORS
import com.quizango.gallery.ui.creative.collage.COLLAGE_PAINT_COLORS
import com.quizango.gallery.ui.creative.collage.COLLAGE_RATIOS
import androidx.compose.ui.geometry.Rect
import com.quizango.gallery.ui.creative.collage.CollageTemplate
import com.quizango.gallery.ui.creative.collage.TextCase
import com.quizango.gallery.ui.creative.collage.UnitRect
import com.quizango.gallery.ui.creative.collage.brushStrokeSize
import com.quizango.gallery.ui.creative.collage.cellRect
import com.quizango.gallery.ui.creative.collage.collageFileName
import com.quizango.gallery.ui.creative.collage.collageTemplates
import com.quizango.gallery.ui.creative.collage.evenGroups
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.UUID

class CollageModelTest {
    @Test
    fun everyTemplateHoldsExactlyItsPhotosAndTilesTheSquare() {
        for (count in 2..15) {
            val templates = collageTemplates(count)
            assertTrue("count $count has templates", templates.size >= 2)
            for (t in templates) {
                val cells = t.cells()
                assertEquals(count, cells.size)
                val area = cells.sumOf { ((it.right - it.left) * (it.bottom - it.top)).toDouble() }
                assertEquals("count $count $t", 1.0, area, 1e-4)
            }
            assertEquals("no duplicate layouts for $count", templates.size, templates.map { it.cells() }.toSet().size)
        }
    }

    @Test
    fun twoPhotosDefaultToSideBySide() {
        assertEquals(listOf(UnitRect(0f, 0f, 0.5f, 1f), UnitRect(0.5f, 0f, 1f, 1f)), collageTemplates(2).first().cells())
        assertEquals(CollageTemplate(false, listOf(2, 2)), collageTemplates(4).first())
    }

    @Test
    fun evenGroupsPutTheFullerRowsLast() {
        assertEquals(listOf(2, 3), evenGroups(5, 2))
        assertEquals(listOf(3, 3, 4), evenGroups(10, 3))
        assertEquals(listOf(2, 2), evenGroups(4, 2))
    }

    @Test
    fun ratiosMatchTheTwentyPresetsWithOneToOneFirst() {
        assertEquals(20, COLLAGE_RATIOS.size)
        assertEquals(1 to 1, COLLAGE_RATIOS.first().let { it.width to it.height })
        assertEquals(10 to 8, COLLAGE_RATIOS.last().let { it.width to it.height })
    }

    @Test
    fun palettesKeepTheReferenceLengths() {
        assertEquals(102, COLLAGE_BG_COLORS.size)
        assertEquals(0, COLLAGE_BG_COLORS.first())
        assertEquals(101, COLLAGE_PAINT_COLORS.size)
        assertEquals(0xFFFFFFFF.toInt(), COLLAGE_PAINT_COLORS.first())
    }

    @Test
    fun cellsKeepThePaddingBetweenCellsAndAtTheEdges() {
        assertEquals(Rect(6f, 6f, 97f, 94f), cellRect(UnitRect(0f, 0f, 0.5f, 1f), 200f, 100f, 6f))
        assertEquals(Rect(103f, 6f, 194f, 94f), cellRect(UnitRect(0.5f, 0f, 1f, 1f), 200f, 100f, 6f))
    }

    @Test
    fun textCaseButtonsRewriteTheTypedText() {
        assertEquals("Hello World", TextCase.AS_TYPED.apply("Hello World"))
        assertEquals("hello world", TextCase.LOWER.apply("Hello World"))
        assertEquals("Hello World", TextCase.CAPITAL.apply("hELLO wORLD"))
        assertEquals("HELLO", TextCase.UPPER.apply("Hello"))
        assertEquals("hELLO", TextCase.TOGGLE.apply("Hello"))
    }

    @Test
    fun brushSizeIsTheSliderPlusTenAndSavesUseAUuidPng() {
        assertEquals(20, brushStrokeSize(10))
        assertEquals(35, brushStrokeSize(99))
        val id = UUID.fromString("123e4567-e89b-12d3-a456-426614174000")
        assertEquals("123e4567-e89b-12d3-a456-426614174000.png", collageFileName(id))
    }
}
