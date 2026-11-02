package com.quizango.gallery.ui.editor.crop

import android.graphics.Bitmap
import android.graphics.Matrix
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import com.quizango.gallery.ui.editor.CropRatio
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

/**
 * Handle positions for interactive cropping box matching reference APK CanHub CropOverlayView.
 */
enum class CropHandle {
    TOP_LEFT,
    TOP_RIGHT,
    BOTTOM_LEFT,
    BOTTOM_RIGHT,
    LEFT,
    TOP,
    RIGHT,
    BOTTOM,
    CENTER
}

/**
 * Pure geometry utilities for aspect ratio calculations, handle hit-testing, and bitmap cropping.
 */
object CropGeometry {

    /**
     * Calculates the initial crop rectangle fitted inside [imageBounds] according to [aspectX] and [aspectY].
     * If either aspect is <= 0 (Free mode), returns [imageBounds].
     */
    fun calculateAspectCropRect(
        imageBounds: Rect,
        aspectX: Float,
        aspectY: Float
    ): Rect {
        if (aspectX <= 0f || aspectY <= 0f || imageBounds.width <= 0f || imageBounds.height <= 0f) {
            return imageBounds
        }
        val targetRatio = aspectX / aspectY
        val imageRatio = imageBounds.width / imageBounds.height
        val cropW: Float
        val cropH: Float
        if (targetRatio > imageRatio) {
            cropW = imageBounds.width
            cropH = (cropW / targetRatio).coerceAtMost(imageBounds.height)
        } else {
            cropH = imageBounds.height
            cropW = (cropH * targetRatio).coerceAtMost(imageBounds.width)
        }
        val left = imageBounds.left + (imageBounds.width - cropW) / 2f
        val top = imageBounds.top + (imageBounds.height - cropH) / 2f
        return Rect(left, top, left + cropW, top + cropH)
    }

    /**
     * Determines which crop handle or interior region [touchPoint] hits.
     */
    fun hitTest(
        touchPoint: Offset,
        cropRect: Rect,
        touchRadiusPx: Float
    ): CropHandle? {
        val radiusSq = touchRadiusPx * touchRadiusPx

        // 1. Check 4 corners (highest priority)
        if ((touchPoint - cropRect.topLeft).getDistanceSquared() <= radiusSq) return CropHandle.TOP_LEFT
        if ((touchPoint - cropRect.topRight).getDistanceSquared() <= radiusSq) return CropHandle.TOP_RIGHT
        if ((touchPoint - cropRect.bottomLeft).getDistanceSquared() <= radiusSq) return CropHandle.BOTTOM_LEFT
        if ((touchPoint - cropRect.bottomRight).getDistanceSquared() <= radiusSq) return CropHandle.BOTTOM_RIGHT

        // 2. Check 4 edges
        val isYInRange = touchPoint.y >= cropRect.top - touchRadiusPx && touchPoint.y <= cropRect.bottom + touchRadiusPx
        val isXInRange = touchPoint.x >= cropRect.left - touchRadiusPx && touchPoint.x <= cropRect.right + touchRadiusPx

        if (isYInRange && abs(touchPoint.x - cropRect.left) <= touchRadiusPx) return CropHandle.LEFT
        if (isYInRange && abs(touchPoint.x - cropRect.right) <= touchRadiusPx) return CropHandle.RIGHT
        if (isXInRange && abs(touchPoint.y - cropRect.top) <= touchRadiusPx) return CropHandle.TOP
        if (isXInRange && abs(touchPoint.y - cropRect.bottom) <= touchRadiusPx) return CropHandle.BOTTOM

        // 3. Check inside (drag crop box)
        if (cropRect.contains(touchPoint)) return CropHandle.CENTER

        return null
    }

    /**
     * Updates [currentRect] based on the dragged [handle] and [dragDelta], respecting [bounds] and aspect ratio.
     */
    fun updateCropRect(
        currentRect: Rect,
        handle: CropHandle,
        dragDelta: Offset,
        bounds: Rect,
        aspectX: Float,
        aspectY: Float,
        minSizePx: Float = 60f
    ): Rect {
        val isFixedRatio = aspectX > 0f && aspectY > 0f
        val ratio = if (isFixedRatio) aspectX / aspectY else 1f

        when (handle) {
            CropHandle.CENTER -> {
                var newLeft = currentRect.left + dragDelta.x
                var newTop = currentRect.top + dragDelta.y

                if (newLeft < bounds.left) newLeft = bounds.left
                if (newLeft + currentRect.width > bounds.right) newLeft = bounds.right - currentRect.width

                if (newTop < bounds.top) newTop = bounds.top
                if (newTop + currentRect.height > bounds.bottom) newTop = bounds.bottom - currentRect.height

                return Rect(newLeft, newTop, newLeft + currentRect.width, newTop + currentRect.height)
            }

            CropHandle.LEFT -> {
                if (isFixedRatio) {
                    val newLeft = (currentRect.left + dragDelta.x).coerceIn(bounds.left, currentRect.right - minSizePx)
                    val newW = currentRect.right - newLeft
                    val newH = (newW / ratio).coerceAtMost(bounds.height)
                    val cy = currentRect.center.y
                    val top = (cy - newH / 2f).coerceIn(bounds.top, bounds.bottom - newH)
                    return Rect(newLeft, top, currentRect.right, top + newH)
                } else {
                    val newLeft = (currentRect.left + dragDelta.x).coerceIn(bounds.left, currentRect.right - minSizePx)
                    return Rect(newLeft, currentRect.top, currentRect.right, currentRect.bottom)
                }
            }

            CropHandle.RIGHT -> {
                if (isFixedRatio) {
                    val newRight = (currentRect.right + dragDelta.x).coerceIn(currentRect.left + minSizePx, bounds.right)
                    val newW = newRight - currentRect.left
                    val newH = (newW / ratio).coerceAtMost(bounds.height)
                    val cy = currentRect.center.y
                    val top = (cy - newH / 2f).coerceIn(bounds.top, bounds.bottom - newH)
                    return Rect(currentRect.left, top, newRight, top + newH)
                } else {
                    val newRight = (currentRect.right + dragDelta.x).coerceIn(currentRect.left + minSizePx, bounds.right)
                    return Rect(currentRect.left, currentRect.top, newRight, currentRect.bottom)
                }
            }

            CropHandle.TOP -> {
                if (isFixedRatio) {
                    val newTop = (currentRect.top + dragDelta.y).coerceIn(bounds.top, currentRect.bottom - minSizePx)
                    val newH = currentRect.bottom - newTop
                    val newW = (newH * ratio).coerceAtMost(bounds.width)
                    val cx = currentRect.center.x
                    val left = (cx - newW / 2f).coerceIn(bounds.left, bounds.right - newW)
                    return Rect(left, newTop, left + newW, currentRect.bottom)
                } else {
                    val newTop = (currentRect.top + dragDelta.y).coerceIn(bounds.top, currentRect.bottom - minSizePx)
                    return Rect(currentRect.left, newTop, currentRect.right, currentRect.bottom)
                }
            }

            CropHandle.BOTTOM -> {
                if (isFixedRatio) {
                    val newBottom = (currentRect.bottom + dragDelta.y).coerceIn(currentRect.top + minSizePx, bounds.bottom)
                    val newH = newBottom - currentRect.top
                    val newW = (newH * ratio).coerceAtMost(bounds.width)
                    val cx = currentRect.center.x
                    val left = (cx - newW / 2f).coerceIn(bounds.left, bounds.right - newW)
                    return Rect(left, currentRect.top, left + newW, newBottom)
                } else {
                    val newBottom = (currentRect.bottom + dragDelta.y).coerceIn(currentRect.top + minSizePx, bounds.bottom)
                    return Rect(currentRect.left, currentRect.top, currentRect.right, newBottom)
                }
            }

            CropHandle.BOTTOM_RIGHT -> {
                if (isFixedRatio) {
                    val maxW = bounds.right - currentRect.left
                    val maxH = bounds.bottom - currentRect.top
                    val desiredW = currentRect.width + dragDelta.x
                    val desiredH = currentRect.height + dragDelta.y
                    val candidateW = max(desiredW, desiredH * ratio)
                    val finalW = candidateW.coerceIn(minSizePx, min(maxW, maxH * ratio))
                    val finalH = finalW / ratio
                    return Rect(currentRect.left, currentRect.top, currentRect.left + finalW, currentRect.top + finalH)
                } else {
                    val newRight = (currentRect.right + dragDelta.x).coerceIn(currentRect.left + minSizePx, bounds.right)
                    val newBottom = (currentRect.bottom + dragDelta.y).coerceIn(currentRect.top + minSizePx, bounds.bottom)
                    return Rect(currentRect.left, currentRect.top, newRight, newBottom)
                }
            }

            CropHandle.BOTTOM_LEFT -> {
                if (isFixedRatio) {
                    val maxW = currentRect.right - bounds.left
                    val maxH = bounds.bottom - currentRect.top
                    val desiredW = currentRect.width - dragDelta.x
                    val desiredH = currentRect.height + dragDelta.y
                    val candidateW = max(desiredW, desiredH * ratio)
                    val finalW = candidateW.coerceIn(minSizePx, min(maxW, maxH * ratio))
                    val finalH = finalW / ratio
                    return Rect(currentRect.right - finalW, currentRect.top, currentRect.right, currentRect.top + finalH)
                } else {
                    val newLeft = (currentRect.left + dragDelta.x).coerceIn(bounds.left, currentRect.right - minSizePx)
                    val newBottom = (currentRect.bottom + dragDelta.y).coerceIn(currentRect.top + minSizePx, bounds.bottom)
                    return Rect(newLeft, currentRect.top, currentRect.right, newBottom)
                }
            }

            CropHandle.TOP_RIGHT -> {
                if (isFixedRatio) {
                    val maxW = bounds.right - currentRect.left
                    val maxH = currentRect.bottom - bounds.top
                    val desiredW = currentRect.width + dragDelta.x
                    val desiredH = currentRect.height - dragDelta.y
                    val candidateW = max(desiredW, desiredH * ratio)
                    val finalW = candidateW.coerceIn(minSizePx, min(maxW, maxH * ratio))
                    val finalH = finalW / ratio
                    return Rect(currentRect.left, currentRect.bottom - finalH, currentRect.left + finalW, currentRect.bottom)
                } else {
                    val newRight = (currentRect.right + dragDelta.x).coerceIn(currentRect.left + minSizePx, bounds.right)
                    val newTop = (currentRect.top + dragDelta.y).coerceIn(bounds.top, currentRect.bottom - minSizePx)
                    return Rect(currentRect.left, newTop, newRight, currentRect.bottom)
                }
            }

            CropHandle.TOP_LEFT -> {
                if (isFixedRatio) {
                    val maxW = currentRect.right - bounds.left
                    val maxH = currentRect.bottom - bounds.top
                    val desiredW = currentRect.width - dragDelta.x
                    val desiredH = currentRect.height - dragDelta.y
                    val candidateW = max(desiredW, desiredH * ratio)
                    val finalW = candidateW.coerceIn(minSizePx, min(maxW, maxH * ratio))
                    val finalH = finalW / ratio
                    return Rect(currentRect.right - finalW, currentRect.bottom - finalH, currentRect.right, currentRect.bottom)
                } else {
                    val newLeft = (currentRect.left + dragDelta.x).coerceIn(bounds.left, currentRect.right - minSizePx)
                    val newTop = (currentRect.top + dragDelta.y).coerceIn(bounds.top, currentRect.bottom - minSizePx)
                    return Rect(newLeft, newTop, currentRect.right, currentRect.bottom)
                }
            }
        }
    }

    /**
     * Applies rotation and flip transformations to [source], maps [cropRect] to pixel space, and extracts the crop.
     */
    fun cropBitmap(
        source: Bitmap,
        rotationDegrees: Float,
        flipX: Boolean,
        flipY: Boolean,
        cropRect: Rect,
        imageBounds: Rect
    ): Bitmap {
        val matrix = Matrix()
        if (flipX) matrix.postScale(-1f, 1f)
        if (flipY) matrix.postScale(1f, -1f)
        if (rotationDegrees != 0f) matrix.postRotate(rotationDegrees)

        val transformed = if (flipX || flipY || rotationDegrees != 0f) {
            Bitmap.createBitmap(source, 0, 0, source.width, source.height, matrix, true)
        } else {
            source
        }

        if (imageBounds.width <= 0f || imageBounds.height <= 0f) {
            return transformed
        }

        val scaleX = transformed.width.toFloat() / imageBounds.width
        val scaleY = transformed.height.toFloat() / imageBounds.height

        val relLeft = (cropRect.left - imageBounds.left).coerceAtLeast(0f)
        val relTop = (cropRect.top - imageBounds.top).coerceAtLeast(0f)
        val relRight = (cropRect.right - imageBounds.left).coerceAtMost(imageBounds.width)
        val relBottom = (cropRect.bottom - imageBounds.top).coerceAtMost(imageBounds.height)

        val pxLeft = (relLeft * scaleX).toInt().coerceIn(0, transformed.width - 1)
        val pxTop = (relTop * scaleY).toInt().coerceIn(0, transformed.height - 1)
        val pxWidth = ((relRight - relLeft) * scaleX).toInt().coerceIn(1, transformed.width - pxLeft)
        val pxHeight = ((relBottom - relTop) * scaleY).toInt().coerceIn(1, transformed.height - pxTop)

        return Bitmap.createBitmap(transformed, pxLeft, pxTop, pxWidth, pxHeight)
    }
}

/**
 * Interactive Jetpack Compose crop overlay component matching CanHub CropImageView and reference APK item_crop_layout.
 */
@Composable
fun ImageCropView(
    bitmap: Bitmap,
    rotationDegrees: Float,
    flipX: Boolean,
    flipY: Boolean,
    selectedRatio: CropRatio,
    modifier: Modifier = Modifier,
    onCropRectChanged: (Rect, Rect) -> Unit = { _, _ -> }
) {
    val density = LocalDensity.current
    val touchRadiusPx = remember(density) { with(density) { 32.dp.toPx() } }
    val cornerLenPx = remember(density) { with(density) { 22.dp.toPx() } }
    val handleThicknessPx = remember(density) { with(density) { 3.5.dp.toPx() } }
    val borderThicknessPx = remember(density) { with(density) { 1.5.dp.toPx() } }
    val gridThicknessPx = remember(density) { with(density) { 1.dp.toPx() } }

    // Pre-transformed bitmap according to in-crop rotation and flips
    val transformedBitmap = remember(bitmap, rotationDegrees, flipX, flipY) {
        val matrix = Matrix()
        if (flipX) matrix.postScale(-1f, 1f)
        if (flipY) matrix.postScale(1f, -1f)
        if (rotationDegrees != 0f) matrix.postRotate(rotationDegrees)
        if (flipX || flipY || rotationDegrees != 0f) {
            Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
        } else {
            bitmap
        }
    }

    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        val containerWidth = constraints.maxWidth.toFloat()
        val containerHeight = constraints.maxHeight.toFloat()

        // Calculate fitted image display bounds inside container
        val scale = min(
            containerWidth / transformedBitmap.width.toFloat(),
            containerHeight / transformedBitmap.height.toFloat()
        )
        val displayedW = transformedBitmap.width * scale
        val displayedH = transformedBitmap.height * scale
        val imgLeft = (containerWidth - displayedW) / 2f
        val imgTop = (containerHeight - displayedH) / 2f
        val imageBounds = remember(displayedW, displayedH, imgLeft, imgTop) {
            Rect(imgLeft, imgTop, imgLeft + displayedW, imgTop + displayedH)
        }

        var cropRect by remember {
            mutableStateOf(
                CropGeometry.calculateAspectCropRect(
                    imageBounds = imageBounds,
                    aspectX = selectedRatio.aspectX,
                    aspectY = selectedRatio.aspectY
                )
            )
        }

        var activeHandle by remember { mutableStateOf<CropHandle?>(null) }

        // Recalculate crop rect when ratio or image orientation changes
        LaunchedEffect(selectedRatio, imageBounds) {
            cropRect = CropGeometry.calculateAspectCropRect(
                imageBounds = imageBounds,
                aspectX = selectedRatio.aspectX,
                aspectY = selectedRatio.aspectY
            )
            onCropRectChanged(cropRect, imageBounds)
        }

        // Notify parent when cropRect or imageBounds updates
        LaunchedEffect(cropRect, imageBounds) {
            onCropRectChanged(cropRect, imageBounds)
        }

        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(selectedRatio, imageBounds) {
                    detectDragGestures(
                        onDragStart = { offset ->
                            activeHandle = CropGeometry.hitTest(offset, cropRect, touchRadiusPx)
                        },
                        onDrag = { change, dragAmount ->
                            if (activeHandle != null) {
                                change.consume()
                                cropRect = CropGeometry.updateCropRect(
                                    currentRect = cropRect,
                                    handle = activeHandle!!,
                                    dragDelta = dragAmount,
                                    bounds = imageBounds,
                                    aspectX = selectedRatio.aspectX,
                                    aspectY = selectedRatio.aspectY
                                )
                            }
                        },
                        onDragEnd = {
                            activeHandle = null
                        },
                        onDragCancel = {
                            activeHandle = null
                        }
                    )
                }
        ) {
            // 1. Draw transformed image within imageBounds
            drawImage(
                image = transformedBitmap.asImageBitmap(),
                dstOffset = IntOffset(imgLeft.toInt(), imgTop.toInt()),
                dstSize = IntSize(displayedW.toInt(), displayedH.toInt())
            )

            // 2. Draw darkened overlay (#BB000000) outside crop window
            val dimColor = Color(0xBB000000)
            // Top dim
            if (cropRect.top > imageBounds.top) {
                drawRect(
                    color = dimColor,
                    topLeft = Offset(imageBounds.left, imageBounds.top),
                    size = Size(imageBounds.width, cropRect.top - imageBounds.top)
                )
            }
            // Bottom dim
            if (cropRect.bottom < imageBounds.bottom) {
                drawRect(
                    color = dimColor,
                    topLeft = Offset(imageBounds.left, cropRect.bottom),
                    size = Size(imageBounds.width, imageBounds.bottom - cropRect.bottom)
                )
            }
            // Left dim
            if (cropRect.left > imageBounds.left) {
                drawRect(
                    color = dimColor,
                    topLeft = Offset(imageBounds.left, cropRect.top),
                    size = Size(cropRect.left - imageBounds.left, cropRect.height)
                )
            }
            // Right dim
            if (cropRect.right < imageBounds.right) {
                drawRect(
                    color = dimColor,
                    topLeft = Offset(cropRect.right, cropRect.top),
                    size = Size(imageBounds.right - cropRect.right, cropRect.height)
                )
            }

            // 3. Draw crop window border
            drawRect(
                color = Color.White.copy(alpha = 0.85f),
                topLeft = cropRect.topLeft,
                size = cropRect.size,
                style = Stroke(width = borderThicknessPx)
            )

            // 4. Draw 3x3 Rule of Thirds guidelines
            val guidelineColor = Color.White.copy(alpha = 0.45f)
            val thirdW = cropRect.width / 3f
            val thirdH = cropRect.height / 3f
            // Vertical guidelines
            drawLine(
                color = guidelineColor,
                start = Offset(cropRect.left + thirdW, cropRect.top),
                end = Offset(cropRect.left + thirdW, cropRect.bottom),
                strokeWidth = gridThicknessPx
            )
            drawLine(
                color = guidelineColor,
                start = Offset(cropRect.left + 2 * thirdW, cropRect.top),
                end = Offset(cropRect.left + 2 * thirdW, cropRect.bottom),
                strokeWidth = gridThicknessPx
            )
            // Horizontal guidelines
            drawLine(
                color = guidelineColor,
                start = Offset(cropRect.left, cropRect.top + thirdH),
                end = Offset(cropRect.right, cropRect.top + thirdH),
                strokeWidth = gridThicknessPx
            )
            drawLine(
                color = guidelineColor,
                start = Offset(cropRect.left, cropRect.top + 2 * thirdH),
                end = Offset(cropRect.right, cropRect.top + 2 * thirdH),
                strokeWidth = gridThicknessPx
            )

            // 5. Draw authentic Corner L-Brackets (CanHub / reference APK style)
            drawCornerBrackets(
                cropRect = cropRect,
                cornerLen = cornerLenPx,
                thickness = handleThicknessPx,
                color = Color.White
            )

            // 6. Draw Midpoint Edge Bars
            drawEdgeBars(
                cropRect = cropRect,
                barLen = cornerLenPx * 0.9f,
                thickness = handleThicknessPx,
                color = Color.White
            )
        }
    }
}

/**
 * Draws L-shaped corner brackets on the crop window.
 */
private fun DrawScope.drawCornerBrackets(
    cropRect: Rect,
    cornerLen: Float,
    thickness: Float,
    color: Color
) {
    val halfThick = thickness / 2f

    // TOP-LEFT
    drawLine(
        color = color,
        start = Offset(cropRect.left - halfThick, cropRect.top),
        end = Offset(cropRect.left + cornerLen, cropRect.top),
        strokeWidth = thickness
    )
    drawLine(
        color = color,
        start = Offset(cropRect.left, cropRect.top - halfThick),
        end = Offset(cropRect.left, cropRect.top + cornerLen),
        strokeWidth = thickness
    )

    // TOP-RIGHT
    drawLine(
        color = color,
        start = Offset(cropRect.right + halfThick, cropRect.top),
        end = Offset(cropRect.right - cornerLen, cropRect.top),
        strokeWidth = thickness
    )
    drawLine(
        color = color,
        start = Offset(cropRect.right, cropRect.top - halfThick),
        end = Offset(cropRect.right, cropRect.top + cornerLen),
        strokeWidth = thickness
    )

    // BOTTOM-LEFT
    drawLine(
        color = color,
        start = Offset(cropRect.left - halfThick, cropRect.bottom),
        end = Offset(cropRect.left + cornerLen, cropRect.bottom),
        strokeWidth = thickness
    )
    drawLine(
        color = color,
        start = Offset(cropRect.left, cropRect.bottom + halfThick),
        end = Offset(cropRect.left, cropRect.bottom - cornerLen),
        strokeWidth = thickness
    )

    // BOTTOM-RIGHT
    drawLine(
        color = color,
        start = Offset(cropRect.right + halfThick, cropRect.bottom),
        end = Offset(cropRect.right - cornerLen, cropRect.bottom),
        strokeWidth = thickness
    )
    drawLine(
        color = color,
        start = Offset(cropRect.right, cropRect.bottom + halfThick),
        end = Offset(cropRect.right, cropRect.bottom - cornerLen),
        strokeWidth = thickness
    )
}

/**
 * Draws midpoint edge bars on the crop window.
 */
private fun DrawScope.drawEdgeBars(
    cropRect: Rect,
    barLen: Float,
    thickness: Float,
    color: Color
) {
    val halfBar = barLen / 2f

    // Top edge bar
    drawLine(
        color = color,
        start = Offset(cropRect.center.x - halfBar, cropRect.top),
        end = Offset(cropRect.center.x + halfBar, cropRect.top),
        strokeWidth = thickness
    )
    // Bottom edge bar
    drawLine(
        color = color,
        start = Offset(cropRect.center.x - halfBar, cropRect.bottom),
        end = Offset(cropRect.center.x + halfBar, cropRect.bottom),
        strokeWidth = thickness
    )
    // Left edge bar
    drawLine(
        color = color,
        start = Offset(cropRect.left, cropRect.center.y - halfBar),
        end = Offset(cropRect.left, cropRect.center.y + halfBar),
        strokeWidth = thickness
    )
    // Right edge bar
    drawLine(
        color = color,
        start = Offset(cropRect.right, cropRect.center.y - halfBar),
        end = Offset(cropRect.right, cropRect.center.y + halfBar),
        strokeWidth = thickness
    )
}
