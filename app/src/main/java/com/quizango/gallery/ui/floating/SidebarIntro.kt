package com.quizango.gallery.ui.floating

import android.content.Context
import android.view.ViewGroup
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Easing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animate
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.layout.offset
import com.airbnb.lottie.LottieProperty
import com.airbnb.lottie.compose.LottieAnimation
import com.airbnb.lottie.compose.LottieCompositionSpec
import com.airbnb.lottie.compose.rememberLottieComposition
import com.airbnb.lottie.compose.rememberLottieDynamicProperties
import com.airbnb.lottie.compose.rememberLottieDynamicProperty
import com.quizango.gallery.R
import com.quizango.gallery.ui.theme.GalleryText
import com.quizango.gallery.ui.theme.sdp
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.roundToInt
import kotlin.math.sin

/**
 * bo/ak + FloatingSidebarIntroView (layout_floating_sidebar_intro): the first-run tour over the handle. The user's tools
 * fly into the handle, then "Your quick tools live here" (swipe demo) and "Put it where you like" (long-press and drag
 * demo) run twice each; a tap skips ahead. The replay a week later shows only the first step.
 */
internal class SidebarIntro(private val context: Context) {
    private enum class Phase { FLY, SWIPE, MOVE }

    private var overlay: ComposeOverlay? = null
    private var onFinish: (() -> Unit)? = null
    private var finishing by mutableStateOf(false)
    private var firstPhase = true
    private var replay = false
    private var left = false
    private var screenWidth = 0f
    private var screenHeight = 0f
    private var handleCenterY = 0f
    private var handleAnchorX = 0f
    private var tools = emptyList<FloatingTool>()
    private var captionAtBottom = true
    private var captionMargin = 0

    private var phase by mutableStateOf(Phase.FLY)
    private var rootAlpha by mutableFloatStateOf(1f)
    private var dim by mutableFloatStateOf(0f)
    private var glow by mutableFloatStateOf(0f)
    private var anchorX by mutableFloatStateOf(0f)
    private var anchorY by mutableFloatStateOf(0f)
    private var orbX by mutableFloatStateOf(0f)
    private var orbY by mutableFloatStateOf(0f)
    private var orbAlpha by mutableFloatStateOf(0f)
    private var trailStartX by mutableFloatStateOf(0f)
    private var trailAlpha by mutableFloatStateOf(0f)
    private var press by mutableFloatStateOf(0f)
    private var handFrame by mutableFloatStateOf(HAND_REST)
    private var cardX by mutableStateOf<Float?>(null)
    private var cardSize by mutableStateOf(Size.Zero)
    private var pillY by mutableStateOf<Float?>(null)
    private var pillAlpha by mutableFloatStateOf(0f)
    private var pillScale by mutableFloatStateOf(PILL_START_SCALE)
    private var captionAlpha by mutableFloatStateOf(0f)
    private var captionSwipe by mutableStateOf(true) // swaps only once the old caption has faded out
    private var typed by mutableIntStateOf(0)
    private var descAlpha by mutableFloatStateOf(0f)
    private val flying = mutableStateListOf<Flyer>()

    private class Flyer(val tool: FloatingTool) {
        var x by mutableFloatStateOf(0f)
        var y by mutableFloatStateOf(0f)
        var scale by mutableFloatStateOf(0f)
        var rotation by mutableFloatStateOf(0f)
        var alpha by mutableFloatStateOf(1f)
    }

    private val density = context.resources.displayMetrics.density
    private fun dp(value: Float) = value * density

    /** bo/ak.OooOOOO: adds the full-screen intro window; false when it could not be shown. */
    fun show(
        handleCenterY: Int, left: Boolean, screenWidth: Int, screenHeight: Int, statusBar: Int, navBar: Int,
        tools: List<FloatingTool>, replay: Boolean, onFinish: () -> Unit
    ): Boolean {
        if (overlay != null) return true
        this.replay = replay
        this.left = left
        this.screenWidth = screenWidth.toFloat()
        this.screenHeight = screenHeight.toFloat()
        this.handleCenterY = handleCenterY.toFloat()
        this.tools = tools.take(5)
        this.onFinish = onFinish
        handleAnchorX = dp(ANCHOR_INSET_DP) * inward + if (left) 0f else this.screenWidth
        anchorX = handleAnchorX
        anchorY = this.handleCenterY
        // The caption keeps clear of the handle: under it when the handle is high, above it when low
        captionAtBottom = handleCenterY < screenHeight / 2f
        captionMargin = dp(CAPTION_EDGE_DP).roundToInt() + if (captionAtBottom) navBar else statusBar
        phase = if (replay || this.tools.isEmpty()) Phase.SWIPE else Phase.FLY
        val window = ComposeOverlay(context, overlayParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)) { IntroContent() }
        if (!window.add()) return false
        overlay = window
        return true
    }

    /** +1 toward the screen from the handle's edge. */
    private val inward get() = if (left) 1f else -1f

    /** bo/ak.OooOOoo: where the real handle ended up, in screen px. */
    fun pointAt(center: Pair<Float, Float>) {
        handleAnchorX = center.first
        handleCenterY = center.second
        if (phase == Phase.FLY || orbAlpha == 0f) {
            anchorX = handleAnchorX
            anchorY = handleCenterY
        }
    }

    /** bo/ak.OooO0oo: fade out over 500 ms, then remove; the handle learns at once that the intro is over. */
    fun finish() {
        if (overlay == null || finishing) return
        finishing = true
        val done = onFinish
        onFinish = null
        done?.invoke()
    }

    /** Remove now, no callback (bo/ak.OooO0oO). */
    fun dismiss() {
        onFinish = null
        overlay?.remove()
        overlay = null
    }

    /** A tap, or the end of a step: skip the flight, go from the swipe step to the move step, or finish. */
    private fun advance() {
        when {
            finishing -> Unit
            phase == Phase.FLY -> {
                flying.clear()
                phase = Phase.SWIPE
            }
            phase == Phase.SWIPE && !replay -> phase = Phase.MOVE
            else -> finish()
        }
    }

    @Composable
    private fun IntroContent() {
        val accent = colorResource(R.color.floating_sidebar_accent)
        val white = colorResource(R.color.onlyWhite)
        val ripple by rememberInfiniteTransition(label = "ripple").animateFloat(
            0f, 1f, infiniteRepeatable(tween(RIPPLE_MS, easing = LinearEasing), RepeatMode.Restart), label = "ring"
        )
        LaunchedEffect(Unit) { animate(0f, 1f, animationSpec = tween(350)) { v, _ -> dim = v } }
        LaunchedEffect(phase) { runPhase(phase) }
        LaunchedEffect(finishing) {
            if (finishing) {
                animate(1f, 0f, animationSpec = tween(500, easing = Decelerate)) { v, _ -> rootAlpha = v }
                overlay?.remove()
                overlay = null
            }
        }
        Box(
            Modifier
                .fillMaxSize()
                .graphicsLayer { alpha = rootAlpha }
                .clickable(remember { MutableInteractionSource() }, null) { advance() }
        ) {
            // Dim with a spotlight on the handle, two rings rippling out of it, and the arrival glow
            Canvas(Modifier.fillMaxSize().graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen }) {
                if (dim <= 0f) return@Canvas
                val anchor = Offset(anchorX, anchorY)
                val spot = dp(SPOTLIGHT_DP)
                drawRect(Color.Black.copy(alpha = 190f / 255f * dim))
                drawCircle(
                    Brush.radialGradient(0f to Color.Black, 0.45f to Color.Black, 1f to Color.Transparent, center = anchor, radius = spot),
                    radius = spot, center = anchor, blendMode = BlendMode.DstOut
                )
                repeat(2) { i ->
                    val f = (i / 2f + ripple) % 1f
                    drawCircle(white.copy(alpha = (1f - f) * 150f / 255f * dim), (0.9f * f + 0.35f) * spot, anchor, style = Stroke(dp(1.5f)))
                }
                if (glow > 0f) drawCircle(
                    Brush.radialGradient(listOf(accent, accent.copy(alpha = 0f)), center = anchor, radius = spot * 0.8f),
                    radius = spot * 0.8f, center = anchor, alpha = glow * dim
                )
            }
            PreviewCard()
            flying.forEach { tile ->
                ToolTile(
                    tile.tool, FLY_TILE_DP.dp, (FLY_TILE_DP * TILE_ICON_PADDING).dp,
                    Modifier
                        .offset { IntOffset(tile.x.roundToInt(), tile.y.roundToInt()) }
                        .graphicsLayer { scaleX = tile.scale; scaleY = tile.scale; rotationZ = tile.rotation; alpha = tile.alpha }
                )
            }
            DragPill(accent)
            Hand(white)
            Caption(accent, white)
        }
    }

    @Composable
    private fun PreviewCard() {
        val x = cardX ?: return
        val shape = RoundedCornerShape(18.sdp)
        Column(
            Modifier
                .offset { IntOffset(x.roundToInt(), (handleCenterY - cardSize.height / 2f).roundToInt()) }
                .onSizeChanged { cardSize = Size(it.width.toFloat(), it.height.toFloat()) }
                .shadow(6.sdp, shape)
                .background(colorResource(R.color.floating_sidebar_panel_bg), shape)
                .padding(horizontal = 8.sdp, vertical = 6.sdp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            tools.take(3).forEach { tool ->
                ToolTile(tool, PREVIEW_TILE_DP.dp, (PREVIEW_TILE_DP * TILE_ICON_PADDING).dp, Modifier.padding(vertical = ANCHOR_INSET_DP.dp))
            }
        }
    }

    @Composable
    private fun DragPill(accent: Color) {
        val y = pillY ?: return
        val width = with(LocalDensity.current) { 30.sdp.toPx() }
        val x = if (left) dp(PILL_MARGIN_DP) else screenWidth - width - dp(PILL_MARGIN_DP)
        Box(
            Modifier
                .offset { IntOffset(x.roundToInt(), y.roundToInt()) }
                .graphicsLayer { alpha = pillAlpha; scaleX = pillScale; scaleY = pillScale }
                .size(30.sdp, 76.sdp)
                .background(colorResource(R.color.onlyWhite).copy(alpha = 0.9f), RoundedCornerShape(14.sdp)),
            contentAlignment = Alignment.Center
        ) {
            Image(painterResource(R.drawable.ic_floating_sidebar_drag), null, Modifier.size(16.sdp), colorFilter = ColorFilter.tint(accent))
        }
    }

    /** The demo finger (raw/hand) with its trail and long-press ring, its fingertip on the orb. */
    @Composable
    private fun Hand(white: Color) {
        val composition by rememberLottieComposition(LottieCompositionSpec.RawRes(R.raw.hand))
        // 2.7.2 hides the hand's own tap ripples (TRANSFORM_OPACITY 0 on both shape layers)
        val hidden = rememberLottieDynamicProperties(
            rememberLottieDynamicProperty(LottieProperty.TRANSFORM_OPACITY, 0, "Shape Layer 1"),
            rememberLottieDynamicProperty(LottieProperty.TRANSFORM_OPACITY, 0, "Shape Layer 2")
        )
        if (orbAlpha <= 0f) return
        Canvas(Modifier.fillMaxSize()) {
            val orb = dp(ORB_RADIUS_DP)
            if (trailAlpha > 0f && trailStartX != orbX) drawLine(
                Brush.horizontalGradient(listOf(white.copy(alpha = 0f), white.copy(alpha = 170f / 255f * trailAlpha * orbAlpha)), trailStartX, orbX),
                Offset(trailStartX, orbY), Offset(orbX, orbY), strokeWidth = orb * 1.1f, cap = StrokeCap.Round
            )
            if (press > 0f) {
                val r = orb * 1.6f
                drawArc(white.copy(alpha = orbAlpha), -90f, press * 360f, false, Offset(orbX - r, orbY - r), Size(2 * r, 2 * r), style = Stroke(dp(3f), cap = StrokeCap.Round))
            }
        }
        val size = with(LocalDensity.current) { Size(56.sdp.toPx(), 109.sdp.toPx()) }
        LottieAnimation(
            composition,
            progress = { composition?.getProgressForFrame(handFrame) ?: 0f },
            dynamicProperties = hidden,
            modifier = Modifier
                .offset { IntOffset((orbX - size.width * if (left) 0.47f else 0.53f).roundToInt(), (orbY - size.height * 0.45f).roundToInt()) }
                .size(56.sdp, 109.sdp)
                .graphicsLayer { alpha = orbAlpha; scaleX = if (left) 1f else -1f }
        )
    }

    @Composable
    private fun Caption(accent: Color, white: Color) {
        val swipe = captionSwipe
        val title = stringResource(if (swipe) R.string.floating_intro_swipe_title else R.string.floating_intro_move_title)
        val shadow = Color.Black.copy(alpha = 0.5f)
        Box(Modifier.fillMaxSize(), contentAlignment = if (captionAtBottom) Alignment.BottomCenter else Alignment.TopCenter) {
            Column(
                Modifier
                    .fillMaxWidth()
                    .padding(top = if (captionAtBottom) 0.dp else with(LocalDensity.current) { captionMargin.toDp() })
                    .padding(bottom = if (captionAtBottom) with(LocalDensity.current) { captionMargin.toDp() } else 0.dp)
                    .padding(horizontal = 30.sdp)
                    .graphicsLayer { alpha = captionAlpha },
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    buildAnnotatedString {
                        append(title.take(typed))
                        withStyle(SpanStyle(color = Color.Transparent)) { append(title.drop(typed)) }
                    },
                    style = GalleryText.BodyXLargeSemiBold.copy(shadow = Shadow(shadow, blurRadius = 8f)),
                    color = white,
                    textAlign = TextAlign.Center
                )
                Text(
                    stringResource(if (swipe) R.string.floating_intro_swipe_desc else R.string.floating_intro_move_desc),
                    style = GalleryText.BodyMediumRegular.copy(shadow = Shadow(shadow, blurRadius = 6f)),
                    color = white,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(top = 6.sdp).graphicsLayer { alpha = descAlpha }
                )
                if (!replay) Row(Modifier.padding(top = 14.sdp), horizontalArrangement = Arrangement.spacedBy(4.sdp)) {
                    Dot(swipe, accent)
                    Dot(!swipe, accent)
                }
                Text(
                    stringResource(if (!swipe || replay) R.string.floating_intro_tap_finish else R.string.floating_intro_tap_continue),
                    style = GalleryText.BodyXSmallRegular,
                    color = white,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(top = 10.sdp).graphicsLayer { alpha = 0.6f }
                )
            }
        }
    }

    @Composable
    private fun Dot(active: Boolean, accent: Color) {
        Box(
            Modifier
                .width(if (active) 16.sdp else 5.sdp)
                .height(5.sdp)
                .graphicsLayer { alpha = if (active) 1f else 0.35f }
                .background(accent, RoundedCornerShape(3.sdp))
        )
    }

    /** Each step plays its demo twice (600 ms apart) under its caption, then moves on by itself. */
    private suspend fun runPhase(phase: Phase) = coroutineScope {
        if (firstPhase && phase != Phase.FLY) delay(350) // the replay starts once the dim is in
        firstPhase = false
        if (phase == Phase.FLY) return@coroutineScope fly()
        if (phase == Phase.MOVE) resetDemo()
        launch { showCaption(phase == Phase.SWIPE) }
        repeat(2) {
            if (phase == Phase.SWIPE) swipeDemo() else moveDemo()
            delay(LOOP_PAUSE_MS)
        }
        advance()
    }

    /** The tools pop up in a ring, then each flies into the handle, which glows as it lands. */
    private suspend fun fly() = coroutineScope {
        val size = dp(FLY_TILE_DP)
        val centerX = screenWidth / 2f
        val centerY = screenHeight * FLY_CENTER_Y
        val ring = dp(FLY_RING_DP)
        tools.forEachIndexed { i, tool ->
            val angle = Math.toRadians(i * 360.0 / tools.size - 90.0)
            val tile = Flyer(tool).apply {
                x = centerX + cos(angle).toFloat() * ring - size / 2f
                y = centerY + sin(angle).toFloat() * ring - size / 2f
            }
            flying += tile
            launch {
                delay(POP_DELAY_MS + i * POP_STAGGER_MS)
                animate(0f, 1f, animationSpec = tween(POP_MS, easing = overshoot(2f))) { v, _ -> tile.scale = v }
            }
            launch {
                delay(FLY_START_MS + tools.size * POP_STAGGER_MS + i * FLY_STAGGER_MS)
                val startX = tile.x
                val startY = tile.y
                val endX = handleAnchorX - size / 2f
                val endY = handleCenterY - size / 2f
                val controlX = (startX + endX) / 2f
                val controlY = min(startY, endY) - dp(FLY_ARC_DP)
                animate(0f, 1f, animationSpec = tween(FLY_MS, easing = Easing { it.pow(2.4f) })) { t, _ ->
                    val u = 1f - t
                    tile.x = u * u * startX + 2f * u * t * controlX + t * t * endX
                    tile.y = u * u * startY + 2f * u * t * controlY + t * t * endY
                    tile.scale = 1f - 0.7f * t
                    tile.rotation = 140f * t * -inward
                    tile.alpha = if (t >= 0.75f) u / 0.25f else 1f
                }
                flying -= tile
                animate(0f, 1f, animationSpec = tween(GLOW_MS / 2)) { v, _ -> glow = v }
                animate(1f, 0f, animationSpec = tween(GLOW_MS / 2)) { v, _ -> glow = v }
            }
        }
        delay((tools.size - 1) * FLY_STAGGER_MS + tools.size * POP_STAGGER_MS + FLY_START_MS + 1050)
        advance()
    }

    private fun resetDemo() {
        cardX = null
        pillY = null
        orbAlpha = 0f
        trailAlpha = 0f
        press = 0f
        glow = 0f
        anchorX = handleAnchorX
        anchorY = handleCenterY
    }

    /** bo/ak.OooO0Oo: the caption fades in with its title typed out, 40 ms a letter (at most 1.2 s). */
    private suspend fun showCaption(swipe: Boolean) = coroutineScope {
        if (captionAlpha > 0f) animate(captionAlpha, 0f, animationSpec = tween(200)) { v, _ -> captionAlpha = v }
        captionSwipe = swipe
        val title = context.getString(if (swipe) R.string.floating_intro_swipe_title else R.string.floating_intro_move_title)
        typed = 0
        descAlpha = 0f
        launch { animate(0f, 1f, animationSpec = tween(200)) { v, _ -> captionAlpha = v } }
        launch {
            animate(0f, title.length.toFloat(), animationSpec = tween(min(title.length * 40, 1200), easing = LinearEasing)) { v, _ -> typed = v.toInt() }
            typed = title.length
        }
        launch {
            delay(250)
            animate(0f, 0.85f, animationSpec = tween(300)) { v, _ -> descAlpha = v }
        }
    }

    /** Hand frames 25 rest, 30 pressed, 35 lifted, played at 0.66x. */
    private suspend fun playHand(from: Float, to: Float) {
        animate(from, to, animationSpec = tween(((to - from) / HAND_FPS * 1000f / HAND_SPEED).toInt(), easing = LinearEasing)) { v, _ -> handFrame = v }
    }

    /** Step 1 demo (2.95 s): a finger swipes in from the edge and a preview of the sidebar slides out with it. */
    private suspend fun swipeDemo() = coroutineScope {
        val cardWidth = cardSize.width.takeIf { it > 0f } ?: (dp(PREVIEW_TILE_DP) + 16.sdpPx())
        val margin = dp(PREVIEW_MARGIN_DP)
        val hiddenX = if (left) -cardWidth - margin else screenWidth + margin
        val shownX = if (left) margin else screenWidth - cardWidth - margin
        val startX = dp(ORB_EDGE_DP) * inward + if (left) 0f else screenWidth
        val endX = startX + dp(SWIPE_DP) * inward
        cardX = hiddenX
        orbX = startX
        orbY = handleCenterY
        trailStartX = startX
        trailAlpha = 0f
        press = 0f
        launch { playHand(HAND_REST, HAND_TAP_END) }
        animate(0f, 1f, animationSpec = tween(600, easing = Decelerate)) { v, _ -> orbAlpha = v }
        animate(0f, 1f, animationSpec = tween(1000, easing = Easing { 1f - (1f - it).pow(2.8f) })) { t, _ ->
            orbX = startX + (endX - startX) * t
            trailAlpha = t
            cardX = hiddenX + (shownX - hiddenX) * t
        }
        delay(600)
        animate(1f, 0f, animationSpec = tween(350)) { v, _ -> orbAlpha = v; trailAlpha = v }
        animate(0f, 1f, animationSpec = tween(400, easing = Easing { it * it })) { t, _ -> cardX = shownX + (hiddenX - shownX) * t }
    }

    /** Step 2 demo (4.35 s): a long press on the handle picks up the drag pill, which goes up and down, then lets go. */
    private suspend fun moveDemo() = coroutineScope {
        val pillHeight = 76.sdpPx()
        val pillWidth = 30.sdpPx()
        val pillTop = handleCenterY - pillHeight / 2f
        val pillCenterX = (if (left) dp(PILL_MARGIN_DP) else screenWidth - pillWidth - dp(PILL_MARGIN_DP)) + pillWidth / 2f
        pillY = pillTop
        pillAlpha = 0f
        pillScale = PILL_START_SCALE
        orbX = pillCenterX
        orbY = handleCenterY
        trailAlpha = 0f
        press = 0f
        launch { playHand(HAND_REST, HAND_PRESSED) }
        animate(0f, 1f, animationSpec = tween(600, easing = Decelerate)) { t, _ ->
            orbAlpha = t
            anchorX = handleAnchorX + (pillCenterX - handleAnchorX) * t
        }
        animate(0f, 1f, animationSpec = tween(800, easing = LinearEasing)) { v, _ -> press = v }
        launch { animate(0f, 1f, animationSpec = tween(300)) { v, _ -> pillAlpha = v } }
        launch { animate(PILL_START_SCALE, 1f, animationSpec = tween(300, easing = overshoot(2f))) { v, _ -> pillScale = v } }
        animate(1f, 0f, animationSpec = tween(300, easing = LinearEasing)) { v, _ -> glow = v; press = v }
        val travel = dp(MOVE_DP)
        animate(0f, 1f, animationSpec = tween(2000, easing = Easing { (cos((it + 1f) * Math.PI) / 2.0 + 0.5).toFloat() })) { t, _ ->
            // 0 -> -1 -> 1 -> 0 in three equal legs
            val shift = when {
                t < 1f / 3f -> -3f * t
                t < 2f / 3f -> -1f + 6f * (t - 1f / 3f)
                else -> 1f - 3f * (t - 2f / 3f)
            } * travel
            pillY = pillTop + shift
            orbY = handleCenterY + shift
            anchorY = handleCenterY + shift
        }
        delay(300)
        launch { playHand(HAND_PRESSED, HAND_TAP_END) }
        animate(0f, 1f, animationSpec = tween(350)) { t, _ ->
            orbAlpha = 1f - t
            anchorX = pillCenterX + (handleAnchorX - pillCenterX) * t
            pillAlpha = 1f - t
            pillScale = 1f - (1f - PILL_START_SCALE) * t
        }
    }

    private fun Int.sdpPx(): Float = toFloat() * density * sdpScale

    private val sdpScale = context.resources.getDimension(R.dimen._10sdp) / dp(10f)

    private companion object {
        const val ANCHOR_INSET_DP = 6f
        const val CAPTION_EDGE_DP = 56f
        const val SPOTLIGHT_DP = 70f
        const val ORB_RADIUS_DP = 13f
        const val RIPPLE_MS = 1800
        const val FLY_TILE_DP = 46f
        const val FLY_CENTER_Y = 0.42f
        const val FLY_RING_DP = 78f
        const val FLY_ARC_DP = 90f
        const val FLY_MS = 750
        const val FLY_START_MS = 945L
        const val FLY_STAGGER_MS = 170L
        const val POP_DELAY_MS = 175L
        const val POP_STAGGER_MS = 90L
        const val POP_MS = 320
        const val GLOW_MS = 420
        const val TILE_ICON_PADDING = 0.24f
        const val PREVIEW_TILE_DP = 30f
        const val PREVIEW_MARGIN_DP = 10f
        const val ORB_EDGE_DP = 30f
        const val SWIPE_DP = 160f
        const val PILL_MARGIN_DP = 4f
        const val PILL_START_SCALE = 0.6f
        const val MOVE_DP = 90f
        const val LOOP_PAUSE_MS = 600L
        const val HAND_REST = 25f
        const val HAND_PRESSED = 30f
        const val HAND_TAP_END = 35f
        const val HAND_FPS = 25f
        const val HAND_SPEED = 0.66f
    }
}
