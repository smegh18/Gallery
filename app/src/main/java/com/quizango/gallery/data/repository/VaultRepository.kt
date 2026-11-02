package com.quizango.gallery.data.repository

import android.content.Context
import com.quizango.gallery.data.local.database.dao.GalleryDao
import com.quizango.gallery.data.local.database.dao.VaultDao
import com.quizango.gallery.data.local.database.entity.VaultEntity
import com.quizango.gallery.domain.model.MediaItem
import com.quizango.gallery.domain.model.MediaType
import com.quizango.gallery.domain.model.VaultItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import java.io.File
import java.util.UUID

interface VaultRepository {
    fun getVaultItemsFlow(): Flow<List<VaultItem>>
    suspend fun moveToVault(mediaItem: MediaItem): Boolean
    suspend fun restoreFromVault(vaultItem: VaultItem): Boolean
    suspend fun deleteFromVault(vaultItem: VaultItem): Boolean
}

class VaultRepositoryImpl(
    private val context: Context,
    private val vaultDao: VaultDao,
    private val galleryDao: GalleryDao
) : VaultRepository {

    private val vaultDir: File
        get() = File(context.filesDir, "vault_encrypted").also { if (!it.exists()) it.mkdirs() }

    override fun getVaultItemsFlow(): Flow<List<VaultItem>> {
        return vaultDao.getAllVaultItemsFlow().map { list ->
            list.map { entity ->
                VaultItem(
                    filePath = entity.filePath,
                    fileName = entity.fileName,
                    folderName = entity.folderName,
                    folderPath = entity.folderPath,
                    fileSize = entity.fileSize,
                    dateHidden = entity.lastDate,
                    mediaType = MediaType.fromRaw(entity.mediaType),
                    durationMs = entity.videoDuration,
                    dimension = entity.dimension,
                    resolution = entity.resolution
                )
            }
        }
    }

    override suspend fun moveToVault(mediaItem: MediaItem): Boolean = withContext(Dispatchers.IO) {
        try {
            val sourceFile = File(mediaItem.filePath)
            if (!sourceFile.exists()) return@withContext false

            val targetFile = File(vaultDir, "${UUID.randomUUID()}_${mediaItem.fileName}")
            sourceFile.copyTo(targetFile, overwrite = true)
            sourceFile.delete()

            val vaultEntity = VaultEntity(
                filePath = targetFile.absolutePath,
                fileName = mediaItem.fileName,
                folderName = mediaItem.folderName,
                folderPath = mediaItem.folderPath,
                fileSize = mediaItem.fileSize,
                date = mediaItem.dateTaken,
                lastDate = System.currentTimeMillis(),
                stringDate = mediaItem.dateString,
                mediaType = mediaItem.mediaType.rawCode,
                animatedType = if (mediaItem.isAnimated) 1 else 0,
                videoDuration = mediaItem.durationMs,
                dimension = mediaItem.dimension,
                resolution = mediaItem.resolution
            )

            vaultDao.insert(vaultEntity)
            galleryDao.deleteByPath(mediaItem.filePath)
            true
        } catch (_: Exception) {
            false
        }
    }

    override suspend fun restoreFromVault(vaultItem: VaultItem): Boolean = withContext(Dispatchers.IO) {
        try {
            val vaultFile = File(vaultItem.filePath)
            if (!vaultFile.exists()) return@withContext false

            val originalFolder = File(vaultItem.folderPath)
            if (!originalFolder.exists()) originalFolder.mkdirs()

            val restoredFile = File(originalFolder, vaultItem.fileName)
            vaultFile.copyTo(restoredFile, overwrite = true)
            vaultFile.delete()

            vaultDao.deleteByPaths(listOf(vaultItem.filePath))
            true
        } catch (_: Exception) {
            false
        }
    }

    override suspend fun deleteFromVault(vaultItem: VaultItem): Boolean = withContext(Dispatchers.IO) {
        try {
            val vaultFile = File(vaultItem.filePath)
            if (vaultFile.exists()) vaultFile.delete()
            vaultDao.deleteByPaths(listOf(vaultItem.filePath))
            true
        } catch (_: Exception) {
            false
        }
    }
}
