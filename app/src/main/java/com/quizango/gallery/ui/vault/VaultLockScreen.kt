package com.quizango.gallery.ui.vault

import android.widget.Toast
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.quizango.gallery.R
import com.quizango.gallery.data.local.VaultSecret
import com.quizango.gallery.data.local.datastore.AppPreferences
import com.quizango.gallery.ui.navigation.Screen
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/**
 * Reference VaultLockActivity: PIN or pattern lock.
 * Setup: "Set PIN" -> "Re-enter PIN" (or "Draw Your Pattern" -> "Re-draw your pattern"), switchable.
 * Unlock: "Enter your PIN" / "Draw Your Pattern" with "Forgot password?". Feedback is by toast, as in the reference.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VaultLockScreen(
    appPreferences: AppPreferences,
    onNavigate: (String) -> Unit,
    onNavigateBack: () -> Unit,
    // Change Password: set a new lock; the old one stays until the new one is confirmed
    changeMode: Boolean = false
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var savedPin by remember { mutableStateOf<String?>(null) }
    var savedPattern by remember { mutableStateOf<String?>(null) }
    var loaded by remember { mutableStateOf(false) }
    var usePattern by remember { mutableStateOf(false) }
    var entered by remember { mutableStateOf("") }
    var firstEntry by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        savedPin = appPreferences.vaultPin.first()
        savedPattern = appPreferences.vaultPattern.first()
        usePattern = !changeMode && !savedPattern.isNullOrEmpty()
        loaded = true
        // Fingerprint unlock (Vault Settings); PIN/pattern stays available underneath
        val hasLock = !savedPin.isNullOrEmpty() || !savedPattern.isNullOrEmpty()
        val activity = context as? androidx.fragment.app.FragmentActivity
        if (!changeMode && hasLock && activity != null && appPreferences.isFingerprintEnabled.first() &&
            VaultBiometric.unavailableReason(context) == null
        ) {
            VaultBiometric.prompt(activity) { onNavigate(Screen.VaultMedia.route) }
        }
    }
    val isSettingUp = loaded && (changeMode || (savedPin.isNullOrEmpty() && savedPattern.isNullOrEmpty()))
    // Vault Settings "Show Pattern": draw the traced line (off by default, like the reference)
    val showPatternLine by appPreferences.isPatternMode.collectAsState(initial = false)

    // Error shake (STORY-4); the reference also shows a toast
    val shakeOffset = remember { Animatable(0f) }
    fun fail(message: String) {
        Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
        entered = ""
        scope.launch {
            for (x in listOf(-18f, 18f, -12f, 12f, -6f, 6f, 0f)) shakeOffset.animateTo(x, tween(45))
        }
    }

    fun openVault() = onNavigate(Screen.VaultMedia.route)

    fun submit(value: String) {
        if (isSettingUp) {
            val first = firstEntry
            if (first == null) {
                firstEntry = value
                entered = ""
            } else if (value == first) {
                scope.launch {
                    // Only one lock type is active at a time
                    if (usePattern) {
                        appPreferences.setVaultPattern(value)
                        appPreferences.setVaultPin("")
                    } else {
                        appPreferences.setVaultPin(value)
                        appPreferences.setVaultPattern("")
                    }
                    if (changeMode) {
                        Toast.makeText(context, if (usePattern) "Pattern changed" else "Password changed", Toast.LENGTH_SHORT).show()
                        onNavigateBack()
                        return@launch
                    }
                    val hasQuestion = !appPreferences.vaultQuestion.first().isNullOrEmpty()
                    onNavigate(if (hasQuestion) Screen.VaultMedia.route else Screen.VaultRecoveryQuestion.route)
                }
            } else {
                firstEntry = null
                fail(if (usePattern) "The confirm pattern does not match!" else "The confirm password does not match!")
            }
        } else {
            val stored = if (usePattern) savedPattern else savedPin
            if (VaultSecret.matches(stored, value)) {
                scope.launch {
                    // Upgrade a plain value saved by an older version to a hash
                    if (!VaultSecret.isHashed(stored)) {
                        if (usePattern) appPreferences.setVaultPattern(value) else appPreferences.setVaultPin(value)
                    }
                    openVault()
                }
            } else {
                // shortcut: wrong-pattern toast text not captured from the reference; mirrors "Incorrect password!"
                fail(if (usePattern) "Incorrect pattern!" else "Incorrect password!")
            }
        }
    }

    val title = when {
        !loaded -> ""
        usePattern && isSettingUp && firstEntry != null -> "Re-draw your pattern"
        usePattern -> "Draw Your Pattern"
        isSettingUp && firstEntry == null -> "Set PIN"
        isSettingUp -> "Re-enter PIN"
        else -> "Enter your PIN"
    }

    Scaffold(
        topBar = {
            Column(modifier = Modifier.fillMaxWidth()) {
                TopAppBar(
                    title = {
                        Text(
                            text = "Private Vault",
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
                tint = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.size(34.dp)
            )
            Spacer(modifier = Modifier.height(18.dp))
            Text(
                text = title,
                fontSize = 18.sp,
                color = MaterialTheme.colorScheme.onBackground
            )

            if (usePattern) {
                Spacer(modifier = Modifier.height(40.dp))
                PatternPad(
                    showLine = showPatternLine || isSettingUp,
                    modifier = Modifier.offset { IntOffset(shakeOffset.value.dp.roundToPx(), 0) },
                    resetKey = "$title|${firstEntry != null}",
                    onComplete = { pattern ->
                        // Reference accepted a 3-dot pattern; shorter limit not observed
                        if (pattern.length < 3) fail("Connect at least 3 dots") else submit(pattern)
                    }
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
                    onConfirm = { if (entered.length == 4) submit(entered) }
                )
            }

            Spacer(modifier = Modifier.height(if (usePattern) 11.dp else 15.dp))
            if (isSettingUp) {
                // "Switch to Pattern" / "Switch to PIN" only while setting up
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .clickable {
                            usePattern = !usePattern
                            firstEntry = null
                            entered = ""
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
                        text = if (usePattern) "Switch to PIN" else "Switch to Pattern",
                        fontSize = 16.sp,
                        textDecoration = TextDecoration.Underline,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                }
            } else if (loaded) {
                Text(
                    text = "Forgot password?",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onBackground,
                    modifier = Modifier
                        .clickable { onNavigate(Screen.VaultForgotPassword.route) }
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
