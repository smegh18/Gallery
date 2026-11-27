package com.quizango.gallery.ui.navigation

import androidx.compose.animation.core.tween
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.fadeOut
import androidx.compose.animation.fadeIn
import android.widget.Toast
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.quizango.gallery.GalleryApplication
import com.quizango.gallery.domain.model.MediaType
import com.quizango.gallery.ui.album.AlbumDetailScreen
import com.quizango.gallery.ui.album.AllAlbumsScreen
import com.quizango.gallery.ui.cleaner.CleanerScreen
import com.quizango.gallery.ui.cleaner.DuplicateCompleteScreen
import com.quizango.gallery.ui.cleaner.DuplicateDeleteProcessScreen
import com.quizango.gallery.ui.cleaner.DuplicateDeleteRequest
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
import com.quizango.gallery.ui.discovery.PlaceScreen
import com.quizango.gallery.ui.discovery.momentRoute
import com.quizango.gallery.ui.discovery.OrganizerRecycleScreen
import com.quizango.gallery.ui.discovery.QuickOrganizerScreen
import com.quizango.gallery.ui.discovery.DEFAULT_SEARCH_COLOR
import com.quizango.gallery.ui.discovery.SearchColorScreen
import com.quizango.gallery.ui.discovery.colorFromArg
import com.quizango.gallery.ui.discovery.StoryViewScreen
import com.quizango.gallery.ui.editor.PhotoEditorScreen
import com.quizango.gallery.ui.favorites.FavoritesScreen
import com.quizango.gallery.ui.home.HomeScreen
import com.quizango.gallery.ui.home.HomeViewModel
import com.quizango.gallery.ui.onboarding.FeaturePromoScreen
import com.quizango.gallery.ui.permission.PermissionScreen
import com.quizango.gallery.ui.recycle.RecycleScreen
import com.quizango.gallery.ui.search.SearchScreen
import com.quizango.gallery.ui.search.SearchViewAllScreen
import com.quizango.gallery.ui.settings.AboutScreen
import com.quizango.gallery.ui.settings.LanguageScreen
import com.quizango.gallery.ui.settings.SettingsScreen
import com.quizango.gallery.ui.settings.ThemeScreen
import com.quizango.gallery.ui.splash.SplashScreen
import com.quizango.gallery.ui.vault.VaultForgotPasswordScreen
import com.quizango.gallery.ui.vault.VaultLockScreen
import com.quizango.gallery.ui.vault.VaultAddMediaScreen
import com.quizango.gallery.ui.vault.VaultMediaScreen
import com.quizango.gallery.ui.vault.VaultRecoveryQuestionScreen
import com.quizango.gallery.ui.vault.VaultSettingScreen
import com.quizango.gallery.ui.vault.settingsToast
import com.quizango.gallery.ui.viewer.MediaViewerScreen
import com.quizango.gallery.ui.viewer.VideoPlayerScreen

private const val DUPLICATE_DELETE_PROCESS_ROUTE = "duplicates/delete-process"

@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
fun GalleryNavHost(
    navController: NavHostController,
    app: GalleryApplication
) {
    var pendingDuplicateDelete by remember { mutableStateOf<DuplicateDeleteRequest?>(null) }
    var retryDuplicatePaths by remember { mutableStateOf(emptySet<String>()) }
    // Short fade + slight slide between screens; plain fade in and out of the splash
    val fromSplash: androidx.compose.animation.AnimatedContentTransitionScope<androidx.navigation.NavBackStackEntry>.() -> Boolean =
        { initialState.destination.route == Screen.Splash.route }
    NavHost(
        navController = navController,
        startDestination = Screen.Splash.route,
        enterTransition = {
            if (fromSplash()) fadeIn(tween(350))
            else fadeIn(tween(220)) + slideInHorizontally(tween(260)) { it / 10 }
        },
        exitTransition = {
            if (initialState.destination.route == Screen.Splash.route) fadeOut(tween(300))
            else fadeOut(tween(180)) + slideOutHorizontally(tween(260)) { -it / 10 }
        },
        popEnterTransition = { fadeIn(tween(220)) + slideInHorizontally(tween(260)) { -it / 10 } },
        popExitTransition = { fadeOut(tween(180)) + slideOutHorizontally(tween(260)) { it / 10 } }
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

        composable(Screen.Permission.route) {
            PermissionScreen(
                appPreferences = app.preferences,
                onNavigate = { route ->
                    navController.navigate(route) {
                        popUpTo(Screen.Permission.route) { inclusive = true }
                    }
                }
            )
        }

        composable(Screen.FeaturePromo.route) {
            FeaturePromoScreen(
                appPreferences = app.preferences,
                onNavigateToHome = {
                    navController.navigate(Screen.Home.route) {
                        popUpTo(Screen.FeaturePromo.route) { inclusive = true }
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
                onNavigate = { route -> navController.navigateOnce(route) }
            )
        }

        composable(
            route = Screen.AlbumDetail.route,
            arguments = listOf(
                navArgument("bucketId") { type = NavType.StringType },
                navArgument("bucketName") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            // Navigation has already percent-decoded route arguments; decoding again corrupts "+" and "%".
            val rawId = backStackEntry.arguments?.getString("bucketId") ?: ""
            val rawName = backStackEntry.arguments?.getString("bucketName") ?: ""
            val bucketId = rawId
            val bucketName = rawName

            AlbumDetailScreen(
                bucketId = bucketId,
                bucketName = bucketName,
                mediaRepository = app.mediaRepository,
                vaultRepository = app.vaultRepository,
                onNavigate = { route -> navController.navigateOnce(route) },
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
            val filePath = rawPath
            val rawFolder = backStackEntry.arguments?.getString("folder") ?: ""
            val folderPath = rawFolder
            val isRecycle = backStackEntry.arguments?.getBoolean("isRecycle") ?: false
            val isVault = backStackEntry.arguments?.getBoolean("isVault") ?: false

            MediaViewerScreen(
                filePath = filePath,
                folderPath = folderPath,
                isRecycle = isRecycle,
                isVault = isVault,
                mediaRepository = app.mediaRepository,
                vaultRepository = app.vaultRepository,
                onNavigate = { route -> navController.navigateOnce(route) },
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(
            route = Screen.VideoPlayer.route,
            arguments = listOf(
                navArgument("path") { type = NavType.StringType; defaultValue = "" },
                navArgument("folder") { type = NavType.StringType; defaultValue = "" },
                navArgument("isRecycle") { type = NavType.BoolType; defaultValue = false },
                navArgument("isVault") { type = NavType.BoolType; defaultValue = false }
            )
        ) { backStackEntry ->
            val args = backStackEntry.arguments
            VideoPlayerScreen(
                filePath = args?.getString("path") ?: "",
                folderPath = args?.getString("folder") ?: "",
                isRecycle = args?.getBoolean("isRecycle") ?: false,
                isVault = args?.getBoolean("isVault") ?: false,
                mediaRepository = app.mediaRepository,
                vaultRepository = app.vaultRepository,
                preferences = app.preferences,
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(Screen.Favorites.route) {
            FavoritesScreen(
                mediaRepository = app.mediaRepository,
                vaultRepository = app.vaultRepository,
                onNavigate = { route -> navController.navigateOnce(route) },
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(Screen.AllAlbums.route) {
            // Its own selection state: the Albums tab's selection never leaks in
            val albumsViewModel: HomeViewModel = viewModel(
                factory = HomeViewModel.Factory(
                    mediaRepository = app.mediaRepository,
                    vaultRepository = app.vaultRepository,
                    preferences = app.preferences
                )
            )
            AllAlbumsScreen(
                viewModel = albumsViewModel,
                onNavigate = { route -> navController.navigateOnce(route) },
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(Screen.RecycleBin.route) {
            RecycleScreen(
                mediaRepository = app.mediaRepository,
                appPreferences = app.preferences,
                onNavigate = { route -> navController.navigateOnce(route) },
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(Screen.Duplicates.route) {
            DuplicatePhotoScreen(
                mediaRepository = app.mediaRepository,
                duplicateFinder = app.duplicates,
                onNavigateBack = { navController.popBackStack() },
                retrySelection = retryDuplicatePaths,
                onRetrySelectionConsumed = { retryDuplicatePaths = emptySet() },
                onDelete = { request ->
                    pendingDuplicateDelete = request
                    navController.navigate(DUPLICATE_DELETE_PROCESS_ROUTE) { launchSingleTop = true }
                }
            )
        }

        composable(DUPLICATE_DELETE_PROCESS_ROUTE) { entry ->
            val request = pendingDuplicateDelete
            if (request == null) {
                // A restored entry has no request and closes itself; after onFinished the exit transition lands here too
                LaunchedEffect(Unit) { navController.popIfCurrent(entry) }
            } else {
                DuplicateDeleteProcessScreen(
                    request = request,
                    mediaRepository = app.mediaRepository,
                    onFinished = { result ->
                        pendingDuplicateDelete = null
                        if (result.failedItems.isNotEmpty()) {
                            // Do not make a partial cleanup claim; return remaining paths selected for a safe retry.
                            retryDuplicatePaths = result.failedItems.mapTo(LinkedHashSet()) { it.filePath }
                            navController.popBackStack()
                        } else if (result.successfulItems.isNotEmpty()) {
                            navController.navigate(
                                Screen.DuplicateComplete.createRoute(result.successfulItems.size, result.freedBytes)
                            ) {
                                popUpTo(Screen.Duplicates.route) { inclusive = true }
                            }
                        } else {
                            navController.popBackStack()
                        }
                    }
                )
            }
        }

        composable(Screen.DuplicateWaiting.route) {
            DuplicateWaitingScreen(
                scan = { app.duplicates.fingerprints(app.mediaRepository.getActiveMediaFlow().first()) },
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
                onDoneClick = {
                    if (!navController.popBackStack(Screen.Cleaner.route, inclusive = false)) {
                        navController.popBackStack()
                    }
                }
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
                appPreferences = app.preferences,
                onNavigateBack = { navController.popBackStack() },
                onNavigateToDuplicates = { navController.navigateOnce(Screen.DuplicateWaiting.route) },
                onNavigateToScreenshots = { navController.navigateOnce(Screen.Screenshots.route) },
                onNavigateToLargeVideos = { navController.navigateOnce(Screen.LargeVideos.route) }
            )
        }

        composable(Screen.VaultLock.route) {
            VaultLockDestination(navController, app, Screen.VaultLock.route, type = 0, openVault = true)
        }

        composable(Screen.VaultMedia.route) {
            VaultMediaScreen(
                vaultRepository = app.vaultRepository,
                onNavigate = { route -> navController.navigateOnce(route) },
                onNavigateBack = { navController.popBackStack() },
                onNavigateToSettings = { navController.navigateOnce(Screen.VaultSetting.route) }
            )
        }

        composable(Screen.VaultAdd.route) {
            VaultAddMediaScreen(
                mediaRepository = app.mediaRepository,
                vaultRepository = app.vaultRepository,
                onDone = { navController.popBackStack() },
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(
            route = Screen.VaultLockFor.route,
            arguments = listOf(
                navArgument("type") { type = NavType.IntType },
                navArgument("openVault") { type = NavType.BoolType }
            )
        ) { entry ->
            VaultLockDestination(
                navController, app, Screen.VaultLockFor.route,
                type = entry.arguments?.getInt("type") ?: 0,
                openVault = entry.arguments?.getBoolean("openVault") ?: true
            )
        }

        composable(Screen.VaultSetting.route) {
            VaultSettingScreen(
                appPreferences = app.preferences,
                onNavigateToChangePin = { navController.navigate(Screen.VaultLockFor.createRoute(1, false)) { launchSingleTop = true } },
                // S4: Settings never opens the question directly; the lock unlocks first (TYPE 2)
                onNavigateToRecoveryQuestion = { navController.navigate(Screen.VaultLockFor.createRoute(2, false)) { launchSingleTop = true } },
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(Screen.VaultRecoveryQuestion.route) {
            val scope = androidx.compose.runtime.rememberCoroutineScope()
            val context = androidx.compose.ui.platform.LocalContext.current
            VaultRecoveryQuestionScreen(
                appPreferences = app.preferences,
                // VL.o00Ooo: Back closes the question and the lock; with no question saved the half-finished lock is wiped
                onNavigateBack = {
                    scope.launch {
                        if (!app.preferences.hasRecoveryQuestion.first()) app.preferences.clearVaultLock()
                        val lockId = navController.previousBackStackEntry?.destination?.id
                        if (lockId != null) navController.popBackStack(lockId, inclusive = true) else navController.popBackStack()
                    }
                },
                onSaved = {
                    scope.launch {
                        // The lock entry below tells how it was opened (a plain VaultLock route means type 0, open the vault)
                        val lock = navController.previousBackStackEntry
                        val lockType = lock?.arguments?.getInt("type") ?: 0
                        val opensVault = lock?.arguments?.takeIf { it.containsKey("openVault") }?.getBoolean("openVault") ?: true
                        if (lockType == 0) app.preferences.setPrivateToastShow(true)
                        val lockId = lock?.destination?.id
                        if (opensVault) {
                            navController.navigate(Screen.VaultMedia.route) {
                                if (lockId != null) popUpTo(lockId) { inclusive = true }
                            }
                        } else {
                            if (lockId != null) navController.popBackStack(lockId, inclusive = true) else navController.popBackStack()
                            settingsToast(lockType, false)?.let { Toast.makeText(context, context.getString(it), Toast.LENGTH_SHORT).show() }
                        }
                    }
                }
            )
        }

        composable(Screen.VaultForgotPassword.route) { entry ->
            val scope = androidx.compose.runtime.rememberCoroutineScope()
            VaultForgotPasswordScreen(
                appPreferences = app.preferences,
                // The screen already wiped the lock: the lock underneath reloads as "Set PIN".
                // A reset that finishes after Back already left this screen must not pop the one below.
                onAnswerVerified = { navController.popIfCurrent(entry) },
                // VL.o00Oo0: opened by the lockout, Back closes the lock too; opened by the link, Back returns to it
                onNavigateBack = {
                    scope.launch {
                        val lockId = navController.previousBackStackEntry?.destination?.id
                        if (lockId != null && app.preferences.isPrivateLock.first()) navController.popBackStack(lockId, inclusive = true)
                        else navController.popBackStack()
                    }
                }
            )
        }

        composable(Screen.Cleaner.route) {
            CleanerScreen(
                onNavigateBack = { navController.popBackStack() },
                onNavigateToDuplicates = { navController.navigateOnce(Screen.DuplicateWaiting.route) },
                onNavigateToLargeVideos = { navController.navigateOnce(Screen.LargeVideos.route) },
                onNavigateToScreenshots = { navController.navigateOnce(Screen.Screenshots.route) },
                onNavigateToRemoveJunk = { navController.navigateOnce(Screen.RemoveJunk.route) }
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
            val path = if (rawPath.isNotEmpty()) rawPath else null
            val rawTab = backStackEntry.arguments?.getString("tab") ?: ""
            val tab = if (rawTab.isNotEmpty()) rawTab else null

            // The AI tools report "editResult" (they rewrote the working copy); Save hands "editedPath" to the caller
            val toolEdited by backStackEntry.savedStateHandle.getStateFlow("editResult", false).collectAsState()
            PhotoEditorScreen(
                initialPath = path,
                initialTab = tab,
                mediaRepository = app.mediaRepository,
                preferences = app.preferences,
                toolEdited = toolEdited,
                onToolEditConsumed = { backStackEntry.savedStateHandle["editResult"] = false },
                onOpenTool = { route -> navController.navigateOnce(route) },
                onSaved = { saved -> navController.previousBackStackEntry?.savedStateHandle?.set("editedPath", saved); navController.popBackStack() },
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(Screen.Search.route) {
            SearchScreen(
                mediaRepository = app.mediaRepository,
                moments = app.moments,
                onNavigate = { route -> navController.navigateOnce(route) },
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(
            route = Screen.SearchViewAll.route,
            arguments = listOf(
                navArgument("section") { type = NavType.StringType; defaultValue = "" },
                navArgument("query") { type = NavType.StringType; defaultValue = "" }
            )
        ) { backStackEntry ->
            SearchViewAllScreen(
                sectionArg = backStackEntry.arguments?.getString("section"),
                queryArg = backStackEntry.arguments?.getString("query"),
                mediaRepository = app.mediaRepository,
                moments = app.moments,
                onNavigate = { route -> navController.navigateOnce(route) },
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(
            route = Screen.SearchColor.route,
            arguments = listOf(
                navArgument("color") {
                    type = NavType.StringType
                    defaultValue = DEFAULT_SEARCH_COLOR
                }
            )
        ) { backStackEntry ->
            SearchColorScreen(
                colorHex = colorFromArg(backStackEntry.arguments?.getString("color")),
                mediaRepository = app.mediaRepository,
                vaultRepository = app.vaultRepository,
                onNavigate = { route -> navController.navigateOnce(route) },
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(Screen.Moments.route) {
            MomentsScreen(
                mediaRepository = app.mediaRepository,
                moments = app.moments,
                onMomentClick = { label -> navController.navigateOnce(momentRoute(label)) },
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(
            route = Screen.Place.route,
            arguments = listOf(navArgument("key") { type = NavType.StringType; defaultValue = "" })
        ) { backStackEntry ->
            PlaceScreen(
                selectedKey = backStackEntry.arguments?.getString("key") ?: "",
                mediaRepository = app.mediaRepository,
                onNavigate = { route -> navController.navigateOnce(route) },
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(
            route = Screen.StoryView.route,
            arguments = listOf(
                navArgument("path") { type = NavType.StringType; defaultValue = "" },
                navArgument("title") { type = NavType.StringType; defaultValue = "Story" }
            ),
            // Theme.NewGallery.Story: fade_in_screen / fade_out_screen, 500 ms
            enterTransition = { fadeIn(tween(500)) },
            exitTransition = { fadeOut(tween(500)) },
            popEnterTransition = { fadeIn(tween(500)) },
            popExitTransition = { fadeOut(tween(500)) }
        ) { backStackEntry ->
            val rawTitle = backStackEntry.arguments?.getString("title") ?: "Story"
            val title = rawTitle
            val rawPath = backStackEntry.arguments?.getString("path") ?: ""
            val path = rawPath
            val allMedia by app.mediaRepository.getActiveMediaFlow().collectAsState(initial = emptyList())
            // Built-in albums (Screenshots, Camera...) are "filter:" ids, not folders
            val filterMedia by androidx.compose.runtime.produceState(emptyList<com.quizango.gallery.domain.model.MediaItem>(), path) {
                if (path.startsWith("filter:")) app.mediaRepository.getMediaByFolderFlow(path).collect { value = it }
            }

            val storyItems = androidx.compose.runtime.remember(path, title, allMedia, filterMedia) {
                val photosOnly = allMedia.filter { it.mediaType != MediaType.VIDEO }
                val rowStory = com.quizango.gallery.ui.discovery.StoryQueue.find(path)
                when {
                    // Albums row story: play exactly the photos the row picked
                    rowStory != null -> rowStory.items
                    path == "recent" -> {
                        photosOnly.sortedByDescending { it.dateModified }.take(20)
                    }
                    path == "random" -> {
                        photosOnly.shuffled().take(20)
                    }
                    path.startsWith("filter:") -> {
                        filterMedia.filter { it.mediaType != MediaType.VIDEO }.take(20)
                    }
                    path == "favorite" -> {
                        val favs = photosOnly.filter { it.isFavorite }
                        if (favs.isNotEmpty()) favs.take(20) else photosOnly.take(20)
                    }
                    path.isNotEmpty() -> {
                        // Check if path points to a specific cover photo (e.g. from MomentsScreen)
                        val exactFileIdx = photosOnly.indexOfFirst { it.filePath == path }
                        if (exactFileIdx >= 0) {
                            val start = exactFileIdx
                            val end = (exactFileIdx + 15).coerceAtMost(photosOnly.size)
                            photosOnly.subList(start, end)
                        } else {
                            val albumPhotos = photosOnly.filter {
                                it.folderPath == path || it.folderPath.equals(path, ignoreCase = true)
                            }
                            if (albumPhotos.isNotEmpty()) albumPhotos.take(20) else photosOnly.take(20)
                        }
                    }
                    else -> photosOnly.take(20)
                }
            }

            // Vault slideshow: decrypted cache copies of the vault's photos
            val vaultStory by androidx.compose.runtime.produceState<List<com.quizango.gallery.domain.model.MediaItem>?>(null, path) {
                if (path == "vault") {
                    value = app.vaultRepository.getVaultItemsFlow().first()
                        .filter { it.mediaType != MediaType.VIDEO }
                        .mapNotNull { v ->
                            app.vaultRepository.viewFile(v)?.let { f ->
                                com.quizango.gallery.domain.model.MediaItem(
                                    id = 0, filePath = f.path, fileName = v.fileName, fileSize = v.fileSize,
                                    folderName = v.folderName, folderPath = v.folderPath,
                                    dateModified = v.dateHidden, dateTaken = v.dateHidden, mediaType = v.mediaType
                                )
                            }
                        }
                }
            }

            val editTipSeen by app.preferences.isStoriesEditToolTip.collectAsState(initial = true)
            val tipScope = androidx.compose.runtime.rememberCoroutineScope()

            StoryViewScreen(
                storyTitle = title,
                mediaItems = if (path == "vault") vaultStory.orEmpty() else storyItems,
                // No editing from the vault: edits would write the decrypted copy out
                onEditClick = if (path == "vault") null else { editPath -> navController.navigateOnce(Screen.Editor.createRoute(editPath)) },
                onNavigateBack = { navController.popBackStack() },
                fromStoryRow = com.quizango.gallery.ui.discovery.StoryQueue.find(path) != null,
                editTipSeen = editTipSeen,
                onEditTipSeen = { tipScope.launch { app.preferences.setStoriesEditToolTip(true) } },
                onFinished = {
                    val next = com.quizango.gallery.ui.discovery.StoryQueue.next(path)
                    if (next == null) navController.popBackStack()
                    else navController.navigate(Screen.StoryView.createRoute(next.id, "")) {
                        popUpTo(Screen.StoryView.route) { inclusive = true }
                    }
                }
            )
        }

        composable(Screen.AnimatedMedia.route) {
            AnimatedMediaScreen(
                mediaRepository = app.mediaRepository,
                vaultRepository = app.vaultRepository,
                onNavigate = { route -> navController.navigateOnce(route) },
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(Screen.BurstPhotos.route) {
            BurstPhotoScreen(
                mediaRepository = app.mediaRepository,
                onNavigate = { route -> navController.navigateOnce(route) },
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(Screen.QuickOrganizer.route) {
            QuickOrganizerScreen(
                mediaRepository = app.mediaRepository,
                onNavigateToRecycle = { navController.navigateOnce(Screen.OrganizerRecycle.route) },
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(Screen.OrganizerRecycle.route) {
            OrganizerRecycleScreen(
                mediaRepository = app.mediaRepository,
                appPreferences = app.preferences,
                onNavigate = { route -> navController.navigateOnce(route) },
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
            val path = rawPath

            WallpaperScreen(
                imagePath = path,
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(Screen.Stickers.route) {
            StickerScreen(onBack = { navController.popBackStack() })
        }

        composable(
            route = Screen.RemoveBg.route,
            arguments = listOf(
                navArgument("path") { type = NavType.StringType; defaultValue = "" }
            )
        ) { backStackEntry ->
            val rawPath = backStackEntry.arguments?.getString("path") ?: ""
            val path = rawPath

            RemoveBgScreen(
                imagePath = path,
                mediaRepository = app.mediaRepository,
                preferences = app.preferences,
                onSaved = { navController.previousBackStackEntry?.savedStateHandle?.set("editResult", true); navController.popBackStack() },
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
            val path = rawPath

            BlurBgScreen(
                imagePath = path,
                preferences = app.preferences,
                onSaved = { navController.previousBackStackEntry?.savedStateHandle?.set("editResult", true); navController.popBackStack() },
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
            val path = rawPath

            ColorSplashScreen(
                imagePath = path,
                preferences = app.preferences,
                onSaved = { navController.previousBackStackEntry?.savedStateHandle?.set("editResult", true); navController.popBackStack() },
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(Screen.Language.route) {
            val canNavigateBack = navController.previousBackStackEntry != null &&
                navController.previousBackStackEntry?.destination?.route != Screen.Splash.route
            LanguageScreen(
                appPreferences = app.preferences,
                canNavigateBack = canNavigateBack,
                isOnboarding = !canNavigateBack,
                // Reference: onboarding Language continues to Feature promo
                onNavigateToPermission = {
                    navController.navigate(Screen.FeaturePromo.route) {
                        popUpTo(Screen.Language.route) { inclusive = true }
                    }
                },
                onApply = {
                    navController.popBackStack()
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
                onNavigate = { route -> navController.navigateOnce(route) },
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

/**
 * Navigate without stacking a second copy of a screen that is already on the back stack
 * (Cleaner <-> Remove Junk, vault Change Password -> vault): go back to it instead.
 */
private fun NavHostController.navigateOnce(route: String) {
    if (!popBackStack(route, inclusive = false)) navigate(route) { launchSingleTop = true }
}

/** Pops [entry] only while it is still on top, so a late callback from a screen already left never pops the next one. */
private fun NavHostController.popIfCurrent(entry: NavBackStackEntry) {
    if (currentBackStackEntry == entry) popBackStack()
}

/** One lock destination for the plain vault route and the Settings / Add to Private routes. */
@Composable
private fun VaultLockDestination(navController: NavHostController, app: GalleryApplication, route: String, type: Int, openVault: Boolean) {
    val context = androidx.compose.ui.platform.LocalContext.current
    // VaultLockActivity sets isVaultHintToolTip on open: the Home vault hint never shows again
    LaunchedEffect(Unit) { app.preferences.setVaultHintToolTip(true) }
    VaultLockScreen(
        appPreferences = app.preferences,
        type = type,
        openVault = openVault,
        onOpenVault = {
            // Back to an open vault instead of stacking a second one; the vault replaces the lock so Back never returns to it
            if (!navController.popBackStack(Screen.VaultMedia.route, inclusive = false)) {
                navController.navigate(Screen.VaultMedia.route) { popUpTo(route) { inclusive = true } }
            }
        },
        onResultOk = { passwordSaved ->
            navController.popBackStack()
            settingsToast(type, passwordSaved)?.let { Toast.makeText(context, context.getString(it), Toast.LENGTH_SHORT).show() }
        },
        onOpenRecovery = { navController.navigate(Screen.VaultRecoveryQuestion.route) { launchSingleTop = true } },
        onOpenForgot = { navController.navigate(Screen.VaultForgotPassword.route) { launchSingleTop = true } },
        onNavigateBack = { navController.popBackStack() }
    )
}
