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

    /** RemoveJunkViewModel posts these after its five category passes; only 100 changes the scan screen. */
    val PROGRESS_STEPS = listOf(20, 40, 60, 80, 100)

    // Standard top-level folders are never reported as empty junk.
    private val PROTECTED_TOP = setOf(
        "android", "dcim", "download", "pictures", "movies", "music", "documents",
        "alarms", "notifications", "podcasts", "ringtones", "audiobooks", "recordings"
    )

    // ex/j: the text after the last '.', "" when there is none
    private fun ext(f: File) = f.name.substringAfterLast('.', "")

    /**
     * @param storageRoot shared storage root (skips its Android/ folder: other apps' data)
     * @param appCacheDirs this app's own cache folders
     * @param onProgress called with [PROGRESS_STEPS] as the categories complete
     * @param onPath called with each folder as it is scanned, for the live readout
     */
    fun scan(
        storageRoot: File?,
        appCacheDirs: List<File>,
        onProgress: (Int) -> Unit = {},
        onPath: (String) -> Unit = {}
    ): List<JunkScanResult> {
        val cache = appCacheDirs.flatMap { dir ->
            onPath(dir.path)
            dir.walkTopDown().filter { it.isFile }.toList()
        }.distinct()
        onProgress(PROGRESS_STEPS[0])
        val apks = mutableListOf<File>()
        val temps = mutableListOf<File>()
        val logs = mutableListOf<File>()
        val empties = mutableListOf<File>()

        // Unreadable branches: FileTreeWalk skips folders it cannot list; a SecurityException keeps what was found
        if (storageRoot != null && storageRoot.isDirectory) try {
            storageRoot.walkTopDown()
                .onEnter { dir ->
                    val skip = dir.parentFile == storageRoot && dir.name.equals("Android", ignoreCase = true)
                    if (!skip) onPath(dir.path)
                    !skip
                }
                .forEach { f ->
                    if (f.isFile) {
                        val ext = ext(f)
                        when {
                            ext.equals("apk", ignoreCase = true) -> apks += f
                            ext == "tmp" -> temps += f // dr/c case 4: exact lowercase, no .temp
                            ext.equals("log", ignoreCase = true) -> logs += f
                        }
                    } else if (f != storageRoot && f.list()?.isEmpty() == true &&
                        !(f.parentFile == storageRoot && f.name.lowercase() in PROTECTED_TOP)
                    ) {
                        empties += f
                    }
                }
        } catch (_: SecurityException) {
        }
        // One walk fills the four storage categories, so they complete together
        PROGRESS_STEPS.drop(1).forEach(onProgress)

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
