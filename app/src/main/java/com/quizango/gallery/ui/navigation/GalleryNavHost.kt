package com.quizango.gallery.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.quizango.gallery.GalleryApplication
import com.quizango.gallery.domain.model.MediaType
import com.quizango.gallery.ui.album.AlbumDetailScreen
import com.quizango.gallery.ui.cleaner.CleanerScreen
import com.quizango.gallery.ui.cleaner.DuplicateCompleteScreen
import com.quizango.gallery.ui.cleaner.DuplicatePhotoScreen
import com.quizango.gallery.ui.cleaner.DuplicateWaitingScreen
import com.quizango.gallery.ui.cleaner.LargeVideoScreen
import com.quizango.gallery.ui.cleaner.RemoveJunkScreen
import com.quizango.gallery.ui.cleaner.ScreenShortScreen
import com.quizango.gallery.ui.creative.BlurBgScreen
import com.quizango.gallery.ui.creative.CollageMakerScreen
import com.quizango.gallery.ui.creative.ColorSplashScreen
import com.quizango.gallery.ui.creative.RemoveBgScreen
import com.quizango.gallery.ui.creative.StickerScreen
import com.quizango.gallery.ui.creative.WallpaperScreen
import com.quizango.gallery.ui.discovery.AnimatedMediaScreen
import com.quizango.gallery.ui.discovery.BurstPhotoScreen
import com.quizango.gallery.ui.discovery.MomentsScreen
import com.quizango.gallery.ui.discovery.OrganizerRecycleScreen
import com.quizango.gallery.ui.discovery.QuickOrganizerScreen
import com.quizango.gallery.ui.discovery.SearchColorScreen
import com.quizango.gallery.ui.discovery.StoryViewScreen
import com.quizango.gallery.ui.editor.PhotoEditorScreen
import com.quizango.gallery.ui.favorites.FavoritesScreen
import com.quizango.gallery.ui.home.HomeScreen
import com.quizango.gallery.ui.home.HomeViewModel
import com.quizango.gallery.ui.onboarding.WelcomeScreen
import com.quizango.gallery.ui.permission.PermissionScreen
import com.quizango.gallery.ui.recycle.RecycleScreen
import com.quizango.gallery.ui.search.SearchScreen
import com.quizango.gallery.ui.settings.AboutScreen
import com.quizango.gallery.ui.settings.LanguageScreen
import com.quizango.gallery.ui.settings.SettingsScreen
import com.quizango.gallery.ui.settings.ThemeScreen
import com.quizango.gallery.ui.splash.SplashScreen
import com.quizango.gallery.ui.vault.VaultForgotPasswordScreen
import com.quizango.gallery.ui.vault.VaultLockScreen
import com.quizango.gallery.ui.vault.VaultMediaScreen
import com.quizango.gallery.ui.vault.VaultRecoveryQuestionScreen
import com.quizango.gallery.ui.vault.VaultSettingScreen
import com.quizango.gallery.ui.viewer.MediaViewerScreen
import java.net.URLDecoder
import java.nio.charset.StandardCharsets

@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
fun GalleryNavHost(
    navController: NavHostController,
    app: GalleryApplication
) {
    NavHost(
        navController = navController,
        startDestination = Screen.Splash.route
    ) {
        composable(Screen.Splash.route) {
            SplashScreen(
                appPreferences = app.preferences,
                onNavigate = { route ->
                    navController.navigate(route) {
                        popUpTo(Screen.Splash.route) { inclusive = true }
                    }
                }
            )
        }

        composable(Screen.Welcome.route) {
            WelcomeScreen(
                appPreferences = app.preferences,
                onNavigate = { route ->
                    navController.navigate(route) {
                        popUpTo(Screen.Welcome.route) { inclusive = true }
                    }
                }
            )
        }

        composable(Screen.Permission.route) {
            PermissionScreen(
                onNavigate = { route ->
                    navController.navigate(route) {
                        popUpTo(Screen.Permission.route) { inclusive = true }
                    }
                }
            )
        }

        composable(Screen.Home.route) {
            val homeViewModel: HomeViewModel = viewModel(
                factory = HomeViewModel.Factory(
                    mediaRepository = app.mediaRepository,
                    vaultRepository = app.vaultRepository,
                    preferences = app.preferences
                )
            )
            HomeScreen(
                viewModel = homeViewModel,
                onNavigate = { route -> navController.navigate(route) }
            )
        }

        composable(
            route = Screen.AlbumDetail.route,
            arguments = listOf(
                navArgument("bucketId") { type = NavType.StringType },
                navArgument("bucketName") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val rawId = backStackEntry.arguments?.getString("bucketId") ?: ""
            val rawName = backStackEntry.arguments?.getString("bucketName") ?: ""
            val bucketId = URLDecoder.decode(rawId, StandardCharsets.UTF_8.toString())
            val bucketName = URLDecoder.decode(rawName, StandardCharsets.UTF_8.toString())

            AlbumDetailScreen(
                bucketId = bucketId,
                bucketName = bucketName,
                mediaRepository = app.mediaRepository,
                vaultRepository = app.vaultRepository,
                onNavigate = { route -> navController.navigate(route) },
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(
            route = Screen.MediaViewer.route,
            arguments = listOf(
                navArgument("path") { type = NavType.StringType; defaultValue = "" },
                navArgument("folder") { type = NavType.StringType; defaultValue = "" },
                navArgument("isRecycle") { type = NavType.BoolType; defaultValue = false },
                navArgument("isVault") { type = NavType.BoolType; defaultValue = false }
            )
        ) { backStackEntry ->
            val rawPath = backStackEntry.arguments?.getString("path") ?: ""
            val filePath = URLDecoder.decode(rawPath, StandardCharsets.UTF_8.toString())
            val rawFolder = backStackEntry.arguments?.getString("folder") ?: ""
            val folderPath = URLDecoder.decode(rawFolder, StandardCharsets.UTF_8.toString())
            val isRecycle = backStackEntry.arguments?.getBoolean("isRecycle") ?: false
            val isVault = backStackEntry.arguments?.getBoolean("isVault") ?: false

            MediaViewerScreen(
                filePath = filePath,
                folderPath = folderPath,
                isRecycle = isRecycle,
                isVault = isVault,
                mediaRepository = app.mediaRepository,
                vaultRepository = app.vaultRepository,
                onNavigate = { route -> navController.navigate(route) },
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(Screen.Favorites.route) {
            FavoritesScreen(
                mediaRepository = app.mediaRepository,
                onNavigate = { route -> navController.navigate(route) },
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(Screen.RecycleBin.route) {
            RecycleScreen(
                mediaRepository = app.mediaRepository,
                appPreferences = app.preferences,
                onNavigate = { route -> navController.navigate(route) },
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(Screen.Duplicates.route) {
            DuplicatePhotoScreen(
                mediaRepository = app.mediaRepository,
                onNavigateBack = { navController.popBackStack() },
                onNavigateToComplete = { count, size ->
                    navController.navigate(Screen.DuplicateComplete.createRoute(count, size)) {
                        popUpTo(Screen.Duplicates.route) { inclusive = true }
                    }
                }
            )
        }

        composable(Screen.DuplicateWaiting.route) {
            DuplicateWaitingScreen(
                onScanFinished = {
                    navController.navigate(Screen.Duplicates.route) {
                        popUpTo(Screen.DuplicateWaiting.route) { inclusive = true }
                    }
                }
            )
        }

        composable(
            route = Screen.DuplicateComplete.route,
            arguments = listOf(
                navArgument("count") { type = NavType.IntType; defaultValue = 0 },
                navArgument("size") { type = NavType.LongType; defaultValue = 0L }
            )
        ) { backStackEntry ->
            val count = backStackEntry.arguments?.getInt("count") ?: 0
            val size = backStackEntry.arguments?.getLong("size") ?: 0L

            DuplicateCompleteScreen(
                freedFilesCount = count,
                freedSizeBytes = size,
                onDoneClick = { navController.popBackStack() }
            )
        }

        composable(Screen.LargeVideos.route) {
            LargeVideoScreen(
                mediaRepository = app.mediaRepository,
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(Screen.Screenshots.route) {
            ScreenShortScreen(
                mediaRepository = app.mediaRepository,
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(Screen.RemoveJunk.route) {
            RemoveJunkScreen(
                onNavigateBack = { navController.popBackStack() },
                onNavigateToCleaner = { navController.navigate(Screen.Cleaner.route) },
                onNavigateToDuplicates = { navController.navigate(Screen.DuplicateWaiting.route) },
                onNavigateToScreenshots = { navController.navigate(Screen.Screenshots.route) },
                onNavigateToLargeVideos = { navController.navigate(Screen.LargeVideos.route) }
            )
        }

        composable(Screen.VaultLock.route) {
            VaultLockScreen(
                appPreferences = app.preferences,
                onNavigate = { route ->
                    navController.navigate(route) {
                        popUpTo(Screen.VaultLock.route) { inclusive = true }
                    }
                },
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(Screen.VaultMedia.route) {
            VaultMediaScreen(
                vaultRepository = app.vaultRepository,
                onNavigate = { route -> navController.navigate(route) },
                onNavigateBack = { navController.popBackStack() },
                onNavigateToSettings = { navController.navigate(Screen.VaultSetting.route) }
            )
        }

        composable(Screen.VaultSetting.route) {
            VaultSettingScreen(
                appPreferences = app.preferences,
                onNavigateToChangePin = { navController.navigate(Screen.VaultLock.route) },
                onNavigateToRecoveryQuestion = { navController.navigate(Screen.VaultRecoveryQuestion.route) },
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(Screen.VaultRecoveryQuestion.route) {
            VaultRecoveryQuestionScreen(
                appPreferences = app.preferences,
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(Screen.VaultForgotPassword.route) {
            VaultForgotPasswordScreen(
                appPreferences = app.preferences,
                onAnswerVerified = {
                    navController.navigate(Screen.VaultLock.route) {
                        popUpTo(Screen.VaultForgotPassword.route) { inclusive = true }
                    }
                },
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(Screen.Cleaner.route) {
            CleanerScreen(
                onNavigateBack = { navController.popBackStack() },
                onNavigateToDuplicates = { navController.navigate(Screen.DuplicateWaiting.route) },
                onNavigateToLargeVideos = { navController.navigate(Screen.LargeVideos.route) },
                onNavigateToScreenshots = { navController.navigate(Screen.Screenshots.route) },
                onNavigateToRemoveJunk = { navController.navigate(Screen.RemoveJunk.route) }
            )
        }

        composable(
            route = Screen.Editor.route,
            arguments = listOf(
                navArgument("path") { type = NavType.StringType; defaultValue = "" },
                navArgument("tab") { type = NavType.StringType; defaultValue = "" }
            )
        ) { backStackEntry ->
            val rawPath = backStackEntry.arguments?.getString("path") ?: ""
            val path = if (rawPath.isNotEmpty()) URLDecoder.decode(rawPath, StandardCharsets.UTF_8.toString()) else null
            val rawTab = backStackEntry.arguments?.getString("tab") ?: ""
            val tab = if (rawTab.isNotEmpty()) URLDecoder.decode(rawTab, StandardCharsets.UTF_8.toString()) else null

            PhotoEditorScreen(
                initialPath = path,
                initialTab = tab,
                mediaRepository = app.mediaRepository,
                onNavigateBack = { navController.popBackStack() },
                onNavigate = { route -> navController.navigate(route) }
            )
        }

        composable(Screen.Search.route) {
            SearchScreen(
                mediaRepository = app.mediaRepository,
                onMediaClick = { mediaItem ->
                    navController.navigate(Screen.MediaViewer.createRoute(path = mediaItem.filePath))
                },
                onAlbumClick = { album ->
                    navController.navigate(Screen.AlbumDetail.createRoute(album.id, album.name))
                },
                onNavigateBack = { navController.popBackStack() },
                onNavigateToColorSearch = { colorHex ->
                    navController.navigate(Screen.SearchColor.createRoute(colorHex))
                }
            )
        }

        composable(
            route = Screen.SearchColor.route,
            arguments = listOf(
                navArgument("color") {
                    type = NavType.StringType
                    defaultValue = "#FF0000"
                }
            )
        ) { backStackEntry ->
            val colorHex = backStackEntry.arguments?.getString("color") ?: "#FF0000"
            SearchColorScreen(
                colorHex = colorHex,
                mediaRepository = app.mediaRepository,
                onMediaClick = { mediaItem ->
                    navController.navigate(Screen.MediaViewer.createRoute(path = mediaItem.filePath))
                },
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(Screen.Moments.route) {
            MomentsScreen(
                mediaRepository = app.mediaRepository,
                onMomentClick = { moment ->
                    navController.navigate(Screen.StoryView.createRoute(moment.coverPath, moment.title))
                },
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(
            route = Screen.StoryView.route,
            arguments = listOf(
                navArgument("path") { type = NavType.StringType; defaultValue = "" },
                navArgument("title") { type = NavType.StringType; defaultValue = "Story" }
            )
        ) { backStackEntry ->
            val rawTitle = backStackEntry.arguments?.getString("title") ?: "Story"
            val title = URLDecoder.decode(rawTitle, StandardCharsets.UTF_8.toString())
            val allMedia by app.mediaRepository.getActiveMediaFlow().collectAsState(initial = emptyList())
            val photosOnly = allMedia.filter { it.mediaType != MediaType.VIDEO }.take(8)

            StoryViewScreen(
                storyTitle = title,
                mediaItems = photosOnly,
                onEditClick = { path -> navController.navigate(Screen.Editor.createRoute(path)) },
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(Screen.AnimatedMedia.route) {
            AnimatedMediaScreen(
                mediaRepository = app.mediaRepository,
                onMediaClick = { mediaItem ->
                    navController.navigate(Screen.MediaViewer.createRoute(path = mediaItem.filePath))
                },
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(Screen.BurstPhotos.route) {
            BurstPhotoScreen(
                mediaRepository = app.mediaRepository,
                onMediaClick = { mediaItem ->
                    navController.navigate(Screen.MediaViewer.createRoute(path = mediaItem.filePath))
                },
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(Screen.QuickOrganizer.route) {
            QuickOrganizerScreen(
                mediaRepository = app.mediaRepository,
                onNavigateToRecycle = { navController.navigate(Screen.OrganizerRecycle.route) },
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(Screen.OrganizerRecycle.route) {
            OrganizerRecycleScreen(
                mediaRepository = app.mediaRepository,
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(Screen.CollageMaker.route) {
            CollageMakerScreen(
                mediaRepository = app.mediaRepository,
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(
            route = Screen.Wallpaper.route,
            arguments = listOf(
                navArgument("path") { type = NavType.StringType; defaultValue = "" }
            )
        ) { backStackEntry ->
            val rawPath = backStackEntry.arguments?.getString("path") ?: ""
            val path = URLDecoder.decode(rawPath, StandardCharsets.UTF_8.toString())

            WallpaperScreen(
                imagePath = path,
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(Screen.Stickers.route) {
            StickerScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(
            route = Screen.RemoveBg.route,
            arguments = listOf(
                navArgument("path") { type = NavType.StringType; defaultValue = "" }
            )
        ) { backStackEntry ->
            val rawPath = backStackEntry.arguments?.getString("path") ?: ""
            val path = URLDecoder.decode(rawPath, StandardCharsets.UTF_8.toString())

            RemoveBgScreen(
                imagePath = path,
                mediaRepository = app.mediaRepository,
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(
            route = Screen.BlurBg.route,
            arguments = listOf(
                navArgument("path") { type = NavType.StringType; defaultValue = "" }
            )
        ) { backStackEntry ->
            val rawPath = backStackEntry.arguments?.getString("path") ?: ""
            val path = URLDecoder.decode(rawPath, StandardCharsets.UTF_8.toString())

            BlurBgScreen(
                imagePath = path,
                mediaRepository = app.mediaRepository,
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(
            route = Screen.ColorSplash.route,
            arguments = listOf(
                navArgument("path") { type = NavType.StringType; defaultValue = "" }
            )
        ) { backStackEntry ->
            val rawPath = backStackEntry.arguments?.getString("path") ?: ""
            val path = URLDecoder.decode(rawPath, StandardCharsets.UTF_8.toString())

            ColorSplashScreen(
                imagePath = path,
                mediaRepository = app.mediaRepository,
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(Screen.Language.route) {
            val canNavigateBack = navController.previousBackStackEntry != null &&
                navController.previousBackStackEntry?.destination?.route != Screen.Welcome.route
            LanguageScreen(
                appPreferences = app.preferences,
                canNavigateBack = canNavigateBack,
                onApply = {
                    if (canNavigateBack) {
                        navController.popBackStack()
                    } else {
                        navController.navigate(Screen.Permission.route) {
                            popUpTo(Screen.Language.route) { inclusive = true }
                        }
                    }
                },
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(Screen.Theme.route) {
            ThemeScreen(
                appPreferences = app.preferences,
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(Screen.Settings.route) {
            SettingsScreen(
                appPreferences = app.preferences,
                onNavigate = { route -> navController.navigate(route) },
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(Screen.About.route) {
            AboutScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }
    }
}
