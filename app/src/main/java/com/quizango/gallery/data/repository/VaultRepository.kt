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
import com.quizango.gallery.data.local.VaultCrypto
import java.io.File
import java.util.UUID

interface VaultRepository {
    fun getVaultItemsFlow(): Flow<List<VaultItem>>
    suspend fun moveToVault(mediaItem: MediaItem): Boolean
    suspend fun restoreFromVault(vaultItem: VaultItem): Boolean
    suspend fun deleteFromVault(vaultItem: VaultItem): Boolean
    /** Decrypted copy for display/sharing, kept in app-private cache until [clearViewCache]. */
    suspend fun viewFile(vaultItem: VaultItem): File?
    fun clearViewCache()
    /** Renames the name the item gets back on restore (keeps the extension). */
    suspend fun renameInVault(vaultItem: VaultItem, newName: String): Boolean
}

class VaultRepositoryImpl(
    private val context: Context,
    private val vaultDao: VaultDao,
    private val galleryDao: GalleryDao
) : VaultRepository {

    private val vaultDir: File
        get() = File(context.filesDir, "vault_encrypted").also { if (!it.exists()) it.mkdirs() }

    private val viewDir: File
        get() = File(context.cacheDir, "vault_view").also { if (!it.exists()) it.mkdirs() }

    private val key by lazy { VaultCrypto.keystoreKey() }

    /** Writes the plain content of [vaultFile] to [target]; older vault files are plain copies. */
    private fun decryptTo(vaultFile: File, target: File) {
        if (VaultCrypto.isEncrypted(vaultFile)) {
            vaultFile.inputStream().use { input -> target.outputStream().use { VaultCrypto.decrypt(input, it, key) } }
        } else {
            vaultFile.copyTo(target, overwrite = true)
        }
    }

    override fun getVaultItemsFlow(): Flow<List<VaultItem>> {
        return vaultDao.getAllVaultItemsFlow().map { list ->
            list.map { entity ->
                VaultItem(
                    filePath = entity.filePath,
                    fileName = entity.fileName,
                    folderName = entity.folderName,
                    folderPath = entity.folderPath,
                    fileSize = entity.fileSize,
                    // Reference shows and groups by the photo's own date, not the hide time
                    dateHidden = if (entity.date > 0) entity.date else entity.lastDate,
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
            try {
                sourceFile.inputStream().use { input -> targetFile.outputStream().use { VaultCrypto.encrypt(input, it, key) } }
            } catch (e: Exception) {
                targetFile.delete() // never delete the original if encryption failed
                return@withContext false
            }
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
            try {
                decryptTo(vaultFile, restoredFile)
            } catch (e: Exception) {
                restoredFile.delete() // keep the vault copy if decryption failed
                return@withContext false
            }
            vaultFile.delete()

            File(context.cacheDir, "vault_view/${File(vaultItem.filePath).name}").delete() // drop plain cache copy
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
            File(context.cacheDir, "vault_view/${File(vaultItem.filePath).name}").delete() // drop plain cache copy
            vaultDao.deleteByPaths(listOf(vaultItem.filePath))
            true
        } catch (_: Exception) {
            false
        }
    }

    override suspend fun viewFile(vaultItem: VaultItem): File? = withContext(Dispatchers.IO) {
        val vaultFile = File(vaultItem.filePath)
        if (!vaultFile.exists()) return@withContext null
        // Keep the original extension so viewers and share targets detect the type
        val out = File(viewDir, vaultFile.name)
        if (!out.exists() || out.length() == 0L) {
            try {
                decryptTo(vaultFile, out)
            } catch (e: Exception) {
                out.delete()
                return@withContext null
            }
        }
        out
    }

    override fun clearViewCache() {
        File(context.cacheDir, "vault_view").deleteRecursively()
    }

    override suspend fun renameInVault(vaultItem: VaultItem, newName: String): Boolean = withContext(Dispatchers.IO) {
        val ext = vaultItem.fileName.substringAfterLast('.', "")
        val name = if (ext.isEmpty() || newName.endsWith(".$ext")) newName else "$newName.$ext"
        vaultDao.rename(vaultItem.filePath, name)
        true
    }
}
