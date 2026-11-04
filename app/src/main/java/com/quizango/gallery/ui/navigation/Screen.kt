package com.quizango.gallery.ui.navigation

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

    data object MediaViewer : Screen("viewer?path={path}&folder={folder}&isRecycle={isRecycle}&isVault={isVault}") {
        fun createRoute(
            path: String,
            folder: String = "",
            isRecycle: Boolean = false,
            isVault: Boolean = false
        ): String {
            val encodedPath = URLEncoder.encode(path, StandardCharsets.UTF_8.toString())
            val encodedFolder = URLEncoder.encode(folder, StandardCharsets.UTF_8.toString())
            return "viewer?path=$encodedPath&folder=$encodedFolder&isRecycle=$isRecycle&isVault=$isVault"
        }
    }

    data object Favorites : Screen("favorites")
    data object RecycleBin : Screen("recycle")
    data object Duplicates : Screen("duplicates")
    data object VaultLock : Screen("vault_lock")
    data object VaultMedia : Screen("vault_media")
    data object VaultAdd : Screen("vault/add")
    data object Cleaner : Screen("cleaner")
    data object Editor : Screen("editor?path={path}&tab={tab}") {
        fun createRoute(path: String = "", initialTab: String = ""): String {
            val encoded = URLEncoder.encode(path, StandardCharsets.UTF_8.toString())
            val encodedTab = URLEncoder.encode(initialTab, StandardCharsets.UTF_8.toString())
            return "editor?path=$encoded&tab=$encodedTab"
        }
    }
    data object Search : Screen("search")
    data object SearchColor : Screen("search/color?color={color}") {
        fun createRoute(color: String = "#FF0000"): String {
            val encoded = URLEncoder.encode(color, StandardCharsets.UTF_8.toString())
            return "search/color?color=$encoded"
        }
    }
    data object Language : Screen("language")
    data object Theme : Screen("theme")
    data object Settings : Screen("settings")
    data object About : Screen("about")

    // Cleaner Suite
    data object LargeVideos : Screen("cleaner/large_videos")
    data object Screenshots : Screen("cleaner/screenshots")
    data object RemoveJunk : Screen("cleaner/remove_junk")
    data object DuplicateWaiting : Screen("duplicates/waiting")
    data object DuplicateComplete : Screen("duplicates/complete?count={count}&size={size}") {
        fun createRoute(count: Int, size: Long): String {
            return "duplicates/complete?count=$count&size=$size"
        }
    }

    // Vault Suite
    data object VaultSetting : Screen("vault/settings")
    data object VaultRecoveryQuestion : Screen("vault/recovery_question")
    data object VaultForgotPassword : Screen("vault/forgot_password")

    // Discovery & Media Types
    data object Moments : Screen("moments")
    data object StoryView : Screen("story?path={path}&title={title}") {
        fun createRoute(path: String, title: String): String {
            val encodedPath = URLEncoder.encode(path, StandardCharsets.UTF_8.toString())
            val encodedTitle = URLEncoder.encode(title, StandardCharsets.UTF_8.toString())
            return "story?path=$encodedPath&title=$encodedTitle"
        }
    }
    data object AnimatedMedia : Screen("media/animated")
    data object BurstPhotos : Screen("media/burst")
    data object QuickOrganizer : Screen("organizer")
    data object OrganizerRecycle : Screen("organizer/recycle")

    // Creative Suite
    data object CollageMaker : Screen("creative/collage")
    data object Wallpaper : Screen("creative/wallpaper?path={path}") {
        fun createRoute(path: String): String {
            val encoded = URLEncoder.encode(path, StandardCharsets.UTF_8.toString())
            return "creative/wallpaper?path=$encoded"
        }
    }
    data object Stickers : Screen("creative/stickers")
    data object RemoveBg : Screen("creative/remove_bg?path={path}") {
        fun createRoute(path: String = ""): String {
            val encoded = URLEncoder.encode(path, StandardCharsets.UTF_8.toString())
            return "creative/remove_bg?path=$encoded"
        }
    }
    data object BlurBg : Screen("creative/blur_bg?path={path}") {
        fun createRoute(path: String = ""): String {
            val encoded = URLEncoder.encode(path, StandardCharsets.UTF_8.toString())
            return "creative/blur_bg?path=$encoded"
        }
    }
    data object ColorSplash : Screen("creative/color_splash?path={path}") {
        fun createRoute(path: String = ""): String {
            val encoded = URLEncoder.encode(path, StandardCharsets.UTF_8.toString())
            return "creative/color_splash?path=$encoded"
        }
    }
}
