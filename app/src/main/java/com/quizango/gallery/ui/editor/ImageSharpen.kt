package com.quizango.gallery.ui.editor

import android.graphics.Bitmap

/**
 * Sharpen with a 3x3 Laplacian kernel: out = c + k * (4c - up - down - left - right).
 * [amount] is the slider value 0..100; 100 means k = 1. Alpha and edge pixels are kept.
 */
object ImageSharpen {

    fun sharpenPixels(src: IntArray, width: Int, height: Int, amount: Float): IntArray {
        val k = (amount / 100f).coerceIn(0f, 1f)
        if (k == 0f || width < 3 || height < 3) return src.copyOf()
        val out = src.copyOf()
        for (y in 1 until height - 1) {
            val row = y * width
            for (x in 1 until width - 1) {
                val i = row + x
                val c = src[i]
                val u = src[i - width]; val d = src[i + width]; val l = src[i - 1]; val r = src[i + 1]
                fun ch(shift: Int): Int {
                    val cv = (c shr shift) and 0xFF
                    val lap = 4 * cv - ((u shr shift) and 0xFF) - ((d shr shift) and 0xFF) -
                        ((l shr shift) and 0xFF) - ((r shr shift) and 0xFF)
                    return (cv + k * lap).toInt().coerceIn(0, 255)
                }
                out[i] = (c and 0xFF000000.toInt()) or (ch(16) shl 16) or (ch(8) shl 8) or ch(0)
            }
        }
        return out
    }

    fun sharpen(bitmap: Bitmap, amount: Float): Bitmap {
        if (amount <= 0f) return bitmap
        val w = bitmap.width
        val h = bitmap.height
        val px = IntArray(w * h).also { bitmap.getPixels(it, 0, w, 0, 0, w, h) }
        val result = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        result.setPixels(sharpenPixels(px, w, h, amount), 0, w, 0, 0, w, h)
        return result
    }
}
