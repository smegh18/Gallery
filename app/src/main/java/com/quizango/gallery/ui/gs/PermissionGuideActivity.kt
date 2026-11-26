package com.quizango.gallery.ui.gs

import android.app.Activity
import android.content.Intent
import android.graphics.Color as AndroidColor
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.provider.Settings
import android.view.Gravity
import android.view.ViewGroup
import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.window.DialogWindowProvider
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import com.airbnb.lottie.compose.LottieAnimation
import com.airbnb.lottie.compose.LottieCompositionSpec
import com.airbnb.lottie.compose.LottieConstants
import com.airbnb.lottie.compose.rememberLottieComposition
import com.quizango.gallery.R
import com.quizango.gallery.ui.theme.GalleryText
import com.quizango.gallery.ui.theme.sdp

/**
 * 2.7.2 GuideActivity: a see-through screen over the system settings page with a bottom card that shows how to flip the
 * switch (dialog_overlay_guide_anime for Display over other apps, dialog_gs_permission_guide_anime for Phone). Any touch
 * or 6 s closes it.
 */
class PermissionGuideActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // 2.7.2 sets FLAG_FULLSCREEN: the status bar hides while the guide shows
        WindowCompat.getInsetsController(window, window.decorView).hide(WindowInsetsCompat.Type.statusBars())
        val type = intent.getIntExtra(EXTRA_TYPE, GUIDE_OVERLAY)
        setContent {
            Dialog(onDismissRequest = ::finish, properties = DialogProperties(usePlatformDefaultWidth = false)) {
                val window = (LocalView.current.parent as? DialogWindowProvider)?.window
                SideEffect {
                    window?.run {
                        setGravity(Gravity.BOTTOM)
                        setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
                        setBackgroundDrawable(ColorDrawable(AndroidColor.TRANSPARENT))
                    }
                }
                val tap = Modifier.clickable(remember { MutableInteractionSource() }, indication = null, onClick = ::finish)
                if (type == GUIDE_PHONE) PhoneGuide(tap) else OverlayGuide(tap)
            }
        }
        window.decorView.postDelayed({ finish() }, 6000)
    }

    companion object {
        private const val EXTRA_TYPE = "guide_type"
        const val GUIDE_OVERLAY = 1
        const val GUIDE_PHONE = 727

        /**
         * 2.7.2 ua/l: the guide follows 1 s after the system page opens; for the overlay page it also watches the switch
         * and brings [activity] back as soon as it is on. Best effort: Android may refuse a start from the background.
         */
        fun showLater(activity: Activity, type: Int) {
            val handler = Handler(Looper.getMainLooper())
            handler.postDelayed({
                runCatching { activity.startActivity(Intent(activity, PermissionGuideActivity::class.java).putExtra(EXTRA_TYPE, type)) }
            }, 1000)
            if (type != GUIDE_OVERLAY) return
            val until = SystemClock.elapsedRealtime() + 120_000 // shortcut: 2.7.2 polls until granted; we stop after 2 min
            handler.postDelayed(object : Runnable {
                override fun run() {
                    if (activity.isDestroyed) return
                    if (Settings.canDrawOverlays(activity)) {
                        runCatching {
                            activity.startActivity(
                                Intent(activity, activity.javaClass).addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
                            )
                        }
                    } else if (SystemClock.elapsedRealtime() < until) {
                        handler.postDelayed(this, 200)
                    }
                }
            }, 200)
        }
    }
}

@Composable
private fun OverlayGuide(modifier: Modifier) {
    Column(
        modifier
            .fillMaxWidth()
            .height(100.sdp)
            .background(colorResource(R.color.colorPrimary), RoundedCornerShape(topStart = 20.sdp, topEnd = 20.sdp)),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            stringResource(R.string.gallery_allow_display), style = GalleryText.BodyLargeSemiBold, color = colorResource(R.color.white),
            modifier = Modifier.padding(top = 12.sdp)
        )
        GuideLottie(R.raw.anime_overlay, Modifier.fillMaxWidth().weight(1f).padding(horizontal = 10.sdp))
    }
}

@Composable
private fun PhoneGuide(modifier: Modifier) {
    GuideLottie(R.raw.app_setings, modifier.fillMaxWidth())
}

@Composable
private fun GuideLottie(raw: Int, modifier: Modifier) {
    val composition by rememberLottieComposition(LottieCompositionSpec.RawRes(raw))
    LottieAnimation(composition, iterations = LottieConstants.IterateForever, contentScale = ContentScale.Fit, modifier = modifier)
}
