package com.quizango.gallery.ui.cleaner

import android.os.SystemClock
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.stringResource
import com.quizango.gallery.R
import com.quizango.gallery.data.local.PathMutationOutcome
import com.quizango.gallery.data.repository.MediaRepository
import com.quizango.gallery.domain.model.MediaItem
import com.quizango.gallery.ui.theme.GalleryText
import com.quizango.gallery.ui.theme.sdp
import kotlinx.coroutines.delay

/** The result is derived from repository outcomes, never from the requested items. */
internal data class DuplicateDeleteProcessResult(
    val successfulItems: List<MediaItem>,
    val failedItems: List<MediaItem>
) {
    val freedBytes: Long get() = successfulItems.sumOf { it.fileSize }
}

/**
 * Runs one confirmed request at most once. An absent or failed repository outcome remains retryable.
 */
internal class DuplicateDeleteProcessCoordinator(
    private val request: DuplicateDeleteRequest,
    private val nowMillis: () -> Long = SystemClock::elapsedRealtime,
    private val minimumDurationMillis: Long = MINIMUM_PROCESS_DURATION_MILLIS
) {
    private var completed: DuplicateDeleteProcessResult? = null

    suspend fun run(
        mutate: suspend (DuplicateDeleteRequest) -> List<PathMutationOutcome>,
        wait: suspend (Long) -> Unit = ::delay
    ): DuplicateDeleteProcessResult {
        completed?.let { return it }
        val startedAt = nowMillis()
        val outcomes = runCatching { mutate(request) }.getOrDefault(emptyList())
        val result = outcomesFor(request.items, outcomes)
        wait((minimumDurationMillis - (nowMillis() - startedAt)).coerceAtLeast(0L))
        completed = result
        return result
    }

    private fun outcomesFor(
        requested: List<MediaItem>,
        outcomes: List<PathMutationOutcome>
    ): DuplicateDeleteProcessResult {
        val byPath = outcomes.associateBy { it.path }
        val successful = requested.filter { byPath[it.filePath]?.succeeded == true }
        val failed = requested.filterNot { byPath[it.filePath]?.succeeded == true }
        return DuplicateDeleteProcessResult(successful, failed)
    }

    private companion object {
        const val MINIMUM_PROCESS_DURATION_MILLIS = 3_000L
    }
}

/** activity_duplicate_delete_running: real work begins once and Back is intentionally blocked. */
@Composable
internal fun DuplicateDeleteProcessScreen(
    request: DuplicateDeleteRequest,
    mediaRepository: MediaRepository,
    onFinished: (DuplicateDeleteProcessResult) -> Unit
) {
    val coordinator = remember(request) { DuplicateDeleteProcessCoordinator(request) }
    BackHandler(enabled = true) { }

    LaunchedEffect(coordinator) {
        onFinished(
            coordinator.run(
                mutate = { frozen ->
                    if (frozen.moveToRecycleBin) mediaRepository.moveToTrash(frozen.paths)
                    else mediaRepository.deletePermanently(frozen.paths)
                }
            )
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(colorResource(R.color.color_success_lottie_blue))
            .padding(14.sdp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        CircularProgressIndicator(color = colorResource(R.color.onlyWhite))
        Spacer(Modifier.height(18.sdp))
        Text(
            text = stringResource(R.string.deleting),
            style = GalleryText.BodyLargeSemiBold,
            color = colorResource(R.color.onlyWhite)
        )
        Spacer(Modifier.height(8.sdp))
        Text(
            text = stringResource(R.string.dup_media_wait_hint),
            style = GalleryText.BodyMediumRegular,
            color = colorResource(R.color.onlyWhite)
        )
    }
}
