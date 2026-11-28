package com.quizango.gallery.data.local.mediastore

import android.content.Context
import android.net.Uri
import android.os.Build
import android.util.Size
import androidx.core.graphics.drawable.toDrawable
import coil.ImageLoader
import coil.decode.DataSource
import coil.fetch.DrawableResult
import coil.fetch.FetchResult
import coil.fetch.Fetcher
import coil.request.Options
import coil.size.Scale
import coil.size.pxOrElse
import kotlin.math.roundToInt

class MediaStoreThumbnailFetcher(
    private val context: Context,
    private val uri: Uri,
    private val options: Options
) : Fetcher {

    override suspend fun fetch(): FetchResult? {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) return null
        return try {
            val width = options.size.width.pxOrElse { 512 }.coerceAtLeast(120)
            val height = options.size.height.pxOrElse { 512 }.coerceAtLeast(120)
            var bitmap = context.contentResolver.loadThumbnail(uri, Size(width, height), null)
            // loadThumbnail fits inside the size; a cropped cell needs it to cover, or a tall screenshot is upscaled and soft
            val grow = coverGrowth(bitmap.width, bitmap.height, width, height, options.scale == Scale.FILL)
            if (grow > 1f) {
                bitmap = context.contentResolver.loadThumbnail(uri, Size((width * grow).roundToInt(), (height * grow).roundToInt()), null)
            }
            DrawableResult(
                drawable = bitmap.toDrawable(context.resources),
                isSampled = true,
                dataSource = DataSource.DISK
            )
        } catch (_: Exception) {
            // Fall back to standard Coil fetcher pipeline if OS thumbnail is unavailable
            null
        }
    }

    companion object {
        /** How much bigger to request so a [got] thumbnail covers [want] when cropping (FILL); 1 when it already does or when fitting. */
        internal fun coverGrowth(gotW: Int, gotH: Int, wantW: Int, wantH: Int, fill: Boolean): Float {
            if (!fill || gotW <= 0 || gotH <= 0) return 1f
            val cover = maxOf(wantW.toFloat() / gotW, wantH.toFloat() / gotH)
            return if (cover > 1.05f) cover else 1f // a few % short is invisible; skip the second load
        }
    }

    class Factory(private val context: Context) : Fetcher.Factory<Uri> {
        override fun create(data: Uri, options: Options, imageLoader: ImageLoader): Fetcher? {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q &&
                data.scheme == "content" &&
                (data.authority == "media" || data.authority?.contains("media") == true)
            ) {
                return MediaStoreThumbnailFetcher(context, data, options)
            }
            return null
        }
    }
}
