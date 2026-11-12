package com.quizango.gallery.data.local

import android.content.ContentUris
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Build
import android.provider.MediaStore
import android.util.Size
import com.google.android.gms.tasks.Tasks
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.label.ImageLabeling
import com.google.mlkit.vision.label.defaults.ImageLabelerOptions
import com.quizango.gallery.domain.model.MediaItem
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.io.File

/**
 * Moments: label every photo on-device (ML Kit, model from Play Services, like the reference)
 * and remember the label per file, so photos can be grouped by what is in them.
 */
class MomentScanner(private val context: Context) {

    data class Progress(val done: Int, val total: Int)

    private val file = File(context.filesDir, "moment_labels.tsv")
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private var job: Job? = null

    /** file path -> label ("" = nothing confident found, so it is not scanned again). */
    private val _labels = MutableStateFlow<Map<String, String>>(emptyMap())
    val labels: StateFlow<Map<String, String>> = _labels

    /** Non-null while a scan runs. */
    private val _progress = MutableStateFlow<Progress?>(null)
    val progress: StateFlow<Progress?> = _progress

    private val loaded = scope.launch(Dispatchers.IO) {
        if (file.exists()) _labels.value = parse(file.readLines())
    }

    /** Labels photos not seen yet; keeps running if the screen closes. */
    fun scan(photos: List<MediaItem>) {
        if (job?.isActive == true) return
        job = scope.launch {
            loaded.join()
            val todo = photos.filter { it.filePath !in _labels.value }
            if (todo.isEmpty()) return@launch
            val labeler = ImageLabeling.getClient(ImageLabelerOptions.DEFAULT_OPTIONS)
            try {
                _progress.value = Progress(0, todo.size)
                val batch = StringBuilder()
                todo.forEachIndexed { i, item ->
                    val label = runCatching { labelOf(labeler, item) }.getOrNull()
                    if (label != null) {
                        _labels.value = _labels.value + (item.filePath to label)
                        batch.append(line(item.filePath, label))
                    }
                    _progress.value = Progress(i + 1, todo.size)
                    if ((i + 1) % 50 == 0 || i == todo.lastIndex) {
                        file.appendText(batch.toString()); batch.clear()
                    }
                }
            } finally {
                labeler.close()
                _progress.value = null
            }
        }
    }

    private fun labelOf(labeler: com.google.mlkit.vision.label.ImageLabeler, item: MediaItem): String? {
        val bitmap = thumbnail(item) ?: return null
        val labels = Tasks.await(labeler.process(InputImage.fromBitmap(bitmap, 0)))
        bitmap.recycle()
        return labels.filter { it.confidence >= MIN_CONFIDENCE }.maxByOrNull { it.confidence }?.text ?: ""
    }

    private fun thumbnail(item: MediaItem): Bitmap? {
        if (Build.VERSION.SDK_INT >= 29 && item.id > 0) {
            val uri = ContentUris.withAppendedId(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, item.id)
            runCatching { return context.contentResolver.loadThumbnail(uri, Size(SIDE, SIDE), null) }
        }
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeFile(item.filePath, bounds)
        var sample = 1
        while (bounds.outWidth / (sample * 2) >= SIDE && bounds.outHeight / (sample * 2) >= SIDE) sample *= 2
        return BitmapFactory.decodeFile(item.filePath, BitmapFactory.Options().apply { inSampleSize = sample })
    }

    companion object {
        private const val SIDE = 320
        private const val MIN_CONFIDENCE = 0.7f

        fun line(path: String, label: String) = "${path.replace('\t', ' ').replace('\n', ' ')}\t$label\n"

        fun parse(lines: List<String>): Map<String, String> =
            lines.mapNotNull { l -> l.split('\t').takeIf { it.size == 2 }?.let { it[0] to it[1] } }.toMap()

        /** Label groups, biggest first, for photos that still exist. */
        fun group(photos: List<MediaItem>, labels: Map<String, String>): List<Pair<String, List<MediaItem>>> =
            photos.groupBy { labels[it.filePath].orEmpty() }
                .filterKeys { it.isNotEmpty() }
                .toList()
                .sortedByDescending { it.second.size }
    }
}
