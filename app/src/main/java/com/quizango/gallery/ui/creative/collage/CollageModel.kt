package com.quizango.gallery.ui.creative.collage

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import com.quizango.gallery.R
import java.util.UUID
import kotlin.math.roundToInt
import kotlin.math.sqrt

/** A cell of a template in the unit square (0..1 on both axes). */
internal data class UnitRect(val left: Float, val top: Float, val right: Float, val bottom: Float)

/**
 * A straight template (2.7.2 kf/a..kf/c): [groups] rows stacked top to bottom, each split evenly into that many cells,
 * or the same as columns when [columns] is true. Cells come out in photo order.
 */
internal data class CollageTemplate(val columns: Boolean, val groups: List<Int>) {
    val size: Int get() = groups.sum()

    fun cells(): List<UnitRect> = groups.flatMapIndexed { g, n ->
        val a0 = g.toFloat() / groups.size
        val a1 = (g + 1).toFloat() / groups.size
        (0 until n).map { i ->
            val b0 = i.toFloat() / n
            val b1 = (i + 1).toFloat() / n
            if (columns) UnitRect(a0, b0, a1, b1) else UnitRect(b0, a0, b1, a1)
        }
    }
}

/** [count] cells over [rows] rows as evenly as possible, the fuller rows last (5 over 2 rows is 2 + 3). */
internal fun evenGroups(count: Int, rows: Int): List<Int> = List(rows) { r -> count / rows + if (r >= rows - count % rows) 1 else 0 }

/**
 * The template list for [count] photos (2.7.2 il/b.SJowARcXwM). Element 0 is the default: side by side for two photos,
 * the near-square grid above that.
 * shortcut: straight templates only (no slant ones) and a generated subset of 2.7.2's per-count lists; port kf/a.. if exact sets matter.
 */
internal fun collageTemplates(count: Int): List<CollageTemplate> {
    require(count >= 1)
    val specs = LinkedHashSet<List<Int>>()
    val square = evenGroups(count, sqrt(count.toDouble()).roundToInt().coerceAtLeast(1))
    specs += square
    specs += square.reversed()
    if (count in 2..6) specs += listOf(1, count - 1)
    if (count in 3..6) specs += listOf(count - 1, 1)
    for (rows in 2..minOf(count, 5)) {
        evenGroups(count, rows).let { specs += it; specs += it.reversed() }
    }
    if (count <= 5) specs += listOf(count)
    return specs.flatMap { listOf(CollageTemplate(false, it), CollageTemplate(true, it)) }.distinctBy { it.cells() }
}

/** layout_ratio_collage presets (je/c): label (a string when 2.7.2 shows a word), aspect width:height and the two icons. */
internal data class CollageRatio(
    val label: String,
    @StringRes val labelRes: Int,
    val width: Int,
    val height: Int,
    @DrawableRes val icon: Int,
    @DrawableRes val selectedIcon: Int
)

internal val COLLAGE_RATIOS = listOf(
    CollageRatio("1:1", 0, 1, 1, R.drawable.ic_1_1_ratio, R.drawable.ic_1_1_ratio_select),
    CollageRatio("5:4", 0, 5, 4, R.drawable.ic_5_4_ratio, R.drawable.ic_5_4_ratio_select),
    CollageRatio("4:3", 0, 4, 3, R.drawable.ic_4_3_ratio, R.drawable.ic_4_3_ratio_select),
    CollageRatio("4:5", 0, 4, 5, R.drawable.ic_4_5_ratio, R.drawable.ic_4_5_ratio_select),
    CollageRatio("1:2", 0, 1, 2, R.drawable.ic_1_2_ratio, R.drawable.ic_1_2_ratio_select),
    CollageRatio("", R.string.collage_ratio_story, 9, 16, R.drawable.ic_instag, R.drawable.ic_instag_select),
    CollageRatio("", R.string.collage_ratio_movie, 16, 7, R.drawable.ic_movie, R.drawable.ic_movie_select),
    CollageRatio("2:3", 0, 2, 3, R.drawable.ic_2_3_ratio, R.drawable.ic_2_3_ratio_select),
    CollageRatio("", R.string.collage_ratio_post, 4, 3, R.drawable.ic_fb_post, R.drawable.ic_fb_post_select),
    CollageRatio("", R.string.collage_ratio_cover, 16, 6, R.drawable.ic_fb_cover, R.drawable.ic_fb_cover_select),
    CollageRatio("16:9", 0, 16, 9, R.drawable.ic_16_9_ratio, R.drawable.ic_16_9_ratio_select),
    CollageRatio("3:2", 0, 3, 2, R.drawable.ic_3_2_ratio, R.drawable.ic_3_2_ratio_select),
    CollageRatio("", R.string.collage_ratio_post, 2, 3, R.drawable.ic_pint, R.drawable.ic_pint_select),
    CollageRatio("", R.string.collage_ratio_cover, 16, 9, R.drawable.ic_yt_ratio, R.drawable.ic_yt_select),
    CollageRatio("9:16", 0, 9, 16, R.drawable.ic_9_16_ratio, R.drawable.ic_9_16_ratio_select),
    CollageRatio("3:4", 0, 3, 4, R.drawable.ic_3_4_ratio, R.drawable.ic_3_4_ratio_select),
    CollageRatio("", R.string.collage_ratio_post, 16, 8, R.drawable.ic_x_post, R.drawable.ic_x_post_select),
    CollageRatio("", R.string.collage_ratio_header, 16, 5, R.drawable.ic_x_header, R.drawable.ic_x_header_select),
    CollageRatio("A4", 0, 10, 16, R.drawable.ic_a4_ratio, R.drawable.ic_a4_ratio_select),
    CollageRatio("A5", 0, 10, 8, R.drawable.ic_a5_ratio, R.drawable.ic_a5_ratio_select)
)

/** Grid defaults (CollageScreenActivity 19756-19922) and the code-set slider maxima (the XML maxima are overridden). */
internal const val DEFAULT_RADIUS = 16
internal const val MAX_RADIUS = 200
internal const val DEFAULT_PADDING = 6
internal const val MAX_PADDING = 50
internal const val MAX_BLUR = 25
internal const val MAX_BRUSH = 25
internal const val DEFAULT_BRUSH = 10
internal const val MAX_ROTATE = 100

/** sbPaint / sbErase: the stroke is the slider value plus 10. */
internal fun brushStrokeSize(progress: Int) = progress.coerceIn(0, MAX_BRUSH) + 10

private fun rgb(vararg hex: String) = hex.map { (it.removePrefix("#").toLong(16) or if (it.length == 7) 0xFF000000 else 0L).toInt() }

/** Background "Color" swatches (je/i), the transparent one first. */
internal val COLLAGE_BG_COLORS = listOf(0x00000000) + rgb(
    "#FFFFFF", "#BBBBBB", "#4E4E4E", "#212121", "#F9AB9D", "#EC5C60", "#000000", "#FFD7CD", "#FF0000", "#FFF2CA", "#CB3243",
    "#CD181F", "#FC7F3D", "#ED3F0F", "#FDE472", "#F3AF59", "#FFA4B9", "#FF679F", "#FFF1F1", "#FFE1E4", "#D3A6D8", "#BA66AF",
    "#FB2C78", "#E7D5E7", "#99D2F9", "#81ADEA", "#A53B8E", "#65218C", "#171982", "#A5E7F6", "#2961A9", "#0E2E89", "#058BC0",
    "#08447E", "#7CE3FF", "#00B0D0", "#4DAF9D", "#21887C", "#DEEEE9", "#B3D0C5", "#AACE87", "#A3AF38", "#0F664E", "#D3E5A6",
    "#E4D9C0", "#D6C392", "#6D822B", "#366131", "#3E3129", "#A7A7A7", "#A3815A", "#72462F", "#CCBFD4", "#A67C80", "#995452",
    "#E1D6D7", "#6E5055", "#666563", "#D4D3CF", "#7A7185", "#E1CCD1", "#C3C9DF", "#F0E4FB", "#EED0D5", "#E2E4E1", "#8596A4",
    "#929493", "#807D5C", "#AFB0B2", "#BEC0BF", "#ECECEC", "#96A48C", "#FFFAF7", "#7D8970", "#B7C5B3", "#A0A7BA", "#3EA2D7",
    "#FFA258", "#FF7063", "#FF3274", "#7285F2", "#3ADB7C", "#F22D52", "#F2A0B6", "#0278A6", "#0D8C39", "#F2C4CD", "#129AEF",
    "#4AE3D2", "#00D28E", "#5639A5", "#A482F4", "#730068", "#729D38", "#FA4848", "#434882", "#900C3F", "#FF5733", "#C6E377",
    "#022D3C", "#FFC300"
)

/** Brush and text colours (rj/c). */
internal val COLLAGE_PAINT_COLORS = rgb(
    "#FFFFFF", "#BBBBBB", "#4E4E4E", "#212121", "#000000", "#EC5C60", "#CB3243", "#FFD7CD", "#F9AB9D", "#FFF2CA", "#FDE472",
    "#CD181F", "#FF0000", "#ED3F0F", "#FFF1F1", "#F3AF59", "#FC7F3D", "#FF679F", "#FB2C78", "#FFE1E4", "#FFA4B9", "#BA66AF",
    "#A53B8E", "#E7D5E7", "#D3A6D8", "#81ADEA", "#2961A9", "#65218C", "#99D2F9", "#A5E7F6", "#7CE3FF", "#0E2E89", "#171982",
    "#08447E", "#DEEEE9", "#00B0D0", "#058BC0", "#21887C", "#0F664E", "#B3D0C5", "#4DAF9D", "#A3AF38", "#6D822B", "#D3E5A6",
    "#AACE87", "#D6C392", "#A3815A", "#366131", "#E4D9C0", "#A7A7A7", "#995452", "#72462F", "#3E3129", "#A67C80", "#D4D3CF",
    "#E1D6D7", "#CCBFD4", "#666563", "#F0E4FB", "#7A7185", "#6E5055", "#C3C9DF", "#929493", "#EED0D5", "#E1CCD1", "#8596A4",
    "#ECECEC", "#807D5C", "#E2E4E1", "#BEC0BF", "#B7C5B3", "#96A48C", "#AFB0B2", "#7D8970", "#FF7063", "#A0A7BA", "#FFFAF7",
    "#FFA258", "#F22D52", "#FF3274", "#3EA2D7", "#3ADB7C", "#F2C4CD", "#F2A0B6", "#7285F2", "#0D8C39", "#5639A5", "#129AEF",
    "#0278A6", "#00D28E", "#FA4848", "#A482F4", "#4AE3D2", "#729D38", "#C6E377", "#434882", "#730068", "#FF5733", "#FFC300",
    "#022D3C", "#900C3F"
)

/** layout_adjust_text_section case buttons (lower, Capital Words, UPPER, tOGGLE); AS_TYPED while none is on. */
internal enum class TextCase {
    AS_TYPED, LOWER, CAPITAL, UPPER, TOGGLE;

    fun apply(text: String): String = when (this) {
        AS_TYPED -> text
        LOWER -> text.lowercase()
        UPPER -> text.uppercase()
        CAPITAL -> text.split(' ').joinToString(" ") { w -> w.lowercase().replaceFirstChar { it.uppercaseChar() } }
        TOGGLE -> text.map { if (it.isUpperCase()) it.lowercaseChar() else it.uppercaseChar() }.joinToString("")
    }
}

/** k9/a.OooOO0o: collages land in Pictures/Gallery Collage under a random UUID. */
internal const val COLLAGE_FOLDER = "Gallery Collage"

internal fun collageFileName(id: UUID = UUID.randomUUID()) = "$id.png"
