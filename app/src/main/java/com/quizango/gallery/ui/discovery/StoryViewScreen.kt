package com.quizango.gallery.ui.discovery

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Easing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.changedToUp
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties
import coil.compose.AsyncImage
import com.quizango.gallery.R
import com.quizango.gallery.domain.model.MediaItem
import com.quizango.gallery.ui.components.UsageAccessKind
import com.quizango.gallery.ui.components.UsageAccessQueue
import com.quizango.gallery.ui.home.drawableBackground
import com.quizango.gallery.ui.home.shareSelectedMedia
import com.quizango.gallery.ui.theme.GalleryText
import com.quizango.gallery.ui.theme.sdp
import java.io.File
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.max

/** The Albums tab's story row, so the viewer plays the same photos and a finished story opens the next one. */
object StoryQueue {
    var stories: List<Story> = emptyList()

    fun find(id: String): Story? = stories.firstOrNull { it.id == id }

    fun next(id: String): Story? {
        val i = stories.indexOfFirst { it.id == id }
        return if (i < 0) null else stories.getOrNull(i + 1)
    }
}

/** progressSingleDisplayedTime 3.0 in activity_story_view_photo. */
const val STORY_STEP_MS = 3000

/** story.ad_position: an ad step after every 3rd photo. */
const val STORY_AD_EVERY = 3

/** 2.7.2 StoryViewPhotoActivity.ooOO: ad (null) after every 3rd photo, so also after the last one of a multiple of 3. */
fun storySteps(items: List<MediaItem>, withAds: Boolean): List<MediaItem?> {
    if (!withAds) return items
    val steps = ArrayList<MediaItem?>()
    items.forEachIndexed { i, item ->
        steps.add(item)
        if ((i + 1) % STORY_AD_EVERY == 0) steps.add(null)
    }
    return steps
}

enum class StoryTouch { PREVIOUS, NEXT, RESUME }

/** StoryViewPhotoActivity.OoooOO0: a hold of 500 ms or a move of 100 px resumes; otherwise the half under the finger (mirrored in RTL). */
fun storyTouch(heldMs: Long, movedPx: Float, x: Float, width: Float, rtl: Boolean): StoryTouch {
    if (heldMs >= 500 || movedPx >= 100f) return StoryTouch.RESUME
    val left = x < width / 2
    return if (left != rtl) StoryTouch.PREVIOUS else StoryTouch.NEXT
}

/** Ken Burns alternates per photo (ad steps do not count): zoom_in.xml first, then zoom_out.xml. */
fun kenBurnsZoomIn(photoOrdinal: Int): Boolean = photoOrdinal % 2 == 0

/** Android's AccelerateDecelerateInterpolator, which the zoom animators use. */
private val AccelerateDecelerate = Easing { t -> (cos((t + 1) * PI) / 2 + 0.5).toFloat() }

/** 2.7.2 queues ta/d.STORY on the viewer's Back; Home tries that prompt on its next resume. */
internal fun recordStoryUsageAccessTrigger() = UsageAccessQueue.queueForHome(UsageAccessKind.STORY)

/** Reference story title: the photo's date, e.g. "04 Nov 2026". */
private fun storyDate(item: MediaItem): String {
    val millis = if (item.dateTaken > 0) item.dateTaken else item.dateModified
    return java.text.SimpleDateFormat("dd MMM yyyy", java.util.Locale.getDefault()).format(java.util.Date(millis))
}

@Composable
fun StoryViewScreen(
    storyTitle: String,
    mediaItems: List<MediaItem>,
    onEditClick: ((String) -> Unit)? = null,
    onNavigateBack: () -> Unit,
    // Reference: after the last photo, play the next story in the row
    onFinished: () -> Unit = onNavigateBack,
    // True only for Albums-row stories: they get ad steps and the usage-access hook (the viewer slideshow and the vault do not)
    fromStoryRow: Boolean = false,
    editTipSeen: Boolean = true,
    onEditTipSeen: () -> Unit = {}
) {
    val context = LocalContext.current
    val shareTitle = stringResource(R.string.share)
    val close = {
        if (fromStoryRow) recordStoryUsageAccessTrigger()
        onNavigateBack()
    }

    if (mediaItems.isEmpty()) {
        Box(Modifier.fillMaxSize().background(Color.Black)) {
            StoryHeader(title = stringResource(R.string.stories), onClose = close, onShare = {})
        }
        val latest by rememberUpdatedState(mediaItems) // items may still be loading
        LaunchedEffect(Unit) {
            kotlinx.coroutines.delay(800)
            if (latest.isEmpty()) onNavigateBack()
        }
        return
    }

    val steps = remember(mediaItems, fromStoryRow) { storySteps(mediaItems, fromStoryRow) }
    var stepIndex by remember { mutableIntStateOf(0) }
    var restart by remember { mutableIntStateOf(0) }
    var touching by remember { mutableStateOf(false) }
    var tipDismissed by remember { mutableStateOf(false) }
    val rtl = LocalLayoutDirection.current == LayoutDirection.Rtl

    val index = stepIndex.coerceIn(0, steps.lastIndex)
    val step = steps[index]
    val tipVisible = onEditClick != null && !editTipSeen && !tipDismissed && step != null
    val paused = touching || tipVisible
    val progress = remember(index, restart) { Animatable(0f) }

    val goNext = { if (index < steps.lastIndex) stepIndex = index + 1 else onFinished() }
    val goPrevious = { if (index > 0) stepIndex = index - 1 else restart++ }

    LaunchedEffect(index, restart, paused) {
        if (!paused) {
            val ms = ((1f - progress.value) * STORY_STEP_MS).toInt().coerceAtLeast(1)
            progress.animateTo(1f, tween(ms, easing = LinearEasing))
            goNext()
        }
    }

    // 2.7.2 Oooo0oO: leaving on an ad step only finishes, it queues no STORY prompt
    BackHandler {
        if (fromStoryRow && step != null) recordStoryUsageAccessTrigger()
        onNavigateBack()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .pointerInput(rtl, index) {
                // Press pauses at once; the release decides: tap on a half, or resume after a hold or drag.
                awaitEachGesture {
                    val down = awaitFirstDown()
                    touching = true
                    var moved = 0f
                    var action = StoryTouch.RESUME
                    while (true) {
                        val change = awaitPointerEvent().changes.firstOrNull { it.id == down.id } ?: break
                        moved = max(moved, (change.position - down.position).getDistance())
                        if (change.changedToUp()) {
                            action = storyTouch(change.uptimeMillis - down.uptimeMillis, moved, change.position.x, size.width.toFloat(), rtl)
                            break
                        }
                        if (!change.pressed) break
                    }
                    touching = false
                    when (action) {
                        StoryTouch.PREVIOUS -> goPrevious()
                        StoryTouch.NEXT -> goNext()
                        StoryTouch.RESUME -> {}
                    }
                }
            }
    ) {
        if (step != null) {
            val zoomIn = kenBurnsZoomIn(steps.take(index).count { it != null })
            AsyncImage(
                model = File(step.filePath),
                contentDescription = null,
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer {
                        // zoom_in 1.0 -> 1.1, zoom_out 1.1 -> 1.0, tied to the paused progress
                        val e = AccelerateDecelerate.transform(progress.value)
                        val s = if (zoomIn) 1f + 0.1f * e else 1.1f - 0.1f * e
                        scaleX = s
                        scaleY = s
                        alpha = 0.9f
                    },
                contentScale = ContentScale.Fit
            )
        } else {
            StoryAdSkeleton(Modifier.align(Alignment.Center))
        }

        Box(Modifier.fillMaxSize().statusBarsPadding()) {
            StoryProgressBar(steps.size, index, rtl, { progress.value }, Modifier.padding(top = 8.dp))
            StoryHeader(
                title = step?.let { storyDate(it) }.orEmpty(),
                onClose = {
                    if (fromStoryRow && step != null) recordStoryUsageAccessTrigger()
                    onNavigateBack()
                },
                onShare = { step?.let { shareSelectedMedia(context, setOf(it.filePath), shareTitle) } },
                modifier = Modifier.padding(top = 20.sdp)
            )
        }

        // Reference Edit pill (rlStoriesEdit): 30.sdp above the bottom, photo steps only
        if (onEditClick != null && step != null) {
            Row(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .navigationBarsPadding()
                    .padding(bottom = 30.sdp)
                    .height(32.sdp)
                    .widthIn(min = 80.sdp)
                    .clip(RoundedCornerShape(6.dp))
                    .drawableBackground(R.drawable.story_edit_bg)
                    .clickable { onEditClick(step.filePath) }
                    .padding(start = 14.sdp, end = 16.sdp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Icon(painter = painterResource(R.drawable.ic_edit), contentDescription = null, tint = Color.Unspecified)
                Text(
                    text = stringResource(R.string.edit),
                    style = GalleryText.BodySmallSemiBold,
                    color = colorResource(R.color.onlyWhite),
                    maxLines = 1,
                    modifier = Modifier.padding(start = 6.sdp)
                )
            }
        }

        if (tipVisible && step != null && onEditClick != null) {
            val bottomInset = WindowInsets.navigationBars.getBottom(LocalDensity.current)
            val pillMargin = with(LocalDensity.current) { 30.sdp.roundToPx() }
            val dismiss = { tipDismissed = true; onEditTipSeen() }
            // custom_toast_stories_edit_layout: bubble with OK, and a hit area over the pill below it
            Popup(
                alignment = Alignment.BottomCenter,
                offset = IntOffset(0, -(bottomInset + pillMargin)),
                onDismissRequest = dismiss,
                properties = PopupProperties(focusable = true)
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.sdp).drawableBackground(R.drawable.popup_stories_edit_bg),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = stringResource(R.string.edit_your_photo),
                            style = GalleryText.BodyLargeSemiBold,
                            color = colorResource(R.color.onlyWhite),
                            textAlign = TextAlign.Center,
                            modifier = Modifier.weight(1f, fill = false).padding(start = 5.sdp, top = 3.sdp, bottom = 8.sdp)
                        )
                        Text(
                            text = stringResource(R.string.ok),
                            style = GalleryText.BodyMediumSemiBold,
                            color = colorResource(R.color.cropIconColor),
                            textAlign = TextAlign.Center,
                            modifier = Modifier
                                .padding(start = 8.sdp, end = 8.sdp, bottom = 5.sdp)
                                .clip(RoundedCornerShape(18.dp))
                                .drawableBackground(R.drawable.btn_stories_edit_bg)
                                .clickable(onClick = dismiss)
                                .padding(horizontal = 15.sdp, vertical = 5.sdp)
                        )
                    }
                    Box(
                        Modifier
                            .padding(top = 5.sdp)
                            .size(80.sdp, 32.sdp)
                            .clickable(interactionSource = null, indication = ripple(bounded = false)) {
                                dismiss()
                                onEditClick(step.filePath)
                            }
                    )
                }
            }
        }
    }
}

/** MultiProgressBar: 3.dp segments 8.dp apart, white fill on a grayTitle track. */
@Composable
private fun StoryProgressBar(count: Int, current: Int, rtl: Boolean, currentProgress: () -> Float, modifier: Modifier = Modifier) {
    val track = colorResource(R.color.grayTitle)
    val fill = colorResource(R.color.onlyWhite)
    Row(modifier.fillMaxWidth().padding(horizontal = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        for (i in 0 until count) {
            Box(
                Modifier
                    .weight(1f)
                    .height(3.dp)
                    .clip(RoundedCornerShape(1.5.dp))
                    .background(track)
                    .drawBehind {
                        val p = when {
                            i < current -> 1f
                            i == current -> currentProgress()
                            else -> 0f
                        }
                        val w = size.width * p
                        drawRect(fill, Offset(if (rtl) size.width - w else 0f, 0f), Size(w, size.height))
                    }
            )
        }
    }
}

/** rlLayout: close, the photo date and share, 28.sdp high. */
@Composable
private fun StoryHeader(title: String, onClose: () -> Unit, onShare: () -> Unit, modifier: Modifier = Modifier) {
    Row(modifier.fillMaxWidth().height(28.sdp), verticalAlignment = Alignment.CenterVertically) {
        Icon(
            painter = painterResource(R.drawable.ic_story_close),
            contentDescription = stringResource(R.string.close),
            tint = Color.Unspecified,
            modifier = Modifier
                .padding(start = 10.sdp)
                .clickable(interactionSource = null, indication = ripple(bounded = false), onClick = onClose)
                .padding(5.sdp)
        )
        Text(
            text = title,
            style = GalleryText.BodySmallSemiBold.copy(shadow = Shadow(Color(0x80000000), Offset(2f, 2f), 4f)),
            color = colorResource(R.color.onlyWhite),
            maxLines = 1,
            modifier = Modifier.padding(start = 16.sdp).weight(1f)
        )
        Icon(
            painter = painterResource(R.drawable.ic_share_story),
            contentDescription = stringResource(R.string.share),
            tint = Color.Unspecified,
            modifier = Modifier
                .padding(end = 10.sdp)
                .clickable(interactionSource = null, indication = ripple(bounded = false), onClick = onShare)
        )
    }
}

/** Static story ad skeleton (gnt_story_template_view shimmer layout): no animation, not clickable (D5 placeholder). */
@Composable
private fun StoryAdSkeleton(modifier: Modifier = Modifier) {
    val dark = colorResource(R.color.shimmerColorDark)
    val medium = colorResource(R.color.shimmerColorMedium)
    Box(modifier.fillMaxWidth()) {
        Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
            Spacer(Modifier.size(65.sdp).background(dark))
            // one text line tall: 20.ssp / 15.ssp at about 1.2 line height
            Spacer(Modifier.padding(top = 15.sdp).width(200.sdp).height(24.sdp).background(dark))
            Spacer(Modifier.padding(top = 15.sdp, start = 10.sdp, end = 10.sdp).fillMaxWidth().height(18.sdp).background(medium))
            Spacer(Modifier.padding(top = 15.sdp).fillMaxWidth().height(150.sdp).background(medium))
            Spacer(
                Modifier
                    .padding(start = 15.sdp, end = 15.sdp, top = 25.sdp, bottom = 5.sdp)
                    .fillMaxWidth()
                    .height(37.sdp)
                    .background(colorResource(R.color.shimmerColorCTAButton), RoundedCornerShape(4.sdp))
            )
        }
        Box(
            Modifier
                .align(Alignment.TopEnd)
                .drawableBackground(R.drawable.ad_icon_bg_stroke_1)
                .padding(horizontal = 5.dp, vertical = 2.dp)
        )
    }
}
