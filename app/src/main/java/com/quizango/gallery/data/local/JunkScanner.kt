package com.quizango.gallery.data.local

import java.io.File
import java.util.Calendar
import java.util.Locale

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
    /** Shown as "Log Files"; 2.7.2 fills it with old documents (aw/b), not .log files. Id kept for the UI. */
    const val LOG = "log_files"
    const val EMPTY_FOLDER = "empty_folder"

    /** RemoveJunkViewModel posts these after its five category passes; only 100 changes the scan screen. */
    val PROGRESS_STEPS = listOf(20, 40, 60, 80, 100)

    // Standard top-level folders are never reported as empty junk.
    private val PROTECTED_TOP = setOf(
        "android", "dcim", "download", "pictures", "movies", "music", "documents",
        "alarms", "notifications", "podcasts", "ringtones", "audiobooks", "recordings"
    )

    // aw/b: old-document extensions, compared lowercased
    private val OLD_DOC_EXT = setOf("doc", "docx", "pdf", "txt", "csv", "xls", "xlsx", "ppt", "pptx", "xml")

    // ex/j: the text after the last '.', "" when there is none
    private fun ext(f: File) = f.name.substringAfterLast('.', "")

    /**
     * @param storageRoot shared storage root (skips its Android/ folder: other apps' data)
     * @param appCacheDirs this app's own cache folders
     * @param now clock for the old-document cutoff (now minus one calendar year, dx/e)
     * @param onProgress called with [PROGRESS_STEPS] as the categories complete
     * @param onPath called with each folder as it is scanned, for the live readout
     */
    fun scan(
        storageRoot: File?,
        appCacheDirs: List<File>,
        now: Long = System.currentTimeMillis(),
        onProgress: (Int) -> Unit = {},
        onPath: (String) -> Unit = {}
    ): List<JunkScanResult> {
        val cache = appCacheDirs.flatMap { dir ->
            onPath(dir.path)
            // RemoveJunkViewModel lists direct children only and skips empty files, so an empty cache reads "No Junk"
            dir.listFiles()?.filter { it.isFile && it.length() >= 1L }.orEmpty()
        }.distinct()
        onProgress(PROGRESS_STEPS[0])
        val docCutoff = Calendar.getInstance().apply { timeInMillis = now; add(Calendar.YEAR, -1) }.timeInMillis
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
                            ext == "apk" -> apks += f // dr/c case 2: exact lowercase
                            ext == "tmp" -> temps += f // dr/c case 4: exact lowercase, no .temp
                            ext.lowercase(Locale.ROOT) in OLD_DOC_EXT && f.lastModified() < docCutoff -> logs += f
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

    /**
     * Deletes each path once, in order: a folder only while still empty, an already-absent path counts as done.
     * Junk has no Room rows, so the outcome is SUCCESS or FILE_FAILED; failed paths stay selectable for a retry.
     */
    fun clean(paths: List<String>): List<PathMutationOutcome> = paths.distinct().map { path ->
        val f = File(path)
        val gone = try {
            !f.exists() || if (f.isDirectory) f.list()?.isEmpty() == true && f.delete() else f.delete()
        } catch (_: SecurityException) {
            false
        }
        PathMutationOutcome(path, if (gone) PathMutationOutcome.Status.SUCCESS else PathMutationOutcome.Status.FILE_FAILED)
    }
}
