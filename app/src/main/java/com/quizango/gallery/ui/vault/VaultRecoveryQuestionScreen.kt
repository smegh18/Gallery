package com.quizango.gallery.ui.vault

import android.widget.Toast
import androidx.compose.foundation.background
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
import kotlinx.coroutines.launch

val DEFAULT_SECURITY_QUESTIONS = listOf(
    "What was your childhood nickname?",
    "What is your favorite pet's name?",
    "What city were you born in?",
    "What is your mother's maiden name?",
    "What primary school did you attend?",
    "What was your first car model?",
    "What is your favorite movie?",
    "What was the name of your first teacher?"
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VaultRecoveryQuestionScreen(
    appPreferences: AppPreferences,
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var currentStep by remember { mutableIntStateOf(1) } // 1, 2, 3

    var selectedQuestion1 by remember { mutableStateOf(DEFAULT_SECURITY_QUESTIONS[0]) }
    var answer1 by remember { mutableStateOf("") }

    var selectedQuestion2 by remember { mutableStateOf(DEFAULT_SECURITY_QUESTIONS[1]) }
    var answer2 by remember { mutableStateOf("") }

    var selectedQuestion3 by remember { mutableStateOf(DEFAULT_SECURITY_QUESTIONS[2]) }
    var answer3 by remember { mutableStateOf("") }

    var isDropdownExpanded by remember { mutableStateOf(false) }

    val currentQuestion = when (currentStep) {
        1 -> selectedQuestion1
        2 -> selectedQuestion2
        else -> selectedQuestion3
    }

    val currentAnswer = when (currentStep) {
        1 -> answer1
        2 -> answer2
        else -> answer3
    }

    Scaffold(
        topBar = {
            Column(modifier = Modifier.fillMaxWidth()) {
                TopAppBar(
                    title = {
                        Text(
                            text = "Set Security Question",
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 18.sp,
                            color = Color(0xFF1E3354)
                        )
                    },
                    navigationIcon = {
                        IconButton(onClick = {
                            if (currentStep > 1) {
                                currentStep--
                            } else {
                                onNavigateBack()
                            }
                        }) {
                            Icon(
                                painter = painterResource(id = R.drawable.ic_back),
                                contentDescription = "Back",
                                tint = Color.Unspecified,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.background
                    )
                )
                HorizontalDivider(thickness = 1.dp, color = Color(0xFFE4E6E9))
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
                            if (currentAnswer.trim().isEmpty()) {
                                Toast.makeText(context, "Please enter an answer", Toast.LENGTH_SHORT).show()
                                return@Button
                            }

                            if (currentStep < 3) {
                                currentStep++
                            } else {
                                scope.launch {
                                    appPreferences.setThreeSecurityQuestions(
                                        selectedQuestion1, answer1.trim(),
                                        selectedQuestion2, answer2.trim(),
                                        selectedQuestion3, answer3.trim()
                                    )
                                    Toast.makeText(context, "Security questions saved", Toast.LENGTH_SHORT).show()
                                    onNavigateBack()
                                }
                            }
                        },
                        enabled = currentAnswer.trim().isNotEmpty(),
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
                            text = if (currentStep == 3) "Submit" else "Next",
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
                .padding(horizontal = 20.dp, vertical = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Header matching activity_vault_recovery_question.xml
            Text(
                text = "Set Security Question!",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF1E3354)
            )

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = "The security question lets you reset your password if you forgot in future",
                fontSize = 13.sp,
                color = Color(0xFF707C93),
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Step Indicator: 1 -> 2 -> 3 matching activity_vault_recovery_question.xml
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                StepCircle(stepNumber = 1, isActive = currentStep >= 1)
                Spacer(modifier = Modifier.width(6.dp))
                Icon(
                    painter = painterResource(id = R.drawable.ic_recover_arrow),
                    contentDescription = null,
                    tint = Color.Unspecified,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                StepCircle(stepNumber = 2, isActive = currentStep >= 2)
                Spacer(modifier = Modifier.width(6.dp))
                Icon(
                    painter = painterResource(id = R.drawable.ic_recover_arrow),
                    contentDescription = null,
                    tint = Color.Unspecified,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                StepCircle(stepNumber = 3, isActive = currentStep >= 3)
            }

            Spacer(modifier = Modifier.height(28.dp))

            Text(
                text = "Answer at least 1 Question",
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color(0xFF1E3354),
                modifier = Modifier.align(Alignment.Start)
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Question Spinner Box
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFFF2F4F7), RoundedCornerShape(12.dp))
                    .clickable { isDropdownExpanded = true }
                    .padding(horizontal = 14.dp, vertical = 14.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = currentQuestion,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color(0xFF1E3354),
                        modifier = Modifier.weight(1f)
                    )
                    Icon(
                        imageVector = Icons.Default.ArrowDropDown,
                        contentDescription = "Select question",
                        tint = Color(0xFF1E3354)
                    )
                }

                DropdownMenu(
                    expanded = isDropdownExpanded,
                    onDismissRequest = { isDropdownExpanded = false },
                    modifier = Modifier.fillMaxWidth(0.85f)
                ) {
                    DEFAULT_SECURITY_QUESTIONS.forEach { question ->
                        DropdownMenuItem(
                            text = {
                                Text(
                                    text = question,
                                    fontSize = 14.sp,
                                    color = if (question == currentQuestion) GalleryBlue else Color(0xFF1E3354),
                                    fontWeight = if (question == currentQuestion) FontWeight.Bold else FontWeight.Normal
                                )
                            },
                            onClick = {
                                when (currentStep) {
                                    1 -> selectedQuestion1 = question
                                    2 -> selectedQuestion2 = question
                                    3 -> selectedQuestion3 = question
                                }
                                isDropdownExpanded = false
                            }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Answer Input Box
            OutlinedTextField(
                value = currentAnswer,
                onValueChange = { newText ->
                    when (currentStep) {
                        1 -> answer1 = newText
                        2 -> answer2 = newText
                        3 -> answer3 = newText
                    }
                },
                placeholder = { Text("Your answer", color = Color(0xFFB0B7C3), fontSize = 14.sp) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = GalleryBlue,
                    unfocusedBorderColor = Color(0xFFE4E6E9),
                    cursorColor = GalleryBlue
                )
            )
        }
    }
}

@Composable
private fun StepCircle(stepNumber: Int, isActive: Boolean) {
    Box(
        modifier = Modifier
            .size(36.dp)
            .clip(CircleShape)
            .background(if (isActive) GalleryBlue else Color(0xFFE4E6E9)),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = "$stepNumber",
            color = if (isActive) Color.White else Color(0xFF707C93),
            fontSize = 15.sp,
            fontWeight = FontWeight.Bold
        )
    }
}
