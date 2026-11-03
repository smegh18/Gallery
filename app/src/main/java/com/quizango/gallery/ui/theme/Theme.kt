package com.quizango.gallery.ui.theme

import android.app.Activity
import android.content.ContextWrapper
import androidx.appcompat.app.AppCompatDelegate
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
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
    surfaceVariant = ScreenBackgroundLight,
    onSurfaceVariant = TextSecondaryLight,
    outline = DividerLight,
    outlineVariant = SettingsDividerLight,
    surfaceContainer = AlbumOptionBackLight
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
    surfaceVariant = ScreenBackgroundDark,
    onSurfaceVariant = TextSecondaryDark,
    outline = DividerDark,
    outlineVariant = SettingsDividerDark,
    surfaceContainer = AlbumOptionBackDark
)

val ColorScheme.isDark: Boolean
    get() = background == GalleryBackgroundDark

val ColorScheme.cardBackground: Color
    get() = if (isDark) GallerySurfaceDark else GallerySurfaceLight

val ColorScheme.cardBorder: Color
    get() = if (isDark) CardBorderDark else CardBorderLight

val ColorScheme.screenBackground: Color
    get() = if (isDark) ScreenBackgroundDark else ScreenBackgroundLight

val ColorScheme.topContainerBg: Color
    get() = if (isDark) TopContainerBgDark else TopContainerBgLight

val ColorScheme.drawerFooterBg: Color
    get() = if (isDark) DrawerFooterBgDark else DrawerFooterBgLight

val ColorScheme.mediaTypeCardBg: Color
    get() = if (isDark) MediaTypeCardDark else MediaTypeCardLight

val ColorScheme.albumOptionBg: Color
    get() = if (isDark) AlbumOptionBackDark else AlbumOptionBackLight

val ColorScheme.settingsDivider: Color
    get() = if (isDark) SettingsDividerDark else SettingsDividerLight

val ColorScheme.searchBarBg: Color
    get() = if (isDark) SearchBarBgDark else SearchBarBgLight

val ColorScheme.textPrimary: Color
    get() = if (isDark) TextPrimaryDark else TextPrimaryLight

val ColorScheme.textSecondary: Color
    get() = if (isDark) TextSecondaryDark else TextSecondaryLight

val ColorScheme.textTertiary: Color
    get() = if (isDark) TextTertiaryDark else TextTertiaryLight

fun applyNightMode(themeMode: Int) {
    val mode = when (themeMode) {
        0 -> AppCompatDelegate.MODE_NIGHT_NO
        1 -> AppCompatDelegate.MODE_NIGHT_YES
        else -> AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM
    }
    AppCompatDelegate.setDefaultNightMode(mode)
}

@Composable
fun GalleryTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as? Activity
                ?: (view.context as? ContextWrapper)?.baseContext as? Activity)?.window
            if (window != null) {
                window.statusBarColor = colorScheme.background.toArgb()
                window.navigationBarColor = colorScheme.background.toArgb()
                val insetsController = WindowCompat.getInsetsController(window, view)
                insetsController.isAppearanceLightStatusBars = !darkTheme
                insetsController.isAppearanceLightNavigationBars = !darkTheme
            }
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = GalleryTypography,
        shapes = GalleryShapes,
        content = content
    )
}
