package albums.gallery.photo.folder.picasa.app.web.gallery.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val LightColorScheme = lightColorScheme(
    primary = GalleryBlue,
    onPrimary = GalleryBackgroundLight,
    primaryContainer = DuplicateBackground,
    onPrimaryContainer = GalleryDarkBlue,
    secondary = GalleryPrimaryBlue,
    onSecondary = GalleryBackgroundLight,
    background = GalleryBackgroundLight,
    onBackground = TextPrimaryLight,
    surface = GallerySurfaceLight,
    onSurface = TextPrimaryLight,
    surfaceVariant = GalleryBackgroundLight,
    onSurfaceVariant = TextSecondaryLight,
    outline = DividerLight
)

private val DarkColorScheme = darkColorScheme(
    primary = GalleryPrimaryBlue,
    onPrimary = GalleryBackgroundDark,
    primaryContainer = GalleryDarkBlue,
    onPrimaryContainer = GalleryBackgroundLight,
    secondary = GalleryBlue,
    onSecondary = GalleryBackgroundLight,
    background = GalleryBackgroundDark,
    onBackground = TextPrimaryDark,
    surface = GallerySurfaceDark,
    onSurface = TextPrimaryDark,
    surfaceVariant = GallerySurfaceDark,
    onSurfaceVariant = TextSecondaryDark,
    outline = DividerDark
)

@Composable
fun GalleryTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = colorScheme.background.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = GalleryTypography,
        shapes = GalleryShapes,
        content = content
    )
}
