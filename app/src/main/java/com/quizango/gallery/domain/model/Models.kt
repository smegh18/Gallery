package com.quizango.gallery.domain.model

import android.content.ContentUris
import android.net.Uri
import android.provider.MediaStore
import java.io.File

enum class MediaType(val rawCode: Int) {
    IMAGE(1),
    VIDEO(2),
    SCREENSHOT(4),
    ANIMATED(16),
    BURST(64);

    val isImage: Boolean get() = this != VIDEO

    companion object {
        fun fromRaw(code: Int): MediaType {
            return entries.firstOrNull { it.rawCode == code } ?: IMAGE
        }
    }
}

data class MediaItem(
    val id: Long,
    val filePath: String,
    val fileName: String,
    val fileSize: Long,
    val folderName: String,
    val folderPath: String,
    val relativePath: String = "",
    val dateModified: Long,
    val dateTaken: Long,
    val dateString: String = "",
    val deletedDate: Long = 0L,
    val mediaType: MediaType = MediaType.IMAGE,
    val isAnimated: Boolean = false,
    val durationMs: Long = 0L,
    val dimension: String = "",
    val resolution: String = "",
    val orientation: Int = 0,
    val rotateAngle: Float = 0f,
    val isFavorite: Boolean = false,
    val isDuplicate: Boolean = false,
    val duplicateGroup: Long = 0L,
    val duplicateHash: String = "",
    val isLocation: Boolean = false,
    val latitude: Float = 0f,
    val longitude: Float = 0f,
    val placeName: String = "",
    val isPortrait: Boolean = false,
    val dominantColors: List<Int> = emptyList()
) {
    val dateAdded: Long
        get() = if (dateTaken > 0L) dateTaken else dateModified

    val contentUri: Any
        get() = if (id > 0) {
            if (mediaType == MediaType.VIDEO) {
                ContentUris.withAppendedId(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, id)
            } else {
                ContentUris.withAppendedId(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, id)
            }
        } else {
            File(filePath)
        }

    val isInRecycleBin: Boolean
        get() = deletedDate > 0L

    val isVideo: Boolean
        get() = mediaType == MediaType.VIDEO

    val formattedDuration: String
        get() {
            if (!isVideo || durationMs <= 0) return ""
            val totalSeconds = durationMs / 1000
            val minutes = totalSeconds / 60
            val seconds = totalSeconds % 60
            return "%02d:%02d".format(minutes, seconds)
        }
}

data class Album(
    val id: String,
    val name: String,
    val path: String,
    val coverPath: String,
    val itemCount: Int,
    val isPinned: Boolean = false,
    val coverId: Long = 0L,
    val isVideoCover: Boolean = false
) {
    val coverUri: Any
        get() = if (coverId > 0) {
            if (isVideoCover) {
                ContentUris.withAppendedId(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, coverId)
            } else {
                ContentUris.withAppendedId(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, coverId)
            }
        } else {
            File(coverPath)
        }
}

data class VaultItem(
    val filePath: String,
    val fileName: String,
    val folderName: String,
    val folderPath: String,
    val fileSize: Long,
    val dateHidden: Long,
    val mediaType: MediaType = MediaType.IMAGE,
    val durationMs: Long = 0L,
    val dimension: String = "",
    val resolution: String = ""
)

enum class SortField {
    DATE_MODIFIED,
    DATE_TAKEN,
    NAME,
    SIZE
}

enum class SortDirection {
    ASCENDING,
    DESCENDING
}

data class SortOrder(
    val field: SortField = SortField.DATE_MODIFIED,
    val direction: SortDirection = SortDirection.DESCENDING
) {
    companion object {
        val DATE_DESC = SortOrder(SortField.DATE_MODIFIED, SortDirection.DESCENDING)
        val DATE_ASC = SortOrder(SortField.DATE_MODIFIED, SortDirection.ASCENDING)
        val NAME_ASC = SortOrder(SortField.NAME, SortDirection.ASCENDING)
        val NAME_DESC = SortOrder(SortField.NAME, SortDirection.DESCENDING)
        val SIZE_DESC = SortOrder(SortField.SIZE, SortDirection.DESCENDING)
        val SIZE_ASC = SortOrder(SortField.SIZE, SortDirection.ASCENDING)
    }
}
