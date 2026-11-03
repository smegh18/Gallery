package com.quizango.gallery.data.local

import java.io.File

/** What the junk scan found for one category. */
data class JunkScanResult(
    val id: String,
    val files: List<File>
) {
    val sizeBytes: Long get() = files.sumOf { it.length() }
}

/**
 * Real junk scan matching the reference RemoveJunkActivity categories.
 * Only reports files that exist; nothing is estimated.
 */
object JunkScanner {

    const val APP_CACHE = "app_cache"
    const val APK = "apk_files"
    const val TEMP = "temp_files"
    const val LOG = "log_files"
    const val EMPTY_FOLDER = "empty_folder"

    // Standard top-level folders are never reported as empty junk.
    private val PROTECTED_TOP = setOf(
        "android", "dcim", "download", "pictures", "movies", "music", "documents",
        "alarms", "notifications", "podcasts", "ringtones", "audiobooks", "recordings"
    )

    /**
     * @param storageRoot shared storage root (skips its Android/ folder: other apps' data)
     * @param appCacheDirs this app's own cache folders
     * @param onPath called with each folder as it is scanned, for the live readout
     */
    fun scan(
        storageRoot: File?,
        appCacheDirs: List<File>,
        onPath: (String) -> Unit = {}
    ): List<JunkScanResult> {
        val cache = appCacheDirs.flatMap { dir ->
            onPath(dir.path)
            dir.walkTopDown().filter { it.isFile }.toList()
        }
        val apks = mutableListOf<File>()
        val temps = mutableListOf<File>()
        val logs = mutableListOf<File>()
        val empties = mutableListOf<File>()

        if (storageRoot != null && storageRoot.isDirectory) {
            storageRoot.walkTopDown()
                .onEnter { dir ->
                    val skip = dir.parentFile == storageRoot && dir.name.equals("Android", ignoreCase = true)
                    if (!skip) onPath(dir.path)
                    !skip
                }
                .forEach { f ->
                    if (f.isFile) {
                        val name = f.name.lowercase()
                        when {
                            name.endsWith(".apk") -> apks += f
                            name.endsWith(".tmp") || name.endsWith(".temp") -> temps += f
                            name.endsWith(".log") -> logs += f
                        }
                    } else if (f != storageRoot && f.list()?.isEmpty() == true &&
                        !(f.parentFile == storageRoot && f.name.lowercase() in PROTECTED_TOP)
                    ) {
                        empties += f
                    }
                }
        }

        return listOf(
            JunkScanResult(APP_CACHE, cache),
            JunkScanResult(APK, apks),
            JunkScanResult(TEMP, temps),
            JunkScanResult(LOG, logs),
            JunkScanResult(EMPTY_FOLDER, empties)
        )
    }

    /** Deletes the given files; folders only if still empty. Returns bytes actually freed. */
    fun clean(files: List<File>): Long {
        var freed = 0L
        for (f in files) {
            val size = f.length()
            val ok = try {
                if (f.isDirectory) f.list()?.isEmpty() == true && f.delete() else f.delete()
            } catch (_: SecurityException) {
                false
            }
            if (ok) freed += size
        }
        return freed
    }
}
