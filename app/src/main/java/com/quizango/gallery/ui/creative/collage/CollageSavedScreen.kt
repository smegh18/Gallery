package com.quizango.gallery.ui.creative.collage

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.quizango.gallery.R
import com.quizango.gallery.ui.theme.sdp

/** llShare targets (CollageSavedActivity): "Other" is the plain chooser, the rest go to that app's share sheet. */
private enum class ShareTarget(@DrawableRes val icon: Int, @StringRes val label: Int, val pkg: String?) {
    OTHER(R.drawable.iv_sr, R.string.other, null),
    FACEBOOK(R.drawable.iv_fb, R.string.facebook, "com.facebook.katana"),
    WHATSAPP(R.drawable.iv_wp, R.string.whatsapp, "com.whatsapp"),
    INSTAGRAM(R.drawable.iv_in, R.string.instagram, "com.instagram.android"),
    X(R.drawable.iv_tw, R.string.x, "com.twitter.android"),
    MESSENGER(R.drawable.iv_ms, R.string.messenger, "com.facebook.orca"),
    SNAPCHAT(R.drawable.iv_sc, R.string.snapchat, "com.snapchat.android"),
    PINTEREST(R.drawable.iv_np, R.string.pinterest, "com.pinterest")
}

/**
 * activity_share_screen (CollageSavedActivity): the saved collage in a 250.sdp frame and the share row. The back arrow,
 * Back and the toolbar tick all leave the flow; there is no way back into the editor (2.7.2 finishes it on save).
 */
@Composable
internal fun CollageSavedScreen(uri: Uri, onBack: () -> Unit) {
    val context = LocalContext.current
    val ink = colorResource(R.color.collage_header_text_color)
    Column(
        Modifier
            .fillMaxSize()
            .background(colorResource(R.color.picker_toolbar_color))
            .statusBarsPadding()
            .navigationBarsPadding()
            .verticalScroll(rememberScrollState())
    ) {
        Row(Modifier.fillMaxWidth().height(56.dp), verticalAlignment = Alignment.CenterVertically) {
            Image(
                painterResource(R.drawable.ic_back),
                contentDescription = stringResource(R.string.viewer_back),
                modifier = Modifier.size(56.dp).clickable(onClick = onBack).padding(16.dp)
            )
            Spacer(Modifier.weight(1f))
            // menu_saved_option's tick (ic_check_collage is drawn white, as in 2.7.2)
            Image(
                painterResource(R.drawable.ic_check_collage),
                contentDescription = stringResource(R.string.save_clg_menu),
                modifier = Modifier.size(48.dp).clickable(onClick = onBack).padding(12.dp)
            )
        }
        AsyncImage(
            model = uri,
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .padding(top = 15.sdp)
                .align(Alignment.CenterHorizontally)
                .size(250.sdp)
                .clip(RoundedCornerShape(7.sdp))
                .border(1.sdp, colorResource(R.color.lineColor), RoundedCornerShape(7.sdp))
        )
        Column(Modifier.fillMaxWidth().padding(7.sdp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                stringResource(R.string.share_image_via_title),
                style = inter(8, FontWeight.W700),
                color = ink,
                modifier = Modifier.padding(9.sdp)
            )
            Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(bottom = 10.dp), horizontalArrangement = Arrangement.Center) {
                ShareTarget.entries.forEach { t ->
                    Column(
                        Modifier.padding(12.sdp).clickable { shareCollage(context, uri, t.pkg) },
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Image(painterResource(t.icon), contentDescription = null, modifier = Modifier.size(38.sdp))
                        Text(stringResource(t.label), style = inter(8), color = ink, modifier = Modifier.padding(top = 7.sdp))
                    }
                }
            }
        }
    }
}

/** ACTION_SEND of the image only (2.7.2 also appends a store link, which we leave out); an uninstalled app gets the empty chooser. */
private fun shareCollage(context: Context, uri: Uri, pkg: String?) {
    try {
        val send = Intent(Intent.ACTION_SEND).apply {
            type = "image/*"
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            pkg?.let { setPackage(it) }
        }
        context.startActivity(Intent.createChooser(send, null))
    } catch (_: Exception) {
    }
}
