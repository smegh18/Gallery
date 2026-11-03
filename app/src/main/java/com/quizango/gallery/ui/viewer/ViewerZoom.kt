package com.quizango.gallery.ui.viewer

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size

/** Zoom maths for the photo viewer, kept pure so it can be unit-tested. */
object ViewerZoom {

    /** Reference (PhotoView default) double-tap zoom level. */
    const val DOUBLE_TAP_SCALE = 1.75f
    const val MAX_SCALE = 5f

    /** Size of [image] drawn with ContentScale.Fit inside [container]. */
    fun fittedSize(image: Size, container: Size): Size {
        if (image.width <= 0f || image.height <= 0f) return container
        val s = minOf(container.width / image.width, container.height / image.height)
        return Size(image.width * s, image.height * s)
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
