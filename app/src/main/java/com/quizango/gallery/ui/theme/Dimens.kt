package com.quizango.gallery.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/** Reference @dimen/_Nsdp maps to N.sdp and _Nssp to N.ssp; reference values in plain dip/sp stay unscaled N.dp / N.sp. */
internal fun sdpFactor(sw: Int): Float = (sw / 30 * 30).coerceIn(300, 1080) / 300f

val Int.sdp: Dp
    @Composable @ReadOnlyComposable
    get() = (this * sdpFactor(LocalConfiguration.current.smallestScreenWidthDp)).dp

val Int.ssp: TextUnit
    @Composable @ReadOnlyComposable
    get() = (this * sdpFactor(LocalConfiguration.current.smallestScreenWidthDp)).sp
