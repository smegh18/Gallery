package com.quizango.gallery.ui.vault

import android.widget.Toast
import androidx.annotation.StringRes
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.drag
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import com.quizango.gallery.R
import com.quizango.gallery.data.local.VaultSecret
import com.quizango.gallery.data.local.datastore.AppPreferences
import com.quizango.gallery.ui.browse.SharedMediaToolbar
import com.quizango.gallery.ui.theme.GalleryText
import com.quizango.gallery.ui.theme.Inter
import com.quizango.gallery.ui.theme.sdp
import com.quizango.gallery.ui.theme.ssp
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/**
 * Reference VaultLockActivity: PIN or pattern lock. Every rule lives in [VaultLockFlow]; this screen only
 * draws the state and reports where to go next, so the NavHost owns all routing.
 * [type]: 0 unlock or first setup, 1 Change Password, 2 Change Recovery Question; [openVault] false for the Add to Private gate.
 */
@Composable
fun VaultLockScreen(
    appPreferences: AppPreferences,
    type: Int = 0,
    openVault: Boolean = true,
    onOpenVault: () -> Unit,
    onResultOk: (passwordSaved: Boolean) -> Unit,
    onOpenRecovery: () -> Unit,
    onOpenForgot: () -> Unit,
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var flow by remember { mutableStateOf<VaultLockFlow?>(null) }
    var storedPin by remember { mutableStateOf<String?>(null) }
    var storedPattern by remember { mutableStateOf<String?>(null) }
    var entered by remember { mutableStateOf("") }
    // Saved with the entry so the count and the one-time prompt survive Forgot Password covering this screen
    var wrongCount by rememberSaveable { mutableIntStateOf(0) }
    var promptShown by rememberSaveable { mutableStateOf(false) }
    var version by remember { mutableIntStateOf(0) } // the flow is plain Kotlin: bump to redraw after it changes

    fun toast(@StringRes res: Int) = Toast.makeText(context, context.getString(res), Toast.LENGTH_SHORT).show()

    fun route(next: LockNext) {
        when (next) {
            LockNext.Vault -> onOpenVault()
            LockNext.Recovery -> onOpenRecovery()
            is LockNext.ResultOk -> onResultOk(next.passwordSaved)
            LockNext.Stay -> version++
        }
    }

    fun handle(f: VaultLockFlow, event: LockEvent, value: String) {
        when (event) {
            is LockEvent.Toast -> {
                toast(event.res)
                if (entered.length == 4) entered = "" // a mismatch restarts setup; a short PIN keeps its digits
            }
            LockEvent.Clear -> scope.launch { delay(30); entered = "" } // reference clears 30 ms later
            is LockEvent.Save -> scope.launch {
                appPreferences.saveVaultLock(event.value, event.pattern)
                route(f.afterSuccess(passwordSaved = true))
            }
            LockEvent.Success -> scope.launch {
                // Upgrade a plain value saved by an older version to a hash
                if (!VaultSecret.isHashed(if (f.isPattern) storedPattern else storedPin)) appPreferences.saveVaultLock(value, f.isPattern)
                entered = ""
                route(f.afterSuccess(passwordSaved = false))
            }
            is LockEvent.Wrong -> {
                wrongCount = f.wrongCount
                event.toast?.let { toast(it) }
                entered = ""
                if (event.lockOut) scope.launch {
                    appPreferences.setPrivateLock(true)
                    onOpenForgot()
                }
            }
        }
        version++
    }

    fun matcher(f: VaultLockFlow): (String) -> Boolean =
        { VaultSecret.matches(if (f.isPattern) storedPattern else storedPin, it) }

    LaunchedEffect(Unit) {
        val pin = appPreferences.vaultPin.first()
        val pattern = appPreferences.vaultPattern.first()
        val lockSet = VaultGate.lockSet(pin, pattern)
        // L5: after the fifth miss every open goes to Forgot Password, before any fingerprint prompt.
        // Once Forgot Password wiped the lock there is nothing left to guard, so setup shows instead.
        if (lockSet && appPreferences.isPrivateLock.first()) {
            onOpenForgot()
            return@LaunchedEffect
        }
        storedPin = pin
        storedPattern = pattern
        val f = VaultLockFlow(
            type = if (lockSet) type else 0, // L10: a wiped lock restarts as plain setup
            openVault = openVault,
            lockSet = lockSet,
            storedIsPattern = !pattern.isNullOrEmpty(),
            hasQuestion = appPreferences.hasRecoveryQuestion.first(),
            wrongCount = wrongCount
        )
        flow = f
        // Fingerprint (Vault Settings) only for a plain unlock; PIN/pattern stays available underneath
        val activity = context as? androidx.fragment.app.FragmentActivity
        if (type == 0 && lockSet && !promptShown && activity != null && appPreferences.isFingerprintEnabled.first() &&
            VaultBiometric.unavailableReason(context) == null
        ) {
            promptShown = true
            delay(600) // reference onCreate postDelayed
            VaultBiometric.prompt(activity) { route(f.afterSuccess(passwordSaved = false)) }
        }
    }
    version // read so redraws follow flow changes
    val f = flow
    val usePattern = f?.isPattern == true
    // Vault Settings "Show Pattern": draw the traced line (on by default, S3)
    val showPatternLine by appPreferences.isPatternMode.collectAsState(initial = true)

    val title = if (f == null) "" else stringResource(f.stepTitle)
    val toolbarTitle = if (f == null) "" else stringResource(f.toolbarTitle)

    val bg = colorResource(R.color.backgroundColor)
    val titleColor = colorResource(R.color.titleColor)
    val linkColor = colorResource(R.color.ic_lock_color)

    Column(modifier = Modifier.fillMaxSize().background(bg)) {
        SharedMediaToolbar(title = toolbarTitle, onBack = onNavigateBack)
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .navigationBarsPadding(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Image(
                painter = painterResource(R.drawable.ic_lock),
                contentDescription = null, // decorative
                modifier = Modifier.padding(top = 40.sdp)
            )
            Text(
                text = title,
                style = GalleryText.BodyXLargeMedium,
                color = colorResource(R.color.black),
                modifier = Modifier.padding(top = 15.sdp)
            )

            if (usePattern) {
                PatternPad(
                    showPath = showPatternLine,
                    modifier = Modifier.padding(top = 33.sdp),
                    resetKey = "$title|${f?.stage}",
                    onComplete = { pattern ->
                        f?.let {
                            val event = it.submitPattern(pattern, matcher(it))
                            handle(it, event, pattern)
                            when (event) {
                                LockEvent.Success, is LockEvent.Save -> true
                                is LockEvent.Wrong -> false
                                else -> null
                            }
                        }
                    }
                )
            } else {
                // Digits and keypad keep their order under RTL
                CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                    Row(modifier = Modifier.padding(top = 20.sdp)) {
                        val lineColor = colorResource(R.color.vault_divider_color)
                        val underline = 1.5.dp
                        repeat(4) { i ->
                            Box(
                                modifier = Modifier
                                    .padding(5.sdp)
                                    .width(36.sdp)
                                    .height(30.sdp)
                                    .drawBehind {
                                        val y = size.height - underline.toPx() / 2
                                        drawLine(lineColor, Offset(0f, y), Offset(size.width, y), underline.toPx())
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                // shortcut: the filled dot stays until a device capture shows the reference glyph (A-pin)
                                if (i < entered.length) Text("●", style = GalleryText.BodyXLargeSemiBold, color = titleColor)
                            }
                        }
                    }
                    VaultKeypad(
                        onDigitClick = { d -> if (entered.length < 4) entered += d },
                        onBackspace = { if (entered.isNotEmpty()) entered = entered.dropLast(1) },
                        // Reference checks only when the check mark is pressed
                        onConfirm = { f?.let { handle(it, it.submitPin(entered, matcher(it)), entered) } }
                    )
                }
            }

            if (f?.showForgot == true) {
                Text(
                    text = stringResource(R.string.forgot_password_lock),
                    style = GalleryText.BodyLargeSemiBold,
                    color = linkColor,
                    modifier = Modifier
                        .padding(10.sdp)
                        .clickable { onOpenForgot() }
                        .padding(horizontal = 16.sdp, vertical = 10.sdp)
                )
            }
            if (f?.showSwitch == true) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .padding(16.sdp)
                        .clickable {
                            f.switchMode()
                            entered = ""
                            version++
                        }
                ) {
                    Image(
                        painter = painterResource(if (usePattern) R.drawable.ic_switch_pin else R.drawable.ic_switch_pattern),
                        contentDescription = null, // decorative, the text names the action
                        colorFilter = ColorFilter.tint(linkColor)
                    )
                    Text(
                        text = stringResource(if (usePattern) R.string.switch_to_pin else R.string.switch_to_pattern),
                        style = GalleryText.BodyLargeSemiBold,
                        color = linkColor,
                        textDecoration = TextDecoration.Underline,
                        modifier = Modifier.padding(start = 10.sdp)
                    )
                }
            }
        }
    }
}

/** 3x3 pattern grid; reports the visited dots as a string like "0367" and shows the verdict: true green, false red, null neutral. */
@Composable
private fun PatternPad(
    showPath: Boolean,
    modifier: Modifier = Modifier,
    resetKey: Any,
    onComplete: (String) -> Boolean?
) {
    val dotColor = colorResource(R.color.titleColor)
    val neutral = colorResource(R.color.patternColor)
    val green = colorResource(R.color.green_color_picker)
    val red = colorResource(R.color.red_color_picker)
    var path by remember(resetKey) { mutableStateOf(listOf<Int>()) }
    var finger by remember(resetKey) { mutableStateOf<Offset?>(null) }
    var verdict by remember(resetKey) { mutableStateOf<Boolean?>(null) }
    var lifts by remember(resetKey) { mutableIntStateOf(0) }
    var cell by remember { mutableStateOf(0f) }

    // Reference postDelayed(30 ms) clearPattern after every finished draw
    LaunchedEffect(lifts) {
        if (lifts > 0) {
            delay(30)
            if (finger == null) {
                path = emptyList()
                verdict = null
            }
        }
    }

    fun centerOf(i: Int) = Offset(cell * (i % 3) + cell / 2, cell * (i / 3) + cell / 2)
    fun hit(p: Offset): Int? = (0..8).firstOrNull { (centerOf(it) - p).getDistance() < cell * 0.3f }

    Canvas(
        modifier = modifier
            .size(260.sdp)
            .pointerInput(resetKey) {
                // Track from the touch-down point so the first dot always counts
                awaitEachGesture {
                    val down = awaitFirstDown()
                    verdict = null
                    path = listOfNotNull(hit(down.position))
                    finger = down.position
                    drag(down.id) { change ->
                        change.consume()
                        finger = change.position
                        hit(change.position)?.let { if (it !in path) path = path + it }
                    }
                    finger = null
                    val done = path.joinToString("")
                    if (done.isNotEmpty()) {
                        verdict = onComplete(done)
                        lifts++
                    }
                }
            }
    ) {
        cell = size.width / 3f
        val stateColor = when (verdict) { true -> green; false -> red; null -> neutral }
        if (showPath) {
            for (k in 1 until path.size) {
                drawLine(stateColor, centerOf(path[k - 1]), centerOf(path[k]), strokeWidth = 3.dp.toPx(), cap = StrokeCap.Round)
            }
            val last = path.lastOrNull()
            val f = finger
            if (last != null && f != null) drawLine(stateColor, centerOf(last), f, strokeWidth = 3.dp.toPx(), cap = StrokeCap.Round)
        }
        for (i in 0..8) {
            // the activated dot is 28.dp across, the others 12.dp; stealth keeps every dot plain
            if (showPath && i in path) drawCircle(stateColor, radius = 14.dp.toPx(), center = centerOf(i))
            else drawCircle(dotColor, radius = 6.dp.toPx(), center = centerOf(i))
        }
    }
}

@Composable
private fun VaultKeypad(
    onDigitClick: (String) -> Unit,
    onBackspace: () -> Unit,
    onConfirm: () -> Unit
) {
    val rows = listOf(
        listOf("1", "2", "3"),
        listOf("4", "5", "6"),
        listOf("7", "8", "9"),
        listOf("BACK", "0", "CONFIRM")
    )
    val digit = TextStyle(fontFamily = Inter, fontWeight = FontWeight.W500, fontSize = 18.ssp)
    val black = colorResource(R.color.black)
    Column(modifier = Modifier.fillMaxWidth().padding(start = 30.sdp, end = 30.sdp, top = 4.sdp)) {
        rows.forEachIndexed { r, row ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 10.sdp, bottom = if (r == rows.lastIndex) 10.sdp else 0.sdp)
            ) {
                row.forEach { item ->
                    Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
                        Box(
                            modifier = Modifier
                                .width(50.sdp)
                                .height(47.sdp)
                                .clickable {
                                    when (item) {
                                        "BACK" -> onBackspace()
                                        "CONFIRM" -> onConfirm()
                                        else -> onDigitClick(item)
                                    }
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            when (item) {
                                "BACK" -> Image(
                                    painter = painterResource(R.drawable.ic_backspace),
                                    contentDescription = stringResource(R.string.delete),
                                    colorFilter = ColorFilter.tint(black),
                                    modifier = Modifier.padding(7.sdp)
                                )
                                "CONFIRM" -> Image(
                                    painter = painterResource(R.drawable.ic_confirm),
                                    contentDescription = stringResource(R.string.confirm),
                                    colorFilter = ColorFilter.tint(black),
                                    modifier = Modifier.padding(7.sdp)
                                )
                                else -> Text(text = item, style = digit, color = colorResource(R.color.titleColor))
                            }
                        }
                    }
                }
            }
        }
    }
}
