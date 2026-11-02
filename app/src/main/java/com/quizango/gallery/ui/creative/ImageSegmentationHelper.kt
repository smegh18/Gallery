package com.quizango.gallery.ui.creative

import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.Paint
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import android.graphics.RadialGradient
import android.graphics.Rect
import android.graphics.RectF
import android.graphics.Shader
import android.media.MediaScannerConnection
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import com.google.mlkit.vision.common.InputImage
import com.google.android.gms.tasks.Tasks
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

object ImageSegmentationHelper {

    /**
     * Attempts ML Kit Subject Segmentation to extract the isolated foreground subject bitmap.
     * Falls back to a center portrait subject mask if ML Kit is not ready, offline, or unavailable.
     */
    suspend fun extractSubject(context: Context, sourceBitmap: Bitmap): Bitmap = withContext(Dispatchers.Default) {
        try {
            // ML Kit subject segmentation reflection or direct call
            val optionsClass = Class.forName("com.google.mlkit.vision.segmentation.subject.SubjectSegmenterOptions")
            val builderClass = Class.forName("com.google.mlkit.vision.segmentation.subject.SubjectSegmenterOptions\$Builder")
            val builder = builderClass.getDeclaredConstructor().newInstance()
            val enableForegroundMethod = builderClass.getMethod("enableForegroundBitmap")
            enableForegroundMethod.invoke(builder)
            val buildMethod = builderClass.getMethod("build")
            val options = buildMethod.invoke(builder)

            val segmentationClass = Class.forName("com.google.mlkit.vision.segmentation.subject.SubjectSegmentation")
            val getClientMethod = segmentationClass.getMethod("getClient", optionsClass)
            val client = getClientMethod.invoke(null, options)

            val inputImage = InputImage.fromBitmap(sourceBitmap, 0)
            val processMethod = client.javaClass.getMethod("process", InputImage::class.java)
            val task = processMethod.invoke(client, inputImage) as com.google.android.gms.tasks.Task<*>

            val result = Tasks.await(task, 4, TimeUnit.SECONDS)
            val getForegroundMethod = result.javaClass.getMethod("getForegroundBitmap")
            val fg = getForegroundMethod.invoke(result) as? Bitmap
            if (fg != null) {
                return@withContext fg
            }
        } catch (_: Throwable) {
            // Graceful fallback below
        }

        // Resilient fallback: Generate smooth portrait/subject mask cutout
        createFallbackForegroundCutout(sourceBitmap)
    }

    /**
     * Generates a center-weighted portrait subject cutout with feathered edges
     * when offline or when Play Services ML Kit module is unavailable.
     */
    fun createFallbackForegroundCutout(source: Bitmap): Bitmap {
        val width = source.width
        val height = source.height
        val output = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(output)

        // Draw the original image first
        canvas.drawBitmap(source, 0f, 0f, null)

        // Create an alpha mask to isolate the center subject
        val maskBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val maskCanvas = Canvas(maskBitmap)

        val cx = width / 2f
        val cy = height * 0.48f // Slightly above center for portrait framing
        val radiusX = width * 0.40f
        val radiusY = height * 0.46f

        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.BLACK
        }

        // Draw feathered oval
        maskCanvas.save()
        maskCanvas.scale(1f, radiusY / radiusX, cx, cy)
        val radial = RadialGradient(
            cx, cy, radiusX,
            intArrayOf(Color.BLACK, Color.BLACK, Color.argb(160, 0, 0, 0), Color.TRANSPARENT),
            floatArrayOf(0f, 0.65f, 0.88f, 1f),
            Shader.TileMode.CLAMP
        )
        paint.shader = radial
        maskCanvas.drawCircle(cx, cy, radiusX, paint)
        maskCanvas.restore()

        // Apply alpha mask to output
        val xferPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            xfermode = PorterDuffXfermode(PorterDuff.Mode.DST_IN)
        }
        canvas.drawBitmap(maskBitmap, 0f, 0f, xferPaint)
        maskBitmap.recycle()

        return output
    }

    /**
     * Produces a smooth blurred version of the bitmap.
     * Uses downsampled multi-pass box blur for high performance on all Android versions.
     */
    fun blurBitmap(source: Bitmap, radiusPercent: Int): Bitmap {
        if (radiusPercent <= 0) return source.copy(source.config ?: Bitmap.Config.ARGB_8888, true)

        // Downscale bitmap to achieve wide, soft bokeh efficiently
        val scale = when {
            radiusPercent > 50 -> 0.25f
            radiusPercent > 20 -> 0.35f
            else -> 0.5f
        }
        val smallWidth = (source.width * scale).toInt().coerceAtLeast(1)
        val smallHeight = (source.height * scale).toInt().coerceAtLeast(1)

        val smallBitmap = Bitmap.createScaledBitmap(source, smallWidth, smallHeight, true)
        val boxRadius = ((radiusPercent / 100f) * 16).toInt().coerceIn(1, 25)

        fastBoxBlur(smallBitmap, boxRadius)

        val blurred = Bitmap.createScaledBitmap(smallBitmap, source.width, source.height, true)
        if (blurred != smallBitmap) {
            smallBitmap.recycle()
        }
        return blurred
    }

    /**
     * In-place fast multi-pass box blur (approximates Gaussian blur).
     */
    private fun fastBoxBlur(bitmap: Bitmap, radius: Int) {
        val w = bitmap.width
        val h = bitmap.height
        val pix = IntArray(w * h)
        bitmap.getPixels(pix, 0, w, 0, 0, w, h)

        val r = radius.coerceAtLeast(1)
        val div = r + r + 1
        val vmin = IntArray(w.coerceAtLeast(h))

        val r1 = IntArray(w * h)
        val g1 = IntArray(w * h)
        val b1 = IntArray(w * h)

        var yw = 0
        var yi = 0

        for (y in 0 until h) {
            var rinsum = 0
            var ginsum = 0
            var binsum = 0
            for (i in -r..r) {
                val p = pix[yi + i.coerceIn(0, w - 1)]
                rinsum += (p shr 16) and 0xff
                ginsum += (p shr 8) and 0xff
                binsum += p and 0xff
            }
            for (x in 0 until w) {
                r1[yi] = rinsum / div
                g1[yi] = ginsum / div
                b1[yi] = binsum / div

                if (y == 0) vmin[x] = (x + r + 1).coerceAtMost(w - 1)
                val p1 = pix[yw + vmin[x]]
                val p2 = pix[yw + (x - r).coerceAtLeast(0)]

                rinsum += ((p1 shr 16) and 0xff) - ((p2 shr 16) and 0xff)
                ginsum += ((p1 shr 8) and 0xff) - ((p2 shr 8) and 0xff)
                binsum += (p1 and 0xff) - (p2 and 0xff)
                yi++
            }
            yw += w
        }

        for (x in 0 until w) {
            var rinsum = 0
            var ginsum = 0
            var binsum = 0
            val yp = -r * w
            for (i in -r..r) {
                val yiIndex = (x + (i.coerceIn(0, h - 1) * w))
                rinsum += r1[yiIndex]
                ginsum += g1[yiIndex]
                binsum += b1[yiIndex]
            }
            yi = x
            for (y in 0 until h) {
                pix[yi] = (-0x1000000 or ((rinsum / div) shl 16) or ((ginsum / div) shl 8) or (binsum / div))
                if (x == 0) vmin[y] = (y + r + 1).coerceAtMost(h - 1) * w
                val p1 = x + vmin[y]
                val p2 = x + (y - r).coerceAtLeast(0) * w

                rinsum += r1[p1] - r1[p2]
                ginsum += g1[p1] - g1[p2]
                binsum += b1[p1] - b1[p2]
                yi += w
            }
        }

        bitmap.setPixels(pix, 0, w, 0, 0, w, h)
    }

    /**
     * Desaturates a bitmap according to a saturation percentage (0 = pure B&W, 100 = full original color).
     */
    fun desaturateBitmap(source: Bitmap, saturationPercent: Float): Bitmap {
        val sat = (saturationPercent / 100f).coerceIn(0f, 1f)
        val output = Bitmap.createBitmap(source.width, source.height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(output)
        val colorMatrix = ColorMatrix().apply {
            setSaturation(sat)
        }
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            colorFilter = ColorMatrixColorFilter(colorMatrix)
        }
        canvas.drawBitmap(source, 0f, 0f, paint)
        return output
    }

    /**
     * Composites a background bitmap with a foreground subject cutout bitmap.
     */
    fun composite(background: Bitmap, foreground: Bitmap): Bitmap {
        val output = Bitmap.createBitmap(background.width, background.height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(output)
        canvas.drawBitmap(background, 0f, 0f, null)
        val rect = Rect(0, 0, background.width, background.height)
        canvas.drawBitmap(foreground, null, rect, null)
        return output
    }

    /**
     * Composites a colored (or transparent) background with a foreground cutout bitmap.
     */
    fun compositeColoredBackground(foreground: Bitmap, bgColor: Int?): Bitmap {
        val output = Bitmap.createBitmap(foreground.width, foreground.height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(output)
        if (bgColor != null && bgColor != 0) {
            canvas.drawColor(bgColor)
        }
        canvas.drawBitmap(foreground, 0f, 0f, null)
        return output
    }

    /**
     * Saves a bitmap to disk in the Pictures/Gallery directory and registers it in MediaStore.
     */
    suspend fun saveImageToGallery(
        context: Context,
        bitmap: Bitmap,
        folderName: String = "Gallery",
        prefix: String = "IMG"
    ): String? = withContext(Dispatchers.IO) {
        try {
            val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
            val filename = "${prefix}_$timestamp.png"

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val values = ContentValues().apply {
                    put(MediaStore.Images.Media.DISPLAY_NAME, filename)
                    put(MediaStore.Images.Media.MIME_TYPE, "image/png")
                    put(MediaStore.Images.Media.RELATIVE_PATH, "${Environment.DIRECTORY_PICTURES}/$folderName")
                    put(MediaStore.Images.Media.IS_PENDING, 1)
                }

                val uri = context.contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)
                if (uri != null) {
                    context.contentResolver.openOutputStream(uri)?.use { stream ->
                        bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream)
                    }
                    values.clear()
                    values.put(MediaStore.Images.Media.IS_PENDING, 0)
                    context.contentResolver.update(uri, values, null, null)

                    val realPath = queryRealPath(context, uri)
                    if (realPath != null) {
                        MediaScannerConnection.scanFile(context, arrayOf(realPath), arrayOf("image/png"), null)
                        return@withContext realPath
                    }
                    return@withContext uri.toString()
                }
            }

            // Legacy Android or fallback path
            val picturesDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES)
            val targetDir = File(picturesDir, folderName).apply { mkdirs() }
            val file = File(targetDir, filename)
            FileOutputStream(file).use { out ->
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
            }
            MediaScannerConnection.scanFile(context, arrayOf(file.absolutePath), arrayOf("image/png"), null)
            return@withContext file.absolutePath
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    private fun queryRealPath(context: Context, uri: Uri): String? {
        val proj = arrayOf(MediaStore.Images.Media.DATA)
        context.contentResolver.query(uri, proj, null, null, null)?.use { cursor ->
            if (cursor.moveToFirst()) {
                val idx = cursor.getColumnIndex(MediaStore.Images.Media.DATA)
                if (idx != -1) return cursor.getString(idx)
            }
        }
        return null
    }
}
