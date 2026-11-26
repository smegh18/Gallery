package com.quizango.gallery.ui.editor

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Shader
import android.graphics.Typeface
import android.text.Layout
import android.text.StaticLayout
import android.text.TextPaint
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.layout
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.quizango.gallery.R
import com.quizango.gallery.ui.home.drawableBackground
import com.quizango.gallery.ui.theme.GalleryText
import com.quizango.gallery.ui.theme.sdp
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.sin

/**
 * Stand-ins for the eight assets/font faces of the 2.7.2 text tool (Amatic, American Typewriter, Bebas Neue,
 * Cocogoose x2, Metropolis, Raleway, TT Commons): third-party font files are not copied, so platform families are used.
 */
internal val EDITOR_FONTS: List<Typeface> by lazy {
    listOf(
        Typeface.create("sans-serif-condensed", Typeface.BOLD),
        Typeface.create(Typeface.SERIF, Typeface.BOLD),
        Typeface.create("sans-serif-black", Typeface.NORMAL),
        Typeface.create("casual", Typeface.BOLD),
        Typeface.create("sans-serif-thin", Typeface.NORMAL),
        Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD),
        Typeface.create("cursive", Typeface.BOLD),
        Typeface.create(Typeface.MONOSPACE, Typeface.BOLD)
    )
}

/** textGradientLayout: two end colours and the slider's split point. */
internal data class TextGradient(val start: Int, val end: Int, val split: Float)

/**
 * One text_art sticker. [cx]/[cy] are its centre as fractions of the shown image, [scale] multiplies [TEXT_BASE],
 * [align] is fragment_text's 0 left / 1 centre / 2 right.
 */
internal data class TextItem(
    val id: Long,
    val text: String,
    val color: Int,
    val align: Int = 1,
    val gradient: TextGradient? = null,
    val font: Int = 0,
    val opacity: Float = 1f,
    val shadow: Float = 0f,
    val cx: Float = 0.5f,
    val cy: Float = 0.5f,
    val scale: Float = 1f,
    val rotation: Float = 0f
)

internal val TEXT_BASE = 30.sp // shown size at scale 1
private val TEXT_INSET = 12.dp // outring / textviewart margins
private const val MIN_SCALE = 0.3f
private const val MAX_SCALE = 6f

/** A sticker part under a touch: its body or one of the four text_art corner buttons. */
internal enum class TextHit { BODY, CLOSE, ROTATE, ZOOM, EDIT }

/**
 * Which part of a sticker of [w] x [h] px centred at [center], turned [rotationDeg], the point [p] hits;
 * corner buttons of [handle] px count only on the selected sticker.
 */
internal fun textHit(p: Offset, center: Offset, w: Float, h: Float, rotationDeg: Float, handle: Float, selected: Boolean): TextHit? {
    val rad = Math.toRadians(-rotationDeg.toDouble())
    val dx = p.x - center.x
    val dy = p.y - center.y
    val lx = (dx * cos(rad) - dy * sin(rad)).toFloat()
    val ly = (dx * sin(rad) + dy * cos(rad)).toFloat()
    val hw = w / 2
    val hh = h / 2
    if (selected) {
        fun near(x: Float, y: Float) = abs(lx - x) <= handle && abs(ly - y) <= handle
        val c = handle / 2
        when {
            near(-hw + c, -hh + c) -> return TextHit.CLOSE
            near(hw - c, -hh + c) -> return TextHit.ROTATE
            near(hw - c, hh - c) -> return TextHit.ZOOM
            near(-hw + c, hh - c) -> return TextHit.EDIT
        }
    }
    return if (abs(lx) <= hw && abs(ly) <= hh) TextHit.BODY else null
}

/** The zoom button scales by how far the finger moved from the centre. */
internal fun zoomedScale(start: Float, center: Offset, from: Offset, to: Offset): Float {
    val d0 = hypot(from.x - center.x, from.y - center.y)
    if (d0 < 1f) return start
    return (start * hypot(to.x - center.x, to.y - center.y) / d0).coerceIn(MIN_SCALE, MAX_SCALE)
}

/** The rotate button turns by the angle the finger swept around the centre. */
internal fun rotatedBy(start: Float, center: Offset, from: Offset, to: Offset): Float {
    val a0 = atan2(from.y - center.y, from.x - center.x)
    val a1 = atan2(to.y - center.y, to.x - center.x)
    return start + Math.toDegrees((a1 - a0).toDouble()).toFloat()
}

private fun TextItem.composeAlign() = when (align) {
    0 -> TextAlign.Start
    2 -> TextAlign.End
    else -> TextAlign.Center
}

@Composable
private fun TextItem.style(shadowColor: Color): TextStyle {
    val g = gradient
    val base = TextStyle(
        fontFamily = FontFamily(EDITOR_FONTS[font]),
        fontSize = TEXT_BASE * scale,
        textAlign = composeAlign(),
        shadow = if (shadow != 0f) Shadow(shadowColor, Offset(shadow, shadow), abs(shadow)) else null
    )
    return if (g != null) base.copy(brush = Brush.horizontalGradient(0f to Color(g.start), g.split.coerceIn(0.01f, 1f) to Color(g.end)), alpha = opacity)
    else base.copy(color = Color(color).copy(alpha = opacity))
}

/**
 * text_art: the text inside the back_edit outline with close, rotate, zoom and edit at the corners while selected.
 * Gestures are handled by the overlay that owns all stickers; this only draws, centred on [center].
 */
@Composable
internal fun TextSticker(item: TextItem, center: Offset, selected: Boolean, onMeasured: (Int, Int) -> Unit) {
    val handle = 22.sdp
    Box(
        Modifier
            .layout { measurable, constraints ->
                val p = measurable.measure(constraints.copy(minWidth = 0, minHeight = 0))
                onMeasured(p.width, p.height)
                layout(constraints.maxWidth, constraints.maxHeight) {
                    p.place((center.x - p.width / 2f).toInt(), (center.y - p.height / 2f).toInt())
                }
            }
            .graphicsLayer { rotationZ = item.rotation }
    ) {
        Box(
            Modifier
                .padding(TEXT_INSET)
                .then(if (selected) Modifier.border(2.dp, colorResource(R.color.dark_blue_trans), RoundedCornerShape(6.sdp)) else Modifier)
                .padding(TEXT_INSET)
        ) {
            Text(item.text, style = item.style(colorResource(R.color.blackTran1)))
        }
        if (selected) {
            CornerButton(R.drawable.ic_text_close, Alignment.TopStart, handle)
            CornerButton(R.drawable.ic_text_rotate, Alignment.TopEnd, handle)
            CornerButton(R.drawable.ic_text_resize, Alignment.BottomEnd, handle)
            CornerButton(R.drawable.ic_text_edit, Alignment.BottomStart, handle)
        }
    }
}

@Composable
private fun androidx.compose.foundation.layout.BoxScope.CornerButton(icon: Int, alignment: Alignment, size: Dp) {
    Image(painterResource(icon), contentDescription = null, modifier = Modifier.align(alignment).size(size))
}

/** Draws the stickers onto a copy of [src]; [shownWidth] is the on-screen image width the sizes were set against. */
internal fun renderTexts(src: Bitmap, items: List<TextItem>, shownWidth: Float, density: Float, fontScale: Float, shadowColor: Int): Bitmap {
    val out = src.copy(Bitmap.Config.ARGB_8888, true)
    val canvas = Canvas(out)
    val k = src.width / shownWidth
    items.filter { it.text.isNotBlank() }.forEach { t ->
        val paint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            typeface = EDITOR_FONTS[t.font]
            textSize = TEXT_BASE.value * fontScale * density * t.scale * k
            color = t.color
            alpha = (t.opacity * 255).toInt()
            if (t.shadow != 0f) setShadowLayer(abs(t.shadow) * density * k, t.shadow * density * k, t.shadow * density * k, shadowColor)
        }
        val width = t.text.lines().maxOf { paint.measureText(it) }.toInt().coerceAtLeast(1)
        t.gradient?.let { g ->
            paint.shader = LinearGradient(0f, 0f, width.toFloat(), 0f, intArrayOf(g.start, g.end), floatArrayOf(0f, g.split.coerceIn(0.01f, 1f)), Shader.TileMode.CLAMP)
        }
        val align = when (t.align) {
            0 -> Layout.Alignment.ALIGN_NORMAL
            2 -> Layout.Alignment.ALIGN_OPPOSITE
            else -> Layout.Alignment.ALIGN_CENTER
        }
        val layout = StaticLayout.Builder.obtain(t.text, 0, t.text.length, paint, width).setAlignment(align).build()
        canvas.save()
        canvas.translate(t.cx * out.width, t.cy * out.height)
        canvas.rotate(t.rotation)
        canvas.translate(-width / 2f, -layout.height / 2f)
        layout.draw(canvas)
        canvas.restore()
    }
    return out
}

/**
 * fragment_text (cx/f): the full-screen #80000000 input with the three alignment buttons, Done, and a
 * 40sp white field of up to 100 characters. Done returns the text and alignment; Back cancels.
 */
@Composable
internal fun TextInputDialog(initial: String, initialAlign: Int, onDone: (String, Int) -> Unit, onDismiss: () -> Unit) {
    var text by remember { mutableStateOf(initial) }
    var align by remember { mutableIntStateOf(initialAlign) }
    val focus = remember { FocusRequester() }
    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false)) {
        BackHandler(onBack = onDismiss)
        Box(Modifier.fillMaxSize().background(colorResource(R.color.blackTran1)).statusBarsPadding().imePadding()) {
            Box(Modifier.align(Alignment.TopCenter).padding(top = 10.sdp).width(110.sdp).drawableBackground(R.drawable.ic_text_line_bg)) {
                listOf(Alignment.CenterStart to R.drawable.ic_text_align_left, Alignment.Center to R.drawable.ic_text_align_center, Alignment.CenterEnd to R.drawable.ic_text_align_right)
                    .forEachIndexed { i, (where, icon) ->
                        val on = align == i
                        Image(
                            painterResource(icon),
                            contentDescription = null,
                            colorFilter = if (on) ColorFilter.tint(colorResource(R.color.onlyWhite)) else null,
                            modifier = Modifier
                                .align(where)
                                .padding(horizontal = 5.sdp, vertical = 3.sdp)
                                .then(if (on) Modifier.drawableBackground(R.drawable.ic_plus_text_gravity_select) else Modifier)
                                .borderlessClick { align = i }
                                .padding(5.sdp)
                        )
                    }
            }
            Text(
                stringResource(R.string.done),
                style = GalleryText.BodyMediumSemiBold,
                color = colorResource(R.color.black),
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(top = 13.sdp, end = 10.sdp)
                    .drawableBackground(R.drawable.ic_text_button)
                    .borderlessClick { onDone(text.trim(), align) }
                    .padding(horizontal = 12.sdp, vertical = 6.sdp)
            )
            BasicTextField(
                value = text,
                onValueChange = { if (it.length <= 100) text = it },
                textStyle = TextStyle(
                    fontSize = 40.sp,
                    color = colorResource(R.color.onlyWhite),
                    textAlign = when (align) { 0 -> TextAlign.Start; 2 -> TextAlign.End; else -> TextAlign.Center }
                ),
                cursorBrush = SolidColor(colorResource(R.color.onlyWhite)),
                modifier = Modifier.align(Alignment.Center).fillMaxWidth().padding(horizontal = 10.sdp).focusRequester(focus)
            )
        }
    }
    LaunchedEffect(Unit) { focus.requestFocus() }
}
