package com.quizango.gallery.ui.search

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.quizango.gallery.R
import com.quizango.gallery.ui.theme.GalleryText
import kotlin.math.max
import kotlin.math.min

/** The picker opens on this colour (the reference's initial `#007AFF`). */
internal const val PICKER_START_HEX = "#007AFF"

/** The ten `colorSearch1..10` swatches, in order (item_search_color). */
internal val SEARCH_SWATCH_HEX = listOf(
    "#F2F2F2", "#000000", "#FF0000", "#FFE500", "#FF7A00", "#FF00E5", "#9E00FF", "#0066FF", "#00C2FF", "#008625"
)

/** `#RRGGBB` upper-case; the form Search Color receives and compares. */
internal fun hexOf(color: Color): String = "#%06X".format(color.toArgb() and 0xFFFFFF)

internal fun colorOfHex(hex: String): Color = Color(("FF" + hex.removePrefix("#")).toLong(16).toInt())

/** Hue 0..360, saturation and value 0..1 of a `#RRGGBB` colour. */
internal fun hsvOf(hex: String): Triple<Float, Float, Float> {
    val c = colorOfHex(hex)
    val hi = max(c.red, max(c.green, c.blue))
    val lo = min(c.red, min(c.green, c.blue))
    val d = hi - lo
    val hue = when {
        d == 0f -> 0f
        hi == c.red -> 60f * (((c.green - c.blue) / d) % 6f)
        hi == c.green -> 60f * ((c.blue - c.red) / d + 2f)
        else -> 60f * ((c.red - c.green) / d + 4f)
    }
    return Triple((hue + 360f) % 360f, if (hi == 0f) 0f else d / hi, hi)
}

/** "Choose color": a preview and hue / saturation / value sliders, Cancel and Ok; Ok returns the chosen `#RRGGBB`. */
@Composable
internal fun SearchColorPickerDialog(onChosen: (String) -> Unit, onDismiss: () -> Unit) {
    val start = remember { hsvOf(PICKER_START_HEX) }
    var hue by remember { mutableFloatStateOf(start.first) }
    var sat by remember { mutableFloatStateOf(start.second) }
    var value by remember { mutableFloatStateOf(start.third) }
    val chosen = Color.hsv(hue, sat, value)
    val primary = colorResource(R.color.colorPrimary)

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = colorResource(R.color.backgroundColor),
        title = { Text(stringResource(R.string.choose_color), style = GalleryText.BodyLargeSemiBold, color = colorResource(R.color.titleColor)) },
        text = {
            Column {
                Spacer(
                    Modifier.fillMaxWidth().height(48.dp)
                        .background(chosen, RoundedCornerShape(10.dp))
                        .border(1.dp, colorResource(R.color.dividerColor), RoundedCornerShape(10.dp))
                )
                Spacer(Modifier.height(12.dp))
                val colors = SliderDefaults.colors(thumbColor = primary, activeTrackColor = primary)
                Slider(hue, { hue = it }, valueRange = 0f..360f, colors = colors, modifier = Modifier.padding(vertical = 2.dp))
                Slider(sat, { sat = it }, colors = colors, modifier = Modifier.padding(vertical = 2.dp))
                Slider(value, { value = it }, colors = colors, modifier = Modifier.padding(vertical = 2.dp))
            }
        },
        confirmButton = {
            TextButton({ onChosen(hexOf(chosen)) }) { Text(stringResource(R.string.ok), color = primary) }
        },
        dismissButton = {
            TextButton(onDismiss) { Text(stringResource(R.string.cancel), color = primary) }
        }
    )
}
