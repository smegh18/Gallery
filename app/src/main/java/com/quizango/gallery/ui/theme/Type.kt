package com.quizango.gallery.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.quizango.gallery.R

val Inter = FontFamily(
    Font(R.font.if_inter_light, FontWeight.W300),
    Font(R.font.if_inter_regular, FontWeight.W400),
    Font(R.font.if_inter_medium, FontWeight.W500),
    Font(R.font.if_inter_semi_bold, FontWeight.W600),
    Font(R.font.if_inter_bold, FontWeight.W700)
)

private val Material = Typography()

val GalleryTypography = Typography(
    displayLarge = Material.displayLarge.copy(fontFamily = Inter),
    displayMedium = Material.displayMedium.copy(fontFamily = Inter),
    displaySmall = Material.displaySmall.copy(fontFamily = Inter),
    headlineLarge = TextStyle(
        fontFamily = Inter,
        fontWeight = FontWeight.Bold,
        fontSize = 24.sp,
        lineHeight = 32.sp,
        letterSpacing = 0.sp
    ),
    headlineMedium = TextStyle(
        fontFamily = Inter,
        fontWeight = FontWeight.SemiBold,
        fontSize = 20.sp,
        lineHeight = 28.sp,
        letterSpacing = 0.sp
    ),
    headlineSmall = Material.headlineSmall.copy(fontFamily = Inter),
    titleLarge = TextStyle(
        fontFamily = Inter,
        fontWeight = FontWeight.SemiBold,
        fontSize = 18.sp,
        lineHeight = 24.sp,
        letterSpacing = 0.sp
    ),
    titleSmall = Material.titleSmall.copy(fontFamily = Inter),
    titleMedium = TextStyle(
        fontFamily = Inter,
        fontWeight = FontWeight.Medium,
        fontSize = 16.sp,
        lineHeight = 22.sp,
        letterSpacing = 0.15.sp
    ),
    bodyLarge = TextStyle(
        fontFamily = Inter,
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp,
        lineHeight = 24.sp,
        letterSpacing = 0.5.sp
    ),
    bodyMedium = TextStyle(
        fontFamily = Inter,
        fontWeight = FontWeight.Normal,
        fontSize = 14.sp,
        lineHeight = 20.sp,
        letterSpacing = 0.25.sp
    ),
    bodySmall = Material.bodySmall.copy(fontFamily = Inter),
    labelLarge = TextStyle(
        fontFamily = Inter,
        fontWeight = FontWeight.Medium,
        fontSize = 14.sp,
        lineHeight = 20.sp,
        letterSpacing = 0.1.sp
    ),
    labelMedium = Material.labelMedium.copy(fontFamily = Inter),
    labelSmall = TextStyle(
        fontFamily = Inter,
        fontWeight = FontWeight.Normal,
        fontSize = 11.sp,
        lineHeight = 16.sp,
        letterSpacing = 0.5.sp
    )
)

/** The 23 reference Body* styles (2.7.2 values/styles.xml); size is ssp-scaled, line box is Inter's natural one. */
object GalleryText {
    val BodyXXSmallMedium: TextStyle @Composable @ReadOnlyComposable get() = body(7, FontWeight.W500)
    val BodyXSmallRegular: TextStyle @Composable @ReadOnlyComposable get() = body(8, FontWeight.W400)
    val BodyXSmallMedium: TextStyle @Composable @ReadOnlyComposable get() = body(8, FontWeight.W500)
    val BodyXSmallMediumGrid: TextStyle @Composable @ReadOnlyComposable get() = BodyXSmallMedium // identical in the reference
    val BodyXSmallSemiBold: TextStyle @Composable @ReadOnlyComposable get() = body(8, FontWeight.W600)
    val BodyXSmallBold: TextStyle @Composable @ReadOnlyComposable get() = body(8, FontWeight.W700)
    val BodySmallRegular: TextStyle @Composable @ReadOnlyComposable get() = body(10, FontWeight.W400)
    val BodySmallMedium: TextStyle @Composable @ReadOnlyComposable get() = body(10, FontWeight.W500)
    val BodySmallSemiBold: TextStyle @Composable @ReadOnlyComposable get() = body(10, FontWeight.W600)
    val BodySmallBold: TextStyle @Composable @ReadOnlyComposable get() = body(10, FontWeight.W700)
    val BodyMediumRegular: TextStyle @Composable @ReadOnlyComposable get() = body(11, FontWeight.W400)
    val BodyMediumMedium: TextStyle @Composable @ReadOnlyComposable get() = body(11, FontWeight.W500)
    val BodyMediumSemiBold: TextStyle @Composable @ReadOnlyComposable get() = body(11, FontWeight.W600)
    val BodyLargeRegular: TextStyle @Composable @ReadOnlyComposable get() = body(13, FontWeight.W400)
    val BodyLargeMedium: TextStyle @Composable @ReadOnlyComposable get() = body(13, FontWeight.W500)
    val BodyLargeSemiBold: TextStyle @Composable @ReadOnlyComposable get() = body(13, FontWeight.W600)
    val BodyLargeBold: TextStyle @Composable @ReadOnlyComposable get() = body(13, FontWeight.W700)
    val BodyXLargeMedium: TextStyle @Composable @ReadOnlyComposable get() = body(14, FontWeight.W500)
    val BodyXLargeSemiBold: TextStyle @Composable @ReadOnlyComposable get() = body(14, FontWeight.W600)
    val BodyXLargeBold: TextStyle @Composable @ReadOnlyComposable get() = body(14, FontWeight.W700)
    val BodyXXLargeMedium: TextStyle @Composable @ReadOnlyComposable get() = body(16, FontWeight.W500)
    val BodyXXLargeSemiBold: TextStyle @Composable @ReadOnlyComposable get() = body(16, FontWeight.W600)
    val BodyXXLargeBold: TextStyle @Composable @ReadOnlyComposable get() = body(16, FontWeight.W700)

    @Composable @ReadOnlyComposable
    private fun body(size: Int, weight: FontWeight) = TextStyle(fontFamily = Inter, fontWeight = weight, fontSize = size.ssp)
}
