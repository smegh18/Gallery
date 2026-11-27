package com.quizango.gallery.ui.creative

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.Matrix
import android.graphics.Paint
import android.media.ExifInterface
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import com.google.android.gms.common.moduleinstall.InstallStatusListener
import com.google.android.gms.common.moduleinstall.ModuleInstall
import com.google.android.gms.common.moduleinstall.ModuleInstallRequest
import com.google.android.gms.common.moduleinstall.ModuleInstallStatusUpdate.InstallState
import com.google.android.gms.tasks.Tasks
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.segmentation.subject.SubjectSegmentation
import com.google.mlkit.vision.segmentation.subject.SubjectSegmenterOptions
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import java.io.File

/** Shared work of Remove BG, Blur BG and Color Splash (S272 RB / BB / CS). */
object ImageSegmentationHelper {

    /** The image at [path], upright (EXIF applied), at most [maxSide] px on its long side. */
    fun decodeUpright(path: String, maxSide: Int = MAX_SIDE): Bitmap? {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeFile(path, bounds)
        var sample = 1
        while (maxOf(bounds.outWidth, bounds.outHeight) / sample > maxSide) sample *= 2
        val bmp = BitmapFactory.decodeFile(path, BitmapFactory.Options().apply { inSampleSize = sample }) ?: return null
        val degrees = runCatching {
            when (ExifInterface(path).getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)) {
                ExifInterface.ORIENTATION_ROTATE_90 -> 90f
                ExifInterface.ORIENTATION_ROTATE_180 -> 180f
                ExifInterface.ORIENTATION_ROTATE_270 -> 270f
                else -> 0f
            }
        }.getOrDefault(0f)
        if (degrees == 0f) return bmp
        return Bitmap.createBitmap(bmp, 0, 0, bmp.width, bmp.height, Matrix().apply { postRotate(degrees) }, true)
    }

    /**
     * ML Kit subject segmentation, as 2.7.2 (Play services module, installed on first use): the subject on a transparent
     * bitmap of the source's size, or null when the module or the segmentation failed.
     */
    suspend fun extractSubject(context: Context, source: Bitmap): Bitmap? = withContext(Dispatchers.IO) {
        val segmenter = SubjectSegmentation.getClient(SubjectSegmenterOptions.Builder().enableForegroundBitmap().build())
        try {
            val modules = ModuleInstall.getClient(context)
            if (!Tasks.await(modules.areModulesAvailable(segmenter)).areModulesAvailable()) {
                val done = CompletableDeferred<Boolean>()
                val listener = InstallStatusListener { update ->
                    when (update.installState) {
                        InstallState.STATE_COMPLETED -> done.complete(true)
                        InstallState.STATE_CANCELED, InstallState.STATE_FAILED -> done.complete(false)
                        else -> Unit
                    }
                }
                val request = ModuleInstallRequest.newBuilder().addApi(segmenter).setListener(listener).build()
                val installed = Tasks.await(modules.installModules(request)).areModulesAlreadyInstalled() ||
                    withTimeoutOrNull(INSTALL_TIMEOUT_MS) { done.await() } == true
                modules.unregisterListener(listener)
                if (!installed) return@withContext null
            }
            Tasks.await(segmenter.process(InputImage.fromBitmap(source, 0))).foregroundBitmap
        } catch (e: Exception) {
            null
        } finally {
            segmenter.close()
        }
    }

    /** RB.o000000O: the subject cut to its non-transparent box, with the box's top-left in the source. */
    fun trim(subject: Bitmap): Pair<Bitmap, IntArray>? {
        val px = IntArray(subject.width * subject.height)
        subject.getPixels(px, 0, subject.width, 0, 0, subject.width, subject.height)
        val (l, t, r, b) = opaqueBounds(px, subject.width, subject.height) ?: return null
        return Bitmap.createBitmap(subject, l, t, r - l + 1, b - t + 1) to intArrayOf(l, t)
    }

    /** Left, top, right, bottom (inclusive) of the pixels with any alpha, or null when all are transparent. */
    internal fun opaqueBounds(px: IntArray, width: Int, height: Int): IntArray? {
        var l = width; var t = height; var r = -1; var b = -1
        for (y in 0 until height) for (x in 0 until width) {
            if (px[y * width + x] ushr 24 != 0) {
                if (x < l) l = x
                if (x > r) r = x
                if (y < t) t = y
                if (y > b) b = y
            }
        }
        return if (r < 0) null else intArrayOf(l, t, r, b)
    }

    /** BB (Blurry): the background blurred at [radius] 0..25 px of a display-sized copy (0 = sharp). */
    fun blur(source: Bitmap, radius: Int): Bitmap {
        val copy = source.copy(Bitmap.Config.ARGB_8888, true)
        if (radius > 0) {
            repeat(3) { boxBlur(copy, radius) } // three box passes approximate the reference's gaussian
        }
        return copy
    }

    /** CS (GPUImage kw/p + lw/b): red copied to every channel, mixed with the original by [intensity] 0..1. */
    fun splash(source: Bitmap, intensity: Float): Bitmap {
        val out = Bitmap.createBitmap(source.width, source.height, Bitmap.Config.ARGB_8888)
        val filter = ColorMatrixColorFilter(ColorMatrix(splashMatrixValues(intensity)))
        Canvas(out).drawBitmap(source, 0f, 0f, Paint(Paint.FILTER_BITMAP_FLAG).apply { colorFilter = filter })
        return out
    }

    /** mix(original, mono, t) as a 4x5 colour matrix: identity weighted by (1 - t) plus the red row weighted by t. */
    internal fun splashMatrixValues(intensity: Float): FloatArray {
        val t = intensity.coerceIn(0f, 1f)
        val k = 1 - t
        return floatArrayOf(
            1f, 0f, 0f, 0f, 0f,
            t, k, 0f, 0f, 0f,
            t, 0f, k, 0f, 0f,
            0f, 0f, 0f, 1f, 0f
        )
    }

    /** RB / BB / CS save: PNG quality 100 over the editor's working copy. */
    suspend fun writePng(bitmap: Bitmap, path: String): Boolean = withContext(Dispatchers.IO) {
        runCatching { File(path).outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) } }.getOrDefault(false)
    }

    /** s9/k.Oooo000: picks which segmentation error to show. */
    fun isOnline(context: Context): Boolean {
        val cm = context.getSystemService(ConnectivityManager::class.java) ?: return false
        return cm.getNetworkCapabilities(cm.activeNetwork)?.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) == true
    }

    /** One horizontal then one vertical running-sum box pass, in place. */
    private fun boxBlur(bitmap: Bitmap, radius: Int) {
        val w = bitmap.width
        val h = bitmap.height
        val src = IntArray(w * h)
        bitmap.getPixels(src, 0, w, 0, 0, w, h)
        val tmp = IntArray(w * h)
        pass(src, tmp, w, h, radius, 1, w)
        pass(tmp, src, h, w, radius, w, 1)
        bitmap.setPixels(src, 0, w, 0, 0, w, h)
    }

    /** Blurs [lines] runs of [len] pixels (stride [step] inside a run, [lineStep] between runs) from [from] into [to]. */
    private fun pass(from: IntArray, to: IntArray, len: Int, lines: Int, r: Int, step: Int, lineStep: Int) {
        val div = 2 * r + 1
        for (line in 0 until lines) {
            val base = line * lineStep
            var a = 0; var red = 0; var g = 0; var b = 0
            for (i in -r..r) {
                val p = from[base + i.coerceIn(0, len - 1) * step]
                a += p ushr 24; red += (p shr 16) and 0xff; g += (p shr 8) and 0xff; b += p and 0xff
            }
            for (i in 0 until len) {
                to[base + i * step] = ((a / div) shl 24) or ((red / div) shl 16) or ((g / div) shl 8) or (b / div)
                val add = from[base + (i + r + 1).coerceAtMost(len - 1) * step]
                val drop = from[base + (i - r).coerceAtLeast(0) * step]
                a += (add ushr 24) - (drop ushr 24)
                red += ((add shr 16) and 0xff) - ((drop shr 16) and 0xff)
                g += ((add shr 8) and 0xff) - ((drop shr 8) and 0xff)
                b += (add and 0xff) - (drop and 0xff)
            }
        }
    }

    // shortcut: photos over 4096 px come back downsampled, raise it if 2.7.2's full-size output matters on such files
    private const val MAX_SIDE = 4096
    private const val INSTALL_TIMEOUT_MS = 60_000L
}
