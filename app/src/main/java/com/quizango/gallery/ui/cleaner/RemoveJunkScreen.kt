package com.quizango.gallery.ui.cleaner

import android.os.Environment
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Easing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.graphics.drawable.toBitmap
import com.airbnb.lottie.compose.LottieAnimation
import com.airbnb.lottie.compose.LottieCompositionSpec
import com.airbnb.lottie.compose.LottieConstants
import com.airbnb.lottie.compose.rememberLottieAnimatable
import com.airbnb.lottie.compose.rememberLottieComposition
import com.quizango.gallery.R
import com.quizango.gallery.data.local.JunkScanResult
import com.quizango.gallery.data.local.JunkScanner
import com.quizango.gallery.data.local.datastore.AppPreferences
import com.quizango.gallery.data.local.failedPaths
import com.quizango.gallery.data.local.successfulPaths
import com.quizango.gallery.ui.browse.SharedMediaToolbar
import com.quizango.gallery.ui.home.drawableBackground
import com.quizango.gallery.ui.theme.GalleryText
import com.quizango.gallery.ui.theme.Inter
import com.quizango.gallery.ui.theme.sdp
import com.quizango.gallery.ui.theme.ssp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import kotlin.math.PI
import kotlin.math.cos

/** JunkScanActivity, RemoveJunkActivity (results, cleaning) and JunkClearedActivity as states of one route. */
enum class StorageCleanPhase { SCANNING, OVERVIEW, CLEANING, COMPLETED }

/** dx/f: the done text stays up this long before the results open. */
internal const val JUNK_SCAN_HOLD_MS = 2500L

/** dx/f uses the fixed `junk_scan_completed_message` ("1.40 GB" baked in, Q1), never the measured size. */
internal fun junkScanStatus(done: Boolean) = if (done) R.string.junk_scan_completed_message else R.string.scanning

/** RJ.OoooooO: the cleaning size counts down over 800 ms; the cleaning state never closes sooner, even on fast IO. */
internal const val JUNK_CLEAN_MIN_MS = 800L

/** RJ.Oooooo: the button hint Lottie appears this long after the results bind. */
private const val JUNK_HINT_DELAY_MS = 3000L

/** android.view.animation.AccelerateDecelerateInterpolator, used by both RJ and dx/a counters. */
private val AccelerateDecelerate = Easing { (cos((it + 1) * PI) / 2 + 0.5).toFloat() }

/** Clean and Deep Clean act only from the results with a positive ticked size, so a repeated tap cannot start a second delete. */
internal fun canStartJunkClean(phase: StorageCleanPhase, selectedBytes: Long) =
    phase == StorageCleanPhase.OVERVIEW && selectedBytes > 0

/** RJ dataMap rows in order (the ads slot before them stays collapsed, D5): dy/m names, dy/g icons. */
internal enum class JunkRow(val id: String, @StringRes val title: Int, @DrawableRes val icon: Int) {
    JUNK(JunkScanner.APP_CACHE, R.string.junk_files_display, R.drawable.ic_cleaner_junk_file),
    APK(JunkScanner.APK, R.string.obsolete_apk_files, R.drawable.ic_cleaner_apk_file),
    TEMP(JunkScanner.TEMP, R.string.temp_file, R.drawable.ic_cleaner_temp_file),
    LOG(JunkScanner.LOG, R.string.log_files, R.drawable.ic_cleaner_log_file),
    EMPTY(JunkScanner.EMPTY_FOLDER, R.string.empty_folder, R.drawable.ic_cleaner_folder_file);

    companion object {
        fun of(id: String) = entries.first { it.id == id }
    }
}

/** Scan defaults: every found path is ticked except Log Files, which are real documents (Q6: the user opts in). */
internal fun initialJunkSelection(results: List<JunkScanResult>): Set<String> =
    results.filter { it.id != JunkScanner.LOG }.flatMapTo(mutableSetOf()) { r -> r.files.map { it.path } }

/** dy/g: ic_check_new when every path is ticked, ic_uncheck_new when none, ic_radio_foreground_new when some. */
@DrawableRes
internal fun junkCheckIcon(paths: List<String>, selected: Set<String>): Int {
    val ticked = paths.count { it in selected }
    return when (ticked) {
        0 -> R.drawable.ic_uncheck_new
        paths.size -> R.drawable.ic_check_new
        else -> R.drawable.ic_radio_foreground_new
    }
}

/** Category check tap: a fully ticked category clears, any other ticks every path. */
internal fun toggleJunkCategory(paths: List<String>, selected: Set<String>): Set<String> =
    if (paths.all { it in selected }) selected - paths.toSet() else selected + paths

internal fun junkBytes(paths: Iterable<String>, sizes: Map<String, Long>): Long = paths.sumOf { sizes[it] ?: 0L }

/** What one Clean did: the paths that are gone, their scanned bytes, and the paths that failed (left ticked to retry). */
internal data class JunkCleanResult(val removed: Set<String>, val failed: Set<String>, val freedBytes: Long)

/** Junk cleared only when at least one path is really gone; if every path failed, back to the results to retry. */
internal fun phaseAfterJunkClean(result: JunkCleanResult) =
    if (result.removed.isEmpty()) StorageCleanPhase.OVERVIEW else StorageCleanPhase.COMPLETED

/**
 * Deletes a frozen snapshot of ticked paths; freed bytes count only paths whose outcome says they are gone.
 * Cleaned-today (JC isJunkCleanedForToday) is persisted only after the delete, and only when something was removed.
 */
internal suspend fun runJunkClean(snapshot: List<String>, sizes: Map<String, Long>, markCleaned: suspend () -> Unit): JunkCleanResult {
    val outcomes = JunkScanner.clean(snapshot)
    val removed = outcomes.successfulPaths()
    if (removed.isNotEmpty()) markCleaned()
    return JunkCleanResult(removed.toSet(), outcomes.failedPaths().toSet(), junkBytes(removed, sizes))
}

/** activity_junk_scan: orange while scanning, red with done_cleaner_lottie once progress reaches 100. */
@Composable
private fun JunkScanState(done: Boolean, path: String, onBack: () -> Unit) {
    val white = colorResource(R.color.onlyWhite)
    val scanLottie by rememberLottieComposition(LottieCompositionSpec.RawRes(R.raw.scan_cleaner_lottie))
    val doneLottie by rememberLottieComposition(LottieCompositionSpec.RawRes(R.raw.done_cleaner_lottie))
    Column(
        Modifier
            .fillMaxSize()
            .drawableBackground(if (done) R.drawable.layout_complete_background else R.drawable.layout_progress_background)
            .padding(bottom = 22.sdp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(Modifier.padding(top = 24.sdp).fillMaxWidth().height(56.dp), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack, modifier = Modifier.size(56.dp)) {
                Image(painterResource(R.drawable.ic_back_white), stringResource(R.string.viewer_back))
            }
            Text(stringResource(R.string.remove_junk), style = GalleryText.BodyXXLargeSemiBold, color = white, maxLines = 1)
        }
        Column(
            Modifier.weight(1f).fillMaxWidth(),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            if (done) LottieAnimation(doneLottie, modifier = Modifier.size(180.sdp))
            else LottieAnimation(scanLottie, iterations = LottieConstants.IterateForever, modifier = Modifier.size(180.sdp))
            Text(
                stringResource(junkScanStatus(done)),
                style = GalleryText.BodyLargeSemiBold,
                color = white,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 24.sdp)
            )
            if (!done) Text(
                path.ifBlank { "/storage/emulated/0" },
                style = GalleryText.BodyMediumRegular,
                color = white,
                textAlign = TextAlign.Center,
                maxLines = 1,
                // shortcut: end ellipsis, Compose 1.7 has no MiddleEllipsis; switch when the BOM reaches Compose 1.8
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(top = 6.sdp, start = 24.sdp, end = 24.sdp)
            )
        }
    }
}

/** Junk scan, results, cleaning and Junk cleared (UI-SPEC §2). Back from any state leaves the route. */
@Composable
fun RemoveJunkScreen(
    appPreferences: AppPreferences,
    onNavigateBack: () -> Unit,
    onNavigateToDuplicates: () -> Unit = {},
    onNavigateToScreenshots: () -> Unit = {},
    onNavigateToLargeVideos: () -> Unit = {}
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    // Saved so Back from a "Release more space" row returns to Junk cleared, not a new scan
    var phase by rememberSaveable { mutableStateOf(StorageCleanPhase.SCANNING) }
    var scanningPath by remember { mutableStateOf("") }
    var scanProgress by remember { mutableIntStateOf(0) }
    var freedBytes by rememberSaveable { mutableLongStateOf(0L) }
    var results by remember { mutableStateOf(emptyList<JunkScanResult>()) }
    var sizes by remember { mutableStateOf(emptyMap<String, Long>()) }
    var selected by remember { mutableStateOf(emptySet<String>()) }
    var cleaningBytes by remember { mutableLongStateOf(0L) }
    var cleanDone by remember { mutableStateOf(false) }

    // Real scan (JunkScanner): every size shown comes from files that exist.
    // Leaving the screen cancels this effect, so a stale scan never reaches the results (and stops walking).
    LaunchedEffect(Unit) {
        if (phase == StorageCleanPhase.COMPLETED) return@LaunchedEffect
        phase = StorageCleanPhase.SCANNING
        scanProgress = 0
        val job = coroutineContext.job
        val (found, foundSizes) = withContext(Dispatchers.IO) {
            var last = 0L
            val r = JunkScanner.scan(
                storageRoot = Environment.getExternalStorageDirectory(),
                appCacheDirs = listOfNotNull(context.cacheDir, context.externalCacheDir),
                onProgress = { scanProgress = it }
            ) { path ->
                job.ensureActive()
                val now = System.currentTimeMillis()
                if (now - last > 80) { scanningPath = path; last = now } // throttle UI updates
            }
            // Sizes are read once here, so the results never stat files on recomposition
            r to r.flatMap { it.files }.associate { it.path to it.length() }
        }
        results = found
        sizes = foundSizes
        selected = initialJunkSelection(found)
        delay(JUNK_SCAN_HOLD_MS)
        phase = StorageCleanPhase.OVERVIEW
    }

    val selectedBytes = junkBytes(selected, sizes)

    fun startClean() {
        if (!canStartJunkClean(phase, selectedBytes)) return
        val snapshot = selected.toList() // frozen at the tap: later ticks cannot widen the delete
        val snapshotSizes = sizes
        cleaningBytes = selectedBytes
        cleanDone = false
        phase = StorageCleanPhase.CLEANING
        scope.launch {
            val result = coroutineScope {
                // NonCancellable: once deletion starts it runs to the end even if the user leaves
                val work = async(Dispatchers.IO + NonCancellable) {
                    runJunkClean(snapshot, snapshotSizes) { appPreferences.setJunkCleanedForToday(true) }
                }
                delay(JUNK_CLEAN_MIN_MS)
                work.await()
            }
            results = results.map { r -> r.copy(files = r.files.filter { it.path !in result.removed }) }
            selected = selected - result.removed
            freedBytes = result.freedBytes
            if (phaseAfterJunkClean(result) == StorageCleanPhase.OVERVIEW) phase = StorageCleanPhase.OVERVIEW
            else cleanDone = true // junk_done plays once, then Junk cleared
        }
    }

    when (phase) {
        StorageCleanPhase.SCANNING -> JunkScanState(scanProgress >= 100, scanningPath, onNavigateBack)

        StorageCleanPhase.OVERVIEW -> JunkResultsState(
            results = results,
            sizes = sizes,
            selected = selected,
            selectedBytes = selectedBytes,
            onSelectionChange = { selected = it },
            onClean = ::startClean
        )

        StorageCleanPhase.CLEANING -> JunkCleaningState(cleaningBytes, cleanDone, onNavigateBack) {
            phase = StorageCleanPhase.COMPLETED
        }

        // JC: Back (toolbar or system) finishes the route to the caller; the rows keep Junk cleared under them (aw/b)
        StorageCleanPhase.COMPLETED -> JunkClearedState(freedBytes, onNavigateBack) { e ->
            e.pick({}, onNavigateToDuplicates, onNavigateToScreenshots, onNavigateToLargeVideos)()
        }
    }
}

/** RemoveJunkActivity clAnimation: cleaner_lottie with the ticked size counting down, then junk_done once (dx/j). */
@Composable
private fun JunkCleaningState(cleaningBytes: Long, done: Boolean, onBack: () -> Unit, onDoneEnd: () -> Unit) {
    val white = colorResource(R.color.onlyWhite)
    val cleaner by rememberLottieComposition(LottieCompositionSpec.RawRes(R.raw.cleaner_lottie))
    val doneResult = rememberLottieComposition(LottieCompositionSpec.RawRes(R.raw.junk_done))
    val doneAnim = rememberLottieAnimatable()
    val finish by rememberUpdatedState(onDoneEnd)
    // RJ.OoooooO: ValueAnimator.ofFloat(checked, 0) over 800 ms, AccelerateDecelerate
    val count = remember { Animatable(cleaningBytes.toFloat()) }
    LaunchedEffect(Unit) { count.animateTo(0f, tween(JUNK_CLEAN_MIN_MS.toInt(), easing = AccelerateDecelerate)) }
    LaunchedEffect(done) {
        if (!done) return@LaunchedEffect
        // A composition that fails to load must not strand the user on this state
        runCatching { doneResult.await() }.getOrNull()?.let { doneAnim.animate(it) }
        finish()
    }
    val sizeText = with(LocalDensity.current) { 35.sdp.toSp() } // txt_scanning_size uses sdp for text
    Box(Modifier.fillMaxSize()) {
        LottieAnimation(cleaner, iterations = LottieConstants.IterateForever, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
        Column(Modifier.align(Alignment.BottomCenter).padding(bottom = 50.sdp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                formatCleanerSize(count.value.toLong()),
                style = GalleryText.BodyXXLargeBold.copy(fontSize = sizeText),
                color = white,
                modifier = Modifier.padding(bottom = 10.sdp)
            )
            Text(stringResource(R.string.cleaning_item), style = GalleryText.BodyLargeSemiBold, color = white, modifier = Modifier.padding(start = 5.sdp))
        }
        if (done) LottieAnimation(doneResult.value, progress = { doneAnim.progress }, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
        Row(Modifier.padding(top = 24.sdp).fillMaxWidth().height(56.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(56.dp).clickable(onClick = onBack), contentAlignment = Alignment.Center) {
                Image(painterResource(R.drawable.ic_back_white), stringResource(R.string.viewer_back))
            }
            Text(
                stringResource(R.string.remove_junk),
                style = GalleryText.BodyXXLargeSemiBold,
                color = white,
                maxLines = 1,
                modifier = Modifier.weight(1f).padding(start = 5.sdp)
            )
        }
    }
}

/** activity_junk_cleared: freed size counting up, the caption, and the three System Cleaner rows (dy/d, aw/b). */
@Composable
private fun JunkClearedState(freedBytes: Long, onBack: () -> Unit, onOpen: (CleanerEntry) -> Unit) {
    val black = colorResource(R.color.black)
    // dx/a: counts 0 -> freed over 800 ms, AccelerateDecelerate; 0 reads "Finished"
    val count = remember { Animatable(0f) }
    LaunchedEffect(Unit) { count.animateTo(freedBytes.toFloat(), tween(JUNK_CLEAN_MIN_MS.toInt(), easing = AccelerateDecelerate)) }
    Column(Modifier.fillMaxSize().background(colorResource(R.color.backgroundColor))) {
        SharedMediaToolbar(title = stringResource(R.string.tool_bar_remove_junk), onBack = onBack)
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).navigationBarsPadding().padding(bottom = 10.sdp)) {
            Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                Image(painterResource(R.drawable.img_junk_cleared_done), contentDescription = null, Modifier.padding(top = 30.sdp))
                Text(
                    if (freedBytes == 0L) stringResource(R.string.finish) else formatCleanerSize(count.value.toLong()),
                    style = GalleryText.BodyXXLargeBold.copy(fontSize = 18.ssp),
                    color = black,
                    modifier = Modifier.padding(top = 12.sdp)
                )
                Text(
                    stringResource(R.string.phone_good_condition),
                    style = GalleryText.BodyMediumMedium,
                    color = black,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(top = 10.sdp)
                )
            }
            Text(
                stringResource(R.string.release_more_space),
                style = GalleryText.BodyLargeBold,
                color = black,
                modifier = Modifier.padding(top = 20.sdp, start = 20.sdp)
            )
            SystemCleanerRows(onOpen)
        }
    }
}

/** activity_remove_junk results: collapsing red header with the ticked size, the five rows, Clean / Deep Clean. */
@Composable
private fun JunkResultsState(
    results: List<JunkScanResult>,
    sizes: Map<String, Long>,
    selected: Set<String>,
    selectedBytes: Long,
    onSelectionChange: (Set<String>) -> Unit,
    onClean: () -> Unit
) {
    val white = colorResource(R.color.onlyWhite)
    var expanded by remember { mutableStateOf(emptySet<String>()) }
    // RJ.Ooooo00: the header shrinks 350 -> 300 dp over 500 ms when the result binds
    val headerHeight = remember { Animatable(350f) }
    LaunchedEffect(Unit) { headerHeight.animateTo(300f, tween(500)) }
    val subText = with(LocalDensity.current) { 12.sdp.toSp() } // item_remove_junk_sub uses sdp for text
    val hint by rememberLottieComposition(LottieCompositionSpec.RawRes(R.raw.remove_junk_button_animation))
    var hintReady by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { delay(JUNK_HINT_DELAY_MS); hintReady = true }

    Box(Modifier.fillMaxSize().background(colorResource(R.color.backgroundColor))) {
        LazyColumn(
            Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = 70.sdp + WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding())
        ) {
            item(key = "header") {
                Box(
                    Modifier
                        .fillMaxWidth()
                        .drawableBackground(R.drawable.layout_progress_image_view)
                        .statusBarsPadding()
                        .height(headerHeight.value.dp)
                ) {
                    // Expanded header draws no back arrow on device; system Back leaves the route
                    Text(
                        stringResource(R.string.storage_clean),
                        style = GalleryText.BodyXXLargeSemiBold,
                        color = white,
                        maxLines = 1,
                        modifier = Modifier.height(56.dp).wrapContentHeight().padding(start = 16.dp)
                    )
                    Column(
                        Modifier
                            .align(Alignment.TopCenter)
                            .padding(top = 50.sdp)
                            .size(150.sdp)
                            .drawableBackground(R.drawable.storage_clean_bg_img),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            formatCleanerSizeSpanned(selectedBytes),
                            style = GalleryText.BodyXXLargeBold.copy(fontSize = 18.ssp),
                            color = white
                        )
                        Text(
                            stringResource(R.string.junk_found),
                            style = GalleryText.BodySmallBold,
                            color = white,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
            results.forEach { r ->
                val row = JunkRow.of(r.id)
                val paths = r.files.map { it.path }
                val isOpen = r.id in expanded && paths.isNotEmpty()
                item(key = r.id) {
                    Row(
                        Modifier
                            .padding(start = 10.sdp, end = 10.sdp, top = 12.sdp)
                            .fillMaxWidth()
                            .height(22.sdp)
                            .clickable(enabled = paths.isNotEmpty()) {
                                expanded = if (r.id in expanded) expanded - r.id else expanded + r.id
                            },
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Image(painterResource(row.icon), contentDescription = null, Modifier.size(18.sdp))
                        Text(
                            stringResource(row.title),
                            style = GalleryText.BodyLargeSemiBold,
                            color = colorResource(R.color.titleColor),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f).padding(horizontal = 8.sdp)
                        )
                        if (paths.isEmpty()) {
                            Text(
                                stringResource(R.string.no_junk),
                                style = GalleryText.BodySmallSemiBold,
                                color = colorResource(R.color.junkCleanerSubTitleColor)
                            )
                        } else {
                            Image(
                                painterResource(R.drawable.ic_expand_arrow),
                                contentDescription = null,
                                colorFilter = ColorFilter.tint(colorResource(R.color.black)),
                                modifier = Modifier.padding(start = 2.sdp, top = 2.sdp)
                            )
                            Text(
                                formatCleanerSize(junkBytes(paths, sizes)),
                                style = GalleryText.BodyMediumSemiBold,
                                color = colorResource(R.color.black),
                                modifier = Modifier.padding(end = 5.sdp)
                            )
                            Image(
                                painterResource(junkCheckIcon(paths, selected)),
                                contentDescription = stringResource(row.title),
                                modifier = Modifier.size(20.sdp).clickable { onSelectionChange(toggleJunkCategory(paths, selected)) }
                            )
                        }
                    }
                }
                if (isOpen) {
                    // Every file, so each one the category tick selects can be seen and unticked (the list is lazy)
                    items(r.files, key = { "${r.id}:${it.path}" }) { file ->
                        val ticked = file.path in selected
                        Row(
                            Modifier.padding(start = 10.sdp, end = 10.sdp, top = 8.sdp).fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            JunkFileIcon(file, row.icon)
                            Text(
                                if (row == JunkRow.APK) file.nameWithoutExtension else file.name,
                                fontFamily = Inter,
                                fontSize = subText,
                                color = colorResource(if (ticked) R.color.black else R.color.junkCleanerSubTitleColor),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f).padding(horizontal = 8.sdp)
                            )
                            Text(
                                formatCleanerSize(sizes[file.path] ?: 0L),
                                fontFamily = Inter,
                                fontSize = subText,
                                color = colorResource(R.color.titleColor),
                                modifier = Modifier.padding(end = 5.sdp)
                            )
                            Image(
                                painterResource(if (ticked) R.drawable.ic_check_new else R.drawable.ic_uncheck_new_sub),
                                contentDescription = file.name,
                                modifier = Modifier
                                    .size(20.sdp)
                                    .clickable { onSelectionChange(if (ticked) selected - file.path else selected + file.path) }
                                    .padding(2.sdp)
                            )
                        }
                    }
                }
            }
        }
        if (hintReady && selectedBytes > 0) LottieAnimation(
            hint,
            iterations = LottieConstants.IterateForever,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(bottom = 35.dp)
                .fillMaxWidth()
                .height(38.sdp)
                .padding(4.sdp)
        )
        JunkActionBar(selectedBytes, onClean, Modifier.align(Alignment.BottomCenter))
    }
}

/** bottomActionLayout: Clean keeps its grey look; Deep Clean keeps the AD badge (Q2) and dims at 0. Both clean at once. */
@Composable
private fun JunkActionBar(selectedBytes: Long, onClean: () -> Unit, modifier: Modifier) {
    val canClean = selectedBytes > 0
    val pill = RoundedCornerShape(22.sdp)
    Row(
        modifier
            .navigationBarsPadding()
            .padding(start = 19.sdp, end = 19.sdp, bottom = 12.sdp)
            .fillMaxWidth()
            .height(38.sdp)
    ) {
        Box(
            Modifier
                .weight(1f)
                .fillMaxHeight()
                .padding(end = 4.sdp)
                .clip(pill)
                .drawableBackground(R.drawable.bg_remove_junk_clean_disabled)
                .clickable(enabled = canClean, onClick = onClean),
            contentAlignment = Alignment.Center
        ) {
            Text(
                stringResource(R.string.clean),
                fontFamily = Inter,
                fontWeight = FontWeight.Bold,
                fontSize = 11.ssp,
                color = colorResource(R.color.junkCleanerSubTitleColor)
            )
        }
        Row(
            Modifier
                .weight(1f)
                .fillMaxHeight()
                .padding(start = 4.sdp)
                .alpha(if (canClean) 1f else 0.6f)
                .clip(pill)
                .drawableBackground(R.drawable.bg_remove_junk_deep_clean)
                .clickable(enabled = canClean, onClick = onClean)
                .padding(horizontal = 12.sdp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Image(
                painterResource(R.drawable.deep_clean_ic),
                contentDescription = null,
                colorFilter = ColorFilter.tint(colorResource(R.color.onlyWhite))
            )
            Text(
                stringResource(R.string.deep_clean),
                fontFamily = Inter,
                fontWeight = FontWeight.Bold,
                fontSize = 11.ssp,
                color = colorResource(R.color.onlyWhite),
                maxLines = 1,
                modifier = Modifier.padding(start = 5.sdp)
            )
        }
    }
}

/** APKs show their app icon like the reference; other files show the category icon (dy/n). */
@Composable
private fun JunkFileIcon(file: File, @DrawableRes fallbackRes: Int) {
    val context = LocalContext.current
    val icon by produceState<android.graphics.drawable.Drawable?>(null, file) {
        if (file.name.endsWith(".apk")) {
            value = withContext(Dispatchers.IO) {
                try {
                    val pm = context.packageManager
                    pm.getPackageArchiveInfo(file.path, 0)?.applicationInfo?.let { ai ->
                        ai.sourceDir = file.path
                        ai.publicSourceDir = file.path
                        ai.loadIcon(pm)
                    }
                } catch (_: Exception) {
                    null
                }
            }
        }
    }
    val d = icon
    if (d != null) {
        Image(d.toBitmap(64, 64).asImageBitmap(), contentDescription = null, modifier = Modifier.size(20.sdp))
    } else {
        Image(painterResource(fallbackRes), contentDescription = null, modifier = Modifier.size(20.sdp))
    }
}
