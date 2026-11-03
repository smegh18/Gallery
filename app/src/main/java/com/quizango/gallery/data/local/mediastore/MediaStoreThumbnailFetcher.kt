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
import coil.size.pxOrElse

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
            val bitmap = context.contentResolver.loadThumbnail(uri, Size(width, height), null)
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
