package com.quizango.gallery.data.repository

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.media.ExifInterface
import android.media.MediaMetadataRetriever
import android.os.Environment
import com.quizango.gallery.R
import com.quizango.gallery.data.local.PathMutationOutcome
import com.quizango.gallery.data.local.PathMutationOutcome.Status
import com.quizango.gallery.data.local.database.dao.GalleryDao
import com.quizango.gallery.data.local.database.entity.MediaEntity
import com.quizango.gallery.data.local.mediastore.MediaStoreScanner
import com.quizango.gallery.domain.model.Album
import com.quizango.gallery.domain.model.MediaItem
import com.quizango.gallery.domain.model.MediaType
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
    fun getLargeVideosFlow(minSizeBytes: Long = LARGE_VIDEO_MIN_BYTES): Flow<List<MediaItem>>
    fun getScreenshotsFlow(): Flow<List<MediaItem>>
    fun getAnimatedMediaFlow(): Flow<List<MediaItem>>
    fun getBurstPhotosFlow(): Flow<List<MediaItem>>
    fun searchByColorFlow(colorHex: String): Flow<List<MediaItem>>
    suspend fun syncMediaStore()
    suspend fun setFavorite(path: String, favorite: Boolean)
    suspend fun moveToTrash(path: String): PathMutationOutcome
    suspend fun moveToTrash(paths: List<String>): List<PathMutationOutcome>
    suspend fun restoreFromTrash(path: String): PathMutationOutcome
    suspend fun restoreFromTrash(paths: List<String>): List<PathMutationOutcome>
    suspend fun deletePermanently(path: String): PathMutationOutcome
    suspend fun deletePermanently(paths: List<String>): List<PathMutationOutcome>
    suspend fun renameMedia(oldPath: String, newName: String): Boolean
    suspend fun copyMediaToFolder(filePath: String, destFolder: String): Boolean
    suspend fun moveMediaToFolder(filePath: String, destFolder: String): Boolean

    /** Rotates the image file itself 90° clockwise (viewer Rotate, PA.onRotateBitmap) and rescans it. */
    suspend fun rotateImage(path: String): Boolean

    /** Video Capture: saves the frame at [positionMs] as a PNG under Pictures/Screenshots and rescans it. */
    suspend fun saveVideoFrame(videoPath: String, positionMs: Long): Boolean

    /** Renames the folder on disk and moves its rows to the new paths; the new folder path, or null when refused or failed. */
    suspend fun renameFolder(folderPath: String, newName: String): String?
}

/** LargeVideoViewModel 0x4600000: a video counts as large from 70 MiB. */
internal const val LARGE_VIDEO_MIN_BYTES = 73_400_320L

/** Large Video list: videos of at least [minBytes], newest first. */
internal fun largeVideos(media: List<MediaItem>, minBytes: Long = LARGE_VIDEO_MIN_BYTES): List<MediaItem> =
    media.filter { it.isVideo && it.fileSize >= minBytes }.sortedByDescending { it.dateAdded }

/** VideoPlayerActivity capture name: "IMG_" + yyyyMMdd_ + epoch millis + ".png". */
internal fun captureFileName(millis: Long): String =
    "IMG_" + java.text.SimpleDateFormat("yyyyMMdd_", java.util.Locale.US).format(java.util.Date(millis)) + millis + ".png"

/**
 * The 2.7.2 burst group key (`relativePath`, set by S272/s9/ao.OooOOOO): a camera JPEG whose name carries the maker's
 * burst marker is grouped by its path up to the last "_" (a trailing "_cover" frame joins its burst); "" when the file
 * is not a burst frame. Keys are only compared with other derived keys, never used as a path.
 */
internal fun burstGroupKey(filePath: String, fileName: String, folderPath: String, isImage: Boolean, manufacturer: String): String {
    val maker = manufacturer.lowercase(java.util.Locale.ROOT)
    val oppo = maker == "oppo"
    val marker = when (maker) {
        "oppo", "samsung" -> ""
        "xiaomi" -> "timeburst"
        else -> "burst"
    }
    val name = fileName.lowercase(java.util.Locale.ROOT)
    if (!isImage || !folderPath.contains(if (oppo) "DCIM/Camera/Cshot" else "DCIM/Camera")) return ""
    if (!oppo && !fileName.contains("_")) return ""
    if (!name.contains(marker) || !name.contains(".jpg")) return ""
    val cut = filePath.lastIndexOf('_')
    if (cut == -1 && !oppo) return ""
    val key = when (maker) {
        "oppo" -> File(filePath).parentFile?.name.orEmpty()
        "motorola" -> filePath.substring(cut)
        else -> filePath.substring(0, cut)
    }
    val coverCut = key.lastIndexOf('_')
    return if (name.contains("cover") && coverCut != -1) key.substring(0, coverCut) else key
}

internal fun MediaItem.burstKey(manufacturer: String = android.os.Build.MANUFACTURER.orEmpty()): String =
    burstGroupKey(filePath, fileName, folderPath, mediaType == MediaType.IMAGE, manufacturer)

/** A usable photo position: not the 0,0 "no location" default and inside the lat/lng ranges (NaN fails both). */
internal fun hasCoordinates(lat: Float, lng: Float): Boolean =
    (lat != 0f || lng != 0f) && lat in -90f..90f && lng in -180f..180f

/**
 * Place group key (MapViewModel groups by getPlace()): the trimmed place name, else "lat,lng" at 4 decimals (~11 m)
 * when only coordinates exist, else "". Travels as filter:place:<key> and is only compared, never used as a path.
 */
internal fun placeKey(place: String, lat: Float, lng: Float): String = place.trim().ifEmpty {
    if (hasCoordinates(lat, lng)) String.format(java.util.Locale.ROOT, "%.4f,%.4f", lat, lng) else ""
}

/** Keeps the GPS already read for this file version; a changed file (new lastDate) is read again. */
internal fun keepLocation(scanned: MediaEntity, old: MediaEntity): MediaEntity =
    if (old.lastDate != scanned.lastDate) scanned
    else scanned.copy(isLocation = old.isLocation, lat = old.lat, longs = old.longs, place = old.place)

/** 2.7.2 place name (s9/k.OooO): the first non-blank of sub-locality, locality, sub-admin area, admin area, country. */
internal fun pickPlaceName(vararg parts: String?): String = parts.firstOrNull { !it.isNullOrBlank() }?.trim().orEmpty()

internal fun MediaItem.placeKey(): String = placeKey(placeName, latitude, longitude)

/** [path] after [oldFolder] became [newFolder]: the folder itself and everything under it move, any other path stays. */
internal fun movedPath(path: String, oldFolder: String, newFolder: String): String = when {
    path == oldFolder -> newFolder
    path.startsWith("$oldFolder/") -> newFolder + path.substring(oldFolder.length)
    else -> path
}

/**
 * Renames [folder] to [newName] and rewrites the rows of its whole tree (active and Recycle Bin, subfolders included)
 * under the new paths with every other field kept. The rows are snapshotted before the rename and written (REPLACE,
 * so they win over any default rows a sync inserted meanwhile) in ONE [inTransaction] call after it succeeds. Returns the
 * old and new file paths for the MediaStore scan, or null with nothing touched when the folder is missing, a sibling
 * already has the name (ignoring case, so also an unchanged or case-only rename) or [renameDir] fails.
 */
internal suspend fun renameFolderRows(
    dao: GalleryDao,
    folder: File,
    newName: String,
    inTransaction: suspend (suspend () -> Unit) -> Unit,
    renameDir: (File, File) -> Boolean = { from, to -> from.renameTo(to) }
): Pair<List<String>, List<String>>? {
    val parent = folder.parentFile ?: return null
    if (!folder.isDirectory || newName.isBlank()) return null
    if (parent.listFiles()?.any { it.name.equals(newName, ignoreCase = true) } != false) return null // taken, or unreadable
    val oldFolder = folder.path.replace(File.separatorChar, '/')
    val newFolder = File(parent, newName).path.replace(File.separatorChar, '/')
    // Snapshot BEFORE the rename: a sync running right after it would drop these rows (files gone) and lose their flags
    val tree = dao.getAllMedia().filter { it.filePath.startsWith("$oldFolder/") }
    if (!renameDir(folder, File(parent, newName))) return null
    var oldPaths: List<String> = emptyList()
    var newPaths: List<String> = emptyList()
    try {
        inTransaction {
            val moved = tree.map { row ->
                val tail = row.folderPath.removePrefix(oldFolder) // "" for a direct file, "/sub" below it
                val rel = row.relativePath.trimEnd('/')
                row.copy(
                    filePath = movedPath(row.filePath, oldFolder, newFolder),
                    folderPath = movedPath(row.folderPath, oldFolder, newFolder),
                    folderName = if (tail.isEmpty()) newName else row.folderName,
                    relativePath = if (rel.endsWith(folder.name + tail)) {
                        rel.removeSuffix(folder.name + tail) + newName + tail + row.relativePath.substring(rel.length)
                    } else row.relativePath
                )
            }
            oldPaths = tree.map { it.filePath }
            newPaths = moved.map { it.filePath }
            dao.insertAll(moved)
            oldPaths.chunked(500).forEach { dao.deleteByPaths(it) } // SQLite caps the variables of one IN list
        }
    } catch (e: Exception) {
        renameDir(File(parent, newName), folder) // the transaction rolled back, so put the folder back to match the rows
        throw e
    }
    return oldPaths to newPaths
}

/** S272/s9/k.OooOOO: [fileName] in [dir], or "name(n).ext" with the first free n. Like 2.7.2 it drops a leading dot. */
internal fun uniqueTargetFile(dir: File, fileName: String): File {
    val name = fileName.removePrefix(".")
    val dot = name.lastIndexOf('.')
    val base = if (dot > 0) name.substring(0, dot) else name // 2.7.2 loops forever on a name without "."
    val ext = if (dot > 0) name.substring(dot) else ""
    var target = File(dir, name)
    var n = 0
    while (target.exists()) target = File(dir, "$base(${++n})$ext")
    return target
}

/** Copies [src] into [dir] under a free name, so a copy into its own folder makes "name(1).ext". Null on failure. */
internal fun copyIntoFolder(src: File, dir: File): File? {
    dir.mkdirs()
    val dest = uniqueTargetFile(dir, src.name)
    return try {
        src.copyTo(dest)
    } catch (e: Exception) {
        if (e !is FileAlreadyExistsException) dest.delete() // a partial copy; a file that was already there is kept
        null
    }
}

/**
 * Moves [src] into [dir]: copy, check the size, then delete the source. On any failure the source stays and the
 * copy goes, so nothing is lost. A file already in [dir] is left as it is. Null on failure.
 */
internal fun moveIntoFolder(src: File, dir: File): File? {
    if (src.parentFile?.canonicalFile == dir.canonicalFile) return src
    val dest = copyIntoFolder(src, dir) ?: return null
    if (dest.length() == src.length() && src.delete()) return dest
    dest.delete()
    return null
}

/** True when no file is left at [path]: deleted now or already gone. */
internal fun deleteFileIfPresent(path: String): Boolean {
    val file = File(path)
    return file.delete() || !file.exists()
}

/** Runs [block]; false on any failure. Cancellation still propagates. */
private inline fun attempt(block: () -> Boolean): Boolean = try {
    block()
} catch (e: kotlin.coroutines.cancellation.CancellationException) {
    throw e
} catch (e: Exception) {
    false
}

/**
 * Permanent delete, file first: a row goes only once its file is gone, so a failed file delete keeps its row. A row
 * delete that fails after the file went is retried by sending the same request again (the file is then absent).
 * Repeated paths are handled once, outcomes come back in input order.
 */
internal suspend fun deleteFilesThenRows(
    paths: List<String>,
    deleteFile: (String) -> Boolean,
    deleteRows: suspend (List<String>) -> Unit
): List<PathMutationOutcome> {
    val unique = paths.distinct()
    val filesGone = unique.filterTo(LinkedHashSet()) { attempt { deleteFile(it) } }
    val rowsGone = HashSet<String>()
    // SQLite caps the variables of one IN list
    filesGone.chunked(500).forEach { chunk -> if (attempt { deleteRows(chunk); true }) rowsGone += chunk }
    return unique.map { path ->
        PathMutationOutcome(path, when (path) {
            in rowsGone -> Status.SUCCESS
            in filesGone -> Status.METADATA_FAILED
            else -> Status.FILE_FAILED
        })
    }
}

/** Trash and restore change rows only (files stay where they are); one row failing does not hide the rest. */
internal suspend fun updateRows(paths: List<String>, update: suspend (String) -> Unit): List<PathMutationOutcome> =
    paths.distinct().map { path ->
        PathMutationOutcome(path, if (attempt { update(path); true }) Status.SUCCESS else Status.METADATA_FAILED)
    }

class MediaRepositoryImpl(
    private val dao: GalleryDao,
    private val scanner: MediaStoreScanner,
    private val momentLabels: Flow<Map<String, String>>,
    private val inTransaction: suspend (suspend () -> Unit) -> Unit = { it() }
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
                    name = "", // titleRes is resolved by the UI
                    path = "filter:all_photos",
                    coverPath = photos.firstOrNull()?.filePath ?: "",
                    itemCount = photos.size,
                    coverId = photos.firstOrNull()?.id ?: 0L,
                    isVideoCover = false,
                    titleRes = R.string.all_photos
                )
            )

            // 2. Videos (mediaType == 2)
            val videos = list.filter { it.mediaType == 2 }
            result.add(
                Album(
                    id = "filter:videos",
                    name = "",
                    path = "filter:videos",
                    coverPath = videos.firstOrNull()?.filePath ?: "",
                    itemCount = videos.size,
                    coverId = videos.firstOrNull()?.id ?: 0L,
                    isVideoCover = true,
                    titleRes = R.string.videos
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
                    name = "",
                    path = "filter:camera",
                    coverPath = cameraItems.firstOrNull()?.filePath ?: "",
                    itemCount = cameraItems.size,
                    coverId = cameraItems.firstOrNull()?.id ?: 0L,
                    isVideoCover = cameraItems.firstOrNull()?.mediaType == 2,
                    titleRes = R.string.camera
                )
            )

            // 4. Screenshots (Matching reference screenshot order)
            val screenshotItems = list.filter {
                it.folderPath.contains("screenshot", ignoreCase = true) ||
                it.folderName.contains("screenshot", ignoreCase = true)
            }
            // The Albums tab drops empty virtual cards (UI-SPEC 1.4); other lists still see this album.
            val firstShot = screenshotItems.firstOrNull()
            result.add(
                Album(
                    id = "filter:screenshots",
                    name = "",
                    path = "filter:screenshots",
                    coverPath = firstShot?.filePath ?: "",
                    itemCount = screenshotItems.size,
                    coverId = firstShot?.id ?: 0L,
                    isVideoCover = firstShot?.mediaType == 2,
                    titleRes = R.string.screenshot
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
            folderPath == "filter:burst" -> getBurstPhotosFlow()
            folderPath.startsWith("filter:burst:") -> {
                // One burst group: its frames among the current rows, nothing from other bursts
                val key = folderPath.removePrefix("filter:burst:")
                getBurstPhotosFlow().map { frames -> frames.filter { it.burstKey() == key } }
            }
            folderPath == "filter:place" -> {
                activeEntities.map { list ->
                    list.filter { placeKey(it.place, it.lat, it.longs).isNotEmpty() }.map { it.toDomain() }
                }
            }
            folderPath.startsWith("filter:place:") -> {
                // One place: its media among the current rows, matched by key
                val key = folderPath.removePrefix("filter:place:")
                activeEntities.map { list ->
                    list.filter { placeKey(it.place, it.lat, it.longs) == key }.map { it.toDomain() }
                }
            }
            folderPath == "filter:favorites" -> {
                dao.getFavoritesFlow().map { list -> list.map { it.toDomain() } }
            }
            folderPath.startsWith("filter:moment:") -> {
                val label = folderPath.removePrefix("filter:moment:")
                kotlinx.coroutines.flow.combine(activeEntities, momentLabels) { list, labels ->
                    list.filter { it.mediaType != 2 && labels[it.filePath] == label }.map { it.toDomain() }
                }
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
        activeEntities.map { list -> largeVideos(list.filter { it.mediaType == 2 }.map { it.toDomain() }, minSizeBytes) }
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
        // 2.7.2: images with a burst key, newest first (the active rows' order)
        activeEntities.map { list ->
            val maker = android.os.Build.MANUFACTURER.orEmpty()
            list.filter { burstGroupKey(it.filePath, it.fileName, it.folderPath, it.mediaType == 1, maker).isNotEmpty() }.map { it.toDomain() }
        }.flowOn(Dispatchers.Default)
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
                keepLocation(entity, existing).copy(
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
        // After the lists are written (PlaceFindService runs later too): EXIF GPS once per file version, then the place name
        val located = entities.mapNotNull { e ->
            var r = e
            if (!r.isLocation && r.mediaType != MediaType.VIDEO.rawCode) {
                val ll = scanner.readLatLong(r.filePath)
                r = r.copy(isLocation = true, lat = ll?.get(0) ?: 0f, longs = ll?.get(1) ?: 0f)
            }
            // shortcut: a failed lookup (offline) is retried on every sync, for geotagged rows only
            if (r.place.isEmpty() && hasCoordinates(r.lat, r.longs)) r = r.copy(place = scanner.placeName(r.lat, r.longs))
            r.takeIf { it != e }
        }
        if (located.isNotEmpty()) inTransaction { located.forEach { dao.setLocation(it.filePath, it.lat, it.longs, it.place) } }
    }

    override suspend fun setFavorite(path: String, favorite: Boolean) = withContext(Dispatchers.IO) {
        dao.setFavorite(path, favorite)
    }

    override suspend fun moveToTrash(path: String): PathMutationOutcome = moveToTrash(listOf(path)).single()

    override suspend fun moveToTrash(paths: List<String>): List<PathMutationOutcome> = withContext(Dispatchers.IO) {
        updateRows(paths) { dao.moveToTrash(it) }
    }

    override suspend fun restoreFromTrash(path: String): PathMutationOutcome = restoreFromTrash(listOf(path)).single()

    override suspend fun restoreFromTrash(paths: List<String>): List<PathMutationOutcome> = withContext(Dispatchers.IO) {
        updateRows(paths) { dao.restoreFromTrash(it) }
    }

    override suspend fun deletePermanently(path: String): PathMutationOutcome = deletePermanently(listOf(path)).single()

    override suspend fun deletePermanently(paths: List<String>): List<PathMutationOutcome> = withContext(Dispatchers.IO) {
        deleteFilesThenRows(paths, ::deleteFileIfPresent) { dao.deleteByPaths(it) }
    }

    override suspend fun renameMedia(oldPath: String, newName: String): Boolean = withContext(Dispatchers.IO) {
        try {
            val oldFile = File(oldPath)
            if (!oldFile.exists()) return@withContext false
            val ext = oldFile.extension
            val finalName = if (newName.endsWith(".$ext", ignoreCase = true)) newName else "$newName.$ext"
            val newFile = File(oldFile.parentFile, finalName)
            if (newFile.exists()) return@withContext false
            // Snapshot first: the next sync drops the old row (file gone) and would lose its favourite flag
            val row = dao.getMediaByPath(oldPath)
            if (oldFile.renameTo(newFile)) {
                row?.let {
                    inTransaction {
                        dao.insert(it.copy(filePath = newFile.path, fileName = newFile.name))
                        dao.deleteByPath(oldPath)
                    }
                }
                scanner.scanFiles(newFile.path, oldFile.path) // MIUI does not index java.io renames by itself
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
            val copy = copyIntoFolder(src, File(destFolder)) ?: return@withContext false
            scanner.scanFiles(copy.path)
            syncMediaStore()
            true
        } catch (_: Exception) {
            false
        }
    }

    override suspend fun moveMediaToFolder(filePath: String, destFolder: String): Boolean = withContext(Dispatchers.IO) {
        try {
            val src = File(filePath)
            if (!src.exists()) return@withContext false
            val moved = moveIntoFolder(src, File(destFolder)) ?: return@withContext false
            if (moved != src) {
                scanner.scanFiles(moved.path, src.path)
                dao.deleteByPath(filePath) // the new path comes in with the next sync
            }
            true
        } catch (_: Exception) {
            false
        }
    }

    override suspend fun saveVideoFrame(videoPath: String, positionMs: Long): Boolean = withContext(Dispatchers.IO) {
        val retriever = MediaMetadataRetriever()
        try {
            retriever.setDataSource(videoPath)
            val frame = retriever.getFrameAtTime(positionMs * 1000L, MediaMetadataRetriever.OPTION_CLOSEST)
                ?: return@withContext false
            val dir = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES), "Screenshots")
            if (!dir.isDirectory && !dir.mkdirs()) return@withContext false
            val file = File(dir, captureFileName(System.currentTimeMillis()))
            file.outputStream().use { frame.compress(Bitmap.CompressFormat.PNG, 100, it) }
            scanner.scanFiles(file.path)
            syncMediaStore()
            true
        } catch (_: Exception) {
            false
        } finally {
            retriever.release()
        }
    }

    override suspend fun rotateImage(path: String): Boolean = withContext(Dispatchers.IO) {
        try {
            val file = File(path)
            if (!file.exists()) return@withContext false
            when (file.extension.lowercase()) {
                // JPEG: lossless, only the EXIF orientation turns (flips are dropped)
                "jpg", "jpeg" -> {
                    val exif = ExifInterface(path)
                    val next = when (exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)) {
                        ExifInterface.ORIENTATION_ROTATE_90 -> ExifInterface.ORIENTATION_ROTATE_180
                        ExifInterface.ORIENTATION_ROTATE_180 -> ExifInterface.ORIENTATION_ROTATE_270
                        ExifInterface.ORIENTATION_ROTATE_270 -> ExifInterface.ORIENTATION_NORMAL
                        else -> ExifInterface.ORIENTATION_ROTATE_90
                    }
                    exif.setAttribute(ExifInterface.TAG_ORIENTATION, next.toString())
                    exif.saveAttributes()
                }
                "png", "webp" -> {
                    val source = BitmapFactory.decodeFile(path) ?: return@withContext false
                    val rotated = Bitmap.createBitmap(source, 0, 0, source.width, source.height, Matrix().apply { postRotate(90f) }, true)
                    @Suppress("DEPRECATION")
                    val format = if (file.extension.equals("png", true)) Bitmap.CompressFormat.PNG else Bitmap.CompressFormat.WEBP
                    // Write beside the original and swap, so a failed encode never leaves a half-written photo
                    val tmp = File(file.parentFile, ".${file.name}.rotating")
                    val written = tmp.outputStream().use { rotated.compress(format, 100, it) }
                    if (!written || !tmp.renameTo(file)) {
                        tmp.delete()
                        return@withContext false
                    }
                }
                else -> return@withContext false
            }
            scanner.scanFiles(path)
            syncMediaStore()
            true
        } catch (_: Exception) {
            false
        }
    }

    override suspend fun renameFolder(folderPath: String, newName: String): String? = withContext(Dispatchers.IO) {
        try {
            val folder = File(folderPath)
            val (oldPaths, newPaths) = renameFolderRows(dao, folder, newName, inTransaction) ?: return@withContext null
            scanner.scanFiles(*(newPaths + oldPaths).toTypedArray())
            File(folder.parentFile, newName).path
        } catch (_: Exception) {
            null
        }
    }
}
