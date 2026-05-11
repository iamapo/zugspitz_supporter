package de.zugspitz.supporter

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.remember
import de.zugspitz.supporter.components.AppBottomBar
import de.zugspitz.supporter.components.AppTab
import de.zugspitz.supporter.presentation.SupporterViewModel
import de.zugspitz.supporter.screens.RaceSelectionScreen
import de.zugspitz.supporter.screens.RoleSelectionScreen
import de.zugspitz.supporter.screens.RunSummaryScreen
import de.zugspitz.supporter.screens.SettingsScreen
import de.zugspitz.supporter.screens.SetupScreen
import de.zugspitz.supporter.screens.SupportCodeScreen
import de.zugspitz.supporter.screens.VpPauseSetupScreen
import de.zugspitz.supporter.screens.VpCardScreen
import de.zugspitz.supporter.screens.VpListScreen
import de.zugspitz.supporter.theme.SupporterColors
import de.zugspitz.supporter.theme.SupporterTheme
import androidx.compose.ui.tooling.preview.Preview
import de.zugspitz.supporter.data.LiveRaceRepository
import de.zugspitz.supporter.data.NoOpLiveRaceRepository

@Composable
fun SupporterApp(
    liveRaceRepository: LiveRaceRepository = NoOpLiveRaceRepository(),
    liveSharingEnabled: Boolean = AppFeatureFlags.LiveSharingEnabled,
) {
    SupporterTheme {
        SupporterAppRoot(
            liveRaceRepository = liveRaceRepository,
            liveSharingEnabled = liveSharingEnabled,
        )
    }
}

@Composable
fun SupporterAppRoot(
    liveRaceRepository: LiveRaceRepository = NoOpLiveRaceRepository(),
    liveSharingEnabled: Boolean = AppFeatureFlags.LiveSharingEnabled,
) {
    val viewModel = remember(liveRaceRepository, liveSharingEnabled) {
        SupporterViewModel(
            liveRaceRepository = liveRaceRepository,
            liveSharingEnabled = liveSharingEnabled,
        )
    }
    val state by viewModel.uiState.collectAsState()

    Scaffold(
        containerColor = SupporterColors.Paper,
        bottomBar = if (state.tab == AppTab.Role || state.tab == AppTab.SupportCode || state.tab == AppTab.Race || state.tab == AppTab.Setup || state.tab == AppTab.PauseSetup) {
            {}
        } else {
            {
                AppBottomBar(
                    selectedTab = state.tab,
                    onTabSelected = viewModel::onTabSelected,
                )
            }
        },
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(SupporterColors.Paper)
                .padding(padding),
        ) {
            when (state.tab) {
                AppTab.Role -> RoleSelectionScreen(
                    liveSharingEnabled = liveSharingEnabled,
                    onRunnerSelected = viewModel::onRunnerModeSelected,
                    onSupporterSelected = viewModel::onSupporterModeSelected,
                )
                AppTab.SupportCode -> SupportCodeScreen(
                    liveRunLink = state.settings.liveRunLink,
                    onRunCodeChanged = viewModel::onRunCodeChanged,
                    onConnectClick = viewModel::onSupportCodeConnect,
                    onContinueWithoutCodeClick = viewModel::onContinueWithoutSupportCode,
                )
                AppTab.Race -> RaceSelectionScreen(
                    selectedRaceId = if (state.setup.hasSelectedRace) state.setup.estimate.raceId else "",
                    onRaceSelected = viewModel::onRaceSelected,
                )
                AppTab.Setup -> SetupScreen(
                    estimate = state.setup.estimate,
                    onEstimateChange = viewModel::onEstimateChange,
                    onCalculateClick = viewModel::onCalculateClick,
                )
                AppTab.PauseSetup -> VpPauseSetupScreen(
                    raceId = state.setup.estimate.raceId,
                    pauseMinutesBySection = state.setup.pauseMinutesBySection,
                    onPauseMinutesChange = viewModel::onPauseMinutesChanged,
                    onContinueClick = viewModel::onPauseSetupContinue,
                )
                AppTab.Vp -> VpCardScreen(
                    projection = state.vp.projection,
                    selectedIndex = state.vp.selectedIndex,
                    canEditCheckIns = !state.settings.liveRunLink.canSubscribe,
                    onPageChanged = viewModel::onVpPageChanged,
                    onCheckInNowClick = viewModel::onCheckInNowSave,
                    onCheckOutClick = viewModel::onCheckOutNowSave,
                )
                AppTab.Summary -> RunSummaryScreen(
                    projection = state.vp.projection,
                    onStationClick = viewModel::onStationSelected,
                )
                AppTab.List -> VpListScreen(
                    projection = state.vp.projection,
                    onStationClick = viewModel::onStationSelected,
                )
                AppTab.Settings -> SettingsScreen(
                    state = state.settings,
                    liveSharingEnabled = liveSharingEnabled,
                    onRoleSelected = viewModel::onLiveRoleSelected,
                    onRunCodeChanged = viewModel::onRunCodeChanged,
                    onCreateRunCode = viewModel::onCreateRunCode,
                    onLiveSharingToggle = viewModel::onLiveSharingToggle,
                    onResetClick = viewModel::onResetAllData,
                )
            }

        }
    }
}

@Preview
@Composable
fun SupporterAppPreview() {
    SupporterApp()
}
