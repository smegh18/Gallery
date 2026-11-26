package com.quizango.gallery.ui.settings

import android.content.Context
import android.content.Intent
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLinkStyles
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withLink
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import com.quizango.gallery.R
import com.quizango.gallery.ui.home.drawableBackground
import com.quizango.gallery.ui.theme.Inter
import com.quizango.gallery.ui.theme.isDark
import com.quizango.gallery.ui.theme.sdp
import com.quizango.gallery.ui.theme.ssp
import kotlinx.coroutines.delay

/** paywall_data.paywall_ui of the bundled remote-config defaults. */
internal object PaywallLinks {
    const val PRIVACY_POLICY = "https://appsspecials.com/bestgallery/privacy"
    const val TERMS = "https://appsspecials.com/bestgallery/terms"
}

/** PaywallSliderStaticContent.SLIDE_CONTENT, in order: (title, description). */
internal val PAYWALL_SLIDES = listOf(
    R.string.paywall_slider_ads_title to R.string.paywall_slider_ads_desc,
    R.string.paywall_slider_vault_title to R.string.paywall_slider_vault_desc,
    R.string.paywall_slider_story_title to R.string.paywall_slider_story_desc,
    R.string.paywall_slider_organized_title to R.string.paywall_slider_organized_desc,
    R.string.paywall_slider_duplicate_title to R.string.paywall_slider_duplicate_desc,
    R.string.paywall_slider_recycle_title to R.string.paywall_slider_recycle_desc
)

/** d1/b: the slider moves on every 3.5 s. */
internal const val PAYWALL_SLIDE_MS = 3_500L

private fun online(context: Context): Boolean {
    val cm = context.getSystemService(ConnectivityManager::class.java) ?: return false
    return cm.getNetworkCapabilities(cm.activeNetwork)?.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) == true
}

/**
 * activity_subscription_purchase (PayWallActivity) in the state 2.7.2 reaches when Play billing returns no products
 * (o0ooOOo): heading, feature slider, the no-data block with Refresh, the billing note and the terms line.
 * There is no billing here, so no plan, price or Continue button is ever shown and nothing can be bought.
 */
@Composable
fun ProScreen(onClose: () -> Unit) {
    val context = LocalContext.current
    val dark = androidx.compose.material3.MaterialTheme.colorScheme.isDark
    fun pick(light: Int, night: Int) = if (dark) night else light
    val background = colorResource(pick(R.color.paywall_background_light, R.color.paywall_background_dark))
    val fontDefault = colorResource(pick(R.color.paywall_font_default_light, R.color.paywall_font_default_dark))
    val button = colorResource(pick(R.color.paywall_button_light, R.color.paywall_button_dark))
    var refreshes by remember { mutableIntStateOf(0) }
    val isOnline = remember(refreshes) { online(context) }

    fun openLink(url: String) = runCatching { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url))) }
        .onFailure { Toast.makeText(context, R.string.browser_not_available, Toast.LENGTH_SHORT).show() }

    Column(
        Modifier
            .fillMaxSize()
            .background(background)
            .statusBarsPadding()
            .verticalScroll(rememberScrollState())
            .navigationBarsPadding()
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            // layHeading: the packed chain (gone left diamond, heading, empty right diamond) centred before the close button
            Row(Modifier.weight(1f), horizontalArrangement = Arrangement.Center) {
                Column(Modifier.padding(start = 16.sdp, end = 2.sdp).weight(1f, fill = false)) {
                    Text(
                        text = stringResource(R.string.sub_intro_sub_pro),
                        style = TextStyle(fontFamily = Inter, fontWeight = FontWeight.Bold, fontSize = 20.ssp),
                        color = colorResource(pick(R.color.paywall_title_light, R.color.paywall_title_dark)),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.padding(top = 6.sdp)
                    )
                    Text(
                        text = stringResource(R.string.sub_intro_sub_unlock_premium_feature),
                        style = TextStyle(fontFamily = Inter, fontWeight = FontWeight.Normal, fontSize = 10.ssp),
                        color = colorResource(pick(R.color.paywall_subtitle_light, R.color.paywall_subtitle_dark))
                    )
                }
                Spacer(Modifier.size(28.sdp)) // diamondRight: no diamond image is configured
            }
            Image(
                painter = painterResource(R.drawable.sub_ic_close),
                contentDescription = stringResource(R.string.cancel),
                modifier = Modifier
                    .size(48.sdp)
                    .clickable(remember { MutableInteractionSource() }, ripple(bounded = false), role = Role.Button, onClick = onClose)
                    .padding(12.sdp)
            )
        }
        PaywallSlider(dark, button)
        // layoutNoInternet: shown in place of the plans whenever billing has none to offer
        Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
            Image(
                painter = painterResource(R.drawable.ic_no_internet_available),
                contentDescription = null,
                modifier = Modifier.padding(horizontal = 18.sdp).fillMaxWidth().height(120.sdp)
            )
            Text(
                text = stringResource(if (isOnline) R.string.sub_no_data_found else R.string.sub_network_error),
                style = TextStyle(fontFamily = Inter, fontSize = 12.ssp),
                color = colorResource(pick(R.color.paywall_font_normal_light, R.color.paywall_font_normal_dark))
            )
            Box(
                Modifier
                    .padding(start = 12.sdp, end = 12.sdp, top = 12.sdp, bottom = 8.sdp)
                    .fillMaxWidth()
                    .drawableBackground(R.drawable.sub_btn_transparent, button.toArgb())
                    .clip(RoundedCornerShape(24.sdp))
                    .clickable(role = Role.Button) { refreshes++ }
                    .padding(4.sdp)
                    .height(34.sdp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = stringResource(R.string.refresh).uppercase(LocalConfiguration.current.locales[0]),
                    style = TextStyle(fontFamily = Inter, fontWeight = FontWeight.Bold, fontSize = dpText(14.sdp)),
                    color = colorResource(R.color.lib_white)
                )
            }
        }
        Text(
            text = stringResource(R.string.sub_subscription_note).replace(Regex("\\s+"), " "),
            style = TextStyle(fontFamily = Inter, fontSize = dpText(11.sdp), textAlign = TextAlign.Justify),
            color = fontDefault,
            modifier = Modifier.padding(horizontal = 10.sdp, vertical = 8.sdp).padding(2.sdp)
        )
        val bold = TextLinkStyles(SpanStyle(fontWeight = FontWeight.Bold, textDecoration = TextDecoration.Underline))
        val intro = stringResource(R.string.sub_intro_subscription_text)
        val terms = stringResource(R.string.sub_terms)
        val and = stringResource(R.string.sub_and)
        val privacy = stringResource(R.string.sub_privacy_policy)
        // 2.7.2 appends TERMS straight after the intro, with no space
        val line = buildAnnotatedString {
            append(intro)
            withLink(LinkAnnotation.Clickable("terms", bold) { openLink(PaywallLinks.TERMS) }) { append(terms) }
            append(" $and ")
            withLink(LinkAnnotation.Clickable("privacy", bold) { openLink(PaywallLinks.PRIVACY_POLICY) }) { append(privacy) }
        }
        Row(
            Modifier
                .align(Alignment.CenterHorizontally)
                .padding(start = 12.sdp, end = 12.sdp, top = 8.sdp, bottom = 14.sdp)
                .padding(horizontal = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Image(painterResource(R.drawable.sub_checkbox), contentDescription = null)
            Text(
                text = line,
                style = TextStyle(fontFamily = Inter, fontSize = dpText(9.sdp)),
                color = fontDefault, // wq/b.Oooo0o0 replaces the layout's lib_text_color
                modifier = Modifier.padding(start = 4.sdp)
            )
        }
    }
}

/** Library text sizes given in dp (textSize="@dimen/_Nsdp") ignore the font scale. */
@Composable
private fun dpText(size: Dp): TextUnit = with(LocalDensity.current) { size.toSp() }

/** lay_slider: sub_item_slider pages (remote images in 2.7.2; offline the banner keeps sub_ic_mask_anim) and IndicatorView. */
@Composable
private fun PaywallSlider(dark: Boolean, button: Color) {
    val pager = rememberPagerState { PAYWALL_SLIDES.size }
    LaunchedEffect(pager) {
        while (true) {
            delay(PAYWALL_SLIDE_MS)
            pager.animateScrollToPage((pager.currentPage + 1) % PAYWALL_SLIDES.size)
        }
    }
    val titleColor = colorResource(if (dark) R.color.paywall_slider_title_dark else R.color.paywall_slider_title_light)
    val descColor = colorResource(if (dark) R.color.paywall_slider_desc_dark else R.color.paywall_slider_desc_light)
    Column(Modifier.padding(top = 8.sdp).fillMaxWidth()) {
        HorizontalPager(state = pager, verticalAlignment = Alignment.Top) { page ->
            val (title, desc) = PAYWALL_SLIDES[page]
            Column(Modifier.padding(start = 8.sdp, end = 8.sdp, top = 3.sdp, bottom = 10.sdp)) {
                // sub_ic_mask_anim is a layer-list, which painterResource cannot load
                Box(
                    Modifier
                        .fillMaxWidth()
                        .aspectRatio(3f / 2f)
                        .clip(RoundedCornerShape(8.sdp))
                        .background(colorResource(R.color.lib_white_for_slider))
                        .drawableBackground(R.drawable.sub_ic_mask_anim)
                )
                Text(
                    text = stringResource(title),
                    style = TextStyle(fontFamily = Inter, fontWeight = FontWeight.Bold, fontSize = dpText(15.sdp), textAlign = TextAlign.Center),
                    color = titleColor,
                    modifier = Modifier.fillMaxWidth().padding(top = 12.sdp)
                )
                Text(
                    text = stringResource(desc),
                    style = TextStyle(fontFamily = Inter, fontSize = dpText(11.sdp), textAlign = TextAlign.Center),
                    color = descColor,
                    modifier = Modifier.fillMaxWidth().padding(top = 12.sdp)
                )
            }
        }
        // IndicatorView: 6sdp high, 6sdp dots and a 10sdp selected pill, 4dp apart; button colour and its 100/255 tint
        Row(
            Modifier.align(Alignment.CenterHorizontally).padding(horizontal = 16.sdp, vertical = 5.sdp),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            repeat(PAYWALL_SLIDES.size) { i ->
                val selected = i == pager.currentPage
                Box(
                    Modifier
                        .size(width = if (selected) 10.sdp else 6.sdp, height = 6.sdp)
                        .clip(RoundedCornerShape(3.sdp))
                        .background(if (selected) button else button.copy(alpha = 100 / 255f))
                )
            }
        }
    }
}
