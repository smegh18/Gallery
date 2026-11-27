package com.quizango.gallery.ui.editor

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.ColorMatrixColorFilter
import android.graphics.Matrix
import android.graphics.Paint
import android.media.ExifInterface
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import com.quizango.gallery.R
import java.io.File
import java.io.FileOutputStream
import java.io.OutputStream
import kotlin.math.floor
import kotlin.math.pow

/** EDIT_TAB_* strip items of PhotoEditActivity.onCreate, in order; [newKey] is the pref that hides the "New" badge. */
internal enum class EditTool(@StringRes val label: Int, @DrawableRes val icon: Int, val ai: Boolean = false, val newKey: String? = null, val tipKey: String? = null) {
    REMOVE_BG(R.string.remove_n_background, R.drawable.ic_remove_background, true, "isRemoveNewToolTip", "isRemoveEditToolTip"),
    COLOR_SPLASH(R.string.color_bg, R.drawable.ic_color_splash_bg, true, "isColorSplashNewToolTip", "isColorEditToolTip"),
    BLUR_BG(R.string.blur_bg, R.drawable.ic_blur, true, "isBlurNewToolTip", "isBlurEditToolTip"),
    CROP(R.string.crop, R.drawable.ic_crop),
    ADJUST(R.string.adjust, R.drawable.ic_adjust),
    FILTER(R.string.filter, R.drawable.ic_filter),
    BRUSH(R.string.brush, R.drawable.ic_brush),
    STICKER(R.string.sticker, R.drawable.ic_sticker),
    TEXT(R.string.text, R.drawable.ic_text)
}

/** s9/k.OooOo0o: the three AI tools lead the strip only on Android 10+ with at least 3 GB of RAM. */
// shortcut: Sticker is hidden because StickerScreen cannot hand a sticker back to place on the photo yet; drop the filter once placement and export exist
internal fun editTools(aiAvailable: Boolean): List<EditTool> = EditTool.entries.filter { it != EditTool.STICKER && (aiAvailable || !it.ai) }

internal const val AI_MIN_SDK = 29
internal const val AI_MIN_RAM = 3L * 1024 * 1024 * 1024

/** PE.onCreate: one first-run tooltip at a time, Remove BG first, only for a plain edit with the AI tools shown. */
internal fun firstTooltip(aiAvailable: Boolean, plainEdit: Boolean, seen: (String) -> Boolean): EditTool? =
    if (!aiAvailable || !plainEdit) null else EditTool.entries.firstOrNull { it.tipKey != null && !seen(it.tipKey) }

/** editOpenType of a suggestion route (viewer chips): panels open in place, AI tools start on the working copy. */
internal fun initialTool(tab: String?): EditTool? = when (tab?.lowercase()) {
    "crop" -> EditTool.CROP
    "brush", "markup" -> EditTool.BRUSH
    "adjust" -> EditTool.ADJUST
    "filter" -> EditTool.FILTER
    "remove_bg" -> EditTool.REMOVE_BG
    "color_splash" -> EditTool.COLOR_SPLASH
    "blur_bg" -> EditTool.BLUR_BG
    else -> null
}

/** isFromSuggestion: crop and markup chips open the editor for that one tool, so cancelling it leaves the editor. */
internal fun isSuggestionTool(tab: String?) = initialTool(tab).let { it == EditTool.CROP || it == EditTool.BRUSH }

/**
 * ratio_recycler items (PE ~31735) and the aspect each one sets (PE.o000ooo). 2.7.2 sets 4:3 for "IG 4:5"
 * and 4:5 for "Ins Story"; kept as shipped.
 */
internal enum class CropPreset(@StringRes val label: Int, @DrawableRes val icon: Int, @DrawableRes val background: Int?, val aspectX: Int, val aspectY: Int) {
    FREE(R.string.free, R.drawable.ic_expand, null, 0, 0),
    IG_1_1(R.string.IG_1, R.drawable.ic_crop_insta, R.drawable.ic_crop_bg, 1, 1),
    IG_4_5(R.string.IG_2, R.drawable.ic_crop_insta, R.drawable.rectangle_crop_bg, 4, 3),
    INSTA_STORY(R.string.Ins_story, R.drawable.ic_crop_insta, R.drawable.rectangle_crop_img_bg, 4, 5),
    FB_COVER(R.string.cover, R.drawable.ic_crop_fb, R.drawable.rectangle_fb_bg, 16, 9),
    FB_5_4(R.string.IG_3, R.drawable.ic_crop_fb, R.drawable.ic_rectangle_9, 5, 4),
    YOUTUBE(R.string.youtube, R.drawable.ic_youtube_crop, R.drawable.rectangle_fb_bg, 16, 9)
}

/** adjust_bottom_nav items with their SliderVal range and default (PhotoEditViewModel ~340). */
internal enum class AdjustParam(@StringRes val label: Int, @DrawableRes val icon: Int, val min: Int, val max: Int, val default: Int) {
    BRIGHTNESS(R.string.brightness, R.drawable.ic_brightness, -25, 25, 0),
    CONTRAST(R.string.contrast, R.drawable.ic_contrast, 0, 50, 25),
    SATURATION(R.string.saturation, R.drawable.ic_saturation, 0, 50, 25),
    SHARPEN(R.string.sharpen, R.drawable.ic_sharpness, -25, 25, 0)
}

/** PhotoEditViewModel ~2646: each GPUImage parameter is the ruler value × 4 / 100. */
internal fun adjustAmount(value: Int): Float = value * 4 / 100f

internal fun AdjustParam.isNeutral(value: Int) = value == default

/** The 25 GPUImage filters of PhotoEditViewModel, in its list order. */
internal enum class EditorFilter {
    NORMAL, BRIGHTNESS, CONTRAST, SATURATION, EXPOSURE, RETRO, RGB, POSTERIZE, JUST, HUME, LIMO, SOLAR, NEUTRON, MILK,
    CLASSIC, ATOM, YELI, MARS, DESERT, OLD_TIMES, MONOCHROME, SHARPEN, CLUE, LUMINANCE, MULI
}

private val LUMA = floatArrayOf(0.2125f, 0.7154f, 0.0721f) // GPUImage luminanceWeighting

/** GPUImageColorMatrixFilter arrays (:array_0..e) and intensities, row i = the weights of output channel i. */
private val GPU_MATRICES: Map<EditorFilter, Pair<Float, FloatArray>> = mapOf(
    EditorFilter.RETRO to (1f to floatArrayOf(1f, 0f, 0f, 0f, 0f, 1f, 0.2f, 0f, 0.1f, 0.1f, 1f, 0f, 1f, 0f, 0f, 1f)),
    EditorFilter.JUST to (0.9f to floatArrayOf(0.4f, 0.6f, 0.5f, 0f, 0f, 0.4f, 1f, 0f, 0.05f, 0.1f, 0.4f, 0.4f, 1f, 1f, 1f, 1f)),
    EditorFilter.HUME to (1f to floatArrayOf(1.25f, 0f, 0.2f, 0f, 0f, 1f, 0.2f, 0f, 0f, 0.3f, 1f, 0.3f, 0f, 0f, 0f, 1f)),
    EditorFilter.DESERT to (1f to floatArrayOf(0.6f, 0.4f, 0.2f, 0.05f, 0f, 0.8f, 0.3f, 0.05f, 0.3f, 0.3f, 0.5f, 0.08f, 0f, 0f, 0f, 1f)),
    EditorFilter.OLD_TIMES to (1f to floatArrayOf(1f, 0.05f, 0f, 0f, -0.2f, 1.1f, -0.2f, 0.11f, 0.2f, 0f, 1f, 0f, 0f, 0f, 0f, 1f)),
    EditorFilter.LIMO to (1f to floatArrayOf(1f, 0f, 0.08f, 0f, 0.4f, 1f, 0f, 0f, 0f, 0f, 1f, 0.1f, 0f, 0f, 0f, 1f)),
    EditorFilter.SOLAR to (1f to floatArrayOf(1.5f, 0f, 0f, 0f, 0f, 1f, 0f, 0f, 0f, 0f, 1f, 0f, 0f, 0f, 0f, 1f)),
    EditorFilter.NEUTRON to (1f to floatArrayOf(0f, 1f, 0f, 0f, 0f, 1f, 0f, 0f, 0f, 0.6f, 1f, 0f, 0f, 0f, 0f, 1f)),
    EditorFilter.MILK to (1f to floatArrayOf(0f, 1f, 0f, 0f, 0f, 1f, 0f, 0f, 0f, 0.64f, 0.5f, 0f, 0f, 0f, 0f, 1f)),
    EditorFilter.CLUE to (1f to floatArrayOf(0f, 0f, 1f, 0f, 0f, 0f, 1f, 0f, 0f, 0f, 1f, 0f, 0f, 0f, 0f, 1f)),
    EditorFilter.MULI to (1f to floatArrayOf(1f, 0f, 0f, 0f, 1f, 0f, 0f, 0f, 1f, 0f, 0f, 0f, 0f, 0f, 0f, 1f)),
    EditorFilter.CLASSIC to (1f to floatArrayOf(0.763f, 0f, 0.2062f, 0f, 0f, 0.9416f, 0f, 0f, 0.1623f, 0.2614f, 0.8052f, 0f, 0f, 0f, 0f, 1f)),
    EditorFilter.ATOM to (1f to floatArrayOf(0.5162f, 0.3799f, 0.3247f, 0f, 0.039f, 1f, 0f, 0f, -0.4773f, 0.461f, 1f, 0f, 0f, 0f, 0f, 1f)),
    EditorFilter.MARS to (1f to floatArrayOf(0f, 0f, 0.5183f, 0.3183f, 0f, 0.5497f, 0.5416f, 0f, 0.5237f, 0.5269f, 0f, 0f, 0f, 0f, 0f, 1f)),
    EditorFilter.YELI to (1f to floatArrayOf(1f, -0.3831f, 0.3883f, 0f, 0f, 1f, 0.2f, 0f, -0.1961f, 0f, 1f, 0f, 0f, 0f, 0f, 1f))
)

/** A GPUImage 4x4 colour matrix as an android ColorMatrix (4x5), mixed with the input by [intensity] as the shader does. */
internal fun gpuColorMatrix(m: FloatArray, intensity: Float): FloatArray = FloatArray(20) { k ->
    val row = k / 5
    val col = k % 5
    if (col == 4) 0f else m[row * 4 + col] * intensity + if (row == col) 1f - intensity else 0f
}

private fun scaleOffset(scale: Float, offset: Float) = floatArrayOf(
    scale, 0f, 0f, 0f, offset, 0f, scale, 0f, 0f, offset, 0f, 0f, scale, 0f, offset, 0f, 0f, 0f, 1f, 0f
)

/** GPUImageBrightnessFilter: rgb + b. */
internal fun brightnessMatrix(b: Float) = scaleOffset(1f, b * 255f)

/** GPUImageContrastFilter: (rgb - 0.5) × c + 0.5. */
internal fun contrastMatrix(c: Float) = scaleOffset(c, 127.5f * (1f - c))

/** GPUImageSaturationFilter: mix(luminance, rgb, s). */
internal fun saturationMatrix(s: Float): FloatArray {
    val g = 1f - s
    return floatArrayOf(
        g * LUMA[0] + s, g * LUMA[1], g * LUMA[2], 0f, 0f,
        g * LUMA[0], g * LUMA[1] + s, g * LUMA[2], 0f, 0f,
        g * LUMA[0], g * LUMA[1], g * LUMA[2] + s, 0f, 0f,
        0f, 0f, 0f, 1f, 0f
    )
}

/** The colour matrix of a filter, or null when it is a per-pixel effect (posterize, monochrome, sharpen). */
internal fun filterMatrix(filter: EditorFilter): FloatArray? = when (filter) {
    EditorFilter.NORMAL -> brightnessMatrix(0f)
    EditorFilter.BRIGHTNESS -> brightnessMatrix(0.15f)
    EditorFilter.CONTRAST -> contrastMatrix(1.6f)
    EditorFilter.SATURATION -> saturationMatrix(1.8f)
    EditorFilter.EXPOSURE -> scaleOffset(2f.pow(0.3f), 0f)
    EditorFilter.RGB -> floatArrayOf(1.1f, 0f, 0f, 0f, 0f, 0f, 1.3f, 0f, 0f, 0f, 0f, 0f, 1.6f, 0f, 0f, 0f, 0f, 0f, 0f, 255f)
    EditorFilter.LUMINANCE -> FloatArray(20) { k -> if (k < 15) (if (k % 5 < 3) LUMA[k % 5] else 0f) else if (k == 18) 1f else 0f }
    EditorFilter.POSTERIZE, EditorFilter.MONOCHROME, EditorFilter.SHARPEN -> null
    else -> GPU_MATRICES.getValue(filter).let { (intensity, m) -> gpuColorMatrix(m, intensity) }
}

/** GPUImagePosterizeFilter(10): floor(c × 10 + 0.5) / 10 on every channel. */
internal fun posterize(c: Int, levels: Int = 10): Int = (floor(c / 255f * levels + 0.5f) / levels * 255f).toInt().coerceIn(0, 255)

/** GPUImageMonochromeFilter (intensity 1, colour 0.6/0.45/0.3): an overlay of the colour on the pixel's luminance. */
internal fun monochrome(argb: Int): Int {
    val l = (LUMA[0] * ((argb shr 16) and 0xFF) + LUMA[1] * ((argb shr 8) and 0xFF) + LUMA[2] * (argb and 0xFF)) / 255f
    fun overlay(f: Float) = (if (l < 0.5f) 2f * l * f else 1f - 2f * (1f - l) * (1f - f)).let { (it * 255f).toInt().coerceIn(0, 255) }
    return (argb and 0xFF000000.toInt()) or (overlay(0.6f) shl 16) or (overlay(0.45f) shl 8) or overlay(0.3f)
}

internal fun applyMatrix(src: Bitmap, matrix: FloatArray): Bitmap {
    val out = Bitmap.createBitmap(src.width, src.height, Bitmap.Config.ARGB_8888)
    Canvas(out).drawBitmap(src, 0f, 0f, Paint(Paint.FILTER_BITMAP_FLAG).apply { colorFilter = ColorMatrixColorFilter(matrix) })
    return out
}

private fun mapPixels(src: Bitmap, op: (IntArray) -> IntArray): Bitmap {
    val w = src.width
    val h = src.height
    val px = IntArray(w * h).also { src.getPixels(it, 0, w, 0, 0, w, h) }
    return Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888).apply { setPixels(op(px), 0, w, 0, 0, w, h) }
}

internal fun applyFilter(src: Bitmap, filter: EditorFilter): Bitmap = filterMatrix(filter)?.let { applyMatrix(src, it) } ?: when (filter) {
    EditorFilter.POSTERIZE -> mapPixels(src) { px ->
        IntArray(px.size) { i ->
            val c = px[i]
            (posterize((c ushr 24) and 0xFF) shl 24) or (posterize((c shr 16) and 0xFF) shl 16) or
                (posterize((c shr 8) and 0xFF) shl 8) or posterize(c and 0xFF)
        }
    }
    EditorFilter.MONOCHROME -> mapPixels(src) { px -> IntArray(px.size) { monochrome(px[it]) } }
    else -> ImageSharpen.sharpen(src, 200f) // GPUImageSharpenFilter(2.0)
}

/** The adjust group in GPUImage order (brightness, contrast, saturation, sharpen), one clamped pass each. */
internal fun applyAdjust(src: Bitmap, values: Map<AdjustParam, Int>): Bitmap {
    var out = src
    fun value(p: AdjustParam) = values[p] ?: p.default
    if (!AdjustParam.BRIGHTNESS.isNeutral(value(AdjustParam.BRIGHTNESS))) out = applyMatrix(out, brightnessMatrix(adjustAmount(value(AdjustParam.BRIGHTNESS))))
    if (!AdjustParam.CONTRAST.isNeutral(value(AdjustParam.CONTRAST))) out = applyMatrix(out, contrastMatrix(adjustAmount(value(AdjustParam.CONTRAST))))
    if (!AdjustParam.SATURATION.isNeutral(value(AdjustParam.SATURATION))) out = applyMatrix(out, saturationMatrix(adjustAmount(value(AdjustParam.SATURATION))))
    if (!AdjustParam.SHARPEN.isNeutral(value(AdjustParam.SHARPEN))) out = ImageSharpen.sharpen(out, adjustAmount(value(AdjustParam.SHARPEN)) * 100f)
    return out
}

/** PE: the working copy is filesDir/"image." + the last "." segment of the source name. */
internal fun workingCopyName(sourceName: String) = "image." + sourceName.substringAfterLast('.')

/** PE.o000oo0: a new "Image_<millis>.png" in the source's folder (Pictures when it has none); the source is untouched. */
internal fun savedImageFile(folder: String?, pictures: File, now: Long) = File(folder?.takeIf { it.isNotEmpty() }?.let(::File) ?: pictures, "Image_$now.png")

/** Which picker pick the editor refuses before opening (ca/bt, ca/ca): svg, heif and unreadable files. */
@StringRes
internal fun editRefusal(fileName: String, decodable: Boolean): Int? {
    val ext = fileName.substringAfterLast('.', "").lowercase()
    return when {
        ext == "svg" -> R.string.toast_file
        ext == "heif" || ext == "heic" -> R.string.heif_image_toast
        !decodable -> R.string.toast_corrupted_image
        else -> null
    }
}

/** Smallest power-of-two sample size keeping the longer side within [maxSide]. */
internal fun sampleSizeFor(width: Int, height: Int, maxSide: Int): Int {
    var sample = 1
    while (maxOf(width, height) / sample > maxSide) sample *= 2
    return sample
}

/**
 * Decodes the working copy upright (EXIF orientation applied, as GPUImage's file loader does).
 * shortcut: photos over 4096 px on the long side are decoded downsampled, upgrade with tiled rendering if full-size output matters.
 */
internal fun decodeForEdit(file: File): Bitmap? {
    val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
    BitmapFactory.decodeFile(file.path, bounds)
    if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return null
    val opts = BitmapFactory.Options().apply {
        inSampleSize = sampleSizeFor(bounds.outWidth, bounds.outHeight, 4096)
        inPreferredConfig = Bitmap.Config.ARGB_8888
    }
    val bmp = BitmapFactory.decodeFile(file.path, opts) ?: return null
    val degrees = runCatching {
        when (ExifInterface(file.path).getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)) {
            ExifInterface.ORIENTATION_ROTATE_90 -> 90f
            ExifInterface.ORIENTATION_ROTATE_180 -> 180f
            ExifInterface.ORIENTATION_ROTATE_270 -> 270f
            else -> 0f
        }
    }.getOrDefault(0f)
    if (degrees == 0f) return bmp
    return Bitmap.createBitmap(bmp, 0, 0, bmp.width, bmp.height, Matrix().apply { postRotate(degrees) }, true)
}

/** Every applied tool rewrites the working copy as PNG at quality 100 (PE ~11706). */
internal fun writePng(bitmap: Bitmap, file: File): Boolean = replaceFile(file) { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }

/**
 * Writes [file] through a sibling temp file and renames it over [file] only after [write] returned true and the stream
 * closed, so a failed encode (full storage, OOM) leaves the previous content intact.
 */
internal fun replaceFile(file: File, write: (OutputStream) -> Boolean): Boolean {
    val tmp = File(file.path + ".tmp")
    val ok = runCatching { FileOutputStream(tmp).use { out -> write(out).also { if (it) out.fd.sync() } } }.getOrDefault(false) &&
        tmp.renameTo(file) // rename(2) replaces the target atomically on Android
    if (!ok) tmp.delete()
    return ok
}

/** A copy scaled so its longer side is at most [maxSide], for previews and filter thumbnails. */
internal fun scaledDown(src: Bitmap, maxSide: Int): Bitmap {
    val scale = maxSide.toFloat() / maxOf(src.width, src.height)
    if (scale >= 1f) return src
    return Bitmap.createScaledBitmap(src, (src.width * scale).toInt().coerceAtLeast(1), (src.height * scale).toInt().coerceAtLeast(1), true)
}
