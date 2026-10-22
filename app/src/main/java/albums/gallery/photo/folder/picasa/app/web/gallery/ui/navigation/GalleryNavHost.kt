package albums.gallery.photo.folder.picasa.app.web.gallery.ui.navigation

import androidx.compose.runtime.Composable
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import albums.gallery.photo.folder.picasa.app.web.gallery.GalleryApplication
import albums.gallery.photo.folder.picasa.app.web.gallery.ui.album.AlbumDetailScreen
import albums.gallery.photo.folder.picasa.app.web.gallery.ui.cleaner.CleanerScreen
import albums.gallery.photo.folder.picasa.app.web.gallery.ui.cleaner.DuplicatePhotoScreen
import albums.gallery.photo.folder.picasa.app.web.gallery.ui.editor.PhotoEditorScreen
import albums.gallery.photo.folder.picasa.app.web.gallery.ui.favorites.FavoritesScreen
import albums.gallery.photo.folder.picasa.app.web.gallery.ui.home.HomeScreen
import albums.gallery.photo.folder.picasa.app.web.gallery.ui.home.HomeViewModel
import albums.gallery.photo.folder.picasa.app.web.gallery.ui.onboarding.WelcomeScreen
import albums.gallery.photo.folder.picasa.app.web.gallery.ui.permission.PermissionScreen
import albums.gallery.photo.folder.picasa.app.web.gallery.ui.recycle.RecycleScreen
import albums.gallery.photo.folder.picasa.app.web.gallery.ui.search.SearchScreen
import albums.gallery.photo.folder.picasa.app.web.gallery.ui.settings.LanguageScreen
import albums.gallery.photo.folder.picasa.app.web.gallery.ui.settings.SettingsScreen
import albums.gallery.photo.folder.picasa.app.web.gallery.ui.settings.ThemeScreen
import albums.gallery.photo.folder.picasa.app.web.gallery.ui.splash.SplashScreen
import albums.gallery.photo.folder.picasa.app.web.gallery.ui.vault.VaultLockScreen
import albums.gallery.photo.folder.picasa.app.web.gallery.ui.vault.VaultMediaScreen
import albums.gallery.photo.folder.picasa.app.web.gallery.ui.viewer.MediaViewerScreen
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
                navArgument("folder") { type = NavType.StringType; defaultValue = "" }
            )
        ) { backStackEntry ->
            val rawPath = backStackEntry.arguments?.getString("path") ?: ""
            val filePath = URLDecoder.decode(rawPath, StandardCharsets.UTF_8.toString())
            val rawFolder = backStackEntry.arguments?.getString("folder") ?: ""
            val folderPath = URLDecoder.decode(rawFolder, StandardCharsets.UTF_8.toString())

            MediaViewerScreen(
                filePath = filePath,
                folderPath = folderPath,
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
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(Screen.Duplicates.route) {
            DuplicatePhotoScreen(
                mediaRepository = app.mediaRepository,
                onNavigateBack = { navController.popBackStack() }
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
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(Screen.Cleaner.route) {
            CleanerScreen(
                onNavigateBack = { navController.popBackStack() },
                onNavigateToDuplicates = { navController.navigate(Screen.Duplicates.route) }
            )
        }

        composable(
            route = Screen.Editor.route,
            arguments = listOf(
                navArgument("path") { type = NavType.StringType; defaultValue = "" }
            )
        ) { backStackEntry ->
            val rawPath = backStackEntry.arguments?.getString("path") ?: ""
            val path = if (rawPath.isNotEmpty()) URLDecoder.decode(rawPath, StandardCharsets.UTF_8.toString()) else null

            PhotoEditorScreen(
                initialPath = path,
                mediaRepository = app.mediaRepository,
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(Screen.Search.route) {
            SearchScreen(
                mediaRepository = app.mediaRepository,
                onMediaClick = { mediaItem ->
                    navController.navigate(Screen.MediaViewer.createRoute(path = mediaItem.filePath))
                },
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(Screen.Language.route) {
            LanguageScreen(
                appPreferences = app.preferences,
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
    }
}
