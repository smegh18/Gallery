package com.quizango.gallery.ui.vault

import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties
import com.quizango.gallery.R
import com.quizango.gallery.data.local.datastore.AppPreferences
import com.quizango.gallery.ui.browse.SharedMediaToolbar
import com.quizango.gallery.ui.theme.GalleryText
import com.quizango.gallery.ui.theme.sdp
import kotlinx.coroutines.launch

/** activity_vault_recovery_question: three steps, each a free choice of the seven questions plus an answer. */
@Composable
fun VaultRecoveryQuestionScreen(
    appPreferences: AppPreferences,
    onNavigateBack: () -> Unit,
    onSaved: () -> Unit = onNavigateBack
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val keyboard = LocalSoftwareKeyboardController.current
    val focus = LocalFocusManager.current
    val flow = remember { RecoveryQuestionFlow() }
    var version by remember { mutableIntStateOf(0) } // flow is plain Kotlin: bump to redraw after it changes
    var answer by remember { mutableStateOf("") }
    var saving by remember { mutableStateOf(false) }
    // keyed on version so a flow change redraws
    val step = remember(version) { flow.step }
    val remaining = remember(version) { flow.remaining }
    val selected = remember(version) { flow.selected }
    val buttonLabel = remember(version) { flow.buttonLabel }

    // System Back takes the same path as the toolbar Back (the NavHost wipes a half-made lock)
    BackHandler { onNavigateBack() }

    fun next() {
        if (saving) return
        when (val event = flow.next(answer)) {
            is RecoveryEvent.Toast -> Toast.makeText(context, context.getString(event.res), Toast.LENGTH_SHORT).show()
            RecoveryEvent.Advanced -> { answer = ""; version++ }
            is RecoveryEvent.Done -> {
                saving = true
                keyboard?.hide()
                focus.clearFocus()
                val (a, b, c) = event.entries
                scope.launch {
                    // AppPreferences hashes the normalised answers (D3)
                    appPreferences.setThreeSecurityQuestions(
                        a.first.toString(), a.second, b.first.toString(), b.second, c.first.toString(), c.second
                    )
                    onSaved()
                }
            }
        }
    }

    val titleColor = colorResource(R.color.titleColor)
    Column(modifier = Modifier.fillMaxSize().background(colorResource(R.color.backgroundColor))) {
        SharedMediaToolbar(title = stringResource(R.string.set_security_questions_title), onBack = onNavigateBack)
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
                text = stringResource(R.string.set_security_question),
                style = GalleryText.BodyXXLargeSemiBold,
                color = colorResource(R.color.black),
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 30.sdp)
            )
            Text(
                text = stringResource(R.string.set_security_question_subtitle),
                style = GalleryText.BodyLargeRegular,
                color = colorResource(R.color.security_question_bottom_color),
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(start = 20.sdp, end = 20.sdp, top = 4.sdp)
            )
            Row(modifier = Modifier.padding(top = 30.sdp), verticalAlignment = Alignment.CenterVertically) {
                for (i in 0..2) {
                    StepCircle(i + 1, reached = i <= step, titleColor = titleColor)
                    if (i < 2) {
                        Image(
                            painter = painterResource(R.drawable.ic_recover_arrow),
                            contentDescription = null, // decorative
                            modifier = Modifier.padding(horizontal = 5.sdp)
                        )
                    }
                }
            }
            Text(
                text = stringResource(R.string.answer_at_least_Question),
                style = GalleryText.BodyXLargeMedium,
                color = titleColor,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 30.sdp)
            )
            val labels = remaining.map { stringResource(questionRes(it.toString())!!) }
            QuestionSelector(
                labels = labels,
                selected = remaining.indexOf(selected),
                onSelect = { flow.select(remaining[it]); version++ },
                modifier = Modifier.padding(start = 16.sdp, end = 16.sdp, top = 20.sdp)
            )
            AnswerField(
                value = answer,
                onValueChange = { answer = it },
                modifier = Modifier.padding(start = 16.sdp, end = 16.sdp, top = 10.sdp)
            )
            Text(
                text = stringResource(buttonLabel),
                style = GalleryText.BodyLargeSemiBold,
                color = colorResource(R.color.onlyWhite),
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .padding(start = 16.sdp, end = 16.sdp, top = 30.sdp)
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(25.sdp))
                    .background(colorResource(R.color.dark_blue))
                    .clickable(role = Role.Button) { next() }
                    .padding(vertical = 10.sdp)
            )
        }
    }
}

/** Numbered step circle: ic_recover_select once reached, ic_recover_unselect while pending. */
@Composable
private fun StepCircle(number: Int, reached: Boolean, titleColor: androidx.compose.ui.graphics.Color) {
    val stroke = colorResource(R.color.recoverUnselectBorder)
    Box(
        modifier = Modifier
            .size(36.sdp)
            .background(colorResource(if (reached) R.color.recoverSelectBg else R.color.recoverUnselectBg), CircleShape)
            .border(BorderStroke(if (reached) 1.5.dp else 1.dp, stroke), CircleShape),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = "$number",
            style = GalleryText.BodyXLargeMedium,
            color = if (reached) colorResource(R.color.onlyWhite) else titleColor
        )
    }
}

/**
 * questionSpinner + item_question: the closed row and the popup rows share ic_recover_spinner_bg.
 * Also used by Forgot Password.
 */
@Composable
internal fun QuestionSelector(
    labels: List<String>,
    selected: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    var open by remember { mutableStateOf(false) }
    var size by remember { mutableStateOf(androidx.compose.ui.unit.IntSize.Zero) }
    val shape = RoundedCornerShape(12.sdp)
    val frame = Modifier
        .background(colorResource(R.color.ic_recover_spinner_bg_color), shape)
        .border(1.dp, colorResource(R.color.recoverUnselectBorder), shape)
    val rowStyle = GalleryText.BodySmallMedium
    val rowColor = colorResource(R.color.titleColor)
    Box(modifier = modifier.fillMaxWidth().onSizeChanged { size = it }) {
        Text(
            text = labels.getOrElse(selected) { "" },
            style = rowStyle,
            color = rowColor,
            modifier = frame
                .fillMaxWidth()
                .clickable(enabled = labels.size > 1, role = Role.DropdownList) { open = true }
                .padding(horizontal = 15.sdp, vertical = 10.sdp)
        )
        if (open) {
            Popup(
                offset = IntOffset(0, size.height),
                onDismissRequest = { open = false },
                properties = PopupProperties(focusable = true)
            ) {
                Column(modifier = frame.width(with(LocalDensity.current) { size.width.toDp() })) {
                    labels.forEachIndexed { i, label ->
                        Text(
                            text = label,
                            style = rowStyle,
                            color = rowColor,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { open = false; onSelect(i) }
                                .padding(horizontal = 15.sdp, vertical = 10.sdp)
                        )
                    }
                }
            }
        }
    }
}

/** securityAnswer: one line on ic_recover_answer_bg, hinted your_answer. Shared with Forgot Password. */
@Composable
internal fun AnswerField(value: String, onValueChange: (String) -> Unit, modifier: Modifier = Modifier) {
    val focus = LocalFocusManager.current
    val shape = RoundedCornerShape(12.sdp)
    val textColor = colorResource(R.color.black)
    BasicTextField(
        value = value,
        onValueChange = onValueChange,
        singleLine = true,
        // the keyboard's check mark closes it so the button underneath is reachable
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
        keyboardActions = KeyboardActions(onDone = { focus.clearFocus() }),
        textStyle = GalleryText.BodyMediumRegular.copy(color = textColor),
        cursorBrush = SolidColor(textColor),
        modifier = modifier
            .fillMaxWidth()
            .background(colorResource(R.color.white), shape)
            .border(1.dp, colorResource(R.color.security_answer_stroke), shape)
            .padding(horizontal = 15.sdp, vertical = 10.sdp),
        decorationBox = { inner ->
            Box(contentAlignment = Alignment.CenterStart) {
                if (value.isEmpty()) {
                    Text(
                        text = stringResource(R.string.your_answer),
                        style = GalleryText.BodyMediumRegular,
                        color = colorResource(R.color.hintLanguageColor)
                    )
                }
                inner()
            }
        }
    )
}
