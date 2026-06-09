package de.zugspitz.supporter.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.input.pointer.pointerInput
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
    onAutoCheckInOutToggle: (Boolean) -> Unit,
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
                    onAutoCheckInOutToggle = onAutoCheckInOutToggle,
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
            onAutoCheckInOutToggle = {},
            onResetClick = {},
        )
    }
}
