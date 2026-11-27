package com.quizango.gallery.ui.creative

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.window.Dialog
import com.quizango.gallery.R
import com.quizango.gallery.ui.theme.GalleryText
import com.quizango.gallery.ui.theme.sdp
import kotlin.math.PI
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.roundToInt
import kotlin.math.sin

/**
 * The "Choose color" picker (S272 ge/cd, flask builder: CIRCLE wheel of density 12 with a lightness slider, Ok / Cancel).
 * Hue runs round the wheel, saturation outwards, the slider sets the lightness.
 */
@Composable
internal fun ColorPickerDialog(initial: Color, onPick: (Color) -> Unit, onDismiss: () -> Unit) {
    val hsv = remember { FloatArray(3).also { android.graphics.Color.colorToHSV(initial.toArgb(), it) } }
    var hue by remember { mutableFloatStateOf(hsv[0]) }
    var sat by remember { mutableFloatStateOf(hsv[1]) }
    var value by remember { mutableFloatStateOf(hsv[2]) }
    val picked = Color(android.graphics.Color.HSVToColor(floatArrayOf(hue, sat, value)))
    var wheelSize by remember { mutableStateOf(0f) }
    val marker = colorResource(R.color.onlyWhite)

    fun pickAt(o: Offset) {
        val c = wheelSize / 2
        val dx = o.x - c
        val dy = o.y - c
        hue = ((atan2(dy, dx) * 180 / PI).toFloat() + 360) % 360
        sat = (hypot(dx, dy) / c).coerceIn(0f, 1f)
    }

    Dialog(onDismissRequest = onDismiss) {
        Column(
            Modifier
                .fillMaxWidth()
                .background(colorResource(R.color.dialogBack), RoundedCornerShape(14.sdp))
                .padding(20.sdp)
        ) {
            Text(stringResource(R.string.choose_color), style = GalleryText.BodyXLargeSemiBold, color = colorResource(R.color.titleColor))
            Canvas(
                Modifier
                    .padding(top = 15.sdp)
                    .fillMaxWidth()
                    .aspectRatio(1f)
                    .pointerInput(Unit) { detectTapGestures { pickAt(it) } }
                    .pointerInput(Unit) { detectDragGestures { change, _ -> pickAt(change.position) } }
            ) {
                wheelSize = size.width
                val radius = size.width / 2
                val step = radius / (DENSITY - 1)
                for (ring in 0 until DENSITY) {
                    val r = ring * step
                    val dots = if (ring == 0) 1 else ((2 * PI * r) / step).roundToInt()
                    for (k in 0 until dots) {
                        val a = 2 * PI * k / dots
                        val dotColor = Color(android.graphics.Color.HSVToColor(floatArrayOf((a * 180 / PI).toFloat(), r / radius, value)))
                        drawCircle(dotColor, step * 0.45f, Offset(radius + (r * cos(a)).toFloat(), radius + (r * sin(a)).toFloat()))
                    }
                }
                val a = hue * PI / 180
                val at = Offset(radius + (sat * radius * cos(a)).toFloat(), radius + (sat * radius * sin(a)).toFloat())
                drawCircle(marker, step * 0.6f, at, style = Stroke(step * 0.15f))
            }
            Canvas(
                Modifier
                    .padding(top = 15.sdp)
                    .fillMaxWidth()
                    .height(24.sdp)
                    .pointerInput(Unit) { detectTapGestures { value = (it.x / size.width).coerceIn(0f, 1f) } }
                    .pointerInput(Unit) { detectDragGestures { change, _ -> value = (change.position.x / size.width).coerceIn(0f, 1f) } }
            ) {
                val full = Color(android.graphics.Color.HSVToColor(floatArrayOf(hue, sat, 1f)))
                drawRect(Brush.horizontalGradient(listOf(Color.Black, full))) // HSV value 0 to 1
                drawCircle(marker, size.height / 2, Offset(value * size.width, size.height / 2), style = Stroke(size.height / 8))
            }
            Row(Modifier.fillMaxWidth().padding(top = 15.sdp), horizontalArrangement = Arrangement.End) {
                DialogTextButton(R.string.cancel, onDismiss)
                DialogTextButton(R.string.ok) { onPick(picked) }
            }
        }
    }
}

@Composable
private fun DialogTextButton(text: Int, onClick: () -> Unit) {
    Text(
        text = stringResource(text),
        style = GalleryText.BodyLargeSemiBold,
        color = colorResource(R.color.colorPrimary),
        modifier = Modifier.clickable(onClick = onClick).padding(horizontal = 10.sdp, vertical = 6.sdp)
    )
}

private const val DENSITY = 12
