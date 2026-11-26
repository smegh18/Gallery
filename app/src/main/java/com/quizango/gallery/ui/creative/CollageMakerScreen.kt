package com.quizango.gallery.ui.creative

import android.net.Uri
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import com.quizango.gallery.data.repository.MediaRepository
import com.quizango.gallery.ui.components.PhotoPicker
import com.quizango.gallery.ui.creative.collage.CollageSavedScreen
import com.quizango.gallery.ui.creative.collage.CollageScreen

/**
 * Explore > Collage in 2.7.2 (EF.OooO0o): the in-app picker (InternalSelectMediaActivity TYPE 4, 2 to 15 photos), the
 * collage screen, then the saved screen. There is no collage home; Back from the picker or the saved screen, and Exit
 * from the collage screen, return to where Collage was opened.
 */
@Composable
fun CollageMakerScreen(mediaRepository: MediaRepository, onNavigateBack: () -> Unit) {
    var photos by rememberSaveable { mutableStateOf<List<String>?>(null) }
    var saved by rememberSaveable { mutableStateOf<Uri?>(null) }
    val picked = photos
    val done = saved
    when {
        done != null -> CollageSavedScreen(done, onNavigateBack)
        picked == null -> PhotoPicker(mediaRepository, multi = true, onBack = onNavigateBack) { items -> photos = items.map { it.filePath } }
        else -> CollageScreen(picked, mediaRepository, onExit = onNavigateBack, onSaved = { saved = it })
    }
}
