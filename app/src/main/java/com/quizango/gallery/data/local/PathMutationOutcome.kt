package com.quizango.gallery.data.local

/** What one path's delete, trash or restore actually did, so callers count, keep selection and report by truth. */
data class PathMutationOutcome(val path: String, val status: Status) {
    enum class Status {
        /** The file is gone (delete) or untouched (trash/restore) and the row change was written. */
        SUCCESS,
        /** The file is still there; its row was left alone. */
        FILE_FAILED,
        /** The file side is done but the row change failed; sending the same request again retries only the row. */
        METADATA_FAILED
    }

    val succeeded: Boolean get() = status == Status.SUCCESS
}

fun List<PathMutationOutcome>.successfulPaths(): List<String> = filter { it.succeeded }.map { it.path }

fun List<PathMutationOutcome>.failedPaths(): List<String> = filterNot { it.succeeded }.map { it.path }
