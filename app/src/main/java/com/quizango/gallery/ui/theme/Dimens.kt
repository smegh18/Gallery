package com.quizango.gallery.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/** Reference @dimen/_Nsdp maps to N.sdp and _Nssp to N.ssp; reference values in plain dip/sp stay unscaled N.dp / N.sp. */
internal fun sdpFactor(sw: Int): Float = (sw / 30 * 30).coerceIn(300, 1080) / 300f

val Int.sdp: Dp
    @Composable @ReadOnlyComposable
    get() = (this * sdpFactor(LocalConfiguration.current.smallestScreenWidthDp)).dp

/** Whole px, like TextView reading android:textSize with getDimensionPixelSize (e.g. 9ssp = 21.6px is drawn at 22px). */
val Int.ssp: TextUnit
    @Composable @ReadOnlyComposable
    get() = wholePxSp(this * sdpFactor(LocalConfiguration.current.smallestScreenWidthDp), LocalDensity.current)

internal fun wholePxSp(sp: Float, density: Density): TextUnit {
    val pxPerSp = density.density * density.fontScale
    return (Math.round(sp * pxPerSp).toFloat() / pxPerSp).sp
}
