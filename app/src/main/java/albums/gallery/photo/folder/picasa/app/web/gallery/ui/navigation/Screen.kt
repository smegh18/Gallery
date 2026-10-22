package albums.gallery.photo.folder.picasa.app.web.gallery.ui.navigation

import java.net.URLEncoder
import java.nio.charset.StandardCharsets

sealed class Screen(val route: String) {
    data object Splash : Screen("splash")
    data object Welcome : Screen("welcome")
    data object Permission : Screen("permission")
    data object Home : Screen("home")
    
    data object AlbumDetail : Screen("album/{bucketId}/{bucketName}") {
        fun createRoute(bucketId: String, bucketName: String): String {
            val encodedName = URLEncoder.encode(bucketName, StandardCharsets.UTF_8.toString())
            val encodedId = URLEncoder.encode(bucketId, StandardCharsets.UTF_8.toString())
            return "album/$encodedId/$encodedName"
        }
    }

    data object MediaViewer : Screen("viewer?path={path}&folder={folder}") {
        fun createRoute(path: String, folder: String = ""): String {
            val encodedPath = URLEncoder.encode(path, StandardCharsets.UTF_8.toString())
            val encodedFolder = URLEncoder.encode(folder, StandardCharsets.UTF_8.toString())
            return "viewer?path=$encodedPath&folder=$encodedFolder"
        }
    }

    data object Favorites : Screen("favorites")
    data object RecycleBin : Screen("recycle")
    data object Duplicates : Screen("duplicates")
    data object VaultLock : Screen("vault_lock")
    data object VaultMedia : Screen("vault_media")
    data object Cleaner : Screen("cleaner")
    data object Editor : Screen("editor?path={path}") {
        fun createRoute(path: String = ""): String {
            val encoded = URLEncoder.encode(path, StandardCharsets.UTF_8.toString())
            return "editor?path=$encoded"
        }
    }
    data object Search : Screen("search")
    data object Language : Screen("language")
    data object Theme : Screen("theme")
    data object Settings : Screen("settings")
    data object About : Screen("about")
}
