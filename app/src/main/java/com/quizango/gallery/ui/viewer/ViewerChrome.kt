package com.quizango.gallery.ui.viewer

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.ripple
import androidx.compose.runtime.remember
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.platform.LocalLayoutDirection
import com.quizango.gallery.domain.model.MediaItem
import com.quizango.gallery.domain.model.MediaType
import com.quizango.gallery.ui.home.drawableBackground
import com.quizango.gallery.ui.navigation.Screen
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupPositionProvider
import androidx.compose.ui.window.PopupProperties
import com.quizango.gallery.ui.theme.GalleryText
import com.quizango.gallery.ui.theme.sdp
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.quizango.gallery.R

internal const val CHROME_ANIMATION_MS = 150

data class ViewerChromeControls(
    val rotate: Boolean,
    val favorite: Boolean,
    val capture: Boolean,
    val details: Boolean
)

/** Visibility rules from the reference viewer toolbar. */
fun viewerChromeControls(
    isVideo: Boolean,
    isRecycle: Boolean,
    isVault: Boolean,
    isAnimated: Boolean,
    isHeif: Boolean,
    isShortVideoPlaying: Boolean
): ViewerChromeControls = ViewerChromeControls(
    rotate = !isVideo && !isRecycle && !isVault && !isAnimated && !isHeif,
    favorite = !isRecycle && !isVault && !isShortVideoPlaying,
    capture = isShortVideoPlaying,
    details = !isRecycle && !isVault
)

@Composable
fun ViewerTopChrome(
    visible: Boolean,
    controls: ViewerChromeControls,
    isFavorite: Boolean,
    onBack: () -> Unit,
    onRotate: () -> Unit,
    onFavorite: () -> Unit,
    onCapture: () -> Unit,
    onDetails: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(modifier.fillMaxWidth()) {
        if (!visible) {
            Box(
                Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .height(24.dp)
                    .background(Brush.verticalGradient(listOf(Color(0x73000000), Color.Transparent)))
            )
        }
        AnimatedVisibility(
            visible = visible,
            enter = slideInVertically(animationSpec = tween(CHROME_ANIMATION_MS)) { -it } + fadeIn(tween(CHROME_ANIMATION_MS)),
            exit = slideOutVertically(animationSpec = tween(CHROME_ANIMATION_MS)) { -it } + fadeOut(tween(CHROME_ANIMATION_MS)),
            modifier = Modifier.align(Alignment.TopCenter)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .height(56.dp)
                    .padding(horizontal = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                ViewerGlassButton(R.drawable.ic_back, R.string.viewer_back, onBack)
                Spacer(Modifier.weight(1f))
                if (controls.rotate) ViewerGlassButton(R.drawable.ic_media_rotate, R.string.rotate, onRotate)
                if (controls.favorite) ViewerGlassButton(
                    if (isFavorite) R.drawable.ic_fav_fill else R.drawable.ic_favorite,
                    R.string.favorite,
                    onFavorite
                )
                if (controls.capture) ViewerGlassButton(R.drawable.ic_camera_ss_short, R.string.viewer_capture, onCapture)
                if (controls.details) ViewerGlassButton(R.drawable.ic_property, R.string.details, onDetails)
            }
        }
    }
}

@Composable
private fun ViewerGlassButton(@androidx.annotation.DrawableRes icon: Int, @StringRes label: Int, onClick: () -> Unit) {
    IconButton(
        onClick = onClick,
        modifier = Modifier
            .size(36.dp)
            .clip(androidx.compose.foundation.shape.CircleShape)
            .background(Color(0x8C1C1D21))
    ) {
        Icon(
            painter = painterResource(icon),
            contentDescription = stringResource(label),
            tint = Color.Unspecified,
            modifier = Modifier.size(22.dp)
        )
    }
}

/** item_bottom_option_layout slots; the labels stay hidden and only serve TalkBack. */
internal enum class DockAction(@DrawableRes val icon: Int, @StringRes val label: Int) {
    SHARE(R.drawable.ic_share, R.string.share),
    PRIVATE(R.drawable.ic_hide, R.string.private_bottom),
    EDIT(R.drawable.ic_main_edit, R.string.edit),
    ADD_TO(R.drawable.ic_add, R.string.add_to),
    DELETE(R.drawable.ic_delete, R.string.delete),
    MORE(R.drawable.ic_more, R.string.more),
    RESTORE(R.drawable.ic_restore, R.string.restore),
    UNHIDE(R.drawable.ic_unhide, R.string.unhide)
}

/** MF.o0000OOo rows: normal (video swaps Edit for Add to), recyclerBottomAction, and the vault row. */
internal fun dockActions(isRecycle: Boolean, isVault: Boolean, isVideo: Boolean): List<DockAction> = when {
    isRecycle -> listOf(DockAction.RESTORE, DockAction.DELETE)
    isVault -> listOf(DockAction.SHARE, DockAction.UNHIDE, DockAction.DELETE, DockAction.MORE)
    else -> listOf(DockAction.SHARE, DockAction.PRIVATE, if (isVideo) DockAction.ADD_TO else DockAction.EDIT, DockAction.DELETE, DockAction.MORE)
}

/** bg_media_full_pill_dock: the inline-video row (if any) over equal icon-only slots; [moreMenu] is anchored to More. */
@Composable
internal fun ViewerDock(
    actions: List<DockAction>,
    onAction: (DockAction) -> Unit,
    moreMenu: @Composable () -> Unit,
    shortVideoRow: (@Composable () -> Unit)? = null
) {
    val tint = colorResource(R.color.media_full_glass_icon)
    Column(
        Modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(start = 14.dp, end = 14.dp, bottom = 14.dp)
            .shadow(6.dp, RoundedCornerShape(26.dp))
            .drawableBackground(R.drawable.bg_media_full_pill_dock)
            .padding(vertical = 2.dp)
    ) {
        shortVideoRow?.invoke()
        Row(Modifier.fillMaxWidth()) {
        actions.forEach { action ->
            Box(
                Modifier
                    .weight(1f)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = ripple(bounded = false),
                        onClick = { onAction(action) }
                    )
                    .padding(vertical = 12.sdp),
                contentAlignment = Alignment.Center
            ) {
                Icon(painterResource(action.icon), stringResource(action.label), tint = tint)
                if (action == DockAction.MORE) moreMenu()
            }
        }
        }
    }
}

/** item_bottom_option_layout short-video row: mute start, play centre, speed end, all white. */
@Composable
internal fun ShortVideoRow(
    muted: Boolean,
    playing: Boolean,
    speed: Float,
    onToggleMute: () -> Unit,
    onTogglePlay: () -> Unit,
    onSpeed: (Float) -> Unit
) {
    val white = colorResource(R.color.onlyWhite)
    var speedMenu by remember { mutableStateOf(false) }
    val round = Modifier.clip(CircleShape)
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Icon(
            painterResource(if (muted) R.drawable.ic_short_video_mute else R.drawable.ic_short_video_unmute),
            stringResource(if (muted) R.string.unmute else R.string.mute),
            tint = white,
            modifier = Modifier.padding(start = 18.sdp).then(round).clickable(onClick = onToggleMute).padding(12.sdp)
        )
        Spacer(Modifier.weight(1f))
        Icon(
            painterResource(if (playing) R.drawable.ic_short_video_pause else R.drawable.ic_short_video_play),
            stringResource(if (playing) PAUSE_LABEL else PLAY_LABEL),
            tint = white,
            modifier = round.clickable(onClick = onTogglePlay).padding(horizontal = 2.sdp, vertical = 6.sdp)
        )
        Spacer(Modifier.weight(1f))
        Box(Modifier.padding(end = 18.sdp)) {
            Icon(
                painterResource(R.drawable.ic_short_video_speed),
                stringResource(R.string.speed),
                tint = white,
                modifier = round.clickable { speedMenu = true }.padding(12.sdp)
            )
            if (speedMenu) {
                OptionsPopup(
                    options = VIDEO_SPEEDS.map(::speedLabel),
                    selected = VIDEO_SPEEDS.indexOf(speed),
                    position = AnchoredPopup(above = true, end = true),
                    background = colorResource(R.color.dialogBack),
                    textColor = colorResource(R.color.titleColor),
                    selectIcon = R.drawable.ic_short_video_speed_select,
                    onPick = { speedMenu = false; onSpeed(VIDEO_SPEEDS[it]) },
                    onDismiss = { speedMenu = false }
                )
            }
        }
    }
}

/** The reference icons carry no labels; TalkBack reads Media3's translated control descriptions. */
internal val PLAY_LABEL = androidx.media3.ui.R.string.exo_controls_play_description
internal val PAUSE_LABEL = androidx.media3.ui.R.string.exo_controls_pause_description

/** videoSeek + start/end times above the dock while an inline video plays (section 1.8). */
@Composable
internal fun ShortVideoSeekRow(positionMs: Long, durationMs: Long, onSeek: (Long) -> Unit) {
    val progress = colorResource(R.color.shortVideoProgress)
    val time = GalleryText.BodyXSmallMedium.copy(shadow = Shadow(Color.Black, Offset(2f, 2f), 4f))
    Column(Modifier.fillMaxWidth().padding(bottom = 4.sdp)) {
        VideoSeekBar(positionMs, durationMs, progress, onSeek, Modifier.fillMaxWidth().padding(horizontal = 10.sdp))
        Row(
            Modifier.fillMaxWidth().padding(top = 4.sdp).padding(horizontal = 10.sdp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(formatDuration(positionMs), style = time, color = colorResource(R.color.onlyWhite))
            Text(formatDuration(durationMs), style = time, color = colorResource(R.color.onlyWhite))
        }
    }
}

/**
 * AppCompatSeekBar with sb_progress_drawable / sb_thumb, both tinted [color]: a 5.dp track (the drawable's
 * #26 alpha under the tint), the progress in [color] and a 16.dp thumb. Tap or drag seeks; mirrors in RTL.
 */
@Composable
internal fun VideoSeekBar(positionMs: Long, durationMs: Long, color: Color, onSeek: (Long) -> Unit, modifier: Modifier = Modifier) {
    val rtl = LocalLayoutDirection.current == LayoutDirection.Rtl
    val duration by rememberUpdatedState(durationMs)
    val seek by rememberUpdatedState(onSeek)
    val fraction = if (durationMs > 0) (positionMs.toFloat() / durationMs).coerceIn(0f, 1f) else 0f
    fun seekAt(x: Float, width: Int, thumb: Float) {
        val track = width - 2 * thumb
        if (duration <= 0 || track <= 0f) return
        val f = ((x - thumb) / track).coerceIn(0f, 1f)
        seek(((if (rtl) 1f - f else f) * duration).toLong())
    }
    Canvas(
        modifier
            .height(16.dp)
            .pointerInput(rtl) { detectTapGestures { seekAt(it.x, size.width, 8.dp.toPx()) } }
            .pointerInput(rtl) {
                detectHorizontalDragGestures { change, _ ->
                    change.consume()
                    seekAt(change.position.x, size.width, 8.dp.toPx())
                }
            }
    ) {
        val thumb = 8.dp.toPx()
        val y = size.height / 2
        val start = thumb
        val end = size.width - thumb
        val at = if (rtl) end - (end - start) * fraction else start + (end - start) * fraction
        val stroke = 5.dp.toPx()
        drawLine(color.copy(alpha = 0x26 / 255f), Offset(start, y), Offset(end, y), stroke, StrokeCap.Round)
        drawLine(color, Offset(if (rtl) end else start, y), Offset(at, y), stroke, StrokeCap.Round)
        drawCircle(color, thumb, Offset(at, y))
    }
}

/**
 * popmenu_short_video_speed (inline) / popmenu_video_speed (player) card, 100.sdp wide with the [selected] row ticked;
 * the player's subtitle and audio pickers reuse it.
 */
@Composable
internal fun OptionsPopup(
    options: List<String>,
    selected: Int,
    position: PopupPositionProvider,
    background: Color,
    textColor: Color,
    @DrawableRes selectIcon: Int,
    onPick: (Int) -> Unit,
    onDismiss: () -> Unit
) {
    Popup(popupPositionProvider = position, onDismissRequest = onDismiss, properties = PopupProperties(focusable = true)) {
        Column(
            Modifier
                .width(100.sdp)
                .shadow(1.dp, RoundedCornerShape(16.sdp))
                .background(background, RoundedCornerShape(16.sdp))
                .padding(horizontal = 10.sdp, vertical = 8.sdp)
        ) {
            options.forEachIndexed { index, option ->
                Row(
                    Modifier.fillMaxWidth().clickable { onPick(index) }.padding(vertical = 8.sdp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        option,
                        style = GalleryText.BodyMediumMedium,
                        color = textColor,
                        modifier = Modifier.weight(1f).padding(start = 5.sdp)
                    )
                    if (index == selected) Icon(painterResource(selectIcon), null, tint = Color.Unspecified)
                }
            }
        }
    }
}

internal const val SUGGESTION_DELAY_MS = 1_500L

internal val MediaItem.isAnimatedMedia: Boolean get() = isAnimated || mediaType == MediaType.ANIMATED

/** suggestionRunnable runs for still images only, never in the Recycle Bin or the vault. */
internal fun suggestionsEligible(item: MediaItem, isRecycle: Boolean, isVault: Boolean): Boolean =
    !item.isVideo && !item.isAnimatedMedia && !isRecycle && !isVault

/** item_suggestion_layout pills in reference order (Copy text stays hidden: enable_copy_text_feature is false). */
internal enum class Suggestion(@DrawableRes val icon: Int, @StringRes val label: Int) {
    REMOVE_BG(R.drawable.ic_edit_small_remove_bg, R.string.remove_background),
    BLUR_BG(R.drawable.ic_edit_small_blur_bg, R.string.blur_background_guide),
    COLOR_SPLASH(R.drawable.ic_edit_small_color_splash, R.string.color_splash),
    CROP(R.drawable.ic_edit_small_crop, R.string.crop),
    MARKUP(R.drawable.ic_edit_small_draw, R.string.markup),
    ADJUST(R.drawable.ic_edit_small_adjust, R.string.adjust),
    FILTER(R.drawable.ic_edit_small_filter, R.string.filter)
}

internal fun suggestionRoute(suggestion: Suggestion, path: String): String = when (suggestion) {
    Suggestion.REMOVE_BG -> Screen.RemoveBg.createRoute(path)
    Suggestion.BLUR_BG -> Screen.BlurBg.createRoute(path)
    Suggestion.COLOR_SPLASH -> Screen.ColorSplash.createRoute(path)
    Suggestion.CROP -> Screen.Editor.createRoute(path, initialTab = "crop")
    Suggestion.MARKUP -> Screen.Editor.createRoute(path, initialTab = "brush")
    Suggestion.ADJUST -> Screen.Editor.createRoute(path, initialTab = "adjust")
    Suggestion.FILTER -> Screen.Editor.createRoute(path, initialTab = "filter")
}

/** Horizontally scrolling edit_opt_back pills, the close pill first, 10.sdp above the dock. */
@Composable
internal fun SuggestionStrip(onClose: () -> Unit, onPick: (Suggestion) -> Unit, modifier: Modifier = Modifier) {
    val white = colorResource(R.color.onlyWhite)
    val pill = RoundedCornerShape(4.sdp)
    Row(
        modifier
            .fillMaxWidth()
            .padding(bottom = 10.sdp)
            .horizontalScroll(rememberScrollState())
            .padding(5.sdp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            Modifier
                .padding(horizontal = 5.sdp)
                .clip(pill)
                .drawableBackground(R.drawable.edit_opt_back)
                .clickable(onClick = onClose)
                .padding(3.sdp)
        ) {
            Icon(painterResource(R.drawable.ic_close), stringResource(R.string.close), tint = white, modifier = Modifier.size(12.sdp))
        }
        Suggestion.entries.forEach { suggestion ->
            Row(
                Modifier
                    .padding(horizontal = 5.sdp)
                    .clip(pill)
                    .drawableBackground(R.drawable.edit_opt_back)
                    .clickable { onPick(suggestion) }
                    .padding(horizontal = 4.sdp, vertical = 3.sdp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(painterResource(suggestion.icon), null, tint = white, modifier = Modifier.size(13.sdp))
                Text(
                    stringResource(suggestion.label),
                    style = GalleryText.BodySmallMedium,
                    color = white,
                    maxLines = 1,
                    modifier = Modifier.padding(start = 3.sdp)
                )
            }
        }
    }
}

/** MF slide show: slideShowInterval starts at 2 s (slideDuration = interval × 1000). */
internal const val SLIDESHOW_DEFAULT_SECONDS = 2

/** Tapping the duration text: +1 s up to 5, then back to 1 (MF `if-ge 0x5`). */
internal fun nextSlideSeconds(seconds: Int): Int = if (seconds < 5) seconds + 1 else 1

/** The page the slide show turns to, or null on the last page, where it stops. */
internal fun nextSlidePage(current: Int, lastIndex: Int): Int? = (current + 1).takeIf { it <= lastIndex }

/** Recycle Bin and vault pages never start a slide show. */
internal fun slideshowAllowed(isRecycle: Boolean, isVault: Boolean): Boolean = !isRecycle && !isVault

/**
 * slideControl: a touch layer over the pager (no swiping or chrome taps while it runs) with three 28.sdp
 * ic_slide_control_back circles, 40.sdp in: Close bottom-end, Volume top-end, the "2s" duration bottom-start.
 */
@Composable
internal fun SlideshowControls(seconds: Int, muted: Boolean, onClose: () -> Unit, onToggleMute: () -> Unit, onCycleSeconds: () -> Unit) {
    val white = colorResource(R.color.onlyWhite)
    Box(
        Modifier
            .fillMaxSize()
            .pointerInput(Unit) { detectTapGestures { } }
            .padding(40.sdp)
    ) {
        SlideButton(Modifier.align(Alignment.TopEnd), onToggleMute) {
            Icon(
                painterResource(if (muted) R.drawable.ic_slide_mute else R.drawable.ic_slide_unmute),
                stringResource(if (muted) R.string.unmute else R.string.mute),
                tint = Color.Unspecified,
                modifier = Modifier.fillMaxSize().padding(horizontal = 4.sdp)
            )
        }
        SlideButton(Modifier.align(Alignment.BottomStart), onCycleSeconds) {
            Text("${seconds}s", style = GalleryText.BodyMediumMedium, color = white)
        }
        SlideButton(Modifier.align(Alignment.BottomEnd), onClose) {
            Icon(painterResource(R.drawable.ic_story_close), stringResource(R.string.close), tint = Color.Unspecified, modifier = Modifier.fillMaxSize().padding(6.sdp))
        }
    }
}

@Composable
private fun SlideButton(modifier: Modifier, onClick: () -> Unit, content: @Composable () -> Unit) {
    Box(
        modifier
            .size(28.sdp)
            .clip(CircleShape)
            .drawableBackground(R.drawable.ic_slide_control_back)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) { content() }
}

/** popmenu_media_more rows in reference order. */
internal enum class MoreAction(@DrawableRes val icon: Int, @StringRes val label: Int) {
    ADD_TO(R.drawable.ic_add, R.string.add_to),
    RENAME(R.drawable.ic_rename, R.string.rename),
    SLIDE_SHOW(R.drawable.ic_slideshow, R.string.slide_show),
    WALLPAPER(R.drawable.ic_set_as, R.string.set_as_wallpaper),
    PRINT(R.drawable.ic_print, R.string.print),
    SHOW_ON_MAP(R.drawable.ic_map, R.string.show_on_map)
}

/**
 * MF More popup per type: normal shows everything, Show on map only with a place; the vault (type 2) hides Add to and
 * Show on map. Slide show stays out of the vault, and Wallpaper / Print need a still image to decode.
 */
internal fun moreActions(isVault: Boolean, isVideo: Boolean, hasPlace: Boolean): List<MoreAction> = buildList {
    if (!isVault) add(MoreAction.ADD_TO)
    add(MoreAction.RENAME)
    if (!isVault) add(MoreAction.SLIDE_SHOW)
    if (!isVideo) {
        add(MoreAction.WALLPAPER)
        add(MoreAction.PRINT)
    }
    if (!isVault && hasPlace) add(MoreAction.SHOW_ON_MAP)
}

/** Places a popup above or below its anchor, aligned to the anchor's start or end edge (mirrored in RTL). */
internal class AnchoredPopup(private val above: Boolean, private val end: Boolean) : PopupPositionProvider {
    override fun calculatePosition(
        anchorBounds: IntRect,
        windowSize: IntSize,
        layoutDirection: LayoutDirection,
        popupContentSize: IntSize
    ): IntOffset {
        val alignRight = end == (layoutDirection == LayoutDirection.Ltr)
        val x = if (alignRight) anchorBounds.right - popupContentSize.width else anchorBounds.left
        return IntOffset(x, if (above) anchorBounds.top - popupContentSize.height else anchorBounds.bottom)
    }
}

/** popmenu_media_more: dialogBack card, radius 16.sdp, 160.sdp wide, anchored above the dock it is placed in. */
@Composable
internal fun ViewerMoreCard(actions: List<MoreAction>, onAction: (MoreAction) -> Unit, onDismiss: () -> Unit) {
    Popup(popupPositionProvider = AnchoredPopup(above = true, end = true), onDismissRequest = onDismiss, properties = PopupProperties(focusable = true)) {
        Column(
            Modifier
                .width(160.sdp)
                .shadow(1.dp, RoundedCornerShape(16.sdp))
                .background(colorResource(R.color.dialogBack), RoundedCornerShape(16.sdp))
                .padding(horizontal = 8.sdp, vertical = 5.sdp)
        ) {
            actions.forEach { action ->
                Row(
                    Modifier
                        .fillMaxWidth()
                        .clickable { onAction(action) }
                        .padding(vertical = 5.sdp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(painterResource(action.icon), null, tint = Color.Unspecified, modifier = Modifier.padding(5.sdp))
                    Text(
                        stringResource(action.label),
                        style = GalleryText.BodySmallMedium,
                        color = colorResource(R.color.titleColor),
                        maxLines = 1,
                        modifier = Modifier.padding(start = 5.sdp)
                    )
                }
            }
        }
    }
}
