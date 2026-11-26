package com.quizango.gallery.ui.floating

import android.animation.ObjectAnimator
import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Rect
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.view.Gravity
import android.view.HapticFeedbackConstants
import android.view.MotionEvent
import android.view.VelocityTracker
import android.view.View
import android.view.ViewConfiguration
import android.view.WindowInsets
import android.view.WindowManager
import android.view.animation.AccelerateDecelerateInterpolator
import android.view.animation.DecelerateInterpolator
import android.widget.FrameLayout
import android.widget.ImageView
import androidx.core.content.ContextCompat
import androidx.core.content.edit
import com.quizango.gallery.R
import kotlinx.coroutines.CoroutineScope
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

/**
 * bo/l: the edge handle (layout_floating_button) in a small window over other apps. Swipe in opens the panel, a long
 * press picks it up to move it (it snaps to the nearer edge), a tap wiggles it, and after 3 s it dims to 55 %.
 * Plain views: the window is moved and resized on every move, which a raw touch listener handles most directly.
 */
internal class SidebarHandle(private val context: Context, scope: CoroutineScope) {
    private enum class Gesture { NONE, PANEL, MOVE, IGNORED }

    private val windowManager = context.getSystemService(WindowManager::class.java)
    private val prefs = FloatingButtons.prefs(context)
    private val main = Handler(Looper.getMainLooper())
    private val touchSlop = ViewConfiguration.get(context).scaledTouchSlop
    private val panel = SidebarPanel(context, scope)
    private var intro: SidebarIntro? = null

    private var root: FrameLayout? = null
    private lateinit var pill: View
    private lateinit var drag: View
    private var params: WindowManager.LayoutParams? = null
    private var onLeft = false
    private var screenWidth = 0
    private var top = 0
    private var bottom = 0
    private var gesture = Gesture.NONE
    private var downX = 0f
    private var downY = 0f
    private var lastX = 0f
    private var lastY = 0f
    private var velocity: VelocityTracker? = null
    private var dimmed = false
    private var snap: ValueAnimator? = null

    private val autoDim = Runnable { if (!dimmed && root != null) { fade(1f, DIM_ALPHA); dimmed = true } }
    private val longPress = Runnable { pickUp() }
    private val nudge = Runnable { nudge() }

    private fun dp(value: Float) = (value * context.resources.displayMetrics.density).toInt()
    private fun dimen(id: Int) = context.resources.getDimensionPixelSize(id)

    /** bo/l.OooOOo: [firstTime] shows the intro over the new handle (assistive_touch_hint_showed not yet set). */
    fun show(firstTime: Boolean): Boolean {
        if (root != null) return true
        onLeft = prefs.getBoolean(FloatingButtons.KEY_ON_LEFT, false)
        measureScreen()
        pill = View(context).apply { background = ContextCompat.getDrawable(context, R.drawable.bg_floating_sidebar_handle) }
        drag = FrameLayout(context).apply {
            background = ContextCompat.getDrawable(context, R.drawable.bg_floating_sidebar_handle_drag)
            visibility = View.GONE
            addView(
                ImageView(context).apply { setImageResource(R.drawable.ic_floating_sidebar_drag); importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_NO },
                FrameLayout.LayoutParams(dimen(R.dimen._16sdp), dimen(R.dimen._16sdp), Gravity.CENTER)
            )
        }
        val view = FrameLayout(context).apply {
            contentDescription = context.getString(R.string.quick_access_floating_button_title)
            layoutDirection = if (onLeft) View.LAYOUT_DIRECTION_RTL else View.LAYOUT_DIRECTION_LTR
            addView(pill, FrameLayout.LayoutParams(dimen(R.dimen._4sdp), dimen(R.dimen._64sdp), Gravity.END or Gravity.CENTER_VERTICAL)
                .apply { marginEnd = dimen(R.dimen._2sdp) })
            addView(drag, FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, dimen(R.dimen._76sdp), Gravity.CENTER))
            setOnTouchListener { _, event -> onTouch(event) }
            // bo/g: system back gestures must not steal the swipe that starts on the handle
            addOnLayoutChangeListener { v, _, _, _, _, _, _, _, _ ->
                if (Build.VERSION.SDK_INT >= 29) v.systemGestureExclusionRects = listOf(Rect(0, 0, v.width, v.height))
            }
        }
        val height = dp(HEIGHT_DP)
        val lp = overlayParams(dp(TOUCH_WIDTH_DP), height).apply {
            x = edgeX(width)
            y = clampHandleY(prefs.getInt(FloatingButtons.KEY_HANDLE_Y, -1), top, bottom, height)
        }
        if (!firstTime) FloatingButtons.markIntroShown(context)
        val replay = !firstTime && replaysIntro(FloatingButtons.daysSinceIntro(context), prefs.getBoolean(FloatingButtons.KEY_INTRO_REPLAYED, false))
        val introShown = (firstTime || replay) && showIntro(lp, replay)
        if (runCatching { windowManager.addView(view, lp) }.isFailure) {
            intro?.dismiss()
            intro = null
            return false
        }
        root = view
        params = lp
        if (introShown) {
            view.post { intro?.pointAt(pillCenter()) }
            return true
        }
        scheduleDim()
        val days = FloatingButtons.daysSinceIntro(context)
        val today = System.currentTimeMillis() / FloatingButtons.DAY_MS
        if (nudges(days, prefs.getInt(FloatingButtons.KEY_NUDGE_COUNT, 0), prefs.getLong(FloatingButtons.KEY_NUDGE_DAY, -1L), today)) {
            main.postDelayed(nudge, NUDGE_DELAY_MS)
        }
        return true
    }

    private fun showIntro(lp: WindowManager.LayoutParams, replay: Boolean): Boolean {
        val screenHeight = screenSize().second
        val tour = SidebarIntro(context)
        val shown = tour.show(
            handleCenterY = lp.y + lp.height / 2, left = onLeft, screenWidth = screenWidth, screenHeight = screenHeight,
            statusBar = top, navBar = screenHeight - bottom, tools = FloatingButtons.sidebarTools(context), replay = replay
        ) { onIntroFinished() }
        if (!shown) return false
        intro = tour
        if (replay) prefs.edit { putBoolean(FloatingButtons.KEY_INTRO_REPLAYED, true) } else FloatingButtons.markIntroShown(context)
        return true
    }

    private fun onIntroFinished() {
        intro = null
        scheduleDim()
        prefs.edit { putBoolean(FloatingButtons.KEY_HINT_SHOWED, true) }
    }

    private fun pillCenter(): Pair<Float, Float> {
        val at = IntArray(2).also { pill.getLocationOnScreen(it) }
        return (at[0] + pill.width / 2f) to (at[1] + pill.height / 2f)
    }

    private fun screenSize(): Pair<Int, Int> =
        if (Build.VERSION.SDK_INT >= 30) windowManager.currentWindowMetrics.bounds.let { it.width() to it.height() }
        else android.util.DisplayMetrics().also { @Suppress("DEPRECATION") windowManager.defaultDisplay.getRealMetrics(it) }
            .let { it.widthPixels to it.heightPixels }

    /** bo/l.OooO0oo: the handle moves between the status bar and the navigation bar, across the whole width. */
    private fun measureScreen() {
        val (width, height) = screenSize()
        val res = context.resources
        fun bar(name: String) = res.getIdentifier(name, "dimen", "android").let { if (it > 0) res.getDimensionPixelSize(it) else 0 }
        top = bar("status_bar_height").takeIf { it > 0 } ?: dp(24f)
        val nav = if (Build.VERSION.SDK_INT >= 30) {
            windowManager.currentWindowMetrics.windowInsets.getInsets(WindowInsets.Type.navigationBars()).bottom.takeIf { it > 0 }
                ?: bar("navigation_bar_height")
        } else bar("navigation_bar_height")
        bottom = height - nav
        screenWidth = width
    }

    private fun edgeX(width: Int) = if (onLeft) 0 else screenWidth - width

    private fun onTouch(event: MotionEvent): Boolean {
        val view = root ?: return false
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                if (snap != null) {
                    gesture = Gesture.IGNORED
                    return true
                }
                main.removeCallbacks(nudge)
                velocity?.recycle()
                velocity = VelocityTracker.obtain().apply { addMovement(event) }
                downX = event.rawX; downY = event.rawY; lastX = event.rawX; lastY = event.rawY
                if (gesture != Gesture.MOVE) {
                    gesture = Gesture.NONE
                    main.removeCallbacks(autoDim)
                    if (dimmed) {
                        dimmed = false
                        fade(DIM_ALPHA, 1f)
                    }
                    main.postDelayed(longPress, min(ViewConfiguration.getLongPressTimeout().toLong(), LONG_PRESS_MAX_MS))
                }
            }
            MotionEvent.ACTION_MOVE -> {
                velocity?.addMovement(event)
                lastX = event.rawX; lastY = event.rawY
                val inward = (event.rawX - downX) * if (onLeft) 1f else -1f
                val dy = event.rawY - downY
                val wasNone = gesture == Gesture.NONE
                if (wasNone) {
                    gesture = when {
                        inward > touchSlop && inward > abs(dy) -> if (openPanel(view)) Gesture.PANEL else Gesture.IGNORED
                        -inward > dp(OUTWARD_CANCEL_DP) || max(abs(event.rawX - downX), abs(dy)) > touchSlop * 3 -> Gesture.IGNORED
                        else -> Gesture.NONE
                    }
                    if (gesture != Gesture.NONE) main.removeCallbacks(longPress)
                }
                when (gesture) {
                    Gesture.PANEL -> panel.dragOpen(inward)
                    Gesture.MOVE -> moveTo(event.rawX, event.rawY)
                    else -> Unit
                }
                // 2.7.2 bo/l: once the panel opens, later moves count from here, not from the touch-down point
                if (wasNone && gesture == Gesture.PANEL) downX = event.rawX
            }
            MotionEvent.ACTION_UP -> {
                velocity?.addMovement(event)
                release(cancelled = false)
            }
            MotionEvent.ACTION_CANCEL -> release(cancelled = true)
        }
        return true
    }

    /** The swipe went inward: the panel follows the finger and the handle slides out of the way. */
    private fun openPanel(view: View): Boolean {
        val lp = params ?: return false
        if (!panel.open(lp.y + lp.height / 2, onLeft) { onPanelClosed() }) return false
        intro?.finish()
        main.removeCallbacks(autoDim)
        view.animate().alpha(0f).translationX(if (onLeft) -view.width.toFloat() else view.width.toFloat())
            .setDuration(SLIDE_MS).setInterpolator(DecelerateInterpolator())
            .withEndAction { view.visibility = View.INVISIBLE }.start()
        return true
    }

    /** bo/l.OyIbF7L6XB: the panel went: the handle slides back in. */
    private fun onPanelClosed() {
        val view = root ?: return
        view.animate().cancel()
        view.visibility = View.VISIBLE
        view.translationX = if (onLeft) -view.width.toFloat() else view.width.toFloat()
        view.alpha = 0f
        view.animate().alpha(1f).translationX(0f).setDuration(SLIDE_MS).setInterpolator(DecelerateInterpolator())
            .withEndAction { dimmed = false }.start()
        scheduleDim()
    }

    private fun release(cancelled: Boolean) {
        main.removeCallbacks(longPress)
        when (gesture) {
            Gesture.PANEL -> {
                val vx = velocity?.run { computeCurrentVelocity(1000); xVelocity } ?: 0f
                panel.release(if (cancelled) 0f else vx * if (onLeft) 1f else -1f)
            }
            Gesture.MOVE -> snapToEdge()
            Gesture.NONE -> if (cancelled) scheduleDim() else if (!panel.showing) {
                tapHint()
                scheduleDim()
            }
            Gesture.IGNORED -> scheduleDim()
        }
        gesture = Gesture.NONE
        velocity?.recycle()
        velocity = null
    }

    /** Long press: the pill turns into the drag pad, which follows the finger. */
    private fun pickUp() {
        val view = root ?: return
        val lp = params ?: return
        if (gesture != Gesture.NONE) return
        gesture = Gesture.MOVE
        intro?.finish()
        view.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
        view.animate().cancel()
        view.alpha = 1f
        view.translationX = 0f
        dimmed = false
        pill.visibility = View.GONE
        drag.visibility = View.VISIBLE
        drag.scaleX = PICK_UP_SCALE
        drag.scaleY = PICK_UP_SCALE
        drag.animate().scaleX(1f).scaleY(1f).setDuration(PICK_UP_MS).start()
        lp.width = dp(DRAG_WIDTH_DP)
        moveTo(lastX, lastY)
    }

    private fun moveTo(x: Float, y: Float) {
        val lp = params ?: return
        lp.x = (x.toInt() - lp.width / 2).coerceIn(0, max(0, screenWidth - lp.width))
        lp.y = (y.toInt() - lp.height / 2).coerceIn(top, max(top, bottom - lp.height))
        update()
    }

    /** Dropped: glide to the nearer edge, then settle there and remember the side and height. */
    private fun snapToEdge() {
        val lp = params ?: return
        val toLeft = snapsLeft(lp.x, lp.width, screenWidth)
        snap?.cancel()
        snap = ValueAnimator.ofInt(lp.x, if (toLeft) 0 else screenWidth - lp.width).apply {
            duration = SNAP_MS
            interpolator = DecelerateInterpolator()
            addUpdateListener { lp.x = it.animatedValue as Int; update() }
            onFinished {
                snap = null
                settle(toLeft)
            }
            start()
        }
    }

    private fun settle(toLeft: Boolean) {
        val view = root ?: return
        val lp = params ?: return
        onLeft = toLeft
        drag.visibility = View.GONE
        pill.visibility = View.VISIBLE
        view.layoutDirection = if (onLeft) View.LAYOUT_DIRECTION_RTL else View.LAYOUT_DIRECTION_LTR
        lp.width = dp(TOUCH_WIDTH_DP)
        lp.x = edgeX(lp.width)
        update()
        prefs.edit {
            putBoolean(FloatingButtons.KEY_ON_LEFT, toLeft)
            putInt(FloatingButtons.KEY_HANDLE_Y, lp.y)
        }
        scheduleDim()
    }

    /** A tap without a swipe: the handle peeks 7 dp inward and back. */
    private fun tapHint() = wiggle(TAP_HINT_MS, 1)

    /** bo/l.R7N8DF4OVS: two peeks, while the user has not got the hang of it yet. */
    private fun nudge() {
        if (gesture != Gesture.NONE || panel.showing || intro != null || root == null) return
        prefs.edit {
            putInt(FloatingButtons.KEY_NUDGE_COUNT, prefs.getInt(FloatingButtons.KEY_NUDGE_COUNT, 0) + 1)
            putLong(FloatingButtons.KEY_NUDGE_DAY, System.currentTimeMillis() / FloatingButtons.DAY_MS)
        }
        main.removeCallbacks(autoDim)
        wiggle(NUDGE_MS, 2).onFinished { scheduleDim() }
    }

    private fun wiggle(duration: Long, peeks: Int): ObjectAnimator {
        val view = root!!
        view.animate().cancel()
        view.alpha = 1f
        dimmed = false
        val peek = dp(PEEK_DP).toFloat() * if (onLeft) 1f else -1f
        val path = FloatArray(peeks * 2 + 1) { if (it % 2 == 1) peek else 0f }
        return ObjectAnimator.ofFloat(view, View.TRANSLATION_X, *path).apply {
            this.duration = duration
            interpolator = AccelerateDecelerateInterpolator()
            start()
        }
    }

    private fun scheduleDim() {
        main.removeCallbacks(autoDim)
        main.postDelayed(autoDim, AUTO_DIM_MS)
    }

    private fun fade(from: Float, to: Float) {
        val view = root ?: return
        ValueAnimator.ofFloat(from, to).apply {
            duration = TAP_HINT_MS
            interpolator = DecelerateInterpolator()
            addUpdateListener { view.alpha = it.animatedValue as Float }
            start()
        }
    }

    private fun update() {
        val view = root ?: return
        runCatching { windowManager.updateViewLayout(view, params) }
    }

    /** bo/l.OooOOOo: everything goes, the panel and intro windows included. */
    fun remove() {
        main.removeCallbacksAndMessages(null)
        snap?.cancel()
        snap = null
        velocity?.recycle()
        velocity = null
        panel.destroy()
        intro?.dismiss()
        intro = null
        root?.let { view ->
            view.animate().cancel()
            runCatching { windowManager.removeView(view) }
        }
        root = null
        params = null
        dimmed = false
    }

    private companion object {
        const val AUTO_DIM_MS = 3000L
        const val DRAG_WIDTH_DP = 40f
        const val HEIGHT_DP = 120f
        const val TOUCH_WIDTH_DP = 32f
        const val DIM_ALPHA = 0.55f
        const val SLIDE_MS = 200L
        const val LONG_PRESS_MAX_MS = 450L
        const val NUDGE_DELAY_MS = 1500L
        const val PEEK_DP = 7f
        const val NUDGE_MS = 600L
        const val OUTWARD_CANCEL_DP = 3f
        const val PICK_UP_MS = 150L
        const val PICK_UP_SCALE = 0.6f
        const val SNAP_MS = 220L
        const val TAP_HINT_MS = 300L
    }
}
