package com.quizango.gallery.ui.vault

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
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
import com.quizango.gallery.data.local.datastore.AppPreferences
import com.quizango.gallery.ui.theme.GalleryBlue
import com.quizango.gallery.ui.theme.cardBackground
import com.quizango.gallery.ui.theme.isDark
import com.quizango.gallery.ui.theme.settingsDivider
import kotlinx.coroutines.launch

/** The reference's three questions, one per step (VaultRecoveryQuestionActivity). */
val DEFAULT_SECURITY_QUESTIONS = listOf(
    "What was your childhood nickname?",
    "What is your pet name?",
    "Who is your favorite teacher?"
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VaultRecoveryQuestionScreen(
    appPreferences: AppPreferences,
    onNavigateBack: () -> Unit,
    onSaved: () -> Unit = onNavigateBack
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var step by remember { mutableIntStateOf(0) } // 0..2
    val questions = remember { DEFAULT_SECURITY_QUESTIONS.toMutableStateList() }
    val answers = remember { mutableStateListOf("", "", "") }
    var expanded by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            Column(modifier = Modifier.fillMaxWidth()) {
                TopAppBar(
                    title = {
                        Text(
                            text = "Set Security Questions",
                            fontWeight = FontWeight.Medium,
                            fontSize = 18.sp,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                    },
                    navigationIcon = {
                        IconButton(onClick = { if (step > 0) step-- else onNavigateBack() }) {
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
            Spacer(modifier = Modifier.height(36.dp))
            Text(
                text = "Set Security Question!",
                fontSize = 20.sp,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onBackground
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = "The security question lets you reset your password if you forgot in future",
                fontSize = 15.sp,
                color = MaterialTheme.colorScheme.onBackground,
                textAlign = TextAlign.Center,
                lineHeight = 19.sp
            )
            Spacer(modifier = Modifier.height(36.dp))

            // Step indicator 1 -> 2 -> 3
            Row(verticalAlignment = Alignment.CenterVertically) {
                (0..2).forEach { i ->
                    StepCircle(i + 1, active = i <= step)
                    if (i < 2) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_recover_arrow),
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onBackground,
                            modifier = Modifier
                                .padding(horizontal = 7.dp)
                                .size(width = 32.dp, height = 16.dp)
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(36.dp))
            Text(
                text = "Answer at least 3 Question",
                fontSize = 18.sp,
                color = MaterialTheme.colorScheme.onBackground
            )
            Spacer(modifier = Modifier.height(24.dp))

            // Question dropdown
            Box {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(38.dp)
                        .clip(RoundedCornerShape(19.dp))
                        .background(MaterialTheme.colorScheme.cardBackground)
                        .border(1.dp, MaterialTheme.colorScheme.onBackground.copy(alpha = 0.12f), RoundedCornerShape(19.dp))
                        .clickable { expanded = true }
                        .padding(start = 26.dp, end = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = questions[step],
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onBackground,
                        modifier = Modifier.weight(1f)
                    )
                    Icon(Icons.Default.ArrowDropDown, contentDescription = "Select question", tint = MaterialTheme.colorScheme.onBackground)
                }
                DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                    DEFAULT_SECURITY_QUESTIONS.forEach { q ->
                        DropdownMenuItem(text = { Text(q, fontSize = 15.sp) }, onClick = {
                            questions[step] = q
                            expanded = false
                        })
                    }
                }
            }
            Spacer(modifier = Modifier.height(12.dp))
            AnswerField(value = answers[step], onValueChange = { answers[step] = it })
            Spacer(modifier = Modifier.height(36.dp))

            // Reference: button sits under the field, "Next" then "Submit"
            Button(
                onClick = {
                    if (answers[step].isBlank()) {
                        Toast.makeText(context, "Please enter an answer", Toast.LENGTH_SHORT).show()
                        return@Button
                    }
                    if (questions.toSet().size < 3) {
                        Toast.makeText(context, "Please select different questions", Toast.LENGTH_SHORT).show()
                        return@Button
                    }
                    if (step < 2) {
                        step++
                    } else {
                        scope.launch {
                            appPreferences.setThreeSecurityQuestions(
                                questions[0], answers[0], questions[1], answers[1], questions[2], answers[2]
                            )
                            onSaved()
                        }
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(42.dp),
                shape = RoundedCornerShape(21.dp),
                colors = ButtonDefaults.buttonColors(containerColor = GalleryBlue)
            ) {
                Text(
                    text = if (step == 2) "Submit" else "Next",
                    color = Color.White,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}

/** Outlined answer field shared with Forgot Password (reference: black field, light outline). */
@Composable
internal fun AnswerField(value: String, onValueChange: (String) -> Unit) {
    // 40dp outlined field like the reference (OutlinedTextField can't go below 56dp)
    val focus = androidx.compose.ui.platform.LocalFocusManager.current
    androidx.compose.foundation.text.BasicTextField(
        value = value,
        onValueChange = onValueChange,
        singleLine = true,
        // ✓ on the keyboard closes it so the button underneath is reachable
        keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(imeAction = androidx.compose.ui.text.input.ImeAction.Done),
        keyboardActions = androidx.compose.foundation.text.KeyboardActions(onDone = { focus.clearFocus() }),
        textStyle = androidx.compose.ui.text.TextStyle(fontSize = 16.sp, color = MaterialTheme.colorScheme.onBackground),
        cursorBrush = androidx.compose.ui.graphics.SolidColor(GalleryBlue),
        modifier = Modifier
            .fillMaxWidth()
            .height(40.dp)
            .border(1.dp, MaterialTheme.colorScheme.onBackground.copy(alpha = 0.85f), RoundedCornerShape(20.dp))
            .padding(horizontal = 18.dp),
        decorationBox = { inner ->
            Box(contentAlignment = Alignment.CenterStart, modifier = Modifier.fillMaxSize()) {
                if (value.isEmpty()) Text("Your answer", fontSize = 16.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                inner()
            }
        }
    )
}

@Composable
private fun StepCircle(number: Int, active: Boolean) {
    val inactive = if (MaterialTheme.colorScheme.isDark) Color(0xFF2E2F33) else Color(0xFFE4E6E9)
    Box(
        modifier = Modifier
            .size(43.dp)
            .clip(CircleShape)
            .background(if (active) GalleryBlue else inactive),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = "$number",
            color = if (active) Color.White else MaterialTheme.colorScheme.onBackground,
            fontSize = 18.sp
        )
    }
}
