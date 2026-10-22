package albums.gallery.photo.folder.picasa.app.web.gallery.data.repository

import albums.gallery.photo.folder.picasa.app.web.gallery.data.local.database.dao.GalleryDao
import albums.gallery.photo.folder.picasa.app.web.gallery.data.local.database.entity.MediaEntity
import albums.gallery.photo.folder.picasa.app.web.gallery.data.local.mediastore.MediaStoreScanner
import albums.gallery.photo.folder.picasa.app.web.gallery.domain.model.Album
import albums.gallery.photo.folder.picasa.app.web.gallery.domain.model.MediaItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import java.io.File

interface MediaRepository {
    fun getActiveMediaFlow(): Flow<List<MediaItem>>
    fun getAlbumsFlow(): Flow<List<Album>>
    fun getMediaByFolderFlow(folderPath: String): Flow<List<MediaItem>>
    fun getFavoritesFlow(): Flow<List<MediaItem>>
    fun getRecycleBinFlow(): Flow<List<MediaItem>>
    fun getDuplicatesFlow(): Flow<List<MediaItem>>
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

    override fun getActiveMediaFlow(): Flow<List<MediaItem>> {
        return dao.getAllActiveMediaFlow().map { list -> list.map { it.toDomain() } }
    }

    override fun getAlbumsFlow(): Flow<List<Album>> {
        return dao.getAllActiveMediaFlow().map { list ->
            val result = mutableListOf<Album>()

            // 1. All Photos (mediaType != 2)
            val photos = list.filter { it.mediaType != 2 }
            result.add(
                Album(
                    id = "filter:all_photos",
                    name = "All Photos",
                    path = "filter:all_photos",
                    coverPath = photos.firstOrNull()?.filePath ?: "",
                    itemCount = photos.size
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
                    itemCount = videos.size
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
                    itemCount = cameraItems.size
                )
            )

            // 4. Screenshots (Matching reference screenshot order)
            val screenshotItems = list.filter {
                it.folderPath.contains("screenshot", ignoreCase = true) ||
                it.folderName.contains("screenshot", ignoreCase = true)
            }
            if (screenshotItems.isNotEmpty()) {
                result.add(
                    Album(
                        id = "filter:screenshots",
                        name = "Screenshots",
                        path = "filter:screenshots",
                        coverPath = screenshotItems.firstOrNull()?.filePath ?: "",
                        itemCount = screenshotItems.size
                    )
                )
            }

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
                        itemCount = items.size
                    )
                }
                .sortedByDescending { it.itemCount }

            result.addAll(folders)
            result
        }
    }

    override fun getMediaByFolderFlow(folderPath: String): Flow<List<MediaItem>> {
        return when {
            folderPath == "filter:all_photos" -> {
                dao.getAllActiveMediaFlow().map { list ->
                    list.filter { it.mediaType != 2 }.map { it.toDomain() }
                }
            }
            folderPath == "filter:camera" -> {
                dao.getAllActiveMediaFlow().map { list ->
                    list.filter {
                        it.folderPath.contains("Camera", ignoreCase = true) ||
                        it.folderName.equals("Camera", ignoreCase = true) ||
                        it.filePath.contains("/DCIM/Camera", ignoreCase = true)
                    }.map { it.toDomain() }
                }
            }
            folderPath == "filter:screenshots" -> {
                dao.getAllActiveMediaFlow().map { list ->
                    list.filter {
                        it.folderPath.contains("screenshot", ignoreCase = true) ||
                        it.folderName.contains("screenshot", ignoreCase = true)
                    }.map { it.toDomain() }
                }
            }
            folderPath == "filter:videos" -> {
                dao.getAllActiveMediaFlow().map { list ->
                    list.filter { it.mediaType == 2 }.map { it.toDomain() }
                }
            }
            folderPath == "filter:portrait" -> {
                dao.getAllActiveMediaFlow().map { list ->
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
                dao.getAllActiveMediaFlow().map { list ->
                    list.filter {
                        it.mediaType == 1 && (it.fileSize >= 1_500_000 || it.dimension.contains("1920") || it.dimension.contains("1080") || it.dimension.contains("4K") || it.resolution.contains("1080"))
                    }.map { it.toDomain() }
                }
            }
            folderPath == "filter:animated" -> {
                dao.getAllActiveMediaFlow().map { list ->
                    list.filter {
                        it.animatedType == 1 || it.filePath.endsWith(".gif", ignoreCase = true)
                    }.map { it.toDomain() }
                }
            }
            folderPath == "filter:burst" -> {
                dao.getAllActiveMediaFlow().map { list ->
                    list.filter {
                        it.filePath.contains("BURST", ignoreCase = true) || it.fileName.contains("BURST", ignoreCase = true)
                    }.map { it.toDomain() }
                }
            }
            else -> {
                dao.getMediaByFolderFlow(folderPath).map { list -> list.map { it.toDomain() } }
            }
        }
    }

    override fun getFavoritesFlow(): Flow<List<MediaItem>> {
        return dao.getFavoritesFlow().map { list -> list.map { it.toDomain() } }
    }

    override fun getRecycleBinFlow(): Flow<List<MediaItem>> {
        return dao.getRecycleBinFlow().map { list -> list.map { it.toDomain() } }
    }

    override fun getDuplicatesFlow(): Flow<List<MediaItem>> {
        return dao.getDuplicatesFlow().map { list -> list.map { it.toDomain() } }
    }

    override suspend fun syncMediaStore() = withContext(Dispatchers.IO) {
        val scanned = scanner.scanMedia()
        val entities = scanned.map { MediaEntity.fromDomain(it) }
        dao.insertAll(entities)
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
