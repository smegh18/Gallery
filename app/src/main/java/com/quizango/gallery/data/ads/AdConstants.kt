package com.quizango.gallery.data.ads

/**
 * Advertising Unit Identifiers and Configuration for Gallery application.
 *
 * Non-negotiable requirements:
 * 1. Reference APK unit IDs are preserved as documented forensic evidence.
 * 2. Official Google Mobile Ads Test Ad Unit IDs are used for safe runtime execution and development.
 */
object AdConstants {
    // Official Google Mobile Ads Test Ad Units (Safe for verification and testing)
    const val TEST_APP_ID = "ca-app-pub-3940256099942544~3347511713"
    const val TEST_BANNER_ID = "ca-app-pub-3940256099942544/6300978111"
    const val TEST_INTERSTITIAL_ID = "ca-app-pub-3940256099942544/1033173712"
    const val TEST_NATIVE_ID = "ca-app-pub-3940256099942544/2247696110"
    const val TEST_REWARDED_ID = "ca-app-pub-3940256099942544/5224354917"
    const val TEST_APP_OPEN_ID = "ca-app-pub-3940256099942544/9257395921"

    // Functional aliases for screen placements
    const val BANNER_HOME = TEST_BANNER_ID
    const val BANNER_CLEANER = TEST_BANNER_ID
    const val BANNER_DUPLICATE_FINDER = TEST_BANNER_ID
    const val BANNER_FULLSCREEN_MEDIA = TEST_BANNER_ID
    const val BANNER_FAVORITES = TEST_BANNER_ID
    const val BANNER_SETTINGS = TEST_BANNER_ID
    const val BANNER_VAULT = TEST_BANNER_ID

    // Reference APK Recovered Ad Units (Extracted from strings.xml for evidence audit)
    const val REF_APP_ID = "ca-app-pub-2613001078711441~6552065013"
    const val REF_BANNER_ID = "ca-app-pub-2613001078711441/1299738331"
    const val REF_INTERSTITIAL_ID = "ca-app-pub-2613001078711441/3221811205"
    const val REF_NATIVE_ID = "ca-app-pub-2613001078711441/4343321189"
    const val REF_REWARDED_ID = "ca-app-pub-2613001078711441/4785340166"
    const val REF_APP_OPEN_ID = "ca-app-pub-2613001078711441/4766735589"

    // Discovered Reference Placement Identifiers
    const val PLACEMENT_HOME_BANNER = "home_banner"
    const val PLACEMENT_ALBUMS_NATIVE = "albums_native"
    const val PLACEMENT_CLEANER_BANNER = "cleaner_banner"
    const val PLACEMENT_FAVORITES_BANNER = "favorites_banner"
    const val PLACEMENT_RECYCLE_BANNER = "recycle_banner"
    const val PLACEMENT_SETTINGS_BANNER = "settings_banner"
    const val PLACEMENT_DUPLICATES_BANNER = "duplicates_banner"
    const val PLACEMENT_MEDIA_VIEWER_BANNER = "media_viewer_banner"

    // Interstitial Trigger Events matching Reference APK
    const val EVENT_ALBUM_CLICK = "enable_album_click_interstitial"
    const val EVENT_MEDIA_LIST_CLICK = "enable_media_list_click_interstitial"
    const val EVENT_MEDIA_DELETE = "enable_fscrn_media_delete_interstitial"
    const val EVENT_JUNK_SCAN = "enable_junk_scan_interstitial"
    const val EVENT_SCREEN_BACKPRESS = "enable_screen_backpress_interstitial"
    const val EVENT_EDIT_SAVE = "enable_edit_save_interstitial"

    // Rewarded Trigger Events
    const val EVENT_JUNK_REWARD = "enable_junk_reward"

    // Cooldown between full-screen ads in milliseconds (to prevent ad spam)
    const val INTERSTITIAL_COOLDOWN_MS = 30_000L
}
