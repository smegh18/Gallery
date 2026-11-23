package com.quizango.gallery.ui.vault

import android.widget.Toast
import androidx.annotation.StringRes
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.drag
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.quizango.gallery.R
import com.quizango.gallery.data.local.VaultSecret
import com.quizango.gallery.data.local.datastore.AppPreferences
import com.quizango.gallery.ui.theme.isDark
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/**
 * Reference VaultLockActivity: PIN or pattern lock. Every rule lives in [VaultLockFlow]; this screen only
 * draws the state and reports where to go next, so the NavHost owns all routing.
 * [type]: 0 unlock or first setup, 1 Change Password, 2 Change Recovery Question; [openVault] false for the Add to Private gate.
 */
@OptIn(ExperimentalMaterial3Api::class)
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
    val isSettingUp = f != null && f.stage != LockStage.VERIFY
    val usePattern = f?.isPattern == true
    // Vault Settings "Show Pattern": draw the traced line (on by default, S3)
    val showPatternLine by appPreferences.isPatternMode.collectAsState(initial = true)

    // Error shake (STORY-4); removed with the rest of the old chrome in the look task
    val shakeOffset = remember { Animatable(0f) }

    val title = if (f == null) "" else stringResource(f.stepTitle)
    val toolbarTitle = if (f == null) "" else stringResource(f.toolbarTitle)

    // Reference: lock icon and links #0066FF in light mode, white in dark mode
    val accent = if (MaterialTheme.colorScheme.isDark) Color.White else com.quizango.gallery.ui.theme.GalleryBlueLight

    Scaffold(
        topBar = {
            Column(modifier = Modifier.fillMaxWidth()) {
                TopAppBar(
                    title = {
                        Text(
                            text = toolbarTitle,
                            fontWeight = FontWeight.Medium,
                            fontSize = 18.sp,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                    },
                    navigationIcon = {
                        IconButton(onClick = onNavigateBack) {
                            Icon(
                                painter = painterResource(id = R.drawable.ic_back),
                                contentDescription = "Back",
                                tint = MaterialTheme.colorScheme.onBackground,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background),
                    expandedHeight = 56.dp // reference toolbar height
                )
                HorizontalDivider(thickness = 1.dp, color = MaterialTheme.colorScheme.outline)
            }
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .navigationBarsPadding(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(50.dp))
            Icon(
                painter = painterResource(id = R.drawable.ic_lock),
                contentDescription = "Lock",
                tint = accent,
                modifier = Modifier.size(34.dp)
            )
            Spacer(modifier = Modifier.height(18.dp))
            Text(
                text = title,
                fontSize = 18.sp,
                color = if (MaterialTheme.colorScheme.isDark) Color.White else Color.Black
            )

            if (usePattern) {
                Spacer(modifier = Modifier.height(40.dp))
                PatternPad(
                    showLine = showPatternLine || isSettingUp,
                    modifier = Modifier.offset { IntOffset(shakeOffset.value.dp.roundToPx(), 0) },
                    resetKey = "$title|${f?.stage}",
                    onComplete = { pattern -> f?.let { handle(it, it.submitPattern(pattern, matcher(it)), pattern) } }
                )
            } else {
                Spacer(modifier = Modifier.height(24.dp))
                // 4 PIN slots: small dot above a line (reference)
                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.offset { IntOffset(shakeOffset.value.dp.roundToPx(), 0) }
                ) {
                    repeat(4) { i ->
                        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.width(43.dp)) {
                            Box(modifier = Modifier.height(30.dp), contentAlignment = Alignment.Center) {
                                if (i < entered.length) {
                                    Box(
                                        Modifier
                                            .size(6.dp)
                                            .clip(CircleShape)
                                            .background(MaterialTheme.colorScheme.onBackground)
                                    )
                                }
                            }
                            HorizontalDivider(thickness = 1.5.dp, color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.85f))
                        }
                    }
                }
                Spacer(modifier = Modifier.height(18.dp))
                VaultKeypad(
                    onDigitClick = { d -> if (entered.length < 4) entered += d },
                    onBackspace = { if (entered.isNotEmpty()) entered = entered.dropLast(1) },
                    // Reference checks only when ✓ is pressed
                    onConfirm = { f?.let { handle(it, it.submitPin(entered, matcher(it)), entered) } }
                )
            }

            Spacer(modifier = Modifier.height(if (usePattern) 11.dp else 15.dp))
            if (isSettingUp) {
                // "Switch to Pattern" / "Switch to PIN" only while setting up
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .clickable {
                            f?.switchMode()
                            entered = ""
                            version++
                        }
                        .padding(8.dp)
                ) {
                    Icon(
                        painter = painterResource(id = if (usePattern) R.drawable.ic_switch_pin else R.drawable.ic_switch_pattern),
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onBackground,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = stringResource(if (usePattern) R.string.switch_to_pin else R.string.switch_to_pattern),
                        fontSize = 16.sp,
                        textDecoration = TextDecoration.Underline,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                }
            } else if (f?.showForgot == true) {
                Text(
                    text = stringResource(R.string.forgot_password_lock),
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium,
                    color = accent,
                    modifier = Modifier
                        .clickable { onOpenForgot() }
                        .padding(horizontal = 16.dp, vertical = 12.dp)
                )
            }
        }
    }
}

/** 3x3 pattern grid; reports the visited dots as a string like "0367". */
@Composable
private fun PatternPad(
    showLine: Boolean,
    modifier: Modifier = Modifier,
    resetKey: Any,
    onComplete: (String) -> Unit
) {
    val dotColor = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.92f)
    var path by remember(resetKey) { mutableStateOf(listOf<Int>()) }
    var finger by remember(resetKey) { mutableStateOf<Offset?>(null) }
    var cell by remember { mutableStateOf(0f) }

    fun centerOf(i: Int) = Offset(cell * (i % 3) + cell / 2, cell * (i / 3) + cell / 2)
    fun hit(p: Offset): Int? = (0..8).firstOrNull { (centerOf(it) - p).getDistance() < cell * 0.3f }

    Canvas(
        modifier = modifier
            .size(312.dp) // 3 x 104dp, dot spacing taken from the reference
            .pointerInput(resetKey) {
                // Track from the touch-down point so the first dot always counts
                awaitEachGesture {
                    val down = awaitFirstDown()
                    path = listOfNotNull(hit(down.position))
                    finger = down.position
                    drag(down.id) { change ->
                        change.consume()
                        finger = change.position
                        hit(change.position)?.let { if (it !in path) path = path + it }
                    }
                    val done = path.joinToString("")
                    path = emptyList()
                    finger = null
                    if (done.isNotEmpty()) onComplete(done)
                }
            }
    ) {
        cell = size.width / 3f
        val line = Color(0xFF1A5BD6)
        if (!showLine) {
            for (i in 0..8) drawCircle(dotColor, radius = 6.dp.toPx(), center = centerOf(i))
            return@Canvas
        }
        for (k in 1 until path.size) {
            drawLine(line, centerOf(path[k - 1]), centerOf(path[k]), strokeWidth = 4.dp.toPx(), cap = StrokeCap.Round)
        }
        val last = path.lastOrNull()
        val f = finger
        if (last != null && f != null) {
            drawLine(line, centerOf(last), f, strokeWidth = 4.dp.toPx(), cap = StrokeCap.Round)
        }
        for (i in 0..8) drawCircle(if (i in path) line else dotColor, radius = 6.dp.toPx(), center = centerOf(i))
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
    // Reference keys: 96dp apart horizontally, 68.5dp vertically
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        rows.forEach { row ->
            Row {
                row.forEach { item ->
                    Box(
                        modifier = Modifier
                            .size(width = 96.dp, height = 68.5.dp)
                            .clip(CircleShape)
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
                            "BACK" -> Icon(
                                painter = painterResource(id = R.drawable.ic_backspace),
                                contentDescription = "Backspace",
                                tint = MaterialTheme.colorScheme.onBackground,
                                modifier = Modifier.size(26.dp)
                            )
                            "CONFIRM" -> Icon(
                                painter = painterResource(id = R.drawable.ic_confirm),
                                contentDescription = "Confirm",
                                tint = MaterialTheme.colorScheme.onBackground,
                                modifier = Modifier.size(26.dp)
                            )
                            else -> Text(
                                text = item,
                                fontSize = 24.sp,
                                color = MaterialTheme.colorScheme.onBackground
                            )
                        }
                    }
                }
            }
        }
    }
}
