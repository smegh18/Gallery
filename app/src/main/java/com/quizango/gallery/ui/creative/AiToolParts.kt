package com.quizango.gallery.ui.creative

import androidx.activity.compose.BackHandler
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import com.airbnb.lottie.compose.LottieAnimation
import com.airbnb.lottie.compose.LottieCompositionSpec
import com.airbnb.lottie.compose.LottieConstants
import com.airbnb.lottie.compose.rememberLottieComposition
import com.quizango.gallery.R
import com.quizango.gallery.ui.home.drawableBackground
import com.quizango.gallery.ui.theme.GalleryText
import com.quizango.gallery.ui.theme.sdp

/** The three AI tools, each with its first-run guide (S272 tooltip/view/activity *GuideActivity) and its 2.7.2 flag key. */
internal enum class AiTool(
    val guideKey: String,
    @DrawableRes val hero: Int,
    @DrawableRes val panel: Int?,
    @DrawableRes val icon: Int,
    @StringRes val subtitle: Int,
    val bottomGap: Int
) {
    REMOVE_BG("isRemoveBgGuide", R.drawable.remove_background, R.drawable.remove_bg, R.drawable.ic_remove_background_guide, R.string.instantly_remove_bg, 30),
    BLUR("isBlurBgGuide", R.drawable.blur_background, R.drawable.blur_image_bg, R.drawable.ic_blur_guide, R.string.instantly_blur_background, 15),
    COLOR_SPLASH("isColorSplashGuide", R.drawable.color_splash_background, null, R.drawable.ic_color_splash_guide, R.string.instantly_color_splash, 15)
}

/** RB.onCreate (banner ai_feature): the guide shows until its X or CTA was tapped once. */
internal fun showsGuide(guideSeen: Boolean?) = guideSeen == false

/** blurAndGrayRulerText: the ruler runs 0..25 and its label reads 0..100. */
internal fun rulerLabel(value: Int) = value * 4

/** CS (e9/c): the splash intensity, 1 - (25 - v) * 0.04. */
internal fun splashIntensity(value: Int) = value / 25f

// ColorSplashGuideActivity (~5167): LinearGradient over "Color", code constants with no resource names
private val SPLASH_TITLE_GRADIENT = listOf(
    0xFF00C9BD, 0xFF00C2FF, 0xFF4200FF, 0xFFCC00FF, 0xFFFF00B8,
    0xFFFF0101, 0xFFFF7A00, 0xFFCED137, 0xFF76BE54, 0xFF09B54E
).map { Color(it) }

/**
 * activity_*_guide: hero image with X over the panel (icon + AI badge, title, subtitle, "Try Now").
 * X and the CTA both continue (ai_feature banner); system back cancels the tool.
 */
@Composable
internal fun AiToolGuide(tool: AiTool, onContinue: () -> Unit, onCancel: () -> Unit) {
    BackHandler(onBack = onCancel)
    Column(
        Modifier
            .fillMaxSize()
            .background(colorResource(R.color.backgroundColor))
            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = {})
    ) {
        Box(Modifier.weight(1f).fillMaxWidth()) {
            Image(painterResource(tool.hero), contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
            Image(
                painter = painterResource(R.drawable.ic_close_guide),
                contentDescription = stringResource(R.string.close),
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .statusBarsPadding()
                    .padding(top = 20.sdp, end = 15.sdp)
                    .clickable(interactionSource = remember { MutableInteractionSource() }, indication = ripple(bounded = false), onClick = onContinue)
            )
        }
        Box(Modifier.fillMaxWidth()) {
            tool.panel?.let { Image(painterResource(it), contentDescription = null, contentScale = ContentScale.FillBounds, modifier = Modifier.matchParentSize()) }
            Column(Modifier.fillMaxWidth().navigationBarsPadding(), horizontalAlignment = Alignment.CenterHorizontally) {
                IconWithAiBadge(tool.icon)
                Text(
                    text = when (tool) {
                        AiTool.REMOVE_BG -> buildAnnotatedString { append(stringResource(R.string.remove_bg)) }
                        AiTool.BLUR -> buildAnnotatedString { append(stringResource(R.string.blur_background_guide)) }
                        AiTool.COLOR_SPLASH -> buildAnnotatedString {
                            withStyle(SpanStyle(brush = Brush.horizontalGradient(SPLASH_TITLE_GRADIENT))) { append(stringResource(R.string.color)) }
                            append(" ")
                            append(stringResource(R.string.splash))
                        }
                    },
                    style = if (tool == AiTool.REMOVE_BG) GalleryText.BodyXXLargeBold else GalleryText.BodyXXLargeSemiBold,
                    color = colorResource(R.color.titleColor),
                    modifier = Modifier.padding(top = 8.sdp)
                )
                Text(
                    text = stringResource(tool.subtitle),
                    style = GalleryText.BodyLargeRegular,
                    color = colorResource(R.color.titleColor),
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(vertical = 5.sdp)
                )
                Box(
                    Modifier
                        .padding(start = 15.sdp, end = 15.sdp, top = 15.sdp, bottom = tool.bottomGap.sdp)
                        .fillMaxWidth()
                        .height(38.sdp)
                        .drawableBackground(R.drawable.ic_try_it_free_btn_bg)
                        .clip(RoundedCornerShape(25.sdp))
                        .clickable(onClick = onContinue),
                    contentAlignment = Alignment.Center
                ) {
                    Text(stringResource(R.string.try_now), style = GalleryText.BodyXLargeSemiBold, color = colorResource(R.color.onlyWhite))
                }
            }
        }
    }
}

/** The guide icon centred with the "AI" badge (ic_ai_edit_bg) just past its end, 5sdp higher (RelativeLayout toEndOf). */
@Composable
private fun IconWithAiBadge(@DrawableRes icon: Int) {
    val iconGap = 20.sdp
    val badgeGap = 15.sdp
    Layout(
        content = {
            Image(painterResource(icon), contentDescription = null)
            Box(Modifier.defaultMinSize(16.sdp, 12.sdp).drawableBackground(R.drawable.ic_ai_edit_bg), contentAlignment = Alignment.Center) {
                Text(stringResource(R.string.AI), style = GalleryText.BodyXSmallMedium, color = colorResource(R.color.onlyWhite))
            }
        },
        modifier = Modifier.fillMaxWidth()
    ) { measurables, constraints ->
        val free = constraints.copy(minWidth = 0, minHeight = 0)
        val iconP = measurables[0].measure(free)
        val badgeP = measurables[1].measure(free)
        val iconTop = iconGap.roundToPx()
        val badgeTop = badgeGap.roundToPx()
        layout(constraints.maxWidth, maxOf(iconTop + iconP.height, badgeTop + badgeP.height)) {
            val x = (constraints.maxWidth - iconP.width) / 2
            iconP.place(x, iconTop)
            badgeP.place(x + iconP.width, badgeTop)
        }
    }
}

/** The tools' bottom sheet header (AddBgLay / blurAndGrayLay): X, centred title, done, both tinted ic_lock_color. */
@Composable
internal fun ToolSheetHeader(title: String, onClose: () -> Unit, onDone: () -> Unit, modifier: Modifier = Modifier) {
    Box(modifier.fillMaxWidth().padding(horizontal = 10.sdp)) {
        val tint = ColorFilter.tint(colorResource(R.color.ic_lock_color))
        Image(
            painter = painterResource(R.drawable.ic_cancle),
            contentDescription = stringResource(R.string.cancel),
            colorFilter = tint,
            modifier = Modifier
                .align(Alignment.CenterStart)
                .clickable(interactionSource = remember { MutableInteractionSource() }, indication = ripple(bounded = false), onClick = onClose)
        )
        Text(title, style = GalleryText.BodyLargeMedium, color = colorResource(R.color.titleColor), modifier = Modifier.align(Alignment.Center))
        Image(
            painter = painterResource(R.drawable.ic_done),
            contentDescription = stringResource(R.string.done),
            colorFilter = tint,
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .clickable(interactionSource = remember { MutableInteractionSource() }, indication = ripple(bounded = false), onClick = onDone)
        )
    }
}

/** The bottom sheet surface: `white` (black at night) with the 10sdp elevation shadow. */
@Composable
internal fun ToolSheet(content: @Composable () -> Unit) {
    Column(
        Modifier
            .fillMaxWidth()
            .shadow(10.sdp)
            .background(colorResource(R.color.white))
            .navigationBarsPadding()
    ) { content() }
}

/** `progressBar` / `pbGrayScaleBg`: the indeterminate bar under the toolbar while the subject is segmented. */
@Composable
internal fun SegmentingBar(modifier: Modifier = Modifier) {
    LinearProgressIndicator(
        modifier = modifier.fillMaxWidth().height(4.dp),
        color = colorResource(R.color.dark_blue),
        trackColor = Color.Transparent
    )
}

/** `lottieProgress`: remove_bg_loader, played for 1000 ms after a successful segmentation. */
@Composable
internal fun RemoveBgLoader(size: Int, modifier: Modifier = Modifier) {
    val composition by rememberLottieComposition(LottieCompositionSpec.RawRes(R.raw.remove_bg_loader))
    LottieAnimation(composition, iterations = LottieConstants.IterateForever, modifier = modifier.size(size.sdp))
}

/** `progressLoader` / `progressBar`: full-screen touch-blocking spinner while the result is written. */
@Composable
internal fun SavingLayer() {
    Box(
        Modifier
            .fillMaxSize()
            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = {}),
        contentAlignment = Alignment.Center
    ) {
        CircularProgressIndicator(color = colorResource(R.color.colorPrimary))
    }
}
