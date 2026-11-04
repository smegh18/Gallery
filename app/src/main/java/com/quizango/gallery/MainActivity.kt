package com.quizango.gallery

import android.os.Bundle
import androidx.fragment.app.FragmentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.navigation.compose.rememberNavController
import com.quizango.gallery.ui.navigation.GalleryNavHost
import com.quizango.gallery.ui.theme.GalleryTheme
import com.quizango.gallery.ui.theme.applyNightMode

// FragmentActivity: the system fingerprint prompt (BiometricPrompt) needs it
class MainActivity : FragmentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val app = application as GalleryApplication

        setContent {
            val themeMode by app.preferences.themeMode.collectAsState(initial = 2)
            LaunchedEffect(themeMode) {
                applyNightMode(themeMode)
            }
            val isDark = when (themeMode) {
                0 -> false
                1 -> true
                else -> isSystemInDarkTheme()
            }

            GalleryTheme(darkTheme = isDark) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    val navController = rememberNavController()
                    GalleryNavHost(navController = navController, app = app)
                }
            }
        }
    }
}
