package com.quizango.gallery.ui.vault

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.quizango.gallery.R
import com.quizango.gallery.data.local.datastore.AppPreferences
import com.quizango.gallery.ui.navigation.Screen
import com.quizango.gallery.ui.theme.GalleryBlue
import com.quizango.gallery.ui.theme.isDark
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VaultLockScreen(
    appPreferences: AppPreferences,
    onNavigate: (String) -> Unit,
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var savedPin by remember { mutableStateOf<String?>(null) }
    var enteredPin by remember { mutableStateOf("") }
    var firstEnteredPin by remember { mutableStateOf<String?>(null) }
    var errorMessage by remember { mutableStateOf("") }
    var isSettingUp by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        val pin = appPreferences.vaultPin.first()
        savedPin = pin
        isSettingUp = pin.isNullOrEmpty()
    }

    val verifyOrConfirmPin: () -> Unit = {
        if (enteredPin.length == 4) {
            if (isSettingUp) {
                if (firstEnteredPin == null) {
                    firstEnteredPin = enteredPin
                    enteredPin = ""
                } else {
                    if (enteredPin == firstEnteredPin) {
                        scope.launch {
                            appPreferences.setVaultPin(enteredPin)
                            val hasQuestion = !appPreferences.vaultQuestion.first().isNullOrEmpty()
                            if (hasQuestion) {
                                onNavigate(Screen.VaultMedia.route)
                            } else {
                                onNavigate(Screen.VaultRecoveryQuestion.route)
                            }
                        }
                    } else {
                        errorMessage = "PINs do not match. Try again."
                        firstEnteredPin = null
                        enteredPin = ""
                    }
                }
            } else {
                if (enteredPin == savedPin) {
                    onNavigate(Screen.VaultMedia.route)
                } else {
                    errorMessage = "Incorrect PIN. Try again."
                    enteredPin = ""
                }
            }
        }
    }

    val onDigitClick: (String) -> Unit = { digit ->
        if (enteredPin.length < 4) {
            val newPin = enteredPin + digit
            enteredPin = newPin
            errorMessage = ""
            if (newPin.length == 4 && !isSettingUp) {
                // Auto verify on 4 digits when unlocking
                if (newPin == savedPin) {
                    onNavigate(Screen.VaultMedia.route)
                } else {
                    errorMessage = "Incorrect PIN. Try again."
                    enteredPin = ""
                }
            }
        }
    }

    val onBackspace: () -> Unit = {
        if (enteredPin.isNotEmpty()) {
            enteredPin = enteredPin.dropLast(1)
            errorMessage = ""
        }
    }

    Scaffold(
        topBar = {
            Column(modifier = Modifier.fillMaxWidth()) {
                TopAppBar(
                    title = {
                        Text(
                            text = if (isSettingUp) "Set PIN" else "Private Vault",
                            fontWeight = FontWeight.SemiBold,
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
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.background
                    )
                )
                HorizontalDivider(thickness = 1.dp, color = MaterialTheme.colorScheme.outline)
            }
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(top = 28.dp)
            ) {
                // Centered reference lock icon
                Icon(
                    painter = painterResource(id = R.drawable.ic_lock),
                    contentDescription = "Lock",
                    tint = GalleryBlue,
                    modifier = Modifier.size(60.dp)
                )

                Spacer(modifier = Modifier.height(18.dp))

                val titleText = when {
                    isSettingUp && firstEnteredPin == null -> "Set PIN"
                    isSettingUp && firstEnteredPin != null -> "Confirm your PIN"
                    else -> "Enter your PIN"
                }

                Text(
                    text = titleText,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onBackground
                )

                Spacer(modifier = Modifier.height(26.dp))

                // 4 PIN Dashes with horizontal baseline matching activity_vault_lock.xml
                Row(
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    repeat(4) { index ->
                        val hasDigit = index < enteredPin.length
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.width(38.dp)
                        ) {
                            Text(
                                text = if (hasDigit) "●" else "",
                                fontSize = 22.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onBackground,
                                modifier = Modifier.height(28.dp)
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(2.dp)
                                    .background(
                                        if (hasDigit) GalleryBlue else if (MaterialTheme.colorScheme.isDark) Color(0xFF35373B) else Color(0xFFBFD4E4)
                                    )
                            )
                        }
                    }
                }

                if (errorMessage.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = errorMessage,
                        color = Color(0xFFE11A1A),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            // Reference Keypad matching activity_vault_lock.xml
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                VaultKeypad(
                    onDigitClick = onDigitClick,
                    onBackspace = onBackspace,
                    onConfirm = verifyOrConfirmPin
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Forgot Password Button (visible only when PIN is already set)
                if (!isSettingUp) {
                    Text(
                        text = "Forgot password?",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = GalleryBlue,
                        modifier = Modifier
                            .clickable {
                                onNavigate(Screen.VaultForgotPassword.route)
                            }
                            .padding(8.dp)
                    )
                }
            }
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

    Column(
        verticalArrangement = Arrangement.spacedBy(14.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.fillMaxWidth()
    ) {
        rows.forEach { row ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                row.forEach { item ->
                    when (item) {
                        "BACK" -> {
                            Box(
                                modifier = Modifier
                                    .size(68.dp)
                                    .clip(CircleShape)
                                    .clickable(onClick = onBackspace),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    painter = painterResource(id = R.drawable.ic_backspace),
                                    contentDescription = "Backspace",
                                    tint = MaterialTheme.colorScheme.onBackground,
                                    modifier = Modifier.size(26.dp)
                                )
                            }
                        }
                        "CONFIRM" -> {
                            Box(
                                modifier = Modifier
                                    .size(68.dp)
                                    .clip(CircleShape)
                                    .clickable(onClick = onConfirm),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    painter = painterResource(id = R.drawable.ic_confirm),
                                    contentDescription = "Confirm",
                                    tint = MaterialTheme.colorScheme.onBackground,
                                    modifier = Modifier.size(26.dp)
                                )
                            }
                        }
                        else -> {
                            Box(
                                modifier = Modifier
                                    .size(68.dp)
                                    .clip(CircleShape)
                                    .clickable { onDigitClick(item) },
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = item,
                                    fontSize = 24.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = MaterialTheme.colorScheme.onBackground
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}


