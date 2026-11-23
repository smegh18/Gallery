package com.quizango.gallery.ui.vault

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.quizango.gallery.GalleryApplication
import com.quizango.gallery.ui.navigation.Screen
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/** Lock checks shared by every Add to Private entry point (rules L5, M6). */
internal object VaultGate {
    /** A lock counts as set when either secret is non-empty, including plain values saved by older builds. */
    fun lockSet(pin: String?, pattern: String?) = !pin.isNullOrEmpty() || !pattern.isNullOrEmpty()

    fun setupFinished(lockSet: Boolean, hasQuestion: Boolean) = lockSet && hasQuestion

    /** The paths to hide once the setup trip is over; null when nothing waits or setup was cancelled before it finished. */
    fun resolveReturn(awaiting: List<String>?, lockSet: Boolean, hasQuestion: Boolean): List<String>? =
        awaiting?.takeIf { setupFinished(lockSet, hasQuestion) }
}

/**
 * Add to Private gate (D5, 2.7.2 isPassword): with a lock, [onReady] gets the paths at once; without one, first-time
 * setup opens and [onReady] runs when this screen resumes with a lock and a recovery question saved. The stored lock
 * is the setup result, and 07-03 wipes it when setup is cancelled, so Back hides nothing.
 */
@Composable
internal fun rememberVaultHideGate(onNavigate: (String) -> Unit, onReady: (List<String>) -> Unit): (List<String>) -> Unit {
    val prefs = (LocalContext.current.applicationContext as GalleryApplication).preferences
    val scope = rememberCoroutineScope()
    // shortcut: saved as a Bundle entry, a selection of thousands of paths could overflow it; fine for hand-picked selections
    var awaiting by rememberSaveable { mutableStateOf<ArrayList<String>?>(null) }
    val ready by rememberUpdatedState(onReady)
    val navigate by rememberUpdatedState(onNavigate)

    val lifecycle = LocalLifecycleOwner.current.lifecycle
    DisposableEffect(lifecycle) {
        val observer = LifecycleEventObserver { _, event ->
            val waiting = awaiting
            if (event == Lifecycle.Event.ON_RESUME && waiting != null) {
                awaiting = null
                scope.launch {
                    VaultGate.resolveReturn(waiting, prefs.isVaultLockSet.first(), prefs.hasRecoveryQuestion.first())?.let(ready)
                }
            }
        }
        lifecycle.addObserver(observer)
        onDispose { lifecycle.removeObserver(observer) }
    }

    return { paths ->
        if (paths.isNotEmpty()) scope.launch {
            if (prefs.isVaultLockSet.first()) ready(paths) else {
                awaiting = ArrayList(paths)
                navigate(Screen.VaultLockFor.createRoute(0, false))
            }
        }
    }
}
