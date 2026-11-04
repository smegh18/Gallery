package com.quizango.gallery.data.local.mediastore

import android.content.ContentUris
import android.content.Context
import android.database.Cursor
import android.os.Build
import android.provider.MediaStore
import com.quizango.gallery.domain.model.MediaItem
import com.quizango.gallery.domain.model.MediaType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

class MediaStoreScanner(private val context: Context) {

    private val dateFormat = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())

    suspend fun scanMedia(): List<MediaItem> = withContext(Dispatchers.IO) {
        val items = mutableListOf<MediaItem>()
        items.addAll(scanImages())
        items.addAll(scanVideos())
        items.sortedByDescending { it.dateModified }
    }

    private fun scanImages(): List<MediaItem> {
        val images = mutableListOf<MediaItem>()
        val projection = arrayOf(
            MediaStore.Images.Media._ID,
            MediaStore.Images.Media.DATA,
            MediaStore.Images.Media.DISPLAY_NAME,
            MediaStore.Images.Media.SIZE,
            MediaStore.Images.Media.DATE_MODIFIED,
            MediaStore.Images.Media.DATE_TAKEN,
            MediaStore.Images.Media.BUCKET_DISPLAY_NAME,
            MediaStore.Images.Media.WIDTH,
            MediaStore.Images.Media.HEIGHT,
            MediaStore.Images.Media.ORIENTATION
        )

        val sortOrder = "${MediaStore.Images.Media.DATE_MODIFIED} DESC"
        val queryUri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI

        context.contentResolver.query(queryUri, projection, null, null, sortOrder)?.use { cursor ->
            val idCol = cursor.getColumnIndexOrThrow(MediaStore.Images.Media._ID)
            val dataCol = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA)
            val nameCol = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DISPLAY_NAME)
            val sizeCol = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.SIZE)
            val modCol = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATE_MODIFIED)
            val takenCol = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATE_TAKEN)
            val bucketCol = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.BUCKET_DISPLAY_NAME)
            val widthCol = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.WIDTH)
            val heightCol = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.HEIGHT)
            val orientCol = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.ORIENTATION)

            while (cursor.moveToNext()) {
                val path = cursor.getString(dataCol) ?: continue
                val file = File(path)
                // MediaStore SIZE replaces a disk stat per file (slow over FUSE with thousands of photos)
                if (cursor.getLong(sizeCol) <= 0L) continue

                val id = cursor.getLong(idCol)
                val size = cursor.getLong(sizeCol)
                val name = cursor.getString(nameCol) ?: file.name
                val mod = cursor.getLong(modCol) * 1000L
                val taken = cursor.getLong(takenCol).let { if (it > 0) it else mod }
                val folderPath = file.parent ?: path.substringBeforeLast('/', "")
                val bucket = cursor.getString(bucketCol) ?: file.parentFile?.name ?: "Photos"
                val width = cursor.getInt(widthCol)
                val height = cursor.getInt(heightCol)
                val orient = cursor.getInt(orientCol)

                val isScreenshot = path.contains("screenshot", ignoreCase = true)
                val isGif = name.endsWith(".gif", ignoreCase = true)

                val mediaType = when {
                    isScreenshot -> MediaType.SCREENSHOT
                    isGif -> MediaType.ANIMATED
                    else -> MediaType.IMAGE
                }

                images.add(
                    MediaItem(
                        id = id,
                        filePath = path,
                        fileName = name,
                        fileSize = size,
                        folderName = bucket,
                        folderPath = folderPath,
                        dateModified = mod,
                        dateTaken = taken,
                        dateString = dateFormat.format(Date(taken)),
                        mediaType = mediaType,
                        isAnimated = isGif,
                        dimension = if (width > 0 && height > 0) "${width}x${height}" else "",
                        resolution = getResolutionLabel(width, height),
                        orientation = orient
                    )
                )
            }
        }
        return images
    }

    private fun scanVideos(): List<MediaItem> {
        val videos = mutableListOf<MediaItem>()
        val projection = arrayOf(
            MediaStore.Video.Media._ID,
            MediaStore.Video.Media.DATA,
            MediaStore.Video.Media.DISPLAY_NAME,
            MediaStore.Video.Media.SIZE,
            MediaStore.Video.Media.DATE_MODIFIED,
            MediaStore.Video.Media.DATE_TAKEN,
            MediaStore.Video.Media.BUCKET_DISPLAY_NAME,
            MediaStore.Video.Media.DURATION,
            MediaStore.Video.Media.WIDTH,
            MediaStore.Video.Media.HEIGHT
        )

        val sortOrder = "${MediaStore.Video.Media.DATE_MODIFIED} DESC"
        val queryUri = MediaStore.Video.Media.EXTERNAL_CONTENT_URI

        context.contentResolver.query(queryUri, projection, null, null, sortOrder)?.use { cursor ->
            val idCol = cursor.getColumnIndexOrThrow(MediaStore.Video.Media._ID)
            val dataCol = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DATA)
            val nameCol = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DISPLAY_NAME)
            val sizeCol = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.SIZE)
            val modCol = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DATE_MODIFIED)
            val takenCol = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DATE_TAKEN)
            val bucketCol = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.BUCKET_DISPLAY_NAME)
            val durCol = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DURATION)
            val widthCol = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.WIDTH)
            val heightCol = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.HEIGHT)

            while (cursor.moveToNext()) {
                val path = cursor.getString(dataCol) ?: continue
                val file = File(path)
                // MediaStore SIZE replaces a disk stat per file (slow over FUSE with thousands of photos)
                if (cursor.getLong(sizeCol) <= 0L) continue

                val id = cursor.getLong(idCol)
                val size = cursor.getLong(sizeCol)
                val name = cursor.getString(nameCol) ?: file.name
                val mod = cursor.getLong(modCol) * 1000L
                val taken = cursor.getLong(takenCol).let { if (it > 0) it else mod }
                val folderPath = file.parent ?: path.substringBeforeLast('/', "")
                val bucket = cursor.getString(bucketCol) ?: file.parentFile?.name ?: "Videos"
                val duration = cursor.getLong(durCol)
                val width = cursor.getInt(widthCol)
                val height = cursor.getInt(heightCol)

                videos.add(
                    MediaItem(
                        id = id,
                        filePath = path,
                        fileName = name,
                        fileSize = size,
                        folderName = bucket,
                        folderPath = folderPath,
                        dateModified = mod,
                        dateTaken = taken,
                        dateString = dateFormat.format(Date(taken)),
                        mediaType = MediaType.VIDEO,
                        durationMs = duration,
                        dimension = if (width > 0 && height > 0) "${width}x${height}" else "",
                        resolution = getResolutionLabel(width, height)
                    )
                )
            }
        }
        return videos
    }

    private fun getResolutionLabel(width: Int, height: Int): String {
        val maxDim = maxOf(width, height)
        return when {
            maxDim >= 3840 -> "4K"
            maxDim >= 1920 -> "FHD"
            maxDim >= 1280 -> "HD"
            maxDim > 0 -> "SD"
            else -> ""
        }
    }
}
