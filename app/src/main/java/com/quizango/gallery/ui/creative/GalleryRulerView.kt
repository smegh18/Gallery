package com.quizango.gallery.ui.creative

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp

/**
 * Custom interactive RulerView matching the reference APK `RulerView.java`.
 * Features a fixed center needle indicator (`#0066FF`) and a horizontally scrollable tick scale (`#E0E0E0`).
 */
@Composable
fun GalleryRulerView(
    value: Int,
    onValueChange: (Int) -> Unit,
    minValue: Int = 0,
    maxValue: Int = 100,
    modifier: Modifier = Modifier,
    middleLineColor: Color = Color(0xFF0066FF),
    stickColor: Color = Color(0xFFE0E0E0)
) {
    val totalRange = (maxValue - minValue).coerceAtLeast(1)
    val tickSpacingPx = 32f // Pixels per unit/tick interval

    var dragAccumulator by remember { mutableFloatStateOf(0f) }

    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(42.dp)
            .pointerInput(value, minValue, maxValue) {
                detectDragGestures(
                    onDragStart = { dragAccumulator = 0f },
                    onDrag = { change, dragAmount ->
                        change.consume()
                        dragAccumulator -= dragAmount.x
                        val deltaTicks = (dragAccumulator / tickSpacingPx).toInt()
                        if (deltaTicks != 0) {
                            val newValue = (value + deltaTicks).coerceIn(minValue, maxValue)
                            if (newValue != value) {
                                onValueChange(newValue)
                                dragAccumulator -= deltaTicks * tickSpacingPx
                            }
                        }
                    }
                )
            }
            .pointerInput(value, minValue, maxValue) {
                detectTapGestures { offset ->
                    val centerX = size.width / 2f
                    val dx = offset.x - centerX
                    val deltaTicks = (dx / tickSpacingPx).toInt()
                    val newValue = (value + deltaTicks).coerceIn(minValue, maxValue)
                    onValueChange(newValue)
                }
            }
    ) {
        val width = size.width
        val height = size.height
        val centerX = width / 2f
        val centerY = height / 2f

        // Draw moving ruler ticks
        val visibleHalfTicks = (centerX / tickSpacingPx).toInt() + 2
        for (i in -visibleHalfTicks..visibleHalfTicks) {
            val tickValue = value + i
            if (tickValue in minValue..maxValue) {
                val tickX = centerX + (i * tickSpacingPx) - (dragAccumulator % tickSpacingPx)
                if (tickX in 0f..width) {
                    val isMajor = tickValue % 5 == 0
                    val tickHeight = if (isMajor) 22.dp.toPx() else 13.dp.toPx()
                    val tickAlpha = ((1f - kotlin.math.abs(tickX - centerX) / centerX).coerceIn(0.15f, 1f))
                    val strokeW = if (isMajor) 1.8.dp.toPx() else 1.2.dp.toPx()

                    drawLine(
                        color = stickColor.copy(alpha = tickAlpha),
                        start = Offset(tickX, centerY - tickHeight / 2f),
                        end = Offset(tickX, centerY + tickHeight / 2f),
                        strokeWidth = strokeW,
                        cap = StrokeCap.Round
                    )
                }
            }
        }

        // Draw center fixed needle indicator
        val needleHeight = 30.dp.toPx()
        drawLine(
            color = middleLineColor,
            start = Offset(centerX, centerY - needleHeight / 2f),
            end = Offset(centerX, centerY + needleHeight / 2f),
            strokeWidth = 2.8.dp.toPx(),
            cap = StrokeCap.Round
        )
    }
}
