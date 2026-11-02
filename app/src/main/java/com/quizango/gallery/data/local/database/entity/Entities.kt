package com.quizango.gallery.data.local.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.quizango.gallery.domain.model.MediaItem
import com.quizango.gallery.domain.model.MediaType

@Entity(tableName = "images_table")
data class MediaEntity(
    @PrimaryKey
    val filePath: String,
    val id: Long = 0L,
    val fileName: String = "",
    val fileSize: Long = 0L,
    val folderName: String = "",
    val folderPath: String = "",
    val relativePath: String = "",
    val date: Long = 0L,
    val lastDate: Long = 0L,
    val stringDate: String = "",
    val deletedDate: Long = 0L,
    val mediaType: Int = 1,
    val animatedType: Int = 0,
    val videoDuration: Long = 0L,
    val dimension: String = "",
    val resolution: String = "",
    val orientation: Int = 0,
    val rotateAngle: Float = 0f,
    val isFavorite: Boolean = false,
    val isSimilar: Boolean = false,
    val isDuplicateHash: Boolean = false,
    val duplicateHash: String = "",
    val duplicateGroup: Long = 0L,
    val isLocation: Boolean = false,
    val lat: Float = 0f,
    val longs: Float = 0f,
    val place: String = "",
    val isPortrait: Boolean = false,
    val isMemories: Boolean = false,
    val memoriesList: List<String> = emptyList(),
    val isColorList: Boolean = false,
    val colorList: List<Int> = emptyList()
) {
    fun toDomain(): MediaItem = MediaItem(
        id = id,
        filePath = filePath,
        fileName = fileName,
        fileSize = fileSize,
        folderName = folderName,
        folderPath = folderPath,
        relativePath = relativePath,
        dateModified = lastDate,
        dateTaken = date,
        dateString = stringDate,
        deletedDate = deletedDate,
        mediaType = MediaType.fromRaw(mediaType),
        isAnimated = animatedType == 1,
        durationMs = videoDuration,
        dimension = dimension,
        resolution = resolution,
        orientation = orientation,
        rotateAngle = rotateAngle,
        isFavorite = isFavorite,
        isDuplicate = isDuplicateHash,
        duplicateGroup = duplicateGroup,
        duplicateHash = duplicateHash,
        isLocation = isLocation,
        latitude = lat,
        longitude = longs,
        placeName = place,
        isPortrait = isPortrait,
        dominantColors = colorList
    )

    companion object {
        fun fromDomain(item: MediaItem): MediaEntity = MediaEntity(
            filePath = item.filePath,
            id = item.id,
            fileName = item.fileName,
            fileSize = item.fileSize,
            folderName = item.folderName,
            folderPath = item.folderPath,
            relativePath = item.relativePath,
            date = item.dateTaken,
            lastDate = item.dateModified,
            stringDate = item.dateString,
            deletedDate = item.deletedDate,
            mediaType = item.mediaType.rawCode,
            animatedType = if (item.isAnimated) 1 else 0,
            videoDuration = item.durationMs,
            dimension = item.dimension,
            resolution = item.resolution,
            orientation = item.orientation,
            rotateAngle = item.rotateAngle,
            isFavorite = item.isFavorite,
            isDuplicateHash = item.isDuplicate,
            duplicateGroup = item.duplicateGroup,
            duplicateHash = item.duplicateHash,
            isLocation = item.isLocation,
            lat = item.latitude,
            longs = item.longitude,
            place = item.placeName,
            isPortrait = item.isPortrait,
            isColorList = item.dominantColors.isNotEmpty(),
            colorList = item.dominantColors
        )
    }
}

@Entity(tableName = "vault_table")
data class VaultEntity(
    @PrimaryKey
    val filePath: String,
    val fileName: String = "",
    val folderName: String = "",
    val folderPath: String = "",
    val fileSize: Long = 0L,
    val date: Long = 0L,
    val lastDate: Long = 0L,
    val stringDate: String = "",
    val mediaType: Int = 1,
    val animatedType: Int = 0,
    val videoDuration: Long = 0L,
    val dimension: String = "",
    val resolution: String = ""
)
