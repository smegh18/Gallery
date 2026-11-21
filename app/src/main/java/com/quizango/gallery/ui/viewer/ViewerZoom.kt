package com.quizango.gallery.ui.viewer

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size

/** Zoom maths for the photo viewer, kept pure so it can be unit-tested. */
object ViewerZoom {

    const val FIT_SCALE = 1f
    const val OVERZOOM_FACTOR = 2f
    const val DOUBLE_TAP_DURATION_MS = 200

    /** Size of [image] drawn with ContentScale.Fit inside [container]. */
    fun fittedSize(image: Size, container: Size): Size {
        if (image.width <= 0f || image.height <= 0f) return container
        val s = minOf(container.width / image.width, container.height / image.height)
        return Size(image.width * s, image.height * s)
    }

    /**
     * The reference chooses a maximum per decoded image rather than a global constant.
     * Animated images use their shorter side; still landscape photos stop at 2x.
     */
    fun maxZoomFor(width: Float, height: Float, isAnimated: Boolean): Float {
        if (isAnimated) {
            return when (minOf(width, height)) {
                in Float.NEGATIVE_INFINITY..150f -> 10f
                in 150f..250f -> 6f
                in 250f..350f -> 5f
                in 350f..450f -> 4f
                else -> 3f
            }
        }
        return if (width > height) 2f else 5f
    }

    /** Keeps the zoomed photo's edges from moving inside the screen (no panning into empty bars). */
    fun clamp(offset: Offset, scale: Float, fitted: Size, container: Size): Offset {
        val maxX = ((fitted.width * scale - container.width) / 2f).coerceAtLeast(0f)
        val maxY = ((fitted.height * scale - container.height) / 2f).coerceAtLeast(0f)
        return Offset(offset.x.coerceIn(-maxX, maxX), offset.y.coerceIn(-maxY, maxY))
    }

    /** Offset that keeps the tapped point under the finger after zooming from 1x to [scale]. */
    fun offsetForTap(tap: Offset, scale: Float, fitted: Size, container: Size): Offset {
        val center = Offset(container.width / 2f, container.height / 2f)
        return clamp((center - tap) * (scale - 1f), scale, fitted, container)
    }
}
