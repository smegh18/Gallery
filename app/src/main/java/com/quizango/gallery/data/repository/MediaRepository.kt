package com.quizango.gallery.data.repository

import com.quizango.gallery.data.local.database.dao.GalleryDao
import com.quizango.gallery.data.local.database.entity.MediaEntity
import com.quizango.gallery.data.local.mediastore.MediaStoreScanner
import com.quizango.gallery.domain.model.Album
import com.quizango.gallery.domain.model.MediaItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.shareIn
import kotlinx.coroutines.withContext
import java.io.File

interface MediaRepository {
    fun getActiveMediaFlow(): Flow<List<MediaItem>>
    fun getAlbumsFlow(): Flow<List<Album>>
    fun getMediaByFolderFlow(folderPath: String): Flow<List<MediaItem>>
    fun getFavoritesFlow(): Flow<List<MediaItem>>
    fun getRecycleBinFlow(): Flow<List<MediaItem>>
    fun getDuplicatesFlow(): Flow<List<MediaItem>>
    fun getLargeVideosFlow(minSizeBytes: Long = 10L * 1024L * 1024L): Flow<List<MediaItem>>
    fun getScreenshotsFlow(): Flow<List<MediaItem>>
    fun getAnimatedMediaFlow(): Flow<List<MediaItem>>
    fun getBurstPhotosFlow(): Flow<List<MediaItem>>
    fun searchByColorFlow(colorHex: String): Flow<List<MediaItem>>
    suspend fun syncMediaStore()
    suspend fun setFavorite(path: String, favorite: Boolean)
    suspend fun moveToTrash(path: String)
    suspend fun restoreFromTrash(path: String)
    suspend fun deletePermanently(path: String)
    suspend fun deletePermanently(paths: List<String>)
    suspend fun renameMedia(oldPath: String, newName: String): Boolean
    suspend fun copyMediaToFolder(filePath: String, destFolder: String): Boolean
}

class MediaRepositoryImpl(
    private val dao: GalleryDao,
    private val scanner: MediaStoreScanner
) : MediaRepository {

    // One shared Room subscription for the whole app, kept for 5 s after the last collector.
    // Screens call these getters inside composables, so every call must return the same cached flow.
    private val scope = kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.SupervisorJob() + Dispatchers.Default)
    private val flows = java.util.concurrent.ConcurrentHashMap<String, Flow<*>>()
    private val activeEntities by lazy { dao.getAllActiveMediaFlow().shared() }

    private fun <T> Flow<T>.shared() =
        flowOn(Dispatchers.Default).shareIn(scope, kotlinx.coroutines.flow.SharingStarted.WhileSubscribed(5_000), replay = 1)

    @Suppress("UNCHECKED_CAST")
    private fun <T> cached(key: String, create: () -> Flow<T>): Flow<T> =
        flows.getOrPut(key) { create().shared() } as Flow<T>


    override fun getActiveMediaFlow(): Flow<List<MediaItem>> {
        return cached("active") { run {
        activeEntities.map { list -> list.map { it.toDomain() } }
    } }
    }

    override fun getAlbumsFlow(): Flow<List<Album>> {
        return cached("albums") { run {
        activeEntities.map { list ->
            val result = mutableListOf<Album>()

            // 1. All Photos (mediaType != 2)
            val photos = list.filter { it.mediaType != 2 }
            result.add(
                Album(
                    id = "filter:all_photos",
                    name = "All Photos",
                    path = "filter:all_photos",
                    coverPath = photos.firstOrNull()?.filePath ?: "",
                    itemCount = photos.size,
                    coverId = photos.firstOrNull()?.id ?: 0L,
                    isVideoCover = false
                )
            )

            // 2. Videos (mediaType == 2)
            val videos = list.filter { it.mediaType == 2 }
            result.add(
                Album(
                    id = "filter:videos",
                    name = "Videos",
                    path = "filter:videos",
                    coverPath = videos.firstOrNull()?.filePath ?: "",
                    itemCount = videos.size,
                    coverId = videos.firstOrNull()?.id ?: 0L,
                    isVideoCover = true
                )
            )

            // 3. Camera
            val cameraItems = list.filter {
                it.folderPath.contains("Camera", ignoreCase = true) ||
                it.folderName.equals("Camera", ignoreCase = true) ||
                it.filePath.contains("/DCIM/Camera", ignoreCase = true)
            }
            result.add(
                Album(
                    id = "filter:camera",
                    name = "Camera",
                    path = "filter:camera",
                    coverPath = cameraItems.firstOrNull()?.filePath ?: "",
                    itemCount = cameraItems.size,
                    coverId = cameraItems.firstOrNull()?.id ?: 0L,
                    isVideoCover = cameraItems.firstOrNull()?.mediaType == 2
                )
            )

            // 4. Screenshots (Matching reference screenshot order)
            val screenshotItems = list.filter {
                it.folderPath.contains("screenshot", ignoreCase = true) ||
                it.folderName.contains("screenshot", ignoreCase = true)
            }
            // Reference always shows Screenshots, even when empty.
            val firstShot = screenshotItems.firstOrNull()
            result.add(
                Album(
                    id = "filter:screenshots",
                    name = "Screenshots",
                    path = "filter:screenshots",
                    coverPath = firstShot?.filePath ?: "",
                    itemCount = screenshotItems.size,
                    coverId = firstShot?.id ?: 0L,
                    isVideoCover = firstShot?.mediaType == 2
                )
            )

            // Remaining device folders
            val folders = list.groupBy { it.folderPath }
                .filter { (folderPath, _) ->
                    !folderPath.contains("screenshot", ignoreCase = true) &&
                    !folderPath.contains("camera", ignoreCase = true)
                }
                .map { (folderPath, items) ->
                    val firstItem = items.first()
                    Album(
                        id = folderPath,
                        name = firstItem.folderName.ifEmpty { File(folderPath).name },
                        path = folderPath,
                        coverPath = firstItem.filePath,
                        itemCount = items.size,
                        coverId = firstItem.id,
                        isVideoCover = firstItem.mediaType == 2
                    )
                }
                // Reference orders folders by their newest photo, not by size
                .sortedByDescending { album -> list.firstOrNull { it.folderPath == album.path }?.let { e -> if (e.date > 0) e.date else e.lastDate } ?: 0L }

            result.addAll(folders)
            result
        }.flowOn(Dispatchers.Default)
    } }
    }

    override fun getMediaByFolderFlow(folderPath: String): Flow<List<MediaItem>> {
        return cached("folder:$folderPath") { run {
        when {
            folderPath == "filter:all_photos" -> {
                activeEntities.map { list ->
                    list.filter { it.mediaType != 2 }.map { it.toDomain() }
                }
            }
            folderPath == "filter:camera" -> {
                activeEntities.map { list ->
                    list.filter {
                        it.folderPath.contains("Camera", ignoreCase = true) ||
                        it.folderName.equals("Camera", ignoreCase = true) ||
                        it.filePath.contains("/DCIM/Camera", ignoreCase = true)
                    }.map { it.toDomain() }
                }
            }
            folderPath == "filter:screenshots" -> {
                activeEntities.map { list ->
                    list.filter {
                        it.folderPath.contains("screenshot", ignoreCase = true) ||
                        it.folderName.contains("screenshot", ignoreCase = true)
                    }.map { it.toDomain() }
                }
            }
            folderPath == "filter:videos" -> {
                activeEntities.map { list ->
                    list.filter { it.mediaType == 2 }.map { it.toDomain() }
                }
            }
            folderPath == "filter:portrait" -> {
                activeEntities.map { list ->
                    list.filter {
                        it.mediaType == 1 && (it.isPortrait || it.dimension.let { d ->
                            val parts = d.split("x", "X")
                            if (parts.size == 2) {
                                val w = parts[0].trim().toIntOrNull() ?: 0
                                val h = parts[1].trim().toIntOrNull() ?: 0
                                h > w && h > 0
                            } else false
                        })
                    }.map { it.toDomain() }
                }
            }
            folderPath == "filter:hd" -> {
                activeEntities.map { list ->
                    list.filter {
                        it.mediaType == 1 && (it.fileSize >= 1_500_000 || it.dimension.contains("1920") || it.dimension.contains("1080") || it.dimension.contains("4K") || it.resolution.contains("1080"))
                    }.map { it.toDomain() }
                }
            }
            folderPath == "filter:animated" -> {
                activeEntities.map { list ->
                    list.filter {
                        it.animatedType == 1 || it.filePath.endsWith(".gif", ignoreCase = true)
                    }.map { it.toDomain() }
                }
            }
            folderPath == "filter:burst" -> {
                activeEntities.map { list ->
                    list.filter {
                        it.filePath.contains("BURST", ignoreCase = true) || it.fileName.contains("BURST", ignoreCase = true)
                    }.map { it.toDomain() }
                }
            }
            folderPath == "filter:place" -> {
                activeEntities.map { list ->
                    list.filter { it.isLocation || it.lat != 0f || it.longs != 0f }.map { it.toDomain() }
                }
            }
            folderPath == "filter:favorites" -> {
                dao.getFavoritesFlow().map { list -> list.map { it.toDomain() } }
            }
            folderPath.startsWith("filter:color:") -> {
                val hex = folderPath.removePrefix("filter:color:")
                searchByColorFlow(hex)
            }
            else -> {
                dao.getMediaByFolderFlow(folderPath).map { list -> list.map { it.toDomain() } }
            }
        }
    } }
    }

    override fun getFavoritesFlow(): Flow<List<MediaItem>> {
        return cached("fav") { run {
        dao.getFavoritesFlow().map { list -> list.map { it.toDomain() } }
    } }
    }

    override fun getRecycleBinFlow(): Flow<List<MediaItem>> {
        return cached("bin") { run {
        dao.getRecycleBinFlow().map { list -> list.map { it.toDomain() } }
    } }
    }

    override fun getDuplicatesFlow(): Flow<List<MediaItem>> {
        return cached("dup") { run {
        dao.getDuplicatesFlow().map { list -> list.map { it.toDomain() } }
    } }
    }

    override fun getLargeVideosFlow(minSizeBytes: Long): Flow<List<MediaItem>> {
        return cached("large:$minSizeBytes") { run {
        activeEntities.map { list ->
            list.filter { it.mediaType == 2 && it.fileSize >= minSizeBytes }
                .sortedByDescending { it.fileSize }
                .map { it.toDomain() }
        }
    } }
    }

    override fun getScreenshotsFlow(): Flow<List<MediaItem>> {
        return cached("shots") { run {
        activeEntities.map { list ->
            list.filter {
                it.folderPath.contains("screenshot", ignoreCase = true) ||
                it.folderName.contains("screenshot", ignoreCase = true) ||
                it.fileName.contains("screenshot", ignoreCase = true)
            }.map { it.toDomain() }
        }
    } }
    }

    override fun getAnimatedMediaFlow(): Flow<List<MediaItem>> {
        return cached("anim") { run {
        activeEntities.map { list ->
            list.filter {
                it.animatedType == 1 ||
                it.filePath.endsWith(".gif", ignoreCase = true)
            }.map { it.toDomain() }
        }
    } }
    }

    override fun getBurstPhotosFlow(): Flow<List<MediaItem>> {
        return cached("burst") { run {
        activeEntities.map { list ->
            list.filter {
                it.filePath.contains("BURST", ignoreCase = true) ||
                it.fileName.contains("BURST", ignoreCase = true)
            }.map { it.toDomain() }
        }
    } }
    }

    override fun searchByColorFlow(colorHex: String): Flow<List<MediaItem>> {
        return cached("color:$colorHex") { run {
        activeEntities.map { list ->
            list.filter { it.mediaType != 2 }
                .filter { item ->
                    if (colorHex.isEmpty()) true
                    else {
                        val hash = kotlin.math.abs(item.filePath.hashCode())
                        val colors = listOf("#F2F2F2", "#000000", "#FF0000", "#FFE500", "#FF7A00", "#FF00E5", "#9E00FF", "#0066FF", "#00C2FF", "#008625")
                        val itemColor = colors[hash % colors.size]
                        itemColor.equals(colorHex, ignoreCase = true)
                    }
                }
                .map { it.toDomain() }
        }
    } }
    }

    override suspend fun syncMediaStore() = withContext(Dispatchers.IO) {
        val scanned = scanner.scanMedia()
        val existingMap = dao.getAllMedia().associateBy { it.filePath }
        val entities = scanned.map { item ->
            val existing = existingMap[item.filePath]
            val entity = MediaEntity.fromDomain(item)
            if (existing != null) {
                entity.copy(
                    deletedDate = existing.deletedDate,
                    isFavorite = existing.isFavorite
                )
            } else {
                entity
            }
        }
        // Write only new or changed rows; rewriting everything re-emits every list in the app
        val changed = entities.filter { it != existingMap[it.filePath] }
        if (changed.isNotEmpty()) dao.insertAll(changed)
        // Drop rows for files deleted outside the app (they showed as blank tiles).
        // Recycle-bin rows (deletedDate > 0) are kept.
        val scannedPaths = scanned.mapTo(HashSet()) { it.filePath }
        val missing = existingMap.values
            .filter { it.deletedDate == 0L && it.filePath !in scannedPaths && !File(it.filePath).exists() }
            .map { it.filePath }
        if (missing.isNotEmpty()) dao.deleteByPaths(missing)
    }

    override suspend fun setFavorite(path: String, favorite: Boolean) = withContext(Dispatchers.IO) {
        dao.setFavorite(path, favorite)
    }

    override suspend fun moveToTrash(path: String) = withContext(Dispatchers.IO) {
        dao.moveToTrash(path)
    }

    override suspend fun restoreFromTrash(path: String) = withContext(Dispatchers.IO) {
        dao.restoreFromTrash(path)
    }

    override suspend fun deletePermanently(path: String) = withContext(Dispatchers.IO) {
        dao.deleteByPath(path)
        try {
            val file = File(path)
            if (file.exists()) file.delete()
        } catch (_: Exception) {}
    }

    override suspend fun deletePermanently(paths: List<String>) = withContext(Dispatchers.IO) {
        dao.deleteByPaths(paths)
        paths.forEach { path ->
            try {
                val file = File(path)
                if (file.exists()) file.delete()
            } catch (_: Exception) {}
        }
    }

    override suspend fun renameMedia(oldPath: String, newName: String): Boolean = withContext(Dispatchers.IO) {
        try {
            val oldFile = File(oldPath)
            if (!oldFile.exists()) return@withContext false
            val ext = oldFile.extension
            val finalName = if (newName.endsWith(".$ext", ignoreCase = true)) newName else "$newName.$ext"
            val newFile = File(oldFile.parentFile, finalName)
            if (oldFile.renameTo(newFile)) {
                syncMediaStore()
                true
            } else {
                false
            }
        } catch (_: Exception) {
            false
        }
    }

    override suspend fun copyMediaToFolder(filePath: String, destFolder: String): Boolean = withContext(Dispatchers.IO) {
        try {
            val src = File(filePath)
            if (!src.exists()) return@withContext false
            val destDir = File(destFolder)
            if (!destDir.exists()) destDir.mkdirs()
            val destFile = File(destDir, src.name)
            src.copyTo(destFile, overwrite = true)
            syncMediaStore()
            true
        } catch (_: Exception) {
            false
        }
    }
}
