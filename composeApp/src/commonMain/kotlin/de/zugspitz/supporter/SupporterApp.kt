package de.zugspitz.supporter

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.draw.alpha
import de.zugspitz.supporter.components.AppBottomBar
import de.zugspitz.supporter.components.AppTab
import de.zugspitz.supporter.presentation.SupporterViewModel
import de.zugspitz.supporter.screens.RaceSelectionScreen
import de.zugspitz.supporter.screens.RoleSelectionScreen
import de.zugspitz.supporter.screens.OfflineMapScreen
import de.zugspitz.supporter.screens.RunOverviewScreen
import de.zugspitz.supporter.screens.SettingsScreen
import de.zugspitz.supporter.screens.SetupScreen
import de.zugspitz.supporter.screens.SupportCodeScreen
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
        bottomBar = if (state.tab == AppTab.Role || state.tab == AppTab.SupportCode || state.tab == AppTab.Race || state.tab == AppTab.Setup) {
            {}
        } else {
            {
                AppBottomBar(
                    selectedTab = if (state.tab == AppTab.Summary) AppTab.List else state.tab,
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
            var keepMapMounted by remember { mutableStateOf(false) }
            if (state.tab == AppTab.Map) {
                keepMapMounted = true
            }

            if (keepMapMounted) {
                OfflineMapScreen(
                    projection = state.vp.projection,
                    offlineMap = state.offlineMap,
                    onDownloadStart = viewModel::onOfflineMapDownloadStart,
                    onDownloadProgress = viewModel::onOfflineMapDownloadProgress,
                    modifier = Modifier
                        .fillMaxSize()
                        .alpha(if (state.tab == AppTab.Map) 1f else 0f),
                )
            }

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
                    pauseMinutesBySection = state.setup.pauseMinutesBySection,
                    onEstimateChange = viewModel::onEstimateChange,
                    onPauseMinutesChange = viewModel::onPauseMinutesChanged,
                    onCalculateClick = viewModel::onCalculateClick,
                )
                AppTab.PauseSetup -> SetupScreen(
                    estimate = state.setup.estimate,
                    pauseMinutesBySection = state.setup.pauseMinutesBySection,
                    onEstimateChange = viewModel::onEstimateChange,
                    onPauseMinutesChange = viewModel::onPauseMinutesChanged,
                    onCalculateClick = viewModel::onCalculateClick,
                )
                AppTab.Vp -> VpCardScreen(
                    projection = state.vp.projection,
                    selectedIndex = state.vp.selectedIndex,
                    canEditCheckIns = !state.settings.liveRunLink.canSubscribe,
                    onPageChanged = viewModel::onVpPageChanged,
                    onCheckInNowClick = viewModel::onCheckInNowSave,
                    onCheckOutClick = viewModel::onCheckOutNowSave,
                )
                AppTab.Summary -> RunOverviewScreen(
                    projection = state.vp.projection,
                    onStationClick = viewModel::onStationSelected,
                )
                AppTab.Map -> Unit
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
