package com.quizango.gallery.ui.vault

import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextAlign
import com.quizango.gallery.R
import com.quizango.gallery.data.local.datastore.AppPreferences
import com.quizango.gallery.ui.browse.SharedMediaToolbar
import com.quizango.gallery.ui.theme.GalleryText
import com.quizango.gallery.ui.theme.sdp
import kotlinx.coroutines.launch

/** activity_vault_forgot_password: pick one of the three saved questions and answer it. */
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
    var answer by remember { mutableStateOf("") }

    // System Back takes the toolbar Back path (the lock-aware routing in the NavHost)
    BackHandler { onNavigateBack() }

    // A stored id shows localised; a question text saved by an older build shows as it is
    val labels = pairs.map { (q, _) -> questionRes(q)?.let { stringResource(it) } ?: q }

    Column(modifier = Modifier.fillMaxSize().background(colorResource(R.color.backgroundColor))) {
        SharedMediaToolbar(title = stringResource(R.string.forgot_password), onBack = onNavigateBack)
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .navigationBarsPadding()
                .imePadding(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = stringResource(R.string.password_recovery_option),
                style = GalleryText.BodyXXLargeSemiBold,
                color = colorResource(R.color.black),
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 40.sdp)
            )
            Text(
                text = stringResource(R.string.password_recovery_questions),
                style = GalleryText.BodyLargeRegular,
                color = colorResource(R.color.titleColor),
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(start = 40.sdp, end = 40.sdp, top = 8.sdp)
            )
            QuestionSelector(
                labels = labels,
                selected = selected,
                onSelect = { selected = it },
                modifier = Modifier.padding(start = 16.sdp, end = 16.sdp, top = 60.sdp)
            )
            AnswerField(
                value = answer,
                onValueChange = { answer = it },
                modifier = Modifier.padding(start = 16.sdp, end = 16.sdp, top = 10.sdp)
            )
            Text(
                text = stringResource(R.string.confirm),
                style = GalleryText.BodyLargeSemiBold,
                color = colorResource(R.color.onlyWhite),
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .padding(top = 30.sdp, bottom = 5.sdp)
                    .clip(RoundedCornerShape(25.sdp))
                    .background(colorResource(R.color.dark_blue))
                    .clickable(role = Role.Button) {
                        // Only a right answer touches stored state; every toast leaves the lock as it is
                        when (val event = forgotAnswerEvent(answer, pairs.getOrNull(selected)?.second)) {
                            is ForgotEvent.Toast -> Toast.makeText(context, context.getString(event.res), Toast.LENGTH_SHORT).show()
                            ForgotEvent.Ok -> scope.launch {
                                appPreferences.clearVaultLock()
                                onAnswerVerified()
                            }
                        }
                    }
                    .padding(horizontal = 95.sdp, vertical = 10.sdp)
            )
        }
    }
}
