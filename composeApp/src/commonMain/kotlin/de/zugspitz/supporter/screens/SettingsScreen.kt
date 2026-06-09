package de.zugspitz.supporter.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import de.zugspitz.supporter.components.ScreenHeader
import de.zugspitz.supporter.data.CheckEventType
import de.zugspitz.supporter.data.LiveRunLink
import de.zugspitz.supporter.data.LiveRole
import de.zugspitz.supporter.presentation.state.LastLiveEventUiState
import de.zugspitz.supporter.presentation.state.SettingsUiState
import de.zugspitz.supporter.theme.SupporterColors
import de.zugspitz.supporter.theme.SupporterTheme
import org.jetbrains.compose.resources.stringResource
import zugspitz_supporter.composeapp.generated.resources.Res
import zugspitz_supporter.composeapp.generated.resources.create_support_code
import zugspitz_supporter.composeapp.generated.resources.live_code
import zugspitz_supporter.composeapp.generated.resources.live_code_empty
import zugspitz_supporter.composeapp.generated.resources.live_event_check_in
import zugspitz_supporter.composeapp.generated.resources.live_event_check_out
import zugspitz_supporter.composeapp.generated.resources.live_event_status
import zugspitz_supporter.composeapp.generated.resources.live_last_event
import zugspitz_supporter.composeapp.generated.resources.live_not_connected
import zugspitz_supporter.composeapp.generated.resources.live_role_runner
import zugspitz_supporter.composeapp.generated.resources.live_role_supporter
import zugspitz_supporter.composeapp.generated.resources.live_runner_name
import zugspitz_supporter.composeapp.generated.resources.live_sharing
import zugspitz_supporter.composeapp.generated.resources.live_sharing_hint
import zugspitz_supporter.composeapp.generated.resources.reset_all_data
import zugspitz_supporter.composeapp.generated.resources.reset_all_data_hint
import zugspitz_supporter.composeapp.generated.resources.settings_subtitle
import zugspitz_supporter.composeapp.generated.resources.settings_title
import kotlinx.coroutines.delay

@Composable
fun SettingsScreen(
    state: SettingsUiState,
    liveSharingEnabled: Boolean,
    onRoleSelected: (LiveRole) -> Unit,
    onRunCodeChanged: (String) -> Unit,
    onRunnerNameChanged: (String) -> Unit,
    onCreateRunCode: () -> Unit,
    onLiveSharingToggle: (Boolean) -> Unit,
    onResetClick: () -> Unit,
    modifier: Modifier = Modifier,
    isKeyboardVisible: Boolean = false,
) {
    val focusManager = LocalFocusManager.current
    val listState = rememberLazyListState()

    LaunchedEffect(isKeyboardVisible, liveSharingEnabled, state.liveRunLink.role) {
        if (isKeyboardVisible && liveSharingEnabled) {
            delay(120)
            listState.animateScrollToItem(1)
        }
    }

    LazyColumn(
        state = listState,
        modifier = modifier
            .fillMaxSize()
            .background(SupporterColors.Paper)
            .pointerInput(Unit) {
                detectTapGestures(onTap = { focusManager.clearFocus() })
            },
        contentPadding = PaddingValues(
            start = 18.dp,
            top = 18.dp,
            end = 18.dp,
            bottom = 18.dp,
        ),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            ScreenHeader(
                eyebrow = "",
                title = stringResource(Res.string.settings_title),
                subtitle = stringResource(Res.string.settings_subtitle),
            )
        }

        if (liveSharingEnabled) {
            item {
                LiveSharingSettingsCard(
                    state = state,
                    onRoleSelected = onRoleSelected,
                    onRunCodeChanged = onRunCodeChanged,
                    onRunnerNameChanged = onRunnerNameChanged,
                    onCreateRunCode = onCreateRunCode,
                    onLiveSharingToggle = onLiveSharingToggle,
                    onRunnerNameDone = { focusManager.clearFocus() },
                )
            }
        }

        if (!isKeyboardVisible) {
            item {
                ResetDataCard(onResetClick = onResetClick)
            }
        }
    }
}

@Composable
private fun LiveSharingSettingsCard(
    state: SettingsUiState,
    onRoleSelected: (LiveRole) -> Unit,
    onRunCodeChanged: (String) -> Unit,
    onRunnerNameChanged: (String) -> Unit,
    onCreateRunCode: () -> Unit,
    onLiveSharingToggle: (Boolean) -> Unit,
    onRunnerNameDone: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(SupporterColors.Card, RoundedCornerShape(8.dp))
            .padding(15.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Column(Modifier.weight(1f)) {
                Text(stringResource(Res.string.live_sharing), fontWeight = FontWeight.Black)
                Text(
                    stringResource(Res.string.live_sharing_hint),
                    style = MaterialTheme.typography.bodySmall,
                    color = SupporterColors.Muted,
                )
            }
            Switch(
                checked = state.liveRunLink.isEnabled,
                onCheckedChange = onLiveSharingToggle,
            )
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
            RoleSegment(
                label = stringResource(Res.string.live_role_runner),
                selected = state.liveRunLink.role == LiveRole.Runner,
                modifier = Modifier.weight(1f),
                onClick = { onRoleSelected(LiveRole.Runner) },
            )
            RoleSegment(
                label = stringResource(Res.string.live_role_supporter),
                selected = state.liveRunLink.role == LiveRole.Supporter,
                modifier = Modifier.weight(1f),
                onClick = { onRoleSelected(LiveRole.Supporter) },
            )
        }
        if (state.liveRunLink.role == LiveRole.Runner) {
            val hasRunCode = state.liveRunLink.runCode.isNotBlank()
            OutlinedTextField(
                value = state.liveRunLink.runnerName,
                onValueChange = onRunnerNameChanged,
                enabled = !hasRunCode,
                label = { Text(stringResource(Res.string.live_runner_name)) },
                singleLine = true,
                keyboardOptions = KeyboardOptions(
                    capitalization = KeyboardCapitalization.Words,
                    imeAction = ImeAction.Done,
                ),
                keyboardActions = KeyboardActions(onDone = { onRunnerNameDone() }),
                modifier = Modifier.fillMaxWidth(),
            )
            Button(
                onClick = onCreateRunCode,
                enabled = !hasRunCode,
                shape = RoundedCornerShape(8.dp),
                colors = ButtonDefaults.buttonColors(containerColor = SupporterColors.Pine),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(stringResource(Res.string.create_support_code), fontWeight = FontWeight.Black)
            }
            SupportCodeDisplay(
                runCode = state.liveRunLink.runCode,
                modifier = Modifier.fillMaxWidth(),
            )
        } else {
            OutlinedTextField(
                value = state.liveRunLink.runCode,
                onValueChange = onRunCodeChanged,
                label = { Text(stringResource(Res.string.live_code)) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
        }
        Text(
            "${stringResource(Res.string.live_last_event)} ${liveEventText(state.lastLiveEvent)}",
            style = MaterialTheme.typography.bodySmall,
            color = SupporterColors.Muted,
        )
    }
}

@Composable
private fun ResetDataCard(
    onResetClick: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(SupporterColors.Card, RoundedCornerShape(8.dp))
            .padding(15.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Text(
            stringResource(Res.string.reset_all_data_hint),
            style = MaterialTheme.typography.bodyMedium,
            color = SupporterColors.Muted,
        )
        Button(
            onClick = onResetClick,
            shape = RoundedCornerShape(8.dp),
            colors = ButtonDefaults.buttonColors(containerColor = SupporterColors.Pine),
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(stringResource(Res.string.reset_all_data), fontWeight = FontWeight.Black)
        }
    }
}

@Composable
private fun SupportCodeDisplay(
    runCode: String,
    modifier: Modifier = Modifier,
) {
    val displayedCode = if (runCode.isBlank()) stringResource(Res.string.live_code_empty) else runCode
    Column(
        modifier = modifier
            .border(1.dp, SupporterColors.Line, RoundedCornerShape(8.dp))
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Text(
            text = stringResource(Res.string.live_code),
            style = MaterialTheme.typography.bodySmall,
            color = SupporterColors.Muted,
            fontWeight = FontWeight.Bold,
        )
        SelectionContainer {
            Text(
                text = displayedCode,
                color = if (runCode.isBlank()) SupporterColors.Muted else SupporterColors.Pine,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Black,
            )
        }
    }
}

@Composable
private fun liveEventText(event: LastLiveEventUiState?): String {
    if (event == null) return stringResource(Res.string.live_not_connected)
    val action = when (event.type) {
        CheckEventType.CheckIn -> stringResource(Res.string.live_event_check_in)
        CheckEventType.CheckOut -> stringResource(Res.string.live_event_check_out)
    }
    return stringResource(Res.string.live_event_status, action, event.stationName, event.raceTime)
}

@Composable
private fun RoleSegment(
    label: String,
    selected: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    Text(
        text = label,
        color = if (selected) SupporterColors.Pine else SupporterColors.Muted,
        fontWeight = FontWeight.Black,
        modifier = modifier
            .clickable(onClick = onClick)
            .background(
                if (selected) SupporterColors.Mint else androidx.compose.ui.graphics.Color(0xFFEEF2EC),
                RoundedCornerShape(8.dp),
            )
            .then(
                if (selected) {
                    Modifier.border(1.dp, SupporterColors.Moss.copy(alpha = 0.35f), RoundedCornerShape(8.dp))
                } else {
                    Modifier
                },
            )
            .padding(vertical = 10.dp),
        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
    )
}

@Preview
@Composable
fun SettingsScreenPreview() {
    SupporterTheme {
        SettingsScreen(
            state = SettingsUiState(
                liveRunLink = LiveRunLink(
                    role = LiveRole.Supporter,
                    runCode = "ZUT-4821",
                    isEnabled = true,
                ),
                lastLiveEvent = LastLiveEventUiState(
                    type = CheckEventType.CheckIn,
                    stationName = "Hämmermoosalm",
                    raceTime = "06:42",
                ),
            ),
            liveSharingEnabled = true,
            onRoleSelected = {},
            onRunCodeChanged = {},
            onRunnerNameChanged = {},
            onCreateRunCode = {},
            onLiveSharingToggle = {},
            onResetClick = {},
        )
    }
}
