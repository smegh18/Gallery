package com.quizango.gallery.ui.vault

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.quizango.gallery.R
import com.quizango.gallery.data.local.datastore.AppPreferences
import com.quizango.gallery.ui.theme.GalleryBlue
import com.quizango.gallery.ui.theme.cardBackground
import com.quizango.gallery.ui.theme.cardBorder
import com.quizango.gallery.ui.theme.isDark
import com.quizango.gallery.ui.theme.settingsDivider
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VaultForgotPasswordScreen(
    appPreferences: AppPreferences,
    onAnswerVerified: () -> Unit,
    onNavigateBack: () -> Unit
) {
    val scope = rememberCoroutineScope()
    val savedQuestion by appPreferences.vaultQuestion.collectAsState(initial = null)
    val savedAnswer by appPreferences.vaultAnswer.collectAsState(initial = null)

    var inputAnswer by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    val displayQuestion = savedQuestion ?: "What was your childhood nickname?"

    Scaffold(
        topBar = {
            Column(modifier = Modifier.fillMaxWidth()) {
                TopAppBar(
                    title = {
                        Text(
                            text = "Forgot Password",
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
                HorizontalDivider(thickness = 1.dp, color = MaterialTheme.colorScheme.settingsDivider)
            }
        },
        bottomBar = {
            Surface(
                tonalElevation = 6.dp,
                color = MaterialTheme.colorScheme.surface,
                modifier = Modifier.fillMaxWidth()
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 14.dp)
                ) {
                    Button(
                        onClick = {
                            val trimmedInput = inputAnswer.trim()
                            val expected = savedAnswer?.trim() ?: ""

                            if (expected.isEmpty() || trimmedInput.equals(expected, ignoreCase = true)) {
                                scope.launch {
                                    // Reset pin so user can re-setup
                                    appPreferences.setVaultPin("")
                                    onAnswerVerified()
                                }
                            } else {
                                errorMessage = "Incorrect answer. Please try again."
                            }
                        },
                        enabled = inputAnswer.trim().isNotEmpty(),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp),
                        shape = RoundedCornerShape(24.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = GalleryBlue,
                            disabledContainerColor = GalleryBlue.copy(alpha = 0.4f)
                        )
                    ) {
                        Text(
                            text = "Confirm",
                            color = Color.White,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 20.dp, vertical = 20.dp)
        ) {
            Text(
                text = "Reset Your PIN",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = "Answer your security question to reset your Private Vault PIN",
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = "Security Question",
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onBackground
            )

            Spacer(modifier = Modifier.height(8.dp))

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.cardBackground, RoundedCornerShape(12.dp))
                    .padding(horizontal = 14.dp, vertical = 14.dp)
            ) {
                Text(
                    text = displayQuestion,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            Spacer(modifier = Modifier.height(18.dp))

            Text(
                text = "Your Answer",
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onBackground
            )

            Spacer(modifier = Modifier.height(8.dp))

            OutlinedTextField(
                value = inputAnswer,
                onValueChange = {
                    inputAnswer = it
                    errorMessage = null
                },
                placeholder = { Text("Your answer", color = Color(0xFFB0B7C3), fontSize = 14.sp) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = GalleryBlue,
                    unfocusedBorderColor = MaterialTheme.colorScheme.cardBorder,
                    cursorColor = GalleryBlue,
                    focusedTextColor = MaterialTheme.colorScheme.onSurface,
                    unfocusedTextColor = MaterialTheme.colorScheme.onSurface
                )
            )

            if (errorMessage != null) {
                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = errorMessage!!,
                    color = Color(0xFFE11A1A),
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}

