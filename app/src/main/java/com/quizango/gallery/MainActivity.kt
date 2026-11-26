package com.quizango.gallery

import android.content.ClipData
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.rememberNavController
import com.quizango.gallery.ui.external.ExternalPickScreen
import com.quizango.gallery.ui.external.PickRequest
import com.quizango.gallery.ui.external.pickKind
import com.quizango.gallery.ui.home.LocalOpenedFromOutside
import com.quizango.gallery.ui.navigation.EXTRA_NOTIFICATION_ACTION
import com.quizango.gallery.ui.navigation.EXTRA_SHORTCUT_ROUTE
import com.quizango.gallery.ui.navigation.Entry
import com.quizango.gallery.ui.navigation.GalleryNavHost
import com.quizango.gallery.ui.navigation.Screen
import com.quizango.gallery.ui.navigation.deepLinkEntry
import com.quizango.gallery.ui.navigation.isDeepLink
import com.quizango.gallery.ui.navigation.notificationEntry
import com.quizango.gallery.ui.navigation.shortcutEntry
import com.quizango.gallery.ui.theme.GalleryTheme
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.first

// AppCompatActivity (a FragmentActivity, so BiometricPrompt still works) applies the in-app theme and language to XML resources too
class MainActivity : AppCompatActivity() {

    /** A deep link, notification or shortcut intent waiting for Home to be on the back stack. */
    private var pendingEntry by mutableStateOf<Intent?>(null)

    /** HA.o000O00O: Home opened from a notification, deep link or shortcut never shows the vault hint. */
    private var openedFromOutside by mutableStateOf(false)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val app = application as GalleryApplication
        val pick = pickRequestOf(intent)
        if (pick == null && savedInstanceState == null) accept(intent)

        setContent {
            val themeMode by app.preferences.themeMode.collectAsState(initial = 2)
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
                    if (pick != null) {
                        ExternalPickScreen(pick, app.mediaRepository, ::finishPick)
                    } else {
                        val navController = rememberNavController()
                        CompositionLocalProvider(LocalOpenedFromOutside provides openedFromOutside) {
                            GalleryNavHost(navController = navController, app = app)
                        }
                        val entry = pendingEntry
                        LaunchedEffect(entry) {
                            if (entry != null) openEntry(entry, navController, app)
                        }
                    }
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        accept(intent)
    }

    private fun accept(intent: Intent) {
        val outside = runCatching {
            isDeepLink(intent.data?.scheme, intent.data?.host, intent.data?.path) ||
                intent.hasExtra(EXTRA_NOTIFICATION_ACTION) || intent.hasExtra(EXTRA_SHORTCUT_ROUTE)
        }.getOrDefault(false) // a malformed extras bundle from another app is ignored
        if (outside) {
            openedFromOutside = true
            pendingEntry = intent
        }
    }

    /** 2.7.2 lands every outside entry on Home first (Splash, Home, then the screen); Splash and onboarding run as usual. */
    private suspend fun openEntry(intent: Intent, navController: NavHostController, app: GalleryApplication) {
        navController.currentBackStackEntryFlow.first {
            runCatching { navController.getBackStackEntry(Screen.Home.route) }.isSuccess
        }
        val entry = runCatching {
            val data = intent.data
            when {
                isDeepLink(data?.scheme, data?.host, data?.path) ->
                    deepLinkEntry(data?.getQueryParameter("activity"), data?.getQueryParameter("key"))
                intent.hasExtra(EXTRA_NOTIFICATION_ACTION) ->
                    notificationEntry(intent.getStringExtra(EXTRA_NOTIFICATION_ACTION), app.preferences.isJunkCleanedForToday.first())
                else -> shortcutEntry(intent.getStringExtra(EXTRA_SHORTCUT_ROUTE))
            }
        }.onFailure { if (it is CancellationException) throw it }.getOrNull() // a cancelled read must not fall back to Home
        navController.popBackStack(Screen.Home.route, inclusive = false)
        when (entry) {
            is Entry.Open -> navController.navigate(entry.route) { launchSingleTop = true }
            Entry.Camera -> {
                // HA.o000O0oo: the still-image camera, or "Required app not available"
                val camera = Intent(MediaStore.INTENT_ACTION_STILL_IMAGE_CAMERA)
                if (camera.resolveActivity(packageManager) != null) startActivity(camera)
                else Toast.makeText(this, R.string.required_app_not_found, Toast.LENGTH_SHORT).show()
            }
            is Entry.Browse -> runCatching { startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(entry.url))) }
                .onFailure { Toast.makeText(this, R.string.browser_not_available, Toast.LENGTH_SHORT).show() }
            null -> Unit // Home
        }
        // Cleared only now: clearing earlier changes the LaunchedEffect key and cancels this dispatch mid-read
        if (pendingEntry === intent) pendingEntry = null
    }

    /** ExternalSelectMediaActivity's filter: GET_CONTENT / PICK; the caller's mime type and ALLOW_MULTIPLE are only read. */
    private fun pickRequestOf(intent: Intent): PickRequest? {
        if (intent.action != Intent.ACTION_GET_CONTENT && intent.action != Intent.ACTION_PICK) return null
        return runCatching {
            PickRequest(
                kind = pickKind(intent.type ?: intent.resolveType(contentResolver)),
                multiple = intent.getBooleanExtra(Intent.EXTRA_ALLOW_MULTIPLE, false)
            )
        }.getOrDefault(PickRequest(pickKind(null), multiple = false))
    }

    /** One pick returns data, several a ClipData, both with read access granted (2.7.2 OooooO0 / bl/e). */
    private fun finishPick(uris: List<Uri>) {
        if (uris.isEmpty()) {
            setResult(RESULT_CANCELED)
        } else {
            val result = Intent().addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            if (uris.size == 1) result.data = uris[0]
            else result.clipData = ClipData.newRawUri(null, uris[0]).apply { uris.drop(1).forEach { addItem(ClipData.Item(it)) } }
            setResult(RESULT_OK, result)
        }
        finish()
    }
}
