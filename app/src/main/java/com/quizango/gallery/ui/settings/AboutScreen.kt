package com.quizango.gallery.ui.settings

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
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
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.quizango.gallery.R
import com.quizango.gallery.ui.browse.SharedMediaToolbar
import com.quizango.gallery.ui.home.drawableBackground
import com.quizango.gallery.ui.theme.GalleryText
import com.quizango.gallery.ui.theme.Inter
import com.quizango.gallery.ui.theme.sdp
import com.quizango.gallery.ui.theme.ssp
import kotlin.math.roundToInt

/**
 * The links AboutActivity opens: common_data.social_media_links of the bundled remote-config defaults
 * (the about_* strings are only its fallback when the config has none).
 */
internal object AboutLinks {
    const val PRIVACY_POLICY = "https://appsspecials.com/bestgallery/privacy"
    const val TERMS = "https://appsspecials.com/bestgallery/terms"
    const val INSTAGRAM = "https://www.instagram.com/galleryphotovault/"
    const val YOUTUBE = "https://www.youtube.com/@appspecials4546"
    const val FACEBOOK = "https://www.facebook.com/photogallerybyappspecials"
    const val PINTEREST = "https://in.pinterest.com/appphotogallery/"
    const val TIKTOK = "https://www.tiktok.com/@photogalleryapp?lang=en"
}

/** AboutActivity hard-codes the version it shows. */
internal const val ABOUT_VERSION_NAME = "2.7.2"
internal const val ABOUT_VERSION_CODE = 152

/** s9/k.Oooo0o0: a browser (custom tab in 2.7.2), else "The browser is not available." */
private fun openLink(context: Context, url: String) {
    runCatching { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url))) }
        .onFailure { Toast.makeText(context, R.string.browser_not_available, Toast.LENGTH_SHORT).show() }
}

/** Gravity.CENTER floors the leftover space; Compose's CenterHorizontally rounds it, putting odd gaps 1px right. */
private val FloorCenter = Alignment.Horizontal { size, space, _ -> (space - size) / 2 }

/** activity_about. */
@Composable
fun AboutScreen(
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val white = colorResource(R.color.onlyWhite)
    val detailStyle = GalleryText.BodySmallMedium.let { style ->
        with(LocalDensity.current) {
            // StaticLayout pitch: Inter hhea ascent/descent (2728/680 of 2816 upm) each rounded, + round(lineSpacingExtra 1sdp)
            val font = style.fontSize.toPx()
            val pitch = (font * 2728f / 2816f).roundToInt() + (font * 680f / 2816f).roundToInt() + 1.sdp.roundToPx()
            style.copy(lineHeight = pitch.toSp())
        }
    }

    Column(Modifier.fillMaxSize().background(colorResource(R.color.backgroundColor))) {
        SharedMediaToolbar(title = stringResource(R.string.about_us), onBack = onNavigateBack)
        Column(
            Modifier
                .fillMaxWidth()
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(top = 3.dp)
                .navigationBarsPadding(),
            horizontalAlignment = FloorCenter
        ) {
            Image(
                painter = painterResource(R.drawable.about_gallery_logo),
                contentDescription = null,
                modifier = Modifier.padding(top = 37.sdp).size(75.sdp)
            )
            Text(
                text = stringResource(R.string.gallery),
                style = TextStyle(fontFamily = Inter, fontWeight = FontWeight.W600, fontSize = 28.ssp),
                color = colorResource(R.color.ic_lock_color),
                modifier = Modifier.padding(top = 14.sdp)
            )
            Text(
                text = stringResource(R.string.current_version, ABOUT_VERSION_NAME, ABOUT_VERSION_CODE),
                style = GalleryText.BodySmallRegular,
                color = colorResource(R.color.about_text),
                modifier = Modifier.padding(top = 6.sdp)
            )
            Text(
                text = stringResource(R.string.about_detail),
                style = detailStyle,
                color = colorResource(R.color.about_text),
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(start = 35.sdp, end = 35.sdp, top = 26.sdp)
            )
            AboutButton(R.string.privacy_policy, Modifier.padding(top = 40.sdp), white) { openLink(context, AboutLinks.PRIVACY_POLICY) }
            AboutButton(R.string.terms_conditions, Modifier.padding(top = 15.sdp), white) { openLink(context, AboutLinks.TERMS) }
            Text(
                text = stringResource(R.string.follow_us_on),
                style = GalleryText.BodySmallMedium,
                color = colorResource(R.color.titleColor),
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 65.sdp)
            )
            Box(
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 60.sdp, vertical = 5.sdp)
                    .height(1.dp)
                    .background(colorResource(R.color.about_divider))
            )
            Row(horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
                SocialLink(R.drawable.ic_about_insta, R.string.instagram, Modifier.padding(end = 10.sdp)) { openLink(context, AboutLinks.INSTAGRAM) }
                SocialLink(R.drawable.ic_about_youttube, R.string.youtube, Modifier.padding(horizontal = 10.sdp)) { openLink(context, AboutLinks.YOUTUBE) }
                SocialLink(R.drawable.ic_about_facebook, R.string.facebook, Modifier.padding(start = 10.sdp)) { openLink(context, AboutLinks.FACEBOOK) }
            }
            Row(Modifier.padding(vertical = 10.sdp), horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
                SocialLink(R.drawable.ic_about_pinterest, R.string.pinterest, Modifier.padding(end = 10.sdp)) { openLink(context, AboutLinks.PINTEREST) }
                SocialLink(R.drawable.ic_about_tiktok, R.string.tiktok, Modifier.padding(start = 10.sdp)) { openLink(context, AboutLinks.TIKTOK) }
            }
        }
    }
}

/** btnPrivacyPolicy / btnTermsConditions: btn_permission pill, BodyLargeSemiBold white, 45.sdp side margins. */
@Composable
private fun AboutButton(@StringRes label: Int, modifier: Modifier, textColor: androidx.compose.ui.graphics.Color, onClick: () -> Unit) {
    Box(
        modifier
            .fillMaxWidth()
            .padding(horizontal = 45.sdp)
            .drawableBackground(R.drawable.btn_permission)
            .clickable(role = Role.Button, onClick = onClick)
            .padding(vertical = 12.sdp),
        contentAlignment = Alignment.Center
    ) {
        Text(stringResource(label), style = GalleryText.BodyLargeSemiBold, color = textColor, textAlign = TextAlign.Center)
    }
}

@Composable
private fun SocialLink(@DrawableRes icon: Int, @StringRes label: Int, modifier: Modifier, onClick: () -> Unit) {
    Column(
        modifier.clickable(remember { MutableInteractionSource() }, ripple(bounded = false), role = Role.Button, onClick = onClick),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Image(painterResource(icon), contentDescription = null)
        Text(
            text = stringResource(label),
            style = GalleryText.BodyXSmallMedium,
            color = colorResource(R.color.titleColor),
            modifier = Modifier.padding(top = 2.sdp)
        )
    }
}
