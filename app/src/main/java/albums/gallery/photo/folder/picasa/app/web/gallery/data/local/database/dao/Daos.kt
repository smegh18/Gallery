package albums.gallery.photo.folder.picasa.app.web.gallery.data.local.database.dao

import androidx.room.*
import albums.gallery.photo.folder.picasa.app.web.gallery.data.local.database.entity.MediaEntity
import albums.gallery.photo.folder.picasa.app.web.gallery.data.local.database.entity.VaultEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface GalleryDao {
    @Query("SELECT * FROM images_table WHERE deletedDate = 0 ORDER BY lastDate DESC")
    fun getAllActiveMediaFlow(): Flow<List<MediaEntity>>

    @Query("SELECT * FROM images_table WHERE deletedDate = 0 ORDER BY lastDate DESC")
    suspend fun getAllActiveMedia(): List<MediaEntity>

    @Query("SELECT * FROM images_table WHERE deletedDate = 0 AND folderPath = :folderPath ORDER BY lastDate DESC")
    fun getMediaByFolderFlow(folderPath: String): Flow<List<MediaEntity>>

    @Query("SELECT * FROM images_table WHERE deletedDate = 0 AND isFavorite = 1 ORDER BY lastDate DESC")
    fun getFavoritesFlow(): Flow<List<MediaEntity>>

    @Query("SELECT * FROM images_table WHERE deletedDate > 0 ORDER BY deletedDate DESC")
    fun getRecycleBinFlow(): Flow<List<MediaEntity>>

    @Query("SELECT * FROM images_table WHERE filePath = :path LIMIT 1")
    suspend fun getMediaByPath(path: String): MediaEntity?

    @Query("SELECT * FROM images_table WHERE deletedDate = 0 AND mediaType = 2 ORDER BY lastDate DESC")
    fun getVideosFlow(): Flow<List<MediaEntity>>

    @Query("SELECT * FROM images_table WHERE deletedDate = 0 AND mediaType = 4 ORDER BY lastDate DESC")
    fun getScreenshotsFlow(): Flow<List<MediaEntity>>

    @Query("SELECT * FROM images_table WHERE deletedDate = 0 AND duplicateGroup != 0 ORDER BY duplicateGroup, lastDate DESC")
    fun getDuplicatesFlow(): Flow<List<MediaEntity>>

    @Query("SELECT * FROM images_table WHERE deletedDate = 0 AND isLocation = 1 AND place != '' ORDER BY lastDate DESC")
    fun getGeotaggedMediaFlow(): Flow<List<MediaEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(media: List<MediaEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(media: MediaEntity)

    @Update
    suspend fun update(media: MediaEntity)

    @Delete
    suspend fun delete(media: MediaEntity)

    @Query("DELETE FROM images_table WHERE filePath = :path")
    suspend fun deleteByPath(path: String)

    @Query("DELETE FROM images_table WHERE filePath IN (:paths)")
    suspend fun deleteByPaths(paths: List<String>)

    @Query("UPDATE images_table SET deletedDate = :timestamp WHERE filePath = :path")
    suspend fun moveToTrash(path: String, timestamp: Long = System.currentTimeMillis())

    @Query("UPDATE images_table SET deletedDate = 0 WHERE filePath = :path")
    suspend fun restoreFromTrash(path: String)

    @Query("UPDATE images_table SET isFavorite = :favorite WHERE filePath = :path")
    suspend fun setFavorite(path: String, favorite: Boolean)

    @Query("DELETE FROM images_table WHERE deletedDate > 0 AND deletedDate < :thresholdTimestamp")
    suspend fun purgeExpiredTrash(thresholdTimestamp: Long)
}

@Dao
interface VaultDao {
    @Query("SELECT * FROM vault_table ORDER BY lastDate DESC")
    fun getAllVaultItemsFlow(): Flow<List<VaultEntity>>

    @Query("SELECT * FROM vault_table ORDER BY lastDate DESC")
    suspend fun getAllVaultItems(): List<VaultEntity>

    @Query("SELECT * FROM vault_table WHERE filePath = :path LIMIT 1")
    suspend fun getVaultItem(path: String): VaultEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(item: VaultEntity)

    @Delete
    suspend fun delete(item: VaultEntity)

    @Query("DELETE FROM vault_table WHERE filePath IN (:paths)")
    suspend fun deleteByPaths(paths: List<String>)
}
