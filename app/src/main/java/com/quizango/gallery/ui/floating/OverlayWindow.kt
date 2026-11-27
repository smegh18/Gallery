package com.quizango.gallery.ui.floating

import android.content.Context
import android.graphics.PixelFormat
import android.os.Build
import android.view.Gravity
import android.view.WindowManager
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.ComposeView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleRegistry
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.savedstate.SavedStateRegistry
import androidx.savedstate.SavedStateRegistryController
import androidx.savedstate.SavedStateRegistryOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner

/**
 * The window flags 2.7.2 gives every floating window (776): laid out under the bars and cutout, never focused. Windows
 * added outside an activity are not hardware accelerated unless asked, which Compose layers and shadows need.
 */
fun overlayParams(width: Int, height: Int, touchable: Boolean = true): WindowManager.LayoutParams =
    WindowManager.LayoutParams(
        width, height,
        if (Build.VERSION.SDK_INT >= 26) WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        else @Suppress("DEPRECATION") WindowManager.LayoutParams.TYPE_PHONE,
        WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS or WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED or
            (if (touchable) 0 else WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE),
        PixelFormat.TRANSLUCENT
    ).apply {
        gravity = Gravity.TOP or Gravity.LEFT // absolute, so x stays the distance from the left edge in RTL too
        if (Build.VERSION.SDK_INT >= 30) setFitInsetsTypes(0)
        windowAnimations = 0
        if (Build.VERSION.SDK_INT >= 28) layoutInDisplayCutoutMode =
            if (Build.VERSION.SDK_INT >= 30) WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_ALWAYS
            else WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
    }

/**
 * A Compose window over other apps. A window added from a service has no activity, so it brings its own lifecycle
 * and saved-state owner; [remove] destroys them, which disposes the composition and stops its animations.
 */
class ComposeOverlay(context: Context, val params: WindowManager.LayoutParams, content: @Composable () -> Unit) : SavedStateRegistryOwner {
    private val registry = LifecycleRegistry(this)
    private val savedState = SavedStateRegistryController.create(this).apply { performRestore(null) }
    private val windowManager = context.getSystemService(WindowManager::class.java)
    override val lifecycle: Lifecycle get() = registry
    override val savedStateRegistry: SavedStateRegistry get() = savedState.savedStateRegistry

    val view = ComposeView(context).apply {
        setViewTreeLifecycleOwner(this@ComposeOverlay)
        setViewTreeSavedStateRegistryOwner(this@ComposeOverlay)
        setContent(content)
    }

    fun add(): Boolean = runCatching {
        registry.currentState = Lifecycle.State.RESUMED
        windowManager.addView(view, params)
    }.onFailure {
        android.util.Log.w("ComposeOverlay", "addView failed", it)
        destroy()
    }.isSuccess

    fun update() {
        runCatching { windowManager.updateViewLayout(view, params) }
    }

    fun remove() {
        runCatching { windowManager.removeView(view) }
        destroy()
    }

    private fun destroy() {
        if (registry.currentState.isAtLeast(Lifecycle.State.CREATED)) registry.currentState = Lifecycle.State.DESTROYED
    }
}
