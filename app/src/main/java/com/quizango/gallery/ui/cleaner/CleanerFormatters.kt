package com.quizango.gallery.ui.cleaner

import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.unit.em
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.util.Locale
import kotlin.math.log10
import kotlin.math.pow

private val UNITS = arrayOf("B", "kB", "MB", "GB", "TB")

/** s9/k.OooO0o0: 1024-based unit from log10, "#,##0.#" with US symbols in every locale, "0 B" for <= 0. */
internal fun formatCleanerSize(bytes: Long): String {
    if (bytes <= 0) return "0 B"
    // The reference indexes past TB (crash) from 1 PiB; clamp instead.
    val i = (log10(bytes.toDouble()) / log10(1024.0)).toInt().coerceAtMost(UNITS.lastIndex)
    val number = DecimalFormat("#,##0.#", DecimalFormatSymbols(Locale.US)).format(bytes / 1024.0.pow(i))
    return "$number ${UNITS[i]}"
}

/** s9/k.OooO0Oo: the same text with the unit at half size (RelativeSizeSpan 0.5). */
internal fun formatCleanerSizeSpanned(bytes: Long): AnnotatedString {
    val text = formatCleanerSize(bytes)
    val unitStart = text.lastIndexOf(' ') + 1
    return buildAnnotatedString {
        append(text)
        addStyle(SpanStyle(fontSize = 0.5.em), unitStart, text.length)
    }
}
