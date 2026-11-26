package com.quizango.gallery.ui.floating

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.ValueAnimator
import android.content.Context
import android.content.Intent
import android.provider.MediaStore
import android.view.ViewGroup
import android.view.WindowManager
import android.view.animation.DecelerateInterpolator
import android.view.animation.LinearInterpolator
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Easing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.PointerInputScope
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.util.VelocityTracker
import androidx.compose.ui.layout.layout
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.core.graphics.ColorUtils
import com.quizango.gallery.GalleryApplication
import com.quizango.gallery.MainActivity
import com.quizango.gallery.R
import com.quizango.gallery.ui.navigation.Screen
import com.quizango.gallery.ui.theme.GalleryText
import com.quizango.gallery.ui.theme.sdp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin

/** DecelerateInterpolator(1). */
internal val Decelerate = Easing { t -> 1f - (1f - t) * (1f - t) }

/** OvershootInterpolator(tension). */
internal fun overshoot(tension: Float) = Easing { t -> val x = t - 1f; x * x * ((tension + 1f) * x + tension) + 1f }

/** Runs [block] when the animation reaches its end, not when it is cancelled (a new drag, or the window going). */
internal fun Animator.onFinished(block: () -> Unit) = addListener(object : AnimatorListenerAdapter() {
    private var cancelled = false
    override fun onAnimationCancel(animation: Animator) { cancelled = true }
    override fun onAnimationEnd(animation: Animator) { if (!cancelled) block() }
})

/** 24 px kept clear of the bars and added past the edge (bo/s EDGE_PADDING_PX). */
private const val EDGE_GAP = 24

/**
 * bo/s: the quick-tools panel (layout_floating_popup_menu) in a full-screen window over other apps. The handle's swipe
 * drags it in while the window ignores touches; once released it settles open or closed and takes touches itself.
 */
internal class SidebarPanel(private val context: Context, private val scope: CoroutineScope) {
    private val res = context.resources
    private val marginEnd = res.getDimension(R.dimen._20sdp)
    private var overlay: ComposeOverlay? = null
    private var animator: ValueAnimator? = null
    private var wobbler: ValueAnimator? = null
    private var onClosed: (() -> Unit)? = null
    private var dismissing = false
    private var closedOffset = 1f
    private var statusBar = 0
    private var navBar = 0

    var left by mutableStateOf(false); private set
    var anchorY by mutableIntStateOf(0); private set
    var tools by mutableStateOf(DEFAULT_SIDEBAR); private set
    var editing by mutableStateOf(false); private set
    var moreOpen by mutableStateOf(false); private set
    var cascaded by mutableStateOf(true); private set
    var translation by mutableFloatStateOf(0f); private set // 0 open, closedOffset hidden past the edge
    var wobble by mutableFloatStateOf(0f); private set
    var rowSize by mutableStateOf(Size.Zero)
    var cardSize by mutableStateOf(Size.Zero)
    var moreSize by mutableStateOf(Size.Zero)

    val showing get() = overlay != null
    private val openFraction get() = if (closedOffset > 0f) 1f - abs(translation) / closedOffset else 1f

    /** bo/s.OooO0O0: adds the panel just past the edge at the handle's height; false when one is already up. */
    fun open(anchorY: Int, left: Boolean, onClosed: () -> Unit): Boolean {
        if (showing) return false
        fun bar(name: String) = res.getIdentifier(name, "dimen", "android").let { if (it > 0) res.getDimensionPixelSize(it) else 0 }
        statusBar = bar("status_bar_height")
        navBar = bar("navigation_bar_height")
        this.anchorY = anchorY
        this.left = left
        this.onClosed = onClosed
        editing = false
        moreOpen = false
        cascaded = false
        dismissing = false
        tools = FloatingButtons.sidebarTools(context)
        closedOffset = marginEnd + res.getDimension(R.dimen._68sdp) + EDGE_GAP
        translation = closedOffset
        val window = ComposeOverlay(
            context,
            overlayParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT, touchable = false)
        ) { PanelContent(this) }
        if (!window.add()) return false
        overlay = window
        return true
    }

    /** The handle's swipe: [inward] px dragged in from the edge. */
    fun dragOpen(inward: Float) = moveTo(closedOffset - inward)

    /** The panel's own swipe back out: [outward] px from the open position. */
    fun dragClosed(outward: Float) = moveTo(outward)

    private fun moveTo(value: Float) {
        if (dismissing) return
        animator?.cancel()
        translation = value.coerceIn(0f, closedOffset)
        if (openFraction > 0.5f) cascaded = true
    }

    /** bo/s.OooOO0O: [velocity] is px/s towards opening. From here on the window takes touches. */
    fun release(velocity: Float) {
        overlay?.let {
            it.params.flags = it.params.flags and WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE.inv()
            it.update()
        }
        settle(opensOnRelease(velocity, openFraction))
    }

    fun close() {
        if (showing) settle(false)
    }

    /** bo/s.OooOo0: slide open (then the neck wobbles) or out past the edge (then the window goes), 260 ms the full way. */
    private fun settle(open: Boolean) {
        if (dismissing) return
        if (open) cascaded = true
        else {
            dismissing = true
            if (rowSize.width > 0f) closedOffset = rowSize.width + marginEnd + EDGE_GAP
        }
        val target = if (open) 0f else closedOffset
        val way = abs(abs(translation) - target) / closedOffset
        animator?.cancel()
        animator = ValueAnimator.ofFloat(translation, target).apply {
            duration = max(90L, (260f * way).toLong())
            interpolator = DecelerateInterpolator()
            addUpdateListener { translation = it.animatedValue as Float }
            onFinished { if (open) wobbleNeck() else finishClose() }
            start()
        }
    }

    /** FloatingSidebarLayout liquid wobble: sin(3πt)(1 - t) over 700 ms. */
    private fun wobbleNeck() {
        wobbler = ValueAnimator.ofFloat(0f, 1f).apply {
            duration = 700
            interpolator = LinearInterpolator()
            addUpdateListener { val t = it.animatedValue as Float; wobble = (sin(t * Math.PI * 3) * (1f - t)).toFloat() }
            start()
        }
    }

    private fun finishClose() {
        val done = onClosed
        destroy()
        done?.invoke()
    }

    /** Removes the window at once, without the closed callback (bo/s.OooO0oo). */
    fun destroy() {
        onClosed = null
        animator?.cancel()
        wobbler?.cancel()
        overlay?.remove()
        overlay = null
    }

    /** btnEdit: editing opens All tools too; Done keeps All tools as it is. */
    fun toggleEdit() = setModes(editing = !editing, more = !editing || moreOpen)

    /** btnMore: closing All tools also leaves edit mode. */
    fun toggleMore() = setModes(editing = editing && !moreOpen, more = !moreOpen)

    private fun setModes(editing: Boolean, more: Boolean) {
        this.editing = editing
        moreOpen = more
    }

    /** Edit mode: a sidebar item is removed, an All tools item added; otherwise the tool opens. */
    fun onTool(tool: FloatingTool, inSidebar: Boolean) {
        if (!editing) return launch(tool)
        val next = if (inSidebar) removeSidebarTool(tools, tool) else addSidebarTool(tools, tool)
        if (next == null) {
            val message = if (inSidebar) context.getString(R.string.floating_sidebar_keep_one)
            else context.getString(R.string.floating_sidebar_limit, MAX_SIDEBAR_TOOLS)
            runCatching { Toast.makeText(context.applicationContext, message, Toast.LENGTH_SHORT).show() }
        } else if (next != tools) {
            tools = next
            FloatingButtons.saveSidebarTools(context, next)
        }
    }

    fun openSettings() {
        close()
        start(FloatingButtons.routeIntent(context, Screen.FloatingSetting.route))
    }

    private fun launch(tool: FloatingTool) {
        close()
        scope.launch {
            val junkCleaned = tool == FloatingTool.CLEANER &&
                (context.applicationContext as GalleryApplication).preferences.isJunkCleanedForToday.first()
            start(
                when (tool) {
                    FloatingTool.GALLERY -> Intent(context, MainActivity::class.java)
                        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
                    FloatingTool.CAMERA -> Intent(MediaStore.INTENT_ACTION_STILL_IMAGE_CAMERA).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    else -> FloatingButtons.routeIntent(context, tool.route(junkCleaned)!!)
                }
            )
        }
    }

    /** bo/s.OooOo0o: the target starts 200 ms after the panel begins to close. */
    private fun start(intent: Intent) {
        scope.launch {
            delay(200)
            runCatching { context.startActivity(intent) }
        }
    }

    /** bo/s.OooOOo0: the cards centred on the handle, kept 24 px clear of the status and navigation bars. */
    fun rowTop(screenHeight: Int): Int {
        val low = statusBar + EDGE_GAP
        return (anchorY - rowSize.height.toInt() / 2).coerceIn(low, max(low, screenHeight - navBar - rowSize.height.toInt() - EDGE_GAP))
    }

    /** Where the sidebar card and the All tools card are on screen right now, in px. */
    fun cards(screen: Size, moreGap: Float): Pair<Rect, Rect?> {
        val top = rowTop(screen.height.toInt()).toFloat()
        val end = screen.width - marginEnd + translation
        fun place(fromEnd: Float, size: Size): Rect {
            val r = Rect(end - fromEnd - size.width, top, end - fromEnd, top + size.height)
            return if (left) Rect(screen.width - r.right, r.top, screen.width - r.left, r.bottom) else r
        }
        return place(0f, cardSize) to (if (moreOpen) place(cardSize.width + moreGap, moreSize) else null)
    }
}

@Composable
private fun PanelContent(p: SidebarPanel) {
    val localeDirection = LocalLayoutDirection.current
    val marginEnd = 20.sdp
    val moreGap = 8.sdp
    val panelBg = colorResource(R.color.floating_sidebar_panel_bg)
    val accent = colorResource(R.color.floating_sidebar_accent)
    // The handle's side is the end of the panel: RTL on the left edge; inside, the cards keep the locale's direction.
    // Ripples take the text colour, as ?selectableItemBackground follows the theme
    CompositionLocalProvider(
        LocalLayoutDirection provides if (p.left) LayoutDirection.Rtl else LayoutDirection.Ltr,
        LocalContentColor provides colorResource(R.color.floating_sidebar_text)
    ) {
        Box(
            Modifier
                .fillMaxSize()
                .pointerInput(Unit) { panelGestures(p, moreGap.toPx()) }
                .drawBehind { drawNeck(p, panelBg, accent) }
        ) {
            Row(
                Modifier
                    .layout { measurable, constraints ->
                        val row = measurable.measure(constraints.copy(minWidth = 0, minHeight = 0))
                        layout(constraints.maxWidth, constraints.maxHeight) {
                            row.placeRelative(constraints.maxWidth - marginEnd.roundToPx() - row.width, p.rowTop(constraints.maxHeight))
                        }
                    }
                    .graphicsLayer { translationX = p.translation * if (p.left) -1f else 1f }
                    .onSizeChanged { p.rowSize = Size(it.width.toFloat(), it.height.toFloat()) }
            ) {
                AnimatedVisibility(p.moreOpen, enter = fadeIn(tween(200)), exit = fadeOut(tween(200))) {
                    Row {
                        CompositionLocalProvider(LocalLayoutDirection provides localeDirection) { MoreCard(p) }
                        Spacer(Modifier.width(moreGap))
                    }
                }
                CompositionLocalProvider(LocalLayoutDirection provides localeDirection) { SidebarCard(p) }
            }
        }
    }
}

/** FloatingSidebarLayout touch: a swipe back toward the edge drags the panel out; a tap off the cards closes it. */
private suspend fun PointerInputScope.panelGestures(p: SidebarPanel, moreGap: Float) = awaitEachGesture {
    val down = awaitFirstDown(requireUnconsumed = false, pass = PointerEventPass.Initial)
    val (card, more) = p.cards(Size(size.width.toFloat(), size.height.toFloat()), moreGap)
    val onCard = card.contains(down.position) || more?.contains(down.position) == true
    val tracker = VelocityTracker().apply { addPosition(down.uptimeMillis, down.position) }
    var startX = down.position.x
    var dragging = false
    while (true) {
        val change = awaitPointerEvent(PointerEventPass.Initial).changes.firstOrNull { it.id == down.id } ?: break
        tracker.addPosition(change.uptimeMillis, change.position)
        val outward = (change.position.x - startX) * if (p.left) -1f else 1f
        if (!change.pressed) {
            if (dragging) p.release(-tracker.calculateVelocity().x * if (p.left) -1f else 1f)
            else if (!onCard) p.close()
            break
        }
        if (!dragging && outward > viewConfiguration.touchSlop && outward > abs(change.position.y - down.position.y)) {
            dragging = true
            startX = change.position.x
        } else if (dragging) {
            change.consume()
            p.dragClosed(max(0f, outward))
        }
    }
}

/** FloatingSidebarLayout.dispatchDraw: the liquid neck from the sidebar card to the edge at the handle, with its accent bar. */
private fun DrawScope.drawNeck(p: SidebarPanel, fill: Color, accent: Color) {
    if (p.cardSize.width == 0f) return
    val card = p.cards(size, 0f).first
    val sign = if (p.left) -1f else 1f
    val edge = if (p.left) 0f else size.width
    val x = if (p.left) card.left else card.right
    val gapPx = (edge - x) * sign
    if (gapPx <= 2f * density) return
    val cardHalf = 36f * density
    val edgeHalf = 30f * density
    val waist = (1f + p.wobble * 0.45f) * (11f * density / (gapPx / edgeHalf).coerceIn(0.6f, 2f))
    val guard = 18f * density
    val lo = card.top + guard + cardHalf
    val hi = card.bottom - guard - cardHalf
    val cy = if (lo <= hi) p.anchorY.toFloat().coerceIn(lo, hi) else (card.top + card.bottom) / 2f
    val x0 = x - 6f * density * sign
    val x1 = edge + density * sign
    val mid = x + 0.5f * gapPx * sign
    val curve = 0.2f * gapPx * sign
    val neck = Path().apply {
        moveTo(x0, cy - cardHalf)
        quadraticTo(x + curve, cy - waist, mid, cy - waist)
        quadraticTo(edge - curve, cy - waist, x1, cy - edgeHalf)
        lineTo(x1, cy + edgeHalf)
        quadraticTo(edge - curve, cy + waist, mid, cy + waist)
        quadraticTo(x + curve, cy + waist, x0, cy + cardHalf)
        close()
    }
    drawPath(neck, fill)
    val half = edgeHalf * 0.55f
    val barWidth = 3.5f * density
    val a = edge - 3f * density * sign
    val b = a - barWidth * sign
    drawRoundRect(accent, Offset(min(a, b), cy - half), Size(barWidth, 2f * half), CornerRadius(barWidth, barWidth))
}

@Composable
private fun SidebarCard(p: SidebarPanel) {
    val shape = RoundedCornerShape(24.sdp)
    Column(
        Modifier
            .width(68.sdp)
            .onSizeChanged { p.cardSize = Size(it.width.toFloat(), it.height.toFloat()) }
            .shadow(3.sdp, shape)
            .background(colorResource(R.color.floating_sidebar_panel_bg), shape)
            .animateContentSize(tween(200))
            .padding(horizontal = 3.sdp, vertical = 10.sdp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        p.tools.forEachIndexed { i, tool -> key(tool) { ToolItem(p, tool, inSidebar = true, cascadeIndex = i) } }
        val editLabel = stringResource(if (p.editing) R.string.done else R.string.edit)
        Column(
            Modifier
                .padding(top = 2.sdp)
                .cascade(p, p.tools.size)
                .fillMaxWidth()
                .clickable(remember { MutableInteractionSource() }, ripple()) { p.toggleEdit() }
                .padding(top = 3.sdp, bottom = 5.sdp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            RoundButton(
                if (p.editing) R.drawable.ic_floating_sidebar_done else R.drawable.ic_floating_sidebar_edit,
                colorResource(if (p.editing) R.color.floating_sidebar_accent else R.color.floating_sidebar_collapse_bg)
            )
            ItemLabel(editLabel)
        }
        RoundButton(
            R.drawable.ic_floating_sidebar_collapse,
            colorResource(R.color.floating_sidebar_collapse_bg),
            Modifier
                .padding(top = 4.sdp)
                .cascade(p, p.tools.size + 1)
                .rotate(if (p.moreOpen != p.left) 0f else 180f)
                .clip(CircleShape)
                .clickable(remember { MutableInteractionSource() }, ripple(), onClickLabel = stringResource(R.string.more)) { p.toggleMore() }
        )
    }
}

@Composable
private fun MoreCard(p: SidebarPanel) {
    val shape = RoundedCornerShape(24.sdp)
    Column(
        Modifier
            .width(190.sdp)
            .onSizeChanged { p.moreSize = Size(it.width.toFloat(), it.height.toFloat()) }
            .shadow(6.sdp, shape)
            .background(colorResource(R.color.floating_sidebar_panel_bg), shape)
            .animateContentSize(tween(200))
            .padding(start = 10.sdp, top = 12.sdp, end = 10.sdp, bottom = 10.sdp)
    ) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 4.sdp), verticalAlignment = Alignment.CenterVertically) {
            Text(
                stringResource(R.string.floating_sidebar_all_tools),
                style = GalleryText.BodySmallMedium,
                color = colorResource(R.color.floating_sidebar_text),
                textAlign = TextAlign.Start,
                modifier = Modifier.weight(1f)
            )
            RoundButton(
                R.drawable.home_shell_ic_settings,
                colorResource(R.color.floating_sidebar_collapse_bg),
                Modifier.clip(CircleShape).clickable(remember { MutableInteractionSource() }, ripple(), onClickLabel = stringResource(R.string.quick_access_settings)) { p.openSettings() },
                tint = colorResource(R.color.floating_sidebar_collapse_icon)
            )
        }
        if (p.editing) Text(
            stringResource(R.string.floating_sidebar_edit_hint),
            style = GalleryText.BodyXSmallRegular,
            color = colorResource(R.color.floating_sidebar_subtext),
            textAlign = TextAlign.Start,
            modifier = Modifier.fillMaxWidth().padding(top = 4.sdp).padding(horizontal = 4.sdp)
        )
        Column(Modifier.fillMaxWidth().padding(top = 8.sdp)) {
            FloatingTool.entries.chunked(3).forEach { row ->
                Row(Modifier.fillMaxWidth()) {
                    row.forEach { tool -> Box(Modifier.weight(1f)) { ToolItem(p, tool, inSidebar = false, cascadeIndex = -1) } }
                    repeat(3 - row.size) { Spacer(Modifier.weight(1f)) }
                }
            }
        }
    }
}

/** item_floating_sidebar_action: glow, tile, edit badge and a two-line label; the tile shrinks while pressed. */
@Composable
private fun ToolItem(p: SidebarPanel, tool: FloatingTool, inSidebar: Boolean, cascadeIndex: Int) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val scale by animateFloatAsState(if (pressed) 0.88f else 1f, tween(120, easing = Decelerate), label = "press")
    val inBar = tool in p.tools
    val badge = when {
        !p.editing -> 0
        inSidebar -> R.drawable.ic_floating_sidebar_badge_remove
        inBar -> 0
        else -> R.drawable.ic_floating_sidebar_badge_add
    }
    Column(
        Modifier
            .then(if (cascadeIndex >= 0) Modifier.cascade(p, cascadeIndex) else Modifier)
            .graphicsLayer { alpha = if (p.editing && !inSidebar && inBar) 0.4f else 1f }
            .fillMaxWidth()
            .clickable(interaction, ripple()) { p.onTool(tool, inSidebar) }
            .padding(top = 3.sdp, bottom = 5.sdp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(Modifier.size(46.sdp, 44.sdp)) {
            if (tool != FloatingTool.GALLERY) {
                val glow = Color(tool.tileColor)
                // bg_floating_sidebar_glow tinted with the tile colour, hanging 8 sdp below the tile
                Box(
                    Modifier
                        .align(Alignment.BottomCenter)
                        .offset(y = 8.sdp)
                        .size(56.sdp, 52.sdp)
                        .drawBehind { drawOval(Brush.radialGradient(listOf(glow.copy(alpha = 0.6f), glow.copy(alpha = 0f)), radius = size.minDimension / 2f)) }
                )
            }
            ToolTile(tool, 38.sdp, 9.sdp, Modifier.align(Alignment.BottomCenter).graphicsLayer { scaleX = scale; scaleY = scale })
            if (badge != 0) Image(painterResource(badge), null, Modifier.align(Alignment.TopEnd).size(16.sdp))
        }
        ItemLabel(stringResource(tool.label))
    }
}

/** bo/am: the app logo for Gallery; otherwise a 13 dp tile shaded from the tool colour with a top gloss and a white icon. */
@Composable
internal fun ToolTile(tool: FloatingTool, size: Dp, iconPadding: Dp, modifier: Modifier = Modifier) {
    if (tool == FloatingTool.GALLERY) {
        // The artwork fills the middle 67 % of the drawable; 2.7.2 insets it by -24.6 % so the logo fills the tile
        Image(painterResource(tool.icon), null, modifier.size(size).graphicsLayer { scaleX = 1.4925f; scaleY = 1.4925f })
        return
    }
    val argb = tool.tileColor.toInt()
    val shades = listOf(ColorUtils.blendARGB(argb, android.graphics.Color.WHITE, 0.28f), argb, ColorUtils.blendARGB(argb, android.graphics.Color.BLACK, 0.18f))
        .map { Color(it) }
    Box(
        modifier
            .size(size)
            .drawBehind {
                val corner = CornerRadius(13.dp.toPx())
                drawRoundRect(Brush.linearGradient(shades, Offset.Zero, Offset(this.size.width, this.size.height)), cornerRadius = corner)
                val gloss = Path().apply {
                    addRoundRect(RoundRect(0f, 0f, this@drawBehind.size.width, this@drawBehind.size.height / 2f, corner, corner))
                }
                drawPath(gloss, Brush.verticalGradient(listOf(Color.White.copy(alpha = 64 / 255f), Color.Transparent), 0f, this.size.height / 2f))
            },
        contentAlignment = Alignment.Center
    ) {
        Image(painterResource(tool.icon), null, Modifier.fillMaxSize().padding(iconPadding), colorFilter = ColorFilter.tint(Color.White))
    }
}

@Composable
private fun ItemLabel(text: String) {
    Text(
        text,
        style = GalleryText.BodyXSmallRegular,
        color = colorResource(R.color.floating_sidebar_text),
        textAlign = TextAlign.Center,
        maxLines = 2,
        overflow = TextOverflow.Ellipsis,
        modifier = Modifier.padding(top = 2.sdp)
    )
}

/** FloatingSidebarRoundButton: a 30 sdp circle around a 7 sdp padded icon. */
@Composable
private fun RoundButton(icon: Int, background: Color, modifier: Modifier = Modifier, tint: Color? = null) {
    Image(
        painterResource(icon), null,
        modifier.size(30.sdp).background(background, CircleShape).padding(7.sdp),
        colorFilter = tint?.let { ColorFilter.tint(it) }
    )
}

/** bo/s.OooO0o0: once the panel is half out, items fade in from 26 dp toward the edge, 40 ms apart, overshooting. */
@Composable
private fun Modifier.cascade(p: SidebarPanel, index: Int): Modifier {
    val shift = with(LocalDensity.current) { 26.dp.toPx() } * if (p.left) -1f else 1f
    val appear = remember { Animatable(if (p.cascaded) 1f else 0f) }
    LaunchedEffect(p.cascaded) {
        if (p.cascaded && appear.value < 1f) {
            delay(index * 40L)
            appear.animateTo(1f, tween(380, easing = overshoot(1.6f)))
        }
    }
    return graphicsLayer {
        alpha = appear.value.coerceIn(0f, 1f)
        translationX = (1f - appear.value) * shift
    }
}
