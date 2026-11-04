package com.quizango.gallery.ui.vault

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.quizango.gallery.R
import com.quizango.gallery.data.local.VaultSecret
import com.quizango.gallery.data.local.datastore.AppPreferences
import com.quizango.gallery.ui.theme.GalleryBlue
import com.quizango.gallery.ui.theme.cardBackground
import com.quizango.gallery.ui.theme.settingsDivider
import kotlinx.coroutines.launch

/** Reference VaultForgotPasswordActivity: pick one of your saved questions and answer it. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VaultForgotPasswordScreen(
    appPreferences: AppPreferences,
    onAnswerVerified: () -> Unit,
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val pairs by appPreferences.vaultRecoveryPairs.collectAsState(initial = emptyList())
    var selected by remember { mutableIntStateOf(0) }
    var expanded by remember { mutableStateOf(false) }
    var answer by remember { mutableStateOf("") }

    fun answerMatches(stored: String): Boolean =
        if (VaultSecret.isHashed(stored)) VaultSecret.matches(stored, VaultSecret.normalizeAnswer(answer))
        else answer.trim().equals(stored.trim(), ignoreCase = true) // older plain answer

    Scaffold(
        topBar = {
            Column(modifier = Modifier.fillMaxWidth()) {
                TopAppBar(
                    title = {
                        Text(
                            text = "Forgot Password",
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
                    expandedHeight = 56.dp
                )
                HorizontalDivider(thickness = 1.dp, color = MaterialTheme.colorScheme.settingsDivider)
            }
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 19.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(48.dp))
            Text(
                text = "Password Recovery Option",
                fontSize = 20.sp,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onBackground
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Please answer these questions to reset your password.",
                fontSize = 15.sp,
                color = MaterialTheme.colorScheme.onBackground,
                textAlign = TextAlign.Center,
                lineHeight = 19.sp,
                modifier = Modifier.padding(horizontal = 29.dp)
            )
            Spacer(modifier = Modifier.height(72.dp))

            val question = pairs.getOrNull(selected)?.first ?: DEFAULT_SECURITY_QUESTIONS[0]
            Box {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(38.dp)
                        .clip(RoundedCornerShape(19.dp))
                        .background(MaterialTheme.colorScheme.cardBackground)
                        .border(1.dp, MaterialTheme.colorScheme.onBackground.copy(alpha = 0.12f), RoundedCornerShape(19.dp))
                        .clickable(enabled = pairs.size > 1) { expanded = true }
                        .padding(start = 26.dp, end = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(question, fontSize = 14.sp, color = MaterialTheme.colorScheme.onBackground, modifier = Modifier.weight(1f))
                    Icon(Icons.Default.ArrowDropDown, contentDescription = "Select question", tint = MaterialTheme.colorScheme.onBackground)
                }
                DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                    pairs.forEachIndexed { i, (q, _) ->
                        DropdownMenuItem(text = { Text(q, fontSize = 15.sp) }, onClick = {
                            selected = i
                            expanded = false
                        })
                    }
                }
            }
            Spacer(modifier = Modifier.height(12.dp))
            AnswerField(value = answer, onValueChange = { answer = it })
            Spacer(modifier = Modifier.height(36.dp))

            Button(
                onClick = {
                    val stored = pairs.getOrNull(selected)?.second
                    // No saved answer never resets the PIN
                    if (stored != null && answer.isNotBlank() && answerMatches(stored)) {
                        scope.launch {
                            appPreferences.setVaultPin("")
                            appPreferences.setVaultPattern("")
                            onAnswerVerified()
                        }
                    } else {
                        Toast.makeText(context, "Enter the correct security answer.", Toast.LENGTH_SHORT).show()
                    }
                },
                modifier = Modifier
                    .padding(horizontal = 16.dp)
                    .fillMaxWidth()
                    .height(42.dp),
                shape = RoundedCornerShape(21.dp),
                colors = ButtonDefaults.buttonColors(containerColor = GalleryBlue)
            ) {
                Text("Confirm", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Medium)
            }
        }
    }
}
