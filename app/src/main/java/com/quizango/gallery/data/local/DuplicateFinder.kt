package com.quizango.gallery.data.local

import android.content.ContentUris
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Matrix
import android.media.ThumbnailUtils
import android.os.Build
import android.provider.MediaStore
import android.util.Size
import com.quizango.gallery.domain.model.MediaItem
import com.quizango.gallery.domain.model.MediaType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.File
import java.security.MessageDigest

/**
 * Duplicate photos as the reference DuplicateFindService finds them: 32x32 system thumbnail,
 * scaled to 8x8, PNG, MD5. Photos with the same fingerprint are duplicates.
 * Fingerprints are cached per file (path + size + date), so later scans only hash new photos.
 */
class DuplicateFinder(private val context: Context) {
    private val file = File(context.filesDir, "duplicate_hashes.tsv")
    private val lock = Mutex()
    private var cache: MutableMap<String, String>? = null

    /** Fingerprint per photo path; [onProgress] gets 0..1 while new photos are hashed. */
    suspend fun fingerprints(media: List<MediaItem>, onProgress: (Float) -> Unit = {}): Map<String, String> = lock.withLock {
        withContext(Dispatchers.IO) {
            val known = cache ?: (if (file.exists()) parse(file.readLines()) else mutableMapOf()).also { cache = it }
            val photos = media.filter { it.mediaType != MediaType.VIDEO }
            val todo = photos.filter { key(it) !in known }
            var done = 0
            coroutineScope {
                todo.chunked(32).forEach { batch ->
                    val results = batch.map { item -> async { key(item) to (runCatching { fingerprint(item) }.getOrNull() ?: "") } }.awaitAll()
                    results.forEach { (k, v) -> known[k] = v }
                    file.appendText(results.joinToString("") { (k, v) -> line(k, v) })
                    done += batch.size
                    onProgress(done.toFloat() / todo.size)
                }
            }
            photos.associate { it.filePath to known[key(it)].orEmpty() }.filterValues { it.isNotEmpty() }
        }
    }

    private fun fingerprint(item: MediaItem): String? {
        val thumb = thumbnail(item) ?: return null
        val small = Bitmap.createBitmap(thumb, 0, 0, thumb.width, thumb.height,
            Matrix().apply { postScale(8f / thumb.width, 8f / thumb.height) }, false)
        val bytes = ByteArrayOutputStream().also { small.compress(Bitmap.CompressFormat.PNG, 100, it) }.toByteArray()
        if (small !== thumb) small.recycle()
        thumb.recycle()
        return hex(MessageDigest.getInstance("MD5").digest(bytes))
    }

    private fun thumbnail(item: MediaItem): Bitmap? {
        if (item.id <= 0) return null
        return if (Build.VERSION.SDK_INT >= 29) {
            val uri = ContentUris.withAppendedId(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, item.id)
            context.contentResolver.loadThumbnail(uri, Size(32, 32), null)
        } else {
            @Suppress("DEPRECATION")
            MediaStore.Images.Thumbnails.getThumbnail(context.contentResolver, item.id, MediaStore.Images.Thumbnails.MINI_KIND, null)
                ?.let { ThumbnailUtils.extractThumbnail(it, 32, 32) }
        }
    }

    companion object {
        /** A changed file (new size or date) gets a new fingerprint. */
        fun key(item: MediaItem) = "${item.filePath}|${item.fileSize}|${item.dateModified}"

        fun hex(bytes: ByteArray) = bytes.joinToString("") { "%02x".format(it) }

        fun line(key: String, hash: String) = "${key.replace('\t', ' ').replace('\n', ' ')}\t$hash\n"

        fun parse(lines: List<String>): MutableMap<String, String> =
            lines.mapNotNull { l -> l.split('\t').takeIf { it.size == 2 }?.let { it[0] to it[1] } }.toMap(mutableMapOf())

        /** Groups of 2+ photos with the same fingerprint, newest first inside each group. */
        fun group(media: List<MediaItem>, fingerprints: Map<String, String>): List<List<MediaItem>> =
            media.filter { fingerprints[it.filePath] != null }
                .groupBy { fingerprints.getValue(it.filePath) }
                .values.filter { it.size > 1 }
                .map { g -> g.sortedByDescending { it.dateModified } }
    }
}
