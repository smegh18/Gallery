package com.quizango.gallery.ui.creative

import androidx.compose.runtime.Composable
import com.quizango.gallery.R
import com.quizango.gallery.data.local.datastore.AppPreferences

/** activity_color_splash (S272 ColorSplashActivity): the subject in colour over a monochrome background, ruler 0..25. */
@Composable
fun ColorSplashScreen(imagePath: String, preferences: AppPreferences, onSaved: () -> Unit, onNavigateBack: () -> Unit) =
    BackgroundEffectScreen(AiTool.COLOR_SPLASH, R.string.color_splash, imagePath, preferences, onSaved, onNavigateBack)
