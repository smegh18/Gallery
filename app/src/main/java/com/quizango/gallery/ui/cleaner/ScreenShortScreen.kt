package com.quizango.gallery.ui.cleaner

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.remember
import com.quizango.gallery.data.repository.MediaRepository

/** ScreenShortActivity: the shared cleaner grid in 4 columns with the screenshot copy (§3). */
@Composable
fun ScreenShortScreen(mediaRepository: MediaRepository, onNavigateBack: () -> Unit) {
    val flow = remember { mediaRepository.getScreenshotsFlow() }
    CleanerMediaScreen(CleanerMediaKind.SCREENSHOTS, flow.collectAsState(initial = null).value, mediaRepository, onNavigateBack)
}
