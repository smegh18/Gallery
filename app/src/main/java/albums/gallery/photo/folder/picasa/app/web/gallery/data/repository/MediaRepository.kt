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
            list.groupBy { it.folderPath }
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
        }
    }

    override fun getMediaByFolderFlow(folderPath: String): Flow<List<MediaItem>> {
        return dao.getMediaByFolderFlow(folderPath).map { list -> list.map { it.toDomain() } }
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
}
